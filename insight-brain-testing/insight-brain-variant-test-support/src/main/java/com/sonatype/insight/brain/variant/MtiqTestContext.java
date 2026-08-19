/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.variant;

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.product.TestProductLicenseRule;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TestRestTenantUtil;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.mock.hds.HdsMockResponse;

import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.licensing.product.ProductLicenseManager;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_LICENSE_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_SSO_ONLY;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SAML_ENABLED;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;

/**
 * The single injected object that gives a {@code @MtiqTest} class the same capabilities the legacy
 * {@code AbstractMultiTenantBaseIntegrationTest}/{@code AbstractMultiTenantBaseIntegrationResourceTest}
 * base classes provided — but by <b>composition</b>, not inheritance. It is the tenant-aware analog of
 * {@link IqTestContext}: it wraps the shared, reused multi-tenant {@link TestCLMServer} (booted once by
 * {@link AbstractMtiqServerExtension}) plus the {@link MultiTenantDatabaseContainerRule}, and per test
 * provisions a fresh tenant via the admin provisioning endpoint (JWT-authenticated), running data setup
 * and the test body under that tenant's context.
 *
 * <p>
 * All tenant plumbing (JWT stub, provisioning, admin requests, per-tenant license install, tenant/global
 * reset) is copied verbatim from {@code AbstractMultiTenantBaseIntegrationTest} — it is proven code.
 * Because the server is reused across every test method, {@link #beforeTest(String)} and
 * {@link #afterTest()} do the per-test setup/reset without restarting the server, provisioning a NEW
 * tenant each test (the primary state-isolation mechanism for MTIQ) and restoring the global tenant on
 * teardown.
 */
public final class MtiqTestContext
{
  private static final Logger log = LoggerFactory.getLogger(MtiqTestContext.class);

  private static final String JWT_ISSUER = "local/";

  private final TestCLMServer server;

  private final MultiTenantDatabaseContainerRule databaseContainerRule;

  private final TemporaryFolder tempFolder = new TemporaryFolder();

  private DAOFactory daoFactory;

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private TestProductLicenseRule testProductLicenseRule;

  private Tenant testTenant;

  // Not a rule - controlled below within the context of a tenant. MUST USE with `testAsTestTenant`.
  private TemporaryEntity tenantTemporaryEntity;

  MtiqTestContext(final TestCLMServer server, final MultiTenantDatabaseContainerRule databaseContainerRule) {
    this.server = server;
    this.databaseContainerRule = databaseContainerRule;
  }

  // --- per-test lifecycle (no server restart) --------------------------------------------------

  void beforeTest(final String testMethodName) throws Exception {
    tempFolder.create();

    // Reset the mocked HDS server so a prior test's stubbed responses/processors do not leak.
    getHdsServer().reset();

    // Re-establish the DAO-backed static collaborators (condition types, etc.) needed by policy
    // evaluation — statics are process-wide and get clobbered between tests.
    daoFactory = new TestDAOFactory(databaseContainerRule);
    StaticInjectionTestHelper.inject(daoFactory);

    systemConfigurationPropertyDAO = lookup(SystemConfigurationPropertyDAO.class);
    testProductLicenseRule = new TestProductLicenseRule(databaseContainerRule);

    // Stub MultiTenantJwkProvider so admin API JWTs validate.
    jwtSetup();

    // Provision a fresh tenant per test via the admin endpoint + JWT, seed base URL + license +
    // Configuration.register().
    provisionTestTenant(testMethodName);

    // For MTIQ the TemporaryEntity runs as the test tenant.
    testAsTestTenant(test -> {
      tenantTemporaryEntity = new TemporaryEntity(databaseContainerRule);
      tenantTemporaryEntity.before();
    });

    // setUpTestLicenseThreatGroups is intentionally a no-op for MTIQ (global is write-protected).
  }

  void afterTest() {
    try {
      try {
        if (testTenant != null && tenantTemporaryEntity != null) {
          testAsTestTenant(test -> tenantTemporaryEntity.after());
        }
      }
      finally {
        // Always restore the global tenant, even when the per-tenant entity cleanup above throws, so
        // the next test does not inherit a thread-local still pointing at this test's (dead) tenant.
        TenantTestHelper.setGlobalTenant();
      }

      if (systemConfigurationPropertyDAO != null && testTenant != null) {
        afterDatabaseReset();
      }

      if (server.isRunning() && systemConfigurationPropertyDAO != null) {
        try {
          disableSsoWithSaml();
          disableSsoWithOAuth2();
        }
        catch (RuntimeException e) {
          log.warn("Failed to disable SSO after test: {}", e.getMessage());
        }
      }
    }
    finally {
      resetSharedServerState();
      databaseContainerRule.resetMocks();
      getHdsServer().reset();
      tempFolder.delete();
    }
  }

  private void resetSharedServerState() {
    var clmServer = server.getCLMServer();
    clmServer.resetDisableForTesting();
    com.sonatype.insight.brain.service.InsightConfig insightConfig = clmServer.getConfiguration();
    if (insightConfig != null) {
      insightConfig.setFeatures(Collections.emptyMap());
      insightConfig.setSystemAllowlist(Collections.emptyList());
    }
  }

  // --- tenant provisioning (copied verbatim from AbstractMultiTenantBaseIntegrationTest) --------

  private void jwtSetup() {
    MultiTenantJwkProvider multiTenantJwkTestProvider = lookup(MultiTenantJwkProvider.class);
    try {
      String jwt = AuthorizationTestHelper.createJwt(JWT_ISSUER);
      DecodedJWT decodedJWT = JWT.decode(jwt);
      com.auth0.jwk.Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT.getKeyId());

      org.mockito.Mockito.lenient().when(multiTenantJwkTestProvider.denyRequest()).thenReturn(false);
      org.mockito.Mockito.lenient()
          .when(multiTenantJwkTestProvider.getJsonWebKey(decodedJWT.getKeyId()))
          .thenReturn(jwk);
      org.mockito.Mockito.lenient()
          .when(multiTenantJwkTestProvider.getIssuers())
          .thenReturn(new String[]{decodedJWT.getIssuer()});
    }
    catch (Exception e) {
      throw new IllegalStateException("Failed to setup mock JWT for MtiqTestContext", e);
    }
  }

  private void provisionTestTenant(final String testMethodName) {
    TestName testName = new TestName()
    {
      @Override
      public String getMethodName() {
        return testMethodName;
      }
    };
    testTenant = TenantTestHelper.setupNewTestTenant(testName);
    provisionTenant(testTenant.tenantSlug);
  }

  private void provisionTenant(final String tenantName) {
    setTenantSlug(tenantName);
    TenantProvisioningService tenantProvisioningService = lookup(TenantProvisioningService.class);

    TenantTestHelper.testAsNewTenant(tenantName, tenant -> {
      try {
        tenantProvisioningService.provisionTenant(tenant.tenantSlug);
      }
      catch (com.sonatype.insight.error.exception.ConflictException e) {
        log.info("Skipping tenant creation as tenant already exists: {}", tenantName);
      }
      // Initialize Configuration for the newly provisioned tenant.
      initializeConfigurationForTenant();
    });

    TenantTestHelper.testAsNewTenant(tenantName, tenant -> testProductLicenseRule.insertLicenseIfNeeded());

    // This endpoint call ensures the full tenant registration process is invoked via AdminTenantFilter,
    // which triggers TenantManager.setTenant() -> validateAndRegisterTenant() -> performRegistration().
    // This is necessary because TenantTestHelper.testAsNewTenant() bypasses TenantManager.setTenant().
    try {
      adminRestRequest(ADMIN_CONFIG_PATH)
          .parameter(tenantName)
          .get();
    }
    catch (Exception e) {
      throw new UncheckedException(e);
    }

    // AbstractBaseIntegrationTest seeds the global base URL after server startup, but MTIQ requests execute
    // under the tenant configuration. Persist the base URL directly for newly provisioned tenants and
    // refresh the tenant-scoped configuration cache so request-scoped flows do not see a null base URL.
    TenantTestHelper.testAsNewTenant(tenantName, tenant -> {
      systemConfigurationPropertyDAO.set(SystemConfigurationProperty.BASE_URL, "http://localhost");
      systemConfigurationPropertyDAO.set(SystemConfigurationProperty.FORCE_BASE_URL, Boolean.toString(false));
      initializeConfigurationForTenant();
    });
  }

  private void initializeConfigurationForTenant() {
    lookup(Configuration.class).register();
  }

  private void afterDatabaseReset() {
    // Reset Global tenant temp entity system props.
    testAsGlobal(g -> systemConfigurationPropertyDAO.set(ENABLE_SSO_ONLY, Boolean.toString(true)));
    testAsTestTenant(test -> systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true)));
  }

  private void disableSsoWithSaml() {
    if (Boolean.parseBoolean(systemConfigurationPropertyDAO.get(SAML_ENABLED))) {
      SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);
      samlConfigurationService.delete();
    }
    loadSsoConfiguration();
  }

  private void disableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    loadSsoConfiguration();
  }

  private void loadSsoConfiguration() {
    SsoUserService ssoUserService = lookup(SsoUserService.class);
    ssoUserService.loadSsoConfiguration();
  }

  private void setTenantSlug(final String tenantSlug) {
    ((TestRestTenantUtil) lookup(TenantUtil.class)).setTenantSlug(tenantSlug);
  }

  // --- tenant accessors / passthroughs ---------------------------------------------------------

  public Tenant getTestTenant() {
    return testTenant;
  }

  public String tenantSlug() {
    return testTenant.tenantSlug;
  }

  public void testAsTestTenant(final ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(testTenant, test);
  }

  public void testAsGlobal(final ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(GLOBAL_TENANT, test);
  }

  // --- REST access -----------------------------------------------------------------------------

  /**
   * Authenticated request against the running server. Tenant routing is via the shared
   * {@link TestRestTenantUtil} slug (set to the test tenant during provisioning), so requests execute as
   * the test tenant.
   */
  public HttpRequest restRequest() {
    HttpRequest request = HttpRequest.to(getRestBaseUrl());
    var serverAuth = server.getCLMServer().getClientConfiguration().getServerAuth();
    if (serverAuth != null) {
      return request.auth(serverAuth.getUsername(), new String(serverAuth.getPassword()));
    }
    return request;
  }

  public String getRestBaseUrl() {
    String restBaseUrl = server.getCLMServer().getClientConfiguration().getServerUrl();
    if (!restBaseUrl.endsWith("/")) {
      restBaseUrl = restBaseUrl + "/";
    }
    return restBaseUrl;
  }

  /** Unauthenticated-by-default admin-port request (mirrors AbstractBaseIntegrationTest.adminRequest). */
  public HttpRequest adminRequest() {
    return HttpRequest.to(server.getCLMServer().getClientConfiguration().getServerAdminUrl());
  }

  /**
   * JWT-bearer admin API request: {@code adminRequest().header(Authorization, Bearer <jwt>).path("api/").path(path)}.
   */
  public HttpRequest adminRestRequest(final String path) {
    String jwt;
    try {
      jwt = AuthorizationTestHelper.createJwt(JWT_ISSUER);
    }
    catch (Exception e) {
      throw new UncheckedException(e);
    }
    return adminRequest()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
        .path("api/")
        .path(path);
  }

  public void assertResponseStatus(final int expectedStatus, final HttpResponse response) {
    if (response.getStatusCode() != expectedStatus) {
      throw new AssertionError("Expected status " + expectedStatus + " but got " + response.getStatusCode()
          + ". URI:" + response.getUrl() + ", StatusText:" + response.getStatusText() + ", ResponseBody:"
          + response.getBodyText());
    }
  }

  public String getUsername() {
    return server.getCLMServer().getClientConfiguration().getServerAuth().getUsername();
  }

  public User createUserWithRole(final Permission... permissions) {
    AtomicReference<User> user = new AtomicReference<>();
    testAsTestTenant(t -> {
      User created = tempEntity().newUser();
      Role role = tempEntity().newRole(false /* global */, permissions);
      tempEntity().newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), created.getUsername());
      user.set(created);
    });
    return user.get();
  }

  // --- bean / data access ----------------------------------------------------------------------

  public <T> T lookup(final Class<T> type) {
    return server.getCLMServer().getInstance(type);
  }

  /** The <b>tenant</b> temporary entity (created and run inside {@code testAsTestTenant}). */
  public TemporaryEntity tempEntity() {
    return tenantTemporaryEntity;
  }

  public DAOFactory daoFactory() {
    return daoFactory;
  }

  public java.io.File tempDir() {
    return tempFolder.getRoot();
  }

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
    URL resource = MtiqTestContext.class.getResource(bodyResource);
    if (resource == null) {
      throw new IllegalArgumentException("HDS mock resource not found on classpath: " + bodyResource);
    }
    return hdsRespondWith(resource);
  }

  public void mockReport(final String scanId, final String resourceName) {
    URL resourceUrl = ReportHelper.zipReport(resourceName, tempFolder);
    hdsRespondWith(resourceUrl).atUri("rest/application/analysis/" + scanId);
  }

  public InsightWork insightWork() {
    return lookup(InsightWork.class);
  }

  // --- config property helpers -----------------------------------------------------------------

  public Object getProperty(final String propertyName) {
    return lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class)
        .getConfigurationNoAuthz(propertyName);
  }

  public Map<String, Object> getProperties(final String... propertyNames) {
    return lookup(com.sonatype.insight.brain.api.v2.service.ApiConfigurationService.class)
        .getConfigurationNoAuthz(new java.util.HashSet<>(java.util.Arrays.asList(propertyNames)));
  }

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

  public void setBaseUrl(final String baseUrl) {
    Map<String, Object> properties = new java.util.HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, baseUrl);
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, false);
    setProperties(properties);
  }

  // --- per-tenant license helpers (PUT + JWT against ADMIN_TENANT_LICENSE_PATH) -----------------

  public void installLicense() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(204, response);
    if (!licenseManager().isValid()) {
      throw new AssertionError("License manager is not valid after install");
    }
  }

  public HttpRequest licenseRequest() {
    return adminRestRequest(ADMIN_TENANT_LICENSE_PATH)
        .parameter(getTestTenant().tenantSlug)
        .part("file", "sonatype.lic", new byte[1]);
  }

  public HttpResponse uploadLicense(final HttpRequest licenseRequest) throws Exception {
    return licenseRequest.put();
  }

  public void setFeatures(final LicensedFeature... features) throws Exception {
    // MTIQ license must support EXTERNAL_DATABASE.
    LicensedFeature[] allFeatures =
        Stream.concat(Arrays.stream(features), Stream.of(LicensedFeature.EXTERNAL_DATABASE))
            .toArray(LicensedFeature[]::new);
    licenseManager().setFeatures(allFeatures);
    installLicense();
  }

  public void setMissingFeature(final LicensedFeature feature) throws Exception {
    setMissingFeatures(feature);
  }

  public void setMissingFeatures(final LicensedFeature first, final LicensedFeature... rest) throws Exception {
    LicensedFeature[] present = EnumSet.complementOf(EnumSet.of(first, rest)).toArray(new LicensedFeature[0]);
    LicensedFeature[] allFeatures =
        Stream.concat(Arrays.stream(present), Stream.of(LicensedFeature.EXTERNAL_DATABASE))
            .toArray(LicensedFeature[]::new);
    licenseManager().setFeatures(allFeatures);
    installLicense();
  }

  private TestProductLicenseManager licenseManager() {
    return (TestProductLicenseManager) lookup(ProductLicenseManager.class);
  }
}
