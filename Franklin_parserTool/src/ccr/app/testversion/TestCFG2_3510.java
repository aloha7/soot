// This is mutant program.
// Author : ysma

package ccr.app.testversion;
import ccr.app.*;

import java.util.*;


public class TestCFG2_3510 extends ccr.app.Application
{

    private final long STAY_TIME = 200;

    private final double WALK_DIST = 0.9;

    private final double VELOCITY = 2.0;

    private final double NOISE = 0.5;

    private final double ERR = 0.5;

    private final int MAX_STAY = 5;

    private final int sid = 0;

    private int counter = 0;

    private double curEstX = 0.0;

    private double curEstY = 0.0;

    private java.util.Vector queue;

    private long timestamp;

    private ccr.app.Context candidate;

    public java.lang.Object application( java.lang.String testcase )
    {
        int seed = Integer.parseInt( testcase );
        queue = new java.util.Vector();
        ccr.app.Coordinates location = null;
        java.util.Random rand = new java.util.Random( seed );
        ccr.app.CCRScenarios scenarios = new ccr.app.CCRScenarios( seed );
        long t;
        ccr.app.Coordinates actLoc;
        ccr.app.Coordinates estLoc;
        ccr.app.Coordinates lastLoc;
        double dist;
        double displace;
        double error;
        int c = 0;
        int bPos = -1;
        int cPos = -1;
        int stay = 0;
        int lastPos = -1;
        timestamp = System.currentTimeMillis();
        counter = 0;
        int moved = 0;
        int reliable = 0;
        ccr.app.Coordinates lastLocation = new ccr.app.Coordinates( 0, 0 );
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        t = 0;
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        while (cPos == -1 || cPos == bPos || Coordinates.calDist( scenarios.getActLoc( sid, bPos ), scenarios.getActLoc( sid, cPos ) ) < WALK_DIST) {
            cPos = rand.nextInt( CCRScenarios.POS_NUM );
        }
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 - rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        lastLoc = scenarios.getActLoc( sid, lastPos );
        dist = Coordinates.calDist( lastLoc, actLoc );
        t = (long) (dist / VELOCITY * 1000);
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            lastPos = cPos;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        while (cPos == -1 || cPos == bPos || Coordinates.calDist( scenarios.getActLoc( sid, bPos ), scenarios.getActLoc( sid, cPos ) ) < WALK_DIST) {
            cPos = rand.nextInt( CCRScenarios.POS_NUM );
        }
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        lastLoc = scenarios.getActLoc( sid, lastPos );
        dist = Coordinates.calDist( lastLoc, actLoc );
        t = (long) (dist / VELOCITY * 1000);
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            lastPos = cPos;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        while (cPos == -1 || cPos == bPos || Coordinates.calDist( scenarios.getActLoc( sid, bPos ), scenarios.getActLoc( sid, cPos ) ) < WALK_DIST) {
            cPos = rand.nextInt( CCRScenarios.POS_NUM );
        }
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        lastLoc = scenarios.getActLoc( sid, lastPos );
        dist = Coordinates.calDist( lastLoc, actLoc );
        t = (long) (dist / VELOCITY * 1000);
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            lastPos = cPos;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        while (cPos == -1 || cPos == bPos || Coordinates.calDist( scenarios.getActLoc( sid, bPos ), scenarios.getActLoc( sid, cPos ) ) < WALK_DIST) {
            cPos = rand.nextInt( CCRScenarios.POS_NUM );
        }
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        lastLoc = scenarios.getActLoc( sid, lastPos );
        dist = Coordinates.calDist( lastLoc, actLoc );
        t = (long) (dist / VELOCITY * 1000);
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            lastPos = cPos;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        cPos = rand.nextInt( CCRScenarios.POS_NUM );
        while (cPos == -1 || cPos == bPos || Coordinates.calDist( scenarios.getActLoc( sid, bPos ), scenarios.getActLoc( sid, cPos ) ) < WALK_DIST) {
            cPos = rand.nextInt( CCRScenarios.POS_NUM );
        }
        stay = rand.nextInt( MAX_STAY ) + 1;
        c = c + stay;
        bPos = cPos;
        stay = stay - 1;
        actLoc = scenarios.getActLoc( sid, cPos );
        estLoc = scenarios.getEstLoc( sid, cPos );
        curEstX = estLoc.x;
        curEstY = estLoc.y;
        curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
        lastLoc = scenarios.getActLoc( sid, lastPos );
        dist = Coordinates.calDist( lastLoc, actLoc );
        t = (long) (dist / VELOCITY * 1000);
        timestamp = timestamp + t;
        lastPos = cPos;
        candidate = generateCtx();
        resolve();
        location = toCoordinates( candidate );
        displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
        moved = moved + toBoolean( displace );
        error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
        lastLocation = location;
        counter = counter + 1;
        while (stay > 0) {
            stay = stay - 1;
            actLoc = scenarios.getActLoc( sid, cPos );
            estLoc = scenarios.getEstLoc( sid, cPos );
            curEstX = estLoc.x;
            curEstY = estLoc.y;
            curEstX = curEstX + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            curEstY = curEstY + ((double) 2 * rand.nextDouble() - (double) 1) * NOISE;
            lastLoc = scenarios.getActLoc( sid, lastPos );
            dist = Coordinates.calDist( lastLoc, actLoc );
            t = STAY_TIME;
            timestamp = timestamp + t;
            lastPos = cPos;
            candidate = generateCtx();
            resolve();
            location = toCoordinates( candidate );
            displace = Math.sqrt( (location.x - lastLocation.x) * (location.x - lastLocation.x) + (location.y - lastLocation.y) * (location.y - lastLocation.y) );
            moved = moved + toBoolean( displace );
            error = Math.sqrt( (actLoc.x - location.x) * (actLoc.x - location.x) + (actLoc.y - location.y) * (actLoc.y - location.y) );
            if (error <= ERR) {
                reliable = reliable + 1;
            }
            lastLocation = location;
            counter = counter + 1;
        }
        ccr.app.ApplicationResult result = new ccr.app.ApplicationResult( moved, reliable );
        return result;
    }

    private ccr.app.Context generateCtx()
    {
        ccr.app.Context ctx = new ccr.app.Context();
        ctx.put( Context.FLD_CATEGORY, getCategory() );
        ctx.put( Context.FLD_SUBJECT, getSubject() );
        ctx.put( Context.FLD_PREDICATE, getPredicate() );
        ctx.put( Context.FLD_OBJECT, getObject() );
        ctx.put( Context.FLD_START_FROM, getStartFrom() );
        ctx.put( Context.FLD_END_AT, getEndAt() );
        ctx.put( Context.FLD_SITE, getSite() );
        ctx.put( Context.FLD_OWNER, getOwner() );
        ctx.put( Context.FLD_TIMESTAMP, TimeFormat.convert( timestamp ) );
        return ctx;
    }

    private java.lang.String getCategory()
    {
        return "location";
    }

    private java.lang.String getSubject()
    {
        return "Jialin";
    }

    private java.lang.String getPredicate()
    {
        return "estimated at";
    }

    private java.lang.String getObject()
    {
        return "" + curEstX + " " + curEstY;
    }

    private java.lang.String getStartFrom()
    {
        return TimeFormat.convert( System.currentTimeMillis() );
    }

    private java.lang.String getEndAt()
    {
        return TimeFormat.convert( System.currentTimeMillis() );
    }

    private java.lang.String getSite()
    {
        return "HKUST";
    }

    private java.lang.String getOwner()
    {
        return "" + counter;
    }

    private boolean filterLocCons2Stay( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        int c1 = Integer.parseInt( (java.lang.String) ctx1.get( Context.FLD_OWNER ) );
        int c2 = Integer.parseInt( (java.lang.String) ctx2.get( Context.FLD_OWNER ) );
        long t1 = TimeFormat.convert( (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP ) );
        long t2 = TimeFormat.convert( (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP ) );
        if (c1 + 1 == c2 && t2 - t1 <= STAY_TIME + 100) {
            return true;
        } else {
            return false;
        }
    }

    private boolean filterLocCons2Walk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        int c1 = Integer.parseInt( (java.lang.String) ctx1.get( Context.FLD_OWNER ) );
        int c2 = Integer.parseInt( (java.lang.String) ctx2.get( Context.FLD_OWNER ) );
        long t1 = TimeFormat.convert( (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP ) );
        long t2 = TimeFormat.convert( (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP ) );
        if (c1 + 1 == c2 && t2 - t1 > STAY_TIME + 100) {
            return true;
        } else {
            return false;
        }
    }

    private boolean filterLocSkip1Stay( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        int c1 = Integer.parseInt( (java.lang.String) ctx1.get( Context.FLD_OWNER ) );
        int c2 = Integer.parseInt( (java.lang.String) ctx2.get( Context.FLD_OWNER ) );
        long t1 = TimeFormat.convert( (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP ) );
        long t2 = TimeFormat.convert( (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP ) );
        if (c1 + 2 == c2 && t2 - t1 <= 2 * (STAY_TIME + 100)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean filterLocSkip1Walk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        int c1 = Integer.parseInt( (java.lang.String) ctx1.get( Context.FLD_OWNER ) );
        int c2 = Integer.parseInt( (java.lang.String) ctx2.get( Context.FLD_OWNER ) );
        long t1 = TimeFormat.convert( (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP ) );
        long t2 = TimeFormat.convert( (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP ) );
        if (c1 + 2 == c2 && t2 - t1 >= 2 * (long) (WALK_DIST / VELOCITY * 1000)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean filterLocSkip1Mix( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        int c1 = Integer.parseInt( (java.lang.String) ctx1.get( Context.FLD_OWNER ) );
        int c2 = Integer.parseInt( (java.lang.String) ctx2.get( Context.FLD_OWNER ) );
        long t1 = TimeFormat.convert( (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP ) );
        long t2 = TimeFormat.convert( (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP ) );
        if (c1 + 2 == c2 && t2 - t1 > 2 * (STAY_TIME + 100) && t2 - t1 < 2 * (long) (WALK_DIST / VELOCITY * 1000)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean funcLocDistOk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        java.lang.String v1 = (java.lang.String) ctx1.get( Context.FLD_OBJECT );
        java.lang.String v2 = (java.lang.String) ctx2.get( Context.FLD_OBJECT );
        if (v1 == null || v2 == null) {
            return false;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer( v1 );
        double x1 = Double.parseDouble( st.nextToken() );
        double y1 = Double.parseDouble( st.nextToken() );
        st = new java.util.StringTokenizer( v2 );
        double x2 = Double.parseDouble( st.nextToken() );
        double y2 = Double.parseDouble( st.nextToken() );
        double dist = Coordinates.calDist( x1, y1, x2, y2 );
        boolean result = false;
        if (dist <= 2 * ERR) {
            result = true;
        }
        return result;
    }

    private boolean funcLocWalkAdjVeloOk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        java.lang.String v1 = (java.lang.String) ctx1.get( Context.FLD_OBJECT );
        java.lang.String v2 = (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP );
        java.lang.String v3 = (java.lang.String) ctx2.get( Context.FLD_OBJECT );
        java.lang.String v4 = (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP );
        if (v1 == null || v2 == null || v3 == null || v4 == null) {
            return false;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer( v1 );
        double x1 = Double.parseDouble( st.nextToken() );
        double y1 = Double.parseDouble( st.nextToken() );
        st = new java.util.StringTokenizer( v3 );
        double x2 = Double.parseDouble( st.nextToken() );
        double y2 = Double.parseDouble( st.nextToken() );
        double dist = Coordinates.calDist( x1, y1, x2, y2 );
        long t = TimeFormat.convert( v4 ) - TimeFormat.convert( v2 );
        boolean result = false;
        double vmin = (VELOCITY * ((double) t / 1000) - 2 * ERR) / ((double) t / 1000);
        double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
        double ve = dist / ((double) t / 1000);
        if (ve >= vmin && ve <= vmax) {
            result = true;
        }
        return result;
    }

    private boolean funcLocWalkSkipVeloOk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        java.lang.String v1 = (java.lang.String) ctx1.get( Context.FLD_OBJECT );
        java.lang.String v2 = (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP );
        java.lang.String v3 = (java.lang.String) ctx2.get( Context.FLD_OBJECT );
        java.lang.String v4 = (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP );
        if (v1 == null || v2 == null || v3 == null || v4 == null) {
            return false;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer( v1 );
        double x1 = Double.parseDouble( st.nextToken() );
        double y1 = Double.parseDouble( st.nextToken() );
        st = new java.util.StringTokenizer( v3 );
        double x2 = Double.parseDouble( st.nextToken() );
        double y2 = Double.parseDouble( st.nextToken() );
        double dist = Coordinates.calDist( x1, y1, x2, y2 );
        long t = TimeFormat.convert( v4 ) - TimeFormat.convert( v2 );
        boolean result = false;
        double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
        double ve = dist / ((double) t / 1000);
        if (ve <= vmax) {
            result = true;
        }
        return result;
    }

    private boolean funcLocMixVeloOk( ccr.app.Context ctx1, ccr.app.Context ctx2 )
    {
        java.lang.String v1 = (java.lang.String) ctx1.get( Context.FLD_OBJECT );
        java.lang.String v2 = (java.lang.String) ctx1.get( Context.FLD_TIMESTAMP );
        java.lang.String v3 = (java.lang.String) ctx2.get( Context.FLD_OBJECT );
        java.lang.String v4 = (java.lang.String) ctx2.get( Context.FLD_TIMESTAMP );
        if (v1 == null || v2 == null || v3 == null || v4 == null) {
            return false;
        }
        java.util.StringTokenizer st = new java.util.StringTokenizer( v1 );
        double x1 = Double.parseDouble( st.nextToken() );
        double y1 = Double.parseDouble( st.nextToken() );
        st = new java.util.StringTokenizer( v3 );
        double x2 = Double.parseDouble( st.nextToken() );
        double y2 = Double.parseDouble( st.nextToken() );
        double dist = Coordinates.calDist( x1, y1, x2, y2 );
        long t = TimeFormat.convert( v4 ) - TimeFormat.convert( v2 ) - STAY_TIME;
        boolean result = false;
        double vmin = (VELOCITY * ((double) t / 1000) - 2 * ERR) / ((double) t / 1000);
        double vmax = (VELOCITY * ((double) t / 1000) + 2 * ERR) / ((double) t / 1000);
        double ve = dist / ((double) t / 1000);
        if (ve >= vmin && ve <= vmax) {
            result = true;
        }
        return result;
    }

    private int toBoolean( double d )
    {
        int result = 0;
        if (d != (double) 0) {
            result = 1;
        }
        return result;
    }

    private ccr.app.Coordinates toCoordinates( ccr.app.Context ctx )
    {
        java.util.StringTokenizer st = new java.util.StringTokenizer( (java.lang.String) ctx.get( Context.FLD_OBJECT ) );
        double x = Double.parseDouble( st.nextToken() );
        double y = Double.parseDouble( st.nextToken() );
        return new ccr.app.Coordinates( x, y );
    }

    protected void resolve()
    {
        boolean consistent = true;
        for (int i = 0; i < queue.size() && i < 10; i++) {
            ccr.app.Context ctx = (ccr.app.Context) queue.get( i );
            if (filterLocCons2Stay( ctx, candidate ) && !funcLocDistOk( ctx, candidate )) {
                consistent = false;
                break;
            }
            if (filterLocCons2Walk( ctx, candidate ) && !funcLocWalkAdjVeloOk( ctx, candidate )) {
                consistent = false;
                break;
            }
            if (filterLocSkip1Stay( ctx, candidate ) && !funcLocDistOk( ctx, candidate )) {
                consistent = false;
                break;
            }
            if (filterLocSkip1Walk( ctx, candidate ) && !funcLocWalkSkipVeloOk( ctx, candidate )) {
                consistent = false;
                break;
            }
            if (filterLocSkip1Mix( ctx, candidate ) && !funcLocMixVeloOk( ctx, candidate )) {
                consistent = false;
                break;
            }
        }
        if (consistent) {
            queue.add( 0, candidate );
        } else {
            candidate = (ccr.app.Context) queue.get( 0 );
        }
    }

    public static void main( java.lang.String[] argv )
    {
        java.lang.String testcase = "10";
        System.out.println( "result = " + (new ccr.app.TestCFG2()).application( testcase ) );
    }

}
