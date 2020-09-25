package fe.igscraper.instagram;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import fe.igscraper.instagram.exception.InstagramLoginFailedException;
import fe.igscraper.instagram.request.InstagramRequest;
import fe.logger.Logger;
import fe.request.RequestOverride;
import fe.request.RequestUtil;
import fe.request.data.Cookie;
import fe.request.data.Header;
import fe.request.postcontent.FormUrlEncoded;
import fe.request.proxy.AuthenticationProxy;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstagramAccount {
    private final String username;
    private final String password;
    private final AuthenticationProxy proxy;
    private final Logger logger;
    private String sessionId;
    private final InstagramRequest request;
    public static final long RATE_LIMIT_DELAY = 1200000L;
    public static final String MID_URL = "https://www.instagram.com/web/__mid/";
    public static final String LOGIN_URL = "https://www.instagram.com/accounts/login/ajax/";
    public static final String KEY_INFO = "https://www.instagram.com/data/shared_data/";
    public static final String INSTAGRAM_PASSWORD_STRING = "#PWD_INSTAGRAM_BROWSER:%s:%d:%s";
    public static final String SESSION_TEST_URL = "https://www.instagram.com/graphql/query/?query_hash=d6f4427fbe92d846298cf93df0b937d3";

    public static final Pattern DATE_TIME_PATTERN = Pattern.compile(".*(\\%current_datetime\\{(.+)\\}\\%)");

    public InstagramAccount(String username, String password, String sessionId, AuthenticationProxy proxy) {
        this.username = username;
        this.password = password;
        this.sessionId = sessionId;
        this.proxy = proxy;
        this.logger = new Logger(String.format("InstagramSession: %s", username), true);
        this.request = new InstagramRequest(this.proxy, true);
        if (this.sessionId != null) {
            this.request.putCookie(new Cookie("sessionid", this.sessionId));
        }
    }

    public HttpURLConnection sendGetRequest(String url) throws IOException {
        return this.request.sendGetRequest(url);
    }

    public HttpURLConnection sendGetRequest(String url, String useragent) throws IOException {
        return this.request.sendGetRequest(url, new RequestOverride().putHeader(new Header("User-Agent", useragent)));
    }

    private JsonElement readJson(HttpURLConnection con) throws JsonSyntaxException, IOException {
        return new JsonParser().parse(RequestUtil.readResponse(con));
    }

    public JsonElement readGetRequestJson(String url, String useragent) throws IOException {
        return this.readGetRequestJson(url, new RequestOverride().putHeader(new Header("User-Agent", useragent)));
    }

    public JsonElement readGetRequestJson(String url) throws IOException {
        return this.readGetRequestJson(url, new RequestOverride());
    }

    private JsonElement readGetRequestJson(String url, RequestOverride requestOverride) throws IOException {
        HttpURLConnection con;
        while ((con = this.request.sendGetRequest(url, requestOverride)) == null) {
            this.logger.print(Logger.Type.WARNING, "Account ratelimited, going to sleep for %dseconds..", RATE_LIMIT_DELAY / 10000);
            try {
                Thread.sleep(RATE_LIMIT_DELAY);
            } catch (InterruptedException ignored) {
            }
            this.logger.print(Logger.Type.INFO, "Retrying..");
        }

//        System.out.println("response code " + con.getResponseCode() + " for url " + url);

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
        if (obj.getAsJsonPrimitive("sessionid") != null) {
            obj.remove("sessionid");
        }

        obj.addProperty("sessionid", this.sessionId);
    }

    private static final Random RANDOM = new Random();
    private static final int GCM_TAG_LENGTH = 16;

//    public static void main(String[] args) {
//        try {
//            encryptPassword("4jk41329@cock.li");
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    private String encryptPassword(String password) throws Exception {
//        JsonObject object = (JsonObject) JsonParser.parseReader(new BufferedReader(new InputStreamReader(this.request.sendGetRequest(KEY_INFO).getInputStream())));
//        JsonObject encryptionObj = object.get("encryption").getAsJsonObject();
//
//        String keyId = encryptionObj.get("key_id").getAsString();
//        String pubKey = encryptionObj.get("public_key").getAsString();
//        String version = encryptionObj.get("version").getAsString();
//
//        long time = System.currentTimeMillis() / 1000;
//        byte[] keyBytes = Util.hexStringToByteArray(pubKey);
//        byte[] key = new byte[32];
//
//        RANDOM.nextBytes(key);
//
//        byte[] iv = new byte[12];
//
//        byte[] plainText = password.getBytes();
//
//        AESGCm
//
//        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
//        SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
//        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(GCM_TAG_LENGTH * 8, iv);
//        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmParameterSpec);
//        cipher.updateAAD(String.valueOf(time).getBytes());
//
//        byte[] output = cipher.doFinal(plainText);
//
//        byte[] cipherText = new byte[output.length - GCM_TAG_LENGTH];
//        byte[] tag = new byte[GCM_TAG_LENGTH];
//
//        System.arraycopy(output, 0, cipherText, 0, cipherText.length);
//        System.arraycopy(output, output.length - GCM_TAG_LENGTH, tag, 0, tag.length);
//
//        SimpleBox box = new SimpleBox(keyBytes);
//        byte[] encryptedKey = box.seal(key);
//
//        ByteBuffer buffer = ByteBuffer.allocate(2);
//        buffer.putShort((short) encryptedKey.length);
//        byte[] bytesOfLen = buffer.array();
//
//        byte[] info = new byte[]{1, (byte) Integer.parseInt(keyId)};
//
//        byte[] x1 = concat(info, bytesOfLen);
//        byte[] x2 = concat(x1, encryptedKey);
//        byte[] x3 = concat(x2, tag);
//        byte[] x4 = concat(x3, cipherText);
//
//        String s = String.format(INSTAGRAM_PASSWORD_STRING, version, time, Base64.getEncoder().encodeToString(x4));
//        System.out.println(s);
//        return s;
//    }


    public static byte[] concat(byte[] first, byte[] second) {
        byte[] result = Arrays.copyOf(first, first.length + second.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    private void generateNewSessionId() throws IOException, InstagramLoginFailedException {
        this.request.putCookie(new Cookie("sessionid", "null"));
        InstagramRequest ir = new InstagramRequest(this.proxy, true);
        ir.putCookies(new Cookie("ig_pr", "1"), new Cookie("ig_vw", "1920"), new Cookie("ig_cb", "1"));

        HttpURLConnection midCon = ir.sendGetRequest(MID_URL);
        Cookie csrftokenCookie = RequestUtil.findCookie("csrftoken", midCon);
        Cookie midCookie = RequestUtil.findCookie("mid", midCon);
        ir.putHeader(new Header("X-CSRFToken", csrftokenCookie.getValue()));

        Cookie sessionCookie;
        HttpURLConnection connection
                = ir.sendPostRequest(LOGIN_URL, new FormUrlEncoded(
                new String[][]{
                        {"username", this.username},
                        {"enc_password", String.format(INSTAGRAM_PASSWORD_STRING, 0, System.currentTimeMillis() / 1000, this.password)},
                        {"queryParams", "{}"},
                        {"optIntoOneTap", "false"}}));

        if (connection.getResponseCode() == 400) {
            JsonObject obj = (JsonObject) new JsonParser().parse(RequestUtil.readResponse(connection, RequestUtil.StreamType.ERRORSTREAM));
            String url = String.format("https://www.instagram.com%s", obj.getAsJsonPrimitive("checkpoint_url").getAsString());
            ir.putHeader(new Header("Referer", url));
            ir.putCookies(new Cookie("mid", midCookie.getValue()), new Cookie("csrftoken", csrftokenCookie.getValue()));

            ir.sendPostRequest(url, new FormUrlEncoded(new String[][]{{"choice", "1"}}));

            this.logger.print(Logger.Type.WARNING, "A code has been sent to your email address, input it here: ");

            Scanner scanner = new Scanner(System.in);
            String code = scanner.nextLine().trim();

            HttpURLConnection securityConnection = ir.sendPostRequest(url, new FormUrlEncoded(new String[][]{{"security_code", code}}));
            String securityResponse = RequestUtil.readResponse(securityConnection, RequestUtil.StreamType.ERRORSTREAM);
            JsonObject securityResponseObj = (JsonObject) new JsonParser().parse(securityResponse);
            if (!securityResponseObj.getAsJsonPrimitive("status").getAsString().equalsIgnoreCase("ok")) {
                throw new UnsupportedOperationException("Replay will be implemented soon");
            }
            sessionCookie = RequestUtil.findCookie("sessionid", securityConnection);
        } else {
            sessionCookie = RequestUtil.findCookie("sessionid", connection);
        }
        if (sessionCookie != null) {
            this.sessionId = sessionCookie.getValue();
            this.request.putCookie(sessionCookie);

            if (!this.checkSessionId()) {
                this.logger.print(Logger.Type.ERROR, "Something is wrong with the generated sessionId, please log into instagram via the webpage and resolve the error");
            }
            return;
        }
        this.logger.print(Logger.Type.ERROR, String.format("Login failed (%s)", RequestUtil.readResponse(connection)));
        throw new InstagramLoginFailedException();
    }

    private boolean checkSessionId() throws IOException {
        return this.request.sendGetRequest(SESSION_TEST_URL).getResponseCode() == 200;
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
            InstagramUser.ContentType ct;
            if (saveJe.isJsonPrimitive()) {
                ct = InstagramUser.ContentType.findByType(saveJe.getAsString());
            } else {
                JsonObject saveObj = (JsonObject) saveJe;
                ct = InstagramUser.ContentType.findByType(saveObj.getAsJsonPrimitive("type").getAsString());
                if (ct != null) {
                    ct.setNameScheme(saveObj.getAsJsonPrimitive("fileNameScheme").getAsString());
                }
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

}
