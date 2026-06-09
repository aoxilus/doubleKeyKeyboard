package com.blackberrykeyboard.prefs;

public final class KeyboardTheme {
    public final String id;
    public final String displayName;
    public final int background;
    public final int keyBackground;
    public final int keyPressed;
    public final int keyActive;
    public final int textPrimary;
    public final int textSecondary;
    public final int accent;
    public final int suggestionBackground;

    public KeyboardTheme(String id, String displayName,
                         int background, int keyBackground, int keyPressed, int keyActive,
                         int textPrimary, int textSecondary, int accent, int suggestionBackground) {
        this.id = id;
        this.displayName = displayName;
        this.background = background;
        this.keyBackground = keyBackground;
        this.keyPressed = keyPressed;
        this.keyActive = keyActive;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.accent = accent;
        this.suggestionBackground = suggestionBackground;
    }

    public static final KeyboardTheme PEARL = new KeyboardTheme(
            "pearl", "Pearl Burdeos",
            0xFF1E0A12, 0xFF5C2438, 0xFF7A3350, 0xFF8B4560,
            0xFFFFFFFF, 0xFFDDDDDD, 0xFF00A4BD, 0xFF2A1018);

    public static final KeyboardTheme CLASSIC = new KeyboardTheme(
            "classic", "Classic Negro",
            0xFF1A1A1A, 0xFF2D2D2D, 0xFF404040, 0xFF505050,
            0xFFF5F5F5, 0xFFAAAAAA, 0xFF00A4BD, 0xFF252525);

    public static final KeyboardTheme OCEAN = new KeyboardTheme(
            "ocean", "Ocean Azul",
            0xFF0A1628, 0xFF1A3A5C, 0xFF255078, 0xFF2E6090,
            0xFFFFFFFF, 0xFFB0C4DE, 0xFF4FC3F7, 0xFF0F2035);

    public static final KeyboardTheme[] ALL = {PEARL, CLASSIC, OCEAN};

    public static KeyboardTheme fromId(String id) {
        for (KeyboardTheme theme : ALL) {
            if (theme.id.equals(id)) {
                return theme;
            }
        }
        return PEARL;
    }
}
