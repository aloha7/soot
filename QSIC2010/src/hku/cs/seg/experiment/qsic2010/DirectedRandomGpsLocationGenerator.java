package hku.cs.seg.experiment.qsic2010;

import java.util.Random;

import com.hp.hpl.jena.query.extension.library.print;

public class DirectedRandomGpsLocationGenerator extends SimpleGpsLocationGenerator {
//	private static final double StartLongitude = 114.170324;	
//	private static final double StartLatitude = 22.277726;
	private static final double StartLongitude = 114.15;	
	private static final double StartLatitude = 22.28;
	private static final int Steptime = 10;
	private static final double MaxMovingSpeedDegree = 0.000009 * 30 * Steptime;
	
	private double m_Lon;
	private double m_Lat;
	
	private int m_CD;
	
	public DirectedRandomGpsLocationGenerator() {
		super(Steptime);
		m_Rand = new Random();
	}

	public void init() {
		super.init();
		
		m_Lon = StartLongitude;
		m_Lat = StartLatitude;
		m_CD = 0;
	}

	public void generate() {
		super.generate();
		
		double direction = m_Rand.nextDouble() * 0.5 * Math.PI - 0.25 * Math.PI;		
		double step = (m_Rand.nextDouble() * 0.5 + 0.5) * MaxMovingSpeedDegree;			
		m_Lon += step * Math.cos(direction);
		m_Lat += step * Math.sin(direction);
		
		if (step > MaxMovingSpeedDegree * 0.3) {
			m_CD++;
		}

	}

	public double getLatitude() {
		return m_Lat;
	}

	public double getLongitude() {
		return m_Lon;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		DirectedRandomGpsLocationGenerator o = new DirectedRandomGpsLocationGenerator();
		o.init();
		for (int i = 0; i < 10; i++) {
			System.out.println(o.getLongitude() + "," + o.getLatitude());
			o.generate();
		}

	}

	public int getContextDiversity() {
		return m_CD;
	}

}
