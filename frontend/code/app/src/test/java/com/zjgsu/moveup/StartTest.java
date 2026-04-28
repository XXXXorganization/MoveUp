package com.zjgsu.moveup;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowApplication;

import static org.junit.Assert.assertEquals;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class StartTest {

    @Test
    public void testClickLogin_TransitionsToLogin() {
        Start activity = Robolectric.buildActivity(Start.class).create().resume().get();
        activity.findViewById(R.id.btn_login).performClick();

        Intent expectedIntent = new Intent(activity, Login.class);
        assertEquals(expectedIntent.getComponent(), ShadowApplication.getInstance().getNextStartedActivity().getComponent());
    }

    @Test
    public void testClickJoin_TransitionsToRegister() {
        Start activity = Robolectric.buildActivity(Start.class).create().resume().get();
        activity.findViewById(R.id.btn_join).performClick();

        Intent expectedIntent = new Intent(activity, Register.class);
        assertEquals(expectedIntent.getComponent(), ShadowApplication.getInstance().getNextStartedActivity().getComponent());
    }
}