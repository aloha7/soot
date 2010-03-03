package hku.cs.seg.experiment.qsic2010;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;

public class LocListFileGpsLocationGenerator extends SimpleGpsLocationGenerator {
	private static String GpxFilename = "C:\\Jack\\workspace\\QSIC2010_PRI\\gpstrace.gpx";
	private double m_Lon;
	private double m_Lat;
	private int m_CD;
	private ArrayList<double[]> m_Locations;
	private HashSet<Integer> m_Selected;
	private Random m_Rand;
	
	public LocListFileGpsLocationGenerator() {
		m_Locations = new ArrayList<double[]>();
		m_Selected = new HashSet<Integer>();
		m_Rand = new Random();
		initFromGpx(GpxFilename);
	}
	
	private void initFromGpx(String filename) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));			
			String str = null;			
			while ((str = br.readLine()) != null) {
				if (str.equals("")) break;
				int loc1 = str.indexOf("<trkpt lat=\"");
				if (loc1 < 0) continue;
				int loc2 = str.indexOf("\" lon=\"");
				int loc3 = str.indexOf("\">");
				
				double[] pair = new double[2];
				pair[0] = Double.parseDouble(str.substring(loc2 + 7, loc3));
				pair[1] = Double.parseDouble(str.substring(loc1 + 12, loc2));
				
				m_Locations.add(pair);
			}
			
			generate();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void generate() {
		super.generate();
		
		int index = 0;
		do {
			index = m_Rand.nextInt(m_Locations.size());
		} while (m_Selected.contains(index));
		
		m_Lon = m_Locations.get(index)[0];
		m_Lat = m_Locations.get(index)[1];
		
		m_Selected.add(index);
	}

	public int getContextDiversity() {
		return m_CD;
	}

	public double getLatitude() {
		return m_Lat;
	}

	public double getLongitude() {
		return m_Lon;
	}

	public void init() {
		super.init();
		
		m_CD = 0;
		m_Selected.clear();
	}

	public static void main(String[] args) {
		GpxGpsLocationGenerator o = new GpxGpsLocationGenerator();
		o.init();
		for (int i = 0; i < 10; i++) {
			System.out.println(o.getLongitude() + "," + o.getLatitude());
			o.generate();
		}

	}

}
