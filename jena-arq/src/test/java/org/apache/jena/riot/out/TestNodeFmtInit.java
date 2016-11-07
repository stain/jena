/**
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
package org.apache.jena.riot.out;

import static org.junit.Assert.assertEquals;

import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.junit.Test;

/**
 * Tests {@link NodeFmtLib} initialization
 * 
 * @see <a href="https://issues.apache.org/jira/browse/JENA-1258">JENA-1258</a>
 */
public class TestNodeFmtInit {
    @Test
    public void strWithoutJenaSystemInit() throws Exception {
        // NOTE: Deliberately NOT calling first
        // JenaSystem.init();
        Node node = NodeFactory.createLiteral("Hello world", "en");
        assertEquals("\"Hello world\"@en", NodeFmtLib.str(node));
    }

}
