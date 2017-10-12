/*
 * Copyright (C) 2018 De'vID jonpIn (David Yonge-Mallo)
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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONException;

public class LessonActivity extends AppCompatActivity implements LessonFragment.Callback, TextToSpeech.OnInitListener {
  private static final String TAG = "LessonActivity";

  // TTS:
  // All TTS operations are identical to the ones in EntryActivity.
  private TextToSpeech mTts = null;
  private boolean ttsInitialized = false;

  // For going between lesson pages.
  private LessonViewPager mPager;
  private PagerAdapter mPagerAdapter;
  private LinearLayout mTabStrip;

  // The unit and lesson numbers are 1-based. A unit has multiple lessons.
  // There is a summary page associated with each lesson.
  private int mUnitNumber = 1;
  private int mLessonNumber = 1;
  private int mSectionNumber = 1;
  private boolean mShowSummary = false;

  // Keys for saving lesson progress.
  public static final String KEY_UNIT_NUMBER = "unit_number";
  public static final String KEY_LESSON_NUMBER = "lesson_number";
  public static final String KEY_SECTION_NUMBER = "section_number";
  public static final String KEY_SHOW_SUMMARY = "show_summary";

  // For keeping a summary of user's choices and quiz answers.
  private ArrayList<String> mSelectedChoices = new ArrayList<String>();
  private int mCorrectlyAnswered = 0;
  private int mTotalQuestions = 0;

  // Keys for saving and restoring summary page.
  private static final String KEY_CORRECTLY_ANSWERED = "correctly_answered";
  private static final String KEY_TOTAL_QUESTIONS = "total_questions";
  private static final String KEY_SELECTED_CHOICES = "selected_choices";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // TTS:
    clearTTS();
    mTts =
        new TextToSpeech(
            this,
            this, // TextToSpeech.OnInitListener
            "org.tlhInganHol.android.klingonttsengine");

    if (savedInstanceState != null) {
      mCorrectlyAnswered = savedInstanceState.getInt(KEY_CORRECTLY_ANSWERED);
      mTotalQuestions = savedInstanceState.getInt(KEY_TOTAL_QUESTIONS);
      mSelectedChoices = savedInstanceState.getStringArrayList(KEY_SELECTED_CHOICES);
    }

    setContentView(R.layout.activity_lesson);

    // Set some space between the icon and header text.
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    // Display icon.
    getSupportActionBar().setIcon(R.drawable.ic_ka);

    // Set up pager.
    mPager = (LessonViewPager) findViewById(R.id.lesson_pager);
    mPagerAdapter = new SwipeAdapter(getSupportFragmentManager(), this);
    mPager.setAdapter(mPagerAdapter);
    mPager.setCurrentItem(0, /* smoothScroll */ false);
    if (mPagerAdapter.getCount() > 1) {
      TabLayout tabLayout = (TabLayout) findViewById(R.id.lesson_tab_dots);
      tabLayout.setupWithViewPager(mPager, true);
      LinearLayout mTabStrip = (LinearLayout) tabLayout.getChildAt(0);
      mTabStrip.setEnabled(false);
      for (int i = 0; i < mTabStrip.getChildCount(); i++) {
        mTabStrip.getChildAt(i).setClickable(false);
      }
    }
  }

  private void setHeader(String header) {
    // Add some spaces in front of the header to work around the difficulty of
    // setting the header margin start with the support toolbar.
    getSupportActionBar().setTitle("    " + header);
  }

  // A helper class to build a lesson.
  private class LessonBuilder {
    private List<LessonFragment> mLessonFragments = null;

    public LessonBuilder() {
      mLessonFragments = new ArrayList<LessonFragment>();
    }

    // Start a new page which only has lesson text.
    public LessonBuilder startNewPage(int topicResId, int bodyResId) {
      mLessonFragments.add(LessonFragment.newInstance(getString(topicResId), getString(bodyResId)));
      return this;
    }

    // Helper to get the lesson currently being built.
    private LessonFragment getCurrentLesson() {
      if (mLessonFragments.size() == 0) {
        // Log.e();
        return null;
      }
      return mLessonFragments.get(mLessonFragments.size() - 1);
    }

    // TODO: Add a method for adding a reading and/or listening section.
    // public LessonBuilder addReadingAndListening(boolean showText, boolean showTTS) {
    //   return this;
    // }

    // Add a plain list.
    public LessonBuilder addPlainList(ArrayList<String> entries) {
      getCurrentLesson().addPlainList(entries);
      return this;
    }

    // Add a page which allows the user to select from multiple choices.
    public LessonBuilder addMultipleChoiceSelection(ArrayList<String> entries) {
      getCurrentLesson().addMultipleChoiceSelection(entries);
      return this;
    }

    // Add a quiz.
    public LessonBuilder addQuiz(
        ArrayList<String> entries, LessonFragment.ChoiceTextType choiceTextType) {
      // TODO: Allow "none/all of the above" options.
      getCurrentLesson().addQuiz(entries, choiceTextType);
      return this;
    }

    // Add text after other sections.
    public LessonBuilder addClosingText(int body2ResId) {
      getCurrentLesson().addClosingText(getString(body2ResId));
      return this;
    }

    public List<LessonFragment> build() {
      return mLessonFragments;
    }
  }

  @Override
  protected void onResume() {
    super.onResume();

    // TTS:
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
    clearTTS();
    super.onDestroy();
  }

  public void speakSentence(String sentence) {
    if (!ttsInitialized) {
      try {
        launchExternal("market://details?id=org.tlhInganHol.android.klingonttsengine");
      } catch (android.content.ActivityNotFoundException e) {
        // Fall back to browser.
        launchExternal(
            "https://play.google.com/store/apps/details?id=org.tlhInganHol.android.klingonttsengine");
      }
    } else {
      mTts.speak(sentence, TextToSpeech.QUEUE_FLUSH, null);
    }
  }

  // Method to launch an external app or web site.
  // See identical method in BaseActivity.
  private void launchExternal(String externalUrl) {
    Intent intent = new Intent(Intent.ACTION_VIEW);
    // Set NEW_TASK so the external app or web site is independent.
    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    intent.setData(Uri.parse(externalUrl));
    startActivity(intent);
  }

  // TTS:
  // Implements TextToSpeech.OnInitListener.
  // See comments in EntryActivity.
  @Override
  public void onInit(int status) {
    if (status == TextToSpeech.SUCCESS) {
      int result = mTts.setLanguage(new Locale("tlh", "", ""));
      if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
        // Lanuage data is missing or the language is not supported.
        Log.e(TAG, "Language is not available.");
      } else {
        // The TTS engine has been successfully initialized.
        ttsInitialized = true;
      }
    } else {
      // Initialization failed.
      Log.e(TAG, "Could not initialize TextToSpeech.");
    }
  }

  @Override
  public void goToNextPage() {
    int currentItem = mPager.getCurrentItem();
    if (currentItem != mPagerAdapter.getCount() - 1) {
      // Go to the next page of the current lesson.
      mPager.setCurrentItem(currentItem + 1);
    } else {
      // Going to the next page from the last page of a lesson results in going
      // to the summary page. The summary page cannot be in the same ViewPager
      // as the lesson itself, since the ViewPager pre-loads fragments. This
      // means that the summary page will not have access to results of the
      // user's actions on the page just prior to it.
      mShowSummary = true;
      saveProgress();
      reloadLessonActivity();
    }
  }

  @Override
  public void redoSection() {
    mShowSummary = false;
    mCorrectlyAnswered = 0;
    mTotalQuestions = 0;
    mSelectedChoices = new ArrayList<String>();
    saveProgress();
    reloadLessonActivity();
  }

  private void saveProgress() {
    SharedPreferences.Editor sharedPrefsEd =
        PreferenceManager.getDefaultSharedPreferences(this).edit();
    sharedPrefsEd.putInt(KEY_UNIT_NUMBER, mUnitNumber);
    sharedPrefsEd.putInt(KEY_LESSON_NUMBER, mLessonNumber);
    sharedPrefsEd.putInt(KEY_SECTION_NUMBER, mSectionNumber);
    sharedPrefsEd.putBoolean(KEY_SHOW_SUMMARY, mShowSummary);
    sharedPrefsEd.putInt(KEY_CORRECTLY_ANSWERED, mCorrectlyAnswered);
    sharedPrefsEd.putInt(KEY_TOTAL_QUESTIONS, mTotalQuestions);

    JSONArray choicesArray = new JSONArray();
    for (int i = 0; i < mSelectedChoices.size(); i++) {
      choicesArray.put(mSelectedChoices.get(i));
    }
    if (!mSelectedChoices.isEmpty()) {
      sharedPrefsEd.putString(KEY_SELECTED_CHOICES, choicesArray.toString());
    } else {
      sharedPrefsEd.putString(KEY_SELECTED_CHOICES, null);
    }

    sharedPrefsEd.apply();
  }

  // Helper method to finish and restart this activity due to lesson progress
  // having changed state. Usually called after saveProgress().
  private void reloadLessonActivity() {
    Intent intent = new Intent(this, LessonActivity.class);
    finish();
    startActivity(intent);
  }

  @Override
  public void commitChoice(String choice) {
    mSelectedChoices.add(choice);
    // Log.d(TAG, "commitChoice: " + choice);
    // Log.d(TAG, "getSummary(): " + getSummary());
  }

  @Override
  public void scoreQuiz(boolean correctlyAnswered) {
    mTotalQuestions++;
    if (correctlyAnswered) {
      mCorrectlyAnswered++;
    }
    // Log.d(TAG, "scoreQuiz: " + correctlyAnswered);
    // Log.d(TAG, "getSummary(): " + getSummary());
  }

  // private String getSummary() {
  //   return mSelectedChoices.size()
  //       + " - "
  //       + mSelectedChoices.toString()
  //       + " ; "
  //       + mCorrectlyAnswered
  //       + "/"
  //       + mTotalQuestions;
  // }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);

    savedInstanceState.putInt(KEY_CORRECTLY_ANSWERED, mCorrectlyAnswered);
    savedInstanceState.putInt(KEY_TOTAL_QUESTIONS, mTotalQuestions);
    savedInstanceState.putStringArrayList(KEY_SELECTED_CHOICES, mSelectedChoices);
  }

  @Override
  public void onBackPressed() {
    if (mShowSummary) {
      // Don't intercept "Back" on the summary page.
      super.onBackPressed();
      return;
    }

    AlertDialog.Builder builder = new AlertDialog.Builder(this);
    builder
        .setMessage(getResources().getString(R.string.exit_lesson_confirmation))
        .setCancelable(false)
        .setPositiveButton(
            getResources().getString(R.string.exit_lesson_yes),
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                LessonActivity.this.finish();
              }
            })
        .setNegativeButton(
            getResources().getString(R.string.exit_lesson_no),
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            });
    AlertDialog alert = builder.create();
    alert.show();
  }

  // Swipe
  private class SwipeAdapter extends FragmentStatePagerAdapter {
    private List<LessonFragment> mLessonFragments = null;

    // For now, use a "summary" variable to simulate requesting a summary.
    public SwipeAdapter(FragmentManager fm, LessonActivity activity) {
      super(fm);

      // Restore progress from storage.
      restoreProgress(activity);

      // TODO: Switch on unit and lesson numbers here.
      String header = getHeader(mUnitNumber, mLessonNumber, mSectionNumber);
      activity.setHeader(header);

      switch (mUnitNumber) {
        case 1:
        default:
          Unit_1(mLessonNumber, mSectionNumber);
          break;
      }
    }

    private void restoreProgress(LessonActivity activity) {
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity);
      mUnitNumber = sharedPrefs.getInt(KEY_UNIT_NUMBER, /* default */ 1);
      mLessonNumber = sharedPrefs.getInt(KEY_LESSON_NUMBER, /* default */ 1);
      mSectionNumber = sharedPrefs.getInt(KEY_SECTION_NUMBER, /* default */ 1);
      mShowSummary = sharedPrefs.getBoolean(KEY_SHOW_SUMMARY, /* default */ false);
      mCorrectlyAnswered = sharedPrefs.getInt(KEY_CORRECTLY_ANSWERED, 0);
      mTotalQuestions = sharedPrefs.getInt(KEY_TOTAL_QUESTIONS, 0);

      String json = sharedPrefs.getString(KEY_SELECTED_CHOICES, null);
      mSelectedChoices = new ArrayList<String>();
      if (json != null) {
        try {
          JSONArray choicesArray = new JSONArray(json);
          for (int i = 0; i < choicesArray.length(); i++) {
            String s = choicesArray.optString(i);
            mSelectedChoices.add(s);
          }
        } catch (JSONException e) {
          mSelectedChoices = null;
        }
      }
    }

    // Helper method to get the header for the toolbar.
    private String getHeader(int unit, int lesson, int section) {
      return String.format(getResources().getString(R.string.lesson_header), unit, lesson, section);
    }

    @Override
    public Fragment getItem(int position) {
      return mLessonFragments.get(position);
    }

    @Override
    public int getCount() {
      return mLessonFragments.size();
    }

    // The layout of all the lessons are defined below.
    private void Unit_1(int lesson, int section) {
      switch (lesson) {
        case 1:
        default:
          Unit_1_Lesson_1(section);
          break;
      }
    }

    private void Unit_1_Lesson_1(int section) {
      switch (section) {
        case 1:
        default:
          Unit_1_Lesson_1_1();
          break;
      }
    }

    // Unit 1, Lesson 1.1
    // The Basic Sentence
    private void Unit_1_Lesson_1_1() {
      ArrayList someVerbs =
          new ArrayList(Arrays.asList("{Qong:v}", "{Sop:v}", "{HIv:v}", "{legh:v}", "{yaj:v}"));
      // ArrayList choiceList2 = new ArrayList(Arrays.asList("{Doch:n}", "{taj:n}", "{vIqraq:n}"));

      if (!mShowSummary) {
        mLessonFragments =
            new LessonBuilder()
                .startNewPage(R.string.topic_introduction, R.string.body_introduction)
                .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence_1)
                .addPlainList(someVerbs)
                .addClosingText(R.string.body_basic_sentence_2)
                .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence_3)
                .addMultipleChoiceSelection(someVerbs)

                // // quiz
                // .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence1)
                // .addQuiz(someVerbs, LessonFragment.ChoiceTextType.ENTRY_NAME_ONLY)
                // .addClosingText(R.string.body_basic_sentence2)

                // // choice
                // .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence1)
                // .addMultipleChoiceSelection(choiceList2)
                // .addClosingText(R.string.body_basic_sentence2)

                // // quiz
                // .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence1)
                // .addQuiz(choiceList2, LessonFragment.ChoiceTextType.DEFINITION_ONLY)
                // .addClosingText(R.string.body_basic_sentence2)
                .build();
      } else {
        mLessonFragments = new ArrayList<LessonFragment>();
        String verb = mSelectedChoices.get(0);
        String[] translations =
            getResources().getStringArray(R.array.translation_your_first_sentence);
        String translation = translations[0];
        for (int i = 1; i < translations.length; i++) {
          if (verb.equals(someVerbs.get(i))) {
            translation = translations[i];
          }
        }
        // Strip "{" from front and ":v}" from end.
        String specialSentence = verb.substring(1, verb.length() - 3);
        String summaryBody =
            getString(
                R.string.body_your_first_sentence,
                new Object[] {"{" + specialSentence + ".:sen}", translation});

        LessonFragment summaryFragment =
            LessonFragment.newInstance(getString(R.string.topic_your_first_sentence), summaryBody);
        summaryFragment.setAsSummaryPage();
        summaryFragment.setSpecialSentence(specialSentence);
        summaryFragment.setCannotContinue();
        mLessonFragments.add(summaryFragment);
        // TODO: Show progress tree for lesson 2 onwards.
      }
    }
  }
}
