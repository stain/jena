/* Generated By:JavaCC: Do not edit this line. ParseException.java Version 3.0 */
/*
 * (c) Copyright 2001-2003 Hewlett-Packard Development Company, LP All rights
 * reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met: 1.
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. 2. Redistributions in
 * binary form must reproduce the above copyright notice, this list of
 * conditions and the following disclaimer in the documentation and/or other
 * materials provided with the distribution. 3. The name of the author may not
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *  * $Id: ParseException.java,v 1.6 2003-12-06 21:46:59 jeremy_carroll Exp $
 * 
 * AUTHOR: Jeremy J. Carroll
 */
package com.hp.hpl.jena.rdf.arp;

import org.xml.sax.SAXParseException;
import java.util.*;

/**
 * An exception during the RDF processing of ARP. Note: it is distinguished
 * from an XML related exception from Xerces because while both are
 * SAXParseException's, the latter are not
 * com.hp.hpl.jena.arp.ParseException's.
 *  
 */
public class ParseException
	extends SAXParseException
	implements ARPErrorNumbers, RDFParserConstants {

	/**
	 * @param cTok
	 * @param expectedTokenSequencesVal
	 * @param tokenImageVal
	 */
	ParseException(
		Token cTok,
		int[][] expectedTokenSequencesVal,
		String[] tokenImageVal) {
		super(null, publicId(cTok), systemId(cTok), line(cTok), col(cTok));
		specialConstructor = true;
		where = cTok.location;
		currentToken = cTok;
		expectedTokenSequences = expectedTokenSequencesVal;
		id = ERR_SYNTAX_ERROR;
		tokenImage = tokenImageVal;
	}

	private static String systemId(Token c) {
		return c.location != null ? c.location.inputName : null;
	}
	private static String publicId(Token c) {
		return null;
	}
	private static int line(Token c) {
		return c.location != null ? c.location.endLine : -1;
	}
	private static int col(Token c) {
		return c.location != null ? c.location.endColumn : -1;
	}

	ParseException(int id, Location where, String msg, SAXParseException e) {
		super(msg, where.inputName, null, where.endLine, where.endColumn, e);
		this.where = where;
		this.id = id;
		specialConstructor = false;
	}

	ParseException(int id, Location where, String msg) {
		super(msg, where.inputName, null, where.endLine, where.endColumn);
		this.where = where;
		this.id = id;
		specialConstructor = false;
	}

	ParseException(int id, String message) {
		super(message, null);
		this.id = id;
		specialConstructor = false;
	}

	ParseException() {
		super(null, null);
		specialConstructor = false;
	}
	SAXParseException rootCause() {
		Exception e = getException();
		return e == null ? this : (SAXParseException) e;
	}

	private int id;
	/**
	 * The error number (from {@link ARPErrorNumbers}) related to this
	 * exception.
	 * 
	 * @return The error number.
	 */
	public int getErrorNumber() {
		return id;
	}
	private Location where;

	boolean specialConstructor;

	Token currentToken;

	private boolean isFatal;
	void setFatal(boolean v) {
		isFatal = v;
	}
	boolean getFatal() {
		return isFatal;
	}

	int[][] expectedTokenSequences;
	private String tokenImage[];

	boolean promoteMe;

	/**
	 * Intended for use within an RDFErrorHandler. This method is untested.
	 * Marks the exception to be promoted from a warning to an error, or from
	 * an error to a fatal error, or from a fatal error to be thrown from the
	 * parser's entry method.
	 */
	public void promote() {
		promoteMe = true;
	}

	private Token startAttributes;

	void setStartAttribute(Token t) {
		startAttributes = t;
	}
	private void startAttrSkip(Token upto) {
		Token t;
		for (t = startAttributes; t != upto.next; t = t.next) {
			switch (t.kind) {
				case A_XMLBASE :
				case A_XMLLANG :
				case AV_STRING :
				case X_WARNING :
				case A_XMLNS :
					continue;
				default :
					startAttributes = t;
					return;
			}
		}
	}

	private String getAttributes(Token upto) {
		Token t;
		String rslt = "";
		for (t = startAttributes; t != upto.next; t = t.next) {
			switch (t.kind) {
				case A_TYPE :
					rslt += ", rdf:type";
					break;
				case A_ABOUT :
					rslt += ", rdf:about";
					break;
				case A_DATATYPE :
					rslt += ", rdf:datatype";
					break;
				case A_NODEID :
					rslt += ", rdf:nodeID";
					break;
				case A_ID :
					rslt += ", rdf:ID";
					break;
				case A_PARSETYPE :
					rslt += ", rdf:parseType";
					break;
				case A_RESOURCE :
					rslt += ", rdf:resource";
					break;
				case A_RDF_N :
					rslt += ", rdf:_NNN";
					break;

				case A_OTHER :
					rslt += ", a property attribute";
					break;

				case AV_LITERAL :
					rslt += "='Literal'";
					break;
				case AV_RESOURCE :
					rslt += "='Resource'";
					break;
				case AV_DAMLCOLLECTION :
					rslt += "='daml:collection'";
					break;

				case AV_STRING :
				case X_WARNING :
					continue;
				case A_XMLNS :
				case A_XMLBASE :
				case A_XMLLANG :
				default :
					String msg =
						"Internal mishap in ParseException.getAttributes()";
					System.err.println(msg);
					return msg;
			}
		}
		return rslt.length() > 2 ? rslt.substring(2) : rslt;
	}

	/**
	 * The message without location information. Use either the formatMessage
	 * method, or the SAXParseException interface, to access the location
	 * information.
	 * 
	 * @return The exception message.
	 */
	public String getMessage() {
		// turn 1 to W001
		// turn 204 to E204
		String idStr =
			id != 0
				? "{"
					+ (id < 200 ? "W" : "E")
					+ ("" + (1000 + id)).substring(1)
					+ "} "
				: "";
		if (!specialConstructor) {
			return idStr + super.getMessage();
		}
		Token tok = currentToken.next;
		startAttrSkip(tok);
		String retval =
			"Syntax error when processing " + tok.toString() + "." + eol;
		// First check for the following case:
		// <property>
		//     <obj1 about="ffoo"/>
		//     *HERE*<obj2 about="error"/>
		// </property>
		if (isElementStripingProblem(tok)) {
			return idStr
				+ retval
				+ "Cannot have another XML element here."
				+ eol
				+ "(Maybe one object has already been given as the value of the enclosing property).";
		}
		switch (tok.kind) {
			case CD_STRING :
				break;
			case EOF :
				retval
					+= "Input to RDF parser ended prematurely. This is often related to an XML parser abort."
					+ eol;
				break;

			case E_DESCRIPTION :
				return idStr
					+ retval
					+ "rdf:Description elements generally may"
					+ eol
					+ "only occur to describe an object.";
			case E_RDF :
				return idStr
					+ retval
					+ "rdf:RDF element tags generally may not occur inside RDF content.";
			case A_OTHER :
			case A_RDF_N :
				if (startAttributes != null) {
					if (startAttributes == tok) {
						return idStr
							+ retval
							+ "Cannot have property attributes in this context.";
					} else {
						return idStr
							+ retval
							+ "Cannot have property attributes with the following other attributes:"
							+ eol
							+ "    "
							+ getAttributes(currentToken);
					}
				}
			case A_TYPE :
				if (startAttributes != null) {
					if (startAttributes == tok) {
						return idStr
							+ retval
							+ "Cannot have rdf:type attribute in this context.";
					} else {
						return idStr
							+ retval
							+ "Cannot have rdf:type attribute with the following other attributes:"
							+ eol
							+ "    "
							+ getAttributes(currentToken);
					}
				}
			case A_ABOUT :
			case A_ID :
			case A_PARSETYPE :
			case A_RESOURCE :
			case A_NODEID :
			case A_DATATYPE :
				if (startAttributes != null) {
					if (startAttributes == tok) {
						return idStr
							+ retval
							+ "Cannot have "
							+ tokenImage[tok.kind]
							+ " in this context.";
					} else {
						return idStr
							+ retval
							+ "In this context, the following attributes are not allowed together:"
							+ eol
							+ "    "
							+ getAttributes(tok);
					}
				}

			case A_XMLBASE :
			case A_XMLLANG :
			case A_XMLNS :
			default :
				retval = "Unusual " + retval;

		}
		String expected = "";
		int maxSize = 0;
		BitSet suppress = new BitSet();
		suppress.set(X_WARNING);
		for (int i = 0; i < expectedTokenSequences.length; i++) {
			switch (expectedTokenSequences[i][0]) {
				case E_OTHER :
					suppress.set(E_LI);
					suppress.set(E_RDF_N);
					break;
				case A_OTHER :
					suppress.set(A_RDF_N);
					break;
				case CD_STRING :
					suppress.set(COMMENT);
					suppress.set(PROCESSING_INSTRUCTION);
					break;
			}
		}
		for (int i = 0; i < expectedTokenSequences.length; i++) {
			if (suppress.get(expectedTokenSequences[i][0]))
				continue;
			if (maxSize < expectedTokenSequences[i].length) {
				maxSize = expectedTokenSequences[i].length;
			}
			for (int j = 0; j < expectedTokenSequences[i].length; j++) {
				expected += tokenImage[expectedTokenSequences[i][j]] + " ";
			}
			expected += eol + "    ";
		}
		retval += "Encountered ";
		for (int i = 0; i < maxSize; i++) {
			if (i != 0)
				retval += " ";
			retval += tok.toString();
			if (tok.kind == 0) {
				break;
			}
			tok = tok.next;
		}
		if (expectedTokenSequences.length == 1) {
			retval += " Was expecting:" + eol + "    ";
		} else {
			retval += " Was expecting one of:" + eol + "    ";
		}
		retval += expected;
		return idStr + retval;

	}
	static final private int elementStriping[] =
		new int[] {
			CD_STRING,
			PROCESSING_INSTRUCTION,
			COMMENT,
			X_SAX_EX,
			E_END };
	static {
		Arrays.sort(elementStriping);
	}
	private boolean isElementStripingProblem(Token tok) {
		if ( expectedTokenSequences.length != elementStriping.length )
			return false;
		int e[] = new int[elementStriping.length];
		for (int i=0;i<e.length; i++)
			e[i] = expectedTokenSequences[i][0];
		Arrays.sort(e);
		for (int i=0;i<e.length;i++)
			if (e[i]!=elementStriping[i])
				return false;
	   return true;
	}

	/**
	 * The end of line string for this machine.
	 */
	static String eol;

	static {
		try {
			eol = System.getProperty("line.separator", "\n");
		} catch (SecurityException e) {
			eol = "\n";
		}
	}

	/**
	 * Calls e.getMessage() and also accesses line and column information for
	 * SAXParseException's.
	 * 
	 * @return e.getMessage() possibly prepended by error location information.
	 * @param e
	 *            The exception to describe.
	 */
	static public String formatMessage(Exception e) {
		String msg = e.getMessage();
		if (msg == null)
			msg = e.toString();
		if (e instanceof SAXParseException) {
			SAXParseException sax = (SAXParseException) e;
			String file = sax.getSystemId();
			if (file == null)
				file = sax.getPublicId();
			String rslt = file == null ? "" : file;
			if (sax.getLineNumber() != -1) {
				if (sax.getColumnNumber() == -1) {
					return rslt + "[" + sax.getLineNumber() + "]: " + msg;
				} else {
					return rslt
						+ "["
						+ sax.getLineNumber()
						+ ":"
						+ sax.getColumnNumber()
						+ "]: "
						+ msg;
				}
			} else {
				return (file != null ? (file + ": ") : "") + msg;
			}
		} else {
			return msg;
		}
	}

}
