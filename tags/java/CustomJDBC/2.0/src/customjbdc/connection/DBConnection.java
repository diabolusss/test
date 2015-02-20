package customjbdc.connection;

import functions.Functions;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;


public class DBConnection{
    private static DBConnection instance;
    private static int clients;
    private String URL;
    private String db_user;
    private String db_pass;
    private int maxConn;
    private ArrayList freeConnections = new ArrayList();

    public DBConnection(String URL, String db_user, String db_pass, int maxConn){
        this.URL = URL;
        this.db_user = db_user;
        this.db_pass = db_pass;
        this.maxConn = maxConn;

        try{
            Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
            DriverManager.registerDriver(driver);
            Functions.printLog("JDBC:[INIT] INF - Registered JDBC driver ");

        } catch(Exception e){
            Functions.printLog("JDBC:[INIT] ERR - " + e.getLocalizedMessage());
        }
    }

    public static synchronized DBConnection getInstance(String URL, String db_user, String db_pass, int maxConn)   {
        if (instance == null) {
            instance = new DBConnection(URL, db_user, db_pass, maxConn);
        }
        clients += 1;
        return instance;
    }


    public synchronized Connection getConnection(){
        Connection con = null;

        if (!this.freeConnections.isEmpty()) {
            con = (Connection)this.freeConnections.get(this.freeConnections.size() - 1);

            try {            
                if (con.isClosed()) {
                    //This method is guaranteed to return true only when it is called 
                    //after the method Connection.close has been called.
                    //con.close();
                    this.freeConnections.remove(con);
                    Functions.printLog("JDBC:[GETCONN] INF - Removed bad connection ");
                    con = getConnection();
                }

            } catch (Exception e) {
                this.freeConnections.remove(con);
                Functions.printLog("JDBC:[GETCONN] ERR - Removed bad connection. " + e.getLocalizedMessage());

                con = getConnection();
            }
            
        } else {
           Functions.printLog("JDBC:[GETCONN] INF - Try to get new connection...");
           con = newConnection();
        }
                
        return con;
    }

    private Connection newConnection() {
        Connection con = null;
        try {
            DriverManager.setLoginTimeout(15);
            con = DriverManager.getConnection(this.URL, this.db_user, this.db_pass);            
            Functions.printLog("JDBC:[NEWCONN] INF - Created a new connection in pool");

        } catch (SQLException e) {
            Functions.printLog("JDBC:[NEWCONN] ERR - Can't create a new connection for [" + this.URL + ", " + this.db_user + ", " + this.db_pass + "]. " + e.getLocalizedMessage());
            return null;
        }
        return con;
    }

    /**
     * NB What would do if 'if' statement is false!?
     * @param con 
     */
    public synchronized void freeConnection(Connection con){
        if ((con != null) && (this.freeConnections.size() <= this.maxConn)){
            this.freeConnections.add(con);
        }
    }

    public synchronized void release()   {
        Iterator allConnections = this.freeConnections.iterator();
        
        while (allConnections.hasNext()) {
            Connection con = (Connection)allConnections.next();
            
            try {
                con.close();
                Functions.printLog("JDBC:[RLES] INF - Closed connection for pool ");
                
            } catch (SQLException e) {
                Functions.printLog("JDBC:[RLES] ERR - Can't close connection for pool. "+e.getLocalizedMessage());
            }
        }
        this.freeConnections.clear();
    }
    
}