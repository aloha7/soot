package edu.cs.hku.instrument;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;

import soot.tagkit.AttributeValueException;
import soot.tagkit.Tag;

public class MyTag implements Tag {

	String value;
	public MyTag(String value){
		this.value = value;
	}
	public String getName() {
		// TODO Auto-generated method stub
		return "MyTag";
	}

	public byte[] getValue() throws AttributeValueException {
		// TODO Auto-generated method stub
		ByteArrayOutputStream baos = new ByteArrayOutputStream(4);
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
