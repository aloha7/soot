package dua.util;

public class Pair<T1,T2> {
	private T1 first;
	private T2 second;
	
	public T1 first() { return first; }
	public T2 second() { return second; }
	
	public Pair(T1 first, T2 second) { this.first = first; this.second = second; }
	
	@Override
	public String toString() { return "<" + ((first == null)? "null" : first.toString()) + "," + ((second == null)? "null" : second.toString()) + ">"; }
	
	@Override
	public int hashCode() { return ((first == null)? 0 : first.hashCode()) + ((second == null)? 0 : second.hashCode()); }
	
	@Override
	public boolean equals(Object obj) {
		Pair<T1,T2> other = (Pair<T1,T2>) obj;
		return ((first == null)? other.first == null : first.equals(other.first)) &&
			   ((second == null)? other.second == null : second.equals(other.second));
	}
	
}
