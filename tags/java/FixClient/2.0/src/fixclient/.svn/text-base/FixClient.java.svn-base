/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package fixclient;

import customjbdc.connection.DBConnection;
import fixclient.app.DukasApp;
import fixclient.ticks.TicksManager;
import fixclient.wsocket.SimpleClient;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
//import org.custom.Functions;
import emailer.Emailer;
import functions.Functions;
//import org.wsocket.jdbc.DBConnection;
import quickfix.ConfigError;
import quickfix.SessionSettings;

/**
 *
 * @author colt
 */
public class FixClient {
    //# fix vars
    public static String app_id;
    private static final String DEFAULT_FIXCLIENT_PROPERTIES_PATH = "/resources/fixclient.properties";    
    
    public static Properties fixClientProperties = new Properties();
    public static ArrayList<String> host_list = new ArrayList<String>();
    public static ConcurrentHashMap<URI, SimpleClient> WSConnectionList = new ConcurrentHashMap();
    
    public static DukasApp dukasFix;
    
    //# db connection settings
    private static /*final*/    String  jdbc_real_name;
    public static /*final*/     String  jdbc_demo_name;
    private static /*final*/    String  jdbc_URL;
    private static /*final*/    String  db_user;
    private static /*final*/    String  db_pass;
    private static /*final*/    int     db_max_conn;
    
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
            } catch (FileNotFoundException ex) {
                Functions.printLog("FIXPROCESS:[INIT] ERR - Failed to open FIX properties. Halt");
                return;
            }
        }           

        SessionSettings fixSettings = null;

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
                        "FIXPROCESS:[CLOSEPROP] WRN - " +
                        ex.getLocalizedMessage()
                        );

            }

        } catch (ConfigError ex) {
            //ex.printStackTrace(System.out);
            Functions.printLog(
                    "FIXPROCESS:[PARSEPROP] ERR - " +
                    ex.getLocalizedMessage()
                    );
            return;                    
        }
        
        dukasFix = new DukasApp(fixSettings);
        dukasFix.start();

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
            Functions.printLog("FIXPROCESS:[PARSEPROP] ERR - Wrong num format[db_max_conn]: " + ex.getLocalizedMessage());
            //return;
        }

        //get wsocket server ports
        String[] resultSet = fixClientProperties.getProperty("wsocket_server_host_set").split(","); 
        for(String host : resultSet){
            host_list.add("ws://"+host);
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

        
        
        for(String host:host_list){
            Functions.printLog("FIXPROCESS:[WSCLIENT] INF - Trying connect to "+host);
            try {
                URI  hostURI= new URI(host);
                SimpleClient client = new SimpleClient(hostURI);
                client.connect();
                WSConnectionList.put(hostURI, client);
            } catch (URISyntaxException ex) {
                Functions.printLog("FIXPROCESS:[WSCLIENT] ERR - "+ex.getLocalizedMessage());
            }
        }
        
        
    }
    
    
}