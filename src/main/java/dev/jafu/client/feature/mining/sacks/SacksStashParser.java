package dev.jafu.client.feature.mining.sacks;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SacksStashParser {
    private static final Pattern SACKS_GAIN = Pattern.compile("^\\[Sacks]\\s+\\+([\\d,]+)\\s+items?\\.\\s+\\(Last\\s+([^)]+)\\.\\)$");
    private static final Pattern STASH_TOTAL = Pattern.compile("^You have\\s+([\\d,]+)\\s+materials stashed away!$");
    private static final Pattern STASH_TYPES = Pattern.compile("^\\(This totals\\s+([\\d,]+)\\s+types? of materials stashed!\\)$");

    private SacksStashParser() {
    }

    public static Optional<SacksStashGain> parseSacksGain(String message, long nowMillis) {
        Matcher matcher = SACKS_GAIN.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        long amount = parseLong(matcher.group(1));
        String windowLabel = matcher.group(2).trim();
        return Optional.of(new SacksStashGain(amount, windowLabel, nowMillis));
    }

    public static Optional<Long> parseStashTotal(String message) {
        Matcher matcher = STASH_TOTAL.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(parseLong(matcher.group(1)));
    }

    public static Optional<Integer> parseStashTypes(String message) {
        Matcher matcher = STASH_TYPES.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of((int) parseLong(matcher.group(1)));
    }

    public static String clean(String rawMessage) {
        return rawMessage
                .replaceAll("§.", "")
                .replaceAll("Â§.", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }

    private static long parseLong(String value) {
        return Long.parseLong(value.replace(",", ""));
    }
}
