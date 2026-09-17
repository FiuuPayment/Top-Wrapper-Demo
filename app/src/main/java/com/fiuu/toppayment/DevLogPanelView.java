package com.fiuu.toppayment;

import com.fiuu.toppayment.app.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Tap-to-toggle drawer docked to the bottom edge, mounted once in MainActivity's layout and once
 * in the tap-card dialog's layout. Both instances render the same DevLogBuffer, so the log is
 * continuous across the two windows.
 */
public class DevLogPanelView extends FrameLayout implements DevLogBuffer.Listener {

    private static final long ANIM_DURATION_MS = 200L;

    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);

    private TextView handleLabel;
    private ScrollView logScroll;
    private TextView logText;
    private float contentHeightPx;
    private boolean isOpen = false;

    private final int touchSlop;
    private float dragStartRawY;
    private float dragStartTranslationY;

    public DevLogPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(R.layout.view_dev_log_panel, this, true);

        View header = findViewById(R.id.devLogHeader);
        handleLabel = findViewById(R.id.devLogHandleLabel);
        TextView clearButton = findViewById(R.id.devLogClearBtn);
        logScroll = findViewById(R.id.devLogScroll);
        logText = findViewById(R.id.devLogText);

        // Starts collapsed: pushed down by the content area's fixed height so only the header peeks up.
        contentHeightPx = getResources().getDimension(R.dimen.dev_log_panel_content_height);
        setTranslationY(contentHeightPx);

        touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        header.setOnTouchListener(this::onHeaderTouch);
        clearButton.setOnClickListener(v -> DevLogBuffer.getInstance().clear());

        // Keeps the handle above the system nav bar. On hosts that already consume system-bar
        // insets at the root (MainActivity, SuccessPayment, FailPayment), this receives zero
        // insets and is a no-op; on the tap-card dialog, which lets its blurred background draw
        // full-bleed, this is the only place the bottom inset gets applied.
        ViewCompat.setOnApplyWindowInsetsListener(this, (v, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) getLayoutParams();
            if (params != null && params.bottomMargin != bars.bottom) {
                params.bottomMargin = bars.bottom;
                setLayoutParams(params);
            }
            return insets;
        });
    }

    /** Handles both a plain tap (snap fully open/closed) and a drag (free resize between the two). */
    private boolean onHeaderTouch(View v, MotionEvent event) {
        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                dragStartRawY = event.getRawY();
                dragStartTranslationY = getTranslationY();
                return true;
            case MotionEvent.ACTION_MOVE:
                float delta = event.getRawY() - dragStartRawY;
                float target = Math.max(0f, Math.min(contentHeightPx, dragStartTranslationY + delta));
                setTranslationY(target);
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (Math.abs(event.getRawY() - dragStartRawY) < touchSlop) {
                    toggle();
                } else {
                    settleAfterDrag();
                }
                v.performClick();
                return true;
            default:
                return false;
        }
    }

    private void toggle() {
        isOpen = !isOpen;
        animateTo(isOpen ? 0f : contentHeightPx);
    }

    /** After a free drag, snaps to fully open/closed only if left within touch-slop of an edge; otherwise stays put. */
    private void settleAfterDrag() {
        float current = getTranslationY();
        if (current < touchSlop) {
            isOpen = true;
            animateTo(0f);
        } else if (contentHeightPx - current < touchSlop) {
            isOpen = false;
            animateTo(contentHeightPx);
        } else {
            isOpen = current < contentHeightPx / 2f;
            handleLabel.setText(isOpen ? R.string.dev_log_hide : R.string.dev_log_show);
        }
    }

    private void animateTo(float translationY) {
        animate().translationY(translationY)
                .setDuration(ANIM_DURATION_MS)
                .setInterpolator(new DecelerateInterpolator())
                .start();
        handleLabel.setText(isOpen ? R.string.dev_log_hide : R.string.dev_log_show);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        DevLogBuffer buffer = DevLogBuffer.getInstance();
        buffer.addListener(this);
        renderAll(buffer.getAll());
    }

    @Override
    protected void onDetachedFromWindow() {
        DevLogBuffer.getInstance().removeListener(this);
        super.onDetachedFromWindow();
    }

    private void renderAll(List<DevLogBuffer.Entry> entries) {
        StringBuilder sb = new StringBuilder();
        for (DevLogBuffer.Entry entry : entries) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(formatLine(entry));
        }
        logText.setText(sb.toString());
        scrollToBottom();
    }

    private String formatLine(DevLogBuffer.Entry entry) {
        return timeFormat.format(entry.timestamp) + "  " + entry.tag + "  " + entry.message;
    }

    @Override
    public void onAppended(DevLogBuffer.Entry entry) {
        if (logText.length() > 0) {
            logText.append("\n");
        }
        logText.append(formatLine(entry));
        scrollToBottom();
    }

    @Override
    public void onCleared() {
        logText.setText("");
    }

    private void scrollToBottom() {
        logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
    }
}
