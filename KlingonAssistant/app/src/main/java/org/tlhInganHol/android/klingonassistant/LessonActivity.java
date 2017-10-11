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
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;

public class LessonActivity extends AppCompatActivity implements LessonFragment.Callback {
  private static final String TAG = "LessonActivity";

  private LessonViewPager mPager;
  private PagerAdapter mPagerAdapter;
  private LinearLayout mTabStrip;

  // The unit and lesson numbers are 1-based. A unit has multiple lessons.
  // There is a summary page associated with each lesson.
  int mUnitNumber = 1;
  int mLessonNumber = 1;
  boolean mIsSummaryPage = false;

  // Keys for saving lesson progress.
  public static final String KEY_UNIT_NUMBER = "unit_number";
  public static final String KEY_LESSON_NUMBER = "lesson_number";
  public static final String KEY_IS_SUMMARY_PAGE = "is_summary_page";

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
      mLessonFragments.add(
          LessonFragment.newInstance(getString(topicResId), getString(bodyResId)));
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
      mIsSummaryPage = true;
      saveProgress();
      reloadLessonActivity();
    }
  }

  @Override
  public void redoThisLesson() {
    mIsSummaryPage = false;
    mCorrectlyAnswered = 0;
    mTotalQuestions = 0;
    mSelectedChoices = new ArrayList<String>();
    saveProgress();
    reloadLessonActivity();
  }

  private void saveProgress() {
    SharedPreferences.Editor sharedPrefsEd =
        PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
    sharedPrefsEd.putInt(KEY_UNIT_NUMBER, mUnitNumber);
    sharedPrefsEd.putInt(KEY_LESSON_NUMBER, mLessonNumber);
    sharedPrefsEd.putBoolean(KEY_IS_SUMMARY_PAGE, mIsSummaryPage);
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
    // overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
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

  @Override
  public String getSummary() {
    return mSelectedChoices.size()
        + " - "
        + mSelectedChoices.toString()
        + " ; "
        + mCorrectlyAnswered
        + "/"
        + mTotalQuestions;
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);

    savedInstanceState.putInt(KEY_CORRECTLY_ANSWERED, mCorrectlyAnswered);
    savedInstanceState.putInt(KEY_TOTAL_QUESTIONS, mTotalQuestions);
    savedInstanceState.putStringArrayList(KEY_SELECTED_CHOICES, mSelectedChoices);
  }

  @Override
  public void onBackPressed() {
    if (mIsSummaryPage) {
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
      String header = getHeader(1, 1);
      activity.setHeader(header);

      switch (mUnitNumber) {
        case 1:
        default:
          switch (mLessonNumber) {
            case 1:
            default:
              if (!mIsSummaryPage) {
                Unit_1_Lesson_1();
              } else {
                Unit_1_Lesson_1_Summary();
              }
              break;
          }
          break;
      }
    }

    private void restoreProgress(LessonActivity activity) {
      SharedPreferences sharedPrefs =
          PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
      mUnitNumber = sharedPrefs.getInt(KEY_UNIT_NUMBER, /* default */ 1);
      mLessonNumber = sharedPrefs.getInt(KEY_LESSON_NUMBER, /* default */ 1);
      mIsSummaryPage = sharedPrefs.getBoolean(KEY_IS_SUMMARY_PAGE, /* default */ false);
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
    private String getHeader(int unit, int lesson) {
      return String.format(
          getBaseContext().getResources().getString(R.string.lesson_header), unit, lesson);
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
    private void Unit_1_Lesson_1() {
      ArrayList choiceList1 = new ArrayList(Arrays.asList("{Qong:v}", "{Sop:v}", "{Suv:v}"));
      ArrayList choiceList2 = new ArrayList(Arrays.asList("{Doch:n}", "{taj:n}", "{vIqraq:n}"));
      mLessonFragments =
          new LessonBuilder()
              // intro
              .startNewPage(R.string.topic_introduction, R.string.body_introduction)

              // plain list
              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addPlainList(choiceList1)
              .addClosingText(R.string.body_basic_sentence2)

              // choice
              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addMultipleChoiceSelection(choiceList1)
              .addClosingText(R.string.body_basic_sentence2)

              // quiz
              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addQuiz(choiceList1, LessonFragment.ChoiceTextType.ENTRY_NAME_ONLY)
              .addClosingText(R.string.body_basic_sentence2)

              // choice
              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addMultipleChoiceSelection(choiceList2)
              .addClosingText(R.string.body_basic_sentence2)

              // quiz
              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addQuiz(choiceList2, LessonFragment.ChoiceTextType.DEFINITION_ONLY)
              .addClosingText(R.string.body_basic_sentence2)
              .build();
    }

    private void Unit_1_Lesson_1_Summary() {
      // Fake summary.
      mLessonFragments = new ArrayList<LessonFragment>();
      LessonFragment summaryFragment = LessonFragment.newInstance("Summary", "summary");
      summaryFragment.setAsSummaryPage();
      summaryFragment.setNoMoreLessons();
      mLessonFragments.add(summaryFragment);
      return;
    }

    // TODO: Show progress tree for lesson 2 onwards.
  }
}
