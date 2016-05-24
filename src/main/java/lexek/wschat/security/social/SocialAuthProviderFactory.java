package lexek.wschat.security.social;

import com.google.common.collect.ImmutableSet;
import lexek.wschat.security.SecureTokenGenerator;
import org.apache.http.client.HttpClient;

import java.util.Set;

public class SocialAuthProviderFactory {
    private final String baseUrl;
    private final HttpClient httpClient;
    private final SecureTokenGenerator secureTokenGenerator;

    public SocialAuthProviderFactory(String baseUrl, HttpClient httpClient, SecureTokenGenerator secureTokenGenerator) {
        this.baseUrl = baseUrl;
        this.httpClient = httpClient;
        this.secureTokenGenerator = secureTokenGenerator;
    }

    public SocialAuthProvider newProvider(String providerName, SocialAuthCredentials credentials, boolean signIn) {
        switch (providerName) {
            case "twitch":
                return new TwitchAuthProvider(
                    credentials.getClientId(),
                    credentials.getClientSecret(),
                    getUrl(providerName, signIn),
                    ImmutableSet.of("user_read", "chat_login"),
                    providerName,
                    httpClient,
                    secureTokenGenerator
                );
            case "twitter":
                return new TwitterSocialAuthProvider(
                    signIn,
                    false,
                    credentials.getClientId(),
                    credentials.getClientSecret(),
                    getUrl(providerName, signIn),
                    providerName,
                    httpClient
                );
            case "google": {
                Set<String> scopes;
                if (signIn) {
                    scopes = ImmutableSet.of(
                        "profile",
                        "email"
                    );
                } else {
                    scopes = ImmutableSet.of(
                        "https://www.googleapis.com/auth/youtube.readonly",
                        "profile",
                        "email"
                    );
                }
                return new GoogleSocialAuthProvider(
                    credentials.getClientId(),
                    credentials.getClientSecret(),
                    getUrl(providerName, signIn),
                    scopes,
                    providerName,
                    httpClient,
                    secureTokenGenerator
                );
            }
            case "vk":
                return new VkSocialAuthProvider(
                    credentials.getClientId(),
                    credentials.getClientSecret(),
                    getUrl(providerName, signIn),
                    providerName,
                    httpClient,
                    secureTokenGenerator
                );
            case "goodgame":
                return new GoodGameSocialAuthProvider(
                    credentials.getClientId(),
                    credentials.getClientSecret(),
                    getUrl(providerName, signIn),
                    providerName,
                    httpClient,
                    secureTokenGenerator
                );
            default:
                throw new IllegalArgumentException("Unsupported provider.");
        }
    }

    private String getUrl(String providerName, boolean signIn) {
        if (signIn) {
            return baseUrl + "/rest/auth/social/" + providerName;
        }
        return baseUrl + "/rest/proxy/auth/oauth/" + providerName;
    }
}
