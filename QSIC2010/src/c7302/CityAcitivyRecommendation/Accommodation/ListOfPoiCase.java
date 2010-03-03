package c7302.CityAcitivyRecommendation.Accommodation;

import java.util.ArrayList;
import java.util.Collections;

//import sun.text.CompactShortArray.Iterator;

/**
 * This class represents the list of accommodations.
 */
public class ListOfPoiCase {

    private ArrayList PoiList;
    
    public ListOfPoiCase() {
        this.PoiList = new ArrayList();
    }

    public ArrayList getAccommodationList() {
        return this.PoiList;
    }
    
    public void addAccommodation(PoiCase accom) {
        this.PoiList.add(accom);
    }
    
    public String toString()
    {
        String ret = "<Response>";
        for (int i = 0; i < PoiList.size(); i++) {
        	Object o = PoiList.get(i);
        	PoiCase poi = (PoiCase)o;
            ret += poi.toString();
        }
        ret += "</Response>";
        
        return ret;
    }
}
