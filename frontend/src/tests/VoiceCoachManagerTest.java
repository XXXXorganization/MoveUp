package com.zjgsu.moveup;

import android.speech.tts.TextToSpeech;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class VoiceCoachManagerTest {

    @Test
    public void testVoiceCoach_InitAndSpeak() {
        // 测试单例获取
        VoiceCoachManager manager = VoiceCoachManager.getInstance(RuntimeEnvironment.application);
        assertNotNull(manager);

        // 模拟 TTS 引擎初始化成功
        manager.onInit(TextToSpeech.SUCCESS);

        // 模拟触发说话和关闭
        manager.speak("加油，保持配速！");
        manager.shutdown();
    }
}