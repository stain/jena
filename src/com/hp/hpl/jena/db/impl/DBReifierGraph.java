/*
  (c) Copyright 2003, Hewlett-Packard Company, all rights reserved.
  [See end of file]
*/

package com.hp.hpl.jena.db.impl;

import com.hp.hpl.jena.db.GraphRDB;
import com.hp.hpl.jena.db.RDFRDBException;
import com.hp.hpl.jena.graph.*;
import com.hp.hpl.jena.graph.query.QueryHandler;
import com.hp.hpl.jena.graph.query.SimpleQueryHandler;
import com.hp.hpl.jena.shared.PrefixMapping;
import com.hp.hpl.jena.util.iterator.*;

import java.util.*;

/**
 *
 * Implementation of a "hidden triples" graph for reified statements in GraphRDB.
 * 
 * This makes the list of specializedGraphReifers in the GraphRDB into a read-only
 * Graph - suitable to be returned by Reifier.getHiddenTriples() )
 * 
 * @since Jena 2.0
 * 
 * @author csayers 
 * @version $Revision: 1.1 $
 */
public class DBReifierGraph implements Graph {

	protected List m_specializedGraphs = null; // list of SpecializedGraphReifiers
	protected GraphRDB m_parent = null; // parent graph;
	
	/**
	 * Construct a new DBReifierGraph
	 * @param reifier is the specialized graph which holds the reified triples.
	 */
	public DBReifierGraph( GraphRDB parent, List reifiers) {
	
		m_parent = parent;
		m_specializedGraphs = reifiers;
		
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#add(com.hp.hpl.jena.graph.Triple)
	 */
	public void add(Triple t) {
		throw new RuntimeException("Error - attempt to modify a read-only graph");		
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#delete(com.hp.hpl.jena.graph.Triple)
	 */
	public void delete(Triple t) {
		throw new RuntimeException("Error - attempt to modify a read-only graph");
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#size()
	 */
	public int size() {
		if(m_specializedGraphs == null)
			throw new RDFRDBException("Error - attempt to call size on a Graph that has already been closed");
		int result =0;		
		Iterator it = m_specializedGraphs.iterator();
		while( it.hasNext() ) {
			SpecializedGraph sg = (SpecializedGraph) it.next();
			result += sg.tripleCount();
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#contains(com.hp.hpl.jena.graph.Triple)
	 */
	public boolean contains(Triple t) {
		if(m_specializedGraphs == null)
			throw new RDFRDBException("Error - attempt to call contains on a Graph that has already been closed");
		SpecializedGraph.CompletionFlag complete = new SpecializedGraph.CompletionFlag();
		Iterator it = m_specializedGraphs.iterator();
		while( it.hasNext() ) {
			SpecializedGraph sg = (SpecializedGraph) it.next();
			boolean result = sg.contains( t, complete);
			if( result == true || complete.isDone() == true )
				return result;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#contains(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node)
	 */
	public boolean contains(Node s, Node p, Node o) {
		return contains(new Triple(s, p, o));
	} 
			

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#find(com.hp.hpl.jena.graph.TripleMatch)
	 */
	public ExtendedIterator find(TripleMatch m) {
		if(m_specializedGraphs == null)
			throw new RDFRDBException("Error - attempt to call find on a Graph that has already been closed");
		ExtendedIterator result = null;
		SpecializedGraph.CompletionFlag complete = new SpecializedGraph.CompletionFlag();
		Iterator it = m_specializedGraphs.iterator();
		while( it.hasNext() ) {
			SpecializedGraph sg = (SpecializedGraph) it.next();
			ExtendedIterator partialResult = sg.find( m, complete);
			if( result == null)
				result = partialResult;
			else
				result = result.andThen(partialResult);
			if( complete.isDone())
				break;
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#getPrefixMapping()
	 */
	public PrefixMapping getPrefixMapping() { 
		return m_parent.getPrefixMapping();
	}


	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#getTransactionHandler()
	 */
	public TransactionHandler getTransactionHandler() {
		return m_parent.getTransactionHandler();
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#close()
	 */
	public void close() {
		m_specializedGraphs = null;
		m_parent = null;
	}
	
	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#dependsOn(com.hp.hpl.jena.graph.Graph)
	 */
	public boolean dependsOn(Graph other) {
		return m_parent.dependsOn(other);
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#queryHandler()
	 */
	public QueryHandler queryHandler() {
		return new SimpleQueryHandler(this);
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#getBulkUpdateHandler()
	 */
	public BulkUpdateHandler getBulkUpdateHandler() {
		return m_parent.getBulkUpdateHandler();
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#getCapabilities()
	 */
	public Capabilities getCapabilities() {
		return null;
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#getReifier()
	 */
	public Reifier getReifier() {
		throw new RuntimeException("Error - read-only hidden triple graphs don't support reifiers");
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#find(com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node, com.hp.hpl.jena.graph.Node)
	 */
	public ExtendedIterator find(Node s, Node p, Node o) {
		StandardTripleMatch m = new StandardTripleMatch(s,p,o);
		return find(m);
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#isIsomorphicWith(com.hp.hpl.jena.graph.Graph)
	 */
	public boolean isIsomorphicWith(Graph g) {
		return g != null && GraphMatcher.equals( this, g );
	}

	/* (non-Javadoc)
	 * @see com.hp.hpl.jena.graph.Graph#capabilities()
	 */
	public int capabilities() {
		return 0;
	}
}

/*
 *  (c) Copyright Hewlett-Packard Company 2003.
 *  All rights reserved.
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