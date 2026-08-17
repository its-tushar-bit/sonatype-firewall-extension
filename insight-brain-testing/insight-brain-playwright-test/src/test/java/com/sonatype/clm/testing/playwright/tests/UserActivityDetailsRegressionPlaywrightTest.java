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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.GZIPOutputStream;

import com.microsoft.playwright.Download;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.LoadState;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.pages.UserActivityDetailsRegressionPage;
import com.sonatype.clm.testing.playwright.pages.UserActivityDetailsRegressionPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.service.InsightConfig;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for the User Activity Details screen
 * ({@code UserActivityDetails.jsx}, URL fragment {@code /users/activity/{username}}).
 * <p>
 * All tests target the built-in admin user, whose login activity is seeded via a
 * gzip-compressed audit log file written to {@code sonatypeWork/logs/} in {@link #setUp()}.
 * This avoids the non-deterministic timing of waiting for the Logback audit appender to
 * flush events that the React app generates during login.
 * <p>
 * The sanity-level page object {@code UserActivityDetailsPage} and its assertions
 * class are NOT modified — this test class creates and uses
 * {@link UserActivityDetailsRegressionPage} exclusively.
 */
public class UserActivityDetailsRegressionPlaywrightTest
    extends AbstractIqUiTest
{
  private UserActivityDetailsRegressionPage detailsPage;

  private UserActivityDetailsRegressionPageAssertions detailsAssertions;

  /** Paths to seeded audit log files; deleted in {@link #tearDown()}. */
  private List<Path> auditLogFiles;

  /** Date format matching the UI's {@code USER_ACTIVITY_DATE_FORMAT} ({@code M/D/YYYY}). */
  private static final DateTimeFormatter UI_DATE_FORMAT = DateTimeFormatter.ofPattern("M/d/yyyy");

  private static final String SORT_DESCENDING = "descending";

  private static final String SORT_ASCENDING = "ascending";

  private static final String ACTIVITY_TYPE_LOGIN = "login";

  /**
   * Captured once per test run to ensure seeding and assertions reference the same calendar day.
   * Using a shared reference prevents divergence when a test executes across a midnight boundary.
   */
  private LocalDate today;

  private boolean originalTracking;

  @BeforeEach
  public void setUp() throws IOException {
    today = LocalDate.now();
    originalTracking = SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.isEnabled();
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(true);
    auditLogFiles = writeAdminAuditLogs();
    playwrightRefreshOrOpen(UserActivityDetailsRegressionPage.url(TestCredentials.ADMIN_USERNAME));
    playwrightLogin();
    // Reload after login so the page fetches fresh data with the seeded audit file.
    playwrightRefreshOrOpen(UserActivityDetailsRegressionPage.url(TestCredentials.ADMIN_USERNAME));
    page.waitForLoadState(LoadState.NETWORKIDLE);
    detailsPage = new UserActivityDetailsRegressionPage();
    detailsAssertions = new UserActivityDetailsRegressionPageAssertions(detailsPage);
  }

  @AfterEach
  public void tearDown() throws IOException {
    SystemConfigurationPropertyFeature.USER_ACTIVITY_TRACKING.setEnabled(originalTracking);
    if (auditLogFiles != null) {
      for (Path f : auditLogFiles) {
        Files.deleteIfExists(f);
      }
    }
  }

  /**
   * Writes three admin LOGIN audit events — one each for 3 days ago, 2 days ago, and yesterday —
   * into separate dated gzip archives under {@code sonatypeWork/logs/}.
   * <p>
   * Three rows with distinct dates make sort-order assertions meaningful: descending puts
   * yesterday first; ascending puts 3-days-ago first. Using separate daily files matches the
   * naming convention expected by {@link com.sonatype.insight.brain.audit.DefaultAuditLogFilesProvider}.
   * <p>
   * Note on fork safety: {@code DefaultAuditLogFilesProvider.parseArchiveDate()} extracts the
   * date portion between {@code audit-} and {@code .log.gz} and calls {@code LocalDate.parse()}
   * on it — any fork-unique suffix (e.g. {@code audit-{date}-fork1.log.gz}) causes
   * {@code DateTimeParseException} and the file is silently skipped. Because Failsafe distributes
   * whole test <em>classes</em> across forks, only one fork ever runs this class, so the
   * same-content write and delete races do not occur in practice.
   *
   * @return the list of written archive paths (deleted in {@link #tearDown()})
   */
  private List<Path> writeAdminAuditLogs() throws IOException {
    InsightConfig insightConfig = lookup(InsightConfig.class);
    Path auditDir = insightConfig.getSonatypeWork().toPath().resolve("logs");
    Files.createDirectories(auditDir);

    List<Path> written = new ArrayList<>();
    for (int daysBack = 3; daysBack >= 1; daysBack--) {
      String date = today.minusDays(daysBack).toString();
      // TestCredentials.ADMIN_USERNAME is the fixed literal "admin" — safe to interpolate
      // directly into JSON without escaping (no '"', '\', or control characters).
      String auditEntry = "{\"timestamp\":\"" + date + "T10:00:00.000Z\","
          + "\"username\":\"" + TestCredentials.ADMIN_USERNAME + "\","
          + "\"type\":\"login\",\"domain\":\"security.user\","
          + "\"requestMethod\":\"POST\",\"requestUri\":\"/api/v2/auth/login\","
          + "\"remoteIpAddress\":\"127.0.0.1\",\"userAgent\":\"Playwright/test\"}";
      Path file = auditDir.resolve("audit-" + date + ".log.gz");
      try (GZIPOutputStream gz = new GZIPOutputStream(Files.newOutputStream(file))) {
        gz.write(auditEntry.getBytes(StandardCharsets.UTF_8));
      }
      written.add(file);
    }
    return written;
  }

  /**
   * All eight column headers required by the data model are visible in the Activity Details table.
   */
  @Test
  @Tag("regression")
  public void testDetails_allEightColumnHeadersVisible() {
    detailsAssertions.shouldShowTimestampColumnHeader();
    detailsAssertions.shouldShowDomainColumnHeader();
    detailsAssertions.shouldShowTypeColumnHeader();
    detailsAssertions.shouldShowErrorColumnHeader();
    detailsAssertions.shouldShowRequestUriColumnHeader();
    detailsAssertions.shouldShowMethodColumnHeader();
    detailsAssertions.shouldShowIpAddressColumnHeader();
    detailsAssertions.shouldShowUserAgentColumnHeader();
  }

  /**
   * The "Showing N activities" summary paragraph is visible after the page loads.
   */
  @Test
  @Tag("regression")
  public void testDetails_showingActivitiesSummaryVisible() {
    detailsAssertions.shouldShowActivitiesSummary();
  }

  /**
   * The Timestamp column has {@code aria-sort="descending"} on first render and the most-recent
   * entry (yesterday) appears in the first data row.
   * <p>
   * Assumption: the embedded test server does not flush today's login audit event to
   * {@code audit.log} during the test, so the seeded yesterday-dated entry sorts first and
   * no {@code today}-dated row appears above it. This holds because the Logback audit appender
   * buffers events and the test completes before any flush to disk.
   */
  @Test
  @Tag("regression")
  public void testDetails_defaultSortIsTimestampDescending() {
    detailsAssertions.shouldHaveTimestampSortDirection(SORT_DESCENDING);
    detailsAssertions.shouldHaveFirstTimestampContaining(
        today.minusDays(1).format(UI_DATE_FORMAT));
  }

  /**
   * Clicking the Timestamp column header toggles the sort direction to ascending and moves
   * the oldest entry (3 days ago) to the first data row.
   */
  @Test
  @Tag("regression")
  public void testDetails_clickTimestampHeaderTogglesSortToAscending() {
    detailsAssertions.shouldHaveTimestampSortDirection(SORT_DESCENDING);

    detailsPage.timestampColumnHeader().click();

    detailsAssertions.shouldHaveTimestampSortDirection(SORT_ASCENDING);
    detailsAssertions.shouldHaveFirstTimestampContaining(
        today.minusDays(3).format(UI_DATE_FORMAT));
  }

  /**
   * Clicking the "Back" button navigates from the Details screen to the User Activity Overview.
   */
  @Test
  @Tag("regression")
  public void testDetails_backButtonNavigatesToOverview() {
    detailsAssertions.shouldShowBackButton();

    detailsPage.backButton().click();

    assertThat(page).hasURL(Pattern.compile(".*#/users/activity$"));
  }

  /**
   * The "Filter" button opens the details filter drawer, which contains three multi-select
   * sections (Activity Type, Domain, Error Type); Apply and Reset are initially disabled
   * because no filter selection has been changed ({@code filtersAreDirty} is false).
   * Expanding the Activity Type section and selecting "login" makes both buttons active
   * and shows the stale-filter mask behind the open drawer ({@code filtersAreDirty=true}).
   */
  @Test
  @Tag("regression")
  public void testDetails_filterDrawerOpensWithThreeSectionsAndButtonsDisabled() {
    detailsPage.filterButton().click();

    detailsAssertions.shouldShowFilterDrawer();
    detailsAssertions.shouldShowActivityTypeSectionToggle();
    detailsAssertions.shouldShowDomainSectionToggle();
    detailsAssertions.shouldShowErrorTypeSectionToggle();
    detailsAssertions.shouldShowFilterApplyDisabled();
    detailsAssertions.shouldShowFilterResetDisabled();

    detailsPage.activityTypeSectionToggle().click();
    detailsPage.activityTypeOption(ACTIVITY_TYPE_LOGIN).click();
    detailsAssertions.shouldShowFilterApplyEnabled();
    detailsAssertions.shouldShowFilterResetEnabled();
    detailsAssertions.shouldShowFilterMask();
  }

  /**
   * Clicking "Apply" in the filter drawer closes the drawer — the component calls
   * {@code onApply()} then {@code onClose()} when Apply is clicked.
   */
  @Test
  @Tag("regression")
  public void testDetails_filterApplyClosesDrawer() {
    detailsPage.filterButton().click();
    detailsPage.activityTypeSectionToggle().click();
    detailsPage.activityTypeOption(ACTIVITY_TYPE_LOGIN).click();
    detailsPage.filterApplyButton().click();

    detailsAssertions.shouldHideFilterDrawer();
  }

  /**
   * Clicking "Reset" in the filter drawer closes the drawer — the component calls
   * {@code onReset()} then {@code onClose()} when Reset is clicked.
   */
  @Test
  @Tag("regression")
  public void testDetails_filterResetClosesDrawer() {
    detailsPage.filterButton().click();
    detailsPage.activityTypeSectionToggle().click();
    detailsPage.activityTypeOption(ACTIVITY_TYPE_LOGIN).click();
    detailsPage.filterResetButton().click();

    detailsAssertions.shouldHideFilterDrawer();
  }

  /**
   * The "Export Activity" button is enabled when the admin user has tracked activity records.
   * Clicking it triggers a CSV file download; after the download the button returns to its
   * normal "Export Activity" state and no error alert is shown — confirming the export succeeded.
   * Filename pattern: {@code user_activity_detail_{username}_{timestamp}.csv}.
   */
  @Test
  @Tag("regression")
  public void testDetails_exportActivityEnabledForAdminWithActivity() {
    detailsAssertions.shouldShowActivitiesSummary();
    detailsAssertions.shouldShowExportActivityEnabled();

    Download download = page.waitForDownload(
        new Page.WaitForDownloadOptions().setTimeout(PlaywrightTiming.LONG_OPERATION_TIMEOUT_MS),
        () -> detailsPage.exportActivityButton().click());

    assertTrue(
        download.suggestedFilename().startsWith("user_activity_detail_" + TestCredentials.ADMIN_USERNAME + "_"),
        "export filename should identify the admin user");
    assertTrue(download.suggestedFilename().endsWith(".csv"), "export file should be CSV");

    detailsAssertions.shouldShowExportActivityReady();
    detailsAssertions.shouldNotShowExportError();
  }

  /**
   * The "Export Activity" button is disabled when the details table has no activity records
   * ({@code activities.length === 0}).
   * <p>
   * Navigates to a username that has no seeded audit log entries so the table loads empty.
   * Only admin's entries are seeded in {@link #setUp()}, so any other username yields zero rows.
   */
  @Test
  @Tag("regression")
  public void testDetails_exportDisabledWhenNoActivities() {
    playwrightRefreshOrOpen(UserActivityDetailsRegressionPage.url("no-activity-user"));
    detailsPage = new UserActivityDetailsRegressionPage();
    detailsAssertions = new UserActivityDetailsRegressionPageAssertions(detailsPage);
    assertThat(detailsPage.exportActivityButton()).isVisible();
    detailsAssertions.shouldShowExportActivityDisabled();
  }
}
