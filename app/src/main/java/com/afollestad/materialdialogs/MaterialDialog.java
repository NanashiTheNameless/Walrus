package com.afollestad.materialdialogs;

import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.EnumMap;

import dev.namelessnanashi.walrus.util.AppFontManager;

public class MaterialDialog extends AlertDialog {

    private final EnumMap<DialogAction, View> actionButtonPlaceholders =
            new EnumMap<>(DialogAction.class);
    private final EnumMap<DialogAction, Integer> actionButtonColorResIds =
            new EnumMap<>(DialogAction.class);

    @Nullable
    private final View customView;

    @Nullable
    private final Integer backgroundColorResId;

    @Nullable
    private final Integer titleColorResId;

    @Nullable
    private final Integer contentColorResId;

    protected MaterialDialog(@NonNull Builder builder) {
        super(builder.context);

        customView = builder.customView != null
                ? builder.customView
                : builder.showProgress
                        ? createProgressView(builder.context, builder.content,
                                builder.indeterminateProgress)
                        : null;
        backgroundColorResId = builder.backgroundColorResId;
        titleColorResId = builder.titleColorResId;
        contentColorResId = builder.contentColorResId;

        for (DialogAction action : DialogAction.values()) {
            actionButtonPlaceholders.put(action, new View(getContext()));
        }

        if (!TextUtils.isEmpty(builder.title)) {
            setTitle(builder.title);
        }
        if (customView == null && !TextUtils.isEmpty(builder.content)) {
            setMessage(builder.content);
        }
        if (customView != null) {
            setView(customView);
        }

        setCancelable(builder.cancelable);
        setCanceledOnTouchOutside(builder.canceledOnTouchOutside);

        configureButton(DialogAction.POSITIVE, builder.positiveText, builder.positiveCallback);
        configureButton(DialogAction.NEGATIVE, builder.negativeText, builder.negativeCallback);
        configureButton(DialogAction.NEUTRAL, builder.neutralText, builder.neutralCallback);

        if (builder.positiveColorResId != null) {
            actionButtonColorResIds.put(DialogAction.POSITIVE, builder.positiveColorResId);
        }
        if (builder.negativeColorResId != null) {
            actionButtonColorResIds.put(DialogAction.NEGATIVE, builder.negativeColorResId);
        }
        if (builder.neutralColorResId != null) {
            actionButtonColorResIds.put(DialogAction.NEUTRAL, builder.neutralColorResId);
        }
        if (builder.widgetColorResId != null) {
            for (DialogAction action : DialogAction.values()) {
                if (!actionButtonColorResIds.containsKey(action)) {
                    actionButtonColorResIds.put(action, builder.widgetColorResId);
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (backgroundColorResId != null && getWindow() != null) {
            getWindow().setBackgroundDrawable(new ColorDrawable(
                    ContextCompat.getColor(getContext(), backgroundColorResId)));
        }

        if (customView != null && getWindow() != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        }

        syncTextColor(android.R.id.message, contentColorResId);
        syncTextColor(androidx.appcompat.R.id.alertTitle, titleColorResId);

        for (DialogAction action : DialogAction.values()) {
            View actionButton = getButton(toAlertDialogButton(action));
            if (actionButton == null) {
                continue;
            }

            actionButton.setEnabled(actionButtonPlaceholders.get(action).isEnabled());

            Integer colorResId = actionButtonColorResIds.get(action);
            if (colorResId != null && actionButton instanceof TextView) {
                ((TextView) actionButton).setTextColor(
                        ContextCompat.getColor(getContext(), colorResId));
            }
        }

        ensureSoftInputForTextEditor();
        AppFontManager.applyToDialog(this);
    }

    @Nullable
    public View getCustomView() {
        return customView;
    }

    @NonNull
    public View getActionButton(@NonNull DialogAction action) {
        View actionButton = getButton(toAlertDialogButton(action));
        return actionButton != null ? actionButton : actionButtonPlaceholders.get(action);
    }

    private void configureButton(@NonNull final DialogAction action, @Nullable CharSequence text,
            @Nullable final SingleButtonCallback callback) {
        if (TextUtils.isEmpty(text)) {
            return;
        }

        setButton(toAlertDialogButton(action), text, (dialog, which) -> {
            if (callback != null) {
                callback.onClick(this, action);
            }
        });
    }

    private void syncTextColor(int viewId, @Nullable Integer colorResId) {
        if (colorResId == null) {
            return;
        }

        TextView textView = findViewById(viewId);
        if (textView != null) {
            textView.setTextColor(ContextCompat.getColor(getContext(), colorResId));
        }
    }

    private void ensureSoftInputForTextEditor() {
        View textEditor = findFirstTextEditor(customView);
        if (textEditor == null) {
            return;
        }

        if (getWindow() != null) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
            getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        }

        textEditor.post(() -> {
            if (!textEditor.isShown()) {
                return;
            }

            textEditor.requestFocus();

            InputMethodManager inputMethodManager =
                    (InputMethodManager) getContext().getSystemService(
                            Context.INPUT_METHOD_SERVICE);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(textEditor, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    @Nullable
    private View findFirstTextEditor(@Nullable View view) {
        if (view == null || view.getVisibility() != View.VISIBLE || !view.isEnabled()) {
            return null;
        }

        if (view.onCheckIsTextEditor()) {
            return view;
        }

        if (!(view instanceof ViewGroup)) {
            return null;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            View textEditor = findFirstTextEditor(viewGroup.getChildAt(i));
            if (textEditor != null) {
                return textEditor;
            }
        }

        return null;
    }

    private static int toAlertDialogButton(@NonNull DialogAction action) {
        switch (action) {
            case POSITIVE:
                return BUTTON_POSITIVE;
            case NEGATIVE:
                return BUTTON_NEGATIVE;
            case NEUTRAL:
                return BUTTON_NEUTRAL;
            default:
                throw new IllegalArgumentException("Unsupported action: " + action);
        }
    }

    @NonNull
    private static View createProgressView(@NonNull Context context, @Nullable CharSequence content,
            boolean indeterminateProgress) {
        LinearLayout container = new LinearLayout(context);
        container.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        container.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
        container.setPadding(padding, padding, padding, padding);

        ProgressBar progressBar = new ProgressBar(context);
        progressBar.setIndeterminate(indeterminateProgress);
        container.addView(progressBar);

        if (!TextUtils.isEmpty(content)) {
            TextView messageView = new TextView(context);
            messageView.setId(android.R.id.message);
            messageView.setText(content);
            int topPadding = (int) (16 * context.getResources().getDisplayMetrics().density);
            messageView.setPadding(0, topPadding, 0, 0);
            container.addView(messageView);
        }

        return container;
    }

    public interface SingleButtonCallback {
        void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which);
    }

    public static class Builder {

        private final Context context;

        @Nullable
        private CharSequence title;

        @Nullable
        private CharSequence content;

        @Nullable
        private View customView;

        @Nullable
        private CharSequence positiveText;

        @Nullable
        private CharSequence negativeText;

        @Nullable
        private CharSequence neutralText;

        @Nullable
        private SingleButtonCallback positiveCallback;

        @Nullable
        private SingleButtonCallback negativeCallback;

        @Nullable
        private SingleButtonCallback neutralCallback;

        @Nullable
        private Integer backgroundColorResId;

        @Nullable
        private Integer titleColorResId;

        @Nullable
        private Integer contentColorResId;

        @Nullable
        private Integer positiveColorResId;

        @Nullable
        private Integer negativeColorResId;

        @Nullable
        private Integer neutralColorResId;

        @Nullable
        private Integer widgetColorResId;

        private boolean cancelable = true;

        private boolean canceledOnTouchOutside = true;

        private boolean showProgress;

        private boolean indeterminateProgress;

        public Builder(@NonNull Context context) {
            this.context = context;
        }

        @NonNull
        public Builder title(@StringRes int titleResId) {
            return title(context.getText(titleResId));
        }

        @NonNull
        public Builder title(@Nullable CharSequence title) {
            this.title = title;
            return this;
        }

        @NonNull
        public Builder content(@StringRes int contentResId) {
            return content(context.getText(contentResId));
        }

        @NonNull
        public Builder content(@Nullable CharSequence content) {
            this.content = content;
            return this;
        }

        @NonNull
        public Builder positiveText(@StringRes int positiveTextResId) {
            return positiveText(context.getText(positiveTextResId));
        }

        @NonNull
        public Builder positiveText(@Nullable CharSequence positiveText) {
            this.positiveText = positiveText;
            return this;
        }

        @NonNull
        public Builder negativeText(@StringRes int negativeTextResId) {
            return negativeText(context.getText(negativeTextResId));
        }

        @NonNull
        public Builder negativeText(@Nullable CharSequence negativeText) {
            this.negativeText = negativeText;
            return this;
        }

        @NonNull
        public Builder neutralText(@StringRes int neutralTextResId) {
            return neutralText(context.getText(neutralTextResId));
        }

        @NonNull
        public Builder neutralText(@Nullable CharSequence neutralText) {
            this.neutralText = neutralText;
            return this;
        }

        @NonNull
        public Builder onPositive(@Nullable SingleButtonCallback positiveCallback) {
            this.positiveCallback = positiveCallback;
            return this;
        }

        @NonNull
        public Builder onNegative(@Nullable SingleButtonCallback negativeCallback) {
            this.negativeCallback = negativeCallback;
            return this;
        }

        @NonNull
        public Builder onNeutral(@Nullable SingleButtonCallback neutralCallback) {
            this.neutralCallback = neutralCallback;
            return this;
        }

        @NonNull
        public Builder customView(@LayoutRes int layoutResId, boolean wrapInScrollView) {
            return customView(LayoutInflater.from(context).inflate(layoutResId, null, false),
                    wrapInScrollView);
        }

        @NonNull
        public Builder customView(@NonNull View customView, boolean wrapInScrollView) {
            this.customView = customView;
            return this;
        }

        @NonNull
        public Builder adapter(@NonNull RecyclerView.Adapter<?> adapter, @Nullable Object callback) {
            RecyclerView recyclerView = new RecyclerView(context);
            recyclerView.setLayoutManager(new LinearLayoutManager(context));
            recyclerView.setAdapter(adapter);
            customView = recyclerView;
            return this;
        }

        @NonNull
        public Builder progress(boolean indeterminate, int max) {
            showProgress = true;
            indeterminateProgress = indeterminate;
            return this;
        }

        @NonNull
        public Builder cancelable(boolean cancelable) {
            this.cancelable = cancelable;
            if (!cancelable) {
                canceledOnTouchOutside = false;
            }
            return this;
        }

        @NonNull
        public Builder titleColorRes(@ColorRes int colorResId) {
            titleColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder contentColorRes(@ColorRes int colorResId) {
            contentColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder backgroundColorRes(@ColorRes int colorResId) {
            backgroundColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder positiveColorRes(@ColorRes int colorResId) {
            positiveColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder negativeColorRes(@ColorRes int colorResId) {
            negativeColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder neutralColorRes(@ColorRes int colorResId) {
            neutralColorResId = colorResId;
            return this;
        }

        @NonNull
        public Builder widgetColorRes(@ColorRes int colorResId) {
            widgetColorResId = colorResId;
            return this;
        }

        @NonNull
        public MaterialDialog build() {
            return new MaterialDialog(this);
        }

        @NonNull
        public MaterialDialog show() {
            MaterialDialog dialog = build();
            dialog.show();
            return dialog;
        }
    }
}
