package hku.cs.seg.experiment.core;

import java.util.Hashtable;

public class ExecutionTimer {
	private Hashtable<String, Long> m_TimeDurations;
	private Hashtable<String, Long> m_LastStarts;
	private boolean m_IsSwitchedOn;
	
	public ExecutionTimer() {
		m_TimeDurations = new Hashtable<String, Long>();
		m_LastStarts = new Hashtable<String, Long>();
		m_IsSwitchedOn = true;
	}
	
	public void SwitchOn() { m_IsSwitchedOn = true; }
	public void SwitchOff() { m_IsSwitchedOn = false; }
	
	public long startCounter(String name) {
		if (!m_IsSwitchedOn) return -1;
		long now = System.currentTimeMillis();
		long rval = endCounter(name);		
		m_LastStarts.put(name, now);
		return rval;
	}
	
	public long endCounter(String name) {
		if (!m_IsSwitchedOn) return -1;
		long now = System.currentTimeMillis();
		if (!m_TimeDurations.containsKey(name) && !m_LastStarts.containsKey(name)) {
			return -1;
		}
		long duration = 0;
		if (m_TimeDurations.containsKey(name)) {
			duration = m_TimeDurations.get(name);
		}
		if (m_LastStarts.containsKey(name)) {
			long start = m_LastStarts.get(name);
			long val = duration + now - start;
			m_TimeDurations.put(name, val);
			m_LastStarts.remove(name);
			return val;
		} else {
			return duration;
		}
	}
	
	public long getDuration(String name) {
		if (!m_IsSwitchedOn) return -1;
		long now = System.currentTimeMillis();
		if (!m_TimeDurations.containsKey(name)) return -1;
		long duration = m_TimeDurations.get(name);
		if (m_LastStarts.containsKey(name)) {
			return duration + now - m_LastStarts.get(name);			
		} else {
			return duration;
		}		
	}
	
	public long clear(String name) {
		long rval = getDuration(name);
		if (m_TimeDurations.containsKey(name)) {
			m_TimeDurations.remove(name);
		}
		if (m_LastStarts.containsKey(name)) {
			m_LastStarts.remove(name);
		}
		return rval;
	}
	
	public void clearAll() {
		m_TimeDurations.clear();
		m_LastStarts.clear();
	}
	
	private static ExecutionTimer Me;
	public static ExecutionTimer me() {
		if (Me == null) {
			Me = new ExecutionTimer();
		}
		return Me;
	}
}
