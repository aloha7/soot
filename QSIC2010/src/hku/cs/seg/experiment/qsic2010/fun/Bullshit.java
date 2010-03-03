package hku.cs.seg.experiment.qsic2010.fun;

import java.util.Hashtable;

public class Bullshit {
	private Hashtable<Integer, Integer> ht = null;
    public enum PaymentMethod {
        MasterCard, Visa, AmericanExpress, DinersClub, JBC, CUP, Cash, Octopus
    };
	
	public Bullshit() {
		ht = new Hashtable<Integer, Integer>();
	}
	
	public Hashtable<Integer, Integer> getHT() {
		return ht;
	}
}
