package fe.igscraper.instagram.request;

import fe.request.*;

public class InstagramRequest extends Request
{
    public InstagramRequest(final AuthenticationProxy proxy, final boolean defaultHeaders) {
        super(proxy, defaultHeaders);
        this.putHeader("Host", "www.instagram.com");
        this.putHeader("Referer", "https://www.instagram.com/");
        this.putHeader("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:64.0) Gecko/20100101 Firefox/64.0");
        this.putHeader("X-Instagram-AJAX", "1");
        this.putHeader("X-Requested-With", "XMLHttpRequest");
    }
}
