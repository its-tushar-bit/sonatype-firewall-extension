/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import ch.qos.logback.classic.Logger;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.codahale.metrics.jvm.BufferPoolMetricSet;
import com.codahale.metrics.jvm.GarbageCollectorMetricSet;
import com.codahale.metrics.jvm.MemoryUsageGaugeSet;
import com.codahale.metrics.jvm.ThreadDeadlockDetector;
import com.codahale.metrics.jvm.ThreadStatesGaugeSet;
import com.codahale.metrics.logback.InstrumentedAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.sonatype.insight.brain.api.v2.service.ConfigurationUtils;
import io.dropwizard.jackson.AnnotationSensitivePropertyNamingStrategy;
import io.dropwizard.jackson.DiscoverableSubtypeResolver;
import io.dropwizard.jackson.GuavaExtrasModule;
import com.sonatype.insight.brain.aws.credentials.DefaultInsightAwsCredentialProvider;
import com.sonatype.insight.brain.aws.s3.S3AsyncClientProvider;
import com.sonatype.insight.brain.aws.s3.S3ClientProvider;
import com.sonatype.insight.brain.dashboard.ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.DashboardComponentRiskService;
import com.sonatype.insight.brain.dashboard.DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.H2ApplicationRiskService;
import com.sonatype.insight.brain.dashboard.H2ComponentRiskService;
import com.sonatype.insight.brain.dashboard.H2DashboardViolationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresApplicationRiskService;
import com.sonatype.insight.brain.dashboard.PostgresComponentRiskService;
import com.sonatype.insight.brain.dashboard.PostgresDashboardViolationRiskService;
import com.sonatype.insight.brain.dataaccess.ComponentCategoryDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.HashComponentIdentifierDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.dataaccess.vulnerability.VulnerabilityGroupDAO;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import io.dropwizard.metrics.jetty12.AbstractInstrumentedHandler;
import com.sonatype.insight.brain.eventbus.AsyncEventBus;
import com.sonatype.insight.brain.eventbus.AsyncEventBusImpl;
import com.sonatype.insight.brain.hds.ComponentDetailsLoader;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.conditions.*;
import com.sonatype.insight.brain.model.policy.conditions.valuetype.ConditionValueTypes;
import com.sonatype.insight.brain.security.CipherFactory;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.shutdown.ShutdownPriority;
import com.sonatype.insight.scan.anon.Anonymizer;
import com.sonatype.insight.scan.client.ClientScanner;
import com.sonatype.insight.scan.config.ScanPropertiesLoader;
import com.sonatype.insight.scan.file.FileScanner;
import com.sonatype.insight.scan.hash.Digester;
import com.sonatype.insight.scan.hash.internal.DefaultDigester;
import com.sonatype.insight.scan.hash.internal.JavaDigester;
import com.sonatype.insight.scan.model.io.ScanWriter;
import com.sonatype.insight.scan.model.io.ScanWriterFactory;
import com.sonatype.nexus.git.utils.VersionRemediationTitleGenerator;
import com.sonatype.nexus.iq.location.discovery.LocationDiscoveryExecutor;
import com.sonatype.nexus.iq.location.discovery.PositionDiscoveryExecutor;
import com.sonatype.nexus.iq.manager.PullRequestExecutor;
import com.sonatype.nexus.scm.GitApiClientFactory;
import com.sonatype.insight.brain.operational.check.AbstractOperationalCheck;
import java.lang.management.ManagementFactory;
import java.util.Set;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Response;
import org.slf4j.LoggerFactory;
import org.sonatype.plexus.components.cipher.PlexusCipher;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.jetty.JettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * Core Spring configuration - DataSource, ObjectMapper, Metrics.
 */
@Configuration
public class CoreConfiguration
{

  @Bean
  @Primary
  public MetricRegistry metricRegistry() {
    MetricRegistry registry = new MetricRegistry();
    registry.register("jvm.buffers", new BufferPoolMetricSet(ManagementFactory.getPlatformMBeanServer()));
    registry.register("jvm.gc", new GarbageCollectorMetricSet());
    registry.register("jvm.memory", new MemoryUsageGaugeSet());
    registry.register("jvm.threads", new ThreadStatesGaugeSet());

    InstrumentedAppender logbackMetrics = new InstrumentedAppender(registry);
    Logger rootLogger = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
    logbackMetrics.setContext(rootLogger.getLoggerContext());
    logbackMetrics.start();
    rootLogger.addAppender(logbackMetrics);

    return registry;
  }

  @Bean
  public HealthCheckRegistry healthCheckRegistry(Set<AbstractOperationalCheck> operationalChecks) {
    HealthCheckRegistry registry = new HealthCheckRegistry();
    for (AbstractOperationalCheck check : operationalChecks) {
      registry.register(check.getName(), new HealthCheck()
      {
        @Override
        protected Result check() throws Exception {
          Health health = check.check();
          ResultBuilder builder = health.getStatus() == Status.UP
              ? Result.builder().healthy()
              : Result.builder().unhealthy();
          health.getDetails().forEach(builder::withDetail);
          return builder.build();
        }
      });
    }
    registry.register("deadlocks", new HealthCheck()
    {
      private final ThreadDeadlockDetector detector = new ThreadDeadlockDetector();

      @Override
      protected Result check() {
        Set<String> deadlocks = detector.getDeadlockedThreads();
        if (deadlocks.isEmpty()) {
          return Result.healthy();
        }
        return Result.unhealthy(String.join(System.lineSeparator(), deadlocks));
      }
    });
    return registry;
  }

  @Bean
  public JettyServerCustomizer instrumentedHandlerCustomizer(MetricRegistry metricRegistry) {
    return server -> {
      Handler original = server.getHandler();
      AbstractInstrumentedHandler instrumented = new AbstractInstrumentedHandler(metricRegistry)
      {
        @Override
        protected void setupServletListeners(Request request, Response response) {
        }

        @Override
        protected boolean isSuspended(Request request, Response response) {
          return false;
        }
      };
      instrumented.setHandler(original);
      server.setHandler(instrumented);
    };
  }

  @Bean
  @Primary
  public ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    configureMapper(mapper);
    return mapper;
  }

  /**
   * Creates the AsyncEventBus for event-driven communication.
   */
  @Bean
  public AsyncEventBus asyncEventBus(
      com.sonatype.insight.brain.service.Configuration configuration,
      ShutdownHandler shutdownHandler)
  {
    AsyncEventBusImpl eventBus = new AsyncEventBusImpl(configuration.getEventBusMaxThreadPoolSize());
    shutdownHandler.add(eventBus.getThreadPoolExecutor(), ShutdownPriority.ASYNC_EVENT_BUS);
    return eventBus;
  }

  /**
   * Provides the GitApiClientFactory from the nexus-scm library.
   */
  @Bean
  public GitApiClientFactory gitApiClientFactory() {
    return new GitApiClientFactory();
  }

  @Bean
  public PlexusCipher plexusCipher() {
    return CipherFactory.createCipher();
  }

  /**
   * Provides external library beans required by integration services.
   */
  @Bean
  public PullRequestExecutor pullRequestExecutor() {
    return new PullRequestExecutor();
  }

  @Bean
  public VersionRemediationTitleGenerator versionRemediationTitleGenerator() {
    return new VersionRemediationTitleGenerator();
  }

  @Bean
  public LocationDiscoveryExecutor locationDiscoveryExecutor() {
    return new LocationDiscoveryExecutor();
  }

  @Bean
  public PositionDiscoveryExecutor positionDiscoveryExecutor() {
    return new PositionDiscoveryExecutor();
  }

  /**
   * Provides ScanPropertiesLoader from the insight-scanner library.
   */
  @Bean
  public ScanPropertiesLoader scanPropertiesLoader() {
    return new ScanPropertiesLoader();
  }

  /**
   * ScannerModule-compatible beans from the insight-scanner library.
   */
  @Bean
  public JavaDigester javaDigester() {
    return new JavaDigester();
  }

  @Bean
  public Digester digester(JavaDigester javaDigester) {
    return new DefaultDigester(javaDigester);
  }

  @Bean
  public ClientScanner clientScanner() {
    return new ClientScanner(LoggerFactory.getLogger(ClientScanner.class));
  }

  @Bean
  public FileScanner fileScanner(JavaDigester javaDigester) {
    return new FileScanner(
        new DefaultDigester(javaDigester, LoggerFactory.getLogger(DefaultDigester.class)),
        new Anonymizer(),
        LoggerFactory.getLogger(FileScanner.class));
  }

  @Bean
  public ScanWriterFactory scanWriterFactory() {
    return new ScanWriterFactory(LoggerFactory.getLogger(ScanWriter.class));
  }

  /**
   * Provides AwsCredentialsProvider from AWS SDK.
   */
  @Bean
  public AwsCredentialsProvider awsCredentialsProvider(
      DefaultInsightAwsCredentialProvider provider)
  {
    return provider.get();
  }

  @Bean
  public S3Client s3Client(final S3ClientProvider provider) {
    return provider.get();
  }

  @Bean
  public S3AsyncClient s3AsyncClient(final S3AsyncClientProvider provider) {
    return provider.get();
  }

  /**
   * Provides CsvMapper for CSV export functionality.
   */
  @Bean
  public CsvMapper csvMapper() {
    CsvMapper csvMapper = new CsvMapper();
    configureMapper(csvMapper);
    return csvMapper;
  }

  /**
   * Registers Jackson modules and defaults that were previously configured by Dropwizard's
   * {@code Jackson.newObjectMapper()} / {@code InsightBrainService.configureObjectMapper()}.
   * Ensures consistent serialization of Guava types, Joda/JSR-310 dates, JDK8 Optionals,
   * constructor-parameter name resolution, annotation-sensitive naming, and discoverable subtypes.
   */
  private static void configureMapper(ObjectMapper mapper) {
    mapper.registerModule(new GuavaModule());
    mapper.registerModule(new GuavaExtrasModule());
    mapper.registerModule(new JodaModule());
    mapper.registerModule(new ParameterNamesModule());
    mapper.registerModule(new Jdk8Module());
    mapper.registerModule(new JavaTimeModule());
    mapper.setPropertyNamingStrategy(new AnnotationSensitivePropertyNamingStrategy());
    mapper.setSubtypeResolver(new DiscoverableSubtypeResolver());
  }

  @Bean
  @Primary
  public DashboardViolationRiskService dashboardViolationRiskService(
      final OperationalDataStore operationalDataStore,
      final H2DashboardViolationRiskService h2DashboardViolationRiskService,
      final PostgresDashboardViolationRiskService postgresDashboardViolationRiskService)
  {
    return isEmbeddedDatabase(operationalDataStore)
        ? h2DashboardViolationRiskService
        : postgresDashboardViolationRiskService;
  }

  @Bean
  @Primary
  public DashboardComponentRiskService dashboardComponentRiskService(
      final OperationalDataStore operationalDataStore,
      final H2ComponentRiskService h2ComponentRiskService,
      final PostgresComponentRiskService postgresComponentRiskService)
  {
    return isEmbeddedDatabase(operationalDataStore)
        ? h2ComponentRiskService
        : postgresComponentRiskService;
  }

  @Bean
  @Primary
  public ApplicationRiskService applicationRiskService(
      final OperationalDataStore operationalDataStore,
      final H2ApplicationRiskService h2ApplicationRiskService,
      final PostgresApplicationRiskService postgresApplicationRiskService)
  {
    return isEmbeddedDatabase(operationalDataStore)
        ? h2ApplicationRiskService
        : postgresApplicationRiskService;
  }

  private static boolean isEmbeddedDatabase(final OperationalDataStore operationalDataStore) {
    return DatabaseUtil.isDatabaseEmbedded(operationalDataStore.getDatabaseConfig());
  }

  /**
   * Initializes the remaining static collaborators that still require explicit bootstrap wiring.
   *
   * Using @DependsOn ensures the database-backed DAOs are ready before we initialize these statics.
   */
  @Bean
  @DependsOn("databaseContainer")
  public StaticInjectionInitializer staticInjectionInitializer(
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      ComponentCategoryDAO componentCategoryDAO,
      LicenseDAO licenseDAO,
      OwnerDAO ownerDAO,
      LicenseThreatGroupDAO licenseThreatGroupDAO,
      LabelDAO labelDAO,
      VulnerabilityGroupDAO vulnerabilityGroupDAO,
      RepositoryDAO repositoryDAO,
      ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      HashComponentIdentifierDAO hashComponentIdentifierDAO,
      MultiLicenseDAO multiLicenseDAO,
      ExecutorThreadPools executorThreadPools)
  {
    ExecutorThreadPools.injectInstance(executorThreadPools);
    SystemConfigurationPropertyFeature.injectDependencies(systemConfigurationPropertyDAO);

    ConditionTypes.injectConditionTypes(
        new AgeInDaysConditionType(),
        new CoordinatesConditionType(),
        new ComponentFormatConditionType(),
        new PackageUrlConditionType(),
        new LabelConditionType(labelDAO),
        new LicenseConditionType(licenseDAO),
        new LicenseStatusConditionType(),
        new LicenseThreatGroupConditionType(licenseThreatGroupDAO, licenseDAO, ownerDAO),
        new LicenseThreatGroupLevelConditionType(),
        new RelativePopularityConditionType(),
        new MatchStateConditionType(),
        new DeprecatedSecurityVulnerabilityConditionType(),
        new SecurityVulnerabilitySeverityConditionType(),
        new SecurityVulnerabilityStatusConditionType(),
        new SecurityVulnerabilitySourceConditionType(systemConfigurationPropertyDAO),
        new SecurityVulnerabilityResearchConditionType(),
        new ProprietaryConditionType(),
        new ProprietaryNameConflictConditionType(repositoryDAO),
        new IdentificationSourceConditionType(),
        new ComponentCategoryConditionType(componentCategoryDAO),
        new HygieneRatingConditionType(),
        new IntegrityRatingConditionType(),
        new DataSourceConditionType(),
        new DependencyTypeConditionType(),
        new SecurityVulnerabilityCategoryConditionType(),
        new SecurityVulnerabilityCweConditionType(),
        new SecurityVulnerabilityCustomRemediationConditionType(),
        new IacControlConditionType(thirdPartyVulnerabilityDAO),
        new VulnerabilityGroupConditionType(vulnerabilityGroupDAO, ownerDAO),
        new SecurityVulnerabilityCustomCVSSVectorStringConditionType(),
        new ComponentEndOfLifeConditionType(),
        new DerivativeAiModelConditionType(),
        new AiModelContentConditionType(),
        new SecurityVulnerabilityDetectionConditionType(),
        new KevStatusConditionType(),
        new SecurityVulnerabilityEpssScoreConditionType());

    ConditionValueTypes.injectConditionValueTypes(
        componentCategoryDAO,
        licenseDAO,
        ownerDAO,
        licenseThreatGroupDAO,
        labelDAO,
        vulnerabilityGroupDAO);

    ComponentDetailsLoader.inject(hashComponentIdentifierDAO, multiLicenseDAO);
    ConfigurationUtils.injectDependencies(systemConfigurationPropertyDAO);

    LoggerFactory.getLogger(CoreConfiguration.class).info("Legacy static injection completed");
    return new StaticInjectionInitializer();
  }

  /**
   * Marker class for static injection initialization.
   */
  public static class StaticInjectionInitializer
  {
    // Marker class for static injection
  }
}
