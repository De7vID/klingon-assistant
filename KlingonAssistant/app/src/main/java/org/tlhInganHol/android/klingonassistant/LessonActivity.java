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
import android.widget.LinearLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

  // For keeping a summary of user's choices and quiz answers.
  private ArrayList<String> mSelectedChoices = new ArrayList<String>();
  private int mCorrectlyAnswered = 0;
  private int mTotalQuestions = 0;
  private static final String STATE_CORRECTLY_ANSWERED = "correctly_answered";
  private static final String STATE_TOTAL_QUESTIONS = "total_questions";
  private static final String STATE_SELECTED_CHOICES = "selected_choices";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (savedInstanceState != null) {
      mCorrectlyAnswered = savedInstanceState.getInt(STATE_CORRECTLY_ANSWERED);
      mTotalQuestions = savedInstanceState.getInt(STATE_TOTAL_QUESTIONS);
      mSelectedChoices = savedInstanceState.getStringArrayList(STATE_SELECTED_CHOICES);
    }

    setContentView(R.layout.activity_lesson);

    // Set some space between the icon and title text.
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

  private void setTitle(String title) {
    // Add some spaces in front of the title to work around the difficulty of
    // setting the title margin start with the support toolbar.
    getSupportActionBar().setTitle("    " + title);
  }

  // A helper class to build a lesson.
  private class LessonBuilder {
    private List<LessonFragment> mLessonFragments = null;

    public LessonBuilder() {
      mLessonFragments = new ArrayList<LessonFragment>();
    }

    // Helper to get string from resource ID.
    private String getStringFromResId(int resId) {
      return getBaseContext().getResources().getString(resId);
    }

    // Start a new page which only has lesson text.
    public LessonBuilder startNewPage(int topicResId, int bodyResId) {
      mLessonFragments.add(
          LessonFragment.newInstance(
              getStringFromResId(topicResId), getStringFromResId(bodyResId)));
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
      getCurrentLesson().addClosingText(getStringFromResId(body2ResId));
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

      // Intent summaryPageIntent = new Intent(this, LessonActivity.class);
      // finish();
      // startActivity(summaryPageIntent);
      mPagerAdapter = new SwipeAdapter(getSupportFragmentManager(), this);
      mPager.setAdapter(mPagerAdapter);
      // TODO: Hide tab dots, change summary buttons.
    }
  }

  private void saveProgress() {
      SharedPreferences.Editor sharedPrefsEd = PreferenceManager.getDefaultSharedPreferences(getBaseContext()).edit();
      sharedPrefsEd.putInt(Preferences.KEY_UNIT_NUMBER, mUnitNumber);
      sharedPrefsEd.putInt(Preferences.KEY_LESSON_NUMBER, mLessonNumber);
      sharedPrefsEd.putBoolean(Preferences.KEY_IS_SUMMARY_PAGE, true);
      sharedPrefsEd.apply();
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

    savedInstanceState.putInt(STATE_CORRECTLY_ANSWERED, mCorrectlyAnswered);
    savedInstanceState.putInt(STATE_TOTAL_QUESTIONS, mTotalQuestions);
    savedInstanceState.putStringArrayList(STATE_SELECTED_CHOICES, mSelectedChoices);
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
      String title = getTitle(1, 1);
      activity.setTitle(title);

      switch(mUnitNumber) {
        case 1:
        default:
          switch(mLessonNumber) {
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
      SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getBaseContext());
      mUnitNumber = sharedPrefs.getInt(Preferences.KEY_UNIT_NUMBER, /* default */ 1);
      mLessonNumber = sharedPrefs.getInt(Preferences.KEY_LESSON_NUMBER, /* default */ 1);
      mIsSummaryPage = sharedPrefs.getBoolean(Preferences.KEY_IS_SUMMARY_PAGE, /* default */ false);
    }

    private String getTitle(int unit, int lesson) {
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
      mLessonFragments = new LessonBuilder()
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
        summaryFragment.setSummary();
        mLessonFragments.add(summaryFragment);
        return;
    }
  }
}
