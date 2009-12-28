package edu.cs.hku.instrument;

import soot.Unit;
import soot.tagkit.Tag;
import soot.tagkit.TagAggregator;

public class MyTagAggregator extends TagAggregator {

	@Override
	public String aggregatedName() {
		// TODO Auto-generated method stub
		return "MyTag";
	}

	@Override
	public void considerTag(Tag t, Unit u) {
		// TODO Auto-generated method stub
		units.add(u);
		tags.add(t);
	}

	@Override
	public boolean wantTag(Tag t) {
		// TODO Auto-generated method stub
		return (t instanceof MyCodeAttribute);
	}

}
