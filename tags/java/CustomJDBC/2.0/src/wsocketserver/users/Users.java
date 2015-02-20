
package wsocketserver.users;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Users{
    
/*  25 */   public static ConcurrentHashMap<String, Users> UserList;
/*     */   public String key;
/*     */   public String hash;
/*     */   public int check;
/*     */   public int id;
    public int order_open;
/*     */   public String name;
/*     */   public String role;
/*     */   public String type;
             

    public abstract void addNewUser(String key, Users user);
    public abstract void removeUser(String key);
    public abstract void assignHash(String key, String hash, String type);    
    public abstract boolean checkUser(String hash, String type_s);  
    public abstract Users getUser(String key);
}