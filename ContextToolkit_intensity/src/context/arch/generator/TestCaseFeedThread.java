package context.arch.generator;

import java.util.Hashtable;
import java.util.Vector;

import context.apps.Tour.TourApp;
import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.server.STourId;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.subscriber.Subscriber;
import context.arch.util.Constants;
import context.arch.widget.WPersonNamePresence2;
import context.arch.widget.WTourDemo;
import context.arch.widget.WTourEnd;
import context.arch.widget.WTourRegistration;
import context.arch.widget.Widget;

public class TestCaseFeedThread implements Runnable {

//	private Vector widgetSet;
	private String info;
	private IButtonData data;
	private String serverHost;
	private int serverPort;

	public TestCaseFeedThread(/*Vector widgets,*/ String information,
			IButtonData datum, String serverHosts, int serverPorts) {
//		this.widgetSet = widgets;
		this.info = information;
		this.data = datum;
		this.serverHost = serverHosts;
		this.serverPort = serverPorts;
	}

	public void run() {
//		for (int i = 0; i < widgetSet.size(); i++) {
			BaseObject server = new BaseObject(PositionIButton
					.getAvailablePort());

			// 1.prepare data
			Vector m = new Vector();
			AttributeNameValues subAtts = new AttributeNameValues();
		    subAtts.addAttributeNameValue(WTourRegistration.NAME,"Wang Huai");
		    subAtts.addAttributeNameValue(WTourRegistration.AFFILIATION,"HKU");
		    subAtts.addAttributeNameValue(WTourRegistration.EMAIL,"dragonwanghuai@gmail.com");
		    AttributeNameValues atts = new AttributeNameValues();
		    atts.addAttributeNameValue(WTourRegistration.CONTACT_INFO,subAtts,Attribute.STRUCT);		  		    			   
		    atts.addAttributeNameValue(WTourRegistration.INTERESTS,"context,application,capture");
		    atts.addAttributeNameValue(WTourRegistration.DISPLAY_DEVICE,"127.0.0.1");			
			m.addElement(atts.toDataObject());

			
			
//			 DataObject subid = new DataObject(Subscriber.SUBSCRIBER_ID, sub.getId());
//	          DataObject callbackTag = new DataObject(Subscriber.CALLBACK_TAG, sub.getTag());
//	          Vector v = new Vector();
//	          v.addElement(subid);
//	          v.addElement(callbackTag);
//	          v.addElement(atts.getSubset(sub.getAttributes()).toDataObject());
//	          DataObject send = new DataObject(Subscriber.SUBSCRIPTION_CALLBACK, v);
//	          String host = sub.getHostName();
//	          int port = new Integer(sub.getPort()).intValue();
//	          try {
//	            result = userRequest(send, Subscriber.SUBSCRIPTION_CALLBACK, host, port);
	            
	            
			// 2.prepare callbacks
			String callBack = "";
			if (info.equals(WTourRegistration.UPDATE)) {
				callBack = WTourRegistration.CLASSNAME
						+ WTourRegistration.SPACER + "test"
						+ WTourRegistration.SPACER + info;
			} else if (info.equals(WTourEnd.END)) {
				callBack = WTourEnd.CLASSNAME + WTourEnd.SPACER + "test"
						+ WTourEnd.SPACER + info;
			} else if ((info.equals(WTourDemo.INTEREST) || (info
					.equals(WTourDemo.VISIT)))) {
				callBack = WTourDemo.CLASSNAME + WTourDemo.SPACER + "test"
						+ WTourDemo.SPACER + info;
			}

			Vector v = new Vector();
			v.addElement(new DataObject(Subscriber.SUBSCRIBER_ID,
					STourId.CLASSNAME + "_01020304"));
			// v.addElement(new DataObject(Subscriber.CALLBACK_TAG,
			// WTourDemo.CLASSNAME + WTourDemo.SPACER + "test"
			// + WTourDemo.SPACER + "interest"));

			v.addElement(new DataObject(Subscriber.CALLBACK_TAG, callBack));
			v.addElement(new DataObject(
					AttributeNameValues.ATTRIBUTE_NAME_VALUES, m));
			
			// 3. package all info in DataObject
			DataObject send = new DataObject(Subscriber.SUBSCRIPTION_CALLBACK, v);

			try {
				server.userRequest(send, Subscriber.SUBSCRIPTION_CALLBACK,
						this.serverHost, this.serverPort);
			} catch (EncodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (DecodeException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidDecoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InvalidEncoderException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
//	}

}
