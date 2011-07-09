/*
 * (c) Copyright 2009 Talis Systems Ltd
 * All rights reserved.
 * [See end of file]
 */

package com.hp.hpl.jena.tdb.sys;


/** A Session is a set of operations that are either all read actions 
 * or a mixture of read and write (an update). Sessions are not necessarily long - 
 * they are just a grouping of operations. 
 * 
 * Most implementations of this interface do not enforce the policy - it
 * is up to the caller to preserve the invariant for the object called.
 * 
 * An implementation may allow policies such as transactional (ACID)
 * but, unless otherwise documented, an application can not 
 * assume that.
 */
public interface Session
{
    /** Signal the start of an update operation */
    public void startUpdate() ;
    
    /** Signal the completion of an update operation */
    public void finishUpdate();

    /** Signal the start of a read operation */
    public void startRead();
    
    /** Signal the completion of a read operation */
    public void finishRead();
}

/*
 * (c) Copyright 2009 Talis Systems Ltd
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