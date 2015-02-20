package wsocketserver;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.Date;
import java.util.Properties;

import emailer.Emailer;

import org.java_websocket.WebSocketImpl;

import wsocketserver.server.SimpleServer;

import customjbdc.connection.DBConnection;

import functions.Functions;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author colt
 */
public class WSocketServer {
    public static String app_uid;
    
    public static String server_host;
    
    public static InetSocketAddress wsocket_port = new InetSocketAddress(0);
    
    //# db connection settings
    private static /*final*/    String  jdbc_real_name;
    public static /*final*/     String  jdbc_demo_name;
    private static /*final*/    String  jdbc_URL;
    private static /*final*/    String  db_user;
    private static /*final*/    String  db_pass;
    private  static /*final*/   int     db_max_conn;
        
    private static final String DEFAULT_SERVER_PROPERTIES_PATH = "/resources/server.properties";
    
    public static Properties serverProperties = new Properties();
    
    public static DBConnection jdbc;
    public static SimpleServer server;
    
    static Timer pinger;
    
    //private static String dtFormat;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        //debugging mode
        WebSocketImpl.DEBUG = false;
        
        //#REGION get settings            
        if (args.length == 0){
            serverProperties = Functions.getProperties(WSocketServer.class.getResourceAsStream(DEFAULT_SERVER_PROPERTIES_PATH));
        }else{
            serverProperties = Functions.getProperties(args[0]);
        }

        if(serverProperties == null){
            Functions.printLog("SERVER:[GETPROP] ERR - No properties were found. Halt");
            return;
        }  

        initVariables(serverProperties);
        Functions.printLog("SERVER:[PARSEPROP] INF - Properties parsed successfully");       

        Functions.printLog(
                "SERVER:[INIT] Creating jdbc connection" +
                "[URL:" + jdbc_URL +
                "; User:" + db_user + 
                "; Max_conn:" + db_max_conn +
                "]"
                );

        jdbc = DBConnection.getInstance(jdbc_URL, db_user, db_pass, db_max_conn);
        Connection con = jdbc.getConnection();
        jdbc.freeConnection(con);  
        
        server = new SimpleServer(wsocket_port);
        server.start();
        
        new Emailer().sendEmail(WSocketServer.app_uid,"<"+new Date().toString()+">:Server started on port "+ server.getPort());
        Functions.printLog( "SERVER:[INIT] INF - Server started on port: " + server.getPort() );
        
        pinger = new Timer();
        pinger.scheduleAtFixedRate(new Pinger(), 0, TimeUnit.SECONDS.toMillis(15));
    }
    
    /*
     * Sets properties 
     * 
     */
    private static void initVariables(Properties properties){
        //RunServer.wsocket_port   = Integer.parseInt(properties.getProperty("websocket_connection_port"));
        WSocketServer.wsocket_port = new InetSocketAddress(
                Integer.parseInt(properties.getProperty("websocket_connection_port"))
                );
        
        //get jdbc settings
        Properties jdbc_props = Functions.getProperties( properties.getProperty("jdbc_props_path") );           
        
        //# db connection settings
        WSocketServer.jdbc_real_name = jdbc_props.getProperty("jdbc_real_database");
        WSocketServer.jdbc_demo_name = jdbc_props.getProperty("jdbc_demo_database");
        WSocketServer.jdbc_URL       = jdbc_props.getProperty("jdbc_host")+jdbc_real_name;
        WSocketServer.db_user        = jdbc_props.getProperty("jdbc_username");
        WSocketServer.db_pass        = jdbc_props.getProperty("jdbc_password");
        WSocketServer.db_max_conn    = Integer.parseInt(jdbc_props.getProperty("jdbc_max_connections"));
        
        WSocketServer.app_uid       = properties.getProperty("application_uid");
        WSocketServer.server_host   = properties.getProperty("websocket_host_name")+":"+properties.getProperty("websocket_connection_port");
    }

    private static class Pinger extends TimerTask {

        public Pinger() {
        }

        @Override
        public void run() {
            Functions.printLog("SERVER:[Pinger] INF - pinging");
            server.broadcastMessage("PING", 1);
        }
    }
}