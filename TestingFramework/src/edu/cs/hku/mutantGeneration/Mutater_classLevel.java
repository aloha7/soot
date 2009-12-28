package edu.cs.hku.mutantGeneration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.bcel.Constants;
import org.apache.bcel.classfile.ClassFormatException;
import org.apache.bcel.classfile.ClassParser;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ACONST_NULL;
import org.apache.bcel.generic.ARETURN;
import org.apache.bcel.generic.ATHROW;
import org.apache.bcel.generic.ArithmeticInstruction;
import org.apache.bcel.generic.ArrayInstruction;
import org.apache.bcel.generic.BIPUSH;
import org.apache.bcel.generic.ConstantPoolGen;
import org.apache.bcel.generic.DADD;
import org.apache.bcel.generic.DASTORE;
import org.apache.bcel.generic.DCMPG;
import org.apache.bcel.generic.DCONST;
import org.apache.bcel.generic.DDIV;
import org.apache.bcel.generic.DMUL;
import org.apache.bcel.generic.DNEG;
import org.apache.bcel.generic.DREM;
import org.apache.bcel.generic.DRETURN;
import org.apache.bcel.generic.DSTORE;
import org.apache.bcel.generic.DSUB;
import org.apache.bcel.generic.DUP;
import org.apache.bcel.generic.DUP2;
import org.apache.bcel.generic.FADD;
import org.apache.bcel.generic.FCMPG;
import org.apache.bcel.generic.FCONST;
import org.apache.bcel.generic.FDIV;
import org.apache.bcel.generic.FMUL;
import org.apache.bcel.generic.FNEG;
import org.apache.bcel.generic.FREM;
import org.apache.bcel.generic.FRETURN;
import org.apache.bcel.generic.FSUB;
import org.apache.bcel.generic.FieldInstruction;
import org.apache.bcel.generic.IADD;
import org.apache.bcel.generic.IAND;
import org.apache.bcel.generic.ICONST;
import org.apache.bcel.generic.IDIV;
import org.apache.bcel.generic.IFEQ;
import org.apache.bcel.generic.IFNE;
import org.apache.bcel.generic.IFNONNULL;
import org.apache.bcel.generic.IINC;
import org.apache.bcel.generic.IMUL;
import org.apache.bcel.generic.INEG;
import org.apache.bcel.generic.INVOKESTATIC;
import org.apache.bcel.generic.INVOKEVIRTUAL;
import org.apache.bcel.generic.IOR;
import org.apache.bcel.generic.IREM;
import org.apache.bcel.generic.IRETURN;
import org.apache.bcel.generic.ISHL;
import org.apache.bcel.generic.ISHR;
import org.apache.bcel.generic.ISUB;
import org.apache.bcel.generic.IUSHR;
import org.apache.bcel.generic.IXOR;
import org.apache.bcel.generic.IfInstruction;
import org.apache.bcel.generic.Instruction;
import org.apache.bcel.generic.InstructionFactory;
import org.apache.bcel.generic.InstructionHandle;
import org.apache.bcel.generic.InstructionList;
import org.apache.bcel.generic.InvokeInstruction;
import org.apache.bcel.generic.LADD;
import org.apache.bcel.generic.LAND;
import org.apache.bcel.generic.LASTORE;
import org.apache.bcel.generic.LCONST;
import org.apache.bcel.generic.LDIV;
import org.apache.bcel.generic.LMUL;
import org.apache.bcel.generic.LNEG;
import org.apache.bcel.generic.LOOKUPSWITCH;
import org.apache.bcel.generic.LOR;
import org.apache.bcel.generic.LREM;
import org.apache.bcel.generic.LRETURN;
import org.apache.bcel.generic.LSHL;
import org.apache.bcel.generic.LSHR;
import org.apache.bcel.generic.LSTORE;
import org.apache.bcel.generic.LSUB;
import org.apache.bcel.generic.LUSHR;
import org.apache.bcel.generic.LXOR;
import org.apache.bcel.generic.MethodGen;
import org.apache.bcel.generic.NOP;
import org.apache.bcel.generic.POP;
import org.apache.bcel.generic.POP2;
import org.apache.bcel.generic.PUTFIELD;
import org.apache.bcel.generic.PUTSTATIC;
import org.apache.bcel.generic.ReturnInstruction;
import org.apache.bcel.generic.SIPUSH;
import org.apache.bcel.generic.Select;
import org.apache.bcel.generic.StackConsumer;
import org.apache.bcel.generic.StoreInstruction;
import org.apache.bcel.generic.TABLESWITCH;
import org.apache.bcel.generic.TargetLostException;
import org.apache.bcel.generic.Type;

public class Mutater_classLevel {
	
	/*Set of methods to be ignored(i.e., never mutated)*/
	private Collection<String> mIgnoredMethods = new HashSet<String>();
	
	/*The most recent modification*/
	private String mModification = "";
	
	
	private static final int[] ICONST_MAP = new int[]{1, 1, 0, 3, 4, 5, -1};
	
	
	
	/*Table of mutable instructions. If defined and not a NOP this gives the mutated instruction to use*/
	private final Instruction[] mMutatable = new Instruction[256];{
		 mMutatable[Constants.IADD] = new ISUB();
		    mMutatable[Constants.ISUB] = new IADD();
		    mMutatable[Constants.IMUL] = new IDIV();
		    mMutatable[Constants.IDIV] = new IMUL();
		    mMutatable[Constants.IREM] = new IMUL();
		    mMutatable[Constants.IAND] = new IOR();
		    mMutatable[Constants.IOR] = new IAND();
		    mMutatable[Constants.IXOR] = new IAND();
		    mMutatable[Constants.ISHL] = new ISHR();
		    mMutatable[Constants.ISHR] = new ISHL();
		    mMutatable[Constants.IUSHR] = new ISHL();
		    mMutatable[Constants.LADD] = new LSUB();
		    mMutatable[Constants.LSUB] = new LADD();
		    mMutatable[Constants.LMUL] = new LDIV();
		    mMutatable[Constants.LDIV] = new LMUL();
		    mMutatable[Constants.LREM] = new LMUL();
		    mMutatable[Constants.LAND] = new LOR();
		    mMutatable[Constants.LOR] = new LAND();
		    mMutatable[Constants.LXOR] = new LAND();
		    mMutatable[Constants.LSHL] = new LSHR();
		    mMutatable[Constants.LSHR] = new LSHL();
		    mMutatable[Constants.LUSHR] = new LSHL();
		    mMutatable[Constants.FADD] = new FSUB();
		    mMutatable[Constants.FSUB] = new FADD();
		    mMutatable[Constants.FMUL] = new FDIV();
		    mMutatable[Constants.FDIV] = new FMUL();
		    mMutatable[Constants.FREM] = new FMUL();
		    mMutatable[Constants.DADD] = new DSUB();
		    mMutatable[Constants.DSUB] = new DADD();
		    mMutatable[Constants.DMUL] = new DDIV();
		    mMutatable[Constants.DDIV] = new DMUL();
		    mMutatable[Constants.DREM] = new DMUL();
		    mMutatable[Constants.IF_ACMPEQ] = new NOP();
		    mMutatable[Constants.IF_ACMPNE] = new NOP();
		    mMutatable[Constants.IF_ICMPEQ] = new NOP();
		    mMutatable[Constants.IF_ICMPGE] = new NOP();
		    mMutatable[Constants.IF_ICMPGT] = new NOP();
		    mMutatable[Constants.IF_ICMPLE] = new NOP();
		    mMutatable[Constants.IF_ICMPLT] = new NOP();
		    mMutatable[Constants.IF_ICMPNE] = new NOP();
		    mMutatable[Constants.IFEQ] = new NOP();
		    mMutatable[Constants.IFGE] = new NOP();
		    mMutatable[Constants.IFGT] = new NOP();
		    mMutatable[Constants.IFLE] = new NOP();
		    mMutatable[Constants.IFLT] = new NOP();
		    mMutatable[Constants.IFNE] = new NOP();
		    mMutatable[Constants.IFNONNULL] = new NOP();
		    mMutatable[Constants.IFNULL] = new NOP();
	}
	
	/**
	 * 
	 * @param methods
	 * @param methodidex
	 * @param className
	 * @param cpg
	 * @param count
	 * @return
	 */
	public int mutate(Method[] methods, int methodidex, final String className, final ConstantPoolGen cpg, int count){
		final Method m = methods[methodidex];
		if(count < 0 || !isMutatableMethod(m)){
			return 0;
		}
		final MethodGen mg = new MethodGen(m, className, cpg);
		final InstructionList il = mg.getInstructionList();
		final InstructionHandle[] ihs = il.getInstructionHandles();
		final InstructionFactory ifactory = new InstructionFactory(cpg);
		
		for(int j = 0; j < ihs.length; j ++){
			final Instruction i = ihs[j].getInstruction();
			final int points = isMutatable(ihs, j, cpg);
			if(points != 0 && (count -= points) < 0){
				//we can mutate it now
				final int lineNumber = (m.getLineNumberTable() == null ? 0: m.getLineNumberTable().getSourceLine(ihs[j].getPosition()));
				StringBuilder mod = new StringBuilder(className).append(":").append(lineNumber).append(":");
				if(i instanceof IfInstruction){
					ihs[j].setInstruction(((IfInstruction)i).negate());
					mod.append("negated conditional");
				}else if(i instanceof INEG || i instanceof DNEG || i instanceof FNEG || i instanceof LNEG){
					ihs[j].setInstruction(new NOP());
					mod.append("removed negation");
				}else if(i instanceof ArithmeticInstruction){
					final Instruction inew = mutateIntegerArithmetic((ArithmeticInstruction)i, cpg, ifactory);
					ihs[j].setInstruction(inew);
					mod.append(describe(i) + " -> " +  describe(inew));
				}else if(i instanceof ReturnInstruction){
					il.insert(ihs[j], mutateRETURN((ReturnInstruction)i, ifactory));
					mod.append(describe(i));
				}else if(i instanceof StoreInstruction){
					ihs[j].setInstruction(i instanceof DSTORE || i instanceof LSTORE ? new POP2(): new POP());
					mod.append("removed local assignment");
				}else if(i instanceof InvokeInstruction){
					mutateInvokeInstruction(cpg, il, ihs, j, i, mod);
				}else if( i instanceof PUTFIELD || i instanceof PUTSTATIC){
					final FieldInstruction fi = (FieldInstruction)i;
					final int size = fi.getFieldType(cpg).getSize();
					final InstructionList lil = new InstructionList();
					lil.append(size > 1 ? new POP2(): new POP()); //pop the value
					lil.append(new POP()); // pop the object reference
					final InstructionHandle ins =  ihs[j];
					il.insert(ins, lil);
					
					try {
						il.delete(ins);
					} catch (TargetLostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					mod.append("removed assignment to " + fi.getFieldName(cpg));
				}else if(i instanceof ArrayInstruction && i instanceof StackConsumer){
					//i.e., an array store
					final boolean big = i instanceof DASTORE|| i instanceof LASTORE;
					final InstructionList lil = new InstructionList();
					lil.append(big? new POP2(): new POP()); //pop values
					lil.append(new POP2()); // pop index and array reference
					final InstructionHandle ins = ihs[j];
					il.insert(ins, lil);
					try {
						il.delete(ins);
					} catch (TargetLostException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					mod.append("removed array assignment");
				}else if(i instanceof Select){
					final Select select = (Select)i;
					final int index = -1 - count;
					
					//
					final int[] matches = select.getMatchs();
					final InstructionHandle[] handles = select.getTargets();
					final InstructionHandle newDefHandle = handles[index];
					final InstructionHandle oldDefHandle = select.getTarget();
					if(newDefHandle == oldDefHandle){
						for(int k = 0; k < matches.length; k ++){
							if(k != index && newDefHandle != handles[k]){
								mod.append("switches case " + matches[index] + " with case " + k);
								handles[index] = handles[k];
								handles[k] = newDefHandle;
								if(select instanceof TABLESWITCH){
									ihs[j].setInstruction(new TABLESWITCH(matches, handles, oldDefHandle));
								}else{
									ihs[j].setInstruction(new LOOKUPSWITCH(matches, handles, oldDefHandle));
								}
								break;
							}
						}
						
						//still didn't find an option, just mutate the case value itself
						mod.append("switches case " + matches[index] + " -> " + matches[index]);
						if(select instanceof TABLESWITCH){
							ihs[j].setInstruction(new TABLESWITCH(matches, handles, oldDefHandle));
						}else{
							ihs[j].setInstruction(new LOOKUPSWITCH(matches, handles, oldDefHandle));
						}
					}else{
						handles[index] = oldDefHandle;
						mod.append("switched case " +  matches[index] + " with default case");
						if(select instanceof TABLESWITCH){
							ihs[j].setInstruction(new TABLESWITCH(matches, handles, newDefHandle));
						}else if(select instanceof LOOKUPSWITCH){
							ihs[j].setInstruction(new LOOKUPSWITCH(matches, handles, newDefHandle));
						}
					}
				}else{
					final Instruction inew;
					if(i instanceof ICONST){
						inew = mutateICONST((ICONST)i, cpg);
					}else if(i instanceof FCONST){
						inew = mutateFCONST((FCONST)i, cpg);
					}else if(i instanceof DCONST){
						inew = mutateDCONST((DCONST)i, cpg);
					}else if(i instanceof LCONST){
						inew = mutateLCONST((LCONST)i, cpg);
					}else if(i instanceof BIPUSH){
						inew = mutateBIPUSH((BIPUSH)i, cpg);
					}else if(i instanceof SIPUSH){
						inew = mutateSIPUSH((SIPUSH)i, cpg);
					}else if(i instanceof IINC){
						inew = mutateIINC((IINC)i, cpg);
					}else{
						inew = null;
					}
					if(inew != null){
						ihs[j].setInstruction(inew);
						mod.append(describe(i) + " -> " + describe(inew));
					}
				}
				mModification = mod.toString();
				break; //one seeded fault per version
			}
		}
		
		mg.setMaxLocals();
		methods[methodidex] = mg.getMethod();
		il.dispose();
		return count;
	}
	
	public int countMutablePoints(final Method m, final String className, final ConstantPoolGen cpg){
		if(!isMutatableMethod(m)){
			return 0;
		}
		
		final InstructionList il = new MethodGen(m, className, cpg).getInstructionList();
		final InstructionHandle[] ihs = il.getInstructionHandles();
		int count = 0;
		for(int i = 0; i < ihs.length; i ++){
			count += this.isMutatable(ihs, i, cpg);
		}
		return count;
	}
	
	public boolean isMutatableMethod(Method m){
		return (m != null && 
				!m.isAbstract() && !m.isInterface() && 
				!m.isNative() && !m.isSynthetic() && !mIgnoredMethods.contains(m.getName()));
	}
	
	
	
	public int isMutatable(final InstructionHandle[] ihs, final int offset, final ConstantPoolGen cpg){
		final Instruction i = ihs[offset].getInstruction();
		
		if(mMutatable[i.getOpcode()] == null){
			return 0;
		}
		
		//handle special situation of .class invocations
		if(i instanceof ICONST && offset + 1 < ihs.length){
			final Instruction context = ihs[offset+1].getInstruction();
			if(context instanceof INVOKESTATIC && "class".equals(((INVOKESTATIC)context).getMethodName(cpg))){
				return 0;
			}
		}
		
		if(i instanceof ICONST){
			if(offset >= 2 && ((ICONST)i).getValue().intValue() == 1 && checkAssertInstruction(cpg, ihs[offset-2].getInstruction())){
				return 0;
			}
			if(offset >= 4 && ((ICONST)i).getValue().intValue() == 0 && checkAssertInstruction(cpg, ihs[offset-4].getInstruction())){
				return 0;
			}			
		}
		if(i instanceof IFNE && offset >= 1 && checkAssertInstruction(cpg, ihs[offset - 1].getInstruction())){
			return 0;
		}
		
		
		//we can only mutate invokes that return void
		if(i instanceof InvokeInstruction && ((InvokeInstruction)i).getReturnType(cpg)!= Type.VOID){
			return 0;
		}
		
		//switches statement has multiple points
		if(i instanceof Select){
			return ((Select)i).getMatchs().length;
		}
		
		return 1;
	}
	
	
	public static boolean checkAssertInstruction(final ConstantPoolGen cpg, final Instruction ins){
		return ins instanceof INVOKEVIRTUAL && "desiredAssertionStatus".equals(((INVOKEVIRTUAL)ins).getMethodName(cpg));
	}
	
	private String describe(Instruction i){
		if(i instanceof IADD || i instanceof LADD || i instanceof DADD || i instanceof FADD){
			return "+";
		}
		if(i instanceof ISUB || i instanceof LSUB || i instanceof DSUB || i instanceof FSUB){
			return "-";
		}
		if(i instanceof IMUL || i instanceof LMUL || i instanceof DMUL || i instanceof FMUL){
			return "*";
		}
		if(i instanceof IDIV || i instanceof LDIV || i instanceof DDIV || i instanceof FDIV){
			return "/";
		}
		if(i instanceof IREM || i instanceof LREM || i instanceof DREM || i instanceof FREM){
			return "%";
		}
		if(i instanceof IOR || i instanceof LOR){
			return "|";
		}
		if(i instanceof IXOR || i instanceof LXOR){
			return "^";
		}
		if(i instanceof IAND || i instanceof LAND){
			return "&";
		}
		if(i instanceof ISHL || i instanceof LSHL){
			return "<<";
		}
		if(i instanceof ISHR || i instanceof LSHR){
			return ">>";
		}
		if(i instanceof IUSHR || i instanceof LUSHR){
			return ">>>";
		}
		if(i instanceof ICONST){
			return ((ICONST)i).getValue().toString();
		}
		if(i instanceof FCONST){
			return ((FCONST)i).getValue().toString();
		}
		if(i instanceof DCONST){
			return ((DCONST)i).getValue().toString();
		}
		if(i instanceof LCONST){
			return ((LCONST)i).getValue().toString();
		}
		if(i instanceof BIPUSH){
			final byte b = ((BIPUSH)i).getValue().byteValue();
			if(b >= ' ' && b <='~'){
				return (b + " (" + (char)b + ") ");
			}
			return ""+ b;
		}
		if(i instanceof SIPUSH){
			return ((SIPUSH)i).getValue().toString();
		}
		if(i instanceof ReturnInstruction){
			return "changed return value (" + i.getName() + ")";
		}
		
		if(i instanceof IINC){
			if(((IINC)i).getIncrement() >= 0){
				return "+=";
			}else{
				return "-=";
			}
		}
		return "unknown";
	}
	
	private void mutateInvokeInstruction(final ConstantPoolGen cpg, final InstructionList il, final InstructionHandle[] ihs, int j,
			final Instruction i, StringBuilder mod){
		final Type[] argTypes = ((InvokeInstruction)i).getArgumentTypes(cpg);
		final InstructionList lil = new InstructionList();
		
		//Pop all arguments (assume they are in reverse orders)
		for(int k = argTypes.length; k >= 0; k --){
			lil.append(argTypes[k].getSize() > 1? new POP2(): new POP());
		}
		final InstructionHandle ins = ihs[j];
		il.insert(ins, lil);
		
		try {
			il.delete(ins);
		} catch (TargetLostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mod.append("removed method call to " + ((InvokeInstruction)i).getMethodName(cpg));
	}
	
	private Instruction mutateIntegerArithmetic(final ArithmeticInstruction current, final ConstantPoolGen cp, final InstructionFactory ifactory){
		return mMutatable[current.getOpcode()];
	}
	
	private InstructionList mutateRETURN(final ReturnInstruction ret, final InstructionFactory ifactory){
		final InstructionList il = new InstructionList();
		if(ret instanceof IRETURN){
			final IFEQ ifeq = new IFEQ(null);
			il.append(ifeq);
			il.append(new ICONST(0));
			il.append(new IRETURN());
			il.append(new ICONST(1));
			ifeq.setTarget(il.getEnd());
		}else if(ret instanceof LRETURN){
			il.append(new LCONST(1));
			il.append(new LADD());
		}else if(ret instanceof FRETURN){
			 // The following is complicated by the problem of NaNs.  By default
		      // the new value is -(x + 1), but this doesn't work for NaNs.  But
		      // for a NaN x != x is true, and we use this to detect them.
			il.append(new DUP());
			il.append(new DUP());
			il.append(new FCMPG());
			final IFEQ ifeq = new IFEQ(null);
			il.append(ifeq);
			il.append(new POP());
			il.append(new FCONST(0));
			il.append(new FCONST(1));
			ifeq.setTarget(il.getEnd());
			il.append(new FADD());
			il.append(new FNEG());
		}else if(ret instanceof DRETURN){
			il.append(new DUP2());
			il.append(new DUP2());
			il.append(new DCMPG());
			final IFEQ ifeq = new IFEQ(null);
			il.append(ifeq);
			il.append(new POP2());
			il.append(new DCONST(0));
			il.append(new DCONST(1));
			ifeq.setTarget(il.getEnd());
			il.append(new DADD());
			il.append(new DNEG());
		}else if(ret instanceof ARETURN){
			final IFNONNULL ifnonnull = new IFNONNULL(null);
			il.append(ifnonnull);
			il.append(ifactory.createNew("java.lang.RuntimeException"));
			il.append(new DUP());
			il.append(ifactory.createInvoke("java.lang.RuntimeException", "<init>", Type.VOID, new Type[0], Constants.INVOKESPECIAL));
			il.append(new ATHROW());
			il.append(new ACONST_NULL());
			ifnonnull.setTarget(il.getEnd());
		}
		return il;
	}
	
	private static Instruction mutateICONST(final ICONST i, final ConstantPoolGen cpg){
		return new ICONST(ICONST_MAP[i.getValue().intValue() + 1]);
	}
	
	private static Instruction mutateFCONST(final FCONST i, final ConstantPoolGen cpg){
		final float v = i.getValue().floatValue();
		if( v == 0.0F){
			return new FCONST(1.0F);
		}else{
			return new FCONST(0.0F);
		}
	}
	
	private static Instruction mutateDCONST(final DCONST i, final ConstantPoolGen cpg){
		final double v = i.getValue().doubleValue();
		if(v == 0.0){
			return new DCONST(1.0);
		}else{
			return new DCONST(0.0);
		}
	}
	
	private static Instruction mutateLCONST(final LCONST i, final ConstantPoolGen cpg){
		final long v = i.getValue().longValue();
		if(v ==0L){
			return new LCONST(1L);
		}else{
			return new LCONST(0L);
		}
	}
	
	private static Instruction mutateBIPUSH(final BIPUSH i, final ConstantPoolGen cpg){
		return new BIPUSH((byte)(i.getValue().byteValue() + 1));
	}
	
	private static Instruction mutateSIPUSH(final SIPUSH i, final ConstantPoolGen cpg){
		return new SIPUSH((byte)(i.getValue().shortValue() + 1));
	}
	
	private static Instruction mutateIINC(final IINC i, final ConstantPoolGen cpg){
		return new IINC(i.getIndex(), - i.getIncrement());
	}
	
	/**
	 * 
	 * @param className:the file name without accurate path(e.g., trivia.Mover)
	 * @param mutantDir: the directory to save mutant versions
	 */
	private void mutateClass(String className, String mutantDir){
		String fileName = System.getProperty("user.dir") + edu.cs.hku.util.Constants.FS + 
		"bin" + edu.cs.hku.util.Constants.FS +className.replace('.', '\\') + ".class";
		File classFile  = new File(fileName);
		
		if(mutantDir.equals("")){
			mutantDir = classFile.getParent();
		}
		try {
			JavaClass jc = new ClassParser(fileName).parse();			
			
			Method[] methods = jc.getMethods();			
			ConstantPoolGen cpg = new ConstantPoolGen(jc.getConstantPool());
			
			StringBuilder sb = new StringBuilder();
			int totalMutants = 0;
			for(int i = 0; i < methods.length; i ++){
				totalMutants += this.countMutablePoints(methods[i], className, cpg);	
			}
			
			String lastModification = "";
			for(int j = 0; j < totalMutants; j ++){
				int count = j;
				for(int i = 0; i < methods.length; i ++){
					JavaClass jc_copy = jc.copy();	
					
					count = this.mutate(jc_copy.getMethods(), i, className, 
							new ConstantPoolGen(jc_copy.getConstantPool()), count);
					
					if(!this.mModification.equals(lastModification)){// seed a mutation
						jc_copy.setConstantPool(cpg.getFinalConstantPool());
						String mutantFile = mutantDir + edu.cs.hku.util.Constants.FS +
											className.substring(className.indexOf(".") + ".".length()) + j + ".class"; 
						jc_copy.dump(mutantFile);						
						sb.append(j + ":" + this.mModification + edu.cs.hku.util.Constants.LS);
						lastModification = this.mModification;
					}	
					
					if(count < 0 ){
						break;
					}
				}	
			}
			
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(mutantDir + 
					edu.cs.hku.util.Constants.FS + "Mutants.txt"));
			bw.write(sb.toString());
			bw.close();
			
		} catch (ClassFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	public void runMain(String[] args){
		if(args.length > 0){
			final Set<String> s = new HashSet<String>();
			s.addAll(Arrays.asList(args));	
			for(String className: s){
				this.mutateClass(className,"");
			}
		}
	}
	
	public static void main(String[] args){
		new Mutater_classLevel().runMain(args);
	}
	
}
