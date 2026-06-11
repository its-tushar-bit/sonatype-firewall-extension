/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SSLSocketAppender;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.rolling.RollingFileAppender;
import com.sonatype.insight.brain.service.InsightConfig;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.StandardEnvironment;

public class DropwizardLoggingAppenderConfigurationTest
{
  @Rule
  public TemporaryFolder tempFolder = new TemporaryFolder();

  private LoggerContext loggerContext;

  private String lastConfigYaml;

  private final List<String> cleanupLoggerNames = new ArrayList<>();

  @After
  public void tearDown() {
    if (loggerContext != null) {
      for (String name : cleanupLoggerNames) {
        Logger logger = loggerContext.getLogger(name);
        logger.detachAndStopAllAppenders();
        if (!Logger.ROOT_LOGGER_NAME.equals(name)) {
          logger.setLevel(null);
        }
        logger.setAdditive(true);
      }
    }
  }

  @Test
  public void testRootFileAppender_createsRollingFileAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  level: INFO",
        "  appenders:",
        "    - type: file",
        "      currentLogFilename: " + tempFile("server.log"),
        "      archivedLogFilenamePattern: " + tempFile("server-%d.log.gz"),
        "      archivedFileCount: 30",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);
    assertThat(((FileAppender<?>) appender).getFile()).endsWith("server.log");
  }

  @Test
  public void testRootConsoleAppender_createsConsoleAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: console",
        "      threshold: WARN",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "console");
    assertThat(appender).isInstanceOf(ConsoleAppender.class);
  }

  @Test
  public void testPerLoggerFileAppender_createsAppenderOnNamedLogger() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    com.sonatype.insight.audit:",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit.log"),
        "          archivedLogFilenamePattern: " + tempFile("audit-%d.log.gz"),
        "          archivedFileCount: 50",
        ""));

    initializeAppenders(env);

    Logger auditLogger = loggerContext.getLogger("com.sonatype.insight.audit");
    cleanupLoggerNames.add("com.sonatype.insight.audit");
    assertThat(auditLogger.isAdditive()).isFalse();
    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "file");
    assertThat(appender).isInstanceOf(RollingFileAppender.class);
    assertThat(((FileAppender<?>) appender).getFile()).endsWith("audit.log");
  }

  @Test
  public void testPerLoggerWithLevelAndAppenders_configuresBoth() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    com.example.custom:",
        "      level: DEBUG",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("custom.log"),
        ""));

    assertThat(env.getProperty("logging.level.com.example.custom")).isEqualTo("DEBUG");

    initializeAppenders(env);
    cleanupLoggerNames.add("com.example.custom");

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.example.custom", "file");
    assertThat(appender).isNotNull();
  }

  @Test
  public void testFileAppender_withArchiveFalse_createsPlainFileAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        "      currentLogFilename: " + tempFile("app.log"),
        "      archive: false",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(appender).isInstanceOf(FileAppender.class);
    assertThat(appender).isNotInstanceOf(RollingFileAppender.class);
  }

  @Test
  public void testSyslogAppender_createsSyslogAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: syslog",
        "      host: loghost.example.com",
        "      port: 5140",
        "      facility: LOCAL7",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "syslog");
    assertThat(appender).isInstanceOf(SyslogAppender.class);
    SyslogAppender syslog = (SyslogAppender) appender;
    assertThat(syslog.getSyslogHost()).isEqualTo("loghost.example.com");
    assertThat(syslog.getPort()).isEqualTo(5140);
  }

  @Test
  public void testTcpAppender_createsSocketAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: tcp",
        "      host: logserver.example.com",
        "      port: 4560",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "tcp");
    assertThat(appender).isInstanceOf(SocketAppender.class);
  }

  @Test
  public void testTlsAppender_createsSSLSocketAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: tls",
        "      host: logserver.example.com",
        "      port: 6514",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "tls");
    assertThat(appender).isInstanceOf(SSLSocketAppender.class);
  }

  @Test
  public void testTlsAppender_validateCertsAndValidatePeers_parseButAreIgnored() throws IOException {
    // Pre-Spring Dropwizard accepted these and passed them to Jetty's SslContextFactory; logback's stock SSL
    // appender has no equivalent, so they must still parse under strict conversion (warned as ignored) rather
    // than fail startup.
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: tls",
        "      host: logserver.example.com",
        "      port: 6514",
        "      validateCerts: true",
        "      validatePeers: true",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "tls");
    assertThat(appender).isInstanceOf(SSLSocketAppender.class);
  }

  @Test
  public void testUnsupportedAppenderType_logsWarningAndContinues() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: udp",
        "      host: localhost",
        "    - type: file",
        "      currentLogFilename: " + tempFile("server.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> fileAppender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(fileAppender).isNotNull();
    Appender<ILoggingEvent> udpAppender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "udp");
    assertThat(udpAppender).isNull();
  }

  @Test
  public void testNoLoggingSection_isNoOp() throws IOException {
    StandardEnvironment env = loadConfig("sonatypeWork: ./work\n");

    initializeAppenders(env);
  }

  @Test
  public void testFileAppender_withMissingFilename_isSkipped() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(appender).isNull();
  }

  private StandardEnvironment loadConfig(String yaml) throws IOException {
    lastConfigYaml = yaml;
    File configFile = tempFolder.newFile("config.yml");
    Files.writeString(configFile.toPath(), yaml);

    StandardEnvironment environment = new StandardEnvironment();
    new DropwizardConfigLoader().loadConfig(configFile, environment);
    return environment;
  }

  private void initializeAppenders(StandardEnvironment env) {
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    initializeAppenders(lastConfigYaml);
  }

  private void initializeAppenders(String yaml) {
    if (yaml == null) {
      return;
    }
    DropwizardConfigSourceReader reader = new DropwizardConfigSourceReader();
    try {
      Map<String, Object> configMap =
          reader.readConfigMap(Files.writeString(tempFolder.newFile("init.yml").toPath(), yaml).toFile());
      InsightConfig insightConfig = reader.convertValue(configMap, InsightConfig.class);

      DropwizardLoggingAppenderConfiguration config = new DropwizardLoggingAppenderConfiguration(new ObjectProvider<>()
      {
        @Override
        public Stream<DropwizardLoggingAppenderConfiguration.CustomAppenderFactory> orderedStream() {
          return Stream.empty();
        }
      });
      config.dropwizardLoggingAppenderInitializer(insightConfig).afterSingletonsInstantiated();
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private Appender<ILoggingEvent> findAppenderOnLogger(String loggerName, String typeSubstring) {
    cleanupLoggerNames.add(loggerName);
    Logger logger = loggerContext.getLogger(loggerName);
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      Appender<ILoggingEvent> appender = iter.next();
      if (appender.getName() != null && appender.getName().contains("." + typeSubstring + ".")) {
        if (appender instanceof AsyncAppender asyncAppender) {
          Iterator<Appender<ILoggingEvent>> innerIter = asyncAppender.iteratorForAppenders();
          if (innerIter.hasNext()) {
            return innerIter.next();
          }
        }
        return appender;
      }
    }
    return null;
  }

  @Test
  public void testMultipleRootAppenders_consoleAndFileTogether() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  level: DEBUG",
        "  appenders:",
        "    - type: console",
        "      threshold: INFO",
        "    - type: file",
        "      threshold: ALL",
        "      currentLogFilename: " + tempFile("clm-server.log"),
        "      archivedLogFilenamePattern: " + tempFile("clm-server-%d.log.gz"),
        "      archivedFileCount: 50",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> consoleAppender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "console");
    Appender<ILoggingEvent> fileAppender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(consoleAppender).isInstanceOf(ConsoleAppender.class);
    assertThat(fileAppender).isInstanceOf(RollingFileAppender.class);
  }

  @Test
  public void testRootAppenders_replacesExistingAppenders() throws IOException {
    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
    cleanupLoggerNames.add(Logger.ROOT_LOGGER_NAME);

    ConsoleAppender<ILoggingEvent> existingAppender = new ConsoleAppender<>();
    existingAppender.setName("pre-existing-console");
    existingAppender.setContext(loggerContext);
    existingAppender.start();
    rootLogger.addAppender(existingAppender);

    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: console",
        ""));

    initializeAppenders(env);

    assertThat(rootLogger.getAppender("pre-existing-console")).isNull();
    int appenderCount = countAppenders(rootLogger);
    assertThat(appenderCount).isEqualTo(1);
  }

  @Test
  public void testPerLoggerAppenders_withoutExplicitLevel_inheritsRootLevel() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  level: DEBUG",
        "  loggers:",
        "    com.sonatype.insight.audit:",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit.log"),
        ""));

    assertThat(env.getProperty("logging.level.root")).isEqualTo("DEBUG");
    assertThat(env.getProperty("logging.level.com.sonatype.insight.audit")).isNull();

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "file");
    assertThat(appender).isNotNull();
  }

  @Test
  public void testAppendersAreWrappedInAsyncAppender() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        "      currentLogFilename: " + tempFile("async-test.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> rawAppender = findRawAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(rawAppender).isInstanceOf(AsyncAppender.class);
  }

  @Test
  public void testScalarAndMapLoggersTogether() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  level: INFO",
        "  loggers:",
        "    com.sonatype.insight.scan: DEBUG",
        "    org.apache.http: WARN",
        "    com.sonatype.insight.audit:",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit2.log"),
        ""));

    assertThat(env.getProperty("logging.level.com.sonatype.insight.scan")).isEqualTo("DEBUG");
    assertThat(env.getProperty("logging.level.org.apache.http")).isEqualTo("WARN");

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "file");
    assertThat(appender).isNotNull();
  }

  @Test
  public void testFileAppenderWithThreshold_appliesFilter() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        "      threshold: WARN",
        "      currentLogFilename: " + tempFile("threshold-test.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> fileAppender = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(fileAppender).isNotNull();
    assertThat(fileAppender.getCopyOfAttachedFiltersList()).isNotEmpty();
  }

  @Test
  public void testDefaultConfigStructure_allSectionsHandled() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "sonatypeWork: ./sonatype-work/clm-server",
        "server:",
        "  applicationContextPath: /",
        "  applicationConnectors:",
        "    - type: http",
        "      port: 8070",
        "  adminConnectors:",
        "    - type: http",
        "      port: 8071",
        "      bindHost: 127.0.0.1",
        "  requestLog:",
        "    appenders:",
        "      - type: file",
        "        currentLogFilename: " + tempFile("request.log"),
        "        archivedLogFilenamePattern: " + tempFile("request-%d.log.gz"),
        "        archivedFileCount: 50",
        "logging:",
        "  level: DEBUG",
        "  loggers:",
        "    com.sonatype.insight.scan: INFO",
        "    com.sonatype.insight.audit:",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit3.log"),
        "          archivedLogFilenamePattern: " + tempFile("audit3-%d.log.gz"),
        "          archivedFileCount: 50",
        "  appenders:",
        "    - type: console",
        "      threshold: INFO",
        "    - type: file",
        "      threshold: ALL",
        "      currentLogFilename: " + tempFile("clm-server2.log"),
        "      archivedLogFilenamePattern: " + tempFile("clm-server2-%d.log.gz"),
        "      archivedFileCount: 50",
        "createSampleData: true",
        ""));

    assertThat(env.getProperty("server.port")).isEqualTo("8070");
    assertThat(env.getProperty("management.server.port")).isEqualTo("8071");
    assertThat(env.getProperty("management.server.address")).isEqualTo("127.0.0.1");
    assertThat(env.getProperty("server.servlet.context-path")).isEqualTo("/");
    assertThat(env.getProperty("logging.level.root")).isEqualTo("DEBUG");
    assertThat(env.getProperty("logging.level.com.sonatype.insight.scan")).isEqualTo("INFO");

    initializeAppenders(env);

    Appender<ILoggingEvent> rootConsole = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "console");
    Appender<ILoggingEvent> rootFile = findAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    Appender<ILoggingEvent> auditFile = findAppenderOnLogger("com.sonatype.insight.audit", "file");

    assertThat(rootConsole).isInstanceOf(ConsoleAppender.class);
    assertThat(rootFile).isInstanceOf(RollingFileAppender.class);
    assertThat(auditFile).isInstanceOf(RollingFileAppender.class);
    assertThat(((FileAppender<?>) auditFile).getFile()).endsWith("audit3.log");
  }

  private Appender<ILoggingEvent> findRawAppenderOnLogger(String loggerName, String typeSubstring) {
    cleanupLoggerNames.add(loggerName);
    Logger logger = loggerContext.getLogger(loggerName);
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      Appender<ILoggingEvent> appender = iter.next();
      if (appender.getName() != null && appender.getName().contains("." + typeSubstring + ".")) {
        return appender;
      }
    }
    return null;
  }

  private int countAppenders(Logger logger) {
    int count = 0;
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      iter.next();
      count++;
    }
    return count;
  }

  private String tempFile(String name) throws IOException {
    return tempFolder.newFile(name).getAbsolutePath();
  }

  @Test
  public void testRootLevel_appliedFromConfig() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  level: WARN",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add(Logger.ROOT_LOGGER_NAME);
    assertThat(loggerContext.getLogger(Logger.ROOT_LOGGER_NAME).getLevel()).isEqualTo(Level.WARN);
  }

  @Test
  public void testSimpleStringLoggerLevel_applied() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"org.example.test\": ERROR",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add("org.example.test");
    assertThat(loggerContext.getLogger("org.example.test").getLevel()).isEqualTo(Level.ERROR);
  }

  @Test
  public void testMapLoggerLevel_appliedAlongsideAppenders() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"org.example.test\":",
        "      level: WARN",
        "      appenders:",
        "        - type: console",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add("org.example.test");
    Logger logger = loggerContext.getLogger("org.example.test");
    assertThat(logger.getLevel()).isEqualTo(Level.WARN);
    assertThat(logger.isAdditive()).isFalse();
    assertThat(findAppenderOnLogger("org.example.test", "console")).isNotNull();
  }

  @Test
  public void testMapLoggerNoLevel_inheritsFromParent() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"org.example.test\":",
        "      appenders:",
        "        - type: console",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add("org.example.test");
    assertThat(loggerContext.getLogger("org.example.test").getLevel()).isNull();
  }

  @Test
  public void testMapLoggerAdditive_explicitTrue() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"org.example.test\":",
        "      additive: true",
        "      appenders:",
        "        - type: console",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add("org.example.test");
    assertThat(loggerContext.getLogger("org.example.test").isAdditive()).isTrue();
  }

  @Test
  public void testMapLoggerLevelOnly_noAppenders() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"org.example.test\":",
        "      level: DEBUG",
        ""));

    initializeAppenders(env);

    cleanupLoggerNames.add("org.example.test");
    assertThat(loggerContext.getLogger("org.example.test").getLevel()).isEqualTo(Level.DEBUG);
  }

  @Test
  public void testCustomAppenderFactory_delegatesToRegisteredFactory() throws IOException {
    loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.audit\":",
        "      appenders:",
        "        - type: mtiq-audit-log",
        "          auditLogBasePath: ./target/test-audit-logs",
        ""));

    loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    DropwizardLoggingAppenderConfiguration.CustomAppenderFactory testFactory =
        new DropwizardLoggingAppenderConfiguration.CustomAppenderFactory()
        {
          @Override
          public String supportedType() {
            return "mtiq-audit-log";
          }

          @Override
          public Appender<ILoggingEvent> create(LoggerContext context, Object rawConfig) {
            ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
            appender.setName("test-mtiq-audit-appender");
            appender.setContext(context);
            appender.start();
            return appender;
          }
        };

    DropwizardConfigSourceReader reader = new DropwizardConfigSourceReader();
    Map<String, Object> configMap =
        reader.readConfigMap(Files.writeString(tempFolder.newFile("custom.yml").toPath(), lastConfigYaml).toFile());
    InsightConfig insightConfig = reader.convertValue(configMap, InsightConfig.class);

    ObjectProvider<DropwizardLoggingAppenderConfiguration.CustomAppenderFactory> provider = new ObjectProvider<>()
    {
      @Override
      public DropwizardLoggingAppenderConfiguration.CustomAppenderFactory getObject() {
        return testFactory;
      }

      @Override
      public Stream<DropwizardLoggingAppenderConfiguration.CustomAppenderFactory> orderedStream() {
        return Stream.of(testFactory);
      }
    };

    DropwizardLoggingAppenderConfiguration config = new DropwizardLoggingAppenderConfiguration(provider);
    config.dropwizardLoggingAppenderInitializer(insightConfig).afterSingletonsInstantiated();

    cleanupLoggerNames.add("com.sonatype.insight.audit");
    Logger auditLogger = loggerContext.getLogger("com.sonatype.insight.audit");
    Appender<ILoggingEvent> topAppender = auditLogger.iteratorForAppenders().next();
    assertThat(topAppender).isInstanceOf(ch.qos.logback.classic.AsyncAppender.class);
    ch.qos.logback.classic.AsyncAppender asyncAppender = (ch.qos.logback.classic.AsyncAppender) topAppender;
    Appender<ILoggingEvent> inner = asyncAppender.iteratorForAppenders().next();
    assertThat(inner.getName()).isEqualTo("test-mtiq-audit-appender");
  }

  private Appender<ILoggingEvent> findAppenderByName(String loggerName, String appenderName) {
    Logger logger = loggerContext.getLogger(loggerName);
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      Appender<ILoggingEvent> appender = iter.next();
      if (appenderName.equals(appender.getName())) {
        return appender;
      }
    }
    return null;
  }

  @Test
  public void testAuditLogger_fileAppenderWithoutLogFormat_defaultsToMessageOnly() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.audit\":",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "file");
    assertThat(patternOf(appender)).isEqualTo("%message%n");
  }

  @Test
  public void testAuditLogger_consoleAppenderWithoutLogFormat_defaultsToMessageOnly() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.audit\":",
        "      appenders:",
        "        - type: console",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "console");
    assertThat(patternOf(appender)).isEqualTo("%message%n");
  }

  @Test
  public void testPolicyViolationLogger_fileAppenderWithoutLogFormat_defaultsToMessageOnly() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.policy.violation\":",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("policy-violation.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.policy.violation", "file");
    assertThat(patternOf(appender)).isEqualTo("%message%n");
  }

  @Test
  public void testAuditLogger_explicitLogFormat_isRespected() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    \"com.sonatype.insight.audit\":",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("audit.log"),
        "          logFormat: \"%level %msg%n\"",
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.sonatype.insight.audit", "file");
    assertThat(patternOf(appender)).isEqualTo("%level %msg%n");
  }

  @Test
  public void testNonIndependentLogger_fileAppenderWithoutLogFormat_usesDefaultFormat() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  loggers:",
        "    com.example.custom:",
        "      appenders:",
        "        - type: file",
        "          currentLogFilename: " + tempFile("custom.log"),
        ""));

    initializeAppenders(env);

    Appender<ILoggingEvent> appender = findAppenderOnLogger("com.example.custom", "file");
    assertThat(patternOf(appender)).isEqualTo(DropwizardAppenderFactory.DEFAULT_LOG_FORMAT);
  }

  private String patternOf(Appender<ILoggingEvent> appender) {
    assertThat(appender).isInstanceOf(OutputStreamAppender.class);
    LayoutWrappingEncoder<ILoggingEvent> encoder =
        (LayoutWrappingEncoder<ILoggingEvent>) ((OutputStreamAppender<ILoggingEvent>) appender).getEncoder();
    assertThat(encoder.getLayout()).isInstanceOf(PatternLayout.class);
    return ((PatternLayout) encoder.getLayout()).getPattern();
  }

  @Test
  public void testFileAppender_withoutDiscardingThreshold_defaultsToNoLoss() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        "      currentLogFilename: " + tempFile("server.log"),
        ""));

    initializeAppenders(env);

    AsyncAppender async = findAsyncAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(async).isNotNull();
    assertThat(async.getDiscardingThreshold()).isEqualTo(0);
    assertThat(async.isNeverBlock()).isFalse();
  }

  @Test
  public void testFileAppender_explicitDiscardingThreshold_isHonored() throws IOException {
    StandardEnvironment env = loadConfig(String.join("\n",
        "logging:",
        "  appenders:",
        "    - type: file",
        "      currentLogFilename: " + tempFile("server.log"),
        "      discardingThreshold: 10",
        ""));

    initializeAppenders(env);

    AsyncAppender async = findAsyncAppenderOnLogger(Logger.ROOT_LOGGER_NAME, "file");
    assertThat(async.getDiscardingThreshold()).isEqualTo(10);
  }

  private AsyncAppender findAsyncAppenderOnLogger(String loggerName, String typeSubstring) {
    cleanupLoggerNames.add(loggerName);
    Logger logger = loggerContext.getLogger(loggerName);
    Iterator<Appender<ILoggingEvent>> iter = logger.iteratorForAppenders();
    while (iter.hasNext()) {
      Appender<ILoggingEvent> appender = iter.next();
      if (appender instanceof AsyncAppender async
          && async.getName() != null
          && async.getName().contains("." + typeSubstring + "."))
      {
        return async;
      }
    }
    return null;
  }
}
