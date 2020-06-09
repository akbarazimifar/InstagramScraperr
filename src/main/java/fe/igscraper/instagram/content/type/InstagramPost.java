package fe.igscraper.instagram.content.type;

import fe.igscraper.instagram.*;

import java.io.*;

import com.google.gson.*;

import java.util.*;

import fe.igscraper.instagram.util.*;

public class InstagramPost extends InstagramContent {
    private final String shortCode;
    public static final String POST_JSON = "https://www.instagram.com/p/%s?__a=1";

    public InstagramPost(String fileNameScheme, String shortCode, InstagramUser owner, boolean metadataEnabled) {
        super(InstagramUser.ContentType.POST, fileNameScheme, owner, metadataEnabled);
        this.shortCode = shortCode;
    }

    @Override
    public JsonObject getMetadataIdentifier() {
        JsonObject obj = new JsonObject();
        obj.addProperty("username", this.instagramUser.getUsername());
        obj.addProperty("shortcode", this.shortCode);
        return obj;
    }

    @Override
    public String getMetadataFilename() {
        return String.format("%s-%s.metdata.json", this.shortCode, this.instagramUser.getUsername());
    }

    @Override
    public void download() throws IOException {
        this.findDirectUrls();
        super.download();
    }

    @Override
    public String toDatabaseString() {
        return this.shortCode;
    }

    private void findDirectUrls() throws IOException {
        JsonElement element = this.instagramUser.readGetRequestJson(String.format(POST_JSON, this.shortCode));
        JsonObject shortCodeMedia = element.getAsJsonObject().getAsJsonObject("graphql").getAsJsonObject("shortcode_media");
        if (this.metadataEnabled) {
            JsonObject obj = new JsonObject();
            obj.add("id", shortCodeMedia.getAsJsonPrimitive("id"));
            obj.add("accessibility_caption", shortCodeMedia.get("accessibility_caption"));
            obj.add("tracking_token", shortCodeMedia.get("tracking_token"));
            obj.add("caption_available", shortCodeMedia.get("caption_is_edited"));
            obj.add("caption", shortCodeMedia.get("edge_media_to_caption"));
            obj.add("posted_at_timestamp", shortCodeMedia.get("taken_at_timestamp"));
            obj.add("location", shortCodeMedia.get("location"));
            this.addMetadata(obj);
        }
        String typeName = shortCodeMedia.getAsJsonPrimitive("__typename").getAsString();
        int counter = 1;
        if (!typeName.equals("GraphSidecar")) {
            this.addUrl(this.getImageVideo(shortCodeMedia, typeName), counter);
        } else {
            for (JsonElement edge : shortCodeMedia.getAsJsonObject("edge_sidecar_to_children").getAsJsonArray("edges")) {
                JsonObject node = edge.getAsJsonObject().getAsJsonObject("node");
                this.addUrl(this.getImageVideo(node, node.getAsJsonPrimitive("__typename").getAsString()), counter++);
            }
        }
    }

    private void addUrl(String url, int counter) {
        this.addUrl(url, Util.fileUrlNameScheme(
                this.fileNameScheme, this.contentType, this.instagramUser, url)
                .replace("%shortcode%", this.shortCode)
                .replace("%media_counter%", String.valueOf(counter)));
    }

    private String getImageVideo(JsonObject parent, String typename) {
        return parent.getAsJsonPrimitive(typename.equals("GraphImage") ? "display_url" : "video_url").getAsString();
    }

    public String getShortCode() {
        return this.shortCode;
    }
}
