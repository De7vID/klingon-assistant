/*
 * Copyright (C) 2014 De'vID jonpIn (David Yonge-Mallo)
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.tlhInganHol.android.klingonttsengine;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaPlayer;
import android.speech.tts.SynthesisCallback;
import android.speech.tts.SynthesisRequest;
import android.speech.tts.TextToSpeech;
import android.speech.tts.TextToSpeechService;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * A text to speech engine that generates "speech" that a robot might understand.
 * The engine supports two different Klingon voices.
 *
 * It exercises all aspects of the Text to speech engine API
 * {@link android.speech.tts.TextToSpeechService}.
 */
public class KlingonSpeakTtsService extends TextToSpeechService implements android.media.MediaPlayer.OnCompletionListener {
    private static final String TAG = "KlingonSpeakTtsService";

    // The media player object used to play the sounds.
    private MediaPlayer mMediaPlayer = null;
    private LinkedList<Integer> mSyllableList = null;

    private volatile String[] mCurrentLanguage = null;
    private volatile boolean mStopRequested = false;
    private SharedPreferences mSharedPrefs = null;

    private static final Map<String, Integer> FRONT_HALF_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();
        initMap.put("ba", R.raw.audio_ba);
        initMap.put("xa", R.raw.audio_xa);

        // --- Verb prefixes ---
        initMap.put("jI", R.raw.audio_ji_);
        initMap.put("bI", R.raw.audio_bi_);

        FRONT_HALF_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    private static final Map<String, Integer> BACK_HALF_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();
        initMap.put("ab", R.raw.audio_ab);
        initMap.put("ax", R.raw.audio_ax);

        BACK_HALF_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    private static final Map<String, Integer> SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();

        // --- Verb suffixes ---
        initMap.put("zeG", R.raw.audio_zeg_);   // 'egh
        initMap.put("Cuq", R.raw.audio_c_uq);  // chuq
        initMap.put("nIS", R.raw.audio_ni_s_);
        initMap.put("qaF", R.raw.audio_qaf_);   // qang
        initMap.put("rup", R.raw.audio_rup);
        initMap.put("beH", R.raw.audio_beh_);
        initMap.put("vIp", R.raw.audio_vi_p);
        initMap.put("CoH", R.raw.audio_c_oh_);  // choH
        initMap.put("qaz", R.raw.audio_qaz);   // qa'
        initMap.put("moH", R.raw.audio_moh_);
        initMap.put("luz", R.raw.audio_luz);   // lu'
        initMap.put("laH", R.raw.audio_lah_);
        initMap.put("Cuz", R.raw.audio_c_uz);  // chu'
        initMap.put("bej", R.raw.audio_bej);
        initMap.put("lawz", R.raw.audio_lawz);  // law'
        initMap.put("baz", R.raw.audio_baz);   // ba'
        initMap.put("puz", R.raw.audio_puz);   // pu'
        initMap.put("taz", R.raw.audio_taz);   // ta'
        initMap.put("taH", R.raw.audio_tah_);
        initMap.put("lIz", R.raw.audio_li_z);   // lI'
        initMap.put("neS", R.raw.audio_nes_);   // neS
        initMap.put("DIz", R.raw.audio_d_i_z);  // DI'
        initMap.put("CuG", R.raw.audio_c_u_g_);  // chugh
        initMap.put("paz", R.raw.audio_paz);   // pa'
        initMap.put("vIS", R.raw.audio_vi_s_);
        initMap.put("boG", R.raw.audio_bog_);   // bogh
        initMap.put("meH", R.raw.audio_meh_);
        initMap.put("zaz", R.raw.audio_zaz);
        initMap.put("wIz", R.raw.audio_wi_z);   // wI'
        initMap.put("moz", R.raw.audio_moz);   // mo'
        initMap.put("jaj", R.raw.audio_jaj);
        initMap.put("GaC", R.raw.audio_g_ac_);   // ghach
        initMap.put("bez", R.raw.audio_bez);   // be'
        initMap.put("Qoz", R.raw.audio_q_oz);   // Qo'
        initMap.put("Haz", R.raw.audio_h_az);   // Ha'
        initMap.put("quz", R.raw.audio_quz);   // qu'

        // --- Noun suffixes ---
        // Note: {'a'}, {pu'}, {wI'}, {lI'}, and {mo'} are already in the verb suffixes.
        // Also, {oy} requires special handling since it doesn't start with a consonant.
        initMap.put("Hom", R.raw.audio_h_om);
        initMap.put("Duz", R.raw.audio_d_uz);   // Du'
        initMap.put("mey", R.raw.audio_mey);
        initMap.put("qoq", R.raw.audio_qoq);
        initMap.put("Hey", R.raw.audio_h_ey);
        initMap.put("naz", R.raw.audio_naz);   // na'
        initMap.put("wIj", R.raw.audio_wi_j);
        initMap.put("lIj", R.raw.audio_li_j);
        initMap.put("maj", R.raw.audio_maj);
        initMap.put("maz", R.raw.audio_maz);   // ma'
        initMap.put("raj", R.raw.audio_raj);
        initMap.put("raz", R.raw.audio_raz);   // ra'
        initMap.put("Daj", R.raw.audio_d_aj);
        initMap.put("Caj", R.raw.audio_c_aj);   // chaj
        initMap.put("vam", R.raw.audio_vam);
        initMap.put("vex", R.raw.audio_vex);   // vetlh
        initMap.put("Daq", R.raw.audio_d_aq);
        initMap.put("voz", R.raw.audio_voz);   // vo'
        initMap.put("vaD", R.raw.audio_vad_);
        initMap.put("zez", R.raw.audio_zez);   // 'e'

        // --- Non-standard phonology ---
        initMap.put("qarD", R.raw.audio_silence);  // From {pIqarD}.
        initMap.put("qIrq", R.raw.audio_silence);  // From {jemS tIy qIrq}
        initMap.put("jemS", R.raw.audio_silence);  // From {jemS tIy qIrq}.
        initMap.put("turn", R.raw.audio_silence);  // From {Saturn}.

        // --- Common verbs ---
        initMap.put("jeG", R.raw.audio_jeg_);   // jegh
        initMap.put("HeG", R.raw.audio_h_eg_);   // jegh

        SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mSharedPrefs = getSharedPreferences(GeneralSettingsFragment.SHARED_PREFS_NAME,
                Context.MODE_PRIVATE);
        // We load the default language when we start up. This isn't strictly
        // required though, it can always be loaded lazily on the first call to
        // onLoadLanguage or onSynthesizeText. This a tradeoff between memory usage
        // and the latency of the first call.
        onLoadLanguage("tlh", "CAN", "");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    protected String[] onGetLanguage() {
        // Note that mCurrentLanguage is volatile because this can be called from
        // multiple threads.
        return mCurrentLanguage;
    }

    @Override
    protected int onIsLanguageAvailable(String lang, String country, String variant) {
        // The robot speak synthesizer supports only english.
        if ("tlh".equals(lang)) {
            // We support two specific klingon languages, the canadian klingon language
            // and the american klingon language.
            if ("USA".equals(country) || "CAN".equals(country)) {
                // If the engine supported a specific variant, we would have
                // something like.
                //
                // if ("android".equals(variant)) {
                //     return TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE;
                // }
                return TextToSpeech.LANG_COUNTRY_AVAILABLE;
            }

            // We support the language, but not the country.
            return TextToSpeech.LANG_AVAILABLE;
        }

        return TextToSpeech.LANG_NOT_SUPPORTED;
    }

    /*
     * Note that this method is synchronized, as is onSynthesizeText because
     * onLoadLanguage can be called from multiple threads (while onSynthesizeText
     * is always called from a single thread only).
     */
    @Override
    protected synchronized int onLoadLanguage(String lang, String country, String variant) {
        final int isLanguageAvailable = onIsLanguageAvailable(lang, country, variant);

        if (isLanguageAvailable == TextToSpeech.LANG_NOT_SUPPORTED) {
            return isLanguageAvailable;
        }

        String loadCountry = country;
        if (isLanguageAvailable == TextToSpeech.LANG_AVAILABLE) {
            loadCountry = "USA";
        }

        // If we've already loaded the requested language, we can return early.
        if (mCurrentLanguage != null) {
            if (mCurrentLanguage[0].equals(lang) && mCurrentLanguage[1].equals(country)) {
                return isLanguageAvailable;
            }
        }

        mCurrentLanguage = new String[] { lang, loadCountry, "" };

        return isLanguageAvailable;
    }

    @Override
    protected void onStop() {
        mStopRequested = true;
    }

    @Override
    protected synchronized void onSynthesizeText(SynthesisRequest request,
            SynthesisCallback callback) {
        // Note that we call onLoadLanguage here since there is no guarantee
        // that there would have been a prior call to this function.
        int load = onLoadLanguage(request.getLanguage(), request.getCountry(),
                request.getVariant());

        // We might get requests for a language we don't support - in which case
        // we error out early before wasting too much time.
        if (load == TextToSpeech.LANG_NOT_SUPPORTED) {
            callback.error();
            return;
        }

        // At this point, we have loaded the language we need for synthesis and
        // it is guaranteed that we support it so we proceed with synthesis.

        // We construct a list of syllables to be played.
        mSyllableList = new LinkedList<Integer>();
        String condensedText = condenseKlingonDiTrigraphs(request.getText());
        Log.d(TAG, "condensedText: " + condensedText);
        while (!condensedText.equals("")) {
            // Syllables in the main syllable map must have length 3 or 4.
            boolean matched = false;
            for (int len = 3; len <= 4; len++) {
                if (condensedText.length() < len) {
                    // Remaining text is too short to have a complete syllable.
                    break;
                }
                String tail = condensedText.substring(condensedText.length() - len);
                Integer resId = SYLLABLE_TO_AUDIO_MAP.get(tail);
                if (resId != null) {
                    mSyllableList.addFirst(resId);
                    condensedText = condensedText.substring(0, condensedText.length() - len);
                    matched = true;
                    Log.d(TAG, "Matched tail: " + tail);
                    break;
                }
            }
            if (!matched) {
                String syllable = removeTailSyllable(condensedText);
                if (!syllable.equals("")) {
                    condensedText = condensedText.substring(0, condensedText.length() - syllable.length());
                    String vowel = getSyllableVowel(syllable);
                    int vowelIndex = syllable.indexOf(vowel);

                    // Process the back half of the syllable.
                    String syllableBack = syllable.substring(vowelIndex);
                    Integer backResId = BACK_HALF_SYLLABLE_TO_AUDIO_MAP.get(syllableBack);
                    if (backResId != null) {
                        mSyllableList.addFirst(backResId);
                    }
                    if (syllableBack.equals(vowel)) {
                        syllableBack = "";
                    }

                    // Process the front half of the syllable.
                    String syllableFront = syllable.substring(0, vowelIndex + vowel.length());
                    Integer frontResId = FRONT_HALF_SYLLABLE_TO_AUDIO_MAP.get(syllableFront);
                    if (frontResId != null) {
                        mSyllableList.addFirst(frontResId);
                    }

                    matched = true;
                    Log.d(TAG, "Matched syllable: " + syllableFront + " " + syllableBack);
                }
            }
            if (!matched) {
                // No match for a complete syllable.
                char value = condensedText.charAt(condensedText.length() - 1);
                condensedText = condensedText.substring(0, condensedText.length() - 1);
                mSyllableList.addFirst(getResourceIdForChar(value));
                Log.d(TAG, "Stripped char: " + value);
            }
        }
        playNextSyllableOfRemainingText();

        // Alright, we're done with our synthesis - yay!
        callback.done();
    }

    private static boolean isSimpleVowel(char value) {
        final String aeIou = "aeIou";
        return aeIou.indexOf(value) != -1;
    }

    // Attempt to remove a syllable from the end of a given text and return it. Return empty string
    // if unsuccessful. Both input and output are in condensed format.
    private static String removeTailSyllable(String input) {
        // Syllables can have the following forms, where C is a consonant, and V is a vowel:
        //   CV
        //   CVC   (excluding {ow} and {uw})
        //   CVrgh
        //   CVw'  (excluding {ow'} and {uw'})
        //   CVy'
        // Log.d(TAG, "removeTailSyllable from: " + input);

        String remainingText = input;
        String tail = "";

        // Deal with the ending.
        if (remainingText.length() > 3 && remainingText.endsWith("wz")) {
            // Ends in {w'}. Peak at the preceding vowel.
            char value = remainingText.charAt(remainingText.length() - 3);
            if (value == 'o' || value == 'u') {
                // Drop the "w".
                tail = "z";
            } else {
                tail = "wz";
            }
            // Remove the "wz", but leave the vowel.
            remainingText = remainingText.substring(0, remainingText.length() - 2);
        } else if (remainingText.length() > 2 && remainingText.endsWith("w")) {
            // Ends in {w}. Peak at the preceding vowel.
            char value = remainingText.charAt(remainingText.length() - 2);
            if (value == 'o' || value == 'u') {
                // Drop the "w".
                tail = "";
            } else {
                tail = "w";
            }
            // Remove the "w", but leave the vowel.
            remainingText = remainingText.substring(0, remainingText.length() - 1);
        } else if (remainingText.length() > 3 && remainingText.endsWith("yz")) {
            // Ends in {y'}.
            tail = "yz";
            remainingText = remainingText.substring(0, remainingText.length() - 2);
        } else if (remainingText.length() > 3 && remainingText.endsWith("rG")) {
            // Ends in {rgh}.
            tail = "rG";
            remainingText = remainingText.substring(0, remainingText.length() - 2);
        } else if (remainingText.length() > 2 && !isSimpleVowel(remainingText.charAt(remainingText.length() - 1))) {
            // Ends in something other than a vowel. Assume it's a consonant.
            tail = remainingText.substring(remainingText.length() - 1);
            remainingText = remainingText.substring(0, remainingText.length() - 1);
        }
        // Log.d(TAG, "After ending: " + remainingText + " / " + tail);

        // Look for the vowel.
        if (remainingText.length() < 2 ||
            !isSimpleVowel(remainingText.charAt(remainingText.length() - 1))) {
            // Failed to extract a syllable from the tail.
            return "";
        }
        tail = remainingText.substring(remainingText.length() - 1) + tail;
        remainingText = remainingText.substring(0, remainingText.length() - 1);
        // Log.d(TAG, "After middle: " + remainingText + " / " + tail);

        // Look for the initial consonant.
        if (remainingText.length() < 1 ||
            isSimpleVowel(remainingText.charAt(remainingText.length() - 1))) {
            // Also a failure.
            return "";
        }
        tail = remainingText.substring(remainingText.length() - 1) + tail;
        remainingText = remainingText.substring(0, remainingText.length() - 1);
        // Log.d(TAG, "After beginning: " + remainingText + " / " + tail);

        return tail;
    }

    private static String getSyllableVowel(String syllable) {
        // Given a legitimate Klingon syllable, return its (possibly complex) vowel.
        // Note that "ow" and "uw" are not possible.
        String[] possibleVowels = {"aw", "ew", "Iw", "ay", "ey", "Iy", "oy", "uy", "a", "e", "I", "o", "u"};
        for (int i = 0; i < possibleVowels.length; ++i) {
            if (syllable.indexOf(possibleVowels[i]) != -1) {
                return possibleVowels[i];
            }
        }
        return "";
    }

    private static int getResourceIdForChar(char value) {
        switch(value) {
          case 'a':
            return R.raw.audio_silence;
          case 'b':
            return R.raw.audio_silence;
          case 'C': // {ch}
            return R.raw.audio_silence;
          case 'D':
            return R.raw.audio_silence;
          case 'e':
            return R.raw.audio_silence;
          case 'G': // {gh}
            return R.raw.audio_silence;
          case 'H':
            return R.raw.audio_silence;
          case 'I':
            return R.raw.audio_silence;
          case 'j':
            return R.raw.audio_silence;
          case 'l':
            return R.raw.audio_silence;
          case 'm':
            return R.raw.audio_silence;
          case 'n':
            return R.raw.audio_silence;
          case 'F': // {ng}
            return R.raw.audio_silence;
          case 'o':
            return R.raw.audio_silence;
          case 'p':
            return R.raw.audio_silence;
          case 'q':
            return R.raw.audio_silence;
          case 'Q':
            return R.raw.audio_silence;
          case 'r':
            return R.raw.audio_silence;
          case 'S':
            return R.raw.audio_silence;
          case 't':
            return R.raw.audio_silence;
          case 'x': // {tlh}
            return R.raw.audio_silence;
          case 'u':
            return R.raw.audio_silence;
          case 'v':
            return R.raw.audio_silence;
          case 'w':
            return R.raw.audio_silence;
          case 'y':
            return R.raw.audio_silence;
          case 'z': // {'}
            return R.raw.audio_silence;
          case ' ':
            return R.raw.audio_silence;
          default:
            // Note that 0 denotes an invalid resource ID in Android.
            return 0;
        }
    }

    /*
     * Condense {tlhIngan Hol} with a mapping that represents diagraphs and trigraphs as single characters.
     * Also replace {'} with "z" for ease of processing. The input is assumed to be proper Klingon orthography.
     */
    private static String condenseKlingonDiTrigraphs(String input) {
        return input.replaceAll("[^A-Za-z']+", " ")  // Strip all non-alphabetical characters.
                    .replaceAll("ch", "C")
                    .replaceAll("gh", "G")   // {gh} has to be done before {ng} so that {ngh} -> "nG" and not "Fh".
                    .replaceAll("ng", "F")
                    .replaceAll("tlh", "x")
                    .replaceAll("'", "z");
    }

    private void playNextSyllableOfRemainingText() {
        // Someone called onStop, end the current synthesis and return.
        // The mStopRequested variable will be reset at the beginning of the
        // next synthesis.
        //
        // In general, a call to onStop( ) should make a best effort attempt
        // to stop all processing for the *current* onSynthesizeText request (if
        // one is active).
        if (mStopRequested) {
            return;
        }
        if (!mSyllableList.isEmpty()) {
            Integer resId = mSyllableList.pop();
            if (resId.intValue() != 0) {
                // Play the audio file.
                // Alternatively: mMediaPlayer = new MediaPlayer(); mMediaPlayer.setDataSource(filename); mMediaPlayer.prepare();
                mMediaPlayer = MediaPlayer.create(this, resId.intValue());
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.start();
                Log.d(TAG, "Playing: " + resId);
            }
        }
    }

    public void onCompletion(MediaPlayer mp) {
        // Be sure to release the audio resources when playback is completed.
        mp.release();

        // Play the next character.
        playNextSyllableOfRemainingText();
    }
}
