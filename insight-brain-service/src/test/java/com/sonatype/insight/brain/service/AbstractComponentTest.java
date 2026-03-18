/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

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
import jakarta.inject.Inject;

import com.sonatype.insight.brain.MockCleaner;
import com.sonatype.insight.brain.TestLicenseFingerprinter;
import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.dto.ApiJiraConfigurationDTO;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ApiJiraConfigurationService;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import com.sonatype.insight.brain.configuration.saml.SamlConfigurationService;
import com.sonatype.insight.brain.dataaccess.DatamartUpdaterState;
import com.sonatype.insight.brain.dataaccess.PerpetualLockDAO;
import com.sonatype.insight.brain.dataaccess.TestSamlFactory;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.configuration.saml.SamlPasswordFactory;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDataHelper;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.hds.TelemetryId;
import com.sonatype.insight.brain.model.PerpetualLock;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.conditions.ConditionTypes;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductLicenseDetailsCache;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFile;
import com.sonatype.insight.brain.report.ApplicationReport.ReportFileLocationType;
import com.sonatype.insight.brain.report.ApplicationReportPersistenceService;
import com.sonatype.insight.brain.scheduler.QuartzJobSchedulingServiceRule;
import com.sonatype.insight.brain.scheduler.QuartzJobStoreTX;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.security.EncryptionKeyStore;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.brain.security.SsoUserService;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.testing.BrainInjectedTest;
import com.sonatype.insight.brain.utils.ReportHelper;
import com.sonatype.insight.json.store.JsonUtils;
import org.sonatype.licensing.product.ProductLicenseManager;
import org.sonatype.licensing.product.util.LicenseFingerprinter;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matcher;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import io.dropwizard.lifecycle.Managed;
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
import ru.vyarus.dropwizard.guice.module.context.SharedConfigurationState;

import static com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature.SAML_ENABLED;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * Support class for tests of Sisu components.
 */
public class AbstractComponentTest
    extends BrainInjectedTest
{
  private final Logger log = LoggerFactory.getLogger(getClass());

  @Inject
  protected SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  protected SamlConfigurationService samlConfigurationService;

  @Inject
  protected InsightWork insightWork;

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

  @Mock
  protected Subject subject;

  @Mock
  private SecurityManager securityManager;

  private final Collection<Managed> managedComponents = new ArrayList<>();

  @Before
  public void beforeTest() {
    log.info("Before: {}", testName.getMethodName());
    cleanupInsightWorkFiles();
    setUpTestLicenseThreatGroups();
    setUpSecurity();
  }

  @After
  public void afterTest() {
    log.info("After: {}", testName.getMethodName());
    releaseScmPerpetualLock();
    stopManagedComponents();
    tearDownSecurity();
    resetBaseUrl();
    resetAccessAllowlist();
    resetApiAccessAllowList();
    SharedConfigurationState.clear();
    disableSsoWithOAuth2();
    disableSsoWithSaml();
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
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames =
        ImmutableSet.of(SystemConfigurationProperty.BASE_URL, SystemConfigurationProperty.FORCE_BASE_URL);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetAccessAllowlist() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.ACCESS_ALLOWLIST);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  public void resetApiAccessAllowList() {
    ApiConfigurationService service = lookup(ApiConfigurationService.class);
    Set<String> propertyNames = ImmutableSet.of(SystemConfigurationProperty.API_ACCESS_ALLOW_LIST);
    service.deleteConfigurationInDatabaseNoAuthz(propertyNames);
    service.applyConfigurationToClients(propertyNames);
  }

  private void releaseScmPerpetualLock() {
    PerpetualLockDAO perpetualLockDAO = lookup(PerpetualLockDAO.class);
    String perpetualLockId = SourceControlLoadBalancer.SOURCE_CONTROL_EVENT_MAINTENANCE_LOCK;
    PerpetualLock perpetualLock = perpetualLockDAO.getPerpetualLockById(perpetualLockId);
    if (perpetualLock != null) {
      perpetualLockDAO.releasePerpetualLockForOwner(perpetualLockId, perpetualLock.getOwner());
    }
  }

  protected void setUpTestLicenseThreatGroups() {
    // Make sure the test LTGs are created on the root organization
    LicenseThreatGroupDataHelper.createTestLicenseThreatGroups(tempEntity);
  }

  protected void setUpSecurity() {
    lenient().when(subject.getPrincipal()).thenReturn(new UserPrincipal(USERNAME, "Test User", InternalRealm.ID));
    lenient().when(subject.associateWith(any(Runnable.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(subject.associateWith(any(Callable.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    lenient().when(securityManager.createSubject(any(SubjectContext.class))).thenReturn(subject);
    ThreadContext.bind(securityManager);
    ThreadContext.bind(subject);
  }

  protected void tearDownSecurity() {
    ThreadContext.unbindSecurityManager();
    ThreadContext.unbindSubject();
  }

  private void bindManagedComponentObserver(Binder binder) {
    InjectionListener<Object> injectionListener = injectee -> managedComponents.add((Managed) injectee);
    TypeListener typeListener = new TypeListener()
    {
      @Override
      public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
        encounter.register(injectionListener);
      }
    };
    binder.bindListener(new Matcher<TypeLiteral<?>>()
    {
      @Override
      public boolean matches(TypeLiteral<?> typeLiteral) {
        return Managed.class.isAssignableFrom(typeLiteral.getRawType());
      }
    }, typeListener);
  }

  private void stopManagedComponents() {
    // avoid leaking resources like thread pools
    for (Managed managedComponent : managedComponents) {
      try {
        managedComponent.stop();
      }
      catch (Exception ignored) {
        // irrelevant
      }
    }
  }

  @Override
  protected void overrideTestBindings(Binder binder) {
    // Call super to get any parent test bindings
    super.overrideTestBindings(binder);

    // Bind test-specific implementations in the override module so that
    // subclass configure() methods can override these bindings
    bindManagedComponentObserver(binder);
    InsightConfig config = new InsightConfig();
    try {
      // Get or create the sonatype-work folder - may already exist from previous test in same JVM
      File sonatypeWorkDir = new File(tempDir.getRoot(), "sonatype-work");
      if (!sonatypeWorkDir.exists()) {
        sonatypeWorkDir = tempDir.newFolder("sonatype-work");
      }
      config.setSonatypeWork(sonatypeWorkDir.getAbsolutePath());
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    tempEntity.newSystemConfigurationProperty(SystemConfigurationProperty.HDS_URL, "https://clm-staging.sonatype.com/");
    customizeConfig(config);
    binder.bind(InsightConfig.class).toInstance(config);
    binder.bind(ProductLicense.class).to(TestProductLicense.class);
    binder.bind(ProductLicenseDetailsCache.class).to(TestProductLicenseDetailsCache.class);
    binder.bind(ProductLicenseManager.class).to(TestProductLicenseManager.class);
    binder.bind(LicenseFingerprinter.class).to(TestLicenseFingerprinter.class);
    binder.bind(QuartzJobStoreTX.class).to(TestQuartzJobStoreTx.class);
    binder.bind(TaskScheduler.class).to(TestTaskScheduler.class);
    binder.bind(TelemetryId.class).toInstance(mock(TelemetryId.class));

    binder.requestStaticInjection(ConditionTypes.class);
    binder.requestStaticInjection(ConditionValueTypes.class);
    binder.requestStaticInjection(ConfigurationUtils.class);
    binder.requestStaticInjection(ComponentDetailsLoader.class);
    binder.requestStaticInjection(SystemConfigurationPropertyFeature.class);
    binder.bind(EncryptionKeyStore.class).to(TestEncryptionKeyStore.class);

    // Ensure SAML tests use the test password factory
    TestSamlFactory testSamlFactory = new TestSamlFactory();
    binder.bind(SamlPasswordFactory.class).toInstance(testSamlFactory.createSamlPasswordFactory());
  }

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
    SamlConfigurationService samlConfigurationService = lookup(SamlConfigurationService.class);

    // maintain previous
    boolean previousValue = SAML_ENABLED.isEnabled();
    SAML_ENABLED.setEnabled(true);
    samlConfigurationService.delete();
    SAML_ENABLED.setEnabled(previousValue);

    loadSsoConfiguration();
  }

  private void loadSsoConfiguration() {
    SsoUserService ssoUserService = lookup(SsoUserService.class);
    ssoUserService.loadSsoConfiguration();
  }

  private void cleanupInsightWorkFiles() {
    // Clean up SBOM files from previous test runs to ensure test isolation
    if (insightWork == null) {
      return;
    }

    try {
      File sbomDir = insightWork.getSbomDir();
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
}
