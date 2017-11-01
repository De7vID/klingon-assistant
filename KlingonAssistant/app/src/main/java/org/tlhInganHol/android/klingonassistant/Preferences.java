/*
 * Copyright (C) 2014 De'vID jonpIn (David Yonge-Mallo)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tlhInganHol.android.klingonassistant;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.ViewGroup;
import java.util.Locale;

public class Preferences extends AppCompatPreferenceActivity
    implements OnSharedPreferenceChangeListener {

  // Tutorial preferences.
  // public static final String KEY_RUN_TUTORIAL_CHECKBOX_PREFERENCE =
  // "run_tutorial_checkbox_preference";

  // Language preferences.
  private static final String KEY_KLINGON_UI_CHECKBOX_PREFERENCE = "klingon_ui_checkbox_preference";
  private static final String KEY_KLINGON_FONT_LIST_PREFERENCE = "klingon_font_list_preference";
  private static final String KEY_LANGUAGE_DEFAULT_ALREADY_SET = "language_default_already_set";
  public static final String KEY_SHOW_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE =
      "show_german_definitions_checkbox_preference";
  public static final String KEY_SEARCH_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE =
      "search_german_definitions_checkbox_preference";

  // Input preferences.
  public static final String KEY_XIFAN_HOL_CHECKBOX_PREFERENCE = "xifan_hol_checkbox_preference";
  public static final String KEY_SWAP_QS_CHECKBOX_PREFERENCE = "swap_qs_checkbox_preference";

  // Social preferences.
  public static final String KEY_SOCIAL_NETWORK_LIST_PREFERENCE = "social_network_list_preference";

  // Informational preferences.
  public static final String KEY_SHOW_TRANSITIVITY_CHECKBOX_PREFERENCE =
      "show_transitivity_checkbox_preference";
  public static final String KEY_SHOW_ADDITIONAL_INFORMATION_CHECKBOX_PREFERENCE =
      "show_additional_information_checkbox_preference";

  // Under construction.
  public static final String KEY_SHOW_UNSUPPORTED_FEATURES_CHECKBOX_PREFERENCE =
      "show_unsupported_features_checkbox_preference";
  public static final String KEY_KWOTD_CHECKBOX_PREFERENCE = "kwotd_checkbox_preference";

  // Detect if the system language is German.
  public static boolean shouldPreferGerman() {
    Locale locale = KlingonAssistant.getSystemLocale();
    return locale.getLanguage().equals(Locale.GERMAN.getLanguage());
  }

  // Whether the UI (menus, hints, etc.) should be displayed in Klingon.
  public static boolean useKlingonUI(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    return sharedPrefs.getBoolean(
        Preferences.KEY_KLINGON_UI_CHECKBOX_PREFERENCE, /* default */ false);
  }

  // Whether a Klingon font should be used when display Klingon text.
  public static boolean useKlingonFont(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    String value = sharedPrefs.getString(KEY_KLINGON_FONT_LIST_PREFERENCE, /* default */ "LATIN");
    return value.equals("TNG") || value.equals("DSC");
  }

  // Whether the DSC font should be used instead of the TNG one.
  public static boolean useDSCKlingonFont(Context context) {
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(context);
    String value = sharedPrefs.getString(KEY_KLINGON_FONT_LIST_PREFERENCE, /* default */ "LATIN");
    return value.equals("DSC");
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Restore system (non-Klingon) locale.
    restoreLocaleConfiguration();

    // Set up the toolbar for an AppCompatPreferenceActivity.
    setupActionBar();

    // Load the preferences from an XML resource.
    addPreferencesFromResource(R.xml.preferences);

    // Get a reference to the {pIqaD} list preference, and apply the display option to it.
    ListPreference klingonFontListPreference =
        (ListPreference) getPreferenceScreen().findPreference(KEY_KLINGON_FONT_LIST_PREFERENCE);
    String title = klingonFontListPreference.getTitle().toString();
    SpannableString ssb;
    if (!useKlingonFont(getBaseContext())) {
      // Display in bold serif.
      ssb = new SpannableString(title);
      ssb.setSpan(
          new StyleSpan(android.graphics.Typeface.BOLD),
          0,
          ssb.length(),
          Spanned.SPAN_EXCLUSIVE_EXCLUSIVE | Spanned.SPAN_INTERMEDIATE);
      ssb.setSpan(new TypefaceSpan("serif"), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    } else {
      String klingonTitle = KlingonContentProvider.convertStringToKlingonFont(title);
      ssb = new SpannableString(klingonTitle);
      Typeface klingonTypeface = KlingonAssistant.getKlingonFontTypeface(getBaseContext());
      ssb.setSpan(
          new KlingonTypefaceSpan("", klingonTypeface),
          0,
          ssb.length(),
          Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    klingonFontListPreference.setTitle(ssb);

    // Set the defaults for the German options based on the user's language, if it hasn't been
    // already set.
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    if (!sharedPrefs.getBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, /* default */ false)) {
      CheckBoxPreference mShowGermanCheckBoxPreference =
          (CheckBoxPreference)
              getPreferenceScreen().findPreference(KEY_SHOW_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE);
      CheckBoxPreference mSearchGermanCheckBoxPreference =
          (CheckBoxPreference)
              getPreferenceScreen()
                  .findPreference(KEY_SEARCH_GERMAN_DEFINITIONS_CHECKBOX_PREFERENCE);
      mShowGermanCheckBoxPreference.setChecked(shouldPreferGerman());
      mSearchGermanCheckBoxPreference.setChecked(shouldPreferGerman());

      SharedPreferences.Editor sharedPrefsEd = sharedPrefs.edit();
      sharedPrefsEd.putBoolean(KEY_LANGUAGE_DEFAULT_ALREADY_SET, true);
      sharedPrefsEd.apply();
    }

    // TUTORIAL
    // if (KlingonAssistant.INCLUDE_TUTORIAL) {
    //   Preference tutorialPreference = findPreference(KEY_RUN_TUTORIAL_CHECKBOX_PREFERENCE);
    //   tutorialPreference.setEnabled(true);
    // }
  }

  private void restoreLocaleConfiguration() {
    // Always restore system (non-Klingon) locale here.
    Locale locale = KlingonAssistant.getSystemLocale();
    Configuration configuration = getBaseContext().getResources().getConfiguration();
    configuration.locale = locale;
    getBaseContext()
        .getResources()
        .updateConfiguration(configuration, getBaseContext().getResources().getDisplayMetrics());
  }

  private void setupActionBar() {
    // This only works in ICS (API 14) and up.
    ViewGroup root =
        (ViewGroup) findViewById(android.R.id.list).getParent().getParent().getParent();
    Toolbar toolbar =
        (Toolbar) LayoutInflater.from(this).inflate(R.layout.view_toolbar, root, false);
    root.addView(toolbar, 0);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case android.R.id.home:
        finish();
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // Restore system (non-Klingon) locale.
    restoreLocaleConfiguration();

    // Set up a listener whenever a key changes.
    getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  protected void onPause() {
    super.onPause();

    // Unregister the listener whenever a key changes.
    getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(final SharedPreferences sharedPrefs, final String key) {
    if (key.equals(KEY_KLINGON_FONT_LIST_PREFERENCE)
        || key.equals(KEY_KLINGON_UI_CHECKBOX_PREFERENCE)) {
      // User has changed the Klingon font option or UI language, display a warning.
      new AlertDialog.Builder(this)
          .setIcon(R.drawable.alert_dialog_icon)
          .setTitle(R.string.warning)
          .setMessage(R.string.change_ui_language_warning)
          .setCancelable(false) // Can't be canceled with the BACK key.
          .setPositiveButton(
              android.R.string.yes,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int whichButton) {
                  // Since the display options have changed, everything needs to be redrawn.
                  recreate();
                }
              })
          .show();
    }
  }
}
