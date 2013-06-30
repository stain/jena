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

package org.apache.jena.web;

import java.io.ByteArrayInputStream ;
import java.io.ByteArrayOutputStream ;
import java.io.IOException ;

import org.apache.http.Header ;
import org.apache.http.HttpEntity ;
import org.apache.http.HttpResponse ;
import org.apache.http.HttpVersion ;
import org.apache.http.client.HttpClient ;
import org.apache.http.client.methods.HttpHead ;
import org.apache.http.client.methods.HttpUriRequest ;
import org.apache.http.entity.ContentType ;
import org.apache.http.entity.InputStreamEntity ;
import org.apache.http.impl.client.SystemDefaultHttpClient ;
import org.apache.http.params.BasicHttpParams ;
import org.apache.http.params.HttpConnectionParams ;
import org.apache.http.params.HttpParams ;
import org.apache.http.params.HttpProtocolParams ;
import org.apache.http.protocol.HttpContext ;
import org.apache.jena.atlas.logging.Log ;
import org.apache.jena.atlas.web.HttpException ;
import org.apache.jena.atlas.web.TypedInputStream ;
import org.apache.jena.atlas.web.auth.HttpAuthenticator ;
import org.apache.jena.riot.* ;
import org.apache.jena.riot.system.IRILib ;
import org.apache.jena.riot.web.* ;

import com.hp.hpl.jena.Jena ;
import com.hp.hpl.jena.graph.Graph ;
import com.hp.hpl.jena.graph.Node ;
import com.hp.hpl.jena.shared.JenaException ;

// TODO Support use of a HttpAuthenticator

public class DatasetGraphAccessorHTTP implements DatasetGraphAccessor
{
    // Test for this class are in Fuseki so they can be run with a server. 
    
    private final String remote ;
    private static final HttpResponseHandler noResponse = HttpResponseLib.nullResponse ;

    /** Create a DatasetUpdater for the remote URL */
    public DatasetGraphAccessorHTTP(String remote)
    {
        this.remote = remote ;
    }
    
    @Override
    public Graph httpGet()                            { return doGet(targetDefault()) ; }

    @Override
    public Graph httpGet(Node graphName)              { return doGet(target(graphName)) ; }
    
    private Graph doGet(String url)
    {
        HttpCaptureResponse<Graph> graph = HttpResponseLib.graphHandler() ;
        try {
            HttpOp.execHttpGet(url, WebContent.defaultGraphAcceptHeader, graph) ;
        } catch (HttpException ex) {
            if ( ex.getResponseCode() == HttpSC.NOT_FOUND_404 )
                return null ;
            throw ex ;
        }
        return graph.get(); 
    }
    
    @Override
    public boolean httpHead()
    {
        return doHead(targetDefault()) ;
    }
    
    @Override
    public boolean httpHead(Node graphName)
    {
        return doHead(target(graphName)) ;
    }

    private boolean doHead(String url)
    {
        HttpUriRequest httpHead = new HttpHead(url) ;
        try {
            HttpOp.execHttpGet(url, WebContent.defaultGraphAcceptHeader, noResponse) ;
            return true ;
        } catch (HttpException ex) {
            if ( ex.getResponseCode() == HttpSC.NOT_FOUND_404 )
                return false ;
            throw ex ;
        }
    }

    @Override
    public void httpPut(Graph data)                   { doPut(targetDefault(), data) ; }

    @Override
    public void httpPut(Node graphName, Graph data)   { doPut(target(graphName), data) ; }

    private void doPut(String url, Graph data)
    {
        HttpEntity entity = graphToHttpEntity(data) ;
        HttpOp.execHttpPut(url, entity, (HttpContext)null, (HttpAuthenticator)null) ;
    }
    
    @Override
    public void httpDelete()                          { doDelete(targetDefault()) ; }

    @Override
    public void httpDelete(Node graphName)            { doDelete(target(graphName)) ; }

    private void doDelete(String url)
    {
        try {
            HttpOp.execHttpDelete(url, noResponse) ;
        } catch (HttpException ex) {
            if ( ex.getResponseCode() == HttpSC.NOT_FOUND_404 )
                return ;
        }
    }
    
    @Override
    public void httpPost(Graph data)                  { doPost(targetDefault(), data) ; }

    @Override
    public void httpPost(Node graphName, Graph data)  { doPost(target(graphName), data) ; }

    private void doPost(String url, Graph data)
    {
        HttpEntity entity = graphToHttpEntity(data) ;
        HttpOp.execHttpPost(url, entity) ;
    }

    @Override
    public void httpPatch(Graph data)                 { throw new UnsupportedOperationException() ; }

    @Override
    public void httpPatch(Node graphName, Graph data) { throw new UnsupportedOperationException() ; }

    private String targetDefault()
    {
        return remote+"?"+HttpNames.paramGraphDefault+"=" ;
    }

    private String target(Node name)
    {
        if ( ! name.isURI() )
            throw new JenaException("Not a URI: "+name) ;
        String guri = name.getURI() ;
        // Encode
        guri = IRILib.encodeUriComponent(guri) ;
        return remote+"?"+HttpNames.paramGraph+"="+guri ;
    }

    static private HttpParams httpParams = createHttpParams() ;
    
    static private HttpParams createHttpParams()
    {
        HttpParams httpParams$ = new BasicHttpParams() ;
        // See DefaultHttpClient.createHttpParams
        HttpProtocolParams.setVersion(httpParams$,               HttpVersion.HTTP_1_1);
        HttpProtocolParams.setContentCharset(httpParams$,        WebContent.charsetUTF8);
        HttpProtocolParams.setUseExpectContinue(httpParams$,     true);
        HttpConnectionParams.setTcpNoDelay(httpParams$,          true);
        HttpConnectionParams.setSocketBufferSize(httpParams$,    32*1024);
        HttpProtocolParams.setUserAgent(httpParams$,             Jena.NAME+"/"+Jena.VERSION);
        return httpParams$;
    }
    
    private static String getHeader(HttpResponse response, String headerName)
    {
        Header h = response.getLastHeader(headerName) ;
        if ( h == null )
            return null ;
        return h.getValue() ;
    }

    private static RDFFormat sendLang = RDFFormat.RDFXML_PLAIN ;

    // Impedence mismatch - is there a better way?
    private static byte[] graphToBytes(Graph graph) {
        ByteArrayOutputStream out = new ByteArrayOutputStream() ;
        RDFDataMgr.write(out, graph, sendLang) ;
        byte[] bytes = out.toByteArray() ;
        return bytes ;
    }
    
    // Better : ContentProducer
    // (But this way ensures the graph can be serialized.)
    
    private static HttpEntity graphToHttpEntity(Graph graph) {
        byte[] bytes = graphToBytes(graph) ;
        ByteArrayInputStream in = new ByteArrayInputStream(bytes) ;
        String ct = sendLang.getLang().getContentType().getContentType() ;
        ContentType contentType = ContentType.create(ct) ;
        InputStreamEntity reqEntity = new InputStreamEntity(in, bytes.length, contentType) ;
        return reqEntity ;
    }

    private void execSimple(String targetStr, HttpUriRequest httpRequest, HttpAuthenticator httpAuthenticator)
    {
        HttpClient httpclient = new SystemDefaultHttpClient(httpParams) ;
        try {
            HttpResponse response = httpclient.execute(httpRequest) ;
            int responseCode = response.getStatusLine().getStatusCode() ;
            String responseMessage = response.getStatusLine().getReasonPhrase() ;
            
            if ( HttpSC.isClientError(responseCode) || HttpSC.isServerError(responseCode) )
                throw JenaHttpException.create(responseCode, responseMessage) ;

            if ( responseCode == HttpSC.NO_CONTENT_204) return ;
            if ( responseCode == HttpSC.CREATED_201 ) return ;
            
            if ( responseCode != HttpSC.OK_200 )
            {
                Log.warn(this, "Unexpected status code") ;
                throw JenaHttpException.create(responseCode, responseMessage) ;
            }
        } catch (IOException ex)
        {
            httpRequest.abort() ;
        }
    }

    private void readGraph(Graph graph, TypedInputStream ts, String base)
    {
        // Yes - we ignore the charset.
        // Either it's XML and so the XML parser deals with it, or the 
        // language determines the charset and the parsers offer InputStreams.   
       
        Lang lang = WebContent.contentTypeToLang(ts.getContentType()) ;
        if ( lang == null )
            throw new RiotException("Unknown lang for "+ts.getMediaType()) ;
        RDFDataMgr.read(graph, ts, lang) ; 
    }        
}
