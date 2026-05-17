package com.zjgsu.moveup;

import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class VoiceCoachManager implements TextToSpeech.OnInitListener {
    private static VoiceCoachManager instance;
    private Context context;
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;

    private boolean isTtsInitialized = false;
    private boolean isSpeaking = false; // 核心锁：判断 AI 是否正在说话
    private boolean isRecognizerAvailable = false; // 标记设备是否支持语音识别

    private VoiceListener voiceListener;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface VoiceListener {
        void onRecognized(String text);
        void onStatus(String status);
    }

    private VoiceCoachManager(Context context) {
        this.context = context.getApplicationContext();
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        this.tts = new TextToSpeech(this.context, this);
        mainHandler.post(this::initSpeechRecognizer);
    }

    public static VoiceCoachManager getInstance(Context context) {
        if (instance == null) instance = new VoiceCoachManager(context);
        return instance;
    }

    public void setVoiceListener(VoiceListener listener) {
        this.voiceListener = listener;
    }

    private void initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.e("VoiceCoach", "❌ 致命错误：当前设备没有语音识别引擎！");
            isRecognizerAvailable = false;
            return;
        }

        isRecognizerAvailable = true;
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context);
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override public void onReadyForSpeech(Bundle params) {
                if (voiceListener != null) voiceListener.onStatus("Listening");
            }
            @Override public void onBeginningOfSpeech() {}
            @Override public void onRmsChanged(float rmsdB) {}
            @Override public void onBufferReceived(byte[] buffer) {}
            @Override public void onEndOfSpeech() {
                if (voiceListener != null) voiceListener.onStatus("Idle");
            }

            @Override public void onError(int error) {
                if (voiceListener != null) voiceListener.onStatus("Idle");

                String errorMsg;
                switch (error) {
                    case SpeechRecognizer.ERROR_AUDIO: errorMsg = "音频录制错误"; break;
                    case SpeechRecognizer.ERROR_CLIENT: errorMsg = "客户端设备错误(环境问题)"; break;
                    case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: errorMsg = "权限不足(未授予录音权限)"; break;
                    case SpeechRecognizer.ERROR_NETWORK: errorMsg = "网络异常(无法连接Google)"; break;
                    case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: errorMsg = "网络连接超时"; break;
                    case SpeechRecognizer.ERROR_NO_MATCH: errorMsg = "没听清你说什么"; break;
                    case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: errorMsg = "识别引擎忙碌中"; break;
                    case SpeechRecognizer.ERROR_SERVER: errorMsg = "语音服务器错误"; break;
                    case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: errorMsg = "未检测到说话声音"; break;
                    case 12: errorMsg = "设备不支持该语言(缺少中文包)"; break;
                    case 13: errorMsg = "语言暂时不可用"; break;
                    default: errorMsg = "其他错误: " + error; break;
                }

                Log.e("VoiceCoach", "❌ [语音识别失败] " + errorMsg);
                mainHandler.post(() -> Toast.makeText(context, "录音失败: " + errorMsg, Toast.LENGTH_SHORT).show());
            }

            @Override public void onResults(Bundle results) {
                if (voiceListener != null) voiceListener.onStatus("Idle");
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String recognizedText = matches.get(0);
                    Log.d("VoiceCoach", "🎙️ [语音识别] 听到用户说: " + recognizedText);
                    if (voiceListener != null) voiceListener.onRecognized(recognizedText);
                }
            }
            @Override public void onPartialResults(Bundle partialResults) {}
            @Override public void onEvent(int eventType, Bundle params) {}
        });
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.CHINA);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mainHandler.post(() -> Toast.makeText(context, "⚠️ 手机缺少中文语音包，AI无法发声！", Toast.LENGTH_LONG).show());
            } else {
                isTtsInitialized = true;
                tts.setPitch(1.1f);
                tts.setSpeechRate(1.1f);
                setupTtsListener();
            }
        }
    }

    public void startListening() {
        if (!isRecognizerAvailable) {
            mainHandler.post(() -> Toast.makeText(context, "当前设备不支持语音录入", Toast.LENGTH_SHORT).show());
            return;
        }
        if (isSpeaking) {
            mainHandler.post(() -> Toast.makeText(context, "AI正在说话，请稍候", Toast.LENGTH_SHORT).show());
            return;
        }
        mainHandler.post(() -> {
            try {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "zh-CN");

                if (speechRecognizer != null) {
                    speechRecognizer.startListening(intent);
                }
            } catch (Exception e) {
                Log.e("VoiceCoach", "Start error", e);
            }
        });
    }

    public void stopListening() {
        mainHandler.post(() -> { if (speechRecognizer != null) speechRecognizer.stopListening(); });
        if (voiceListener != null) voiceListener.onStatus("Idle");
    }

    public void cancelListening() {
        mainHandler.post(() -> { if (speechRecognizer != null) speechRecognizer.cancel(); });
        if (voiceListener != null) voiceListener.onStatus("Idle");
    }

    public void speak(String text) {
        if (!isTtsInitialized || text == null || text.isEmpty()) return;
        Log.d("VoiceCoach", "🔊 [语音合成] AI准备说: " + text);
        isSpeaking = true;
        cancelListening();
        requestAudioFocus();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "moveup_coach");
        } else {
            HashMap<String, String> params = new HashMap<>();
            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "moveup_coach");
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, params);
        }
    }

    private void setupTtsListener() {
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override public void onStart(String utteranceId) {}
            @Override public void onDone(String utteranceId) { abandonAudioFocus(); isSpeaking = false; }
            @Override public void onError(String utteranceId) { abandonAudioFocus(); isSpeaking = false; }
        });
    }

    private void requestAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AudioAttributes attrs = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build();
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attrs)
                    .setAcceptsDelayedFocusGain(true)
                    // 🌟 修复闪退的核心代码：补上强制要求的监听器
                    .setOnAudioFocusChangeListener(focusChange -> {})
                    .build();
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
        cancelListening();
        if (speechRecognizer != null) speechRecognizer.destroy();
        if (tts != null) { tts.stop(); tts.shutdown(); }
        abandonAudioFocus();
    }
}