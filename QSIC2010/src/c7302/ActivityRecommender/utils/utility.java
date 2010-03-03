/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package c7302.ActivityRecommender.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;
import jcolibri.cbrcore.Attribute;
import jcolibri.cbrcore.CBRCase;
import jcolibri.cbrcore.CaseComponent;
import jcolibri.util.AttributeUtils;


public class utility {


//     public static Vector<Object> extractColumnNames(CBRCase c) {
//        Vector<Object> res = new Vector<Object>();
//        res.add("Select");
//        extractColumnNames(c.getDescription(), res);
//        extractColumnNames(c.getSolution(), res);
//        extractColumnNames(c.getJustificationOfSolution(), res);
//        extractColumnNames(c.getResult(), res);
//        return res;
//    }

    public static Vector getAttributes(CBRCase c) {
        Vector res = new Vector();


        getAttributes(c.getDescription(), res);
        getAttributes(c.getSolution(), res);
        getAttributes(c.getJustificationOfSolution(), res);
        getAttributes(c.getResult(), res);

        return res;
    }
    
    public static void getAttributes(CaseComponent cc, Vector res) {
        Collection atts = AttributeUtils.getAttributes(cc);
        if (atts == null) {
            return;
        }

        Attribute id = cc.getIdAttribute();
        for (Iterator i = atts.iterator(); i.hasNext();) {
        	Object o = i.next();
        	Attribute a = (Attribute)o;
            if (!a.equals(id)) {
                res.add(AttributeUtils.findValue(a, cc));
            }
        }
    }

    public static Object getValueofAttribute(CaseComponent cc, Attribute a)
    {
        return AttributeUtils.findValue(a, cc);
    }

    /**
     * Extracts the column names (names of the attributes) from
     * a CaseComponent.
     * @param cc is the CaseComponent.
     * @param res List to fill.
     */
//    public static void extractColumnNames(CaseComponent cc, Vector<Object> res) {
//        Collection<Attribute> atts = AttributeUtils.getAttributes(cc);
//        if (atts == null) {
//            return;
//        }
//        Attribute id = cc.getIdAttribute();
//        for (Attribute a : atts) {
//            if (!a.equals(id)) {
//                res.add(a.getName());
//            }
//        }
//    }
}
