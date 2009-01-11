// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.widget.WContactPresence;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.subscriber.Subscriber;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.Retrieval;
import context.arch.storage.Conditions;
import context.arch.storage.StorageObject;
import context.arch.storage.Storage;
import context.arch.storage.RetrievalResults;
import context.arch.util.Error;
import context.arch.subscriber.Callbacks;
import java.util.Vector;
import java.util.Hashtable;
import context.arch.widget.WPersonNamePresence2;


public class ContactPresenceApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "ContactPresenceApp";

    private int numHandles = 0;

    public ContactPresenceApp( java.lang.String location, java.lang.String ppwHost, int ppwPort )
    {
        this( location, DEFAULT_PORT, ppwHost, ppwPort );
    }

    public ContactPresenceApp( java.lang.String location, int localport, java.lang.String ppwHost, int ppwPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        int port = localport;
        java.lang.String widgetId = WContactPresence.CLASSNAME + "_" + location;
        context.arch.comm.DataObject attributes2 = server.getWidgetAttributes( ppwHost, ppwPort, widgetId );
        java.lang.String attributesError2 = (new context.arch.util.Error( attributes2 )).getError();
        context.arch.storage.Attributes atts2 = null;
        if (attributesError2.equals( Error.NO_ERROR )) {
            atts2 = new context.arch.storage.Attributes( attributes2 );
        }
        System.out.println( "Attributes with valid id: " + attributes2 + "\n" );
        System.out.println( "error = " + attributesError2 + ", attributes = " + atts2 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject callbacks = server.getWidgetCallbacks( ppwHost, ppwPort, widgetId );
        java.lang.String callbacksError = (new context.arch.util.Error( callbacks )).getError();
        context.arch.subscriber.Callbacks calls = null;
        if (callbacksError.equals( Error.NO_ERROR )) {
            calls = new context.arch.subscriber.Callbacks( callbacks );
        }
        System.out.println( "Callbacks with valid id: " + callbacks + "\n" );
        System.out.println( "error = " + callbacksError + ", callbacks = " + calls );
        System.out.println( "**********************************************\n" );
        context.arch.util.Error error = server.subscribeTo( this, port, SUBSCRIBER_ID, ppwHost, ppwPort, widgetId, WContactPresence.UPDATE, "presenceUpdate" );
        System.out.println( "Subscription with no attributes/conditions: " + error.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock " + (3 - numHandles) + " more times" );
        while (numHandles < 3) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.storage.Attributes atts = new context.arch.storage.Attributes();
        atts.addAttribute( WContactPresence.CONTACT_INFO );
        atts.addAttribute( WContactPresence.USERID );
        context.arch.util.Error error2 = server.subscribeTo( this, port, SUBSCRIBER_ID, ppwHost, ppwPort, widgetId, WContactPresence.UPDATE, "presenceUpdate", atts );
        System.out.println( "Subscription with some attributes (contact,userid) and no conditions: " + error2.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock " + (6 - numHandles) + " more times" );
        while (numHandles < 6) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.storage.Attributes atts3 = new context.arch.storage.Attributes();
        atts3.addAttribute( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME );
        atts3.addAttribute( WContactPresence.LOCATION );
        context.arch.util.Error error3 = server.subscribeTo( this, port, SUBSCRIBER_ID, ppwHost, ppwPort, widgetId, WContactPresence.UPDATE, "presenceUpdate", atts3 );
        System.out.println( "Subscription with some attributes (contactinfo.name,location) and no conditions: " + error3.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock " + (9 - numHandles) + " more times" );
        while (numHandles < 9) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.storage.Attributes atts4 = new context.arch.storage.Attributes();
        atts4.addAttribute( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME );
        atts4.addAttribute( WContactPresence.LOCATION );
        context.arch.storage.Conditions conds4 = new context.arch.storage.Conditions();
        conds4.addCondition( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME, Storage.EQUAL, "Anind" );
        conds4.addCondition( WContactPresence.USERID, Storage.EQUAL, "16AC850600000044" );
        context.arch.util.Error error4 = server.subscribeTo( this, port, SUBSCRIBER_ID, ppwHost, ppwPort, widgetId, WContactPresence.UPDATE, "presenceUpdate", conds4, atts4 );
        System.out.println( "Subscription with some attributes (contactinfo.name,location) and some conditions (contactinfo.name=Anind,userid=first): " + error4.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock " + (12 - numHandles) + " more times" );
        while (numHandles < 12) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.storage.Attributes prePollAtts = new context.arch.storage.Attributes();
        prePollAtts.addAttribute( WContactPresence.CONTACT_INFO );
        context.arch.comm.DataObject poll = server.pollWidget( ppwHost, ppwPort, widgetId, prePollAtts );
        java.lang.String pollError = (new context.arch.util.Error( poll )).getError();
        context.arch.storage.AttributeNameValues pollAtts = null;
        if (pollError.equals( Error.NO_ERROR )) {
            pollAtts = new context.arch.storage.AttributeNameValues( poll );
        }
        System.out.println( "Poll (contactInfo): " + poll + "\n" );
        System.out.println( "error = " + pollError + ", attributes = " + pollAtts );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes prePollAtts2 = new context.arch.storage.Attributes();
        prePollAtts2.addAttribute( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME );
        context.arch.comm.DataObject poll2 = server.pollWidget( ppwHost, ppwPort, widgetId, prePollAtts2 );
        java.lang.String pollError2 = (new context.arch.util.Error( poll2 )).getError();
        context.arch.storage.AttributeNameValues pollAtts2 = null;
        if (pollError2.equals( Error.NO_ERROR )) {
            pollAtts2 = new context.arch.storage.AttributeNameValues( poll2 );
        }
        System.out.println( "Poll (contactInfo.name): " + poll2 + "\n" );
        System.out.println( "error = " + pollError2 + ", attributes = " + pollAtts2 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes prePollAtts3 = new context.arch.storage.Attributes();
        prePollAtts3.addAttribute( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + "blah" );
        context.arch.comm.DataObject poll3 = server.pollWidget( ppwHost, ppwPort, widgetId, prePollAtts3 );
        java.lang.String pollError3 = (new context.arch.util.Error( poll3 )).getError();
        context.arch.storage.AttributeNameValues pollAtts3 = null;
        if (pollError3.equals( Error.NO_ERROR )) {
            pollAtts3 = new context.arch.storage.AttributeNameValues( poll3 );
        }
        System.out.println( "Poll (contactInfo.blah): " + poll3 + "\n" );
        System.out.println( "error = " + pollError3 + ", attributes = " + pollAtts3 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject retrieve3 = server.retrieveDataFrom( ppwHost, ppwPort, widgetId );
        java.lang.String retrieveError3 = (new context.arch.util.Error( retrieve3 )).getError();
        context.arch.storage.RetrievalResults retrieveData3 = null;
        if (retrieveError3.equals( Error.NO_ERROR )) {
            retrieveData3 = new context.arch.storage.RetrievalResults( retrieve3 );
        }
        System.out.println( "Retrieval: " + retrieve3 + "\n" );
        System.out.println( "error = " + retrieveError3 + ", retrieval3 = " + retrieveData3 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.AttributeFunctions retAtts = new context.arch.storage.AttributeFunctions();
        retAtts.addAttributeFunction( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME );
        context.arch.storage.Conditions retConds = new context.arch.storage.Conditions();
        retConds.addCondition( WContactPresence.CONTACT_INFO + Attributes.SEPARATOR_STRING + WContactPresence.NAME, Storage.EQUAL, "Anind" );
        retConds.addCondition( WContactPresence.USERID, Storage.EQUAL, "16AC850600000044" );
        context.arch.comm.DataObject retrieve4 = server.retrieveDataFrom( ppwHost, ppwPort, widgetId, new context.arch.storage.Retrieval( retAtts, retConds ) );
        java.lang.String retrieveError4 = (new context.arch.util.Error( retrieve4 )).getError();
        context.arch.storage.RetrievalResults retrieveData4 = null;
        if (retrieveError4.equals( Error.NO_ERROR )) {
            retrieveData4 = new context.arch.storage.RetrievalResults( retrieve4 );
        }
        System.out.println( "Retrieval with atts and conds: " + retrieve4 + "\n" );
        System.out.println( "error = " + retrieveError4 + ", retrieval4 = " + retrieveData4 );
        System.out.println( "**********************************************\n" );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "presenceUpdate" )) {
            context.arch.storage.AttributeNameValues attributes = new context.arch.storage.AttributeNameValues( data );
            System.out.println( "Subscription: " + attributes + "\n\n" );
            numHandles++;
        } else {
            numHandles++;
            throw new context.arch.InvalidMethodException( Error.UNKNOWN_CALLBACK_ERROR );
        }
        return null;
    }

    public static void main( java.lang.String[] argv )
    {
        if (argv.length == 3) {
            System.out.println( "Attempting to create a ContactPresenceApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.ContactPresenceApp cpa = new context.apps.PersonPresenceApp.ContactPresenceApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create a ContactPresenceApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.ContactPresenceApp cpa = new context.apps.PersonPresenceApp.ContactPresenceApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java ContactPresenceApp <location> [port] <WContactPresenceHost/ip> <WContactPresencePort>" );
            }
        }
    }

}
