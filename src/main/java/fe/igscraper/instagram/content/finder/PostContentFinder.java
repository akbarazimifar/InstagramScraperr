package fe.igscraper.instagram.content.finder;

import fe.igscraper.instagram.*;
import fe.igscraper.instagram.content.type.*;
import com.google.gson.*;

import java.util.*;
import java.io.*;

public class PostContentFinder extends ContentFinder {
    private static final String POST_URL = "https://www.instagram.com/graphql/query/?query_id=17888483320059182&variables={\"id\":\"%s\",\"first\":50,\"after\":%s}";

    public PostContentFinder() {
        super("%shortcode%_%type%_%owner%_%urlfile%");
    }

    @Override
    public List<InstagramContent> findContent(InstagramUser iu, List<String> ignore) throws IOException {
        List<InstagramContent> posts = new ArrayList<>();
        boolean hasNext = true;
        String nextToken = "null";
        while (hasNext) {
            JsonElement element = iu.readGetRequestJson(String.format(POST_URL, iu.getId(), nextToken));
            JsonObject edgeOwnerToTimelineMedia = element.getAsJsonObject().getAsJsonObject("data").getAsJsonObject("user").getAsJsonObject("edge_owner_to_timeline_media");
            JsonObject pageInfo = edgeOwnerToTimelineMedia.getAsJsonObject("page_info");
            for (JsonElement obj : edgeOwnerToTimelineMedia.getAsJsonArray("edges")) {
                String shortCode = obj.getAsJsonObject().getAsJsonObject("node").getAsJsonPrimitive("shortcode").getAsString();
                if (!ignore.contains(shortCode)) {
                    posts.add(new InstagramPost(this.fileNameScheme, shortCode, iu, this.metadataEnabled));
                }
            }
            hasNext = pageInfo.getAsJsonPrimitive("has_next_page").getAsBoolean();
            if (hasNext) {
                nextToken = String.format("\"%s\"", pageInfo.getAsJsonPrimitive("end_cursor").getAsString());
            }
        }
        return posts;
    }
}
