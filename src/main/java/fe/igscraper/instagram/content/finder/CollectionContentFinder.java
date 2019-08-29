package fe.igscraper.instagram.content.finder;

import fe.igscraper.instagram.*;
import fe.igscraper.instagram.util.*;
import fe.igscraper.instagram.content.type.*;
import com.google.gson.*;

import java.io.*;
import java.util.*;

public class CollectionContentFinder extends ContentFinder {
    private static final String COLLECTION_OVERVIEW_URL = "https://www.instagram.com/graphql/query/?query_hash=7c16654f22c819fb63d1183034a5162f&variables={\"user_id\":\"%s\",\"include_chaining\":false,\"include_reel\":false,\"include_suggested_users\":false,\"include_logged_out_extras\":false,\"include_highlight_reels\":true}";
    private static final String COLLECTION_SOURCE_URL = "https://www.instagram.com/graphql/query/?query_hash=712a7914241d7e01719fb760a810fbfc&variables={\"reel_ids\":[],\"tag_names\":[],\"location_ids\":[],\"highlight_reel_ids\":[%s],\"precomposed_overlay\":false,\"show_story_viewer_list\":true,\"story_viewer_fetch_count\":50,\"story_viewer_cursor\":\"\"}";

    public CollectionContentFinder() {
        super("%collection_name%_%type%_%owner%_%urlfile%");
    }

    @Override
    public List<InstagramContent> findContent(InstagramUser iu, List<String> ignore) throws IOException {
        Map<String, String> collectionIds = new HashMap<>();
        List<InstagramContent> collections = new ArrayList<>();
        JsonElement element = iu.readGetRequestJson(String.format(COLLECTION_OVERVIEW_URL, iu.getId()));
        JsonArray edges = element.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("user").getAsJsonObject("edge_highlight_reels").getAsJsonArray("edges");
        for (JsonElement obj : edges) {
            JsonObject node = obj.getAsJsonObject().getAsJsonObject("node");
            collectionIds.put(node.getAsJsonPrimitive("title").getAsString(), node.getAsJsonPrimitive("id").getAsString());
        }
        JsonElement resourceElement = iu.readGetRequestJson(String.format(COLLECTION_SOURCE_URL, this.toIdString(collectionIds.values())));
        JsonArray reelsMedia = resourceElement.getAsJsonObject().getAsJsonObject("data").getAsJsonArray("reels_media");
        int count = 0;
        for (String collectionName : collectionIds.keySet()) {
            JsonArray items = reelsMedia.get(count++).getAsJsonObject().getAsJsonArray("items");
            for (JsonElement item : items) {
                String url = null;
                if (item.getAsJsonObject().getAsJsonPrimitive("__typename").getAsString().equals("GraphStoryVideo")) {
                    JsonArray arr = item.getAsJsonObject().getAsJsonArray("video_resources");
                    url = arr.get(arr.size() - 1).getAsJsonObject().getAsJsonPrimitive("src").getAsString();
                } else {
                    url = item.getAsJsonObject().getAsJsonPrimitive("display_url").getAsString();
                }
                String filename = Util.urlToFileName(url);
                if (!ignore.contains(filename)) {
                    collections.add(new InstagramCollection(this.fileNameScheme, collectionName, url, filename, iu, this.metadataEnabled));
                }
            }
        }
        return collections;
    }

    private String toIdString(Collection<String> collection) {
        StringBuilder sb = new StringBuilder();
        int count = 0;
        for (String c : collection) {
            sb.append(String.format("\"%s\"", c));
            if (count++ < collection.size() - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }
}
