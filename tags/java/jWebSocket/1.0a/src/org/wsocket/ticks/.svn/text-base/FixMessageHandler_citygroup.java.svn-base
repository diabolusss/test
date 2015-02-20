package org.wsocket.ticks;

import java.util.Properties;

import quickfix.ConfigError;
import quickfix.Session;
import quickfix.SessionID;
import quickfix.SessionNotFound;
import quickfix.SessionSettings;
import quickfix.field.Account;
import quickfix.field.ClOrdID;
import quickfix.field.Currency;
import quickfix.field.HandlInst;
import quickfix.field.OrdType;
import quickfix.field.OrderQty;
import quickfix.field.QuoteReqID;
import quickfix.field.QuoteRequestType;
import quickfix.field.QuoteType;
import quickfix.field.Side;
import quickfix.field.Symbol;
import quickfix.field.TransactTime;
import quickfix.fix44.QuoteRequest;

/*
 * This class constructs the FIX business messages that are sent by the client to the FIX server.
 */
public class FixMessageHandler_citygroup {
	
	//private SessionID tradeSession = null;
	private SessionID quoteSession = null;
	public SessionSettings settings = null;
	private Account quoteAccount;
	private static Account tradeAccount;
	
	public FixMessageHandler_citygroup(SessionID quoteSession, SessionID tradeSession, SessionSettings settings){
		this.quoteSession = quoteSession;
		//this.tradeSession = tradeSession;
		this.settings = settings;

		try {
			Properties props = this.settings.getSessionProperties(quoteSession, true);
                        
			String accountId = (String)props.get("Account");
			quoteAccount = new Account(accountId);
			
			props = this.settings.getSessionProperties(tradeSession, true);
                        
			accountId = (String)props.get("Account");
			tradeAccount = new Account(accountId);
                        
		} catch(ConfigError e){
			System.out.println("FIXMSGHANDLERCITI:[INIT] ConfigError" + e.getMessage());
		}
	}
	
	/*
	 * Create a quote request
	 */
	public QuoteRequest createQuoteRequest(){
		System.out.println("FIXMSGHANDLERCITI:[createQuoteRequest] Creating a quote request");

		String base = null;
		String pair = null;
		int quantity;
		OrderQty orderQty = null;

		Currency currency = null;

		try {
			Properties props = this.settings.getSessionProperties(quoteSession, true);
			
                        base = ((String)props.get("Base")).toUpperCase();
			pair = ((String)props.get("Pair")).toUpperCase();
			quantity = Integer.parseInt((String)props.get("Quantity"));
                        
			orderQty = new OrderQty(quantity);
			currency = new Currency(base);
                        
		} catch(ConfigError e){
			System.out.println("FIXMSGHANDLERCITI:[createQuoteRequest] ERROR ConfigError" + e.getMessage());
			return null;
                        
		}
		
		QuoteRequest quoteRequest = new QuoteRequest();
		quoteRequest.set(new QuoteReqID(pair + "-" + quantity + "-" + System.currentTimeMillis()));
		quickfix.fix44.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.QuoteRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(pair));
		noRelatedSym.set(new QuoteRequestType(QuoteRequestType.AUTOMATIC));
		noRelatedSym.set(new QuoteType(QuoteType.TRADEABLE));
		noRelatedSym.set(new Side(Side.BUY));
		noRelatedSym.set(orderQty);
		noRelatedSym.set(currency);
		noRelatedSym.set(quoteAccount);
		quoteRequest.addGroup(noRelatedSym);
		
		System.out.println("FIXMSGHANDLERCITI:[createQuoteRequest] QR object " + quoteRequest.toString());
		return quoteRequest;
	}
	
	/*
	 * Create a quote request for a mass quote
	 */
	public QuoteRequest createMassQuoteRequest(){
		System.out.println("FIXMSGHANDLERCITI:[createMassQouteRequest] Creating a mass quote request");
		
		String base = "USD"; //TODO must be in uppercase.
		String pair = "EURUSD"; //TODO must be in uppercase.
		
		QuoteRequestType quoteRequestType = new QuoteRequestType(QuoteRequestType.AUTOMATIC);
		QuoteType quoteType = new QuoteType(QuoteType.TRADEABLE);
		Side side = new Side(Side.BUY);
		int quantity = 5000000; //5,000,000
		OrderQty orderQty = new OrderQty(quantity);
		Currency currency = new Currency(base);
		
		QuoteRequest quoteRequest = new QuoteRequest();		
		
		quoteRequest.set(new QuoteReqID(pair + "-" + quantity + "-" + System.currentTimeMillis()));
		quickfix.fix44.QuoteRequest.NoRelatedSym noRelatedSym = new quickfix.fix44.QuoteRequest.NoRelatedSym();
		noRelatedSym.set(new Symbol(pair));
		noRelatedSym.set(quoteRequestType);
		noRelatedSym.set(quoteType);
		noRelatedSym.set(side);
		noRelatedSym.set(orderQty);
		noRelatedSym.set(currency);
		noRelatedSym.set(quoteAccount);
		//noRelatedSym.setField(new MassQuoteField('i'));
		quoteRequest.addGroup(noRelatedSym);

		return quoteRequest;
	}
	        
	/*
	 * Create a market order trade
	 */
       
	public static quickfix.fix44.NewOrderSingle createMarketOrderTrade(){
		String pair = "EURUSD";
		int quantity = 10000;
		ClOrdID orderId = new ClOrdID("111" + System.currentTimeMillis());
		quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(orderId, new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.MARKET));
		
		newOrderSingle.set(tradeAccount);
		newOrderSingle.set(new OrderQty(quantity));
		newOrderSingle.set(new Symbol(pair));
		newOrderSingle.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE));
		
		return newOrderSingle;
	}
	
	/*
	 * Create a limit order trade
	 */
/*        
	public quickfix.fix44.NewOrderSingle createLimitOrderTrade(){
		String pair = "EURUSD";
		int quantity = 2000000;
		
		ClOrdID orderId = new ClOrdID("222" + System.currentTimeMillis());
		quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(orderId, new Side(Side.BUY), new TransactTime(), new OrdType(OrdType.LIMIT));
		newOrderSingle.set(new OrderQty(quantity));
		newOrderSingle.set(new Symbol(pair));
		newOrderSingle.setField(new Price(78.324));
		newOrderSingle.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE));
		newOrderSingle.set(tradeAccount);
		return newOrderSingle;
	}

	/*
	 * Create a trade based on a previous quote
	 * Such orders require that a valid quote id is supplied and the order quantity is for less than the previously quoted amount
	 * 
	 * This method wont't block as the application is a basic single-threaded app. If a tradable quote has not been received, then
	 * this method will throw an exception.
	 */
/*
        public quickfix.fix44.NewOrderSingle createPreviousQuotedTrade() throws Exception {
		log.debug("createPreviousQuotedTrade called");
		if(CitiConsole.getInstance().getApplication().getTradeableQuote() == null){ 
			throw new Exception("No tradable quote was received with which to trade.");
		}
				
		Quote quote = CitiConsole.getInstance().getApplication().getTradeableQuote();
		QuoteID quoteID = quote.get(new QuoteID());
		
		ClOrdID orderId = new ClOrdID("333" + System.currentTimeMillis());
		Side s = new Side(Side.BUY);
		TransactTime t = new TransactTime();
		OrdType o = new OrdType(OrdType.PREVIOUSLY_QUOTED);
		
		quickfix.fix44.NewOrderSingle newOrderSingle = new quickfix.fix44.NewOrderSingle(orderId, s, t, o);

		newOrderSingle.set(quoteID);
		newOrderSingle.set(quote.getSymbol());
		
		newOrderSingle.set(new Price(quote.get(new BidPx()).getValue()));
		
		
		newOrderSingle.set(new OrderQty(1000000));
		
		newOrderSingle.set(new HandlInst(HandlInst.AUTOMATED_EXECUTION_ORDER_PRIVATE));
		newOrderSingle.set(tradeAccount);
		log.debug("Finished constructing previously quoted trade.");
		return newOrderSingle;
	}
*/
       
	public void send(quickfix.Message message, SessionID sessionID) {
		try {
			Session.sendToTarget(message, sessionID);
                        
		} catch (SessionNotFound e) {
			System.out.println("FIXMSGHANDLERCITI:[send] Error SessionNotFound " + e.getMessage());
			e.printStackTrace(System.out);
                        
                        
		}
	}
	
}
