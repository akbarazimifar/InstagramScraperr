package fe.igscraper.instagram.content.type;

import fe.igscraper.instagram.*;
import com.google.gson.*;
import fe.igscraper.instagram.util.*;

public class InstagramStory extends InstagramContent {
    private final String filename;
    private final String storyId;

    public InstagramStory(String fileNameScheme, String storyId, String url, String filename, InstagramUser owner, boolean metadata, JsonObject metadataObj) {
        super(InstagramUser.ContentType.STORY, fileNameScheme, owner, metadata);

        this.storyId = storyId;
        this.filename = filename;
        this.addUrl(url, Util.fileNameScheme(this.fileNameScheme, this.contentType, owner, filename).replace("%story_id%", this.storyId));
        if (metadata) {
            this.addMetadata(metadataObj);
        }
    }

    @Override
    public JsonObject getMetadataIdentifier() {
        JsonObject obj = new JsonObject();
        obj.addProperty("username", this.instagramUser.getUsername());
        obj.addProperty("story_id", this.storyId);
        return obj;
    }

    @Override
    public String getMetadataFilename() {
        return String.format("story-%s-%s.metadata.json", this.storyId, this.instagramUser.getUsername());
    }

    @Override
    public String toDatabaseString() {
        return this.filename;
    }
}
