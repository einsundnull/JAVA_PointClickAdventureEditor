package main.ui.theme;

import java.awt.Color;

/**
 * Color palette for the application.
 * Supports both Light and Dark themes.
 */
public class ColorPalette {

    // === PRIMARY COLORS ===
    private final Color primary;
    private final Color primaryHover;
    private final Color primaryActive;
    private final Color primaryForeground;

    // === SECONDARY COLORS ===
    private final Color secondary;
    private final Color secondaryHover;
    private final Color secondaryForeground;

    // === SEMANTIC COLORS ===
    private final Color success;
    private final Color successBackground;
    private final Color warning;
    private final Color warningBackground;
    private final Color danger;
    private final Color dangerBackground;
    private final Color info;
    private final Color infoBackground;

    // === BACKGROUND COLORS ===
    private final Color backgroundRoot;
    private final Color backgroundPanel;
    private final Color backgroundElevated;
    private final Color backgroundInput;
    private final Color backgroundHover;
    private final Color backgroundSelected;

    // === BORDER COLORS ===
    private final Color borderDefault;
    private final Color borderStrong;
    private final Color borderFocus;
    private final Color borderDisabled;

    // === TEXT COLORS ===
    private final Color textPrimary;
    private final Color textSecondary;
    private final Color textTertiary;
    private final Color textDisabled;
    private final Color textOnPrimary;

    // === SHADOW COLORS ===
    private final Color shadow;
    private final Color shadowStrong;

    /**
     * Light theme color palette.
     */
    public static ColorPalette light() {
        return new ColorPalette(
            // Primary (Blue)
            new Color(0x3B82F6),      // primary
            new Color(0x2563EB),      // primaryHover
            new Color(0x1D4ED8),      // primaryActive
            Color.WHITE,              // primaryForeground

            // Secondary (Indigo)
            new Color(0x6366F1),      // secondary
            new Color(0x4F46E5),      // secondaryHover
            Color.WHITE,              // secondaryForeground

            // Semantic
            new Color(0x10B981),      // success
            new Color(0xD1FAE5),      // successBackground
            new Color(0xF59E0B),      // warning
            new Color(0xFEF3C7),      // warningBackground
            new Color(0xEF4444),      // danger
            new Color(0xFEE2E2),      // dangerBackground
            new Color(0x06B6D4),      // info
            new Color(0xCFFAFE),      // infoBackground

            // Background (Slate)
            new Color(0xF8FAFC),      // backgroundRoot (Slate 50)
            Color.WHITE,              // backgroundPanel
            new Color(0xF1F5F9),      // backgroundElevated (Slate 100)
            Color.WHITE,              // backgroundInput
            new Color(0xF1F5F9),      // backgroundHover
            new Color(0xDBEAFE),      // backgroundSelected (Blue 50)

            // Border (Slate)
            new Color(0xE2E8F0),      // borderDefault (Slate 200)
            new Color(0xCBD5E1),      // borderStrong (Slate 300)
            new Color(0x3B82F6),      // borderFocus
            new Color(0xE2E8F0),      // borderDisabled

            // Text (Slate)
            new Color(0x0F172A),      // textPrimary (Slate 900)
            new Color(0x475569),      // textSecondary (Slate 600)
            new Color(0x94A3B8),      // textTertiary (Slate 400)
            new Color(0xCBD5E1),      // textDisabled (Slate 300)
            Color.WHITE,              // textOnPrimary

            // Shadow
            new Color(0, 0, 0, 20),    // shadow (rgba(0,0,0,0.08))
            new Color(0, 0, 0, 40)     // shadowStrong
        );
    }

    /**
     * Dark theme color palette.
     */
    public static ColorPalette dark() {
        return new ColorPalette(
            // Primary (Lighter Blue for dark mode)
            new Color(0x60A5FA),      // primary
            new Color(0x3B82F6),      // primaryHover
            new Color(0x2563EB),      // primaryActive
            new Color(0x0F172A),      // primaryForeground

            // Secondary
            new Color(0x818CF8),      // secondary
            new Color(0x6366F1),      // secondaryHover
            new Color(0x0F172A),      // secondaryForeground

            // Semantic
            new Color(0x34D399),      // success
            new Color(0x064E3B),      // successBackground
            new Color(0xFBBF24),      // warning
            new Color(0x78350F),      // warningBackground
            new Color(0xF87171),      // danger
            new Color(0x7F1D1D),      // dangerBackground
            new Color(0x22D3EE),      // info
            new Color(0x164E63),      // infoBackground

            // Background (Dark Slate)
            new Color(0x0F172A),      // backgroundRoot (Slate 900)
            new Color(0x1E293B),      // backgroundPanel (Slate 800)
            new Color(0x334155),      // backgroundElevated (Slate 700)
            new Color(0x1E293B),      // backgroundInput
            new Color(0x334155),      // backgroundHover
            new Color(0x1E3A5F),      // backgroundSelected

            // Border (Dark Slate)
            new Color(0x334155),      // borderDefault
            new Color(0x475569),      // borderStrong
            new Color(0x60A5FA),      // borderFocus
            new Color(0x1E293B),      // borderDisabled

            // Text (Light Slate)
            new Color(0xF1F5F9),      // textPrimary (Slate 100)
            new Color(0x94A3B8),      // textSecondary (Slate 400)
            new Color(0x64748B),      // textTertiary (Slate 500)
            new Color(0x475569),      // textDisabled
            new Color(0x0F172A),      // textOnPrimary

            // Shadow
            new Color(0, 0, 0, 40),
            new Color(0, 0, 0, 60)
        );
    }

    /**
     * Create a custom color palette.
     */
    public ColorPalette(Color primary, Color primaryHover, Color primaryActive, Color primaryForeground,
                       Color secondary, Color secondaryHover, Color secondaryForeground,
                       Color success, Color successBackground, Color warning, Color warningBackground,
                       Color danger, Color dangerBackground, Color info, Color infoBackground,
                       Color backgroundRoot, Color backgroundPanel, Color backgroundElevated, Color backgroundInput,
                       Color backgroundHover, Color backgroundSelected,
                       Color borderDefault, Color borderStrong, Color borderFocus, Color borderDisabled,
                       Color textPrimary, Color textSecondary, Color textTertiary, Color textDisabled, Color textOnPrimary,
                       Color shadow, Color shadowStrong) {
        this.primary = primary;
        this.primaryHover = primaryHover;
        this.primaryActive = primaryActive;
        this.primaryForeground = primaryForeground;
        this.secondary = secondary;
        this.secondaryHover = secondaryHover;
        this.secondaryForeground = secondaryForeground;
        this.success = success;
        this.successBackground = successBackground;
        this.warning = warning;
        this.warningBackground = warningBackground;
        this.danger = danger;
        this.dangerBackground = dangerBackground;
        this.info = info;
        this.infoBackground = infoBackground;
        this.backgroundRoot = backgroundRoot;
        this.backgroundPanel = backgroundPanel;
        this.backgroundElevated = backgroundElevated;
        this.backgroundInput = backgroundInput;
        this.backgroundHover = backgroundHover;
        this.backgroundSelected = backgroundSelected;
        this.borderDefault = borderDefault;
        this.borderStrong = borderStrong;
        this.borderFocus = borderFocus;
        this.borderDisabled = borderDisabled;
        this.textPrimary = textPrimary;
        this.textSecondary = textSecondary;
        this.textTertiary = textTertiary;
        this.textDisabled = textDisabled;
        this.textOnPrimary = textOnPrimary;
        this.shadow = shadow;
        this.shadowStrong = shadowStrong;
    }

    // === PRIMARY ===
    public Color getPrimary() { return primary; }
    public Color getPrimaryHover() { return primaryHover; }
    public Color getPrimaryActive() { return primaryActive; }
    public Color getPrimaryForeground() { return primaryForeground; }

    // === SECONDARY ===
    public Color getSecondary() { return secondary; }
    public Color getSecondaryHover() { return secondaryHover; }
    public Color getSecondaryForeground() { return secondaryForeground; }

    // === SEMANTIC ===
    public Color getSuccess() { return success; }
    public Color getSuccessBackground() { return successBackground; }
    public Color getWarning() { return warning; }
    public Color getWarningBackground() { return warningBackground; }
    public Color getDanger() { return danger; }
    public Color getDangerBackground() { return dangerBackground; }
    public Color getInfo() { return info; }
    public Color getInfoBackground() { return infoBackground; }

    // === BACKGROUND ===
    public Color getBackgroundRoot() { return backgroundRoot; }
    public Color getBackgroundPanel() { return backgroundPanel; }
    public Color getBackgroundElevated() { return backgroundElevated; }
    public Color getBackgroundInput() { return backgroundInput; }
    public Color getBackgroundHover() { return backgroundHover; }
    public Color getBackgroundSelected() { return backgroundSelected; }

    // === BORDER ===
    public Color getBorderDefault() { return borderDefault; }
    public Color getBorderStrong() { return borderStrong; }
    public Color getBorderFocus() { return borderFocus; }
    public Color getBorderDisabled() { return borderDisabled; }

    // === TEXT ===
    public Color getTextPrimary() { return textPrimary; }
    public Color getTextSecondary() { return textSecondary; }
    public Color getTextTertiary() { return textTertiary; }
    public Color getTextDisabled() { return textDisabled; }
    public Color getTextOnPrimary() { return textOnPrimary; }

    // === SHADOW ===
    public Color getShadow() { return shadow; }
    public Color getShadowStrong() { return shadowStrong; }
}
