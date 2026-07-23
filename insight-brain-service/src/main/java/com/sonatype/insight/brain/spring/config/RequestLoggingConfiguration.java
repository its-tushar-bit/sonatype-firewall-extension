/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.access.common.spi.IAccessEvent;
import ch.qos.logback.access.jetty.RequestLogImpl;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sonatype.insight.brain.security.AuthenticationLoggingFilter;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.telemetry.UserTelemetryRequestLoggingFilter;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.logging.common.AppenderFactory;
import io.dropwizard.logging.common.FileAppenderFactory;
import io.dropwizard.logging.common.async.AsyncAppenderFactory;
import io.dropwizard.logging.common.filter.LevelFilterFactory;
import io.dropwizard.logging.common.filter.NullLevelFilterFactory;
import io.dropwizard.logging.common.layout.LayoutFactory;
import io.dropwizard.request.logging.async.AsyncAccessEventAppenderFactory;
import io.dropwizard.request.logging.layout.LogbackAccessRequestLayoutFactory;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.eclipse.jetty.server.CustomRequestLog;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.RequestLog;
import org.slf4j.LoggerFactory;
import org.springframework.boot.jetty.servlet.JettyServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RequestLoggingConfiguration
{
  private static final org.slf4j.Logger log = LoggerFactory.getLogger(RequestLoggingConfiguration.class);

  static final String DEFAULT_REQUEST_LOG_TIME_ZONE = "UTC";

  // Jetty's %t wraps the date in [brackets] itself (no literal brackets here, or the line gets [[date]]), and %D is
  // microseconds, so elapsed time uses %{ms}T - both so the rendered line matches pre-migration logback-access output
  // ("[%date]" single-bracketed, "%elapsedTime" in milliseconds).
  static final String DEFAULT_REQUEST_LOG_FORMAT =
      "%{client}a - %u %{dd/MMM/yyyy:HH:mm:ss Z|__TIME_ZONE__}t \"%r\" %s %O %{ms}T \"%{User-Agent}i\"";

  static final String LEGACY_REQUEST_LOG_FORMAT =
      "%clientHost %l %user [%date] \"%requestURL\" %statusCode %bytesSent %elapsedTime \"%header{User-Agent}\"";

  static final String REQUEST_LOGGER_NAME_PREFIX = "com.sonatype.insight.requestlog.";

  // Application name passed to Dropwizard's access AppenderFactory.build (used only to name the appenders).
  private static final String ACCESS_LOG_APPLICATION_NAME = "insight-brain";

  // Request-log lines are pre-formatted by Jetty's CustomRequestLog, so every appender emits the raw message only.
  private static final String REQUEST_LOG_MESSAGE_FORMAT = "%msg%n";

  // Request-log appender types Dropwizard's request log supported (same AppenderFactory set as general logging).
  // udp is recognized but ignored, matching DropwizardLoggingAppenderConfiguration.
  private static final Set<String> SUPPORTED_REQUEST_LOG_TYPES = Set.of("console", "file", "syslog", "tcp", "tls",
      "udp");

  private final DropwizardConfigSourceReader configSourceReader = new DropwizardConfigSourceReader();

  @Bean
  UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter() {
    return new UserTelemetryRequestLoggingFilter();
  }

  @Bean
  WebServerFactoryCustomizer<JettyServletWebServerFactory> requestLoggingCustomizer(
      final InsightConfig insightConfig,
      final UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter)
  {
    return factory -> {
      RequestLogConfig requestLogConfig =
          insightConfig.getServer() == null ? null : insightConfig.getServer().requestLog;
      if (requestLogConfig == null) {
        // No server.requestLog section: pre-Spring defaulted to logback-access with a single console appender, i.e.
        // request logging on. Reproduce that default so omitting the section does not silently disable it.
        installRequestLog(factory,
            createAccessRequestLog(List.of(Map.of("type", "console")), userTelemetryRequestLoggingFilter));
        return;
      }
      String type = requestLogConfig.type;
      if ("external".equalsIgnoreCase(type)) {
        // Pre-Spring routed 'external' request logs through SLF4J; that is not supported here. Warn rather than fail
        // (the config still parses) so an operator who set REQUEST_LOG_TYPE=external knows no request log is emitted.
        log.warn("server.requestLog 'type: external' is not supported and installs no request log;"
            + " use 'classic', 'logback-access', or an access-json appender instead.");
        return;
      }
      List<Map<String, Object>> appenders = requestLogConfig.appenders;
      if (appenders == null || appenders.isEmpty()) {
        // requestLog present but with no appenders (an explicitly empty list): nothing to install.
        return;
      }

      boolean accessJson = hasAccessJsonAppender(appenders);
      if (!accessJson && isUnsupportedRequestLogType(type)) {
        log.warn("server.requestLog 'type: {}' is not supported; request logging is disabled.", type);
        return;
      }

      // Route by type, matching pre-Spring: 'classic' uses the single-format Jetty CustomRequestLog (NCSA) path;
      // an unset type, 'logback-access', or any access-json layout uses the logback-access IAccessEvent path, where
      // every appender formats its own line (per-appender logFormat / access-json layout).
      if ("classic".equalsIgnoreCase(type) && !accessJson) {
        RequestLogSettings requestLogSettings = requestLogSettings(requestLogConfig);
        if (!requestLogSettings.enabled()) {
          return;
        }
        String requestLogFilename = insightConfig.getRequestLogFilename();
        RequestLog.Writer requestLogWriter = requestLogWriter(requestLogFilename, resolveActiveAppenders(appenders));
        if (requestLogWriter == null) {
          return;
        }
        installRequestLog(factory, requestLog(requestLogWriter, userTelemetryRequestLoggingFilter, requestLogSettings));
      }
      else {
        installRequestLog(factory, createAccessRequestLog(appenders, userTelemetryRequestLoggingFilter));
      }
    };
  }

  private void installRequestLog(final JettyServletWebServerFactory factory, final RequestLog requestLog) {
    if (requestLog != null) {
      factory.addServerCustomizers(server -> server.setRequestLog(requestLog));
    }
  }

  private boolean isUnsupportedRequestLogType(final String type) {
    return type != null && !type.isBlank()
        && !"classic".equalsIgnoreCase(type) && !"logback-access".equalsIgnoreCase(type);
  }

  private boolean hasAccessJsonAppender(final List<?> appenders) {
    return appenderMaps(appenders).anyMatch(this::isAccessJsonAppender);
  }

  private boolean isAccessJsonAppender(final Map<String, Object> appender) {
    return appender.get("layout") instanceof Map<?, ?> layout && "access-json".equals(asMap(layout).get("type"));
  }

  /**
   * Builds the logback-access ({@link IAccessEvent}) request log. Every configured appender is built through
   * Dropwizard's own {@link AppenderFactory} - the same machinery the pre-Spring InsightConfigurationFactory used - so
   * all appender types and multiple appenders are honoured, and each appender formats its own line: an explicit
   * {@code access-json} layout or {@code logFormat} is kept, and an appender with neither gets the IQ default request
   * log format (matching pre-Spring, which injected it into appenders without a logFormat). Appenders whose
   * {@code threshold} resolves to OFF are dropped first - pre-Spring ignored request-log thresholds entirely, but the
   * classic path now honours OFF (so {@code REQUEST_LOG_FILE_THRESHOLD=OFF} works); applying it here too keeps the two
   * paths consistent. Returns {@code null} (install nothing) when no appender remains. Async wrapping is taken from
   * each appender's config and is lossless by default (access events have no level, so they are never discardable,
   * and {@code neverBlock} defaults to blocking).
   */
  RequestLog createAccessRequestLog(
      final List<Map<String, Object>> appenders,
      final UserTelemetryRequestLoggingFilter telemetryFilter)
  {
    List<AppenderFactory<IAccessEvent>> appenderFactories = accessAppenderFactories(appenders);
    if (appenderFactories.isEmpty()) {
      return null;
    }
    log.info("Configuring access-event request log with {} appender(s)", appenderFactories.size());
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

    RequestLogImpl requestLog = new RequestLogImpl();
    requestLog.setQuiet(true);

    LayoutFactory<IAccessEvent> layoutFactory = new LogbackAccessRequestLayoutFactory();
    LevelFilterFactory<IAccessEvent> levelFilterFactory = new NullLevelFilterFactory<>();
    AsyncAppenderFactory<IAccessEvent> asyncAppenderFactory = new AsyncAccessEventAppenderFactory();
    for (AppenderFactory<IAccessEvent> appenderFactory : appenderFactories) {
      requestLog.addAppender(appenderFactory.build(
          loggerContext, ACCESS_LOG_APPLICATION_NAME, layoutFactory, levelFilterFactory, asyncAppenderFactory));
    }

    // The OTel Java agent does not instrument logback-access. Attach a custom bridge appender so
    // HTTP access events still reach OTLP. The bridge is a no-op when the OTel SDK is not active.
    OpenTelemetryLogbackInstaller.installAccessAppender(loggerContext, requestLog);
    requestLog.start();

    return (request, response) -> {
      if (!telemetryFilter.shouldSkip(Request.getPathInContext(request))) {
        requestLog.log(request, response);
      }
    };
  }

  private boolean warnAndExcludeUdp(final Map<String, Object> appender) {
    if ("udp".equals(stringProperty(appender.get("type")))) {
      log.warn("UDP request-log appenders are not supported and will be ignored");
      return false;
    }
    return true;
  }

  /**
   * Prepares an access appender's {@code logFormat} for the logback-access path. An {@code access-json} appender's
   * layout type is rewritten to the IQ {@code iq-access-json} variant (see {@link #withRemoteUserAccessJsonLayout}) so
   * the authenticated username renders under {@code remoteUser}. Every other appender is given a {@link
   * ch.qos.logback.access.common.PatternLayout} format: its own {@code logFormat} if set, otherwise the IQ default
   * ({@link #LEGACY_REQUEST_LOG_FORMAT}). In both cases the remote-user token is
   * rewritten (see {@link #rewriteRemoteUserToken}) so the authenticated username actually renders on this path.
   */
  private Map<String, Object> withDefaultAccessLogFormat(final Map<String, Object> appender) {
    if (isAccessJsonAppender(appender)) {
      return withRemoteUserAccessJsonLayout(appender);
    }
    String logFormat = appender.get("logFormat") instanceof String configured && !configured.isBlank()
        ? configured
        : LEGACY_REQUEST_LOG_FORMAT;
    Map<String, Object> withFormat = new LinkedHashMap<>(appender);
    withFormat.put("logFormat", rewriteRemoteUserToken(logFormat));
    return withFormat;
  }

  /**
   * Rewrites an {@code access-json} appender's layout type to the IQ {@link RemoteUserAccessJsonLayoutFactory}
   * variant so the authenticated username renders under {@code remoteUser}. The stock {@code access-json} layout
   * sources {@code remoteUser} from {@code IAccessEvent.getRemoteUser()}, which the logback-access
   * {@code RequestWrapper} stubs to {@code null}. Operators keep configuring {@code layout: {type: access-json}};
   * the rewrite is internal, mirroring the pattern path's {@code %user}->{@code %reqAttribute} rewrite. The
   * appender and its nested layout map are copied so the incoming config is not mutated. CLM-42654.
   */
  private Map<String, Object> withRemoteUserAccessJsonLayout(final Map<String, Object> appender) {
    Map<String, Object> layout = new LinkedHashMap<>(asMap((Map<?, ?>) appender.get("layout")));
    layout.put("type", RemoteUserAccessJsonLayoutFactory.TYPE_NAME);
    Map<String, Object> withLayout = new LinkedHashMap<>(appender);
    withLayout.put("layout", layout);
    return withLayout;
  }

  /**
   * Rewrites the logback-access remote-user token ({@code %user}, or its {@code %u} alias) in an access-path format to
   * a request-attribute lookup ({@code %reqAttribute{...}}) that reads the username
   * {@link AuthenticationLoggingFilter} stashes on the request. This is the access-path counterpart to the classic
   * path's {@code %user}->{@code %u} rewrite in {@link #toJettyRequestLogFormat}: on the logback-access path
   * {@code %user}/{@code %u} resolve to the {@code RemoteUserConverter}, whose backing
   * {@code RequestWrapper.getRemoteUser()} is stubbed to {@code null} and renders "-". The uppercase {@code %U}
   * (request URI) is deliberately left untouched. The negative lookahead only guards letter-continuation, so it
   * relies on match case-sensitivity to skip {@code %U}; a brace form like {@code %u{...}} is not a valid
   * logback-access user token and never appears in practice. Operators keep configuring {@code %user} as before -
   * no config change is required. CLM-41689.
   */
  private String rewriteRemoteUserToken(final String logFormat) {
    return logFormat.replaceAll("%u(?:ser)?(?![a-zA-Z])",
        "%reqAttribute{" + AuthenticationLoggingFilter.REQUEST_LOG_REMOTE_USER_ATTRIBUTE + "}");
  }

  List<AppenderFactory<IAccessEvent>> accessAppenderFactories(final List<Map<String, Object>> appenders) {
    // Drop OFF appenders and warn-and-skip udp (both consistent with the classic path), then give layout/logFormat-less
    // appenders the IQ default format and deserialize into Dropwizard's per-appender access factories.
    List<Map<String, Object>> activeAppenders = appenders.stream()
        .filter(appender -> !isOff(appender.get("threshold")))
        .filter(this::warnAndExcludeUdp)
        .map(this::withDefaultAccessLogFormat)
        .toList();
    ObjectMapper mapper = Jackson.newObjectMapper();
    // Reject unknown appender keys, matching the classic path and pre-Spring (Dropwizard's YamlConfigurationFactory
    // parsed config strictly), so a typo'd key fails the same way under logback-access as it does under classic.
    mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.setSubtypeResolver(new DiscoverableSubtypeResolver());
    // Async (queue size, neverBlock) comes from each appender's own config via Dropwizard's deserialization; the
    // default is lossless for access events (never discardable, neverBlock defaults to blocking).
    return mapper.convertValue(activeAppenders, new TypeReference<List<AppenderFactory<IAccessEvent>>>()
    {
    })
        .stream()
        .filter(this::completeFileAppenderOrSkip)
        .toList();
  }

  /**
   * Completes or drops an incomplete file appender factory the way the classic path does, since {@code convertValue}
   * does not run the bean validation pre-Spring Dropwizard enforced on these factories: a missing
   * {@code archivedLogFilenamePattern} (required when {@code archive} is on, the default) is derived from
   * {@code currentLogFilename}, and an appender with no target file at all is skipped with a warning rather than
   * silently failing to start.
   */
  private boolean completeFileAppenderOrSkip(final AppenderFactory<IAccessEvent> factory) {
    if (!(factory instanceof FileAppenderFactory<IAccessEvent> file)) {
      return true;
    }
    boolean hasFilename = file.getCurrentLogFilename() != null && !file.getCurrentLogFilename().isBlank();
    if (!hasFilename && (!file.isArchive() || file.getArchivedLogFilenamePattern() == null)) {
      log.warn("Request log file appender has no currentLogFilename (or archivedLogFilenamePattern) and will be"
          + " skipped.");
      return false;
    }
    if (file.isArchive() && file.getArchivedLogFilenamePattern() == null) {
      file.setArchivedLogFilenamePattern(DropwizardAppenderFactory.deriveArchivePattern(file.getCurrentLogFilename()));
    }
    return true;
  }

  RequestLog.Writer requestLogWriter(
      final String requestLogFilename,
      final List<Map<String, Object>> activeAppenders)
  {
    LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    String identity = requestLogFilename == null || requestLogFilename.isBlank()
        ? "console"
        : Integer.toHexString(requestLogFilename.hashCode());
    String loggerName = REQUEST_LOGGER_NAME_PREFIX + identity;
    Logger logger = loggerContext.getLogger(loggerName);

    logger.detachAndStopAllAppenders();
    logger.setAdditive(false);
    logger.setLevel(Level.INFO);

    // Restore the pre-Spring-migration behaviour: attach every active request-log appender, each gated by its own
    // threshold and handed to its own no-loss background thread so request logging never blocks the request thread on
    // I/O. Appenders are built and finished through the same DropwizardAppenderFactory dispatch as regular loggers; the
    // Jetty CustomRequestLog pre-formats the line, so each appender emits it raw (logFormat forced to %msg%n).
    int appenderIndex = 0;
    for (Map<String, Object> appenderConfig : activeAppenders) {
      String type = stringProperty(appenderConfig.get("type"));
      // Index the name so multiple appenders of the same type (e.g. two file appenders) get distinct names rather
      // than colliding on loggerName.type (and loggerName.type.async).
      String appenderName = loggerName + "." + type + "." + appenderIndex++;
      DropwizardAppenderConfig config = requestLogAppenderConfig(appenderConfig, type, requestLogFilename);
      if (config == null) {
        continue;
      }
      Appender<ILoggingEvent> appender = DropwizardAppenderFactory.createAppender(loggerContext, appenderName, type,
          config);
      if (appender == null) {
        continue;
      }
      DropwizardAppenderFactory.applyThresholdFilter(appender, config.threshold);
      logger.addAppender(DropwizardAppenderFactory.wrapAsync(
          loggerContext, appender, DropwizardAppenderFactory.asyncSettings(config)));
    }

    if (!logger.iteratorForAppenders().hasNext()) {
      log.warn("Request logging is enabled but no usable appenders were configured (e.g. a file appender with no"
          + " currentLogFilename); request logging is disabled.");
      return null;
    }
    return new LogbackRequestLogWriter(logger);
  }

  /**
   * Converts a raw request-log appender map to the typed config the shared {@link DropwizardAppenderFactory} dispatch
   * expects, applying the request-log specific rule that the message is emitted raw ({@code %msg%n}) since Jetty
   * already formats the line. A file appender keeps its own {@code currentLogFilename} (pre-Spring wrote a file per
   * appender) and only falls back to the single resolved request-log filename when it sets none. Returns {@code null}
   * for an unrecognized type or a file with no filename at all.
   */
  private DropwizardAppenderConfig requestLogAppenderConfig(
      final Map<String, Object> appenderConfig,
      final String type,
      final String requestLogFilename)
  {
    // Strict conversion (fail on unknown keys), matching pre-Spring Dropwizard config parsing and the application
    // logging path, via the shared DropwizardAppenderFactory dispatch so a typo'd/unsupported field is surfaced
    // rather than silently ignored. access-json appenders never reach here - they are handled by the access-json
    // path; udp is recognized by the shared converter but unsupported for request logging, so it is skipped too.
    DropwizardAppenderConfig config = DropwizardAppenderFactory.convertConfig(configSourceReader, type, appenderConfig);
    if (config == null || config instanceof DropwizardAppenderConfig.Udp) {
      return null;
    }
    DropwizardConfigCompat.warnOnDeprecatedFields(config, "request log appender '" + type + "'");
    config.logFormat = REQUEST_LOG_MESSAGE_FORMAT;
    if (config instanceof DropwizardAppenderConfig.File fileConfig) {
      if (fileConfig.currentLogFilename == null || fileConfig.currentLogFilename.isBlank()) {
        fileConfig.currentLogFilename = requestLogFilename;
      }
      if (fileConfig.currentLogFilename == null || fileConfig.currentLogFilename.isBlank()) {
        log.warn("Request log file appender has no currentLogFilename and no request log filename is set;"
            + " skipping file request logging.");
        return null;
      }
    }
    return config;
  }

  CustomRequestLog requestLog(
      final RequestLog.Writer requestLogWriter,
      final UserTelemetryRequestLoggingFilter userTelemetryRequestLoggingFilter,
      final RequestLogSettings requestLogSettings)
  {
    CustomRequestLog requestLog = new CustomRequestLog(requestLogWriter, requestLogSettings.format());
    requestLog.setFilter(
        (request, response) -> !userTelemetryRequestLoggingFilter.shouldSkip(Request.getPathInContext(request)));
    return requestLog;
  }

  RequestLogSettings requestLogSettings(final RequestLogConfig requestLogConfig) {
    List<Map<String, Object>> appenders = requestLogConfig.appenders == null ? List.of() : requestLogConfig.appenders;

    // Pre-Spring honoured whichever request-log factory the configured type selected (InsightConfigurationFactory
    // handled both LogbackClassicRequestLogFactory and the default LogbackAccessRequestLogFactory). Both produce NCSA
    // request logs, so 'classic', 'logback-access' and an unset type all map to this NCSA path; an access-json layout
    // is routed to the access-json path earlier, regardless of type. Any other type (e.g. 'external') is unsupported.
    String requestLogType = requestLogConfig.type;
    if (requestLogType != null && !requestLogType.isBlank()
        && !"classic".equalsIgnoreCase(requestLogType) && !"logback-access".equalsIgnoreCase(requestLogType))
    {
      return RequestLogSettings.disabled();
    }

    if (appenderMaps(appenders).anyMatch(appender -> !isSupportedRequestLogAppender(appender))) {
      log.warn("Request log appenders with unsupported configuration (e.g. filterFactories, or a missing or"
          + " unrecognized type) are not supported. Request logging is disabled."
          + " Correct the entries under server.requestLog.appenders to re-enable request logging.");
      return RequestLogSettings.disabled();
    }
    appenderMaps(appenders)
        .filter(appender -> "udp".equals(stringProperty(appender.get("type"))))
        .findFirst()
        .ifPresent(appender -> log.warn(
            "UDP request-log appenders are not supported and will be ignored"));
    if (resolveActiveAppenders(appenders).isEmpty()) {
      return RequestLogSettings.disabled();
    }

    String timeZone = requestLogConfig.timeZone;
    if (timeZone == null || timeZone.isBlank()) {
      timeZone = DEFAULT_REQUEST_LOG_TIME_ZONE;
    }

    // The Jetty CustomRequestLog format is taken from the first ACTIVE appender that sets a logFormat, regardless of
    // type (the shipped config puts REQUEST_LOG_FORMAT on the console appender); per-appender logFormat is otherwise
    // unused in classic mode. OFF appenders are skipped so a disabled appender cannot dictate the format,
    // consistent with resolveActiveAppenders.
    String configuredFormat = appenderMaps(appenders)
        .filter(appender -> !isOff(appender.get("threshold")))
        .map(appender -> stringProperty(appender.get("logFormat")))
        .filter(format -> format != null && !format.isBlank())
        .findFirst()
        .orElse(null);

    return new RequestLogSettings(true, toJettyRequestLogFormat(configuredFormat, timeZone));
  }

  private Stream<Map<String, Object>> appenderMaps(final List<?> appenders) {
    return appenders.stream()
        .filter(Map.class::isInstance)
        .map(Map.class::cast)
        .map(this::asMap);
  }

  /**
   * The supported, active request-log appenders in config order: console/file/syslog/tcp/tls entries whose threshold
   * is not OFF. Mirrors the pre-Spring-migration behaviour where every configured appender was attached to the request
   * log. access-json appenders (handled by the access-json path) and udp (recognized but unsupported) are excluded.
   */
  private List<Map<String, Object>> resolveActiveAppenders(final List<?> appenders) {
    return appenderMaps(appenders).filter(this::isActiveRequestLogAppender).toList();
  }

  private boolean isActiveRequestLogAppender(final Map<String, Object> appender) {
    return isSupportedRequestLogAppender(appender)
        && !isAccessJsonAppender(appender)
        && !"udp".equals(stringProperty(appender.get("type")))
        && !isOff(appender.get("threshold"));
  }

  private String toJettyRequestLogFormat(final String configuredFormat, final String timeZone) {
    String defaultFormat = DEFAULT_REQUEST_LOG_FORMAT.replace("__TIME_ZONE__", timeZone);
    String legacyOrDefaultFormat = configuredFormat;
    if (legacyOrDefaultFormat == null || legacyOrDefaultFormat.isBlank()) {
      legacyOrDefaultFormat = LEGACY_REQUEST_LOG_FORMAT;
    }

    if (LEGACY_REQUEST_LOG_FORMAT.equals(legacyOrDefaultFormat)) {
      return defaultFormat;
    }

    // %elapsedTime was milliseconds in logback-access; Jetty's %D is microseconds, so map to %{ms}T. Jetty's %t
    // brackets the date itself, so "[%date]" maps to a bare %t (a rare unbracketed %date still gains brackets -
    // Jetty offers no unbracketed time code).
    String jettyDate = "%{dd/MMM/yyyy:HH:mm:ss Z|" + timeZone + "}t";
    // %header{NAME} -> %{NAME}i generically (not just User-Agent): the help docs tell reverse-proxy users to add
    // e.g. %header{REMOTE_USER} or %header{x-forwarded-*}, which logback-access understood natively pre-migration.
    // A quote in NAME means env substitution displaced the brace (the shipped default's failure mode) - leave it
    // unconverted so the leftover %header makes the format invalid and it falls back to the default below.
    String converted = legacyOrDefaultFormat
        .replaceAll("%header\\{([^}\"]+)\\}", "%{$1}i")
        .replace("%clientHost", "%{client}a")
        .replace("%requestURL", "%r")
        .replace("%statusCode", "%s")
        .replace("%elapsedTime", "%{ms}T")
        .replace("%bytesSent", "%O")
        .replace("[%date]", jettyDate)
        .replace("%date", jettyDate)
        .replace("%user", "%u")
        .replaceAll("%l(?![a-zA-Z])", "-");

    // Jetty's CustomRequestLog rejects unknown '%' codes at construction. A configured logFormat can reach here in a
    // form that does not convert cleanly - e.g. the shipped REQUEST_LOG_FORMAT default contains "%header{User-Agent}"
    // and env-var substitution displaces the closing brace, leaving an unconverted "%header" (read as "%h"). Rather
    // than crash the server, fall back to the default format.
    if (!isValidJettyRequestLogFormat(converted)) {
      log.warn("Configured request log format is not a valid Jetty format; using the default request log format."
          + " Configured value: {}", legacyOrDefaultFormat);
      return defaultFormat;
    }
    return converted;
  }

  private boolean isValidJettyRequestLogFormat(final String format) {
    try {
      new CustomRequestLog(message -> {
      }, format);
      return true;
    }
    catch (IllegalArgumentException e) {
      return false;
    }
  }

  private boolean isSupportedRequestLogAppender(final Map<String, Object> appender) {
    String type = stringProperty(appender.get("type"));
    return type != null && SUPPORTED_REQUEST_LOG_TYPES.contains(type)
        && appender.get("filterFactories") == null;
  }

  private boolean isOff(final Object thresholdValue) {
    // thresholdValue is the raw config value: a bare YAML 'OFF' arrives as Boolean false, an explicit one as "OFF".
    return DropwizardAppenderFactory.toLevel(thresholdValue, Level.ALL) == Level.OFF;
  }

  private String stringProperty(final Object value) {
    return value instanceof String ? (String) value : null;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(final Map<?, ?> value) {
    return (Map<String, Object>) value;
  }

  private record LogbackRequestLogWriter(Logger logger)
      implements RequestLog.Writer
  {
    @Override
    public void write(final String requestEntry) {
      logger.info(requestEntry);
    }
  }

  record RequestLogSettings(
      boolean enabled,
      String format)
  {
    static RequestLogSettings disabled() {
      return new RequestLogSettings(false, null);
    }
  }
}
