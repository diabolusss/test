    package wsocketstatusmanager.client;

import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;
import functions.Functions;
import java.util.Date;
import emailer.Emailer;
import java.io.IOException;
import wsocketstatusmanager.WSocketStatusManager;

public class ExampleClient extends WebSocketClient {
    static Timer timer = new Timer();
    static Integer connected = 0;
    static Integer reset = 0;
    static boolean closed = true;
    
    static Long delay = 10000L;
    
    static String cmd;   

    static int mail_counter = 0;
    
    public ExampleClient( URI serverUri , Draft draft ) {
            super( serverUri, draft );
    }

    public ExampleClient( URI serverURI ) {
            super( serverURI );
    }

    @Override
    public void onOpen( ServerHandshake handshakedata ) {
        if(mail_counter > 100){
            new Emailer().sendEmail(WSocketStatusManager.app_uid,"<"+new Date().toString()+">:Connection established");
            mail_counter = 0;
            
        }else mail_counter++;
        
        Functions.printLog("CLIENT:[onOpen] INF - opened connection" );
        timerReset();
        // if you plan to refuse connection based on ip or httpfields overload: onWebsocketHandshakeReceivedAsClient
        
    }

    @Override
    public void onMessage( String message ) {
        if(message.startsWith("pong")){
            Functions.printLog("SERVERLISTENER:[onMessage] INF - ["+message+"]");
            timerReset();

        }else{
            //Functions.printLog("SERVERLISTENER:[onMessage] WRN - Not expected message["+message+"]");

        }               
    }

    @Override
    public void onClose( int code, String reason, boolean remote ) {
        // The codecodes are documented in class org.java_websocket.framing.CloseFrame
        Functions.printLog("SERVERLISTENER:[onClose] INF - Connection closed by " + ( remote ? "remote peer" : "us" ) );

        closed = true;
        connected = 0;
    }

    @Override
    public void onError( Exception ex ) {// if the error is fatal then onClose will be called additionally
        Functions.printLog("SERVERLISTENER:[onError] INF - "+ex.getLocalizedMessage());    

        closed = true;
        connected = 0;
    }

    public static void start(URI host, String cmd, Long delay) { 
        ExampleClient.delay = delay;
        ExampleClient.cmd = cmd;
        ExampleClient c = null;

        //c = new ExampleClient(host);
        //c.connect();                    
        //closed = false;
        timerReset();

        while(true){
            if(closed){   
                //new Emailer().sendEmail(WSocketStatusManager.app_uid,"<"+new Date().toString()+">:Connecting to "+host.toString());
                Functions.printLog(WSocketStatusManager.app_uid+":[start] INF - Connecting to "+host.toString());

                //timerReset();

                c = new ExampleClient(host);
                if(WSocketStatusManager.tls_enabled_server) try {
                    c.setSocket(WSocketStatusManager.SecureSocketFactory.createSocket());                    
                    
                } catch (IOException ex) {
                    Functions.printLog("SECURITY:[INIT] ERR - Connection not available. Exception: " + ex.getMessage());
                    
                }
                
                c.connect();
                closed = false;

            }else{

                if(c != null && c.isOpen()) {
                    c.send("ping:WSSM");
                    c.send("{\"type\":\"ping\",\"msg\":{\"from\":\"WWSM\"}}");
                }
                else closed=true;               

            }

            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                Functions.printLog("CLIENT:[start] ERR - failed to sleep");
            }
        }

    }       

    public static void timerReset(){
        timer.cancel();
        timer = new Timer();
        timer.scheduleAtFixedRate(new Delayer(), 0, delay);
    }

    static class Delayer extends TimerTask{
        Delayer(){
            Functions.printLog("DELAYER: WDT resetted");
            reset = 0;
            connected = 1;
        }
        @Override
        public void run() {
            //if(connected == 1) {
                if(reset == 1){
                    try {
                        if(mail_counter > 100){
                            new Emailer().sendEmail(WSocketStatusManager.app_uid,"<"+new Date().toString()+">:Running cmd["+cmd+"]");
                            mail_counter = 0;
                        }else mail_counter++;
                        
                        Functions.printLog("DELAYER: INF - Running command["+cmd+"]");
                        Runtime.getRuntime().exec(cmd);
                        closed = true;
                        connected = 0;
                        reset = 0;

                    } catch (Exception ex) {
                        Functions.printLog("DELAYER: ERR - "+ex.getLocalizedMessage());
                    }
                } else{
                    reset = 1;
                }
            //}
        }

    }
}