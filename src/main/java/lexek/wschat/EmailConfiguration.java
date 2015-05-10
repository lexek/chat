package lexek.wschat;

public class EmailConfiguration {
    private final String smtpHost;
    private final int smtpPort;
    private final String email;
    private final String password;

    public EmailConfiguration(String smtpHost, int smtpPort, String email, String password) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.email = email;
        this.password = password;
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
}
