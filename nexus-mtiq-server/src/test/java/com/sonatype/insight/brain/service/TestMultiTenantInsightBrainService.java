/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.stream.Stream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import com.sonatype.insight.brain.api.v2.service.ApiConfigurationService;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.TenantSizeMetricsJob;
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
import com.sonatype.insight.brain.product.license.FirewallReleaseIntegrityLicenseListener;
import com.sonatype.insight.brain.product.notifications.HdsProductNotificationService;
import com.sonatype.insight.brain.report.ReportPurger;
import com.sonatype.insight.brain.repository.ReevaluateCascadeRequestCleaner;
import com.sonatype.insight.brain.repository.autorelease.AutomaticQuarantineReleaseScheduler;
import com.sonatype.insight.brain.sbom.PendingSbomMetadataCleaner;
import com.sonatype.insight.brain.scan.PersistedScanTicketCleaner;
import com.sonatype.insight.brain.scheduler.MultiTenantTaskScheduler;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.index.IndexService;
import com.sonatype.insight.brain.security.PasswordHandler;
import com.sonatype.insight.brain.security.PasswordService;
import com.sonatype.insight.brain.security.TestMultiTenantEncryptionKeyStore;
import com.sonatype.insight.brain.sourcecontrol.SourceControlLoadBalancer;
import com.sonatype.insight.brain.successmetrics.SuccessMetricsPurger;
import com.sonatype.insight.brain.telemetry.ClusterTelemetryTask;
import com.sonatype.insight.brain.telemetry.HistoricalPolicyViolationTelemetryTask;
import com.sonatype.insight.brain.telemetry.PolicyWaiverTelemetryBackfillTask;
import com.sonatype.insight.brain.tenancy.TenantUtil;
import com.sonatype.insight.client.utils.HttpClientUtils;
import com.sonatype.insight.client.utils.SimpleAuthentication;

import com.google.inject.Module;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.core.server.DefaultServerFactory;
import io.dropwizard.core.setup.Bootstrap;
import io.dropwizard.core.setup.Environment;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.util.Duration;
import org.eclipse.jetty.server.Server;

public class TestMultiTenantInsightBrainService
    extends MultiTenantInsightBrainService
    implements TestInsightBrainService
{
  private static final String FORK_ID = System.getProperty("test.forkId", "");

  private Configurator configurator;

  private File workDir = new File("target/test-brain-work" + FORK_ID);

  private int testPort;

  private int testAdminPort;

  private String testKeystore;

  private String testKeystorePassword;

  private String testHdsUrl;

  private ProxyServerConfiguration testProxyServerConfiguration;

  private Server testBrainServer;

  private LicenseDataUpdater savedLicenseDataUpdater;

  private InsightConfig insightConfig;

  private BiConsumer<ServletRequest, ServletResponse> restRequestFilterHandler;

  private final List<Module> overrideModules = new ArrayList<>();

  @Override
  public void setHttpPort(final int port) {
    testPort = port;
  }

  @Override
  public void setHttpAdminPort(final int port) {
    testAdminPort = port;
  }

  public void setKeyStore(final String path, final String password) {
    testKeystore = path;
    testKeystorePassword = password;
  }

  @Override
  public void setHdsUrl(final String hdsUrl) {
    testHdsUrl = hdsUrl;
  }

  @Override
  public void setDatabaseContainer(final DatabaseContainer databaseContainer) {
    this.databaseContainer = databaseContainer;
  }

  @Override
  public ProxyServerConfiguration getTestProxyServerConfiguration() {
    return testProxyServerConfiguration;
  }

  @Override
  public void setProxyServerConfiguration(final String host, final int port, final String user, final String pass) {
    testProxyServerConfiguration = new ProxyServerConfiguration();
    testProxyServerConfiguration.setHostname(host);
    testProxyServerConfiguration.setPort(port);
    testProxyServerConfiguration.setUsername(user);
    testProxyServerConfiguration
        .setPassword(
            new PasswordHandler(new TestMultiTenantEncryptionKeyStore()).encryptPassword(
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

  public TestMultiTenantInsightBrainService setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getWorkDir() {
    return workDir;
  }

  public File getClusterDir() {
    return new File(workDir, "cluster");
  }

  @Override
  public HttpClientUtils.Configuration getClientConfiguration() {
    final HttpClientUtils.Configuration configuration = new HttpClientUtils.Configuration();
    configuration.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    String protocol = "http";
    if (testKeystore != null || ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationConnectors()
        .get(0) instanceof HttpsConnectorFactory)
    {
      protocol = "https";
    }
    String contextPath = ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationContextPath();
    String adminProtocol = "http";
    if (testAdminPort == testPort) {
      adminProtocol = protocol;
    }
    configuration.setServerUrl(protocol + "://localhost:" + testPort + contextPath);
    configuration.setServerAdminUrl(adminProtocol + "://localhost:" + testAdminPort
        + (testAdminPort != testPort ? "" : "/admin"));
    return configuration;
  }

  @Override
  public void start() throws Exception {
    if (testBrainServer != null) {
      throw new IllegalStateException("Brain server already started");
    }

    // The brain server will set up a license updater on startup
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();

    // This reduces the test execution time for this module by ~30%.
    PasswordService.useWeakHashIterationForTestsOnly();

    String[] args;
    File dropWizardConfigFile = new File(
        configurator == null ? DEFAULT_CONFIG_FILE_PATH : configurator.getConfigFilePath());
    if (dropWizardConfigFile.exists()) {
      // I have no idea why, but INFO level is not enabled at this point
      log.warn("Using DropWizard config file {}", dropWizardConfigFile.getAbsolutePath());
      args = new String[]{"server", dropWizardConfigFile.getAbsolutePath()};
    }
    else {
      log.warn("Cannot find DropWizard config file {}", dropWizardConfigFile.getAbsolutePath());
      args = new String[]{"server"};
    }

    TestMultiTenantInsightBrainService.this.run(args);
  }

  @Override
  public void initialize(Bootstrap<InsightConfig> bootstrap) {
    super.initialize(bootstrap);

    // wrap the configuration factory to allow customization of configuration before it gets applied
    ConfigurationFactoryFactory<InsightConfig> configurationFactoryFactory = bootstrap.getConfigurationFactoryFactory();
    bootstrap.setConfigurationFactoryFactory((type, validator, objectMapper, propertyPrefix) -> {
      ConfigurationFactory<InsightConfig> configurationFactory = configurationFactoryFactory.create(type, validator,
          objectMapper, propertyPrefix);
      return new ConfigurationFactory<InsightConfig>()
      {
        @Override
        public InsightConfig build(
            ConfigurationSourceProvider provider,
            String path) throws IOException, ConfigurationException
        {
          return augment(configurationFactory.build(provider, path));
        }

        @Override
        public InsightConfig build() throws IOException, ConfigurationException {
          return augment(configurationFactory.build());
        }

        private InsightConfig augment(InsightConfig config) {
          config.setSonatypeWork(getWorkDir().getPath());
          config.setClusterDirectory(getClusterDir().getPath());

          if (configurator != null) {
            configurator.configure(config);
          }
          return config;
        }
      };
    });
  }

  @Override
  public void run(final InsightConfig config, final Environment env) throws Exception {
    new TenantUtil().setGlobalTenant();

    DefaultServerFactory defaultServerFactory = (DefaultServerFactory) config.getServerFactory();
    defaultServerFactory.setIdleThreadTimeout(Duration.seconds(1));
    if (testKeystore != null) {
      HttpsConnectorFactory applicationHttpsConnector = new HttpsConnectorFactory();
      applicationHttpsConnector.setUseForwardedHeaders(true);
      applicationHttpsConnector.setKeyStorePath(new File(testKeystore).getAbsolutePath());
      applicationHttpsConnector.setKeyStorePassword(testKeystorePassword);
      defaultServerFactory.setApplicationConnectors(Collections.singletonList(applicationHttpsConnector));
    }
    HttpConnectorFactory applicationConnector = (HttpConnectorFactory) defaultServerFactory.getApplicationConnectors()
        .get(0);
    applicationConnector.setPort(testPort);
    HttpConnectorFactory adminConnector = (HttpConnectorFactory) defaultServerFactory.getAdminConnectors().get(0);
    adminConnector.setPort(testAdminPort);
    // disable graceful shutdown, i.e. don't waste time waiting nor risk timeout errors
    defaultServerFactory.setShutdownGracePeriod(Duration.milliseconds(0));

    insightConfig = config;

    initWorkDirectory(getWorkDir());

    env.lifecycle().addServerLifecycleListener(server -> {
      testBrainServer = server;
      getInstance(ApiConfigurationService.class).applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
    });

    SlowMoFilter.configure(env);

    RestRequestFilter.configure(env, (request, response) -> getRestRequestFilterHandler().accept(request, response));

    // Note: beans have already been constructed at this point, so we need to not only set the HDS url in the config
    // but also update the HdsClients
    setHdsUrl(config);

    super.run(config, env);

    disableForTesting();

    getInstance(DefaultApplicationLifecycle.class).boot();
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    // If no DatabaseContainer was pre-configured then create the default one
    if (databaseContainer == null) {
      databaseContainer = super.createDatabaseContainer(insightConfig);
    }
    return databaseContainer;
  }

  /**
   * Provide a handler to install a RestRequestFilter handler or <i>null</i> to disable.
   */
  public void setRestRequestFilterHandler(final BiConsumer<ServletRequest, ServletResponse> restRequestFilterHandler) {
    this.restRequestFilterHandler = restRequestFilterHandler;
  }

  public BiConsumer<ServletRequest, ServletResponse> getRestRequestFilterHandler() {
    return restRequestFilterHandler != null ? restRequestFilterHandler : (a, b) -> {
    };
  }

  @Override
  public void disableForTesting() {
    getInstance(TaskScheduler.class).disableForTesting = true;
    getInstance(MultiTenantTaskScheduler.class).disableForTesting = true;
    getInstance(PolicyMonitorScheduler.class).disableForTesting = true;
    getInstance(SuccessMetricsPurger.class).disableForTesting = true;
    getInstance(ReportPurger.class).disableForTesting = true;
    getInstance(PullRequestPollingScheduler.class).disableForTesting = true;
    getInstance(ScanFileCleaner.class).disableForTesting = true;
    getInstance(PolicyEvaluateService.class).disablePollingIntervalForTesting = true;
    getInstance(HdsProductNotificationService.class).disableCacheForTesting = true;
    getInstance(ClusterTelemetryTask.class).disableForTesting = true;
    getInstance(FirewallIgnorePatternUpdater.class).disableForTesting = true;
    getInstance(IndexService.class).disableForTesting = true;
    getInstance(PersistedPolicyEvaluationPollingResultCleaner.class).disableForTesting = true;
    getInstance(PersistedScanTicketCleaner.class).disableForTesting = true;
    getInstance(FirewallReleaseIntegrityLicenseListener.class).disableForTesting = true;
    getInstance(PullRequestMonitor.class).disableForTesting = true;
    getInstance(DefaultBranchMonitor.class).disableForTesting = true;
    getInstance(SourceControlEventOrchestrator.class).disableForTesting = true;
    getInstance(PullRequestCommentPurger.class).disableForTesting = true;
    getInstance(AutomaticQuarantineReleaseScheduler.class).disableForTesting = true;
    getInstance(ApplicationCountHistoryKeeper.class).disableForTesting = true;
    getInstance(SourceControlLoadBalancer.class).disableForTesting = true;
    getInstance(PendingSbomMetadataCleaner.class).disableForTesting = true;
    getInstance(TenantSizeMetricsJob.class).disableForTesting = true;
    getInstance(HistoricalPolicyViolationTelemetryTask.class).disableForTesting = true;
    getInstance(PolicyWaiverTelemetryBackfillTask.class).disableForTesting = true;
    getInstance(ReevaluateCascadeRequestCleaner.class).disableForTesting = true;
  }

  @Override
  void bootApplicationLifecycle() {
    // noop
  }

  @Override
  public void stop() throws Exception {
    if (testBrainServer != null) {
      testBrainServer.stop();
      testBrainServer = null;

      LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
    }
  }

  @Override
  public InsightConfig getConfiguration() {
    return insightConfig;
  }

  @Override
  public void addOverrideModules(final List<Module> overrideModules) {
    if (overrideModules != null) {
      this.overrideModules.addAll(overrideModules);
    }
  }

  @Override
  protected List<Module> overrideModules() {
    return new ArrayList<>(overrideModules);
  }

  private void setHdsUrl(InsightConfig config) {
    config.setHdsUrl(testHdsUrl);
    ApiConfigurationService configurationService = getInstance(ApiConfigurationService.class);
    configurationService.setConfigurationInDatabaseNoAuthz(SystemConfigurationProperty.HDS_URL, testHdsUrl);
    configurationService.applyConfigurationToClients(SystemConfigurationProperty.HDS_URL);
  }

  private void initWorkDirectory(File workDir) throws Exception {
    try (Stream<Path> walk = Files.walk(workDir.toPath())) {
      walk.sorted(Comparator.reverseOrder()) // so directories get deleted _after_ their contents

          // the lock file may already exist and be undeletable on Windows, so skip it and its parent directories
          .filter(path -> !path.getFileName().toString().equals("lock"))
          .forEach(path -> {
            try {
              if (!Files.isDirectory(path) || Files.list(path).findAny().isEmpty()) {
                Files.delete(path);
              }
            }
            catch (IOException e) {
              throw new RuntimeException("Failed to delete " + path, e);
            }
          });
    }
  }
}
