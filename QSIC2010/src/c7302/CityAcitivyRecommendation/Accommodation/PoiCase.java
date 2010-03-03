package c7302.CityAcitivyRecommendation.Accommodation;

import java.sql.Timestamp;

import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;

/**
 * This represents an accommodation and its information.
 */
public class PoiCase {

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

    String menuItems;

    // currency
    Code code;
    Double buyRate;


    // entertainment
    FunType funType;
    Integer movieID;
    Integer periodID;

    //HouseHoldItemShopping
    String shopItems;

    // malling shopping

    ProductCategory productCategory;
    String productIDs;
    
    Integer poiID;
    String name;
    String address;

    public PoiCase() {
    }

    public AccomType getAccomType() {
        return accomType;
    }

    public void setAccomType(AccomType accomType) {
        this.accomType = accomType;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public Integer getPoiID() {
        return poiID;
    }

    public void setPoiID(Integer poiID) {
        this.poiID = poiID;
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

    public Double getBuyRate() {
        return buyRate;
    }

    public void setBuyRate(Double buyRate) {
        this.buyRate = buyRate;
    }

    public String getMenuItems() {
        return menuItems;
    }

    public void setMenuItems(String menuItems) {
        this.menuItems = menuItems;
    }

    public Integer getMovieID() {
        return movieID;
    }

    public void setMovieID(Integer movieID) {
        this.movieID = movieID;
    }

    public Integer getPeriodID() {
        return periodID;
    }

    public void setPeriodID(Integer periodID) {
        this.periodID = periodID;
    }

    public String getProductIDs() {
        return productIDs;
    }

    public void setProductIDs(String productIDs) {
        this.productIDs = productIDs;
    }

    public String getShopItems() {
        return shopItems;
    }

    public void setShopItems(String shopItems) {
        this.shopItems = shopItems;
    }


    public String toString() {

        String gpslocation = this.getGpsLocation();
        String[] values = gpslocation.split(",");

        String ret = "";
        ret = "<POI ActivityType=\"" + this.activityType.toString() +
                "\" CaseID=\"" + this.getCaseId() +
                "\" Name=\"" + this.name +
                "\" Address=\"" + this.address +
                "\" Latitude=\"" + values[1] +
                "\" Longitude=\"" + values[0] +
                "\" Timestamp=\"" + this.getTimestamp().getTime() +
                "\" Expenditure=\"" + this.getExpenditure() +
                "\" PoiID=\"" + this.getPoiID() +
                "\" PaymentMethod=\"" + this.getPaymentMethod().toString() + "\">";

        switch (this.activityType) {
            case Accommodation:
                ret += "<Accommodation RoomType=\"" + this.getRoomType().toString() +
                "\" AccomType=\"" + this.getAccomType().toString() +
                "\" Facilities=\"" +this.getFacility() +
                "\" Rating=\"" + this.getRate();
                ret += "\">";
                ret += "</Accommodation>";
                break;
            case Currency:
                ret += "<Currency CurrencyCode=\"" + this.getCode() +
                        "\" BuyRate=\"" + this.getBuyRate() +
                        "\">";
                ret += "</Currency>";
                break;
            case Dinning:
                ret += "<Dining FoodStyle=\"" + this.getFoodStyle() +
                        "\" MenuItems=\"" + this.getMenuItems() +
                        "\">";
                ret += "</Dining>";
                break;
            case Entertainment:
                ret += "<Entertainment FunType=\"" + this.getFunType() +
                        "\" MovieID=\"" + this.getMovieID() +
                        "\" PeriodID=\"" + this.getPeriodID() +
                        "\">";
                ret += "</Entertainment>";
                break;
                
            case HouseholdShopping:
                ret += "<HouseholdShopping ShopItems=\"" + this.getShopItems() +
                        "\">";
                ret += "</HouseholdShopping>";
                break;

            case MallShopping:
                ret += "<MallShopping ProductCategory=\"" + this.getProductCategory() +
                        "\" ProductIDs=\"" + this.getProductIDs() +
                        "\">";
                ret += "</MallShopping>";
                break;
            case Tourism:
                break;
            default:
                break;
        }

        ret += "</POI>";

        return ret;
    }
}
