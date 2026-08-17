/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.BasePage;
import com.sonatype.clm.testing.playwright.pages.UserActivityDetailsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.UserActivityOverviewPage;
import com.sonatype.clm.testing.playwright.pages.UserActivityOverviewPageAssertions;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.InsightConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Playwright regression tests for the User Activity Overview screen
 * (Administration → Users → Activity tab, URL fragment {@code /users/activity}).
 * <p>
 * Sanity-level tests (basic page render, tab navigation) live in
 * {@link UserManagementPlaywrightTest}.
 */
public class UserActivityOverviewPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String NO_MATCH_SEARCH_TERM = "zzz-no-match-user-zzz";

  private static final String TIME_FRAME_PAST_7_DAYS = "past 7 days";

  /**
   * Second audit-log user seeded with 1 login (vs admin's 2).
   * Enables observable row-order verification in the sort-toggle test:
   * descending → admin first; ascending → this user first.
   */
  private static final String SECOND_ACTIVITY_USER = "playwright-activity-user";

  private UserActivityOverviewPage overviewPage;

  private UserActivityOverviewPageAssertions overviewAssertions;

  /** Path to the seeded audit log file; deleted in {@link #tearDown()}. */
  private Path auditLogFile;

  private boolean originalTracking;

  @BeforeEach
  public void setUp() throws IOException {
    originalTracking = SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.isEnabled();
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    writeAdminAuditLog();
    playwrightRefreshOrOpen(UserActivityOverviewPage.url());
    playwrightLogin();
    // Reload after login so the page fetches fresh data with the seeded audit file.
    playwrightRefreshOrOpen(UserActivityOverviewPage.url());
    overviewPage = new UserActivityOverviewPage();
    overviewAssertions = new UserActivityOverviewPageAssertions(overviewPage);
    overviewPage.overviewTable().waitFor();
  }

  @AfterEach
  public void tearDown() throws IOException {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(originalTracking);
    if (auditLogFile != null) {
      Files.deleteIfExists(auditLogFile);
    }
  }

  /**
   * Writes a single admin LOGIN audit event for yesterday into the server's audit log directory
   * ({@code sonatypeWork/logs/audit-{yesterday}.log.gz}), so the User Activity Overview API
   * returns admin as an active user.
   * <p>
   * The {@link com.sonatype.insight.brain.audit.DefaultAuditLogFilesProvider} reads from
   * {@code sonatypeWork/logs/} when no explicit {@code auditLogFilename} is configured. Writing
   * a dated archive for yesterday falls within the component's default 30-day date range and
   * avoids conflicting with the live {@code audit.log} file written by Logback.
   * <p>
   * Note on fork safety: {@code DefaultAuditLogFilesProvider.parseArchiveDate()} requires the
   * portion between {@code audit-} and {@code .log.gz} to be a parseable {@code LocalDate} —
   * any fork-unique suffix causes {@code DateTimeParseException} and the file is silently
   * skipped. Because Failsafe distributes whole test <em>classes</em> across forks, only one
   * fork ever runs this class, so no concurrent write or delete races occur in practice.
   */
  private void writeAdminAuditLog() throws IOException {
    InsightConfig insightConfig = lookup(InsightConfig.class);
    Path auditDir = insightConfig.getSonatypeWork().toPath().resolve("logs");
    Files.createDirectories(auditDir);

    String yesterday = LocalDate.now().minusDays(1).toString();
    // Both usernames are fixed alphanumeric/hyphen literals — safe to interpolate into JSON
    // without escaping (no '"', '\', or control characters).
    // admin gets 2 login entries (higher count); SECOND_ACTIVITY_USER gets 1 (lower count).
    // This makes the descending→ascending sort toggle observable: admin first ↔ second user first.
    String adminEntry1 = "{\"timestamp\":\"" + yesterday + "T10:00:00.000Z\","
        + "\"username\":\"" + TestCredentials.ADMIN_USERNAME + "\","
        + "\"type\":\"login\",\"domain\":\"security.user\","
        + "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\","
        + "\"remoteIpAddress\":\"127.0.0.1\",\"userAgent\":\"Playwright/test\"}";
    String adminEntry2 = "{\"timestamp\":\"" + yesterday + "T11:00:00.000Z\","
        + "\"username\":\"" + TestCredentials.ADMIN_USERNAME + "\","
        + "\"type\":\"login\",\"domain\":\"security.user\","
        + "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\","
        + "\"remoteIpAddress\":\"127.0.0.1\",\"userAgent\":\"Playwright/test\"}";
    String secondUserEntry = "{\"timestamp\":\"" + yesterday + "T09:00:00.000Z\","
        + "\"username\":\"" + SECOND_ACTIVITY_USER + "\","
        + "\"type\":\"login\",\"domain\":\"security.user\","
        + "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\","
        + "\"remoteIpAddress\":\"127.0.0.1\",\"userAgent\":\"Playwright/test\"}";

    auditLogFile = auditDir.resolve("audit-" + yesterday + ".log.gz");
    try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(auditLogFile))) {
      gz.write((adminEntry1 + "\n" + adminEntry2 + "\n" + secondUserEntry).getBytes(StandardCharsets.UTF_8));
    }
  }

  /**
   * The Overview table renders all required column headers plus the Export Activity and Filter
   * action buttons, and the "Showing N of M users" summary is present.
   */
  @Test
  @Tag("regression")
  public void testOverview_tableColumnsAndActionButtons() {
    overviewAssertions.shouldShowTable();
    overviewAssertions.shouldShowUsernameColumnHeader();
    overviewAssertions.shouldShowLoginCountColumnHeader();
    overviewAssertions.shouldShowLastActiveColumnHeader();
    overviewAssertions.shouldShowExportActivityButton();
    overviewAssertions.shouldShowFilterButton();
    overviewAssertions.shouldShowShowingSummary();
    overviewAssertions.shouldShowUser(TestCredentials.ADMIN_USERNAME);
  }

  /**
   * Typing a search term filters the table: a matching term shows the user row;
   * a non-matching term shows the empty-state message.
   */
  @Test
  @Tag("regression")
  public void testOverview_searchFiltersTable() {
    overviewPage.searchInput().fill(TestCredentials.ADMIN_USERNAME);
    overviewAssertions.shouldShowUser(TestCredentials.ADMIN_USERNAME);

    overviewPage.searchInput().fill(NO_MATCH_SEARCH_TERM);
    overviewAssertions.shouldShowEmptyState();
  }

  /**
   * The Login Count column has {@code aria-sort="descending"} by default — the initial sort
   * direction applied by the component on first render.
   */
  @Test
  @Tag("regression")
  public void testOverview_defaultSortIsLoginCountDescending() {
    overviewAssertions.shouldShowTable();
    overviewAssertions.shouldHaveLoginCountSortDirection("descending");
  }

  /**
   * Clicking the Login Count column header toggles the sort direction to ascending
   * and reorders the rows: admin (2 logins) is first when descending; the second
   * seeded user (1 login) is first when ascending.
   */
  @Test
  @Tag("regression")
  public void testOverview_clickLoginCountHeaderTogglesSortToAscending() {
    overviewAssertions.shouldShowTable();
    overviewAssertions.shouldHaveLoginCountSortDirection("descending");
    // admin has 2 seeded logins → sorts first under descending (highest count first)
    assertThat(overviewPage.firstDataRow()).containsText(TestCredentials.ADMIN_USERNAME);

    overviewPage.loginCountColumnHeader().click();

    overviewAssertions.shouldHaveLoginCountSortDirection("ascending");
    // SECOND_ACTIVITY_USER ("playwright-activity-user") has 1 seeded login → sorts first under
    // ascending (lowest count first). The username is synthetic and never generated by other test
    // classes (which only call playwrightLogin() as "admin"), so its count stays at exactly 1
    // across all test runs, guaranteeing it is always the minimum-count user in the dataset.
    assertThat(overviewPage.firstDataRow()).containsText(SECOND_ACTIVITY_USER);
  }

  /**
   * Clicking a user row navigates to that user's Activity Details page and the details
   * table renders. Verifies both URL change and page content load — navigation is wired
   * via the parent UserManagement component's stateGo handler.
   */
  @Test
  @Tag("regression")
  public void testOverview_rowClickNavigatesToUserDetails() {
    overviewPage.userRow(TestCredentials.ADMIN_USERNAME).click();
    assertThat(page).hasURL(
        Pattern.compile(".*#/users/activity/" + BasePage.escapeForJsRegex(TestCredentials.ADMIN_USERNAME)));
    assertThat(new UserActivityDetailsRegressionPage().detailsTable()).isVisible();
  }

  /**
   * Clicking "Filter" opens the drawer with Apply/Reset disabled (no dirty state).
   * Selecting a time-frame radio activates both buttons and surfaces the stale-filter mask
   * behind the open drawer ({@code filtersAreDirty=true}).
   */
  @Test
  @Tag("regression")
  public void testOverview_filterDrawerOpensWithButtonsDisabled() {
    overviewPage.filterButton().click();
    overviewAssertions.shouldShowFilterDrawer();
    overviewAssertions.shouldShowApplyDisabled();
    overviewAssertions.shouldShowResetDisabled();

    overviewPage.timeFrameTreeViewToggle().click();
    overviewPage.timeFrameOption(TIME_FRAME_PAST_7_DAYS).click();
    overviewAssertions.shouldShowApplyEnabled();
    overviewAssertions.shouldShowResetEnabled();
    // Mask appears behind the open drawer because filtersAreDirty is now true.
    overviewAssertions.shouldShowFilterMask();
  }

  /**
   * Clicking "Apply" in the filter drawer closes the drawer and updates the Login Count
   * column header to reflect the selected time period.
   */
  @Test
  @Tag("regression")
  public void testOverview_filterApplyUpdatesLoginCountHeader() {
    overviewPage.filterButton().click();
    overviewPage.timeFrameTreeViewToggle().click();
    overviewPage.timeFrameOption(TIME_FRAME_PAST_7_DAYS).click();
    overviewPage.filterApplyButton().click();

    overviewAssertions.shouldHideFilterDrawer();
    assertThat(overviewPage.loginCountColumnHeader()).containsText(TIME_FRAME_PAST_7_DAYS);
  }

  /**
   * Clicking "Reset" in the filter drawer reverts the selection and closes the drawer
   * without applying the pending change — the Login Count header retains the default period.
   */
  @Test
  @Tag("regression")
  public void testOverview_filterResetRevertsSelection() {
    overviewPage.filterButton().click();
    overviewPage.timeFrameTreeViewToggle().click();
    overviewPage.timeFrameOption(TIME_FRAME_PAST_7_DAYS).click();
    overviewPage.filterResetButton().click();

    overviewAssertions.shouldHideFilterDrawer();
    // Login Count header still shows the default period because Reset discarded the change.
    assertThat(overviewPage.loginCountColumnHeader()).containsText("past 30 days");
  }

  /**
   * When the table has no results (search finds nothing), the "Export Activity" button is
   * disabled because {@code users.length === 0}.
   */
  @Test
  @Tag("regression")
  public void testOverview_exportDisabledWhenTableIsEmpty() {
    overviewPage.searchInput().fill(NO_MATCH_SEARCH_TERM);
    overviewAssertions.shouldShowEmptyState();
    overviewAssertions.shouldShowExportActivityDisabled();
  }
}
