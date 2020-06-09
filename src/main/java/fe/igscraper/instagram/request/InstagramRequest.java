package fe.igscraper.instagram.request;

import fe.request.Request;
import fe.request.data.Header;
import fe.request.proxy.AuthenticationProxy;

public class InstagramRequest extends Request {
    public InstagramRequest(AuthenticationProxy proxy, boolean defaultHeaders) {
        super(proxy, defaultHeaders);

        this.putHeaders(new Header("Host", "www.instagram.com"),
                new Header("Referer", "https://www.instagram.com/"),
                new Header("X-Instagram-AJAX", "1"),
                new Header("X-Requested-With", "XMLHttpRequest"));
    }
}
