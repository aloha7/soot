package dua.cls;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class ClassTag implements Tag {
	public static final String TAG_NAME = "clsdflow";
	
	public String getName() { return TAG_NAME; }
	
	public byte[] getValue() throws AttributeValueException { return null; }
	
}
