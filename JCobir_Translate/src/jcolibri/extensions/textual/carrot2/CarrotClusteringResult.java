/**
 * CarrotClusteringResult.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-García.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 17/05/2007
 */
package jcolibri.extensions.textual.carrot2;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import jcolibri.extensions.textual.lucene.LuceneDocument;
import jcolibri.extensions.textual.lucene.LuceneIndex;

import org.carrot2.core.clustering.RawCluster;
import org.carrot2.core.clustering.RawDocument;
import org.carrot2.core.impl.ArrayOutputComponent;

/**
 * Result of a clustering.
 * Uses an internal class "Cluster" that stores the requiered information for each cluster:
 * <ul>
 * <li>The labels assigned to the cluster.
 * <li>The documents that belong to the cluster (LuceneDocuments).
 * </ul>
 * 
 * @author Juan A. Recio-García
 * @version 1.0
 * @see jcolibri.extensions.textual.lucene.LuceneDocument
 */
public class CarrotClusteringResult {

	private ArrayList clusters;
	
	/**
	 * Internal class that stores the labels and documents for a cluster.
	 * @author Juan A. Recio-García
	 */
	public class Cluster
	{
		List labels;
		List docs;
		protected Cluster(List labels, List docs)
		{
			this.labels = labels;
			this.docs   = docs;
		}
		/**
		 * @return the documents of the cluster
		 */
		public List getDocs() {
			return docs;
		}
		/**
		 * @return the labels of the cluster
		 */
		public List getLabels() {
			return labels;
		}
		
		
	}
	
	/**
	 * Creates a CarrotClusteringResult object from the Carrot2 output.
	 */
	protected CarrotClusteringResult(ArrayOutputComponent.Result result, LuceneIndex index)
	{
		clusters = new ArrayList();
		
        final List carrotClusters = result.clusters;
        for (Iterator i = carrotClusters.iterator(); i.hasNext(); )
        {
            RawCluster rawc = (RawCluster) i.next();
            List labels = rawc.getClusterDescription();
            ArrayList docs = new ArrayList();
            for (Iterator d = rawc.getDocuments().iterator(); d.hasNext(); ) 
            {
                RawDocument document = (RawDocument) d.next();
                LuceneDocument ld = index.getDocument(document.getTitle());
                docs.add(ld);
            }
            
            CarrotClusteringResult.Cluster c =  new Cluster(labels,docs);
            clusters.add(c);
        }

	}
	
	/**
	 * Returns the list of clusters.
	 */
	public List getClusters()
	{
		return this.clusters;
	}
	
}
