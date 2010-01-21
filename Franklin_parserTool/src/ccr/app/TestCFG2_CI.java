package ccr.app;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.Vector;

import ccr.test.Logger;

public class TestCFG2_CI extends Application {
	
//	private final int PATH_LEN = /*100*/100;  // Length of a random path
	private final long STAY_TIME = 200; // Each stay takes 200 ms	
	private final double WALK_DIST = /*1.8*/0.9; // At least 1.8 m for each step in walk
	private final double VELOCITY = 2.0; // Supposed velocity: 2.0 m/s (corresponding freshness requirement: 5 s)
	private final double NOISE = 0.5;  // At most 0.5 m noise in locations
	private final double ERR = /*0.7*/0.5;  // At most 0.7 m allowed error in locations
//	private final int FIX_PID = 1;  // Fix position id
	private final int MAX_STAY = 5;  // At most 5 stays at the same position in a random path
//	private final int MODE_STAY = 0;  // Always in stay
//	private final int MODE_WALK = 1;  // Always in walk
//	private final int MODE_MIX = 2;  // In stay and walk
	private final int sid = 0; // Scenario id
	
	private int counter = 0;
//	private int lastPos = -1; // Last position id
	private double curEstX = 0.0, curEstY = 0.0; // Current estimated location
//	private int mode = MODE_MIX;
	
	//2009-12-14: test suite reduction research
	public Vector queue;
	public long timestamp;
	public Context candidate;
	public Vector PositionQueue = new Vector();
	public int lastPos = -1;
	public int activation = 0;
	public Object application(String testcase) {
		
		// Program ID [c]
		
		// Ordinary Variable [location, lastLocation, moved, reliable, displace, error, curEstX, curEstY, c, bPos, cPos, stay, lastPos, timestamp, counter, t, actLoc, estLoc, lastLoc, dist]
		
		// Context Variable [candidate]
		
		// Assignment [=]
		
		int seed = Integer.parseInt(testcase);
		queue = new Vector();
		Coordinates location = null;
		Random rand = new Random(seed);
		CCRScenarios scenarios = new CCRScenarios(seed);
		long t;
		Coordinates actLoc;
		Coordinates estLoc;
		Coordinates lastLoc;
		double dist;
		double displace;
		double error;
		
		// ENTRY // NODE
		
		int c = 0;
		int bPos = -1;
		int cPos = -1;
		int stay = 0;
	//	int mode = MODE_MIX;
//		int lastPos = -1;
		timestamp = System.currentTimeMillis();
		counter = 0;
	//	double distance = 0;
		int moved = 0;
		int reliable = 0;
		Coordinates lastLocation = new Coordinates(0, 0);
		
		// initial
	//	if (bPos == -1) {
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
	//	} else {
	//		cPos = rand.nextInt(CCRScenarios.POS_NUM);
	//		while (cPos == -1 || cPos == bPos ||
	//			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
	//			cPos = rand.nextInt(CCRScenarios.POS_NUM);
	//		}
	//	}
		stay = rand.nextInt(MAX_STAY) + 1;
	//	mode = MODE_MIX; // Experimentation: Variation on mode
	//	if (mode == MODE_WALK) {
	//		stay = 1;  // Always walk without stay
	//	}
	//	if (c + stay > PATH_LEN) {
	//		stay = PATH_LEN - c;
	//	}
		c = c + stay;
		bPos = cPos;
		
		stay = stay - 1; // initial
		
		actLoc = scenarios.getActLoc(sid, cPos);
		// Set the estimated location for pos
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		
		// Apply the noise
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		
		// Decide the time we should wait before sending the next context
		t = 0;  // Only for the first time
	//	if (lastPos != -1) {  // Not the first time
	//		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
	//		dist = Coordinates.calDist(lastLoc, actLoc);
	//		if (lastPos != cPos) {  // Walk
	//			t = (long) (dist / VELOCITY * 1000); // Estimated time required from lastLoc to actLoc
	//		} else {  // Stay
	//			t = STAY_TIME;
	//		}
	//	}
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate); //2010-01-11:may add this into the PositionQueue
	//	System.out.println(counter + ":\t(" + location.x + ", " + location.y + ")");
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		
		while (stay > 0) { // initial
			stay = stay - 1;
			
			actLoc = scenarios.getActLoc(sid, cPos);
			// Set the estimated location for pos
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			
			// Apply the noise
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			
			// Decide the time we should wait before sending the next context
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
		//	if (lastPos != cPos) {  // Walk
		//		t = (long) (dist / VELOCITY * 1000); // Estimated time required from lastLoc to actLoc
		//	} else {  // Stay
				t = STAY_TIME;
		//	}
			timestamp = timestamp + t;
		//	lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	System.out.println(counter + ":\t(" + location.x + ", " + location.y + ")");
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}
		
		// 1
		cPos = rand.nextInt(CCRScenarios.POS_NUM);
		while (cPos == -1 || cPos == bPos ||
			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
			//WALK_DIST is the default distance to determine whether two locations are the same one
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
		}
		stay = rand.nextInt(MAX_STAY) + 1;
		c = c + stay;
		bPos = cPos;
		stay = stay - 1; // 1
		actLoc = scenarios.getActLoc(sid, cPos);
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
		dist = Coordinates.calDist(lastLoc, actLoc);
		t = (long) (dist / VELOCITY * 1000); // Experimentation: Estimated time required from lastLoc to actLoc
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate);
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		while (stay > 0) { // 1
			stay = stay - 1;
			actLoc = scenarios.getActLoc(sid, cPos);
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
			t = STAY_TIME; // Experimentation:
			timestamp = timestamp + t;
			lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}

		// 2
		cPos = rand.nextInt(CCRScenarios.POS_NUM);
		while (cPos == -1 || cPos == bPos ||
			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
		}
		stay = rand.nextInt(MAX_STAY) + 1;
		c = c + stay;
		bPos = cPos;
		stay = stay - 1; // 2
		actLoc = scenarios.getActLoc(sid, cPos);
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
		dist = Coordinates.calDist(lastLoc, actLoc);
		t = (long) (dist / VELOCITY * 1000); // Experimentation: Estimated time required from lastLoc to actLoc
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate);
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		while (stay > 0) { // 2
			stay = stay - 1;
			actLoc = scenarios.getActLoc(sid, cPos);
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
			t = STAY_TIME; // Experimentation:
			timestamp = timestamp + t;
			lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}

		// 3
		cPos = rand.nextInt(CCRScenarios.POS_NUM);
		while (cPos == -1 || cPos == bPos ||
			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
		}
		stay = rand.nextInt(MAX_STAY) + 1;
		c = c + stay;
		bPos = cPos;
		stay = stay - 1; // 3
		actLoc = scenarios.getActLoc(sid, cPos);
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
		dist = Coordinates.calDist(lastLoc, actLoc);
		t = (long) (dist / VELOCITY * 1000); // Experimentation: Estimated time required from lastLoc to actLoc
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate);
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		while (stay > 0) { // 3
			stay = stay - 1;
			actLoc = scenarios.getActLoc(sid, cPos);
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
			t = STAY_TIME; // Experimentation:
			timestamp = timestamp + t;
			lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}

		// 4
		cPos = rand.nextInt(CCRScenarios.POS_NUM);
		while (cPos == -1 || cPos == bPos ||
			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
		}
		stay = rand.nextInt(MAX_STAY) + 1;
		c = c + stay;
		bPos = cPos;
		stay = stay - 1; // 4
		actLoc = scenarios.getActLoc(sid, cPos);
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
		dist = Coordinates.calDist(lastLoc, actLoc);
		t = (long) (dist / VELOCITY * 1000); // Experimentation: Estimated time required from lastLoc to actLoc
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate);
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		while (stay > 0) { // 4
			stay = stay - 1;
			actLoc = scenarios.getActLoc(sid, cPos);
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
			t = STAY_TIME; // Experimentation:
			timestamp = timestamp + t;
			lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}

		// 5
		cPos = rand.nextInt(CCRScenarios.POS_NUM);
		while (cPos == -1 || cPos == bPos ||
			Coordinates.calDist(scenarios.getActLoc(sid, bPos), scenarios.getActLoc(sid, cPos)) < WALK_DIST) {
			cPos = rand.nextInt(CCRScenarios.POS_NUM);
		}
		stay = rand.nextInt(MAX_STAY) + 1;
		c = c + stay;
		bPos = cPos;
		stay = stay - 1; // 5
		actLoc = scenarios.getActLoc(sid, cPos);
		estLoc = scenarios.getEstLoc(sid, cPos);
		curEstX = estLoc.x;
		curEstY = estLoc.y;
		curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
		lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
		dist = Coordinates.calDist(lastLoc, actLoc);
		t = (long) (dist / VELOCITY * 1000); // Experimentation: Estimated time required from lastLoc to actLoc
		timestamp = timestamp + t;
		lastPos = cPos;
		candidate = generateCtx();
		resolve();
		location = toCoordinates(candidate);
	//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
		displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
				(location.y - lastLocation.y) * (location.y - lastLocation.y));
		moved = moved + toBoolean(displace);
		error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
				(actLoc.y - location.y) * (actLoc.y - location.y));
		lastLocation = location;
		counter = counter + 1;
		while (stay > 0) { // 5
			stay = stay - 1;
			actLoc = scenarios.getActLoc(sid, cPos);
			estLoc = scenarios.getEstLoc(sid, cPos);
			curEstX = estLoc.x;
			curEstY = estLoc.y;
			curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // [- NOISE, + NOISE)
			lastLoc = scenarios.getActLoc(sid, lastPos); // The actual location for lastPos
			dist = Coordinates.calDist(lastLoc, actLoc);
			t = STAY_TIME; // Experimentation:
			timestamp = timestamp + t;
			lastPos = cPos;
			candidate = generateCtx();
			resolve();
			location = toCoordinates(candidate);
		//	distance = distance + Coordinates.calDist(location, lastLocation); // Experiments: calculate distance
			displace = Math.sqrt((location.x - lastLocation.x) * (location.x - lastLocation.x) + 
					(location.y - lastLocation.y) * (location.y - lastLocation.y));
			moved = moved + toBoolean(displace);
			error = Math.sqrt((actLoc.x - location.x) * (actLoc.x - location.x) + 
					(actLoc.y - location.y) * (actLoc.y - location.y));
			if (error <= ERR) {
				reliable = reliable + 1;
			}
			lastLocation = location;
			counter = counter + 1;
		}
		
	//	Double result = new Double(distance);
		ApplicationResult result = new ApplicationResult(moved, reliable);
		// EXIT // NODE
		
		return result;
	}
	
	private Context generateCtx() {
		
		Context ctx = new Context();
		ctx.put(Context.FLD_CATEGORY, getCategory());
		ctx.put(Context.FLD_SUBJECT, getSubject());
		ctx.put(Context.FLD_PREDICATE, getPredicate());
		ctx.put(Context.FLD_OBJECT, getObject());
		ctx.put(Context.FLD_START_FROM, getStartFrom());
		ctx.put(Context.FLD_END_AT, getEndAt());
		ctx.put(Context.FLD_SITE, getSite());
		ctx.put(Context.FLD_OWNER, getOwner());
		
		// FLD_ID, FLD_TIMESTAMP, FLD_CONSISTENCY are unnecessary
		
		ctx.put(Context.FLD_TIMESTAMP, TimeFormat.convert(timestamp));  // Set FLD_TIMESTAMP
		
		return ctx;
	}
	
	private String getCategory() {
	
		return "location";
	}
	
	private String getSubject() {
		
		return "Jialin";
	}
	
	private String getPredicate() {
		
		return "estimated at";
	}
	
	private String getObject() {
		
		return "" + curEstX + " " + curEstY;  // Estimated location
	}
	
	private String getStartFrom() {
		
		return TimeFormat.convert(System.currentTimeMillis());
	}
	
	private String getEndAt() {
		
		return TimeFormat.convert(System.currentTimeMillis());
	}
	
	private String getSite() {
		
		return "HKUST";
	}
	
	private String getOwner() {
		
		return "" + counter;  // Special purpose, used to decide if two contexts are adjacent or not
	}
	
	private boolean filterLocCons2Stay(Context ctx1, Context ctx2) { // filter 1
		
		int c1 = Integer.parseInt((String) ctx1.get(Context.FLD_OWNER));
		int c2 = Integer.parseInt((String) ctx2.get(Context.FLD_OWNER));
		long t1 = TimeFormat.convert((String) ctx1.get(Context.FLD_TIMESTAMP));
		long t2 = TimeFormat.convert((String) ctx2.get(Context.FLD_TIMESTAMP));//System.out.print(" f1:" + t2 + " ");
		if (c1 + 1 == c2 && t2 - t1 <= STAY_TIME + 100) { // Adjacent and in stay
		//	System.out.print(" f1 true ");
			return true;
		} else {
			return false;
		}
	}
	
	private boolean filterLocCons2Walk(Context ctx1, Context ctx2) { // filter 2
		
		int c1 = Integer.parseInt((String) ctx1.get(Context.FLD_OWNER));
		int c2 = Integer.parseInt((String) ctx2.get(Context.FLD_OWNER));
		long t1 = TimeFormat.convert((String) ctx1.get(Context.FLD_TIMESTAMP));
		long t2 = TimeFormat.convert((String) ctx2.get(Context.FLD_TIMESTAMP));
		if (c1 + 1 == c2 && t2 - t1 > STAY_TIME + 100) { // Adjacent and in walk
			return true;
		} else {
			return false;
		}
	}
	
	private boolean filterLocSkip1Stay(Context ctx1, Context ctx2) { // filter 3
		
		int c1 = Integer.parseInt((String) ctx1.get(Context.FLD_OWNER));
		int c2 = Integer.parseInt((String) ctx2.get(Context.FLD_OWNER));
		long t1 = TimeFormat.convert((String) ctx1.get(Context.FLD_TIMESTAMP));
		long t2 = TimeFormat.convert((String) ctx2.get(Context.FLD_TIMESTAMP));
		if (c1 + 2 == c2 && t2 - t1 <= 2 * (STAY_TIME + 100)) { // Skipping 1 and in stay
			return true;
		} else {
			return false;
		}
	}
	
	private boolean filterLocSkip1Walk(Context ctx1, Context ctx2) { // filter 4
		
		int c1 = Integer.parseInt((String) ctx1.get(Context.FLD_OWNER));
		int c2 = Integer.parseInt((String) ctx2.get(Context.FLD_OWNER));
		long t1 = TimeFormat.convert((String) ctx1.get(Context.FLD_TIMESTAMP));
		long t2 = TimeFormat.convert((String) ctx2.get(Context.FLD_TIMESTAMP));
		if (c1 + 2 == c2 && t2 - t1 >= 2 * (long) (WALK_DIST / VELOCITY * 1000)) { // Skipping 1 and in walk
			return true;
		} else {
			return false;
		}
	}
	
	private boolean filterLocSkip1Mix(Context ctx1, Context ctx2) { // filter 5
		
		int c1 = Integer.parseInt((String) ctx1.get(Context.FLD_OWNER));
		int c2 = Integer.parseInt((String) ctx2.get(Context.FLD_OWNER));
		long t1 = TimeFormat.convert((String) ctx1.get(Context.FLD_TIMESTAMP));
		long t2 = TimeFormat.convert((String) ctx2.get(Context.FLD_TIMESTAMP));
		if (c1 + 2 == c2 && t2 - t1 > 2 * (STAY_TIME + 100) &&
			t2 - t1 < 2 * (long) (WALK_DIST / VELOCITY * 1000)) { // Skipping 1 and in stay and walk
			return true;
		} else {
			return false;
		}
	}
	
	private boolean funcLocDistOk(Context ctx1, Context ctx2) { // boolean function 1
		
		String v1 = (String) ctx1.get(Context.FLD_OBJECT);
		String v2 = (String) ctx2.get(Context.FLD_OBJECT);
		if (v1 == null || v2 == null) {
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(v1);
		double x1 = Double.parseDouble(st.nextToken());
		double y1 = Double.parseDouble(st.nextToken());
		st = new StringTokenizer(v2);
		double x2 = Double.parseDouble(st.nextToken());
		double y2 = Double.parseDouble(st.nextToken());
		double dist = Coordinates.calDist(x1, y1, x2, y2);
		
		// The distance should not be larger than two times the allowed error
		boolean result = false;
		if (dist <= 2 * ERR) {
			result = true;
		}
		
		return result;
	}
	
	private boolean funcLocWalkAdjVeloOk(Context ctx1, Context ctx2) { // boolean function 2
		
		String v1 = (String) ctx1.get(Context.FLD_OBJECT);
		String v2 = (String) ctx1.get(Context.FLD_TIMESTAMP);
		String v3 = (String) ctx2.get(Context.FLD_OBJECT);
		String v4 = (String) ctx2.get(Context.FLD_TIMESTAMP);
		if (v1 == null || v2 == null || v3 == null || v4 == null) {
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(v1);
		double x1 = Double.parseDouble(st.nextToken());
		double y1 = Double.parseDouble(st.nextToken());
		st = new StringTokenizer(v3);
		double x2 = Double.parseDouble(st.nextToken());
		double y2 = Double.parseDouble(st.nextToken());
		double dist = Coordinates.calDist(x1, y1, x2, y2);
		long t = TimeFormat.convert(v4) - TimeFormat.convert(v2);
		
		// The velocity should be between vmin and vmax
		boolean result = false;
		double vmin = (VELOCITY * ((double) t / 1000) - 2 * ERR) / ((double) t / 1000);
		double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
		double ve = dist / ((double) t / 1000);
		if (ve >= vmin && ve <= vmax) {
			result = true;
		}
		
		return result;
	}
	
	private boolean funcLocWalkSkipVeloOk(Context ctx1, Context ctx2) { // boolean function 3
		
		String v1 = (String) ctx1.get(Context.FLD_OBJECT);
		String v2 = (String) ctx1.get(Context.FLD_TIMESTAMP);
		String v3 = (String) ctx2.get(Context.FLD_OBJECT);
		String v4 = (String) ctx2.get(Context.FLD_TIMESTAMP);
		if (v1 == null || v2 == null || v3 == null || v4 == null) {
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(v1);
		double x1 = Double.parseDouble(st.nextToken());
		double y1 = Double.parseDouble(st.nextToken());
		st = new StringTokenizer(v3);
		double x2 = Double.parseDouble(st.nextToken());
		double y2 = Double.parseDouble(st.nextToken());
		double dist = Coordinates.calDist(x1, y1, x2, y2);
		long t = TimeFormat.convert(v4) - TimeFormat.convert(v2);
		
		// The velocity should be less than vmax
		boolean result = false;
		double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
		double ve = dist / ((double) t / 1000);
		if (ve <= vmax) {
			result = true;
		}
		
		return result;
	}
	
	private boolean funcLocMixVeloOk(Context ctx1, Context ctx2) { // boolean function 4
		
		String v1 = (String) ctx1.get(Context.FLD_OBJECT);
		String v2 = (String) ctx1.get(Context.FLD_TIMESTAMP);
		String v3 = (String) ctx2.get(Context.FLD_OBJECT);
		String v4 = (String) ctx2.get(Context.FLD_TIMESTAMP);
		if (v1 == null || v2 == null || v3 == null || v4 == null) {
			return false;
		}
		
		StringTokenizer st = new StringTokenizer(v1);
		double x1 = Double.parseDouble(st.nextToken());
		double y1 = Double.parseDouble(st.nextToken());
		st = new StringTokenizer(v3);
		double x2 = Double.parseDouble(st.nextToken());
		double y2 = Double.parseDouble(st.nextToken());
		double dist = Coordinates.calDist(x1, y1, x2, y2);
		long t = TimeFormat.convert(v4) - TimeFormat.convert(v2) - STAY_TIME; // Different here
		
		// The velocity should be between vmin and vmax
		boolean result = false;
		double vmin = (VELOCITY * ((double) t / 1000) - 2 * ERR) / ((double) t / 1000);
		double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
		double ve = dist / ((double) t / 1000);
		if (ve >= vmin && ve <= vmax) {
			result = true;
		}
		
		return result;
	}
	
	private int toBoolean(double d) {
		
		int result = 0;
		if (d != (double) 0) {
			result = 1;
		}
		return result;
	}
	
	private Coordinates toCoordinates(Context ctx) {
		
		StringTokenizer st = new StringTokenizer((String) ctx.get(Context.FLD_OBJECT));
		double x = Double.parseDouble(st.nextToken());
		double y = Double.parseDouble(st.nextToken());
		return new Coordinates(x, y);
	}

	protected void resolve() {
		PositionQueue.add(this.toCoordinates(candidate)); 
		boolean consistent = true;
		for (int i = 0; i < queue.size() && i < 10; i++) {
			Context ctx = (Context) queue.get(i);
			if (filterLocCons2Stay(ctx, candidate) && !funcLocDistOk(ctx, candidate)) {
				// Policy Context[candidate] Constraint[constraint0 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocCons2Walk(ctx, candidate) && !funcLocWalkAdjVeloOk(ctx, candidate)) {
				// Policy Context[candidate] Constraint[constraint1 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Stay(ctx, candidate) && !funcLocDistOk(ctx, candidate)) {
				// Policy Context[candidate] Constraint[constraint2 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Walk(ctx, candidate) && !funcLocWalkSkipVeloOk(ctx, candidate)) {
				// Policy Context[candidate] Constraint[constraint3 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Mix(ctx, candidate) && !funcLocMixVeloOk(ctx, candidate)) {
				// Policy Context[candidate] Constraint[constraint4 on candidate] Solution[discard]
				consistent = false;
				break;
			}
		}
		if (consistent) {
			// Context definition
			queue.add(0, candidate);			
		} else {
			candidate = (Context) queue.get(0);
			activation ++;
		}
	//	System.out.println(candidate.get(Context.FLD_OWNER) + ":\t" + candidate.get(Context.FLD_OBJECT));
	}
	
	public int getChanges(Vector queue){
		int changes = 0;
		Object previous = null;
		Object last = null;
		if(queue.size() == 1){
			changes = 0;
		}
		
		for(int i = 0; i < queue.size() - 1; i ++){
			previous = queue.get(i);
			last = queue.get(i+1);
			if(previous != last){
				changes ++;
			}
		}
		
		return changes;
	}
	
	public int getChanges(double alpha, String[] CoordinateArray){
		Vector contextStream = new Vector();
		//the first element is the test case id rather than context stream fragment
		for(int i = 1; i < CoordinateArray.length; i ++){
			Coordinates ins = new Coordinates(CoordinateArray[i]);
			contextStream.add(ins);
		}
		return this.getChanges(alpha, contextStream);
	}
	
	public int getChanges(double alpha, Vector queue){
		int changes = 0;
		Coordinates last = null;
		Coordinates previous = null;
		
		if(queue.size() == 1){
			changes = 0;
		}
		double[] dists = new double[queue.size()-1];
		for(int i = 0; i < queue.size()-1; i ++){
			previous = (Coordinates)queue.get(i);
			last = (Coordinates)queue.get(i +1);
			dists[i] = Coordinates.calDist(previous, last);			
		}
		
		for(int i = 0; i < queue.size()-1; i++){
			double dist = dists[i];			
			if(dist >= alpha*WALK_DIST){
				changes ++;
			}
		}
		return changes;
	}
	
	public HashMap getStatistics(double[] numArray){
		HashMap statistics = new HashMap();
		double sum = 0.0;
		double mean = 0.0;
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		double std = 0.0;
		
		for(int i = 0; i < numArray.length; i ++){
			double num = numArray[i];
			sum += num;
			
			if(num < min){
				min = num;
			}else if(num > max){
				max = num;
			}
		}
		
		mean = sum / numArray.length;
		double temp = 0.0;
		for(int i = 0; i < numArray.length; i ++){
			temp += (numArray[i] - mean) * (numArray[i]-mean);
		}
		
		std = Math.sqrt(temp/numArray.length);
		statistics.put("min", min);
		statistics.put("mean", mean);
		statistics.put("max", max);
		statistics.put("std", std);
		
		return statistics;
	}
	
	
	/**2010-01-13: load context stream from the file, and
	 * get the context diversity of each test input
	 * @param alpha_min: inclusive
	 * @param alpha_max: exclusive
	 * @param alpha_interval
	 * @param date
	 * @param tc_min: inclusive
	 * @param tc_max: exclusive
	 * @param containHeader
	 */
	public void getDetails_alpha_offline(double alpha_min, double alpha_max, double alpha_interval, 
			String date, int tc_min, int tc_max, boolean containHeader){
		
		String contextStreamFile = "src/ccr/experiment/Context-Intensity_backup" +
		"/TestHarness/" + date +"/ContextStream/ContextStream_"+tc_min + "_"+ tc_max + ".txt";
		
		File tmp = new File(contextStreamFile);
		if(tmp.exists()){
			DecimalFormat formater = new DecimalFormat("0.0000");
			StringBuilder sb = new StringBuilder();
			
			for(double alpha = alpha_min; alpha < alpha_max; alpha = alpha + alpha_interval){

				double[] lengths = new double[tc_max - tc_min];
				double[] CIs = new double[tc_max - tc_min];
				double[] rates = new double[tc_max - tc_min];
				
				sb.append("TestCase").append("\t").append("Length").append("\t").append("CI")
				.append("\t").append("Rate").append("\n");
				
				try {
					BufferedReader br = new BufferedReader(new FileReader(contextStreamFile));
					if(containHeader)
						br.readLine();
					
					String str = null;
					int i = 0;
					while((str = br.readLine())!= null){
						String[] strs = str.split("\t");
						
						double length = strs.length - 1;				
						double CI = getChanges(alpha, strs);
						double rate =Double.parseDouble(formater.format((double)CI/(double)length)); 
						System.out.println(formater.format(alpha) + ":" + strs[0]);											
						lengths[i] = length;
						CIs[i] =  CI;
						rates[i] = rate;
						i ++ ;
						sb.append(strs[0]).append("\t").append(length).append("\t").append(CI).append("\t").
						append(rate).append("\n");
					}
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}									
				
				sb.append("CI statistics").append("\n");			
				HashMap statistics = getStatistics(CIs);
				sb.append("min:").append(statistics.get("min")).append("\n");
				sb.append("mean:").append(statistics.get("mean")).append("\n");
				sb.append("max:").append(statistics.get("max")).append("\n");
				sb.append("std:").append(statistics.get("std")).append("\n");
				
				sb.append("Length statistics").append("\n");			
				statistics = getStatistics(lengths);
				sb.append("min:").append(statistics.get("min")).append("\n");
				sb.append("mean:").append(statistics.get("mean")).append("\n");
				sb.append("max:").append(statistics.get("max")).append("\n");
				sb.append("std:").append(statistics.get("std")).append("\n");
				
				sb.append("Rate statistics").append("\n");			
				statistics = getStatistics(rates);
				sb.append("min:").append(statistics.get("min")).append("\n");
				sb.append("mean:").append(statistics.get("mean")).append("\n");
				sb.append("max:").append(statistics.get("max")).append("\n");
				sb.append("std:").append(statistics.get("std")).append("\n");
				
				String saveFile = "src/ccr/experiment/Context-Intensity_backup" +
						"/TestHarness/"+date+"/TestPool_Alpha/TestPool_"+
						formater.format(alpha)+"_"+ tc_min + "_" + tc_max
						+ ".txt";

				Logger.getInstance().setPath(saveFile, false);
				Logger.getInstance().write(sb.toString());
				Logger.getInstance().close();
				sb.setLength(0);
			}		
		}else{
			System.out.println("Context stream file does not exist:" + tmp.getAbsolutePath());
		}
	}
	
	/**given a alpha range and the increase/decrease steps,
	 * we need to get the details of test cases (length, CI, CI/length) 
	 * @param alpha_min:inclusive
	 * @param alpha_max:exclusive
	 * @param alpha_interval
	 */
	public void getDetails_alpha_online(double alpha_min, double alpha_max, double alpha_interval, String date){
		StringBuilder sb = new StringBuilder();	
		DecimalFormat formater = new DecimalFormat("0.0000");
		
		for(double alpha = alpha_min; alpha < alpha_max; alpha = alpha + alpha_interval){
			double[] lengths = new double[20000];
			double[] CIs = new double[20000];
			double[] rates = new double[20000];
			
			sb.append("TestCase").append("\t").append("Length").append("\t").append("CI")
			.append("\t").append("Rate").append("\n");
			
			for(int i = -10000; i < 10000; i ++){
				String testcase = "" + i;
				TestCFG2_CI ins = new TestCFG2_CI();
				ins.application(testcase);
			
				double length = ins.PositionQueue.size();				
				double CI = ins.getChanges(alpha, ins.PositionQueue);
				double rate =Double.parseDouble(formater.format((double)CI/(double)ins.PositionQueue.size())); 
				System.out.println(formater.format(alpha) + ":" + testcase);			
				sb.append(testcase).append("\t").append(length).append("\t")
				.append(CI).append("\t").append(rate).append("\n");
				
				lengths[i+10000] = length;
				CIs[i+10000] =  CI;
				rates[i+10000] = rate;
			}			
			
			sb.append("Length statistics").append("\n");			
			HashMap statistics = getStatistics(lengths);
			sb.append("min:").append(statistics.get("min")).append("\n");
			sb.append("mean:").append(statistics.get("mean")).append("\n");
			sb.append("max:").append(statistics.get("max")).append("\n");
			sb.append("std:").append(statistics.get("std")).append("\n");
			
			sb.append("CI statistics").append("\n");			
			statistics = getStatistics(CIs);
			sb.append("min:").append(statistics.get("min")).append("\n");
			sb.append("mean:").append(statistics.get("mean")).append("\n");
			sb.append("max:").append(statistics.get("max")).append("\n");
			sb.append("std:").append(statistics.get("std")).append("\n");
			
			sb.append("Rate statistics").append("\n");			
			statistics = getStatistics(rates);
			sb.append("min:").append(statistics.get("min")).append("\n");
			sb.append("mean:").append(statistics.get("mean")).append("\n");
			sb.append("max:").append(statistics.get("max")).append("\n");
			sb.append("std:").append(statistics.get("std")).append("\n");
			
			String saveFile = "src/ccr/experiment/Context-Intensity_backup" +
					"/TestHarness/"+date+"/TestPool_"+formater.format(alpha)+".txt";

			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();
			sb.setLength(0);
		}
	}
	
	/**load context stream of each test case from the file, 
	 * and parse the min, max, mean and std of context diveristy/context length 
	 * of each test case
	 * 
	 * @param alpha_min:inclusive
	 * @param alpha_max: exclusive
	 * @param alpha_interval
	 * @param date
	 * @param min_tc:inclusive
	 * @param max_tc: exclusive
	 * @param containHeader:whether context stream file contains the header or not
	 */
	public void getSummaries_alpha_offline(double alpha_min, double alpha_max, double alpha_interval, 
			String date, int min_tc, int max_tc, boolean containHeader){
		
		String contextStreamFile = "src/ccr/experiment/Context-Intensity_backup" +
		"/TestHarness/" + date +"/ContextStream/ContextStream_"+min_tc + "_"+ max_tc+".txt";
		
		File tmp = new File(contextStreamFile);
		if(tmp.exists()){
			
			DecimalFormat formater = new DecimalFormat("0.0000");
			StringBuilder sb = new StringBuilder();
			
			sb.append("Alpha").append("\t");
			sb.append("CI_Min").append("\t").append("CI_Mean").append("\t").append("CI_Max").append("\t").append("CI_STD").append("\t");
			sb.append("Length_Min").append("\t").append("Length_Mean").append("\t").append("Length_Max").append("\t").append("Length_STD").append("\t");
			sb.append("Rate_Min").append("\t").append("Rate_Mean").append("\t").append("Rate_Max").append("\t").append("Rate_STD").append("\n");
			
			for(double alpha = alpha_min; alpha < alpha_max; alpha = alpha + alpha_interval){
				
				double[] lengths = new double[20000];
				double[] CIs = new double[20000];
				double[] rates = new double[20000];
				
				try {
					BufferedReader br = new BufferedReader(new FileReader(contextStreamFile));
					if(containHeader)
						br.readLine();
					
					String str = null;
					int i = 0;
					while((str = br.readLine())!= null){
						String[] strs = str.split("\t");
						
						double length = strs.length - 1;				
						double CI = getChanges(alpha, strs);
						double rate =Double.parseDouble(formater.format((double)CI/(double)length)); 
						System.out.println(formater.format(alpha) + ":" + strs[0]);											
						lengths[i] = length;
						CIs[i] =  CI;
						rates[i] = rate;
						i ++ ;
					}
					
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				sb.append(formater.format(alpha)).append("\t");						
				
				HashMap statistics = getStatistics(CIs);
				sb.append(formater.format(statistics.get("min"))).append("\t");
				sb.append(formater.format(statistics.get("mean"))).append("\t");
				sb.append(formater.format(statistics.get("max"))).append("\t");
				sb.append(formater.format(statistics.get("std"))).append("\t");
				
				statistics = getStatistics(lengths);
				sb.append(formater.format(statistics.get("min"))).append("\t");
				sb.append(formater.format(statistics.get("mean"))).append("\t");
				sb.append(formater.format(statistics.get("max"))).append("\t");
				sb.append(formater.format(statistics.get("std"))).append("\t");
							
				statistics = getStatistics(rates);
				sb.append(formater.format(statistics.get("min"))).append("\t");
				sb.append(formater.format(statistics.get("mean"))).append("\t");
				sb.append(formater.format(statistics.get("max"))).append("\t");
				sb.append(formater.format(statistics.get("std"))).append("\t");
				sb.append("\n");
			}
			
			String saveFile = "src/ccr/experiment/Context-Intensity_backup" +
			"/TestHarness/" + date +"/TestPool_Alpha/TestPool_summary_"+formater.format(alpha_min)
			+"_"+ formater.format(alpha_max)+".txt";

			Logger.getInstance().setPath(saveFile, false);
			Logger.getInstance().write(sb.toString());
			Logger.getInstance().close();

			
		}else{
			System.out.println("Context stream file does not exist:" + tmp.getAbsolutePath());
		}
	}
	
	/**2010-01-13:Given a alpha range and the interval, we report
	 * the descriptive statistics of test case information(length, CI, CI/length)
	 * @param alpha_min:inclusive
	 * @param alpha_max:exclusive
	 * @param alpha_interval
	 */
	public void getSummaries_alpha_online(double alpha_min, double alpha_max, double alpha_interval, String date){
		StringBuilder sb = new StringBuilder();	
		DecimalFormat formater = new DecimalFormat("0.0000");
		
		sb.append("Alpha").append("\t").
		append("Length_Min").append("\t").append("Length_Mean").append("\t").append("Length_Max").append("\t").append("Length_STD").append("\t").
		append("CI_Min").append("\t").append("CI_Mean").append("\t").append("CI_Max").append("\t").append("CI_STD").append("\t").
		append("Rate_Min").append("\t").append("Rate_Mean").append("\t").append("Rate_Max").append("\t").append("Rate_STD").append("\t").
		append("\n");
		
		for(double alpha = alpha_min; alpha < alpha_max; alpha = alpha - alpha_interval){
			double[] lengths = new double[20000];
			double[] CIs = new double[20000];
			double[] rates = new double[20000];
			
			for(int i = -10000; i < 10000; i ++){
				String testcase = "" + i;
				TestCFG2_CI ins = new TestCFG2_CI();
				ins.application(testcase);
			
				double length = ins.PositionQueue.size();				
				double CI = ins.getChanges(alpha, ins.PositionQueue);
				double rate =Double.parseDouble(formater.format((double)CI/(double)ins.PositionQueue.size())); 
				System.out.println(formater.format(alpha) + ":" + testcase);											
				lengths[i+10000] = length;
				CIs[i+10000] =  CI;
				rates[i+10000] = rate;
			}			
			sb.append(formater.format(alpha)).append("\t");
			
			HashMap statistics = getStatistics(CIs);
			sb.append(formater.format(statistics.get("min"))).append("\t");
			sb.append(formater.format(statistics.get("mean"))).append("\t");
			sb.append(formater.format(statistics.get("max"))).append("\t");
			sb.append(formater.format(statistics.get("std"))).append("\t");
			
			statistics = getStatistics(lengths);
			sb.append(formater.format(statistics.get("min"))).append("\t");
			sb.append(formater.format(statistics.get("mean"))).append("\t");
			sb.append(formater.format(statistics.get("max"))).append("\t");
			sb.append(formater.format(statistics.get("std"))).append("\t");
						
			statistics = getStatistics(rates);
			sb.append(formater.format(statistics.get("min"))).append("\t");
			sb.append(formater.format(statistics.get("mean"))).append("\t");
			sb.append(formater.format(statistics.get("max"))).append("\t");
			sb.append(formater.format(statistics.get("std"))).append("\t");
			sb.append("\n");
			
		}
		String saveFile = "src/ccr/experiment/Context-Intensity_backup" +
		"/TestHarness/" + date +"/TestPool_Alpha/TestPool_summary_"+formater.format(alpha_min)
		+"_"+ formater.format(alpha_max)+".txt";

		Logger.getInstance().setPath(saveFile, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
		sb.setLength(0);
	}
	
	/**save the context stream fragments of test cases within a range 
	 * 
	 * @param tc_min: inclusive
	 * @param tc_max: exclusive
	 * @param date
	 */
	public void getContextStream(int tc_min, int tc_max, String date){
		StringBuilder sb = new StringBuilder();
		
		for(int i = tc_min; i < tc_max; i ++){
			System.out.println("Processing test case:" + i);
			
			//1.record the test case index
			sb.append(i).append("\t");
			
			TestCFG2_CI ins = new TestCFG2_CI();
			ins.application(""+i);			
			
			//2.record the context stream fragment. 
			for(int j = 0; j < ins.PositionQueue.size(); j ++){
				Coordinates location = (Coordinates)ins.PositionQueue.get(j);
				sb.append(location.toString()).append("\t");				
			}
			sb.append("\n");
		}
		
		String filename = "src/ccr/experiment/Context-Intensity_backup" +
		"/TestHarness/" + date +"/ContextStream/ContextStream_"+tc_min
		+"_"+ tc_max +".txt";
		
		Logger.getInstance().setPath(filename, false);
		Logger.getInstance().write(sb.toString());
		Logger.getInstance().close();
	}
	
	/**the default range and interval are 0.0(min and inclusive), 
	 * 1.0 (max and inclusive), 0.1(interval) 
	 * 
	 */
	public void getSummaries_online(String date){
		double alpha_min = 0.0;
		double alpha_max = 1.0;
		double alpha_interval = 0.1;
		getSummaries_alpha_online(alpha_min, alpha_max, alpha_interval, date);
	}
	
	public static void main(String argv[]) {
		String instruction = argv[0];
		TestCFG2_CI ins = new TestCFG2_CI();
		
		if(instruction.equals("getSummary")){
			
			String date = argv[1];
			double alpha_min = Double.parseDouble(argv[2]);//0.440
			double alpha_max = Double.parseDouble(argv[3]);//0.450
			double alpha_interval = Double.parseDouble(argv[4]); //0.001
			int tc_min = -10000;
			int tc_max = 10000;
//			ins.getSummaries_alpha_online(alpha_min, 
//					alpha_max, alpha_interval, date);
			
			boolean containHeader = false;
			ins.getSummaries_alpha_offline(alpha_min, alpha_max, alpha_interval, 
					date, tc_min, tc_max, containHeader);
		}else if(instruction.equals("getDetails_ContextDiversity")){
			String date = argv[1];
			int tc_min = Integer.parseInt(argv[2]);
			int tc_max = Integer.parseInt(argv[3]);
			double alpha_min = 0.43;
			double alpha_max = 0.44;
			double alpha_interval = 0.01;
			if(argv.length == 7){
				alpha_min = Double.parseDouble(argv[4]);
				alpha_max = Double.parseDouble(argv[5]);
				alpha_interval = Double.parseDouble(argv[6]);
			}					
			
//			ins.getDetails_alpha_online(alpha_min,
//					alpha_max, alpha_interval, date);
			
			boolean containHeader = false;
			ins.getDetails_alpha_offline(alpha_min,
					alpha_max, alpha_interval, date, tc_min, tc_max, containHeader);
			
		}else if(instruction.equals("getContextStream")){
			
			String date = argv[1];
			int testcase_min = Integer.parseInt(argv[2]);
			int testcase_max = Integer.parseInt(argv[3]);
			ins.getContextStream(testcase_min, testcase_max, date);			
		}else if(instruction.equals("genAndGetContextDiversity")){
			String date = argv[1];
			int tc_min = Integer.parseInt(argv[2]);
			int tc_max = Integer.parseInt(argv[3]);
			double alpha_min = 0.43;
			double alpha_max = 0.44;
			double alpha_interval = 0.01;
			if(argv.length == 7){
				alpha_min = Double.parseDouble(argv[4]);
				alpha_max = Double.parseDouble(argv[5]);
				alpha_interval = Double.parseDouble(argv[6]);
			}
			
			ins.getContextStream(tc_min, tc_max, date);
			boolean containHeader = false;
			
			ins.getDetails_alpha_offline(alpha_min, alpha_max, alpha_interval, date, tc_min, tc_max, containHeader);
		}
		
	}
}
