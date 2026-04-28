package com.zjgsu.moveup;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import java.util.HashMap;
import java.util.Locale;

public class VoiceCoachManager implements TextToSpeech.OnInitListener {
    private static VoiceCoachManager instance;
    private TextToSpeech tts;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private boolean isTtsInitialized = false;

    private VoiceCoachManager(Context context) {
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.tts = new TextToSpeech(context.getApplicationContext(), this);
    }

    public static VoiceCoachManager getInstance(Context context) {
        if (instance == null) instance = new VoiceCoachManager(context);
        return instance;
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINA);
            if (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED) {
                isTtsInitialized = true;
                tts.setPitch(1.2f); // 稍微调高音调，显得更有活力
                tts.setSpeechRate(1.1f);
                setupTtsListener();
            }
        }
    }

    public void speak(String text) {
        if (!isTtsInitialized || text == null || text.isEmpty()) return;
        requestAudioFocus();
        HashMap<String, String> params = new HashMap<>();
        params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "moveup_coach");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
    }

    private void setupTtsListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { abandonAudioFocus(); }
            @Override public void onError(String utteranceId) { abandonAudioFocus(); }
        });
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs).setAcceptsDelayedFocusGain(true).setOnAudioFocusChangeListener(f -> {}).build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
    }

    private void abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
            audioManager.abandonAudioFocusRequest(audioFocusRequest);
        } else {
            audioManager.abandonAudioFocus(null);
        }
    }

    public void shutdown() {
        if (tts != null) { tts.stop(); tts.shutdown(); }
        abandonAudioFocus();
    }
}