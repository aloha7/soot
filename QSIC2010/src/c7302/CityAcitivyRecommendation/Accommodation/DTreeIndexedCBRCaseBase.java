/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package c7302.CityAcitivyRecommendation.Accommodation;

import be.ac.ulg.montefiore.run.jadti.*;
import be.ac.ulg.montefiore.run.jadti.io.DecisionTreeToDot;
import c7302.ActivityRecommender.Accommodation.Description;
import c7302.ActivityRecommender.Accommodation.Solution;
import c7302.ActivityRecommender.utils.IEnhancedCBRCaseBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import java.util.Hashtable;
import java.util.Vector;
import jcolibri.cbrcore.*;
import jcolibri.exception.InitializingException;

/**
 *
 * @author hyhu
 */
class DTreeIndexedCBRCaseBase implements IEnhancedCBRCaseBase {

    private jcolibri.cbrcore.Connector connector;
    private Collection allCases;
    //private LearningDecisionTree dtree;
    private DecisionTree dtree;
    private Hashtable nodeCases;

    public void init(Connector connector) throws InitializingException {
        this.connector = connector;
        this.allCases = this.connector.retrieveAllCases();

        // initialize DTree from cases.
        initializeDTree(allCases);
    }

    public void close() {
        this.connector.close();
    }

    public Collection getCases() {
        return allCases;
    }

    public Collection getCases(CaseBaseFilter filter) {
        return null;
    }

    public Collection getCases(CBRQuery query) {
        // query against DTree and return a subset
        Item item = this.getTestItemFromDescription((Description)query.getDescription());
        Node leafNode = dtree.leafNode(item);
        if (nodeCases.containsKey(leafNode)) {
            ArrayList collection = (ArrayList)((ArrayList)nodeCases.get(leafNode)).clone();
            return collection;
        }
        else {
            return null;
        }

        //return allCases;
    }

    public void learnCases(Collection cases) {
        for (Iterator i = cases.iterator(); i.hasNext();) {
        	Object o = i.next();
        	CBRCase c = (CBRCase)o;
            fallCBRCaseToLeaves(dtree, c);
        }

        //connector.storeCases(cases);
        //allCases.addAll(cases);

        
    }

    public void forgetCases(Collection cases) {
        
    }

    private void initializeDTree(Collection allCases) {
        Vector attrSetVector = new Vector();
        attrSetVector.add(new SymbolicAttribute("PaymentMethod", Description.PaymentMethod.values().length));
        attrSetVector.add(new SymbolicAttribute("RoomType", Description.RoomType.values().length));
        attrSetVector.add(new SymbolicAttribute("AccomType", Description.AccomType.values().length));
//        attrSetVector.add(new SymbolicAttribute("SlottedTimestampType", Description.SlottedTimestampType.values().length));

        Vector testAttrSetVector = (Vector)attrSetVector.clone();

        SymbolicAttribute goalAttr = new SymbolicAttribute("Stay", 2);
        attrSetVector.add(goalAttr);

        //AttributeSet attrSet = new AttributeSet(attrSetVector);
        ItemSet learningSet = new ItemSet(new AttributeSet(attrSetVector));

        goalAttr = (SymbolicAttribute)learningSet.attributeSet().findByName("Stay");

        for (Iterator i = allCases.iterator(); i.hasNext();) {
        	Object o = i.next();
        	CBRCase c = (CBRCase)o;
            learningSet.add(getLearningItemFromCBRCase(c));
        }

        dtree = (new DecisionTreeBuilder(learningSet, new AttributeSet(testAttrSetVector), goalAttr)).build().decisionTree();
        //System.out.println((new DecisionTreeToDot(dtree)).produce());

        nodeCases = new Hashtable();
        for (Iterator i = allCases.iterator(); i.hasNext();) {
        	Object o = i.next();
        	CBRCase c = (CBRCase)o;
            fallCBRCaseToLeaves(dtree, c);
        }
    }

    private AttributeValue[] getValuesFromDescription(Description des) {
        AttributeValue[] rval = new AttributeValue[/*5*/4];
        rval[0] = new KnownSymbolicValue(des.getPaymentMethod().ordinal());
        rval[1] = new KnownSymbolicValue(des.getRoomType().ordinal());
        rval[2] = new KnownSymbolicValue(des.getAccomType().ordinal());
//        rval[3] = new KnownSymbolicValue(des.getSlottedTimestampType().ordinal());

        return rval;
    }

    private Item getTestItemFromDescription(Description des) {
        return new Item(getValuesFromDescription(des));
    }

    private Item getLearningItemFromCBRCase(CBRCase c) {
        AttributeValue[] rval = getValuesFromDescription((Description)c.getDescription());
        rval[/*4*/3] = new KnownSymbolicValue(((Solution)c.getSolution()).getStay() ? 1 : 0);
        return new Item(rval);
    }

    private void fallCBRCaseToLeaves(DecisionTree dtree, CBRCase c) {
        //AttributeValue[] values = getAttributeValuesFromCBRCase(c);
        Item item = getTestItemFromDescription((Description)c.getDescription());
        Node leafNode = dtree.leafNode(item);
        ArrayList collection;

        if (nodeCases.containsKey(leafNode)) {
            collection = (ArrayList)nodeCases.get(leafNode);
        }
        else {
            collection = new ArrayList();
            nodeCases.put(leafNode, collection);
        }

        if (!collection.contains(c)) {
            collection.add(c);
        }
    }

	public void discardConfirmedCases() {
		nodeCases.clear();
        for (Iterator i = allCases.iterator(); i.hasNext();) {
        	Object o = i.next();
        	CBRCase c = (CBRCase) o;
            fallCBRCaseToLeaves(dtree, c);
        }
	}
}