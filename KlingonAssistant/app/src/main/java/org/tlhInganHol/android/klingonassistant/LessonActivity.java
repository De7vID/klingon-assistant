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

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

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
    private List<LessonFragment> lessonFragments = null;
    private String mTitle = null;

    public LessonBuilder(String title) {
      mTitle = title;
      lessonFragments = new ArrayList<LessonFragment>();
    }

    // Helper to get string from resource ID.
    private String getStringFromResId(int resId) {
      return getBaseContext().getResources().getString(resId);
    }

    // Start a new page which only has lesson text.
    public LessonBuilder startNewPage(int topicResId, int bodyResId) {
      lessonFragments.add(
          LessonFragment.newInstance(
              mTitle, getStringFromResId(topicResId), getStringFromResId(bodyResId)));
      return this;
    }

    // Helper to get the lesson currently being built.
    private LessonFragment getCurrentLesson() {
      if (lessonFragments.size() == 0) {
        // Log.e();
        return null;
      }
      return lessonFragments.get(lessonFragments.size() - 1);
    }

    // Add a page which allows the user to select from multiple choices.
    public LessonBuilder addMultipleChoiceSelection(List<String> entries) {
      getCurrentLesson().addMultipleChoiceSelection(entries);
      return this;
    }

    // Add text after other sections.
    public LessonBuilder addClosingText(int body2ResId) {
      getCurrentLesson().addClosingText(getStringFromResId(body2ResId));
      return this;
    }

    public List<LessonFragment> build() {
      // TODO: Add summary page.
      return lessonFragments;
    }
  }

  @Override
  public void goToNextPage() {
    mPager.setCurrentItem(mPager.getCurrentItem() + 1);
  }

  // Swipe
  private class SwipeAdapter extends FragmentStatePagerAdapter {
    // The unit and lesson numbers are 1-based.
    int mUnitNumber = 1;
    int mLessonNumber = 1;
    private List<LessonFragment> lessonFragments = null;

    public SwipeAdapter(FragmentManager fm, LessonActivity activity) {
      super(fm);

      // TODO: Initialise unit, lesson, and page number here.
      String title = getTitle(1, 1);
      activity.setTitle(title);
      lessonFragments =
          new LessonBuilder(title)

              .startNewPage(R.string.topic_introduction, R.string.body_introduction)

              .startNewPage(R.string.topic_basic_sentence, R.string.body_basic_sentence)
              .addMultipleChoiceSelection(Arrays.asList("{Qong:v}", "{Sop:v}"))
              .addClosingText(R.string.body_basic_sentence2)

              .build();

      // TODO: Use notifyDataSetChanged to switch between lessons.
    }

    private String getTitle(int unit, int lesson) {
      return String.format(
          getBaseContext().getResources().getString(R.string.lesson_header), unit, lesson);
    }

    @Override
    public Fragment getItem(int position) {
      return lessonFragments.get(position);
    }

    @Override
    public int getCount() {
      return lessonFragments.size();
    }
  }
}
