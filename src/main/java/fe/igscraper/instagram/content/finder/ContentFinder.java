package fe.igscraper.instagram.content.finder;

import fe.igscraper.instagram.*;
import java.util.*;
import fe.igscraper.instagram.content.type.*;
import java.io.*;

public abstract class ContentFinder
{
    protected String fileNameScheme;
    protected boolean metadataEnabled;
    
    public ContentFinder(String defaultFileNameScheme) {
        this.fileNameScheme = defaultFileNameScheme;
    }
    
    public abstract List<InstagramContent> findContent(InstagramUser p0, List<String> p1) throws IOException;
    
    public void enableMetadata() {
        this.metadataEnabled = true;
    }
    
    public void applyNamingScheme(final String fileNameScheme) {
        this.fileNameScheme = fileNameScheme;
    }
}
