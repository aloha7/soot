package hku.cs.instrument;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.IntConstant;
import soot.jimple.InvokeExpr;
import soot.jimple.InvokeStmt;
import soot.jimple.Jimple;
import soot.jimple.StringConstant;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.ExceptionalBlockGraph;

class StmtInstrument extends BodyTransformer {
    private static StmtInstrument instance = new StmtInstrument();
    private StmtInstrument() {
    }

    public static StmtInstrument v() {
        return instance;
    }

   
    SootClass counterClass;
    SootMethod increaseCounter, reportCounter;
    

 
    protected void internalTransform(Body b, String phaseName, Map options) {
        counterClass = Scene.v().getSootClass("hku.cs.instrument.Counter");
        increaseCounter = counterClass.getMethod("void increase(java.lang.String,java.lang.String,int)");           
       
        ExceptionalBlockGraph bGraph = new ExceptionalBlockGraph(b);

        Iterator<Block> blockIt = bGraph.getBlocks().iterator();
        Local counterLocal = Jimple.v().newLocal("counterRef",
                counterClass.getType());
        b.getLocals().add(counterLocal);
        String methodName = b.getMethod().toString();
        String className = methodName.substring(1, methodName.indexOf(":"));
        methodName = methodName.substring(methodName.indexOf(":")+2, methodName.length()-1);
       
        while (blockIt.hasNext()) {
            Block block = (Block) blockIt.next();
            int blkIdx = block.getIndexInMethod();
                           
            List args = new ArrayList();
            args.add(StringConstant.v(className));
            args.add(StringConstant.v(methodName));
            args.add(IntConstant.v(blkIdx));
            InvokeExpr incExpr = Jimple.v().newStaticInvokeExpr(
                    increaseCounter.makeRef(), args);
            InvokeStmt incStmt = Jimple.v().newInvokeStmt(incExpr);
            Unit uh = block.getTail();
            incStmt.addTag(uh.getTag("LineNumberTag"));
           
            if (uh instanceof IdentityStmt) {
                block.insertAfter(incStmt, uh);
            }else{
                block.insertBefore(incStmt, uh);           
                uh.redirectJumpsToThisTo(incStmt);
            }
               
        }

    }
}