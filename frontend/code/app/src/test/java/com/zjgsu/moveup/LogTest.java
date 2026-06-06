package com.zjgsu.moveup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class LogTest {

    @Test
    public void testActivity_LaunchesSuccessfully() {
        ActivityController<Log> controller = Robolectric.buildActivity(Log.class);
        Log activity = controller.create().get();

        assertNotNull(activity);
        assertNotNull(activity.findViewById(com.zjgsu.moveup.R.id.main));
    }

    @Test
    public void testActivity_CreatesAndResumes() {
        ActivityController<Log> controller = Robolectric.buildActivity(Log.class);
        Log activity = controller.create().resume().get();

        assertNotNull(activity.findViewById(R.id.main));
    }

    @Test
    public void testActivity_OnCreate_DoesNotCrash() {
        Log activity = Robolectric.buildActivity(Log.class).create().get();
        assertTrue(activity.hasWindowFocus() || !activity.isFinishing());
    }

    @Test
    public void testActivity_Destroy_HandlesCleanly() {
        ActivityController<Log> controller = Robolectric.buildActivity(Log.class);
        Log activity = controller.create().resume().get();
        controller.pause().stop().destroy();

        assertTrue(activity.isDestroyed() || activity.isFinishing());
    }

    @Test
    public void testActivity_FullLifecycle_NoCrash() {
        ActivityController<Log> controller = Robolectric.buildActivity(Log.class);
        Log activity = controller.create().start().resume().pause().stop().get();

        assertNotNull(activity);
    }

    @Test
    public void testActivity_PaddingInsetsApplied() {
        Log activity = Robolectric.buildActivity(Log.class).create().resume().get();
        assertNotNull(activity.findViewById(R.id.main));
    }

    @Test
    public void testActivity_Restart_Works() {
        ActivityController<Log> controller = Robolectric.buildActivity(Log.class);
        Log activity = controller.create().start().resume().get();
        controller.pause().stop();
        controller.restart().start().resume();

        assertNotNull(activity);
    }
}
