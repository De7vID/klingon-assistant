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
import java.util.List;

public class LessonActivity extends AppCompatActivity {
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

  // Swipe
  private class SwipeAdapter extends FragmentStatePagerAdapter {
    // The unit and lesson numbers are 1-based. The page number is 0-based.
    int mUnitNumber = 1;
    int mLessonNumber = 1;
    int mPageNumber = 0;
    private List<LessonFragment> lessonFragments = null;

    public SwipeAdapter(FragmentManager fm, LessonActivity activity) {
      super(fm);

      // TODO: Initialise unit, lesson, and page number here.
      activity.setTitle(getTitle(1, 1));
      lessonFragments = new ArrayList<LessonFragment>();
      switch (mUnitNumber) {
        case 1:
        default:
          lessonFragments.add(
              LessonFragment.newInstance(
                  activity, 1, 1, R.string.topic_introduction, R.string.body_introduction));
          lessonFragments.add(
              LessonFragment.newInstance(
                  activity, 1, 1, R.string.topic_basic_sentence, R.string.body_basic_sentence));
          break;
      }

      // TODO: Use notifyDataSetChanged to switch between lessons.
    }

    private String getTitle(int unit, int lesson) {
      return String.format(
          getBaseContext().getResources().getString(R.string.lesson_header), unit, lesson);
    }

    private String getTopic(int id) {
      return getBaseContext().getResources().getString(id);
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
