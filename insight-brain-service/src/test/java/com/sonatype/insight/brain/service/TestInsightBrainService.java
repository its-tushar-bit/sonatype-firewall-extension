/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.io.File;
import java.lang.reflect.Field;
import java.util.Iterator;

import com.sonatype.insight.brain.common.io.FileCleaner;
import com.sonatype.insight.brain.dataaccess.license.LicenseDataUpdater;
import com.sonatype.insight.brain.policy.evaluator.PolicyMonitorScheduler;
import com.sonatype.insight.brain.security.CLMRealm;
import com.sonatype.insight.client.utils.HttpClientUtils.Configuration;
import com.sonatype.insight.db.DatabaseConfig;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.google.common.base.Optional;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.config.HttpConfiguration;
import com.yammer.dropwizard.config.SslConfiguration;
import com.yammer.dropwizard.jetty.AsyncRequestLog;
import com.yammer.dropwizard.lifecycle.ServerLifecycleListener;
import com.yammer.dropwizard.logging.AsyncAppender;
import com.yammer.dropwizard.util.Duration;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.RequestLog;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.sisu.space.BeanScanning;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestInsightBrainService
    extends InsightBrainService
{
  private static final Logger log = LoggerFactory.getLogger(TestInsightBrainService.class);

  private int testPort;

  private int testAdminPort;

  private String testKeystore;

  private String testKeystorePassword;

  private String testSaasAddress;

  private String testBaseUrl;

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

  public void setSaasAddress(final String saasAddress) {
    testSaasAddress = saasAddress;
  }

  public void setBaseUrl(final String baseUrl) {
    this.testBaseUrl = baseUrl;
  }

  public void setProxyConfig(final String host, final int port, final String user, final String pass) {
    testProxyConfig = new ProxyConfig();
    testProxyConfig.setHostname(host);
    testProxyConfig.setPort(port);
    testProxyConfig.setUsername(user);
    testProxyConfig.setPassword(pass);
  }

  public File getWorkDir() {
    return new File("target/test-brain-work");
  }

  public Configuration getClientConfiguration() {
    final Configuration configuration = new Configuration();
    String protocol = "http";
    if (testKeystore != null) {
      protocol = "https";
    }
    String adminProtocol = "http";
    if (testAdminPort == testPort) {
      adminProtocol = protocol;
    }
    configuration.setServerUrl(protocol + "://localhost:" + testPort);
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
    log.debug("Excluding {} from test CLM server", name);
    return false;
  }

  public void start() throws Exception {
    if (testBrainServer != null) {
      throw new IllegalStateException("Brain server already started");
    }

    // The brain server will set up a license updater on startup
    savedLicenseDataUpdater = LicenseDataUpdater.getUpdater();

    // This reduces the test execution time for this module by ~30%.
    CLMRealm.useWeakHashIterationForTestsOnly();

    String[] args;
    File dropWizardConfigFile = new File("target/test-classes/config-test.yml");
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
  public void run(final InsightConfig config, final Environment env) throws Exception {
    config.getHttpConfiguration().setPort(testPort);
    config.getHttpConfiguration().setAdminPort(testAdminPort);
    // Don't wait 2 seconds after each brain service test, 1 millisecond seems to be enough
    config.getHttpConfiguration().setShutdownGracePeriod(Duration.milliseconds(1));
    if (testKeystore != null) {
      final SslConfiguration sslConfiguration = new SslConfiguration();
      sslConfiguration.setKeyStore(Optional.of(new File(testKeystore).getAbsoluteFile()));
      sslConfiguration.setKeyStorePassword(Optional.of(testKeystorePassword));

      config.getHttpConfiguration().setConnectorType(HttpConfiguration.ConnectorType.NONBLOCKING_SSL);
      config.getHttpConfiguration().setSslConfiguration(sslConfiguration);
    }
    config.setSonatypeWork(getWorkDir().getPath());
    if (testSaasAddress != null) {
      config.setSaasAddress(testSaasAddress);
    }
    config.setBaseUrl(testBaseUrl);

    if (testProxyConfig != null) {
      config.setProxyConfig(testProxyConfig);
    }
    insightConfig = config;

    new FileCleaner().delete(config.getSonatypeWork());

    env.addServerLifecycleListener(new ServerLifecycleListener()
    {
      @Override
      public void serverStarted(final Server server) {
        testBrainServer = server;
      }
    });

    SlowMoFilter.configure(env);

    super.run(config, env);

    getInjector().getInstance(PolicyMonitorScheduler.class).disableForTesting = true;
  }

  public void stop() throws Exception {
    if (testBrainServer != null) {
      testBrainServer.stop();
      // Dropwizard starts some threads for logging. These threads are blocked on internal blocking queues when there is
      // nothing to log and they are not stopped when the server is stopped.
      // This is a problem only in our test code, because we start/stop a new server for each test and these threads
      // accumulate to a point where the JVM cannot create more native threads, resulting in
      // "java.lang.OutOfMemoryError: unable to create new native thread" exceptions.
      stopDropwizardLoggingAsyncAppenders();
      stopDropwizardLoggingAsyncRequestLogs(testBrainServer);
      testBrainServer = null;

      LicenseDataUpdater.setUpdater(savedLicenseDataUpdater);
    }
  }

  private void stopDropwizardLoggingAsyncAppenders() {
    ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    Iterator<Appender<ILoggingEvent>> logAppenderIter = rootLogger.iteratorForAppenders();
    while (logAppenderIter.hasNext()) {
      Appender<ILoggingEvent> logAppender = logAppenderIter.next();
      if (!(logAppender instanceof AsyncAppender)) {
        continue;
      }

      AsyncAppender dropwizardAsyncAppender = (AsyncAppender) logAppender;
      // This doesn't stop the dispatcher thread used by the AsyncAppender, it only marks the AsyncAppender as stopped.
      dropwizardAsyncAppender.stop();
      // The AsyncAppender dispatcher thread is blocked trying to take an element from its queue. Interrupt the
      // dispatcher thread.
      try {
        Field dispatcherField = AsyncAppender.class.getDeclaredField("dispatcher");
        dispatcherField.setAccessible(true);
        Thread dispatcherThread = (Thread) dispatcherField.get(dropwizardAsyncAppender);
        dispatcherThread.interrupt();
      }
      catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void stopDropwizardLoggingAsyncRequestLogs(Server server) {
    HandlerCollection handlerCollection = (HandlerCollection) server.getHandler();
    Handler[] handlers = handlerCollection.getHandlers();
    for (Handler handler : handlers) {
      if (!(handler instanceof RequestLogHandler)) {
        continue;
      }

      RequestLogHandler requestLogHandler = (RequestLogHandler) handler;
      RequestLog requestLog = requestLogHandler.getRequestLog();
      if (!(requestLog instanceof AsyncRequestLog)) {
        continue;
      }

      AsyncRequestLog asyncRequestLog = (AsyncRequestLog) requestLog;
      // The AsyncRequestLog dispatcher thread is blocked trying to take an element from its queue. Interrupt the
      // dispatcher thread.
      try {
        Field dispatchThreadField = AsyncRequestLog.class.getDeclaredField("dispatchThread");
        dispatchThreadField.setAccessible(true);
        Thread dispatchThread = (Thread) dispatchThreadField.get(asyncRequestLog);
        dispatchThread.interrupt();
      }
      catch (NoSuchFieldException e) {
        throw new RuntimeException(e);
      }
      catch (IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  @Override
  protected DatabaseConfig getDatabaseConfig(File databaseDir, String databaseName) {
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

  public InsightConfig getConfiguration() {
    return insightConfig;
  }
}
