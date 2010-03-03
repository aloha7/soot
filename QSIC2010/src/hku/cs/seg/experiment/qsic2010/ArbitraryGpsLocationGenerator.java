package hku.cs.seg.experiment.qsic2010;

public class ArbitraryGpsLocationGenerator extends SimpleGpsLocationGenerator {

	private static final double StartLongitudeMin = 114.13987;	
	private static final double StartLongitudeMax = 114.193532;	
	private static final double StartLatitudeMin = 22.274512;
	private static final double StartLatitudeMax = 22.287851;
	private static final int Steptime = 10;
	private static final double MaxMovingSpeedDegree = 0.000009 * 30 * Steptime;
	private static final double MaxDeltaDirection = Math.PI / 2;
	
	private double m_Direction;
	
	private double m_Lon;
	private double m_Lat;
	private int m_CD;
	
	@Override
	public void generate() {
		super.generate();
		
		double step = (m_Rand.nextDouble() * 0.5 + 0.5) * MaxMovingSpeedDegree;		
		m_Direction += m_Rand.nextDouble() * 2 * MaxDeltaDirection - MaxDeltaDirection;
		if (m_Direction > Math.PI) {
			m_Direction -= 2 * Math.PI;
		} else if (m_Direction < -Math.PI) {
			m_Direction += 2 * Math.PI;
		}
			
		m_Lon += step * Math.cos(m_Direction);
		m_Lat += step * Math.sin(m_Direction);
		
		if (step > MaxMovingSpeedDegree * 0.3) {
			m_CD++;
		}
	}
	
	@Override
	public void init() {
		super.init();
		
		m_Lon = m_Rand.nextDouble() * (StartLongitudeMax - StartLongitudeMin) + StartLongitudeMin;
		m_Lat = m_Rand.nextDouble() * (StartLatitudeMax - StartLatitudeMin) + StartLatitudeMin;
		
		m_Direction = m_Rand.nextDouble() * 2 * Math.PI - Math.PI;
		
		m_CD = 0;
	}
	
	@Override
	public int getContextDiversity() {
		return m_CD;
	}

	@Override
	public double getLatitude() {
		return m_Lat;
	}

	@Override
	public double getLongitude() {
		return m_Lon;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ArbitraryGpsLocationGenerator o = new ArbitraryGpsLocationGenerator();
		o.init();
		for (int i = 0; i < 10; i++) {
			System.out.println(o.getLongitude() + "," + o.getLatitude());
			o.generate();
		}
	}

}
