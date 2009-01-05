package ccr.app.version01;

import ccr.app.*;

import java.util.*;

public class TestCFG2_14 extends Application {
// Fault notes: 
	
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
	
	private Vector queue;
	private long timestamp;
	private Context candidate;

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
		int lastPos = -1;
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
		location = toCoordinates(candidate);
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
		curEstY = curEstY /*+*/- ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;  // Seeded fault // [- NOISE, + NOISE)
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
		}
	//	System.out.println(candidate.get(Context.FLD_OWNER) + ":\t" + candidate.get(Context.FLD_OBJECT));
	}
	
	public static void main(String argv[]) {
		
		String testcase = "10"; 
		System.out.println("result = " + (new TestCFG2()).application(testcase));
	//	System.out.println((new TestCFG2()).application(testcase).equals((new TestCFG2()).application(testcase)));
	}

}
