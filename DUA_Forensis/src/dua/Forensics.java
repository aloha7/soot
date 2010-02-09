package dua;
/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-1999 Raja Vallee-Rai
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */

/*
 * Modified by the Sable Research Group and others 1997-1999.  
 * See the 'credits' file distributed with Soot for the complete list of
 * contributors.  (Soot is distributed at http://www.sable.mcgill.ca/soot)
 */

/* Reference Version: $SootVersion: 1.beta.6.dev.51 $ */

//package ashes.examples.countgotos;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import profile.BranchInstrumenter;
import profile.CatchWrapInstrumenter;
import profile.DUAInstrumenter;
import profile.EPPInstrumenter;
import profile.TestLabelInstrumenter;
import soot.PackManager;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Transform;
import dua.global.ProgramFlowGraph;
import dua.global.ReachabilityAnalysis;
import dua.global.ReqBranchAnalysis;
import dua.global.ProgramFlowGraph.EntryNotFoundException;
import dua.global.p2.P2Analysis;
import dua.method.CFG;
import dua.method.DominatorRelation;
import dua.method.EPPAnalysis;
import dua.method.CFG.CFGNode;
import dua.method.CFGDefUses.Branch;
import dua.method.CFGDefUses.Def;
import dua.method.CFGDefUses.Use;
import fault.BranchStmtMapper;
import fault.DUAStmtMapper;
import fault.PathMapper;
import fault.StmtMapper;

/** Main class for DUA-Forensics. Takes own options and invokes Soot after processing them,
 *  with the remaining options. */
public class Forensics extends SceneTransformer
{
	private static Collection<Extension> extensions = new ArrayList<Extension>();
	public static void registerExtension(Extension ext) { extensions.add(ext); }
	
	public static void main(String[] args) {
		String[] sootArgs = Options.parseFilterArgs(args);
		
		System.out.print("DUA-Forensics args to Soot: ");
		for (String s : sootArgs)
			System.out.print(s + " ");
		System.out.println();
		
		// add this transformation, if not deactivated, and call soot main
		if (Options.doDuas())
			PackManager.v().getPack("wjtp").add(new Transform("wjtp.mt", new Forensics()));
		
		if (Options.allowPhantomClasses())
			soot.options.Options.v().set_allow_phantom_refs(true);
		soot.Main.main(sootArgs);
	}
	
	public void run() {
		internalTransform("", new HashMap());
	}
	
	@Override
	protected void internalTransform(String phaseName, Map options) {
//		final boolean verbose = false; //true;
		
		System.out.println("RAUL's internal transform");
		
		try { ProgramFlowGraph.createInstance(DUAAnalysis.getCFGFactory()); }
		catch (EntryNotFoundException e) { throw new RuntimeException(e.getMessage()); }
		
		// make sure (single) entry exists
		List<SootMethod> entryMethods = ProgramFlowGraph.inst().getEntryMethods();
		
		// having already found call sites, compute reachability
		if (Options.reachability())
			ReachabilityAnalysis.computeReachability(entryMethods);
		
		// the first thing is to perform p2 analysis
		P2Analysis.inst().compute();
		
		// note that local dominance was computed when mtags where created
		DominatorRelation.createInstance(); // option dominance() used inside to decide btw interproc dom or just local dom
		
		DUAAnalysis.createDUAAnalysis();
		DUAAnalysis duaAnalysis = DUAAnalysis.inst();
		
		// do Required Branches Analysis for the set of all CFGs
		ReqBranchAnalysis.createInstance();
		ReqBranchAnalysis reqBrAnalysis = ReqBranchAnalysis.inst();
		reqBrAnalysis.print(); // DEBUG
//		outputDUAReqBranches(duaAnalysis); // DEBUG
		
		// Retrieve set of DUAs, required branches, and acyclic paths
		DUAssocSet duaSet = duaAnalysis.getDUASet();
		List<DUA> duas = duaSet.getAllDUAs();
		List<Branch> brs = Options.allBranches()? reqBrAnalysis.getAllBranches() : reqBrAnalysis.getInstrBranches(duas);
		Map<CFG, EPPAnalysis> cfgEPPAnalyses = Options.eppInstr()?
				EPPAnalysis.computeInterprocEPP(Options.eppDepth(), ProgramFlowGraph.inst().getCFGs()) : null;
		
		// DEBUG
//		PathDUAAnalysis pathDuaAnalysis = new PathDUAAnalysis(duaAnalysis.getDUASet());
//		pathDuaAnalysis.computeInferability();
		
		// Output stmt mapping files for debugging (coverage report files)
		// These files are generated BEFORE instrumenting, to avoid including statements inserted later
		if (Options.allBranches()) {
			// output files mapping stmts to stmts, ordered by global stmt id
			StmtMapper.writeEntityStmtFiles();
			
			// output files with stmts related to each branch, ordered by branch id
			BranchStmtMapper.writeEntityStmtFiles(brs, reqBrAnalysis);
		}
		if (Options.anyDuaInstr()) {
			// output files with stmts related to each DUA, ordered by DUA id
			DUAStmtMapper.writeEntityStmtFiles(duaSet, brs, reqBrAnalysis);
		}
		if (Options.eppInstr()) {
			// output files with stmts related to each acyclic (EPP) path, ordered by global path id
			PathMapper.writeEntityStmtFiles(cfgEPPAnalyses);
		}
		
		////////////////////////
		// Instrument program
		if (Options.anyInstr()) {
			// first of all, wrap entry methods to avoid exceptions and returns 
			// escaping reporter code at the end of each method
			(new CatchWrapInstrumenter()).instrument(entryMethods);
			
			// instrument to print label at the beginning of each 'test*' method
			(new TestLabelInstrumenter()).instrument(entryMethods);
		}
		
		BranchInstrumenter brInstrumenter = null;
		if (Options.brInstr()) {
			// Compute required branches if necessary
			
			// instrument branches
			brInstrumenter = new BranchInstrumenter();
			
			// one probe per branch instrumentation
			if (Options.brSomeProbes()) {
				// experiment: pick an arbitrary subset of branches
				ArrayList<Branch> brsSubset = new ArrayList<Branch>();
				for (int i = 0 ; i < (brs.size()*3)/4; ++i)
					brsSubset.add(brs.get((i*4)/3));
				brs = brsSubset;
			}
			
			if (Options.brOptimalInstr())  // optimal edge instrumentation
				brInstrumenter.instrumentEdgesOptimal(brs, reqBrAnalysis, entryMethods);
			else
				brInstrumenter.instrumentDirect(brs, entryMethods);
		}
		if (Options.eppInstr()) {
			EPPInstrumenter eppInstrumenter = new EPPInstrumenter();
			eppInstrumenter.instrument(cfgEPPAnalyses);
		}
		if (Options.anyDuaInstr()) {
			// Compute required branches if necessary
			if (ReqBranchAnalysis.inst() == null) {
				ReqBranchAnalysis.createInstance();
				reqBrAnalysis = ReqBranchAnalysis.inst();
				reqBrAnalysis.print(); // DEBUG
				outputDUAReqBranches(duaAnalysis);
			}
			
			// instrument DUAs
			DUAInstrumenter duaInstrumenter = new DUAInstrumenter(brInstrumenter);
			if (Options.duaInstrNoProbes()) {
				ArrayList<DUA> duasSubset = new ArrayList<DUA>();
				for (int i = 0 ; i < duas.size()/2; ++i)
					duasSubset.add(duas.get(i*2));
				duas = duasSubset;
				duas.add(duaAnalysis.getDUASet().getAllDUAs().iterator().next());
			}
			duaInstrumenter.instrument(duas, reqBrAnalysis, entryMethods);
		}
		
		// run extensions
		for (Extension ext : extensions)
			ext.run();
	}
	
	// DEBUG
	private void outputDUAReqBranches(DUAAnalysis duaAnalysis) {
		// DEBUG - output RDFs
		ReqBranchAnalysis reqBrAnalysis = ReqBranchAnalysis.inst();
		System.out.println("Required branches:");
		int duaIdx = 0;
		for (DUA dua : duaAnalysis.getDUASet().getAllDUAs()) {
			CFGNode nDef = dua.getDef().getN();
			
			System.out.println(duaIdx++ + ":  " + dua + ": def " + reqBrAnalysis.getReqBranches(nDef) + 
					", use " + reqBrAnalysis.getUseReqBranches(dua.getUse()));
		}
		
		// also print same-BB DUs
		System.out.println("Same-BB DUs:");
		Map<Def, Set<Use>> sameBBDUs = duaAnalysis.getDUASet().getSameBBDUs();
		for (Def def : sameBBDUs.keySet()) {
			Set<Use> uses = sameBBDUs.get(def);
			for (Use u : uses)
				System.out.println("  " + def + ", " + u);
		}
	}
	
}
