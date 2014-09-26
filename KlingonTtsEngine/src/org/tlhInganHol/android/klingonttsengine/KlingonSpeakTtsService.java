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
 * A text to speech engine that generates Klingon speech.
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
        initMap.put("CI-", R.raw.ci0);
        initMap.put("Cu-", R.raw.cu0);
        initMap.put("Do-", R.raw.do0);
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
        // bo, cha, Da, DI, Do, gho, ghu, He, Hu, ja, je, jo, lu, po, QI, ra, ro, So, ta, tI, va, ya, yu, 'a, 'o
        initMap.put("bo", R.raw.bo);
        initMap.put("Ca", R.raw.ca);
        initMap.put("Da", R.raw.da);
        initMap.put("DI", R.raw.di);
        initMap.put("Go", R.raw.go);
        initMap.put("Do", R.raw.do_);  // Can't use "do" because it's a Java keyword!
        initMap.put("je", R.raw.je);
        initMap.put("zo", R.raw.zo);

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

        // --- b ---
        initMap.put("baC", R.raw.bac);
        initMap.put("baG", R.raw.bag);
        initMap.put("baH", R.raw.bah);
        initMap.put("baj", R.raw.baj);
        initMap.put("bal", R.raw.bal);
        initMap.put("bam", R.raw.bam);
        initMap.put("baF", R.raw.baf);
        initMap.put("baQ", R.raw.bak);
        initMap.put("baq", R.raw.baq);
        initMap.put("bar", R.raw.bar);
        initMap.put("barG", R.raw.barg);
        initMap.put("baS", R.raw.bas);
        initMap.put("bav", R.raw.bav);
        initMap.put("bax", R.raw.bax);
        initMap.put("baz", R.raw.baz);   // ba'
        initMap.put("beb", R.raw.beb);
        initMap.put("beC", R.raw.bec);
        initMap.put("beG", R.raw.beg);
        initMap.put("beH", R.raw.beh);
        initMap.put("bej", R.raw.bej);
        initMap.put("bel", R.raw.bel);
        initMap.put("bem", R.raw.bem);
        initMap.put("ben", R.raw.ben);
        initMap.put("bep", R.raw.bep);
        initMap.put("beQ", R.raw.bek);
        initMap.put("beq", R.raw.beq);
        initMap.put("ber", R.raw.ber);
        initMap.put("berG", R.raw.berg);
        initMap.put("bet", R.raw.bet);
        initMap.put("bey", R.raw.bey);
        initMap.put("bez", R.raw.bez);   // be'
        initMap.put("bID", R.raw.bid);
        initMap.put("bIF", R.raw.bif);
        initMap.put("bIG", R.raw.big);
        initMap.put("bIH", R.raw.bih);
        initMap.put("bIj", R.raw.bij);
        initMap.put("bIl", R.raw.bil);
        initMap.put("bIm", R.raw.bim);
        initMap.put("bIp", R.raw.bip);
        initMap.put("bIQ", R.raw.bik);
        initMap.put("bIr", R.raw.bir);
        initMap.put("bIS", R.raw.bis);
        initMap.put("bIt", R.raw.bit);
        initMap.put("bIv", R.raw.biv);
        initMap.put("bIz", R.raw.biz);
        // bo is short
        initMap.put("bob", R.raw.bob);
        initMap.put("boC", R.raw.boc);
        initMap.put("boD", R.raw.bod);
        initMap.put("boF", R.raw.bof);
        initMap.put("boG", R.raw.bog);   // bogh
        initMap.put("boH", R.raw.boh);
        initMap.put("boj", R.raw.boj);
        initMap.put("bol", R.raw.bol);
        initMap.put("bom", R.raw.bom);
        initMap.put("bop", R.raw.bop);
        initMap.put("boQ", R.raw.bok);
        initMap.put("boq", R.raw.boq);
        initMap.put("bor", R.raw.bor);
        initMap.put("boS", R.raw.bos);
        initMap.put("bot", R.raw.bot);
        initMap.put("bov", R.raw.bov);
        initMap.put("box", R.raw.box);
        initMap.put("buD", R.raw.bud);
        initMap.put("buH", R.raw.buh);
        initMap.put("bup", R.raw.bup);
        initMap.put("buQ", R.raw.buk);
        initMap.put("buq", R.raw.buq);
        initMap.put("burG", R.raw.burg);
        initMap.put("bur", R.raw.bur);
        initMap.put("buS", R.raw.bus);
        initMap.put("buv", R.raw.buv);
        initMap.put("bux", R.raw.bux);
        initMap.put("buyz", R.raw.buyz);

        // --- ch ---
        initMap.put("Cab", R.raw.cab);
        initMap.put("CaC", R.raw.cac);
        initMap.put("CaD", R.raw.cad);
        initMap.put("CaG", R.raw.cag);
        initMap.put("CaH", R.raw.cah);
        initMap.put("Caj", R.raw.caj);   // chaj
        initMap.put("Cal", R.raw.cal);
        initMap.put("Cam", R.raw.cam);
        initMap.put("Can", R.raw.can);
        initMap.put("CaF", R.raw.caf);
        initMap.put("Cap", R.raw.cap);   // chaq
        initMap.put("Caq", R.raw.caq);   // chaq
        initMap.put("CaQ", R.raw.cak);
        initMap.put("Car", R.raw.car);
        initMap.put("CarG", R.raw.carg);
        initMap.put("CaS", R.raw.cas);
        initMap.put("Cax", R.raw.cax);
        initMap.put("Cawz", R.raw.cawz);
        initMap.put("Cav", R.raw.cav);
        initMap.put("Cayz", R.raw.cayz);
        initMap.put("Caz", R.raw.caz);
        initMap.put("Ceb", R.raw.ceb);
        initMap.put("CeC", R.raw.cec);
        initMap.put("CeG", R.raw.ceg);
        initMap.put("CeH", R.raw.ceh);
        initMap.put("Cej", R.raw.cej);
        initMap.put("Cel", R.raw.cel);
        initMap.put("Cem", R.raw.cem);
        initMap.put("Cen", R.raw.cen);
        initMap.put("CeF", R.raw.cef);  // cheng
        initMap.put("Cep", R.raw.cep);
        // initMap.put("CeQ", R.raw.cek);
        // initMap.put("Ceq", R.raw.ceq);
        initMap.put("Cer", R.raw.cer);
        initMap.put("CerG", R.raw.cerg);
        initMap.put("CeS", R.raw.ces);
        initMap.put("Cet", R.raw.cet);
        initMap.put("Cev", R.raw.cev);
        // initMap.put("Cey", R.raw.cey);
        initMap.put("Cez", R.raw.cez);
        initMap.put("CIC", R.raw.cic);
        initMap.put("CID", R.raw.cid);
        // initMap.put("CIF", R.raw.cif);
        // initMap.put("CIG", R.raw.cig);
        // initMap.put("CIH", R.raw.cih);
        initMap.put("CIj", R.raw.cij);
        initMap.put("CIl", R.raw.cil);
        initMap.put("CIm", R.raw.cim);
        initMap.put("CIn", R.raw.cin);
        initMap.put("CIp", R.raw.cip);
        // initMap.put("CIQ", R.raw.cik);
        // initMap.put("CIr", R.raw.cir);
        initMap.put("CIrg", R.raw.cirg);
        initMap.put("CIS", R.raw.cis);
        // initMap.put("CIt", R.raw.cit);
        initMap.put("CIv", R.raw.civ);
        initMap.put("CIw", R.raw.ciw);
        initMap.put("CIz", R.raw.ciz);
        initMap.put("Cob", R.raw.cob);
        // initMap.put("CoC", R.raw.coc);
        // initMap.put("CoD", R.raw.cod);
        initMap.put("CoF", R.raw.cof);
        initMap.put("CoG", R.raw.cog);
        initMap.put("CoH", R.raw.coh);  // choH
        // initMap.put("Coj", R.raw.coj);
        initMap.put("Col", R.raw.col);
        initMap.put("Com", R.raw.com);
        initMap.put("Con", R.raw.con);
        initMap.put("Cop", R.raw.cop);
        initMap.put("CoQ", R.raw.cok);
        initMap.put("Coq", R.raw.coq);
        initMap.put("Cor", R.raw.cor);
        initMap.put("CorG", R.raw.corg);
        initMap.put("CoS", R.raw.cos);
        initMap.put("Cot", R.raw.cot);
        initMap.put("Cov", R.raw.cov);
        initMap.put("Coz", R.raw.coz);
        // initMap.put("Cox", R.raw.cox);
        initMap.put("CuC", R.raw.cuc);
        initMap.put("CuD", R.raw.cud);
        initMap.put("CuG", R.raw.cug);  // chugh
        initMap.put("CuH", R.raw.cuh);
        initMap.put("Cun", R.raw.cun);
        initMap.put("CuF", R.raw.cuf);
        initMap.put("Cup", R.raw.cup);
        // initMap.put("CuQ", R.raw.cuk);
        initMap.put("Cuq", R.raw.cuq);  // chuq
        // initMap.put("CurG", R.raw.curg);
        // initMap.put("Cur", R.raw.cur);
        initMap.put("CuS", R.raw.cus);
        initMap.put("Cut", R.raw.cut);
        initMap.put("Cuv", R.raw.cuv);
        // initMap.put("Cux", R.raw.cux);
        initMap.put("Cuy", R.raw.cuy);
        // initMap.put("Cuyz", R.raw.cuyz);
        initMap.put("Cuz", R.raw.cuz);  // chu'

        // --- D ---
        initMap.put("Dab", R.raw.dab);
        initMap.put("Dac", R.raw.dac);
        initMap.put("Dag", R.raw.dag);
        initMap.put("DaH", R.raw.dah);
        initMap.put("Daj", R.raw.daj);
        initMap.put("Dal", R.raw.dal);
        initMap.put("Dan", R.raw.dan);
        initMap.put("Dap", R.raw.dap);
        initMap.put("Daq", R.raw.daq);
        initMap.put("DaQ", R.raw.dak);
        initMap.put("Darg", R.raw.darg);
        initMap.put("DaS", R.raw.das);
        initMap.put("Dat", R.raw.dat);
        initMap.put("Dav", R.raw.dav);
        initMap.put("Dawz", R.raw.dawz);  // Daw'
        initMap.put("Day", R.raw.day);
        initMap.put("Daz", R.raw.daz);
        initMap.put("Deb", R.raw.deb);
        initMap.put("DeC", R.raw.dec);  // Dech
        initMap.put("Deg", R.raw.deg);
        initMap.put("DeH", R.raw.deh);
        initMap.put("Dej", R.raw.dej);
        initMap.put("Del", R.raw.del);
        initMap.put("Den", R.raw.den);
        initMap.put("Dep", R.raw.dep);
        initMap.put("Deq", R.raw.deq);
        initMap.put("DeQ", R.raw.dek);
        initMap.put("Der", R.raw.der);
        initMap.put("DeS", R.raw.des);
        initMap.put("Dev", R.raw.dev);
        initMap.put("Dez", R.raw.dez);   // De'
        initMap.put("DIb", R.raw.dib);
        initMap.put("DIc", R.raw.dic);
        initMap.put("DIg", R.raw.dig);
        initMap.put("DIj", R.raw.dij);
        initMap.put("DIl", R.raw.dil);
        initMap.put("DIn", R.raw.din);
        initMap.put("DIF", R.raw.dif);
        initMap.put("DIp", R.raw.dip);
        initMap.put("DIr", R.raw.dir);
        initMap.put("DIS", R.raw.dis);
        initMap.put("DIv", R.raw.div);
        initMap.put("DIz", R.raw.diz);  // DI'
        initMap.put("DoC", R.raw.doc);   // Doch
        initMap.put("DoD", R.raw.dod);
        initMap.put("DoG", R.raw.dog);   // Dogh
        initMap.put("DoH", R.raw.doh);
        initMap.put("Doj", R.raw.doj);
        initMap.put("Dol", R.raw.dol);
        initMap.put("Dom", R.raw.dom);
        initMap.put("Don", R.raw.don);
        initMap.put("Dop", R.raw.dop);
        initMap.put("Doq", R.raw.doq);
        initMap.put("DoQ", R.raw.dok);
        initMap.put("Dor", R.raw.dor);
        initMap.put("DoS", R.raw.dos);
        initMap.put("Dox", R.raw.dox);
        initMap.put("Dov", R.raw.dov);
        initMap.put("Doyz", R.raw.doyz);   // Doy'
        initMap.put("Doz", R.raw.doz);   // Do'
        initMap.put("Duj", R.raw.duj);
        initMap.put("DuQ", R.raw.duk);   // DuQ
        initMap.put("Duz", R.raw.duz);   // Du'

        // --- gh ---
        initMap.put("Gab", R.raw.gab);
        initMap.put("GaC", R.raw.gac);   // ghach
        initMap.put("GaG", R.raw.gag);
        initMap.put("GaH", R.raw.gah);
        initMap.put("Gaj", R.raw.gaj);   // ghaj
        initMap.put("Gal", R.raw.gal);
        initMap.put("Gam", R.raw.gam);
        initMap.put("Gan", R.raw.gan);
        initMap.put("GaF", R.raw.gaf);
        initMap.put("Gap", R.raw.gap);   // ghap
        initMap.put("Gaq", R.raw.gaq);
        initMap.put("Gar", R.raw.gar);
        initMap.put("GarG", R.raw.garg);   // ghargh
        initMap.put("Gax", R.raw.gax);
        initMap.put("Gaw", R.raw.gaw);
        initMap.put("Gawz", R.raw.gawz);
        initMap.put("Gay", R.raw.gay);
        initMap.put("Gayz", R.raw.gayz);
        initMap.put("Gaz", R.raw.gaz);
        initMap.put("Geb", R.raw.geb);
        initMap.put("GeD", R.raw.ged);
        initMap.put("GeG", R.raw.geg);
        initMap.put("Gel", R.raw.gel);
        initMap.put("Gem", R.raw.gem);
        initMap.put("Ger", R.raw.ger);
        initMap.put("GeS", R.raw.ges);
        initMap.put("Get", R.raw.get);
        initMap.put("Gev", R.raw.gev);
        initMap.put("Gez", R.raw.gez);   // From {ghe''or}.
        initMap.put("GIx", R.raw.gix);   // ghItlh
        initMap.put("Gob", R.raw.gob);
        initMap.put("GoC", R.raw.goc);
        initMap.put("GoD", R.raw.god);   // ghoD
        initMap.put("GoG", R.raw.gog);
        initMap.put("GoH", R.raw.goh);
        initMap.put("Goj", R.raw.goj);   // ghoj
        initMap.put("Gol", R.raw.gol);
        initMap.put("Gom", R.raw.gom);   // ghom
        initMap.put("Gon", R.raw.gon);
        initMap.put("GoF", R.raw.gof);
        initMap.put("Gop", R.raw.gop);   // ghop
        initMap.put("Goq", R.raw.goq);
        initMap.put("GoQ", R.raw.gok);
        initMap.put("Gor", R.raw.gor);   // ghor
        initMap.put("GorG", R.raw.gorg);   // ghorgh
        initMap.put("GoS", R.raw.gos);   // ghoS
        initMap.put("Got", R.raw.got);
        initMap.put("Gov", R.raw.gov);
        initMap.put("Goz", R.raw.goz);

        initMap.put("GuF", R.raw.guf);   // ghung
        initMap.put("Guz", R.raw.guz);   // ghu'

        // --- H ---
        initMap.put("Hab", R.raw.hab);
        initMap.put("HaC", R.raw.hac);
        initMap.put("HaD", R.raw.had);
        initMap.put("HaG", R.raw.hag);   // Hagh
        initMap.put("HaH", R.raw.hah);
        initMap.put("Haj", R.raw.haj);
        initMap.put("Ham", R.raw.ham);   // DavHam, Hamlet
        initMap.put("Han", R.raw.han);
        initMap.put("Hap", R.raw.hap);
        initMap.put("Haq", R.raw.haq);
        initMap.put("HaQ", R.raw.hak);
        initMap.put("Har", R.raw.har);
        initMap.put("HarG", R.raw.harg);
        initMap.put("HaS", R.raw.has);
        initMap.put("Hat", R.raw.hat);
        initMap.put("Hax", R.raw.hax);
        initMap.put("Hawz", R.raw.hawz);
        initMap.put("Hayz", R.raw.hayz);
        initMap.put("Haz", R.raw.haz);   // Ha'
        initMap.put("HeG", R.raw.heg);   // Hegh
        initMap.put("Hey", R.raw.hey);
        initMap.put("HIq", R.raw.hiq);
        initMap.put("HIv", R.raw.hiv);
        initMap.put("HoC", R.raw.hoc);   // Hoch
        initMap.put("HoH", R.raw.hoh);
        initMap.put("HoH", R.raw.hoh);
        initMap.put("Hol", R.raw.hol);
        initMap.put("Hom", R.raw.hom);
        initMap.put("HoS", R.raw.hos);
        initMap.put("Hov", R.raw.hov);
        initMap.put("Hoz", R.raw.hoz);   // Ho'
        initMap.put("HuH", R.raw.huh);
        initMap.put("Hum", R.raw.hum);
        initMap.put("Hut", R.raw.hut);

        // --- j ---
        initMap.put("jaC", R.raw.jac);   // jach
        initMap.put("jaG", R.raw.jag);   // jagh
        initMap.put("jaH", R.raw.jah);   // jaH
        initMap.put("jaj", R.raw.jaj);
        initMap.put("jav", R.raw.jav);
        initMap.put("jax", R.raw.jax);   // jatlh
        initMap.put("jaz", R.raw.jaz);   // ja'
        // je is short
        initMap.put("jeG", R.raw.jeg);   // jegh
        initMap.put("jemS", R.raw.jems);  // From {jemS tIy qIrq}.
        initMap.put("jey", R.raw.jey);
        initMap.put("jez", R.raw.jez);   // je'
        initMap.put("jIb", R.raw.jib);
        initMap.put("jIH", R.raw.jih);
        initMap.put("jon", R.raw.jon);
        initMap.put("joq", R.raw.joq);
        initMap.put("juH", R.raw.juh);   // juH

        // --- l ---
        initMap.put("laH", R.raw.lah);
        initMap.put("laj", R.raw.laj);   // laj
        initMap.put("larG", R.raw.larg);   // From {veqlargh}.
        initMap.put("lawz", R.raw.lawz);  // law'
        initMap.put("lax", R.raw.lax);   // latlh
        initMap.put("layz", R.raw.layz);   // lay'
        initMap.put("laz", R.raw.laz);   // la', or from {Qapla'}.
        initMap.put("leF", R.raw.lef);   // leng
        initMap.put("leG", R.raw.leg);   // legh
        initMap.put("leH", R.raw.leh);
        initMap.put("lIj", R.raw.lij);
        initMap.put("lIS", R.raw.lis);   // From {qeylIS}.
        initMap.put("lIz", R.raw.liz);   // lI'
        initMap.put("loD", R.raw.lod);   // loD
        initMap.put("loj", R.raw.loj);   // From {lojmIt}.
        initMap.put("loS", R.raw.los);
        initMap.put("loz", R.raw.loz);   // lo'
        initMap.put("luS", R.raw.lus);   // From {luspet}.
        initMap.put("luz", R.raw.luz);   // lu'

        // --- m ---
        initMap.put("maC", R.raw.mac);
        initMap.put("maH", R.raw.mah);
        initMap.put("maj", R.raw.maj);
        initMap.put("mayz", R.raw.mayz);   // may'
        initMap.put("maz", R.raw.maz);   // ma'
        initMap.put("meH", R.raw.meh);
        initMap.put("mej", R.raw.mej);
        initMap.put("mev", R.raw.mev);
        initMap.put("mey", R.raw.mey);
        initMap.put("mIt", R.raw.mit);   // From {lojmIt}.
        initMap.put("mIz", R.raw.miz);
        initMap.put("moH", R.raw.moh);
        initMap.put("moj", R.raw.moj);
        initMap.put("moz", R.raw.moz);   // mo'
        initMap.put("muz", R.raw.muz);   // mu'

        // --- n ---
        initMap.put("naj", R.raw.naj);
        initMap.put("naz", R.raw.naz);   // na'
        initMap.put("neH", R.raw.neh);
        initMap.put("neS", R.raw.nes);   // neS
        initMap.put("nex", R.raw.nex);
        initMap.put("nID", R.raw.nid);
        initMap.put("nIH", R.raw.nih);
        initMap.put("nIS", R.raw.nis);
        initMap.put("nob", R.raw.nob);
        initMap.put("noD", R.raw.nod);
        initMap.put("nom", R.raw.nom);
        initMap.put("noS", R.raw.nos);   // From {Qo'noS}.
        initMap.put("not", R.raw.not);
        initMap.put("noz", R.raw.noz);   // no'
        initMap.put("nuq", R.raw.nuq);

        // --- ng ---
        initMap.put("FaG", R.raw.fag);   // ngagh
        initMap.put("Fan", R.raw.fan);  // From {tlhIngan}, etc.
        initMap.put("FaS", R.raw.fas);   // ngaS
        initMap.put("Fuz", R.raw.fuz);   // ngu'

        // --- p ---
        initMap.put("paG", R.raw.pag);   // pagh
        initMap.put("paw", R.raw.paw);
        initMap.put("paz", R.raw.paz);   // pa'
        initMap.put("peG", R.raw.peg);   // pegh
        initMap.put("pem", R.raw.pem);
        initMap.put("pet", R.raw.pet);   // From {luspet}.
        initMap.put("pIn", R.raw.pin);
        initMap.put("poF", R.raw.pof);
        initMap.put("poS", R.raw.pos);
        initMap.put("puC", R.raw.puc);   // puch
        initMap.put("puq", R.raw.puq);
        initMap.put("puS", R.raw.pus);
        initMap.put("puz", R.raw.puz);   // pu'

        // --- q ---
        initMap.put("qab", R.raw.qab);
        initMap.put("qaD", R.raw.qad);
        initMap.put("qaF", R.raw.qaf);   // qang
        initMap.put("qarD", R.raw.qard);  // From {pIqarD}.
        initMap.put("qaS", R.raw.qas);
        initMap.put("qaw", R.raw.qaw);
        initMap.put("qaz", R.raw.qaz);   // qa'
        initMap.put("qem", R.raw.qem);
        initMap.put("qey", R.raw.qey);   // From {qeylIS}.
        initMap.put("qIm", R.raw.qim);
        initMap.put("qIp", R.raw.qip);
        initMap.put("qIrq", R.raw.qirq);  // From {jemS tIy qIrq}.
        initMap.put("qoH", R.raw.qoh);
        initMap.put("qoj", R.raw.qoj);
        initMap.put("qoq", R.raw.qoq);
        initMap.put("qoS", R.raw.qos);
        initMap.put("qox", R.raw.qox);   // qotlh
        initMap.put("qul", R.raw.qul);
        initMap.put("quv", R.raw.quv);
        initMap.put("quz", R.raw.quz);   // qu'

        // --- Q ---
        initMap.put("QaG", R.raw.kag);   // Qagh
        initMap.put("Qap", R.raw.kap);   // From {Qapla'}.
        initMap.put("Qaz", R.raw.kaz);   // From {majQa'}.
        initMap.put("QIH", R.raw.kih);
        initMap.put("QIt", R.raw.kit);
        initMap.put("Qob", R.raw.kob);
        initMap.put("QoF", R.raw.kof);   // Qong
        initMap.put("Qoy", R.raw.koy);   // Qoy
        initMap.put("Qoz", R.raw.koz);   // Qo'
        initMap.put("QuC", R.raw.kuc);   // Quch
        initMap.put("Qun", R.raw.kun);   // From {chuQun}.
        initMap.put("Quv", R.raw.kuv);

        // --- r ---
        initMap.put("raj", R.raw.raj);
        initMap.put("ram", R.raw.ram);
        initMap.put("raz", R.raw.raz);   // ra'
        initMap.put("reG", R.raw.reg);   // regh
        initMap.put("reH", R.raw.reh);
        initMap.put("ruC", R.raw.ruc);   // ruch
        initMap.put("rup", R.raw.rup);
        initMap.put("rur", R.raw.rur);

        // --- S ---
        initMap.put("SaD", R.raw.sad);
        initMap.put("SaH", R.raw.sah);
        initMap.put("Sayz", R.raw.sayz);   // Say'
        initMap.put("SIQ", R.raw.sik);
        initMap.put("SoC", R.raw.soc);
        initMap.put("SoH", R.raw.soh);
        initMap.put("Soj", R.raw.soj);
        initMap.put("Sop", R.raw.sop);
        initMap.put("SoS", R.raw.sos);
        initMap.put("Sov", R.raw.sov);
        initMap.put("SuD", R.raw.sud);
        initMap.put("Suv", R.raw.suv);

        // --- t ---
        initMap.put("taH", R.raw.tah);
        initMap.put("taj", R.raw.taj);
        initMap.put("tar", R.raw.tar);
        initMap.put("tarG", R.raw.targ);
        initMap.put("tayz", R.raw.tayz);   // tay'
        initMap.put("taz", R.raw.taz);   // ta'
        initMap.put("teb", R.raw.teb);   // From {nIteb}
        initMap.put("tIn", R.raw.tin);
        initMap.put("tIq", R.raw.tiq);
        initMap.put("tIv", R.raw.tiv);
        initMap.put("tIy", R.raw.tiy);  // From {jemS tIy qIrq}.
        initMap.put("toz", R.raw.toz);   // to'
        initMap.put("tuH", R.raw.tuh);
        initMap.put("tuj", R.raw.tuj);
        initMap.put("tup", R.raw.tup);
        initMap.put("turn", R.raw.turn);  // From {Saturn}.
        initMap.put("tuz", R.raw.tuz);

        // --- v ---
        initMap.put("vaD", R.raw.vad);
        initMap.put("vaG", R.raw.vag);
        initMap.put("vaj", R.raw.vaj);
        initMap.put("vam", R.raw.vam);
        initMap.put("van", R.raw.van);
        initMap.put("vav", R.raw.vav);
        initMap.put("vax", R.raw.vax);
        initMap.put("vayz", R.raw.vayz);   // vay'
        initMap.put("veq", R.raw.veq);   // From {veqlargh}.
        initMap.put("vex", R.raw.vex);   // vetlh
        initMap.put("vIH", R.raw.vih);
        initMap.put("vIp", R.raw.vip);
        initMap.put("vIS", R.raw.vis);
        initMap.put("vIz", R.raw.viz);   // From {DIvI'}.
        initMap.put("voG", R.raw.vog);   // vogh
        initMap.put("voz", R.raw.voz);   // vo'

        // --- w ---
        initMap.put("waz", R.raw.waz);
        initMap.put("wej", R.raw.wej);
        initMap.put("wIj", R.raw.wij);
        initMap.put("wIz", R.raw.wiz);   // wI'
        initMap.put("woz", R.raw.woz);   // wo'

        // --- x ---
        initMap.put("xap", R.raw.xap);   // tlhap
        initMap.put("xeG", R.raw.xeg);   // tlhegh
        initMap.put("xux", R.raw.xux);   // tlhutlh

        // --- y ---
        initMap.put("yab", R.raw.yab);
        initMap.put("yaj", R.raw.yaj);
        initMap.put("yap", R.raw.yap);
        initMap.put("yIH", R.raw.yih);
        initMap.put("yIn", R.raw.yin);

        // --- z ---
        initMap.put("zaF", R.raw.zaf);   // 'ang
        initMap.put("zar", R.raw.zar);
        initMap.put("zav", R.raw.zav);   // 'av
        initMap.put("zaz", R.raw.zaz);
        initMap.put("zeb", R.raw.zeb);   // 'eb
        initMap.put("zeG", R.raw.zeg);   // 'egh
        initMap.put("zej", R.raw.zej);   // 'ej
        initMap.put("zez", R.raw.zez);   // 'e'
        initMap.put("zIb", R.raw.zib);
        initMap.put("zIg", R.raw.zig);
        initMap.put("zIH", R.raw.zih);
        initMap.put("zIj", R.raw.zij);
        initMap.put("zIl", R.raw.zil);
        initMap.put("zIm", R.raw.zim);
        initMap.put("zIn", R.raw.zin);
        initMap.put("zIp", R.raw.zip);
        initMap.put("zIq", R.raw.ziq);
        initMap.put("zIQ", R.raw.zik);
        initMap.put("zIr", R.raw.zir);
        initMap.put("zIS", R.raw.zis);
        initMap.put("zIt", R.raw.zit);
        initMap.put("zIx", R.raw.zix);
        initMap.put("zIv", R.raw.ziv);
        initMap.put("zIz", R.raw.ziz);

        initMap.put("zIw", R.raw.ziw);
        initMap.put("zob", R.raw.zob);
        initMap.put("zoC", R.raw.zoc);
        initMap.put("zoD", R.raw.zod);
        initMap.put("zoG", R.raw.zog);
        initMap.put("zoH", R.raw.zoh);
        initMap.put("zoj", R.raw.zoj);   // 'oj
        initMap.put("zol", R.raw.zol);
        initMap.put("zom", R.raw.zom);
        initMap.put("zof", R.raw.zof);
        initMap.put("zop", R.raw.zop);
        initMap.put("zoQ", R.raw.zok);
        initMap.put("zor", R.raw.zor);   // From {ghe''or}.
        initMap.put("zoS", R.raw.zos);
        initMap.put("zov", R.raw.zov);
        initMap.put("zox", R.raw.zox);
        initMap.put("zoyz", R.raw.zoyz);
        initMap.put("zoz", R.raw.zoz);
        initMap.put("zuC", R.raw.zuc);
        initMap.put("zuD", R.raw.zud);
        initMap.put("zuG", R.raw.zug);
        initMap.put("zuH", R.raw.zuh);
        initMap.put("zuj", R.raw.zuj);
        initMap.put("zul", R.raw.zul);
        initMap.put("zum", R.raw.zum);
        initMap.put("zun", R.raw.zun);
        initMap.put("zup", R.raw.zup);
        initMap.put("zuQ", R.raw.zuk);   // 'uQ
        initMap.put("zur", R.raw.zur);
        initMap.put("zuS", R.raw.zus);   // 'uS
        initMap.put("zut", R.raw.zut);
        initMap.put("zux", R.raw.zux);
        initMap.put("zuy", R.raw.zuy);
        initMap.put("zuyz", R.raw.zuyz);
        initMap.put("zuz", R.raw.zuz);

        MAIN_SYLLABLE_TO_AUDIO_MAP = Collections.unmodifiableMap(initMap);
    }

    @Override
    public void onCreate() {
        super.onCreate();
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
        Log.d(TAG, "---\n");
        Log.d(TAG, "condensedText: \"" + condensedText + "\"");
        boolean isFinalSyllable = true;
        while (!condensedText.equals("")) {
            // Syllables in the main syllable map must have length 3 or 4 (CVC or CVCC).
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
                    Log.d(TAG, "Matched tail: {" + tail + "}");
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

                    // If the syllable is CV, then it is a short syllable. If it is CV-, then it is an unattached prefix.
                    boolean isShortSyllable = syllableBack.equals(vowel);
                    boolean isUnattachedPrefix = syllableBack.equals(vowel + "-");
                    Log.d(TAG, "Syllable: {" + syllable + "}");
                    Log.d(TAG, "Syllable front: {" + syllableFront + "}");
                    Log.d(TAG, "Syllable back: {" + syllableBack + "}");
                    Log.d(TAG, "Vowel: {" + vowel + "}");
                    Log.d(TAG, "isShortSyllable: {" + isShortSyllable + "}");
                    Log.d(TAG, "isUnattachedPrefix: {" + isUnattachedPrefix + "}");
                    Log.d(TAG, "isFinalSyllable: {" + isFinalSyllable + "}");
                    if (isShortSyllable && !isFinalSyllable || isUnattachedPrefix) {
                        // If it's a short syllable but not the final syllable, then truncate the vowel.
                        // This is either an attached prefix, part of a multisyllabic word, or a CV verb attached to a suffix.
                        // The audio for {bo-} takes precedence over the audio for {bo} if it's the beginning part of a word.
                        // Also treat unattached prefixes the same way.
                        if (frontResId != null) {
                            prependSyllableToList(frontResId);
                            Log.d(TAG, "Matched syllable: {" + syllableFront + "-}");
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
                            // We have a full short syllable, so play it. Ex: {bo}.
                            prependSyllableToList(resId);
                            Log.d(TAG, "Matched syllable: {" + syllable + "}");
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
                            Log.d(TAG, "Matched syllable: {" + syllableFront + "-" + syllableBack + "}");
                        }
                    }

                    // Now the next match won't be the final syllable in a word.
                    isFinalSyllable = false;
                    foundMatch = true;
                }
            }
            if (!foundMatch) {
                // No match for a complete syllable. Use a fallback sound. This should be avoided if possible.
                char value = condensedText.charAt(condensedText.length() - 1);
                condensedText = condensedText.substring(0, condensedText.length() - 1);
                prependSyllableToList(getResIdForFallbackChar(value));
                Log.d(TAG, "Stripped char: \"" + value + "\"");

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
        // Log.d(TAG, "removeTailSyllable from: {" + input + "}");

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
            // Ends in something other than a vowel. Assume it's a consonant, unless it's a space.
            tail = remainingText.substring(remainingText.length() - 1);
            if (tail.equals(" ")) {
                // Not a valid final consonant.
                return "";
            }
            remainingText = remainingText.substring(0, remainingText.length() - 1);
        }
        // Log.d(TAG, "After ending: {" + remainingText + " / " + tail + "}");

        // Look for the vowel.
        if (remainingText.length() < 2 ||
            !isSimpleVowel(remainingText.charAt(remainingText.length() - 1))) {
            // Failed to extract a syllable from the tail.
            return "";
        }
        tail = remainingText.substring(remainingText.length() - 1) + tail;
        remainingText = remainingText.substring(0, remainingText.length() - 1);
        // Log.d(TAG, "After middle: {" + remainingText + " / " + tail + "}");

        // Look for the initial consonant.
        if (remainingText.length() < 1 ||
            isSimpleVowel(remainingText.charAt(remainingText.length() - 1))) {
            // Also a failure.
            return "";
        }
        tail = remainingText.substring(remainingText.length() - 1) + tail;
        remainingText = remainingText.substring(0, remainingText.length() - 1);
        // Log.d(TAG, "After beginning: {" + remainingText + " / " + tail + "}");

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
        return input.replaceAll("[^A-Za-z'\\-]+", " ")  // Strip all non-alphabetical characters (except {'} and "-").
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
