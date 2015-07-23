package lexek.wschat.services;

import lexek.wschat.db.model.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class EmailService {
    private final Session session = Session.getInstance(System.getProperties(), null);
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final String smtpHost;
    private final int smtpPort;
    private final InternetAddress fromAddress;
    private final String password;
    private final String prefix;

    public EmailService(String smtpHost, int smtpPort, InternetAddress fromAddress, String password, String prefix) {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromAddress = fromAddress;
        this.password = password;
        this.prefix = prefix;
    }

    public void sendEmail(Email email) {
        MimeMessage message = new MimeMessage(session);

        try {
            message.setSubject(prefix + email.getSubject());
            message.setFrom(fromAddress);
            message.addRecipient(Message.RecipientType.TO, new InternetAddress(email.getTo()));

            Multipart multipart = new MimeMultipart("alternative");
            BodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(email.getBody());
            multipart.addBodyPart(messageBodyPart);

            message.setContent(multipart);
            Transport transport = session.getTransport("smtps");
            transport.connect(smtpHost, smtpPort, fromAddress.getAddress(), password);
            transport.sendMessage(message, message.getAllRecipients());
            transport.close();
        } catch (MessagingException e) {
            logger.error("exception while sending email", e);
        }

    }
}
