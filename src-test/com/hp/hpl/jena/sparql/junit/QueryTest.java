/*
 * (c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.sparql.junit;

import java.io.IOException ;
import java.io.PrintStream ;
import java.io.PrintWriter ;
import java.util.ArrayList ;
import java.util.HashSet ;
import java.util.Iterator ;
import java.util.List ;
import java.util.Set ;

import org.openjena.riot.checker.CheckerLiterals ;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype ;
import com.hp.hpl.jena.query.* ;
import com.hp.hpl.jena.rdf.model.Model ;
import com.hp.hpl.jena.rdf.model.ModelFactory ;
import com.hp.hpl.jena.rdf.model.Property ;
import com.hp.hpl.jena.rdf.model.Resource ;
import com.hp.hpl.jena.rdf.model.Statement ;
import com.hp.hpl.jena.rdf.model.StmtIterator ;
import com.hp.hpl.jena.shared.JenaException ;
import com.hp.hpl.jena.sparql.engine.QueryIterator ;
import com.hp.hpl.jena.sparql.engine.ResultSetStream ;
import com.hp.hpl.jena.sparql.engine.binding.Binding ;
import com.hp.hpl.jena.sparql.engine.iterator.QueryIterPlainWrapper ;
import com.hp.hpl.jena.sparql.resultset.RSCompare ;
import com.hp.hpl.jena.sparql.resultset.ResultSetRewindable ;
import com.hp.hpl.jena.sparql.resultset.SPARQLResult ;

import org.openjena.atlas.logging.Log ;
import com.hp.hpl.jena.sparql.util.DatasetUtils ;
import com.hp.hpl.jena.sparql.util.graph.GraphFactory ;
import com.hp.hpl.jena.sparql.vocabulary.ResultSetGraphVocab ;
import com.hp.hpl.jena.util.FileManager ;
import com.hp.hpl.jena.util.FileUtils ;
import com.hp.hpl.jena.vocabulary.RDF ;

public class QueryTest extends EarlTestCase
{
    private static int testCounter = 1 ;
    private int testNumber = testCounter++ ;
    private TestItem testItem ;
    private FileManager queryFileManager ;
    private boolean isRDQLtest = false ;
    private boolean resetNeeded = false ;
    
    private SPARQLResult results = null ;    // Maybe null if no testing of results
    
    // If supplied with a model, the test will load that model with data from the source
    // If no model is supplied one is created or attached (e.g. a database)

    public QueryTest(String testName, EarlReport earl, FileManager fm, TestItem t)
    {
        super(fixName(testName), t.getURI(), earl) ;
        queryFileManager = fm ;
        testItem = t ;
        isRDQLtest = (testItem.getQueryFileSyntax().equals(Syntax.syntaxRDQL)) ;
    }
    
    private static String fixName(String s)
    {
        s = s.replace('(','[') ;
        s = s.replace(')',']') ;
        return s ;
    }

    private boolean oldWarningFlag  ;
    
    @Override
    protected void setUp() throws Exception
    {
        super.setUp() ;
        // SPARQL and ARQ tests are done with no value matching (for query execution and results testing)
        if ( ! isRDQLtest )
        {
            resetNeeded = true ;
            ARQ.setTrue(ARQ.strictGraph) ;
        }
        // Turn parser warnings off for the test data. 
        oldWarningFlag = CheckerLiterals.WarnOnBadLiterals ;
        CheckerLiterals.WarnOnBadLiterals = false ;

        // Sort out results.
        results =  testItem.getResults() ;
    }
    
    @Override
    protected void tearDown() throws Exception
    {
        if ( resetNeeded )
            ARQ.setFalse(ARQ.strictGraph) ;
        CheckerLiterals.WarnOnBadLiterals = oldWarningFlag ;
        super.tearDown() ;
    }
    
     private Dataset setUpDataset(Query query, TestItem testItem)
    {
        try {
            //testItem.requiresTextIndex()
            
            if ( doesQueryHaveDataset(query) && doesTestItemHaveDataset(testItem) )
            {
                // Only warn if there are results to test
                // Syntax tests may have FROM etc and a manifest data file. 
                if ( testItem.getResultFile() != null )
                    Log.warn(this, testItem.getName()+" : query data source and also in test file") ; 
            }
            
            // In test file?
            if ( doesTestItemHaveDataset(testItem) )
                // Not specified in the query - get from test item and load
                return createDataset(testItem.getDefaultGraphURIs(), testItem.getNamedGraphURIs()) ;
      
          // Check 3 - were there any at all?
          
          if ( ! doesQueryHaveDataset(query) ) 
              fail("No dataset") ;
      
          // Left to query
          return null ;
      
      } catch (JenaException jEx)
      {
          fail("JenaException creating data source: "+jEx.getMessage()) ;
          return null ;
      }
    }
    
    private static boolean doesTestItemHaveDataset(TestItem testItem)
    {
        boolean r = 
            ( testItem.getDefaultGraphURIs() != null &&  testItem.getDefaultGraphURIs().size() > 0 )
            ||
            ( testItem.getNamedGraphURIs() != null &&  testItem.getNamedGraphURIs().size() > 0 ) ;
        return r ;
    }
    
    private static boolean doesQueryHaveDataset(Query query)
    {
        return query.hasDatasetDescription() ;
    }
    
    private static Dataset createDataset(List<String> defaultGraphURIs, List<String> namedGraphURIs)
    {
        return DatasetUtils.createDataset(defaultGraphURIs, namedGraphURIs, null, null) ;
    }
    
    @Override
    protected void runTestForReal() throws Throwable
    {
        Query query = null ;
        try {
            try { query = queryFromTestItem(testItem) ; }
            catch (QueryException qEx)
            {
                query = null ;
                qEx.printStackTrace(System.err) ;
                fail("Parse failure: "+qEx.getMessage()) ;
                throw qEx ;
            }

            Dataset dataset = setUpDataset(query, testItem) ;
            if ( dataset == null && ! doesQueryHaveDataset(query) ) 
                fail("No dataset for query") ;

            QueryExecution qe = null ;
            
            if ( dataset == null )
                qe = QueryExecutionFactory.create(query, queryFileManager) ;
            else
                qe = QueryExecutionFactory.create(query, dataset) ;
            
            try {
                if ( query.isSelectType() )
                    runTestSelect(query, qe) ;
                else if ( query.isConstructType() )
                    runTestConstruct(query, qe) ;
                else if ( query.isDescribeType() )
                    runTestDescribe(query, qe) ;
                else if ( query.isAskType() )
                    runTestAsk(query, qe) ;
            } finally { qe.close() ; }
        }
        catch (IOException ioEx)
        {
            //log.debug("IOException: ",ioEx) ;
            fail("IOException: "+ioEx.getMessage()) ;
            throw ioEx ;
        }
        catch (NullPointerException ex) { throw ex ; }
        catch (Exception ex)
        {
            ex.printStackTrace(System.err) ;
            fail( "Exception: "+ex.getClass().getName()+": "+ex.getMessage()) ;
        }
    }
    
    void runTestSelect(Query query, QueryExecution qe) throws Exception
    {
        // Do the query!
        ResultSetRewindable resultsActual = ResultSetFactory.makeRewindable(qe.execSelect()) ;
        
        
        // Turn into a resettable version
        //ResultSetRewindable results = ResultSetFactory.makeRewindable(resultsActual) ;
        qe.close() ;
        
        // If ordered, we have to test by trunign into models because only the model form
        // enforces orderuing (it has RFD triples to encode the order).

        if ( results != null )
        {
            if ( results.isResultSet() )
            {
                // XXX Re-enable and check.
                //System.err.println("** "+getName()+": Result set direct testing") ;
                ResultSet rs = this.results.getResultSet() ;
                if ( rs == null )
                    System.err.println("** "+getName()+": bad result set") ;
                ResultSetRewindable resultsExpected = ResultSetFactory.makeRewindable(rs) ;
                if ( query.isReduced() )
                {
                    // Reduced - best we can do is do DISTINCT
                    resultsExpected = unique(resultsExpected) ;
                    resultsActual = unique(resultsActual) ;
                }

                boolean b ;
                if ( query.isOrdered() )
                    b = RSCompare.sameOrdered(resultsExpected, resultsActual) ;
                else
                    b = RSCompare.same(resultsExpected, resultsActual) ;
                if ( ! b)
                    printFailedResultSetTest(query, resultsExpected, resultsActual) ;
                assertTrue("Results do not match: "+testItem.getName(), b) ;
            }
            else if ( results.isModel() )
            {
                Model resultsAsModel = results.getModel() ;
                //System.err.println(getName()+": Result set model testing") ;
                ResultSetRewindable x = ResultSetFactory.makeRewindable(resultsAsModel) ;
                x = unique(x) ;
                resultsActual = unique(resultsActual) ;
                checkResults(query, resultsActual, ResultSetFormatter.toModel(x)) ;
            }
            else
                fail("Wrong result type for SELECT query") ;
        }
//        if ( ! query.isReduced() )
//            checkResults(query, results, resultsModel) ;
//        else
//        {
//            // Unfortunately, we turned the result set into a model. 
//            // Turn into a ResultSet-uniqueify-turn back into a model.
//            // Excessive copying.  Only for small results in the DAWG test suite.
//            ResultSetRewindable x = ResultSetFactory.makeRewindable(resultsModel) ;
//            x = unique(x) ;
//            results = unique(results) ;
//            checkResults(query, results, ResultSetFormatter.toModel(x)) ;
//        }
    }
    
    private static ResultSetRewindable unique(ResultSetRewindable results)
    {
        // VERY crude.  Utilises the fact that bindings have value equality.
        List<Binding> x = new ArrayList<Binding>() ;
        Set<Binding> seen = new HashSet<Binding>() ;
        
        for ( ; results.hasNext() ; )
        {
            Binding b = results.nextBinding() ;
            if ( seen.contains(b) )
                continue ;
            seen.add(b) ;
            x.add(b) ;
        }
        QueryIterator qIter = new QueryIterPlainWrapper(x.iterator()) ;
        ResultSet rs = new ResultSetStream(results.getResultVars(), ModelFactory.createDefaultModel(), qIter) ;
        return ResultSetFactory.makeRewindable(rs) ;
    } 

    private void checkResults(Query query, ResultSetRewindable results, Model resultsModel)
    {
        if ( resultsModel == null )
            return ;
        try {
            ResultSetRewindable qr1 = ResultSetFactory.makeRewindable(results) ;
            ResultSetRewindable qr2 = ResultSetFactory.makeRewindable(resultsModel) ;
            boolean b = resultSetEquivalent(query, qr1, qr2)  ; 
            if ( ! b)
                printFailedResultSetTest(query, qr1, qr2) ;
            assertTrue("Results do not match: "+testItem.getName(), b) ;
        } catch (Exception ex)
        {
            fail("Exception in result testing: "+ex) ;
        }
    }
    
    private static Model resultSetToModel(ResultSet rs)
    {
        Model m = GraphFactory.makeDefaultModel() ;
        ResultSetFormatter.asRDF(m, rs) ;
        if ( m.getNsPrefixURI("rs") == null )
            m.setNsPrefix("rs", ResultSetGraphVocab.getURI() ) ;
        if ( m.getNsPrefixURI("rdf") == null )
            m.setNsPrefix("rdf", RDF.getURI() ) ;
        if ( m.getNsPrefixURI("xsd") == null )
            m.setNsPrefix("xsd", XSDDatatype.XSD+"#") ;
        return m ;
        
    }
    
    /** Are two result sets the same (isomorphic)?
    *
    * @param rs1
    * @param rs2
    * @return boolean
    */

   static public boolean resultSetEquivalent(Query query,
       ResultSet rs1, ResultSet rs2)
   {
       Model model2 = resultSetToModel(rs2) ;
       return resultSetEquivalent(query, rs1, model2) ;
   }

   static public boolean resultSetEquivalent(Query query,
                                             ResultSet rs1,
                                             Model model2)
   {
       Model model1 = resultSetToModel(rs1) ;
       return model1.isIsomorphicWith(model2) ;
   }
   

   void runTestConstruct(Query query, QueryExecution qe) throws Exception
    {
        // Do the query!
        Model resultsActual = qe.execConstruct() ;
        compareGraphResults(resultsActual, query) ;
    }
   
   
   private void compareGraphResults(Model resultsActual, Query query)
   {
        if ( results != null )
        {
            try {
                if ( ! results.isGraph() )
                    fail("Expected results are not a graph: "+testItem.getName()) ;
                    
                Model resultsExpected = results.getModel() ;
                if ( ! resultsExpected.isIsomorphicWith(resultsActual) )
                {
                    printFailedModelTest(query, resultsExpected, resultsActual) ;
                    fail("Results do not match: "+testItem.getName()) ;
                }
            } catch (Exception ex)
            {
                String typeName = (query.isConstructType()?"construct":"describe") ;
                fail("Exception in result testing ("+typeName+"): "+ex) ;
            }
        }
    }
    
    void runTestDescribe(Query query, QueryExecution qe) throws Exception
    {
        Model resultsActual = qe.execDescribe() ;
        compareGraphResults(resultsActual, query) ;
    }
    
    void runTestAsk(Query query, QueryExecution qe) throws Exception
    {
        boolean result = qe.execAsk() ;
        if ( results != null )
        {
            if ( results.isBoolean() )
            {
                boolean b = results.getBooleanResult() ;
                assertEquals("ASK test results do not match", b, result) ;
            }
            else
            {
                Model resultsAsModel = results.getModel() ;
                StmtIterator sIter = results.getModel().listStatements(null, RDF.type, ResultSetGraphVocab.ResultSet) ;
                if ( !sIter.hasNext() )
                    throw new QueryTestException("Can't find the ASK result") ;
                Statement s = sIter.nextStatement() ;
                if ( sIter.hasNext() )
                    throw new QueryTestException("Too many result sets in ASK result") ;
                Resource r = s.getSubject() ;
                Property p = resultsAsModel.createProperty(ResultSetGraphVocab.getURI()+"boolean") ;

                boolean x = r.getRequiredProperty(p).getBoolean() ;
                if ( x != result )
                    assertEquals("ASK test results do not match", x,result);
            }
        }        
        return ;
    }
    
    void printFailedResultSetTest(Query query, ResultSetRewindable qrExpected, ResultSetRewindable qrActual)
   {
       PrintStream out = System.out ;
       out.println() ;
       out.println("=======================================") ;
       out.println("Failure: "+description()) ;
       
       out.println("Got: "+qrActual.size()+" --------------------------------") ;
       qrActual.reset() ;
       ResultSetFormatter.out(out, qrActual, query.getPrefixMapping()) ;
       qrActual.reset() ;
       out.flush() ;

       
       out.println("Expected: "+qrExpected.size()+" -----------------------------") ;
       qrExpected.reset() ;
       ResultSetFormatter.out(out, qrExpected, query.getPrefixMapping()) ;
       qrExpected.reset() ;
       
       out.println() ;
       out.flush() ;
   }

    void printFailedModelTest(Query query, Model expected, Model results)
    {
        PrintWriter out = FileUtils.asPrintWriterUTF8(System.out) ;
        out.println("=======================================") ;
        out.println("Failure: "+description()) ;
        results.write(out, "TTL") ;
        out.println("---------------------------------------") ;
        expected.write(out, "TTL") ;
        out.println() ;
    }
    
    @Override
    public String toString()
    { 
        if ( testItem.getName() != null )
            return testItem.getName() ;
        return super.getName() ;
    }

    // Cache
    String _description = null ;
    private String description()
    {
        if ( _description == null )
            _description = makeDescription() ;
        return _description ;
    }
    
    private String makeDescription()
    {
        String tmp = "" ;
        if ( testItem.getDefaultGraphURIs() != null )
        {
            for ( Iterator<String> iter = testItem.getDefaultGraphURIs().iterator() ; iter.hasNext() ; )
                tmp = tmp+iter.next() ;
        }
        if ( testItem.getNamedGraphURIs() != null )
        {
            for ( Iterator<String> iter = testItem.getNamedGraphURIs().iterator() ; iter.hasNext() ; )
                tmp = tmp+iter.next() ;
        }
        
        String d = "Test "+testNumber+" :: "+testItem.getName() ;
        //+" :: QueryFile="+testItem.getQueryFile()+
        //          ", DataFile="+tmp+", ResultsFile="+testItem.getResultFile() ;
        return d ;
    }
}
/*
 * (c) Copyright 2005, 2006, 2007, 2008, 2009 Hewlett-Packard Development Company, LP
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
