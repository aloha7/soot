package context.arch.comm.language;

import org.xml.sax.Parser;
import org.xml.sax.DocumentHandler;
import org.xml.sax.helpers.ParserFactory;
import org.xml.sax.SAXException;
import org.xml.sax.HandlerBase;
import org.xml.sax.AttributeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.Reader;
import java.util.Hashtable;

import context.arch.comm.DataObject;

/**
 * This class provides access to the SAX XML parsing code using the specified
 * XML parser class/driver.  It implements ParserInterface and extends 
 * HandlerBase
 *
 * @see context.arch.comm.language.DecoderInterface
 * @see org.xml.sax.HandlerBase
 */
public class SAX_XMLDecoder extends HandlerBase implements DecoderInterface {

  /**
   * Debug flag. Set to true to see debug messages.
   */
  private static final boolean DEBUG = false;

  /**
   * The default SAX XML decoder is com.microstar.xml.SAXDriver (AElfred)
   */
  public static final String DEFAULT_SAX_XML_DECODER = "com.microstar.xml.SAXDriver";

  /**
   * The AElfred SAX XML decoder
   */
  public static final String AELFRED_SAX_XML_DECODER = "com.microstar.xml.SAXDriver";

  /**
   * The language for this class is XML
   */
  public static final String LANGUAGE = "XML";

  private Parser parser;
  private String decoderDriver;
  private DataObject data;

  /**
   * Basic constructor which uses the default XML parser and sets the
   * document handler to this class
   *
   * @exception context.arch.comm.language.InvalidDecoderException when the
   *		given decoder can not be created
   * @see #DEFAULT_SAX_XML_DECODER
   */
  public SAX_XMLDecoder() throws InvalidDecoderException {
    decoderDriver = DEFAULT_SAX_XML_DECODER;
    try { 
      parser = ParserFactory.makeParser(decoderDriver);
      DocumentHandler handler = this;
      parser.setDocumentHandler(handler);
    } catch (IllegalAccessException iae) {
        System.out.println("SAX_XMLDecoder IllegalAccess: "+iae);
        throw new InvalidDecoderException();
    } catch (InstantiationException ie) {
        System.out.println("SAX_XMLDecoder Instantiation: "+ie);
        throw new InvalidDecoderException();
    } catch (ClassNotFoundException cnfe) {
        System.out.println("SAX_XMLDecoder ClassNotFound: "+cnfe);
        throw new InvalidDecoderException();
    }
  }

  /**
   * This method decodes the given XML data and returns the result in
   * a DataObject.  It calls the parser created in the constructor
   *
   * @param XMLdata XML data to be decoded
   * @return the DataObject containing the results of the decoded XML data
   * @exception context.arch.comm.language.DecodeException when the
   *		given XML data can not be decoded
   * @see org.xml.sax.Parser#parse(InputSource)
   */
  public DataObject decodeData(Reader XMLdata) throws DecodeException {
    try {
      parser.parse(new InputSource(XMLdata));
//System.out.println(this.getClassName() + ":\r\n" + data);
      return data;
    } catch (IOException ioe) {
        System.out.println("SAX_XMLParser parse IOException: "+ioe);
        throw new DecodeException();
    } catch (SAXException se) {
        System.out.println("SAX_XMLParser parse SAXException: "+se);
        throw new DecodeException();
    }
  }

  /**
   * Returns the language being used in encoding and decoding
   *
   * @return the language being used in encoding and decoding
   * @see #LANGUAGE
   */
  public String getLanguage() {
    return LANGUAGE;
  }

  /**
   * Returns the name of the parser driver being used for encoding and decoding
   *
   * @return the name of the parser driver being used for encoding and decoding
   */
  public String getClassName() {
    return this.getClass().getName();
  }

  /**
   * Receive notification of the beginning of the document.
   * Creates a new DataObject
   *
   * @see context.arch.comm.DataObject
   */
  public void startDocument() {
    data = new DataObject();
  }

  /**
   * Receive notification of the end of the document.  Empty method.
   */
  public void endDocument() {
  }

  /**
   * Receive notification of the start of a new element.
   * Adds attributes to the DataObject
   *
   * @param name String name of new element
   * @param attributes AttributeList object containing attributes for new element
   * @see context.arch.comm.DataObject#addElement(String,Hashtable)
   */
  public void startElement(String name, AttributeList attributes) {
    Hashtable hash = new Hashtable();
    for (int i=0; i<attributes.getLength(); i++) {
      hash.put(attributes.getName(i).trim(), attributes.getValue(i).trim());
    }
    data.addElement(name.trim(), hash);
  }

  /**
   * Receive notification of the end of an element.  Closes the DataObject.
   *
   * @param name String name of ended element
   * @see context.arch.comm.DataObject#closeElement(String)
   */
  public void endElement(String name) {
    data.closeElement(name.trim());
  }

 /**
  * Receive notification of non-element and non-attribute characters ignoring whitespace.
  * Adds the value to the current element
  *
  * @param ch array of characters read in
  * @param start start position in the array
  * @param length number of characters to read in from the array
  * @see context.arch.comm.DataObject#addValue(String)
  */
 public void characters(char ch[], int start, int length) {
   if ((new String(ch,start,length)).trim().length() != 0) {
     data.addValue(new String(ch,start,length).trim());
   }
 }
}
