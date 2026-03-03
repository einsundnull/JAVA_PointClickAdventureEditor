package main.ui.theme;

/**
 * Spacing system based on a 4px base unit.
 * Ensures consistent spacing throughout the application.
 */
public class Spacing {

    /** Base unit: 4px */
    public static final int UNIT = 4;

    /** 0px */
    public static final int NONE = 0;

    /** 4px - xs: Minimal spacing */
    public static final int XS = UNIT;

    /** 8px - sm: Small spacing */
    public static final int SM = UNIT * 2;

    /** 12px - md: Medium spacing */
    public static final int MD = UNIT * 3;

    /** 16px - lg: Large spacing */
    public static final int LG = UNIT * 4;

    /** 20px */
    public static final int XL = UNIT * 5;

    /** 24px - xl: Extra large spacing */
    public static final int XXL = UNIT * 6;

    /** 32px */
    public static final int XXXL = UNIT * 8;

    /** 40px */
    public static final int HUGE = UNIT * 10;

    /** 48px */
    public static final int MASSIVE = UNIT * 12;

    // === COMMON COMPONENT SPACING ===

    /** Button padding: 8px vertical, 16px horizontal */
    public static final int BUTTON_PADDING_V = SM;
    public static final int BUTTON_PADDING_H = LG;

    /** Button height: 32px */
    public static final int BUTTON_HEIGHT = XXXL;

    /** Small button height: 28px */
    public static final int BUTTON_HEIGHT_SM = XL;

    /** Large button height: 36px */
    public static final int BUTTON_HEIGHT_LG = XXL + MD;

    /** Input field padding: 6px vertical, 12px horizontal */
    public static final int INPUT_PADDING_V = XS + SM;
    public static final int INPUT_PADDING_H = MD;

    /** Input field height: 32px */
    public static final int INPUT_HEIGHT = XXXL;

    /** Panel/card padding: 16px */
    public static final int PANEL_PADDING = LG;

    /** Section spacing: 20px */
    public static final int SECTION_SPACING = XL;

    /** Tree row height: 36px */
    public static final int TREE_ROW_HEIGHT = XXL;

    /** Table row height: 32px */
    public static final int TABLE_ROW_HEIGHT = XXXL;

    /** Dialog padding: 24px */
    public static final int DIALOG_PADDING = XXL;

    /** Icon sizes */
    public static final int ICON_SM = 16;
    public static final int ICON_MD = 20;
    public static final int ICON_LG = 24;
    public static final int ICON_XL = 32;

    // === BORDER RADIUS ===

    /** Small radius: 4px - Input fields, small buttons */
    public static final int RADIUS_SM = UNIT;

    /** Medium radius: 6px - Default buttons, cards */
    public static final int RADIUS_MD = XS + SM;

    /** Large radius: 8px - Panels, large cards */
    public static final int RADIUS_LG = SM * 2;

    /** Extra large radius: 12px - Dialogs */
    public static final int RADIUS_XL = MD;

    // === SHADOWS ===

    /** Small shadow: 0 1px 2px rgba(0,0,0,0.05) */
    public static final String SHADOW_SM = "0 1px 2px rgba(0,0,0,0.05)";

    /** Medium shadow: 0 4px 6px rgba(0,0,0,0.07) */
    public static final String SHADOW_MD = "0 4px 6px rgba(0,0,0,0.07)";

    /** Large shadow: 0 10px 15px rgba(0,0,0,0.1) */
    public static final String SHADOW_LG = "0 10px 15px rgba(0,0,0,0.1)";

    // Prevent instantiation
    private Spacing() {}
}
