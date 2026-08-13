/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.options.BoundingBox;
import com.sonatype.clm.testing.playwright.AbstractIqUiTest;
import com.sonatype.clm.testing.playwright.categories.RegressionTest;
import com.sonatype.clm.testing.playwright.pages.SystemNoticePage;
import com.sonatype.insight.brain.configuration.SystemNoticeService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.configuration.SystemNotice;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Playwright tests for the Nexus One shell's notice strip layout.
 *
 * <p>
 * Verifies real browser layout behavior that Jest/jsdom cannot test:
 * <ul>
 * <li>Notice strip renders above TopNav with no visual overlap</li>
 * <li>TopNav's top position adjusts dynamically based on measured notice height</li>
 * <li>Layout flash is minimized (ResizeObserver-based offset updates)</li>
 * </ul>
 *
 * <p>
 * Uses the System Notice as the most straightforward notice to trigger in a test
 * environment (same backend configuration endpoint as Classic UI).
 */
public class NexusOneNoticeStripPlaywrightTest
    extends AbstractIqUiTest
{
  private static final String NOTICE_TEXT = "Nexus One test notice.";

  private static final String NEXUS_ONE_URL = "/assets/nexus-one/index.html";

  @Before
  public void setUp() {
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(true);
    playwrightRefreshOrOpen(SystemNoticePage.url());
    playwrightLogin();
  }

  @After
  public void cleanup() {
    // playwrightLogout() drives HeaderComponent, which targets Classic UI header markup
    // (#user-menu, #header-login-button) that the Nexus One shell's TopNav does not have —
    // navigate back to a Classic page first or the user-menu button can never be found.
    playwrightRefreshOrOpen(SystemNoticePage.url());
    playwrightLogout();
    SystemNotice empty = new SystemNotice();
    empty.setMessage("");
    empty.setEnabled(false);
    lookup(SystemNoticeService.class).updateSystemNotice(empty);
    setEnableDefaultPasswordWarning(false);
    SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.setEnabled(false);
  }

  @Test
  @Category(RegressionTest.class)
  public void testNoticeStripRendersAboveTopNavWithNoOverlap() {
    // Enable a system notice to trigger a visible notice strip
    SystemNotice notice = new SystemNotice();
    notice.setMessage(NOTICE_TEXT);
    notice.setEnabled(true);
    lookup(SystemNoticeService.class).updateSystemNotice(notice);

    // Navigate to Nexus One shell
    playwrightRefreshOrOpen(NEXUS_ONE_URL);

    // Wait for the notice strip and TopNav to render
    Locator noticeStrip = page.locator("[data-testid='nosc-notice-strip']");
    Locator topNav = page.locator("[data-testid='nexus-one-top-nav']");

    assertThat(noticeStrip).isVisible();
    assertThat(topNav).isVisible();

    // Get bounding boxes for real pixel measurements (jsdom cannot do this)
    BoundingBox stripBox = noticeStrip.boundingBox();
    BoundingBox topNavBox = topNav.boundingBox();

    // Verify notice strip is at the top of the viewport
    org.junit.Assert.assertNotNull("Notice strip bounding box should not be null", stripBox);
    org.junit.Assert.assertTrue("Notice strip y position should be >= 0", stripBox.y >= 0);

    // Verify TopNav is positioned below the notice strip with no overlap
    org.junit.Assert.assertNotNull("TopNav bounding box should not be null", topNavBox);
    org.junit.Assert.assertTrue(
        "TopNav should be at or below notice strip bottom edge",
        topNavBox.y >= stripBox.y + stripBox.height);

    // Verify the notice content is visible
    assertThat(noticeStrip).containsText(NOTICE_TEXT);
  }

  @Test
  @Category(RegressionTest.class)
  public void testNoticeStripCollapsesWhenNoNoticesVisible() {
    // Ensure no system notice is configured
    SystemNotice empty = new SystemNotice();
    empty.setMessage("");
    empty.setEnabled(false);
    lookup(SystemNoticeService.class).updateSystemNotice(empty);

    // ENABLE_DEFAULT_PASSWORD_WARNING defaults to true and the test-env admin keeps the
    // default password, so DefaultAdminPasswordNotice would render unless explicitly disabled
    // here — this must not depend on a sibling test's @After cleanup having already run.
    setEnableDefaultPasswordWarning(false);

    // Navigate to Nexus One shell
    playwrightRefreshOrOpen(NEXUS_ONE_URL);

    // Wait for TopNav to render
    Locator topNav = page.locator("[data-testid='nexus-one-top-nav']");
    assertThat(topNav).isVisible();

    // Notice strip should still render (it's always mounted), but with 0 height — isVisible()
    // requires a non-empty bounding box, which a 0-height element never satisfies, so this
    // checks DOM presence only; the real behavioral check is the height assertion below.
    Locator noticeStrip = page.locator("[data-testid='nosc-notice-strip']");
    assertThat(noticeStrip).isAttached();

    // boundingBox() returns null for elements Playwright considers "not visible" — and per its
    // own docs, a zero-height element qualifies as not visible, so it would return null here
    // rather than a box with height 0. getBoundingClientRect() via JS evaluation always returns
    // a real DOMRect regardless of visibility, so it's used here instead of noticeStrip.boundingBox().
    double stripHeight =
        ((Number) page.evaluate(
            "document.querySelector('[data-testid=\"nosc-notice-strip\"]').getBoundingClientRect().height"))
                .doubleValue();
    BoundingBox topNavBox = topNav.boundingBox();

    // When no notice is visible, the strip should have 0 height
    org.junit.Assert.assertEquals("Notice strip height should be 0", 0.0, stripHeight, 0.01);

    // TopNav should be at the top of the viewport (no offset)
    org.junit.Assert.assertNotNull("TopNav bounding box should not be null", topNavBox);
    org.junit.Assert.assertEquals("TopNav y position should be 0", 0, topNavBox.y, 0.01);
  }

  /**
   * Verifies that stacked notices render in the correct order and the notice strip
   * height accumulates correctly. This is the core reason NoticeStrip uses ResizeObserver
   * instead of a hardcoded constant — jsdom/Jest cannot test real layout math.
   *
   * <p>
   * The test uses two notices that are independently controllable from the backend:
   * <ul>
   * <li>System Notice - via {@link SystemNoticeService}</li>
   * <li>Default Admin Password Notice - via the feature flag (admin password remains
   * "admin123" in the test environment, so enabling the flag triggers the notice)</li>
   * </ul>
   */
  @Test
  @Category(RegressionTest.class)
  public void testStackedNoticesRenderInCorrectOrderWithAccumulatedHeight() {
    // Enable system notice (first notice)
    SystemNotice notice = new SystemNotice();
    notice.setMessage(NOTICE_TEXT);
    notice.setEnabled(true);
    lookup(SystemNoticeService.class).updateSystemNotice(notice);

    // Enable default password warning notice (second notice)
    // The admin password is "admin123" by default in the test environment, so this flag alone
    // guarantees the notice appears (see UserService.shouldDisplayDefaultPasswordWarning)
    setEnableDefaultPasswordWarning(true);

    // Navigate to Nexus One shell
    playwrightRefreshOrOpen(NEXUS_ONE_URL);

    // Wait for both notices to render
    Locator systemNotice = page.locator("[data-testid='nosc-system-notice']");
    Locator passwordNotice = page.locator("[data-testid='nosc-default-admin-password-notice']");
    Locator noticeStrip = page.locator("[data-testid='nosc-notice-strip']");
    Locator topNav = page.locator("[data-testid='nexus-one-top-nav']");

    assertThat(systemNotice).isVisible();
    assertThat(passwordNotice).isVisible();
    assertThat(noticeStrip).isVisible();
    assertThat(topNav).isVisible();

    // Get bounding boxes for real pixel measurements
    BoundingBox systemBox = systemNotice.boundingBox();
    BoundingBox passwordBox = passwordNotice.boundingBox();
    BoundingBox stripBox = noticeStrip.boundingBox();
    BoundingBox topNavBox = topNav.boundingBox();

    // Verify both notices have non-zero height
    org.junit.Assert.assertNotNull("System notice bounding box should not be null", systemBox);
    org.junit.Assert.assertTrue("System notice height should be > 0", systemBox.height > 0);
    org.junit.Assert.assertNotNull("Password notice bounding box should not be null", passwordBox);
    org.junit.Assert.assertTrue("Password notice height should be > 0", passwordBox.height > 0);

    // Verify notices appear in correct visual order (System Notice on top, Password Notice below)
    org.junit.Assert.assertTrue(
        "System notice should be above password notice in the viewport",
        systemBox.y < passwordBox.y);

    // Verify the combined strip height is greater than either single notice alone
    // This is the key regression assertion: it would fail if height calculation
    // only accounted for the last notice or overwrote instead of accumulating
    org.junit.Assert.assertNotNull("Notice strip bounding box should not be null", stripBox);
    org.junit.Assert.assertTrue(
        "Combined strip height should be greater than system notice height",
        stripBox.height > systemBox.height);
    org.junit.Assert.assertTrue(
        "Combined strip height should be greater than password notice height",
        stripBox.height > passwordBox.height);

    // Verify TopNav sits at or below the combined strip's bottom edge (no overlap)
    org.junit.Assert.assertNotNull("TopNav bounding box should not be null", topNavBox);
    org.junit.Assert.assertTrue(
        "TopNav should be at or below notice strip bottom edge",
        topNavBox.y >= stripBox.y + stripBox.height);

    // Verify notice content
    assertThat(systemNotice).containsText(NOTICE_TEXT);
    assertThat(passwordNotice).containsText("Change Administrator Password");
  }
}
