/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableSet;
import com.sonatype.insight.brain.MockCleaner;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.DatamartUpdaterState;
import com.sonatype.insight.brain.dataaccess.TestSamlFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlPasswordFactory;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.sbom.datastore.SbomPersistenceService;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingServiceRule;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.FIPSModeDetector;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SamlDeploymentManager;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.security.TestFipsEncryptionKeyStore;
import com.sonatype.insight.brain.service.config.StorageConfig;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.SubjectContext;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.TestName;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.mockito.quality.Strictness;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.AopTestUtils;

/**
 * Support class for tests of Spring components.
 *
 * <p>
 * <b>Migration Note:</b> This class uses the Spring-based test infrastructure.
 * The old module-style customization hook is no longer supported. Use `@TestConfiguration`
 * inner classes or override `setUpTestConfiguration()` to customize beans.
 * </p>
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AbstractComponentTest
    extends BrainInjectedTest
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  protected SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  protected SamlConfigurationService samlConfigurationService;

  protected InsightConfig insightConfig;

  protected InsightWork insightWork;

  private String originalSonatypeWork;

  @Rule
  public MockCleaner mockCleaner = new MockCleaner();

  @Rule
  public DatamartUpdaterState datamartUpdaterState = new DatamartUpdaterState();

  @Rule
  public TemporaryFolder tempDir = new TemporaryFolder();

  @Rule
  public TestName testName = new TestName();

  @Rule
  public MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);

  @Rule
  public QuartzJobSchedulingServiceRule quartzJobSchedulingServiceRule = new QuartzJobSchedulingServiceRule();

  protected static final String USERNAME = "testuser";

  private static final Class<?>[] STORAGE_SCOPED_SINGLETON_TYPES = {
    com.sonatype.insight.brain.scan.datastore.ScanPersistenceService.class,
    ApplicationReportPersistenceService.class,
    SbomPersistenceService.class
  };

  @Mock
  protected Subject subject;

  @Mock
  private SecurityManager securityManager;

  private final Collection<DisposableBean> disposableComponents = new ArrayList<>();

  @Before
  public void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    if (systemConfigurationPropertyDAO == null) {
      systemConfigurationPropertyDAO = daoFactory.createSystemConfigurationPropertyDAO();
    }
    if (samlConfigurationService == null) {
      samlConfigurationService = lookupIfAvailable(SamlConfigurationService.class);
    }
    if (insightConfig == null) {
      insightConfig = lookupIfAvailable(InsightConfig.class);
    }
    if (insightWork == null) {
      insightWork = lookupIfAvailable(InsightWork.class);
    }
    if (insightConfig != null) {
      originalSonatypeWork = insightConfig.getSonatypeWork().getPath();
      customizeConfig(insightConfig);
    }
    cleanupInsightWorkFiles();
    if (systemConfigurationPropertyDAO != null) {
      systemConfigurationPropertyDAO.invalidateCache();
    }
    setUpTestLicenseThreatGroups();
    grantDefaultTestUserAllPermissions();
    setUpSecurity();
    unwrapInjectedSpringProxies();
  }

  @After
  public void afterTest() {
    log.info("After: {}", testName.getMethodName());
    runCleanupStep("stop disposable components", this::stopDisposableComponents);
    runCleanupStep("tear down security", this::tearDownSecurity);
    runCleanupStep("reset base URL", this::resetBaseUrl);
    runCleanupStep("reset access allowlist", this::resetAccessAllowlist);
    runCleanupStep("reset API access allow list", this::resetApiAccessAllowList);
    runCleanupStep("reset advanced reporting insights", this::resetAdvancedReportingInsightsEnabled);
    runCleanupStep("disable OAuth2 SSO", this::disableSsoWithOAuth2);
    runCleanupStep("disable SAML SSO", this::disableSsoWithSaml);
    runCleanupStep("reset mutable singleton test state", this::resetMutableSingletonTestState);
  }

  public String getBaseUrl() {
    return systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL);
  }

  public void setBaseUrl(String baseUrl) {
    setBaseUrl(baseUrl, false);
  }

  public void setBaseUrl(String baseUrl, boolean forceBaseUrl) {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Map<String, Object> properties = new HashMap<>();
    properties.put(SystemConfigurationProperty.BASE_URL, baseUrl);
    properties.put(SystemConfigurationProperty.FORCE_BASE_URL, forceBaseUrl);
    service.setConfigurationInDatabaseNoAuthz(properties);
    service.applyConfigurationToClients(properties.keySet());
  }

  public void resetBaseUrl() {
    ApiConfigurationService service = lookupIfAvailable(ApiConfigurationService.class);
    if (service == null) {
      return;
    }
    Set<String> propertyNames =
        ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetAccessAllowlist() {
    ApiConfigurationService service = lookupIfAvailable(ApiConfigurationService.class);
    if (service == null) {
      return;
    }
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.ACCESS_ALLOWLIST);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetApiAccessAllowList() {
    ApiConfigurationService service = lookupIfAvailable(ApiConfigurationService.class);
    if (service == null) {
      return;
    }
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetAdvancedReportingInsightsEnabled() {
    ApiConfigurationService service = lookupIfAvailable(ApiConfigurationService.class);
    if (service == null) {
      return;
    }
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.ADVANCED_REPORTING_INSIGHTS_ENABLED);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  protected void setUpTestLicenseThreatGroups() {
    // Make sure the test LTGs are created on the root organization
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  protected void grantDefaultTestUserAllPermissions() {
    tempEntity.newUser(USERNAME);
    var role = tempEntity.newRole(true, Permission.values());
    tempEntity.newMembershipMapping(MembershipMapping.GLOBAL_CONTEXT_ID, role.getId(), USERNAME);
  }

  protected void setUpSecurity() {
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal(USERNAME, "Test User", InternalRealm.ID));
    lenient().when(subject.associateWith(any(Runnable.class)))
        .thenAnswer(invocation -> {
          Runnable runnable = invocation.getArgument(0);
          return (Runnable) () -> {
            ThreadContext.bind(securityManager);
            ThreadContext.bind(subject);
            try {
              runnable.run();
            }
            finally {
              ThreadContext.unbindSubject();
              ThreadContext.unbindSecurityManager();
            }
          };
        });
    lenient().when(subject.associateWith(any(Callable.class)))
        .thenAnswer(invocation -> {
          Callable<?> callable = invocation.getArgument(0);
          return (Callable<Object>) () -> {
            ThreadContext.bind(securityManager);
            ThreadContext.bind(subject);
            try {
              return callable.call();
            }
            finally {
              ThreadContext.unbindSubject();
              ThreadContext.unbindSecurityManager();
            }
          };
        });
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  protected void tearDownSecurity() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  private void stopDisposableComponents() {
    // avoid leaking resources like thread pools
    for (DisposableBean component : disposableComponents) {
      try {
        component.destroy();
      }
      catch (Exception ignored) {
        // irrelevant
      }
    }
  }

  /**
   * Register a disposable component to be cleaned up after the test.
   */
  protected void registerDisposable(DisposableBean component) {
    disposableComponents.add(component);
  }

  /**
   * Hook for tests that need to mutate {@link InsightConfig} before lazy beans are looked up.
   * Prefer dedicated {@code @TestConfiguration} beans when possible.
   *
   * <p>
   * Must be side-effect-free beyond mutating the config argument - may be called during
   * Spring context initialization before {@code @Before} methods run.
   * </p>
   */
  protected void customizeConfig(@SuppressWarnings("unused") InsightConfig config) {
    // hook for tests to tweak config before components grab it
  }

  public void createJiraConfiguration(Map<String, Object> customFields) {
    ApiJiraConfigurationDTO dto = new ApiJiraConfigurationDTO();
    dto.url = "http://url";
    dto.customFields = customFields;
    ApiJiraConfigurationService jiraConfigurationService = lookup(ApiJiraConfigurationService.class);
    jiraConfigurationService.setConfigurationInDatabaseNoAuthz(JsonUtils.asTree(dto));
    jiraConfigurationService.applyJiraConfigurationToClients();
  }

  public void setHdsUrl(String hdsUrl) {
    ApiConfigurationService configurationService = lookup(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL, hdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  public void testCallable_DisallowConcurrentExecution(
      Callable<Void> callable,
      Consumer<Answer<Void>> answerConsumer,
      final boolean callRealMethod) throws Exception
  {
    testCallable_ConcurrentExecution(callable, answerConsumer, false, callRealMethod);
  }

  public void testCallable_DisallowConcurrentExecution(
      Callable<Void> callable,
      Consumer<Answer<Void>> answerConsumer) throws Exception
  {
    testCallable_ConcurrentExecution(callable, answerConsumer, false, false);
  }

  public void testCallable_AllowConcurrentExecution(
      Callable<Void> callable,
      Consumer<Answer<Void>> answerConsumer) throws Exception
  {
    testCallable_ConcurrentExecution(callable, answerConsumer, true, false);
  }

  public void testCallable_ConcurrentExecution(
      Callable<Void> callable,
      Consumer<Answer<Void>> answerConsumer,
      boolean isAllowed,
      boolean callRealMethod) throws Exception
  {
    CountDownLatch started = new CountDownLatch(2);
    CountDownLatch block = new CountDownLatch(1);

    answerConsumer.accept(invocation -> {
      if (callRealMethod) {
        invocation.callRealMethod();
      }
      started.countDown();
      block.await();
      return null;
    });

    CountDownLatch oneFinished = new CountDownLatch(1);
    AtomicReference<Exception> oneException = new AtomicReference<>();
    Thread threadOne = new Thread(() -> {
      try {
        callable.call();
      }
      catch (Exception e) {
        oneException.set(e);
      }
      finally {
        oneFinished.countDown();
      }
    });

    CountDownLatch twoFinished = new CountDownLatch(1);
    AtomicReference<Exception> twoException = new AtomicReference<>();
    Thread threadTwo = new Thread(() -> {
      try {
        callable.call();
      }
      catch (Exception e) {
        twoException.set(e);
      }
      finally {
        twoFinished.countDown();
      }
    });

    threadOne.start();
    await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(1));

    threadTwo.start();
    if (isAllowed) {
      await().atMost(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(0));
    }
    else {
      await().pollDelay(2, TimeUnit.SECONDS).untilAsserted(() -> assertThat(started.getCount()).isEqualTo(1));
    }

    block.countDown();

    assertThat(oneFinished.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(oneException).hasValue(null);
    assertThat(twoFinished.await(2, TimeUnit.SECONDS)).isTrue();
    assertThat(twoException).hasValue(null);
  }

  public void enableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(true);
    tempEntity.newOAuth2Configuration();
    loadSsoConfiguration();
  }

  public void disableSsoWithOAuth2() {
    SystemConfigurationPropertyFeature.OAUTH2_ENABLED.setEnabled(false);
    loadSsoConfiguration();
  }

  public void enableSsoWithSaml() {
    samlConfigurationService.insert(tempEntity.newSamlConfiguration());
    loadSsoConfiguration();
  }

  public void disableSsoWithSaml() {
    SamlConfigurationService samlConfigurationService = lookupIfAvailable(SamlConfigurationService.class);
    if (samlConfigurationService == null) {
      return;
    }

    // maintain previous
    boolean previousValue = SAML_ENABLED.isEnabled();
    SAML_ENABLED.setEnabled(true);
    samlConfigurationService.delete();
    SAML_ENABLED.setEnabled(previousValue);

    loadSsoConfiguration();
  }

  private void loadSsoConfiguration() {
    SsoUserService ssoUserService = lookupIfAvailable(SsoUserService.class);
    if (ssoUserService == null) {
      return;
    }
    ssoUserService.loadSsoConfiguration();
  }

  private <T> T lookupIfAvailable(Class<T> type) {
    try {
      return lookup(type);
    }
    catch (BeansException e) {
      return null;
    }
  }

  private void unwrapInjectedSpringProxies() {
    if (preserveAopProxies()) {
      return;
    }

    for (Class<?> type = getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
      for (java.lang.reflect.Field field : type.getDeclaredFields()) {
        if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
          continue;
        }
        if (!field.isAnnotationPresent(jakarta.inject.Inject.class)
            && !field.isAnnotationPresent(org.springframework.beans.factory.annotation.Autowired.class))
        {
          continue;
        }
        try {
          field.setAccessible(true);
          Object bean = field.get(this);
          Object target = unwrapProxy(bean);
          if (target != null && target != bean) {
            field.set(this, target);
          }
        }
        catch (IllegalAccessException ignored) {
          // best-effort test compatibility only
        }
      }
    }
  }

  private static Object unwrapProxy(final Object bean) {
    if (bean == null) {
      return null;
    }
    try {
      if (bean instanceof Advised advised) {
        Object target = advised.getTargetSource().getTarget();
        if (target != null) {
          return target;
        }
      }
      return AopTestUtils.getUltimateTargetObject(bean);
    }
    catch (Exception ignored) {
      return bean;
    }
  }

  private void resetMutableSingletonTestState() {
    if (insightConfig != null) {
      if (originalSonatypeWork != null) {
        insightConfig.setSonatypeWork(originalSonatypeWork);
      }
      insightConfig.setStorage(new StorageConfig());
      insightConfig.setDatabase(null);
    }

    resetProductLicenseTestState();
    destroyStorageScopedSingletons();

    SamlDeploymentManager samlDeploymentManager = lookupIfAvailable(SamlDeploymentManager.class);
    if (samlDeploymentManager != null) {
      samlDeploymentManager.deregister();
    }
  }

  private void runCleanupStep(final String description, final Runnable cleanup) {
    try {
      cleanup.run();
    }
    catch (RuntimeException e) {
      log.warn("Skipping cleanup step '{}' after partial test initialization failure", description, e);
    }
  }

  private void resetProductLicenseTestState() {
    TestProductLicenseManager testProductLicenseManager = lookupIfAvailable(TestProductLicenseManager.class);
    if (testProductLicenseManager != null) {
      testProductLicenseManager.reset();
    }

    TestProductLicenseDetailsCache testProductLicenseDetailsCache =
        lookupIfAvailable(TestProductLicenseDetailsCache.class);
    if (testProductLicenseDetailsCache != null) {
      testProductLicenseDetailsCache.resetToDefaults();
    }

    TestProductLicense testProductLicense = lookupIfAvailable(TestProductLicense.class);
    if (testProductLicense != null) {
      testProductLicense.reset();
    }
  }

  private void destroyStorageScopedSingletons() {
    if (!(getApplicationContext() instanceof ConfigurableApplicationContext applicationContext)) {
      return;
    }

    ConfigurableListableBeanFactory factory = applicationContext.getBeanFactory();
    if (!(factory instanceof DefaultListableBeanFactory beanFactory)) {
      return;
    }
    for (Class<?> singletonType : STORAGE_SCOPED_SINGLETON_TYPES) {
      for (String beanName : applicationContext.getBeanNamesForType(singletonType, false, false)) {
        if (beanFactory.containsSingleton(beanName)) {
          beanFactory.destroySingleton(beanName);
        }
      }
    }
  }

  private void cleanupInsightWorkFiles() {
    // Clean up SBOM files from previous test runs to ensure test isolation
    if (insightWork == null) {
      return;
    }

    try {
      File sbomDir = insightWork.getSbomDir(false);
      if (sbomDir.exists()) {
        deleteDirectory(sbomDir);
      }
    }
    catch (IOException e) {
      throw new UncheckedIOException("Failed to clean up SBOM directory", e);
    }
  }

  public void createReport(
      final ApplicationReportPersistenceService service,
      final PolicyEvaluation eval,
      final int contentSizeInBytes) throws Exception
  {
    createReport(service, eval, null, contentSizeInBytes, null);
  }

  public void createReport(
      final ApplicationReportPersistenceService service,
      final PolicyEvaluation eval,
      final String prefix,
      final int contentSizeInBytes,
      final String suffix) throws Exception
  {
    String reportZipName = "report.zip";
    Path zipPath = tempDir.getRoot().toPath().resolve(reportZipName);
    ReportHelper.createEmptyZip(zipPath);
    for (ReportFile reportFile : ReportFile.values()) {
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL)) {
        ReportHelper.addToZip(zipPath, zipPath.resolve(reportFile.getName()),
            createRandomInputStream(prefix, contentSizeInBytes, suffix));
      }
    }
    // Create report.zip
    try (InputStream inputStream = new FileInputStream(zipPath.toFile())) {
      service.saveOriginalReport(eval.getApplicationId(), eval.getScanId(), inputStream);
    }
    for (ReportFile reportFile : ReportFile.values()) {
      // Create report.cache
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ORIGINAL) ||
          reportFile.getLocationTypes().contains(ReportFileLocationType.CACHE))
      {
        try (InputStream content = createRandomInputStream(prefix, contentSizeInBytes, suffix)) {
          service.saveReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
      // Create additional.files
      if (reportFile.getLocationTypes().contains(ReportFileLocationType.ADDITIONAL)) {
        try (InputStream content = createRandomInputStream(prefix, contentSizeInBytes, suffix)) {
          service.saveAdditionalReportFile(eval.getApplicationId(), eval.getScanId(), reportFile.getName(), content);
        }
      }
    }
    // Create report.pdf
    try (InputStream inputStream = createRandomInputStream(prefix, contentSizeInBytes, suffix);
        OutputStream outputStream = service.getPdfEntity(eval.getApplicationId(), eval.getScanId())
            .getOutputStream())
    {
      inputStream.transferTo(outputStream);
    }
  }

  public InputStream createRandomInputStream(final int numberOfBytes) {
    return createRandomInputStream(null, numberOfBytes, null);
  }

  public InputStream createRandomInputStream(
      final String prefix,
      final int numberOfBytes,
      final String suffix)
  {
    byte[] prefixBytes = prefix != null ? prefix.getBytes(StandardCharsets.UTF_8) : new byte[0];
    byte[] suffixBytes = suffix != null ? suffix.getBytes(StandardCharsets.UTF_8) : new byte[0];
    return new InputStream()
    {
      private static final char[] ALLOWED_CHARS =
          "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

      private final Random random = new Random();

      private int prefixIndex = 0;

      private int randomRemaining = numberOfBytes;

      private int suffixIndex = 0;

      @Override
      public int read() {
        // 1. Read prefix
        if (prefixIndex < prefixBytes.length) {
          return prefixBytes[prefixIndex++] & 0xFF;
        }

        // 2. Read random bytes
        if (randomRemaining > 0) {
          randomRemaining--;
          return ALLOWED_CHARS[random.nextInt(ALLOWED_CHARS.length)];
        }

        // 3. Read suffix
        if (suffixIndex < suffixBytes.length) {
          return suffixBytes[suffixIndex++] & 0xFF;
        }

        // 4. End of stream
        return -1;
      }
    };
  }

  /**
   * Test configuration for common test beans.
   * Subclasses can override by providing their own @TestConfiguration.
   *
   * <p>
   * Note: Many test beans (TestProductLicense, TestQuartzJobStoreTx, TestTaskScheduler, etc.)
   * are auto-discovered by Spring via @Named/@Singleton annotations and @Inject constructors.
   * Only beans without auto-discovery are defined here.
   */
  @TestConfiguration
  static class ComponentTestConfiguration
  {

    @Bean
    public InsightConfig insightConfig() {
      return new InsightConfig();
    }

    @Bean
    @Primary
    public EncryptionKeyStore encryptionKeyStore() {
      if (FIPSModeDetector.isEnabled()) {
        return new TestFipsEncryptionKeyStore();
      }
      return new TestEncryptionKeyStore();
    }

    @Bean
    public TelemetryId telemetryId() {
      return mock(TelemetryId.class);
    }

    @Bean
    public SamlPasswordFactory samlPasswordFactory() {
      TestSamlFactory testSamlFactory = new TestSamlFactory();
      return testSamlFactory.createSamlPasswordFactory();
    }
  }
}
