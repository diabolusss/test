/*     */ package wsocketserver.jdbc;
/*     */ 
/*     */ import java.sql.Connection;
/*     */ import java.sql.Driver;
/*     */ import java.sql.DriverManager;
/*     */ import java.sql.SQLException;
/*     */ import java.util.ArrayList;
/*     */ import java.util.Iterator;
/*     */ 
/*     */ public class DBConnection
/*     */ {
/*     */   private static DBConnection instance;
/*     */   private static int clients;
/*     */   private String URL;
/*     */   private String db_user;
/*     */   private String db_pass;
/*     */   private int maxConn;
/*  31 */   private ArrayList freeConnections = new ArrayList();
/*     */ 
/*     */   public DBConnection(String URL, String db_user, String db_pass, int maxConn)
/*     */   {
/*  44 */     this.URL = URL;
/*  45 */     this.db_user = db_user;
/*  46 */     this.db_pass = db_pass;
/*  47 */     this.maxConn = maxConn;

/*     */     try{
/*  51 */       Driver driver = (Driver)Class.forName("com.mysql.jdbc.Driver").newInstance();
/*  52 */       DriverManager.registerDriver(driver);
/*  53 */       System.out.println("JDBC: [INIT] Registered JDBC driver ");

/*     */     } catch (ClassNotFoundException e){
                    System.out.println("JDBC: [INIT] ERROR " + e.getLocalizedMessage());
    
              } catch (InstantiationException e){
                  System.out.println("JDBC: [INIT] ERROR " + e.getLocalizedMessage());
                  
              } catch (IllegalAccessException e){
                  System.out.println("JDBC: [INIT] ERROR " + e.getLocalizedMessage());
                  
              } catch (SQLException e) {
/*  55 */       System.out.println("JDBC: [INIT] ERROR Can't register JDBC driver " + e.getLocalizedMessage());

/*     */     }
/*     */   }
/*     */ 
/*     */   public static synchronized DBConnection getInstance(String URL, String db_user, String db_pass, int maxConn)
/*     */   {
/*  70 */     if (instance == null) {
/*  71 */       instance = new DBConnection(URL, db_user, db_pass, maxConn);
/*     */     }
/*  73 */     clients += 1;
/*  74 */     return instance;
/*     */   }
/*     */ 
/*     */   public synchronized Connection getConnection()
/*     */   {
/*  83 */     Connection con = null;
/*  84 */     if (!this.freeConnections.isEmpty()) {
/*  85 */       con = (Connection)this.freeConnections.get(this.freeConnections.size() - 1);
/*     */       try {
/*  87 */         if (con.isClosed()) {
/*  88 */           con.close();
/*  89 */           this.freeConnections.remove(con);
/*  90 */           System.out.println("JDBC: [CONN] Removed bad connection ");
/*     */ 
/*  93 */           con = getConnection();
/*     */         }
/*     */       } catch (SQLException e) {
/*  96 */         e.printStackTrace(System.out);
/*  97 */         this.freeConnections.remove(con);
/*  98 */         System.out.println("JDBC: [CONN] ERROR Removed bad connection ");
/*     */ 
/* 101 */         con = getConnection();
/*     */       }
/*     */       catch (Exception e) {
/* 104 */         e.printStackTrace(System.out);
/*     */ 
/* 106 */         this.freeConnections.remove(con);
/* 107 */         System.out.println("JDBC: [CONN] ERROR Removed bad connection ");
/*     */ 
/* 110 */         con = getConnection();
/*     */       }
/*     */     } else {
/* 113 */       System.out.println("JDBC: [CONN] Try to get new connection...");
/* 114 */       con = newConnection();
/*     */     }
/* 116 */     return con;
/*     */   }
/*     */ 
/*     */   private Connection newConnection() {
/* 120 */     Connection con = null;
/*     */     try {
/* 122 */       con = DriverManager.getConnection(this.URL, this.db_user, this.db_pass);
/* 123 */       System.out.println("JDBC: [CONN] Created a new connection in pool");

/*     */     } catch (SQLException e) {
/* 125 */       System.out.println("JDBC: [CONN] ERROR Can't create a new connection for " + this.URL + " " + this.db_user + " " + this.db_pass);
/* 126 */       e.printStackTrace(System.out);

/* 127 */       return null;
/*     */     }
/* 129 */     return con;
/*     */   }
/*     */ 
            /**
             * NB What would do if 'if' statement is false!?
             * @param con 
             */
/*     */   public synchronized void freeConnection(Connection con)
/*     */   {
/* 138 */     if ((con != null) && 
/* 139 */       (this.freeConnections.size() <= this.maxConn))
/* 140 */       this.freeConnections.add(con);
/*     */   }
/*     */ 
/*     */   public synchronized void release()
/*     */   {
/* 149 */     Iterator allConnections = this.freeConnections.iterator();
/* 150 */     while (allConnections.hasNext()) {
/* 151 */       Connection con = (Connection)allConnections.next();
/*     */       try {
/* 153 */         con.close();
/* 154 */         System.out.println("JDBC: [RLES] Closed connection for pool ");
/*     */       } catch (SQLException e) {
/* 156 */         System.out.println("JDBC: [RLES] ERROR Can't close connection for pool ");
/*     */       }
/*     */     }
/* 159 */     this.freeConnections.clear();
/*     */   }
/*     */ }