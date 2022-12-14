/**
 *
 */
package com.colaorange.dailymoney.core.data;

import com.colaorange.commons.util.Formats;
import com.colaorange.dailymoney.core.util.I18N;
import com.colaorange.dailymoney.core.R;


/**
 * @author dennis
 */
public enum SymbolPosition {

    NONE(0),
    FRONT(1),
    AFTER(2);

    int type;

    SymbolPosition(int type) {
        this.type = type;
    }

    public int getType() {
        return type;
    }


    public String getDisplay(I18N i18n) {
        return getDisplay(i18n, type);
    }


    static public SymbolPosition find(int type) {
        switch (type) {
            case 1:
                return FRONT;
            case 2:
                return AFTER;
            default:
                return NONE;
        }
    }

    static public String getDisplay(I18N i18n, int type) {
        SymbolPosition pos = find(type);
        switch (pos) {
            case FRONT:
                return i18n.string(R.string.label_position_front);
            case AFTER:
                return i18n.string(R.string.label_position_after);
            default:
                return i18n.string(R.string.label_position_none);
        }
    }

    static private final SymbolPosition[] available = new SymbolPosition[]{NONE, FRONT, AFTER};

    public static SymbolPosition[] getAvailable() {
        return available;
    }


    public static String money2String(Number money, String symbol, SymbolPosition pos) {
        StringBuilder sb = new StringBuilder();
        if (SymbolPosition.FRONT.equals(pos) && symbol != null) {
            sb.append(symbol);
        }
        sb.append(Formats.getMoneyFormat().format(money == null ? 0D : money));
        if (SymbolPosition.AFTER.equals(pos) && symbol != null) {
            sb.append(symbol);
        }
        return sb.toString();
    }

    public static String money2String(Number money, String symbol, SymbolPosition pos, int decmialLength) {
        StringBuilder sb = new StringBuilder();
        if (SymbolPosition.FRONT.equals(pos) && symbol != null) {
            sb.append(symbol);
        }
        sb.append(Formats.getFormat(true, decmialLength).format(money == null ? 0D : money));
        if (SymbolPosition.AFTER.equals(pos) && symbol != null) {
            sb.append(symbol);
        }
        return sb.toString();
    }
}
