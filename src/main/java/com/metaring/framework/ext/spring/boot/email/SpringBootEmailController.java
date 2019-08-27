package com.metaring.framework.ext.spring.boot.email;

import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import com.metaring.framework.SysKB;
import com.metaring.framework.email.EmailController;
import com.metaring.framework.email.EmailMessage;
import com.metaring.framework.email.EmailMessageSeries;
import com.metaring.framework.email.EmailTypeEnumerator;
import com.metaring.framework.exception.ManagedException;
import com.metaring.framework.ext.spring.boot.MetaRingSpringBootApplication;
import com.metaring.framework.functionality.UnmanagedException;
import com.metaring.framework.util.ObjectUtil;

public class SpringBootEmailController implements EmailController {

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10);

    private static JavaMailSender JAVA_MAIL_SENDER;

    @Override
    public void reinit(SysKB sysKB) {
        if (JAVA_MAIL_SENDER != null) {
            return;
        }
        JAVA_MAIL_SENDER = MetaRingSpringBootApplication.getBean(JavaMailSender.class);
    }

    @Override
    public void send(EmailMessageSeries emailMessageSeries) throws ManagedException, UnmanagedException {
        if (ObjectUtil.isNullOrEmpty(emailMessageSeries)) {
            return;
        }
        CompletableFuture.allOf(emailMessageSeries.stream().map(this::send).toArray(CompletableFuture[]::new));
    }

    private final CompletableFuture<Void> send(final EmailMessage emailMessage) {
        return CompletableFuture.runAsync(() -> {
            final MimeMessage message = JAVA_MAIL_SENDER.createMimeMessage();
            MimeMessageHelper helper;
            try {
                helper = new MimeMessageHelper(message, true);
                if (!ObjectUtil.isNullOrEmpty(emailMessage.getTos())) {
                    helper.setTo(
                            emailMessage.getTos().asEnumerable().select(it -> it.getMail().toString().replace("\"", ""))
                                    .toList().toArray(new String[emailMessage.getTos().size()]));
                }
                if (!ObjectUtil.isNullOrEmpty(emailMessage.getCcs())) {
                    helper.setCc(
                            emailMessage.getCcs().asEnumerable().select(it -> it.getMail().toString().replace("\"", ""))
                                    .toList().toArray(new String[emailMessage.getCcs().size()]));
                }
                if (!ObjectUtil.isNullOrEmpty(emailMessage.getBccs())) {
                    helper.setBcc(emailMessage.getBccs().asEnumerable()
                            .select(it -> it.getMail().toString().replace("\"", "")).toList()
                            .toArray(new String[emailMessage.getBccs().size()]));
                }
                helper.setText(emailMessage.getMessage(), emailMessage.getType() == EmailTypeEnumerator.HTML);
                helper.setSubject(emailMessage.getSubject());
                helper.setFrom(emailMessage.getFrom().getMail().toString().replace("\"", ""));
                helper.setSentDate(new Date());
                JAVA_MAIL_SENDER.send(message);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }, EXECUTOR_SERVICE);
    }
}