/*
 * Copyright (c) 2001, University of Washington, Department of
 * Computer Science and Engineering.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following
 * disclaimer in the documentation and/or other materials provided
 * with the distribution.
 *
 * 3. Neither name of the University of Washington, Department of
 * Computer Science and Engineering nor the names of its contributors
 * may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package one.world.io;

import one.fonda.*;
import one.world.core.*;
import one.world.binding.*;
import one.world.util.*;
import one.util.Bug;
import one.util.Guid;
import one.world.env.*;
import one.world.data.BinaryData;
import java.util.TreeSet;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.LinkedList;
import java.util.Collections;
import java.util.List;
import java.io.*;


/**
 * This class holds an element of a response.
 *
 * <p>It holds fields for an integer closure which
 *    orders the responses, a String message that
 *    describes the response(and serves as a 
 *    secondary ordering) and an otherInfo field
 *    that holds extra info.  The otherInfo field
 *    is displayed with toString, but is NOT used
 *    when testing equality.</p> 
 *
 */
class ListEntry implements Comparable
{
  /** The sequence number of this entry */
  public Integer intClosure;
  /** The message string */
  public Object message;
  /** Anything we want printed, but NOT tested for equality. */
  public Object otherInfo;

  public int compareTo(Object o) 
  {
    int val;

    ListEntry other=(ListEntry)o;
    val=intClosure.compareTo(other.intClosure);  
    if (val==0) {
      if ((message instanceof String) &&
          (other.message instanceof String)) { 
        String strm=(String)message;
        String otherStrm=(String)other.message;
        val=strm.compareTo(otherStrm);
      } else {
        return 0;
      }
    }
    return val;
   
  }
  /**
   * Returns true if and only if o points to another ListEntry
   * with the same message and intClosure.
   *
   * @param o The object to check equality with
   * @return true if the objects are equal
   */
  public boolean equals(Object o) {
    if (o instanceof ListEntry) {
      ListEntry le=(ListEntry)o;
      return intClosure.equals(le.intClosure) &&
             message.equals(le.message);
    } else {
      return false;
    }
  }

  public String toString() {
    if (otherInfo==null) {
      return "\n"+message+":"+intClosure;
    } else {
      return "\n"+message+":("+otherInfo+"):"+intClosure;
    }
  }
 
  ListEntry(Integer intClosure,Object message) {
    this.intClosure=intClosure;
    this.message=message;
    this.otherInfo=null;
  }
  ListEntry(Integer intClosure,Object message,Object otherInfo) {
    this.intClosure=intClosure;
    this.message=message;
    this.otherInfo=otherInfo;
  }
}

/**
 * To do operations on a tuple store, I need a component who 
 * can issue bind requests.
 */

class BindTestComponent extends Component {
  /**
   * Class of test tuples 
   */


  private static class TTuple extends Tuple {
    public TTuple (int field1, int field2, int field3) {
      this.field1 = field1;
      this.field2 = field2;
      this.field3 = new Integer( field3);
    }
    public TTuple (){
    }
    public int field1;
    public int field2;
    public Integer field3;
    public String toString() {
      return "("+field1+","+field2+","+field3+")";
    }
  }
  /**
   * Second class of test tuples 
   */ 
  private static class TTuple2 extends Tuple {
    public TTuple2 (String field1, int field2, int field3) {
      this.field1 = field1;
      this.field2 = field2;
      this.field3 = new Integer( field3);
    }
    public TTuple2 (){
    }
    public String field1;
    public int field2;
    public Integer field3;
    public String toString() {
      return "("+field1+","+field2+","+field3+")";
    }
  }

  /**
   * Add the result to the list l in the form of a ListEntry.
   *
   * @param l The list to add to
   * @param intClosure The ordering integer.  If not an integer, this
   *                   function will assign -10 to the new list entry. 
   * @param message The string message
   */
  static void resultAdd(List l,Object intClosure,Object message)   {
    ListEntry le;
  //  System.out.println("Debug: "+message+": "+intClosure);
    if ((intClosure!=null) && (intClosure instanceof Integer)) {
      l.add(new ListEntry((Integer)intClosure,message));
    } else {
      l.add(new ListEntry(new Integer(-10),message));
    }
  }

  /**
   * Add the result to the list l in the form of a ListEntry.
   *
   * @param l The list to add to
   * @param intClosure The ordering integer. 
   * @param message The string message
   */
  static void resultAdd(List l,int intClosure,String message)   {
    ListEntry le;
    l.add(new ListEntry(new Integer(intClosure),message));
  }


  /**
   * Add the result to the list l in the form of a ListEntry.
   *
   * @param l The list to add to
   * @param intClosure The ordering integer.  If not an integer, this
   *                   function will assign -10 to the new list entry. 
   * @param message The string message
   * @param otherInfo Any other info to be printed, but not compared
   */
  static void resultAdd(List l,Object intClosure,String message,
                               Object otherInfo)   {
    ListEntry le;
    if ((intClosure!=null) && (intClosure instanceof Integer)) {
      l.add(new ListEntry((Integer)intClosure,message,otherInfo));
    } else {
      l.add(new ListEntry(new Integer(-10),message,otherInfo));
    }
  }

  /**
   * Add the result to the list l in the form of a ListEntry.
   *
   * @param l The list to add to
   * @param intClosure The ordering integer. 
   * @param message The string message
   * @param otherInfo Any other info to be printed, but not compared
   */
  static void resultAdd(List l,int intClosure,String message,
                               Object otherInfo)   {
    ListEntry le;
    l.add(new ListEntry(new Integer(intClosure),message,otherInfo));
  }


  // =======================================================================
  //                           The main handler
  // =======================================================================
  //The tuple store instance
  TupleStore tsf;
  /** Are we done?*/
  public boolean done;
  /** What is our response? */
  public List testResponse=Collections.synchronizedList(new LinkedList());

  /** The Environment we are running in*/
  Environment thisEnv;
  /** The stores we are bound to */
  BindingResponse boundStores[] = new BindingResponse[15];

  /** The listens we have running */
  ListenResponse boundListen[]  = new ListenResponse[15];
 
  /** The queries we have active */
  QueryResponse boundQuery[]    = new QueryResponse[15];
  /** How many bound stores? */
  int numBound  = 0;
  /** How many listens? */
  int numListen = 0;
  /** How many queries? */
  int numQuery  = 0;
 
  /** int counter */
  int counter;
  int counter2;

  boolean haveTimer;
 
  /** Hack for a delete test */
  Tuple keptT;
  Object testLock=new Object();
  Tuple keptTuples[];
  /** 
   * Counter for listen events so we only move to the next test
   * once 
   */
  int count11=0;  


  Object HANDLE_LOCK=new Object();

  boolean showTuple=true;
  Environment.Descriptor otherOldDescriptor;
  Environment.Descriptor newDescriptor;
  Environment.Descriptor newDescriptor2;
  Environment.Descriptor newDescriptor3;
  Environment mainEnv;
  Environment.Descriptor origDescriptor1;
  Environment.Descriptor origDescriptor2;
  
  Object sourceClosure;
  Object destClosure;
 
  /**
   * Environments to delete when we are done.
   */     
  private Set envsToDelete;

  /** The main exported event handler. */
  final class MainHandler extends AbstractHandler {
    /**
     * Add a tuple to the store.
     *
     * @param ts  The tuple store
     * @param t   The tuple to add
     * @param cls The closure 
     */
    void add(EventHandler ts,Tuple t,Object cls) {
      ts.handle(new OutputRequest(this, cls, 
                        t, 
                        null));
    }

    /**
     * Remove a tuple from the store.
     *
     * @param ts  The tuple store
     * @param g   The tuple to delete 
     * @param cls The closure 
     */
    void del(EventHandler ts,Guid g,Object cls) {
      ts.handle(new DeleteRequest(this, cls, 
                      g, 
                      null));
    }

    /**
     * REad a tuple from the store.
     *
     * @param ts  The tuple store
     * @param q   The query 
     * @param cls The closure 
     */
    void find(EventHandler ts,Query q,Object cls) {
      ts.handle(new InputRequest(this, 
                cls,
                InputRequest.READ,
                q,
                0,
                false,
                null));
    }


    /**
     * Read a tuple guid from the store.
     *
     * @param ts  The tuple store
     * @param q   The query 
     * @param cls The closure 
     */
    void findIdOnly(EventHandler ts,Query q,Object cls) {
      ts.handle(new InputRequest(this, 
                cls,
                InputRequest.READ,
                q,
                0,
                true,
                null));
    }


    /**
     * Listen for tuples on a store.
     *
     * @param ts  The tuple store
     * @param q   The query 
     * @param cls The closure 
     */
    void listen(EventHandler ts,Query q,Object cls) {
      ts.handle(new InputRequest(this, 
                 cls,
                 InputRequest.LISTEN,
                 q,
                 10000,
                 false,
                 null));
    }
    /**
     * Query for tuples on a store.
     *
     * @param ts  The tuple store
     * @param q   The query 
     * @param cls The closure 
     */
    void query(EventHandler ts,Query q,Object cls) {
      ts.handle(new InputRequest(this, 
                 cls,
                 InputRequest.QUERY,
                 q,
                 10000,
                 false,
                 null));
    }
    /**
     * Get the next query element for tuples on a store.
     *
     * @param iter The iterator 
     * @param cls  The closure 
     */
    void queryNext(EventHandler iter,Object cls) {
      iter.handle(new IteratorRequest(this,cls));
    } 


    /**
     * This controls the actions of test 6. 
     *
     * <p>Tests binding/unbinding/rebinding, add, read</p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */
    void nextOp6(Integer closureNum) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;

      int cnum=closureNum.intValue();
      switch (cnum) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          //store was opened, add a tuple
          keptT=t=new TTuple(1,2,3);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        case 1:
          del(boundStores[0].resource,(Guid)keptT.id,new Integer(2));
          break;
        case 2:
          //store was opened, add a tuple
          t=new TTuple(1,2,9);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        
        case 3:
          //add a tuple
          t=new TTuple(41,72,39);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        case 4:
          //find one of the tuples
          find(boundStores[0].resource,new Query("field2",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(2)),
                                                 new Integer(cnum+1));
          break;
        case 5:
          //cancel the lease
          lev=new LeaseEvent(this,new Integer(cnum+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        case 6:
          //add a tuple.  expect failure since canceled.  
          //Not a great test since it might take a while to 
          //cancel.
          resultAdd(testResponse,closureNum,"Bound:" +
                             TupleStore.getNumberBound(thisEnv.getId()));
 
          t=new TTuple(49,472,39);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        case 7:
          //rebind
          sio=new SioResource(".");
          BindingRequest breq=new BindingRequest(this,new Integer(cnum+1),sio,100000);
          requestHandler.handle(breq);
          break;
        case 8:
          //do a find that shouldn't work
          find(boundStores[1].resource,new Query("field2",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(472)),
                                                 new Integer(cnum+1));
          break;
        case 9:
          //add a tuple
          t=new TTuple(429,473,39);
          add(boundStores[1].resource,t,new Integer(cnum+1));
          break;
        case 10:
          //find one of the old tuples(before we closed)
          find(boundStores[1].resource,new Query("field2",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(2)),
                                                 new Integer(cnum+1));
          break;
        case 11:
          //find the new tuple
          find(boundStores[1].resource,new Query("field2",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(473)),
                                                 new Integer(cnum+1));
          break;
        case 12:
          //add a tuple
          keptT=t=new TTuple(9344,45673,11);
          add(boundStores[1].resource,t,new Integer(cnum+1));
          break;
        case 13:
          //add a tuple
          t=new TTuple(3401,4673,345);
          add(boundStores[1].resource,t,new Integer(cnum+1));
          break;
        case 14:
          //find the new tuple
          find(boundStores[1].resource,new Query("id",
                                                 Query.COMPARE_EQUAL,
                                                 keptT.id),
                                                 new Integer(cnum+1));
          break;
        case 15:
          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
          break;
        
      }   
    }
    /**
     * This controls the actions of test 7. 
     *
     * <p>Tests listen</p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 
    void nextOp7(Integer closureNum) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          //store was opened, add a tuple
          t=new TTuple(1,2,3);
          add(boundStores[0].resource,t,new Integer(1));
          break;
        case 1:
          //add a tuple
          t=new TTuple(41,72,39);
          add(boundStores[0].resource,t,new Integer(2));
          break;
        case 2:
          //add a tuple
          t=new TTuple(429,472,39);
          add(boundStores[0].resource,t,new Integer(3));
          break;
        case 3:
          counter=0;
          //Set up a listen
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(4));
          break;
        case 4:
          synchronized(testLock) {
            switch(counter) {
              case 0:
                t=new TTuple(8,472,39);
                counter++;
                try {
                  Thread.sleep(500);
                } catch (InterruptedException x) {
                   
                }
                add(boundStores[0].resource,t,new Integer(4));
                break;
              case 1:
                counter++;
                break;
              case 2:
                counter++;
                t=new TTuple(47,3472,639);
                add(boundStores[0].resource,t,new Integer(4));
                break;
              case 3:
                counter++;
                t=new TTuple(12,3472,639);
                add(boundStores[0].resource,t,new Integer(4));
                break;
              case 4:
                counter++;
                break;
              case 5:
                counter++;
                t=new TTuple2("Hi There",3472,639);
                add(boundStores[0].resource,t,new Integer(4));
                break;
              case 6:
                counter++;
                lev=new LeaseEvent(this,new Integer(9),LeaseEvent.CANCEL,
                                   null,null,0);
                boundListen[0].lease.handle(lev);
                break;
              default:
                break;
            }
          }

          break;
        case 9:
          t=new TTuple(3,3472,639);
          add(boundStores[0].resource,t,new Integer(10));
          break;
        case 10:
          t=new TTuple(4,3472,639);
          add(boundStores[0].resource,t,new Integer(11));
          break;
        case 11: 
          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;

      }   
    }
    
 
    /**
     * This controls the actions of test 8. 
     *
     * <p>Tests query</p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 
    void nextOp8(Integer closureNum) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;
      int number=closureNum.intValue();

      switch (number) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          //store was opened, add a tuple
          t=new TTuple(1,2,3);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 1:
          //add a tuple
          t=new TTuple(41,72,39);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 2:
          //add a tuple
          t=new TTuple(429,472,39);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 3:
          t=new TTuple(49,47,8339);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 4:
          t=new TTuple(47,3472,639);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 5:
          t=new TTuple(12,3472,639);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 6:
          t=new TTuple2("Hi There",3472,639);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 7:
          t=new TTuple(3,3472,639);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 8:
          t=new TTuple(4,3472,639);
          add(boundStores[0].resource,t,new Integer(number+1));
          break;
        case 9:
          query(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(number+1));

          break;
        case 10:
          queryNext(boundQuery[0].iter,new Integer(number+1));
          break;
        case 11:
          queryNext(boundQuery[0].iter,new Integer(number+1));
          break;
        case 12:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(number+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[0].lease.handle(lev);
          break;
        case 13:
          queryNext(boundQuery[0].iter,new Integer(number+1));
          break;
        case 14:
          query(boundStores[0].resource, new Query("",
                                                 Query.COMPARE_HAS_SUBTYPE,
                                                 Object.class),
                                                 new Integer(number+1));
          break;
        case 15:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 16:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 17:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 18:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 19:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 20:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 21:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 22:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 23:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 24:
          queryNext(boundQuery[1].iter,new Integer(number+1));
          break;
        case 25:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(number+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[1].lease.handle(lev);
          break;
        case 26:
          query(boundStores[0].resource, new Query("rt",
                                                 Query.COMPARE_EQUAL,
                                                 "TERfsf"),
                                                 new Integer(number+1));
          break;
        case 27:
          queryNext(boundQuery[2].iter,new Integer(number+1));
          break;
        case 28:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(number+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[2].lease.handle(lev);
          break;
        case 29:
          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;

      }   
    }

    /**
     * This controls the actions of test 9. 
     *
     * <p>Tests data access using nested and id only queries </p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */
    void nextOp9(Integer closureNum) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;

      int cnum=closureNum.intValue();
      switch (cnum) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          keptTuples=new Tuple[30];
          //store was opened, add a tuple
          keptTuples[0]=t=new TTuple(1,2,3);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        case 1:
          find(boundStores[0].resource,new Query("id",
                                                 Query.COMPARE_EQUAL,
                                                 keptTuples[0].id),
                                                 new Integer(cnum+1));
          break;
        case 2:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;

            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(-1));
            qAnd     = new Query(qGuid,Query.BINARY_AND,qGreater);       
            find(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;
        case 3:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;

            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(5));
            qAnd     = new Query(qGuid,Query.BINARY_AND,qGreater);       
            find(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;
        case 4:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;
            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(5));
            qAnd     = new Query(qGuid,Query.BINARY_OR,qGreater);       
            find(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;
        case 5:
          findIdOnly(boundStores[0].resource,new Query("id",
                                                 Query.COMPARE_EQUAL,
                                                 keptTuples[0].id),
                                                 new Integer(cnum+1));
          break;
        case 6:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;
            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(-1));
            qAnd     = new Query(qGuid,Query.BINARY_AND,qGreater);       
            findIdOnly(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;
        case 7:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;
            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(5));
            qAnd     = new Query(qGuid,Query.BINARY_AND,qGreater);       
            findIdOnly(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;
        case 8:
          {
            Query qAnd;
            Query qGuid;
            Query qGreater;
            qGuid    = new Query("id", Query.COMPARE_EQUAL, keptTuples[0].id);
            qGreater = new Query("field2", Query.COMPARE_GREATER,new Integer(5));
            qAnd     = new Query(qGuid,Query.BINARY_OR,qGreater);       
            findIdOnly(boundStores[0].resource,qAnd, new Integer(cnum+1));
          }
          break;


        case 9:
          del(boundStores[0].resource,(Guid)keptTuples[0].id,new Integer(cnum+1));
          break;
        case 10:
          t=new TTuple(1,2,9);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        
        case 11:
          t=new TTuple(41,72,39);
          add(boundStores[0].resource,t,new Integer(cnum+1));
          break;
        case 12:
          find(boundStores[0].resource,new Query("field2",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(2)),
                                                 new Integer(cnum+1));
          break;
        case 13:
          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;

      }   
    }
 
    /**
     * This controls the actions of test 10. 
     *
     * <p>Tests query</p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 
    void nextOp10(Integer closureNum,Event ev) {
      Tuple t[];
      
      int i;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      showTuple=false;
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          t       = new Tuple[90];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 90; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 90; i++) { 
            add(boundStores[0].resource,t[i],new Integer(num+1));
          }
          break;
        case 1:
          synchronized (testLock) {
            counter++;
            if (counter<90) {
              break;
            } 
            counter=0;
          }
          keptTuples = new Tuple[90];
          for (i = 0; i < 90; i++) {
            find(boundStores[0].resource,
                 new Query("field1",Query.COMPARE_EQUAL,new Integer(i)),
                 new Integer(num+1));      
          }
          break;
        case 2:
          InputResponse iresp=(InputResponse)ev;
          synchronized (testLock) {
            keptTuples[counter]=iresp.tuple;
            counter++;
            
            if (counter<90) {
              break;
            } 
            counter=0;
          }

          for (i = 0; i < 90; i++) {
            del(boundStores[0].resource,keptTuples[i].id,new Integer(num+1));
          }
          break;
        case 3:
          synchronized (testLock) {
            counter++;
            
            if (counter<90) {
              break;
            } 
            counter=0;
          }

          for (i = 0; i < 90; i++) {
            find(boundStores[0].resource,
                 new Query("field1",Query.COMPARE_EQUAL,new Integer(i)),
                 new Integer(num+1));      
          }
          break;

        case 4:
          synchronized (testLock) {
            counter++;
            
            if (counter<90) {
              break;
            } 
            counter=0;
          }

          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;

      }   
    }


    /**
     * This controls the actions of test 11. 
     *
     * <p>Tests clones</p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications.  Note this also 
     *          causes us to call without queuing several places,
     *          the worst offender being queryNext(). </p>
     *
     */ 
    void nextOp11(Integer closureNum,Event ev) {
      Tuple t[];
      Set envTree;
      
      int i;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      showTuple=false;
      Guid newGuid;
      Environment.Descriptor oldDescriptor;

      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {
            mainEnv=Environment.create(null,getEnvironment().getId(),"MainEnv",
                                       false);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          BindingRequest breq;

          sio  =new SioResource("MainEnv");
          breq =new BindingRequest(this,new Integer(num+1),sio,100000);
          requestHandler.handle(breq);
          break;
        }
        case 2:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          t       = new Tuple[90];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 90; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 90; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        case 3:
          synchronized (testLock) {
            counter++;
            
            if (counter<90) {
              break;
            } 
            counter=0;
          }

          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            Object closure;

            closure = tsf.startCopy(new Environment.Descriptor[] {oldDescriptor},
                                     new Environment.Descriptor[] {newDescriptor});
            tsf.commitCopy(closure);
            // cancel the lease
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x);
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        case 4: {
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 5:
          counter=0;
          query(boundStores[2].resource, new Query("id",
                                                   Query.COMPARE_HAS_FIELD,
                                                   null),
                                         new Integer(num+1)); 
          break;
        case 6:
          synchronized(testLock) {
            counter++;
            if (counter<92) {
              queryNext(boundQuery[0].iter,new Integer(num));
              break;
            }
          }
          //break out of the recursive call
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 7:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[0].lease.handle(lev);
          break;
        case 8:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
          break;
        case 9:
          //break out of the recursion
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 10:
          try {
            tsf.delete(new Guid[] {newDescriptor.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(-1));
          break;
      }   
    }



    /**
     * This controls the actions of test 12. 
     *
     * <p>Tests clone border cases </p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications</p>
     */ 
    void nextOp12(Integer closureNum,Event ev) {
      Tuple t[];
      Set envTree; 
      int i;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      Guid newGuid;
      Environment.Descriptor oldDescriptor;
      showTuple=true;
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -4:
          resultAdd(testResponse,closureNum,"Got event, haveTimer:"+haveTimer);
          synchronized(testLock) {
            counter2++;
          }
          break;
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {
            mainEnv=Environment.create(null,getEnvironment().getId(),"MainEnv",
                                       false);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          sio=new SioResource("MainEnv");
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          requestHandler.handle(breq);
          break;
        }

        case 2:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          t       = new Tuple[90];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 10; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 10; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        case 3:
          synchronized (testLock) {
            counter++;
            
            if (counter<10) {
              break;
            } 
            counter=0;
          }

          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.abortAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        case 4: {
          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 5: {
          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone2",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.commitAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }

          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 6:
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 7:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
            
          break;
        case 8:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          try {
            tsf.delete(new Guid[] {newDescriptor.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 9: {
          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          sio=new SioResource();
          sio.ident=oldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 10:
          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          try {
            sourceClosure=tsf.startMove(
                  new Environment.Descriptor[] {oldDescriptor},false);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          find(boundStores[3].resource, new Query("field3",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(8)),
                                                 new Integer(num+1));
          break;
        case 11:
          find(boundStores[3].resource, new Query("field1",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(4)),
                                                 new Integer(num+1));
          break;
        case 12: {
          haveTimer=false;
          Event newEv=new DynamicTuple();
          newEv.closure=new Integer(num+1);
          add(boundStores[3].resource,new TTuple(1,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(145,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(651,34,555),new Integer(-4));

          timer.schedule(Timer.ONCE,
                         SystemUtilities.currentTimeMillis()+3000,
                         0,
                         this,
                         newEv);
          break;
        }
        case 13:
          haveTimer=true;
          tsf.commitMove(sourceClosure);
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 14:
          synchronized(testLock) {
            counter2++;
            if (counter2<4) {
              break;
            }
          }
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 15:
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[3].lease.handle(lev);
            
          break;

      }   
    }



    /**
     * This controls the actions of test 11. 
     *
     * <p>Tests cloning two environments</p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications.  Note this also 
     *          causes us to call without queuing several places,
     *          the worst offender being queryNext(). </p>
     *
     */ 
    void nextOp13(Integer closureNum,Event ev) {
      Tuple t[];
      
      int i;
      LeaseEvent lev;
      SioResource sio;
      Set envTree; 
      int num=closureNum.intValue();
      showTuple=false;
      Guid newGuid;
      Environment.Descriptor oldDescriptor;

      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {
            mainEnv=Environment.create(null,getEnvironment().getId(),"MainEnv",
                                       false);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          sio=new SioResource("MainEnv");
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          requestHandler.handle(breq);
          break;
        }
        case 2: {
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          Environment newEnv;
          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          try {
            newEnv=Environment.create(null,oldDescriptor.ident,"TestRootChild1",
                                      false);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
            throw new Bug("Error in test" +x);
          }
 
          otherOldDescriptor=tsf.guidToDescriptor(newEnv.getId());

          t       = new Tuple[40];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 20; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 20; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        }
        case 3: {
          synchronized (testLock) {
            counter++;
            
            if (counter<20) {
              break;
            } 
            counter=0;
          }
          sio=new SioResource();
          sio.ident=otherOldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 4:
            t       = new Tuple[40];
            counter = 0;
            //store was opened, add a tuple
            for (i = 20; i < 40; i++) {
              t[i]=new TTuple(i,2,3);
            }
            for (i = 20; i < 40; i++) { 
              add(boundStores[2].resource,t[i],new Integer(num+1));
            }

          break;
        case 5:
          synchronized (testLock) {
            counter++;
            
            if (counter<20) {
              break;
            } 
            counter=0;
          }
          //clear the recursion
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 6: 
          oldDescriptor=tsf.guidToDescriptor(mainEnv.getId());
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          newDescriptor2=new Environment.Descriptor(new Guid(),
                                                   "Clone2",
                                                   newDescriptor.ident,
                                                   oldDescriptor.protection);
          try {
            Object closure;

	    closure = tsf.startCopy(new Environment.Descriptor[] {
					 oldDescriptor, otherOldDescriptor},
                                    new Environment.Descriptor[] {
					 newDescriptor, newDescriptor2});

            tsf.abortCopy(closure);

            envTree=new java.util.TreeSet();
            TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
            resultAdd(testResponse,closureNum,envTree);

	    closure = tsf.startCopy(new Environment.Descriptor[] {
					oldDescriptor, otherOldDescriptor},
                                    new Environment.Descriptor[] {
					newDescriptor, newDescriptor2});

            tsf.commitCopy(closure);

          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        case 7: 
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
          break;
        case 8: {
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        }
        case 9: 
          add(boundStores[2].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 10: {
          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 11: 
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 12: {
          sio=new SioResource();
          sio.ident=newDescriptor2.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 13: 
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;

        case 14:
          counter=0;
          query(boundStores[3].resource, new Query("id",
                                                   Query.COMPARE_HAS_FIELD,
                                                   null),
                                         new Integer(num+1)); 
          break;
        case 15:
          counter++;
          if (counter<23) {
            queryNext(boundQuery[0].iter,new Integer(num));
            break;
          }
          //break out of the recursive call
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 16:
          counter=0;
          query(boundStores[4].resource, new Query("id",
                                                   Query.COMPARE_HAS_FIELD,
                                                   null),
                                         new Integer(num+1)); 
          break;
        case 17:
            counter++;
            if (counter<22) {
              queryNext(boundQuery[1].iter,new Integer(num));
              break;
            }
          //break out of the recursive call
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;


        case 18:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[0].lease.handle(lev);
          break;
        case 19: 
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 20:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[1].lease.handle(lev);
          break;
        case 21: 
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 22:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[3].lease.handle(lev);
          break;
        case 23:
          //break out of the recursion
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 24:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[4].lease.handle(lev);
          break;
        case 25:
          //break out of the recursion
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;

        case 26:
          try {
            tsf.delete(new Guid[] {newDescriptor.ident,newDescriptor2.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException x) {
          }

          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(-1));
          break;
      }   
    }

    /**
     * This controls the actions of test 14. 
     *
     * <p>Tests move with commit </p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications</p>
     */ 
    void nextOp14(Integer closureNum,Event ev) {
      Tuple t[];
      Set envTree; 
      int i;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      Guid newGuid;
      Environment.Descriptor oldDescriptor;
      showTuple=true;
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -4:
          resultAdd(testResponse,closureNum,"Got event, haveTimer:"+haveTimer);
          counter2++;
          break;
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {
//            mainEnv=Environment.create(null,getEnvironment().getId(),"MainEnv");
            otherOldDescriptor=new Environment.Descriptor(new Guid(),"MainEnv",getEnvironment().getId(),new Guid());
            tsf.create(otherOldDescriptor);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          sio=new SioResource();
          sio.ident=otherOldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }

        case 2:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          t       = new Tuple[90];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 10; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 10; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        case 3:
          synchronized (testLock) {
            counter++;
            
            if (counter<10) {
              break;
            } 
            counter=0;
            add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
            break;
          }
        case 4:
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.abortAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        case 5: {
          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 6: {
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone2",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.commitAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }

          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 7:
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 8:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
            
          break;
        case 9:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          try {
            tsf.delete(new Guid[] {newDescriptor.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 10: {
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          sio=new SioResource();
          sio.ident=oldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 11:
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          try {
            sourceClosure=tsf.startMove(
                  new Environment.Descriptor[] {oldDescriptor},true);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
            throw new Bug("Error:"+x.toString());
          }
          find(boundStores[3].resource, new Query("field3",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(8)),
                                                 new Integer(num+1));
          break;
        case 12:
          find(boundStores[3].resource, new Query("field1",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(4)),
                                                 new Integer(num+1));
          break;
        case 13: {
          haveTimer=false;
          Event newEv=new DynamicTuple();
          newEv.closure=new Integer(num+1);
          add(boundStores[3].resource,new TTuple(1,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(145,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(651,34,555),new Integer(-4));

          timer.schedule( Timer.ONCE,
                          SystemUtilities.currentTimeMillis()+3000,
                          0,
                          this,
                          newEv);
          break;
        }
        case 14:
          haveTimer=true;
          tsf.commitMove(sourceClosure);
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 15:
          synchronized(testLock) {
            counter2++;
            if (counter2<4) {
              break;
            }
          }
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 16:
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[3].lease.handle(lev);
            
          break;

      }   
    }



    /**
     * This controls the actions of test 15. 
     *
     * <p>Tests move with abort </p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications</p>
     */ 
    void nextOp15(Integer closureNum,Event ev) {
      Tuple t[];
      Set envTree; 
      int i;
      LeaseEvent lev;
      SioResource sio;
      int num=closureNum.intValue();
      Guid newGuid;
      Environment.Descriptor oldDescriptor;
      showTuple=true;
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -4:
          resultAdd(testResponse,closureNum,"Got event, haveTimer:"+haveTimer);
          counter2++;
          break;
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          try {
            tsf.delete(new Guid[] {otherOldDescriptor.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {
            otherOldDescriptor=new Environment.Descriptor(new Guid(),"MainEnv",getEnvironment().getId(),new Guid());
            tsf.create(otherOldDescriptor);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          sio=new SioResource();
          sio.ident=otherOldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }

        case 2:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          t       = new Tuple[90];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 10; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 10; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        case 3:
          synchronized (testLock) {
            counter++;
            
            if (counter<10) {
              break;
            } 
            counter=0;
            add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
            break;
          }
        case 4:
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.abortAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        case 5: {
          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 6: {
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone2",
                                                   oldDescriptor.parent,
                                                   oldDescriptor.protection);
          try {
            destClosure=tsf.startAccept(
                  new Environment.Descriptor[] {newDescriptor});
            tsf.commitAccept(destClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }

          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 7:
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 8:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
            
          break;
        case 9:
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);
          try {
            tsf.delete(new Guid[] {newDescriptor.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          add(boundStores[1].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 10: {
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          sio=new SioResource();
          sio.ident=oldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 11:
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          try {
            sourceClosure=tsf.startMove(
                  new Environment.Descriptor[] {oldDescriptor},true);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
            throw new Bug("Error:"+x.toString());
          }
          find(boundStores[3].resource, new Query("field3",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(8)),
                                                 new Integer(num+1));
          break;
        case 12:
          find(boundStores[3].resource, new Query("field1",
                                                 Query.COMPARE_EQUAL,
                                                 new Integer(4)),
                                                 new Integer(num+1));
          break;
        case 13: {
          haveTimer=false;
          Event newEv=new DynamicTuple();
          newEv.closure=new Integer(num+1);
          add(boundStores[3].resource,new TTuple(1,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(145,34,555),new Integer(-4));
          add(boundStores[3].resource,new TTuple(651,34,555),new Integer(-4));

          timer.schedule( Timer.ONCE,
                          SystemUtilities.currentTimeMillis()+3000,
                          0,
                          this,
                          newEv);
          break;
        }
        case 14:
          haveTimer=true;
          tsf.abortMove(sourceClosure);
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 15:
          synchronized(testLock) {
            counter2++;
            if (counter2<4) {
              break;
            }
          }
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 16:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[3].lease.handle(lev);
            
          break;
        case 17: {
          oldDescriptor=tsf.guidToDescriptor(otherOldDescriptor.ident);
          sio=new SioResource();
          sio.ident=oldDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 18: {
          add(boundStores[4].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        }
        case 19:
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 20:
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[4].lease.handle(lev);
            
          break;
      }   
    }
    /**
     * This controls the actions of test 16. 
     *
     * <p>Tests moving two environments</p>
     *
     * <p>Note: This routine uses static functions not normally 
     *          accessible to applications.  Note this also 
     *          causes us to call without queuing several places,
     *          the worst offender being queryNext(). </p>
     *
     */ 
    void nextOp16(Integer closureNum,Event ev) {
      Tuple t[];
      
      int i;
      LeaseEvent lev;
      SioResource sio;
      Set envTree; 
      int num=closureNum.intValue();
      showTuple=false;
      Guid newGuid;

      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0: {
          try {

         //   mainEnv=Environment.create(null,getEnvironment().getId(),"MainEnv");
            origDescriptor1=new Environment.Descriptor(new Guid(),"MainEnv",getEnvironment().getId(),new Guid());
            tsf.create(origDescriptor1);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        }
        case 1: {
          sio=new SioResource();
          sio.ident=origDescriptor1.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          tsf.bindHandler.handle(breq);
          break;
        }
        case 2:
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 3: {
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          Environment newEnv;
          try {
            origDescriptor2=new Environment.Descriptor(new Guid(),"TestRootChild1",origDescriptor1.ident,new Guid());
            tsf.create(origDescriptor2);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
            throw new Bug("Error in test" +x);
          }
 
          t       = new Tuple[40];
          counter = 0;
          //store was opened, add a tuple
          for (i = 0; i < 20; i++) {
            t[i]=new TTuple(i,2,3);
          }
          for (i = 0; i < 20; i++) { 
            add(boundStores[1].resource,t[i],new Integer(num+1));
          }
          break;
        }
        case 4: {
          synchronized (testLock) {
            counter++;
            
            if (counter<20) {
              break;
            } 
            counter=0;
          }
          sio=new SioResource();
          sio.ident=origDescriptor2.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 5:
            t       = new Tuple[40];
            counter = 0;
            //store was opened, add a tuple
            for (i = 20; i < 40; i++) {
              t[i]=new TTuple(i,2,3);
            }
            for (i = 20; i < 40; i++) { 
              add(boundStores[2].resource,t[i],new Integer(num+1));
            }

          break;
        case 6:
          synchronized (testLock) {
            counter++;
            
            if (counter<20) {
              break;
            } 
            counter=0;
          }
          //clear the recursion
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 7: {
          newDescriptor=new Environment.Descriptor(new Guid(),
                                                   "Clone1",
                                                   origDescriptor1.parent,
                                                   origDescriptor1.protection);
          newDescriptor2=new Environment.Descriptor(new Guid(),
                                                   "Clone2",
                                                   newDescriptor.ident,
                                                   origDescriptor1.protection);
          try {
            Environment.Descriptor[] src  = new Environment.Descriptor[] 
                                    {origDescriptor1,origDescriptor2};
            Environment.Descriptor[] dest = new Environment.Descriptor[] 
                                    {newDescriptor,newDescriptor2};
            int j;     
            Object srcClosure,destClosure;
            srcClosure  = tsf.startMove(src,true);
            destClosure = tsf.startAccept(dest);
            for (j=0;j<src.length;j++) {
              java.util.Iterator it;
              it = tsf.getTuples(src[j].ident,srcClosure);
              int temporaryCount=0;
              while (it.hasNext()) {
                temporaryCount++;
                BinaryData data;
                data=(BinaryData)it.next(); 
                tsf.writeTuple(dest[j].ident,data,destClosure); 
              }
            }
            tsf.commitAccept(destClosure);
            tsf.commitMove(srcClosure);
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
         
          break;
        }
        case 8: 
          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[2].lease.handle(lev);
          break;
        case 9: {
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        }
        case 10: 
          add(boundStores[2].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 11: {
          sio=new SioResource();
          sio.ident=newDescriptor.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 12: 
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 13: {
          sio=new SioResource();
          sio.ident=newDescriptor2.ident;
          sio.type=SioResource.STORAGE;
          BindingRequest breq=new BindingRequest(this,new Integer(num+1),sio,100000);
          //Doesn't pass through queue
          tsf.bindHandler.handle(breq);
          break;
        }
        case 14: 
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;

        case 15:
          counter=0;
          query(boundStores[3].resource, new Query("id",
                                                   Query.COMPARE_HAS_FIELD,
                                                   null),
                                         new Integer(num+1)); 
          break;
        case 16:
          counter++;
          if (counter<23) {
            queryNext(boundQuery[0].iter,new Integer(num));
            break;
          }
          //break out of the recursive call
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 17:
          counter=0;
          query(boundStores[4].resource, new Query("id",
                                                   Query.COMPARE_HAS_FIELD,
                                                   null),
                                         new Integer(num+1)); 
          break;
        case 18:
            counter++;
            if (counter<22) {
              queryNext(boundQuery[1].iter,new Integer(num));
              break;
            }
          //break out of the recursive call
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;


        case 19:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[0].lease.handle(lev);
          break;
        case 20: 
          add(boundStores[1].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 21:
          //cancel the lease on the query 
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundQuery[1].lease.handle(lev);
          break;
        case 22: 
          add(boundStores[0].resource,new TTuple(1,492,3),new Integer(num+1));
          break;
        case 23:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[3].lease.handle(lev);
          break;
        case 24:
          //break out of the recursion
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;
        case 25:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[4].lease.handle(lev);
          break;
        case 26:
          //break out of the recursion
          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(num+1));
          break;

        case 27:
          try {
            tsf.delete(new Guid[] {newDescriptor.ident,newDescriptor2.ident}); 
          } catch (IOException x) {
            resultAdd(testResponse,closureNum,x.toString());
          }
          try {
            Thread.sleep(1000);
          } catch (InterruptedException x) {
          }

          envTree=new java.util.TreeSet();
          TestTupleStore.allPaths("",getEnvironment().getId(),envTree);
          resultAdd(testResponse,closureNum,envTree);

          add(boundStores[0].resource,new TTuple(1,34,555),new Integer(-1));
          break;
      }   
    }

    /**
     * This controls the actions of test 17. 
     *
     * <p>Make sure that closing a store closes child listens </p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 
    void nextOp17(Integer closureNum,Event e) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;
      BindingRequest breq;

      int num=closureNum.intValue();
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          sio=new SioResource(".");
          breq=new BindingRequest(main,new
             Integer(num+1),sio,100000); 
          requestHandler.handle(breq);
          break;
        case 1:
          //store was opened, add a tuple
          t=new TTuple(1,2,3);
          add(boundStores[1].resource,t,new Integer(num+1));
          break;
        case 2:
          //add a tuple
          t=new TTuple(41,72,39);
          add(boundStores[1].resource,t,new Integer(num+1));
          break;
        case 3:
          //add a tuple
          t=new TTuple(429,472,39);
          add(boundStores[1].resource,t,new Integer(num+1));
          break;
        case 4:
          counter=0;
          //Set up a listen
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          break;
        case 5:
          synchronized(testLock) {
            switch(counter) {
              case 0:
                t=new TTuple(8,472,39);
                counter++;
                add(boundStores[1].resource,t,new Integer(num));
                break;
              case 1:
                counter++;
                break;
              case 2:
                counter++;
                lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                                    null,null,0);
                boundStores[0].lease.handle(lev);
                break;
              case 3:
                counter++;
                break;
            }
          }

          break;
        case 6:
          try {
            Thread.sleep(1000);
          } catch (InterruptedException x) {
          }
          t=new TTuple(23,472,39);
          add(boundStores[1].resource,t,new Integer(num+1));
          break;
        case 7:          
          try {
            Thread.sleep(1000);
          } catch (InterruptedException x) {
          }

          //cancel the lease
          lev=new LeaseEvent(this,new Integer(-1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[1].lease.handle(lev);
          break;
      }   
    }
   
    /**
     * This controls the actions of test 18. 
     *
     * <p>Make sure that closing a store closes child listens </p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 

    void nextOp18(Integer closureNum,Event e) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;
      BindingRequest breq;

      int num=closureNum.intValue();
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          counter=0;
          //Set up a listen
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          listen(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          break;
        case 1:
          synchronized(testLock) {
            counter++;
            if ((counter!=4) && (counter!=9)) {
              break;
            } 
          }
          if (counter==4) {
            lev=new LeaseEvent(this,new Integer(num),LeaseEvent.CANCEL,
                                null,null,0);
            boundStores[0].lease.handle(lev);
            break;
          }
          add(boundStores[0].resource,new TTuple(),new Integer(-1));
          break;
      }   
    }
    
  
    /**
     * This controls the actions of test 19. 
     *
     * <p>Make sure that closing a store closes child queries </p>
     *
     * <p>Each op has an integer as a closure.  Pass the closure of
     * a response in here and have it do the next operation.</p>
     */ 

    void nextOp19(Integer closureNum,Event e) {
      Tuple t;
      LeaseEvent lev;
      SioResource sio;
      BindingRequest breq;

      int num=closureNum.intValue();
      //System.out.println("Dispatching "+num);
      switch (num) {
        case -2:
          //do nothing
          break;
        case -1:
          //quit the test
          synchronized(testResponse) {
            done=true;
          }
          break;
        case 0:
          //Set up a query
          query(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          break;
        case 1:
          //Set up a query
          query(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          break;
        case 2:
          //Set up a query
          query(boundStores[0].resource, new Query("field1",
                                                 Query.COMPARE_LESS,
                                                 new Integer(47)),
                                                 new Integer(num+1));
          break;
        case 3:
          lev=new LeaseEvent(this,new Integer(num+1),LeaseEvent.CANCEL,
                             null,null,0);
          boundStores[0].lease.handle(lev);
          break;
        case 4:
          queryNext(boundQuery[0].iter,new Integer(num+1));   
          break;
        case 5:
          queryNext(boundQuery[1].iter,new Integer(num+1));   
          break;
        case 6:
          queryNext(boundQuery[2].iter,new Integer(num+1));   
          break;
        case 7:
          add(boundStores[0].resource,new TTuple(),new Integer(-1));
          break;
      }   
    }
    
 
 
 


 
 

    /** Handle the specified event. */
    protected boolean handle1(Event e) {
      boolean recognized;
      synchronized(HANDLE_LOCK) {
      if (e instanceof EnvironmentEvent) {
        EnvironmentEvent ee=(EnvironmentEvent)e;
        SioResource sio=new SioResource(".");
        switch(ee.type) {
          case EnvironmentEvent.ACTIVATED:
            resultAdd(testResponse,e.closure,"Bound:" +
                               TupleStore.getNumberBound(thisEnv.getId()));
            BindingRequest breq=new BindingRequest(main,new Integer(0),sio,100000);
            requestHandler.handle(breq);
            
            break;
          case EnvironmentEvent.STOP:
            resultAdd(testResponse,e.closure,"STOP");
            respond(e,new EnvironmentEvent(this,null,EnvironmentEvent.STOPPED,null));
          default:
            resultAdd(testResponse,e.closure,"Other event");
            break;
        }
        recognized=true;
      } else if (e instanceof BindingResponse) {
        BindingResponse bResponse=(BindingResponse)e;
        resultAdd(testResponse,e.closure,"BindingResponse");
        resultAdd(testResponse,e.closure,
                  "Bound:"+TupleStore.getNumberBound(thisEnv.getId()));
        
        boundStores[numBound]=bResponse;
        numBound++;
        recognized=true;
      } else if (e instanceof ListenResponse) {
        ListenResponse lResponse=(ListenResponse)e;
        resultAdd(testResponse,e.closure,"ListenResponse");
        
        boundListen[numListen]=lResponse;
        numListen++;
        recognized=true;
      } else if (e instanceof QueryResponse) {
        QueryResponse qResponse=(QueryResponse)e;
        resultAdd(testResponse,e.closure,"QueryResponse");
        
        boundQuery[numQuery]=qResponse;
        numQuery++;
        recognized=true;
      } else if (e instanceof OutputResponse) {
        OutputResponse oResponse=(OutputResponse)e;
        resultAdd(testResponse,e.closure,"OutputResponse",oResponse.ident);
        recognized=true;
      } else if (e instanceof InputResponse) {
        if (showTuple) {
          InputResponse iResponse=(InputResponse)e;
          resultAdd(testResponse,e.closure,
                    "InputResponse("+iResponse.tuple+")");
          recognized=true;
        } else {
          InputResponse iResponse=(InputResponse)e;
          if (iResponse.tuple != null) {
            resultAdd(testResponse,e.closure,
                      "InputResponse",iResponse.tuple);
          } else {
            resultAdd(testResponse,e.closure,
                      "InputResponse(null)",iResponse.tuple);
          }
          recognized=true;
        }
      } else if (e instanceof InputByIdResponse) {
        InputByIdResponse iResponse=(InputByIdResponse)e;
        if (iResponse.ident != null) {
          resultAdd(testResponse,e.closure, "InputByIdResponse",
                    iResponse.ident);
        } else {
          resultAdd(testResponse,e.closure, "InputByIdResponse(null)",
                    iResponse.ident);
        }
        recognized=true;
      } else if (e instanceof IteratorResponse) {
        if (e instanceof IteratorElement) {
          IteratorElement iElement=(IteratorElement)e;
          resultAdd(testResponse,e.closure,"IteratorElement() hasnext:"+ 
                           iElement.hasNext,iElement.element);
        } else {
          //An IteratorEmpty
          resultAdd(testResponse,e.closure,"IteratorEmpty()");
        }
        recognized=true;
      } else if (e instanceof LeaseEvent) {
        LeaseEvent lev=(LeaseEvent)e;
        switch(lev.type) {
          case LeaseEvent.CANCELED:
            resultAdd(testResponse,e.closure,"Lease Cancelled");
            break;
          default:
            resultAdd(testResponse,e.closure,
                      "Unknown Lease Event("+lev.type+")");
            break;
        } 
        recognized=true;
      } else if (e instanceof ExceptionalEvent) {
        ExceptionalEvent ex=(ExceptionalEvent)e;
        resultAdd(testResponse,e.closure,
                  "ExceptEvent("+ex.x+")");
        recognized=true;
      } else if (e instanceof DynamicTuple) {
        resultAdd(testResponse,e.closure,
                  "DynamicTuple");
        recognized=true;
      } else if (e instanceof Timer.Event) {
        resultAdd(testResponse,e.closure,
                  "TimerEvent");
        Timer.Event tev;
        recognized=true;
      } else {
        resultAdd(testResponse,e.closure,"Unknown event: "+e);
        recognized=false;
      }
      if ((e.closure!=null) && (e.closure instanceof Integer)) {
        switch(number) {
          case 6:
            nextOp6((Integer)e.closure);
            break;
          case 7:
            nextOp7((Integer)e.closure);
            break;
          case 8:
            nextOp8((Integer)e.closure);
            break;
          case 9:
            nextOp9((Integer)e.closure);
            break;
          case 10:
            nextOp10((Integer)e.closure,e);
            break;
          case 11:
            nextOp11((Integer)e.closure,e);
            break;
          case 12:
            nextOp12((Integer)e.closure,e);
            break;
          case 13:
            nextOp13((Integer)e.closure,e);
            break;
          case 14:
            nextOp14((Integer)e.closure,e);
            break;
          case 15:
            nextOp15((Integer)e.closure,e);
            break;
          case 16:
            nextOp16((Integer)e.closure,e);
            break;
          case 17:
            nextOp17((Integer)e.closure,e);
            break;
          case 18:
            nextOp18((Integer)e.closure,e);
            break;
          case 19:
            nextOp19((Integer)e.closure,e);
            break;
        }
        }
      }
      return recognized;
    }
  }


  // =======================================================================
  //                           Descriptors
  // =======================================================================

  /** The component descriptor. */
  private static final ComponentDescriptor SELF =
    new ComponentDescriptor("one.world.io.BindTestComponent",
                            "Component to do tests that require a component",
                            true);

  /** The exported event handler descriptor for the main handler. */
  private static final ExportedDescriptor MAIN =
    new ExportedDescriptor("main",
                           "The main handler to give our environment",
                           null,
                           null,
                           false);
  private static final ImportedDescriptor REQUEST =
    new ImportedDescriptor("request",
                           "",
                           new Class[] {},
                           new Class[] {},
                           false,
                           false);

  // =======================================================================
  //                           Instance fields
  // =======================================================================

  /**
   * The main exported event handler.
   *
   * @serial  Must not be <code>null</code>.
   */
  private final EventHandler          main;
  private final Environment.Importer  requestHandler;
  private Timer timer;

  private int number;
  private Harness h;

  // =======================================================================
  //                           Constructor
  // =======================================================================

  /**
   * Create a new instance of <code>BindTestComponent</code>.
   *
   * <p>NOTE: passing an object like Harness is a bad thing.</p>
   *
   * @param  env  The environment for the new instance.
   *
   */
  public BindTestComponent(Environment env,int number, Harness h,Set envsToDelete) {
    super(env);
    main = declareExported(MAIN, new MainHandler());
    requestHandler = declareImported(REQUEST); 
    timer = getTimer(); 
    this.number=number;
    this.h=h;
    done=false;
    tsf=TupleStore.getTupleStoreInstance();
    thisEnv=env;
   
    this.envsToDelete=envsToDelete; 
  }


  // =======================================================================
  //                           Component support
  // =======================================================================

  /** Get the component descriptor. */
  public ComponentDescriptor getDescriptor() {
    return (ComponentDescriptor)SELF.clone();
  }

}


/**
 * Basic tests for TupleStore. 
 *
 * <p>Currently only tests Environment management: Need to add in the
 *    tests for operations on the actual stores</p> 
 *
 * @author  Eric Lemar 
 */
public class TestTupleStore implements TestCollection {
  private Set envsToDelete=Collections.synchronizedSet(new HashSet());
  /** 
   * The event handler that receives binding responses, 
   * input requests, etc.
   */
  private EventHandler myhandle;  

  /** A pointer to the TupleStore instance */
  private TupleStore ts; 

  /** The guid identifier of our fake root Environment */
  private  Guid testRootGuid;

  /** The descriptor of our fake root environment */
  private Environment.Descriptor testRoot;

  /** The Environment passed to initialize() */
  private Environment env;

  /** Report verbose results */
  boolean isVerbose;

  /** The expected result */
  Object expectedResult;

  /**
   * Record the new environment.
   */
  
  public boolean initialize(Environment env) {
    ts=TupleStore.getTupleStoreInstance();
    this.env=env; 
    return false;
  }

  /**
   * Make a new root environment below the environment given to us
   * name TestRootX, where X is the testnum.
   *
   * @param testnum The number of the test we are running
   */
  boolean makeTestRoot(int testnum) throws Throwable {

    /* Create a new fake root.  It has a dummy parent so it should never
     * be encountered by the normal Environment code */
    String testRootName="TestRoot"+testnum;

    testRootGuid=new Guid();
    testRoot=new Environment.Descriptor(testRootGuid,
                                        testRootName,
                                        env.getId(),
                                        new Guid());

    try {
      TupleStore.create(testRoot);
      envsToDelete.add(testRootGuid);
    } catch (IOException x) {
      throw x;
    }
    return false;

  }

  /** returns number of test */
  public int getTestNumber() {
    return 19;
  }

  public String getName() {
    return "one.world.io.TestTupleStore";
  }

  public String getDescription() {
    return "Checks the tuple storage code";
  }
  
  /** Determine whether this test collection needs an environment. */
  public boolean needsEnvironment() {
    return true;
  }

  /** Clean up after the tests */
  public void cleanup() {
  }

  /**
   * Add the result to the list l in the form of a ListEntry.
   *
   * @param l The list to add to
   * @param intClosure The ordering integer. 
   * @param message The string message
   */
  static void resultAdd(List l,int intClosure,String message)   {
    ListEntry le;
    l.add(new ListEntry(new Integer(intClosure),message));
  }
 
 
 
  /**
   * The expected results for test 6
   */
  Object makeExpected6() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"OutputResponse");
    resultAdd(ll,2,"OutputResponse");
    resultAdd(ll,3,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,5,"InputResponse((1,2,9))");
    resultAdd(ll,6,"Bound:0");
    resultAdd(ll,6,"Lease Cancelled");
    resultAdd(ll,7,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,8,"BindingResponse");
    resultAdd(ll,8,"Bound:1");
    resultAdd(ll,9,"InputResponse(null)");
    resultAdd(ll,10,"OutputResponse");
    resultAdd(ll,11,"InputResponse((1,2,9))");
    resultAdd(ll,12,"InputResponse((429,473,39))");
    resultAdd(ll,13,"OutputResponse");
    resultAdd(ll,14,"OutputResponse");
    resultAdd(ll,15,"InputResponse((9344,45673,11))");
    return ll;
  }

  /**
   * The expected results for test 7
   */
  Object makeExpected7() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"OutputResponse");
    resultAdd(ll,2,"OutputResponse");
    resultAdd(ll,3,"OutputResponse");
    resultAdd(ll,4,"InputResponse((12,3472,639))");
    resultAdd(ll,4,"InputResponse((8,472,39))");
    resultAdd(ll,4,"ListenResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,9,"Lease Cancelled");
    resultAdd(ll,10,"OutputResponse");
    resultAdd(ll,11,"OutputResponse");
    return ll;
  }

  /**
   * The expected results for test 8
   */
  Object makeExpected8() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"OutputResponse");
    resultAdd(ll,2,"OutputResponse");
    resultAdd(ll,3,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,5,"OutputResponse");
    resultAdd(ll,6,"OutputResponse");
    resultAdd(ll,7,"OutputResponse");
    resultAdd(ll,8,"OutputResponse");
    resultAdd(ll,9,"OutputResponse");
    resultAdd(ll,10,"QueryResponse");
    resultAdd(ll,11,"IteratorElement() hasnext:true");
    resultAdd(ll,12,"IteratorElement() hasnext:true");
    resultAdd(ll,13,"Lease Cancelled");
    resultAdd(ll,14,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,15,"QueryResponse");
    resultAdd(ll,16,"IteratorElement() hasnext:true");
    resultAdd(ll,17,"IteratorElement() hasnext:true");
    resultAdd(ll,18,"IteratorElement() hasnext:true");
    resultAdd(ll,19,"IteratorElement() hasnext:true");
    resultAdd(ll,20,"IteratorElement() hasnext:true");
    resultAdd(ll,21,"IteratorElement() hasnext:true");
    resultAdd(ll,22,"IteratorElement() hasnext:true");
    resultAdd(ll,23,"IteratorElement() hasnext:true");
    resultAdd(ll,24,"IteratorElement() hasnext:false");
    resultAdd(ll,25,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,26,"Lease Cancelled");
    resultAdd(ll,27,"QueryResponse");
    resultAdd(ll,28,"IteratorEmpty()");
    resultAdd(ll,29,"Lease Cancelled");
    return ll;
  }
  /**
   * The expected results for test 9
   */
  Object makeExpected9() {
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"OutputResponse");
    resultAdd(ll,2,"InputResponse((1,2,3))");
    resultAdd(ll,3,"InputResponse((1,2,3))");
    resultAdd(ll,4,"InputResponse(null)");
    resultAdd(ll,5,"InputResponse((1,2,3))");
    resultAdd(ll,6,"InputByIdResponse");
    resultAdd(ll,7,"InputByIdResponse");
    resultAdd(ll,8,"InputByIdResponse(null)");
    resultAdd(ll,9,"InputByIdResponse");
    resultAdd(ll,10,"OutputResponse");
    resultAdd(ll,11,"OutputResponse");
    resultAdd(ll,12,"OutputResponse");
    resultAdd(ll,13,"InputResponse((1,2,9))");

    return ll;
  }

  /**
   * The expected results for test10 
   */
  Object makeExpected10() {
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    for (i=0;i<90;i++) {
      resultAdd(ll,1,"OutputResponse");
    }
    for (i=0;i<90;i++) {
      resultAdd(ll,2,"InputResponse");
    }
    for (i=0;i<90;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    for (i=0;i<90;i++) {
      resultAdd(ll,4,"InputResponse(null)");
    }
   
    return ll;
  }


  /**
   * The expected results for test11 
   */
  Object makeExpected11() {
    Set envTree;

    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot11/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(2),envTree);
    for (i=0;i<90;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    resultAdd(ll,4,"Lease Cancelled");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot11/Clone1"); 
    envTree.add("/TestRoot11/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(4),envTree);
    resultAdd(ll,5,"BindingResponse");
    resultAdd(ll,5,"Bound:0");
    resultAdd(ll,6,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,6,"IteratorElement() hasnext:false");
    for (i=0;i<89;i++) {
      resultAdd(ll,6,"IteratorElement() hasnext:true");
    }
    resultAdd(ll,6,"QueryResponse");
    resultAdd(ll,7,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,8,"Lease Cancelled");
    resultAdd(ll,9,"Lease Cancelled");
    resultAdd(ll,10,"ExceptEvent(one.world.binding.ResourceRevokedException)");
   
    return ll;
  }

  /**
   * The expected results for test12 
   */
  Object makeExpected12() {
    Set envTree;
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-4,"Got event, haveTimer:true");
    resultAdd(ll,-4,"Got event, haveTimer:true");
    resultAdd(ll,-4,"Got event, haveTimer:true");
    resultAdd(ll,-4,"OutputResponse");
    resultAdd(ll,-4,"OutputResponse");
    resultAdd(ll,-4,"OutputResponse");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot12/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(2),envTree);
    for (i=0;i<10;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    resultAdd(ll,4,"Lease Cancelled");
    resultAdd(ll,5,"ExceptEvent(one.world.binding.UnknownResourceException: Unknown resource)");


    resultAdd(ll,6,"BindingResponse");
    resultAdd(ll,6,"Bound:0");
    resultAdd(ll,7,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,8,"Lease Cancelled");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot12/Clone2"); 
    envTree.add("/TestRoot12/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(8),envTree);
    resultAdd(ll,9,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,10,"BindingResponse");
    resultAdd(ll,10,"Bound:0");
    resultAdd(ll,11,"InputResponse(null)");
    resultAdd(ll,12,"InputResponse((4,2,3))");
    resultAdd(ll,13,"DynamicTuple");
    //resultAdd(ll,13,"TimerEvent");
    resultAdd(ll,14,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,15,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    return ll;
  }


  /**
   * The expected results for test13 
   */
  Object makeExpected13() {
    Set envTree;
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");

    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot13/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(2),envTree);
    for (i=0;i<20;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    resultAdd(ll,4,"BindingResponse");
    resultAdd(ll,4,"Bound:0");
    for (i=0;i<20;i++) {
      resultAdd(ll,5,"OutputResponse");
    }
    resultAdd(ll,6,"OutputResponse");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot13/MainEnv/TestRootChild1"); 
    BindTestComponent.resultAdd(ll,new Integer(6),envTree);
    resultAdd(ll,7,"Lease Cancelled");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot13/Clone1/Clone2"); 
    envTree.add("/TestRoot13/MainEnv/TestRootChild1"); 
    BindTestComponent.resultAdd(ll,new Integer(7),envTree);
    resultAdd(ll,8,"Lease Cancelled");
    resultAdd(ll,9,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,10,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,11,"BindingResponse");
    resultAdd(ll,11,"Bound:0");
    resultAdd(ll,12,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,13,"BindingResponse");
    resultAdd(ll,13,"Bound:0");
    resultAdd(ll,14,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,15,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,15,"IteratorElement() hasnext:false");
    for (i=0;i<20;i++) {
      resultAdd(ll,15,"IteratorElement() hasnext:true");
    }
    resultAdd(ll,15,"QueryResponse");
    resultAdd(ll,16,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,17,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,17,"IteratorElement() hasnext:false");
    for (i=0;i<19;i++) {
      resultAdd(ll,17,"IteratorElement() hasnext:true");
    }
    resultAdd(ll,17,"QueryResponse");
    resultAdd(ll,18,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,19,"Lease Cancelled");
    resultAdd(ll,20,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,21,"Lease Cancelled");
    resultAdd(ll,22,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,23,"Lease Cancelled");
    resultAdd(ll,24,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,25,"Lease Cancelled");
    resultAdd(ll,26,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot13/MainEnv/TestRootChild1"); 
    BindTestComponent.resultAdd(ll,new Integer(26),envTree);
    return ll;
  }


  /**
   * The expected results for test14 
   */
  Object makeExpected14() {
    Set envTree;
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.LeaseRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot14/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(2),envTree);
    for (i=0;i<10;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    resultAdd(ll,4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,5,"Lease Cancelled");
    resultAdd(ll,6,"ExceptEvent(one.world.binding.UnknownResourceException: Unknown resource)");


    resultAdd(ll,7,"BindingResponse");
    resultAdd(ll,7,"Bound:0");
    resultAdd(ll,8,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,9,"Lease Cancelled");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot14/Clone2"); 
    envTree.add("/TestRoot14/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(9),envTree);
    resultAdd(ll,10,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,11,"BindingResponse");
    resultAdd(ll,11,"Bound:0");
    resultAdd(ll,12,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,13,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,14,"DynamicTuple");
    //resultAdd(ll,14,"TimerEvent");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot14"); 
    BindTestComponent.resultAdd(ll,new Integer(14),envTree);
    resultAdd(ll,15,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,16,"ExceptEvent(one.world.binding.ResourceRevokedException)");
 
    return ll;

  }

  /**
   * The expected results for test15 
   */
  Object makeExpected15() {
    Set envTree;
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-4,"Got event, haveTimer:false");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot15/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(2),envTree);
    for (i=0;i<10;i++) {
      resultAdd(ll,3,"OutputResponse");
    }
    resultAdd(ll,4,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,5,"Lease Cancelled");
    resultAdd(ll,6,"ExceptEvent(one.world.binding.UnknownResourceException: Unknown resource)");


    resultAdd(ll,7,"BindingResponse");
    resultAdd(ll,7,"Bound:0");
    resultAdd(ll,8,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,9,"Lease Cancelled");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot15/Clone2"); 
    envTree.add("/TestRoot15/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(9),envTree);
    resultAdd(ll,10,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,11,"BindingResponse");
    resultAdd(ll,11,"Bound:0");
    resultAdd(ll,12,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,13,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,14,"DynamicTuple");
    //resultAdd(ll,14,"TimerEvent");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot15/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(14),envTree);
    resultAdd(ll,15,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,16,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,17,"ExceptEvent(one.world.binding.LeaseRevokedException)");
    resultAdd(ll,18,"BindingResponse");
    resultAdd(ll,18,"Bound:0");
    resultAdd(ll,19,"OutputResponse");
    resultAdd(ll,20,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    return ll;

  }



  /**
   * The expected results for test16 
   */
  Object makeExpected16() {
    Set envTree;
    int i;
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,2,"BindingResponse");
    resultAdd(ll,2,"Bound:0");
    resultAdd(ll,3,"ExceptEvent(one.world.binding.ResourceRevokedException)");

    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot16/MainEnv"); 
    BindTestComponent.resultAdd(ll,new Integer(3),envTree);
    for (i=0;i<20;i++) {
      resultAdd(ll,4,"OutputResponse");
    }
    resultAdd(ll,5,"BindingResponse");
    resultAdd(ll,5,"Bound:0");
    for (i=0;i<20;i++) {
      resultAdd(ll,6,"OutputResponse");
    }
    resultAdd(ll,7,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,8,"ExceptEvent(one.world.binding.LeaseRevokedException)");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot16/Clone1/Clone2"); 
    BindTestComponent.resultAdd(ll,new Integer(8),envTree);
    resultAdd(ll,9,"ExceptEvent(one.world.binding.LeaseRevokedException)");
    resultAdd(ll,10,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,11,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,12,"BindingResponse");
    resultAdd(ll,12,"Bound:0");
    resultAdd(ll,13,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,14,"BindingResponse");
    resultAdd(ll,14,"Bound:0");
    resultAdd(ll,15,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,16,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,16,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,16,"IteratorElement() hasnext:false");
    for (i=0;i<19;i++) {
      resultAdd(ll,16,"IteratorElement() hasnext:true");
    }
    resultAdd(ll,16,"QueryResponse");
    resultAdd(ll,17,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,18,"ExceptEvent(java.util.NoSuchElementException)");
    resultAdd(ll,18,"IteratorElement() hasnext:false");
    for (i=0;i<19;i++) {
      resultAdd(ll,18,"IteratorElement() hasnext:true");
    }
    resultAdd(ll,18,"QueryResponse");
    resultAdd(ll,19,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,20,"Lease Cancelled");
    resultAdd(ll,21,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,22,"Lease Cancelled");
    resultAdd(ll,23,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,24,"Lease Cancelled");
    resultAdd(ll,25,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,26,"Lease Cancelled");
    resultAdd(ll,27,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    envTree=new java.util.TreeSet();
    envTree.add("/TestRoot16"); 
    BindTestComponent.resultAdd(ll,new Integer(27),envTree);
    return ll;
  }


  /**
   * The expected results for test 17
   */
  Object makeExpected17() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"Lease Cancelled");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"BindingResponse");
    resultAdd(ll,1,"Bound:2");
    resultAdd(ll,2,"OutputResponse");
    resultAdd(ll,3,"OutputResponse");
    resultAdd(ll,4,"OutputResponse");
    resultAdd(ll,5,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,5,"InputResponse((8,472,39))");
    resultAdd(ll,5,"ListenResponse");
    resultAdd(ll,5,"OutputResponse");
    resultAdd(ll,6,"Lease Cancelled");
    resultAdd(ll,7,"OutputResponse");
    return ll;
  }



  /**
   * The expected results for test 18
   */
  Object makeExpected18() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,1,"Lease Cancelled");
    resultAdd(ll,1,"ListenResponse");
    resultAdd(ll,1,"ListenResponse");
    resultAdd(ll,1,"ListenResponse");
    resultAdd(ll,1,"ListenResponse");
    return ll;
  }

  /**
   * The expected results for test 19
   */
  Object makeExpected19() {
    List ll=Collections.synchronizedList(new LinkedList());
    resultAdd(ll,-10,"Bound:0");
    resultAdd(ll,-10,"Other event");
    resultAdd(ll,-10,"STOP");
    resultAdd(ll,-1,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,0,"BindingResponse");
    resultAdd(ll,0,"Bound:1");
    resultAdd(ll,1,"QueryResponse");
    resultAdd(ll,2,"QueryResponse");
    resultAdd(ll,3,"QueryResponse");
    resultAdd(ll,4,"Lease Cancelled");
    resultAdd(ll,5,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,6,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    resultAdd(ll,7,"ExceptEvent(one.world.binding.ResourceRevokedException)");
    return ll;
  }




 
  /**
   * Place all paths from the root to a leaf environment in the TreeSet paths.
   * A somewhat compact way to summarize the contents of the environment tree.
   *
   * @param sofar The textual path to the current location
   * @param g The guid we are traversing down from
   * @param paths A TreeSet containing all complete paths.  A TreeSet was used 
   *              as a simple way of sorting the results. 
   */
  static void allPaths(String sofar,Guid g,Set paths) {
    Environment.Descriptor[] children; 
    int i;
    Environment.Descriptor rootDescriptor=TupleStore.guidToDescriptor(g);

   
    children=TupleStore.getChildren(g);
    sofar=sofar+"/"+rootDescriptor.name;
    if (children.length==0) {
      if (paths.contains(sofar)) {
        throw new Bug("duplicate name paths in tree.");
      }
      paths.add(sofar);
    } else {
      for (i=0;i<children.length;i++) {
        if (!((children[i].parent).equals(g))) {
          throw new Bug("Inconsistent Child/Parent pointers in allPaths");
        }
        allPaths(sofar,children[i].ident,paths);
      }
    }
  } 

  /**
   * Make a new environment
   *
   * @param name The name of the new environment
   * @param parent The parent of the new environment
   * @return The Guid of the new environment
   */
  private Guid makeEnv(String name,Guid parent) throws Throwable {
    Guid newGuid;
    newGuid = new Guid();
    TupleStore.create(new Environment.Descriptor(newGuid, name, 
                                                   parent, new Guid()));
    envsToDelete.add(newGuid);
    return newGuid;
  }

 /**
   * Make a new environment
   *
   * @param name The name of the new environment
   * @param parent The parent of the new environment
   * @param protection The protection domain of the new environment
   * @return The Guid of the new environment
   */

  private Guid makeEnv(String name,Guid parent,Guid protection) throws Throwable {
    Guid newGuid=new Guid();
    TupleStore.create(new Environment.Descriptor(newGuid, name, 
                                                   parent, protection));
    envsToDelete.add(newGuid);
    return newGuid;
  }
 
  /**
   * We think this Descriptor should be in the tuplestore with 
   * these values: is it?
   */

  void checkDescriptor(Environment.Descriptor descIn, Set errors) 
         throws Throwable {
    Environment.Descriptor descRead=ts.guidToDescriptor(descIn.ident);
    if (descRead==null) {
      errors.add("Couldn't find "+descIn.name);
      return;
    }
    if (!(descIn.name).equals(descRead.name)) {
      errors.add("Name for" + descIn.name + " wrong: "+
                  descIn.name+","+descRead.name);
    }
    if (!(descIn.ident).equals(descRead.ident)) {
      errors.add("Guid for " + descIn.name + " wrong"+
                  descIn.ident+","+descRead.ident);
    }
    if (!(descIn.parent).equals(descRead.parent)) {
      errors.add("Parent for "+ descIn.name +" wrong"+
                  descIn.parent+","+descRead.parent);
    }
    if (!(descIn.protection).equals(descRead.protection)) {
      errors.add("Protection for "+ descIn.name +" wrong"+
                  descIn.protection+","+descRead.protection);
    }

  }
  /**
   * Remove any stray environments created by the tests.
   */
  void cleanUpEnvironments() {
    int i=0;
    Guid[] allIds=new Guid[envsToDelete.size()];
    java.util.Iterator it;
    it=envsToDelete.iterator();
    while (it.hasNext()) {
      Guid id=(Guid)it.next();
      TupleStore.revokeAll(id); 
      allIds[i]=id;
      i++;
    } 
    try {
      TupleStore.delete(allIds);
    } catch (IOException ioexception) {

    }
   envsToDelete=Collections.synchronizedSet(new HashSet());
  }

  public Object runTest(int number, Harness h, boolean verbose)
    throws Throwable {
    isVerbose=verbose; 
    Object retval=doRunTest(number,h);
    cleanUpEnvironments();
    if (isVerbose) {
      return retval;
    } else {
      if (retval.equals(expectedResult)) {
        return Boolean.TRUE;
      } else {
        return retval;
      }
    }
  }

  Object doRunTest(int number, Harness h) throws Throwable {

    Guid larry,moe,curly,daffy,donald,speedy;
    Guid larry_prot,moe_prot,curly_prot,daffy_prot,donald_prot,speedy_prot;
    Set expectedTree;
    Set resultTree;
    List expectedList;
    int i;
    Environment.Descriptor desc;

    larry=moe=curly=daffy=donald=speedy=null;

    switch(number) { 
      case 1:
        expectedTree=Collections.synchronizedSortedSet(new TreeSet());
        expectedTree.add("/TestRoot1/curly");
        expectedTree.add("/TestRoot1/larry/daffy");
        expectedTree.add("/TestRoot1/larry/speedy");
        expectedTree.add("/TestRoot1/moe/donald");
        expectedResult=expectedTree;
        if (isVerbose) {
          h.enterTest(1,"Create a few environments",expectedResult);
        } else {
          h.enterTest(1,"Create a few environments",Boolean.TRUE);
        }
        makeTestRoot(number);
        larry  = makeEnv("larry",  testRootGuid);
        curly  = makeEnv("curly",  testRootGuid);
        moe    = makeEnv("moe",    testRootGuid);
        daffy  = makeEnv("daffy",  larry);
        donald = makeEnv("donald", moe);
        speedy = makeEnv("speedy", larry);  
                 
        resultTree = Collections.synchronizedSortedSet(new TreeSet());
        allPaths("",testRootGuid,resultTree);      
        return resultTree; 
      case 2:
        expectedTree=Collections.synchronizedSortedSet(new TreeSet());
        expectedTree.add("/TestRoot2/curly");
        expectedTree.add("/TestRoot2/larry/speedy");
        expectedTree.add("/TestRoot2/moe/daffy");
        expectedTree.add("/TestRoot2/moe/donald");

        expectedResult=expectedTree;
        if (isVerbose) {
          h.enterTest(2,"Move an environment",expectedResult);
        } else {
          h.enterTest(2,"Move an environment",Boolean.TRUE);
        }    
        makeTestRoot(number);
        larry  = makeEnv("larry",  testRootGuid);
        curly  = makeEnv("curly",  testRootGuid);
        moe    = makeEnv("moe",    testRootGuid);
        daffy  = makeEnv("daffy",  larry);
        donald = makeEnv("donald", moe);
        speedy = makeEnv("speedy", larry);  
        TupleStore.move(daffy,moe);
         
        resultTree = Collections.synchronizedSortedSet(new TreeSet());
        allPaths("",testRootGuid,resultTree);      

        return resultTree; 
      case 3:
        expectedTree=Collections.synchronizedSortedSet(new TreeSet());
        expectedTree.add("/TestRoot3/curly");
        expectedTree.add("/TestRoot3/moe/donald");
 
        expectedResult=expectedTree;
        if (isVerbose) {
          h.enterTest(3,"Delete a tree of environments",expectedResult);
        } else {
          h.enterTest(3,"Delete a tree of environments",Boolean.TRUE);
        }
        makeTestRoot(number);
        larry  = makeEnv("larry",  testRootGuid);
        curly  = makeEnv("curly",  testRootGuid);
        moe    = makeEnv("moe",    testRootGuid);
        daffy  = makeEnv("daffy",  larry);
        donald = makeEnv("donald", moe);
        speedy = makeEnv("speedy", larry); 
        TupleStore.delete(new Guid[] {larry,daffy,speedy});
        envsToDelete.remove(larry);
        envsToDelete.remove(daffy);
        envsToDelete.remove(speedy);
        resultTree = Collections.synchronizedSortedSet(new TreeSet());
        allPaths("",testRootGuid,resultTree);     
 
        desc=TupleStore.guidToDescriptor(daffy);
        if (desc!=null) {
          resultTree.add("Dangling descriptor");
        }
        desc=TupleStore.guidToDescriptor(larry);
        if (desc!=null) {
          resultTree.add("Dangling descriptor");
        }
        desc=TupleStore.guidToDescriptor(speedy);
        if (desc!=null) {
          resultTree.add("Dangling descriptor");
        }

      return resultTree; 
      case 4:
        expectedTree=Collections.synchronizedSortedSet(new TreeSet());
        expectedTree.add("/TestRoot4/BadBadMoe/donald");
        expectedTree.add("/TestRoot4/curly");
        expectedTree.add("/TestRoot4/larry/SpeedyIsSlow");
        expectedTree.add("/TestRoot4/larry/daffy");

        expectedResult=expectedTree;
        if (isVerbose) {
          h.enterTest(4,"Rename some environments",expectedResult);
        } else {
          h.enterTest(4,"Rename some environments",Boolean.TRUE);
        }
        makeTestRoot(number);
        larry  = makeEnv("larry",  testRootGuid);
        curly  = makeEnv("curly",  testRootGuid);
        moe    = makeEnv("moe",    testRootGuid);
        daffy  = makeEnv("daffy",  larry);
        donald = makeEnv("donald", moe);
        speedy = makeEnv("speedy", larry);  


        TupleStore.rename(moe,"BadBadMoe");
        TupleStore.rename(speedy,"SpeedyIsSlow");        
 
        resultTree = Collections.synchronizedSortedSet(new TreeSet());
        allPaths("",testRootGuid,resultTree);      
        return resultTree;
      case 5:
        expectedTree=Collections.synchronizedSortedSet(new TreeSet());
        expectedTree.add("/TestRoot5/BadBadMoe/donald");
        expectedTree.add("/TestRoot5/BadBadMoe/larry/SpeedyIsSlow");
        expectedTree.add("/TestRoot5/BadBadMoe/larry/daffy");
        expectedTree.add("Couldn't find curly");

        expectedResult=expectedTree;
        if (isVerbose) {
          h.enterTest(5,"Do several operations and test end state",expectedTree);
        } else {
          h.enterTest(5,"Do several operations and test end state",Boolean.TRUE);
        }

        makeTestRoot(number);

        larry_prot  = new Guid();
        curly_prot  = new Guid();
        moe_prot    = new Guid();
        daffy_prot  = new Guid();
        donald_prot = new Guid();
        speedy_prot = new Guid();

        larry  = makeEnv("larry",  testRootGuid,larry_prot);
        curly  = makeEnv("curly",  testRootGuid,curly_prot);
        moe    = makeEnv("moe",    testRootGuid,moe_prot);
        daffy  = makeEnv("daffy",  larry,daffy_prot);
        donald = makeEnv("donald", moe,donald_prot);
        speedy = makeEnv("speedy", larry,speedy_prot);  
        TupleStore.rename(moe,"BadBadMoe");
        TupleStore.delete(new Guid[] {curly});
        envsToDelete.remove(curly);
        TupleStore.rename(speedy,"SpeedyIsSlow");        
        TupleStore.move(larry,moe);
        TupleStore.move(moe,testRootGuid);  
        resultTree = Collections.synchronizedSortedSet(new TreeSet());
        allPaths("",testRootGuid,resultTree);      

        checkDescriptor(new Environment.Descriptor(larry,"larry",
                                                   moe,larry_prot),
                        resultTree);    
        checkDescriptor(new Environment.Descriptor(curly,"curly",
                                                   testRootGuid,curly_prot),
                                                   resultTree);    
        checkDescriptor(new Environment.Descriptor(moe,"BadBadMoe",
                                                   testRootGuid,moe_prot),
                                                   resultTree);    
        checkDescriptor(new Environment.Descriptor(daffy,"daffy",
                                                   larry,daffy_prot),
                                                   resultTree);    
        checkDescriptor(new Environment.Descriptor(donald,"donald",
                                                   moe,donald_prot),
                                                   resultTree);    
        checkDescriptor(new Environment.Descriptor(speedy,"SpeedyIsSlow",
                                                   larry,speedy_prot),
                                                   resultTree);    

        return resultTree;
      case 6:
      case 7:
      case 8:
      case 9:
      case 10:
      {
        /**************************************************
         *
         * Component test utilizing pure application level
         * functionality.
         *
         *************************************************/
 
        //The bind test component
        BindTestComponent btc; 
        //An environment for my BindTestComponent to live in
        Environment btEnvironment;
        String testDescription=null;
       
        switch(number) {
          case 6: 
            expectedResult=makeExpected6();
            testDescription="Test bind, READ, PUT";
            break;
          case 7: 
            expectedResult=makeExpected7();
            testDescription="Test listen";
            break;
          case 8: 
            expectedResult=makeExpected8();
            testDescription="Test query";
            break;
          case 9: 
            expectedResult=makeExpected9();
            testDescription="Test complex reads";
            break;
          case 10:
            expectedResult=makeExpected10(); 
            testDescription="Test concurrent operations";
            break;
        }
        if (isVerbose) {
          h.enterTest(number,testDescription,expectedResult);
        } else {
          h.enterTest(number,testDescription,Boolean.TRUE);
        }

        //Create the new environment
        btEnvironment=Environment.create(null,env.getId(),"TestRoot"+number,
                                         false);
        //Create the new component
        btc=new BindTestComponent(btEnvironment,number,h,envsToDelete);
        //link the request and main handlers
        btEnvironment.link("main","main",btc);
        btc.link("request","request",btEnvironment);
        //activate
        Environment.activate(null,btEnvironment.getId());
        //wait for the BindTestComponent to signal it is finished
        while(true) {
          synchronized(btc.testResponse) {
            if (btc.done==true) {
              break;
            }
          }
          Thread.sleep(500);
        }
        //Wait for it to really finish
        Thread.sleep(1500);
        //Terminate the bind test environment
        Environment.terminate(null,btEnvironment.getId());
        Collections.sort(btc.testResponse);
        
        return btc.testResponse;
      }
      case 11:
      case 12:
      case 13:
      case 14:
      case 15:
      case 16:
      case 17:
      case 18:
      case 19:
      {
        /**************************************************
         *
         * Tests that require access both to prividged 
         * access methods and normal component methods 
         *
         *************************************************/
 
        //The bind test component
        BindTestComponent btc; 
        //An environment for my BindTestComponent to live in
        Environment btEnvironment;
        String testDescription=null;

        switch(number) {
          case 11:
            testDescription="Test environment clones";
            expectedResult=makeExpected11();
            break;
          case 12:
            expectedResult=makeExpected12();
            testDescription="Tests clone border cases";
            break;
          case 13:
            expectedResult=makeExpected13();
            testDescription="Tests cloning two environments";
            break;
          case 14:
            expectedResult=makeExpected14();
            testDescription="Tests move with commit";
            break;
          case 15:
            expectedResult=makeExpected15();
            testDescription="Test move with abort";
            break;
          case 16:
            expectedResult=makeExpected16();
            testDescription="Test moving two environments";
            break;
          case 17:
            expectedResult=makeExpected17();
            testDescription="Killing a bind kills a child listen";
            break;
          case 18:
            expectedResult=makeExpected18();
            testDescription="Killing a bind kills many child listens";
            break;
          case 19:
            expectedResult=makeExpected19();
            testDescription="Killing a bind kills many child queries";
            break;
        }
        if (isVerbose) {
          h.enterTest(number,testDescription,expectedResult);
        } else {
          h.enterTest(number,testDescription,Boolean.TRUE);
        }
        //Create the new environment
        btEnvironment=Environment.create(null,env.getId(),"TestRoot"+number,
                                         false);
        //Create the new component
        btc=new BindTestComponent(btEnvironment,number,h,envsToDelete);
        //link the request and main handlers
        btEnvironment.link("main","main",btc);
        btc.link("request","request",btEnvironment);
        //activate
        Environment.activate(null,btEnvironment.getId());
        //wait for the BindTestComponent to signal it is finished
        while(true) {
          synchronized(btc.testResponse) {
            if (btc.done==true) {
              break;
            }
          }
          Thread.sleep(500);
        }
        //Wait for it to really finish
        Thread.sleep(1500);
        //Terminate the bind test environment
        Environment.terminate(null,btEnvironment.getId());
        Collections.sort(btc.testResponse);
        return btc.testResponse;
      }
    }
    return null;
  }
}

