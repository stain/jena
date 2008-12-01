/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
 * All rights reserved.
 * [See end of file]
 */

package tdb;

import java.util.List;

import lib.Log;
import tdb.cmdline.CmdTDB;
import arq.cmd.CmdUtils;
import arq.cmdline.ArgDecl;

import com.hp.hpl.jena.graph.Graph;
import com.hp.hpl.jena.query.ARQ;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.tdb.pgraph.BulkLoader1;
import com.hp.hpl.jena.tdb.pgraph.PGraph;
import com.hp.hpl.jena.tdb.store.BulkLoader;
import com.hp.hpl.jena.tdb.store.GraphTDB;

public class tdbloader extends CmdTDB
{
//    private static final ArgDecl argGraphDeafult = new ArgDecl(ArgDecl.NoValue, "default") ;
    
    private static final ArgDecl argParallel         = new ArgDecl(ArgDecl.NoValue, "parallel") ;
    private static final ArgDecl argIncremental      = new ArgDecl(ArgDecl.NoValue, "incr", "incrmenetal") ;
    private static final ArgDecl argStats            = new ArgDecl(ArgDecl.NoValue, "stats") ;
    
    private boolean timing = true ;
    private boolean doInParallel = false ;
    private boolean doIncremental = false ;
    private boolean generateStats = false ;
    
    static public void main(String... argv)
    { 
        CmdUtils.setLog4j() ;
        new tdbloader(argv).mainRun() ;
    }

    protected tdbloader(String[] argv)
    {
        super(argv) ;
        
        super.add(argParallel, "--parallel", "Do rebuilding of secondary indexes in a parallel") ;
        super.add(argIncremental, "--incremental", "Do an incremental load (keep indexes during load, don't rebuild)") ;
        super.add(argStats, "--stats", "Generate statistics while loading (new graph only)") ;
    }

    @Override
    protected void processModulesAndArgs()
    {
        super.processModulesAndArgs() ;
        doInParallel = super.contains(argParallel) ;
        doIncremental = super.contains(argIncremental) ;
        generateStats = super.contains(argStats) ;
    }
    
    @Override
    protected String getSummary()
    {
        return getCommandName()+" [--desc DATASET | -loc DIR] FILE ..." ;
    }

    @Override
    protected void exec()
    {
        if ( isVerbose())
        {
            System.out.println("Java maximum memory: "+Runtime.getRuntime().maxMemory());
            System.out.println(ARQ.getContext()) ;
        }
        if ( isVerbose() )
            timing = true ;
        if ( isQuiet() )
            timing = false ;
        
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>)getPositional() ;
        if ( urls.size() == 0 )
            urls.add("-") ;
        
        if ( graphName == null )
        {
            Graph graph = getGraph() ;
            if ( graph instanceof PGraph )
            {
                Log.warn(this, "Old PGraph") ;
                BulkLoader1 loader = new BulkLoader1((PGraph)graph, timing, doInParallel, doIncremental, generateStats) ;
                loader.load(urls) ;
                return ;
            }
            
            BulkLoader loader = new BulkLoader((GraphTDB)graph, timing, doInParallel, doIncremental, generateStats) ;
            loader.load(urls) ;
        }
        else
        {
            Model model = getModel() ;
            BulkLoader.loadSimple(model, urls, timing) ;
        }
    }
}

/*
 * (c) Copyright 2008 Hewlett-Packard Development Company, LP
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