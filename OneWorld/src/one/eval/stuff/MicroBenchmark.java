/*
 * ========================================================================
 *  Copyright 1998, 1999 Robert Grimm.
 *  All rights reserved.
 *  See COPYRIGHT file for a full description.
 * ========================================================================
 */

package one.eval.stuff;

import one.eval.Applicable;
import one.eval.BadPairStructureException;
import one.eval.BadSyntaxException;
import one.eval.BindingException;
import one.eval.Data;
import one.eval.Environment;
import one.eval.HashEnvironment;
import one.eval.Pair;
import one.eval.SimpleEnvironment;
import one.eval.Symbol;


/**
 *
 *
 * @author   &copy; Copyright 1998, 1999 by Robert Grimm.
 *           Written by <a href="mailto:rgrimm@alum.mit.edu">Robert
 *           Grimm</a>.
 * @version  1.0
 */
public class MicroBenchmark {

  public static void main(String[] argv) {

    try {

      Environment env = new HashEnvironment();
      //2010-03-27:
//      Data.install(env, "scheme-report");
      Data.install(env);
      
      Applicable car = (Applicable)env.lookup(Symbol.intern("car"));
      Pair       p;

      long start;
      long end;

      p = Pair.cons(null, Pair.cons(null, Pair.cons(null, null)));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 1 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.length();
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 2 : " + (end - start));

      p = Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, null))))))))));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 3 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.length();
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 4 : " + (end - start));

      Environment env2 = new SimpleEnvironment(env);
      //2010-03-27:
//      Data.install(env2, "extensions");
      Data.install(env2);
      
      env2 = new SimpleEnvironment(env2);
      //2010-03-27:
//      Data.install(env2, "extensions");
      Data.install(env2);
      
      //2010-03-27:
      env2 = new SimpleEnvironment(env2);
//      Data.install(env2, "extensions");
      Data.install(env2);
      
      Symbol s = Symbol.intern("car");

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<1000000; i++) {
        env2.lookup(s);
      }
      end = System.currentTimeMillis();
      System.out.println("test 5 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<1000000; i++) {
        env.lookup(s);
      }
      end = System.currentTimeMillis();
      System.out.println("test 6 : " + (end - start));

      MicroBenchmark t = new MicroBenchmark(s, env);

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<1000000; i++) {
        t.lookup();
      }
      end = System.currentTimeMillis();
      System.out.println("test 7 : " + (end - start));

      

      p = Pair.cons(null, null);

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 8 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.length();
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 9 : " + (end - start));


      p = Pair.cons(null, Pair.cons(null, Pair.cons(null, null)));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 10 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        try {
          p.length();
        } catch (BadPairStructureException x) {
          throw new BadSyntaxException("blah", p);
        }
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 11 : " + (end - start));

      p = Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, Pair.cons(null, null))))))))));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 12 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        try {
          p.length();
        } catch (BadPairStructureException x) {
          throw new BadSyntaxException("blah", p);
        }
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 13 : " + (end - start));

      p = Pair.cons(null, null);

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        p.isList();
        p.length();
      }
      end = System.currentTimeMillis();
      System.out.println("test 14 : " + (end - start));

      System.gc();
      start = System.currentTimeMillis();
      for (int i=0; i<10000000; i++) {
        try {
          p.length();
        } catch (BadPairStructureException x) {
          throw new BadSyntaxException("blah", p);
        }
        car.getMinArgs();
        car.getMaxArgs();
      }
      end = System.currentTimeMillis();
      System.out.println("test 15 : " + (end - start));

    } catch (Throwable x) {
      System.out.println("error " + x.toString());
    }
  }

  Symbol s;
  Environment env;

  MicroBenchmark(Symbol s, Environment env) {
    this.s = s;
    this.env = env;
  }

  public Object lookup() throws BindingException {
    return env.lookup(s);
  }

}
