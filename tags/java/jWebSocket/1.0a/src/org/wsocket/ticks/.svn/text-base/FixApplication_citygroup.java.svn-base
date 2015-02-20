/*    */ package org.wsocket.ticks;
/*    */ 
         import java.util.Iterator;
import quickfix.Application;
import quickfix.ConfigError;
import quickfix.DefaultMessageFactory;
import quickfix.FieldConvertError;
import quickfix.FileStoreFactory;
import quickfix.LogFactory;
import quickfix.MessageStoreFactory;
import quickfix.RuntimeError;
import quickfix.ScreenLogFactory;
import quickfix.SessionID;
import quickfix.SessionSettings;
import quickfix.SocketInitiator;
import quickfix.fix44.QuoteRequest;

         /*
          * Starts comunication wih bank
          * 
          */
/*    */ public class FixApplication_citygroup extends Thread {
    
/* 29 */   public  SessionSettings settings = null;
/*    */   private SocketInitiator initiator;
           private FixMessageHandler_citygroup messageHandler = null;
/* 33 */   private boolean started = false;
           private static final int DEFAULT_MESSAGE_PROCESSING_DELAY_MICRO = 5000;
	   public static int messageProcessingDelayMicroseconds = DEFAULT_MESSAGE_PROCESSING_DELAY_MICRO;
           private SessionID quoteSession = null;
           private SessionID tradeSession = null;
           private TicksManager tm = null;
/*    */ 
/*    */   public FixApplication_citygroup(SessionSettings settings, TicksManager tm) {
/* 40 */     this.settings = settings;
             this.tm = tm;
/*    */   }

          public void getSessions(){
              
		Iterator<SessionID> sessionIds = initiator.getSessions().iterator();
                
                while(sessionIds.hasNext()){
                    
        		SessionID sessionId = sessionIds.next();
        		System.out.println("FIXAPPCITI:[getSessions] SESSION ID: " + sessionId.toString());
                        
                	if(sessionId.toString().contains("-T-")){
                            tradeSession = sessionId;
                        } else if(sessionId.toString().contains("-Q-")){
                            quoteSession = sessionId;
                        }
                        
                }
          } 
/*    */ private void setMessageProcessingDelay(){
    
		System.out.println("FIXAPPCITI:[setMessageProcessingDelay] is called");
                
		try {
			if (settings.getLong("MessageProcessingDelay") > 0){
				messageProcessingDelayMicroseconds = Integer.parseInt(settings.getString("MessageProcessingDelay"));
				System.out.println("FIXAPPCITI:[setMessageProcessingDelay] MPD set to " + messageProcessingDelayMicroseconds + " microseconds. (The default is " + DEFAULT_MESSAGE_PROCESSING_DELAY_MICRO + " microseconds).");
                                
			}
		
                }catch(FieldConvertError e){
			System.out.println("FIXAPPCITI:[setMessageProcessingDelay] ERROR FieldConvertError - " + e.getLocalizedMessage());
		
                }catch(ConfigError e){
			System.out.println("FIXAPPCITI:[setMessageProcessingDelay] ERROR ConfigError - " + e.getLocalizedMessage());
		}
	}

        @Override
        public void run(){
/*    */     while (true) try {
    
/* 48 */         if (!this.started) {
    
                    boolean logHeartbeats = Boolean.valueOf(System.getProperty("logHeartbeats", "true")).booleanValue();
   
                    MessageStoreFactory messageStoreFactory = new FileStoreFactory(this.settings);
                    LogFactory logFactory = new ScreenLogFactory(true, true, true, logHeartbeats);
                    DefaultMessageFactory messageFactory = new DefaultMessageFactory();
                    Application application = null;
                    
                    application = new FixProtocol_citygroup(this.settings,tm);   
                    
/*    */           try{   
    
                    /*
                     * Exception in thread "Thread-10" 
                     *  java.lang.NoClassDefFoundError: org/apache/mina/common/ByteBufferAllocator
                       at org.wsocket.ticks.FixApplication.run(FixApplication.java:89)
                       * 
                       *  added two more libs
                       *   mina-core
                       *   mina-filter
                       *   
                       *   next: java.lang.NoClassDefFoundError: edu/emory/mathcs/backport/java/util/concurrent/Executor
                       *  added one more
                       *   backport-util-concurrent-3.0

                     */
    
/* 65 */             this.initiator = new SocketInitiator(
                        application,         
                        messageStoreFactory, 
                        this.settings, 
                        logFactory, 
                        messageFactory
                        );

                     System.out.println("FIXAPPCITI:[run] Created initiator!");
                     
/*    */           } catch (ConfigError ex) {
/* 67 */             //ex.printStackTrace(System.out);
                     System.out.println("FIXAPPCITI:[run] ERROR " + ex.getLocalizedMessage());
                     
/*    */           }
/*    */ 
/*    */           try{
                     System.out.println("FIXAPPCITI:[run] Starting initiator!");   
/* 72 */             this.initiator.start();
/* 73 */             this.started = true;
/* 74 */             System.out.println("FIXAPPCITI:[run] FixApplication Started!");

/*    */           } catch (ConfigError ex){
                         System.out.println("FIXAPPCITI:[run] ERROR " + ex.getLocalizedMessage());
                         
                   } catch (RuntimeError ex) {
                        System.out.println("FIXAPPCITI:[run] ERROR " + ex.getLocalizedMessage());

/*    */           }

                    System.out.println("FIXAPPCITI:[run] Requesting quotes...");
                    try {
                            Thread.sleep(messageProcessingDelayMicroseconds);
                            
                            System.out.println("FIXAPPCITI:[run] Requesting a standard quote.");
                            getSessions();      
                            
                            messageHandler = new FixMessageHandler_citygroup(quoteSession, tradeSession, settings);
                            tm.tradeSession = tradeSession;
                            
                            QuoteRequest quoteRequest = messageHandler.createQuoteRequest();
                            messageHandler.send(quoteRequest, quoteSession);
                            
                            Thread.sleep(messageProcessingDelayMicroseconds);

                            System.out.println("FIXAPPCITI:[run] Requesting a mass quote.");
                            quoteRequest = messageHandler.createMassQuoteRequest();
                            messageHandler.send(quoteRequest, quoteSession);
                            
                            Thread.sleep(messageProcessingDelayMicroseconds);

                            //TODO may want to logout of quote the session, 
                            // if only for readability of trades that follow 
                            // in the console and logs.

                    } catch(InterruptedException e){
                            System.out.println("FIXAPPCITI:[run] ERROR Thread InterruptedException" + e.getLocalizedMessage());
                    }
/*    */ 
/*    */         }
/*    */ 
/* 83 */         Thread.sleep(10000L);

/*    */       } catch (InterruptedException ex) {
    
/* 85 */         ex.printStackTrace(System.out);

/*    */       } catch (ConfigError ex) {
                    //java.util.logging.Logger.getLogger(FixApplication.class.getName()).log(Level.SEVERE, null, ex);
               
               } catch (FieldConvertError ex) {
                    //java.util.logging.Logger.getLogger(FixApplication.class.getName()).log(Level.SEVERE, null, ex);
               }
/*    */   }
/*    */ }

