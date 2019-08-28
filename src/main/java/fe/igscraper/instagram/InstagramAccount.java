package fe.igscraper.instagram;

import fe.logger.*;
import fe.igscraper.instagram.request.*;

import java.net.*;

import fe.request.*;

import java.util.zip.*;

import fe.igscraper.instagram.exception.*;
import fe.request.postcontent.*;

import java.time.*;
import java.time.format.*;
import java.io.*;

import com.google.gson.*;

import java.util.regex.*;
import java.util.*;

public class InstagramAccount {
    private String username;
    private String password;
    private AuthenticationProxy proxy;
    private Logger logger;
    private String sessionId;
    private InstagramRequest request;
    public static final long RATE_LIMIT_DELAY = 1200000L;
    public static final String MID_URL = "https://www.instagram.com/web/__mid/";
    public static final String LOGIN_URL = "https://www.instagram.com/accounts/login/ajax/";
    public static final String SESSION_TEST_URL = "https://www.instagram.com/graphql/query/?query_hash=d6f4427fbe92d846298cf93df0b937d3";
    public static final Pattern DATE_TIME_PATTERN;

    public InstagramAccount(String username, String password, String sessionId, AuthenticationProxy proxy) {
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
        this.proxy = proxy;
        this.logger = new Logger(String.format("InstagramSession: %s", username), true);
        this.request = new InstagramRequest(this.proxy, true);
        if (this.sessionId != null) {
            this.request.putCookie("sessionid", this.sessionId);
        }
    }

    public HttpURLConnection sendGetRequest(String url) throws IOException {
        return this.request.sendGetRequest(url);
    }

    private JsonElement readJson(HttpURLConnection con) throws JsonSyntaxException, IOException {
        List<String> encoding = Request.findHeader("content-encoding", con);
        return new JsonParser().parse(new BufferedReader(new InputStreamReader((encoding != null && encoding.get(0).equalsIgnoreCase("gzip")) ? new GZIPInputStream(con.getInputStream()) : con.getInputStream())).readLine());
    }

    public JsonElement readGetRequestJson(String url) throws IOException {
        HttpURLConnection con;
        while ((con = this.request.sendGetRequest(url)) == null) {
            this.logger.print(Logger.Type.WARNING, "Account ratelimited, going to sleep for 120seconds..");
            try {
                Thread.sleep(1200000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            this.logger.print(Logger.Type.INFO, "Retrying..");
        }
        return this.readJson(con);
    }

    public void login(JsonObject obj) throws IOException, InstagramLoginFailedException {
        if (this.sessionId == null || !this.checkSessionId()) {
            this.logger.print(Logger.Type.WARNING, "Stored session is invalid or does not exist, generating a new one..");
            this.generateNewSessionId();
            if (obj != null) {
                this.setJsonSession(obj);
            }
        } else {
            this.logger.print(Logger.Type.INFO, "Stored session is still valid!");
        }
        this.logger.print(Logger.Type.INFO, "Logged in successfully");
    }

    private void setJsonSession(JsonObject obj) {
        if (obj.getAsJsonPrimitive("sessionid") == null) {
            obj.addProperty("sessionid", this.sessionId);
        } else {
            obj.remove("sessionid");
            obj.addProperty("sessionid", this.sessionId);
        }
    }

    private void generateNewSessionId() throws InstagramLoginFailedException, IOException {
        this.request.putCookie("sessionid", null);
        InstagramRequest ir = new InstagramRequest(this.proxy, true);
        ir.putCookies(new String[][]{{"ig_pr", "1"}, {"ig_vw", "1920"}, {"ig_cb", "1"}});
        HttpURLConnection midCon = ir.sendGetRequest("https://www.instagram.com/web/__mid/");
        String csrftoken = Request.findCookie("csrftoken", midCon);
        String mid = Request.findCookie("mid", midCon);
        ir.putHeader("X-CSRFToken", csrftoken);
        String sessionFound = null;
        HttpURLConnection connection = ir.sendPostRequest("https://www.instagram.com/accounts/login/ajax/", new FormUrlEncoded(new String[][]{{"username", this.username}, {"password", this.password}}));
        if (connection.getResponseCode() == 400) {
            JsonObject obj = (JsonObject) new JsonParser().parse(Request.readResponse(connection, true));
            String url = String.format("https://www.instagram.com%s", obj.getAsJsonPrimitive("checkpoint_url").getAsString());
            ir.putHeader("Referer", url);
            ir.putCookies(new String[][]{{"mid", mid}, {"csrftoken", csrftoken}});
            ir.sendPostRequest(url, new FormUrlEncoded(new String[][]{{"choice", "1"}}));
            this.logger.print(Logger.Type.WARNING, "A code has been sent to your email address, input it here:");
            Scanner scanner = new Scanner(System.in);
            String code = scanner.nextLine().trim();
            HttpURLConnection securityConnection = ir.sendPostRequest(url, new FormUrlEncoded(new String[][]{{"security_code", code}}));
            String securityResponse = Request.readResponse(securityConnection, false);
            JsonObject securityResponseObj = (JsonObject) new JsonParser().parse(securityResponse);
            if (!securityResponseObj.getAsJsonPrimitive("status").getAsString().equalsIgnoreCase("ok")) {
                throw new UnsupportedOperationException("Replay will be implemented soon");
            }
            sessionFound = Request.findCookie("sessionid", securityConnection);
        } else {
            sessionFound = Request.findCookie("sessionid", connection);
        }
        if (sessionFound != null) {
            this.sessionId = sessionFound;
            this.request.putCookie("sessionid", this.sessionId);
            if (!this.checkSessionId()) {
                this.logger.print(Logger.Type.ERROR, "Something is wrong with the generated sessionId, please log into instagram via the webpage and resolve the error");
            }
            return;
        }
        this.logger.print(Logger.Type.ERROR, String.format("Login failed (%s)", Request.readResponse(connection, true)));
        throw new InstagramLoginFailedException();
    }

    private boolean checkSessionId() throws IOException {
        return this.request.sendGetRequest("https://www.instagram.com/graphql/query/?query_hash=d6f4427fbe92d846298cf93df0b937d3").getResponseCode() == 200;
    }

    public InstagramUser loadUser(String id, String username, JsonObject obj, LocalDateTime now, boolean metadata) throws IOException {
        String saveFolder = obj.getAsJsonPrimitive("save_folder").getAsString();
        Matcher matcher = InstagramAccount.DATE_TIME_PATTERN.matcher(saveFolder);
        if (matcher.matches()) {
            saveFolder = saveFolder.replaceAll(Pattern.quote(matcher.group(1)), now.format(DateTimeFormatter.ofPattern(matcher.group(2))));
        }
        saveFolder = saveFolder.replace("%username%", username);
        boolean overwriteFiles = false;
        if (obj.getAsJsonPrimitive("overwrite_files") != null) {
            overwriteFiles = obj.getAsJsonPrimitive("overwrite_files").getAsBoolean();
        }
        InstagramUser instagramUser = new InstagramUser(id, username, new File(saveFolder), overwriteFiles, this);
        for (JsonElement saveJe : obj.getAsJsonArray("save")) {
            InstagramUser.ContentType ct = null;
            if (saveJe.isJsonPrimitive()) {
                ct = InstagramUser.ContentType.findByType(((JsonPrimitive) saveJe).getAsString());
            } else {
                JsonObject saveObj = (JsonObject) saveJe;
                ct = InstagramUser.ContentType.findByType(saveObj.getAsJsonPrimitive("type").getAsString());
                ct.setNameScheme(saveObj.getAsJsonPrimitive("fileNameScheme").getAsString());
            }
            instagramUser.enableType(ct);
            if (metadata) {
                instagramUser.enableMetadata();
            }
        }
        this.logger.print(Logger.Type.INFO, "Loaded user %s (%s), using account %s to download", username, instagramUser.toContentString(), instagramUser.getAccount().getUsername());
        return instagramUser;
    }

    public String getUsername() {
        return this.username;
    }

    static {
        DATE_TIME_PATTERN = Pattern.compile(".*(\\%current_datetime\\{(.+)\\}\\%)");
    }
}
