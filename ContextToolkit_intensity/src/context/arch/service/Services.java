package context.arch.service;

import context.arch.comm.DataObject;
import context.arch.service.helper.ServiceDescriptions;

import java.util.Hashtable;
import java.util.Enumeration;

/**
 * This class maintains a list of services.
 *
 * @see context.arch.service.Service
 */
public class Services extends Hashtable {

  /**
   * Basic empty constructor
   */
  public Services() {
    super();
  }

  /**
   * Adds the given Service object to the container.
   *
   * @param service Service to add
   */
  public void addService(Service service) {
    put(service.getName(),service);
//System.out.println(this.getClass().getName() + "(addService):\r\n" + service.getName());    
  }

  /**
   * Adds the given Services object to the container.
   *
   * @param services Services to add
   */
  public void addServices(Services services) {
    for (Enumeration e=services.elements(); e.hasMoreElements();) {
      addService((Service)e.nextElement());
    }
  }

  /**
   * Determines whether the given Service object is in the container
   *
   * @param service Name of the service to check
   * @return whether Service is in the container
   */
  public boolean hasService(String service) {
    return containsKey(service);
  }

  /**
   * Returns the number of Services in the container
   *
   * return the number of Services in the container
   */
  public int numServices() {
    return size();
  }

  /**
   * This method returns the Service with the given name
   * from this list of Services.
   *
   * @param name of the Service to return
   * @return Service with the given name
   */
  public Service getService(String name) {
    return (Service)get(name);
  }

  /**
   * Creates a ServiceDescriptions object and returns the DataObject
   * version of it
   *
   * @return Services object converted to an <SERVICES> DataObject
   */
  public DataObject toDataObject() {
    ServiceDescriptions descriptions = new ServiceDescriptions();
    for (Enumeration e = elements(); e.hasMoreElements();) {
      descriptions.addServiceDescription(((Service)e.nextElement()).getServiceDescription());
    }
    return descriptions.toDataObject();
  }  
  
  public String toString(){
	  StringBuffer sb = new StringBuffer();
	  for(Enumeration e = keys(); e.hasMoreElements();){
		  sb.append(e.nextElement()+"\r\n");  
	  }
	  return sb.toString();
  }
}

