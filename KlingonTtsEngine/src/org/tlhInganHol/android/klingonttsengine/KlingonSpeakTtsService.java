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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

/**
 * A text to speech engine that generates "speech" that a robot might understand.
 * The engine supports two different "languages", each with their own frequency
 * mappings.
 *
 * It exercises all aspects of the Text to speech engine API
 * {@link android.speech.tts.TextToSpeechService}.
 */
public class KlingonSpeakTtsService extends TextToSpeechService implements android.media.MediaPlayer.OnCompletionListener {
    private static final String TAG = "ExampleTtsService";

    /*
     * This is the sampling rate of our output audio. This engine outputs
     * audio at 16khz 16bits per sample PCM audio.
     */
    private static final int SAMPLING_RATE_HZ = 16000;

    /*
     * We multiply by a factor of two since each sample contains 16 bits (2 bytes).
     */
    private final byte[] mAudioBuffer = new byte[SAMPLING_RATE_HZ * 2];

    // The media player object used to play the sounds.
    private MediaPlayer mMediaPlayer = null;
    private String mRemainingText = null;

    private Map<Character, Integer> mFrequenciesMap;
    private volatile String[] mCurrentLanguage = null;
    private volatile boolean mStopRequested = false;
    private SharedPreferences mSharedPrefs = null;

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

        Map<Character, Integer> newFrequenciesMap = null;
        try {
            InputStream file = getAssets().open(lang + "-" + loadCountry + ".freq");
            newFrequenciesMap = buildFrequencyMap(file);
            file.close();
        } catch (IOException e) {
            Log.e(TAG, "Error loading data for : " + lang + "-" + country);
        }

        mFrequenciesMap = newFrequenciesMap;
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

        // We denote that we are ready to start sending audio across to the
        // framework. We use a fixed sampling rate (16khz), and send data across
        // in 16bit PCM mono.
        callback.start(SAMPLING_RATE_HZ,
                AudioFormat.ENCODING_PCM_16BIT, 1 /* Number of channels. */);

        // We then scan through each character of the request string and
        // generate audio for it.
        mRemainingText = condenseKlingonDiTrigraphs(request.getText());
        playNextCharOfRemainingText();

        // Alright, we're done with our synthesis - yay!
        callback.done();
    }

    private static int getResourceIdForChar(char value) {
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
        return input.replaceAll("ch", "C")
                    .replaceAll("gh", "G")   // {gh} has to be done before {ng} so that {ngh} -> "nG" and not "Fh".
                    .replaceAll("ng", "F")
                    .replaceAll("tlh", "x")
                    .replaceAll("'", "z");
    }

    private Map<Character, Integer> buildFrequencyMap(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        String line = null;
        Map<Character, Integer> map = new HashMap<Character, Integer>();
        try {
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(":");
                if (parts.length != 2) {
                    throw new IOException("Invalid line encountered: " + line);
                }
                map.put(parts[0].charAt(0), Integer.parseInt(parts[1]));
            }
            map.put(' ', 0);
            return map;
        } finally {
            is.close();
        }
    }

    private void playNextCharOfRemainingText() {
        if (mStopRequested) {
            return;
        }
        for (int i = 0; i < mRemainingText.length(); ++i) {
            // TODO: Instead of reading one character at a time, match longer chunks such as verb prefixes or verb/noun suffixes.
            int resId = getResourceIdForChar(mRemainingText.charAt(i));
            if (resId != 0) {
                // Play the audio file.
                // Alternatively: mMediaPlayer = new MediaPlayer(); mMediaPlayer.setDataSource(filename); mMediaPlayer.prepare();
                mMediaPlayer = MediaPlayer.create(this, resId);
                mMediaPlayer.setOnCompletionListener(this);
                mMediaPlayer.start();
                mRemainingText = mRemainingText.substring(i + 1);
                break;
            }
        }
    }

    public void onCompletion(MediaPlayer mp) {
        // Be sure to release the audio resources when playback is completed.
        mp.release();

        // Play the next character.
        playNextCharOfRemainingText();
    }
}
