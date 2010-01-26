package ccr.help;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
	
	private static DatabaseManager instance = null;
	
	public static int max_allowed_packet = 10*1024*1024;//10M
	
	private static String userID, password, hostname, dbName, URL;
	static Connection conn;
	
	private DatabaseManager(String uID, String pwd, String hostname, String database){
		userID = uID;
		password = pwd;
		hostname = hostname;
		dbName = database; 
		URL = "jdbc:mysql://"+ hostname +"/" + dbName + "?user="
		     + userID +"&password=" + pwd ;
		connect(URL);		
	}
	
	public static DatabaseManager getInstance(String uID, String pwd, String hostname, String database){
		if(instance!=null){
			return instance;
		}else{			
			return new DatabaseManager(uID, pwd, hostname, database);
		}
	}
	
	
	public static DatabaseManager getInstance(){
		if(instance != null){
			return instance;
		}else{
			//2010-01-22: set the parameters correctly.
			userID = (userID == null? "root": userID);
			password = (password == null? "wh19830": password);
			hostname = (hostname == null? "localhost": hostname);
			dbName = (dbName == null? "contextdiversity": dbName);
			
			instance = new DatabaseManager(userID, password, hostname, dbName);
//			instance =  new DatabaseManager("root", "wh19830", 
//					"localhost", "contextdiversity");//the default uID, pwd, hostname, dbname			
			return instance;
		}
	}
	
	public static void setDatabase(String database){
		dbName =database;
	}
	
	private static boolean connect(String URL){
		
		try {
			Class.forName("com.mysql.jdbc.Driver").newInstance();
			conn = DriverManager.getConnection(URL);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	private static boolean disConnect(){
		if(conn!=null){
			try {
				conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return false;
			}
		}
		return true;
	}

	public ResultSet query(String sql){
		ResultSet rs = null;
		Statement s = null;
		try {
			s = conn.createStatement();
			s.execute(sql);
			rs = s.getResultSet();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		return rs;
	}
	
	public void update(String sql){
		Statement s = null;
		
		try {
			s = conn.createStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) {
		System.getProperty("java.class.path");
	}

}
