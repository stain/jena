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

package com.hp.hpl.jena.update;

import com.hp.hpl.jena.query.QuerySolution ;
import com.hp.hpl.jena.sparql.modify.UpdateSink ;
import com.hp.hpl.jena.sparql.util.Context ;

/** An instance of a execution of an UpdateRequest */ 
public interface UpdateProcessorStreaming
{
    /** The properties associated with a query execution -  
     *  implementation specific parameters  This includes
     *  Java objects (so it is not an RDF graph).
     *  Keys should be URIs as strings.  
     *  May be null (this implementation does not provide any configuration).
     */ 
    public Context getContext() ;
    
    /** Set the initial association of variables and values.
     * May not be supported by all UpdateProcessor implementations.
     * @param binding
     */
    public void setInitialBinding(QuerySolution binding) ;
    
    /**
     * The dataset against which the query will execute.
     * May be null, implying the there isn't a local GraphStore target for this UpdateProcessor.
     */
    public GraphStore getGraphStore() ;
    
    /** Start the request, call before putting updates into the Sink */
    public void startRequest() ;
    
    /** Finish the request, call after putting updates into the Sink */
    public void finishRequest() ;
    
    /** The UpdateSink into which Updates are added and executed */
    public UpdateSink getUpdateSink() ;
}
