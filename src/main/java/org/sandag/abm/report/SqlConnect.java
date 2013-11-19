package org.sandag.abm.report;

import com.pb.common.util.ResourceUtil;
import com.sun.rowset.*;
import org.apache.log4j.Logger;
import java.sql.*;
import java.util.*;
import javax.sql.rowset.*;

/**
 * Start a single database connection with convenience methods for querying from it.
 *
 * @author Matt Keating
 *         11/19/2013 
 */
public final class SqlConnect {
    private static final Logger logger = Logger.getLogger(SqlConnect.class);
    public Connection conn;
    private Statement statement;
    private CachedRowSet crs; 
    public static SqlConnect db;
    private SqlConnect() {
        ResourceBundle properties =  ResourceUtil.getResourceBundle("report");
        String driver = properties.getString("database.driver");
        String url = properties.getString("database.url");
        String username = properties.containsKey("database.username") ? properties.getString("database.username") : null;
        String password = properties.containsKey("database.password") ? properties.getString("database.password") : null;
        String dbname = properties.getString("database.name");
        
        try {
            logger.info("Connecting with url "+url+";databaseName="+dbname+", user "+username);
            Class.forName(driver).newInstance();
            this.conn = (Connection)DriverManager.getConnection(url+";databaseName="+dbname,username,password);
        }
        catch (Exception sqle) {
            logger.fatal(sqle);
        }
    }
    /**
     *
     * @return SqlConnect Database connection object
     */
    public static synchronized SqlConnect getDbCon() {
        if ( db == null ) {
            db = new SqlConnect();
        }
        return db;
    }

    /**
     *
     * @param query String The query to be executed
     * @return a CachedResultSet object containing the results or null if not available
     * @throws SQLException
     */
    public CachedRowSet querySimple(String query) throws SQLException {
        ResultSet rs = null;
        crs = new CachedRowSetImpl();
        try {
            statement = db.conn.createStatement();
            rs = statement.executeQuery(query);
            crs.populate(rs);   
        }
        finally {
            statement.close();
            rs.close();   
        }
        return crs;
    }
    
    /**
    *
    * @param query String The query to be executed
    * @param inString String The list of options for a SQL 'IN' clause
    * @return a CachedResultSet object containing the results or null if not available
    * @throws SQLException
    */
    public CachedRowSet queryIn(String query, String inString) throws SQLException {
        ResultSet rs = null;
        crs = new CachedRowSetImpl();
        PreparedStatement statement = null;        
        Set<String> ids = new HashSet<String>();
        for (String id : inString.trim().split(","))
            ids.add(id);
        String sql = String.format(query, preparePlaceHolders(ids.size()));
        try {
            statement = db.conn.prepareStatement(sql);
            setValues(statement, ids.toArray());
            rs = statement.executeQuery();
            crs.populate(rs);
        } 
        catch (Exception sqle) {
            logger.fatal(sqle);
        } finally {
           statement.close();
           rs.close();
        }
        return crs;
    }
 
    /**
    *
    * @param length int The number of items that need a placeholder
    * @return a String with comma-separated placeholders
    */
    public static String preparePlaceHolders(int length) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < length;) {
            builder.append("?");
            if (++i < length) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    /**
    *
    * @param preparedStatement a jdbc prepared statement
    */
    public static void setValues(PreparedStatement preparedStatement, Object... values) throws SQLException {
        for (int i = 0; i < values.length; i++) {
            preparedStatement.setObject(i + 1, values[i]);
        }
    }
}