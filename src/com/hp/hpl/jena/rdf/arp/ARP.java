/*
 *  (c) Copyright 2001, 2003  Hewlett-Packard Development Company, LP
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
 *
   $Id: ARP.java,v 1.10 2003-12-06 21:46:59 jeremy_carroll Exp $
   AUTHOR:  Jeremy J. Carroll
   with modification from PI Software
*/
/*
 * ARP.java
 *
 * Created on June 22, 2001, 6:20 AM
    *
    *  *
    *Possible options
    *
    * embedded RDF
    *
    *
    *
 */

package com.hp.hpl.jena.rdf.arp;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import java.io.IOException;
import org.xml.sax.InputSource;
import org.xml.sax.Locator;
import java.io.InputStream;
import java.io.Reader;
import java.util.*;

import org.apache.xerces.util.EncodingMap;

/** Another RDF Parser.
 * To load an RDF file:
 * <nl>
 * <li>Create an ARP.</li>
 * <li>Set its StatementHandler.</li>
 * <li>Optionally, set the ExtendedHandler.</li>
 * <li>Optionally, modify the error handling.</li>
 * <li>Call a load method.</li>
 * </nl>
 * Xerces is used for parsing the XML.
 * The SAXEvents generated by Xerces are then
 * analysed as RDF by ARP.
 * Errors may occur
 * in either the XML or the RDF part, see setErrorHandler for details
 * of how to distinguish between them.
 * <p>
 * For very large files, ARP does not use any additional
 * memory except when either the {@link #setExtendedHandler}
 * method or the {@link AResource#setUserData} method have been
 * used. In these cases ARP needs to remember the <code>rdf:nodeID</code>
 * usage through the file life time. 
 * @author  Jeremy Carroll, contribution //TODO sdr, newman
 */
public class ARP implements ARPErrorNumbers {

    static private class Fake extends EncodingMap {
        static {
        //    EncodingMap.fJava2IANAMap.put("ISO8859_15_FDIS","ISO-8859-15");
            Iterator it = EncodingMap.fJava2IANAMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                if (!me
                    .getKey()
                    .equals(EncodingMap.fIANA2JavaMap.get(me.getValue()))) {
                //  System.err.println(
                //      "?1? " + me.getKey() + " => " + me.getValue());
                }
            }
            it = EncodingMap.fIANA2JavaMap.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry me = (Map.Entry) it.next();
                if (null == EncodingMap.fJava2IANAMap.get(me.getValue())) {
//                  System.err.println(
//                      "?2? " + me.getKey() + " => " + me.getValue());
                    EncodingMap.fJava2IANAMap.put(me.getValue(),me.getKey());
                }
            }

        }
        static void foo() {
        }
    }
    /**
     * This method is a work-around for a Xerces bug.
     * Speicifically
     * <a href="http://nagoya.apache.org/bugzilla/show_bug.cgi?id=18551">bug
     * 18551</a>. It should be called before using the EncodingMap from Xerces,
     * typically in a static initializer. This is done within Jena code, and is 
     * not normally needed by an end user.
     * It is not part of the ARP or Jena API; and will be removed when the 
     * Xerces bug is fixed.
     */
    static public void initEncoding() {
        Fake.foo();
    }
    private ARPFilter arpf;
    
/** Creates a new RDF Parser.
 * Can parse one file at a time.
 */    
    public ARP()  {
        arpf =  ARPFilter.create();
    }
	/** Sets the ExtendedHandler that provides the callback mechanism
	 * for bnodes as they leave scope, and for the start and end of rdf:RDF
	 * elements.
 * <p>
 * See note about large files in class documentation.
	 * @param sh The handler to use.
	 * @return The old handler.
	 */
		public ExtendedHandler setExtendedHandler(ExtendedHandler sh) {
			return arpf.setExtendedHandler(sh);
		}
/** Sets the NamespaceHandler that provides the callback mechanism
	* for XML namespace declarations.
	* @param sh The handler to use.
	* @return The old handler.
	*/
	   public NamespaceHandler setNamespaceHandler(NamespaceHandler sh) {
		   return arpf.setNamespaceHandler(sh);
	   }
/**
 * When parsing a file, this returns a Locator giving the
 * position of the last XML event processed by ARP.
 * This may return null or misleading results before any
 * tokens have been processed.
 * @return Locator
 */
    public Locator getLocator() {
        return arpf.getLocator();
    }
    
/** Sets the StatementHandler that provides the callback mechanism
 * for each triple in the file.
 * @param sh The statement handler to use.
 * @return The old statement handler.
 */    
    public StatementHandler setStatementHandler(StatementHandler sh) {
        return arpf.setStatementHandler(sh);
    }
    
/** Sets the error handler, for both XML and RDF parse errors.
 * XML errors are reported by Xerces, as instances of
 * SAXParseException;
 * the RDF errors are reported from ARP as instances of
 * ParseException.
 * Code that needs to distingusih between them
 * may look like:
 * <pre>
 *   void error( SAXParseException e ) throws SAXException {
 *     if ( e instanceof com.hp.hpl.jena.rdf.arp.ParseException ) {
 *          ...
 *     } else {
 *          ...
 *     }
 *   }
 * </pre>
 * <p>
 * See the ARP documentation for ErrorHandler for details of
 * the ErrorHandler semantics (in particular how to upgrade a warning to
 * an error, and an error to a fatalError).
 * </p>
 * <p>
 * The Xerces/SAX documentation for ErrorHandler is available on the web.
 * </p>
 *
 * @param eh The error handler to use.
 */    
    public void setErrorHandler(ErrorHandler eh) {
        arpf.setErrorHandler(eh);
    }
    
/** Sets or gets the error handling mode for a specific error condition.
 * Changes that cannot be honoured are silently ignored.
 * Illegal error numbers may result in an ArrayIndexOutOfBoundsException but
 * are usually ignored.
 * @param errno The specific error condition to change.
 * @param mode The new mode one of:
 * <dl>
 * <dt>IGNORE</dt>
 * <dd>Ignore this condition.</dd>
 * <dt>WARNING</dt>
 * <dt>Invoke ErrorHandler.warning() for this condition.</dd>
 * <dt>ERROR</dt>
 * <dt>Invoke ErrorHandler.error() for this condition.</dd>
 * <dt>FATAL</dt>
 * <dt>Aborts parse and invokes ErrorHandler.fatalError() for this condition.
 * In unusual situations, a few further warnings and errors may be reported.
 * </dd>
 * </dl>
 * @return The old error mode for this condition.
 */    
    public int setErrorMode( int errno, int mode ) {
        return arpf.setErrorMode(errno,mode);
    }
/** Resets error mode to the default values:
 * most errors are reported as warnings, but triples are produced.
 */    
    public void setDefaultErrorMode() {
        arpf.setDefaultErrorMode();
    }
/** As many errors as possible are ignored.
 * As many triples as possible are produced.
 */    
    public void setLaxErrorMode() {
        arpf.setLaxErrorMode();
    }
/** This method tries to emulate the latest Working Group recommendations.
 */    
    public void setStrictErrorMode() {
        setStrictErrorMode(EM_IGNORE);
    }
    /**
     * This method detects and prohibits errors according to
     *the latest Working Group recommendations.
     * For other conditions, such as 
     {@link ARPErrorNumbers#WARN_PROCESSING_INSTRUCTION_IN_RDF} and
     {@link ARPErrorNumbers#WARN_LEGAL_REUSE_OF_ID}, nonErrorMode is used. 
     *@param nonErrorMode The way of treating non-error conditions.
     */
    public void setStrictErrorMode(int nonErrorMode) {
        arpf.setStrictErrorMode(nonErrorMode);
    }
/** Sets whether the XML document is only RDF, or contains RDF embedded in other XML.
 * The default is non-embedded mode.
 * Embedded mode also matches RDF documents that use the
 * rdf:RDF tag at the top-level.
 * Non-embeded mode matches RDF documents which omit that optional tag, and consist of a single rdf:Description or
 * typed node.
 * To find embedded RDF it is necessary to setEmbedding(true).
 * @param embed true: Look for embedded RDF; or false: match a typed node or rdf:Description against the whole document (the default).
 */    
    public void setEmbedding(boolean embed) {
        arpf.setEmbedding(embed);
    }
    
/** Load RDF/XML from a Reader.
 * @param in The input XML document.
 * @param xmlBase The base URI for the document.
 * @throws SAXException More serious error during XML or RDF processing; or thrown from the fatalError method of the ErrorHandler.
 * @throws IOException Occurring during XML processing.
 */    
    public void load(Reader in,String xmlBase) throws SAXException, IOException {
        InputSource inputS = new InputSource(in);
        inputS.setSystemId(xmlBase);
         arpf.parse(inputS);
    }
    void load(InputSource is)  throws SAXException, IOException {
        arpf.parse(is);
    }
/** Load RDF/XML from an InputStream.
 * @param in The input XML document.
 * @param xmlBase The base URI for the document.
 * @throws SAXException More serious error during XML or RDF processing; or thrown from the fatalError method of the ErrorHandler.
 * @throws IOException Occurring during XML processing.
 */    
    public void load(InputStream in,String xmlBase) throws SAXException, 
IOException {
        //load(new InputStreamReader(in),xmlBase);
        InputSource inputS = new InputSource(in);
        inputS.setSystemId(xmlBase);
        arpf.parse(inputS, xmlBase);
    }
/** Load RDF/XML from an InputStream, using base URL http://unknown.org/.
 * @param in The input XML document.
 * @throws SAXException More serious error during XML or RDF processing; or thrown from the fatalError method of the ErrorHandler.
 * @throws IOException Occurring during XML processing.
 */   
    public void load(InputStream in) throws SAXException, IOException {
        load(in,"");
    }
/** Load RDF/XML from a Reader, using base URL http://unknown.org/.
 * @param in The input XML document.
 * @throws SAXException More serious error during XML or RDF processing; or thrown from the fatalError method of the ErrorHandler.
 * @throws IOException Occurring during XML processing.
 */    
    public void load(Reader in) throws SAXException, IOException {
        load(in,"");
    }
}
