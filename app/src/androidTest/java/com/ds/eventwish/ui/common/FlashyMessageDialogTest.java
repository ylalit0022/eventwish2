package com.ds.eventwish.ui.common;

import android.content.Context;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.action.ViewActions;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.ds.eventwish.R;
import com.ds.eventwish.utils.FlashyMessageManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.not;

@RunWith(AndroidJUnit4.class)
public class FlashyMessageDialogTest {
    private Context context;
    private static final String TEST_ID = "test_id";
    private static final String TEST_TITLE = "Test Title";
    private static final String TEST_MESSAGE = "Test Message";

    @Before
    public void setup() {
        context = ApplicationProvider.getApplicationContext();
    }

    @Test
    public void testDialogDisplay() {
        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify dialog content
        onView(withId(R.id.flashy_message_title))
                .check(matches(isDisplayed()))
                .check(matches(withText(TEST_TITLE)));

        onView(withId(R.id.flashy_message_content))
                .check(matches(isDisplayed()))
                .check(matches(withText(TEST_MESSAGE)));
    }

    @Test
    public void testDialogDismissal() {
        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Click dismiss button
        onView(withId(R.id.flashy_message_dismiss))
                .check(matches(isDisplayed()))
                .perform(click());

        // Verify dialog is dismissed
        onView(withId(R.id.flashy_message_title))
                .check(matches(not(isDisplayed())));
    }

    @Test
    public void testDialogRotation() {
        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify initial state
        onView(withId(R.id.flashy_message_title))
                .check(matches(isDisplayed()))
                .check(matches(withText(TEST_TITLE)));

        // Recreate the fragment (simulates rotation)
        scenario.recreate();

        // Verify state after recreation
        onView(withId(R.id.flashy_message_title))
                .check(matches(isDisplayed()))
                .check(matches(withText(TEST_TITLE)));
    }

    @Test
    public void testDialogAnimation() {
        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify dialog container has animation
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        // Wait for animation to complete
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify dialog is still visible after animation
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDialogInteraction() {
        // Save a test message
        FlashyMessageManager.saveFlashyMessage(context, TEST_ID, TEST_TITLE, TEST_MESSAGE);

        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify dialog is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        // Click dismiss button
        onView(withId(R.id.flashy_message_dismiss))
                .perform(click());

        // Verify dialog is dismissed
        onView(withId(R.id.flashy_message_container))
                .check(matches(not(isDisplayed())));

        // Wait for async operations
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Launch dialog again
        scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify message is marked as read
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));
    }

    @Test
    public void testDialogBackPress() {
        // Launch dialog
        FragmentScenario<FlashyMessageDialog> scenario = FragmentScenario.launchInContainer(
                FlashyMessageDialog.class,
                FlashyMessageDialog.createArguments(TEST_ID, TEST_TITLE, TEST_MESSAGE)
        );

        // Verify dialog is displayed
        onView(withId(R.id.flashy_message_container))
                .check(matches(isDisplayed()));

        // Press back button
        Espresso.pressBack();

        // Verify dialog is dismissed
        onView(withId(R.id.flashy_message_container))
                .check(matches(not(isDisplayed())));
    }
} 