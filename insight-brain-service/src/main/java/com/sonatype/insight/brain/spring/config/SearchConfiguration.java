/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.spring.config;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.lock.ClusterLockManager;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchMode;
import com.sonatype.insight.brain.search.SearchConfig.AwsHttpOpenSearchConfig;
import com.sonatype.insight.brain.search.SearchConfigSupplier;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.lucene.LuceneSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.AwsSdkHttpClientProvider;
import com.sonatype.insight.brain.search.opensearch.IndexConfigProvider;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.search.opensearch.OpenSearchTransportProvider;
import com.sonatype.insight.brain.search.session.IndexReadSessionFactory;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.inject.Provider;
import java.util.Optional;
import org.opensearch.client.transport.OpenSearchTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;

/**
 * Spring configuration providing search-related beans.
 * <p>
 * Provides the search-related Spring bean definitions.
 * <p>
 * When OpenSearch is configured (via the legacy {@code search:} YAML section), hybrid mode is automatically enabled
 * to provide seamless fallback to Lucene during OpenSearch's initial indexing. The
 * HybridSearchIndexClient uses OpenSearch as the primary search engine and automatically falls
 * back to Lucene if OpenSearch is unavailable or still being indexed.
 * <p>
 * Configuration behavior:
 * <ul>
 * <li>When searchConfig is null: Lucene only mode - SearchIndexClient is bound to LuceneSearchIndexClient</li>
 * <li>When searchConfig is present: Hybrid mode - OpenSearch with Lucene fallback</li>
 * </ul>
 */
@Configuration
public class SearchConfiguration
{

  private static final Logger log = LoggerFactory.getLogger(SearchConfiguration.class);

  /**
   * Provides SearchConfigSupplier as a functional interface that retrieves the SearchConfig
   * from the InsightConfig.
   */
  @Bean
  public SearchConfigSupplier searchConfigSupplier(InsightConfig insightConfig) {
    return insightConfig::getSearchConfig;
  }

  // =========================================================================
  // Lucene Mode (when searchConfig is not configured)
  // =========================================================================

  /**
   * LuceneSearchIndexClient bean.
   * <p>
   * In Lucene-only mode, this is the main SearchIndexClient.
   * In hybrid mode, this serves as the secondary (fallback) client.
   * <p>
   * All constructor parameters are autowired by Spring from the application context.
   */
  /**
   * Implements {@link com.sonatype.insight.brain.tenancy.TenantManaged}. Spring injects every bean of
   * that type into {@code Set<TenantManaged>} for
   * {@link com.sonatype.insight.brain.service.DefaultTenantManagedInitializer} /
   * {@code TenantManager}, so {@code register}/{@code deregister} run on tenant lifecycle.
   */
  @Bean
  public LuceneIndexWriterOwner luceneIndexWriterOwner(
      LuceneComponents luceneComponents,
      ShutdownHandler shutdownHandler,
      @Autowired(required = false) MeterRegistry meterRegistry)
  {
    log.debug("Creating LuceneIndexWriterOwner");
    return new LuceneIndexWriterOwner(luceneComponents, shutdownHandler, meterRegistry);
  }

  /**
   * Implements {@link com.sonatype.insight.brain.tenancy.TenantManaged}. Same Spring
   * {@code Set<TenantManaged>} wiring as {@link #luceneIndexWriterOwner} so per-tenant authz cache
   * state is cleared on deregister.
   */
  @Bean
  public ReadableContextAuthzCache readableContextAuthzCache(
      AuthorizationChecker authorizationChecker,
      PermissionService permissionService,
      OwnerDAO ownerDAO)
  {
    log.debug("Creating ReadableContextAuthzCache");
    return new ReadableContextAuthzCache(authorizationChecker, permissionService, ownerDAO);
  }

  @Bean
  public IndexReadSessionFactory indexReadSessionFactory(
      LuceneIndexWriterOwner luceneIndexWriterOwner,
      CurrentUser currentUser,
      ReadableContextAuthzCache readableContextAuthzCache,
      @Autowired(required = false) OpenSearchSearchIndexClient openSearchSearchIndexClient,
      @Autowired(required = false) SearchConfig searchConfig)
  {
    log.debug("Creating IndexReadSessionFactory");
    return IndexReadSessionFactory.forProduction(
        luceneIndexWriterOwner,
        currentUser,
        readableContextAuthzCache,
        openSearchSearchIndexClient,
        searchConfig);
  }

  @Bean
  public LuceneSearchIndexClient luceneSearchIndexClient(
      ApplicationDAO applicationDAO,
      LabelDAO labelDAO,
      OrganizationDAO organizationDAO,
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      TagDAO tagDAO,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      DocumentBuilderHelper documentBuilderHelper,
      ProductLicense productLicense,
      TelemetrySender telemetrySender,
      SearchIndexChangeDAO searchIndexChangeDAO,
      LuceneComponents luceneComponents,
      LuceneIndexWriterOwner luceneIndexWriterOwner,
      InsightWork insightWork,
      AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      com.sonatype.insight.brain.service.Configuration configuration,
      PermissionService permissionService,
      AuthorizationChecker authorizationChecker,
      CurrentUser currentUser,
      ConversionHelper conversionHelper,
      ShutdownHandler shutdownHandler,
      ReadableContextAuthzCache readableContextAuthzCache)
  {
    log.debug("Creating LuceneSearchIndexClient");
    return new LuceneSearchIndexClient(
        applicationDAO,
        labelDAO,
        organizationDAO,
        ownerDAO,
        policyDAO,
        policyWaiverDAO,
        autoPolicyWaiverDAO,
        tagDAO,
        thirdPartySbomMetadataDAO,
        documentBuilderHelper,
        productLicense,
        telemetrySender,
        searchIndexChangeDAO,
        luceneComponents,
        luceneIndexWriterOwner,
        insightWork,
        advancedSearchTelemetryMetrics,
        configuration,
        permissionService,
        authorizationChecker,
        currentUser,
        conversionHelper,
        shutdownHandler,
        readableContextAuthzCache);
  }

  /**
   * SearchIndexClient bean.
   * <p>
   * Respects SearchMode: LUCENE returns Lucene-only, OPENSEARCH returns OpenSearch-only,
   * HYBRID (default when SearchConfig is present) returns HybridSearchIndexClient.
   * When no SearchConfig is present at all, defaults to Lucene-only.
   */
  @Bean(name = "searchIndexClient")
  @Primary
  public SearchIndexClient searchIndexClient(
      LuceneSearchIndexClient luceneSearchIndexClient,
      @Autowired(required = false) OpenSearchSearchIndexClient openSearchSearchIndexClient,
      @Autowired(required = false) SearchConfig searchConfig)
  {
    if (openSearchSearchIndexClient == null || searchConfig == null) {
      log.debug("Using Lucene search (no OpenSearch configuration)");
      return luceneSearchIndexClient;
    }
    SearchMode mode = searchConfig.getMode();
    if (mode == SearchMode.OPENSEARCH) {
      log.debug("Using OpenSearch search");
      return openSearchSearchIndexClient;
    }
    if (mode == SearchMode.LUCENE) {
      log.debug("Using Lucene search (explicit mode)");
      return luceneSearchIndexClient;
    }
    // Default: HYBRID
    log.debug("Using hybrid search with OpenSearch primary and Lucene fallback");
    return new HybridSearchIndexClient(openSearchSearchIndexClient, luceneSearchIndexClient);
  }

  // =========================================================================
  // Hybrid Mode (when searchConfig is configured)
  // =========================================================================

  /**
   * SearchConfig bean - only created when the legacy {@code search:} YAML section is configured.
   * <p>
   * This bean enables hybrid mode by triggering conditional bean creation.
   */
  @Bean
  @ConditionalOnProperty(prefix = "search", name = "type")
  public SearchConfig searchConfig(SearchConfigSupplier searchConfigSupplier) {
    SearchConfig searchConfig = searchConfigSupplier.getSearchConfig();
    // Validate configuration early to fail fast on startup
    searchConfig.validate();
    log.debug("OpenSearch configured - using hybrid mode with automatic Lucene fallback");
    return searchConfig;
  }

  /**
   * OpenSearchSearchIndexClient bean for hybrid mode.
   * <p>
   * This bean is only created when SearchConfig is present (hybrid mode).
   * All constructor parameters are autowired by Spring from the application context.
   */
  @Bean
  @ConditionalOnBean(SearchConfig.class)
  public OpenSearchSearchIndexClient openSearchSearchIndexClient(
      ApplicationDAO applicationDAO,
      LabelDAO labelDAO,
      OrganizationDAO organizationDAO,
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      PolicyWaiverDAO policyWaiverDAO,
      AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      SearchIndexChangeDAO searchIndexChangeDAO,
      TagDAO tagDAO,
      ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      DocumentBuilderHelper documentBuilderHelper,
      ProductLicense productLicense,
      TelemetrySender telemetrySender,
      LuceneComponents luceneComponents,
      AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      com.sonatype.insight.brain.service.Configuration configuration,
      PermissionService permissionService,
      AuthorizationChecker authorizationChecker,
      CurrentUser currentUser,
      ConversionHelper conversionHelper,
      OpenSearchTransport openSearchTransport,
      IndexConfigProvider indexConfigProvider,
      ClusterLockManager clusterLockManager,
      SearchConfig searchConfig,
      ShutdownHandler shutdownHandler,
      ReadableContextAuthzCache readableContextAuthzCache)
  {
    log.debug("Creating OpenSearchSearchIndexClient");
    return new OpenSearchSearchIndexClient(
        applicationDAO,
        labelDAO,
        organizationDAO,
        ownerDAO,
        policyDAO,
        policyWaiverDAO,
        autoPolicyWaiverDAO,
        searchIndexChangeDAO,
        tagDAO,
        thirdPartySbomMetadataDAO,
        documentBuilderHelper,
        productLicense,
        telemetrySender,
        luceneComponents,
        advancedSearchTelemetryMetrics,
        configuration,
        permissionService,
        authorizationChecker,
        currentUser,
        conversionHelper,
        openSearchTransport,
        indexConfigProvider,
        clusterLockManager,
        searchConfig,
        shutdownHandler,
        readableContextAuthzCache);
  }

  // =========================================================================
  // OpenSearch Transport Configuration (for hybrid mode)
  // =========================================================================

  /**
   * SdkHttpClient bean for AWS OpenSearch configurations.
   * <p>
   * <strong>IMPORTANT:</strong> This SdkHttpClient is exclusively for OpenSearch use.
   * It should not be injected by other components. Its lifecycle is managed by
   * OpenSearchTransportProvider.
   * <p>
   * This bean is only created when the SearchConfig is an AwsHttpOpenSearchConfig.
   */
  @Bean(name = "openSearchSdkHttpClient")
  @ConditionalOnBean(SearchConfig.class)
  public SdkHttpClient sdkHttpClient(SearchConfig searchConfig) {
    if (searchConfig instanceof AwsHttpOpenSearchConfig) {
      log.debug("Creating SdkHttpClient for AWS OpenSearch");
      AwsSdkHttpClientProvider provider = new AwsSdkHttpClientProvider(searchConfig);
      return provider.get();
    }
    // Return null for non-AWS configurations
    return null;
  }

  /**
   * OpenSearchTransport bean for OpenSearch connectivity.
   * <p>
   * This is created as a singleton to ensure a single instance is reused,
   * which is critical for AWS OpenSearch to maintain consistent credential signing.
   * <p>
   * The SdkHttpClient is only required for AWS configurations and is injected optionally.
   */
  @Bean
  @ConditionalOnBean(SearchConfig.class)
  public OpenSearchTransport openSearchTransport(
      SearchConfig searchConfig,
      AwsCredentialsProvider awsCredentialsProvider,
      ShutdownHandler shutdownHandler,
      @Autowired(required = false) SdkHttpClient sdkHttpClient)
  {

    // Create optional provider for SdkHttpClient
    Optional<Provider<SdkHttpClient>> sdkHttpClientProvider = sdkHttpClient != null
        ? Optional.of(() -> sdkHttpClient)
        : Optional.empty();

    OpenSearchTransportProvider provider = new OpenSearchTransportProvider(
        searchConfig,
        awsCredentialsProvider,
        sdkHttpClientProvider,
        shutdownHandler);

    return provider.get();
  }
}
