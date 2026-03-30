package dev.namelessnanashi.walrus.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import android.widget.Button;

import dev.namelessnanashi.walrus.R;

/**
 * Adapted from https://github.com/keepandroidopen/FreeDroidWarn for this support-library app.
 */
public final class FreeDroidWarnCompat {

    private static final String PREF_INSTALL_TOKEN_WARN = "installTokenWarn";
    private static final String INFO_URL = "https://keepandroidopen.org/";
    private static final String SOLUTIONS_URL =
            "https://github.com/keepandroidopen/FreeDroidWarn/blob/master/README.md#solutions";

    private FreeDroidWarnCompat() {
    }

    public static void markCurrentInstallSeen(Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        prefManager.edit()
                .putLong(PREF_INSTALL_TOKEN_WARN, AppInstallStateUtils.getCurrentInstallToken(context))
                .apply();
    }

    public static void showWarningIfNeeded(Context context) {
        SharedPreferences prefManager = PreferenceManager.getDefaultSharedPreferences(context);
        long currentInstallToken = AppInstallStateUtils.getCurrentInstallToken(context);
        long shownInstallToken = prefManager.getLong(PREF_INSTALL_TOKEN_WARN, 0L);
        if (currentInstallToken <= shownInstallToken) {
            return;
        }

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(context);
        alertDialogBuilder.setTitle(R.string.freedroidwarn_title);
        alertDialogBuilder.setMessage(R.string.freedroidwarn_message);
        alertDialogBuilder.setNegativeButton(R.string.freedroidwarn_more_info,
                (dialog, which) -> context.startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(INFO_URL))));
        alertDialogBuilder.setNeutralButton(R.string.freedroidwarn_solutions,
                (dialog, which) -> context.startActivity(
                        new Intent(Intent.ACTION_VIEW, Uri.parse(SOLUTIONS_URL))));
        alertDialogBuilder.setPositiveButton(android.R.string.ok, (dialog, which) ->
                markCurrentInstallSeen(context));

        AlertDialog alertDialog = alertDialogBuilder.create();
        alertDialog.show();
        AppFontManager.applyToDialog(alertDialog);

        Button neutralButton = alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        if (neutralButton != null) {
            neutralButton.setTextColor(
                    ContextCompat.getColor(context, R.color.secondaryLightColor));
        }
    }
}
