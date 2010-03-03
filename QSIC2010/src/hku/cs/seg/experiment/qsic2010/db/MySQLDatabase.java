package hku.cs.seg.experiment.qsic2010.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLDatabase {
	private Connection m_Connection = null;
	private Statement m_Statement = null;
		
	private MySQLDatabase() {
		try {
			m_Connection = DriverManager.getConnection("jdbc:mysql://kzhaipc/accommodation?user=root&password=root");
			m_Statement = m_Connection.createStatement();
		}
		catch (SQLException ex) {
			System.out.println("SQLException: " + ex.getMessage());
			System.out.println("SQLSTate: " + ex.getSQLState());
			System.out.println("VendorError: " + ex.getErrorCode());
		}
	}

	private static MySQLDatabase m_MySQLDatabase;
	
	public static MySQLDatabase Me() {
		if (m_MySQLDatabase == null) {
			m_MySQLDatabase = new MySQLDatabase();
		} 
		return m_MySQLDatabase;
	}
	
	public Statement getStatement() {
		return m_Statement;
	}
	
	public static void main(String[] args) {
		Statement stmt = MySQLDatabase.Me().getStatement();
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM accommodation.accommodation_casebase a;");
			int i = 0;
			while (rs.next()) {
				i++;
			}
			System.out.println(i);
		} catch (SQLException e) {
			System.out.println(e.getMessage());
		}
	}
}
