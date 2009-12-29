package edu.cs.hku.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {
	
	private static DatabaseManager instance = null;
	
	static String userID, password, hostname, dbName, URL;
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
			instance =  new DatabaseManager("root", "wh19830", 
					"localhost", "contextdiversity");//the default uID, pwd, hostname, dbname			
			return instance;
		}
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

	public static ResultSet query(String sql){
		ResultSet rs = null;
		Statement s = null;
		try {
			s = conn.createStatement();
			s.execute(sql);
			rs = s.getResultSet();
			
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				s.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return rs;
	}
	
	public static void update(String sql){
		Statement s = null;
		
		try {
			s = conn.createStatement();
			s.executeUpdate(sql);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally{
			try {
				s.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}	
		}
	}
	
	public static void main(String[] args) {
		System.getProperty("java.class.path");
	}

}
