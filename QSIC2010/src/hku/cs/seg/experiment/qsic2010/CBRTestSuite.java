package hku.cs.seg.experiment.qsic2010;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;

import c7302.CityAcitivyRecommendation.Accommodation.PoiCase;
import c7302.CityAcitivyRecommendation.main.UserPreference;
import c7302.ActivityRecommender.utils.EnumerationDefinitions.*;

import hku.cs.seg.experiment.core.ITestCase;
import hku.cs.seg.experiment.core.SimpleTestCase;
import hku.cs.seg.experiment.core.SimpleTestSuite;

public class CBRTestSuite extends SimpleTestSuite {
	private final int MonitorInterval = 1000;
	
	@Override
	public boolean readFromTextFile(String filename) {
		try {
//			System.out.print("Reading test suite: ");
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String str = null;
			this.clear();
			while((str = br.readLine())!= null){
				String[] strs = str.split("\t");
				
				int id = 0;
				try {
					id = Integer.parseInt(strs[0]);
				} catch (NumberFormatException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				int seqNum = Integer.parseInt(strs[1]);
//				if (id % MonitorInterval == 0) System.out.print("[" + id + "] ");
				
				CBRInput input = new CBRInput();
				int i = 0, j = 2;
				for (; i < seqNum; i++, j += 13) {
					UserPreference up = new UserPreference();
					up.setAccomType(AccomType.valueOf(strs[j + 0]));
					up.setRoomType(RoomType.valueOf(strs[j + 1]));
					up.setFacility(strs[j + 2]);
					up.setPaymentMethod(PaymentMethod.valueOf(strs[j + 3]));
					up.setRate(Integer.parseInt(strs[j + 4]));
					up.setGpsLocation(strs[j + 5]);
					up.setTimestamp(new Timestamp(Long.parseLong(strs[j + 6])));
					up.setExpenditure(Double.parseDouble(strs[j + 7]));
					up.setCode(Code.valueOf(strs[j + 8]));
					up.setFoodStyle(FoodStyle.valueOf(strs[j + 9]));
					up.setProductCategory(ProductCategory.valueOf(strs[j + 10]));
					up.setFunType(FunType.valueOf(strs[j + 11]));
					
					boolean confirm = strs[j + 12].equals("1");
					
					TestContextVariable tcv = new TestContextVariable(up, confirm);
					input.add(tcv);
				}
				
				CBROracle oracle = new CBROracle();
				for (String poiId : strs[j].split(",")) {
					oracle.add(Integer.parseInt(poiId));
				}
				
				this.add(new SimpleTestCase(id, input, oracle));
			}

//			System.out.println("Done.");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	
		return true;
	}

	@Override
	public boolean writeToTextFile(String filename, boolean append) {		
		StringBuilder sb = new StringBuilder();		
		try {
			BufferedWriter bw = new BufferedWriter(new FileWriter(filename, append));
			
			System.out.print("Dumping test suite to file \"" + filename + "\": ");
			for (ITestCase item : this) {
				SimpleTestCase c = (SimpleTestCase)item;
				
				int id = c.getTestCaseId();
				
				CBRInput input = (CBRInput)c.getTestInput();
				sb.setLength(0);
				sb.append(id).append('\t').append(input.size());
				for (TestContextVariable cv : input) {
					UserPreference up = cv.getUserPreference();
					sb.append('\t').append(up.getAccomType().toString());
					sb.append('\t').append(up.getRoomType().toString());
					sb.append('\t').append(up.getFacility());
					sb.append('\t').append(up.getPaymentMethod().toString());
					sb.append('\t').append(up.getRate());
					sb.append('\t').append(up.getGpsLocation());
					sb.append('\t').append(up.getTimestamp().getTime());
					sb.append('\t').append(up.getExpenditure());
					sb.append('\t').append(up.getCode().toString());
					sb.append('\t').append(up.getFoodStyle().toString());
					sb.append('\t').append(up.getProductCategory().toString());
					sb.append('\t').append(up.getFunType().toString());
					
					sb.append('\t').append(cv.getIsUserConfirmed() ? 1 : 0);
				}
				
				CBROracle oracle = (CBROracle)c.getTestOracle();
				sb.append('\t');
				if (oracle.size() > 0) {
					sb.append(oracle.get(0));
					for (int i = 1; i < oracle.size(); i++) {
						sb.append(',').append(oracle.get(i));
					}
				}
				
				sb.append('\n');
				bw.write(sb.toString());
				
				if (id % MonitorInterval == 0) System.out.print("[" + id + "] ");
			}
			
			System.out.println("Done.");
		
			bw.close();
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}
}
