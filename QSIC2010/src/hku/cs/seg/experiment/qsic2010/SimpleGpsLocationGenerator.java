package hku.cs.seg.experiment.qsic2010;

import java.sql.Timestamp;
import java.util.Random;

public abstract class SimpleGpsLocationGenerator implements IGpsLocationGenerator {

	private Timestamp m_Timestamp = null; 
	protected int m_StepTime = 20;
	protected Random m_Rand;
	
	protected SimpleGpsLocationGenerator() {
		m_Rand = new Random();
	}
	
	protected SimpleGpsLocationGenerator(int stepTime) {
		this();
		m_StepTime = stepTime;
	}

	public void generate() {
		m_Timestamp.setTime(m_Timestamp.getTime() + m_StepTime * 1000);
	}
	
	public void init() {
		m_Timestamp = new Timestamp(2009, 12, 25, m_Rand.nextInt(12), m_Rand.nextInt(60), m_Rand.nextInt(60), m_Rand.nextInt(1000000000)); 
	}
	
	public Timestamp getTimestamp() {
		return m_Timestamp;
	}
	
	public abstract int getContextDiversity();
	public abstract double getLatitude();
	public abstract double getLongitude();

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
