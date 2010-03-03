package c7302.CityAcitivyRecommendation.main;

import java.io.Serializable;
import java.sql.Timestamp;

import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;

/**
 * This represents the user preference class.
 */
public class UserPreference implements Serializable {

    Long caseId;
    Timestamp timestamp;
    String gpsLocation; // a string representation of longitude,latitude
    Double expenditure;

    PaymentMethod paymentMethod;

    ActivityType activityType;

    // accommodation
    AccomType accomType;
    Integer rate;

    RoomType roomType;
    String facility;

    // Dining
    FoodStyle foodStyle;

    // currency
    Code code;

    // entertainment
    FunType funType;

    // malling shopping
    ProductCategory productCategory;

    public UserPreference() {
    }

    public AccomType getAccomType() {
        return accomType;
    }

    public void setAccomType(AccomType accomType) {
        this.accomType = accomType;
    }

    public Long getCaseId() {
        return caseId;
    }

    public void setCaseId(Long caseId) {
        this.caseId = caseId;
    }

    public Double getExpenditure() {
        return expenditure;
    }

    public void setExpenditure(Double expenditure) {
        this.expenditure = expenditure;
    }

    public String getFacility() {
        return facility;
    }

    public void setFacility(String facility) {
        this.facility = facility;
    }

    public String getGpsLocation() {
        return gpsLocation;
    }

    public void setGpsLocation(String gpsLocation) {
        this.gpsLocation = gpsLocation;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getRate() {
        return rate;
    }

    public void setRate(Integer rate) {
        this.rate = rate;
    }

    public RoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(RoomType roomType) {
        this.roomType = roomType;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public void setActivityType(ActivityType activityType) {
        this.activityType = activityType;
    }

    public Code getCode() {
        return code;
    }

    public void setCode(Code code) {
        this.code = code;
    }

    public FoodStyle getFoodStyle() {
        return foodStyle;
    }

    public void setFoodStyle(FoodStyle foodStyle) {
        this.foodStyle = foodStyle;
    }

    public FunType getFunType() {
        return funType;
    }

    public void setFunType(FunType funType) {
        this.funType = funType;
    }

    public ProductCategory getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(ProductCategory productCategory) {
        this.productCategory = productCategory;
    }
}
