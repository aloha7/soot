package tests;

/* JUnit test case generated automatically by CUTE */
import junit.framework.*;

public class tests_BSTree_main_Test extends TestCase implements cute.Input {
    private Object[] input;
    private int i;

    public tests_BSTree_main_Test(String name){
        super(name);
    }

    public boolean Boolean() {
        return ((Boolean)input[i++]).booleanValue();
    }

    public short Short() {
        return ((Short)input[i++]).shortValue();
    }

    public int Integer() {
        return ((Integer)input[i++]).intValue();
    }

    public long Long() {
        return ((Long)input[i++]).longValue();
    }

    public float Float() {
        return ((Float)input[i++]).floatValue();
    }

    public double Double() {
        return ((Double)input[i++]).doubleValue();
    }

    public char Character() {
        return ((Character)input[i++]).charValue();
    }

    public byte Byte() {
        return ((Byte)input[i++]).byteValue();
    }

    public Object Object(String type) {
        return input[i++];
    }

    public Object ObjectShallow(String type) {
        return input[i++];
    }
    public void test1(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

    public void test2(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

    public void test6(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(1);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(1);
        input[i++] = new Integer(0);
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

    public void test7(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(1);
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

    public void test9(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(1);
        input[i++] = new Integer(1);
        input[i++] = new Integer(1);
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

    public void test15(){
        i=0;
        input = new Object[9];
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(0);
        input[i++] = new Integer(2);
        input[i++] = null;
        i=0;
        cute.Cute.input = this;
        tests.BSTree.main(null);
    }

}
