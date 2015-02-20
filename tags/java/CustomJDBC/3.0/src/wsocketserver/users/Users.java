
package wsocketserver.users;

import java.util.concurrent.ConcurrentHashMap;

public abstract class Users{
    
    public static ConcurrentHashMap<String, Users> UserList = new ConcurrentHashMap();
    public String   key;
    public String   hash;
    public int      check;
    public int      id;
    public int      order_open;
    public long     team_id;
    public String   name;
    public String   role;
    public String   type;    
    public String   lang = "un";
    public int      dead = 0;
    public Boolean  json_enabled = false; 
    public Boolean  response_encode = false;
             

    public abstract void addNewUser(String key, Users user);
    public abstract void removeUser(String key);
    public abstract void assignHash(String key, String hash, String type);    
    public abstract boolean checkUser(String hash, String type_s);  
    public abstract Users getUser(String key);
}