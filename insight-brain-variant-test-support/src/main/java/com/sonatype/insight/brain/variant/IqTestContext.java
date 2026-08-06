/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.io.File;
import java.net.URL;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.PublicApiPaths;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainServiceRule;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockResponse;

import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.product.ProductLicenseManager;

/**
 * The single injected object that gives a {@code @IqH2Test}/{@code @IqPostgresTest} class the same
 * capabilities the legacy {@code AbstractBaseIntegrationTest}/{@code AbstractBrainServiceIntegrationTest}
 * base classes provided — but by <b>composition</b>, not inheritance. It wraps the shared, reused
 * {@link TestCLMServer} (booted once per variant by {@link AbstractSpikeServerExtension}) plus the
 * variant's {@link DatabaseContainerRule}, and exposes:
 *
 * <ul>
 * <li>{@link #restRequest()} — authenticated (admin/admin123) + JSON REST access, reusing the proven
 * {@link HttpRequest}/{@link HttpResponse} helpers.</li>
 * <li>{@link #lookup(Class)} — a bean from the running Spring context (mirrors
 * {@code AbstractBaseIntegrationTest.lookup}).</li>
 * <li>{@link #tempEntity()} / {@link #daoFactory()} — data setup against the same database fixture.</li>
 * <li>HDS mock helpers ({@link #hdsRespondWith}, {@link #hdsRespondWithResource}, {@link #getHdsServer}).</li>
 * <li>license helpers ({@link #installLicense}, {@link #uninstallLicense}, {@link #setFeatures},
 * {@link #setMissingFeature}).</li>
 * </ul>
 *
 * <p>
 * Because the server is reused across every test method, {@link #beforeTest()} and {@link #afterTest()}
 * do the per-test setup/reset that the legacy JUnit 4 rules/{@code @Before}/{@code @After} used to do,
 * WITHOUT restarting the server — the #1 conversion risk being state leaking between methods.
 * {@link AbstractIqServerExtension} drives those two hooks.
 */
public final class IqTestContext
{
  private static final Logger log = LoggerFactory.getLogger(IqTestContext.class);

  private final TestCLMServer server;

  private final DatabaseContainerRule databaseContainerRule;

  private final TemporaryEntity tempEntity;

  private final TemporaryFolder tempFolder = new TemporaryFolder();

  private DAOFactory daoFactory;

  // Tracks a REST-level license uninstall (which does NOT flow through the mock license manager, so
  // licenseManager.wasChanged()/isValid() cannot detect it). Ensures afterTest reinstalls the license
  // for the next test on the reused server; otherwise the next test sees HTTP 402 (no valid license).
  private boolean licenseUninstalled;

  IqTestContext(final TestCLMServer server, final DatabaseContainerRule databaseContainerRule) {
    this.server = server;
    this.databaseContainerRule = databaseContainerRule;
    this.tempEntity = new TemporaryEntity(databaseContainerRule);
  }

  // --- per-test lifecycle (no server restart) --------------------------------------------------

  void beforeTest() throws Exception {
    // A fresh temp folder for report/zip fixtures (ReportHelper takes a JUnit TemporaryFolder).
    tempFolder.create();

    // Reset the mocked HDS server so a prior test's stubbed responses/processors do not leak.
    getHdsServer().reset();

    // Re-establish the DAO-backed static collaborators (condition types, etc.) needed by policy
    // evaluation — statics are process-wide and get clobbered between tests (mirrors
    // AbstractBaseIntegrationTest.initTest -> StaticInjectionTestHelper.inject).
    daoFactory = new TestDAOFactory(databaseContainerRule);
    com.sonatype.insight.brain.StaticInjectionTestHelper.inject(daoFactory);

    // Save/restore initial DB state and initialize the entity builders.
    tempEntity.before();

    // The default license threat groups every single-tenant integration test relies on.
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);

    // Reset any proxy-server configuration a prior test applied to the reused server's outbound HTTP
    // clients. IqH2ApiProxyServerConfigurationResourceTest points them at a dead "resttest:58285"
    // proxy; tempEntity.before() above restores the DB to the default (no proxy), but the live clients
    // keep the leaked proxy in memory. Re-apply the DB configuration so the installLicense() call below
    // (and any HDS call) is not routed through an unresolvable proxy, which returns HTTP 502.
    lookup(com.sonatype.insight.brain.api.v2.service.ApiProxyServerConfigurationService.class)
        .applyProxyServerConfigurationToClients();

    // The server's DeveloperEnablementService bean is a shared static Mockito mock
    // (AbstractBaseIntegrationTest.mockDeveloperEnablementService). A prior test class can leave
    // shouldEnableDeveloperProduct() stubbed to true, which makes FeaturesService re-add
    // DEVELOPER_DASHBOARD no matter what license is installed. Reset it like the JUnit 4
    // @Before did (AbstractBaseIntegrationTest.initTest).
    DeveloperEnablementService developerEnablementService = lookup(DeveloperEnablementService.class);
    if (Mockito.mockingDetails(developerEnablementService).isMock()) {
      Mockito.reset(developerEnablementService);
    }

    // Re-establish a deterministic license baseline BEFORE each test. The reused server is shared
    // across every test in the fork, and the afterTest() restore is best-effort (it only runs when a
    // change is detected and it swallows install failures), so a prior test can leave the server
    // unlicensed or with a leaked entitlement (e.g. Developer). Reset the mock manager + cached
    // ProductLicense and reinstall the default license — after the HDS mock reset above — so the
    // server's entitlements are recomputed from a clean state regardless of fork ordering.
    licenseManager().reset();
    productLicense().reset();
    installLicense();
  }

  void afterTest() {
    // Reset mutable state so the reused server looks pristine to the next test.
    try {
      tempEntity.after();
    }
    finally {
      restoreLicenseIfChanged();
      resetSharedServerState();
      databaseContainerRule.resetMocks();
      getHdsServer().reset();
      tempFolder.delete();
    }
  }

  private void restoreLicenseIfChanged() {
    TestProductLicenseManager licenseManager = licenseManager();
    if (licenseUninstalled || licenseManager.wasChanged() || !licenseManager.isValid()) {
      // Push a valid, full license back through the REST endpoint so the server's cached license
      // state is refreshed for the next test (mirrors AbstractBaseIntegrationTest cleanup).
      // Reset the mock license MANAGER (products, application limit, features, stage types) back to its
      // defaults. Without this, a test that calls setProducts/setApplicationLimit/setFeatures leaks that
      // state into later tests under the reused server (productLicense().reset() alone does NOT clear it).
      licenseManager.reset();
      productLicense().reset();
      try {
        installLicense();
      }
      catch (Exception e) {
        log.warn("Failed to restore license after test; the next test on this reused server may fail "
            + "with HTTP 402 until a valid license is reinstalled", e);
      }
    }
  }

  private void resetSharedServerState() {
    TestInsightBrainServiceRule clmServer = server.getCLMServer();
    clmServer.resetDisableForTesting();
    InsightConfig insightConfig = clmServer.getConfiguration();
    if (insightConfig != null) {
      insightConfig.setFeatures(Collections.emptyMap());
      insightConfig.setSystemAllowlist(Collections.emptyList());
    }
    TaskScheduler taskScheduler = clmServer.getInstance(TaskScheduler.class);
    if (taskScheduler != null) {
      try {
        taskScheduler.standby();
        taskScheduler.clear();
      }
      catch (Exception e) {
        log.warn("Failed to reset task scheduler after test: {}", e.getMessage());
      }
    }
  }

  // --- REST access -----------------------------------------------------------------------------

  /** Authenticated (admin/admin123) request against the running server, base URL already applied. */
  public HttpRequest restRequest() {
    return HttpRequest.to(server.getCLMServer().getClientConfiguration().getServerUrl());
  }

  public String restBaseUrl() {
    return server.getCLMServer().getClientConfiguration().getServerUrl();
  }

  public void assertResponseStatus(final int expectedStatus, final HttpResponse response) {
    if (response.getStatusCode() != expectedStatus) {
      throw new AssertionError("Expected status " + expectedStatus + " but got " + response.getStatusCode()
          + ". URI:" + response.getUrl() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
          + response.getBodyText());
    }
  }

  // --- bean / data access ----------------------------------------------------------------------

  public <T> T lookup(final Class<T> type) {
    return server.getCLMServer().getInstance(type);
  }

  public TemporaryEntity tempEntity() {
    return tempEntity;
  }

  public DAOFactory daoFactory() {
    return daoFactory;
  }

  public File tempDir() {
    return tempFolder.getRoot();
  }

  /** The per-test JUnit {@link TemporaryFolder} (fresh each test) for helpers like ReportHelper.zipReport. */
  public TemporaryFolder tempFolder() {
    return tempFolder;
  }

  // --- HDS mock helpers ------------------------------------------------------------------------

  public com.sonatype.insight.brain.service.HdsMockServerRule getHdsServer() {
    return server.getHdsServer();
  }

  public HdsMockResponse hdsRespondWith(final Object body) {
    return getHdsServer().respondWith(body);
  }

  public HdsMockResponse hdsRespondWithResource(final String bodyResource) {
    URL resource = IqTestContext.class.getResource(bodyResource);
    if (resource == null) {
      throw new IllegalArgumentException("HDS mock resource not found on classpath: " + bodyResource);
    }
    return hdsRespondWith(resource);
  }

  public void mockReport(final String scanId, final String resourceName) {
    URL resourceUrl = ReportHelper.zipReport(resourceName, tempFolder);
    hdsRespondWith(resourceUrl).atUri("rest/application/analysis/" + scanId);
  }

  // --- license helpers -------------------------------------------------------------------------

  public void installLicense() throws Exception {
    // The server's ProductLicenseDetailsCache.saveJson updates a singleton product_license row and
    // NPEs if it is missing. TemporaryEntity.after() deletes that row on the reused server, so
    // re-establish it (matching a freshly booted server) before every install.
    ensureProductLicenseRow();
    HttpResponse response = HttpRequest.to(restBaseUrl())
        .path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
        .part("file", "sonatype.lic", new byte[1])
        .post();
    assertResponseStatus(200, response);
    licenseUninstalled = false;
  }

  private void ensureProductLicenseRow() {
    com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO dao =
        lookup(com.sonatype.insight.brain.dataaccess.configuration.ProductLicenseDAO.class);
    if (dao.get() == null) {
      // update() has upsert semantics: it inserts (and assigns the singleton id) when absent.
      // license_key is NOT NULL; the value is irrelevant here because the test license manager is a
      // mock — the server never validates this stored key. The REST install then fills in details.
      com.sonatype.insight.brain.model.configuration.ProductLicense productLicense =
          new com.sonatype.insight.brain.model.configuration.ProductLicense();
      productLicense.setLicenseKey("test-license-key");
      dao.update(productLicense);
    }
  }

  public void uninstallLicense() throws Exception {
    HttpResponse response = HttpRequest.to(restBaseUrl())
        .path(PublicApiPaths.PRODUCT_LICENSE_RESOURCE_PATH)
        .delete();
    assertResponseStatus(204, response);
    licenseUninstalled = true;
  }

  public void setFeatures(final LicensedFeature... features) throws Exception {
    licenseManager().setFeatures(features);
    includeSbomManagerStagesIfNeeded(features);
    installLicense();
  }

  public void setMissingFeature(final LicensedFeature feature) throws Exception {
    setMissingFeatures(feature);
  }

  public void setMissingFeatures(final LicensedFeature first, final LicensedFeature... rest) throws Exception {
    licenseManager().setFeatures(EnumSet.complementOf(EnumSet.of(first, rest)).toArray(new LicensedFeature[0]));
    installLicense();
  }

  /**
   * Overrides the products on the installed license and verifies the running server actually picked the change up.
   * <p>
   * This is the ONLY supported way for a test to change license products. Mutating the mock license manager directly
   * (for example {@code ((TestProductLicenseManager) ctx.lookup(ProductLicenseManager.class)).setProducts(...)}) only
   * rebuilds the in-memory license key — the REST install below is what makes the server recompute its cached
   * entitlements. If the mutation never reaches the running server the license stays at the full baseline that
   * {@link #beforeTest()} installs, and tests asserting an entitlement is ABSENT silently see it present (for example
   * {@code DEVELOPER_DASHBOARD}, which {@code FeaturesService} re-adds for any license carrying a Lifecycle product).
   * The check below turns that into an immediate, self-describing failure at the mutation site.
   */
  public void setLicenseProducts(final String... products) throws Exception {
    licenseManager().setProducts(products);
    installLicense();
    assertLicenseProductsReachedServer(products);
  }

  /**
   * Installs a license carrying exactly {@code products} AND a licensed-feature set that excludes
   * {@code missingFeature}, then verifies the running server no longer reports the feature.
   * <p>
   * This is what a test asserting "the entitlement is ABSENT" needs, because {@code FeaturesService.getFeatures()}
   * derives a feature from two independent inputs and BOTH have to be pinned:
   * <ul>
   * <li>{@code ProductLicense.getFeatures()} — the licensed feature set. {@code TestProductLicense.getFeatures()}
   * falls back to the full {@code EnumSet.allOf(LicensedFeature.class)} baseline that {@link #beforeTest()}
   * re-installs whenever the mock license manager's feature override is {@code null}, so the override has to be
   * set explicitly (a product-only downgrade cannot clear it).</li>
   * <li>{@code DeveloperEnablementService.shouldEnableDeveloperProduct()} — re-adds
   * {@code DEVELOPER_DASHBOARD} for any license carrying a Lifecycle product, so {@code products} must be
   * non-Lifecycle.</li>
   * </ul>
   * Unlike {@link #setLicenseProducts(String...)}, the check here is made against the server's COMPUTED feature
   * set, not against the mock the call just mutated — so a stale reused-server entitlement fails at the mutation
   * site with a message naming the layer that is still stale.
   */
  public void setLicenseWithoutFeature(
      final LicensedFeature missingFeature,
      final String... products) throws Exception
  {
    licenseManager().setProducts(products);
    licenseManager().setFeatures(EnumSet.complementOf(EnumSet.of(missingFeature)).toArray(new LicensedFeature[0]));
    installLicense();

    assertLicenseProductsReachedServer(products);
    assertServerFeatureAbsent(missingFeature);
  }

  /**
   * Fails when the running server still computes {@code feature} into its feature set, naming the layer that is
   * still stale. Use after any "downgrade the license" mutation whose test then asserts the entitlement is gone:
   * the mock license manager the mutation touched is NOT the same thing as the server's recomputed entitlements.
   */
  public void assertServerFeatureAbsent(final LicensedFeature feature) {
    java.util.Set<com.sonatype.insight.license.model.Feature> serverFeatures =
        lookup(com.sonatype.insight.brain.features.FeaturesService.class).getFeatures();
    if (serverFeatures.contains(feature)) {
      ProductLicense productLicense = lookup(ProductLicense.class);
      throw new AssertionError("The running server still reports " + feature
          + " after installing a license without it. products=" + productLicense.getProducts()
          + ", ProductLicense.getFeatures() contains it=" + productLicense.getFeatures().contains(feature)
          + ", hasLifecycleProduct="
          + com.sonatype.insight.brain.product.license.CLMLicenseManager.hasLifecycleProduct(productLicense)
          + ", ProductLicense.hasFeature=" + productLicense.hasFeature(feature)
          + ". The reused server's cached entitlements were not recomputed from the mutated license key.");
    }
  }

  private void assertLicenseProductsReachedServer(final String... products) {
    java.util.Set<String> expected = new java.util.LinkedHashSet<>(java.util.Arrays.asList(products));
    java.util.Set<String> actual = lookup(ProductLicense.class).getProducts();
    if (actual == null || !expected.equals(new java.util.LinkedHashSet<>(actual))) {
      throw new AssertionError("License products did not reach the running server: expected " + expected
          + " but the server reports " + actual
          + ". The reused server's cached license was not recomputed from the mutated license key.");
    }
  }

  private void includeSbomManagerStagesIfNeeded(final LicensedFeature[] features) {
    boolean hasSbomManager = false;
    for (LicensedFeature feature : features) {
      if (feature == LicensedFeature.SBOM_MANAGER) {
        hasSbomManager = true;
        break;
      }
    }
    if (!hasSbomManager) {
      return;
    }
    TestProductLicenseManager licenseManager = licenseManager();
    java.util.Set<StageType> stageTypes = licenseManager.getStageTypes();
    if (stageTypes == null) {
      licenseManager.setStageTypes(StageTypes.COMPLIANCE);
    }
    else {
      java.util.Set<StageType> updated = new java.util.LinkedHashSet<>(stageTypes);
      updated.add(StageTypes.COMPLIANCE);
      licenseManager.setStageTypes(updated.toArray(new StageType[0]));
    }
  }

  private TestProductLicenseManager licenseManager() {
    return (TestProductLicenseManager) lookup(ProductLicenseManager.class);
  }

  private TestProductLicense productLicense() {
    return (TestProductLicense) lookup(ProductLicense.class);
  }

  // --- config property helpers -----------------------------------------------------------------

  public void setProperties(final Map<String, Object> properties) {
    com.sonatype.insight.brain.api.v2.service.ApiConfigurationService service =
        lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class);
    service.setConfigurationInDatabaseNoAuthz(properties);
    service.applyConfigurationToClients(properties.keySet());
  }

  public void resetProperties(final String... propertyNames) {
    com.sonatype.insight.brain.api.v2.service.ApiConfigurationService service =
        lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class);
    if (service != null) {
      service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
      service.applyConfigurationToClients(propertyNames);
    }
  }

  public InsightWork insightWork() {
    return lookup(InsightWork.class);
  }

  // --- extra base-parity helpers (added as farmed conversions surfaced the need) --------------

  /** Unauthenticated-by-default admin-port request (mirrors AbstractBaseIntegrationTest.adminRequest). */
  public HttpRequest adminRequest() {
    return HttpRequest.to(server.getCLMServer().getClientConfiguration().getServerAdminUrl());
  }

  public String getUsername() {
    return server.getCLMServer().getClientConfiguration().getServerAuth().getUsername();
  }

  public Object getProperty(final String propertyName) {
    return lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class)
        .getConfigurationNoAuthz(propertyName);
  }

  public Map<String, Object> getProperties(final String... propertyNames) {
    return lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class)
        .getConfigurationNoAuthz(new java.util.HashSet<>(java.util.Arrays.asList(propertyNames)));
  }

  public void setBaseUrl(final String baseUrl) {
    Map<String, Object> properties = new java.util.HashMap<>();
    properties.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.BASE_URL, baseUrl);
    properties.put(com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.FORCE_BASE_URL, false);
    setProperties(properties);
  }
}
