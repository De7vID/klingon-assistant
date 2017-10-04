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

import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.MenuItemCompat;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.widget.ShareActionProvider;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/** Displays an entry and its definition. */
public class EntryActivity extends BaseActivity
    // TTS:
    implements TextToSpeech.OnInitListener {

  private static final String TAG = "EntryActivity";

  // The name of this entry.
  private String mEntryName = null;

  // The parent query that this entry is a part of.
  // private String mParentQuery = null;

  // The intent holding the data to be shared, and the associated UI and action provider.
  private Intent mShareEntryIntent = null;
  MenuItem mShareButton = null;
  ShareActionProvider mShareActionProvider;

  // TTS:
  /** The {@link TextToSpeech} used for speaking. */
  private TextToSpeech mTts = null;

  private MenuItem mSpeakButton;
  private boolean ttsInitialized = false;

  // Handle swipe. The pager widget handles animation and allows swiping
  // horizontally. The pager adapter provides the pages to the pager widget.
  private ViewPager mPager;
  private PagerAdapter mPagerAdapter;
  private int mEntryIndex = -1;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // TTS:
    // Initialize text-to-speech. This is an asynchronous operation.
    // The OnInitListener (second argument) is called after initialization completes.
    // Log.d(TAG, "Initialising TTS");
    clearTTS();
    mTts =
        new TextToSpeech(
            this,
            this, // TextToSpeech.OnInitListener
            "org.tlhInganHol.android.klingonttsengine"); // Requires API 14.

    setDrawerContentView(R.layout.entry_swipe);

    Uri inputUri = getIntent().getData();
    // Log.d(TAG, "EntryActivity - inputUri: " + inputUri.toString());
    // TODO: Disable the "About" menu item if this is the "About" entry.
    // mParentQuery = getIntent().getStringExtra(SearchManager.QUERY);

    // Determine whether we're launching a single entry, or a list of entries.
    // If it's a single entry, the URI will end in "get_entry_by_id/" followed
    // by the one ID. In the case of a list, the URI will end in "get_entry_by_id/"
    // follwoed by a list of comma-separated IDs, with one additional item at the
    // end for the position of the current entry. For a random entry, the URI will
    // end in "get_random_entry", with no ID at all.
    String[] ids = inputUri.getLastPathSegment().split(",");
    Uri queryUri = null;
    List<String> entryIdsList = new ArrayList<String>(Arrays.asList(ids));
    if (entryIdsList.size() == 1) {
      // There is only one entry to display. Either its ID was explicitly
      // given, or we want a random entry.
      queryUri = inputUri;
      mEntryIndex = 0;
    } else {
      // Parse the comma-separated list, the last entry of which is the
      // position index. We nees to construct the queryUri based on the
      // intended current entry.
      mEntryIndex = Integer.parseInt(entryIdsList.get(ids.length - 1));
      entryIdsList.remove(ids.length - 1);
      queryUri =
          Uri.parse(
              KlingonContentProvider.CONTENT_URI
                  + "/get_entry_by_id/"
                  + entryIdsList.get(mEntryIndex));
    }

    // Retrieve the entry's data.
    // Note: managedQuery is deprecated since API 11.
    Cursor cursor = managedQuery(queryUri, KlingonContentDatabase.ALL_KEYS, null, null, null);
    final KlingonContentProvider.Entry entry =
        new KlingonContentProvider.Entry(cursor, getBaseContext());
    int entryId = entry.getId();
    mEntryName = entry.getEntryName();
    if (entryIdsList.size() == 1 && entryIdsList.get(0).equals("get_random_entry")) {
      // For a random entry, replace "get_random_entry" with the ID of randomly
      // chosen entry.
      entryIdsList.clear();
      entryIdsList.add(Integer.toString(entryId));
    }

    // Set the share intent.
    setShareEntryIntent(entry);

    // Instantiate a ViewPager and a PagerAdapter.
    mPager = (ViewPager) findViewById(R.id.entry_pager);
    mPagerAdapter = new SwipeAdapter(getSupportFragmentManager(), entryIdsList);
    mPager.setAdapter(mPagerAdapter);
    mPager.setCurrentItem(mEntryIndex, /* smoothScroll */ false);
    mPager.setOnPageChangeListener(new SwipePageChangeListener(entryIdsList));

    // Don't display the tab dots if there's only one entry, or if there are 25
    // or more (at which point the dots become not that useful). Note that the
    // entry with the most components at the moment ({cheqotlhchugh...}) has
    // 22 components. The category with most entries is 32.
    if (entryIdsList.size() > 1 && entryIdsList.size() < 35) {
      TabLayout tabLayout = (TabLayout) findViewById(R.id.entry_tab_dots);
      tabLayout.setupWithViewPager(mPager, true);
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // TTS:
    // Initialize text-to-speech. This is an asynchronous operation.
    // The OnInitListener (second argument) is called after initialization completes.
    // This is needed in onResume because we send the user to the Google Play Store to
    // install the TTS engine if it isn't already installed, so the status of the TTS
    // engine may change when this app resumes.
    // Log.d(TAG, "Initialising TTS");
    clearTTS();
    mTts =
        new TextToSpeech(
            this,
            this, // TextToSpeech.OnInitListener
            "org.tlhInganHol.android.klingonttsengine"); // Requires API 14.
  }

  private void clearTTS() {
    if (mTts != null) {
      mTts.stop();
      mTts.shutdown();
    }
  }

  @Override
  protected void onDestroy() {
    // TTS:
    // Don't forget to shutdown!
    // Log.d(TAG, "Shutting down TTS");
    clearTTS();
    super.onDestroy();
  }

  /*
   * TODO: Override onSave/RestoreInstanceState, onPause/Resume/Stop, to re-create links.
   *
   * public onSaveInstanceState() { // Save the text and views here. super.onSaveInstanceState(); }
   * public onRestoreInstanceState() { // Restore the text and views here.
   * super.onRestoreInstanceState(); }
   */

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    super.onCreateOptionsMenu(menu);
    mShareButton = menu.findItem(R.id.share);
    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(mShareButton);

    if (mShareActionProvider != null && mShareEntryIntent != null) {
      // Enable "Share" button.
      mShareActionProvider.setShareIntent(mShareEntryIntent);
      mShareButton.setVisible(true);
    }

    // TTS:
    // The button is disabled in the layout. It should only be enabled in EntryActivity.
    mSpeakButton = menu.findItem(R.id.speak);
    // if (ttsInitialized) {
    //   // Log.d(TAG, "enabling TTS button in onCreateOptionsMenu");
    mSpeakButton.setVisible(true);
    // }

    return true;
  }

  // Set the share intent for this entry.
  private void setShareEntryIntent(KlingonContentProvider.Entry entry) {
    if (entry.isAlternativeSpelling()) {
      // Disable sharing alternative spelling entries.
      mShareEntryIntent = null;
      return;
    }

    Resources resources = getResources();
    mShareEntryIntent = new Intent(Intent.ACTION_SEND);
    mShareEntryIntent.putExtra(Intent.EXTRA_TITLE, resources.getString(R.string.share_popup_title));
    mShareEntryIntent.setType("text/plain");
    String subject = "{" + entry.getFormattedEntryName(/* isHtml */ false) + "}";
    mShareEntryIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    String snippet = subject + "\n" + entry.getFormattedDefinition(/* isHtml */ false);
    mShareEntryIntent.putExtra(
        Intent.EXTRA_TEXT, snippet + "\n\n" + resources.getString(R.string.shared_from));
  }

  @Override
  public void onBackPressed() {
    super.onBackPressed();
    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.speak) {
      // TTS:
      if (!ttsInitialized) {
        // The TTS engine is not installed (or disabled). Send user to Google Play Store or other
        // market.
        try {
          launchExternal("market://details?id=org.tlhInganHol.android.klingonttsengine");
        } catch (android.content.ActivityNotFoundException e) {
          // Fall back to browser.
          launchExternal(
              "https://play.google.com/store/apps/details?id=org.tlhInganHol.android.klingonttsengine");
        }
      } else if (mEntryName != null) {
        // The TTS engine is working, and there's something to say, say it.
        // Log.d(TAG, "Speaking");
        // Toast.makeText(getBaseContext(), mEntryName, Toast.LENGTH_LONG).show();
        mTts.speak(mEntryName, TextToSpeech.QUEUE_FLUSH, null);
      }
    }
    return super.onOptionsItemSelected(item);
  }

  // TTS:
  // Implements TextToSpeech.OnInitListener.
  public void onInit(int status) {
    // status can be either TextToSpeech.SUCCESS or TextToSpeech.ERROR.
    if (status == TextToSpeech.SUCCESS) {
      // Set preferred language to Canadian Klingon.
      // Note that a language may not be available, and the result will indicate this.
      int result = mTts.setLanguage(new Locale("tlh", "", ""));
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Lanuage data is missing or the language is not supported.
        Log.e(TAG, "Language is not available.");
      } else {
        // Check the documentation for other possible result codes.
        // For example, the language may be available for the locale,
        // but not for the specified country and variant.

        // The TTS engine has been successfully initialized.
        ttsInitialized = true;
        // if (mSpeakButton != null) {
        //   // Log.d(TAG, "enabling TTS button in onInit");
        //   mSpeakButton.setVisible(true);
        // }
      }
    } else {
      // Initialization failed.
      Log.e(TAG, "Could not initialize TextToSpeech.");
    }
  }

  // Swipe
  private class SwipeAdapter extends FragmentStatePagerAdapter {
    private List<EntryFragment> entryFragments = null;

    public SwipeAdapter(FragmentManager fm, List<String> entryIdsList) {
      super(fm);

      // Set up all of the entry fragments.
      entryFragments = new ArrayList<EntryFragment>();
      for (int i = 0; i < entryIdsList.size(); i++) {
        Uri uri =
            Uri.parse(
                KlingonContentProvider.CONTENT_URI + "/get_entry_by_id/" + entryIdsList.get(i));
        entryFragments.add(EntryFragment.newInstance(uri));
      }
    }

    @Override
    public Fragment getItem(int position) {
      return entryFragments.get(position);
    }

    @Override
    public int getCount() {
      return entryFragments.size();
    }
  }

  private class SwipePageChangeListener implements ViewPager.OnPageChangeListener {
    List<String> mEntryIdsList = null;

    public SwipePageChangeListener(List<String> entryIdsList) {
      mEntryIdsList = entryIdsList;
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}

    @Override
    public void onPageSelected(int position) {
      Uri uri =
          Uri.parse(
              KlingonContentProvider.CONTENT_URI
                  + "/get_entry_by_id/"
                  + mEntryIdsList.get(position));

      // Note: managedQuery is deprecated since API 11.
      Cursor cursor = managedQuery(uri, KlingonContentDatabase.ALL_KEYS, null, null, null);
      final KlingonContentProvider.Entry entry =
          new KlingonContentProvider.Entry(cursor, getBaseContext());
      int entryId = entry.getId();

      // Update the entry name (used for TTS output).
      mEntryName = entry.getEntryName();

      // Update share menu and set the visibility of the share button.
      setShareEntryIntent(entry);
      if (mShareActionProvider != null && mShareEntryIntent != null) {
        // Enable "Share" button. Note that mShareButton can be null if the device has been rotated.
        mShareActionProvider.setShareIntent(mShareEntryIntent);
        if (mShareButton != null) {
          mShareButton.setVisible(true);
        }
      } else {
        // Disable "Share" button.
        if (mShareButton != null) {
          mShareButton.setVisible(false);
        }
      }
    }

    @Override
    public void onPageScrollStateChanged(int state) {}
  }
}
