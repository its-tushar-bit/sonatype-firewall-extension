/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.tests.mtiq;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.dto.model.policy.Action;
import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.testing.playwright.categories.MtiqTest;
import com.sonatype.clm.testing.playwright.mtiq.AbstractMtiqUiTest;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPage;
import com.sonatype.clm.testing.playwright.pages.PrioritiesPageAssertions;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.TestReportEvaluator;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.policy.PolicyImportExport;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.Zipper;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

import static org.junit.Assert.assertFalse;

import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * MTIQ — Developer Priorities page regression tests.
 *
 * <p>
 * Seeding mirrors the on-prem {@code PrioritiesPagePlaywrightTest}: a canned report is zipped
 * from the classpath and submitted for policy evaluation via HTTP. Each test calls
 * {@code evaluator.evaluatePolicy()} itself — lifting this to {@code @Before} triggers an
 * AutoPolicyWaiver-induced 404 race (documented in on-prem Javadoc).
 */
@Category(MtiqTest.class)
public class MtiqPrioritiesPagePlaywrightTest
    extends AbstractMtiqUiTest
{
  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  private static final String SCAN_ID = "e16caf35769f4b3186a7e416d34c2797";

  private static final String REPORT_DIR = "/canned-reports/large-report";

  private static final String REFERENCE_POLICIES_RESOURCE = "/reference-policies-v3.json";

  private static final String ORG_NAME_PREFIX = "MtiqPrioritiesTestOrg";

  private static final String APP_NAME_PREFIX = "MtiqPrioritiesTestApp";

  private static final int FAR_FUTURE_EXPIRY_DAYS = 365;

  private static final int SOON_TO_EXPIRE_DAYS = 7;

  private static final int EXPIRED_DAYS_AGO = 1;

  private Application app;

  private String appName;

  private TestReportEvaluator evaluator;

  private PrioritiesPage prioritiesPage;

  private PrioritiesPageAssertions assertions;

  @Before
  public void setUp() throws IOException {
    setLicensedProducts(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS);
    seedDb();
    prioritiesPage = new PrioritiesPage();
    assertions = new PrioritiesPageAssertions(prioritiesPage);
  }

  @After
  public void resetTestState() {
    // Clear clipboard grants so they don't leak into sibling tests in the same BrowserContext fork.
    context.clearPermissions();
    // Reset the developer product mock to prevent state bleed into subsequent test classes.
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
  }

  @Test
  public void testPrioritiesPage_rendersWithHeaderAndTitleForNoBranch() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    assertions.shouldShowPageHeader();
    assertions.shouldShowBreadcrumbLink("Priorities");
    assertions.shouldHaveHeaderTitleText(appName + " - Priorities");
    assertions.shouldShowTableColumnHeaders();
  }

  @Test
  public void testPrioritiesPage_headerShowsMetadataAndViewDropdown() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    assertions.shouldShowPageHeader();
    prioritiesPage.openViewDropdown();
    assertions.shouldShowViewDropdownLinks();
  }

  @Test
  public void testPrioritiesPage_commitCopyButtonShowsCheckIconAfterClick() throws IOException {
    // Scope clipboard permissions to this test; @After.clearPermissions() is a backstop.
    context.grantPermissions(List.of("clipboard-read", "clipboard-write"));
    try {
      evaluator.evaluatePolicy();
      openPrioritiesPage();

      assertThat(prioritiesPage.commitCopyButton()).isVisible();
      // Hover first to mount the NxTooltip portal; hover-after-click races COPY_STATUS_TOOLTIP_TIMEOUT (1500 ms).
      prioritiesPage.commitCopyButton().hover();
      prioritiesPage.commitCopyButton().click();
      // Clipboard content is the durable signal — independent of the 1500 ms tooltip window.
      assertFalse("clipboard should contain a non-empty commit hash after copy",
          readClipboardOnceSettled().isEmpty());
      // After 1500 ms COPY_STATUS_TOOLTIP_TIMEOUT the "Copied" text reverts — assert it disappears.
      assertThat(prioritiesPage.commitCopyTooltipCopiedText()).isHidden(
          new LocatorAssertions.IsHiddenOptions().setTimeout(PlaywrightTiming.BRIEF_UI_TRANSITION_MS));
    }
    finally {
      context.clearPermissions();
    }
  }

  @Test
  public void testPrioritiesPage_filterInputAndFailWarnToggleUpdateUrlParams() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();
    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");

    prioritiesPage.componentFilterInput().fill(artifactId);
    assertions.shouldHaveComponentNameFilterUrlParam(artifactId);
    assertThat(prioritiesPage.rows().first()).isVisible();

    prioritiesPage.failWarnToggleLabel().click();
    assertions.shouldHaveFilterOnPolicyActionsUrlParamOn();
    assertThat(prioritiesPage.failWarnToggleInput()).isChecked();

    // Non-matching filter triggers "No Results" empty state via NxTable.Body emptyMessage.
    String noMatchFilter = "no-match-" + UUID.randomUUID();
    prioritiesPage.componentFilterInput().fill(noMatchFilter);
    assertions.shouldHaveComponentNameFilterUrlParam(noMatchFilter);
    assertThat(prioritiesPage.emptyStateMessage()).containsText("No Results");
  }

  @Test
  public void testPrioritiesPage_fullyWaivedRowShowsWaivedLabel() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation representative = firstSeedableViolation();
    Date farFutureExpiry = Date.from(Instant.now().plus(FAR_FUTURE_EXPIRY_DAYS, ChronoUnit.DAYS));
    for (PolicyViolation v : uniqueViolationsForHash(representative.getHash())) {
      tempEntity.newWaiver(v.getHash(), v.getPolicyId(), app.getId(),
          "Fully-waived row label regression check", farFutureExpiry);
    }
    evaluator.reevaluatePolicy();

    String artifactId = representative.getComponentIdentifier().getCoordinates().get("artifactId");
    reopenPrioritiesPageFilteredBy(artifactId);
    // rowByArtifactId may match multiple rows; "Waived" only appears on waived rows so .first() is unneeded.
    assertions.shouldShowFullyWaivedLabelOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  public void testPrioritiesPage_expiredWaiverShowsQuestionCircleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();
    tempEntity.newWaiver(violation.getHash(), violation.getPolicyId(), app.getId(),
        "Expired waiver regression check",
        Date.from(Instant.now().minus(EXPIRED_DAYS_AGO, ChronoUnit.DAYS)));
    evaluator.reevaluatePolicy();

    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");
    reopenPrioritiesPageFilteredBy(artifactId);
    // rowByArtifactId may match multiple rows; expired icon only appears on the waived row so .first() is unneeded.
    assertions.shouldShowExpiredWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  public void testPrioritiesPage_soonToExpireWaiverShowsWarningTriangleIcon() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation representative = firstSeedableViolation();
    Date nearFutureExpiry = Date.from(Instant.now().plus(SOON_TO_EXPIRE_DAYS, ChronoUnit.DAYS));
    for (PolicyViolation v : uniqueViolationsForHash(representative.getHash())) {
      tempEntity.newWaiver(v.getHash(), v.getPolicyId(), app.getId(),
          "Soon-to-expire waiver regression check", nearFutureExpiry);
    }
    evaluator.reevaluatePolicy();

    String artifactId = representative.getComponentIdentifier().getCoordinates().get("artifactId");
    reopenPrioritiesPageFilteredBy(artifactId);
    // rowByArtifactId may match multiple rows; soon-to-expire icon only appears on waived rows so .first() is unneeded.
    assertions.shouldShowSoonToExpireWaiverIconOnRow(prioritiesPage.rowByArtifactId(artifactId));
  }

  @Test
  public void testPrioritiesPage_recommendationCellShowsWaiveViolationsForUnknownReachability() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    PolicyViolation violation = firstSeedableViolation();
    String artifactId = violation.getComponentIdentifier().getCoordinates().get("artifactId");
    reopenPrioritiesPageFilteredBy(artifactId);
    // .first() avoids strict-mode violation — multiple rows may share the same artifactId.
    assertions.shouldShowWaiveViolationsRecommendationOnRow(prioritiesPage.rowByArtifactId(artifactId).first());
  }

  @Test
  public void testPrioritiesPage_dependencyIndicatorsRenderInComponentCell() throws IOException {
    evaluator.evaluatePolicy();
    openPrioritiesPage();

    assertions.shouldShowAtLeastOneDependencyIndicatorInTable();
  }

  @Test
  public void testPrioritiesPage_licenseLockScreenShownWhenDeveloperDashboardDisabled() throws IOException {
    evaluator.evaluatePolicy();
    // Disable the developer product via the in-JVM mock — MTIQ-native alternative to the
    // page.route() intercept used on-prem; no IQ Server REST endpoint is intercepted.
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(false);
    playwrightLoginAdminAt(PrioritiesPage.url(app.getPublicId(), SCAN_ID));
    // SPA caches productFeatures on first load; reload forces re-fetch through the disabled mock.
    page.reload();

    assertions.shouldShowLicenseLockScreen();
  }

  /**
   * Resolves the canned report directory from the classpath and returns it as a zipped URL.
   * When running from {@code target/test-classes} (file: URI) the standard {@code ReportHelper}
   * path works; when the resource lives inside a test-jar (jar: URI) it extracts the directory
   * into {@code tempDir} first, then zips it with {@link Zipper}.
   */
  private URL zipCannedReport() throws IOException {
    URL resourceUrl = getClass().getResource(REPORT_DIR);
    if (resourceUrl == null) {
      throw new IllegalStateException("Canned report not found on classpath: " + REPORT_DIR);
    }
    try {
      URI uri = resourceUrl.toURI();
      if ("file".equals(uri.getScheme())) {
        return ReportHelper.zipReport(REPORT_DIR, tempDir);
      }
      Path extractedDir = tempDir.newFolder(Path.of(REPORT_DIR).getFileName().toString()).toPath();
      String uriStr = uri.toString();
      URI jarFileUri = URI.create(uriStr.substring(0, uriStr.indexOf("!/")));
      String internalPath = uriStr.substring(uriStr.indexOf("!/") + 1);
      try (FileSystem jarFs = FileSystems.newFileSystem(jarFileUri, Map.of())) {
        Path source = jarFs.getPath(internalPath);
        try (Stream<Path> walk = Files.walk(source)) {
          walk.forEach(p -> {
            try {
              Path relative = source.relativize(p);
              Path target = extractedDir.resolve(relative.toString());
              if (Files.isDirectory(p)) {
                Files.createDirectories(target);
              }
              else {
                Files.createDirectories(target.getParent());
                Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING);
              }
            }
            catch (IOException e) {
              throw new UncheckedIOException(e);
            }
          });
        }
      }
      // Do NOT use tempDir.newFile() — it creates an empty file and Zipper.zip (JAR file system
      // with "create:true") throws "zip END header not found" on a pre-existing empty file.
      File reportZipFile = new File(tempDir.getRoot(), "MockReport-" + UUID.randomUUID() + ".zip");
      Zipper.zip(extractedDir.toFile(), reportZipFile);
      return reportZipFile.toURI().toURL();
    }
    catch (URISyntaxException e) {
      throw new IOException(e);
    }
  }

  /**
   * Seeds org + policies + app + configures evaluator; does NOT call {@code evaluatePolicy()} —
   * each test runs the initial evaluation itself to avoid an AutoPolicyWaiver-induced 404 race
   * that occurs when evaluation is called from {@code @Before}.
   */
  private void seedDb() throws IOException {
    URL referencePolicyUrl = getClass().getResource(REFERENCE_POLICIES_RESOURCE);
    if (referencePolicyUrl == null) {
      throw new IllegalStateException(
          "Reference policies not found on classpath: " + REFERENCE_POLICIES_RESOURCE);
    }
    PolicyExportResult referencePolicies =
        JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);

    String suffix = TemporaryEntity.uuid();
    String orgName = ORG_NAME_PREFIX + "-" + suffix;
    appName = APP_NAME_PREFIX + "-" + suffix;

    Organization org = tempEntity.newOrganization(orgName);
    lookup(PolicyImportExport.class).importOrganization(org, referencePolicies);
    // Reference policies have no actions — add FAIL so waiver icons render.
    PolicyDAO policyDAO = lookup(PolicyDAO.class);
    for (Policy policy : policyDAO.getByOwnerId(org.getId())) {
      policy.setAction(Stage.ID_BUILD, Action.ID_FAIL);
      policyDAO.update(policy);
    }
    app = tempEntity.newApplication(appName, appName, org.getId());

    URL zippedReport = zipCannedReport();
    InsightWork work = new InsightWork(testCLMServer.getCLMServer().getConfiguration());
    evaluator = new TestReportEvaluator(app, SCAN_ID, zippedReport, baseUrlFromTest, work,
        Stage.ID_BUILD);
  }

  private String readClipboardOnceSettled() {
    page.waitForFunction("() => navigator.clipboard.readText().then(t => t.length > 0)");
    return (String) page.evaluate("navigator.clipboard.readText()");
  }

  private void openPrioritiesPage() {
    playwrightLoginAdminAt(PrioritiesPage.url(app.getPublicId(), SCAN_ID));
    assertions.shouldBeVisible();
  }

  private void reopenPrioritiesPageFilteredBy(String componentNameFilter) {
    playwrightRefreshOrOpen(PrioritiesPage.url(app.getPublicId(), SCAN_ID, componentNameFilter));
    assertions = new PrioritiesPageAssertions(prioritiesPage);
    assertions.shouldBeVisible();
  }

  private PolicyViolation firstSeedableViolation() {
    PolicyViolationDAO dao = lookup(PolicyViolationDAO.class);
    return dao.getByApplicationId(app.getId())
        .stream()
        .filter(v -> v.getHash() != null && v.getPolicyId() != null
            && v.getComponentIdentifier() != null
            && v.getComponentIdentifier().getCoordinates() != null
            && v.getComponentIdentifier().getCoordinates().get("artifactId") != null)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException(
            "No suitable PolicyViolation found after canned-report evaluation for app=" + app.getId()));
  }

  /**
   * All violations on {@code hash} deduped by {@code policyId} (waiver's unique key is
   * {@code (hash, policyId, ownerId)}). Fails closed if the canned report drifts.
   */
  private List<PolicyViolation> uniqueViolationsForHash(String hash) {
    Map<String, PolicyViolation> uniqueByPolicyId = lookup(PolicyViolationDAO.class)
        .getByApplicationId(app.getId())
        .stream()
        .filter(v -> hash.equals(v.getHash()) && v.getPolicyId() != null)
        .collect(Collectors.toMap(PolicyViolation::getPolicyId, v -> v, (a, b) -> a));
    List<PolicyViolation> result = List.copyOf(uniqueByPolicyId.values());
    if (result.isEmpty()) {
      throw new IllegalStateException("violations on the seeded component (precondition) must not be empty");
    }
    return result;
  }
}
