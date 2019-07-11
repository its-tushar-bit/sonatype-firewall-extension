/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.migration.ScanFileCleaner;
import com.sonatype.insight.brain.policy.evaluator.PolicyEvaluateService;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.security.InternalRealm;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.client.utils.SimpleAuthentication;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.test.SslProperties;

import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.ConfigurationFactoryFactory;
import io.dropwizard.configuration.ConfigurationSourceProvider;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import io.dropwizard.server.DefaultServerFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.sisu.space.BeanScanning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestInsightBrainService
    extends InsightBrainService
{
  static {
    SslProperties.use();
  }

  private static final Logger log = LoggerFactory.getLogger(TestInsightBrainService.class);
  
  private static final String DEFAULT_CONFIG_FILE_PATH = "target/test-classes/config-test.yml";

  public interface Configurator
  {
    void configure(InsightConfig config);

    default String getConfigFilePath() {
      return DEFAULT_CONFIG_FILE_PATH;
    }
  }

  private Configurator configurator;

  private File workDir = new File("target/test-brain-work");

  private int testPort;

  private int testAdminPort;

  private String testKeystore;

  private String testKeystorePassword;

  private String testHdsUrl;

  private ProxyConfig testProxyConfig;

  private Server testBrainServer;

  private LicenseDataUpdater savedLicenseDataUpdater;

  private InsightConfig insightConfig;

  public void setHttpPort(final int port) {
    testPort = port;
  }

  public void setHttpAdminPort(final int port) {
    testAdminPort = port;
  }

  public void setKeyStore(final String path, final String password) {
    testKeystore = path;
    testKeystorePassword = password;
  }

  public void setHdsUrl(final String hdsUrl) {
    testHdsUrl = hdsUrl;
  }

  public void setProxyConfig(final String host, final int port, final String user, final String pass) {
    testProxyConfig = new ProxyConfig();
    testProxyConfig.setHostname(host);
    testProxyConfig.setPort(port);
    testProxyConfig.setUsername(user);
    testProxyConfig.setPassword(pass);
  }

  public void setConfigurator(Configurator configurator) {
    this.configurator = configurator;
  }

  public TestInsightBrainService setWorkDir(File workDir) {
    this.workDir = workDir;
    return this;
  }

  public File getWorkDir() {
    return workDir;
  }

  public Configuration getClientConfiguration() {
    final Configuration configuration = new Configuration();
    configuration.setServerAuth(SimpleAuthentication.parse("admin:admin123"));
    String protocol = "http";
    if (testKeystore != null || ((DefaultServerFactory) insightConfig.getServerFactory()).getApplicationConnectors()
        .get(0) instanceof HttpsConnectorFactory) {
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
  protected BeanScanning scanning(InsightConfig configuration) {
    return BeanScanning.CACHE;
  }

  @Override
  protected boolean acceptComponent(Class<?> type) {
    if (!super.acceptComponent(type)) {
      return false;
    }
    // the test classpath can be messy when used in Hudson/Nexus/etc., so let's be a little defensive
    String name = type.getName();
    if (name.startsWith("com.sonatype.insight.") || name.startsWith("com.sonatype.clm.")) {
      return true;
    }
    if (name.startsWith("org.sonatype.licensing.") || name.startsWith("codeguard.licensing.")) {
      return true;
    }
    if (name.startsWith("org.sonatype.micromailer.")) {
      return true;
    }
    log.debug("Excluding {} from test Brain server", name);
    return false;
  }

  public void start() throws Exception {
    if (testBrainServer != null) {
      throw new IllegalStateException("Brain server already started");
    }

    // The brain server will set up a license updater on startup
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();

    // This reduces the test execution time for this module by ~30%.
    InternalRealm.useWeakHashIterationForTestsOnly();

    String[] args;
    File dropWizardConfigFile = new File(
        configurator == null ? DEFAULT_CONFIG_FILE_PATH : configurator.getConfigFilePath());
    if (dropWizardConfigFile.exists()) {
      // I have no idea why, but INFO level is not enabled at this point
      log.warn("Using DropWizard config file {}", dropWizardConfigFile.getAbsolutePath());
      args = new String[] { "server", dropWizardConfigFile.getAbsolutePath() };
    }
    else {
      log.warn("Cannot find DropWizard config file {}", dropWizardConfigFile.getAbsolutePath());
      args = new String[] { "server" };
    }

    TestInsightBrainService.this.run(args);
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
        public InsightConfig build(ConfigurationSourceProvider provider, String path)
            throws IOException, ConfigurationException
        {
          return augment(configurationFactory.build(provider, path));
        }

        @Override
        public InsightConfig build() throws IOException, ConfigurationException {
          return augment(configurationFactory.build());
        }

        private InsightConfig augment(InsightConfig config) {
          config.setSonatypeWork(getWorkDir().getPath());
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
    DefaultServerFactory defaultServerFactory = (DefaultServerFactory) config.getServerFactory();
    if (testKeystore != null) {
      HttpsConnectorFactory applicationHttpsConnector = new HttpsConnectorFactory();
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
    if (testHdsUrl != null) {
      config.setHdsUrl(testHdsUrl);
    }

    if (testProxyConfig != null) {
      config.setProxyConfig(testProxyConfig);
    }
    insightConfig = config;

    initWorkDirectory(config.getSonatypeWork());

    env.lifecycle().addServerLifecycleListener(new ServerLifecycleListener()
    {
      @Override
      public void serverStarted(final Server server) {
        testBrainServer = server;
      }
    });

    SlowMoFilter.configure(env);

    super.run(config, env);

    getInstance(PolicyMonitorScheduler.class).disableForTesting = true;
    getInstance(ScanFileCleaner.class).disableForTesting = true;
    getInstance(PolicyEvaluateService.class).disablePollingIntervalForTesting = true;
  }

  public void stop() throws Exception {
    if (testBrainServer != null) {
      testBrainServer.stop();
      testBrainServer = null;

      LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
    }
  }

  @Override
  protected DatabaseConfig getDatabaseConfig(DatabaseConfigProvider databaseConfigProvider, DatabaseName databaseName) {
    // Use in memory db
    return null;
  }

  public File getAuditDir(String applicationId) {
    return new File(new File(getWorkDir(), "audit"), applicationId);
  }

  public File getDataDir() {
    return new File(getWorkDir(), "data");
  }

  public File getReportDir(String applicationId, String scanId) {
    return new File(new File(new File(getWorkDir(), "report"), applicationId), scanId);
  }

  public File getOrganizationIconDir() {
    return new File(getDataDir(),"organization");
  }

  public File getApplicationIconDir() {
    return new File(getDataDir(),"application");
  }

  public InsightConfig getConfiguration() {
    return insightConfig;
  }

  private void initWorkDirectory(File workDir) throws Exception {
    FileCleaner fileCleaner = new FileCleaner();
    // lock has already been created by this point so ignore it
    for (File file : workDir.listFiles()) {
      if (!file.getName().equals("lock")) {
        fileCleaner.delete(file);
      }
    }
  }
}
