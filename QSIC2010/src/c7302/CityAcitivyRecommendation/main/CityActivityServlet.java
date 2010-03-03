package c7302.CityAcitivyRecommendation.main;

import c7302.CityAcitivyRecommendation.Accommodation.*;
import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;
import java.io.*;
import java.sql.Timestamp;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.concurrent.*;

public class CityActivityServlet extends HttpServlet {

    private QueryAccommodation cmAccomm;

    class RunnableAccom implements Runnable {

        private UserPreference uP;
        private ListOfPoiCase answer;
        private final CountDownLatch startSignal;
        private final CountDownLatch doneSignal;

        public RunnableAccom(UserPreference uP, ListOfPoiCase answer, CountDownLatch startSignal, CountDownLatch doneSignal) {

            this.uP = uP;
            this.answer = answer;
            this.startSignal = startSignal;
            this.doneSignal = doneSignal;

        }

        public void run() {
            try {
                startSignal.await();
                ListOfPoiCase queryAnswerAccomm = cmAccomm.queryResults(uP);
                answer.getAccommodationList().addAll(queryAnswerAccomm.getAccommodationList());
                doneSignal.countDown();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    //use only default constructor
    public CityActivityServlet() {
    }

    // because in constructor won't work
    @Override
    public void init() throws ServletException {
        super.init();

        c7302.CityAcitivyRecommendation.main.MySQLDBserver.init();
        String contextPath = getServletContext().getRealPath("/");
        cmAccomm = new QueryAccommodation(contextPath);


    }

    @Override
    public String getServletInfo() {
        return "ActivityRecommender";
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ListOfPoiCase answer = new ListOfPoiCase();

        // Set user preferences
        UserPreference uP = new UserPreference();
        uP.setAccomType(AccomType.valueOf(request.getParameter("accommodationType")));
        uP.setRoomType(RoomType.valueOf(request.getParameter("roomType")));
        uP.setFacility(request.getParameter("facility"));
        uP.setPaymentMethod(PaymentMethod.valueOf(request.getParameter("paymentMethod")));
        uP.setRate(Integer.parseInt(request.getParameter("rating")));
        uP.setGpsLocation(request.getParameter("longitude") + "," + request.getParameter("latitude"));
        uP.setTimestamp(new Timestamp(Long.parseLong(request.getParameter("currentTime"))));
        uP.setExpenditure(Double.parseDouble(request.getParameter("expenditure")));
        uP.setCode(Code.valueOf(request.getParameter("code")));
        uP.setFoodStyle(FoodStyle.valueOf(request.getParameter("style")));
        uP.setProductCategory(ProductCategory.valueOf(request.getParameter("productcategory")));
        uP.setFunType(FunType.valueOf(request.getParameter("funtype")));

        CountDownLatch startSignal = new CountDownLatch(1);
        CountDownLatch doneSignal = new CountDownLatch(1);

        if (request.getParameter("Messagetype").equalsIgnoreCase("Query") || request.getParameter("Messagetype").equalsIgnoreCase("Refine")) {
            try {
                uP.activityType = ActivityType.Accommodation;
                switch (uP.activityType) {
                    case Accommodation:
                        RunnableAccom runAccom = new RunnableAccom(uP, answer, startSignal, doneSignal);
                        Thread threadHotel = new Thread(runAccom);
                        threadHotel.start();
                        startSignal.countDown();      // let all threads proceed
                        doneSignal.await();           // wait for all to finish
                        // Send response
                        PrintWriter sendOut = new PrintWriter(response.getOutputStream(), true);
                        response.setContentType("text/xml");
                        response.setContentLength(answer.toString().length());
                        sendOut.print(answer.toString());
                        sendOut.close();
                        break;
                    case Currency:
                        break;
                    case Dinning:
                        break;
                    case Entertainment:
                        break;
                    case HouseholdShopping:
                        break;
                    case MallShopping:
                        break;
                    case Tourism:
                        break;
                    default:
                        break;
                }

            } catch (Exception err) {
                err.printStackTrace();
            }
        } else if (request.getParameter("Messagetype").equalsIgnoreCase("Choice")) {
            uP.setActivityType(ActivityType.valueOf(request.getParameter("ActivityType")));
            switch (uP.activityType) {
                case Accommodation:
                    int ChoiceID = Integer.parseInt(request.getParameter("ChoiceID"));
                    boolean exit = Boolean.parseBoolean(request.getParameter("Exit"));
                    boolean refine = Boolean.parseBoolean(request.getParameter("Refine"));
				try {
					cmAccomm.getRecommender().Userconfirm(cmAccomm.getQuery(), refine, ChoiceID, exit);
				} catch (Exception e) {
					e.printStackTrace();
				}
                    break;
                case Currency:
                    break;
                case Dinning:
                    break;
                case Entertainment:
                    break;
                case HouseholdShopping:
                    break;
                case MallShopping:
                    break;
                case Tourism:
                    break;
                default:
                    break;
            }
        } else if (request.getParameter("Messagetype").equalsIgnoreCase("Exit")) {
            // do nothing
        }
    }
}
