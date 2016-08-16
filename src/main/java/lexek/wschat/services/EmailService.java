package lexek.wschat.services;

import lexek.wschat.db.model.Email;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.UnsupportedEncodingException;

@Service
public class EmailService {
    private final Session session = Session.getInstance(System.getProperties(), null);
    private final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private final String smtpHost;
    private final int smtpPort;
    private final InternetAddress fromAddress;
    private final String password;
    private final String prefix;

    @Inject
    public EmailService(
        @Named("email.server") String smtpHost,
        @Named("email.port") int smtpPort,
        @Named("email.from") String fromEmail,
        @Named("email.fromName") String fromName,
        @Named("email.password") String password,
        @Named("email.prefix") String prefix
    ) throws AddressException, UnsupportedEncodingException {
        this.smtpHost = smtpHost;
        this.smtpPort = smtpPort;
        this.fromAddress = new InternetAddress(fromEmail, fromName);
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
            MimeBodyPart messageBodyPart = new MimeBodyPart();
            messageBodyPart.setText(email.getBody(), "UTF-8");
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
