package profile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DUAReporter {
	public static void __link() {}
	
	static class ObjIdxPair {
		final WeakReference weakRef;
		final int origHashCode;
		final int idx;
		public ObjIdxPair(Object o, int idx) { this.weakRef = new WeakReference(o); this.origHashCode = System.identityHashCode(o); this.idx = idx; }
		public boolean equals(Object obj) { return this.weakRef.get() == ((ObjIdxPair)obj).weakRef.get() && this.idx == ((ObjIdxPair)obj).idx; }
		public int hashCode() { return origHashCode + idx; }
	}
	
	private BitSet bsBrDefCov = null;
	private BitSet bsBrPosCov = null;
	private int[] brCovArray = null;
	
	/** Map varId->{(obj,idx)->defId} for last-def of var (per container obj and arrIdx) */
	private static Map varDefsToObjAndIdx = new HashMap();
	
	// Garbage collection of weak references to objects that were destroyed 
	private final static int NUM_DEFS_TO_GC = 1000;
	private static int gcCountdown = NUM_DEFS_TO_GC;
	
	
	/////////////////////////////
	// Methods
	
	public DUAReporter() {}
	
	/**
	 * DUA file format (line sequence): 
	 *   1. in-order DU char (I|N) + dua name string
	 *   2. rdf branch ids for def
	 *   3. rdf branch ids for use
	 *   4. rdf branch ids for in-order kills
	 *   5. rdf branch ids for not-in-order kills
	 * Notes:
	 *   branch ids are separarated by one space
	 *   branch ids are global
	 */
	public void reportFromBranches(int[] brCovArray) {
		// save this array for later use in report-directly method
		this.brCovArray = brCovArray;
		
		// prepare to read dua info from file
		File fDUAs = new File("duas");
		FileInputStream finDUAs;
		try { finDUAs = new FileInputStream(fDUAs); }
		catch (Exception e) { return; }
		
		// prepare to find coverage info at the same time
		int numDefCov = 0, numPossCov = 0, numNotCov = 0;
		int duaIdx = 0;
		bsBrDefCov = new BitSet();
		bsBrPosCov = new BitSet();
		
		// load dua info from file
		BufferedReader rinDUAs = new BufferedReader(new InputStreamReader(finDUAs));
		boolean hybrid = false;
		try {
			hybrid = rinDUAs.readLine().equals("H");
			
			// read dua records, detecting coverage info at the same time
			while (true) {
				// 1. in-order DU char (I|N) + dua name string
				String duaName = rinDUAs.readLine();
				if (duaName == null)
					break; // end of stream
				final boolean inferrOrCondInferr = (duaName.charAt(0) == 'I');
				duaName = duaName.substring(2);
				// 2. rdf branch ids for def
				String brs = rinDUAs.readLine();
				int[] defBrIds = CommonReporter.parseIds(brs);
				// 3. rdf branch ids for use
				brs = rinDUAs.readLine();
				int[] useBrIds = CommonReporter.parseIds(brs);
				// 4. rdf branch ids for in-order kills
				brs = rinDUAs.readLine();
				int[] inOrderKillBrIds = CommonReporter.parseIds(brs);
				// 5. rdf branch ids for not-in-order kills
				brs = rinDUAs.readLine();
				int[] notInOrderKillBrIds = CommonReporter.parseIds(brs);
				
				final boolean definitelyInferrable = inferrOrCondInferr && notInOrderKillBrIds.length == 0;
				
				// check dua if not in hybrid mode, or if it's definitely inferrable
				if (!hybrid || definitelyInferrable) {
					// determine coverage of def and use, and update appropriate counter
					// check coverage of def
					boolean cov = false;
					for (int i = 0; i < defBrIds.length; ++i)
						if (brCovArray[defBrIds[i]] > 0) {
							cov = true;
							break;
						}
					if (cov) {
						// check coverage of use
						cov = false;
						for (int i = 0; i < useBrIds.length; ++i)
							if (brCovArray[useBrIds[i]] > 0) {
								cov = true;
								break;
							}
						if (cov) {
							// check definite and possible kills
							boolean defKilled = false;
							for (int i = 0; i < inOrderKillBrIds.length; ++i) {
								final int killBrId = inOrderKillBrIds[i];
								if (brCovArray[killBrId] > 0) {
									boolean useBr = false; // only kills if br is not use RDF br
									for (int j = 0; j < useBrIds.length; ++j)
										if (killBrId == useBrIds[j]) {
											useBr = true;
											break;
										}
									if (!useBr) {
										defKilled = true;
										break;
									}
								}
							}
							boolean possKilled = false;
							for (int i = 0; i < notInOrderKillBrIds.length; ++i) {
								final int killBrId = notInOrderKillBrIds[i];
								if (brCovArray[killBrId] > 0) {
									boolean useBr = false; // only kills if br is not use RDF br
									for (int j = 0; j < useBrIds.length; ++j)
										if (killBrId == useBrIds[j]) {
											useBr = true;
											break;
										}
									if (!useBr) {
										possKilled = true;
										break;
									}
								}
							}
							
							if (!defKilled) {
								if (possKilled || !inferrOrCondInferr) {
									System.out.println("Br-pos " + duaIdx + ": " + duaName);
									
									++numPossCov;
									bsBrPosCov.set(duaIdx);
								}
								else {
									System.out.println("Br-cov " + duaIdx + ": " + duaName);
									
									++numDefCov;
									bsBrDefCov.set(duaIdx);
								}
							}
							else
								++numNotCov;
						}
					}
					if (!cov)
						++numNotCov;
				}
				
				++duaIdx;
			}
		}
		catch (IOException e) { System.out.println(e); }
		
		// DUA report
		System.out.println("DUA cov from branches: all " + (numDefCov + numPossCov + numNotCov) +
				", def " + numDefCov + ", pos " + numPossCov +
				", not cov " + numNotCov);
		
		// output coverage row to file
		if (!hybrid) {
			// Join in a single bitset the coverage bitsets of possibly- and definitely-covered DUAs
			BitSet bsPosAndDefCov = (BitSet) bsBrDefCov.clone();
			bsPosAndDefCov.or(bsBrPosCov);
			
			addDummyDUAsDefsAndReportToCovMatrix(bsPosAndDefCov, duaIdx, "duainf", "definf", "dublockinf");
		}
	}
	
	/** Reports DUA coverage directly from dua byte array */
	public void reportDirectly(byte[] duaByteArray) {
		// load dua idxs from file, checking coverage of each one
		File f = new File("duasidxs");
		FileInputStream fin;
		try { fin = new FileInputStream(f); }
		catch (Exception e) { return; }
		
		int numCov = 0, numNotCov = 0;
		int duaIdx = 0;
		BitSet bsDirCov = new BitSet();
		
		BufferedReader rin = new BufferedReader(new InputStreamReader(fin));
		boolean hybrid = false;
		try {
			hybrid = rin.readLine().equals("H");
			
			while (true) {
				// read byte index from file
				String duaByteIdx = rin.readLine();
				if (duaByteIdx == null)
					break; // end of stream
				// check coverage immediately
				if (duaByteArray[Integer.parseInt(duaByteIdx)] > 0) {
					System.out.println("Dir-cov " + (numCov + numNotCov));
					
					++numCov;
					bsDirCov.set(duaIdx);
				}
				else
					++numNotCov;
				
				++duaIdx;
			}
		}
		catch (IOException e) { System.out.println(e); }
		
		// report
		System.out.println("DUA cov direct: all " + (numCov + numNotCov) +
				", cov " + numCov + ", not cov " + numNotCov);
		
		if (hybrid) {
			if (bsBrPosCov != null) {
				// join DIR cov results with INF cov (from branches)
				if (bsBrPosCov.cardinality() != 0)
					System.out.println("PROBLEM! some duas marked as pos-cov...");
				else {
					// add bits from branch-inferred duas
					bsDirCov.or(bsBrDefCov);
					
					// report total cov: INF + DIR
					System.out.print("All duas cov:");
					for (int i = 0; i < duaIdx; ++i)
						if (bsDirCov.get(i))
							System.out.print(" " + i);
					System.out.println();
					
					final int totalCov = bsDirCov.cardinality();
					System.out.println("DUA hybrid total cov: all " + duaIdx + ", cov " 
							+ totalCov + ", not cov " + (duaIdx - totalCov));
				}
			}
		}
		else {
			// check against branch-inferred, if available
			int numPosCovInOrder = 0;
			int numPosCovNotInOrder = 0;
			if (bsBrDefCov != null) {
				for (int i = 0; i < duaIdx; ++i) {
					if (bsDirCov.get(i)) {
						if (bsBrPosCov.get(i))
							++numPosCovInOrder;
						else if (!bsBrDefCov.get(i))
							System.out.println("WARNING: dir-cov dua " + i + " not cov by br!");
					}
					else {
						if (bsBrDefCov.get(i))
							System.out.println("WARNING: br-def-cov dua " + i + " not cov directly!");
						else if (bsBrPosCov.get(i))
							++numPosCovNotInOrder;
					}
				}
				
				// report real coverage of possibly covered duas as inferred from branches
				System.out.println("Possibly covered DUA guessed: " + numPosCovInOrder + 
						"/" + (numPosCovInOrder + numPosCovNotInOrder));
			}
		}
		
		// output coverage row to file
		addDummyDUAsDefsAndReportToCovMatrix(bsDirCov, duaIdx, "dua", "def", "dublock");
	}
	
	/**
	 * Reads "dummy" (orphan stmt) duas/defs files, completing coverage bitset using branches for dummy duas/defs, and reports resulting coverage.
	 */
	private void addDummyDUAsDefsAndReportToCovMatrix(BitSet bsCov, int duaIdx, 
			String outDUAFileSuffix, String outDefFileSuffix, String outDUBlockFileSuffix) {
		// make sure that branch cov info is available before proceeding
		if (brCovArray == null) {
			System.out.println("NOTE: branch cov info not available to report on \"dummy\" DUAs");
			return;
		}
		
		// Read br reqs for "dummy" DUAs (i.e., BBs)
		List dummyDUAsReqBrs = getDummyReqBranches("dummyduas");
		List dummyDefsReqBrs = getDummyReqBranches("dummydefs");
		
		BitSet bsDUACov = (BitSet) bsCov.clone();
		final int totalDUAs = addDummyCov(bsDUACov, duaIdx, dummyDUAsReqBrs);
		BitSet bsDefCov = (BitSet) bsCov.clone();
		final int totalDefs = addDummyCov(bsDefCov, duaIdx, dummyDefsReqBrs);
		BitSet bsDUBlockCov = (BitSet) bsCov.clone();
		final int totalDUBlocks = duaIdx;
		
		CommonReporter.reportToCovMatrixFile(bsDUACov, totalDUAs, outDUAFileSuffix);
		CommonReporter.reportToCovMatrixFile(bsDefCov, totalDefs, outDefFileSuffix);
		CommonReporter.reportToCovMatrixFile(bsDUBlockCov, totalDUBlocks, outDUBlockFileSuffix);
	}

	private int addDummyCov(BitSet bsCov, int duaIdx, List dummyDUAsReqBrs) {
		// Append to bitset coverage of "dummy" DUAs (i.e., BBs of statements not included by DUAs)
		for (Iterator itDummyDUA = dummyDUAsReqBrs.iterator(); itDummyDUA.hasNext(); ) {
			int[] dummyDUAReqArray = (int[]) itDummyDUA.next();
			for (int i = 0; i < dummyDUAReqArray.length; ++i) {
				if (brCovArray[ dummyDUAReqArray[i] ] > 0) {
					bsCov.set(duaIdx);
					break; // no need to continue
				}
			}
			
			++duaIdx; // next DUA
		}
		return duaIdx;
	}

	private List getDummyReqBranches(String dummyFilename) {
		List dummiesReqBrs = new ArrayList();
		File fDummyDUAs = new File(dummyFilename);
		try {
			FileInputStream finDummyDUAs = new FileInputStream(fDummyDUAs);
			BufferedReader rinDummyDUAs = new BufferedReader(new InputStreamReader(finDummyDUAs));
			while (true) {
				// read next line with requirements for next dummy DUA
				String sDummyDUAReqs = rinDummyDUAs.readLine();
				if (sDummyDUAReqs == null)
					break; // end of stream
				// parse space-separated ids of required branches for dummy DUAs
				int[] dummyDUAReqArray = CommonReporter.parseIds(sDummyDUAReqs);
				dummiesReqBrs.add(dummyDUAReqArray);
			}
		}
		catch (IOException e) { System.out.println(e); }
		return dummiesReqBrs;
	}
	
	//////////////////////////
	// NON-DEFINITE defs/uses
	
	public static void defEvent(int varId, int defIdx, Object oContainer, int arrIdx) {
		// get/create map (obj,idx)->defId for var id
		final Integer varIdInt = new Integer(varId);
		Map oiPairToDefForVar = (Map) varDefsToObjAndIdx.get(varIdInt);
		if (oiPairToDefForVar == null) {
			oiPairToDefForVar = new HashMap();
			varDefsToObjAndIdx.put(varIdInt, oiPairToDefForVar);
		}
		// store def idx for varId and (obj,idx) pair
		ObjIdxPair oiPair = new ObjIdxPair(oContainer, arrIdx);
		Integer oldDefIdx = (Integer) oiPairToDefForVar.put(oiPair, new Integer(defIdx));
		
		// call GC for weak refs if it's time to do so
		--gcCountdown;
		if (gcCountdown <= 0) {
			gcCountdown = NUM_DEFS_TO_GC;
			CollectGarbageWeakRefs();
		}
	}
	
	public static void useEvent(int varId, int baseByteIdx, byte[] duaCovArray, Object oContainer, int arrIdx) {
		final Integer varIdInt = new Integer(varId);
		// map (obj,idx)->defId
		Map oiPairToDefForVar = (Map) varDefsToObjAndIdx.get(varIdInt);
		if (oiPairToDefForVar != null) {
			ObjIdxPair oiPair = new ObjIdxPair(oContainer, arrIdx);
			Integer defIdx = (Integer) oiPairToDefForVar.get(oiPair);
			if (defIdx != null)
				duaCovArray[baseByteIdx + defIdx.intValue()] = 1;
		}
	}
	
	private static void CollectGarbageWeakRefs() {
		// varDefsToObjAndIdx: Map varId->{(obj,idx)->defId} for last-def of var (per container obj and arrIdx)
		for (Iterator itVarId = varDefsToObjAndIdx.keySet().iterator(); itVarId.hasNext(); ) {
			Object oVarId = itVarId.next();
			Map objIdxToDefIds = (Map) varDefsToObjAndIdx.get(oVarId);
			
			// find obj-idx pairs that need removal for this var
			List objIdxsToRemove = new ArrayList();
			for (Iterator itObjIdx = objIdxToDefIds.keySet().iterator(); itObjIdx.hasNext(); ) {
				ObjIdxPair objIdx = (ObjIdxPair) itObjIdx.next();
				if (objIdx.weakRef.get() == null)
					objIdxsToRemove.add(objIdx);
			}
			
			// remove those obj-idx pairs for this var
			for (Iterator itObjIdxToRemove = objIdxsToRemove.iterator(); itObjIdxToRemove.hasNext(); )
				objIdxToDefIds.remove(itObjIdxToRemove.next());
		}
	}
	
}
