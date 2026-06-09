package com.blackberrykeyboard.ime;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import com.blackberrykeyboard.prefs.KeyboardPreferences;
import com.blackberrykeyboard.prefs.KeyboardTheme;

public class BlackBerryKeyboardView extends View {

    public interface Listener {
        void onLetter(char letter);
        void onSureTypeKey(char first, char second);
        void onTwoTypeFirst(char letter);
        void onTwoTypeSecond(char letter);
        void onSymbol(String text);
        void onBackspace();
        void onSpace();
        void onEnter();
        void onShift();
        void onToggleSym();
        void onToggleAlt();
        void onSuggestionSelected(int index);
    }

    private static final int SUGGESTION_HEIGHT_DP = 58;
    private static final int FLOATING_SUGGESTIONS = 3;
    private static final int DOUBLE_TAP_MS = 450;
    private static final int KEY_HEIGHT_DP = 58;
    private static final int ROW_GAP_DP = 4;
    private static final int KEY_GAP_DP = 5;
    private static final int COLS = 5;
    private static final float GRID_WIDTH_RATIO = 0.94f;
    private static final float TOUCH_PADDING_DP = 4f;

    private static final class KeyDef {
        final String letter1;
        final String letter2;
        final String altLabel;
        final int row;
        final int col;
        final KeyType type;

        KeyDef(String letter1, String letter2, String altLabel, int row, int col, KeyType type) {
            this.letter1 = letter1;
            this.letter2 = letter2;
            this.altLabel = altLabel;
            this.row = row;
            this.col = col;
            this.type = type;
        }

        KeyDef(String label, String altLabel, int row, int col, KeyType type) {
            this(label, null, altLabel, row, col, type);
        }
    }

    private enum KeyType { LETTER, SHIFT, BACKSPACE, SPACE, ENTER, ALT, SYM }

    // Pearl SureType: 5×4 grid simétrico, todas las teclas mismo ancho
    private static final KeyDef[] LAYOUT = {
            new KeyDef("q", "w", "!", 0, 0, KeyType.LETTER),
            new KeyDef("e", "r", "1", 0, 1, KeyType.LETTER),
            new KeyDef("t", "y", "2", 0, 2, KeyType.LETTER),
            new KeyDef("u", "i", "3", 0, 3, KeyType.LETTER),
            new KeyDef("o", "p", "#", 0, 4, KeyType.LETTER),

            new KeyDef("a", "s", "?", 1, 0, KeyType.LETTER),
            new KeyDef("d", "f", "4", 1, 1, KeyType.LETTER),
            new KeyDef("g", "h", "5", 1, 2, KeyType.LETTER),
            new KeyDef("j", "k", "6", 1, 3, KeyType.LETTER),
            new KeyDef("l", null, ",", 1, 4, KeyType.LETTER),

            new KeyDef("z", "x", "@", 2, 0, KeyType.LETTER),
            new KeyDef("c", "v", "7", 2, 1, KeyType.LETTER),
            new KeyDef("b", "n", "8", 2, 2, KeyType.LETTER),
            new KeyDef("m", null, "9", 2, 3, KeyType.LETTER),
            new KeyDef("del", null, null, 2, 4, KeyType.BACKSPACE),

            new KeyDef("alt", null, null, 3, 0, KeyType.ALT),
            new KeyDef("sym", null, "*", 3, 1, KeyType.SYM),
            new KeyDef("space", null, "0 +", 3, 2, KeyType.SPACE),
            new KeyDef("aA", null, "#", 3, 3, KeyType.SHIFT),
            new KeyDef("enter", null, null, 3, 4, KeyType.ENTER),
    };

    private final Paint keyPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint keyPressedPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint keyActivePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint altTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint suggestionPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint suggestionTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chipTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF keyRect = new RectF();
    private final RectF touchRect = new RectF();
    private final RectF[] chipRects = new RectF[FLOATING_SUGGESTIONS];

    private Listener listener;
    private KeyboardTheme theme;
    private boolean shifted;
    private boolean symMode;
    private boolean altMode;
    private boolean twoTypeMode = true;
    private KeyDef pressedKey;
    private KeyDef lastTwoTypeKey;
    private long lastTwoTypeTime;
    private String composingText = "";
    private String[] suggestions = new String[0];
    private float density;
    private float keyWidth;
    private float horizontalInset;
    private float keyScale = 1.15f;
    private int touchPaddingPx;
    private int suggestionHeightPx;
    private int keyHeightPx;
    private int rowGapPx;
    private int keyGapPx;

    public BlackBerryKeyboardView(Context context) {
        super(context);
        init(context);
    }

    public BlackBerryKeyboardView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        density = context.getResources().getDisplayMetrics().density;
        touchPaddingPx = (int) (TOUCH_PADDING_DP * density);
        applyPreferences(context);
        textPaint.setTextAlign(Paint.Align.CENTER);
        altTextPaint.setTextAlign(Paint.Align.CENTER);
        suggestionTextPaint.setTextAlign(Paint.Align.CENTER);
        chipPaint.setStyle(Paint.Style.FILL);
        chipStrokePaint.setStyle(Paint.Style.STROKE);
        chipStrokePaint.setStrokeWidth(2 * density);
        chipTextPaint.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < FLOATING_SUGGESTIONS; i++) {
            chipRects[i] = new RectF();
        }
    }

    public void applyPreferences(Context context) {
        KeyboardPreferences prefs = KeyboardPreferences.get(context);
        applyTheme(prefs.getTheme());
        keyScale = prefs.getKeyScale();
        twoTypeMode = prefs.isTwoTypeMode();
        suggestionHeightPx = (int) (SUGGESTION_HEIGHT_DP * density);
        keyHeightPx = (int) (KEY_HEIGHT_DP * density * keyScale);
        rowGapPx = (int) (ROW_GAP_DP * density);
        keyGapPx = (int) (KEY_GAP_DP * density);
        requestLayout();
        invalidate();
    }

    public void applyTheme(KeyboardTheme theme) {
        this.theme = theme;
        keyPaint.setColor(theme.keyBackground);
        keyPressedPaint.setColor(theme.keyPressed);
        keyActivePaint.setColor(theme.keyActive);
        textPaint.setColor(theme.textPrimary);
        altTextPaint.setColor(theme.textSecondary);
        suggestionPaint.setColor(theme.suggestionBackground);
        suggestionTextPaint.setColor(theme.accent);
        setBackgroundColor(theme.background);
        invalidate();
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setShifted(boolean shifted) {
        this.shifted = shifted;
        invalidate();
    }

    public void updateSuggestions(String composing, String[] suggestions) {
        this.composingText = composing == null ? "" : composing;
        if (suggestions == null || suggestions.length == 0) {
            this.suggestions = new String[0];
        } else {
            int count = Math.min(FLOATING_SUGGESTIONS, suggestions.length);
            this.suggestions = new String[count];
            System.arraycopy(suggestions, 0, this.suggestions, 0, count);
        }
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        float gridWidth = width * GRID_WIDTH_RATIO;
        keyWidth = (gridWidth - keyGapPx * (COLS + 1)) / COLS;
        horizontalInset = (width - gridWidth) / 2f;
        float keyboardHeight = suggestionHeightPx + keyGapPx + 4f * (keyHeightPx + rowGapPx) + keyGapPx;
        setMeasuredDimension(width, (int) keyboardHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        drawSuggestions(canvas);
        drawKeys(canvas);
    }

    private void drawSuggestions(Canvas canvas) {
        float width = getWidth();
        canvas.drawRect(0, 0, width, suggestionHeightPx, suggestionPaint);

        if (composingText != null && !composingText.isEmpty()) {
            suggestionTextPaint.setColor(theme.textSecondary);
            suggestionTextPaint.setTextSize(13 * density);
            canvas.drawText(composingText, horizontalInset + 10 * density,
                    suggestionHeightPx * 0.35f, suggestionTextPaint);
        }

        if (suggestions.length == 0) {
            return;
        }

        float chipGap = 8 * density;
        float chipHeight = suggestionHeightPx * 0.52f;
        float chipTop = suggestionHeightPx * 0.38f;
        float totalChipWidth = width * GRID_WIDTH_RATIO;
        float chipWidth = (totalChipWidth - chipGap * (FLOATING_SUGGESTIONS - 1))
                / FLOATING_SUGGESTIONS;
        float startX = (width - totalChipWidth) / 2f;
        float corner = 14 * density;

        chipTextPaint.setTextSize(16 * density);
        for (int i = 0; i < suggestions.length && i < FLOATING_SUGGESTIONS; i++) {
            if (suggestions[i] == null || suggestions[i].isEmpty()) {
                continue;
            }
            float left = startX + i * (chipWidth + chipGap);
            chipRects[i].set(left, chipTop, left + chipWidth, chipTop + chipHeight);
            chipPaint.setColor(theme.keyBackground);
            chipStrokePaint.setColor(i == 0 ? theme.accent : theme.keyPressed);
            canvas.drawRoundRect(chipRects[i], corner, corner, chipPaint);
            canvas.drawRoundRect(chipRects[i], corner, corner, chipStrokePaint);
            chipTextPaint.setColor(i == 0 ? theme.accent : theme.textPrimary);
            canvas.drawText(suggestions[i], chipRects[i].centerX(),
                    chipRects[i].centerY() + 6 * density, chipTextPaint);
        }
    }

    private void drawKeys(Canvas canvas) {
        textPaint.setTextSize(18 * density * keyScale);
        altTextPaint.setTextSize(12 * density);
        float top = suggestionHeightPx + keyGapPx;
        float corner = 8 * density;

        for (KeyDef key : LAYOUT) {
            computeKeyRect(key, top, keyRect);
            Paint paint = keyPaint;
            if (key == pressedKey) {
                paint = keyPressedPaint;
            } else if ((key.type == KeyType.SYM && symMode)
                    || (key.type == KeyType.ALT && altMode)
                    || (key.type == KeyType.SHIFT && shifted)) {
                paint = keyActivePaint;
            }
            canvas.drawRoundRect(keyRect, corner, corner, paint);
            drawKeyContent(canvas, key);
        }
    }

    private void drawKeyContent(Canvas canvas, KeyDef key) {
        float cx = keyRect.centerX();
        float cy = keyRect.centerY();

        if (symMode && key.type == KeyType.LETTER && key.altLabel != null && !key.altLabel.isEmpty()) {
            canvas.drawText(key.altLabel, cx, cy + 8 * density, textPaint);
            return;
        }

        if (altMode && key.type == KeyType.LETTER) {
            String accent = accentForKey(key);
            if (accent != null) {
                canvas.drawText(accent, cx, cy + 8 * density, textPaint);
                return;
            }
        }

        if (key.altLabel != null && !key.altLabel.isEmpty() && key.type == KeyType.LETTER && !symMode) {
            canvas.drawText(key.altLabel, cx, keyRect.top + 16 * density, altTextPaint);
        }

        switch (key.type) {
            case LETTER:
                if (key.letter2 != null) {
                    float half = keyWidth * 0.20f;
                    String l1 = shifted ? key.letter1.toUpperCase() : key.letter1;
                    String l2 = shifted ? key.letter2.toUpperCase() : key.letter2;
                    canvas.drawText(l1, cx - half, cy + 8 * density, textPaint);
                    canvas.drawText(l2, cx + half, cy + 8 * density, textPaint);
                } else {
                    String l = shifted ? key.letter1.toUpperCase() : key.letter1;
                    canvas.drawText(l, cx, cy + 8 * density, textPaint);
                }
                break;
            case BACKSPACE:
                canvas.drawText("del", cx, cy + 8 * density, textPaint);
                break;
            case SPACE:
                canvas.drawText(symMode && key.altLabel != null ? key.altLabel : "space",
                        cx, cy + 8 * density, symMode ? textPaint : altTextPaint);
                break;
            case SHIFT:
                if (symMode && key.altLabel != null) {
                    canvas.drawText(key.altLabel, cx, cy + 8 * density, textPaint);
                } else {
                    canvas.drawText(shifted ? "AA" : "aA", cx, cy + 8 * density, textPaint);
                }
                break;
            case SYM:
                canvas.drawText(symMode ? "ABC" : "sym", cx, cy + 8 * density, textPaint);
                break;
            case ALT:
                canvas.drawText(altMode ? "abc" : "alt", cx, cy + 8 * density, textPaint);
                break;
            case ENTER:
                canvas.drawText("enter", cx, cy + 8 * density, textPaint);
                break;
            default:
                break;
        }
    }

    private String accentForKey(KeyDef key) {
        if (key.letter2 != null) {
            String a = accentChar(key.letter1);
            String b = accentChar(key.letter2);
            if (a != null && b != null) {
                return a + " " + b;
            }
            return a != null ? a : b;
        }
        return accentChar(key.letter1);
    }

    private static String accentChar(String letter) {
        if (letter == null) {
            return null;
        }
        switch (letter) {
            case "a": return "á";
            case "e": return "é";
            case "i": return "í";
            case "o": return "ó";
            case "u": return "ú";
            case "s": return "ñ";
            case "n": return "ñ";
            case "c": return "ç";
            default: return null;
        }
    }

    private String symbolForKey(KeyDef key) {
        if (altMode) {
            if (key.type == KeyType.LETTER) {
                if (key.letter2 != null) {
                    String a = accentChar(key.letter1);
                    return a != null ? a : key.letter1;
                }
                String a = accentChar(key.letter1);
                return a != null ? a : key.letter1;
            }
        }
        if (key.altLabel != null && !key.altLabel.isEmpty()) {
            return key.altLabel;
        }
        return null;
    }

    private void computeKeyRect(KeyDef key, float top, RectF out) {
        float x = horizontalInset + keyGapPx + key.col * (keyWidth + keyGapPx);
        float y = top + key.row * (keyHeightPx + rowGapPx);
        out.set(x, y, x + keyWidth, y + keyHeightPx);
    }

    private void computeTouchRect(KeyDef key, float top, RectF out) {
        computeKeyRect(key, top, out);
        out.inset(-touchPaddingPx, -touchPaddingPx);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (listener == null) {
            return super.onTouchEvent(event);
        }

        float y = event.getY();
        if (y < suggestionHeightPx && event.getAction() == MotionEvent.ACTION_UP) {
            int index = pickSuggestionIndex(event.getX(), y);
            if (index >= 0) {
                listener.onSuggestionSelected(index);
            }
            return true;
        }

        KeyDef key = findKeyAt(event.getX(), event.getY());
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                pressedKey = key;
                invalidate();
                return true;
            case MotionEvent.ACTION_UP:
                if (key != null && key == pressedKey) {
                    handleKeyUp(key, event.getX());
                }
                pressedKey = null;
                invalidate();
                return true;
            case MotionEvent.ACTION_CANCEL:
                pressedKey = null;
                invalidate();
                return true;
            default:
                return true;
        }
    }

    private int pickSuggestionIndex(float x, float y) {
        for (int i = 0; i < suggestions.length && i < FLOATING_SUGGESTIONS; i++) {
            if (suggestions[i] != null && !suggestions[i].isEmpty()
                    && chipRects[i].contains(x, y)) {
                return i;
            }
        }
        return -1;
    }

    private KeyDef findKeyAt(float x, float y) {
        float top = suggestionHeightPx + keyGapPx;
        for (KeyDef key : LAYOUT) {
            computeTouchRect(key, top, touchRect);
            if (touchRect.contains(x, y)) {
                return key;
            }
        }
        return null;
    }

    private void handleKeyUp(KeyDef key, float tapX) {
        if (symMode || altMode) {
            handleSymbolKey(key);
            return;
        }

        switch (key.type) {
            case LETTER:
                handleLetterKey(key, tapX);
                break;
            case SHIFT:
                listener.onShift();
                break;
            case BACKSPACE:
                listener.onBackspace();
                break;
            case SPACE:
                listener.onSpace();
                break;
            case ENTER:
                listener.onEnter();
                break;
            case SYM:
                symMode = !symMode;
                altMode = false;
                invalidate();
                break;
            case ALT:
                altMode = !altMode;
                symMode = false;
                invalidate();
                break;
            default:
                break;
        }
    }

    private void handleLetterKey(KeyDef key, float tapX) {
        if (key.letter2 == null) {
            lastTwoTypeKey = null;
            listener.onLetter(key.letter1.charAt(0));
            return;
        }

        if (twoTypeMode) {
            long now = System.currentTimeMillis();
            if (key == lastTwoTypeKey && (now - lastTwoTypeTime) < DOUBLE_TAP_MS) {
                listener.onTwoTypeSecond(key.letter2.charAt(0));
                lastTwoTypeKey = null;
                return;
            }
            float top = suggestionHeightPx + keyGapPx;
            computeKeyRect(key, top, keyRect);
            char letter = tapX >= keyRect.centerX()
                    ? key.letter2.charAt(0) : key.letter1.charAt(0);
            listener.onLetter(letter);
            lastTwoTypeKey = key;
            lastTwoTypeTime = now;
        } else {
            lastTwoTypeKey = null;
            listener.onSureTypeKey(key.letter1.charAt(0), key.letter2.charAt(0));
        }
    }

    private void handleSymbolKey(KeyDef key) {
        switch (key.type) {
            case SYM:
                symMode = false;
                altMode = false;
                invalidate();
                return;
            case ALT:
                altMode = false;
                symMode = false;
                invalidate();
                return;
            case BACKSPACE:
                listener.onBackspace();
                return;
            case ENTER:
                listener.onEnter();
                return;
            case LETTER:
            case SPACE:
            case SHIFT:
                String symbol = symbolForKey(key);
                if (symbol != null && !symbol.isEmpty()) {
                    listener.onSymbol(symbol);
                }
                break;
            default:
                break;
        }
    }
}
