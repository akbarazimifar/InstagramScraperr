package fe.igscraper.instagram.content.type;

import fe.igscraper.instagram.*;
import fe.igscraper.instagram.util.*;
import com.google.gson.*;

public class InstagramCollection extends InstagramContent {
    private String collectionName;
    private String filename;

    public InstagramCollection(String fileNameScheme, String collectionName, String url, String filename, InstagramUser owner, boolean metadata) {
        super(InstagramUser.ContentType.COLLECTION, fileNameScheme, owner, metadata);

        this.collectionName = collectionName;
        this.filename = filename;
        this.addUrl(url, Util.fileNameScheme(this.fileNameScheme, this.contentType, owner, filename).replace("%collection_name%", this.collectionName));
    }

    @Override
    public JsonObject getMetadataIdentifier() {
        return null;
    }

    @Override
    public String toDatabaseString() {
        return this.filename;
    }

    @Override
    public String getMetadataFilename() {
        return null;
    }
}
