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
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;

public class LessonFragment extends EntryFragment {

  // Interface to report feedback to the LessonActivity.
  public interface Callback {
    void goToNextPage();

    void commitChoice(String choice);

    void scoreQuiz(boolean correctlyAnswered);

    String getSummary();

    void redoThisLesson();
  }

  private Callback mCallback;

  // For the saved instance state.
  private static final String STATE_CHOICE_TYPE = "choice_type";
  private static final String STATE_CHOICE_TEXT_TYPE = "choice_text_type";
  private static final String STATE_CHOICES = "choices";
  private static final String STATE_CORRECT_ANSWER = "correct_answer";
  private static final String STATE_SELECTED_CHOICE = "selected_choice";
  private static final String STATE_ALREADY_ANSWERED = "already_answered";
  private static final String STATE_CLOSING_TEXT = "closing_text";
  private static final String STATE_IS_SUMMARY = "is_summary";

  // Choices section.
  private ArrayList<String> mChoices = null;
  private String mCorrectAnswer = null;
  private String mSelectedChoice = null;
  private boolean mAlreadyAnswered = false;

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

  public enum ChoiceTextType {
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
  private boolean mIsSummaryPage = false;

  // Set to true if there are no more lessons after this page.
  private boolean mCannotContinue = false;

  public static LessonFragment newInstance(String topic, String body) {
    LessonFragment lessonFragment = new LessonFragment();
    Bundle args = new Bundle();
    args.putString("topic", topic);
    args.putString("body", body);
    lessonFragment.setArguments(args);
    return lessonFragment;
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

    // Restore instance state, if any.
    if (savedInstanceState != null) {
      mChoiceType = (ChoiceType) savedInstanceState.getSerializable(STATE_CHOICE_TYPE);
      mChoiceTextType = (ChoiceTextType) savedInstanceState.getSerializable(STATE_CHOICE_TEXT_TYPE);
      mChoices = savedInstanceState.getStringArrayList(STATE_CHOICES);
      mCorrectAnswer = savedInstanceState.getString(STATE_CORRECT_ANSWER);
      mSelectedChoice = savedInstanceState.getString(STATE_SELECTED_CHOICE);
      mAlreadyAnswered = savedInstanceState.getBoolean(STATE_ALREADY_ANSWERED);
      mClosingText = savedInstanceState.getString(STATE_CLOSING_TEXT);
      mIsSummaryPage = savedInstanceState.getBoolean(STATE_IS_SUMMARY);
    }

    ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.lesson, container, false);
    Resources resources = getActivity().getResources();

    // Set up the title and body text.
    TextView lessonTitle = (TextView) rootView.findViewById(R.id.lesson_title);
    TextView lessonBody = (TextView) rootView.findViewById(R.id.lesson_body);

    lessonTitle.invalidate();
    lessonTitle.setText(getArguments().getString("topic"));

    lessonBody.invalidate();
    if (!mIsSummaryPage) {
      String bodyText = getArguments().getString("body");
      SpannableStringBuilder ssb = new SpannableStringBuilder(bodyText);
      processMixedText(ssb, null);
      // We don't call setMovementMethod on lessonBody, since we disable all
      // entry links.
      lessonBody.setText(ssb);
    } else {
      // TODO: change buttons, etc.
      lessonBody.setText(mCallback.getSummary());
      Button redoButton = (Button) rootView.findViewById(R.id.action_redo);
      redoButton.setVisibility(View.VISIBLE);
      redoButton.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View view) {
            mCallback.redoThisLesson();
          }
        }
      );
    }

    // Set up the "Continue" button.
    // TODO: Change button text depending on context.
    Button continueButton = (Button) rootView.findViewById(R.id.action_continue);
    if (mCannotContinue) {
      continueButton.setEnabled(false);
    } else {
      continueButton.setOnClickListener(
          new View.OnClickListener() {
            @Override
            public void onClick(View view) {
              mCallback.goToNextPage();
            }
          });
    }

    // Set up possible additional views.
    setupChoicesGroup(rootView);

    // Put additional text after other sections.
    setupClosingText(rootView);

    return rootView;
  }

  private void setupChoicesGroup(View rootView) {
    // Compute margins in dp.
    final float scale = getActivity().getResources().getDisplayMetrics().density;
    final int leftRightMargins = (int) (LEFT_RIGHT_MARGINS * scale + 0.5f);
    final int topBottomMargins = (int) (TOP_BOTTOM_MARGINS * scale + 0.5f);
    final LessonFragment thisLesson = this;

    if (mChoiceType == ChoiceType.NONE || mChoices == null) {
      return;
    }

    final RadioGroup choicesGroup = (RadioGroup) rootView.findViewById(R.id.choices);
    final Button checkAnswerButton = (Button) rootView.findViewById(R.id.action_check_answer);
    final Button continueButton = (Button) rootView.findViewById(R.id.action_continue);
    final String CORRECT_STRING =
        getActivity().getResources().getString(R.string.button_check_answer_correct);
    final String INCORRECT_STRING =
        getActivity().getResources().getString(R.string.button_check_answer_incorrect);
    if (mChoiceType == ChoiceType.SELECTION || mChoiceType == ChoiceType.QUIZ) {
      // Disable until user selects something.
      continueButton.setEnabled(false);
    }
    if (mChoiceType == ChoiceType.QUIZ) {
      // Make "Check Answer" button visible for QUIZ only.
      checkAnswerButton.setVisibility(View.VISIBLE);
    }

    // We have to make 3 passes through the buttons. On the first pass, we just add them.
    for (int i = 0; i < mChoices.size(); i++) {
      RadioButton choiceButton = new RadioButton(getActivity());
      choiceButton.setPadding(
          leftRightMargins, topBottomMargins, leftRightMargins, topBottomMargins);

      // Display entry name and/or definition depending on choice text type,
      // and also format the displayed text.
      SpannableStringBuilder choiceText = processChoiceText(mChoices.get(i));
      processMixedText(choiceText, null);
      choiceButton.setText(choiceText);
      choicesGroup.addView(choiceButton);
    }

    // On the second pass, we add their click listeners. Since each button may affect the others,
    // all the buttons had to be added first.
    for (int i = 0; i < mChoices.size(); i++) {
      RadioButton choiceButton = (RadioButton) choicesGroup.getChildAt(i);
      final String choice = mChoices.get(i);
      if (mChoiceType == ChoiceType.SELECTION) {
        // For a selection, update the choice and go to next page when clicked.
        choiceButton.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                LessonFragment.this.setSelectedChoice(choice);
                continueButton.setEnabled(true);
                continueButton.setOnClickListener(
                    new View.OnClickListener() {
                      @Override
                      public void onClick(View view) {
                        mCallback.commitChoice(choice);
                        mCallback.goToNextPage();
                      }
                    });
              }
            });
      } else if (mChoiceType == ChoiceType.QUIZ) {
        // This is a QUIZ which hasn't been answered, enable the "Check answer" button when choice
        // is clicked.
        choiceButton.setOnClickListener(
            new View.OnClickListener() {
              @Override
              public void onClick(View view) {
                LessonFragment.this.setSelectedChoice(choice);
                checkAnswerButton.setEnabled(true);
                checkAnswerButton.setOnClickListener(
                    new View.OnClickListener() {
                      @Override
                      public void onClick(View view) {
                        final boolean isAnswerCorrect = choice.equals(mCorrectAnswer);
                        checkAnswerButton.setEnabled(false);
                        for (int i = 0; i < choicesGroup.getChildCount(); i++) {
                          ((RadioButton) choicesGroup.getChildAt(i)).setEnabled(false);
                        }
                        choicesGroup.setEnabled(false);
                        if (!mAlreadyAnswered) {
                          mCallback.scoreQuiz(isAnswerCorrect);
                          LessonFragment.this.setAlreadyAnswered();
                        }
                        if (isAnswerCorrect) {
                          checkAnswerButton.setText(CORRECT_STRING);
                          checkAnswerButton.setBackgroundColor(Color.GREEN);
                        } else {
                          checkAnswerButton.setText(INCORRECT_STRING);
                          checkAnswerButton.setBackgroundColor(Color.RED);
                        }
                        continueButton.setEnabled(true);
                        continueButton.setOnClickListener(
                            new View.OnClickListener() {
                              @Override
                              public void onClick(View view) {
                                mCallback.goToNextPage();
                              }
                            });
                      }
                    });
                if (mAlreadyAnswered) {
                  checkAnswerButton.performClick();
                }
              }
            });
      } else if (mChoiceType == ChoiceType.PLAIN_LIST) {
        // For a plain list, hide the radio button and just show the text.
        choiceButton.setButtonDrawable(android.R.color.transparent);
      }
    }

    // On the third pass, we restore the previous state of the choices by faking a click. This is
    // necessary if, for example, the device is rotated. This has to happen after the listeners have
    // all been set.
    for (int i = 0; i < mChoices.size(); i++) {
      RadioButton choiceButton = (RadioButton) choicesGroup.getChildAt(i);
      final String choice = mChoices.get(i);
      if (mSelectedChoice != null && choice == mSelectedChoice) {
        // Restore previously selected choice, if one exists.
        choicesGroup.check(choiceButton.getId());
        if (mChoiceType == ChoiceType.SELECTION || mChoiceType == ChoiceType.QUIZ) {
          // This is a SELECTION which has already been made, so enable "Continue".
          choiceButton.performClick();
        }
      }
    }
    choicesGroup.setVisibility(View.VISIBLE);
    choicesGroup.invalidate();
  }

  private void setSelectedChoice(String selectedChoice) {
    mSelectedChoice = selectedChoice;
  }

  private void setAlreadyAnswered() {
    // User has already answered the QUIZ question on this page.
    mAlreadyAnswered = true;
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

  public void addPlainList(ArrayList<String> choices) {
    mChoices = choices;
    mChoiceType = ChoiceType.PLAIN_LIST;
  }

  public void addMultipleChoiceSelection(ArrayList<String> choices) {
    mChoices = choices;
    mChoiceType = ChoiceType.SELECTION;
  }

  public void addQuiz(ArrayList<String> choices, ChoiceTextType choiceTextType) {
    mCorrectAnswer = choices.get(0);
    mChoiceType = ChoiceType.QUIZ;
    mChoiceTextType = choiceTextType;

    // Shuffle has to be done on a copy to preserve the original.
    mChoices = new ArrayList(choices);
    Collections.shuffle(mChoices);
  }

  public void addClosingText(String closingText) {
    mClosingText = closingText;
  }

  public void setAsSummaryPage() {
    mIsSummaryPage = true;
  }

  public void setNoMoreLessons() {
    mCannotContinue = true;
  }

  @Override
  public void onSaveInstanceState(Bundle savedInstanceState) {
    super.onSaveInstanceState(savedInstanceState);

    savedInstanceState.putSerializable(STATE_CHOICE_TYPE, mChoiceType);
    savedInstanceState.putSerializable(STATE_CHOICE_TEXT_TYPE, mChoiceTextType);
    savedInstanceState.putStringArrayList(STATE_CHOICES, mChoices);
    savedInstanceState.putString(STATE_CORRECT_ANSWER, mCorrectAnswer);
    savedInstanceState.putString(STATE_SELECTED_CHOICE, mSelectedChoice);
    savedInstanceState.putBoolean(STATE_ALREADY_ANSWERED, mAlreadyAnswered);
    savedInstanceState.putString(STATE_CLOSING_TEXT, mClosingText);
    savedInstanceState.putBoolean(STATE_IS_SUMMARY, mIsSummaryPage);
  }
}
