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

import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class LessonFragment extends EntryFragment {

  public static LessonFragment newInstance(
      Context context, int unit, int lesson, int topic, int body) {
    LessonFragment lessonFragment = new LessonFragment();
    Bundle args = new Bundle();
    args.putString(
        "title",
        String.format(context.getResources().getString(R.string.lesson_header), unit, lesson));
    args.putString("topic", context.getResources().getString(topic));
    args.putString("body", context.getResources().getString(body));
    lessonFragment.setArguments(args);
    return lessonFragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.entry, container, false);

    Resources resources = getActivity().getResources();

    // Set up the title and body text.
    TextView entryTitle = (TextView) rootView.findViewById(R.id.entry_title);
    TextView entryBody = (TextView) rootView.findViewById(R.id.entry_body);

    entryTitle.invalidate();
    entryTitle.setText(getArguments().getString("topic"));

    entryBody.invalidate();
    String bodyText = getArguments().getString("body");
    SpannableStringBuilder ssb = new SpannableStringBuilder(bodyText);
    processMixedText(ssb, bodyText, null);
    entryBody.setText(ssb);
    entryBody.setMovementMethod(LinkMovementMethod.getInstance());

    // Set up the bottom navigation buttons.
    BottomNavigationView bottomNavView =
        (BottomNavigationView) rootView.findViewById(R.id.bottom_navigation);
    Menu bottomNavMenu = bottomNavView.getMenu();
    bottomNavView.findViewById(R.id.action_random).setVisibility(View.INVISIBLE);
    bottomNavView.findViewById(R.id.action_previous).setVisibility(View.INVISIBLE);
    bottomNavView.setOnNavigationItemSelectedListener(
        new BottomNavigationView.OnNavigationItemSelectedListener() {
          @Override
          public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
              case R.id.action_next:
                break;
            }
            return false;
          }
        });

    return rootView;
  }

  public String getStringResult() {
    return "";
  }

  public boolean getBooleanResult() {
    return false;
  }
}
