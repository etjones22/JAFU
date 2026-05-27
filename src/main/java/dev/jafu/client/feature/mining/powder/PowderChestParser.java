package dev.jafu.client.feature.mining.powder;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PowderChestParser {
    private static final Pattern REWARD_PATTERN = Pattern.compile("^(.+?)(?:\\s+x([\\d,]+))?$");

    private PowderChestParser() {
    }

    public static boolean isChestStart(String message) {
        return message.contains("CHEST LOCKPICKED") || message.contains("LOOT CHEST COLLECTED");
    }

    public static boolean isRewardsHeader(String message) {
        return message.equals("REWARDS");
    }

    public static boolean isSeparator(String message) {
        return message.isBlank() || message.indexOf('\u25AC') >= 0;
    }

    public static Optional<PowderChestDrop> parseReward(String message) {
        Matcher matcher = REWARD_PATTERN.matcher(message);
        if (!matcher.matches()) {
            return Optional.empty();
        }

        String name = matcher.group(1).trim();
        if (name.isEmpty() || isChestStart(name) || isRewardsHeader(name)) {
            return Optional.empty();
        }

        String amountGroup = matcher.group(2);
        long amount = amountGroup == null ? 1 : Long.parseLong(amountGroup.replace(",", ""));
        return Optional.of(new PowderChestDrop(name, amount));
    }

    public static String clean(String rawMessage) {
        return rawMessage
                .replaceAll("§.", "")
                .replaceAll("[\\p{Cc}\\p{Cf}]", "")
                .trim();
    }
}
