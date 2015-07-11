package lexek.wschat.db.model;

import javax.mail.internet.InternetAddress;

public class Email {
    private final InternetAddress to;
    private final String subject;
    private final String body;

    public Email(InternetAddress to, String subject, String body) {
        this.to = to;
        this.subject = subject;
        this.body = body;
    }

    public InternetAddress getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getBody() {
        return body;
    }
}
