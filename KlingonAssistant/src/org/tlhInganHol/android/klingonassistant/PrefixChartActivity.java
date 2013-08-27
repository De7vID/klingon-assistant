/*
 * Copyright (C) 2013 De'vID jonpIn (David Yonge-Mallo)
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

import java.util.regex.Matcher;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.widget.ShareActionProvider;

/**
 * Displays an entry and its definition.
 */
public class PrefixChartActivity extends BaseActivity {
  // private static final String TAG = "PrefixChartActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
    Resources resources = getResources();

    if (sharedPrefs.getBoolean(Preferences.KEY_KLINGON_UI_CHECKBOX_PREFERENCE, /* default */false)) {
      setDrawerContentView(R.layout.prefix_charts_tlh);
    } else {
      setDrawerContentView(R.layout.prefix_charts);
    }
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
  }
}
