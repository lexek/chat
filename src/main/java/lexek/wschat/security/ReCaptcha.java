package lexek.wschat.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;

public class ReCaptcha {
    private static final int TIMEOUT = 3000;
    private final String secret;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReCaptcha(String secret) {
        this.secret = secret;
    }

    public boolean verify(String response, String ip) {
        boolean result = false;
        try {
            URL tokenURL = new URL("https://www.google.com/recaptcha/api/siteverify?secret=" + secret + "&response=" + URLEncoder.encode(response, "utf-8") + "&remoteip=" + URLEncoder.encode(ip, "utf-8"));
            HttpURLConnection httpConnection = (HttpURLConnection) tokenURL.openConnection();
            httpConnection.setConnectTimeout(TIMEOUT);
            httpConnection.setReadTimeout(TIMEOUT);
            httpConnection.setRequestMethod("GET");
            httpConnection.setRequestProperty("Content-Length", "0");
            httpConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConnection.setDoOutput(true);
            httpConnection.setDoInput(true);
            httpConnection.connect();
            result = (boolean) objectMapper.readValue((InputStream) httpConnection.getContent(), Map.class).get("success");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}
