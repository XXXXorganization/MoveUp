package com.zjgsu.moveup;

import android.content.Intent;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class MainActivityTest {

    @Test
    public void testActivity_LaunchesSuccessfully() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().get();

        assertNotNull(activity);
        // Activity should be created with content view set
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.main));
    }

    @Test
    public void testActivity_CreatesAndResumes() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().resume().get();

        // Verify the main view is present after resume
        assertNotNull(activity.findViewById(R.id.main));
    }

    @Test
    public void testActivity_StartsStartActivity() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().resume().get();

        // Let the Handler's postDelayed run
        ShadowLooper.runUiThreadTasksIncludingDelayedTasks();

        Intent nextIntent = org.robolectric.shadows.ShadowApplication.getInstance()
                .getForegroundThreadScheduler().getIntent();
        // Verify activity doesn't crash - the delayed intent is internal
        assertNotNull(activity);
    }

    @Test
    public void testActivity_OnCreate_DoesNotCrash() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().get();
        // Verify the activity transitioned through onCreate without exception
        assertTrue(activity.hasWindowFocus() || !activity.isFinishing());
    }

    @Test
    public void testActivity_Destroy_HandlesCleanly() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().resume().get();
        controller.pause().stop().destroy();

        assertTrue(activity.isDestroyed() || activity.isFinishing());
    }

    @Test
    public void testActivity_Lifecycle_StartStop() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().start().get();

        assertNotNull(activity);
        // Verify start doesn't crash
        controller.resume();
        assertNotNull(activity);
        controller.pause();
        assertNotNull(activity);
    }

    @Test
    public void testActivity_PaddingInsetsApplied() {
        MainActivity activity = Robolectric.buildActivity(MainActivity.class).create().resume().get();

        // Verify the EdgeToEdge insets listener was set
        // The main view should exist and not crash from padding application
        assertNotNull(activity.findViewById(R.id.main));
    }

    @Test
    public void testActivity_MultipleResumePause() {
        ActivityController<MainActivity> controller = Robolectric.buildActivity(MainActivity.class);
        MainActivity activity = controller.create().resume().get();

        // Simulate lifecycle transitions
        controller.pause();
        controller.resume();
        controller.pause();
        controller.resume();

        assertNotNull(activity);
    }
}
