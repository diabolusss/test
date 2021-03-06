package wsocketserver;

import java.net.InetSocketAddress;
import java.sql.Connection;
import java.util.Date;
import java.util.Properties;

import emailer.Emailer;

import org.java_websocket.WebSocketImpl;

import wsocketserver.server.SimpleServer;

import customjbdc.connection.DBConnection;
import customjbdc.queries.DBMakeQuery;

import functions.Functions;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import org.java_websocket.server.DefaultSSLWebSocketServerFactory;

/**
 *
 * @author colt
 */
public class WSocketServer {
    public static String app_uid;
    
    public static String server_host;
    
    public static String FIXCLIENTFAILURECOMMAND = "";
    
    public static InetSocketAddress wsocket_port = new InetSocketAddress(0);
    
    //# db connection settings
    private static /*final*/    String  jdbc_real_name;
    public  static /*final*/    String  jdbc_demo_name;
    private static /*final*/    String  jdbc_URL;
    private static /*final*/    String  db_user;
    private static /*final*/    String  db_pass;
    private static /*final*/    int     db_max_conn;
    
    //#security access data
    // Keystore with certificate created like so (in JKS format):
    //   keytool -genkey -validity 3650 -keystore "keystore.jks" -storepass "storepassword" -keypass "keypassword" -alias "default" -dname "CN=127.0.0.1, OU=MyOrgUnit, O=MyOrg, L=MyCity, S=MyRegion, C=MyCountry"
    private static String STORETYPE        = "JKS";
    private static String KEYSTORE         = "keystore.jks";
    private static String STOREPASSWORD    = "Kx716BFg";
    private static String KEYPASSWORD      = "Kx716BFg";
    public static Boolean tls_enabled_server    = false;

    private static final String DEFAULT_SERVER_PROPERTIES_PATH = "/resources/fixstatusmanager.properties";
    
    public static Properties serverProperties = new Properties();
    public static Properties fixClientProperties = new Properties();
    
    public static DBConnection jdbc;
    public static SimpleServer server;
    public static long PRICE_CHECK_INTERVAL = 300;
    
    private static Timer logChecker;
    private static String LOG_FILENAME = "FixClient.log";
    private static String LOG_FLUSHCMD = "flush_log";       
    //private static String dtFormat;
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args){
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
        
        /** SECURE PART GOES HERE **/
        if(tls_enabled_server){
            KeyStore ks = null;
            try {
                ks = KeyStore.getInstance( STORETYPE );
            } catch (KeyStoreException ex) {
                Functions.printLog("KEYSTORE:[INIT] ERR - " + ex.getLocalizedMessage());
            }
            
            File kf = new File( KEYSTORE );
            try {
                FileInputStream keystore = new FileInputStream( kf );
                if(keystore == null){
                    Functions.printLog("KEYSTORE:[INIT] WRN - Failed to open external ks file. Opening from resources");
                    keystore = (FileInputStream) WSocketServer.class.getResourceAsStream( KEYSTORE );
                }
                
                ks.load( keystore, STOREPASSWORD.toCharArray() );
            
            } catch (Exception ex) {
                Functions.printLog("KEYSTORE:[INIT] ERR - " + ex.getLocalizedMessage());
            } 

            KeyManagerFactory kmf = null;
            try {
                kmf = KeyManagerFactory.getInstance( "SunX509" );                
                kmf.init( ks, KEYPASSWORD.toCharArray() );
            
            } catch (Exception ex) {
                Functions.printLog("KEYMNGRFACTORY:[INIT] ERR - " + ex.getLocalizedMessage());
            }
            
            TrustManagerFactory tmf = null;
            try {
                tmf = TrustManagerFactory.getInstance( "SunX509" );
                tmf.init( ks );
                
            } catch (Exception ex) {
                Functions.printLog("TRUSTMNGRFACTORY:[INIT] ERR - " + ex.getLocalizedMessage());               
            } 

            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance( "TLS" );            
                sslContext.init( kmf.getKeyManagers(), tmf.getTrustManagers(), null );
                server.setWebSocketFactory( new DefaultSSLWebSocketServerFactory( sslContext ) );
                Functions.printLog("SECURITY:[INIT] INF - Running secure websocket server"); 
                
            } catch (Exception ex) {
                Functions.printLog("SSLCONTEXT:[INIT] ERR - " + ex.getLocalizedMessage());
                Functions.printLog("SECURITY:[INIT] FAILED - Running non-secure websocket");                
            }
        }
        /** SECURE PART GOES HERE **/        
        server.start();
        
        new Emailer().sendEmail(WSocketServer.app_uid,"<"+new Date().toString()+">:FixStatusManager started on port "+ server.getPort());
        Functions.printLog( "SERVER:[INIT] INF - Server started on port: " + server.getPort() );
        
        Functions.printLog( "SERVER:[INIT] INF - Checking LogFile size at fixed rate: 24hours" );
        logChecker = new Timer();
        logChecker.scheduleAtFixedRate(new LogChecker(), 0, TimeUnit.HOURS.toMillis(24));
    }
    
    /*
     * Sets properties 
     * 
     */
    private static void initVariables(Properties properties){
        //RunServer.wsocket_port   = Integer.parseInt(properties.getProperty("websocket_connection_port"));
        WSocketServer.fixClientProperties = Functions.getProperties(properties.getProperty("fixclientproperties_path"));
        tls_enabled_server = Boolean.parseBoolean(fixClientProperties.getProperty("secured_socket"));
        STORETYPE = fixClientProperties.getProperty("tls_keystore_type");
        KEYSTORE = fixClientProperties.getProperty("tls_key");        
        if(tls_enabled_server && (STORETYPE == null || KEYSTORE == null)){
            Functions.printLog("FIXPROCESS:[SECURITY] ERR - Not defined keystore or keystore type");
            return;
        }
        //get jdbc settings
        Properties jdbc_props = Functions.getProperties( fixClientProperties.getProperty("jdbc_props_path") );           
        
        //# db connection settings
        WSocketServer.jdbc_real_name    = jdbc_props.getProperty("jdbc_real_database");
        WSocketServer.jdbc_demo_name  = jdbc_props.getProperty("jdbc_demo_database");
        WSocketServer.jdbc_URL        = jdbc_props.getProperty("jdbc_host")+jdbc_real_name;
        WSocketServer.db_user         = jdbc_props.getProperty("jdbc_username");
        WSocketServer.db_pass         = jdbc_props.getProperty("jdbc_password");
        WSocketServer.db_max_conn     = Integer.parseInt(jdbc_props.getProperty("jdbc_max_connections"));
        
        WSocketServer.wsocket_port = new InetSocketAddress(
                Integer.parseInt(properties.getProperty("websocket_connection_port"))
                );
        WSocketServer.PRICE_CHECK_INTERVAL = Long.parseLong(properties.getProperty("price_check_interval"));
        WSocketServer.FIXCLIENTFAILURECOMMAND = properties.getProperty("fix_failure_rstcmd");
                
        WSocketServer.app_uid       = properties.getProperty("application_uid");
        WSocketServer.server_host   = properties.getProperty("websocket_host_name")+":"+properties.getProperty("websocket_connection_port");
         
        WSocketServer.LOG_FILENAME       = properties.getProperty("fix_log");
        WSocketServer.LOG_FLUSHCMD   = properties.getProperty("fix_flush_log");
    }
    
    private static class LogChecker extends TimerTask {

        public LogChecker() {
        }

        @Override
        public void run() {            
            int size = (int) (Functions.fileSize(LOG_FILENAME)/1024/1024/1024);
            Functions.printLog("LogChecker: INF - LogFile size["+size+"]");
            if(size > 1){
                try {
                    Functions.printLog("LogChecker: INF - Running command["+WSocketServer.LOG_FLUSHCMD+"]");

                    Runtime.getRuntime().exec(WSocketServer.LOG_FLUSHCMD);

                } catch (IOException ex) {
                    Functions.printLog("LogChecker: ERR - "+ex.getLocalizedMessage());}
            }
        }
    }
}