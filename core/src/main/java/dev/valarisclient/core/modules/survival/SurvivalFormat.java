package dev.valarisclient.core.modules.survival;

/** Shared formatting for survival HUD modules. */
final class SurvivalFormat {

    private SurvivalFormat() {
    }

    static String clock(long dayTime) {
        int hour = (int) ((dayTime / 1000L + 6) % 24);
        int minute = (int) ((dayTime % 1000L) * 60L / 1000L);
        return String.format("%02d:%02d", hour, minute);
    }

    static boolean isNight(long dayTime) {
        return dayTime >= 13_041 && dayTime < 23_141;
    }

    static String depthHint(int y) {
        if (y <= -59) {
            return " (diamond)";
        }
        if (y <= 16) {
            return " (cave)";
        }
        if (y >= 60) {
            return " (surface)";
        }
        return "";
    }
}
