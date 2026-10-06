package com.plateforward.notify;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.HtmlUtils;

@Component
@RequiredArgsConstructor
@Slf4j
class Mailer {
    private static final String TEMPLATE = """
            <div style="background:#fafaf9;padding:32px 16px;font-family:-apple-system,Segoe UI,Roboto,sans-serif">
              <div style="max-width:480px;margin:auto;background:#fff;border-radius:24px;overflow:hidden;border:1px solid #e7e5e4">
                <div style="background:#059669;color:#fff;padding:20px 28px;font-size:18px;font-weight:700">🌿 PlateForward</div>
                <div style="padding:28px">
                  <h1 style="margin:0 0 12px;font-size:22px;color:#1c1917">{{heading}}</h1>
                  <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#57534e">{{message}}</p>
                  <a href="{{link}}" style="display:inline-block;background:#059669;color:#fff;text-decoration:none;font-weight:600;padding:12px 24px;border-radius:999px">Open in PlateForward</a>
                </div>
              </div>
            </div>""";

    private final ObjectProvider<JavaMailSender> sender;
    @Value("${app.mail-from}")
    private String from;
    @Value("${app.frontend-url}")
    private String baseUrl;

    /** Runs after the publishing transaction commits (or immediately when there is none), off the request thread. */
    @Async
    @TransactionalEventListener(fallbackExecution = true)
    void send(Notice n) {
        var mail = sender.getIfAvailable();
        if (mail == null) {
            log.info("📧 To: {} | {}\n{}\n{}{}", n.toEmail(), n.subject(), n.message(), baseUrl, n.path());
            return;
        }
        try {
            MimeMessage message = mail.createMimeMessage();
            var helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(n.toEmail());
            helper.setSubject(n.subject());
            helper.setText(TEMPLATE.replace("{{heading}}", HtmlUtils.htmlEscape(n.heading()))
                    .replace("{{message}}", HtmlUtils.htmlEscape(n.message()).replace("\n", "<br>"))
                    .replace("{{link}}", baseUrl + n.path()), true);
            mail.send(message);
        } catch (Exception e) {
            log.warn("Could not send '{}' to {}: {}", n.subject(), n.toEmail(), e.getMessage()); // a mail outage must never break the workflow
        }
    }
}
