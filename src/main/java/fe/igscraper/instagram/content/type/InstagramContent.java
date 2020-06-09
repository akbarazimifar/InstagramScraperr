package fe.igscraper.instagram.content.type;

import fe.igscraper.instagram.*;
import fe.logger.*;
import fe.igscraper.instagram.content.metadata.*;
import fe.igscraper.sqlite.*;

import java.time.*;
import java.sql.*;

import com.google.gson.*;

import java.util.*;
import java.io.*;

public abstract class InstagramContent {
    protected Map<String, String> urls;
    protected InstagramUser instagramUser;
    protected String fileNameScheme;
    protected InstagramUser.ContentType contentType;
    private final Logger logger;
    private Metadata metadata;
    protected boolean metadataEnabled;
    private static final String STORE_SQL = "INSERT INTO %s (url, datetime) VALUES (?, ?)";

    public InstagramContent(InstagramUser.ContentType contentType, String fileNameScheme, InstagramUser instagramUser, boolean metadataEnabled) {
        this.urls = new HashMap<>();
        this.contentType = contentType;
        this.fileNameScheme = fileNameScheme;
        this.instagramUser = instagramUser;
        this.metadataEnabled = metadataEnabled;
        if (this.metadataEnabled) {
            this.metadata = new Metadata(this);
        }
        this.logger = new Logger(String.format("Downloader: %s", instagramUser.getUsername()), true);
    }

    public int storeDatabase(SQLiteDatabase database) throws SQLException {
        PreparedStatement stmnt = database.prepareStatement(STORE_SQL, this.contentType.getTable(this.instagramUser));

        stmnt.setString(1, this.toDatabaseString());
        stmnt.setString(2, LocalDateTime.now().toString());
        return stmnt.executeUpdate();
    }

    protected void addMetadata(JsonObject obj) {
        if (this.metadataEnabled) {
            this.metadata.addMetadata(obj);
        }
    }

    protected void addUrl(String url, String filename) {
        this.urls.put(url, filename);
    }

    public abstract JsonObject getMetadataIdentifier();

    public abstract String getMetadataFilename();

    public abstract String toDatabaseString();

    public void printLog(Logger.Type type, String message) {
        this.logger.print(type, message);
    }

    public void download() throws IOException {
        if (!this.instagramUser.getSaveFolder().exists()) {
            this.instagramUser.getSaveFolder().mkdirs();
        }
        for (Map.Entry<String, String> ent : this.urls.entrySet()) {
            String file = ent.getValue();
            if (file.contains("/")) {
                File folder = new File(this.instagramUser.getSaveFolder(), file.substring(0, file.lastIndexOf("/")));
                if (!folder.exists()) {
                    folder.mkdirs();
                }
            }
            if (this.metadataEnabled) {
                this.metadata.save(new File(this.instagramUser.getSaveFolder(), this.getMetadataFilename()));
            }
            File outputFile = new File(this.instagramUser.getSaveFolder(), file);
            boolean canDownload = false;
            if (outputFile.exists()) {
                if (this.instagramUser.shouldOverwriteFiles()) {
                    this.logger.print(Logger.Type.WARNING, "%s does already exist, overwriting..", outputFile.getAbsolutePath());
                    canDownload = true;
                }
            } else {
                canDownload = true;
            }
            if (canDownload) {
                this.download(new FileOutputStream(outputFile), this.instagramUser.sendGetRequest(ent.getKey()).getInputStream());
            }
        }
    }

    private void download(FileOutputStream fos, InputStream is) throws IOException {
        byte[] buf = new byte[2048];
        int i;
        while ((i = is.read(buf)) != -1) {
            fos.write(buf, 0, i);
        }
        fos.close();
    }

    public InstagramUser.ContentType getContentType() {
        return this.contentType;
    }

    public Map<String, String> getUrls() {
        return this.urls;
    }

    public InstagramUser getInstagramUser() {
        return this.instagramUser;
    }
}
