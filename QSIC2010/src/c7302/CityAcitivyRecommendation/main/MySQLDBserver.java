/**
 * HSQLDBserver.java
 * jCOLIBRI2 framework. 
 * @author Juan A. Recio-Garc�a.
 * GAIA - Group for Artificial Intelligence Applications
 * http://gaia.fdi.ucm.es
 * 04/07/2007
 */
package c7302.CityAcitivyRecommendation.main;

import org.hsqldb.Server;

/**
 * Creates a data base server with the tables for the examples/tests using the
 * HSQLDB library.
 * 
 */
public class MySQLDBserver {
	static boolean initialized = false;

	private static Server server;

	/**
	 * Initialize the server
	 */
	public static void init() {
		if (initialized)
			return;
		org.apache.commons.logging.LogFactory.getLog(MySQLDBserver.class).info(
				"Creating data base ...");

		server = new Server();
		server.setDatabaseName(0, "accommodation");
		server.setDatabasePath(0,
				"mem:accommodation;sql.enforce_strict_size=true");

		server.setDatabaseName(1, "currency");
		server.setDatabasePath(1, "mem:currency;sql.enforce_strict_size=true");

		server.setDatabaseName(2, "dining");
		server.setDatabasePath(2, "mem:dining;sql.enforce_strict_size=true");

		server.setDatabaseName(3, "entertainment");
		server.setDatabasePath(3,
				"mem:entertainment;sql.enforce_strict_size=true");

		server.setDatabaseName(4, "householdshopping");
		server.setDatabasePath(4,
				"mem:householdshopping;sql.enforce_strict_size=true");

		server.setDatabaseName(5, "mallshopping");
		server.setDatabasePath(5,
				"mem:mallshopping;sql.enforce_strict_size=true");

		server.setDatabaseName(6, "tourism");
		server.setDatabasePath(6, "mem:tourism;sql.enforce_strict_size=true");

		server.setDatabaseName(7, "userinteraction");
		server.setDatabasePath(7,
				"mem:userinteraction;sql.enforce_strict_size=true");

		server.setLogWriter(null);
		server.setErrWriter(null);
		server.setSilent(true);
		server.start();

		initialized = true;

	}

	/**
	 * Shutdown the server
	 */
	public static void shutDown() {

		if (initialized) {
			server.stop();
			initialized = false;
		}
	}

	/**
	 * Testing method
	 */
	public static void main(String[] args) {
		MySQLDBserver.init();
		MySQLDBserver.shutDown();
		System.exit(0);

	}

}
