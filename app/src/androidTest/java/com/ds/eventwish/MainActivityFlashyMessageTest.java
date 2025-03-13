package com.ds.eventwish;

import android.content.Context;

import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ds.eventwish.data.local.FlashyMessageDatabase;
import com.ds.eventwish.utils.FlashyMessageManager;
import com.ds.eventwish.utils.EspressoIdlingResource;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class MainActivityFlashyMessageTest {
    private Context context;
    private FlashyMessageDatabase database;
    private IdlingResource idlingResource;

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
        database = FlashyMessageDatabase.getInstance(context);
        
        // Clear all messages before each test
        database.flashyMessageDao().deleteAll();
        
        // Register idling resource
        idlingResource = EspressoIdlingResource.getIdlingResource();
        IdlingRegistry.getInstance().register(idlingResource);
    }

    @After
    public void cleanup() {
        // Clear all messages after each test
        database.flashyMessageDao().deleteAll();
        
        // Unregister idling resource
        if (idlingResource != null) {
            IdlingRegistry.getInstance().unregister(idlingResource);
        }
    }

    @Test
    public void testFlashyMessageDisplay() {
        // Save a test message
        String testId = "test_id";
        String testTitle = "Test Title";
        String testMessage = "Test Message";
        FlashyMessageManager.saveFlashyMessage(context, testId, testTitle, testMessage);

        // Launch activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify flashy message dialog is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        onView(withId(R.id.flashy_message_title))
                .check(matches(withText(testTitle)));

        onView(withId(R.id.flashy_message_content))
                .check(matches(withText(testMessage)));
    }

    @Test
    public void testNoFlashyMessageWhenEmpty() {
        // Launch activity without any messages
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify no flashy message dialog is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testMultipleFlashyMessages() {
        // Save multiple test messages
        FlashyMessageManager.saveFlashyMessage(context, "id1", "Title 1", "Message 1");
        FlashyMessageManager.saveFlashyMessage(context, "id2", "Title 2", "Message 2");

        // Launch activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify first message is displayed
        onView(withId(R.id.flashy_message_title))
                .check(matches(withText("Title 1")));

        // Dismiss first message
        onView(withId(R.id.flashy_message_dismiss))
                .perform(click());

        // Wait for animation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify second message is displayed
        onView(withId(R.id.flashy_message_title))
                .check(matches(withText("Title 2")));
    }

    @Test
    public void testFlashyMessagePersistence() {
        // Save a test message
        FlashyMessageManager.saveFlashyMessage(context, "test_id", "Test Title", "Test Message");

        // Launch activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify message is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        // Recreate activity
        scenario.recreate();

        // Verify message is still displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testFlashyMessageBackground() {
        // Save a test message
        FlashyMessageManager.saveFlashyMessage(context, "test_id", "Test Title", "Test Message");

        // Launch activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Move activity to background
        scenario.moveToState(Lifecycle.State.CREATED);

        // Move activity back to foreground
        scenario.moveToState(Lifecycle.State.RESUMED);

        // Verify message is displayed again
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testFlashyMessageDismissal() {
        // Save a test message
        FlashyMessageManager.saveFlashyMessage(context, "test_id", "Test Title", "Test Message");

        // Launch activity
        ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class);

        // Verify message is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        // Dismiss message
        onView(withId(R.id.flashy_message_dismiss))
                .perform(click());

        // Wait for animation
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify message is dismissed
        onView(withId(R.id.flashy_message_container))
                .check(matches(not(isDisplayed())));

        // Recreate activity
        scenario.recreate();

        // Verify message stays dismissed
        onView(withId(R.id.flashy_message_container))
                .check(matches(not(isDisplayed())));
    }
} 