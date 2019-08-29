package fe.igscraper.instagram.content.finder;

import fe.igscraper.instagram.*;

import java.util.*;

import fe.igscraper.instagram.util.*;
import fe.igscraper.instagram.content.type.*;
import com.google.gson.*;

import java.io.*;

public class StoryContentFinder extends ContentFinder {
    private static final String STORY_URL = "https://www.instagram.com/graphql/query/?query_hash=45246d3fe16ccc6577e0bd297a5db1ab&variables={\"reel_ids\":[\"%s\"],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[],\"precomposed_overlay\":false}";

    public StoryContentFinder() {
        super("%type%_%owner%_%urlfile%");
    }

    @Override
    public List<InstagramContent> findContent(InstagramUser iu, List<String> ignore) throws IOException {
        List<InstagramContent> stories = new ArrayList<InstagramContent>();
        JsonObject metadataObj = new JsonObject();
        JsonElement element = iu.readGetRequestJson(String.format(STORY_URL, iu.getId()));
        JsonArray reelsMedia = element.getAsJsonObject().getAsJsonObject("data").getAsJsonArray("reels_media");
        if (reelsMedia.size() > 0) {
            JsonObject reelsMedia2 = reelsMedia.get(0).getAsJsonObject();
            JsonArray items = reelsMedia2.getAsJsonObject().getAsJsonArray("items");
            if (this.metadataEnabled) {
                metadataObj.add("latest_media", reelsMedia2.get("latest_reel_media"));
                metadataObj.add("expiring_at_timestamp", reelsMedia2.get("expiring_at"));
                metadataObj.add("can_reshare", reelsMedia2.get("can_reshare"));
                metadataObj.add("seen", reelsMedia2.get("seen"));
                metadataObj.addProperty("story_size", items.size());
            }
            for (int i = items.size() - 1; i >= 0; --i) {
                JsonObject item = (JsonObject) items.get(i);
                if (this.metadataEnabled) {
                    JsonObject metadataStory = new JsonObject();
                    metadataStory.add("id", item.get("id"));
                    metadataStory.add("type", item.get("__typename"));
                    metadataStory.add("taken_at_timestamp", item.get("taken_at_timestamp"));
                    metadataStory.add("expiring_at_timestamp", item.get("expiring_at_timestamp"));
                    metadataStory.add("tracking_token", item.get("tracking_token"));
                    metadataStory.add("tappable", item.get("tappable_objects"));
                    metadataObj.add("story", metadataStory);
                }
                String storyId = item.getAsJsonPrimitive("id").getAsString();
                String url = null;
                if (item.getAsJsonPrimitive("__typename").getAsString().equals("GraphStoryVideo")) {
                    JsonArray arr = item.getAsJsonObject().getAsJsonArray("video_resources");
                    url = arr.get(arr.size() - 1).getAsJsonObject().getAsJsonPrimitive("src").getAsString();
                } else {
                    url = item.getAsJsonPrimitive("display_url").getAsString();
                }
                String filename = Util.urlToFileName(url);
                if (!ignore.contains(filename)) {
                    stories.add(new InstagramStory(this.fileNameScheme, storyId, url, filename, iu, this.metadataEnabled, metadataObj));
                }
            }
        }
        return stories;
    }
}
