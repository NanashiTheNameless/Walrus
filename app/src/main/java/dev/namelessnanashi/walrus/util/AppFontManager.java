package dev.namelessnanashi.walrus.util;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.preference.ListPreference;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import dev.namelessnanashi.walrus.R;

public final class AppFontManager {

    public static final String PREFERENCE_KEY_APP_FONT = "pref_key_app_font";
    public static final String FONT_VALUE_0XPROTO = "font_0xproto";
    public static final String FONT_VALUE_SYSTEM_DEFAULT = "system_default";

    private static final String FONT_VALUE_SYSTEM_FILE_PREFIX = "system_file:";

    @Nullable
    private static String cachedTypefaceKey;

    @Nullable
    private static Typeface cachedTypeface;

    private static boolean installed;

    private AppFontManager() {
    }

    public static synchronized void install(@NonNull Application application) {
        if (installed) {
            return;
        }

        application.registerActivityLifecycleCallbacks(
                new Application.ActivityLifecycleCallbacks() {
                    @Override
                    public void onActivityCreated(Activity activity,
                            Bundle savedInstanceState) {
                        applyToActivity(activity);
                    }

                    @Override
                    public void onActivityStarted(Activity activity) {
                    }

                    @Override
                    public void onActivityResumed(Activity activity) {
                        applyToActivity(activity);
                    }

                    @Override
                    public void onActivityPaused(Activity activity) {
                    }

                    @Override
                    public void onActivityStopped(Activity activity) {
                    }

                    @Override
                    public void onActivitySaveInstanceState(Activity activity,
                            Bundle outState) {
                    }

                    @Override
                    public void onActivityDestroyed(Activity activity) {
                    }
                });
        installed = true;
    }

    public static void applyToActivity(@NonNull Activity activity) {
        applyToWindow(activity, activity.getWindow());
    }

    public static void applyToDialog(@NonNull Dialog dialog) {
        applyToWindow(dialog.getContext(), dialog.getWindow());
    }

    public static void configurePreference(@NonNull Context context,
            @NonNull ListPreference fontPreference) {
        String currentValue = getStoredFontValue(context);
        List<FontOption> options = getAvailableFontOptions(context);

        if (findOption(options, currentValue) == null
                && currentValue.startsWith(FONT_VALUE_SYSTEM_FILE_PREFIX)) {
            options.add(new FontOption(currentValue,
                    context.getString(R.string.app_font_option_saved_system_font_unavailable)));
        }

        CharSequence[] entries = new CharSequence[options.size()];
        CharSequence[] entryValues = new CharSequence[options.size()];
        for (int i = 0; i < options.size(); i++) {
            FontOption option = options.get(i);
            entries[i] = option.label;
            entryValues[i] = option.value;
        }

        fontPreference.setEntries(entries);
        fontPreference.setEntryValues(entryValues);
        fontPreference.setValue(currentValue);
        updatePreferenceSummary(context, fontPreference, currentValue);
    }

    public static void updatePreferenceSummary(@NonNull Context context,
            @NonNull ListPreference fontPreference, @Nullable String value) {
        FontOption option = findOption(getAvailableFontOptions(context),
                sanitizeStoredFontValue(value));
        String label = option != null
                ? option.label
                : context.getString(R.string.app_font_option_saved_system_font_unavailable);
        fontPreference.setSummary(
                context.getString(R.string.app_font_preference_summary, label));
    }

    @NonNull
    private static List<FontOption> getAvailableFontOptions(@NonNull Context context) {
        List<FontOption> options = new ArrayList<>();
        options.add(new FontOption(FONT_VALUE_0XPROTO,
                context.getString(R.string.app_font_option_0xproto)));
        options.add(new FontOption(FONT_VALUE_SYSTEM_DEFAULT,
                context.getString(R.string.app_font_option_system_default)));

        try {
            options.addAll(buildSystemFontOptions());
        } catch (RuntimeException ignored) {
        }
        return options;
    }

    @NonNull
    private static List<FontOption> buildSystemFontOptions() {
        java.util.Set<android.graphics.fonts.Font> availableFonts =
                android.graphics.fonts.SystemFonts.getAvailableFonts();
        java.util.Map<String, FontCandidate> candidates = new java.util.LinkedHashMap<>();

        for (android.graphics.fonts.Font font : availableFonts) {
            File file = font.getFile();
            if (file == null) {
                continue;
            }

            String displayName = buildDisplayName(file);
            if (TextUtils.isEmpty(displayName)) {
                continue;
            }

            FontCandidate candidate = new FontCandidate(
                    new FontOption(FONT_VALUE_SYSTEM_FILE_PREFIX + file.getAbsolutePath(),
                            displayName),
                    scoreFont(font, file));
            FontCandidate existingCandidate = candidates.get(displayName);
            if (existingCandidate == null || candidate.score < existingCandidate.score
                    || candidate.score == existingCandidate.score
                    && candidate.option.value.compareTo(existingCandidate.option.value) < 0) {
                candidates.put(displayName, candidate);
            }
        }

        List<FontCandidate> sortedCandidates = new ArrayList<>(candidates.values());
        Collections.sort(sortedCandidates, new Comparator<FontCandidate>() {
            @Override
            public int compare(FontCandidate left, FontCandidate right) {
                return left.option.label.compareToIgnoreCase(right.option.label);
            }
        });

        List<FontOption> options = new ArrayList<>(sortedCandidates.size());
        for (FontCandidate candidate : sortedCandidates) {
            options.add(candidate.option);
        }
        return options;
    }

    @NonNull
    private static String buildDisplayName(@NonNull File file) {
        String name = file.getName();
        int extensionSeparator = name.lastIndexOf('.');
        if (extensionSeparator > 0) {
            name = name.substring(0, extensionSeparator);
        }

        name = name.replace('_', ' ')
                .replace('-', ' ')
                .replace('+', ' ')
                .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
                .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
                .replaceAll("\\s+", " ")
                .trim();

        String[] tokens = name.split(" ");
        int endIndex = tokens.length;
        while (endIndex > 1 && isStyleQualifier(tokens[endIndex - 1])) {
            endIndex--;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < endIndex; i++) {
            if (tokens[i].isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }
            builder.append(tokens[i]);
        }

        return builder.toString().trim();
    }

    private static boolean isStyleQualifier(@NonNull String token) {
        String normalizedToken = token.toLowerCase(Locale.ROOT);
        return "regular".equals(normalizedToken)
                || "book".equals(normalizedToken)
                || "roman".equals(normalizedToken)
                || "medium".equals(normalizedToken)
                || "semibold".equals(normalizedToken)
                || "demi".equals(normalizedToken)
                || "demibold".equals(normalizedToken)
                || "bold".equals(normalizedToken)
                || "extrabold".equals(normalizedToken)
                || "black".equals(normalizedToken)
                || "heavy".equals(normalizedToken)
                || "light".equals(normalizedToken)
                || "extralight".equals(normalizedToken)
                || "thin".equals(normalizedToken)
                || "italic".equals(normalizedToken)
                || "oblique".equals(normalizedToken)
                || "variable".equals(normalizedToken)
                || "vf".equals(normalizedToken);
    }

    @NonNull
    private static Typeface resolveTypeface(@NonNull Context context) {
        String fontValue = getStoredFontValue(context);

        synchronized (AppFontManager.class) {
            if (fontValue.equals(cachedTypefaceKey) && cachedTypeface != null) {
                return cachedTypeface;
            }
        }

        Typeface typeface = createTypeface(context, fontValue);

        synchronized (AppFontManager.class) {
            cachedTypefaceKey = fontValue;
            cachedTypeface = typeface;
        }

        return typeface;
    }

    @NonNull
    private static Typeface createTypeface(@NonNull Context context, @NonNull String fontValue) {
        if (FONT_VALUE_SYSTEM_DEFAULT.equals(fontValue)) {
            return Typeface.DEFAULT;
        }

        if (fontValue.startsWith(FONT_VALUE_SYSTEM_FILE_PREFIX)) {
            Typeface systemTypeface = createSystemTypefaceFromFile(fontValue.substring(
                    FONT_VALUE_SYSTEM_FILE_PREFIX.length()));
            return systemTypeface != null ? systemTypeface : Typeface.DEFAULT;
        }

        Typeface bundledTypeface = ResourcesCompat.getFont(context, R.font.font_0xproto);
        return bundledTypeface != null ? bundledTypeface : Typeface.MONOSPACE;
    }

    @Nullable
    private static Typeface createSystemTypefaceFromFile(@Nullable String filePath) {
        if (TextUtils.isEmpty(filePath)) {
            return null;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        try {
            return new Typeface.Builder(file).build();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static void applyToWindow(@NonNull Context context, @Nullable Window window) {
        if (window == null) {
            return;
        }

        View decorView = window.getDecorView();
        if (decorView == null) {
            return;
        }

        applyTypeface(decorView, resolveTypeface(context));
    }

    private static void applyTypeface(@NonNull View view, @NonNull Typeface typeface) {
        if (view instanceof TextView) {
            TextView textView = (TextView) view;
            Typeface currentTypeface = textView.getTypeface();
            int style = currentTypeface != null ? currentTypeface.getStyle() : Typeface.NORMAL;
            textView.setTypeface(typeface, style);
        }

        if (!(view instanceof ViewGroup)) {
            return;
        }

        ViewGroup viewGroup = (ViewGroup) view;
        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            applyTypeface(viewGroup.getChildAt(i), typeface);
        }
    }

    @NonNull
    private static String getStoredFontValue(@NonNull Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sanitizeStoredFontValue(
                preferences.getString(PREFERENCE_KEY_APP_FONT, FONT_VALUE_0XPROTO));
    }

    @NonNull
    private static String sanitizeStoredFontValue(@Nullable String value) {
        return TextUtils.isEmpty(value) ? FONT_VALUE_0XPROTO : value;
    }

    @Nullable
    private static FontOption findOption(@NonNull List<FontOption> options, @Nullable String value) {
        String sanitizedValue = sanitizeStoredFontValue(value);
        for (FontOption option : options) {
            if (option.value.equals(sanitizedValue)) {
                return option;
            }
        }
        return null;
    }

    private static final class FontCandidate {

        @NonNull
        private final FontOption option;

        private final int score;

        private FontCandidate(@NonNull FontOption option, int score) {
            this.option = option;
            this.score = score;
        }
    }

    private static final class FontOption {

        @NonNull
        private final String value;

        @NonNull
        private final String label;

        private FontOption(@NonNull String value, @NonNull String label) {
            this.value = value;
            this.label = label;
        }
    }

    private static int scoreFont(@NonNull android.graphics.fonts.Font font,
            @NonNull File file) {
        android.graphics.fonts.FontStyle style = font.getStyle();
        int score = Math.abs(style.getWeight()
                - android.graphics.fonts.FontStyle.FONT_WEIGHT_NORMAL);

        if (style.getSlant() != android.graphics.fonts.FontStyle.FONT_SLANT_UPRIGHT) {
            score += 200;
        }

        String fileName = file.getName().toLowerCase(Locale.ROOT);
        if (fileName.contains("regular")) {
            score -= 50;
        }
        if (fileName.contains("italic") || fileName.contains("oblique")) {
            score += 150;
        }
        if (fileName.contains("bold")) {
            score += 75;
        }

        return score;
    }
}
