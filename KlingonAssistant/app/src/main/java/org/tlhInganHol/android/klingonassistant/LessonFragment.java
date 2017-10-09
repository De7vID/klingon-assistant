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

import android.app.Activity;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LessonFragment extends EntryFragment {

  // Interface to report feedback to the LessonActivity.
  public interface Callback {
    void goToNextPage();
  }

  private Callback mCallback;

  // Choices section.
  private List<String> mChoices = null;
  private String mChoice = null;
  private String mCorrectAnswer = null;

  private enum ChoiceType {
    // The "choices" radio group can be used for different things.
    // NONE means it's not displayed at all. PLAIN_LIST means it's just a list,
    // with no radio buttons. SELECTION and QUIZ will both display radio buttons,
    // but QUIZ will randomize the list order.
    NONE,
    PLAIN_LIST,
    SELECTION,
    QUIZ
  }

  private ChoiceType mChoiceType = ChoiceType.NONE;

  private enum ChoiceTextType {
    // By default, entries will show both entry name and definition. For QUIZ
    // choices, entries may only show one or the other.
    BOTH,
    ENTRY_NAME_ONLY,
    DEFINITION_ONLY
  }

  private ChoiceTextType mChoiceTextType = ChoiceTextType.BOTH;

  // Dimensions for list items in px.
  private static final float LEFT_RIGHT_MARGINS = 15.0f;
  private static final float TOP_BOTTOM_MARGINS = 6.0f;

  // Closing text section.
  private String mClosingText = null;

  // For the summary page.
  private boolean isSummary = false;
  List<LessonFragment> mLessonFragments = null;

  public static LessonFragment newInstance(String title, String topic, String body) {
    LessonFragment lessonFragment = new LessonFragment();
    Bundle args = new Bundle();
    args.putString("title", title);
    args.putString("topic", topic);
    args.putString("body", body);
    lessonFragment.setArguments(args);
    return lessonFragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.lesson, container, false);

    Resources resources = getActivity().getResources();

    // Set up the title and body text.
    TextView lessonTitle = (TextView) rootView.findViewById(R.id.lesson_title);
    TextView lessonBody = (TextView) rootView.findViewById(R.id.lesson_body);

    lessonTitle.invalidate();
    lessonTitle.setText(getArguments().getString("topic"));

    lessonBody.invalidate();
    if (!isSummary) {
      String bodyText = getArguments().getString("body");
      SpannableStringBuilder ssb = new SpannableStringBuilder(bodyText);
      processMixedText(ssb, null);
      // We don't call setMovementMethod on lessonBody, since we disable all
      // entry links.
      lessonBody.setText(ssb);
    } else {
      // TODO: Fix race condition.
      List<String> choices = new ArrayList<String>();
      int totalQuestions = 0;
      int correctlyAnswered = 0;
      for (LessonFragment lessonFragment : mLessonFragments) {
        if (lessonFragment.isSelection()) {
          choices.add(lessonFragment.getChoice());
        } else if (lessonFragment.isQuiz()) {
          if (lessonFragment.hasCorrectAnswer()) {
            correctlyAnswered++;
          }
          totalQuestions++;
        }
        lessonBody.setText(choices.size() + " - " + choices.toString() + " ; " + correctlyAnswered + "/" + totalQuestions);
      }
    }

    // Set up the bottom navigation buttons. By default, enable just the "Next"
    // button.
    // TODO: Replace this with a regular button.
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
                mCallback.goToNextPage();
                break;
            }
            return false;
          }
        });

    // Set up possible additional views.
    setupChoicesGroup(rootView, bottomNavView);

    // Put additional text after other sections.
    setupClosingText(rootView);

    return rootView;
  }

  private void setupChoicesGroup(View rootView, BottomNavigationView bottomNavView) {
    // Compute margins in dp.
    final float scale = getActivity().getResources().getDisplayMetrics().density;
    final int leftRightMargins = (int) (LEFT_RIGHT_MARGINS * scale + 0.5f);
    final int topBottomMargins = (int) (TOP_BOTTOM_MARGINS * scale + 0.5f);
    final LessonFragment thisLesson = this;

    if (mChoiceType != ChoiceType.NONE && mChoices != null) {
      RadioGroup choicesGroup = (RadioGroup) rootView.findViewById(R.id.choices);
      final MenuItem nextButton = (MenuItem) bottomNavView.getMenu().findItem(R.id.action_next);
      for (int i = 0; i < mChoices.size(); i++) {
        RadioButton choiceButton = new RadioButton(getActivity());
        choiceButton.setPadding(
            leftRightMargins, topBottomMargins, leftRightMargins, topBottomMargins);

        // Update the choice when clicked.
        final String choice = mChoices.get(i);
        choiceButton.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            thisLesson.setChoice(choice);
            nextButton.setEnabled(true);
          }
        });

        // For a plain list, hide the radio button and just show the text.
        if (mChoiceType == ChoiceType.PLAIN_LIST) {
          choiceButton.setButtonDrawable(android.R.color.transparent);
        }

        // Display entry name and/or definition depending on choice text type,
        // and also format the displayed text.
        SpannableStringBuilder choiceText = processChoiceText(mChoices.get(i));
        processMixedText(choiceText, null);
        choiceButton.setText(choiceText);
        choicesGroup.addView(choiceButton);
      }
      choicesGroup.setVisibility(View.VISIBLE);
      choicesGroup.invalidate();
      // TODO: Fix selector font size, colours.
      if (mChoiceType == ChoiceType.SELECTION || mChoiceType == ChoiceType.QUIZ) {
        nextButton.setEnabled(false);
        // choicesGroup.setOnCheckedChangeListener(
        //     new RadioGroup.OnCheckedChangeListener() {
        //       @Override
        //       public void onCheckedChanged(RadioGroup group, int checkedId) {
        //         nextButton.setEnabled(true);
        //       }
        //     });
      }
    }
  }

  // Helper method to update the selected choice.
  private void setChoice(String choice) {
    mChoice = choice;
  }

  private boolean isSelection() {
    return mChoiceType == ChoiceType.SELECTION;
  }

  private String getChoice() {
    return mChoice;
  }

  private boolean isQuiz() {
    return mChoiceType == ChoiceType.QUIZ;
  }

  private boolean hasCorrectAnswer() {
    return (mChoice != null) && mChoice.equals(mCorrectAnswer);
  }

  // Given a string choice text, process it.
  private SpannableStringBuilder processChoiceText(String choiceText) {
    SpannableStringBuilder ssb = new SpannableStringBuilder();
    if (choiceText.length() > 2
        && choiceText.charAt(0) == '{'
        && choiceText.charAt(choiceText.length() - 1) == '}') {
      // This is a database entry.
      String query = choiceText.substring(1, choiceText.length() - 1);
      Cursor cursor =
          getActivity()
              .managedQuery(
                  Uri.parse(KlingonContentProvider.CONTENT_URI + "/lookup"),
                  null /* all columns */,
                  null,
                  new String[] {query},
                  null);
      // Assume cursor.getCount() == 1.
      KlingonContentProvider.Entry entry =
          new KlingonContentProvider.Entry(cursor, getActivity().getBaseContext());
      if (mChoiceTextType != ChoiceTextType.DEFINITION_ONLY) {
        ssb.append(choiceText);
      }
      if (mChoiceTextType == ChoiceTextType.BOTH) {
        ssb.append("\n");
      }
      if (mChoiceTextType != ChoiceTextType.ENTRY_NAME_ONLY) {
        int start = ssb.length();
        String definition;
        if (!entry.shouldDisplayGermanDefinition()) {
          definition = entry.getDefinition();
        } else {
          definition = entry.getDefinition_DE();
        }
        ssb.append(definition);
        ssb.setSpan(
            new ForegroundColorSpan(0xFFC0C0C0),
            start,
            start + definition.length(),
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
      }
      ssb.setSpan(new RelativeSizeSpan(1.2f), 0, ssb.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    }
    return ssb;
  }

  private void setupClosingText(View rootView) {
    if (mClosingText != null) {
      TextView lessonBody2 = (TextView) rootView.findViewById(R.id.lesson_body2);
      lessonBody2.invalidate();
      SpannableStringBuilder closingText = new SpannableStringBuilder(mClosingText);
      processMixedText(closingText, null);
      // We don't call setMovementMethod on lessonBody2, since we disable all
      // entry links.
      lessonBody2.setText(closingText);
    }
  }

  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    mCallback = (Callback) activity;
  }

  public void addPlainList(List<String> choices) {
    mChoices = choices;
    mChoiceType = ChoiceType.PLAIN_LIST;
  }

  public void addMultipleChoiceSelection(List<String> choices) {
    mChoices = choices;
    mChoiceType = ChoiceType.SELECTION;
  }

  public void addQuiz(List<String> choices) {
    mCorrectAnswer = choices.get(0);
    mChoiceType = ChoiceType.QUIZ;

    // Shuffle has to be done on a copy to preserve the original.
    mChoices = new ArrayList(choices);
    Collections.shuffle(mChoices);
  }

  public void addClosingText(String closingText) {
    mClosingText = closingText;
  }

  public void setSummary(List<LessonFragment> lessonFragments) {
    isSummary = true;
    mLessonFragments = lessonFragments;
  }
}
