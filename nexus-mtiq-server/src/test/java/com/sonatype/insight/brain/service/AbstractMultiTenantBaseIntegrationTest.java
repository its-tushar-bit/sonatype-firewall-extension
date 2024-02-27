/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;

import com.sonatype.insight.brain.HttpResponse;
import com.sonatype.insight.brain.api.admin.authorization.AuthorizationTestHelper;
import com.sonatype.insight.brain.api.admin.authorization.provider.MultiTenantJwkProvider;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.rule.DatabaseContainerRule;
import com.sonatype.insight.brain.db.rule.MultiTenantDatabaseContainerRule;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
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

import com.auth0.jwk.Jwk;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import org.apache.commons.lang3.exception.UncheckedException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.api.AdminApiPaths.ADMIN_TENANT_PROVISIONING_PATH;
import static com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty.ENABLE_SSO_ONLY;
import static com.sonatype.insight.brain.tenancy.Tenant.GLOBAL_TENANT;
import static org.mockito.Mockito.when;

/**
 * Base integration test class for multi-tenant IQ. {@link TemporaryEntity} resides here to manipulate data for the
 * single tenant.
 */
public abstract class AbstractMultiTenantBaseIntegrationTest
    extends AbstractBaseIntegrationTest
{
  protected static final TestRestTenantUtil tenantUtil = new TestRestTenantUtil();

  private static final Logger log = LoggerFactory.getLogger(AbstractMultiTenantBaseIntegrationTest.class);

  private SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Rule(order = 0)
  public MultiTenantRule multiTenantRule = new MultiTenantRule();

  private Tenant testTenant;

  // Not a @Rule - controlled below within the context of a tenant. MUST USE with `testAsTestTenant`
  protected TemporaryEntity tenantTemporaryEntity;

  protected static final Configurator MTIQ_DATABASE_CONFIGURATOR = new Configurator()
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
    }

    @Override
    public boolean isReusable() {
      return true;
    }
  };

  @AfterClass
  public static void afterClass() {
    stopClmServer();
    MultiTenantDatabaseContainerRule.getInstance().markDatabaseAsDirty();
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

    super.cleanupTest();
  }

  @Override
  protected void startIqTestServer(Configurator configurator) throws Exception {
    if (configurator == null) {
      configurator = MTIQ_DATABASE_CONFIGURATOR;
    }
    super.startIqTestServer(configurator);

    systemConfigurationPropertyDAO = getCLMServer().getInstance(SystemConfigurationPropertyDAO.class);

    jwtSetup();

    setupNewTestTenant();

    // For MTIQ TemporaryEntity runs as the test tenant
    testAsTestTenant(test -> {
      tenantTemporaryEntity = new TemporaryEntity(databaseContainerRule);
      tenantTemporaryEntity.before();
    });
  }

  @Override
  protected List<Module> getBrainModules() {
    List<Module> modules = super.getBrainModules();
    modules.add(new AbstractModule()
    {
      @Override
      protected void configure() {
        bind(TenantUtil.class).toInstance(tenantUtil);
        bind(EncryptionKeyStore.class).to(TestMultiTenantEncryptionKeyStore.class);
      }
    });

    return modules;
  }

  private void jwtSetup() {
    MultiTenantJwkProvider multiTenantJwkTestProvider = getCLMServer().getInstance(MultiTenantJwkProvider.class);

    try {
      String jwt = AuthorizationTestHelper.createJwt();
      DecodedJWT decodedJWT = JWT.decode(jwt);
      Jwk jwk = AuthorizationTestHelper.createJwk(decodedJWT.getKeyId());

      when(multiTenantJwkTestProvider.denyRequest()).thenReturn(false);
      when(multiTenantJwkTestProvider.getJsonWebKey(decodedJWT.getKeyId())).thenReturn(jwk);
      when(multiTenantJwkTestProvider.getIssuer()).thenReturn(decodedJWT.getIssuer());
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
  }

  @Override
  protected InsightBrainServiceFactory getInsightBrainServiceFactory() {
    return new MultiTenantTestInsightBrainServiceFactory();
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // no-op for MTIQ because the default creates LicenseThreatGroups under global which is write protected
  }

  private void setupNewTestTenant() {
    testTenant = TenantTestHelper.setupNewTestTenant(testName);

    provisionTenant(testTenant.tenantSlug);
  }

  protected String generateTestTenantName() {
    return TenantTestHelper.createTenantNameFromTest(testName);
  }

  protected HttpResponse provisionTenant(String tenantName) {
    setTenantSlug(tenantName);

    try {
      HttpResponse httpResponse = adminRequest()
          .path("api/")
          .path(ADMIN_TENANT_PROVISIONING_PATH)
          .parameter(tenantName)
          .header(HttpHeaders.AUTHORIZATION, "Bearer " + AuthorizationTestHelper.createJwt())
          .post();

      TenantTestHelper.testAs(tenantName, tenant -> {
        testProductLicenseRule.insertLicenseIfNeeded();
      });
      return httpResponse;
    }
    catch (Exception e) {
      throw new UncheckedException(e);
    }
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
    TenantTestHelper.testAs(testTenant, test);
  }

  protected void testAsGlobal(ConsumerWithException<Tenant> test) {
    TenantTestHelper.testAs(GLOBAL_TENANT, test);
  }
}
