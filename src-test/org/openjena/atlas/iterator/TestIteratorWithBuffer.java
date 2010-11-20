/*
 * (c) Copyright 2010 Epimorphics Ltd.
 * All rights reserved.
 * [See end of file]
 */

package org.openjena.atlas.iterator;

import java.util.Arrays ;
import java.util.List ;

import org.junit.Test ;
import org.openjena.atlas.junit.BaseTest ;

public class TestIteratorWithBuffer extends BaseTest
{

    @Test public void iterBuffer_01()
    {
        IteratorWithBuffer<String> iter = createBuffered(1, "a", "b", "c") ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("a", iter.peek(0)) ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("a", iter.next()) ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("b", iter.peek(0)) ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("b", iter.next()) ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("c", iter.peek(0)) ;
        assertEquals(1, iter.currentSize()) ;
        assertEquals("c", iter.next()) ;
        assertEquals(0, iter.currentSize()) ;
        assertEquals(null, iter.peek(0)) ;
        assertEquals(0, iter.currentSize()) ;
    }

    @Test public void iterBuffer_02()
    {
        IteratorWithBuffer<String> iter = createBuffered(2, "a", "b", "c") ;
        assertEquals(2, iter.currentSize()) ;
        assertEquals("a", iter.peek(0)) ;
        assertEquals("b", iter.peek(1)) ;
        assertEquals("a", iter.next()) ;
        
        assertEquals("b", iter.peek(0)) ;
        assertEquals("c", iter.peek(1)) ;
        assertEquals("b", iter.next()) ;
        
        assertEquals("c", iter.peek(0)) ;
        assertEquals(null, iter.peek(1)) ;
        assertEquals("c", iter.next()) ;
        assertEquals(null, iter.peek(0)) ;
    }

    @Test public void iterBuffer_03()
    {
        IteratorWithBuffer<String> iter = createBuffered(1) ;
        assertEquals(null, iter.peek(0)) ;
    }

    @Test(expected=IndexOutOfBoundsException.class)
    public void iterBuffer_04()
    {
        IteratorWithBuffer<String> iter = createBuffered(0, "a") ;
        assertEquals(null, iter.peek(0)) ;
    }

    @Test public void iterBuffer_05()
    {
        IteratorWithBuffer<String> iter = createBuffered(2, "a") ;
        assertEquals("a", iter.peek(0)) ;
        assertEquals(null, iter.peek(1)) ;
        assertEquals("a", iter.next()) ;
    }

    private IteratorWithBuffer<String> createBuffered(int N, String... strings)
    {
        List<String> data = Arrays.asList(strings) ;
        IteratorWithBuffer<String> iter = new IteratorWithBuffer<String>(data.iterator(), N) ;
        return iter ;
    }

}

/*
 * (c) Copyright 2010 Epimorphics Ltd.
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