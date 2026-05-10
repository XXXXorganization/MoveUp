package com.zjgsu.moveup;

import android.content.Context;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27) // 🌟 全局统一使用高版本环境，彻底避开老版本 PackageParser 的崩溃 bug
public class VoiceCoachManagerTest {

    private VoiceCoachManager manager;
    private TextToSpeech mockTts;
    private AudioManager mockAudioManager;

    @Before
    public void setUp() throws Exception {
        // 1. 通过反射清空单例 instance，保证每个测试都是纯净的环境
        Field instanceField = VoiceCoachManager.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, null);

        // 2. 覆盖 getInstance 的 if (instance == null) 分支
        Context context = RuntimeEnvironment.application;
        manager = VoiceCoachManager.getInstance(context);

        // 覆盖 getInstance 的 else 分支 (已有实例时直接返回)
        VoiceCoachManager.getInstance(context);

        // 3. 创建 Mock 对象
        mockTts = mock(TextToSpeech.class);
        mockAudioManager = mock(AudioManager.class);

        // 4. 通过反射将 Mock 对象注入到 manager 中
        Field ttsField = VoiceCoachManager.class.getDeclaredField("tts");
        ttsField.setAccessible(true);
        ttsField.set(manager, mockTts);

        Field audioManagerField = VoiceCoachManager.class.getDeclaredField("audioManager");
        audioManagerField.setAccessible(true);
        audioManagerField.set(manager, mockAudioManager);
    }

    // ==========================================
    // 1. 测试初始化成功、正常播报、以及回调监听
    // ==========================================
    @Test
    public void testOnInit_Success_And_Speak() {
        // 模拟语言包可用
        when(mockTts.setLanguage(Locale.CHINA)).thenReturn(TextToSpeech.LANG_AVAILABLE);

        // 触发成功初始化
        manager.onInit(TextToSpeech.SUCCESS);

        // 验证音调和语速被正确设置
        verify(mockTts).setPitch(1.2f);
        verify(mockTts).setSpeechRate(1.1f);

        // 拦截内部生成的 UtteranceProgressListener 回调
        ArgumentCaptor<UtteranceProgressListener> captor = ArgumentCaptor.forClass(UtteranceProgressListener.class);
        verify(mockTts).setOnUtteranceProgressListener(captor.capture());
        UtteranceProgressListener listener = captor.getValue();

        // 测试播报语音
        manager.speak("加油！");

        // 验证申请了高级别的音频焦点并执行了 speak
        verify(mockAudioManager).requestAudioFocus(any(AudioFocusRequest.class));
        verify(mockTts).speak(eq("加油！"), eq(TextToSpeech.QUEUE_FLUSH), any(HashMap.class));

        // 测试回调逻辑：语音开始、完成、报错
        listener.onStart("moveup_coach"); // 空方法覆盖
        listener.onDone("moveup_coach");  // 触发释放焦点
        verify(mockAudioManager).abandonAudioFocusRequest(any(AudioFocusRequest.class));

        listener.onError("moveup_coach"); // 再次触发释放焦点
    }

    // ==========================================
    // 2. 测试初始化失败的各种边缘分支
    // ==========================================
    @Test
    public void testOnInit_Failures_And_InvalidSpeak() {
        // A. 引擎初始化失败分支
        manager.onInit(TextToSpeech.ERROR);

        // B. 语言包缺失分支
        when(mockTts.setLanguage(Locale.CHINA)).thenReturn(TextToSpeech.LANG_MISSING_DATA);
        manager.onInit(TextToSpeech.SUCCESS);

        // C. 语言不支持分支
        when(mockTts.setLanguage(Locale.CHINA)).thenReturn(TextToSpeech.LANG_NOT_SUPPORTED);
        manager.onInit(TextToSpeech.SUCCESS);

        // D. 此时 tts 未成功初始化，speak 应当直接 return
        manager.speak("无法说话");

        // E. 测试 null 和空字符串防御分支
        manager.speak(null);
        manager.speak("");

        // 验证 tts.speak 绝对没有被调用过
        verify(mockTts, never()).speak(any(String.class), eq(TextToSpeech.QUEUE_FLUSH), any(HashMap.class));
    }

    // ==========================================
    // 3. 测试旧版本系统的音频焦点逻辑 (SDK < 26 分支)
    // ==========================================
    @Test
    public void testLegacyAudio_And_Shutdown() {
        // 🌟 终极黑科技：利用 ReflectionHelpers 强行修改系统常量
        // 把运行环境强行伪装成 API 25，从而触发 VoiceCoachManager 中的 else 老代码分支！
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 25);

        when(mockTts.setLanguage(Locale.CHINA)).thenReturn(TextToSpeech.LANG_AVAILABLE);
        manager.onInit(TextToSpeech.SUCCESS);

        // 测试老版本的请求焦点
        manager.speak("旧手机播报");
        verify(mockAudioManager).requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);

        // 测试老版本的释放焦点
        manager.shutdown();
        verify(mockAudioManager).abandonAudioFocus(null);

        // 验证 tts 资源被释放
        verify(mockTts).stop();
        verify(mockTts).shutdown();

        // 测试完毕后，把系统版本号还原为 27，以免影响其他测试文件
        ReflectionHelpers.setStaticField(Build.VERSION.class, "SDK_INT", 27);
    }

    // ==========================================
    // 4. 测试极端防御分支 (当内部对象为空时)
    // ==========================================
    @Test
    public void testShutdown_WithNullTts() throws Exception {
        // 用反射强行把 tts 设置为 null，覆盖 shutdown() 中的 if (tts != null) 的 false 分支
        Field ttsField = VoiceCoachManager.class.getDeclaredField("tts");
        ttsField.setAccessible(true);
        ttsField.set(manager, null);

        // 覆盖 abandonAudioFocus() 中 audioFocusRequest 为 null 的安全分支
        manager.shutdown();

        // 执行完没有崩溃即代表覆盖成功
    }
}