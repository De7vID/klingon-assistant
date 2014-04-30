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
        initMap.put("jI-", R.raw.ji0);
        initMap.put("qa-", R.raw.qa0);
        initMap.put("vI-", R.raw.vi0);
        initMap.put("Sa-", R.raw.sa0);

        // SoH
        initMap.put("bI-", R.raw.bi0);
        initMap.put("Co-", R.raw.co0);
        initMap.put("Da-", R.raw.da0);
        initMap.put("ju-", R.raw.ju0);

        // ghaH/'oH
        initMap.put("mu-", R.raw.mu0);
        initMap.put("Du-", R.raw.du0);
        initMap.put("nu-", R.raw.nu0);
        initMap.put("lI-", R.raw.li0);

        // maH
        initMap.put("ma-", R.raw.ma0);
        initMap.put("pI-", R.raw.pi0);
        initMap.put("wI-", R.raw.wi0);
        initMap.put("re-", R.raw.re0);
        initMap.put("DI-", R.raw.di0);

        // tlhIH
        initMap.put("Su-", R.raw.su0);
        initMap.put("tu-", R.raw.tu0);
        initMap.put("bo-", R.raw.bo0);
        initMap.put("Ce-", R.raw.ce0);

        // chaH/bIH
        initMap.put("nI-", R.raw.ni0);
        initMap.put("lu-", R.raw.lu0);

        // Imperatives
        initMap.put("yI-", R.raw.yi0);
        initMap.put("HI-", R.raw.hi0);
        initMap.put("Go-", R.raw.go0);
        initMap.put("tI-", R.raw.ti0);
        initMap.put("pe-", R.raw.pe0);

        // Front parts of words.
        initMap.put("na-", R.raw.na0);  // From {naDev}.
        initMap.put("xI-", R.raw.xi0);  // From {tlhIngan}.
        initMap.put("te-", R.raw.te0);  // From {tera'ngan}.
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
        // initMap.put("Da", R.raw.da);

        SHORT_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    // This map contains the back half of full syllables. It is preferable to avoid constructing
    // syllables by concatenating the front and back parts, as there is a noticeable discontinuity
    // in the audio if the vowel sound is not perfectly matched.
    private static final Map<String, Integer> BACK_HALF_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();
        // -a, -e, -I, -o, -u

        BACK_HALF_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    // This map contains full syllables, i.e., those of the form CVC, CVrgh, or CV[wy]'.
    private static final Map<String, Integer> MAIN_SYLLABLE_TO_AUDIO_MAP;
    static {
        Map<String, Integer> initMap = new HashMap<String, Integer>();

        // --- Verb suffixes ---
        initMap.put("zeG", R.raw.zeg);   // 'egh
        initMap.put("Cuq", R.raw.cuq);  // chuq
        initMap.put("nIS", R.raw.nis);
        initMap.put("qaF", R.raw.qaf);   // qang
        initMap.put("rup", R.raw.rup);
        initMap.put("beH", R.raw.beh);
        initMap.put("vIp", R.raw.vip);
        initMap.put("CoH", R.raw.coh);  // choH
        initMap.put("qaz", R.raw.qaz);   // qa'
        initMap.put("moH", R.raw.moh);
        initMap.put("luz", R.raw.luz);   // lu'
        initMap.put("laH", R.raw.lah);
        initMap.put("Cuz", R.raw.cuz);  // chu'
        initMap.put("bej", R.raw.bej);
        initMap.put("lawz", R.raw.lawz);  // law'
        initMap.put("baz", R.raw.baz);   // ba'
        initMap.put("puz", R.raw.puz);   // pu'
        initMap.put("taz", R.raw.taz);   // ta'
        initMap.put("taH", R.raw.tah);
        initMap.put("lIz", R.raw.liz);   // lI'
        initMap.put("neS", R.raw.nes);   // neS
        initMap.put("DIz", R.raw.diz);  // DI'
        initMap.put("CuG", R.raw.cug);  // chugh
        initMap.put("paz", R.raw.paz);   // pa'
        initMap.put("vIS", R.raw.vis);
        initMap.put("boG", R.raw.bog);   // bogh
        initMap.put("meH", R.raw.meh);
        initMap.put("zaz", R.raw.zaz);
        initMap.put("wIz", R.raw.wiz);   // wI'
        initMap.put("moz", R.raw.moz);   // mo'
        initMap.put("jaj", R.raw.jaj);
        initMap.put("GaC", R.raw.gac);   // ghach
        initMap.put("bez", R.raw.bez);   // be'
        initMap.put("Qoz", R.raw.koz);   // Qo'
        initMap.put("Haz", R.raw.haz);   // Ha'
        initMap.put("quz", R.raw.quz);   // qu'

        // --- Noun suffixes ---
        // Note: {'a'}, {pu'}, {wI'}, {lI'}, and {mo'} are already in the verb suffixes.
        // Also, {oy} requires special handling since it doesn't start with a consonant.
        initMap.put("Hom", R.raw.hom);
        initMap.put("Duz", R.raw.duz);   // Du'
        initMap.put("mey", R.raw.mey);
        initMap.put("qoq", R.raw.qoq);
        initMap.put("Hey", R.raw.hey);
        initMap.put("naz", R.raw.naz);   // na'
        initMap.put("wIj", R.raw.wij);
        initMap.put("lIj", R.raw.lij);
        initMap.put("maj", R.raw.maj);
        initMap.put("maz", R.raw.maz);   // ma'
        initMap.put("raj", R.raw.raj);
        initMap.put("raz", R.raw.raz);   // ra'
        initMap.put("Daj", R.raw.daj);
        initMap.put("Caj", R.raw.caj);   // chaj
        initMap.put("vam", R.raw.vam);
        initMap.put("vex", R.raw.vex);   // vetlh
        initMap.put("Daq", R.raw.daq);
        initMap.put("voz", R.raw.voz);   // vo'
        initMap.put("vaD", R.raw.vad);
        initMap.put("zez", R.raw.zez);   // 'e'

        // --- Non-standard phonology ---
        initMap.put("jemS", R.raw.jems);  // From {jemS tIy qIrq}.
        initMap.put("tIy", R.raw.tiy);  // From {jemS tIy qIrq}.
        initMap.put("qIrq", R.raw.qirq);  // From {jemS tIy qIrq}.
        initMap.put("qarD", R.raw.qard);  // From {pIqarD}.
        initMap.put("turn", R.raw.turn);  // From {Saturn}.

        // --- Common verbs ---
        initMap.put("CarG", R.raw.carg);
        initMap.put("Cen", R.raw.cen);
        initMap.put("Cop", R.raw.cop);
        initMap.put("Dab", R.raw.dab);
        initMap.put("Dej", R.raw.dej);
        initMap.put("Dev", R.raw.dev);
        initMap.put("Doq", R.raw.doq);
        initMap.put("Dor", R.raw.dor);
        initMap.put("Doyz", R.raw.doyz);   // Doy'
        initMap.put("GIx", R.raw.gix);   // ghItlh
        initMap.put("Gaj", R.raw.gaj);   // ghaj
        initMap.put("Goj", R.raw.goj);   // ghoj
        initMap.put("GoD", R.raw.god);   // ghoD
        initMap.put("GoS", R.raw.gos);   // ghoS
        initMap.put("GuF", R.raw.guf);   // ghung
        initMap.put("Hab", R.raw.hab);
        initMap.put("HoH", R.raw.hoh);
        initMap.put("HoS", R.raw.hos);
        initMap.put("HeG", R.raw.heg);   // Hegh
        initMap.put("HIv", R.raw.hiv);
        initMap.put("HoH", R.raw.hoh);
        initMap.put("jaC", R.raw.jac);   // jach
        initMap.put("jaH", R.raw.jah);   // jaH
        initMap.put("jax", R.raw.jax);   // jatlh
        initMap.put("jaz", R.raw.jaz);   // ja'
        initMap.put("jeG", R.raw.jeg);   // jegh
        initMap.put("jey", R.raw.jey);
        initMap.put("jez", R.raw.jez);   // je'
        initMap.put("laj", R.raw.laj);   // laj
        initMap.put("leG", R.raw.leg);   // legh
        initMap.put("loz", R.raw.loz);   // lo'
        initMap.put("maC", R.raw.mac);
        initMap.put("mev", R.raw.mev);
        initMap.put("mIz", R.raw.miz);
        initMap.put("naj", R.raw.naj);
        initMap.put("nIH", R.raw.nih);
        initMap.put("noz", R.raw.noz);
        initMap.put("FaG", R.raw.fag);   // ngagh
        initMap.put("FaS", R.raw.fas);   // ngaS
        initMap.put("Fuz", R.raw.fuz);   // ngu'
        initMap.put("paw", R.raw.paw);
        initMap.put("poF", R.raw.pof);
        initMap.put("poS", R.raw.pos);
        initMap.put("rur", R.raw.rur);
        initMap.put("qaS", R.raw.qas);
        initMap.put("qIm", R.raw.qim);
        initMap.put("qIp", R.raw.qip);
        initMap.put("qul", R.raw.qul);
        initMap.put("quv", R.raw.quv);
        initMap.put("Qob", R.raw.kob);
        initMap.put("QoF", R.raw.kof);   // Qong
        initMap.put("Qoy", R.raw.koy);   // Qoy
        initMap.put("QuC", R.raw.kuc);   // Quch
        initMap.put("ruC", R.raw.ruc);   // ruch
        initMap.put("SaH", R.raw.sah);
        initMap.put("Sayz", R.raw.sayz);   // Say'
        initMap.put("SIQ", R.raw.sik);
        initMap.put("Sop", R.raw.sop);
        initMap.put("Sov", R.raw.sov);
        initMap.put("SuD", R.raw.sud);
        initMap.put("Suv", R.raw.suv);
        initMap.put("tayz", R.raw.tayz);   // tay'
        initMap.put("tIn", R.raw.tin);
        initMap.put("tIv", R.raw.tiv);
        initMap.put("tuH", R.raw.tuh);
        initMap.put("tuj", R.raw.tuj);
        initMap.put("tuz", R.raw.tuz);
        initMap.put("xap", R.raw.xap);   // tlhap
        initMap.put("xux", R.raw.xux);   // tlhutlh
        initMap.put("van", R.raw.van);
        initMap.put("vIH", R.raw.vih);
        initMap.put("yaj", R.raw.yaj);
        initMap.put("zav", R.raw.zav);   // 'av
        initMap.put("zoj", R.raw.zoj);   // 'oj
        initMap.put("zaF", R.raw.zaf);   // 'ang

        // --- Common nouns ---
        initMap.put("Dez", R.raw.dez);   // De'
        initMap.put("DoC", R.raw.doc);   // Doch
        initMap.put("Dop", R.raw.dop);
        initMap.put("Duj", R.raw.duj);
        initMap.put("jaG", R.raw.jag);   // jagh
        initMap.put("laz", R.raw.laz);   // From {Qapla'}.
        initMap.put("Fan", R.raw.fan);  // From {tlhIngan}, etc.
        initMap.put("GarG", R.raw.garg);   // ghargh
        initMap.put("Gez", R.raw.gez);   // From {ghe''or}.
        initMap.put("Gom", R.raw.gom);   // ghom
        initMap.put("Gop", R.raw.gop);   // ghop
        initMap.put("Guz", R.raw.guz);   // ghu'
        initMap.put("HoC", R.raw.hoc);   // Hoch
        initMap.put("Hol", R.raw.hol);
        initMap.put("Hov", R.raw.hov);
        initMap.put("Hoz", R.raw.hoz);   // Ho'
        initMap.put("HuH", R.raw.huh);
        initMap.put("jIb", R.raw.jib);
        initMap.put("juH", R.raw.juh);   // juH
        initMap.put("larG", R.raw.larg);   // From {veqlargh}.
        initMap.put("leF", R.raw.lef);   // leng
        initMap.put("lIS", R.raw.lis);   // From {qeylIS}.
        initMap.put("loD", R.raw.lod);   // loD
        initMap.put("loj", R.raw.loj);   // From {lojmIt}.
        initMap.put("luS", R.raw.lus);   // From {luspet}.
        initMap.put("mIt", R.raw.mit);   // From {lojmIt}.
        initMap.put("muz", R.raw.muz);   // mu'
        initMap.put("noS", R.raw.nos);   // From {Qo'noS}.
        initMap.put("peG", R.raw.peg);   // pegh
        initMap.put("pet", R.raw.pet);   // From {luspet}.
        initMap.put("puq", R.raw.puq);
        initMap.put("ram", R.raw.ram);
        initMap.put("qab", R.raw.qab);
        initMap.put("qey", R.raw.qey);   // From {qeylIS}.
        initMap.put("QaG", R.raw.kag);   // Qagh
        initMap.put("Qap", R.raw.kap);   // From {Qapla'}.
        initMap.put("Qaz", R.raw.kaz);   // From {majQa'}.
        initMap.put("Quv", R.raw.kuv);
        initMap.put("Soj", R.raw.soj);
        initMap.put("SoS", R.raw.sos);
        initMap.put("taj", R.raw.taj);
        initMap.put("tarG", R.raw.targ);
        initMap.put("tIq", R.raw.tiq);
        initMap.put("toz", R.raw.toz);   // to'
        initMap.put("woz", R.raw.woz);   // wo'
        initMap.put("xeG", R.raw.xeg);   // tlhegh
        initMap.put("vav", R.raw.vav);
        initMap.put("veq", R.raw.veq);   // From {veqlargh}.
        initMap.put("vIz", R.raw.viz);   // From {DIvI'}.
        initMap.put("yab", R.raw.yab);
        initMap.put("yIn", R.raw.yin);
        initMap.put("yIH", R.raw.yih);
        initMap.put("zIw", R.raw.ziw);
        initMap.put("zor", R.raw.zor);   // From {ghe''or}.
        initMap.put("zoyz", R.raw.zoyz);
        initMap.put("zuQ", R.raw.zuk);   // 'uQ
        initMap.put("zuS", R.raw.zus);   // 'uS

        // --- Conjunctions ---
        initMap.put("zej", R.raw.zej);   // 'ej
        initMap.put("je", R.raw.je);
        initMap.put("qoj", R.raw.qoj);
        initMap.put("joq", R.raw.joq);
        initMap.put("paG", R.raw.pag);   // pagh
        initMap.put("Gap", R.raw.gap);   // ghap

        // --- Question words ---
        initMap.put("GorG", R.raw.gorg);   // ghorgh
        initMap.put("nuq", R.raw.nuq);
        initMap.put("zar", R.raw.zar);
        initMap.put("zIv", R.raw.ziv);

        // --- Common adverbials ---
        initMap.put("Caq", R.raw.caq);   // chaq
        initMap.put("Doz", R.raw.doz);   // Do'
        initMap.put("neH", R.raw.neh);
        initMap.put("nom", R.raw.nom);
        initMap.put("QIt", R.raw.kit);
        initMap.put("reH", R.raw.reh);
        initMap.put("teb", R.raw.teb);   // From {nIteb}
        initMap.put("vaj", R.raw.vaj);

        // --- Pronouns ---
        initMap.put("jIH", R.raw.jih);
        initMap.put("SoH", R.raw.soh);
        initMap.put("GaH", R.raw.gah);
        initMap.put("maH", R.raw.mah);
        initMap.put("zoH", R.raw.zoh);

        // --- Numbers and number-forming elements ---

        // --- b ---
        initMap.put("beb", R.raw.beb);
        initMap.put("bob", R.raw.bob);
        initMap.put("baC", R.raw.bac);
        initMap.put("beC", R.raw.bec);
        initMap.put("boC", R.raw.boc);
        initMap.put("bID", R.raw.bid);
        initMap.put("boD", R.raw.bod);
        initMap.put("buD", R.raw.bud);
        initMap.put("baG", R.raw.bag);
        initMap.put("beG", R.raw.beg);
        initMap.put("bIG", R.raw.big);
        // bogh is a suffix
        initMap.put("baH", R.raw.bah);
        // beH is a suffix
        initMap.put("bIH", R.raw.bih);
        initMap.put("boH", R.raw.boh);
        initMap.put("buH", R.raw.buh);
        initMap.put("baj", R.raw.baj);
        // bej is a suffix
        initMap.put("bIj", R.raw.bij);
        initMap.put("boj", R.raw.boj);
        initMap.put("bal", R.raw.bal);
        initMap.put("bel", R.raw.bel);
        initMap.put("bIl", R.raw.bil);
        initMap.put("bol", R.raw.bol);
        initMap.put("bam", R.raw.bam);
        initMap.put("bem", R.raw.bem);
        initMap.put("bIm", R.raw.bim);
        initMap.put("bom", R.raw.bom);
        initMap.put("ben", R.raw.ben);
        initMap.put("baF", R.raw.baf);
        initMap.put("bIF", R.raw.bif);
        initMap.put("boF", R.raw.bof);
        initMap.put("bep", R.raw.bep);
        initMap.put("bIp", R.raw.bip);
        initMap.put("bop", R.raw.bop);
        initMap.put("bup", R.raw.bup);
        initMap.put("baq", R.raw.baq);
        initMap.put("beq", R.raw.beq);
        initMap.put("boq", R.raw.boq);
        initMap.put("buq", R.raw.buq);
        initMap.put("baQ", R.raw.bak);
        initMap.put("beQ", R.raw.bek);
        initMap.put("bIQ", R.raw.bik);
        initMap.put("boQ", R.raw.bok);
        initMap.put("buQ", R.raw.buk);
        initMap.put("bar", R.raw.bar);
        initMap.put("ber", R.raw.ber);
        initMap.put("bIr", R.raw.bir);
        initMap.put("bor", R.raw.bor);
        initMap.put("bur", R.raw.bur);
        initMap.put("barG", R.raw.barg);
        initMap.put("berG", R.raw.berg);
        initMap.put("burG", R.raw.burg);
        initMap.put("baS", R.raw.bas);
        initMap.put("bIS", R.raw.bis);
        initMap.put("boS", R.raw.bos);
        initMap.put("buS", R.raw.bus);
        initMap.put("bet", R.raw.bet);
        initMap.put("bIt", R.raw.bit);
        initMap.put("bot", R.raw.bot);
        initMap.put("bax", R.raw.bax);
        initMap.put("box", R.raw.box);
        initMap.put("bux", R.raw.bux);
        initMap.put("bav", R.raw.bav);
        initMap.put("bIv", R.raw.biv);
        initMap.put("bov", R.raw.bov);
        initMap.put("buv", R.raw.buv);
        initMap.put("bey", R.raw.bey);
        initMap.put("buyz", R.raw.buyz);
        // ba' and be' are suffixes
        initMap.put("bIz", R.raw.biz);
        initMap.put("bo", R.raw.bo);

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
        prependSyllableToList(R.raw.cough);
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
                            boolean coughed = false;
                            if (backResId != null) {
                                prependSyllableToList(backResId);
                            } else {
                                String backConsonants = syllableBack.substring(vowel.length());
                                if (backConsonants.length() == 1) {
                                  // This should be avoided, but if there is no choice, say the
                                  // consonant by itself. It won't sound smooth.
                                  prependSyllableToList(getResIdForFallbackChar(backConsonants.charAt(0)));
                                } else {
                                  // There was a consonant cluster (-rgh, -w', -y'), can't fake it.
                                  prependCoughToList();
                                  coughed = true;
                                }
                            }
                            if (frontResId != null) {
                                prependSyllableToList(frontResId);
                            } else if (!coughed) {
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
            return R.raw.a;
          case 'b':
            return R.raw.b;
          case 'C': // {ch}
            return R.raw.c;
          case 'D':
            return R.raw.d;
          case 'e':
            return R.raw.e;
          case 'G': // {gh}
            return R.raw.g;
          case 'H':
            return R.raw.h;
          case 'I':
            return R.raw.i;
          case 'j':
            return R.raw.j;
          case 'l':
            return R.raw.l;
          case 'm':
            return R.raw.m;
          case 'n':
            return R.raw.n;
          case 'F': // {ng}
            return R.raw.f;
          case 'o':
            return R.raw.o;
          case 'p':
            return R.raw.p;
          case 'q':
            return R.raw.q;
          case 'Q':
            return R.raw.k;
          case 'r':
            return R.raw.r;
          case 'S':
            return R.raw.s;
          case 't':
            return R.raw.t;
          case 'x': // {tlh}
            return R.raw.x;
          case 'u':
            return R.raw.u;
          case 'v':
            return R.raw.v;
          case 'w':
            return R.raw.w;
          case 'y':
            return R.raw.y;
          case 'z': // {'}
            return R.raw.z;
          case ' ':
            return R.raw.silence;
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
