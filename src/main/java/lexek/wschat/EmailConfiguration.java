package lexek.wschat;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EmailConfiguration {
    private final String smtpHost;
    private final int smtpPort;
    private final String email;
    private final String password;
    private final String prefix;
    private final String fromName;

    public EmailConfiguration(@JsonProperty("smtpHost") String smtpHost,
                              @JsonProperty("smtpPort") int smtpPort,
                              @JsonProperty("email") String email,
                              @JsonProperty("password") String password,
                              @JsonProperty("prefix") String prefix,
                              @JsonProperty("fromName") String fromName) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.email = email;
        this.password = password;
        this.prefix = prefix;
        this.fromName = fromName;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public int getSmtpPort() {
        return smtpPort;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getFromName() {
        return fromName;
    }
}
