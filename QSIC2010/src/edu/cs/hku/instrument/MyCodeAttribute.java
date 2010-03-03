package edu.cs.hku.instrument;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import soot.tagkit.AttributeValueException;
import soot.tagkit.CodeAttribute;

public class MyCodeAttribute extends CodeAttribute {
	String value;
	
	public MyCodeAttribute(String value){
		this.value = value;
	}
	
	public String getName(){
		return "MyCodeAttribute";
	}
	
	public byte[] getValue() throws AttributeValueException{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(baos);
		
		try {
			dos.writeBytes(value);
			dos.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
	
}
