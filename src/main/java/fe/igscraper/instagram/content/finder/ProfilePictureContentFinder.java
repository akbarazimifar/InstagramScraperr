package fe.igscraper.instagram.content.finder;

import fe.igscraper.ConfigLoader;
import fe.igscraper.instagram.*;
import java.util.*;
import fe.igscraper.instagram.util.*;
import fe.igscraper.instagram.content.type.*;
import com.google.gson.*;
import java.io.*;

public class ProfilePictureContentFinder extends ContentFinder
{
    private static final String PROFILE_PIC_URL = "https://i.instagram.com/api/v1/users/%s/info/";

    public ProfilePictureContentFinder() {
        super("%type%_%owner%_%urlfile%");
    }
    
    @Override
    public List<InstagramContent> findContent( InstagramUser iu,  List<String> ignore) throws IOException {
         List<InstagramContent> profilePictures = new ArrayList<>();
         JsonElement element = iu.readGetRequestJson(String.format(PROFILE_PIC_URL, iu.getId()), ConfigLoader.CHECK_ACCOUNT_USERAGENT);
         JsonObject hdPb = element.getAsJsonObject().getAsJsonObject("user").getAsJsonObject("hd_profile_pic_url_info");
         String url = hdPb.getAsJsonPrimitive("url").getAsString();
         String filename = Util.urlToFileName(url);
        if (!ignore.contains(filename)) {
            profilePictures.add(new InstagramProfilePicture(this.fileNameScheme, url, filename, iu, this.metadataEnabled));
        }
        return profilePictures;
    }
}
