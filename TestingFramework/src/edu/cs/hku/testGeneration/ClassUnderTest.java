package edu.cs.hku.testGeneration;

import static cs.hku.hk.testGeneration.Assertion.check;
import static cs.hku.hk.testGeneration.Assertion.isNonNeg;
import static cs.hku.hk.testGeneration.Assertion.notNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ClassUnderTest<T> {
	protected Class<T> wrappedClass = null;
	
	protected ClassUnderTest<T>[] children; //receiver and param types, constructor and method nodes 
	protected BigInteger[] childSizes;
	protected BigInteger planSpaceSize  =BigInteger.ZERO;
	protected BigInteger[] childRanges;
	
	protected ClassUnderTest<T>[] parameters;//parameters for a specified method
	protected BigInteger[] paramSizes;
	protected BigInteger[] canonicalSubSpaceSizes; 
	protected String testBlockSpaces = 
	public ClassUnderTest(){
		
	}
	
	public ClassUnderTest(final ClassWrapper<T> pCW, int remainingRecursion){
		notNull(pCW);
		check(remainingRecursion >= 0);
		
		pCW.setIsNeeded();
		
		String name = pCW.getWrappedClass().getName();
		final List<ClassUnderTest<T>> childSpaces = new ArrayList<ClassUnderTest<T>>();
		childSpaces.add(new LeafNode<T>(pCW.getPresetPlans()));
		
		/* functions only iff wanted and additional chaining allowed */
		if(remainingRecursion > 0){
			//for class and all its children(implementing/extending children)
			final List<Class<? extends T>> classes =
				new ArrayList<Class<? extends T>>(pCW.getChildren());
			classes.add(pCW.getWrappedClass());
			
			for(Class<? extends T> c: classes){
				ClassWrapper<?> cw = ClassAnalyzer.getInstance().getWrapper(c);
				notNull(cw);
				
				if(!cw.isLibraryType()){
					for(Constructor<?> con: cw.getConstrs()){
						childSpaces.add(new ConstructorNode(con, remainingRecursion));
					}
				}
				
				for(Method meth:cw.getConMeths()){
					childSpaces.add(new MethodNode(meth, remainingRecursion));
				}
			}
			
			
			
		}
	}
	
	public ClassUnderTest(final Class<T> c, int remainingRecursion){
		notNull(c);
		check(remainingRecursion >0);
		wrappedClass = c;
		
		List<ClassUnderTest<?>> childSpaces = new ArrayList<ClassUnderTest<?>>();
		
		//Crash any declared public constructor iff class non-abstract
		if(Modifier.isAbstract(c.getModifiers()) == false){
			for(Constructor<T> con: c.getDeclaredConstructors()){
				childSpaces.add(new ConstructorNode<T>(con, remainingRecursion));
			}
		}
		
		//Crash any declared public non-abstract method
		for(Method meth: c.getDeclaredMethods()){
			if(!Modifier.isAbstract(meth.getModifiers())){
				childSpaces.add(new MethodNode(meth, remainingRecursion));
			}
		}
		
		setChildren(childSpaces.toArray(new ClassUnderTest[childSpaces.size()]));
	}
	
	public Block<?> getBlock(BigInteger planIndex){
		Block<?> res = null;
		
		int child = getChildIndex(planIndex);
		ClassUnderTest<?> node = (ClassUnderTest<?>)children[child];
		BigInteger childPlanIndex = getChildPlanIndex(child, planIndex);
		
		Expression<?>[] paramPlans = node.getParamPlans(childPlanIndex, wrappedClass);
		
		if(node instanceof ConstructorNode){
			ConstructorNode<T> conNode = (ConstructorNode<T>)node;
			res = getTestBlockForCon(conNode.getCon(), paramPlans);
		}else if(node instanceof MethodNode){
			MethodNode<?> methNode = (MethodNode<?>)node;
			res = getTestBlockForMeth(methNode.getMeth(), paramPlans);
		}
		return notNull(res);
	}
	
	public Block<?> getTestBlockForCon(Constructor<T> pCon, Expression<?>[] curPlans){
		notNull(pCon);
		notNull(curPlans);
		
		final Class<T> testeeType = pCon.getDeclaringClass();
		final Block<?> b = new Block(testeeType, pCon, testBlockSpaces);
		
		final Block<?>[] bs = new Block[curPlans.length + 1];
		
		final Variable<?>[] ids = new Variable[curPlans.length];
		Class<?>[] paramsTypes = pCon.getParameterTypes();
		
		for(int i = 0; i < curPlans.length; i ++){
			ids[i] = b.getNextID(paramsTypes[i]);
			bs[i] = new LocalVariableDeclarationStatement(ids[i], curPlans[i]);
		}
		
		ConstructorCall<T> conPlan = null;
		if(ClassAnalyzer.getInstance().getWrapper(pCon.getDeclaringClass()).isInnerClass()){
			Expression[] paramPlans = new Expression[curPlans.length -1];
			for(int j = 0; j < paramPlans.length; j ++){
				paramPlans[j] = ids[j + 1];
			}
			conPlan = new ConstructorCall<T>(testeeType, pCon, paramPlans, ids[0]);
		}else{
			conPlan = new ConstructorCall<T>(testeeType, pCon, ids);
		}
		
		//Last statement
		bs[curPlans.length] = new ExpressionStatement<T>(conPlan);
		
		List<Block> blockStatements = new LinkedList<Block>();
		for(Block blockStatement: bs){
			blockStatements.add(blockStatement);
		}
		
		b.setBlockStmts(blockStatements);
		
		return b;
	}
	
	public Block<?> getTestBlockForMeth(Method pMeth, Expression<?>[] curPlans){
		notNull(pMeth);
		notNull(curPlans);
		
		final Class<T> testeeType = (Class<T>)pMeth.getDeclaringClass();
		final Block<?> b = new Block(testeeType, pMeth, testBlockSpaces);
		Block<?>[] bs = new Block[curPlans.length + 1];
		
		Expression<?>[] paramPlans = null;
		if(Modifier.isStatic(pMeth.getModifiers()) == false){
			paramPlans = new Expression[curPlans.length - 1];
			for(int j = 0; j < paramPlans.length; j ++){
				paramPlans[j] = curPlans[j + 1];
			}
		}else{
			paramPlans = curPlans;
		}
		
		Class<?>[] paramsTypes = pMeth.getParameterTypes();
		Variable<?>[] paramIDs = new Variable[paramPlans.length];
		for(int i = 0; i < paramIDs.length; i ++){
			paramIDs[i]=b.getNextID(paramsTypes[i]);
			bs[i] = new LocalVariableDeclarationStatement(paramIDs[i], paramPlans[i]);
		}
		
		MethodCall<?> conPlan = null;
		if(Modifier.isStatic(pMeth.getModifiers()) == false){
			//first dimension is receiver instance
			Variable<?> vID = b.getNextID(pMeth.getDeclaringClass());
			bs[curPlans.length - 1] = new LocalVariableDeclarationStatement(vID, curPlans[0]);
			conPlan = new MethodCall(testeeType, pMeth, paramIDs, vID);
		}else{
			conPlan = new MethodCall(testeeType, pMeth, paramIDs);
		}
		
		//last statement
		bs[curPlans.length] = new ExpressionStatement(conPlan);
		
		List<Block> blockStatements = new LinkedList<Block>();
		for(Block blockStatement: bs){
			blockStatements.add(blockStatement);
		}
		
		b.setBlockStmts(blockStatements);
		
		
		
		
	}
	
	protected void setChildren(final ClassUnderTest<T>[] pChildren){
		this.children = pChildren;
	}
	
	protected ClassUnderTest<T>[] getChildren(){
		return this.children;
	}
	
	public BigInteger getPlanSpaceSize(){
		notNull(children);
		
		if(planSpaceSize != null)
			return planSpaceSize;
		
		childSizes = new BigInteger[children.length];
		childRanges = new BigInteger[children.length];
		for(int i = 0; i < children.length; i ++){
			childSizes[i] = children[i].getPlanSpaceSize();
			planSpaceSize = planSpaceSize.add(childSizes[i]);
			childRanges[i] = planSpaceSize.subtract(BigInteger.ONE);
		}
		
		return planSpaceSize;
	};
	
	
	protected int getChildIndex(BigInteger planIndex){
		check(isNonNeg(planIndex));
		check(planIndex.compareTo(getPlanSpaceSize()) < 0);
		
		for(int i = 0; i < childRanges.length; i ++){
			if(planIndex.compareTo(childRanges[i]) < 0){
				return i;
			}
		}
		throw new IllegalStateException("No correct subrange to locate a child");
	}
	
	protected BigInteger getChildPlanIndex(int childIndex, BigInteger planIndex){
		BigInteger offsetPrevChildren = BigInteger.ZERO;
		
		if(childIndex > 0){
			offsetPrevChildren = childRanges[childIndex -1].add(BigInteger.ONE);
		}
		return planIndex.subtract(offsetPrevChildren);
	}
	
	public Expression<? extends T> getPlan(BigInteger planIndex, Class<?> testeeType){
		check(isNonNeg(planIndex));
		check(planIndex.compareTo(getPlanSpaceSize()) < 0);
		
		int child = getChildIndex(planIndex);
		BigInteger childPlanIndex = getChildPlanIndex(child, planIndex);
		String type = testeeType.getName();
		if(type.equals("Constructor")){
			Expression<?>[] depPlans = getParamPlans(planIndex, testeeType);
			for(Constructor con:testeeType.getConstructors()){
				if((ClassAnalyzer.getInstance().getWrapper(con.getDeclaringClass())).isInnerClass()){
					//for inner-class
					check(depPlans.length >= 1);
					
					Expression<?>[] paramPlans = new Expression[depPlans.length -1];
					for(int j = 0; j < paramPlans.length; j ++){
						paramPlans[j] = depPlans[j + 1];
					}
					return new ConstructorCall<T>(testeeType, con, paramPlans, depPlans[0]);
				}
				//non-inner class constructor with >=0 arguments
				return new ConstructorCall<T>(testeeType, con, depPlans);
			}
		}else if(type.equals("Method")){
			Expression<?>[] depPlans = getParamPlans(planIndex, testeeType);
			Method[] methods = testeeType.getDeclaredMethods();
			for(Method meth: methods){
				if(depPlans.length == 0){
					//non-args static meth
					return new MethodCall<T>(testeeType, meth, new Expression[0]);
				}
				check(depPlans.length > 0); //at least one dimension non-empty
				if(Modifier.isStatic(meth.getModifiers()) == false){ //first dimension is receiver
					Expression<?>[] paramPlans = new Expression[depPlans.length - 1];
					for(int j = 0; j < paramPlans.length; j ++){
						paramPlans[j] = depPlans[j + 1];
					}
					return new MethodCall<T>(testeeType, meth, paramPlans, depPlans[0]);
				}
				
				//Non-static method with >=1 arguments
				return new MethodCall<T>(testeeType, meth, depPlans);
			}
		}
		
		return children[child].getPlan(childPlanIndex, testeeType);
	}
	
	protected void setParams(ClassUnderTest<T>[] pChildren){
		this.parameters = pChildren;
	}
	
	protected Expression<?>[] getParamPlans(BigInteger planIndex, Class<?> testeeType){
		check(isNonNeg(planIndex));
		check(planIndex.compareTo(getPlanSpaceSize()) < 0);
		
		ClassUnderTest<?>[] res = new ClassUnderTest<?>[parameters.length];
		BigInteger currentIndex = planIndex;
		for(int i = 0; i < res.length; i ++){
			BigInteger childIndex = currentIndex.divide(canonicalSubSpaceSizes[i]);
			res[i] = parameters[i].getPlan(childIndex, testeeType);
			
			currentIndex = currentIndex.subtract(childIndex.multiply(canonicalSubSpaceSizes[i]));
		}
		
		return res;
	}
}
