/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.clm.testing.playwright.mtiq;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

import com.microsoft.playwright.assertions.LocatorAssertions;
import com.sonatype.clm.dto.model.remediation.VersionScoringDTO;
import com.sonatype.clm.testing.functional.utils.BaseUrl;
import com.sonatype.clm.testing.functional.utils.proxy.ReverseProxyServer;
import com.sonatype.clm.testing.playwright.AbstractPlaywrightTest;
import com.sonatype.clm.testing.playwright.pages.HeaderComponent;
import com.sonatype.clm.testing.playwright.pages.LoginPage;
import com.sonatype.clm.testing.playwright.pages.LoginPageAssertions;
import com.sonatype.clm.testing.playwright.pages.UnsavedChangesModalComponent;
import com.sonatype.clm.testing.playwright.utils.PlaywrightTiming;
import com.sonatype.clm.testing.playwright.utils.TestCredentials;
import com.sonatype.insight.brain.StaticInjectionTestHelper;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.auth.MultiTenantAuth0ApiSupplier;
import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.developer.integrationdashboard.DeveloperEnablementService;
import com.sonatype.insight.brain.hds.HdsClient;
import com.sonatype.insight.brain.jira.JiraService;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.TestProductLicenseRule;
import com.sonatype.insight.brain.product.license.CLMLicenseManager;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.service.Auth0Config;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.MultiTenantInsightConfig;
import com.sonatype.insight.brain.service.TestCLMServer;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TestRestTenantUtil;
import com.sonatype.insight.brain.testing.MultiTenantTestInsightBrainServiceFactory;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.license.model.LicensedFeature;

import com.auth0.client.auth.Auth0AuthAPI;
import com.auth0.client.mgmt.Auth0ManagementAPI;
import com.auth0.json.auth.TokenHolder;
import com.auth0.net.TokenRequest;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static com.microsoft.playwright.assertions.PlaywrightAssertions.assertThat;
import static com.sonatype.insight.brain.hds.VersionScoringService.HDS_BULK_SCORE_VERSIONING_PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * MTIQ sibling of {@code AbstractIqUiTest}: extends {@link AbstractPlaywrightTest} (browser lifecycle)
 * and bootstraps an embedded multi-tenant IQ server in a static initializer, provisioning a fresh
 * tenant per test via {@link TenantProvisioningService}. Requires PostgreSQL — MTIQ cannot run on H2.
 */
@Category(SlowTest.class)
public abstract class AbstractMtiqUiTest
    extends AbstractPlaywrightTest
{
  private static final Logger log = LoggerFactory.getLogger(AbstractMtiqUiTest.class);

  protected static final TestProductLicenseManager productLicenseManager;

  protected static final TestLicenseFingerprinter licenseFingerprinter;

  protected static final TestProductLicense testProductLicense;

  protected static final JiraService jiraService;

  protected static final MultiTenantAuth0ApiSupplier auth0ApiSupplier;

  protected static final TestCLMServer testCLMServer;

  protected static final ReverseProxyServer reverseProxyServer;

  protected static final TestRestTenantUtil tenantUtil;

  protected static final MultiTenantDatabaseContainerRule multiTenantDatabaseContainerRule =
      MultiTenantDatabaseContainerRule.getInstance();

  private static final TestProductLicenseRule testProductLicenseRule;

  protected static final DeveloperEnablementService mockDeveloperEnablementService =
      mock(DeveloperEnablementService.class);

  /**
   * The per-test tenant. Every browser request runs under this tenant once {@link #beforeTest()}
   * has set the {@link TestRestTenantUtil} slug.
   */
  private Tenant testTenant;

  static {
    MultiTenantInsightConfig insightConfig = startDbAndGetInsightConfig();

    productLicenseManager = new TestProductLicenseManager();
    licenseFingerprinter = new TestLicenseFingerprinter();
    testProductLicense = new TestProductLicense(productLicenseManager, mockDeveloperEnablementService);
    when(mockDeveloperEnablementService.shouldEnableDeveloperProduct()).thenReturn(true);
    tenantUtil = new TestRestTenantUtil();
    testProductLicenseRule = new TestProductLicenseRule(multiTenantDatabaseContainerRule.getDatabaseContainer());
    jiraService = mock(JiraService.class);
    auth0ApiSupplier = mock(MultiTenantAuth0ApiSupplier.class);
    initMocks();

    // SpringMultiTenantTestInsightBrainService always serves at the root context path ("/"),
    // regardless of iq.contextPath, so the browser base URL must not include a context prefix.
    String contextPath = "";

    // Use a dedicated MTIQ config that omits the hardcoded server connector ports. The MTIQ test
    // service does not re-apply the PortAllocator port override the way the single-tenant service
    // does; a hardcoded applicationConnectors port would otherwise win and bind Jetty to the wrong
    // port, causing the reverse proxy to hit connection-refused.
    Configurator configurator = new Configurator()
    {
      @Override
      public void configure(final InsightConfig config) {
        MultiTenantInsightConfig mtiqConfig = (MultiTenantInsightConfig) config;
        // Keep the built-in admin user so UI login as admin/admin123 works without SSO.
        mtiqConfig.setDeleteBuiltInAdmin(false);
        mtiqConfig.setUsingDefaultEncryptionKeyStore(true);
        mtiqConfig.setMainDatabase(insightConfig.getMainDatabase());
        mtiqConfig.setLocksDatabase(insightConfig.getLocksDatabase());
        Auth0Config auth0Config = new Auth0Config();
        auth0Config.setDomain("local/");
        mtiqConfig.setAuth0Config(auth0Config);
      }

      @Override
      public String getConfigFilePath() {
        return "target/test-classes/config-mtiq-test.yml";
      }
    };

    testCLMServer = new TestCLMServer(new MultiTenantTestInsightBrainServiceFactory(),
        false /* isProxyRequiredToReachHds */, List.of(MtiqPlaywrightTestConfiguration.class), configurator,
        multiTenantDatabaseContainerRule.getDatabaseContainer());
    int reverseProxyTarget = TestCLMServer.WEBPACK_DEV_MODE ? 8070 : testCLMServer.getCLMServer().getPort();
    reverseProxyServer = new ReverseProxyServer(reverseProxyTarget);

    try {
      testProductLicenseRule.insertLicenseIfNeeded();

      testCLMServer.start();
      reverseProxyServer.start();

      baseUrlFromTest = BaseUrl.resolveBaseUrl(getBaseUrl(contextPath));
      // Note: BASE_URL is configured per-tenant in beforeTest(), once the tenant slug is bound.
    }
    catch (Throwable e) {
      log.error("Failed to start embedded MTIQ infrastructure in static initializer", e);
      System.exit(1);
    }
  }

  private static MultiTenantInsightConfig startDbAndGetInsightConfig() {
    try {
      TenantTestHelper.initMultiTenantMode();
      multiTenantDatabaseContainerRule.setTestName("MtiqUiTest");
      multiTenantDatabaseContainerRule.before();
    }
    catch (Throwable e) {
      throw new RuntimeException(e);
    }

    DatabaseConfig databaseConfig =
        multiTenantDatabaseContainerRule.getDatabaseConfigProvider().getDatabaseConfig(DatabaseName.ods);

    MultiTenantInsightConfig insightConfig = new MultiTenantInsightConfig();
    insightConfig.setMainDatabase(databaseConfig);
    insightConfig.setLocksDatabase(databaseConfig); // reuse the same database for locks in tests
    return insightConfig;
  }

  private static String getBaseUrl(String contextPath) {
    String url = reverseProxyServer.getUrl();
    if (url.endsWith("/")) {
      url = url.substring(0, url.length() - 1);
    }
    url += contextPath;
    if (!url.endsWith("/")) {
      url += '/';
    }
    return url;
  }

  private static void initMocks() {
    try {
      Mockito.reset(jiraService, auth0ApiSupplier);

      when(jiraService.isEnabled()).thenReturn(false);
      Mockito.doThrow(new IllegalStateException()).when(jiraService).getProjectsWithAcceptableIssueTypes();

      Auth0ManagementAPI managementApi = mock(Auth0ManagementAPI.class);
      Auth0AuthAPI authApi = mock(Auth0AuthAPI.class);
      TokenHolder tokenHolder = mock(TokenHolder.class);
      TokenRequest tokenRequest = mock(TokenRequest.class);
      lenient().when(tokenHolder.getAccessToken()).thenReturn("");
      lenient().when(tokenHolder.getExpiresAt()).thenReturn(new Date(Long.MAX_VALUE));
      lenient().when(tokenRequest.execute()).thenReturn(tokenHolder);
      lenient().when(authApi.requestToken(anyString())).thenReturn(tokenRequest);
      // any() not anyString(): test Auth0Config leaves clientId/clientSecret null.
      lenient().when(auth0ApiSupplier.getManagementApi(any(), any())).thenReturn(managementApi);
      lenient().when(auth0ApiSupplier.getAuthApi(any(), any(), any())).thenReturn(authApi);
    }
    catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static void setBaseUrl(String baseUrl) {
    ApiConfigurationService service = testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationNoAuthz(SystemConfigurationProperty.BASE_URL, baseUrl);
  }

  public static void setEnableDefaultPasswordWarning(boolean enableDefaultPasswordWarning) {
    ApiConfigurationService service = testCLMServer.getCLMServer().getInstance(ApiConfigurationService.class);
    service.setConfigurationNoAuthz(SystemConfigurationProperty.ENABLE_DEFAULT_PASSWORD_WARNING,
        enableDefaultPasswordWarning);
  }

  private static void provisionTenant(String tenantSlug) {
    TenantProvisioningService service = testCLMServer.getCLMServer().getInstance(TenantProvisioningService.class);
    service.provisionTenant(tenantSlug);

    TenantTestHelper.testAsTenantAndInvalidate(tenantSlug,
        tenant -> new TestProductLicenseRule(multiTenantDatabaseContainerRule).insertLicenseIfNeeded());
  }

  @BeforeClass
  public static void disableWaitToCloseOldClients() {
    HdsClient.waitToCloseOldClients = false;
  }

  @BeforeClass
  public static void setUpClass() {
    Subject subject = mock(Subject.class);
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal("admin", "Admin", InternalRealm.ID));
    lenient().when(subject.associateWith(any(Runnable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(subject.associateWith(any(Callable.class))).thenAnswer(invocation -> invocation.getArgument(0));
    SecurityManager securityManager = mock(SecurityManager.class);
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);

    testCLMServer.getCLMServer().setHdsUrl();
  }

  @AfterClass
  public static void tearDownClass() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  /** Per-test {@link TemporaryEntity}; reset runs under the global tenant. */
  @Rule
  public TemporaryEntity tempEntity = new TemporaryEntity(multiTenantDatabaseContainerRule)
  {
    @Override
    public void after() {
      Runnable superAfter = super::after;
      TenantTestHelper.testAsTenant(Tenant.GLOBAL_TENANT, tenant -> {
        superAfter.run();
        afterDatabaseReset();
      });
    }

    @Override
    public void initializePersistedUserSessions() {
      // noop
    }

    @Override
    public void cleanupPersistedUserSessions() {
      // noop
    }
  };

  protected void afterDatabaseReset() {
    // hook for subclasses
  }

  @Before
  public final void beforeTest() {
    log.info("Before: {}", testName.getMethodName());

    // The global tenant must be active for the system-wide configuration calls below; applying them
    // under a freshly provisioned tenant hits an unseeded per-tenant Configuration cache and NPEs.
    TenantTestHelper.setGlobalTenant();
    setEnableDefaultPasswordWarning(false);
    setBaseUrl(baseUrlFromTest);

    DAOFactory daoFactory = new TestDAOFactory(multiTenantDatabaseContainerRule.getDatabaseContainer());
    StaticInjectionTestHelper.inject(daoFactory);

    testTenant = TenantTestHelper.setupNewTestTenant(testName);
    provisionTenant(testTenant.tenantSlug);

    // From now on every browser/API request resolves to the freshly provisioned tenant.
    tenantUtil.setTenantSlug(testTenant.tenantSlug);

    mockHdsVersionScoringResponse();
  }

  @After
  public void afterTest() throws Exception {
    log.info("After: {}", testName.getMethodName());
    InsightConfig insightConfig = testCLMServer.getCLMServer().getConfiguration();
    if (insightConfig != null) {
      insightConfig.setFeatures(Collections.emptyMap());
    }
    initMocks();
    if (!testCLMServer.isRunning()) {
      testCLMServer.start();
    }
    testCLMServer.getHdsServer().reset();

    if (tenantUtil.getTenantSlug() != null && productLicenseManager.wasChanged()) {
      TenantTestHelper.testAsTenantAndInvalidate(tenantUtil.getTenantSlug(), tenant -> {
        productLicenseManager.reset();
        installLicense();
      });
    }

    TenantTestHelper.setGlobalTenant();
    tenantUtil.clearTenantSlug();
  }

  protected <T> T lookup(Class<T> type) {
    return testCLMServer.getCLMServer().getInstance(type);
  }

  private void mockHdsVersionScoringResponse() {
    testCLMServer.getHdsServer()
        .respondWith(new VersionScoringDTO[]{})
        .atUri(HDS_BULK_SCORE_VERSIONING_PATH);
  }

  protected void installLicense() {
    testProductLicenseRule.insertLicenseIfNeeded();
    try {
      lookup(CLMLicenseManager.class).installLicense(new java.io.ByteArrayInputStream(new byte[1]));
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected void setFeatures(LicensedFeature... features) {
    productLicenseManager.setFeatures(features);
    installLicense();
  }

  public String getUsername() {
    return getClass().getSimpleName();
  }

  /**
   * Create a user with the given global permissions in the current tenant, mirroring the legacy
   * MTIQ functional-test {@code newUser(perms)} helper.
   */
  public User newUser(Permission... perms) {
    User user = tempEntity.newUser();
    Role role = tempEntity.newRole(true /* global */, perms);
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), user.getUsername());
    return user;
  }

  /**
   * Create a permissionless user named after the test class, mirroring the legacy
   * {@code AbstractMtiqFunctionalTest.createUser()}. Permissions are then granted incrementally via
   * {@link #grantPermissions(String, String, Permission...)}.
   */
  public User createUser() {
    return tempEntity.newUser(getUsername());
  }

  /**
   * Grant the given permissions to {@code username} within {@code contextId} (e.g.
   * {@link MembershipMapping#GLOBAL_CONTEXT_ID}) by creating a non-global role and a membership
   * mapping. Mirrors {@code AbstractMtiqFunctionalTest.grantPermissions(...)}.
   */
  public void grantPermissions(String username, String contextId, Permission... perms) {
    Role role = tempEntity.newRole(false /* global */, perms);
    tempEntity.newMembershipMapping(contextId, role.getId(), username);
  }

  /**
   * Set the licensed products on the test license manager and re-install so the running server
   * reflects them. Mirrors {@code AbstractMtiqFunctionalTest.setLicensedProducts(...)}.
   */
  protected void setLicensedProducts(String... products) {
    productLicenseManager.setProducts(products);
    installLicense();
  }

  // ---------------------------------------------------------------------------------------------
  // Login helpers (mirror AbstractIqUiTest — the IQ login modal is identical in MTIQ).
  // ---------------------------------------------------------------------------------------------

  protected void playwrightLogin() {
    playwrightLogin(TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);
  }

  protected void playwrightLogin(String username, String password) {
    log.debug("Logging in as '{}' on current page (url='{}')", username, page.url());
    new LoginPage().loginAs(username, password);
    waitForAuthenticatedHeader();
  }

  protected void playwrightLoginAt(String path, String username, String password) {
    playwrightRefreshOrOpen(path);
    new LoginPage().loginAs(username, password);
    waitForAuthenticatedHeader();
  }

  protected void playwrightLoginAdminAt(String path) {
    playwrightLoginAt(path, TestCredentials.ADMIN_USERNAME, TestCredentials.ADMIN_PASSWORD);
  }

  private void waitForAuthenticatedHeader() {
    assertThat(new HeaderComponent().userMenu())
        .isVisible(new LocatorAssertions.IsVisibleOptions()
            .setTimeout(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS));
  }

  protected void playwrightLogout() {
    new HeaderComponent().logout();
    new UnsavedChangesModalComponent().dismissIfAppearsWithin(PlaywrightTiming.SHORT_UI_CUE_MS);
    new LoginPageAssertions(new LoginPage()).shouldBeVisibleWithin(PlaywrightTiming.MODAL_OR_LOGIN_TIMEOUT_MS);
  }
}
