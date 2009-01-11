package context.arch.storage;

import context.arch.comm.DataObject;

import java.util.Vector;

/**
 * This class is a container for the results of a retrieval request.
 */
public class RetrievalResults extends Vector {

  /**
   * Tag for retrieval results
   */
  public static final String RETRIEVAL_RESULTS = "retrievalResults";

  /**
   * Empty constructor
   */
  public RetrievalResults() {
  }

  /**
   * Constructor that takes a DataObject holding the callback info.
   * The DataObject is expected to contain the <RETRIEVAL_RESULTS> tag.
   *
   * @param data DataObject containing the results of a retrieval
   */
  public RetrievalResults(DataObject data) {
    DataObject retrieveData = data.getDataObject(RETRIEVAL_RESULTS);
    if (retrieveData == null) {
      return;
    }
    
    Vector retrieveAttsVec = retrieveData.getChildren();
    for (int i=0; i<retrieveAttsVec.size(); i++) {
      AttributeNameValues retrieveAtts = new AttributeNameValues((DataObject)retrieveAttsVec.elementAt(i));
      if (retrieveAtts != null) {
        addAttributeNameValues(retrieveAtts);
      }
    }
  }

  /** 
   * This method converts the RetrievalResults object to a DataObject
   *
   * @return RetrievalResults object converted to a <RETRIEVAL_RESULTS> DataObject
   */
  public DataObject toDataObject() {
    Vector v = new Vector();
    for (int i=0; i<numAttributeNameValues(); i++) {
      v.addElement(getAttributeNameValuesAt(i).toDataObject());
    }
    return new DataObject(RETRIEVAL_RESULTS, v);
  }

  /**
   * This method adds an AttributeNameValues object to this
   * container
   *
   * @param anvs AttributeNameValues object to be added
   */
  public void addAttributeNameValues(AttributeNameValues anvs) {
    addElement(anvs);
  }
  
  /**
   * This method retrieves the AttributeNameValues object at the
   * given index.
   *
   * @param index at which to retrieve the AttributeNameValues object
   * @return AttributeNameValues object at the given index
   */
  public AttributeNameValues getAttributeNameValuesAt(int index) {
    return (AttributeNameValues)elementAt(index);
  }

  /**
   * This method returns the number of AttributeNameValues objects
   * contained in this container.
   *
   * @return the number of AttributeNameValues objects in this container
   */
  public int numAttributeNameValues() {
    return size();
  }

}
