package dua.util;

import java.util.Comparator;

/** Generic comparator for objects based on the result of toString. */
public class StringBasedComparator<T> implements Comparator<T>{
	public int compare(T o1, T o2) {
		return o1.toString().compareTo(o2.toString());
	}
}
