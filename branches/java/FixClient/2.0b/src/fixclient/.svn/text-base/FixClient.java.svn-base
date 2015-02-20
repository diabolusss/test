/*
 * FixClient entry point
 */
package fixclient;

import customjbdc.connection.DBConnection;
import customjbdc.queries.DBMakeQuery;
import fixclient.app.DukasApp;
import fixclient.ticks.TicksManager;
import fixclient.wsocket.SimpleClient;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import emailer.Emailer;
import fixclient.app.FixAutoRate;
import functions.Functions;
import java.io.File;
import java.security.KeyStore;
import java.security.KeyStoreException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import quickfix.ConfigError;
import quickfix.SessionSettings;

/**
 *
 * @author colt
 */
public class FixClient {
    //# fix vars
    public static String app_id; //application identifier
    private static final String DEFAULT_FIXCLIENT_PROPERTIES_PATH = "/resources/fixclient.properties"; 
    public static String BASH_CMD="";
    
    public static Properties fixClientProperties = new Properties();
    public static SessionSettings fixSettings = null;
    public static ArrayList<String> host_list = new ArrayList<String>();
    public static ConcurrentHashMap<URI, SimpleClient> WSConnectionList = new ConcurrentHashMap();
    
    public static DukasApp dukasFix;
    public static FixAutoRate autoFix;
    public static String rate_mode = "real";
    
    public static String    rateSource  = "unknown";
    
    //# db connection settings
    private static /*final*/    String  jdbc_real_name;
    public static /*final*/     String  jdbc_demo_name;
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
    public static  Boolean tls_enabled_server = false;
    
    public static SSLSocketFactory SecureSocketFactory;
    
    public static DBConnection jdbc;
    
    //# tickmanager
    public static TicksManager tm;  
    
    
    

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new Emailer().sendEmail("FIXPROCESS","<"+new Date().toString()+">:FIX Application starting");
        
        //#REGION get settings
        if (args.length == 0){
            fixClientProperties = Functions.getProperties(FixClient.class.getResourceAsStream(DEFAULT_FIXCLIENT_PROPERTIES_PATH));

        }else{
            fixClientProperties = Functions.getProperties(args[0]);                 
        }
        
        app_id = fixClientProperties.getProperty("application_uid");
                
        //get fix setttings
        InputStream inputStream = null;
        if(fixClientProperties == null){
            Functions.printLog("FIXPROCESS:[INIT] ERR - No properties were found. Halt");
            return;
        }else{
            try {
                inputStream = new FileInputStream(fixClientProperties.getProperty("fix_props_path"));
            } catch (Exception ex) {
                Functions.printLog("FIXPROCESS:[INIT] ERR - Failed to open FIX properties. Halt");
                return;
            }
        }           

        //parse settings
        /*
         * added 3 libss: slf4j-log4j12; slf4j-api; log4j
         * otherwise throws errors on try
         */
        try {
            fixSettings = new SessionSettings(inputStream);
            //Functions.printLog("SERVER:[GETPROP] INF - Settings parsed");

            //close stream
            try {
                inputStream.close();

            } catch (IOException ex) {
                //ex.printStackTrace(System.out);
                Functions.printLog(
                        "FIXPROCESS:[CLOSEPROP] WRN - " + ex.getLocalizedMessage()
                        );

            }

        } catch (ConfigError ex) {
            Functions.printLog(
                    "FIXPROCESS:[PARSEPROP] ERR - " + ex.getLocalizedMessage()
                    );
            return;                    
        }
        
        rate_mode  = fixClientProperties.getProperty("rate_mode");
        
        if(!rate_mode.equalsIgnoreCase("force_auto")){
            dukasFix = new DukasApp(fixSettings);
            dukasFix.start(); 
            
            FixClient.rateSource = "dukas";
        }
        
        BASH_CMD = fixClientProperties.getProperty("run_bash");
        Functions.printLog("FIXPROCESS:[PARSEPROP] INF - some bash command["+BASH_CMD+"]");
        
        //get jdbc settings
        Properties jdbc_props = Functions.getProperties( fixClientProperties.getProperty("jdbc_props_path") );           

        jdbc_real_name  = jdbc_props.getProperty("jdbc_real_database");
        jdbc_demo_name  = jdbc_props.getProperty("jdbc_demo_database");
        jdbc_URL        = jdbc_props.getProperty("jdbc_host")+jdbc_real_name;
        db_user         = jdbc_props.getProperty("jdbc_username");
        db_pass         = jdbc_props.getProperty("jdbc_password");
        try{
            db_max_conn     = Integer.parseInt(jdbc_props.getProperty("jdbc_max_connections"));
        }catch(NumberFormatException ex){
            Functions.printLog("FIXPROCESS:[PARSEPROP] WRN - Wrong num format[db_max_conn]. Default value set; E: " + ex.getLocalizedMessage());
            db_max_conn = 10;
            //return;
        }

        tls_enabled_server = Boolean.parseBoolean(fixClientProperties.getProperty("secured_socket"));
        STORETYPE = fixClientProperties.getProperty("tls_keystore_type");
        KEYSTORE = fixClientProperties.getProperty("tls_key");        
        if(tls_enabled_server && (STORETYPE == null || KEYSTORE == null)){
            Functions.printLog("FIXPROCESS:[SECURITY] ERR - Not defined keystore or keystore type");
            return;
        }
                
        //get wsocket server ports
        String[] resultSet = fixClientProperties.getProperty( (tls_enabled_server)?("tls_wsocket_server_host_set"):("wsocket_server_host_set") ).split(",");
        for(String host : resultSet){            
            String tmp = (tls_enabled_server==true)?("wss://"+host):("ws://"+host);
            host_list.add(tmp);
        }
        //get custom host set
        resultSet = fixClientProperties.getProperty("custom_wsserver_host_set").split(",");
        for(String host : resultSet){            
            String tmp = host;
            if(tmp!=null && !tmp.equalsIgnoreCase("null"))host_list.add(tmp);
        }
        if(host_list.isEmpty()){
            Functions.printLog("FIXPROCESS:[PARSEPROP] WRN - Empty host list. Halt");
            return;
        }
        Functions.printLog("FIXPROCESS:[PARSEPROP] INF - Properties parsed successfully. "+host_list.toString());
        //#ENDREGION get settings
        
        Functions.printLog(
                        "FIXPROCESS:[INIT] Creating jdbc connection" +
                        "[URL:" + jdbc_URL +
                        "; User:" + db_user + 
                        "; Max_conn:" + db_max_conn +
                        "]"
                        );
                
        jdbc = DBConnection.getInstance(jdbc_URL, db_user, db_pass, db_max_conn);
        Connection con = jdbc.getConnection();
        jdbc.freeConnection(con);   

        Functions.printLog("FIXPROCESS:[INIT] Creating and starting new TicksManager");
        tm = new TicksManager(fixClientProperties);                
        tm.start();

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
                    keystore = (FileInputStream) FixClient.class.getResourceAsStream( KEYSTORE );
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
                // sslContext.init( null, null, null ); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates

                Functions.printLog("SECURITY:[INIT] INF - SSL context initialized");                 
                
                SecureSocketFactory = sslContext.getSocketFactory();
                Functions.printLog("SSLFACTORY:[INIT] INF - SSL socket factory is ready");                
                

            } catch (Exception ex) {
                Functions.printLog("SSLCONTEXT:[INIT] ERR - " + ex.getLocalizedMessage());
                Functions.printLog("SECURITY:[INIT] FAILED - Secure connection is not available");                
            }            
        }
        /** SECURE PART GOES HERE **/
        //NB REVISIONED
        DBMakeQuery.setFixTLS(jdbc, null, (tls_enabled_server==true)?(1):(0));
        if(rate_mode.equalsIgnoreCase("force_auto")){
            String result = DBMakeQuery.getOnOffDates(jdbc, null);
            String[] dateResultSet = result.split("#");  
            DateTimeFormatter dtfmt = DateTimeFormat.forPattern(fixClientProperties.getProperty("date_time_format"));
            autoFix = new FixAutoRate(fixClientProperties, DBMakeQuery.getClosingPrice(jdbc, null), dtfmt.parseDateTime(dateResultSet[1]), dtfmt.parseDateTime(dateResultSet[2]));
            //autoFix.start();
            FixClient.rateSource = "auto";
            
        }
        for(String host:host_list){
            Functions.printLog("FIXPROCESS:[WSCLIENT] INF - Trying connect to "+host);
            try {
                URI  hostURI= new URI(host);
                SimpleClient client = new SimpleClient(hostURI);  
                
                try {
                    if(tls_enabled_server) client.setSocket(SecureSocketFactory.createSocket());                    
                    client.connect();
                    //client.connectBlocking();
                    
                } catch (Exception ex) {
                    Functions.printLog("SECURITY:[INIT] ERR - Failed to attach secure socket. Connection not available");
                    continue;
                }
                
                WSConnectionList.put(hostURI, client);
                
            } catch (URISyntaxException ex) {
                Functions.printLog("FIXPROCESS:[WSCLIENT] ERR - "+ex.getLocalizedMessage());
            }
        }        
        
    }
    
    
}