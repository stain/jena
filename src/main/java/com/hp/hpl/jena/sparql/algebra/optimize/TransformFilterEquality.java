/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hp.hpl.jena.sparql.algebra.optimize;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.List ;
import java.util.Set ;

import org.openjena.atlas.lib.Pair ;

import com.hp.hpl.jena.query.ARQ ;
import com.hp.hpl.jena.sparql.algebra.Op ;
import com.hp.hpl.jena.sparql.algebra.OpVars ;
import com.hp.hpl.jena.sparql.algebra.TransformCopy ;
import com.hp.hpl.jena.sparql.algebra.op.* ;
import com.hp.hpl.jena.sparql.core.Substitute ;
import com.hp.hpl.jena.sparql.core.Var ;
import com.hp.hpl.jena.sparql.expr.* ;

public class TransformFilterEquality extends TransformCopy
{
    // The approach taken for { OPTIONAL{} OPTIONAL{} } is more general ... and better?
    // Still need to be careful of double-nested OPTIONALS as intermedates of a different
    // value can block overall results so don't mask immediately.
    public TransformFilterEquality()
    { }
    
    @Override
    public Op transform(OpFilter opFilter, Op subOp)
    {
        Op op = apply(opFilter.getExprs(), subOp) ;
        if ( op == null )
            return super.transform(opFilter, subOp) ;
        return op ;
    }
    
    private static Op apply(ExprList exprs, Op subOp)
    {
        // ---- Find and extract any equality filters.
        Pair<List<Pair<Var, NodeValue>>, ExprList> p = preprocessFilterEquality(exprs) ;
        if ( p == null || p.getLeft().size() == 0 )
            return null ;
        
        List<Pair<Var, NodeValue>> equalities = p.getLeft() ;
        Collection<Var> varsMentioned = varsMentionedInEqualityFilters(equalities) ;
        ExprList remaining = p.getRight() ;
        
        // ---- Check if the subOp is the right shape to transform.
        Op op = subOp ;
        
        // Special case : deduce that the filter will always "eval unbound"
        // hence elimate all rows.  Return the empty table. 
        
        if ( testSpecialCaseUnused(subOp, equalities, remaining))
            return OpTable.empty() ;
        
        // Special case: the deep left op of a OpConditional/OpLeftJoin is unit table.
        // This is 
        // { OPTIONAL{P1} OPTIONAL{P2} ... FILTER(?x = :x) } 
        if ( testSpecialCase1(subOp, equalities, remaining))
        {
            // Find backbone of ops
            List<Op> ops = extractOptionals(subOp) ;
            ops = processSpecialCase1(ops, equalities) ;
            // Put back together
            op = rebuild((Op2)subOp, ops) ;
            // Put all filters - either we optimized, or we left alone.
            // Either way, the complete set of filter expressions.
            op = OpFilter.filter(exprs, op) ;
            return op ;
        }
        
        // ---- Transform

        if ( ! safeToTransform(varsMentioned, op) )
            return null ;
        for ( Pair<Var, NodeValue> equalityTest : equalities )
            op = processFilterWorker(op, equalityTest.getLeft(), equalityTest.getRight()) ;

        // ---- Place any filter expressions around the processed sub op. 
        if ( remaining.size() > 0 )
            op = OpFilter.filter(remaining, op) ;
        return op ;
    }

    // --- find and extract 
    private static Pair<List<Pair<Var, NodeValue>>, ExprList> preprocessFilterEquality(ExprList exprs)
    {
        List<Pair<Var, NodeValue>> exprsFilterEquality = new ArrayList<Pair<Var, NodeValue>>() ;
        ExprList exprsOther = new ExprList() ;
        for (  Expr e : exprs.getList() )
        {
            Pair<Var, NodeValue> p = preprocess(e) ;
            if ( p != null )
                exprsFilterEquality.add(p) ;
            else
                exprsOther.add(e) ;
        }
        if ( exprsFilterEquality.size() == 0 )
            return null ;
        return Pair.create(exprsFilterEquality, exprsOther) ;
    }
    
    private static Pair<Var, NodeValue> preprocess(Expr e)
    {
        if ( !(e instanceof E_Equals) && !(e instanceof E_SameTerm) )
            return null ;

        ExprFunction2 eq = (ExprFunction2)e ;
        Expr left = eq.getArg1() ;
        Expr right = eq.getArg2() ;

        Var var = null ;
        NodeValue constant = null ;

        if ( left.isVariable() && right.isConstant() )
        {
            var = left.asVar() ;
            constant = right.getConstant() ;
        }
        else if ( right.isVariable() && left.isConstant() )
        {
            var = right.asVar() ;
            constant = left.getConstant() ;
        }

        if ( var == null || constant == null )
            return null ;

        // Corner case: sameTerm is false for string/plain literal, 
        // but true in the graph for graph matching. 
        if (e instanceof E_SameTerm)
        {
            if ( ! ARQ.isStrictMode() && constant.isString() )
                return null ;
        }
        
        // Final check for "=" where a FILTER = can do value matching when the graph does not.
        if ( e instanceof E_Equals )
        {
            // Value based?
            if ( ! ARQ.isStrictMode() && constant.isLiteral() )
                return null ;
        }
        
        return Pair.create(var, constant) ;
    }

    private static Collection<Var> varsMentionedInEqualityFilters(List<Pair<Var, NodeValue>> equalities)
    {
        List<Var> vars = new ArrayList<Var>() ;
        for ( Pair<Var, NodeValue> p : equalities )
            vars.add(p.getLeft()) ;
        return vars ;
    }

    private static boolean safeToTransform(Collection<Var> varsEquality, Op op)
    {
        if ( op instanceof OpBGP || op instanceof OpQuadPattern )
            return true ;
        
        // This will be applied also in sub-calls of the Transform but queries 
        // are very rarely so deep that it matters. 
        if ( op instanceof OpSequence )
        {
            OpN opN = (OpN)op ;
            for ( Op subOp : opN.getElements() )
            {
                if ( ! safeToTransform(varsEquality, subOp) )
                    return false ;
            }
            return true ; 
        }
        
        if ( op instanceof OpJoin || op instanceof OpUnion)
        {
            Op2 op2 = (Op2)op ;
            return safeToTransform(varsEquality, op2.getLeft()) && safeToTransform(varsEquality, op2.getRight()) ; 
        }

        // Not safe unless filter variables are mentioned on the LHS. 
        if ( op instanceof OpConditional || op instanceof OpLeftJoin )
        {
            Op2 opleftjoin = (Op2)op ;
            
            if ( ! safeToTransform(varsEquality, opleftjoin.getLeft()) || 
                 ! safeToTransform(varsEquality, opleftjoin.getRight()) )
                return false ;
            
            // Not only must the left and right be safe to transform,
            // but the equality variable must be known to be always set. 

            // If the varsLeft are disjoint from assigned vars,
            // we may be able to push assign down right
            // (this generalises the unit table case specialcase1)
            // Needs more investigation.
            
            Op opLeft = opleftjoin.getLeft() ;
            Set<Var> varsLeft = OpVars.patternVars(opLeft) ;
            if ( varsLeft.containsAll(varsEquality) )
                return true ;
            return false ;
        }        
        
        if ( op instanceof OpGraph )
        {
            OpGraph opg = (OpGraph)op ;
            return safeToTransform(varsEquality, opg.getSubOp()) ;
        }
        
        return false ;
    }
    
    // -- A special case

    private static boolean testSpecialCaseUnused(Op op, List<Pair<Var, NodeValue>> equalities, ExprList remaining)
    {
        // If the op does not contain the var at all, for some equality
        // then the filter expression wil/ be "eval unbound" i.e. false.
        // We can return empty table.
        Set<Var> patternVars = OpVars.patternVars(op) ;
        for ( Pair<Var, NodeValue> p : equalities )
        {
            if ( ! patternVars.contains(p.getLeft()))
                return true ;
        }
        return false ;
    }
    
    // If a sequence of OPTIONALS, and nothing prior to the first, we end up with
    // a unit table on the left sid of a next of LeftJoin/conditionals.

    private static boolean testSpecialCase1(Op op, List<Pair<Var, NodeValue>> equalities , ExprList remaining )
    {
        while ( op instanceof OpConditional || op instanceof OpLeftJoin )
        {
            Op2 opleftjoin2 = (Op2)op ;
            op = opleftjoin2.getLeft() ;
        }
        return isUnitTable(op) ;
    }
    
    private static List<Op> extractOptionals(Op op)
    {
        List<Op> chain = new ArrayList<Op>() ;
        while ( op instanceof OpConditional || op instanceof OpLeftJoin )
        {
            Op2 opleftjoin2 = (Op2)op ;
            chain.add(opleftjoin2.getRight()) ;
            op = opleftjoin2.getLeft() ;
        }
        return chain ;
    }

    private static List<Op> processSpecialCase1(List<Op> ops, List<Pair<Var, NodeValue>> equalities)
    {
        List<Op> ops2 = new ArrayList<Op>() ;
        Collection<Var> vars = varsMentionedInEqualityFilters(equalities) ;
        
        for ( Op op : ops )
        {
            Op op2 = op ;
            if ( safeToTransform(vars, op) )
            {
                for ( Pair<Var, NodeValue> p : equalities )
                        op2 = processFilterWorker(op, p.getLeft(), p.getRight()) ;
            }
            ops2.add(op2) ;
        }
        return ops2 ;
    }

    private static Op rebuild(Op2 subOp, List<Op> ops)
    {
        Op chain = OpTable.unit() ; 
        for ( Op op : ops )
        {
            chain = subOp.copy(chain, op) ;
        }
        return chain ;
    }
  
    private static boolean isUnitTable(Op op)
    {
        if (op instanceof OpTable )
        {
            if ( ((OpTable)op).isJoinIdentity() )
                return true;  
        }
        return false ;
    }
    
    // ---- Transformation
        
    private static Op processFilterWorker(Op op, Var var, NodeValue constant)
    {
        return subst(op, var, constant) ;
    }
    
    private static Op subst(Op subOp , Var var, NodeValue nv)
    {
        Op op = Substitute.substitute(subOp, var, nv.asNode()) ;
        return OpAssign.assign(op, var, nv) ;
    }
    
    // Helper for TransformFilterDisjunction.
    
    /** Apply the FilterEquality transform or return null if no change */

    static Op processFilter(Expr e, Op subOp)
    {
        return apply(new ExprList(e), subOp) ;
    }
}
