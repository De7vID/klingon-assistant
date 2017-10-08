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
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import java.util.List;

public class LessonFragment extends EntryFragment {

  // Interface to report feedback to the LessonActivity.
  public interface Callback {
    void goToNextPage();
  }

  private Callback mCallback;

  // Choices section.
  private List<String> mChoices = null;
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

  // Closing text section.
  private String mClosingText = null;

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
    String bodyText = getArguments().getString("body");
    SpannableStringBuilder ssb = new SpannableStringBuilder(bodyText);
    processMixedText(ssb, bodyText, null);
    // We don't call setMovementMethod on lessonBody, since we disable all
    // entry links.
    lessonBody.setText(ssb);

    // Set up the bottom navigation buttons. By default, enable just the "Next"
    // button.
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
    if (mChoiceType != ChoiceType.NONE && mChoices != null) {
      RadioGroup choicesGroup = (RadioGroup) rootView.findViewById(R.id.choices);
      final MenuItem nextButton = (MenuItem) bottomNavView.getMenu().findItem(R.id.action_next);
      for (int i = 0; i < mChoices.size(); i++) {
        RadioButton choiceButton = new RadioButton(getActivity());
        if (mChoiceType == ChoiceType.PLAIN_LIST) {
          // For a plain list, hide the radio button and just show the text.
          choiceButton.setButtonDrawable(android.R.color.transparent);
          // choiceButton.setPadding(12, 0, 0, 0);
        }

        // TODO: Include entry definition, format text.
        SpannableStringBuilder choiceText = new SpannableStringBuilder(mChoices.get(i));
        processMixedText(choiceText, mChoices.get(i), null);
        choiceButton.setText(choiceText);
        choicesGroup.addView(choiceButton);
      }
      choicesGroup.setVisibility(View.VISIBLE);
      choicesGroup.invalidate();
      // TODO: Fix selector font size, colours.
      if (mChoiceType == ChoiceType.SELECTION || mChoiceType == ChoiceType.QUIZ) {
        nextButton.setEnabled(false);
        choicesGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
              nextButton.setEnabled(true);
            }
        });
      }
    }
  }

  private void setupClosingText(View rootView) {
    if (mClosingText != null) {
      TextView lessonBody2 = (TextView) rootView.findViewById(R.id.lesson_body2);
      lessonBody2.invalidate();
      SpannableStringBuilder closingText = new SpannableStringBuilder(mClosingText);
      processMixedText(closingText, mClosingText, null);
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

  public void addMultipleChoiceSelection(List<String> choices) {
    mChoices = choices;
    mChoiceType = ChoiceType.SELECTION;
  }

  public void addClosingText(String closingText) {
    mClosingText = closingText;
  }

  // List adapter for word selection and multiple-choice quizzes.
  // class MultipleChoiceAdapter extends BaseAdapter implements AdapterView.OnItemClickListener {

  //   private final Cursor mCursor;
  //   private final LayoutInflater mInflater;
  //   private int mSelectedPosition = -1;
  //   private RadioButton mSelectedButton = null;

  //   public MultipleChoiceAdapter(Cursor cursor) {
  //     mCursor = cursor;
  //     mInflater = (LayoutInflater)
  // getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  //   }

  //   @Override
  //   public int getCount() {
  //     return mCursor.getCount();
  //   }

  //   @Override
  //   public Object getItem(int position) {
  //     return position;
  //   }

  //   @Override
  //   public long getItemId(int position) {
  //     return position;
  //   }

  //   @Override
  //   public View getView(int position, View convertView, ViewGroup parent) {
  //     LinearLayout view = (convertView != null) ? (LinearLayout) convertView :
  // createView(parent);

  //     // Have to set the check state here because the views are recycled.
  //     RadioButton button = (RadioButton) view.findViewById(R.id.radio);
  //     if (position == mSelectedPosition) {
  //       button.setChecked(true);
  //     } else {
  //       button.setChecked(false);
  //     }

  //     mCursor.moveToPosition(position);
  //     bindView(view, mCursor);
  //     return view;
  //   }

  //   private LinearLayout createView(ViewGroup parent) {
  //     // Use a modified version of android.R.simple_list_item_2_single_choice
  //     // which has been adapted for our needs.
  //     LinearLayout item =
  //         (LinearLayout)
  //             mInflater.inflate(R.layout.simple_list_item_2_single_choice, parent, false);
  //     return item;
  //   }

  //   private void bindView(LinearLayout view, Cursor cursor) {
  //     KlingonContentProvider.Entry entry = new KlingonContentProvider.Entry(cursor,
  // getActivity());

  //     // Note that we override the typeface and text size here, instead of in
  //     // the xml, because putting it there would also change the appearance of
  //     // the Preferences page. We fully indent suffixes, but only half-indent verbs.
  //     String indent1 =
  //         entry.isIndented() ? (entry.isVerb() ? "&nbsp;&nbsp;" : "&nbsp;&nbsp;&nbsp;&nbsp;") :
  // "";
  //     String indent2 =
  //         entry.isIndented()
  //             ? (entry.isVerb()
  //                 ? "&nbsp;&nbsp;&nbsp;&nbsp;"
  //                 : "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;")
  //             : "";

  //     TextView text1 = view.findViewById(android.R.id.text1);
  //     TextView text2 = view.findViewById(android.R.id.text2);
  //     SharedPreferences sharedPrefs =
  // PreferenceManager.getDefaultSharedPreferences(getActivity());
  //     if (!sharedPrefs.getBoolean(
  //         Preferences.KEY_KLINGON_FONT_CHECKBOX_PREFERENCE, /* default */ false)) {
  //       // Use serif for the entry, so capital-I and lowercase-l are distinguishable.
  //       text1.setTypeface(Typeface.SERIF);
  //       text1.setText(Html.fromHtml(indent1 + entry.getFormattedEntryName(/* isHtml */ true)));
  //     } else {
  //       // Preference is set to display this in {pIqaD}!
  //       text1.setTypeface(KlingonAssistant.getKlingonFontTypeface(getActivity()));
  //       text1.setText(Html.fromHtml(indent1 + entry.getEntryNameInKlingonFont()));
  //     }
  //     text1.setTextSize(22);

  //     // TODO: Colour attached affixes differently from verb.
  //     text1.setTextColor(entry.getTextColor());

  //     // Use sans serif for the definition.
  //     text2.setTypeface(Typeface.SANS_SERIF);
  //     text2.setText(Html.fromHtml(indent2 + entry.getFormattedDefinition(/* isHtml */ true)));
  //     text2.setTextSize(14);
  //     text2.setTextColor(0xFFC0C0C0);
  //   }

  //   @Override
  //   public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
  //     RadioButton button = (RadioButton) view.findViewById(R.id.radio);
  //     if (position != mSelectedPosition && mSelectedButton != null) {
  //       mSelectedButton.setChecked(false);
  //     }
  //     button.setChecked(true);
  //     mSelectedPosition = position;
  //     mSelectedButton = button;

  //     if (getCount() == 1) {
  //       // Launch entry the regular way, as there's only one result.
  //       mCursor.moveToPosition(position);
  //       // launchEntry(mCursor.getString(KlingonContentDatabase.COLUMN_ID));
  //     } else {
  //       // There's a list of results, so launch a list of entries. Instead of passing in
  //       // one ID, we pass in a comma-separated list. We also append the position of the
  //       // selected entry to the end.
  //       StringBuilder entryList = new StringBuilder();
  //       for (int i = 0; i < getCount(); i++) {
  //         mCursor.moveToPosition(i);
  //         entryList.append(mCursor.getString(KlingonContentDatabase.COLUMN_ID));
  //         entryList.append(",");
  //       }
  //       entryList.append(position);
  //       mCursor.moveToPosition(position);
  //       // launchEntry(entryList.toString());
  //     }
  //   }
  // }
}
