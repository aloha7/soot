package hku.cs.seg.experiment.qsic2010;

import hku.cs.seg.experiment.core.*;
import hku.cs.seg.experiment.core.TestEngine.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.Random;

import jcolibri.exception.NoApplicableSimilarityFunctionException;

import residue.ListHits;
import residue.runtime.Monitor;
import residue.tables.Table;
import residue.tables.TableEntry;
import soot.jbco.util.Rand;

import c7302.ActivityRecommender.utils.GPSLocationSimilarity;
import c7302.CityAcitivyRecommendation.Accommodation.PoiCase;
import c7302.CityAcitivyRecommendation.main.UserPreference;
import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;
import edu.cs.hku.instrument.Reporter_StmtCov;
import edu.cs.hku.instrument.Reporter_StmtCov.LinePair;

public class TestSuiteManager {
	
	public static int NubmerOfTestCases = 1000;
	public static int TimeoutTolerance = 500;
	private final int CVSequenceMaxLength = 15;
//	public static final double StartLongitude = 114.170324;	
//	public static final double StartLatitude = 22.277726;
//	public static final int Steptime = 20;
//	public static final double StartLongitude = 114.15;	
//	public static final double StartLatitude = 22.28;
//	public static final int Steptime = 10;
//	public static final double MaxMovingSpeedDegree = 0.000009 * 30 * Steptime;
//	public static final double DeltaAngle = 0.25 * Math.PI;
	//private final double OriDirectionContri = 0;
//	public static final double GPSTimeIntervalSecond = 10;
	private String[] Facilities = new String[]{"FitnessRoom", "Pool", "AirportShuttle", "Parking", "Laundry", "Internet", "Babysitting", "TV", "AirCondition", "CurrencyExchange", "Fax&Copy", "LocalCall", "Reception", "RoomService", "IndependentBathroom"};
	//private final String SQL_InsertTestCasePrefix = 
	private boolean m_doProfiling; 
	private IGpsLocationGenerator m_GPSLocGen;	
	
	public void setDoProfiling(boolean value) {
		m_doProfiling = value;
	}
	
	public void setGPSLocationGenerator(IGpsLocationGenerator value) {
		m_GPSLocGen = value;
	}
	
	public CBRTestSuite generateTestSuite(int startId, BufferedWriter bw, ArrayList<Integer> iBlocks) {
		CBRProgram p = new CBRProgram();
		CBRTestSuite ts = new CBRTestSuite();
	
		p.init();
		int[] basicHits = null;
		int[] blockHitSum = null;
		
		if (m_doProfiling) {
			basicHits = Monitor.hitTable.clone();
			blockHitSum = new int[basicHits.length];
		}
		
		System.out.print("Generating test suite: ");
		try {
			for (int i = 0; i < NubmerOfTestCases; i++) {
				if (i % 10 == 0) System.out.print("[" + i + "] ");				
				
				CBRInput input = new CBRInput();
				int cv = generateInput(input); 
				p.preRun();
				
				long start = 0;
				if (m_doProfiling) {
					bw.write((startId + i) + "\t" + cv + "\t" +input.size());
					Monitor.hitTable = basicHits.clone();
					start = System.currentTimeMillis();
				}
				
				CBROutput output = (CBROutput)p.run(input);

				if (m_doProfiling) {
					int [] hitTable = Monitor.hitTable;
					int hitCount = 0, hitSum = 0;
					StringBuilder tmpSb = new StringBuilder();
					for (int bIndex : iBlocks) {
						tmpSb.append('\t').append(hitTable[bIndex]);
						hitCount += hitTable[bIndex] > 0 ? 1 : 0;
						hitSum += hitTable[bIndex];
						blockHitSum[bIndex] += hitTable[bIndex];
					}					
					bw.write("\t" + (System.currentTimeMillis() - start) + "\t" + p.getQueryResultCount() + "\t" + p.getUserConfirmCount()
							+ "\t" + (new DecimalFormat("0.00").format(hitCount * 100.0 / iBlocks.size())) + "\t" + hitCount + "\t" + hitSum);
					tmpSb.append('\n');
					bw.write(tmpSb.toString());
				}
				
				CBROracle oracle = new CBROracle();
				for (Object o : output.getAccommodationList()) {
					PoiCase c = (PoiCase)o;
					oracle.add(c.getPoiID());
				}
				
				SimpleTestCase stc = new SimpleTestCase(startId + i, input, oracle);
				ts.add(stc);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		for (int bIndex : iBlocks) {
//			if (blockHitSum[bIndex] == 0) {
//				System.out.print(bIndex + " ");
//			}
//		}
//		System.out.println();		
		
		return ts;
	}	
	
	private int generateInput(CBRInput input) {
		Random rand = new Random();
		
		UserPreference dup = generateDefaultUP();
		
		m_GPSLocGen.init();
		double lon = m_GPSLocGen.getLongitude();
		double lat = m_GPSLocGen.getLatitude();
		
//		int seqLen = rand.nextInt(CVSequenceMaxLength) + 1;
		int seqLen = 32;
		for (int i = 0; i < seqLen; i++) {			
			
			UserPreference up = new UserPreference();			
			copyUserPreference(up, dup);
			up.setGpsLocation(String.valueOf(lon) + "," + String.valueOf(lat));
			
			up.setTimestamp((Timestamp)m_GPSLocGen.getTimestamp().clone());
			TestContextVariable tcv = new TestContextVariable(up, (rand.nextInt(4) < 1));
//			TestContextVariable tcv = new TestContextVariable(up, true);
//			TestContextVariable tcv = new TestContextVariable(up, false);
			input.add(tcv);
	
			m_GPSLocGen.generate();
			lon = m_GPSLocGen.getLongitude();
			lat = m_GPSLocGen.getLatitude();
			
		}	
		
		return m_GPSLocGen.getContextDiversity();
	}
	
	private void copyUserPreference(UserPreference up, UserPreference dup) {
		up.setAccomType(dup.getAccomType());
		up.setRoomType(dup.getRoomType());
		up.setFacility(dup.getFacility());
		up.setPaymentMethod(dup.getPaymentMethod());
		up.setRate(dup.getRate());
//		up.setTimestamp(dup.getTimestamp());
		up.setExpenditure(dup.getExpenditure());
		up.setCode(dup.getCode());
		up.setFoodStyle(dup.getFoodStyle());
		up.setProductCategory(dup.getProductCategory());
		up.setFunType(dup.getFunType());
	}
	
	private UserPreference generateDefaultUP() {
		Random rand = new Random();
		
		UserPreference up = new UserPreference();
		up.setAccomType(AccomType.values()[rand.nextInt(AccomType.values().length)]);
		up.setRoomType(RoomType.values()[rand.nextInt(RoomType.values().length)]);
		
		StringBuilder sb = new StringBuilder();
		int n = rand.nextInt(Facilities.length + 1);
		int i = 0;
		for (String fac : Facilities) {
			if (rand.nextInt(Facilities.length + 1) <= n) {
				sb.append(fac);
				sb.append(',');
				i++;
			}
		}
		if (i > 0) {
			sb.setLength(sb.length() - 1);
		}
		up.setFacility(sb.toString());
		
		up.setPaymentMethod(PaymentMethod.values()[rand.nextInt(PaymentMethod.values().length)]);
		up.setRate(5);
		up.setGpsLocation("114.170324,22.277726");
		up.setTimestamp(new Timestamp(1228286553891L));
		up.setExpenditure(2000.0);
		up.setCode(Code.USD);
		up.setFoodStyle(FoodStyle.Chinese);
		up.setProductCategory(ProductCategory.Clothing);
		up.setFunType(FunType.KTV);
		
		return up;
	}

	public static void generateAndPersist(int startId, String tsFilename, String infoFilename, String exFilename, boolean doProfiling, IGpsLocationGenerator gpsLocGen) {
		TestSuiteManager tsm = new TestSuiteManager();
		tsm.setDoProfiling(doProfiling);
		tsm.setGPSLocationGenerator(gpsLocGen);
//		switch (gpsGenName) {
//		case "DR":
//			tsm.setGPSLocationGenerator(new DirectedRandomGPSLocationGenerator(MaxMovingSpeedDegree));
//			
//		}
		
		
		Reporter_StmtCov rsv = null;
		Map<String, Collection<LinePair>> exclusion = null;
		
		if (doProfiling) {
			rsv = new Reporter_StmtCov();
			exclusion = rsv.generateExclusion(exFilename);
		}
		
		ArrayList<Integer> iBlocks = new ArrayList<Integer>(); 
		
		
		int fackIndex = 0;
		BufferedWriter bw = null;
		try {
			if (doProfiling) {
				bw = new BufferedWriter(new FileWriter(infoFilename));
				bw.write("id#\tCD\tLen\tTime\tQry\tCfm\tSC\tHC\tHS");
				
				Table t = ListHits.getTable();
				for (Iterator i = t.iterator(); i.hasNext();) {
					TableEntry o = (TableEntry)i.next();
					if (o != t.getProbedEntry(o.getIndex())) break;
					boolean excluded = false;
					
					Collection<LinePair> excludedLinePairs = exclusion.get(o.getSourceName());
					if (excludedLinePairs != null) {
						for (Object line : o.getLineSet()) {
							if (rsv.isExcluded(excludedLinePairs, (Integer)line)) {
								excluded = true;
								break;
							}
						}
					}
					
					if (doProfiling && !excluded) {
						iBlocks.add(o.getIndex());
						bw.write("\t" + o.getIndex() + "#");
					}
				}
			} 		
		//info.append('\n');
		
			for (int i = 10; i > 0; i--) {
				CBRTestSuite ts = tsm.generateTestSuite(startId, bw, iBlocks);
				ts.writeToTextFile(tsFilename, true);
				System.out.println(startId + "to" + (startId + NubmerOfTestCases - 1) + " Done.");
				startId += NubmerOfTestCases;
			}
			if (doProfiling) {
				bw.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
//		dumpTestsuiteInfo(infoFilename, info.toString());
	}
	
//	private static void dumpTestsuiteInfo(String infoFilename, String info) {		
//		try {
//			bw.write(info);
//			bw.close();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}	
//	}
	
	static int passed = 0, failed = 0, internal = 0, timeout = 0;
	public static String VersionName = "None"; 
	public static void loadAndRun(String filename) {
		CBRTestSuite ts = new CBRTestSuite();
		ts.readFromTextFile(filename);
		
		CBRProgram program = new CBRProgram();
		TestEngine engine = new TestEngine();
		engine.setProgram(program);
		engine.setTestSuite(ts);
		
//		System.out.print("Testing... ");
		
		passed = 0; failed = 0; internal = 0; timeout = 0;
		
		engine.runProgram(TimeoutTolerance, new ProgramRunningCallback(){

			public boolean invoke(ITestCase testCase, ITestOutput testOutput, ProgramTerminationType programTerminationType) {
				StringBuilder sb = new StringBuilder();
				sb.append(VersionName).append("\t").append(testCase.getTestCaseId()).append("\t");
				switch (programTerminationType) {
				case Normal:
					ITestOracle oracle = testCase.getTestOracle();
					boolean isPassed = oracle.isPassed(testOutput);
					if (isPassed) {
						sb.append("Pass");
						passed++;
					} else {
						sb.append("Fail-N");
						failed++;
					}
					break;
				case InternalFailure:
					sb.append("Fail-I");
					internal++;
					break;
				case Timeout:
					sb.append("Fail-T");
					timeout++;
					break;
				}
				
				LogWriter.me().writeln(sb.toString());
				
				int id = testCase.getTestCaseId();
//				if (id % 1000 == 0) System.out.print("[" + id + "] ");
				
				return true;
			}
			
		});		
		
//		System.out.println();
		
		StringBuilder sb1 = new StringBuilder();
		sb1.append("Pass:").append(passed).append(" Fail:").append(failed)
		   .append(" Int-fail:").append(internal).append(" Timeout:").append(timeout);
//		System.out.println(sb1.toString());
//		LogWriter.me().writeln(sb1.toString());
	}
	
//	/**
//	 * @param args
//	 */
//	public static void main(String[] args) {
//		String tsFilename = args[1] + ".pool";
//		String infoFilename = args[1] + ".info";
//		ExecutionTimer.me().startCounter("Total");
//		String logname = "C:\\Jack\\workspace\\QSIC2010\\logs\\" + new SimpleDateFormat("[yyyy-MM-dd] HH-mm-ss").format(new Date(System.currentTimeMillis())) + ".log";
//		LogWriter.me().init(logname);
//		TestSuiteManager.NubmerOfTestCases = Integer.parseInt(args[0]);
//		TestSuiteManager.generateAndPersist(tsFilename, infoFilename);
//		System.out.println("Program init:" + ExecutionTimer.me().endCounter("prog_init") / 1000.0);
//		System.out.println("Query result:" + ExecutionTimer.me().endCounter("query_result") / 1000.0);
//		System.out.println("User confirm:" + ExecutionTimer.me().endCounter("user_confirm") / 1000.0);
//		System.out.println("Similarity computing:" + ExecutionTimer.me().endCounter("siml_comp") / 1000.0);
//		System.out.println("Total:" + ExecutionTimer.me().endCounter("Total") / 1000.0);
//		
//	}

}
