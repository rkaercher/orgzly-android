package com.orgzly.android.espresso;


import android.support.test.rule.ActivityTestRule;

import com.orgzly.R;
import com.orgzly.android.OrgzlyTest;
import com.orgzly.android.ui.ReposActivity;

import org.junit.Rule;
import org.junit.Test;

import static android.support.test.espresso.Espresso.onData;
import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.action.ViewActions.replaceText;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isEnabled;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static com.orgzly.android.espresso.EspressoUtils.onActionItemClick;
import static org.hamcrest.Matchers.anything;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasToString;
import static org.hamcrest.Matchers.not;

public class GitRepoFragmentTest extends OrgzlyTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(ReposActivity.class, true, false);

    private void assertDisabled(int elementId) {
        onView(withId(elementId)).check(matches(not(isEnabled())));
    }

    private void assertEnabled(int elementId) {
        onView(withId(elementId)).check(matches(isEnabled()));
    }

    @Test
    public void testGitRepoSsh() {
        String localDir = "/Documents/testGitRepo";

        activityRule.launchActivity(null);

        navigateToNewGitRepository();

        assertDisabled(R.id.fragment_repo_git_test_login);
        assertDisabled(R.id.fragment_repo_git_copy_pubkey);

        onView(withId(R.id.fragment_repo_git_directory_browse_button)).perform(click());
        onData(hasToString(containsString("Download"))).perform(click());
        onView(withId(R.id.browser_button_use)).perform(click());

        onView(withId(R.id.fragment_repo_git_local_directory)).check(matches(withText(endsWith("Download"))));

        assertDisabled(R.id.fragment_repo_git_test_login);

        selectTransportAuthMethod(0); // SSH Keys

        onView(withId(R.id.fragment_repo_git_generate_keys)).perform(click());

        assertDisabled(R.id.fragment_repo_git_test_login);
        assertEnabled(R.id.fragment_repo_git_copy_pubkey);

        onView(withId(R.id.fragment_repo_git_remote_uri)).perform(replaceText("git@repohost:1234/a/path"));

        assertEnabled(R.id.fragment_repo_git_test_login);

        selectTransportAuthMethod(1); // SSH Password

        assertDisabled(R.id.fragment_repo_git_test_login);

        onView(withId(R.id.fragment_repo_git_password_text)).perform(replaceText("aPassword"));

        assertEnabled(R.id.fragment_repo_git_test_login);

        onView(withId(R.id.done)).perform(click());
    }

    private void selectTransportAuthMethod(int atPosition) {
        onView(withId(R.id.fragment_repo_git_transport_auth_selection_spinner)).perform(click());
        onData(anything()).atPosition(atPosition).perform(click());
    }

    private void navigateToNewGitRepository() {
        onActionItemClick(R.id.repos_options_menu_item_new, R.string.repos_options_menu_new_repo);
        onView(withText(R.string.git_repository)).perform(click());
    }
}
