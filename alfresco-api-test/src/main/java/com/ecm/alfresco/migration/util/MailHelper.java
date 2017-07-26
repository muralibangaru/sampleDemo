package com.ecm.alfresco.migration.util;

import org.apache.log4j.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.File;
import java.util.List;
import java.util.Properties;

/**
 * @author  Miguel Sanchez - TCS
 * @version 1.0
 *          2016-04-04
 */
public class MailHelper {
    private static final Logger logger = Logger.getLogger(MailHelper.class);

    /**
     * Sends email
     * @param to recipient
     * @param from sender
     * @param host smtp server
     * @param subject email subject
     * @param body email body
     */
    public static void send(String to, String from, String host, String subject, String body) {
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);

        try {
            logger.debug("body: " + body + ", subject: " + subject);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setContent(body, "text/html");
            Transport.send(message);
            logger.info("Notification Sent to " + to);

        } catch (MessagingException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Sends email
     * @param to recipient
     * @param from sender
     * @param host smtp server
     * @param subject email subject
     * @param body email body
     * @param fileList attachment file list
     */
    public static void sendWithAttachment(String to, String from, String host, String subject, String body, List<File> fileList) {
        Properties properties = System.getProperties();
        properties.setProperty("mail.smtp.host", host);
        Session session = Session.getDefaultInstance(properties);

        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(from));
            message.addRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);

            BodyPart bodyPart = new MimeBodyPart();
            bodyPart.setContent(body, "text/html");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(bodyPart);

/*            if(fileList != null) {
                for (File oneFile : fileList) {
                    attachFile(oneFile, multipart);
                }
            }*/

            message.setContent(multipart);
            Transport.send(message);
            logger.info("Notification Sent to " + to);

        } catch (MessagingException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Attaches a file
     * @param file
     * @param multipart
     * @throws MessagingException
     */
    public static void attachFile(File file, Multipart multipart) throws MessagingException {
        DataSource source = new FileDataSource(file);
        MimeBodyPart messagePart = new MimeBodyPart();
        messagePart.setDataHandler(new DataHandler(source));
        messagePart.setFileName(file.getName());
        multipart.addBodyPart(messagePart);
    }
}
