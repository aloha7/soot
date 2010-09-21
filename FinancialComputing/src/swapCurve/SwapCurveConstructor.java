package swapCurve;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;

public class SwapCurveConstructor {	
	private static HashMap instrumentList = new HashMap();
	private static HashMap marketDataList = new HashMap();
	private static HashMap zeroCouponList = new HashMap(); //search for interested instrument quickly
	private static ArrayList IDList_ordered = new ArrayList();
	private static ArrayList dateList_ordered = new ArrayList(); //index of instruments to help generate zero coupon curve
	private static HashMap date_ZDF = new HashMap(); //load date_zeroCouponRate_DiscountFactor pairs from files
	
	
	private void loadInstrumentFile(){
		try {
			BufferedReader br = new BufferedReader(new FileReader(Configuration.SPECIFICATION_FILE));
			String line = br.readLine(); //ignore the header of the specification file
			
			while((line = br.readLine())!= null){ //for each row in the specification file
				String[] strs = line.split(",");
				String instrumentType = strs[0];
				String subType = strs[1];
				int ID = Integer.parseInt(strs[2]);
				
				ArrayList instruments = new ArrayList();
				instruments.add(instrumentType);
				instruments.add(subType);
				instrumentList.put(ID, instruments);		
				
				if(!IDList_ordered.contains(ID)){
					//find a right position to insert current to order ID in ascending orders, 
					int i = 0;
					for(; i < IDList_ordered.size(); i++){
						int ID_temp = (Integer)IDList_ordered.get(i);
						if(ID_temp > ID){
							IDList_ordered.add(i, ID);
							break;
						}
					}
					if(i ==  IDList_ordered.size()){
						IDList_ordered.add(ID);
					}					
				}
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	private void loadMarketDataFile(){
		try {
			BufferedReader br = new BufferedReader(new FileReader(Configuration.MARKETDATA_FILE));
			String line = br.readLine(); //ignore the header of the market data file
			
			while((line = br.readLine())!= null){ //for each row in the market data file
				String[] strs = line.split(",");
				int ID = Integer.parseInt(strs[0]);
				String rate = strs[1];			
				marketDataList.put(ID, rate);	
				
				if(!IDList_ordered.contains(ID)){
					//find a right position to insert current to order ID in ascending orders, 
					int i = 0;
					for(; i < IDList_ordered.size(); i++){
						int ID_temp = (Integer)IDList_ordered.get(i);
						if(ID_temp > ID){
							IDList_ordered.add(i, ID);
							break;
						}
					}
					if(i ==  IDList_ordered.size()){
						IDList_ordered.add(ID);
					}					
				}
			}
			
			br.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private HashMap loadDiscountFactorFile(){
		HashMap date_ZDF = new HashMap();
		
		try {
			BufferedReader br = new BufferedReader(new FileReader(Configuration.DISCOUNTFACTOR_FILE));
			String line = null;
			while((line = br.readLine())!= null){
				String[] strs = line.split("\t");
				String date = strs[0];
				double zeroCouponRate = Double.parseDouble(strs[1]);
				double discountFactor = Double.parseDouble(strs[2]);
				ArrayList temp = new ArrayList();
				temp.add(zeroCouponRate);
				temp.add(discountFactor);
				
				date_ZDF.put(date, temp);
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return date_ZDF;
	}
	
	/**given an instrument file and market data file, it can generate 
	 * zero coupon curve based on these data
	 * 
	 */
	public void generateZeroCouponCurve(){	
		loadInstrumentFile();
		loadMarketDataFile();
		
		for(int k = 0; k < IDList_ordered.size(); k ++){
			int ID = (Integer)IDList_ordered.get(k);
			ArrayList instruments = (ArrayList)instrumentList.get(ID);
			String instrument = (String)instruments.get(0);
			String subType = (String)instruments.get(1);
			
			if(marketDataList.containsKey(ID)){
				double rate = Double.parseDouble((String)marketDataList.get(ID));
				Instrument ins = null;
				
				if(instrument.equals("CASH")){
					ins = new Cash(subType, rate);	
					ins.getDF(zeroCouponList);
					ins.getZ(zeroCouponList);
					addDate(ins.endDate);
					
				}else if(instrument.equals("FRA")){
					ins = new FRA(subType, rate);
					ins.getDF(zeroCouponList);
					ins.getZ(zeroCouponList);
					addDate(ins.endDate);
					
				}else if(instrument.equals("SWAP")){
					Swap swap;
					swap = new Swap(subType, rate, zeroCouponList);
					
					swap.getDF(zeroCouponList);
					swap.getZ(zeroCouponList);
					
					for(int i = 0; i < swap.getSubSwaps().size(); i ++){
						ins = (SubSwap)swap.getSubSwaps().get(i);
						addDate(ins.endDate);
					}
				}
			}else{
				System.out.println("Error----There is no market data for ID:" + ID);
			}
		}
	}
	
	private void addDate(GregorianCalendar date){
		//find a right position to insert current to order endDates in ascending orders, 
		if(!dateList_ordered.contains(date)){
			int i = 0;
			for(; i < dateList_ordered.size(); i++){
				GregorianCalendar date_temp = (GregorianCalendar)dateList_ordered.get(i);
				if(TimeHandler.getDeltaDays(date_temp, date)<0){
					//ins.endDate < date_temp, then find a right place 
					dateList_ordered.add(i, date);
					break;
				}
			}
			if(i ==  dateList_ordered.size()){
				dateList_ordered.add(date);
			}	
		}else{
//			System.out.println("Warning:different instruments at the same date:" + TimeHandler.dateToString(date));
		}
		
	}
	
	
	public void saveZeroCouponCurve(){
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(Configuration.DISCOUNTFACTOR_FILE));
			
			StringBuilder sb = new StringBuilder();
			sb.append("Date" + "\t" + "Zero Rate" + "\t" + "Discount Factor" + "\n");
			
			for(int i = 0; i < dateList_ordered.size(); i ++){
				GregorianCalendar endDate = (GregorianCalendar)dateList_ordered.get(i);
				Instrument ins = (Instrument)zeroCouponList.get(endDate);
				sb.append(TimeHandler.dateToString(endDate) + "\t" + 
					RoundTool.roundOff(ins.zeroCouponRate, Configuration.SCALE_ZEROCOUPONRATE) + "\t" + 
					RoundTool.roundOff(ins.discountFactor, Configuration.SCALE_DISCOUNTFACTOR) + "\n");
			}
			
			bw.write(sb.toString());
			bw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getDF(GregorianCalendar date){
		double discountFactor = 0.0;
		
//		if(zeroCouponList.size()==0){
//			//no new zeroCoupon data is generate
//			if(date_ZDF.size() == 0){
//				//load data from files
//				date_ZDF = this.loadDiscountFactorFile();
//			}
//			String date_str = TimeHandler.dateToString(date);	
//			discountFactor = (Double)((ArrayList)date_ZDF.get(date_str)).get(1);
//
//		}else if(zeroCouponList.containsKey(date)){
//			discountFactor = ((Instrument)zeroCouponList.get(date)).discountFactor;
//		}
		if(zeroCouponList.containsKey(date)){
			discountFactor = ((Instrument)zeroCouponList.get(date)).discountFactor;
		}else{
			//1. find two nearest dates to be candidate references for interpolation
			GregorianCalendar largerDate=null, smallerDate=null; //two candidate references
			
			int i;
			for(i = 0; i < dateList_ordered.size(); i++){
				GregorianCalendar date_temp = (GregorianCalendar)dateList_ordered.get(i);
				if(TimeHandler.getDeltaDays(date_temp, date)<0){
					//date < date_temp, then find a right place 
					largerDate = date_temp;
					smallerDate = (GregorianCalendar)dateList_ordered.get(i-1);
					break;
				}
			}
			if(i ==  dateList_ordered.size()){
				//if cannot find the appropriate candidates, then select the last two points
				largerDate = (GregorianCalendar)dateList_ordered.get(dateList_ordered.size()-1);
				smallerDate = (GregorianCalendar)dateList_ordered.get(dateList_ordered.size()-2);
			}
			
			//2. interpolate
			double x1 = (double)TimeHandler.getDeltaDays(largerDate);
			double y1 = ((Instrument)zeroCouponList.get(largerDate)).discountFactor;
			
			double x2 = (double)TimeHandler.getDeltaDays(smallerDate);
			double y2 = ((Instrument)zeroCouponList.get(smallerDate)).discountFactor;
			
			double x = (double)TimeHandler.getDeltaDays(date);
			discountFactor = Instrument.interpolate(x1, y1, x2, y2, x);

			//3. save the discount factor for further query
			dateList_ordered.add(i, date);
			
			Instrument ins = new Cash();	
			ins.discountFactor = discountFactor;
			double t = x/365;
			ins.zeroCouponRate = ins.dfToZ(discountFactor, t);
			
			zeroCouponList.put(date, ins);
			
			System.out.println("Date" + "\t" + "ZeroCouponRate" + "\t" + "DiscountFactor" + "\n"
					+ TimeHandler.dateToString(date) + "\t" + ins.zeroCouponRate + "\t" + ins.discountFactor + "\n");
		}
		
		return discountFactor;
	}
	
	public double getZeroCouponRate(GregorianCalendar date){
		double zeroCouponRate = 0.0;
		
//		if(zeroCouponList.size()==0){
//			//no new zeroCoupon data is generate
//			if(date_ZDF.size() == 0){
//				//load data from files
//				date_ZDF = this.loadDiscountFactorFile();
//			}
//			String date_str = TimeHandler.dateToString(date);	
//			zeroCouponRate = (Double)((ArrayList)date_ZDF.get(date_str)).get(0);
//
//		}else if(zeroCouponList.containsKey(date)){
//			zeroCouponRate = ((Instrument)zeroCouponList.get(date)).zeroCouponRate;
//		}
		
		if(zeroCouponList.containsKey(date)){
			zeroCouponRate = ((Instrument)zeroCouponList.get(date)).zeroCouponRate;			
		}else{
			//1. find two nearest dates to be candidate references for interpolation
			GregorianCalendar largerDate=null, smallerDate=null; //two candidate references
			
			int i;
			for(i = 0; i < dateList_ordered.size(); i++){
				GregorianCalendar date_temp = (GregorianCalendar)dateList_ordered.get(i);
//				System.out.println("Temp Day:" + TimeHandler.dateToString(date_temp));
				
				if(TimeHandler.getDeltaDays(date_temp, date)<0){
					//date < date_temp, then find a right place 
					largerDate = date_temp;
					if(i == 0){
						smallerDate = (GregorianCalendar)dateList_ordered.get(i+1);
					}else{
						smallerDate = (GregorianCalendar)dateList_ordered.get(i-1);	
					}
					
					break;
				}
			}
			if(i ==  dateList_ordered.size()){
				//if cannot find the appropriate candidates, then select the last two points
				largerDate = (GregorianCalendar)dateList_ordered.get(dateList_ordered.size()-1);
				smallerDate = (GregorianCalendar)dateList_ordered.get(dateList_ordered.size()-2);
			}
			
			//2. interpolate
//			System.out.println("Large Day:" + TimeHandler.dateToString(largerDate) + "\t Small Day:" 
//					+ TimeHandler.dateToString(smallerDate));
//			
			double x1 = (double)TimeHandler.getDeltaDays(largerDate);
			double y1 = ((Instrument)zeroCouponList.get(largerDate)).zeroCouponRate;
			
			double x2 = (double)TimeHandler.getDeltaDays(smallerDate);
			double y2 = ((Instrument)zeroCouponList.get(smallerDate)).zeroCouponRate;
			
			double x = (double)TimeHandler.getDeltaDays(date);
//			System.out.println("Today" + TimeHandler.dateToString(TimeHandler.getBaseDate()));
//			System.out.println("Day:" + TimeHandler.dateToString(date));
			
			zeroCouponRate = Instrument.interpolate(x1, y1, x2, y2, x);

			//3. save the discount factor for further query
			dateList_ordered.add(i, date);
			
			Instrument ins = new Cash();
			ins.zeroCouponRate = zeroCouponRate;
			double t = x/365;
			ins.discountFactor = ins.zToDf(zeroCouponRate, t);
			
			zeroCouponList.put(date, ins);
			
			System.out.println("Date" + "\t" + "ZeroCouponRate" + "\t" + "DiscountFactor" + "\n"
					+ TimeHandler.dateToString(date) + "\t" + ins.zeroCouponRate + "\t" + ins.discountFactor + "\n");
			
		}
		
		return zeroCouponRate;
	}
	
	public void showZeroCouponCurve(){
		System.out.println("" + toString());
	}
	
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("Date" + "\t" + "Zero Rate" + "\t" + "Discount Factor" + "\n");
		
		for(int i = 0; i < dateList_ordered.size(); i ++){
			GregorianCalendar endDate = (GregorianCalendar)dateList_ordered.get(i);
			Instrument ins = (Instrument)zeroCouponList.get(endDate);
			sb.append(TimeHandler.dateToString(endDate) + "\t" + ins.zeroCouponRate + "\t" + ins.discountFactor + "\n");
		}
		return sb.toString();
	}
	
	
	public static void main(String[] args){
		SwapCurveConstructor main = new SwapCurveConstructor();	
		
		long start = System.currentTimeMillis();
		main.generateZeroCouponCurve();
		long time_last = (System.currentTimeMillis() - start)/1000;
		System.out.println("Time cost: " + time_last);
		

		String input = null;
		InputStreamReader is_reader = null;
		System.out.println("Input the interest date to retrieve its zero coupon rate and discount factor(e.g. 29-05-2009(DD-MM-YYYY)), and quit with \"Enter\"");
//		do{
//			try {
//				is_reader = new InputStreamReader(System.in);
//				input = new BufferedReader(is_reader).readLine();
//				if(input.equals("\n")||input.equals(""))
//					break;
//				
//				String[] strs = input.split("-");
//				if(strs.length!=3){
//					System.out.println("Please split the date with \"-\"");
//				}else{
//					int dayOfMonth =Integer.parseInt(strs[0]); 
//					int month = Integer.parseInt(strs[1])-1;
//					
//					int year = Integer.parseInt(strs[2]);
//					GregorianCalendar interestDate = new GregorianCalendar(year, month, dayOfMonth);
//					main.getZeroCouponRate(interestDate);
//				}
//			} catch (NumberFormatException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}while(true);
		
		main.showZeroCouponCurve();
		main.saveZeroCouponCurve();
	}
}
