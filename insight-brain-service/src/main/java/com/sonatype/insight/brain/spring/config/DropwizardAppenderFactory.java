/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.classic.AsyncAppender;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.net.SSLSocketAppender;
import ch.qos.logback.classic.net.SocketAppender;
import ch.qos.logback.classic.net.SyslogAppender;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.ConsoleAppender;
import ch.qos.logback.core.FileAppender;
import ch.qos.logback.core.Layout;
import ch.qos.logback.core.encoder.LayoutWrappingEncoder;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.net.ssl.KeyStoreFactoryBean;
import ch.qos.logback.core.net.ssl.SSLConfiguration;
import ch.qos.logback.core.net.ssl.SSLParametersConfiguration;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.spi.FilterReply;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.json.EventJsonLayoutBaseFactory;
import java.time.Duration;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DropwizardAppenderFactory
{
  private static final Logger log = LoggerFactory.getLogger(DropwizardAppenderFactory.class);

  static final String DEFAULT_LOG_FORMAT = "%-5p [%d{ISO8601,UTC}] %c: %m%n%rEx";

  static final int DEFAULT_QUEUE_SIZE = 256;

  // 0 means the async appender never discards events: it blocks instead of dropping INFO-and-below entries (such as
  // audit and policy-violation records) once its queue fills. The previous sentinel (-1) left logback's own default
  // (queueSize / 5) in place, which discards them under load.
  static final int DEFAULT_DISCARDING_THRESHOLD = 0;

  private DropwizardAppenderFactory() {
  }

  static Appender<ILoggingEvent> createFileAppender(
      LoggerContext context,
      String name,
      String filename,
      String archivePattern,
      Integer archiveCount,
      String logFormat,
      boolean archive)
  {
    return createFileAppender(context, name, filename, archivePattern, archiveCount, logFormat, archive, null);
  }

  static Appender<ILoggingEvent> createFileAppender(
      LoggerContext context,
      String name,
      String filename,
      String archivePattern,
      Integer archiveCount,
      String logFormat,
      boolean archive,
      Object layoutConfig)
  {
    LayoutWrappingEncoder<ILoggingEvent> encoder = createEncoderWithLayout(context, logFormat, layoutConfig);

    if (!archive) {
      FileAppender<ILoggingEvent> appender = new FileAppender<>();
      appender.setName(name);
      appender.setContext(context);
      appender.setFile(filename);
      appender.setAppend(true);
      appender.setEncoder(encoder);
      appender.start();
      return appender;
    }

    RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
    appender.setName(name);
    appender.setContext(context);
    appender.setFile(filename);
    appender.setEncoder(encoder);

    TimeBasedRollingPolicy<ILoggingEvent> rollingPolicy = new TimeBasedRollingPolicy<>();
    rollingPolicy.setContext(context);
    rollingPolicy.setFileNamePattern(
        archivePattern != null ? archivePattern : deriveArchivePattern(filename));
    if (archiveCount != null) {
      rollingPolicy.setMaxHistory(archiveCount);
    }
    rollingPolicy.setParent(appender);
    rollingPolicy.start();

    appender.setRollingPolicy(rollingPolicy);
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createConsoleAppender(
      LoggerContext context,
      String name,
      String logFormat,
      String target)
  {
    return createConsoleAppender(context, name, logFormat, target, null);
  }

  static Appender<ILoggingEvent> createConsoleAppender(
      LoggerContext context,
      String name,
      String logFormat,
      String target,
      Object layoutConfig)
  {
    ConsoleAppender<ILoggingEvent> appender = new ConsoleAppender<>();
    appender.setName(name);
    appender.setContext(context);
    appender.setEncoder(createEncoderWithLayout(context, logFormat, layoutConfig));
    if ("stderr".equalsIgnoreCase(target)) {
      appender.setTarget("System.err");
    }
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createSyslogAppender(
      LoggerContext context,
      String name,
      String host,
      int port,
      String facility,
      String suffixPattern,
      String stackTracePrefix)
  {
    SyslogAppender appender = new SyslogAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setSyslogHost(host != null ? host : "localhost");
    appender.setPort(port > 0 ? port : 514);
    appender.setFacility(facility != null ? facility : "LOCAL0");
    if (suffixPattern != null) {
      appender.setSuffixPattern(suffixPattern);
    }
    if (stackTracePrefix != null) {
      appender.setStackTracePattern(stackTracePrefix);
    }
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createTcpAppender(
      LoggerContext context,
      String name,
      String host,
      int port,
      Duration connectionTimeout,
      boolean includeCallerData)
  {
    SocketAppender appender = new SocketAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setRemoteHost(host != null ? host : "localhost");
    appender.setPort(port > 0 ? port : 4560);
    if (connectionTimeout != null) {
      appender.setReconnectionDelay(new ch.qos.logback.core.util.Duration(connectionTimeout.toMillis()));
    }
    appender.setIncludeCallerData(includeCallerData);
    appender.start();
    return appender;
  }

  static Appender<ILoggingEvent> createTlsAppender(
      LoggerContext context,
      String name,
      DropwizardAppenderConfig.Tls config)
  {
    SSLSocketAppender appender = new SSLSocketAppender();
    appender.setName(name);
    appender.setContext(context);
    appender.setRemoteHost(config.host != null ? config.host : "localhost");
    appender.setPort(config.port != null && config.port > 0 ? config.port : 4560);
    // logback's SSLSocketAppender exposes no socket connect-timeout setter, so connectionTimeout is mapped to the
    // reconnection delay - an approximation, but the same one the TCP appender uses (Dropwizard 5 used the value as
    // the actual connect timeout via a custom socket appender that logback's stock appenders do not provide).
    Duration connectionTimeout = parseDuration(config.connectionTimeout);
    if (connectionTimeout != null) {
      appender.setReconnectionDelay(new ch.qos.logback.core.util.Duration(connectionTimeout.toMillis()));
    }
    appender.setIncludeCallerData(Boolean.TRUE.equals(config.includeCallerData));
    SSLConfiguration ssl = buildSslConfiguration(config);
    if (ssl != null) {
      appender.setSsl(ssl);
    }
    appender.start();
    return appender;
  }

  /**
   * Builds a logback {@link SSLConfiguration} from the appender's key/trust store and protocol/cipher settings, so a
   * tls appender honours configured client/server certificates (as pre-Spring's Dropwizard TLS appender did) rather
   * than only the JVM default SSL context. Returns {@code null} when no SSL settings are present (default context).
   */
  private static SSLConfiguration buildSslConfiguration(DropwizardAppenderConfig.Tls config) {
    boolean hasKeyStore = config.keyStorePath != null && !config.keyStorePath.isBlank();
    boolean hasTrustStore = config.trustStorePath != null && !config.trustStorePath.isBlank();
    boolean hasParameters = config.supportedProtocols != null || config.excludedProtocols != null
        || config.supportedCipherSuites != null || config.excludedCipherSuites != null;
    boolean hasProvider = config.jceProvider != null && !config.jceProvider.isBlank();
    if (!hasKeyStore && !hasTrustStore && !hasParameters && !hasProvider) {
      return null;
    }
    SSLConfiguration ssl = new SSLConfiguration();
    if (hasProvider) {
      ssl.setProvider(config.jceProvider);
    }
    if (hasKeyStore) {
      ssl.setKeyStore(keyStore(config.keyStorePath, config.keyStorePassword, config.keyStoreType,
          config.keyStoreProvider));
    }
    if (hasTrustStore) {
      ssl.setTrustStore(keyStore(config.trustStorePath, config.trustStorePassword, config.trustStoreType,
          config.trustStoreProvider));
    }
    if (hasParameters) {
      SSLParametersConfiguration parameters = new SSLParametersConfiguration();
      if (config.supportedProtocols != null) {
        parameters.setIncludedProtocols(String.join(",", config.supportedProtocols));
      }
      if (config.excludedProtocols != null) {
        parameters.setExcludedProtocols(String.join(",", config.excludedProtocols));
      }
      if (config.supportedCipherSuites != null) {
        parameters.setIncludedCipherSuites(String.join(",", config.supportedCipherSuites));
      }
      if (config.excludedCipherSuites != null) {
        parameters.setExcludedCipherSuites(String.join(",", config.excludedCipherSuites));
      }
      ssl.setParameters(parameters);
    }
    return ssl;
  }

  private static KeyStoreFactoryBean keyStore(String location, String password, String type, String provider) {
    KeyStoreFactoryBean keyStore = new KeyStoreFactoryBean();
    keyStore.setLocation(toKeyStoreLocation(location));
    keyStore.setPassword(password);
    keyStore.setType(type);
    keyStore.setProvider(provider);
    return keyStore;
  }

  /**
   * Logback's {@code KeyStoreFactoryBean} resolves a scheme-less location as a <em>classpath resource</em>
   * ({@code LocationUtil}), but Dropwizard's TLS appender passed {@code keyStorePath}/{@code trustStorePath} to
   * Jetty, which accepts plain filesystem paths - so pre-migration configs use bare paths like
   * {@code /etc/ssl/truststore.p12}. Map a scheme-less location to a {@code file:} URL so those configs keep
   * working; explicit {@code file:}/{@code classpath:} locations pass through unchanged.
   */
  static String toKeyStoreLocation(String location) {
    if (location.matches("^\\p{Alpha}[\\p{Alnum}+.-]*:.*$")) {
      return location;
    }
    return "file:" + location;
  }

  /**
   * Maps a Dropwizard threshold/level value to a logback {@link Level}, reproducing Dropwizard's
   * {@code DefaultLoggingFactory.toLevel}. YAML parses a bare {@code OFF}/{@code ON} as a Boolean and strict config
   * conversion then coerces it to {@code "false"}/{@code "true"}; this handles those alongside an explicit
   * {@code OFF}/{@code INFO} string. Returns {@code fallback} for a null/blank/unrecognised value.
   */
  static Level toLevel(Object value, Level fallback) {
    if (value == null) {
      return fallback;
    }
    String level = String.valueOf(value).trim();
    if (level.isEmpty()) {
      return fallback;
    }
    if ("false".equalsIgnoreCase(level)) {
      return Level.OFF;
    }
    if ("true".equalsIgnoreCase(level)) {
      return Level.ALL;
    }
    return Level.toLevel(level, fallback);
  }

  static void applyThresholdFilter(Appender<ILoggingEvent> appender, String threshold) {
    Level level = toLevel(threshold, null);
    if (level == null) {
      return;
    }
    appender.addFilter(new Filter<>()
    {
      @Override
      public FilterReply decide(ILoggingEvent event) {
        if (event.getLevel().isGreaterOrEqual(level)) {
          return FilterReply.NEUTRAL;
        }
        return FilterReply.DENY;
      }
    });
  }

  @SuppressWarnings("unchecked")
  public static LayoutWrappingEncoder<ILoggingEvent> createEncoderWithLayout(
      LoggerContext context,
      String logFormat,
      Object layoutConfig)
  {
    if (layoutConfig instanceof Map<?, ?> layoutMap
        && "json".equals(((Map<String, Object>) layoutMap).get("type")))
    {
      return createJsonEncoder(context, layoutMap);
    }
    return createEncoder(context, logFormat);
  }

  static LayoutWrappingEncoder<ILoggingEvent> createJsonEncoder(LoggerContext context, Map<?, ?> layoutMap) {
    EventJsonLayoutBaseFactory factory =
        Jackson.newObjectMapper().convertValue(layoutMap, EventJsonLayoutBaseFactory.class);
    Layout<ILoggingEvent> layout = factory.build(context, TimeZone.getTimeZone("UTC"));
    layout.setContext(context);
    layout.start();

    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    encoder.setContext(context);
    encoder.setLayout(layout);
    encoder.start();
    return encoder;
  }

  static LayoutWrappingEncoder<ILoggingEvent> createEncoder(LoggerContext context, String logFormat) {
    LayoutWrappingEncoder<ILoggingEvent> encoder = new LayoutWrappingEncoder<>();
    PatternLayout layout = new PatternLayout();
    layout.setContext(context);
    layout.setPattern(translateLogFormat(logFormat != null ? logFormat : DEFAULT_LOG_FORMAT));
    layout.start();
    encoder.setContext(context);
    encoder.setLayout(layout);
    encoder.start();
    return encoder;
  }

  static String translateLogFormat(String format) {
    return format
        .replace("%dwRootException", "%rEx")
        .replace("%dwREx", "%rEx")
        .replace("%dwXException", "%xEx")
        .replace("%dwXThrowable", "%xEx")
        .replace("%dwXEx", "%xEx")
        .replace("%dwException", "%ex")
        .replace("%dwThrowable", "%ex")
        .replace("%dwEx", "%ex");
  }

  static Appender<ILoggingEvent> wrapAsync(
      LoggerContext context,
      Appender<ILoggingEvent> appender,
      int queueSize,
      int discardingThreshold,
      boolean neverBlock)
  {
    if (queueSize <= 0) {
      return appender;
    }
    AsyncAppender asyncAppender = new AsyncAppender();
    asyncAppender.setName(appender.getName() + ".async");
    asyncAppender.setContext(context);
    asyncAppender.setQueueSize(queueSize);
    if (discardingThreshold >= 0) {
      asyncAppender.setDiscardingThreshold(discardingThreshold);
    }
    asyncAppender.setNeverBlock(neverBlock);
    asyncAppender.addAppender(appender);
    asyncAppender.start();
    return asyncAppender;
  }

  /**
   * Resolved async-wrapping settings for an appender, with IQ's defaults applied (queue 256, no-loss
   * discardingThreshold 0, blocking). Shared so every appender - logger, root, and request log - reads the
   * same knobs the same way.
   */
  record AsyncSettings(int queueSize, int discardingThreshold, boolean neverBlock)
  {
  }

  static AsyncSettings asyncSettings(DropwizardAppenderConfig config) {
    return new AsyncSettings(
        config.queueSize != null ? config.queueSize : DEFAULT_QUEUE_SIZE,
        config.discardingThreshold != null ? config.discardingThreshold : DEFAULT_DISCARDING_THRESHOLD,
        Boolean.TRUE.equals(config.neverBlock));
  }

  static Appender<ILoggingEvent> wrapAsync(LoggerContext context, Appender<ILoggingEvent> appender, AsyncSettings s) {
    return wrapAsync(context, appender, s.queueSize(), s.discardingThreshold(), s.neverBlock());
  }

  static String deriveArchivePattern(String filename) {
    int extensionIndex = filename.lastIndexOf('.');
    int pathSeparatorIndex = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
    if (extensionIndex > pathSeparatorIndex) {
      return filename.substring(0, extensionIndex) + "-%d" + filename.substring(extensionIndex);
    }
    return filename + "-%d";
  }

  /**
   * Strictly converts a raw appender config map to the {@link DropwizardAppenderConfig} subtype matching its
   * {@code type}, so unknown keys are rejected (matching pre-Spring Dropwizard parsing). Shared by both the
   * application-logging and request-logging paths so every appender type is parsed and validated identically.
   * Returns {@code null} for an unrecognized type; the caller decides what to do (skip it, or try a custom
   * appender factory). The returned subtype lines up with the {@code type} expected by {@link #createAppender}.
   */
  static DropwizardAppenderConfig convertConfig(
      DropwizardConfigSourceReader configSourceReader,
      String type,
      Object rawConfig)
  {
    return switch (type == null ? "" : type) {
      case "console" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Console.class);
      case "file" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.File.class);
      case "syslog" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Syslog.class);
      case "tcp" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Tcp.class);
      case "tls" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Tls.class);
      case "udp" -> configSourceReader.convertValueStrict(rawConfig, DropwizardAppenderConfig.Udp.class);
      default -> null;
    };
  }

  /**
   * Builds the logback appender for a single Dropwizard appender config, dispatching on type. Shared by both
   * application logging ({@code DropwizardLoggingAppenderConfiguration}) and request logging
   * ({@code RequestLoggingConfiguration}) so every appender type is constructed the same way. Does not apply the
   * threshold filter or async wrapping - callers do that via {@link #applyThresholdFilter} and {@link #wrapAsync}.
   * Returns {@code null} for an unrecognized type or a file appender with no filename (the caller skips it).
   *
   * @param type the appender type string; <b>must</b> match the runtime subtype of {@code config} (e.g. {@code
   *     "console"} with a {@link DropwizardAppenderConfig.Console}). The method downcasts {@code config} based on
   *          {@code type}, so a mismatched pair throws {@link ClassCastException}.
   * @param config a typed {@link DropwizardAppenderConfig} subclass consistent with {@code type}
   */
  static Appender<ILoggingEvent> createAppender(
      LoggerContext context,
      String name,
      String type,
      DropwizardAppenderConfig config)
  {
    Objects.requireNonNull(type, "type");
    return switch (type) {
      case "console" -> {
        DropwizardAppenderConfig.Console c = (DropwizardAppenderConfig.Console) config;
        yield createConsoleAppender(context, name, c.logFormat, c.target, c.layout);
      }
      case "file" -> {
        DropwizardAppenderConfig.File c = (DropwizardAppenderConfig.File) config;
        if (c.currentLogFilename == null || c.currentLogFilename.isBlank()) {
          log.warn("File appender '{}' is missing 'currentLogFilename' and will be skipped", name);
          yield null;
        }
        yield createFileAppender(context, name, c.currentLogFilename, c.archivedLogFilenamePattern,
            c.archivedFileCount, c.logFormat, !Boolean.FALSE.equals(c.archive), c.layout);
      }
      case "syslog" -> {
        DropwizardAppenderConfig.Syslog c = (DropwizardAppenderConfig.Syslog) config;
        yield createSyslogAppender(context, name, c.host, c.port != null ? c.port : 514, c.facility,
            c.logFormat, c.stackTracePrefix);
      }
      case "tcp" -> {
        DropwizardAppenderConfig.Tcp c = (DropwizardAppenderConfig.Tcp) config;
        yield createTcpAppender(context, name, c.host, c.port != null ? c.port : 4560,
            parseDuration(c.connectionTimeout), Boolean.TRUE.equals(c.includeCallerData));
      }
      case "tls" -> createTlsAppender(context, name, (DropwizardAppenderConfig.Tls) config);
      default -> null;
    };
  }

  static Duration parseDuration(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return DropwizardDurationParser.parse(value);
    }
    catch (IllegalArgumentException e) {
      log.warn("Could not parse duration '{}': {}", value, e.getMessage());
      return null;
    }
  }
}
