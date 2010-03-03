/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package c7302.ActivityRecommender.utils;

import java.util.Collection;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CBRCaseBase;
import jcolibri.cbrcore.CBRQuery;

/**
 *
 * @author kzhai
 */
public interface IEnhancedCBRCaseBase extends CBRCaseBase {
    Collection<CBRCase> getCases(CBRQuery query);
    void discardConfirmedCases();
}
