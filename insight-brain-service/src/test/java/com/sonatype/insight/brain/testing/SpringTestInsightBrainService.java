/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.testing;

import com.sonatype.insight.brain.TestProductLicenseManager;
import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.firewall.metrics.DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentQuarantinedConsolidatorCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentWaivedConsolidatorCronJob;
import com.sonatype.insight.brain.firewall.metrics.FirewallMetricsComponentsAutoReleasedConsolidatorCronJob;
import com.sonatype.insight.brain.git.DefaultBranchMonitor;
import com.sonatype.insight.brain.git.PullRequestCommentPurger;
import com.sonatype.insight.brain.git.PullRequestMonitor;
import com.sonatype.insight.brain.git.PullRequestPollingScheduler;
import com.sonatype.insight.brain.git.event.orchestrate.SourceControlEventOrchestrator;
import com.sonatype.insight.brain.integration.repository.FirewallIgnorePatternUpdater;
import com.sonatype.insight.brain.migration.ScanFileCleaner;
import com.sonatype.insight.brain.model.configuration.ProxyServerConfiguration;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.organization.ApplicationCountHistoryKeeper;
import com.sonatype.insight.brain.policy.evaluator.PersistedPolicyEvaluationPollingResultCleaner;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueConsumer;
import com.sonatype.insight.brain.policy.evaluator.queue.EvaluationQueueProducer;
import com.sonatype.insight.brain.policy.waiver.WaivedComponentUpgradeScheduler;
import com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener;
import com.sonatype.insight.brain.product.license.TestProductLicense;
import com.sonatype.insight.brain.product.license.TestProductLicenseDetailsCache;
import com.sonatype.insight.brain.product.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.report.ReportPurger;
import com.sonatype.insight.brain.repository.ReevaluateCascadeRequestCleaner;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.sbom.PendingSbomMetadataCleaner;
import com.sonatype.insight.brain.scan.PersistedScanTicketCleaner;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.scheduler.TestQuartzJobStoreTx;
import com.sonatype.insight.brain.scheduler.TestTaskScheduler;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.security.DefaultEncryptionKeyStore;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.security.TestEncryptionKeyStore;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.TestInsightBrainService;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.spring.config.AdminCompatibilityConfiguration;
import com.sonatype.insight.brain.spring.config.DatabaseConfiguration;
import com.sonatype.insight.brain.spring.config.DropwizardManagementConnectorConfiguration;
import com.sonatype.insight.brain.spring.config.InsightBrainConfiguration;
import com.sonatype.insight.brain.spring.config.NamedBeanRegistrationConfiguration;
import com.sonatype.insight.brain.spring.config.SingleTenantAdminFilterConfiguration;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsPurger;
import com.sonatype.insight.brain.telemetry.ClusterTelemetryTask;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillTask;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import jakarta.inject.Named;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.jersey.autoconfigure.JerseyAutoConfiguration;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MapPropertySource;

/**
 * Spring Boot-based test service that implements TestInsightBrainService.
 * Replaces DefaultTestInsightBrainService which was Dropwizard-based.
 */
@SpringBootApplication(exclude = {
  JerseyAutoConfiguration.class
})
@ComponentScan(
    basePackages = {
      "com.sonatype.insight.brain",
      "com.sonatype.insight.jaxrs"
    },
    includeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = Named.class)
    },
    excludeFilters = {
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = DatabaseConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = AdminCompatibilityConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = DropwizardManagementConnectorConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = SingleTenantAdminFilterConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = DataStoreTestModule.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = DefaultEncryptionKeyStore.class),
      @ComponentScan.Filter(
          type = FilterType.ASSIGNABLE_TYPE,
          classes = {
            TestProductLicense.class,
            TestProductLicenseManager.class,
            TestProductLicenseDetailsCache.class,
            TestQuartzJobStoreTx.class,
            TestTaskScheduler.class
          }),
      @ComponentScan.Filter(
          type = FilterType.CUSTOM,
          classes = ExcludeTestClassPathTypeFilter.class),
      @ComponentScan.Filter(
          type = FilterType.REGEX,
          pattern = ".*(Test|IT)([.$].*)?$"),
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = TestConfiguration.class),
      @ComponentScan.Filter(
          type = FilterType.ANNOTATION,
          classes = TestComponent.class)
    })
@Import({
  InsightBrainConfiguration.class,
  TestDatabaseConfiguration.class,
  NamedBeanRegistrationConfiguration.class
})
public class SpringTestInsightBrainService
    implements TestInsightBrainService
{
  private static final Logger log = LoggerFactory.getLogger(SpringTestInsightBrainService.class);

  private static final String FORK_ID = System.getProperty("test.forkId", "");

  private ConfigurableApplicationContext applicationContext;

  private Configurator configurator;

  private File workDir = new File("target/test-brain-work" + FORK_ID + "-" + UUID.randomUUID());

  private int testPort = 0;

  private int testAdminPort = 0;

  private String testKeystore;

  private String testKeystorePassword;

  private String testHdsUrl;

  private ProxyServerConfiguration testProxyServerConfiguration;

  private LicenseDataUpdater savedLicenseDataUpdater;

  private InsightConfig insightConfig;

  private BiConsumer<ServletRequest, ServletResponse> restRequestFilterHandler;

  private final List<Class<?>> testConfigurations = new ArrayList<>();

  @Override
  public void setHttpPort(int port) {
    this.testPort = port;
  }

  @Override
  public void setHttpAdminPort(int port) {
    this.testAdminPort = port;
  }

  public void setKeyStore(String path, String password) {
    this.testKeystore = path;
    this.testKeystorePassword = password;
  }

  @Override
  public void setHdsUrl(String hdsUrl) {
    this.testHdsUrl = hdsUrl;
  }

  @Override
  public void setDatabaseContainer(DatabaseContainer databaseContainer) {
    // Store in holder for TestDatabaseConfiguration to access during Spring context startup
    TestDatabaseContainerHolder.set(databaseContainer);
  }

  @Override
  public ProxyServerConfiguration getTestProxyServerConfiguration() {
    return testProxyServerConfiguration;
  }

  @Override
  public void setProxyServerConfiguration(String host, int port, String user, String pass) {
    testProxyServerConfiguration = new ProxyServerConfiguration();
    testProxyServerConfiguration.setHostname(host);
    testProxyServerConfiguration.setPort(port);
    testProxyServerConfiguration.setUsername(user);
    testProxyServerConfiguration.setPassword(
        new PasswordHandler(new TestEncryptionKeyStore()).encryptPassword(
            pass.toCharArray()));
  }

  @Override
  public void clearProxyServerConfiguration() {
    testProxyServerConfiguration = null;
  }

  @Override
  public void setConfigurator(Configurator configurator) {
    this.configurator = configurator;
  }

  public SpringTestInsightBrainService setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getWorkDir() {
    return workDir;
  }

  @Override
  public HttpClientUtils.Configuration getClientConfiguration() {
    HttpClientUtils.Configuration configuration = new HttpClientUtils.Configuration();
    configuration.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    String protocol = testKeystore != null ? "https" : "http";
    String contextPath = resolveServerContextPath();
    String basePath = "/".equals(contextPath) ? "" : contextPath;
    configuration.setServerUrl(protocol + "://localhost:" + testPort + basePath + "/");
    configuration.setServerAdminUrl("http://localhost:" + testAdminPort);
    return configuration;
  }

  @Override
  public void start() throws Exception {
    if (applicationContext != null) {
      throw new IllegalStateException("Brain server already started");
    }

    // The brain server will set up a license updater on startup
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();

    // This reduces the test execution time for this module by ~30%.
    PasswordService.useWeakHashIterationForTestsOnly();

    // Clean work directory
    initWorkDirectory(workDir);

    // Build Spring application
    SpringApplicationBuilder builder = new SpringApplicationBuilder(SpringTestInsightBrainService.class);
    String configPath = resolveConfigPath();
    DropwizardConfigBootstrap.configure(builder, configPath);
    builder.listeners(runtimeOverrideListener());

    List<Class<?>> explicitTestConfigurationSources = new ArrayList<>();
    explicitTestConfigurationSources.add(TestDatabaseConfiguration.class);
    explicitTestConfigurationSources.addAll(testConfigurations);
    builder.sources(explicitTestConfigurationSources.toArray(new Class<?>[0]));
    builder.initializers(ExplicitTestConfigurationSupport.initializer(explicitTestConfigurationSources));
    builder.initializers(configuratorInitializer());

    builder.profiles("test");
    // Required: test @Configuration classes intentionally override production beans (e.g.
    // TestDatabaseConfiguration replaces DatabaseConfiguration, mock beans replace real ones)
    builder.properties("spring.main.allow-bean-definition-overriding=true");

    applicationContext = builder.run();

    testPort = applicationContext.getEnvironment().getProperty("local.server.port", Integer.class, testPort);
    testAdminPort = applicationContext.getEnvironment()
        .getProperty("local.management.port", Integer.class, testAdminPort);

    // Get InsightConfig from context
    insightConfig = applicationContext.getBean(InsightConfig.class);

    // Apply HDS URL if set
    if (testHdsUrl != null) {
      setHdsUrlInConfig();
    }

    // Disable background tasks for testing
    disableForTesting();

    log.info("Spring test brain started on port {}", testPort);
  }

  private ApplicationListener<ApplicationEnvironmentPreparedEvent> runtimeOverrideListener() {
    return event -> {
      // The functional test harness allocates ports dynamically. Those runtime values must win over any
      // connector ports translated from Dropwizard config-test.yml, otherwise the reverse proxy targets the
      // wrong port and UI requests fail with connection-refused errors.
      Map<String, Object> overrides = new LinkedHashMap<>();
      overrides.put("server.port", testPort);
      overrides.put("server.servlet.context-path", resolveServerContextPath());
      overrides.put("management.server.port", testAdminPort);
      overrides.put("sonatypeWork", workDir.getAbsolutePath());
      addSearchTypeOverrideIfNeeded(overrides);
      if (testHdsUrl != null) {
        overrides.put("hds.url", testHdsUrl);
      }
      // When testKeystore is set, explicitly configure SSL via overrides so the correct
      // keystore is used regardless of any stale system properties.
      // When testKeystore is null, we do NOT force server.ssl.enabled=false because some
      // tests (e.g. AntiCsrfFilterTest) intentionally set server.ssl.* system properties
      // before server start. Those tests are responsible for cleaning up their own properties.
      if (testKeystore != null) {
        overrides.put("server.ssl.key-store", testKeystore);
        overrides.put("server.ssl.key-store-password", testKeystorePassword);
        overrides.put("server.ssl.key-store-type", "JKS");
        overrides.put("server.ssl.enabled", true);
      }
      event.getEnvironment()
          .getPropertySources()
          .addFirst(new MapPropertySource("springTestRuntimeOverrides", overrides));
    };
  }

  private void addSearchTypeOverrideIfNeeded(final Map<String, Object> overrides) {
    if (configurator == null) {
      return;
    }

    try {
      InsightConfig probeConfig = new InsightConfig();
      configurator.configure(probeConfig);
      if (probeConfig.getSearchConfig() != null) {
        overrides.put("search.type", "test");
      }
    }
    catch (RuntimeException e) {
      log.debug("Unable to probe test configurator for search.type override", e);
    }
  }

  private String resolveServerContextPath() {
    String contextPath = System.getProperty("iq.contextPath", "/").trim();
    if (contextPath.isEmpty() || "/".equals(contextPath)) {
      return "/";
    }
    String normalizedContextPath = contextPath.startsWith("/") ? contextPath : "/" + contextPath;
    return normalizedContextPath.endsWith("/")
        ? normalizedContextPath.substring(0, normalizedContextPath.length() - 1)
        : normalizedContextPath;
  }

  private String resolveConfigPath() {
    String configPath = configurator != null ? configurator.getConfigFilePath() : DEFAULT_CONFIG_FILE_PATH;
    File configFile = new File(configPath);
    if (configFile.exists()) {
      log.info("Using config file: {}", configPath);
    }
    return configPath;
  }

  private org.springframework.context.ApplicationContextInitializer<ConfigurableApplicationContext> configuratorInitializer() {
    return context -> context.addBeanFactoryPostProcessor(beanFactory -> beanFactory.addBeanPostProcessor(
        new BeanPostProcessor()
        {
          @Override
          public Object postProcessBeforeInitialization(Object bean, String beanName) {
            if (bean instanceof InsightConfig insightConfig) {
              insightConfig.setSonatypeWork(workDir.getAbsolutePath());
              if (configurator != null) {
                configurator.configure(insightConfig);
              }
              insightConfig.setAuditLogFilename(resolveAuditLogFile().getAbsolutePath());
              if (testHdsUrl != null) {
                insightConfig.setHdsUrl(testHdsUrl);
              }
            }
            return bean;
          }
        }));
  }

  private void setHdsUrlInConfig() {
    try {
      ApiConfigurationService configurationService = applicationContext.getBean(ApiConfigurationService.class);
      configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL, testHdsUrl);
      configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
    }
    catch (Exception e) {
      log.warn("Could not set HDS URL: {}", e.getMessage());
    }
  }

  private void initWorkDirectory(File workDir) throws Exception {
    if (workDir.exists()) {
      FileCleaner fileCleaner = new FileCleaner();
      File[] files = workDir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (!file.getName().equals("lock")) {
            fileCleaner.delete(file);
          }
        }
      }
    }
    else {
      workDir.mkdirs();
    }

    File auditLogDirectory = resolveAuditLogDirectory();
    if (!auditLogDirectory.exists() && !auditLogDirectory.mkdirs()) {
      throw new IllegalStateException("Could not create audit log directory: " + auditLogDirectory);
    }
  }

  private File resolveAuditLogDirectory() {
    return new File(workDir, "logs");
  }

  private File resolveAuditLogFile() {
    return new File(resolveAuditLogDirectory(), "audit.log");
  }

  // Keep in sync with classes that have a `disableForTesting` field:
  // grep -r 'disableForTesting' insight-brain-service/src/main/java
  @Override
  public void disableForTesting() {
    try {
      getInstance(TaskScheduler.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of TaskScheduler: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling TaskScheduler: {}", e.getMessage());
    }
    try {
      getInstance(PolicyMonitorScheduler.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PolicyMonitorScheduler: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PolicyMonitorScheduler: {}", e.getMessage());
    }
    try {
      getInstance(SuccessMetricsPurger.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of SuccessMetricsPurger: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling SuccessMetricsPurger: {}", e.getMessage());
    }
    try {
      getInstance(ReportPurger.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of ReportPurger: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling ReportPurger: {}", e.getMessage());
    }
    try {
      getInstance(PullRequestPollingScheduler.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PullRequestPollingScheduler: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PullRequestPollingScheduler: {}", e.getMessage());
    }
    try {
      getInstance(ScanFileCleaner.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of ScanFileCleaner: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling ScanFileCleaner: {}", e.getMessage());
    }
    try {
      getInstance(PolicyEvaluateService.class).disablePollingIntervalForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PolicyEvaluateService polling: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PolicyEvaluateService polling: {}", e.getMessage());
    }
    try {
      getInstance(HdsProductNotificationService.class).disableCacheForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of HdsProductNotificationService cache: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling HdsProductNotificationService cache: {}", e.getMessage());
    }
    try {
      getInstance(ClusterTelemetryTask.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of ClusterTelemetryTask: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling ClusterTelemetryTask: {}", e.getMessage());
    }
    try {
      getInstance(FirewallIgnorePatternUpdater.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of FirewallIgnorePatternUpdater: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling FirewallIgnorePatternUpdater: {}", e.getMessage());
    }
    try {
      getInstance(IndexService.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of IndexService: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling IndexService: {}", e.getMessage());
    }
    try {
      getInstance(PersistedPolicyEvaluationPollingResultCleaner.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PersistedPolicyEvaluationPollingResultCleaner: {}",
          e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PersistedPolicyEvaluationPollingResultCleaner: {}", e.getMessage());
    }
    try {
      getInstance(PersistedScanTicketCleaner.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PersistedScanTicketCleaner: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PersistedScanTicketCleaner: {}", e.getMessage());
    }
    try {
      getInstance(FirewallReleaseIntegrityLicenseListener.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of FirewallReleaseIntegrityLicenseListener: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling FirewallReleaseIntegrityLicenseListener: {}", e.getMessage());
    }
    try {
      getInstance(PullRequestMonitor.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PullRequestMonitor: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PullRequestMonitor: {}", e.getMessage());
    }
    try {
      getInstance(DefaultBranchMonitor.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of DefaultBranchMonitor: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling DefaultBranchMonitor: {}", e.getMessage());
    }
    try {
      getInstance(SourceControlEventOrchestrator.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of SourceControlEventOrchestrator: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling SourceControlEventOrchestrator: {}", e.getMessage());
    }
    try {
      getInstance(PullRequestCommentPurger.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PullRequestCommentPurger: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PullRequestCommentPurger: {}", e.getMessage());
    }
    try {
      getInstance(AutomaticQuarantineReleaseScheduler.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of AutomaticQuarantineReleaseScheduler: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling AutomaticQuarantineReleaseScheduler: {}", e.getMessage());
    }
    try {
      getInstance(WaivedComponentUpgradeScheduler.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of WaivedComponentUpgradeScheduler: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling WaivedComponentUpgradeScheduler: {}", e.getMessage());
    }
    try {
      getInstance(ApplicationCountHistoryKeeper.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of ApplicationCountHistoryKeeper: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling ApplicationCountHistoryKeeper: {}", e.getMessage());
    }
    try {
      getInstance(FirewallMetricsComponentsAutoReleasedConsolidatorCronJob.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of FirewallMetricsComponentsAutoReleasedConsolidatorCronJob: {}",
          e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling FirewallMetricsComponentsAutoReleasedConsolidatorCronJob: {}",
          e.getMessage());
    }
    try {
      getInstance(FirewallMetricsComponentWaivedConsolidatorCronJob.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of FirewallMetricsComponentWaivedConsolidatorCronJob: {}",
          e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling FirewallMetricsComponentWaivedConsolidatorCronJob: {}", e.getMessage());
    }
    try {
      getInstance(FirewallMetricsComponentQuarantinedConsolidatorCronJob.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of FirewallMetricsComponentQuarantinedConsolidatorCronJob: {}",
          e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling FirewallMetricsComponentQuarantinedConsolidatorCronJob: {}", e.getMessage());
    }
    try {
      getInstance(DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob: {}",
          e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling DeleteOldFirewallMetricsForSafeComponentsAutoSelectedCronJob: {}",
          e.getMessage());
    }
    try {
      getInstance(SourceControlLoadBalancer.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of SourceControlLoadBalancer: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling SourceControlLoadBalancer: {}", e.getMessage());
    }
    try {
      getInstance(PendingSbomMetadataCleaner.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PendingSbomMetadataCleaner: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PendingSbomMetadataCleaner: {}", e.getMessage());
    }
    try {
      getInstance(HistoricalPolicyViolationTelemetryTask.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of HistoricalPolicyViolationTelemetryTask: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling HistoricalPolicyViolationTelemetryTask: {}", e.getMessage());
    }
    try {
      getInstance(PolicyWaiverTelemetryBackfillTask.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of PolicyWaiverTelemetryBackfillTask: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling PolicyWaiverTelemetryBackfillTask: {}", e.getMessage());
    }
    try {
      getInstance(ReevaluateCascadeRequestCleaner.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of ReevaluateCascadeRequestCleaner: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling ReevaluateCascadeRequestCleaner: {}", e.getMessage());
    }
    try {
      getInstance(EvaluationQueueConsumer.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of EvaluationQueueConsumer: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling EvaluationQueueConsumer: {}", e.getMessage());
    }
    try {
      getInstance(EvaluationQueueProducer.class).disableForTesting = true;
    }
    catch (NullPointerException e) {
      log.debug("Not in context, skipping disable of EvaluationQueueProducer: {}", e.getMessage());
    }
    catch (Exception e) {
      log.warn("Unexpected error disabling EvaluationQueueProducer: {}", e.getMessage());
    }
  }

  @Override
  public void stop() throws Exception {
    if (applicationContext != null) {
      applicationContext.close();
      applicationContext = null;
      LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
    }
    // Clear the holder to prevent memory leaks
    TestDatabaseContainerHolder.clear();
  }

  @Override
  public InsightConfig getConfiguration() {
    return insightConfig;
  }

  @Override
  public void addTestConfigurations(List<Class<?>> testConfigurations) {
    this.testConfigurations.clear();
    if (testConfigurations != null) {
      this.testConfigurations.addAll(testConfigurations);
    }
  }

  @Override
  public ApplicationContext getApplicationContext() {
    return applicationContext;
  }

  @Override
  public <C> C getInstance(Class<C> type) {
    if (applicationContext == null) {
      return null;
    }
    try {
      return applicationContext.getBean(type);
    }
    catch (Exception e) {
      log.debug("Could not get bean of type {}: {}", type.getName(), e.getMessage());
      return null;
    }
  }

  @Override
  public boolean isInitialized() {
    return applicationContext != null && applicationContext.isActive();
  }

  /**
   * Provide a handler to install a RestRequestFilter handler or <i>null</i> to disable.
   */
  public void setRestRequestFilterHandler(BiConsumer<ServletRequest, ServletResponse> restRequestFilterHandler) {
    this.restRequestFilterHandler = restRequestFilterHandler;
  }

  public BiConsumer<ServletRequest, ServletResponse> getRestRequestFilterHandler() {
    return restRequestFilterHandler != null ? restRequestFilterHandler : (a, b) -> {
    };
  }
}
