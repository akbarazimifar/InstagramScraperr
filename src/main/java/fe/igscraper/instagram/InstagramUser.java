package fe.igscraper.instagram;

import fe.igscraper.instagram.content.type.*;

import java.util.*;

import fe.igscraper.sqlite.*;
import com.google.gson.*;

import java.io.*;
import java.net.*;
import java.sql.*;

import fe.igscraper.instagram.content.finder.*;

public class InstagramUser {
    private String id;
    private String username;
    private File saveFolder;
    private boolean overwriteFiles;
    private InstagramAccount account;
    private List<ContentType> contentTypes;
    private List<InstagramContent> newContent;
    private static String CREATE_TABLE_SQL = "CREATE TABLE IF NOT EXISTS %s (id integer PRIMARY KEY AUTOINCREMENT, url text NOT NULL, datetime text)";

    public InstagramUser(String id, String username, File saveFolder, boolean overwriteFiles, InstagramAccount account) {
        this.contentTypes = new ArrayList<ContentType>();
        this.newContent = new ArrayList<InstagramContent>();
        this.id = id;
        this.username = username;
        this.saveFolder = saveFolder;
        this.overwriteFiles = overwriteFiles;
        this.account = account;
    }

    public void enableType(ContentType ct) {
        this.contentTypes.add(ct);
    }

    public void enableMetadata() {
        for (ContentType type : this.contentTypes) {
            type.enableMetadata();
        }
    }

    public String toContentString() {
        return this.contentTypes.toString().substring(1, this.contentTypes.toString().length() - 1);
    }

    private int addNewContent(List<InstagramContent> content) {
        this.newContent.addAll(content);
        return content.size();
    }

    public InstagramUser createTables(SQLiteDatabase database) throws SQLException {
        for (ContentType ct : this.contentTypes) {
            database.prepareStatement("CREATE TABLE IF NOT EXISTS %s (id integer PRIMARY KEY AUTOINCREMENT, url text NOT NULL, datetime text)", ct.getTable(this)).execute();
        }
        return this;
    }

    public InstagramContent getNewContent(int index) {
        return this.newContent.get(index);
    }

    public JsonElement readGetRequestJson(String url) throws IOException {
        return this.account.readGetRequestJson(url);
    }

    public File getSaveFolder() {
        return this.saveFolder;
    }

    public boolean shouldOverwriteFiles() {
        return this.overwriteFiles;
    }

    public InstagramAccount getAccount() {
        return this.account;
    }

    public HttpURLConnection sendGetRequest(String url) throws IOException {
        return this.account.sendGetRequest(url);
    }

    public List<InstagramContent> getContent() {
        return this.newContent;
    }

    public List<ContentType> getContentTypes() {
        return this.contentTypes;
    }

    public String getId() {
        return this.id;
    }

    public String getUsername() {
        return this.username;
    }

    public enum ContentType {
        POST("posts", (ContentFinder) new PostContentFinder()),
        STORY("stories", (ContentFinder) new StoryContentFinder()),
        COLLECTION("collections", (ContentFinder) new CollectionContentFinder()),
        PROFILE_PICTURE("profilepictures", (ContentFinder) new ProfilePictureContentFinder());

        private String type;
        private ContentFinder cf;
        private String fileNameScheme;
        private static String TABLE_PREFIX = "igu_%s_";
        private static String QUERY_DB_CONTENT_SQL = "SELECT url FROM %s";

        private ContentType(String type, ContentFinder cf) {
            this.type = type;
            this.cf = cf;
        }

        public static ContentType findByType(String type) {
            for (ContentType ct : values()) {
                if (ct.type.equalsIgnoreCase(type)) {
                    return ct;
                }
            }
            return null;
        }

        public String getTable(InstagramUser ig) {
            return String.format("igu_%s_" + this.type, ig.getId());
        }

        public void setNameScheme(String fileNameScheme) {
            this.cf.applyNamingScheme(fileNameScheme);
        }

        public void enableMetadata() {
            this.cf.enableMetadata();
        }

        public int findContent(InstagramUser iu, List<String> ignore) throws IOException {
            return iu.addNewContent(this.cf.findContent(iu, ignore));
        }

        public List<String> queryDatabase(SQLiteDatabase database, InstagramUser instagramUser) throws SQLException {
            List<String> existingContent = new ArrayList<String>();
            ResultSet rs = database.prepareStatement("SELECT url FROM %s", this.getTable(instagramUser)).executeQuery();
            while (rs.next()) {
                existingContent.add(rs.getString("url"));
            }
            return existingContent;
        }

        public ContentFinder getFinder() {
            return this.cf;
        }
    }
}
