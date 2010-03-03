package c7302.CityAcitivyRecommendation.Accommodation;

import c7302.CityAcitivyRecommendation.main.UserPreference;
import c7302.ActivityRecommender.Accommodation.Description;
import c7302.ActivityRecommender.Accommodation.Solution;

import java.io.File;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Collection;
import java.util.Iterator;

import jcolibri.cbrcore.Attribute;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.exception.ExecutionException;
import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;

/**
 * This class loads the hotel ontology and modifies and queries it.
 */

public class QueryAccommodation {

    private AccomRecommenderCBR recommender;
    private Description queryDesc;
    private CBRQuery query ;

    public QueryAccommodation(String path) {
        path += File.separator + "WEB-INF" + File.separator + "file" + File.separator;
        recommender = new AccomRecommenderCBR();
        queryDesc = new Description();
        query = new CBRQuery();
        
        try {
	        recommender.configure();
	        recommender.preCycle();
        } catch (ExecutionException ex) {
        	Logger.getLogger(QueryAccommodation.class.getName()).log(Level.SEVERE, null, ex);
        }        
    }

    /**
     * Query for the results.
     * @param up User preferences.
     * @return List of accommodations.
     */
    public ListOfPoiCase queryResults(UserPreference up) {

        /********* Query Definition **********/
        queryDesc.setTimestamp(up.getTimestamp());
        queryDesc.setExpenditure(up.getExpenditure());
        queryDesc.setRoomType(Description.RoomType.valueOf(up.getRoomType().toString()));
        queryDesc.setPaymentMethod(Description.PaymentMethod.valueOf(up.getPaymentMethod().toString()));
        queryDesc.setFacility(up.getFacility());
        queryDesc.setAccomType(Description.AccomType.valueOf(up.getAccomType().toString()));
        queryDesc.setGpslocation(up.getGpsLocation());
        queryDesc.setRate(up.getRate());

        query.setDescription(queryDesc);

        try {
            recommender.cycle(query);
            recommender.postCycle();
        } catch (ExecutionException ex) {
            Logger.getLogger(QueryAccommodation.class.getName()).log(Level.SEVERE, null, ex);
        }


        // Update user information instance
        ListOfPoiCase answers = new ListOfPoiCase();
        PoiCase accom = null;
        Collection retrievedCases = (Collection) recommender.getRetrievedCases();

        for (Iterator i = retrievedCases.iterator(); i.hasNext();) {
        	Object o1 = i.next();
        	CBRCase c = (CBRCase)o1;
            accom = new PoiCase();


            Object o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("accomType", Description.class));
            accom.setAccomType(AccomType.valueOf(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("caseId", Description.class));
            accom.setCaseId(Long.parseLong(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("rate", Description.class));
            accom.setRate(Integer.parseInt(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("roomType", Description.class));
            accom.setRoomType(RoomType.valueOf(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("facility", Description.class));
            accom.setFacility(o.toString());

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("paymentMethod", Description.class));
            accom.setPaymentMethod(PaymentMethod.valueOf(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("timestamp", Description.class));
            accom.setTimestamp((Timestamp) o);

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("expenditure", Description.class));
            accom.setExpenditure(Double.parseDouble(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getDescription(), new Attribute("gpsLocation", Description.class));
            accom.setGpsLocation(o.toString());

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getSolution(), new Attribute("poiID", Solution.class));
            accom.setPoiID(Integer.parseInt(o.toString()));

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getSolution(), new Attribute("name", Solution.class));
            accom.setName(o.toString());

            o = c7302.ActivityRecommender.utils.utility.getValueofAttribute(c.getSolution(), new Attribute("address", Solution.class));
            accom.setAddress(o.toString());

            accom.setActivityType(ActivityType.Accommodation);

            answers.addAccommodation(accom);

        }

        return answers;
    }

    public Description getQueryDesc() {
        return queryDesc;
    }

    public void setQueryDesc(Description queryDesc) {
        this.queryDesc = queryDesc;
    }

    public AccomRecommenderCBR getRecommender() {
        return recommender;
    }

    public void setRecommender(AccomRecommenderCBR recommender) {
        this.recommender = recommender;
    }

    public CBRQuery getQuery() {
        return query;
    }

    public void setQuery(CBRQuery query) {
        this.query = query;
    }
}
