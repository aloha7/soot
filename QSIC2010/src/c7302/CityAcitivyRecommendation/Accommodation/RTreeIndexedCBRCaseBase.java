/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package c7302.CityAcitivyRecommendation.Accommodation;


import c7302.ActivityRecommender.Accommodation.Description;
import c7302.ActivityRecommender.utils.IEnhancedCBRCaseBase;
import com.infomatiq.jsi.*;
import com.infomatiq.jsi.rtree.*;

import java.util.ArrayList;
import java.util.Collection;
import jcolibri.cbrcore.*;
import jcolibri.exception.InitializingException;

/**
 *
 * @author hyhu
 */
class RTreeIndexedCBRCaseBase implements IEnhancedCBRCaseBase {

    private jcolibri.cbrcore.Connector connector;
    private java.util.Collection<CBRCase> allCases;
    private ArrayList<CBRCase> caseArray;
    private RTree rtree;

    private class NearestSearchIntProcedure implements IntProcedure {
        private ArrayList<CBRCase> _caseArray;
        private ArrayList<CBRCase> _nearestCases;

        public NearestSearchIntProcedure(ArrayList<CBRCase> caseArray) {
            _caseArray = caseArray;
            _nearestCases = new ArrayList<CBRCase>();
        }

        public boolean execute(int id) {
            _nearestCases.add(_caseArray.get(id));
            return true;
        }

        public Collection<CBRCase> getNearestCases() {
            return _nearestCases;
        }
    }

    public void init(Connector connector) throws InitializingException {
        this.connector = connector;
        this.allCases = this.connector.retrieveAllCases();

        // initialize RTree from cases.
       initializeRTree();
       
    }

    public void close() {
        this.connector.close();
    }

    public Collection<CBRCase> getCases() {
        return allCases;
    }

    public Collection<CBRCase> getCases(CaseBaseFilter filter) {
        return null;
    }

    public Collection<CBRCase> getCases(CBRQuery query) {
        // query against RTree and return a subset
        // 
        Description des = (Description)query.getDescription();
        String[] loc = ((String)des.getGPSlocation()).split(",");
        float lon = Float.parseFloat(loc[0]) * 1000000;
        float lat = Float.parseFloat(loc[1]) * 1000000;

        NearestSearchIntProcedure proc = new NearestSearchIntProcedure(caseArray);
        rtree.nearest(new Point(lon, lat), proc, Float.POSITIVE_INFINITY);
        
        Collection<CBRCase> rval = proc.getNearestCases();
        System.out.print("The number of near cases is: " + rval.size());
        return rval;
    }

    public void learnCases(Collection<CBRCase> cases) {
        try {
        connector.storeCases(cases);
        }
        catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
        allCases.addAll(cases);
        AddCasesToRTree(cases);
    }

    public void forgetCases(Collection<CBRCase> cases) {
       
    }

    private void initializeRTree() {
        rtree = new RTree();
        rtree.init(new java.util.Properties());
        caseArray = new ArrayList<CBRCase>();
        AddCasesToRTree(allCases);
        System.out.println("The number of cases is " + caseArray.size());
    }

    private void AddCasesToRTree(Collection<CBRCase> cases) {
        int id = caseArray.size();
        for (CBRCase item : cases) {
            Description des = (Description)item.getDescription();
            String[] loc = ((String)des.getGPSlocation()).split(",");
            float lon = Float.parseFloat(loc[0]) * 1000000;
            float lat = Float.parseFloat(loc[1]) * 1000000;

            caseArray.add(item);
            rtree.add(new Rectangle(lon, lat, lon, lat), id++);
        }
    }

	public void discardConfirmedCases() {
		// TODO Auto-generated method stub
		
	}
}
