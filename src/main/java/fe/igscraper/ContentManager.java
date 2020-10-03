package fe.igscraper;

import fe.igscraper.sqlite.*;
import fe.logger.*;
import fe.igscraper.instagram.*;

import java.io.*;
import java.sql.*;
import java.util.*;

import fe.igscraper.instagram.content.type.*;

public class ContentManager {
    private SQLiteDatabase database;
    private Logger logger;
    private List<InstagramUser> users;

    public ContentManager(SQLiteDatabase database, List<InstagramUser> users) {
        this.logger = new Logger("ContentManager", true);
        this.database = database;
        this.users = users;
    }

    public void findContent() {
        for (InstagramUser user : this.users) {
            for (InstagramUser.ContentType contentType : user.getContentTypes()) {
                try {
                    List<String> databaseContent = contentType.queryDatabase(this.database, user);
                    this.logger.print(Logger.Type.INFO, "Loaded %-6d existing %s for user %s, searching for new %s..", databaseContent.size(), contentType.name(), user.getUsername(), contentType.name());
                    int size = contentType.findContent(user, databaseContent);
                    this.logger.print(Logger.Type.INFO, "Found  %-6d new %s for user %s", size, contentType.name(), user.getUsername());
                } catch (IOException | SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void downloadContent() throws InterruptedException {
        for (InstagramUser user : this.users) {
            for (int size = user.getContent().size(), i = 0; i < size; ++i) {
                InstagramContent content = user.getNewContent(i);
                try {
                    content.download();
                    int response = content.storeDatabase(this.database);
                    content.printLog(Logger.Type.INFO, String.format("Downloaded %s (%d/%d, db: %d)", content.getContentType(), i + 1, size, response));
                } catch (IOException | SQLException e) {
                    content.printLog(Logger.Type.ERROR, String.format("Failed download of %s (%d/%d): %s", content.getContentType(), i + 1, size, e.getMessage()));
                    e.printStackTrace();
                }

                Thread.sleep(2000);
            }
        }
    }
}
