package fe.igscraper;

import fe.igscraper.sqlite.*;
import fe.logger.*;
import fe.igscraper.instagram.*;

import java.io.*;
import java.net.*;

import fe.igscraper.instagram.exception.*;

import java.util.*;
import java.time.*;
import java.sql.*;

import com.google.gson.*;
import fe.request.*;

public class ConfigLoader {
    private SQLiteDatabase database;
    private JsonObject jsonConfig;
    private boolean metadata;


    private Logger logger = new Logger("ConfigLoader", true);

    private List<InstagramAccount> accounts = new ArrayList<>();
    private List<InstagramUser> users = new ArrayList<>();

    private static final String CHECK_ACCOUNT_URL = "https://i.instagram.com/api/v1/users/%s/info/";
    //taken from https://github.com/ping/instagram_private_api/blob/master/instagram_private_api/constants.py
    private static final String CHECK_ACCOUNT_USERAGENT = "Instagram 76.0.0.15.395 Android (24/7.0; 640dpi; 1440x2560; samsung; SM-G930F; herolte; samsungexynos8890; en_US; 138226743)";

    private int accountRotate;
    private final static String ID_FINDER_URL = "https://www.instagram.com/web/search/topsearch/?query=%s";

    public ConfigLoader(File config, boolean metadata) throws IOException {
        this.jsonConfig = (JsonObject) new JsonParser().parse(new BufferedReader(new FileReader(config)));
        this.database = new SQLiteDatabase(this.jsonConfig.getAsJsonPrimitive("database_path").getAsString());
        this.metadata = metadata;
    }

    public void loadLogins() {
        for (JsonElement login : this.jsonConfig.getAsJsonArray("instagram_logins")) {
            JsonObject obj = (JsonObject) login;
            String username = obj.getAsJsonPrimitive("username").getAsString();
            String password = obj.getAsJsonPrimitive("password").getAsString();
            AuthenticationProxy instagramProxy = new AuthenticationProxy();
            JsonObject proxyObj;
            if ((proxyObj = obj.getAsJsonObject("proxy")) != null) {
                JsonObject authObj;
                if ((authObj = proxyObj.getAsJsonObject("auth")) != null) {
                    instagramProxy.setUsername(authObj.getAsJsonPrimitive("username").getAsString());
                    instagramProxy.setPassword(authObj.getAsJsonPrimitive("password").getAsString());
                }
                instagramProxy.setProxy(new Proxy(Proxy.Type.valueOf(proxyObj.getAsJsonPrimitive("type").getAsString()), new InetSocketAddress(proxyObj.getAsJsonPrimitive("ip").getAsString(), proxyObj.getAsJsonPrimitive("port").getAsInt())));
            }
            InstagramAccount session = new InstagramAccount(username, password, (obj.getAsJsonPrimitive("sessionid") == null) ? null : obj.getAsJsonPrimitive("sessionid").getAsString(), instagramProxy);
            try {
                this.logger.print(Logger.Type.INFO, "Loaded account %s", username);
                session.login(obj);
                this.accounts.add(session);
            } catch (IOException e) {
                this.logger.print(Logger.Type.ERROR, e.getMessage());
            } catch (InstagramLoginFailedException ex) {
            }
        }
    }

    public void loadUsers() {
        JsonArray downloadUsersArr = this.jsonConfig.getAsJsonArray("download_users");
        LocalDateTime dtNow = LocalDateTime.now();
        for (JsonElement je : downloadUsersArr) {
            try {
                JsonObject obj = (JsonObject) je;
                JsonElement usernameEl = obj.get("username");
                JsonElement idEl = obj.get("id");
                JsonPrimitive igLogin = obj.getAsJsonPrimitive("instagram_login");
                if (usernameEl.isJsonArray()) {
                    JsonArray idArray = null;
                    if (idEl == null) {
                        idArray = new JsonArray();
                        obj.add("id", idArray);
                    } else {
                        idArray = (JsonArray) idEl;
                    }
                    JsonArray usernameElArray = (JsonArray) usernameEl;
                    for (int i = 0; i < usernameElArray.size(); ++i) {
                        String username = usernameElArray.get(i).getAsString();
                        InstagramAccount account = this.getInstagramAccount(username, (igLogin == null) ? null : igLogin.getAsString());
                        String id = null;
                        if (i < idArray.size()) {
                            id = idArray.get(i).getAsString();
                        } else {
                            this.logger.print(Logger.Type.WARNING, "No id found for user %s, searching now..", username);
                            if ((id = this.findUserIdFromUsername(account, username)) == null) {
                                this.logger.print(Logger.Type.ERROR, "Couldn't find id for user %s, skipping..", username);
                                idArray.add("no id found");
                            } else {
                                idArray.add(id);
                            }
                        }
                        if (this.checkExistsAccount(account, id)) {
                            this.users.add(account.loadUser(id, username, obj, dtNow, this.metadata).createTables(this.database));
                        } else {
                            this.logger.print(Logger.Type.WARNING, "User %s (%s) does not seem to exist!", username, id);
                        }
                    }
                } else {
                    String username = usernameEl.getAsString();
                    InstagramAccount account = this.getInstagramAccount(username, (igLogin == null) ? null : igLogin.getAsString());
                    String id = null;
                    if (obj.getAsJsonPrimitive("id") == null) {
                        this.logger.print(Logger.Type.WARNING, "No id found for user %s, searching now..", username);
                        if ((id = this.findUserIdFromUsername(account, username)) == null) {
                            this.logger.print(Logger.Type.ERROR, "Couldn't find id for user %s, skipping..", username);
                        }
                        obj.addProperty("id", id);
                    } else {
                        id = obj.getAsJsonPrimitive("id").getAsString();
                    }

                    if (this.checkExistsAccount(account, id)) {
                        this.users.add(account.loadUser(id, username, obj, dtNow, this.metadata).createTables(this.database));
                    } else {
                        this.logger.print(Logger.Type.WARNING, "User %s (%s) does not seem to exist!", username, id);
                    }
                }
            } catch (IOException | SQLException e) {
                this.logger.print(Logger.Type.ERROR, "%s", e);
            }
        }
    }

    private boolean checkExistsAccount(InstagramAccount account, String id) throws IOException {
        return account.sendGetRequest(String.format(CHECK_ACCOUNT_URL, id), CHECK_ACCOUNT_USERAGENT).getResponseCode() == 200;
    }

    private InstagramAccount getInstagramAccount(String username, String instagramLogin) {
        InstagramAccount account = null;
        if (instagramLogin == null) {
            this.logger.print(Logger.Type.INFO, "No login found for user %s, using account rotate", username);
            account = this.accounts.get(this.accountRotate++);
            if (this.accountRotate == this.accounts.size()) {
                this.accountRotate = 0;
            }
        } else if ((account = this.findAccountByUsername(instagramLogin)) == null) {
            this.logger.print(Logger.Type.ERROR, "Failed to load user %s, because %s seems to be broken", username, instagramLogin);
        }
        return account;
    }

    private InstagramAccount findAccountByUsername(String username) {
        return this.accounts.stream().filter(ia -> ia.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }


    private String findUserIdFromUsername(InstagramAccount account, String username) throws IOException {
        JsonObject obj = (JsonObject) new JsonParser().parse(Request.readResponse(account.sendGetRequest(String.format(ID_FINDER_URL, username)), false));
        try {
            return obj.getAsJsonArray("users").get(0).getAsJsonObject().getAsJsonObject("user").getAsJsonPrimitive("pk").getAsString();
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject getJsonConfig() {
        return this.jsonConfig;
    }

    public SQLiteDatabase getDatabase() {
        return this.database;
    }

    public List<InstagramUser> getUsers() {
        return this.users;
    }
}
