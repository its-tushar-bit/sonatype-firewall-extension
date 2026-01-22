/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Arrays;
import java.util.stream.Stream;
import jakarta.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpRequest;
import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.api.admin.service.TenantProvisioningService;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.TestMultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantTestHelper;
import com.sonatype.insight.brain.tenancy.TenantTestHelper.ConsumerWithException;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.brain.tenancy.TestRestTenantUtil;
import com.sonatype.insight.brain.testing.AbstractBaseIntegrationTest;
import com.sonatype.insight.brain.testing.InsightBrainServiceFactory;
import com.sonatype.insight.brain.testing.MultiTenantRule;
import com.sonatype.insight.brain.testing.MultiTenantTestInsightBrainServiceFactory;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.license.model.LicensedFeature;

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Binder;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_CONFIG_PATH;
import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_LICENSE_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_SSO_ONLY;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.SAML_ENABLED;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Base integration test class for multi-tenant IQ. {@link TemporaryEntity} resides here to manipulate data for the
 * single tenant.
 */
public abstract class AbstractMultiTenantBaseIntegrationTest
    extends AbstractBaseIntegrationTest
{
  protected static final ObjectMapper objectMapper = new ObjectMapper();

  protected static final TestRestTenantUtil tenantUtil = new TestRestTenantUtil();

  private static final Logger log = LoggerFactory.getLogger(AbstractMultiTenantBaseIntegrationTest.class);

  protected SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Rule(order = 0)
  public MultiTenantRule multiTenantRule = new MultiTenantRule();

  private Tenant testTenant;

  // Not a @Rule - controlled below within the context of a tenant. MUST USE with `testAsTestTenant`
  protected TemporaryEntity tenantTemporaryEntity;

  public static class MtiqDatabaseConfigurator
      implements Configurator
  {
    @Override
    public void configure(final InsightConfig insightConfig) {
      // set the MTIQ db attributes
      MultiTenantInsightConfig multiTenantInsightConfig = (MultiTenantInsightConfig) insightConfig;

      // this prevents the built-in admin user from being automatically deleted in TenantProvisioningService
      multiTenantInsightConfig.setDeleteBuiltInAdmin(false);

      MultiTenantDatabaseContainerRule multiTenantDbRule = MultiTenantDatabaseContainerRule.getInstance();

      multiTenantInsightConfig.setMainDatabase(multiTenantDbRule.getDatabaseConfig(DatabaseName.ods.name()));
      multiTenantInsightConfig.setLocksDatabase(multiTenantDbRule.getDatabaseConfig(DatabaseName.ods.name()));

      Auth0Config auth0Config = new Auth0Config();
      auth0Config.setDomain("local/");
      multiTenantInsightConfig.setAuth0Config(auth0Config);
    }

    @Override
    public boolean isReusable() {
      return true;
    }
  }

  @AfterClass
  public static void afterClass() {
    stopClmServer();
    MultiTenantDatabaseContainerRule.getInstance().markFixtureAsDirty();
  }

  @Override
  @Before
  public void initTest() throws Exception {
    super.initTest();
    log.info("@Before (AbstractMultiTenantBaseIntegrationTest.initTest): {}",
        testName.getMethodName());
  }

  @Override
  @After
  public void cleanupTest() throws Exception {
    log.info("@After (AbstractMultiTenantBaseIntegrationTest.cleanupTest): {}", testName.getMethodName());

    testAsTestTenant(test -> {
      tenantTemporaryEntity.after();
    });

    TenantTestHelper.setGlobalTenant();
    afterDatabaseReset();

    if (testCLMServer != null && testCLMServer.isRunning()) {
      disableSsoWithSaml();
      disableSsoWithOAuth2();
    }

    super.cleanupTest();
  }

  @Override
  protected void startIqTestServer(Configurator configurator) throws Exception {
    if (configurator == null) {
      configurator = new MtiqDatabaseConfigurator();
    }
    super.startIqTestServer(configurator);

    systemConfigurationPropertyDAO = getCLMServer().getInstance(SystemConfigurationPropertyDAO.class);

    jwtSetup();

    provisionTestTenant();

    // For MTIQ TemporaryEntity runs as the test tenant
    testAsTestTenant(test -> {
      tenantTemporaryEntity = createTenantTemporaryEntity();
      tenantTemporaryEntity.before();
    });
  }

  /**
   * Creates a new instance of {@link TemporaryEntity}. This method is protected to allow subclasses to override the
   * creation logic if needed, for example, to insert any dependencies or configurations used by the
   * {@link TemporaryEntity}
   *
   * @return a new instance of {@link TemporaryEntity}.
   */
  protected TemporaryEntity createTenantTemporaryEntity() {
    return new TemporaryEntity(databaseContainerRule);
  }

  /**
   * Returns the class of the {@link EncryptionKeyStore} to be used in the tests. This method is protected to allow
   * subclasses to override the key store class if needed.
   *
   * @return the class of the {@link EncryptionKeyStore}
   */
  protected Class<? extends EncryptionKeyStore> getEncryptionKeyStoreClass() {
    return TestMultiTenantEncryptionKeyStore.class;
  }

  @Override
  protected void configureTestBindings(final Binder binder) {
    binder.bind(TenantUtil.class).toInstance(tenantUtil);
    MultiTenantJwkProvider multiTenantJwkTestProvider = mock(MultiTenantJwkProvider.class);
    binder.bind(MultiTenantJwkProvider.class).toInstance(multiTenantJwkTestProvider);
    super.configureTestBindings(binder);
  }

  private void jwtSetup() {
    MultiTenantJwkProvider multiTenantJwkTestProvider = getCLMServer().getInstance(MultiTenantJwkProvider.class);

    try {
      String jwt = AuthorizationTestHelper.createJwt();
      DecodedJWT decodedJWT = JWT.decode(jwt);
      Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT.getKeyId());

      lenient().when(multiTenantJwkTestProvider.denyRequest()).thenReturn(false);
      lenient().when(multiTenantJwkTestProvider.getJsonWebKey(decodedJWT.getKeyId())).thenReturn(jwk);
      lenient().when(multiTenantJwkTestProvider.getIssuers()).thenReturn(new String[]{decodedJWT.getIssuer()});
    }
    catch (Exception e) {
      log.error("Failed to setup mock JWT for TestMultiTenantInsightBrainService", e);
    }
  }

  @Override
  protected DatabaseContainerRule getDatabaseContainerRule() {
    return MultiTenantDatabaseContainerRule.getInstance();
  }

  @Override
  protected void afterDatabaseReset() {
    // Reset Global tenant temp entity system props
    testAsGlobal(g -> {
      systemConfigurationPropertyDAO.set(ENABLE_SSO_ONLY, Boolean.toString(true));
    });

    testAsTestTenant(test -> systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true)));
  }

  @Override
  protected InsightBrainServiceFactory getInsightBrainServiceFactory() {
    return new MultiTenantTestInsightBrainServiceFactory();
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // no-op for MTIQ because the default creates LicenseThreatGroups under global which is write protected
  }

  /**
   * Provision the built-in test tenant
   */
  private void provisionTestTenant() {
    testTenant = TenantTestHelper.setupNewTestTenant(testName);

    provisionTenant(testTenant.tenantSlug);
  }

  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantNameFromTest(testName);
  }

  /**
   * Provision the given tenant name by invoking the admin provisioning endpoint
   */
  protected void provisionTenant(final String tenantName) {
    provisionTenant(tenantName, null);
  }

  /**
   * Provision the given tenant name by invoking the admin provisioning endpoint
   */
  protected void provisionTenant(final String tenantName, final ConsumerWithException<Tenant> consumer) {
    setTenantSlug(tenantName);
    TenantProvisioningService tenantProvisioningService = lookup(TenantProvisioningService.class);

    TenantTestHelper.testAsNewTenant(tenantName,
        tenant -> {
          try {
            tenantProvisioningService.provisionTenant(tenant.tenantSlug);
          }
          catch (ConflictException e) {
            log.info("Skipping tenant creation as tenant already exists: {}", tenantName);
          }
          // Initialize Configuration for the newly provisioned tenant
          initializeConfigurationForTenant();
        });

    TenantTestHelper.testAsNewTenant(tenantName, tenant -> {
      if (consumer != null) {
        consumer.accept(tenant);
      }
      testProductLicenseRule.insertLicenseIfNeeded();
    });

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
  }

  /**
   * Initialize Configuration for the current tenant. This is needed because tests use
   * TenantTestHelper.setTenantWithoutValidation which bypasses TenantManager.setTenant()
   * and thus doesn't trigger Configuration.register().
   */
  private void initializeConfigurationForTenant() {
    lookup(Configuration.class).register();
  }

  protected HttpRequest adminRestRequest(String path) {
    String jwt;
    try {
      jwt = AuthorizationTestHelper.createJwt();
    }
    catch (Exception e) {
      throw new UncheckedException(e);
    }

    return super.adminRequest()
        .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
        .path("api/").path(path);
  }

  @Override
  protected void installLicenseOnCleanup() throws Exception {
    // No-op - MTIQ the license is installed per Tenant in provisionTenant not per IQ instance
  }

  @Override
  protected void installLicense() throws Exception {
    HttpResponse response = uploadLicense(licenseRequest());
    assertResponseStatus(204, response);

    assertThat(licenseManager.isValid()).isTrue();
  }

  @Override
  protected HttpRequest licenseRequest(Object licenseFile) {
    HttpRequest request = null;
    try {
      request = adminRestRequest(ADMIN_TENANT_LICENSE_PATH)
          .parameter(getTestTenant().tenantSlug)
          .part("file", "sonatype.lic", licenseFile);
    }
    catch (Exception e) {
      log.error("Unable to create JWT for admin API access", e);
    }
    return request;
  }

  @Override
  protected HttpResponse uploadLicense(HttpRequest licenseRequest) throws Exception {
    HttpResponse response = licenseRequest.put();
    productlicenseWasUninstalled = false;
    return response;
  }

  @Override
  protected void setFeatures(LicensedFeature... features) throws Exception {
    // MTIQ license must support EXTERNAL_DATABASE
    LicensedFeature[] allFeatures = Stream.concat(Arrays.stream(features), Stream.of(LicensedFeature.EXTERNAL_DATABASE))
        .toArray(LicensedFeature[]::new);
    licenseManager.setFeatures(allFeatures);
    installLicense();
  }

  protected void setTenantSlug(String tenantSlug) {
    tenantUtil.setTenantSlug(tenantSlug);
  }

  /**
   * getTestTenant returns the pre provisioned tenant ready for use in test
   */
  protected Tenant getTestTenant() {
    return testTenant;
  }

  protected void testAsTestTenant(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(testTenant, test);
  }

  protected void testAsGlobal(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAsTenant(GLOBAL_TENANT, test);
  }

  public void enableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tenantTemporaryEntity.newOAuth2Configuration();
    loadSsoConfiguration();
  }

  public void disableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    loadSsoConfiguration();
  }

  public void enableSsoWithSaml() {
    systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true));
    tenantTemporaryEntity.newSamlConfiguration();
    loadSsoConfiguration();
  }

  public void disableSsoWithSaml() {
    if (Boolean.parseBoolean(systemConfigurationPropertyDAO.get(SAML_ENABLED))) {
      SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);
      samlConfigurationService.delete();
    }
    loadSsoConfiguration();
  }

  public void enableSamlByConfiguration() {
    testAsTestTenant(test -> systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(true)));
  }

  public void disableSamlByConfiguration() {
    testAsTestTenant(test -> systemConfigurationPropertyDAO.set(SAML_ENABLED, Boolean.toString(false)));
  }

  private void loadSsoConfiguration() {
    SsoUserService ssoUserService = lookup(SsoUserService.class);
    ssoUserService.loadSsoConfiguration();
  }
}
