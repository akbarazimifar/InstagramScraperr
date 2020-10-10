package fe.igscraper;

import com.google.gson.*;
import fe.igscraper.instagram.InstagramAccount;
import fe.igscraper.instagram.InstagramUser;
import fe.igscraper.instagram.exception.InstagramLoginFailedException;
import fe.igscraper.sqlite.SQLiteDatabase;
import fe.logger.Logger;
import fe.request.RequestUtil;
import fe.request.proxy.AuthenticationProxy;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ConfigLoader {
    private SQLiteDatabase database;
    private JsonObject jsonConfig;
    private boolean metadata;


    private Logger logger = new Logger("ConfigLoader", true);

    private List<InstagramAccount> accounts = new ArrayList<>();
    private List<InstagramUser> users = new ArrayList<>();

    private static final String CHECK_ACCOUNT_URL = "https://i.instagram.com/api/v1/users/%s/info/";
    //taken from https://github.com/ping/instagram_private_api/blob/master/instagram_private_api/constants.py
    public static final String CHECK_ACCOUNT_USERAGENT = "Instagram 76.0.0.15.395 Android (24/7.0; 640dpi; 1440x2560; samsung; SM-G930F; herolte; samsungexynos8890; en_US; 138226743)";

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
            JsonObject proxyObj = obj.getAsJsonObject("proxy");
            if (proxyObj != null) {
                instagramProxy = loadProxy(proxyObj);
            }

            InstagramAccount session = new InstagramAccount(username, password,
                    (obj.getAsJsonPrimitive("sessionid") == null) ? null : obj.getAsJsonPrimitive("sessionid").getAsString(),
                    instagramProxy);
            try {
                this.logger.print(Logger.Type.INFO, "Loaded account %s", username);
                session.login(obj);
                this.accounts.add(session);
            } catch (IOException e) {
                this.logger.print(Logger.Type.ERROR, e.getMessage());
            } catch (InstagramLoginFailedException ignored) {
            }
        }
    }

    private AuthenticationProxy loadProxy(JsonObject proxyObj) {
        AuthenticationProxy instagramProxy = new AuthenticationProxy();
        JsonObject authObj;
        if ((authObj = proxyObj.getAsJsonObject("auth")) != null) {
            instagramProxy.setUsername(authObj.getAsJsonPrimitive("username").getAsString());
            instagramProxy.setPassword(authObj.getAsJsonPrimitive("password").getAsString());
        }
        instagramProxy.setProxy(new Proxy(Proxy.Type.valueOf(proxyObj.getAsJsonPrimitive("type").getAsString()), new InetSocketAddress(proxyObj.getAsJsonPrimitive("ip").getAsString(), proxyObj.getAsJsonPrimitive("port").getAsInt())));

        return instagramProxy;
    }

    public List<AuthenticationProxy> loadDownloadProxies() {
        List<AuthenticationProxy> proxies = new ArrayList<>();

        JsonArray downloadProxies = this.jsonConfig.getAsJsonArray("download_proxies");
        for (JsonElement el : downloadProxies) {
            JsonObject obj = (JsonObject) el;
            proxies.add(loadProxy(obj));
        }

        logger.print(Logger.Type.INFO, "Loaded %d download proxies", proxies.size());
        return proxies;
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
                    JsonArray idArray;
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
                        String id;
                        if (i < idArray.size()) {
                            id = idArray.get(i).getAsString();
                        } else {
                            this.logger.print(Logger.Type.WARNING, "No id found for user %s, searching now..", username);
                            Thread.sleep(3000);
                            if ((id = this.findUserIdFromUsername(account, username)) == null) {
                                this.logger.print(Logger.Type.ERROR, "Couldn't find id for user %s, skipping..", username);
                                idArray.add("no id found");
                            } else {
                                idArray.add(id);
                            }
                        }

                        this.logger.print(Logger.Type.INFO, "Checking if account %s (%s) exists (with %s)", username, id, account.getUsername());
                        this.addAccountIfExists(dtNow, obj, username, account, id);
                    }
                } else {
                    String username = usernameEl.getAsString();
                    InstagramAccount account = this.getInstagramAccount(username, (igLogin == null) ? null : igLogin.getAsString());
                    String id;
                    if (obj.getAsJsonPrimitive("id") == null) {
                        this.logger.print(Logger.Type.WARNING, "No id found for user %s, searching now..", username);
                        Thread.sleep(3000);
                        if ((id = this.findUserIdFromUsername(account, username)) == null) {
                            this.logger.print(Logger.Type.ERROR, "Couldn't find id for user %s, skipping..", username);
                        }
                        obj.addProperty("id", id);
                    } else {
                        id = obj.getAsJsonPrimitive("id").getAsString();
                    }

                    this.addAccountIfExists(dtNow, obj, username, account, id);
                }
            } catch (IOException | SQLException | InterruptedException e) {
                this.logger.print(Logger.Type.ERROR, "%s", e);
            }
        }
    }

    private void addAccountIfExists(LocalDateTime dtNow, JsonObject obj, String username, InstagramAccount account, String id) throws IOException, SQLException {
        AccountType type = this.checkExistsAccount(account, id);
        if (type != AccountType.NON_EXISTENT) {
            this.users.add(account.loadUser(id, username, obj, dtNow, this.metadata, type).createTables(this.database));
        } else {
            this.logger.print(Logger.Type.WARNING, "User %s (%s) does not seem to exist!", username, id);
        }
    }

    private AccountType checkExistsAccount(InstagramAccount account, String id) throws IOException {
        HttpURLConnection con = account.sendGetRequest(String.format(CHECK_ACCOUNT_URL, id), CHECK_ACCOUNT_USERAGENT);
        if (con.getResponseCode() == 200) {
            JsonObject obj = (JsonObject) JsonParser.parseReader(new BufferedReader(new InputStreamReader(con.getInputStream())));
            return obj.getAsJsonObject("user").getAsJsonPrimitive("is_private").getAsBoolean() ? AccountType.PRIVATE : AccountType.NON_PRIVATE;
        }

        return AccountType.NON_EXISTENT;
    }

    public enum AccountType {
        NON_EXISTENT, PRIVATE, NON_PRIVATE;
    }

    private InstagramAccount getInstagramAccount(String username, String instagramLogin) {
        InstagramAccount account;
        if (instagramLogin == null) {
            account = this.accounts.get(this.accountRotate++);
            if (this.accountRotate == this.accounts.size()) {
                this.accountRotate = 0;
            }

            this.logger.print(Logger.Type.INFO, "No login found for user %s, using account rotate: %s", username, account.getUsername());
        } else if ((account = this.findAccountByUsername(instagramLogin)) == null) {
            this.logger.print(Logger.Type.ERROR, "Failed to load user %s, because %s seems to be broken", username, instagramLogin);
        }


        return account;
    }

    private InstagramAccount findAccountByUsername(String username) {
        return this.accounts.stream().filter(ia -> ia.getUsername().equalsIgnoreCase(username)).findFirst().orElse(null);
    }


    private String findUserIdFromUsername(InstagramAccount account, String username) throws IOException, JsonSyntaxException {
        HttpURLConnection con = account.sendGetRequest(String.format(ID_FINDER_URL, username));
        if (con.getResponseCode() == 200) {
            String resp = RequestUtil.readResponse(con);
            JsonObject obj = (JsonObject) new JsonParser().parse(resp);
            try {
                return obj.getAsJsonArray("users").get(0).getAsJsonObject().getAsJsonObject("user").getAsJsonPrimitive("pk").getAsString();
            } catch (Exception e) {
                return null;
            }
        } else {
            logger.print(Logger.Type.ERROR, "Error while requesting id: %s", con.getResponseMessage());
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
