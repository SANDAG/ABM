package org.sandag.abm.reporting.emfac2011;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import nl.tudelft.simulation.logger.Logger;

import com.pb.sawdust.util.exceptions.RuntimeIOException;
import com.pb.sawdust.util.exceptions.RuntimeWrappingException;
import com.pb.sawdust.util.property.PropertyDeluxe;

/**
 * 
 * @author Wu.Sun@sandag.org 1/21/2014
 * 
 */

public class Emfac2011SqlUtil {

	private final String connectionUrl;

	public Emfac2011SqlUtil(PropertyDeluxe properties) {
		connectionUrl = formConnectionUrl(
				properties
						.getString(Emfac2011Properties.REPORTS_DATABASE_IPADDRESS_PROPERTY),
				properties
						.getInt(Emfac2011Properties.REPORTS_DATABASE_PORT_PROPERTY),
				properties
						.getString(Emfac2011Properties.REPORTS_DATABASE_NAME_PROPERTY),
				properties
						.hasKey(Emfac2011Properties.REPORTS_DATABASE_USERNAME_PROPERTY) ? properties
						.getString(Emfac2011Properties.REPORTS_DATABASE_USERNAME_PROPERTY)
						: null,
				properties
						.hasKey(Emfac2011Properties.REPORTS_DATABASE_PASSWORD_PROPERTY) ? properties
						.getString(Emfac2011Properties.REPORTS_DATABASE_PASSWORD_PROPERTY)
						: null,
				properties
						.hasKey(Emfac2011Properties.REPORTS_DATABASE_INSTANCE_PROPERTY) ? properties
						.getString(Emfac2011Properties.REPORTS_DATABASE_INSTANCE_PROPERTY)
						: null);
	}

	public void detemplifyAndRunScript(Path script, String scenarioId,
			String scenarioIdToken) {
		String s = readFile(script).replace(scenarioIdToken, scenarioId);
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement()) {
			connection.setAutoCommit(false);
			statement.execute(s);
			connection.commit();
		} catch (SQLException e) {
			throw new RuntimeWrappingException(e);
		}
	}

	public ArrayList<ArrayList<String>> queryAquavisTables(Path script,
			String scenarioId, String scenarioIdToken) {
		ArrayList<ArrayList<String>> table = null;
		String s = readFile(script).replace(scenarioIdToken, scenarioId);
		try (Connection connection = getConnection();
				Statement statement = connection.createStatement()) {
			System.out.println("query="+s);
			table = extract(statement.executeQuery(s));
			connection.close();
		} catch (SQLException e) {
			throw new RuntimeWrappingException(e);
		}
		return table;
	}

	private ArrayList<ArrayList<String>> extract(ResultSet resultSet)
			throws SQLException {
		ArrayList<ArrayList<String>> table;
		int columnCount = resultSet.getMetaData().getColumnCount();

		if (resultSet.getType() == ResultSet.TYPE_FORWARD_ONLY)
			table = new ArrayList<ArrayList<String>>();
		else {
			resultSet.last();
			table = new ArrayList<ArrayList<String>>(resultSet.getRow());
			resultSet.beforeFirst();
		}

		for (ArrayList<String> row; resultSet.next(); table.add(row)) {
			row = new ArrayList<String>(columnCount);

			for (int c = 1; c <= columnCount; ++c)
				row.add(resultSet.getString(c).intern());
		}
		return table;
	}

	private String readFile(Path file) {
		try (FileReader reader = new FileReader(file.toFile())) {
			StringBuilder sb = new StringBuilder();
			char[] buffer = new char[8192];
			int readCount;
			while ((readCount = reader.read(buffer, 0, buffer.length)) > 0)
				sb.append(buffer, 0, readCount);
			return sb.toString();
		} catch (IOException e) {
			throw new RuntimeIOException(e);
		}
	}

	private String formConnectionUrl(String ipAddress, int port,
			String databaseName, String username, String password,
			String instance) {
		String url = "jdbc:jtds:sqlserver://" + ipAddress + ":" + port + "/"
				+ databaseName;
		if (username != null)
			url += ";user=" + username + ";" + password; // not
															// super
															// secure,
															// btu
															// ok
															// for
															// now
															// -
															// probably
															// will
															// use
															// SSO
															// normally
		if (instance != null)
			url += ";instance=" + instance;
		return url;
	}

	private Connection getConnection() throws SQLException {
		try {
			Class.forName("net.sourceforge.jtds.jdbc.Driver");
		} catch (ClassNotFoundException e) {
			throw new RuntimeWrappingException(e);
		}
		return DriverManager.getConnection(connectionUrl);
	}
}
