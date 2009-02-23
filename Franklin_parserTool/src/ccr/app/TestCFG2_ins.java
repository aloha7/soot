package ccr.app;

import java.util.*;

public class TestCFG2_ins extends Application {
	
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
		
		// Ordinary Variable [moved, reliable, error, c, stay, timestamp, counter, t]
		
		// Context Variable [candidate]
		
		// Assignment [=]
		
		int seed = Integer.parseInt(testcase);
		queue = new Vector();
		Coordinates location = null;
		Random rand = new Random(seed);
		CCRScenarios scenarios = new CCRScenarios(seed);//this input doesn't have any explicit meanings
		long t;
		Coordinates actLoc;
		Coordinates estLoc;
		Coordinates lastLoc;
		double dist;
		double displace;
		double error;
		
		record("c0");
		// ENTRY
		record("c1");
		int c = 0 ;
		record("c2");
		int bPos = - 1 ;
		record("c3");
		int cPos = - 1 ;
		record("c4");
		int stay = 0 ;
		record("c5");
		int lastPos = - 1 ;
		record("c6");
		timestamp = System . currentTimeMillis ( ) ;
		record("c7");
		counter = 0 ;
		record("c8");
		int moved = 0 ;
		record("c9");
		int reliable = 0 ;
		record("c10");
		Coordinates lastLocation = new Coordinates ( 0 , 0 ) ;
		record("c11");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c12");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c13");
		c = c + stay ;
		record("c14");
		bPos = cPos ;
		record("c15");
		stay = stay - 1 ;
		record("c16");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c17");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c18");
		curEstX = estLoc . x ;
		record("c19");
		curEstY = estLoc . y ;
		record("c20");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c21");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c22");
		t = 0 ;
		record("c23");
		timestamp = timestamp + t ;
		record("c24");
		lastPos = cPos ;
		updateIndex = "c25";
		candidate = generateCtx ( ) ;
		record("c26");
		resolve ( ) ;
		record("c27");
		location = toCoordinates ( candidate ) ;
		record("c28");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c29");
		moved = moved + toBoolean ( displace ) ;
		record("c30");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c31");
		lastLocation = location ;
		record("c32");
		counter = counter + 1 ;
		record("c33");
		while ( stay > 0 ) {
			record("c34");
			stay = stay - 1 ;
			record("c35");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c36");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c37");
			curEstX = estLoc . x ;
			record("c38");
			curEstY = estLoc . y ;
			record("c39");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c40");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c41");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c42");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c43");
			t = STAY_TIME ;
			record("c44");
			timestamp = timestamp + t ;
			updateIndex = "c45";
			candidate = generateCtx ( ) ;
			record("c46");
			resolve ( ) ;
			record("c47");
			location = toCoordinates ( candidate ) ;
			record("c48");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c49");
			moved = moved + toBoolean ( displace ) ;
			record("c50");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c51");
			if ( error <= ERR ) {
				record("c52");
				reliable = reliable + 1 ;
			}
			record("c53");
			lastLocation = location ;
			record("c54");
			counter = counter + 1 ;
		}
		record("c55");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c56");
		while ( cPos == - 1 || cPos == bPos || Coordinates . calDist ( scenarios . getActLoc ( sid , bPos ) , scenarios . getActLoc ( sid , cPos ) ) < WALK_DIST ) {
			record("c57");
			cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		}
		record("c58");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c59");
		c = c + stay ;
		record("c60");
		bPos = cPos ;
		record("c61");
		stay = stay - 1 ;
		record("c62");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c63");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c64");
		curEstX = estLoc . x ;
		record("c65");
		curEstY = estLoc . y ;
		record("c66");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c67");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c68");
		lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
		record("c69");
		dist = Coordinates . calDist ( lastLoc , actLoc ) ;
		record("c70");
		t = ( long ) ( dist / VELOCITY * 1000 ) ;
		record("c71");
		timestamp = timestamp + t ;
		record("c72");
		lastPos = cPos ;
		updateIndex = "c73";
		candidate = generateCtx ( ) ;
		record("c74");
		resolve ( ) ;
		record("c75");
		location = toCoordinates ( candidate ) ;
		record("c76");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c77");
		moved = moved + toBoolean ( displace ) ;
		record("c78");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c79");
		lastLocation = location ;
		record("c80");
		counter = counter + 1 ;
		record("c81");
		while ( stay > 0 ) {
			record("c82");
			stay = stay - 1 ;
			record("c83");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c84");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c85");
			curEstX = estLoc . x ;
			record("c86");
			curEstY = estLoc . y ;
			record("c87");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c88");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c89");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c90");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c91");
			t = STAY_TIME ;
			record("c92");
			timestamp = timestamp + t ;
			record("c93");
			lastPos = cPos ;
			updateIndex = "c94";
			candidate = generateCtx ( ) ;
			record("c95");
			resolve ( ) ;
			record("c96");
			location = toCoordinates ( candidate ) ;
			record("c97");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c98");
			moved = moved + toBoolean ( displace ) ;
			record("c99");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c100");
			if ( error <= ERR ) {
				record("c101");
				reliable = reliable + 1 ;
			}
			record("c102");
			lastLocation = location ;
			record("c103");
			counter = counter + 1 ;
		}
		record("c104");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c105");
		while ( cPos == - 1 || cPos == bPos || Coordinates . calDist ( scenarios . getActLoc ( sid , bPos ) , scenarios . getActLoc ( sid , cPos ) ) < WALK_DIST ) {
			record("c106");
			cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		}
		record("c107");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c108");
		c = c + stay ;
		record("c109");
		bPos = cPos ;
		record("c110");
		stay = stay - 1 ;
		record("c111");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c112");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c113");
		curEstX = estLoc . x ;
		record("c114");
		curEstY = estLoc . y ;
		record("c115");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c116");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c117");
		lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
		record("c118");
		dist = Coordinates . calDist ( lastLoc , actLoc ) ;
		record("c119");
		t = ( long ) ( dist / VELOCITY * 1000 ) ;
		record("c120");
		timestamp = timestamp + t ;
		record("c121");
		lastPos = cPos ;
		updateIndex = "c122";
		candidate = generateCtx ( ) ;
		record("c123");
		resolve ( ) ;
		record("c124");
		location = toCoordinates ( candidate ) ;
		record("c125");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c126");
		moved = moved + toBoolean ( displace ) ;
		record("c127");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c128");
		lastLocation = location ;
		record("c129");
		counter = counter + 1 ;
		record("c130");
		while ( stay > 0 ) {
			record("c131");
			stay = stay - 1 ;
			record("c132");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c133");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c134");
			curEstX = estLoc . x ;
			record("c135");
			curEstY = estLoc . y ;
			record("c136");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c137");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c138");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c139");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c140");
			t = STAY_TIME ;
			record("c141");
			timestamp = timestamp + t ;
			record("c142");
			lastPos = cPos ;
			updateIndex = "c143";
			candidate = generateCtx ( ) ;
			record("c144");
			resolve ( ) ;
			record("c145");
			location = toCoordinates ( candidate ) ;
			record("c146");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c147");
			moved = moved + toBoolean ( displace ) ;
			record("c148");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c149");
			if ( error <= ERR ) {
				record("c150");
				reliable = reliable + 1 ;
			}
			record("c151");
			lastLocation = location ;
			record("c152");
			counter = counter + 1 ;
		}
		record("c153");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c154");
		while ( cPos == - 1 || cPos == bPos || Coordinates . calDist ( scenarios . getActLoc ( sid , bPos ) , scenarios . getActLoc ( sid , cPos ) ) < WALK_DIST ) {
			record("c155");
			cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		}
		record("c156");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c157");
		c = c + stay ;
		record("c158");
		bPos = cPos ;
		record("c159");
		stay = stay - 1 ;
		record("c160");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c161");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c162");
		curEstX = estLoc . x ;
		record("c163");
		curEstY = estLoc . y ;
		record("c164");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c165");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c166");
		lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
		record("c167");
		dist = Coordinates . calDist ( lastLoc , actLoc ) ;
		record("c168");
		t = ( long ) ( dist / VELOCITY * 1000 ) ;
		record("c169");
		timestamp = timestamp + t ;
		record("c170");
		lastPos = cPos ;
		updateIndex = "c171";
		candidate = generateCtx ( ) ;
		record("c172");
		resolve ( ) ;
		record("c173");
		location = toCoordinates ( candidate ) ;
		record("c174");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c175");
		moved = moved + toBoolean ( displace ) ;
		record("c176");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c177");
		lastLocation = location ;
		record("c178");
		counter = counter + 1 ;
		record("c179");
		while ( stay > 0 ) {
			record("c180");
			stay = stay - 1 ;
			record("c181");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c182");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c183");
			curEstX = estLoc . x ;
			record("c184");
			curEstY = estLoc . y ;
			record("c185");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c186");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c187");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c188");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c189");
			t = STAY_TIME ;
			record("c190");
			timestamp = timestamp + t ;
			record("c191");
			lastPos = cPos ;
			updateIndex = "c192";
			candidate = generateCtx ( ) ;
			record("c193");
			resolve ( ) ;
			record("c194");
			location = toCoordinates ( candidate ) ;
			record("c195");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c196");
			moved = moved + toBoolean ( displace ) ;
			record("c197");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c198");
			if ( error <= ERR ) {
				record("c199");
				reliable = reliable + 1 ;
			}
			record("c200");
			lastLocation = location ;
			record("c201");
			counter = counter + 1 ;
		}
		record("c202");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c203");
		while ( cPos == - 1 || cPos == bPos || Coordinates . calDist ( scenarios . getActLoc ( sid , bPos ) , scenarios . getActLoc ( sid , cPos ) ) < WALK_DIST ) {
			record("c204");
			cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		}
		record("c205");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c206");
		c = c + stay ;
		record("c207");
		bPos = cPos ;
		record("c208");
		stay = stay - 1 ;
		record("c209");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c210");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c211");
		curEstX = estLoc . x ;
		record("c212");
		curEstY = estLoc . y ;
		record("c213");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c214");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c215");
		lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
		record("c216");
		dist = Coordinates . calDist ( lastLoc , actLoc ) ;
		record("c217");
		t = ( long ) ( dist / VELOCITY * 1000 ) ;
		record("c218");
		timestamp = timestamp + t ;
		record("c219");
		lastPos = cPos ;
		updateIndex = "c220";
		candidate = generateCtx ( ) ;
		record("c221");
		resolve ( ) ;
		record("c222");
		location = toCoordinates ( candidate ) ;
		record("c223");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c224");
		moved = moved + toBoolean ( displace ) ;
		record("c225");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c226");
		lastLocation = location ;
		record("c227");
		counter = counter + 1 ;
		record("c228");
		while ( stay > 0 ) {
			record("c229");
			stay = stay - 1 ;
			record("c230");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c231");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c232");
			curEstX = estLoc . x ;
			record("c233");
			curEstY = estLoc . y ;
			record("c234");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c235");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c236");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c237");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c238");
			t = STAY_TIME ;
			record("c239");
			timestamp = timestamp + t ;
			record("c240");
			lastPos = cPos ;
			updateIndex = "c241";
			candidate = generateCtx ( ) ;
			record("c242");
			resolve ( ) ;
			record("c243");
			location = toCoordinates ( candidate ) ;
			record("c244");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c245");
			moved = moved + toBoolean ( displace ) ;
			record("c246");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c247");
			if ( error <= ERR ) {
				record("c248");
				reliable = reliable + 1 ;
			}
			record("c249");
			lastLocation = location ;
			record("c250");
			counter = counter + 1 ;
		}
		record("c251");
		cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		record("c252");
		while ( cPos == - 1 || cPos == bPos || Coordinates . calDist ( scenarios . getActLoc ( sid , bPos ) , scenarios . getActLoc ( sid , cPos ) ) < WALK_DIST ) {
			record("c253");
			cPos = rand . nextInt ( CCRScenarios . POS_NUM ) ;
		}
		record("c254");
		stay = rand . nextInt ( MAX_STAY ) + 1 ;
		record("c255");
		c = c + stay ;
		record("c256");
		bPos = cPos ;
		record("c257");
		stay = stay - 1 ;
		record("c258");
		actLoc = scenarios . getActLoc ( sid , cPos ) ;
		record("c259");
		estLoc = scenarios . getEstLoc ( sid , cPos ) ;
		record("c260");
		curEstX = estLoc . x ;
		record("c261");
		curEstY = estLoc . y ;
		record("c262");
		curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c263");
		curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
		record("c264");
		lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
		record("c265");
		dist = Coordinates . calDist ( lastLoc , actLoc ) ;
		record("c266");
		t = ( long ) ( dist / VELOCITY * 1000 ) ;
		record("c267");
		timestamp = timestamp + t ;
		record("c268");
		lastPos = cPos ;
		updateIndex = "c269";
		candidate = generateCtx ( ) ;
		record("c270");
		resolve ( ) ;
		record("c271");
		location = toCoordinates ( candidate ) ;
		record("c272");
		displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
		record("c273");
		moved = moved + toBoolean ( displace ) ;
		record("c274");
		error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
		record("c275");
		lastLocation = location ;
		record("c276");
		counter = counter + 1 ;
		record("c277");
		while ( stay > 0 ) {
			record("c278");
			stay = stay - 1 ;
			record("c279");
			actLoc = scenarios . getActLoc ( sid , cPos ) ;
			record("c280");
			estLoc = scenarios . getEstLoc ( sid , cPos ) ;
			record("c281");
			curEstX = estLoc . x ;
			record("c282");
			curEstY = estLoc . y ;
			record("c283");
			curEstX = curEstX + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c284");
			curEstY = curEstY + ( ( double ) 2 * rand . nextDouble ( ) - ( double ) 1 ) * NOISE ;
			record("c285");
			lastLoc = scenarios . getActLoc ( sid , lastPos ) ;
			record("c286");
			dist = Coordinates . calDist ( lastLoc , actLoc ) ;
			record("c287");
			t = STAY_TIME ;
			record("c288");
			timestamp = timestamp + t ;
			record("c289");
			lastPos = cPos ;
			updateIndex = "c290";
			candidate = generateCtx ( ) ;
			record("c291");
			resolve ( ) ;
			record("c292");
			location = toCoordinates ( candidate ) ;
			record("c293");
			displace = Math . sqrt ( ( location . x - lastLocation . x ) * ( location . x - lastLocation . x ) + ( location . y - lastLocation . y ) * ( location . y - lastLocation . y ) ) ;
			record("c294");
			moved = moved + toBoolean ( displace ) ;
			record("c295");
			error = Math . sqrt ( ( actLoc . x - location . x ) * ( actLoc . x - location . x ) + ( actLoc . y - location . y ) * ( actLoc . y - location . y ) ) ;
			record("c296");
			if ( error <= ERR ) {
				record("c297");
				reliable = reliable + 1 ;
			}
			record("c298");
			lastLocation = location ;
			record("c299");
			counter = counter + 1 ;
		}
		record("c300");
		ApplicationResult result = new ApplicationResult ( moved , reliable ) ;
		record("c301");
		// EXIT
		
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
		if (d != (double) 0) { //need to change this line d >= ERR
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
		
		boolean consistent = true;//the context is inconsistent when it is inconsistent with any one element in the queue;
		for (int i = 0; i < queue.size() && i < 10; i++) {
			Context ctx = (Context) queue.get(i);
			if (filterLocCons2Stay(ctx, candidate) && !funcLocDistOk(ctx, candidate)) {
				record(updateIndex + ":P0");
				// Policy Context[candidate] Constraint[constraint0 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocCons2Walk(ctx, candidate) && !funcLocWalkAdjVeloOk(ctx, candidate)) {
				record(updateIndex + ":P1");
				// Policy Context[candidate] Constraint[constraint1 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Stay(ctx, candidate) && !funcLocDistOk(ctx, candidate)) {
				record(updateIndex + ":P2");
				// Policy Context[candidate] Constraint[constraint2 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Walk(ctx, candidate) && !funcLocWalkSkipVeloOk(ctx, candidate)) {
				record(updateIndex + ":P3");
				// Policy Context[candidate] Constraint[constraint3 on candidate] Solution[discard]
				consistent = false;
				break;
			}
			if (filterLocSkip1Mix(ctx, candidate) && !funcLocMixVeloOk(ctx, candidate)) {
				record(updateIndex + ":P4");
				// Policy Context[candidate] Constraint[constraint4 on candidate] Solution[discard]
				consistent = false;
				break;
			}
		}
		if (consistent) {
			record(updateIndex);
			// Context definition
			queue.add(0, candidate);
		} else {
			candidate = (Context) queue.get(0); //basically speaking, get the head of the queue if it is not consistent
		}
	//	System.out.println(candidate.get(Context.FLD_OWNER) + ":\t" + candidate.get(Context.FLD_OBJECT));
	}
	
	public static void main(String argv[]) {
		
		String testcase = "10"; 
		System.out.println("result = " + (new TestCFG2()).application(testcase));
	//	System.out.println((new TestCFG2()).application(testcase).equals((new TestCFG2()).application(testcase)));
	}

}
