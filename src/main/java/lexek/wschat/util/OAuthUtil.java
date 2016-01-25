package lexek.wschat.util;

import com.google.common.io.BaseEncoding;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.ThreadLocalRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class OAuthUtil {
    public static String generateAuthorizationHeader(
        String consumerKey,
        String consumerSecret,
        String accessToken,
        String accessTokenSecret,
        String url,
        HttpMethod method,
        Map<String, String> query
    ) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
        String nonce = genNonce();
        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.put("oauth_consumer_key", consumerKey);
        parameters.put("oauth_nonce", nonce);
        parameters.put("oauth_signature_method", "HMAC-SHA1");
        parameters.put("oauth_timestamp", timestamp);
        parameters.put("oauth_token", accessToken);
        parameters.put("oauth_version", "1.0");
        String signature = signature(parameters, query, method, url, consumerSecret, accessTokenSecret);
        parameters.put("oauth_signature", signature);
        return oauthHeader(parameters);
    }

    private static String genNonce() {
        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        return BaseEncoding.base16().encode(bytes).replaceAll("[^a-zA-Z]+", "");
    }

    private static String collectParameters(Map<String, String> parameters) {
        return parameters
            .entrySet()
            .stream()
            .map(entry -> encode(entry.getKey()) + '=' + encode(entry.getValue()))
            .collect(Collectors.joining("&"));
    }

    private static String signature(
        Map<String, String> oauthParameters,
        Map<String, String> requestParameters,
        HttpMethod method,
        String url,
        String consumerSecret,
        String accessTokenSecret
    ) throws UnsupportedEncodingException, NoSuchAlgorithmException, InvalidKeyException {
        TreeMap<String, String> parameters = new TreeMap<>();
        parameters.putAll(oauthParameters);
        parameters.putAll(requestParameters);
        String base = method.toString() + '&' + encode(url) + '&' + encode(collectParameters(parameters));
        String signingKey = consumerSecret + '&' + accessTokenSecret;
        SecretKeySpec keySpec = new SecretKeySpec(
            signingKey.getBytes(),
            "HmacSHA1"
        );
        Mac mac = Mac.getInstance("HmacSHA1");
        mac.init(keySpec);
        byte[] result = mac.doFinal(base.getBytes());
        return BaseEncoding.base64().encode(result);
    }

    private static String oauthHeader(Map<String, String> parameters) {
        return "OAuth " + parameters
            .entrySet()
            .stream()
            .map(entry -> encode(entry.getKey()) + "=\"" + encode(entry.getValue()) + "\"")
            .collect(Collectors.joining(", "));
    }

    private static String encode(String string) {
        try {
            return URLEncoder.encode(string, "UTF-8").replaceAll("\\+", "%20");
        } catch (UnsupportedEncodingException e) {
            return null;
        }
    }
}
