package fe.igscraper.instagram.content.metadata;

import fe.igscraper.instagram.content.type.*;
import com.google.gson.*;
import fe.igscraper.instagram.util.*;
import java.io.*;

public class Metadata
{
    private final JsonObject root;
    
    public Metadata(InstagramContent instagramContent) {
        this.root = new JsonObject();
        this.root.add("identifier", instagramContent.getMetadataIdentifier());
    }
    
    public void addMetadata(JsonObject obj) {
        this.root.add("metadata", obj);
    }
    
    public void save(File file) throws IOException {
        Util.writeJson(file, this.root);
    }
}
