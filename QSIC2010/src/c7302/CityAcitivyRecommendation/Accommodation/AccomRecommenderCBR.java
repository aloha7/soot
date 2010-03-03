/**
 * Test4.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-Garc�a.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 11/01/2007
 */
package c7302.CityAcitivyRecommendation.Accommodation;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import jcolibri.cbraplications.StandardCBRApplication;
import jcolibri.cbrcore.Attribute;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRCaseBase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.cbrcore.Connector;
import jcolibri.connector.DataBaseConnector;
import jcolibri.exception.ExecutionException;
import jcolibri.method.retrieve.NNretrieval.NNConfig;
import jcolibri.method.retrieve.NNretrieval.NNScoringMethod;
import jcolibri.method.retrieve.NNretrieval.similarity.LocalSimilarityFunction;
import jcolibri.method.retrieve.NNretrieval.similarity.global.Average;
import jcolibri.method.retrieve.selection.SelectCases;
import jcolibri.method.reuse.CombineQueryAndCasesMethod;
import c7302.ActivityRecommender.Accommodation.Description;
import c7302.ActivityRecommender.Accommodation.Solution;
import c7302.ActivityRecommender.utils.EnhancedLinealCaseBase;
import c7302.ActivityRecommender.utils.GPSLocationSimilarity;
import c7302.ActivityRecommender.utils.IEnhancedCBRCaseBase;
import c7302.ActivityRecommender.utils.StringSetSimilarity;
import c7302.ActivityRecommender.utils.TimestampSimilarity;
import c7302.ActivityRecommender.utils.ToyEqual;
import c7302.ActivityRecommender.utils.ToyInrecaLessIsBetter;

//@SuppressWarnings({ "unchecked", "unused" })
public class AccomRecommenderCBR implements StandardCBRApplication {

    Connector _connector1;
    IEnhancedCBRCaseBase _caseBase1;
    //DTreeIndexedCBRCaseBase _caseBase1;
    //RTreeIndexedCBRCaseBase _caseBase2;


    
    /** KNN config */
    NNConfig simConfig;

    /** hidden attributes for obtain query */
//    Collection<Attribute> hiddenAtts;
    
    /** custum labels for obtain query */
//    Map<Attribute, String> labels;
    
    Collection retrievedCases;

    public void configure() throws ExecutionException {
        try {
            _connector1 = new DataBaseConnector();
            _connector1.initFromXMLfile(jcolibri.util.FileIO.findFile("c7302/ActivityRecommender/Configure/Accommodation/Config.xml"));
            _caseBase1 = new EnhancedLinealCaseBase();
//            _caseBase1 = new DTreeIndexedCBRCaseBase();
            //_caseBase1 = new RTreeIndexedCBRCaseBase();
        } catch (Exception e) {
            throw new ExecutionException(e);
        }

//        hiddenAtts = new ArrayList<Attribute>();
//        labels = new HashMap<Attribute, String>();
//
//        labels.put(new Attribute("accomType", Description.class), "Accom Type");
//        labels.put(new Attribute("rate", Description.class), "Rate");
//        labels.put(new Attribute("expenditure", Description.class),
//                "Expenditure");
//        labels.put(new Attribute("slottedTimestampType", Description.class), "Slotted Timestamp");

        simConfig = new NNConfig();
        simConfig.setDescriptionSimFunction(new Average());

        addAttribute(simConfig, "timestamp", new TimestampSimilarity(), 0.0);
        addAttribute(simConfig, "roomType", new ToyEqual(), 0.0);
        addAttribute(simConfig, "gpsLocation", new GPSLocationSimilarity(), 1.0);
        addAttribute(simConfig, "paymentMethod", new ToyEqual(), 0.0);
        addAttribute(simConfig, "accomType", new ToyEqual(), 0.0);
        addAttribute(simConfig, "rate", new ToyEqual(), 0.0);
        addAttribute(simConfig, "expenditure", new ToyInrecaLessIsBetter(1000, 0.5), 0.0);
        addAttribute(simConfig, "facility", new StringSetSimilarity(), 0.0);

//        simConfig.addMapping(new Attribute("timestamp", Description.class), new TimestampSimilarity());
//        simConfig.addMapping(new Attribute("roomType", Description.class), new ToyEqual());
//        simConfig.addMapping(new Attribute("gpsLocation", Description.class),
//                new GPSLocationSimilarity());
//        simConfig.addMapping(new Attribute("paymentMethod", Description.class),
//                new ToyEqual());
//        simConfig.addMapping(new Attribute("accomType", Description.class),
//                new ToyEqual());
//        simConfig.addMapping(new Attribute("rate", Description.class),
//                new ToyEqual());
//        simConfig.addMapping(new Attribute("expenditure", Description.class),
//                new ToyInrecaLessIsBetter(1000, 0.5));
//        simConfig.addMapping(new Attribute("facility", Description.class),
//                new StringSetSimilarity());

        //        simConfig.addMapping(new Attribute("slottedTimestampType", Description.class),
//                new Equal());
    }
    
    private void addAttribute(NNConfig simConfig, String attributeName, LocalSimilarityFunction fun, double weight) {
    	Attribute a = new Attribute(attributeName, Description.class);
    	simConfig.addMapping(a, fun);
    	simConfig.setWeight(a, weight);
    }

    /*
     * (non-Javadoc)
     *
     * @see jcolibri.cbraplications.BasicCBRApplication#preCycle()
     */
    public CBRCaseBase preCycle() throws ExecutionException {
        _caseBase1.init(_connector1);
        //for (jcolibri.cbrcore.CBRCase c : _caseBase1.getCases()) {
        //    System.out.println(c);
        //}
        return _caseBase1;
    }

    /*
     * (non-Javadoc)
     *
     * @see jcolibri.cbraplications.BasicCBRApplication#cycle()
     */
    
    public void cycle(CBRQuery query) throws ExecutionException {
        // Obtain the query
        //ObtainQueryWithFormMethod.obtainQueryWithInitialValues(query,hiddenAtts, labels);
        // Jump to the conversation cycle
        sequence1(query);
        
    }

    public void sequence1(CBRQuery query) {
//		long time = System.currentTimeMillis();

        // Execute KNN
//        Collection<RetrievalResult> eval = (Collection<RetrievalResult>) NNScoringMethod.evaluateSimilarity(
//                _caseBase1.getCases( query ), query, simConfig);
        // Select cases
//        retrievedCases = SelectCases.selectTopK(eval, 3);
//        retrievedCases = SelectCases.selectTopK( NNScoringMethod.evaluateSimilarity(
//                _caseBase1.getCases( query ), query, simConfig), 3);
//        retrievedCases = SelectCases.selectTopK( NNScoringMethod.evaluateSimilarity(
//                _caseBase1.getCases( query ), query, simConfig), _caseBase1.getCases().size());
        retrievedCases = SelectCases.selectTopK( NNScoringMethod.evaluateSimilarity(
                _caseBase1.getCases( query ), query, simConfig), 5);
        
        removeDuplication(retrievedCases, 3);
        
//		LogWriter.me().writeln("queryResults: " + (System.currentTimeMillis() - time) + "ms  ");

        // print out before sending
//        Vector<Object> columnNames = c7302.ActivityRecommender.utils.utility.extractColumnNames(retrievedCases.iterator().next());
//        Vector<Object> rows = new Vector<Object>();
//
//        for (CBRCase c : retrievedCases) {
//            rows.add(c7302.ActivityRecommender.utils.utility.getAttributes(c));
//        }
//        for (int i = 0; i < columnNames.size(); i++) {
//            //System.out.print(columnNames.get(i));
//        }
//        for (int i = 0; i < rows.size(); i++) {
//            //System.out.print(rows.get(i));
//        }
    }
    
    private void removeDuplication(Collection cases, int maxLeft) {
    	ArrayList list = (ArrayList)cases;
    	ArrayList remove = new ArrayList();
    	
    	int i = 1;
    	while (i < list.size() && i < maxLeft) {
    		boolean dup = false;
    		Solution sol1 = (Solution)((CBRCase)list.get(i)).getSolution();
    		for (int j = 0; j < i; j++) {
    			Solution sol2 = (Solution)((CBRCase)list.get(j)).getSolution();
    			if (sol1.getPoiID() == sol2.getPoiID()) {
    				dup = true;
    				list.remove(i);
    				break;
    			}
    		}
    		
    		if (!dup) i++;
    	}
    	
    	while (i < list.size() && i == maxLeft) {
    		list.remove(i);
    	}
    }

    public void Userconfirm(CBRQuery query, boolean refine, int ChoiceID, boolean exit) throws Exception {
        CBRCase[] _cases;
        _cases = new CBRCase[retrievedCases.size()];
        retrievedCases.toArray(_cases);
        // Continue or Finish?
        if (refine) {
            sequence2(query);
        } else {
//        	if (ChoiceID >= _cases.length)
//        		System.out.println("bullshit");
            CBRCase selectedCase = _cases[ChoiceID];
            sequence3(query, ChoiceID, exit, selectedCase);
        }
//    	throw new Exception();
//        while (true);
    }

    public void sequence2(CBRQuery query) {
        // Jump to converstaion cycle
        sequence1(query);
    }

    
	public void sequence3(CBRQuery query, int ChoiceID, boolean exit, CBRCase selectedCase) {

        if (ChoiceID != -1) {
            /********* Reuse **********/
            Collection temp = new ArrayList();
            temp.add(selectedCase);
            Collection newcases = CombineQueryAndCasesMethod.combine(
                    query, temp);

            /********* Revise **********/
            CBRCase bestCase = (CBRCase)newcases.iterator().next();
            HashMap componentsKeys = new HashMap();
            Long caseId = new Long(System.currentTimeMillis());
            componentsKeys.put(new Attribute("caseId", Description.class),
                    caseId);
            componentsKeys.put(new Attribute("caseId", Solution.class), caseId);
            try {
                jcolibri.method.revise.DefineNewIdsMethod.defineNewIdsMethod(
                        bestCase, componentsKeys);
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

            /********* Retain **********/
            jcolibri.method.retain.StoreCasesMethod.storeCase(_caseBase1, bestCase);
         
        } else if (exit = true) {
            //System.out.println("Finish - User Quits");
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see jcolibri.cbraplications.BasicCBRApplication#postCycle()
     */
    public void postCycle() throws ExecutionException {

        this._caseBase1.close();
        // Shutdown DDBB manager
        c7302.CityAcitivyRecommendation.main.MySQLDBserver.shutDown();
    }

    public IEnhancedCBRCaseBase getCaseBase1() {
        return _caseBase1;
    }

    public void setCaseBase1(IEnhancedCBRCaseBase _caseBase1) {
        this._caseBase1 = _caseBase1; 
    }

    public Connector getConnector1() {
        return _connector1;
    }

    public void setConnector1(Connector _connector1) {
        this._connector1 = _connector1;
    }

//    public Collection<Attribute> getHiddenAtts() {
//        return hiddenAtts;
//    }
//
//    public void setHiddenAtts(Collection<Attribute> hiddenAtts) {
//        this.hiddenAtts = hiddenAtts;
//    }

//    public Map<Attribute, String> getLabels() {
//        return labels;
//    }
//
//    public void setLabels(Map<Attribute, String> labels) {
//        this.labels = labels;
//    }

    public Collection getRetrievedCases() {
        return retrievedCases;
    }

//    public void setRetrievedCases(Collection<CBRCase> retrievedCases) {
//        this.retrievedCases = retrievedCases;
//    }

    public NNConfig getSimConfig() {
        return simConfig;
    }

    public void setSimConfig(NNConfig simConfig) {
        this.simConfig = simConfig;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // Launch DDBB manager
        c7302.CityAcitivyRecommendation.main.MySQLDBserver.init();

        AccomRecommenderCBR recommender = new AccomRecommenderCBR();
        try {

            /********* Query Definition **********/
            Description queryDesc = new Description();
            queryDesc.setTimestamp(new Timestamp(System.currentTimeMillis()));
            queryDesc.setExpenditure(2000.0);
            queryDesc.setRoomType(Description.RoomType.Double);
            queryDesc.setPaymentmethod(Description.PaymentMethod.Visa);
            queryDesc.setFacility("TV,Pool,Internet,Laundry");
            queryDesc.setAccomtype(Description.AccomType.Hotel);
            queryDesc.setGpslocation("114.170324,22.277726");
            queryDesc.setRate(5);

            CBRQuery query = new CBRQuery();
            query.setDescription(queryDesc);

            recommender.configure();
            recommender.preCycle();
            recommender.cycle(query);
            recommender.postCycle();

        } catch (ExecutionException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
