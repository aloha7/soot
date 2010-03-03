package c7302.ActivityRecommender.utils;

import java.util.Collection;
import java.util.HashSet;

import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRQuery;
import jcolibri.cbrcore.CaseBaseFilter;
import jcolibri.cbrcore.Connector;
import jcolibri.exception.InitializingException;

public class EnhancedLinealCaseBase implements IEnhancedCBRCaseBase {

	private jcolibri.cbrcore.Connector connector;
    private Collection allCases;
    private HashSet confirmedCases;

	public void discardConfirmedCases() {
		allCases.removeAll(confirmedCases);
		confirmedCases.clear();
	}

	public Collection getCases(CBRQuery query) {
    	return allCases;
	}

	public void close() {
        this.connector.close();
	}

	public void forgetCases(Collection cases) {
 	}

	public Collection getCases() {
    	return allCases;
	}

	public Collection getCases(CaseBaseFilter filter) {
		return allCases;
	}

	public void init(Connector connector) throws InitializingException {
        this.connector = connector;
        this.allCases = this.connector.retrieveAllCases();
        confirmedCases = new HashSet();
	}

	public void learnCases(Collection cases) {
		confirmedCases.addAll(cases);
		allCases.addAll(cases);
	}
}
