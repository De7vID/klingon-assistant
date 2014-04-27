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

    private volatile String[] mCurrentLanguage = null;
    private volatile boolean mStopRequested = false;
    private SharedPreferences mSharedPrefs = null;

    // This map contains the front half of full syllables.
    private static final Map<String, Integer> FRONT_HALF_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();

        // --- Verb prefixes ---
        // jIH
        initMap.put("jI-", R.raw.audio_ji_0);
        initMap.put("qa-", R.raw.audio_qa0);
        initMap.put("vI-", R.raw.audio_vi_0);
        initMap.put("Sa-", R.raw.audio_s_a0);

        // SoH
        initMap.put("bI-", R.raw.audio_bi_0);
        initMap.put("Co-", R.raw.audio_c_o0);
        initMap.put("Da-", R.raw.audio_d_a0);
        initMap.put("ju-", R.raw.audio_ju0);

        // ghaH/'oH
        initMap.put("mu-", R.raw.audio_mu0);
        initMap.put("Du-", R.raw.audio_d_u0);
        initMap.put("nu-", R.raw.audio_nu0);
        initMap.put("lI-", R.raw.audio_li_0);

        // maH
        initMap.put("ma-", R.raw.audio_ma0);
        initMap.put("pI-", R.raw.audio_pi_0);
        initMap.put("wI-", R.raw.audio_wi_0);
        initMap.put("re-", R.raw.audio_re0);
        initMap.put("DI-", R.raw.audio_d_i_0);

        // tlhIH
        initMap.put("Su-", R.raw.audio_s_u0);
        initMap.put("tu-", R.raw.audio_tu0);
        initMap.put("bo-", R.raw.audio_bo0);
        initMap.put("che-", R.raw.audio_c_e0);

        // chaH/bIH
        initMap.put("nI-", R.raw.audio_ni_0);
        initMap.put("lu-", R.raw.audio_lu0);

        // Imperatives
        initMap.put("yI-", R.raw.audio_yi_0);
        initMap.put("HI-", R.raw.audio_h_i_0);
        initMap.put("Go-", R.raw.audio_g_o0);
        initMap.put("tI-", R.raw.audio_ti_0);
        initMap.put("pe-", R.raw.audio_pe0);

        // Front parts of words.
        initMap.put("xI-", R.raw.audio_xi_0);  // From {tlhIngan}.
        initMap.put("te-", R.raw.audio_te0);  // From {tera'ngan}.
        // banan, Human

        FRONT_HALF_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    // This map contains short syllables, i.e., those of the form CV.
    // Unlike for the corresponding front half of a full syllable, the audio for a short syllable
    // doesn't end abruptly. For example, "Da-" (a front half syllable) is the prefix for "you-it",
    // whereas "Da" (a short syllable) is the verb for "behave as".
    private static final Map<String, Integer> SHORT_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();
        // bo, cha, Da, DI, Do, gho, ghu, He, Hu, ja, je, jo, lu, 'o, po, QI, ra, ro, So, ta, tI, va, ya, yu, 'a
        // initMap.put("Da", R.raw.audio_d_a);

        SHORT_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    // This map contains the back half of full syllables.
    private static final Map<String, Integer> BACK_HALF_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();

        BACK_HALF_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    // This map contains full syllables, i.e., those of the form CVC, CVrgh, or CV[wy]'.
    private static final Map<String, Integer> MAIN_SYLLABLE_TO_AUDIO_MAP;
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
        initMap.put("jemS", R.raw.audio_jems_);  // From {jemS tIy qIrq}.
        initMap.put("tIy", R.raw.audio_ti_y);  // From {jemS tIy qIrq}.
        initMap.put("qIrq", R.raw.audio_qi_rq);  // From {jemS tIy qIrq}.
        initMap.put("qarD", R.raw.audio_qard_);  // From {pIqarD}.
        initMap.put("turn", R.raw.audio_turn);  // From {Saturn}.

        // --- Common verbs ---
        initMap.put("Dev", R.raw.audio_d_ev);
        initMap.put("HeG", R.raw.audio_h_eg_);   // Hegh
        initMap.put("HoH", R.raw.audio_h_oh_);
        initMap.put("jax", R.raw.audio_jax);   // jatlh
        initMap.put("jeG", R.raw.audio_jeg_);   // jegh
        initMap.put("jez", R.raw.audio_jez);   // je'
        initMap.put("leG", R.raw.audio_leg_);   // legh
        initMap.put("mev", R.raw.audio_mev);
        initMap.put("QoF", R.raw.audio_q_of_);   // Qong
        initMap.put("SaH", R.raw.audio_s_ah_);
        initMap.put("Sov", R.raw.audio_s_ov);
        initMap.put("Suv", R.raw.audio_s_uv);
        initMap.put("yaj", R.raw.audio_yaj);

        // --- Common nouns ---
        initMap.put("Dez", R.raw.audio_d_ez);   // De'
        initMap.put("QaG", R.raw.audio_q_ag_);   // Qagh
        initMap.put("Fan", R.raw.audio_f_an);  // From {tlhIngan}, etc.
        initMap.put("Hol", R.raw.audio_h_ol);

        // --- Conjunctions ---
        initMap.put("zej", R.raw.audio_zej);   // 'ej
        initMap.put("je", R.raw.audio_je);
        initMap.put("qoj", R.raw.audio_qoj);
        initMap.put("joq", R.raw.audio_joq);
        initMap.put("paG", R.raw.audio_pag_);   // pagh
        initMap.put("Gap", R.raw.audio_g_ap);   // ghap

        // --- Question words ---
        initMap.put("GorG", R.raw.audio_g_org_);   // ghorgh
        initMap.put("nuq", R.raw.audio_nuq);
        initMap.put("zar", R.raw.audio_zar);
        initMap.put("zIv", R.raw.audio_zi_v);

        // --- Common adverbials ---
        initMap.put("neH", R.raw.audio_neh_);
        initMap.put("vaj", R.raw.audio_vaj);

        // --- Pronouns ---

        // --- Numbers and number-forming elements ---

        // --- b ---
        initMap.put("beb", R.raw.audio_beb);
        initMap.put("bob", R.raw.audio_bob);
        initMap.put("baC", R.raw.audio_bac_);
        initMap.put("beC", R.raw.audio_bec_);
        initMap.put("boC", R.raw.audio_boc_);
        initMap.put("bID", R.raw.audio_bi_d_);
        initMap.put("boD", R.raw.audio_bod_);
        initMap.put("buD", R.raw.audio_bud_);
        initMap.put("baG", R.raw.audio_bag_);
        initMap.put("beG", R.raw.audio_beg_);
        initMap.put("bIG", R.raw.audio_bi_g_);
        // bogh is a suffix
        initMap.put("baH", R.raw.audio_bah_);
        // beH is a suffix
        initMap.put("bIH", R.raw.audio_bi_h_);
        initMap.put("boH", R.raw.audio_boh_);
        initMap.put("buH", R.raw.audio_buh_);
        initMap.put("baj", R.raw.audio_baj);
        // bej is a suffix
        initMap.put("bIj", R.raw.audio_bi_j);
        initMap.put("boj", R.raw.audio_boj);
        initMap.put("bal", R.raw.audio_bal);
        initMap.put("bel", R.raw.audio_bel);
        initMap.put("bIl", R.raw.audio_bi_l);
        initMap.put("bol", R.raw.audio_bol);
        initMap.put("bam", R.raw.audio_bam);
        initMap.put("bem", R.raw.audio_bem);
        initMap.put("bIm", R.raw.audio_bi_m);
        initMap.put("bom", R.raw.audio_bom);
        initMap.put("ben", R.raw.audio_ben);
        initMap.put("baF", R.raw.audio_baf_);
        initMap.put("bIF", R.raw.audio_bi_f_);
        initMap.put("boF", R.raw.audio_bof_);
        initMap.put("bep", R.raw.audio_bep);
        initMap.put("bIp", R.raw.audio_bi_p);
        initMap.put("bop", R.raw.audio_bop);
        initMap.put("bup", R.raw.audio_bup);
        initMap.put("baq", R.raw.audio_baq);
        initMap.put("beq", R.raw.audio_beq);
        initMap.put("boq", R.raw.audio_boq);
        initMap.put("buq", R.raw.audio_buq);
        initMap.put("baQ", R.raw.audio_baq_);
        initMap.put("beQ", R.raw.audio_beq_);
        initMap.put("bIQ", R.raw.audio_bi_q_);
        initMap.put("boQ", R.raw.audio_boq_);
        initMap.put("buQ", R.raw.audio_buq_);
        initMap.put("bar", R.raw.audio_bar);
        initMap.put("ber", R.raw.audio_ber);
        initMap.put("bIr", R.raw.audio_bi_r);
        initMap.put("bor", R.raw.audio_bor);
        initMap.put("bur", R.raw.audio_bur);
        initMap.put("barG", R.raw.audio_barg_);
        initMap.put("berG", R.raw.audio_berg_);
        initMap.put("burG", R.raw.audio_burg_);
        initMap.put("baS", R.raw.audio_bas_);
        initMap.put("bIS", R.raw.audio_bi_s_);
        initMap.put("boS", R.raw.audio_bos_);
        initMap.put("buS", R.raw.audio_bus_);
        initMap.put("bet", R.raw.audio_bet);
        initMap.put("bIt", R.raw.audio_bi_t);
        initMap.put("bot", R.raw.audio_bot);
        initMap.put("bax", R.raw.audio_bax);
        initMap.put("box", R.raw.audio_box);
        initMap.put("bux", R.raw.audio_bux);
        initMap.put("bav", R.raw.audio_bav);
        initMap.put("bIv", R.raw.audio_bi_v);
        initMap.put("bov", R.raw.audio_bov);
        initMap.put("buv", R.raw.audio_buv);
        initMap.put("bey", R.raw.audio_bey);
        initMap.put("buyz", R.raw.audio_buyz);
        // ba' and be' are suffixes
        initMap.put("bIz", R.raw.audio_bi_z);
        initMap.put("bo", R.raw.audio_bo);

        MAIN_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
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

    // Helper method to prepare a MediaPlayer with the audio and add it to the
    // front of the play list. Preparing this way helps to reduce any audio gap.
    private void prependSyllableToList(Integer resId) {
        if (resId.intValue() != 0) {
            // Alternatively:
            //   MediaPlayer mp = new MediaPlayer();
            //   mp.setDataSource(filename);
            //   mp.prepare();
            MediaPlayer mp = MediaPlayer.create(this, resId.intValue());

            // Chain this MediaPlayer to the front of the existing one (if any).
            mp.setNextMediaPlayer(mMediaPlayer);
            mp.setOnCompletionListener(this);
            mMediaPlayer = mp;
        }
    }

    private void prependCoughToList() {
        prependSyllableToList(R.raw.audio_cough);
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
        String condensedText = condenseKlingonDiTrigraphs(request.getText());
        Log.d(TAG, "condensedText: " + condensedText);
        boolean isFinalSyllable = true;
        while (!condensedText.equals("")) {
            // Syllables in the main syllable map must have length 3 or 4.
            boolean foundMatch = false;
            for (int len = 3; len <= 4; len++) {
                if (condensedText.length() < len) {
                    // Remaining text is too short to have a complete syllable.
                    break;
                }
                String tail = condensedText.substring(condensedText.length() - len);
                Integer resId = MAIN_SYLLABLE_TO_AUDIO_MAP.get(tail);
                if (resId != null) {
                    prependSyllableToList(resId);
                    condensedText = condensedText.substring(0, condensedText.length() - len);

                    // The next match won't be the final syllable in a word.
                    isFinalSyllable = false;
                    foundMatch = true;
                    Log.d(TAG, "Matched tail: " + tail);
                    break;
                }
            }
            if (!foundMatch) {
                String syllable = removeTailSyllable(condensedText);
                if (!syllable.equals("")) {
                    condensedText = condensedText.substring(0, condensedText.length() - syllable.length());
                    String vowel = getSyllableVowel(syllable);
                    int vowelIndex = syllable.indexOf(vowel);

                    // Split the syllable into front and back.
                    String syllableBack = syllable.substring(vowelIndex);
                    String syllableFront = syllable.substring(0, vowelIndex + vowel.length());
                    Integer backResId = BACK_HALF_SYLLABLE_TO_AUDIO_MAP.get("-" + syllableBack);
                    Integer frontResId = FRONT_HALF_SYLLABLE_TO_AUDIO_MAP.get(syllableFront + "-");

                    // If the syllable is CV, then it is a short syllable.
                    boolean isShortSyllable = syllableBack.equals(vowel);
                    if (isShortSyllable && !isFinalSyllable) {
                        // If it's a short syllable but not the final syllable, then truncate the vowel.
                        if (frontResId != null) {
                            prependSyllableToList(frontResId);
                        } else {
                            prependCoughToList();
                        }
                    } else {
                        // Either it's not short, or it's short and final. So play audio for the
                        // whole syllable.
                        Integer resId = null;
                        if (isShortSyllable) {
                            // Try to get audio of the full short syllable in the map of short syllables.
                            resId = SHORT_SYLLABLE_TO_AUDIO_MAP.get(syllable);
                        }
                        if (resId != null) {
                            // We have a full short syllable, so play it.
                            prependSyllableToList(resId);
                        } else {
                            // If the syllable isn't short, or it is short and we've failed to get
                            // audio for the full short syllable, then add both the front and the
                            // back.
                            if (backResId != null) {
                                prependSyllableToList(backResId);
                            } else {
                                prependCoughToList();
                            }
                            if (frontResId != null) {
                                prependSyllableToList(frontResId);
                            } else if (backResId != null) {
                                // Cough only once if both parts are missing.
                                prependCoughToList();
                            }
                        }
                    }

                    // Now the next match won't be the final syllable in a word.
                    isFinalSyllable = false;
                    foundMatch = true;
                    Log.d(TAG, "Matched syllable: " + syllableFront + " " + syllableBack);
                }
            }
            if (!foundMatch) {
                // No match for a complete syllable. Use a fallback sound. This should be avoided if possible.
                char value = condensedText.charAt(condensedText.length() - 1);
                condensedText = condensedText.substring(0, condensedText.length() - 1);
                prependSyllableToList(getResIdForFallbackChar(value));
                Log.d(TAG, "Stripped char: " + value);

                // The next match will be considered the final syllable in a new word.
                isFinalSyllable = true;
            }
        }
        beginPlayback();

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

    private static int getResIdForFallbackChar(char value) {
        switch(value) {
          case 'a':
            return R.raw.audio_a;
          case 'b':
            return R.raw.audio_b;
          case 'C': // {ch}
            return R.raw.audio_c_;
          case 'D':
            return R.raw.audio_d_;
          case 'e':
            return R.raw.audio_e;
          case 'G': // {gh}
            return R.raw.audio_g_;
          case 'H':
            return R.raw.audio_h_;
          case 'I':
            return R.raw.audio_i_;
          case 'j':
            return R.raw.audio_j;
          case 'l':
            return R.raw.audio_l;
          case 'm':
            return R.raw.audio_m;
          case 'n':
            return R.raw.audio_n;
          case 'F': // {ng}
            return R.raw.audio_f_;
          case 'o':
            return R.raw.audio_o;
          case 'p':
            return R.raw.audio_p;
          case 'q':
            return R.raw.audio_q;
          case 'Q':
            return R.raw.audio_q_;
          case 'r':
            return R.raw.audio_r;
          case 'S':
            return R.raw.audio_s_;
          case 't':
            return R.raw.audio_t;
          case 'x': // {tlh}
            return R.raw.audio_x;
          case 'u':
            return R.raw.audio_u;
          case 'v':
            return R.raw.audio_v;
          case 'w':
            return R.raw.audio_w;
          case 'y':
            return R.raw.audio_y;
          case 'z': // {'}
            return R.raw.audio_z;
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

    private void beginPlayback() {
        // Someone called onStop, end the current synthesis and return.
        // The mStopRequested variable will be reset at the beginning of the
        // next synthesis.
        //
        // In general, a call to onStop() should make a best effort attempt
        // to stop all processing for the *current* onSynthesizeText request (if
        // one is active).
        if (mStopRequested || mMediaPlayer == null) {
            return;
        }

        // This starts the chain of playback.
        mMediaPlayer.start();

        // It is important to set this to null here, since if onSynthesizeText
        // is called again, we don't want to chain the currently playing
        // syllables to the end of the new chain.
        mMediaPlayer = null;
    }

    public void onCompletion(MediaPlayer mp) {
        // Be sure to release the audio resources when playback is completed.
        // Log.d(TAG, "onCompletion called");
        // mp.reset();
        mp.release();
    }
}
