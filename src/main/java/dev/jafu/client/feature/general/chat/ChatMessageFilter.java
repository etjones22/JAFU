package dev.jafu.client.feature.general.chat;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public final class ChatMessageFilter {
    private static final long REPEATED_WINDOW_MILLIS = 15_000L;
    private static final int MAX_RECENT_MESSAGES = 160;
    private static final Pattern LOBBY_JOIN_LEAVE = Pattern.compile("(?i).*\\b(joined|left) the lobby!$");
    private static final Pattern SERVER_TRANSFER = Pattern.compile("(?i)^sending to server\\s+[a-z0-9_-]+\\.\\.\\.$");
    private static final Pattern DIVIDER_ONLY = Pattern.compile("^[\\s\\-_=*~▬■□]+$");
    private static final Pattern RAW_LOCATION_JSON = Pattern.compile("^\\{\\s*\"server\"\\s*:\\s*\"[^\"]+\".*}$");

    private static final Map<String, Long> RECENT_MESSAGES = new LinkedHashMap<>();

    private ChatMessageFilter() {
    }

    public static synchronized boolean shouldHide(String rawMessage) {
        String message = clean(rawMessage);
        long nowMillis = System.currentTimeMillis();
        prune(nowMillis);

        if (ChatEnhancementsSettings.INSTANCE.spamFilterEnabled() && isSpam(message)) {
            return true;
        }

        if (!ChatEnhancementsSettings.INSTANCE.repeatedMessageFilterEnabled()) {
            return false;
        }

        String key = normalize(message);
        if (key.isEmpty()) {
            return false;
        }

        Long previousMillis = RECENT_MESSAGES.put(key, nowMillis);
        return previousMillis != null && nowMillis - previousMillis <= REPEATED_WINDOW_MILLIS;
    }

    private static boolean isSpam(String message) {
        if (message.isBlank()) {
            return true;
        }
        if (message.length() >= 12 && DIVIDER_ONLY.matcher(message).matches()) {
            return true;
        }
        return LOBBY_JOIN_LEAVE.matcher(message).matches()
                || SERVER_TRANSFER.matcher(message).matches()
                || RAW_LOCATION_JSON.matcher(message).matches();
    }

    private static void prune(long nowMillis) {
        Iterator<Map.Entry<String, Long>> iterator = RECENT_MESSAGES.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            if (nowMillis - entry.getValue() > REPEATED_WINDOW_MILLIS || RECENT_MESSAGES.size() > MAX_RECENT_MESSAGES) {
                iterator.remove();
            }
        }
    }

    private static String clean(String rawMessage) {
        return rawMessage == null ? "" : rawMessage
                .replaceAll("Â§.", "")
                .replaceAll("Ã‚Â§.", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }

    private static String normalize(String message) {
        return clean(message).toLowerCase().replaceAll("\\s+", " ");
    }
}
