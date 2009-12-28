package edu.cs.hku.testGeneration;

import static edu.cs.hku.testGeneration.Assertion.notNull;
import static edu.cs.hku.testGeneration.Constants.FS;
import static edu.cs.hku.testGeneration.Constants.LS;
import static edu.cs.hku.testGeneration.Constants.TAB;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.util.ArrayList;
import java.util.List;

public class Block<T> extends AbstractExpression<T>{
	//method or constructor this block is intended to execute
	protected Member member; //testee, a.sum(), then sum() is a testee
	protected Class<?> testeeType; //type for member
	protected String spaces;
	
	
	//Block ::= (Block)* Sequence of statements, inteneded to crash a method/constructor
	protected final List<Block> blocks = new ArrayList<Block>();
	
	//strictly monotonic increasing counter of local identifiers
	protected int localIDcount = 0;
	
	public Block(){
		
	}
	
	
	public Block(Class<?> testeeType, Member m, String spaces){
		this.testeeType = notNull(testeeType);
		this.member = notNull(m);
		this.spaces = notNull(spaces);
	}
	
	
	public Member getTestee(){
		return notNull(member);
	}
	
	public <V> Variable<V> getNextID(final Class<V> pClass){
		notNull(pClass);
		localIDcount += 1;
		if(pClass.equals(Void.class)){
			return new Variable<V>(pClass, testeeType, "<void" + 
					Integer.toString(localIDcount) + ">");
		}
		
		//Leaf type represents array
		Class<?> leafType = pClass;
		while(leafType.isArray()){
			leafType = leafType.getComponentType();
		}
		
		//Simple name
		String leafName = leafType.getName();
		if(leafType.isPrimitive() == false){
			if(leafType.getDeclaringClass() != null){
				//remove p.Enc$ from Enc$Nested in class p.Enc.Nested
				String enclosingName = leafType.getDeclaringClass().getName();
				leafName = leafName.substring(enclosingName.length() + 1, leafName.length());
			}else{
				//top level class
				String[] leafNameParts = leafName.split("\\.");
				leafName = leafNameParts[leafNameParts.length - 1];
			}
		}
		
		String id = leafName.toLowerCase().charAt(0) + Integer.toString(localIDcount);
		Variable<V> res = new Variable<V>(pClass, testeeType, id);
		
		return notNull(res);
	}
	
	public void setBlockStmts(final List<Block> blocks){
		notNull(blocks);
		this.blocks.clear();
		this.blocks.addAll(blocks);
	}
	
	public T execute() throws InstantiationException, 
	IllegalAccessException, InvocationTargetException{
		Object result = null;
		for(Block block: blocks){
			result = block.execute();
		}
		return ((T)result);
	};
	
	public String text(){
		StringBuilder sb = new StringBuilder("(");
		
		//Get sequences of stmt strings
		for(Block block: blocks){
			sb.append(LS);
			sb.append(spaces + TAB + block.text());
		}
		sb.append(LS);
		sb.append(spaces + "}");
		return sb.toString();
	};
	
	public String toString(){
		return text();
	}
	
}
