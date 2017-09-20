/**
 * @author Rahul Ashok for National University of Singapore
 *
 * Link to the AHU Database (MVPROJECT.AHUDATA)
 */
package org.nus.dlink.smartplug;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// JDK 7 and above
public class SaveDataToDatabase {
	private static Connection conn;
	public SaveDataToDatabase() {
		try {
			//System.out.println("Loading driver...");
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager
					.getConnection(
							"jdbc:mysql://iotlab.amazonaws.com:3306/",
							"iotlab", "password");
			//System.out.println("Driver loaded!");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void savePlugData(String mac, float power, float totalpower, int temperate) {

		try {
			if ((conn == null) || (conn.isClosed())){
				conn = DriverManager
						.getConnection(
								"jdbc:mysql://iotlab.amazonaws.com:3306/",
								"iotlab", "password");
			}
			PreparedStatement stmt = conn
					.prepareStatement("insert into PLUGDATA (macaddress, activepower, temperature, totalpower, time) values(?,?,?,?,?)");
			
			stmt.setString(1, mac);
			stmt.setFloat(2, power);
			stmt.setInt(3, temperate);
			stmt.setFloat(4, totalpower);
			stmt.setTimestamp(5, new Timestamp(System.currentTimeMillis()));
			int i = stmt.executeUpdate();

		} catch (SQLException ex) {
			ex.printStackTrace();
		} 
	}
	
	public void coseConnection(){
		try {
			if (conn != null){
				conn.close();
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}