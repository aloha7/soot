// This is mutant program.
// Author : ysma

package context.apps.PersonPresenceApp;


import context.arch.BaseObject;
import context.arch.comm.DataObject;
import context.arch.server.SUser;
import context.arch.handler.Handler;
import context.arch.InvalidMethodException;
import context.arch.MethodException;
import context.arch.subscriber.Subscriber;
import context.arch.subscriber.Callbacks;
import context.arch.comm.language.DecodeException;
import context.arch.comm.language.InvalidDecoderException;
import context.arch.comm.language.EncodeException;
import context.arch.comm.language.InvalidEncoderException;
import context.arch.comm.protocol.ProtocolException;
import context.arch.comm.protocol.InvalidProtocolException;
import context.arch.interpreter.IIButton2Name;
import context.arch.util.Error;
import context.arch.storage.AttributeFunctions;
import context.arch.storage.Attributes;
import context.arch.storage.Attribute;
import context.arch.storage.AttributeNameValues;
import context.arch.storage.AttributeNameValue;
import context.arch.storage.Retrieval;
import context.arch.storage.Conditions;
import context.arch.storage.StorageObject;
import context.arch.storage.Storage;
import context.arch.storage.RetrievalResults;
import context.arch.util.ContextTypes;
import java.util.Vector;
import java.util.Hashtable;


public class UserApp implements context.arch.handler.Handler
{

    public static final int DEFAULT_PORT = 5555;

    public static final java.lang.String SUBSCRIBER_ID = "UserApp";

    private int numHandles = 0;

    private int port;

    private java.lang.String userid;

    public UserApp( java.lang.String user, java.lang.String usHost, int usPort )
    {
        this( user, DEFAULT_PORT, usHost, usPort );
    }

    public UserApp( java.lang.String user, int localport, java.lang.String usHost, int usPort )
    {
        context.arch.BaseObject server = new context.arch.BaseObject( localport );
        port = localport;
        java.lang.String serverId = SUser.CLASSNAME + "_" + user;
        System.out.println( "**********************************************" );
        context.arch.comm.DataObject version = server.getVersion( usHost, usPort, serverId + "blah" );
        java.lang.String versionError = (new context.arch.util.Error( version )).getError();
        java.lang.String versionNumber = null;
        if (versionError.equals( Error.NO_ERROR )) {
            versionNumber = (java.lang.String) version.getDataObject( SUser.VERSION ).getValue().firstElement();
        }
        System.out.println( "Version with invalid id: " + version + "\n" );
        System.out.println( "error = " + versionError + ", version number = " + versionNumber );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject version2 = server.getVersion( usHost, usPort, serverId );
        java.lang.String versionError2 = (new context.arch.util.Error( version2 )).getError();
        java.lang.String versionNumber2 = null;
        if (versionError2.equals( Error.NO_ERROR )) {
            versionNumber2 = (java.lang.String) version2.getDataObject( SUser.VERSION ).getValue().firstElement();
        }
        System.out.println( "Version with valid id: " + version2 + "\n" );
        System.out.println( "error = " + versionError2 + ", version number = " + versionNumber2 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject attributes = server.getWidgetAttributes( usHost, usPort, serverId + "blah" );
        java.lang.String attributesError = (new context.arch.util.Error( attributes )).getError();
        context.arch.storage.Attributes atts = null;
        if (attributesError.equals( Error.NO_ERROR )) {
            atts = new context.arch.storage.Attributes( attributes );
        }
        System.out.println( "Attributes with invalid id: " + attributes + "\n" );
        System.out.println( "error = " + attributesError + ", attributes = " + atts );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject attributes2 = server.getWidgetAttributes( usHost, usPort, serverId );
        java.lang.String attributesError2 = (new context.arch.util.Error( attributes2 )).getError();
        context.arch.storage.Attributes atts2 = null;
        if (attributesError2.equals( Error.NO_ERROR )) {
            atts2 = new context.arch.storage.Attributes( attributes2 );
        }
        System.out.println( "Attributes with valid id: " + attributes2 + "\n" );
        System.out.println( "error = " + attributesError2 + ", attributes = " + atts2 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject callbacks = server.getWidgetCallbacks( usHost, usPort, serverId );
        java.lang.String callbacksError = (new context.arch.util.Error( callbacks )).getError();
        context.arch.subscriber.Callbacks calls = null;
        if (callbacksError.equals( Error.NO_ERROR )) {
            calls = new context.arch.subscriber.Callbacks( callbacks );
        }
        System.out.println( "Callbacks with valid id: " + callbacks + "\n" );
        System.out.println( "error = " + callbacksError + ", callbacks = " + calls );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes prePollAtts = new context.arch.storage.Attributes();
        prePollAtts.addAttribute( SUser.USERNAME );
        context.arch.comm.DataObject poll = server.pollWidget( usHost, usPort, serverId, prePollAtts );
        java.lang.String pollError = (new context.arch.util.Error( poll )).getError();
        context.arch.storage.AttributeNameValues pollAtts = null;
        if (pollError.equals( Error.NO_ERROR )) {
            pollAtts = new context.arch.storage.AttributeNameValues( poll );
        }
        System.out.println( "Poll before any data: " + poll + "\n" );
        System.out.println( "error = " + pollError + ", attributes = " + pollAtts );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject retrieve = server.retrieveDataFrom( usHost, usPort, serverId );
        java.lang.String retrieveError = (new context.arch.util.Error( retrieve )).getError();
        context.arch.storage.RetrievalResults retrieveData = null;
        if (retrieveError.equals( Error.NO_ERROR )) {
            retrieveData = new context.arch.storage.RetrievalResults( retrieve );
        }
        System.out.println( "Retrieval before any data: " + retrieve + "\n" );
        System.out.println( "error = " + retrieveError + ", retrieval = " + retrieveData );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes preUpdateAtts = new context.arch.storage.Attributes();
        preUpdateAtts.addAttribute( SUser.USERNAME );
        context.arch.comm.DataObject updatePoll = server.updateAndPollWidget( usHost, usPort, serverId, preUpdateAtts );
        java.lang.String updatePollError = (new context.arch.util.Error( updatePoll )).getError();
        context.arch.storage.AttributeNameValues updatePollAtts = null;
        if (updatePollError.equals( Error.NO_ERROR )) {
            updatePollAtts = new context.arch.storage.AttributeNameValues( updatePoll );
        }
        System.out.println( "Update&Poll before any data: " + updatePoll + "\n" );
        System.out.println( "error = " + updatePollError + ", attributes = " + updatePollAtts );
        System.out.println( "**********************************************\n" );
        context.arch.util.Error done = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "blah", "presenceUpdate" );
        System.out.println( "Subscription with invalid callback: " + done.getError() );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes subscribeAtts = new context.arch.storage.Attributes();
        subscribeAtts.addAttribute( SUser.USERNAME );
        subscribeAtts.addAttribute( "blah" );
        context.arch.util.Error done2 = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "PersonNamePresence_here_update", "presenceUpdate", subscribeAtts );
        System.out.println( "Subscription with invalid attributes: " + done2.getError() );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Conditions subscribeConds = new context.arch.storage.Conditions();
        subscribeConds.addCondition( "blah", Storage.EQUAL, "blah" );
        context.arch.util.Error done3 = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "PersonNamePresence_here_update", "presenceUpdate", subscribeConds );
        System.out.println( "Subscription with invalid conditions: " + done3.getError() );
        System.out.println( "**********************************************\n" );
        context.arch.util.Error done4 = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "PersonNamePresence_here_update", "blah" );
        System.out.println( "Subscription valid with no attributes/conditions but invalid callback tag: " + done4.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Dock with the PersonPresence widget once please" );
        while (numHandles < 1) {
            try {
                Thread.sleep( 3000 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        System.out.println( "**********************************************" );
        context.arch.storage.Attributes returnPollAtts2 = new context.arch.storage.Attributes();
        returnPollAtts2.addAttribute( SUser.USERNAME );
        returnPollAtts2.addAttribute( "blah" );
        context.arch.comm.DataObject poll2 = server.pollWidget( usHost, usPort, serverId, returnPollAtts2 );
        java.lang.String pollError2 = (new context.arch.util.Error( poll2 )).getError();
        context.arch.storage.AttributeNameValues pollAtts2 = null;
        if (pollError2.equals( Error.NO_ERROR )) {
            pollAtts2 = new context.arch.storage.AttributeNameValues( poll2 );
        }
        System.out.println( "Poll with invalid attributes: " + poll2 + "\n" );
        System.out.println( "error = " + pollError2 + ", attributes = " + pollAtts2 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes returnUpdatePollAtts2 = new context.arch.storage.Attributes();
        returnUpdatePollAtts2.addAttribute( SUser.USERNAME );
        returnUpdatePollAtts2.addAttribute( "blah" );
        context.arch.comm.DataObject updatePoll2 = server.updateAndPollWidget( usHost, usPort, serverId, returnUpdatePollAtts2 );
        java.lang.String updatePollError2 = (new context.arch.util.Error( updatePoll2 )).getError();
        context.arch.storage.AttributeNameValues updatePollAtts2 = null;
        if (updatePollError2.equals( Error.NO_ERROR )) {
            updatePollAtts2 = new context.arch.storage.AttributeNameValues( updatePoll2 );
        }
        System.out.println( "Update&Poll with invalid attributes: " + updatePoll2 + "\n" );
        System.out.println( "error = " + updatePollError2 + ", attributes = " + updatePollAtts2 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes returnPollAtts3 = new context.arch.storage.Attributes();
        returnPollAtts3.addAttribute( SUser.USERNAME );
        returnPollAtts3.addAttribute( SUser.TIMESTAMP );
        context.arch.comm.DataObject poll3 = server.pollWidget( usHost, usPort, serverId, returnPollAtts3 );
        java.lang.String pollError3 = (new context.arch.util.Error( poll3 )).getError();
        context.arch.storage.AttributeNameValues pollAtts3 = null;
        if (pollError3.equals( Error.NO_ERROR ) || pollError3.equals( Error.INCOMPLETE_DATA_ERROR )) {
            pollAtts3 = new context.arch.storage.AttributeNameValues( poll3 );
        }
        System.out.println( "Poll with valid attributes: " + poll3 + "\n" );
        System.out.println( "error = " + pollError3 + ", attributes = " + pollAtts3 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes returnUpdatePollAtts3 = new context.arch.storage.Attributes();
        returnUpdatePollAtts3.addAttribute( SUser.USERNAME );
        returnUpdatePollAtts3.addAttribute( SUser.TIMESTAMP );
        context.arch.comm.DataObject updatePoll3 = server.updateAndPollWidget( usHost, usPort, serverId, returnUpdatePollAtts3 );
        java.lang.String updatePollError3 = (new context.arch.util.Error( updatePoll3 )).getError();
        context.arch.storage.AttributeNameValues updatePollAtts3 = null;
        if (updatePollError3.equals( Error.NO_ERROR )) {
            updatePollAtts3 = new context.arch.storage.AttributeNameValues( updatePoll3 );
        }
        System.out.println( "Update&Poll with valid attributes: " + updatePoll3 + "\n" );
        System.out.println( "error = " + updatePollError3 + ", attributes = " + updatePollAtts3 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes prePollAtts4 = new context.arch.storage.Attributes();
        prePollAtts4.addAttribute( Attributes.ALL );
        context.arch.comm.DataObject poll4 = server.pollWidget( usHost, usPort, serverId, prePollAtts4 );
        java.lang.String pollError4 = (new context.arch.util.Error( poll4 )).getError();
        context.arch.storage.AttributeNameValues pollAtts4 = null;
        if (pollError4.equals( Error.NO_ERROR )) {
            pollAtts4 = new context.arch.storage.AttributeNameValues( poll4 );
        }
        System.out.println( "Poll all attributes: " + poll4 + "\n" );
        System.out.println( "error = " + pollError4 + ", attributes = " + pollAtts4 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.Attributes preUpdateAtts4 = new context.arch.storage.Attributes();
        preUpdateAtts4.addAttribute( Attributes.ALL );
        context.arch.comm.DataObject updatePoll4 = server.updateAndPollWidget( usHost, usPort, serverId, preUpdateAtts4 );
        java.lang.String updatePollError4 = (new context.arch.util.Error( updatePoll4 )).getError();
        context.arch.storage.AttributeNameValues updatePollAtts4 = null;
        if (updatePollError4.equals( Error.NO_ERROR )) {
            updatePollAtts4 = new context.arch.storage.AttributeNameValues( updatePoll4 );
        }
        System.out.println( "Update&Poll all attributes: " + updatePoll4 + "\n" );
        System.out.println( "error = " + updatePollError4 + ", attributes = " + updatePollAtts4 );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock with the widget " + (5 - numHandles) + " more times" );
        while (numHandles < 5) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.util.Error done5 = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "PersonNamePresence_here_update", "presenceUpdate" );
        System.out.println( "Subscription with no attributes/conditions: " + done5.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock with the widget " + (10 - numHandles) + " more times" );
        while (numHandles < 10) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.storage.Attributes subscribeAtts2 = new context.arch.storage.Attributes();
        subscribeAtts2.addAttribute( SUser.USERNAME );
        subscribeAtts2.addAttribute( SUser.TIMESTAMP );
        context.arch.storage.Conditions subscribeConds2 = new context.arch.storage.Conditions();
        subscribeConds2.addCondition( SUser.USERNAME, Storage.EQUAL, "Anind Dey" );
        context.arch.util.Error done6 = server.subscribeTo( this, port, SUBSCRIBER_ID, usHost, usPort, serverId, "PersonNamePresence_here_update", "presenceUpdate", subscribeConds2, subscribeAtts2 );
        System.out.println( "Subscription with valid attributes/conditions: " + done6.getError() );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject retrieve2 = server.retrieveDataFrom( usHost, usPort, serverId, "blah" );
        java.lang.String retrieveError2 = (new context.arch.util.Error( retrieve2 )).getError();
        context.arch.storage.RetrievalResults retrieveData2 = null;
        if (retrieveError2.equals( Error.NO_ERROR )) {
            retrieveData2 = new context.arch.storage.RetrievalResults( retrieve2 );
        }
        System.out.println( "Retrieval after have data with invalid attributes: " + retrieve2 + "\n" );
        System.out.println( "error = " + retrieveError2 + ", retrieval2 = " + retrieveData2 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject retrieve3 = server.retrieveDataFrom( usHost, usPort, serverId, "blah", Storage.EQUAL, "blah" );
        java.lang.String retrieveError3 = (new context.arch.util.Error( retrieve3 )).getError();
        context.arch.storage.RetrievalResults retrieveData3 = null;
        if (retrieveError3.equals( Error.NO_ERROR )) {
            retrieveData3 = new context.arch.storage.RetrievalResults( retrieve3 );
        }
        System.out.println( "Retrieval after have data with invalid conditions: " + retrieve3 + "\n" );
        System.out.println( "error = " + retrieveError3 + ", retrieval3 = " + retrieveData3 );
        System.out.println( "**********************************************\n" );
        context.arch.comm.DataObject retrieve4 = server.retrieveDataFrom( usHost, usPort, serverId );
        java.lang.String retrieveError4 = (new context.arch.util.Error( retrieve4 )).getError();
        context.arch.storage.RetrievalResults retrieveData4 = null;
        if (retrieveError4.equals( Error.NO_ERROR )) {
            retrieveData4 = new context.arch.storage.RetrievalResults( retrieve4 );
        }
        System.out.println( "Retrieval after have data with no attributes or conditions: " + retrieve4 + "\n" );
        System.out.println( "error = " + retrieveError4 + ", retrieval4 = " + retrieveData4 );
        System.out.println( "**********************************************\n" );
        context.arch.storage.AttributeFunctions preRetrieveAtts5 = new context.arch.storage.AttributeFunctions();
        preRetrieveAtts5.addAttributeFunction( SUser.USERNAME );
        preRetrieveAtts5.addAttributeFunction( SUser.TIMESTAMP );
        context.arch.storage.Conditions preRetrieveConds5 = new context.arch.storage.Conditions();
        preRetrieveConds5.addCondition( SUser.USERNAME, Storage.EQUAL, "Anind Dey" );
        context.arch.comm.DataObject retrieve5 = server.retrieveDataFrom( usHost, usPort, serverId, new context.arch.storage.Retrieval( preRetrieveAtts5, preRetrieveConds5 ) );
        java.lang.String retrieveError5 = (new context.arch.util.Error( retrieve5 )).getError();
        context.arch.storage.RetrievalResults retrieveData5 = null;
        if (retrieveError5.equals( Error.NO_ERROR )) {
            retrieveData5 = new context.arch.storage.RetrievalResults( retrieve5 );
        }
        System.out.println( "Retrieval after have data with valid attributes and conditions: " + retrieve5 + "\n" );
        System.out.println( "error = " + retrieveError5 + ", retrieval5 = " + retrieveData5 );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock with the widget " + (15 - numHandles) + " more times" );
        while (numHandles < 15) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.subscriber.Subscriber unsub = new context.arch.subscriber.Subscriber( SUBSCRIBER_ID, server.getHostAddress(), port, "blah", "presenceUpdate", subscribeConds2, subscribeAtts2 );
        context.arch.util.Error unsubscribe = server.unsubscribeFrom( this, usHost, usPort, serverId, unsub );
        System.out.println( "Subscription removed with invalid data: " + unsubscribe.getError() );
        System.out.println( "**********************************************\n" );
        System.out.println( "Need to dock with the widget " + (20 - numHandles) + " more times" );
        while (numHandles < 20) {
            try {
                Thread.sleep( 500 );
            } catch ( java.lang.InterruptedException ite ) {
                System.out.println( "Interrupted thread: " + ite );
            }
        }
        context.arch.subscriber.Subscriber unsub2 = new context.arch.subscriber.Subscriber( SUBSCRIBER_ID, server.getHostAddress(), port, "PersonNamePresence_here_update", "presenceUpdate", subscribeConds2, subscribeAtts2 );
        context.arch.util.Error unsubscribe2 = server.unsubscribeFrom( this, usHost, usPort, serverId, unsub2 );
        System.out.println( "Subscription removed with valid data: " + unsubscribe2.getError() );
        System.out.println( "**********************************************\n" );
    }

    public context.arch.comm.DataObject handle( java.lang.String callback, context.arch.comm.DataObject data )
        throws context.arch.InvalidMethodException, context.arch.MethodException
    {
        if (callback.equals( "presenceUpdate" )) {
            context.arch.storage.AttributeNameValues atts = new context.arch.storage.AttributeNameValues( data );
            System.out.println( "\n\n\n**********************************************\n" );
            context.arch.storage.AttributeNameValue idAtt = atts.getAttributeNameValue( SUser.USERNAME );
            if (idAtt != null) {
                userid = (java.lang.String) idAtt.getValue();
                System.out.print( userid + " was seen at " );
            } else {
                System.out.print( "null was seen at " );
            }
            context.arch.storage.AttributeNameValue locAtt = atts.getAttributeNameValue( ContextTypes.LOCATION );
            if (locAtt != null) {
                java.lang.String locationValue = (java.lang.String) locAtt.getValue();
                System.out.print( locationValue + " at time " );
            } else {
                System.out.print( "null at time " );
            }
            context.arch.storage.AttributeNameValue timeAtt = atts.getAttributeNameValue( SUser.TIMESTAMP );
            if (timeAtt != null) {
                java.lang.String time = (java.lang.String) timeAtt.getValue();
                System.out.print( time + "\n" );
            } else {
                System.out.print( "null\n" );
            }
            System.out.println( "**********************************************\n" );
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
            System.out.println( "Attempting to create an UserApp on " + DEFAULT_PORT + " at " + argv[0] );
            context.apps.PersonPresenceApp.UserApp ua = new context.apps.PersonPresenceApp.UserApp( argv[0], argv[1], Integer.parseInt( argv[2] ) );
        } else {
            if (argv.length == 4) {
                System.out.println( "Attempting to create an UserApp on " + argv[2] + " at " + argv[0] );
                context.apps.PersonPresenceApp.UserApp ua = new context.apps.PersonPresenceApp.UserApp( argv[0], Integer.parseInt( argv[1] ), argv[2], Integer.parseInt( argv[3] ) );
            } else {
                System.out.println( "USAGE: java UserApp <username> [port] <SUserHost/ip> <SUserPort> " );
            }
        }
    }

}
