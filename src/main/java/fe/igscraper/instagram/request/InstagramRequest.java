package fe.igscraper.instagram.request;

import fe.request.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

public class InstagramRequest extends Request {

    public InstagramRequest(AuthenticationProxy proxy, boolean defaultHeaders) {
        super(proxy, defaultHeaders);

        this.putHeader("Host", "www.instagram.com");
        this.putHeader("Referer", "https://www.instagram.com/");
        this.putHeader("X-Instagram-AJAX", "1");
        this.putHeader("X-Requested-With", "XMLHttpRequest");
    }

    public HttpURLConnection sendGetRequest(String url, String[][] tempHeaderOverride) throws IOException {
        Map<String, String> map = new HashMap<>();
        for(String[] arr : tempHeaderOverride){
            if(this.getHeaders().containsKey(arr[0])){
                map.put(arr[0], this.getHeaders().get(arr[0]));
                this.putHeader(arr[0], arr[1]);
            }
        }

        HttpURLConnection con = this.sendGetRequest(url);
        for(Map.Entry<String, String> ent : map.entrySet()){
            this.putHeader(ent.getKey(), ent.getValue());
        }

        return con;
    }
}
