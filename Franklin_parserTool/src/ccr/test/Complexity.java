package ccr.test;

import ccr.stat.*;

import java.io.*;
import java.lang.reflect.*;

public class Complexity {
	
	public static final String[] METHOD_SET = {"getDUAssociations", "resolveKInline", 
		"resolveFullInline", "resolveKDemand", "resolveFullDemand"};
	
	public static long timeCost(CFG cfg, String methodName, int k, int repeat) {
		long cost = 0;
		try {
			Class parameterTypes[];
			Object args[];
			if (k == -1) {
				parameterTypes = null;
				args = null;
			} else {
				parameterTypes = new Class[1];
				parameterTypes[0] = int.class;
				args = new Object[1];
				args[0] = new Integer(k);
			}
			Method method = cfg.getClass().getMethod(methodName, parameterTypes);
			long startTime = System.currentTimeMillis();
			for (int i = 0; i < repeat; i++) {
				method.invoke(cfg, args);
			}
			long endTime = System.currentTimeMillis();
			cost = endTime - startTime;
		} catch (NoSuchMethodException e) {
			System.out.println(e);
		} catch (IllegalAccessException e) {
			System.out.println(e);
		} catch (InvocationTargetException e) {
			System.out.println(e);
		}
		return cost;
	}
	
	public static void recordTimeCost(CFG cfg, String methodName, int k, int trials, String reportFile) {
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(reportFile));
			for (int i = 0; i < trials; i++) {
				String line = "" + timeCost(cfg, methodName, k, 1);
				bw.write(line);
				bw.newLine();
			}
			bw.flush();
			bw.close();
		} catch (IOException e) {
			System.out.println(e);
		}
	}
	
	public static void main(String argv[]) {
		
		CFG g = new CFG("src/ccr/app/TestCFG2.java");
	//	int repeat = 1;
		
	/*	long startTime = System.currentTimeMillis();
		for (int i = 0; i < repeat; i++) {
			g.resolveKInline(2);
		}
		long endTime = System.currentTimeMillis();
		System.out.println(endTime - startTime);
		
		startTime = System.currentTimeMillis();
		for (int i = 0; i < repeat; i++) {
			g.resolveKDemand(2);
		}
		endTime = System.currentTimeMillis();
		System.out.println(endTime - startTime);*/
		
	/*	System.out.println(timeCost(g, "getDUAssociations", -1, repeat));
		System.out.println(timeCost(g, "resolveKDemand", 1, repeat));
		System.out.println(timeCost(g, "resolveKInline", 1, repeat));
		System.out.println(timeCost(g, "resolveKDemand", 2, repeat));
		System.out.println(timeCost(g, "resolveKInline", 2, repeat));
		System.out.println(timeCost(g, "resolveKDemand", 3, repeat));
		System.out.println(timeCost(g, "resolveKInline", 3, repeat));
		System.out.println(timeCost(g, "resolveFullDemand", -1, repeat));
		System.out.println(timeCost(g, "resolveFullInline", -1, repeat));*/
		
		int trials = 10;
		if (argv.length == 1) {
			trials = Integer.parseInt(argv[0]);
		}

		recordTimeCost(g, "resolveFullDemand", -1, trials, "experiment/timecost-report-fullResolvedDemand.txt");
		recordTimeCost(g, "resolveFullInline", -1, trials, "experiment/timecost-report-fullResolvedInline.txt");
		recordTimeCost(g, "getDUAssociations", -1, trials, "experiment/timecost-report-allDU.txt");
		recordTimeCost(g, "resolveKDemand", 1, trials, "experiment/timecost-report-1ResolvedDemand.txt");
		recordTimeCost(g, "resolveKInline", 1, trials, "experiment/timecost-report-1ResolvedInline.txt");
		recordTimeCost(g, "resolveKDemand", 2, trials, "experiment/timecost-report-2ResolvedDemand.txt");
		recordTimeCost(g, "resolveKInline", 2, trials, "experiment/timecost-report-2ResolvedInline.txt");
	}

}
