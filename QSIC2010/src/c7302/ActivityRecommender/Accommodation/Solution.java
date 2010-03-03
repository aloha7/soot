package c7302.ActivityRecommender.Accommodation;

import jcolibri.cbrcore.Attribute;

public class Solution implements jcolibri.cbrcore.CaseComponent {

	Long caseId;
	Integer poiID;

	String name;
	String address;

        boolean stay;

	public String toString() {
		return "(" + caseId + ";" + poiID + ";" + name + ";" + address + ")";
	}

	public Long getCaseId() {
		return caseId;
	}

	public Integer getPoiID() {
		return poiID;
	}

	public void setPoiID(Integer poiID) {
		this.poiID = poiID;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public void setCaseId(Long caseId) {
		this.caseId = caseId;
	}

	public Attribute getIdAttribute() {

		return new Attribute("caseId", this.getClass());
	}

    /**
     * @return the stay
     */
    public boolean getStay() {
        return stay;
    }

    /**
     * @param stay the stay to set
     */
    public void setStay(boolean stay) {
        this.stay = stay;
    }

}
