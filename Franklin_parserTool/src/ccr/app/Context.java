package ccr.app;

import java.util.*;

public class Context extends HashMap {

    // Each context is a tuple space which contains many key-value tuples

    public static final String FLD_ID = "id";  // Automatically decided

    public static final String FLD_CATEGORY = "category";

    public static final String FLD_SUBJECT = "subject";

    public static final String FLD_PREDICATE = "predicate";

    public static final String FLD_OBJECT = "object";

    public static final String FLD_START_FROM = "start_from";

    public static final String FLD_END_AT = "end_at";

    public static final String FLD_SITE = "site";

    public static final String FLD_TIMESTAMP = "timestamp";  // Automatically decided

    public static final String FLD_OWNER = "owner";

    public static final String FLD_CONSISTENCY = "consistency";  // Automatically decided

    public static final String VAL_NA = "N/A";

    public static final String VAL_DUMMY = "dummy";

    public static final String VAL_UNKNOWN = "unknown";

    public static final String VAL_CONSISTENT = "consistent";

    public static final String VAL_INCONSISTENT = "inconsistent";

    protected static final String SEPARATOR = "~";

    public Context() {

        // Set default values
        put(FLD_ID, VAL_NA);
        put(FLD_CATEGORY, VAL_NA);
        put(FLD_SUBJECT, VAL_NA);
        put(FLD_PREDICATE, VAL_NA);
        put(FLD_OBJECT, VAL_NA);
        put(FLD_START_FROM, VAL_NA);
        put(FLD_END_AT, VAL_NA);
        put(FLD_SITE, VAL_NA);
        put(FLD_TIMESTAMP, VAL_NA);
        put(FLD_OWNER, VAL_NA);
        put(FLD_CONSISTENCY, VAL_NA);
    }

    // For convenience

    public String getId() {

        return (String) get(FLD_ID);
    }

    private String prepString(String field) {

        String result = "";

        String value = (String) get(field);
        if (!value.equals(VAL_NA)) {
            result = field + "/\"" + value + "\"";
        } else {
            result = field + "/???";
        }

        return result;
    }

    public String toString() {

        String result = "(" + prepString(FLD_ID);
        result += ", " + prepString(FLD_CATEGORY);
        result += ", " + prepString(FLD_SUBJECT);
        result += ", " + prepString(FLD_PREDICATE);
        result += ", " + prepString(FLD_OBJECT);
        result += ", " + prepString(FLD_START_FROM);
        result += ", " + prepString(FLD_END_AT);
        result += ", " + prepString(FLD_SITE);
        result += ", " + prepString(FLD_TIMESTAMP);
        result += ", " + prepString(FLD_OWNER);
        result += ", " + prepString(FLD_CONSISTENCY) + ")";

        return result;
    }

    // For the transmission between the middleware and context sources only

    public static String ctx2StrWithoutId(Context ctx) {

        String result = (String) ctx.get(Context.FLD_CATEGORY);
        result += SEPARATOR + (String) ctx.get(FLD_SUBJECT);
        result += SEPARATOR + (String) ctx.get(FLD_PREDICATE);
        result += SEPARATOR + (String) ctx.get(FLD_OBJECT);
        result += SEPARATOR + (String) ctx.get(FLD_START_FROM);
        result += SEPARATOR + (String) ctx.get(FLD_END_AT);
        result += SEPARATOR + (String) ctx.get(FLD_SITE);
        result += SEPARATOR + (String) ctx.get(FLD_OWNER);

        // FLD_ID, FLD_TIMESTAMP, and FLD_CONSISTENCY are unnecessary

        return result;
    }

    // For the transmission between the middleware and context sources only

    public static Context str2CtxWithoutId(String ctxId, String str) {

        Context result = new Context();
        result.put(FLD_ID, ctxId);

        StringTokenizer st = new StringTokenizer(str, SEPARATOR);
        result.put(FLD_CATEGORY, st.nextToken());
        result.put(FLD_SUBJECT, st.nextToken());
        result.put(FLD_PREDICATE, st.nextToken());
        result.put(FLD_OBJECT, st.nextToken());
        result.put(FLD_START_FROM, st.nextToken());
        result.put(FLD_END_AT, st.nextToken());
        result.put(FLD_SITE, st.nextToken());
        result.put(FLD_OWNER, st.nextToken());

        // FLD_TIMESTAMP and FLD_CONSISTENCY are unnecessary

        return result;
    }

    // For the transmission between the middleware and applications only

    public static String ctx2StrWithId(Context ctx) {

        String result = ctx.getId();
        result += SEPARATOR + (String) ctx.get(FLD_CATEGORY);
        result += SEPARATOR + (String) ctx.get(FLD_SUBJECT);
        result += SEPARATOR + (String) ctx.get(FLD_PREDICATE);
        result += SEPARATOR + (String) ctx.get(FLD_OBJECT);
        result += SEPARATOR + (String) ctx.get(FLD_START_FROM);
        result += SEPARATOR + (String) ctx.get(FLD_END_AT);
        result += SEPARATOR + (String) ctx.get(FLD_SITE);
        result += SEPARATOR + (String) ctx.get(FLD_TIMESTAMP);
        result += SEPARATOR + (String) ctx.get(FLD_OWNER);
        result += SEPARATOR + (String) ctx.get(FLD_CONSISTENCY);

        return result;
    }

    // For the transmission between the middleware and applications only

    public static Context str2CtxWithId(String str) {

        Context result = new Context();

        StringTokenizer st = new StringTokenizer(str, SEPARATOR);
        result.put(FLD_ID, st.nextToken());
        result.put(FLD_CATEGORY, st.nextToken());
        result.put(FLD_SUBJECT, st.nextToken());
        result.put(FLD_PREDICATE, st.nextToken());
        result.put(FLD_OBJECT, st.nextToken());
        result.put(FLD_START_FROM, st.nextToken());
        result.put(FLD_END_AT, st.nextToken());
        result.put(FLD_SITE, st.nextToken());
        result.put(FLD_TIMESTAMP, st.nextToken());
        result.put(FLD_OWNER, st.nextToken());
        result.put(FLD_CONSISTENCY, st.nextToken());

        return result;
    }

    public boolean equals(Context other) {

        boolean result = false;

        if (other == null) {
            result = false;
        } else if (this == other) {
            result = true;
        } else {
            if (getId().equals(other.getId())) {
                result = true;
            } else {
                result = false;
            }
        }

        return result;
    }

}
