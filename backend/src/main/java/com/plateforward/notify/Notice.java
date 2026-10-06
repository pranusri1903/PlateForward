package com.plateforward.notify;

import com.plateforward.user.User;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** An email to send once the surrounding transaction commits. {@code path} is a frontend route, e.g. /listings/7. */
public record Notice(String toEmail, String toName, String subject, String heading, String message, String path) {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("EEE MMM d, h:mm a").withZone(ZoneId.systemDefault());

    public Notice {
        subject = subject.replaceAll("\\p{Cntrl}", " "); // user-supplied titles must never inject mail headers
    }

    public static Notice to(User u, String subject, String heading, String message, String path) {
        return new Notice(u.getEmail(), u.getName(), subject, heading, message, path);
    }

    public static String time(Instant t) {
        return TIME.format(t);
    }
}
