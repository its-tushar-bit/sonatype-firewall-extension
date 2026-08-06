/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.global.GlobalSearchSortAllowlist;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.lucene.LuceneRbacFilterQueryBuilder;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.model.security.UserPrincipal;
import com.sonatype.insight.brain.search.session.ReadableContextAuthzCache;
import com.sonatype.insight.brain.security.AuthorizationChecker;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.MatchNoDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermInSetQuery;
import org.apache.lucene.util.BytesRef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.*;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.index.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY_WAIVER;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;

public abstract class AbstractSearchIndexClient
    implements SearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(AbstractSearchIndexClient.class);

  public static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX = "AdvancedSearch.createSearchIndex";

  protected static final String NO_INDEX_ERROR_MESSAGE =
      "Search index not found. The Advanced Search index is unavailable or has not been created yet. " +
          "Re-indexing is required before results can be returned.";

  private static final int INDEX_THREADS_MIN = 1;

  private static final int INDEX_THREADS_MAX = Integer.MAX_VALUE;

  private static final int INDEX_THREADS_DEFAULT = 1;

  private static final int QUEUE_POP_AMOUNT = 64_000;

  private static final int MAX_CONSECUTIVE_FAILURES = 5;

  private static final int MAX_CHANGE_FAILURES = 3;

  private static final Duration MAX_COOLDOWN = Duration.ofMinutes(10);

  private static final Duration INITIAL_COOLDOWN = Duration.ofSeconds(30);

  private final TenantReference<AtomicLong> lastRecordedExceptionEpochMs =
      new TenantReference<>(() -> new AtomicLong(0));

  private final TenantReference<AtomicReference<Duration>> currentCooldown =
      new TenantReference<>(() -> new AtomicReference<>(INITIAL_COOLDOWN));

  private final TenantReference<Cache<String, Integer>> changeFailureCounts =
      new TenantReference<>(() -> CacheBuilder.newBuilder()
          .expireAfterWrite(1, TimeUnit.HOURS)
          .maximumSize(10_000)
          .build());

  public static final BadRequestException TOO_MANY_CLAUSES_EXCEPTION =
      new BadRequestException("Error performing search due to too many clauses. " +
          "Please try narrowing down the query as much as possible " +
          "and consider updating Advanced Search configuration to support larger queries.");

  /**
   * Total-hits cap for Global Search. Lucene truncates collected hits and OpenSearch passes this
   * through as {@code track_total_hits}, so both backends report the same upper bound.
   */
  public static final int GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP = 10_000;

  private final ApplicationDAO applicationDAO;

  private final LabelDAO labelDAO;

  private final OrganizationDAO organizationDAO;

  protected final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final SearchIndexChangeDAO searchIndexChangeDAO;

  private final TagDAO tagDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  protected final DocumentBuilderHelper documentBuilderHelper;

  private final ProductLicense productLicense;

  private final TelemetrySender telemetrySender;

  protected final LuceneComponents luceneComponents;

  private final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics;

  protected final Configuration configuration;

  private final PermissionService permissionService;

  private final AuthorizationChecker authorizationChecker;

  private final CurrentUser currentUser;

  private final ReadableContextAuthzCache readableContextAuthzCache;

  protected final ConversionHelper conversionHelper;

  protected final TenantReference<TenantThreadPoolExecutor> indexingExecutors;

  private final ShutdownHandler shutdownHandler;

  public AbstractSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final LuceneComponents luceneComponents,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler)
  {
    this(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO,
        searchIndexChangeDAO,
        tagDAO, thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, authorizationChecker, currentUser,
        conversionHelper, shutdownHandler, null);
  }

  protected AbstractSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final LuceneComponents luceneComponents,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final AuthorizationChecker authorizationChecker,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler,
      final ReadableContextAuthzCache readableContextAuthzCache)
  {
    this.applicationDAO = applicationDAO;
    this.labelDAO = labelDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.searchIndexChangeDAO = searchIndexChangeDAO;
    this.tagDAO = tagDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.documentBuilderHelper = documentBuilderHelper;
    this.productLicense = productLicense;
    this.telemetrySender = telemetrySender;
    this.luceneComponents = luceneComponents;
    this.advancedSearchTelemetryMetrics = advancedSearchTelemetryMetrics;
    this.configuration = configuration;
    this.permissionService = permissionService;
    this.authorizationChecker = authorizationChecker;
    this.currentUser = currentUser;
    this.readableContextAuthzCache = readableContextAuthzCache;
    this.conversionHelper = conversionHelper;
    this.indexingExecutors = new TenantReference<>();
    this.shutdownHandler = shutdownHandler;
  }

  /**
   * @return a multimap mapping each organization to all of its ancestor orgs, in order
   */
  protected ListMultimap<Organization, Organization> computeParentsByOrganization(
      final Map<String, Organization> organizationsById)
  {
    // Note: the value in this map can be null (e.g. with the Root Org). Collectors.toMap doesn't allow
    // null values, hence the for loop
    Map<String, Organization> immediateParentMap = new HashMap<>();
    for (Organization organization : organizationsById.values()) {
      immediateParentMap.put(organization.getId(), organizationsById.get(organization.getParentOrganizationId()));
    }

    ListMultimap<Organization, Organization> retval = ArrayListMultimap.create(organizationsById.size(), 3);
    for (Organization org : organizationsById.values()) {
      Organization current = org;

      while (current != null) {
        retval.put(org, current);
        current = immediateParentMap.get(current.getId());
      }
    }

    return retval;
  }

  protected Map<String, String> getGroupFieldNamesByItemType(final Set<String> fieldNames) {
    Map<String, String> groupFieldNamesByItemType = new HashMap<>();
    for (ItemType itemType : ItemType.values()) {
      groupFieldNamesByItemType.put(itemType.name(), getGroupFieldName(itemType, fieldNames).label);
    }
    return groupFieldNamesByItemType;
  }

  protected FieldIdentifier getGroupFieldName(final ItemType itemType, final Set<String> fieldNames) {
    // pick a field that is available for the item type, potentially driven by the fields searched on
    switch (itemType) {
      case APPLICATION:
      case SBOM_METADATA:
        return APPLICATION_NAME;
      case APPLICATION_CATEGORY:
        return APPLICATION_CATEGORY_NAME;
      case COMPONENT_LABEL:
        return COMPONENT_LABEL_NAME;
      case ORGANIZATION:
        return ORGANIZATION_NAME;
      case POLICY:
        return POLICY_NAME;
      case SECURITY_VULNERABILITY:
        if (Stream.of(VULNERABILITY_ID, VULNERABILITY_DESCRIPTION, VULNERABILITY_SEVERITY, VULNERABILITY_STATUS)
            .anyMatch(field -> fieldNames.contains(field.label)))
        {
          return VULNERABILITY_ID;
        }
        if (Stream.of(COMPONENT_FORMAT, COMPONENT_HASH, COMPONENT_NAME)
            .anyMatch(field -> fieldNames.contains(field.label))
            || fieldNames.stream().anyMatch(fieldName -> fieldName.startsWith(COMPONENT_COORDINATE.label)))
        {
          return COMPONENT_NAME;
        }
        return APPLICATION_NAME;
      case NON_VULNERABLE_COMPONENT:
        return COMPONENT_NAME;
      case POLICY_VIOLATION:
        if (Stream.of(POLICY_VIOLATION_ID, POLICY_VIOLATION_POLICY_NAME, POLICY_VIOLATION_POLICY_ID,
            POLICY_VIOLATION_THREAT_CATEGORY, POLICY_VIOLATION_THREAT_LEVEL, POLICY_VIOLATION_WAIVER_STATUS,
            POLICY_VIOLATION_CONSTRAINT_NAME)
            .anyMatch(field -> fieldNames.contains(field.label)))
        {
          return POLICY_VIOLATION_POLICY_NAME;
        }
        if (Stream.of(COMPONENT_FORMAT, COMPONENT_HASH, COMPONENT_NAME)
            .anyMatch(field -> fieldNames.contains(field.label))
            || fieldNames.stream().anyMatch(fieldName -> fieldName.startsWith(COMPONENT_COORDINATE.label)))
        {
          return COMPONENT_NAME;
        }
        return APPLICATION_NAME;
      case LEGAL_VIOLATION:
        if (Stream.of(COMPONENT_EFFECTIVE_LICENSE_ID, COMPONENT_EFFECTIVE_LICENSE_NAME,
            COMPONENT_LICENSE_THREAT_GROUP_NAME, COMPONENT_LICENSE_THREAT_LEVEL)
            .anyMatch(field -> fieldNames.contains(field.label)))
        {
          return COMPONENT_EFFECTIVE_LICENSE_ID;
        }
        if (Stream.of(COMPONENT_FORMAT, COMPONENT_HASH, COMPONENT_NAME)
            .anyMatch(field -> fieldNames.contains(field.label))
            || fieldNames.stream().anyMatch(fieldName -> fieldName.startsWith(COMPONENT_COORDINATE.label)))
        {
          return COMPONENT_NAME;
        }
        return APPLICATION_NAME;
      case POLICY_WAIVER:
      case POLICY_WAIVER_REQUEST:
        return POLICY_WAIVER_POLICY_NAME;
      default:
        throw new IllegalArgumentException("Unsupported item type " + itemType);
    }
  }

  protected FieldIdentifier getFieldIdentifier(final String fieldName) {
    FieldIdentifier identifier;
    if (fieldName.startsWith(COMPONENT_COORDINATE.label)) {
      identifier = COMPONENT_COORDINATE;
    }
    else {
      identifier = Arrays.stream(FieldIdentifier.values())
          .filter(fieldIdentifier -> fieldIdentifier.label.equals(fieldName))
          .findAny()
          .orElse(null);
    }
    return identifier;
  }

  protected Map<String, OwnerType> getChildContextIds(final Set<String> contextIdsWithReadPermission) {
    return ownerDAO.expandReadableContexts(contextIdsWithReadPermission);
  }

  @Override
  public void checkGlobalSearchMode(final boolean isSbomManagerMode) {
    checkMode(isSbomManagerMode);
  }

  protected void checkMode(final boolean isSbomManagerMode) {
    if (isSbomManagerMode && !productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)) {
      log.error("License does not have the SBOM Manager feature.");
      throw new InvalidLicenseException("The SBOM Manager feature is not supported by your license.");
    }
    if (!isSbomManagerMode && productLicense.hasFeature(LicensedFeature.SBOM_MANAGER) &&
        !hasProductSupportingDefaultMode())
    {
      log.error("License does not support anything other than SBOM Manager mode.");
      throw new InvalidLicenseException("Only SBOM Manager mode is supported by your license.");
    }
  }

  // TODO possibly add a LicensedFeature.ADVANCED_SEARCH to replace this
  private boolean hasProductSupportingDefaultMode() {
    // Auditor
    return productLicense.hasProduct(ProductLicenseDetails.PRODUCT_RISK)
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_AUDITOR_SAAS)
        // Lifecycle
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_RISK_AND_REMEDIATION)
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_LIFECYCLE_SAAS)
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_LIFECYCLE_CLOUD)
        // Foundation
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_FOUNDATION)
        || productLicense.hasProduct(ProductLicenseDetails.PRODUCT_LIFECYCLE_FOUNDATION_SAAS);
  }

  /** Bounded audit description of the sort — field names only, never the unbounded Sort.toString(). */
  protected static String describeSort(final Sort sort) {
    if (sort == null) {
      return "relevance";
    }
    List<String> fields = new ArrayList<>();
    for (SortField sf : sort.getSort()) {
      // SortField.FIELD_SCORE has a null field name; render it as the relevance sort token.
      fields.add(sf.getField() == null ? GlobalSearchSortAllowlist.RELEVANCE : sf.getField());
    }
    return String.join(",", fields);
  }

  protected void sendAdvancedSearchIndexingTelemetry(final long durationMillis) {
    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_DURATION_SECONDS, durationMillis / 1000);
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_SIZE_BYTES, getIndexSize());
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_REINDEX, true);
    telemetrySender.send(telemetryData);
  }

  protected void updateIndex(final SearchIndexChange change, final IndexingContext indexingContext) throws IOException {
    switch (change.getChangeType()) {
      case APPLICATION:
        updateIndexForApplication(change.getChangeData(), indexingContext);
        break;
      case LAST_POLICY_EVALUATION:
        String[] ids = change.getChangeData().split(":");
        updateIndexForPolicyEvaluation(ids[0], ids[1], indexingContext);
        break;
      case ORGANIZATION:
        updateIndexForOrganization(change.getChangeData(), indexingContext);
        break;
      case LABEL:
        updateIndexForLabel(change.getChangeData(), indexingContext);
        break;
      case POLICY:
        updateIndexForPolicy(change.getChangeData(), indexingContext);
        break;
      case APPLICATION_CATEGORY:
        updateIndexForApplicationCategory(change.getChangeData(), indexingContext);
        break;
      case SBOM:
        String[] appIdAndVersion = change.getChangeData().split(":");
        updateIndexForSbom(appIdAndVersion[0], appIdAndVersion[1], indexingContext);
        break;
      case POLICY_WAIVER:
        updateIndexForPolicyWaiver(change.getChangeData(), indexingContext);
        break;
      case POLICY_WAIVER_REQUEST:
        updateIndexForPolicyWaiverRequest(change.getChangeData(), indexingContext);
        break;
      default:
        throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
    }
  }

  private void updateIndexForPolicyEvaluation(
      final String applicationId,
      final String stageTypeId,
      final IndexingContext indexingContext) throws IOException
  {
    String queryForObsoleteDocs = "(" +
        indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId) +
        " AND " +
        indexingContext.newQuery(FieldIdentifier.POLICY_EVALUATION_STAGE, stageTypeId) +
        ")";
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Application application = applicationDAO.getById(applicationId);
    if (application == null) {
      return;
    }
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      return;
    }

    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    StageType stageType = StageTypes.getById(stageTypeId);
    indexingContext.addNonNullDocuments(
        documentBuilderHelper.buildApplicationStageSVDocs(indexingContext, organization, application, stageType,
            parentOrganizations));
  }

  private void updateIndexForSbom(
      final String applicationId,
      final String applicationVersion,
      final IndexingContext indexingContext) throws IOException
  {
    String queryForObsoleteDocs = "(" +
        indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId) +
        " AND " +
        indexingContext.newQuery(FieldIdentifier.APPLICATION_VERSION, QueryParser.escape(applicationVersion)) +
        ")";
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Application application = applicationDAO.getById(applicationId);
    if (application == null) {
      return;
    }

    ThirdPartySbomMetadata sbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, applicationVersion);
    if (sbomMetadata == null) {
      return;
    }

    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      return;
    }

    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    indexingContext.addOwners(Collections.singletonList(organization));
    indexingContext.addOwners(Collections.singletonList(application));

    Document sbomDoc = documentBuilderHelper.buildDocument(indexingContext, sbomMetadata);
    List<Document> sbomContentsDocs =
        documentBuilderHelper.buildSbomVersionSVDocs(organization, application, sbomMetadata, parentOrganizations);

    List<Document> docsToAdd = new ArrayList<>(sbomContentsDocs.size() + 1);
    docsToAdd.addAll(sbomContentsDocs);
    docsToAdd.add(sbomDoc);

    indexingContext.addNonNullDocuments(docsToAdd);
  }

  private void updateIndexForLabel(final String labelId, final IndexingContext indexingContext) throws IOException {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.COMPONENT_LABEL_ID, labelId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Label label = labelDAO.getById(labelId);

    if (label == null) {
      return;
    }

    indexingContext.addNonNullDocuments(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, label)));
  }

  private void updateIndexForPolicy(final String policyId, final IndexingContext indexingContext) throws IOException {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.POLICY_ID, policyId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    // Manual waiver docs denormalize policyName + threatLevel, so a rename / threat change must rebuild
    // them; the waiver docs key on POLICY_WAIVER_POLICY_ID (not POLICY_ID), so delete + re-add here.
    indexingContext.deleteDocuments(indexingContext.newQuery(FieldIdentifier.POLICY_WAIVER_POLICY_ID, policyId));

    Policy policy = policyDAO.getById(policyId);

    if (policy == null) {
      return;
    }

    indexingContext.addNonNullDocuments(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, policy)));

    // All waivers from getByPolicyId share this policy, so pass the already-fetched policy through
    // and batch-load their reasons once, rather than re-resolving the policy and reason per waiver.
    indexingContext.addNonNullDocuments(
        documentBuilderHelper.buildPolicyWaiverDocsForPolicy(
            indexingContext, policyWaiverDAO.getByPolicyId(policyId), policy));
  }

  private void updateIndexForPolicyWaiver(
      final String changeData,
      final IndexingContext indexingContext) throws IOException
  {
    // changeData is prefixed with the waiver kind (see SearchIndexChange.ChangeType.POLICY_WAIVER) so
    // the correct table is queried directly. The document's POLICY_WAIVER_ID field stores the raw id,
    // so the delete query uses the unprefixed id.
    boolean isAuto;
    String waiverId;
    if (changeData.startsWith(SearchIndexChange.POLICY_WAIVER_AUTO_PREFIX)) {
      isAuto = true;
      waiverId = changeData.substring(SearchIndexChange.POLICY_WAIVER_AUTO_PREFIX.length());
    }
    else if (changeData.startsWith(SearchIndexChange.POLICY_WAIVER_MANUAL_PREFIX)) {
      isAuto = false;
      waiverId = changeData.substring(SearchIndexChange.POLICY_WAIVER_MANUAL_PREFIX.length());
    }
    else {
      // Defensive: every waiver writer prefixes MANUAL:/AUTO: (see SearchIndexChange), so an
      // unprefixed value signals a bug. Treat it as a manual waiver id so a best-effort reindex
      // still runs, and log so the malformed change data is visible.
      log.warn("Policy waiver change data \"{}\" has no kind prefix; treating as manual waiver.", changeData);
      isAuto = false;
      waiverId = changeData;
    }

    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.POLICY_WAIVER_ID, waiverId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Document doc = null;
    if (isAuto) {
      AutoPolicyWaiver autoWaiver = autoPolicyWaiverDAO.getById(waiverId);
      if (autoWaiver != null) {
        doc = documentBuilderHelper.buildDocument(indexingContext, autoWaiver);
      }
    }
    else {
      PolicyWaiver waiver = policyWaiverDAO.getById(waiverId);
      if (waiver != null) {
        doc = documentBuilderHelper.buildDocument(indexingContext, waiver);
      }
    }

    if (doc != null) {
      indexingContext.addNonNullDocuments(Collections.singletonList(doc));
    }
  }

  private void updateIndexForPolicyWaiverRequest(
      final String requestId,
      final IndexingContext indexingContext) throws IOException
  {
    // The request doc's POLICY_WAIVER_ID field stores the request id, so delete by that id then
    // rebuild the single doc (null when the request is gone or its owner is non-indexable). Guard on
    // itemType:policy_waiver_request so a committed POLICY_WAIVER sharing the id (both are UUIDs, so
    // this never happens in practice) can never be cross-deleted — makes the isolation intentional.
    String queryForObsoleteDocs = "(" +
        indexingContext.newQuery(FieldIdentifier.POLICY_WAIVER_ID, requestId) +
        " AND " +
        indexingContext.newQuery(FieldIdentifier.ITEM_TYPE, ItemType.POLICY_WAIVER_REQUEST.searchFieldName()) +
        ")";
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Document doc = documentBuilderHelper.buildPolicyWaiverRequestDocById(indexingContext, requestId);
    if (doc != null) {
      indexingContext.addNonNullDocuments(Collections.singletonList(doc));
    }
  }

  private void updateIndexForApplicationCategory(
      final String tagId,
      final IndexingContext indexingContext) throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.APPLICATION_CATEGORY_ID, tagId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Tag tag = tagDAO.getById(tagId);

    if (tag == null) {
      return;
    }

    indexingContext.addNonNullDocuments(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, tag)));
  }

  private void updateIndexForApplication(
      final String applicationId,
      final IndexingContext indexingContext) throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Application application = applicationDAO.getById(applicationId);
    if (application == null) {
      return;
    }
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      return;
    }

    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    indexingContext.addOwners(Collections.singletonList(organization));
    indexingContext.addOwners(Collections.singletonList(application));

    // Index the app itself
    indexingContext.addNonNullDocuments(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, application)));
    // Index the app labels
    List<Document> appLabelDocs = labelDAO.getByOwnerId(application.getId())
        .stream()
        .map(label -> documentBuilderHelper.buildDocument(indexingContext, label))
        .toList();
    indexingContext.addNonNullDocuments(appLabelDocs);
    // Index the app policies
    List<Document> appPolicyDocs = policyDAO.getByOwnerId(application.getId())
        .stream()
        .map(policy -> documentBuilderHelper.buildDocument(indexingContext, policy))
        .toList();
    indexingContext.addNonNullDocuments(appPolicyDocs);
    // Index the app SVs
    indexingContext.addNonNullDocuments(
        documentBuilderHelper.buildApplicationSVDocs(indexingContext, organization, application,
            ImmutableMap.of(organization, parentOrganizations)));
    // Rebuild the app's waivers: the cascade delete above removed them (they carry applicationId), so
    // without this re-add an app index change makes the app's waiver docs vanish until a full reindex.
    indexingContext.addNonNullDocuments(buildWaiverDocsForOwner(indexingContext, application.getId()));
  }

  private List<Document> buildWaiverDocsForOwner(final IndexingContext indexingContext, final String ownerId) {
    List<Document> docs = new ArrayList<>();
    // buildDocument returns null for container-image / repository-owner / unresolvable waivers; keep
    // this list clean (like buildApplicationSVDocs) rather than leaning on addNonNullDocuments.
    policyWaiverDAO.getByOwnerId(ownerId)
        .stream()
        .map(waiver -> documentBuilderHelper.buildDocument(indexingContext, waiver))
        .filter(Objects::nonNull)
        .forEach(docs::add);
    autoPolicyWaiverDAO.getByOwnerId(ownerId)
        .stream()
        .map(waiver -> documentBuilderHelper.buildDocument(indexingContext, waiver))
        .filter(Objects::nonNull)
        .forEach(docs::add);
    return docs;
  }

  private void updateIndexForOrganization(
      final String organizationId,
      final IndexingContext indexingContext) throws IOException
  {
    Organization org = organizationDAO.getById(organizationId);
    if (org == null) {
      String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.ORGANIZATION_ID, organizationId);
      indexingContext.deleteDocuments(queryForObsoleteDocs);
      return;
    }
    updateIndexForOrganization(org, indexingContext);
  }

  private void updateIndexForOrganization(
      final Organization org,
      final IndexingContext indexingContext) throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.ORGANIZATION_ID, org.getId());
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    List<Application> applications = applicationDAO.getByOrganizationId(org.getId());

    indexingContext.addOwners(Collections.singletonList(org));
    indexingContext.addOwners(applications);

    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(org).forEach(o -> parentOrganizations.add((Organization) o));
    Map<Organization, Collection<Organization>> parentOrgsMap = ImmutableMap.of(org, parentOrganizations);

    // Index the org itself
    indexingContext.addNonNullDocuments(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, org)));
    // Index the org apps
    List<Document> orgAppDocs = applications.stream()
        .map(app -> documentBuilderHelper.buildDocument(indexingContext, app))
        .toList();
    indexingContext.addNonNullDocuments(orgAppDocs);
    // Index the org app categories
    List<Document> orgAppCategoryDocs = tagDAO.getByOrganizationId(org.getId())
        .stream()
        .map(appCategory -> documentBuilderHelper.buildDocument(indexingContext, appCategory))
        .toList();
    indexingContext.addNonNullDocuments(orgAppCategoryDocs);
    // Index the org labels
    List<Document> orgLabelDocs = labelDAO.getByOwnerId(org.getId())
        .stream()
        .map(label -> documentBuilderHelper.buildDocument(indexingContext, label))
        .toList();
    indexingContext.addNonNullDocuments(orgLabelDocs);
    // Index the org policies
    List<Document> orgPolicyDocs = policyDAO.getByOwnerId(org.getId())
        .stream()
        .map(policy -> documentBuilderHelper.buildDocument(indexingContext, policy))
        .toList();
    indexingContext.addNonNullDocuments(orgPolicyDocs);

    // Index the security vulnerability data
    for (Application application : applications) {
      indexingContext.addNonNullDocuments(
          documentBuilderHelper.buildApplicationSVDocs(indexingContext, org, application, parentOrgsMap));
    }

    // Rebuild waivers swept by the cascade delete. The org cascade delete above keys on
    // ORGANIZATION_ID, which removes the org's own waiver docs but NOT app-scoped waiver docs (those
    // are keyed only by APPLICATION_ID). So for each app, delete its POLICY_WAIVER docs first,
    // otherwise re-adding them here would leave duplicates (incl. stale allowedContextIds copies,
    // since DOCUMENT_KEY excludes allowedContextIds) on every org index change.
    indexingContext.addNonNullDocuments(buildWaiverDocsForOwner(indexingContext, org.getId()));
    for (Application application : applications) {
      String appWaiverQuery = "(" +
          indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, application.getId()) +
          " AND " +
          indexingContext.newQuery(FieldIdentifier.ITEM_TYPE, ItemType.POLICY_WAIVER.searchFieldName()) +
          ")";
      indexingContext.deleteDocuments(appWaiverQuery);
      indexingContext.addNonNullDocuments(buildWaiverDocsForOwner(indexingContext, application.getId()));
    }

    List<Organization> byParentOrganizationId = organizationDAO.getByParentOrganizationId(org.getId());
    for (Organization organization : byParentOrganizationId) {
      updateIndexForOrganization(organization, indexingContext);
    }
  }

  protected String createInitialQuery(final String searchQuery, final boolean allComponents) {
    if (StringUtils.isBlank(searchQuery)) {
      throw new BadRequestException("The search query is empty");
    }
    String finalSearchQuery =
        allComponents ? searchQuery : searchQuery + " -" + ITEM_TYPE.label + ":" + NON_VULNERABLE_COMPONENT.name();

    // parentOrganizationName and parentOrganizationId support searching the hierarchy
    // including the organization itself
    // the replacement here has no side effects and allows us to search within the org hierarchy
    finalSearchQuery = finalSearchQuery.replaceAll("organizationName", "parentOrganizationName");
    finalSearchQuery = finalSearchQuery.replaceAll("organizationId", "parentOrganizationId");
    return finalSearchQuery;
  }

  protected Set<String> getFieldNames(final String query) {
    Set<String> fieldNames = new HashSet<>();
    conversionHelper.stringToQuery(query).visit(new QueryVisitor()
    {
      @Override
      public boolean acceptField(String field) {
        fieldNames.add(field);
        return false;
      }
    });
    return fieldNames;
  }

  protected void populateTelemetry(final boolean initialSearch, final Set<String> fieldNames) {
    // We only add telemetry for the initial search request in order to
    // avoid adding the same data when the user navigates search results.
    if (initialSearch) {
      if (fieldNames.remove("parentOrganizationName")) {
        fieldNames.add("organizationName");
      }
      if (fieldNames.remove("parentOrganizationId")) {
        fieldNames.add("organizationId");
      }
      advancedSearchTelemetryMetrics.addSearch(fieldNames);
    }
  }

  protected void checkFieldNames(final Set<String> fieldNames) {
    Set<String> invalidFieldNames = new TreeSet<>();
    for (String fieldName : fieldNames) {
      if (getFieldIdentifier(fieldName) == null) {
        invalidFieldNames.add(fieldName);
      }
    }
    if (!invalidFieldNames.isEmpty()) {
      throw new BadRequestException("The search query contains invalid field names: " + invalidFieldNames);
    }
  }

  protected String createFinalQuery(final String query, final boolean isSbomManagerMode) {
    return createFinalQueryWithRbacMeta(query, isSbomManagerMode).query();
  }

  /**
   * Same as {@link #createFinalQuery} plus RBAC context cardinality for Lucene reader timing.
   * {@code rbacContextCount == -1} means unrestricted/global (no RBAC clause appended).
   */
  protected FinalQueryWithRbacMeta createFinalQueryWithRbacMeta(
      final String query,
      final boolean isSbomManagerMode)
  {
    String queryWithSbomFiltering = appendSbomFilteringToQuery(query, isSbomManagerMode);
    return appendAllowedApplicationsAndOrganizationsToQuery(queryWithSbomFiltering);
  }

  private FinalQueryWithRbacMeta appendAllowedApplicationsAndOrganizationsToQuery(final String query) {
    Optional<Map<String, OwnerType>> readableContexts = resolveReadableContextIdsForCurrentUser();
    if (readableContexts.isEmpty()) {
      return new FinalQueryWithRbacMeta(query, -1);
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = readableContexts.get();
    List<String> allowedContextConditions = new ArrayList<>();

    contextIdsWithReadPermissionMap.forEach((contextId, type) -> {
      if (OwnerType.APPLICATION.equals(type)) {
        allowedContextConditions.add(APPLICATION_ID.label + ":" + contextId);
      }
      else if (OwnerType.ORGANIZATION.equals(type)) {
        allowedContextConditions.add(ORGANIZATION_ID.label + ":" + contextId);
      }
    });

    if (allowedContextConditions.isEmpty()) {
      // No allowed contexts means no results should be returned
      return new FinalQueryWithRbacMeta("(" + query + ") AND (NOT *:*)", 0);
    }

    String allowedContextsQuery = String.join(" OR ", allowedContextConditions);
    return new FinalQueryWithRbacMeta(
        "(" + allowedContextsQuery + ") AND (" + query + ")",
        contextIdsWithReadPermissionMap.size());
  }

  protected record FinalQueryWithRbacMeta(String query, int rbacContextCount)
  {
  }

  /**
   * Resolves readable application/organization context ids for the current user.
   * {@link Optional#empty()} denotes unrestricted (global) access;
   * an empty map denotes fail-closed (no readable contexts).
   */
  protected Optional<Map<String, OwnerType>> resolveReadableContextIdsForCurrentUser() {
    if (readableContextAuthzCache != null) {
      return readableContextAuthzCache.resolveReadableContexts(currentUser.getUserPrincipal());
    }

    if (authorizationChecker == null) {
      // Direct-construction tests may omit both cache and checker; fail closed rather than NPE.
      log.warn("Neither ReadableContextAuthzCache nor AuthorizationChecker is wired; failing closed for RBAC");
      return Optional.of(Map.of());
    }

    UserPrincipal principal = currentUser.getUserPrincipal();
    if (authorizationChecker.isPermitted(principal, Permission.READ, Collections.emptyMap())) {
      return Optional.empty();
    }

    Set<String> contextIdsWithReadPermission =
        permissionService.getContextIdsForUserWithPermission(principal, Permission.READ);

    if (contextIdsWithReadPermission.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        contextIdsWithReadPermission.contains(Organization.ROOT_ORGANIZATION_ID))
    {
      return Optional.empty();
    }

    return Optional.of(getChildContextIds(contextIdsWithReadPermission));
  }

  /**
   * Returns the compiled Lucene RBAC filter from the shared authorization cache. The fallback
   * preserves direct-construction test compatibility while production wiring always supplies the cache.
   */
  protected Query resolveReadableContextRbacFilterForCurrentUser() {
    if (readableContextAuthzCache != null) {
      return readableContextAuthzCache.compiledRbacFilter(currentUser.getUserPrincipal());
    }
    return LuceneRbacFilterQueryBuilder.build(resolveReadableContextIdsForCurrentUser());
  }

  /**
   * Context IDs (org and/or app) on which the current user has READ; input to
   * {@link #buildAllowedContextIdsFilter(Set)}.
   * <p>
   * Routes through {@link #resolveReadableContextIdsForCurrentUser()} so global/unrestricted
   * principals short-circuit via {@link ReadableContextAuthzCache} instead of enumerating every
   * membership row (estate-scale admins can have tens of thousands of Owner mappings). Unrestricted
   * access is signaled as {@link MembershipMapping#GLOBAL_CONTEXT_ID} so
   * {@link #buildAllowedContextIdsLuceneFilter(Set)} applies no filter.
   * <p>
   * This short-circuit is intentional for <em>all</em> entity types that use this path (APPLICATION,
   * VIOLATION, WAIVER, …), not WAIVER-only: the authz cache is the single source of truth.
   * Production callers are gated behind {@code PREVIEW_NEXUS_ONE_UI} ({@code IndexQueryResource},
   * {@code GlobalSearchResource} → {@code IqLocalSearchService}).
   */
  public Set<String> getCurrentUserContextIdsWithReadPermission() {
    final Optional<Map<String, OwnerType>> contexts = resolveReadableContextIdsForCurrentUser();
    if (contexts.isEmpty()) {
      return Set.of(MembershipMapping.GLOBAL_CONTEXT_ID);
    }
    return contexts.get().keySet();
  }

  /**
   * Public entry point matching the {@link SearchIndexClient} interface default. Delegates to
   * {@link #buildAllowedContextIdsLuceneFilter(Set)} for the actual Lucene-flavour composition.
   */
  @Override
  public Query buildAllowedContextIdsFilter(final Set<String> userPermittedContextIds) {
    return buildAllowedContextIdsLuceneFilter(userPermittedContextIds);
  }

  /**
   * Permission filter over the denormalized {@link FieldIdentifier#ALLOWED_CONTEXT_IDS} field.
   * Fail-closed contract: returns {@code null} ONLY for global/root access (no filter needed),
   * a {@link MatchNoDocsQuery} whenever permissions are absent/unresolved, else a
   * {@link TermInSetQuery} over the permitted IDs. A {@code null} return never means "deny".
   * Match is case-sensitive — pass the raw {@code Owner.getId()}, do not lowercase.
   * Wrap the result via {@link #wrapWithPermissionFilter(Query, Query)}.
   *
   * <p>
   * Backward-compat contract (requires prior reindex): the returned {@link TermInSetQuery} matches
   * on the denormalized {@link FieldIdentifier#ALLOWED_CONTEXT_IDS} field, which pre-existing
   * (pre-upgrade) documents do not carry until the one-time backfill/reindex has run. It therefore
   * matches nothing on un-backfilled docs, so a non-global user sees empty results for them
   * (fail-closed/secure, but surprising). A consumer of this permission filter MUST NOT be enabled
   * in production until the {@code allowedContextIds} backfill/reindex has completed for the tenant.
   * That backfill is gated behind the reindex feature flag (default off) and ships with the first
   * consuming feature, not in this foundations change.
   */
  protected Query buildAllowedContextIdsLuceneFilter(final Set<String> userPermittedContextIds) {
    if (userPermittedContextIds == null) {
      return new MatchNoDocsQuery("no permissions resolved");
    }
    if (userPermittedContextIds.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        userPermittedContextIds.contains(Organization.ROOT_ORGANIZATION_ID))
    {
      return null;
    }
    if (userPermittedContextIds.isEmpty()) {
      return new MatchNoDocsQuery("no permitted contexts");
    }
    List<BytesRef> terms = new ArrayList<>(userPermittedContextIds.size());
    for (String id : userPermittedContextIds) {
      if (id != null && !id.isEmpty()) {
        terms.add(new BytesRef(id));
      }
    }
    if (terms.isEmpty()) {
      return new MatchNoDocsQuery("no permitted contexts");
    }
    return new TermInSetQuery(ALLOWED_CONTEXT_IDS.label, terms);
  }

  /**
   * ANDs a base query with a permission filter from {@link #buildAllowedContextIdsFilter(Set)}. A
   * {@code null} filter means global access (base query returned unchanged), never "deny" — so
   * only ever pass a filter from that method, which fails closed on missing permissions.
   *
   * <p>
   * Contract: when both {@code baseQuery} and {@code permissionFilter} are {@code null} this returns
   * {@code null}. A searcher NPEs on a {@code null} query, so direct callers must pass a non-null
   * {@code baseQuery}. The safe composed entry point is {@link #buildPermittedQuery(Query)}, which
   * substitutes a {@link org.apache.lucene.search.MatchAllDocsQuery} instead of ever returning
   * {@code null}.
   */
  public Query wrapWithPermissionFilter(final Query baseQuery, final Query permissionFilter) {
    if (permissionFilter == null) {
      return baseQuery;
    }
    if (baseQuery == null) {
      return permissionFilter;
    }
    return new BooleanQuery.Builder()
        .add(baseQuery, Occur.MUST)
        .add(permissionFilter, Occur.FILTER)
        .build();
  }

  /**
   * When the REST API is called in: <br/>
   * <br/>
   * SBOM Manager mode
   * <ul>
   * <li>Components without an applicationVersion MUST NOT be returned</li>
   * <li>Vulnerabilities without an applicationVersion MUST NOT be returned</li>
   * <li>Application categories MUST NOT be returned</li>
   * <li>Component labels MUST NOT be returned</li>
   * <li>Policies MUST NOT be returned</li>
   * </ul>
   * Default Mode
   * </ul>
   * <li>Components with an applicationVersion MUST NOT be returned</li>
   * <li>Vulnerabilities with an applicationVersion MUST NOT be returned</li>
   * <li>SBOM metadata MUST NOT be returned</li>
   * </ul>
   */
  // Visible for testing (package-private): appendSbomFilteringToQuery_excludesPolicyWaiverInBothModes.
  String appendSbomFilteringToQuery(final String originalQuery, final boolean isSbomManagerMode) {
    StringBuilder queryBuilder = new StringBuilder();

    // Start with the original query wrapped in parentheses
    queryBuilder.append("(").append(originalQuery).append(")");

    // Add component exclusion logic
    String appVersionCondition = isSbomManagerMode ? "NOT applicationVersion:[* TO *]" : "applicationVersion:[* TO *]";
    queryBuilder.append(" AND NOT (itemType:")
        .append(ItemType.NON_VULNERABLE_COMPONENT.searchFieldName())
        .append(" AND ")
        .append(appVersionCondition)
        .append(")");

    // Add vulnerability exclusion logic
    queryBuilder.append(" AND NOT (itemType:")
        .append(ItemType.SECURITY_VULNERABILITY.searchFieldName())
        .append(" AND ")
        .append(appVersionCondition)
        .append(")");

    // Policy waivers and waiver requests are Global Search item types; legacy advanced search must
    // never surface them (AC11: legacy advanced search behavior is unchanged), so exclude both in both modes.
    queryBuilder.append(" AND NOT itemType:").append(POLICY_WAIVER.searchFieldName());
    queryBuilder.append(" AND NOT itemType:").append(ItemType.POLICY_WAIVER_REQUEST.searchFieldName());

    if (isSbomManagerMode) {
      // SBOM Manager mode exclusions
      queryBuilder.append(" AND NOT itemType:").append(APPLICATION_CATEGORY.searchFieldName());
      queryBuilder.append(" AND NOT itemType:").append(COMPONENT_LABEL.searchFieldName());
      queryBuilder.append(" AND NOT itemType:").append(POLICY.searchFieldName());
    }
    else {
      // Default mode exclusions
      queryBuilder.append(" AND NOT itemType:").append(SBOM_METADATA.searchFieldName());
    }

    return queryBuilder.toString();
  }

  /**
   * Only group sequential items if possible. This maintains lucene order/ranking ensuring more relevant results appear
   * earlier. This will also typically only iterate over the default pageSize number of documents which helps avoid too
   * much memory usage. See CLM-29232 for more details.
   */
  protected void groupDocuments(
      final int page,
      final int pageSize,
      final Supplier<Document> documentSupplier,
      final SearchResultDTO searchResultDTO,
      final Map<String, String> groupFieldNamesByItemType)
  {
    int resultIndex = ((page - 1) * pageSize) + 1;
    GroupingByDTO lastGroup = null;
    Document document;
    while ((document = documentSupplier.get()) != null) {
      SearchResultItemDTO searchResultItemDTO = new SearchResultItemDTO(document);
      String groupFieldName = groupFieldNamesByItemType.get(searchResultItemDTO.itemType);
      FieldIdentifier groupIdentifier = getFieldIdentifier(groupFieldName);
      String groupBy = document.get(groupFieldName);

      GroupingByDTO targetGroup = null;
      if (lastGroup != null && groupBy.equals(lastGroup.groupBy)) {
        targetGroup = lastGroup;
      }
      if (targetGroup == null) {
        GroupingByDTO groupingByDTO = new GroupingByDTO();
        groupingByDTO.groupBy = groupBy;
        groupingByDTO.groupIdentifier = groupIdentifier;

        if (groupIdentifier == VULNERABILITY_ID || groupIdentifier == VULNERABILITY_DESCRIPTION) {
          groupingByDTO.additionalInfo = document.get(VULNERABILITY_DESCRIPTION.label);
        }

        searchResultDTO.groupingByDTOS.add(groupingByDTO);
        targetGroup = groupingByDTO;
      }
      targetGroup.searchResultItemDTOS.add(searchResultItemDTO);

      searchResultItemDTO.resultIndex = resultIndex++;
      lastGroup = targetGroup;
    }
  }

  // Visible for testing
  public ExecutorService getIndexingExecutor() {
    return indexingExecutors.computeIfAbsent(tenant -> {
      int threadCount = DefaultExecutorThreadPools.getThreadCount(
          INDEX_THREADS_MIN,
          INDEX_THREADS_MAX,
          INDEX_THREADS_DEFAULT,
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX);
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          threadCount,
          threadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build(),
          new AbortPolicy(),
          "advanced_search_index",
          getClass().getSimpleName());
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  protected void doPopulateIndex(final IndexingContext indexingContext) {
    log.info("begin indexing");

    List<Organization> organizations = organizationDAO.getAll();
    Map<String, Organization> organizationById =
        organizations.stream().collect(Collectors.toMap(Organization::getId, item -> item));
    Map<Organization, Collection<Organization>> parentsByOrganization =
        computeParentsByOrganization(organizationById).asMap();
    List<Application> applications = applicationDAO.getAll();

    indexingContext.addOwners(organizations);
    indexingContext.addOwners(applications);

    AtomicInteger consecutiveFailures = new AtomicInteger(0);

    CompletableFuture<Void> orgDocs = CompletableFuture.supplyAsync(
        () -> documentBuilderHelper.buildOrganizationDocs(indexingContext, organizations, parentsByOrganization),
        getIndexingExecutor())
        .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> appDocs = CompletableFuture.supplyAsync(
        () -> documentBuilderHelper.buildApplicationDocs(indexingContext, applications, parentsByOrganization),
        getIndexingExecutor())
        .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    Function<Application, CompletableFuture<Void>> processSVDocsForApplication =
        application -> CompletableFuture
            .supplyAsync(
                () -> documentBuilderHelper.buildApplicationSVDocs(indexingContext,
                    organizationById.get(application.getOrganizationId()),
                    application, parentsByOrganization),
                getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    List<CompletableFuture<Void>> appSVDocs = applications
        .stream()
        .map(processSVDocsForApplication)
        .toList();

    CompletableFuture<Void> tagDocs =
        CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildTagDocs(indexingContext), getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> labelDocs =
        CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildLabelDocs(indexingContext),
            getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> policyDocs =
        CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildPolicyDocs(indexingContext),
            getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> sbomDocs = CompletableFuture.supplyAsync(
        () -> documentBuilderHelper.buildSbomDocs(indexingContext, parentsByOrganization), getIndexingExecutor())
        .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> policyWaiverDocs =
        CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildPolicyWaiverDocs(indexingContext),
            getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    CompletableFuture<Void> policyWaiverRequestDocs =
        CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildPolicyWaiverRequestDocs(indexingContext),
            getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    Function<Application, CompletableFuture<Void>> processSbomSVDocsForApplication =
        application -> CompletableFuture
            .supplyAsync(
                () -> documentBuilderHelper.buildSbomSVDocs(organizationById.get(application.getOrganizationId()),
                    application, parentsByOrganization),
                getIndexingExecutor())
            .thenAccept(docs -> addDocumentsWithResilience(indexingContext, docs, consecutiveFailures));

    List<CompletableFuture<Void>> sbomSVDocs = applications
        .stream()
        .map(processSbomSVDocsForApplication)
        .toList();

    log.info("indexing threads started");
    orgDocs.join();
    log.info("org indexing complete");
    appDocs.join();
    log.info("app indexing complete");
    CompletableFuture.allOf(appSVDocs.toArray(CompletableFuture[]::new)).join();
    log.info("appSV indexing complete");
    tagDocs.join();
    log.info("tag indexing complete");
    labelDocs.join();
    log.info("label indexing complete");
    policyDocs.join();
    log.info("policy indexing complete");
    policyWaiverDocs.join();
    log.info("policy waiver indexing complete");
    policyWaiverRequestDocs.join();
    log.info("policy waiver request indexing complete");
    sbomDocs.join();
    log.info("SBOM metadata indexing complete");
    CompletableFuture.allOf(sbomSVDocs.toArray(CompletableFuture[]::new)).join();
    log.info("sbomSV indexing complete");
  }

  protected void addDocumentsWithResilience(
      final IndexingContext indexingContext,
      final List<Document> documents,
      final AtomicInteger consecutiveFailures)
  {
    try {
      indexingContext.addNonNullDocuments(documents);
      consecutiveFailures.set(0);
    }
    catch (IOException | RuntimeException e) {
      if (isChangeSpecificError(e)) {
        log.error("Skipping document batch due to change-specific error during population", e);
      }
      else {
        int failures = consecutiveFailures.incrementAndGet();

        if (isSystemicError(e)) {
          log.error("Systemic failure during index population (consecutive failure {} in population)",
              failures, e);
        }
        else {
          log.error("Unknown error during index population (consecutive failure {} in population)",
              failures, e);
        }

        if (failures >= MAX_CONSECUTIVE_FAILURES) {
          log.error("Too many consecutive failures ({}) during index population. Aborting population.",
              failures, e);
          if (e instanceof RuntimeException runtimeException) {
            throw runtimeException;
          }
          throw new UncheckedIOException((IOException) e);
        }
      }
    }
  }

  @Override
  public List<SearchIndexChange> getSearchIndexChanges() {
    // Note: this pops a limited amount of records off the 'queue' as there are cases of large amounts of rows
    // accumulating. See CLM-29339. TODO Future enhancements will further improve this code - CLM-29618
    return searchIndexChangeDAO.getBatch(QUEUE_POP_AMOUNT);
  }

  protected void processSearchIndexChanges(
      final List<SearchIndexChange> searchIndexChanges,
      final IndexingContext indexingContext,
      final Consumer<SearchIndexChange> deletionCallback) throws IOException
  {
    log.debug("Updating search index with {} changes", searchIndexChanges.size());
    Set<String> alreadyApplied = new HashSet<>();
    int consecutiveFailures = 0;
    int maxConsecutiveFailures = Math.min(searchIndexChanges.size(), MAX_CONSECUTIVE_FAILURES);
    for (SearchIndexChange change : searchIndexChanges) {
      String changeId = change.getChangeType() + "\t" + change.getChangeData();
      if (alreadyApplied.add(changeId)) {
        Integer failureCount = changeFailureCounts.get().getIfPresent(changeId);
        if (failureCount != null && failureCount >= MAX_CHANGE_FAILURES) {
          log.warn("Skipping search index update for change {} as it failed {} times", change, failureCount);
          change.setProcessed(true);
          deletionCallback.accept(change);
          changeFailureCounts.get().invalidate(changeId);
          continue;
        }

        try {
          updateIndex(change, indexingContext);
          change.setProcessed(true);
          deletionCallback.accept(change);
          consecutiveFailures = 0;
          changeFailureCounts.get().invalidate(changeId);
          log.debug("Updated search index with change {}", change);
        }
        catch (IOException | RuntimeException e) {
          if (isChangeSpecificError(e)) {
            log.warn("Skipping search index update due to change-specific error for change {}", change, e);
            change.setProcessed(true);
            deletionCallback.accept(change);
          }
          else {
            consecutiveFailures++;

            if (isSystemicError(e)) {
              log.warn("Systemic failure during search index update (attempt {} in this batch). " +
                  "Change {} will be retried later.", consecutiveFailures, change, e);
            }
            else {
              // Count unknown errors against the specific change
              int count = (failureCount == null ? 0 : failureCount) + 1;
              changeFailureCounts.get().put(changeId, count);
              log.warn("Failed to update search index for change {}. Failure count: {}. Continuing with next change.",
                  change, count, e);
            }

            if (consecutiveFailures >= maxConsecutiveFailures) {
              log.error("Too many consecutive failures ({}) in search index update batch. Aborting batch.",
                  consecutiveFailures, e);
              throw e;
            }
          }
        }
      }
    }
    log.debug("Updated search index");
  }

  /**
   * Determines if an exception represents a change-specific error (bad data) rather than a systemic error.
   * <p>
   * Change-specific errors should skip the change immediately without retries. Implementations should call
   * {@link #isCommonChangeSpecificError(Exception)} for shared logic.
   *
   * @param e the exception to check
   * @return true if this is a change-specific error, false otherwise
   */
  protected abstract boolean isChangeSpecificError(Exception e);

  /**
   * Determines if an exception represents a systemic/infrastructure error rather than a change-specific error.
   * <p>
   * Systemic errors indicate temporary infrastructure issues and should not count against individual changes.
   * Implementations should call {@link #isCommonSystemicError(Exception)} for shared logic.
   *
   * @param e the exception to check
   * @return true if this is a systemic error, false otherwise
   */
  protected abstract boolean isSystemicError(Exception e);

  /**
   * Checks for common change-specific errors that apply to all search index implementations.
   * <p>
   * Common change-specific errors:
   * <ul>
   * <li>ParseException - Query parsing errors</li>
   * <li>IllegalArgumentException - Invalid field values</li>
   * <li>NullPointerException - Null field values</li>
   * </ul>
   *
   * @param e the exception to check
   * @return true if this is a common change-specific error
   */
  protected boolean isCommonChangeSpecificError(final Exception e) {
    return hasCauseOrMessage(e, cause -> cause instanceof ParseException || cause instanceof IllegalArgumentException ||
        cause instanceof NullPointerException);
  }

  /**
   * Checks for common systemic errors that apply to all search index implementations.
   * <p>
   * Common systemic errors:
   * <ul>
   * <li>TimeoutException - Generic timeout</li>
   * </ul>
   *
   * @param e the exception to check
   * @return true if this is a common systemic error
   */
  protected boolean isCommonSystemicError(final Exception e) {
    return hasCauseOrMessage(e, cause -> cause instanceof TimeoutException);
  }

  public static boolean hasCauseOrMessage(Throwable e, final Predicate<Throwable> predicate) {
    while (e != null) {
      if (predicate.test(e)) {
        return true;
      }
      e = e.getCause();
    }
    return false;
  }

  public boolean shouldThrow(final Exception e) {
    return shouldThrow(e, this::isSystemicError, lastRecordedExceptionEpochMs.get(), currentCooldown.get(),
        INITIAL_COOLDOWN, MAX_COOLDOWN);
  }

  public static boolean shouldThrow(
      final Exception e,
      final Predicate<Exception> test,
      final AtomicLong lastRecordedExceptionEpochMs,
      final AtomicReference<Duration> currentCooldown,
      final Duration initialCooldown,
      final Duration maxCooldown)
  {
    long now = System.currentTimeMillis();
    if (test.test(e)) {
      Duration duration = Duration.ofMillis(now - lastRecordedExceptionEpochMs.get());
      if (duration.compareTo(currentCooldown.get()) < 0) {
        return false;
      }
    }
    Duration newCooldown = currentCooldown.get();
    long timeSinceLastError = now - lastRecordedExceptionEpochMs.get();
    if (timeSinceLastError > newCooldown.toMillis() * 2) {
      newCooldown = initialCooldown;
    }
    else {
      newCooldown = newCooldown.multipliedBy(2);
      if (newCooldown.compareTo(maxCooldown) > 0) {
        newCooldown = maxCooldown;
      }
    }
    currentCooldown.set(newCooldown);
    lastRecordedExceptionEpochMs.set(now);
    return true;
  }

  @Override
  public void deleteSearchIndexChange(final SearchIndexChange change) {
    searchIndexChangeDAO.delete(change);
  }

  @Override
  public abstract long count(String metricQuery);

  @Override
  public abstract MetricAggregationResult aggregateCountByField(
      String metricQuery,
      String bucketField,
      Map<String, int[]> ranges);

  @Override
  public abstract MetricAggregationResult aggregateCountByFloatField(
      String metricQuery,
      String bucketField,
      Map<String, float[]> ranges,
      String distinctField);

  /**
   * Validates the {@code aggregateCountByFloatField} range contract at the boundary: every bucket's
   * bounds must be a non-null {@code float[2]} {@code [minInclusive, maxExclusive)} half-open pair with
   * {@code minInclusive <= maxExclusive}. Rejects {@code NaN} bounds (an aggregation over {@code NaN}
   * would silently match nothing). Mirrors {@link #validateRangeBounds(Map)} so both backends fail with
   * an explicit, bucket-named {@link IllegalArgumentException} rather than an opaque downstream error.
   */
  protected static void validateFloatRangeBounds(final Map<String, float[]> ranges) {
    if (ranges == null) {
      throw new IllegalArgumentException("ranges must not be null");
    }
    for (Map.Entry<String, float[]> entry : ranges.entrySet()) {
      float[] bounds = entry.getValue();
      if (bounds == null || bounds.length < 2) {
        throw new IllegalArgumentException(
            "Range bounds for '" + entry.getKey() + "' must be a float[2] [minInclusive, maxExclusive); got: "
                + Arrays.toString(bounds));
      }
      if (Float.isNaN(bounds[0]) || Float.isNaN(bounds[1])) {
        throw new IllegalArgumentException(
            "Range bounds for '" + entry.getKey() + "' must not be NaN; got: " + Arrays.toString(bounds));
      }
      if (bounds[0] > bounds[1]) {
        throw new IllegalArgumentException(
            "Range bounds for '" + entry.getKey() + "' must have minInclusive <= maxExclusive; got: "
                + Arrays.toString(bounds));
      }
    }
  }

  /**
   * Validates the {@code aggregateCountByField} range contract at the boundary: every bucket's
   * bounds must be a non-null {@code int[2]} ({@code [minInclusive, maxInclusive]}). Throws an
   * explicit {@link IllegalArgumentException} naming the offending bucket rather than letting a
   * malformed array surface later as an opaque {@code ArrayIndexOutOfBoundsException} wrapped in a
   * generic search exception.
   */
  protected static void validateRangeBounds(final Map<String, int[]> ranges) {
    if (ranges == null) {
      throw new IllegalArgumentException("ranges must not be null");
    }
    for (Map.Entry<String, int[]> entry : ranges.entrySet()) {
      int[] bounds = entry.getValue();
      if (bounds == null || bounds.length < 2) {
        throw new IllegalArgumentException(
            "Range bounds for '" + entry.getKey() + "' must be an int[2] [minInclusive, maxInclusive]; got: "
                + Arrays.toString(bounds));
      }
      if (bounds[0] > bounds[1]) {
        throw new IllegalArgumentException(
            "Range bounds for '" + entry.getKey() + "' must have minInclusive <= maxInclusive; got: "
                + Arrays.toString(bounds));
      }
    }
  }

  protected static void validateCompositeKeyFields(final List<String> compositeKeyFields) {
    if (compositeKeyFields == null || compositeKeyFields.isEmpty()) {
      throw new IllegalArgumentException("compositeKeyFields must not be null or empty");
    }
  }

  /**
   * Resolves a composite-key field name to the canonical indexed label from {@link FieldIdentifier}, rather than
   * interpolating caller-supplied strings into generated scripts.
   */
  protected String resolveCompositeKeyFieldLabel(final String fieldName) {
    FieldIdentifier identifier = getFieldIdentifier(fieldName);
    if (identifier == null) {
      throw new BadRequestException("The search query contains invalid field names: " + fieldName);
    }
    return identifier.label;
  }

  @Override
  public abstract long countDistinct(String metricQuery, List<String> compositeKeyFields);

  @Override
  public abstract Map<String, Long> countDistinctGroupedBy(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues);

  @Override
  public abstract Map<String, Map<String, Long>> countDistinctGroupedByBands(
      String metricQuery,
      String groupField,
      String distinctField,
      Collection<String> groupValues,
      String bandField,
      Map<String, int[]> bands);

  protected abstract void updateMaxQueryClauseCount() throws IOException;

  public static long capTotalHitsForGlobalSearch(final long total) {
    return Math.min(total, GLOBAL_SEARCH_TRACK_TOTAL_HITS_CAP);
  }

  /**
   * Whether the search preview read path is available. Reads {@code PREVIEW_NEXUS_ONE_UI} — the same
   * flag the search resources gate on, so this defence-in-depth guard cannot disagree with the
   * request-boundary gate. Controls code-path selection and visibility only, never security.
   */
  @Override
  public boolean isSearchPreviewEnabled() {
    return SystemConfigurationPropertyFeature.PREVIEW_NEXUS_ONE_UI.isEnabled();
  }

  /**
   * Composed permission-wrap surface; prefer this over calling the lookup/filter/wrap steps directly.
   * Kept {@code final} to fix the lookup+filter+wrap order at the abstract layer, delegating to the
   * single interface default so the permission-sensitive decision is not duplicated.
   */
  @Override
  public final Query buildPermittedQuery(final Query baseQuery) {
    return SearchIndexClient.super.buildPermittedQuery(baseQuery);
  }

  public static <T> HasMoreResult<T> detectHasMore(final List<T> overfetched, final int pageSize) {
    Objects.requireNonNull(overfetched, "overfetched");
    if (pageSize <= 0) {
      throw new IllegalArgumentException("pageSize must be > 0");
    }
    boolean hasMore = overfetched.size() > pageSize;
    int returnCount = Math.min(overfetched.size(), pageSize);
    // Independent copy so the overfetch window is not retained by a live subList view.
    return new HasMoreResult<>(List.copyOf(overfetched.subList(0, returnCount)), hasMore);
  }

  public record HasMoreResult<T>(List<T> rows, boolean hasMore)
  {
  }
}
