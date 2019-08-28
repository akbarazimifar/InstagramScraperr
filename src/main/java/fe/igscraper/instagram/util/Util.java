package fe.igscraper.instagram.util;

import fe.igscraper.instagram.*;
import com.google.gson.*;
import java.io.*;

public class Util
{
    public static String urlToFileName(final String url) {
        return removeQuestionMark(url.substring(url.lastIndexOf("/") + 1));
    }
    
    public static String removeQuestionMark(final String string) {
        if (string.contains("?")) {
            return string.substring(0, string.indexOf("?"));
        }
        return string;
    }
    
    public static String fileUrlNameScheme(final String scheme, final InstagramUser.ContentType ct, final InstagramUser iu, final String url) {
        return fileNameScheme(scheme, ct, iu, urlToFileName(url));
    }
    
    public static String fileNameScheme(final String scheme, final InstagramUser.ContentType ct, final InstagramUser iu, final String fileName) {
        return scheme.replace("%type%", ct.name().toLowerCase()).replace("%owner%", iu.getUsername()).replace("%urlfile%", fileName);
    }
    
    public static void writeJson(final File output, final JsonElement jsonElem) throws IOException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        BufferedWriter bw = new BufferedWriter(new FileWriter(output));
        bw.write(gson.toJson(jsonElem));
        bw.close();
    }
}
