package c7302.ActivityRecommender.Accommodation;

import java.sql.Timestamp;

import jcolibri.cbrcore.Attribute;

public class Description implements jcolibri.cbrcore.CaseComponent {

	Long caseId;
	Timestamp timestamp;

	Double expenditure;

	public enum PaymentMethod {
		MasterCard, Visa, AmericanExpress, DinersClub, JBC, CUP, Cash, Octopus
	};

	PaymentMethod paymentMethod;

	public enum RoomType {
		Single, Double, Triple, Twin, Suite, Family
	};

	RoomType roomType;

	String facility;

	public enum AccomType {
		Hotel, GuestHouse, Hostel
	};

	AccomType accomType;

	String gpsLocation; // a string representation of longitude,latitude

	Integer rate;

    public enum SlottedTimestampType {
        Night, Morning, Afternoon, Evening
    }

//    SlottedTimestampType slottedTimestampType = SlottedTimestampType.Evening;

	public String toString() {
		return "(" + caseId + ";" + expenditure + ";" + paymentMethod + ";"
				+ accomType + ")";
	}

	public String getFacility() {
		return facility;
	}

	public void setFacility(String facility) {
		this.facility = facility;
	}

	public Long getCaseId() {
		return caseId;
	}

	public void setCaseId(Long caseId) {
		this.caseId = caseId;
	}

	public Timestamp getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Timestamp timestamp) {
		this.timestamp = timestamp;
//                int hours = timestamp.getHours();
//                
//                switch (hours / 6) {
//                    case 0:
//                        this.slottedTimestampType = SlottedTimestampType.Night;
//                    case 1:
//                        this.slottedTimestampType = SlottedTimestampType.Morning;
//                    case 2:
//                        this.slottedTimestampType = SlottedTimestampType.Afternoon;
//                    case 3:
//                        this.slottedTimestampType = SlottedTimestampType.Evening;
//                }
	}

	public String getGPSlocation() {
		return gpsLocation;
	}

	public RoomType getRoomType() {
		return roomType;
	}

	public void setRoomType(RoomType roomType) {
		this.roomType = roomType;
	}

	public void setGPSlocation(String slocation) {
		gpsLocation = slocation;
	}

	public Double getExpenditure() {
		return expenditure;
	}

	public void setExpenditure(Double expenditure) {
		this.expenditure = expenditure;
	}

	public PaymentMethod getPaymentmethod() {
		return paymentMethod;
	}

	public void setPaymentmethod(PaymentMethod paymentmethod) {
		this.paymentMethod = paymentmethod;
	}

	public AccomType getAccomtype() {
		return accomType;
	}

	public void setAccomtype(AccomType accomtype) {
		this.accomType = accomtype;
	}

	public String getGpslocation() {
		return gpsLocation;
	}

	public void setGpslocation(String gpslocation) {
		this.gpsLocation = gpslocation;
	}

	public Integer getRate() {
		return rate;
	}

	public void setRate(Integer rate) {
		this.rate = rate;
	}

	public PaymentMethod getPaymentMethod() {
		return paymentMethod;
	}

	public void setPaymentMethod(PaymentMethod paymentMethod) {
		this.paymentMethod = paymentMethod;
	}

	public AccomType getAccomType() {
		return accomType;
	}

	public void setAccomType(AccomType accomType) {
		this.accomType = accomType;
	}

	public String getGpsLocation() {
		return gpsLocation;
	}

	public void setGpsLocation(String gpsLocation) {
		this.gpsLocation = gpsLocation;
	}

//        public SlottedTimestampType getSlottedTimestampType() {
//            return slottedTimestampType;
//        }

	public static void main(String[] args) {

		Description t = new Description();
		t.setAccomtype(AccomType.Hotel);
		Attribute at = new Attribute("accomType", Description.class);
		try {
			System.out.println(at.getValue(t));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Attribute getIdAttribute() {
		return new Attribute("caseId", this.getClass());
	}
}
