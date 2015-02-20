/*     */ package org.wsocket.ticks;
/*     */ 
/*     */ import java.math.BigDecimal;
/*     */ import java.math.RoundingMode;
import org.custom.RunServer;
/*     */ import quickfix.Application;
/*     */ import quickfix.ConfigError;
/*     */ import quickfix.DoNotSend;
/*     */ import quickfix.FieldConvertError;
/*     */ import quickfix.FieldNotFound;
/*     */ import quickfix.IncorrectDataFormat;
/*     */ import quickfix.IncorrectTagValue;
/*     */ import quickfix.Message;
/*     */ import quickfix.RejectLogon;
/*     */ import quickfix.Session;
/*     */ import quickfix.SessionID;
/*     */ import quickfix.SessionNotFound;
          import quickfix.SessionSettings;
/*     */ import quickfix.UnsupportedMessageType;
          import quickfix.field.Account;
/*     */ import quickfix.field.ClOrdID;
/*     */ import quickfix.field.MDEntryPx;
/*     */ import quickfix.field.MsgType;
/*     */ import quickfix.field.NoMDEntries;
/*     */ import quickfix.field.OrdStatus;
/*     */ import quickfix.field.OrdType;
/*     */ import quickfix.field.OrderQty;
/*     */ import quickfix.field.Password;
/*     */ import quickfix.field.Side;
/*     */ import quickfix.field.Symbol;
/*     */ import quickfix.field.TransactTime;
/*     */ import quickfix.field.Username;
/*     */ import quickfix.fix44.ExecutionReport;
          import quickfix.field.HandlInst;
/*     */ //import quickfix.fix44.MarketDataRequest.NoMDEntryTypes;
/*     */ import quickfix.fix44.MarketDataSnapshotFullRefresh;
/*     */ //import quickfix.fix44.MarketDataSnapshotFullRefresh.NoMDEntries;
/*     */ import quickfix.fix44.MessageCracker;
/*     */ //import quickfix.fix44.QuoteRequest.NoRelatedSym;
          import quickfix.field.ResetSeqNumFlag;
          import quickfix.fix44.Quote;
          import quickfix.field.QuoteType;
/*     */ 

/*     */ class FixProtocol_citygroup extends MessageCracker implements Application  {
/*  55 */   private static double market_point = 10000.0D;
/*  56 */   //private String username = null;
/*  57 */   //private String password = null;
            private String username = "";
/*  57 */   private String password = "";
/*  58 */   private TicksManager tm = RunServer.tm;
/*     */ 
/*  60 */   private static boolean subscribed = false;
/*     */   private final SessionSettings settings;
/*     */   private static SessionID trade_sid;
            private Quote tradeableQuote;
            private static Account tradeAccount;
            private static final int DEFAULT_MESSAGE_PROCESSING_DELAY_MICRO = 5000;
            public static int messageProcessingDelayMicroseconds = DEFAULT_MESSAGE_PROCESSING_DELAY_MICRO;            
            
/*     */   public FixProtocol_citygroup(SessionSettings settings, TicksManager tm)
/*     */     throws ConfigError, FieldConvertError {
    
              this.username = settings.getString("Username");
              this.password = settings.getString("Password");
              this.tradeAccount = new Account(settings.getString("Account"));
              this.settings = settings;
/*     */   }
/*     */ 
/*     */   //*private void initTicksManager(String mode) {
/*     */   //}
/*     */   
/*     */   @Override
            public void onCreate(SessionID sid) {
/*  83 */     System.out.println("FIXPROTOCITI:[OnCreate] " + sid.toString());
/*     */   }
/*     */ 
/*     */   @Override
            public void onLogon(SessionID sid){
/*  88 */     subscribed = false;
/*  89 */     System.out.println("FIXPROTOCITI:[OnLogon] " + sid.toString());
/*     */   }
/*     */ 
/*     */   @Override
            public void onLogout(SessionID sid){
/*  94 */     subscribed = false;
/*  95 */     TicksManager.broker_status = 0;
/*  96 */     System.out.println("FIXPROTOCITI:[OnLogout] " + sid.toString());
/*     */   }
/*     */ 

	@Override
	public void toAdmin(Message message, SessionID sessionId) {
		System.out.println("FIXPROTOCITI:[toAdmin] CLIENT toAdmin called");

		if (isMessageOfType(message, MsgType.LOGON)) {
			System.out.println("FIXPROTOCITI:[toAdmin] Message type LOGON");
                        
			addLogonFields(message, sessionId);
                        
			System.out.println("FIXPROTOCITI:[toAdmin] " + message.toString());
			
			message.setField(new ResetSeqNumFlag(true)); //request both client and server to reset sequence numbers on login
			
		}
	}

	private static boolean isMessageOfType(Message message, String type) {
		try {
			return type.equals(message.getHeader().getField(new MsgType()).getValue());
		} catch (FieldNotFound e) {
			System.out.println("FIXPROTOCITI:[isMsgOfType] ERROR " + e.getMessage());
			e.printStackTrace(System.out);
			return false;
		}
	}
		
	private void addLogonFields(Message message, SessionID sessionId){

                        message.getHeader().setField(new Username(this.username));
                        message.getHeader().setField(new Password(this.password));
                        
			System.out.println(
                                "FIXPROTOCITI:[addLogonFields] username " + this.username + 
                                " password " + this.password
                                );
                        
			message.getHeader().setField(new Username(username));
			message.getHeader().setField(new Password(password));

	}
           /* public void toAdmin(Message msg, SessionID sid)
            {
                log.debug("toAdmin called: " + msg + " Session: " + sid);
                if (isMessageOfType(msg, "A")) {
                    addLogonField(msg);
                }

                if ((isMessageOfType(msg, "0")) && (!isTrade(sid)))
                if (!subscribed)
                {
                  MDReqID mdreqid = new MDReqID();
                  mdreqid.setValue("quotesrequest");

                  MarketDataRequest req = new MarketDataRequest(mdreqid, new SubscriptionRequestType('1'), new MarketDepth(1));

                  MarketDataRequest.NoMDEntryTypes group = new MarketDataRequest.NoMDEntryTypes();
                  group.setField(new MDEntryType('0'));
                  req.addGroup(group);

                  group.setField(new MDEntryType('1'));
                  req.addGroup(group);

                  QuoteRequest.NoRelatedSym related = new QuoteRequest.NoRelatedSym();
                  related.set(new Instrument(new Symbol("EUR/USD")));   
                  req.addGroup(related);

                  req.set(new MDUpdateType(0));
                  req.setField(new MsgSeqNum(3));
                  try
                  {
                    Session.sendToTarget(req, sid);
                    subscribed = true;
                    TicksManager.broker_status = 1;
                  } catch (SessionNotFound ex) {
                  ex.printStackTrace();
               }
                } else {
                  TicksManager.broker_status = 1;
                }
            }*/
        
            @Override
            public void fromAdmin(Message msg, SessionID sid)
              throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, RejectLogon{
                    System.out.println(
                            "FIXPROTOCITI:[fromAdmin] " + msg.toString().replace('\001', '|')
                            );
                }

            @Override
            public void toApp(Message msg, SessionID sid)
              throws DoNotSend{
                    System.out.println(
                            "FIXPROTOCITI:[toApp] " + msg.toString().replace('\001', '|')
                            );
                }
/*     */ 
/*             public void fromApp(Message msg, SessionID sid)
             throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue, UnsupportedMessageType {
             
                log.debug("fromApp called: " + msg + " Session: " + sid);
                if (isMessageOfType(msg, "W")) {
                    crack(msg, sid);
                }
                if (isMessageOfType(msg, "8")) {
                    crack(msg, sid);
                }
                if (isMessageOfType(msg, "U3"))
                    log.debug("U3: " + msg.toString().replace('\001', '|'));
              }
*/

	@Override
	public void fromApp(Message message, SessionID sessionId)
			throws FieldNotFound, IncorrectDataFormat, IncorrectTagValue,
			UnsupportedMessageType {
            
		System.out.println("FIXPROTOCITI:[fromApp] CLIENT fromApp called");		
		System.out.println("FIXPROTOCITI:[fromApp] " + message.toString());
		
		if(isMessageOfType(message, MsgType.QUOTE)){
                    
			System.out.println("FIXPROTOCITI:[fromApp] message is a quote");
                        
			try {
                            QuoteType qt = ((Quote)message).get(new QuoteType());
                            
                            if(qt.getValue() == QuoteType.TRADEABLE){
                                
                                    System.out.println("FIXPROTOCITI:[fromApp] quote is tradeable");
                                    
                                    this.tradeableQuote = (Quote)message;
                                    
                                    double bid = this.tradeableQuote.getBidPx().getValue();
                                    double offer = this.tradeableQuote.getBidPx().getValue();
                                    double price = (offer + bid) / 2.0D;
                                    
                                    price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
                                    price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
                                    
                                    System.out.println("FIXPROTOCITI:[fromApp] Current RATE: " + price);
                                    this.tm.newTick(price);
                            }//end of if in try
                            
			} catch(FieldNotFound e){
				System.out.println("FIXPROTOCITI:[fromApp] ERROR FieldNotFound" + e.getLocalizedMessage());
                                
                                
			}//end of try catch
                        
		}//end of if
	}
	
/*     */   @Override
            public void onMessage(MarketDataSnapshotFullRefresh msg, SessionID sid) 
                throws FieldNotFound {
    
/* 186 */     msg.getField(new NoMDEntries());
/* 187 */     MarketDataSnapshotFullRefresh.NoMDEntries group = new MarketDataSnapshotFullRefresh.NoMDEntries();
/*     */ 
/* 190 */     MDEntryPx MDEntryPx = new MDEntryPx();
/*     */ 
/* 192 */     msg.getGroup(1, group);
/* 193 */     double offer = group.get(MDEntryPx).getValue();
/*     */ 
/* 195 */     msg.getGroup(2, group);
/* 196 */     double bid = group.get(MDEntryPx).getValue();
/*     */ 
/* 199 */     double price = (offer + bid) / 2.0D;
/* 200 */     price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(10, RoundingMode.HALF_UP).doubleValue();
/* 201 */     price = new BigDecimal(Double.valueOf(price).doubleValue()).setScale(4, RoundingMode.HALF_DOWN).doubleValue();
/*     */ 
/* 203 */     this.tm.newTick(price);
/*     */   }
/*     */ 
/*     */@Override
         public void onMessage(ExecutionReport msg, SessionID sid)
            throws FieldNotFound {
/* 209 */     String clordeID = msg.getClOrdID().getValue();
/* 210 */     OrdStatus ordstatus = msg.getOrdStatus();
/* 211 */     double price = msg.getAvgPx().getValue();
/* 212 */     String execid = msg.getExecID().getValue();
/* 213 */     String error = msg.getText().getValue();
/*     */ 
/* 215 */     if (ordstatus.valueEquals('2')) {
/* 216 */       TicksManager.setOrderFilled(clordeID, execid, price);
/*     */     }
/*     */ 
/* 219 */     if (ordstatus.valueEquals('8')) {
/* 220 */       TicksManager.setOrderFilled(clordeID, error);
/*     */     }
/* 222 */     System.out.println("FIXPROTOCITI:[onMessage] ExecutionReport: " + msg.toString().replace('\001', '|'));
/*     */ }
/*     */ 
            /*  private void addLogonField(Message message)
                {
                    message.getHeader().setField(new Username(this.username));
                    message.getHeader().setField(new Password(this.password));
                }

                private boolean isMessageOfType(Message message, String type) {
                   try {
                        return type.equals(message.getHeader().getField(new MsgType()).getValue()); } catch (FieldNotFound e) {
                   }
                    return false;
                }*/
/*     */ 
/*     */   private boolean isTrade(SessionID sid) {
/*     */     //try {
/* 242 */       String str = "SessionName";//this.settings.getString(sid, "SessionName");
/* 243 */       if (str.equals("TRADE")) {
/* 244 */         trade_sid = sid;
/* 245 */         return true;
/*     */       }
/*     */     //} catch (ConfigError | FieldConvertError ex) {
/* 248 */     //  ex.printStackTrace(System.out);

/*     */     //}
/*     */ 
/* 253 */     return false;
/*     */   }

        public static void sendNewOrder(int order_id, double amount,SessionID tradeSession) 
         throws InterruptedException {
          Side side = null;
          if (amount < 0.0D)
            side = new Side(Side.SELL);
          else {
            side = new Side(Side.BUY);
          }

          double quantity = (double) Math.abs(amount * market_point);
          String pair = "EURUSD";

          ClOrdID orderId = new ClOrdID("111" + System.currentTimeMillis());
          quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(orderId, side, new TransactTime(), new OrdType(OrdType.MARKET));

          newOrderSingle.set(tradeAccount);
          newOrderSingle.set(new OrderQty(quantity));
          newOrderSingle.set(new Symbol(pair));
          newOrderSingle.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE));              

          try {
              Session.sendToTarget(newOrderSingle, tradeSession);
              
              System.out.println("FIXPROTOCITI:[sendNewOrder] New order has been: " + order_id + " : " + amount);
              System.out.println("FIXPROTOCITI:[sendNewOrder] Order Id: " + orderId);   
              System.out.println("FIXPROTOCITI:[sendNewOrder] Market amount: " + quantity);   
              System.out.println("FIXPROTOCITI:[sendNewOrder] Order type: Market");
              
              if (side.getValue()==1) {
                  System.out.println("FIXPROTOCITI:[sendNewOrder] Order side: Buy");               
              }
              if (side.getValue()==2) {
                  System.out.println("FIXPROTOCITI:[sendNewOrder] Order side: Sell");               
              }
              
              System.out.println("FIXPROTOCITI:[sendNewOrder] Market_pair: " + pair);  
              System.out.println("FIXPROTOCITI:[sendNewOrder] Trade account: " + tradeAccount.toString());  
              System.out.println("FIXPROTOCITI:[sendNewOrder] Trade session: " + tradeSession.getSenderCompID());  

          } catch (SessionNotFound ex) {
             //java.util.logging.Logger.getLogger(FixProtocol.class.getName()).log(Level.SEVERE, null, ex);
              
          }

          Thread.sleep(messageProcessingDelayMicroseconds);

          }
}

