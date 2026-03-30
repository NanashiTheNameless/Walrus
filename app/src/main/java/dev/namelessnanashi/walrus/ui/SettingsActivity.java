/*
 * Copyright 2018 Daniel Underhay & Matthew Daley.
 *
 * This file is part of Walrus.
 *
 * Walrus is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Walrus is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Walrus.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.namelessnanashi.walrus.ui;

import android.os.Bundle;
import androidx.fragment.app.DialogFragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import dev.namelessnanashi.walrus.R;
import dev.namelessnanashi.walrus.card.ui.DeleteAllCardsPreference;
import dev.namelessnanashi.walrus.util.AppFontManager;

public class SettingsActivity extends AppCompatActivity {

    private static final String OPEN_SOURCE_PREFERENCE_KEY = "pref_key_open_source_licenses";
    private static final String OPEN_SOURCE_URL = "file:///android_asset/open_source.html";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getSupportFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            addPreferencesFromResource(R.xml.preferences);
            configureFontPreference();

            Preference openSourcePreference = findPreference(OPEN_SOURCE_PREFERENCE_KEY);
            if (openSourcePreference != null) {
                openSourcePreference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        startActivity(WebViewActivity.createIntent(getContext(), OPEN_SOURCE_URL));
                        return true;
                    }
                });
            }
        }

        private void configureFontPreference() {
            final ListPreference fontPreference =
                    findPreference(AppFontManager.PREFERENCE_KEY_APP_FONT);
            if (fontPreference == null) {
                return;
            }

            AppFontManager.configurePreference(requireContext(), fontPreference);
            fontPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    AppFontManager.updatePreferenceSummary(requireContext(), fontPreference,
                            newValue != null ? newValue.toString() : null);
                    requireActivity().getWindow().getDecorView().post(new Runnable() {
                        @Override
                        public void run() {
                            if (!isAdded()) {
                                return;
                            }
                            AppFontManager.applyToActivity(requireActivity());
                        }
                    });
                    return true;
                }
            });
        }

        @Override
        public void onDisplayPreferenceDialog(Preference preference) {
            if (preference instanceof DeleteAllCardsPreference) {
                DialogFragment dialogFragment =
                        new DeleteAllCardsPreference.ConfirmDialogFragment();
                dialogFragment.show(this.getChildFragmentManager(),
                        "settings_dialog");
            } else {
                super.onDisplayPreferenceDialog(preference);
            }
        }
    }
}
