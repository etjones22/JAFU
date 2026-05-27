package dev.jafu.client.feature.mining.sacks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;

import dev.jafu.client.module.JafuModules;

public final class SacksStashTracker {
    private static final int MAX_RECENT_GAINS = 4;

    private final Deque<SacksStashGain> recentGains = new ArrayDeque<>();
    private long sessionStartMillis;
    private long sessionTotal;
    private long stashTotal;
    private int stashTypes;

    public void acceptMessage(String rawMessage) {
        if (!JafuModules.isEnabled(JafuModules.SACKS_STASH_TRACKER)) {
            return;
        }

        String message = SacksStashParser.clean(rawMessage);
        if (message.isEmpty()) {
            return;
        }

        long nowMillis = System.currentTimeMillis();
        SacksStashParser.parseSacksGain(message, nowMillis).ifPresent(this::recordGain);
        SacksStashParser.parseStashTotal(message).ifPresent(total -> stashTotal = total);
        SacksStashParser.parseStashTypes(message).ifPresent(types -> stashTypes = types);
    }

    public SacksStashSnapshot snapshot() {
        long elapsedMillis = sessionStartMillis == 0L ? 0L : System.currentTimeMillis() - sessionStartMillis;
        return new SacksStashSnapshot(
                elapsedMillis,
                sessionTotal,
                stashTotal,
                stashTypes,
                new ArrayList<>(recentGains)
        );
    }

    private void recordGain(SacksStashGain gain) {
        if (sessionStartMillis == 0L) {
            sessionStartMillis = gain.receivedAtMillis();
        }

        sessionTotal += gain.amount();
        recentGains.addFirst(gain);
        while (recentGains.size() > MAX_RECENT_GAINS) {
            recentGains.removeLast();
        }
    }
}
