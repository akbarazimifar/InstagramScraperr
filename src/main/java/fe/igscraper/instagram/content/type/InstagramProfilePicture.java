package fe.igscraper.instagram.content.type;

import fe.igscraper.instagram.*;
import fe.igscraper.instagram.util.*;
import com.google.gson.*;

public class InstagramProfilePicture extends InstagramContent {
    private String filename;

    public InstagramProfilePicture(String fileNameScheme, String url, String filename, InstagramUser owner, boolean metadata) {
        super(InstagramUser.ContentType.PROFILE_PICTURE, fileNameScheme, owner, metadata);

        this.filename = filename;
        this.addUrl(url, Util.fileNameScheme(this.fileNameScheme, this.contentType, owner, filename));
    }

    @Override
    public String toDatabaseString() {
        return this.filename;
    }

    @Override
    public JsonObject getMetadataIdentifier() {
        return null;
    }

    @Override
    public String getMetadataFilename() {
        return null;
    }
}
