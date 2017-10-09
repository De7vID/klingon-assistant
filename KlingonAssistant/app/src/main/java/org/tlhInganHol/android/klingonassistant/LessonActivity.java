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

import android.os.Bundle;
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
    TabLayout tabLayout = (TabLayout) findViewById(R.id.lesson_tab_dots);
    tabLayout.setupWithViewPager(mPager, true);
    LinearLayout tabStrip = (LinearLayout) tabLayout.getChildAt(0);
    tabStrip.setEnabled(false);
    for (int i = 0; i < tabStrip.getChildCount(); i++) {
      tabStrip.getChildAt(i).setClickable(false);
    }
  }

  private void setTitle(String title) {
    // Add some spaces in front of the title to work around the difficulty of
    // setting the title margin start with the support toolbar.
    getSupportActionBar().setTitle("    " + title);
  }

  // A helper class to build a lesson.
  private class LessonBuilder {
    private String mTitle = null;
    private List<LessonFragment> mLessonFragments = null;
    private LessonFragment mLessonSummary = null;

    public LessonBuilder(String title) {
      mTitle = title;
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
              mTitle, getStringFromResId(topicResId), getStringFromResId(bodyResId)));
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
      // TODO: Make summary page dependent on prior pages.
      LessonFragment summaryFragment = LessonFragment.newInstance(mTitle, "Summary", "summary");
      // Copy the list so that summaryFragment doesn't reference itself.
      summaryFragment.setSummary(new ArrayList<LessonFragment>(mLessonFragments));
      mLessonFragments.add(summaryFragment);
      return mLessonFragments;
    }
  }

  @Override
  public void goToNextPage() {
    mPager.setCurrentItem(mPager.getCurrentItem() + 1);
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
    // The unit and lesson numbers are 1-based.
    int mUnitNumber = 1;
    int mLessonNumber = 1;
    private List<LessonFragment> mLessonFragments = null;

    public SwipeAdapter(FragmentManager fm, LessonActivity activity) {
      super(fm);

      // TODO: Initialise unit, lesson, and page number here.
      String title = getTitle(1, 1);
      activity.setTitle(title);
      ArrayList choiceList1 = new ArrayList(Arrays.asList("{Qong:v}", "{Sop:v}", "{Suv:v}"));
      ArrayList choiceList2 = new ArrayList(Arrays.asList("{Doch:n}", "{taj:n}", "{vIqraq:n}"));
      mLessonFragments =
          new LessonBuilder(title)
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

              // intro - race condition
              // .startNewPage(R.string.topic_introduction, R.string.body_introduction)

              .build();

      // TODO: Use notifyDataSetChanged to switch between lessons.
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
  }
}
