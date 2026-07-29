/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Map;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.component.InvalidComponentIdentifierException;
import com.sonatype.clm.dto.model.policy.ConditionFact;
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.clm.dto.model.policy.TriggerReference;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverReasonDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverRequestDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationConstraintFactsDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyViolationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.PolicyThreatCategory;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiver.ComponentMatcherStrategyForWaiver;
import com.sonatype.insight.brain.model.policy.PolicyWaiverReason;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequest;
import com.sonatype.insight.brain.model.policy.PolicyWaiverRequestStatus;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.report.LifecycleReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.PolicyWaiverExpiryStatuses;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.brain.utils.ThreatLevel;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.PackageURLBuilder;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.LifecycleReport.ReportFile.SECURITY_JSON;

@Named
@Singleton
public class DocumentBuilderHelper
{
  /**
   * Canonical indexed {@code policyViolationWaiverStatus} values written onto {@code POLICY_VIOLATION}
   * documents. Exposed as constants so read-side consumers (e.g. {@code ViolationWaiverStatus} on the
   * Nexus One violations list) bind to them directly and any change to the vocabulary is a compile-time
   * break rather than a silent runtime divergence (CLM-42254 review).
   */
  public static final String POLICY_VIOLATION_WAIVER_STATUS_ACTIVE = "Active";

  public static final String POLICY_VIOLATION_WAIVER_STATUS_WAIVED = "Waived";

  public static final String POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED = "AutoWaived";

  public static final String POLICY_VIOLATION_WAIVER_STATUS_LEGACY = "Legacy";

  /**
   * Canonical indexed {@code componentViolationState} values written onto
   * {@code NON_VULNERABLE_COMPONENT} docs (the Components leg violationStates filter). Lower-cased so
   * the exact-match keyword filter is case-stable. Derived from the same waiver-status classification
   * as {@link #POLICY_VIOLATION_WAIVER_STATUS_ACTIVE} et al: Active&nbsp;&rarr;&nbsp;{@code open},
   * pure-legacy (non-waived)&nbsp;&rarr;&nbsp;{@code legacy}, Waived/AutoWaived&nbsp;&rarr;&nbsp;{@code
   * waived}. Legacy is a distinct grandfathered-in state: a pure-legacy violation is neither open nor
   * waived. A violation that is both waived and legacy classifies as {@code waived}, since
   * {@link #deriveWaiverStatus} resolves it to Waived (waiver precedence).
   */
  public static final String COMPONENT_VIOLATION_STATE_OPEN = "open";

  public static final String COMPONENT_VIOLATION_STATE_WAIVED = "waived";

  public static final String COMPONENT_VIOLATION_STATE_LEGACY = "legacy";

  private static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX_EVAL = "AdvancedSearch.createSearchIndex.eval";

  private static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX_COMPONENT =
      "AdvancedSearch.createSearchIndex.component";

  private static final int EVAL_THREADS_MIN = 1;

  private static final int EVAL_THREADS_MAX = Integer.MAX_VALUE;

  private static final int EVAL_THREADS_DEFAULT = 8;

  private static final int COMPONENT_THREADS_MIN = 1;

  private static final int COMPONENT_THREADS_MAX = Integer.MAX_VALUE;

  private static final int COMPONENT_THREADS_DEFAULT = 8;

  private static final Logger log = LoggerFactory.getLogger(DocumentBuilderHelper.class);

  private final LabelDAO labelDAO;

  private final OrganizationDAO organizationDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final TagDAO tagDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ReportService reportService;

  private final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  private final TenantReference<TenantThreadPoolExecutor> evalExecutors;

  private final TenantReference<TenantThreadPoolExecutor> componentExecutors;

  private final ShutdownHandler shutdownHandler;

  private final PolicyViolationDAO policyViolationDAO;

  private final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO;

  private final MultiLicenseDAO multiLicenseDAO;

  private final LicenseThreatGroupDAO licenseThreatGroupDAO;

  private final PolicyWaiverDAO policyWaiverDAO;

  private final AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  private final PolicyWaiverReasonDAO policyWaiverReasonDAO;

  private final PolicyWaiverRequestDAO policyWaiverRequestDAO;

  @Inject
  public DocumentBuilderHelper(
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final TagDAO tagDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      final ComponentLoaderFactory componentLoaderFactory,
      final ReportService reportService,
      final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher,
      final ShutdownHandler shutdownHandler,
      final PolicyViolationDAO policyViolationDAO,
      final PolicyViolationConstraintFactsDAO policyViolationConstraintFactsDAO,
      final MultiLicenseDAO multiLicenseDAO,
      final LicenseThreatGroupDAO licenseThreatGroupDAO,
      final PolicyWaiverDAO policyWaiverDAO,
      final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
      final PolicyWaiverReasonDAO policyWaiverReasonDAO,
      final PolicyWaiverRequestDAO policyWaiverRequestDAO)
  {
    this.labelDAO = labelDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.tagDAO = tagDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
    this.componentLoaderFactory = componentLoaderFactory;
    this.reportService = reportService;
    this.vulnerabilityDescriptionFetcher = vulnerabilityDescriptionFetcher;
    this.evalExecutors = new TenantReference<>();
    this.componentExecutors = new TenantReference<>();
    this.shutdownHandler = shutdownHandler;
    this.policyViolationDAO = policyViolationDAO;
    this.policyViolationConstraintFactsDAO = policyViolationConstraintFactsDAO;
    this.multiLicenseDAO = multiLicenseDAO;
    this.licenseThreatGroupDAO = licenseThreatGroupDAO;
    this.policyWaiverDAO = policyWaiverDAO;
    this.autoPolicyWaiverDAO = autoPolicyWaiverDAO;
    this.policyWaiverReasonDAO = policyWaiverReasonDAO;
    this.policyWaiverRequestDAO = policyWaiverRequestDAO;
  }

  // Visible for testing
  public ExecutorService getEvalExecutor() {
    return evalExecutors.computeIfAbsent(tenant -> {
      int evalThreadCount = DefaultExecutorThreadPools.getThreadCount(
          EVAL_THREADS_MIN,
          EVAL_THREADS_MAX,
          EVAL_THREADS_DEFAULT,
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX_EVAL);
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          evalThreadCount,
          evalThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("DocumentBuilderHelper-eval-%d").build(),
          new AbortPolicy(),
          "advanced_search_indexing_eval",
          getClass().getSimpleName());
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  // Visible for testing
  public ExecutorService getComponentExecutor() {
    return componentExecutors.computeIfAbsent(tenant -> {
      int componentThreadCount = DefaultExecutorThreadPools.getThreadCount(
          COMPONENT_THREADS_MIN,
          COMPONENT_THREADS_MAX,
          COMPONENT_THREADS_DEFAULT,
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX_COMPONENT);
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          componentThreadCount,
          componentThreadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat("DocumentBuilderHelper-component-%d").build(),
          new AbortPolicy(),
          "advanced_search_indexing_component",
          getClass().getSimpleName());
      tenantThreadPoolExecutor.allowCoreThreadTimeOut(true);
      shutdownHandler.add(tenantThreadPoolExecutor);
      return tenantThreadPoolExecutor;
    });
  }

  public List<Document> buildOrganizationDocs(
      IndexingContext indexingContext,
      Collection<Organization> organizations)
  {
    return buildOrganizationDocs(indexingContext, organizations, null);
  }

  /**
   * Variant that consumes a precomputed {@code parentOrgsByOrganization} map (as built by
   * {@code AbstractSearchIndexClient.computeParentsByOrganization}) so per-org indexing does not
   * re-issue {@code OwnerDAO.walkHierarchy} for every document.
   */
  public List<Document> buildOrganizationDocs(
      IndexingContext indexingContext,
      Collection<Organization> organizations,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    if (CollectionUtils.isEmpty(organizations)) {
      return Collections.emptyList();
    }
    return organizations.stream()
        .map(org -> buildDocument(indexingContext, org, parentOrgsByOrganization))
        .toList();
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Organization organization)
  {
    return buildDocument(indexingContext, organization, null);
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Organization organization,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    if (organization == null) {
      return null;
    }
    List<String> allowedContextIds = resolveClosure(indexingContext, parentOrgsByOrganization, organization, null);
    return new DocumentBuilder(ItemType.ORGANIZATION)
        .setOwner(organization)
        .setAllowedContextIds(allowedContextIds)
        .build();
  }

  /**
   * The permission-filter closure for a document: the owning app id (if any) plus {@code org} and
   * its ancestors. Sentinel ids ({@link Organization#ROOT_ORGANIZATION_ID},
   * {@link MembershipMapping#GLOBAL_CONTEXT_ID}) are omitted — holders of either bypass the filter
   * entirely, so indexing them would waste a term per doc.
   *
   * <p>
   * Resolves ancestors via {@link IndexingContext#getAncestorOrgIds(Organization)}, which memoizes
   * the {@code walkHierarchy} DB walk per org per run — so orgs shared across many docs
   * (labels/policies/tags/apps under one org) walk once, not once per document. Prefer this over
   * {@code OwnerDAO.getAncestorIdsByApplicationIds} (SQL per call, app-owned only).
   */
  List<String> computeAllowedContextIds(IndexingContext indexingContext, Organization org, String ownerAppId) {
    // Route the hierarchy walk through IndexingContext's per-run cache so orgs shared across many
    // docs (labels/policies/tags/apps under one org) walk at most once, not once per document.
    List<String> ancestorOrgIds = org == null ? List.of() : indexingContext.getAncestorOrgIds(org);
    return closureFrom(ancestorOrgIds, ownerAppId);
  }

  /**
   * As {@link #computeAllowedContextIds(IndexingContext, Organization, String)} but for callers that
   * already hold the precomputed ancestor chain ({org, parent, ..., root}).
   *
   * <p>
   * Callers select this overload via {@code parentOrgsByOrganization.containsKey(org)} — an
   * identity-keyed lookup (Organization overrides neither equals nor hashCode), safe only because
   * the map and the doc builders share the same Organization instances via {@link IndexingContext}.
   * A miss is not a correctness bug: it falls back to the (IndexingContext, Organization) overload,
   * whose walk is itself memoized per org per run — so a mismatched instance costs one cached walk,
   * not a per-document DB round-trip.
   */
  List<String> computeAllowedContextIds(Collection<Organization> ancestorOrgs, String ownerAppId) {
    List<String> ancestorOrgIds = ancestorOrgs == null
        ? List.of()
        : ancestorOrgs.stream().filter(Objects::nonNull).map(Organization::getId).toList();
    return closureFrom(ancestorOrgIds, ownerAppId);
  }

  /**
   * Resolves the permission closure for a doc, preferring the precomputed ancestor map when it
   * holds {@code org} (identity-keyed) and otherwise falling back to the IndexingContext-cached
   * walk. Centralizes the map-dispatch ternary shared by the org/app/SBOM build paths.
   */
  private List<String> resolveClosure(
      final IndexingContext indexingContext,
      final Map<Organization, Collection<Organization>> parentOrgsByOrganization,
      final Organization org,
      final String ownerAppId)
  {
    // Identity-keyed lookup (Organization overrides neither equals nor hashCode): callers MUST pass
    // the same Organization instance that built parentOrgsByOrganization — i.e. the org added to
    // IndexingContext via addOwners in doPopulateIndex, NOT a separate ownerDAO.getById re-fetch.
    // A re-fetched instance silently misses here and falls through to the IndexingContext-cached
    // walk below (correctness-safe, but a per-doc perf/behavior surprise).
    if (parentOrgsByOrganization != null && parentOrgsByOrganization.containsKey(org)) {
      return computeAllowedContextIds(parentOrgsByOrganization.get(org), ownerAppId);
    }
    return computeAllowedContextIds(indexingContext, org, ownerAppId);
  }

  /**
   * Shared closure builder: owning app id (if any) followed by the ancestor org ids, sentinels
   * ({@code ROOT_ORGANIZATION_ID}, {@code GLOBAL_CONTEXT_ID}) omitted.
   */
  private List<String> closureFrom(final Collection<String> ancestorOrgIds, final String ownerAppId) {
    List<String> ids = new ArrayList<>();
    if (ownerAppId != null) {
      ids.add(ownerAppId);
    }
    for (String id : ancestorOrgIds) {
      if (id != null && !isSentinelContextId(id)) {
        ids.add(id);
      }
    }
    return ids;
  }

  private static boolean isSentinelContextId(String id) {
    return Organization.ROOT_ORGANIZATION_ID.equals(id) || MembershipMapping.GLOBAL_CONTEXT_ID.equals(id);
  }

  public List<Document> buildApplicationDocs(
      IndexingContext indexingContext,
      Collection<Application> applications)
  {
    return buildApplicationDocs(indexingContext, applications, null);
  }

  /**
   * Variant that consumes a precomputed {@code parentOrgsByOrganization} map (as built by
   * {@code AbstractSearchIndexClient.computeParentsByOrganization}) so per-application indexing
   * does not re-issue {@code OwnerDAO.walkHierarchy} for every document.
   */
  public List<Document> buildApplicationDocs(
      IndexingContext indexingContext,
      Collection<Application> applications,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    if (CollectionUtils.isEmpty(applications)) {
      return Collections.emptyList();
    }
    // Pre-compute the two per-app rollups (latest evaluation time, stage x severity violation counts)
    // across ALL apps in this reindex batch and memoize them on the IndexingContext (mirroring
    // applicationCategoryNames) so we issue a bounded number of queries (one grouped evaluation
    // query + one chunked IN-clause violations query) for the whole reindex instead of two per app.
    Set<String> applicationIds = applications.stream()
        .filter(Objects::nonNull)
        .map(Application::getId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    // Pre-warm the per-app caches on indexingContext; return values are intentionally discarded
    // (the per-app buildDocument calls below read the warmed caches).
    latestEvaluationEpochMsByApp(indexingContext, applicationIds);
    violationRollupByApp(indexingContext, applicationIds);
    categoryNamesByApp(indexingContext, applicationIds);
    return applications.stream()
        .map(app -> buildDocument(indexingContext, app, parentOrgsByOrganization))
        .toList();
  }

  public Document buildDocument(IndexingContext indexingContext, Application application) {
    return buildDocument(indexingContext, application, null);
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    if (application == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(application.getOrganizationId());
    if (!(owner instanceof Organization org)) {
      log.warn("Application {} has non-organization owner {} (id={}); skipping",
          application.getId(),
          owner == null ? "null" : owner.getClass().getSimpleName(),
          application.getOrganizationId());
      return null;
    }
    List<String> allowedContextIds =
        resolveClosure(indexingContext, parentOrgsByOrganization, org, application.getId());
    DocumentBuilder builder = new DocumentBuilder(ItemType.APPLICATION)
        .setOwner(application)
        .setOwner(org)
        .setAllowedContextIds(allowedContextIds);

    // The category and rollup lookups are keyed by application id and warmed via Set.of(id), which
    // rejects a null element. The batch path (buildApplicationDocs) filters null ids upstream, so
    // mirror that here: an app with no id simply omits the category / rollup fields.
    String applicationId = application.getId();
    if (applicationId != null) {
      List<String> categoryNames = applicationCategoryNames(indexingContext, applicationId);
      if (!categoryNames.isEmpty()) {
        builder.setApplicationCategoryNames(categoryNames);
      }

      // Both rollups are memoized on the IndexingContext: the full-reindex path warms them once for
      // the whole app batch (buildApplicationDocs), and the single-app path warms them lazily for a
      // one-element set. A get() miss means the app has no evaluation / no unfixed violations.
      Long lastEvaluationEpochMs =
          latestEvaluationEpochMsByApp(indexingContext, Set.of(applicationId)).get(applicationId);
      if (lastEvaluationEpochMs != null) {
        builder.setApplicationLastEvaluationTimeEpochMs(lastEvaluationEpochMs);
      }

      IndexingContext.ViolationRollup rollup =
          violationRollupByApp(indexingContext, Set.of(applicationId)).get(applicationId);
      if (rollup != null) {
        if (!rollup.stageSeverityTokens().isEmpty()) {
          builder.setApplicationStageSeverityCounts(rollup.stageSeverityTokens());
        }
        builder.setApplicationMaxPolicyThreatLevel(rollup.maxThreatLevel());
        builder.setApplicationViolationStages(rollup.stages());
        builder.setApplicationViolationPolicyTypes(rollup.policyTypes());
        builder.setApplicationViolationStates(rollup.states());
        builder.setApplicationViolationStateSortOrdinal(rollup.stateSortOrdinal());
      }
    }

    return builder.build();
  }

  /**
   * Category (tag) names for an application, memoized on {@code indexingContext} and warmed once per
   * run by a single chunked IN-clause {@link TagDAO#getByApplicationIdsGrouped} query (which
   * preserves the app-to-tag association and auto-chunks large id sets). Empty (not null) when the
   * app has no categories (or has no id) so callers uniformly skip an absent field. {@link Set#of}
   * rejects a null element, so a null id short-circuits to empty here.
   */
  private List<String> applicationCategoryNames(final IndexingContext indexingContext, final String applicationId) {
    if (applicationId == null) {
      return Collections.emptyList();
    }
    return categoryNamesByApp(indexingContext, Set.of(applicationId))
        .getOrDefault(applicationId, Collections.emptyList());
  }

  /**
   * Per-app category (tag) names, memoized on {@code indexingContext} and warmed once per run by a
   * single chunked IN-clause {@link TagDAO#getByApplicationIdsGrouped} query. An app with no
   * categories is absent from the map (so the doc omits the field).
   */
  private Map<String, List<String>> categoryNamesByApp(
      final IndexingContext indexingContext,
      final Set<String> applicationIds)
  {
    return indexingContext.getCategoryNamesByApp(applicationIds, this::loadCategoryNamesByApp);
  }

  private Map<String, List<String>> loadCategoryNamesByApp(final Set<String> applicationIds) {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return Collections.emptyMap();
    }
    Map<String, List<String>> namesByApp = new HashMap<>();
    tagDAO.getByApplicationIdsGrouped(applicationIds).forEach((appId, tags) -> {
      List<String> names = tags.stream()
          .map(Tag::getName)
          .filter(Objects::nonNull)
          .toList();
      if (!names.isEmpty()) {
        namesByApp.put(appId, names);
      }
    });
    return namesByApp;
  }

  /**
   * Delimiter separating the {@code stage}, {@code severity} and {@code count} segments of an
   * {@code applicationStageSeverityCount} token. Shared by {@link #encodeStageSeverityCount} so the
   * encode side and any decode side agree on one wire contract.
   */
  static final char STAGE_SEVERITY_COUNT_DELIMITER = ':';

  /**
   * Encodes one {@code applicationStageSeverityCount} token as
   * {@code stageId + ':' + severity + ':' + count}, where {@code severity} is the lowercase
   * {@link ThreatLevel} name. The inverse is {@link #decodeStageSeverityCount}.
   */
  static String encodeStageSeverityCount(final String stageId, final ThreatLevel level, final int count) {
    // stageId must be delimiter-free or it would corrupt the token / break decodeStageSeverityCount.
    // Holds today because all stage ids come from the ALL_STAGE_IDS registry; guard makes it explicit
    // for any future custom stage id.
    Preconditions.checkArgument(stageId.indexOf(STAGE_SEVERITY_COUNT_DELIMITER) < 0,
        "stageId must not contain the '%s' delimiter: %s", STAGE_SEVERITY_COUNT_DELIMITER, stageId);
    return stageId + STAGE_SEVERITY_COUNT_DELIMITER + level.name().toLowerCase(Locale.ROOT)
        + STAGE_SEVERITY_COUNT_DELIMITER + count;
  }

  /**
   * Splits an {@code applicationStageSeverityCount} token back into its {@code [stageId, severity,
   * count]} segments. Inverse of {@link #encodeStageSeverityCount}; the count segment is left as a
   * string for the caller to parse. Throws {@link IllegalArgumentException} on a malformed token.
   */
  static String[] decodeStageSeverityCount(final String token) {
    String[] parts = token.split(String.valueOf(STAGE_SEVERITY_COUNT_DELIMITER), 3);
    if (parts.length != 3) {
      throw new IllegalArgumentException("Malformed stage:severity:count token: " + token);
    }
    return parts;
  }

  /**
   * Latest-evaluation epoch-millis per app, memoized on {@code indexingContext} and warmed once per
   * run by a single grouped {@link PolicyEvaluationDAO#getLastByApplicationIdsAndStageIds} query
   * (which itself handles large app sets). The returned map's values are the max
   * {@link PolicyEvaluation#getTime} across each app's per-stage latest evaluations; apps with no
   * evaluation rows are absent (so the doc omits the field and reads as "never evaluated").
   */
  private Map<String, Long> latestEvaluationEpochMsByApp(
      final IndexingContext indexingContext,
      final Set<String> applicationIds)
  {
    return indexingContext.getLatestEvaluationEpochMsByApp(applicationIds, this::loadLatestEvaluationEpochMsByApp);
  }

  private Map<String, Long> loadLatestEvaluationEpochMsByApp(final Set<String> applicationIds) {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return Collections.emptyMap();
    }
    Map<String, Long> latestByApp = new HashMap<>();
    for (PolicyEvaluation evaluation : policyEvaluationDAO.getLastByApplicationIdsAndStageIds(applicationIds,
        ALL_STAGE_IDS))
    {
      if (evaluation == null || evaluation.getApplicationId() == null || evaluation.getTime() == null) {
        continue;
      }
      latestByApp.merge(evaluation.getApplicationId(), evaluation.getTime().getTime(), Math::max);
    }
    return latestByApp;
  }

  /**
   * Per-app combined {@link IndexingContext.ViolationRollup}, memoized on {@code indexingContext} and
   * warmed once per run by a SINGLE chunked IN-clause {@link PolicyViolationDAO#getUnfixedByApplicationIds}
   * query (which auto-chunks large collections). One widened fetch backs both the active-only display
   * pills and the denormalized filter/sort aggregates — no extra query, no N+1.
   */
  private Map<String, IndexingContext.ViolationRollup> violationRollupByApp(
      final IndexingContext indexingContext,
      final Set<String> applicationIds)
  {
    return indexingContext.getViolationRollupByApp(applicationIds, this::loadViolationRollupByApp);
  }

  /**
   * Builds every app's {@link IndexingContext.ViolationRollup} from ONE widened
   * {@link PolicyViolationDAO#getUnfixedByApplicationIds} fetch (unfixed = active + waived + legacy),
   * classifying each violation once:
   * <ul>
   * <li>the ACTIVE-only stage:severity:count pills, max threat level, stages and policy-type sets are
   * accumulated from {@code violation.isActive()} rows only — so widening the fetch does NOT change the
   * active-only display pills;</li>
   * <li>the violation-state set (open/waived/legacy) is classified over the whole unfixed set, so waived
   * and legacy states surface;</li>
   * <li>the worst (min) state-sort ordinal is the min priority across that state set.</li>
   * </ul>
   * A violation on a stage outside {@link #ALL_STAGE_IDS} is dropped from the stage rollups. An app with
   * no unfixed violation is absent from the map (so the doc omits every field).
   */
  private Map<String, IndexingContext.ViolationRollup> loadViolationRollupByApp(final Set<String> applicationIds) {
    if (CollectionUtils.isEmpty(applicationIds)) {
      return Collections.emptyMap();
    }
    List<PolicyViolation> violations = policyViolationDAO.getUnfixedByApplicationIds(applicationIds);
    if (CollectionUtils.isEmpty(violations)) {
      return Collections.emptyMap();
    }
    Map<String, RollupAccumulator> accumulators = new HashMap<>();
    for (PolicyViolation violation : violations) {
      String appId = violation.getApplicationId();
      if (appId == null) {
        continue;
      }
      accumulators.computeIfAbsent(appId, id -> new RollupAccumulator()).add(violation);
    }
    Map<String, IndexingContext.ViolationRollup> rollupByApp = new HashMap<>();
    accumulators.forEach((appId, acc) -> rollupByApp.put(appId, acc.toRollup()));
    return rollupByApp;
  }

  /** Violation-state tokens surfaced on APPLICATION docs; the ordinal mirrors the prototype's priority. */
  private static final String STATE_OPEN = "open";

  private static final String STATE_WAIVED = "waived";

  private static final String STATE_LEGACY = "legacy";

  /**
   * Prototype VIOLATION_STATE_PRIORITY: Open sorts before Waived before Legacy (ascending). An
   * unknown token sorts last ({@link Integer#MAX_VALUE}) and is logged rather than throwing, so a
   * future state or unexpected token degrades one app's sort ordinal instead of aborting the whole
   * indexing batch (consistent with the drop-unknown-stage handling earlier in this class).
   */
  private static int stateSortPriority(final String state) {
    return switch (state) {
      case STATE_OPEN -> 0;
      case STATE_WAIVED -> 1;
      case STATE_LEGACY -> 2;
      default -> {
        log.warn("Unknown violation state {}; sorting it last", state);
        yield Integer.MAX_VALUE;
      }
    };
  }

  /**
   * Single-pass per-app accumulator. Active rows feed the display pills, max threat level, stages and
   * policy-type sets; every unfixed row (active/waived/legacy) feeds the state set. Legacy is checked
   * before waived so a legacy-and-waived violation classifies as legacy (matching the isActive gate,
   * which excludes both).
   */
  private static final class RollupAccumulator
  {
    private final Map<String, Map<ThreatLevel, Integer>> activeCountByStageAndLevel = new HashMap<>();

    private final Set<String> activeStages = new HashSet<>();

    private final Set<String> activePolicyTypes = new HashSet<>();

    private final Set<String> states = new HashSet<>();

    private Integer maxActiveThreatLevel = null;

    void add(final PolicyViolation violation) {
      if (violation.isLegacyViolation()) {
        states.add(STATE_LEGACY);
      }
      else if (violation.isWaived()) {
        states.add(STATE_WAIVED);
      }
      else {
        states.add(STATE_OPEN);
      }
      if (!violation.isActive()) {
        return;
      }
      int threatLevel = violation.getThreatLevel();
      maxActiveThreatLevel = maxActiveThreatLevel == null ? threatLevel : Math.max(maxActiveThreatLevel, threatLevel);
      PolicyThreatCategory category = violation.getThreatCategory();
      if (category != null) {
        activePolicyTypes.add(category.getName());
      }
      String stageId = violation.getStageTypeId();
      if (stageId != null && ALL_STAGE_IDS.contains(stageId)) {
        activeStages.add(stageId);
        activeCountByStageAndLevel
            .computeIfAbsent(stageId, id -> new HashMap<>())
            .merge(ThreatLevel.from(threatLevel), 1, Integer::sum);
      }
    }

    IndexingContext.ViolationRollup toRollup() {
      List<String> tokens = new ArrayList<>();
      activeCountByStageAndLevel.forEach((stageId, countByLevel) -> countByLevel.forEach(
          (level, count) -> tokens.add(encodeStageSeverityCount(stageId, level, count))));
      // states is non-empty here (an accumulator exists only for an app with >=1 unfixed violation),
      // so the ordinal is always present; an app with no unfixed violation is absent from the map and
      // has no ordinal, so it sorts last under the ascending violation-state sort.
      OptionalInt minStateOrdinal = states.stream().mapToInt(DocumentBuilderHelper::stateSortPriority).min();
      Integer stateSortOrdinal = minStateOrdinal.isPresent() ? minStateOrdinal.getAsInt() : null;
      return new IndexingContext.ViolationRollup(
          tokens, maxActiveThreatLevel, activeStages, activePolicyTypes, states, stateSortOrdinal);
    }
  }

  /**
   * The ids of every {@link StageType} in the global {@link StageTypes} registry, computed once.
   * {@code StageTypes} is a static, tenant-independent registry, so this is safe to share across
   * runs and tenants (CLAUDE.md §8 static allocation).
   */
  private static final Set<String> ALL_STAGE_IDS =
      StageTypes.getAll().stream().map(StageType::getId).collect(Collectors.toUnmodifiableSet());

  public List<Document> buildTagDocs(IndexingContext indexingContext) {
    return tagDAO.getAll().stream().map(tag -> buildDocument(indexingContext, tag)).toList();
  }

  public Document buildDocument(IndexingContext indexingContext, Tag tag) {
    if (tag == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(tag.getOrganizationId());
    if (owner == null) {
      return null;
    }
    return new DocumentBuilder(ItemType.APPLICATION_CATEGORY)
        .setApplicationCategoryId(tag.getId())
        .setApplicationCategoryName(tag.getName())
        .setApplicationCategoryColor(tag.getColor())
        .setApplicationCategoryDescription(tag.getDescription())
        .setOwner(owner)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner))
        .build();
  }

  /**
   * Permission-filter closure for a doc with a polymorphic owner (org or app) — Labels, Policies,
   * Tags/categories. Resolves the owner via {@code indexingContext.getOwner} to share one owner
   * cache per run rather than a {@code getById} per document.
   *
   * <p>
   * Fail-closed: if an app owner's org cannot be resolved (orphan app — e.g. a transient cache miss
   * during a reindex/org-delete race) this returns an empty closure, matching the empty-closure
   * semantics of {@code setAllowedContextIds}. An empty closure suppresses the doc from
   * permission-filtered results (invisible) rather than leaving it app-only-visible, consistent with
   * the rest of the permission model. The orphan is logged via {@link #logOrphanApp}.
   */
  List<String> computeAllowedContextIdsForOwner(IndexingContext indexingContext, Owner owner) {
    if (owner == null) {
      return Collections.emptyList();
    }
    if (owner instanceof Application app) {
      Owner orgOwner = indexingContext.getOwner(app.getOrganizationId());
      if (orgOwner instanceof Organization org) {
        return computeAllowedContextIds(indexingContext, org, app.getId());
      }
      logOrphanApp(indexingContext, app.getOrganizationId(), app.getId());
      return Collections.emptyList();
    }
    if (owner instanceof Organization org) {
      return computeAllowedContextIds(indexingContext, org, null);
    }
    log.warn("Unsupported owner type for allowedContextIds closure: {}", owner.getClass().getSimpleName());
    return Collections.emptyList();
  }

  /**
   * Logs the "cannot resolve owning organization" condition: WARN the first time an app is seen this
   * reindex run (so a systemic mis-config surfaces), DEBUG on repeats (so it does not flood). The
   * per-run dedupe lives on {@link IndexingContext}, so a recurring orphan re-WARNs on the next run.
   */
  private void logOrphanApp(
      final IndexingContext indexingContext,
      final String organizationId,
      final String applicationId)
  {
    String message = "Cannot resolve owning organization {} for application {}; permission closure is empty "
        + "(fail-closed) so the doc is suppressed from permission-filtered results until the org resolves.";
    if (indexingContext.shouldWarnOrphanApp(applicationId)) {
      log.warn(message, organizationId, applicationId);
    }
    else {
      log.debug(message, organizationId, applicationId);
    }
  }

  public List<Document> buildLabelDocs(IndexingContext indexingContext) {
    return labelDAO.getAll().stream().map(label -> buildDocument(indexingContext, label)).toList();
  }

  public Document buildDocument(IndexingContext indexingContext, Label label) {
    if (label == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(label.getOwnerId());
    if (owner == null) {
      return null;
    }
    return new DocumentBuilder(ItemType.COMPONENT_LABEL)
        .setComponentLabelId(label.getId())
        .setComponentLabelName(label.getLabel())
        .setComponentLabelColor(label.getColor())
        .setComponentLabelDescription(label.getDescription())
        .setOwner(owner)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner))
        .build();
  }

  public List<Document> buildPolicyDocs(IndexingContext indexingContext) {
    return policyDAO.getAll().stream().map(policy -> buildDocument(indexingContext, policy)).toList();
  }

  public Document buildDocument(IndexingContext indexingContext, Policy policy) {
    if (policy == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(policy.getOwnerId());
    if (owner == null) {
      return null;
    }
    return new DocumentBuilder(ItemType.POLICY)
        .setPolicyId(policy.getId())
        .setPolicyName(policy.getName())
        .setPolicyThreatCategory(policy.getThreatCategory())
        .setPolicyThreatLevel(policy.getThreatLevel())
        .setOwner(owner)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner))
        .build();
  }

  /**
   * Full-reindex docs for both waiver kinds: manual {@link PolicyWaiver} and auto
   * {@link AutoPolicyWaiver}. Container-image waivers are excluded by {@code buildDocument}.
   */
  public List<Document> buildPolicyWaiverDocs(IndexingContext indexingContext) {
    // Loads the waiver corpus with getAll(), matching the sibling full-reindex paths (buildTagDocs /
    // buildLabelDocs / buildPolicyDocs). Container-image waivers are never indexed (buildDocument
    // returns null), so drop them up front
    // rather than folding their policy/reason ids into the batch lookups only to discard the docs.
    // TODO(CLM-41642): verify at large-tenant scale before broad rollout; shares the pre-existing
    // unpaged getAll() risk of buildTagDocs/buildLabelDocs/buildPolicyDocs (streaming is a shared
    // future refactor for all builders).
    List<PolicyWaiver> manualWaivers = policyWaiverDAO.getAll()
        .stream()
        .filter(waiver -> !waiver.isForContainerImage() && !waiver.isForContainerImageComponent())
        .toList();

    // Batch-load the policies and reasons referenced across all manual waivers so the full reindex
    // issues one IN-clause query per lookup instead of a getById per waiver (avoids 2*N point queries).
    Set<String> policyIds = manualWaivers.stream()
        .map(PolicyWaiver::getPolicyId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Set<String> reasonIds = manualWaivers.stream()
        .map(PolicyWaiver::getWaiverReasonId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Policy> policiesById = loadPoliciesByIds(policyIds);
    Map<String, PolicyWaiverReason> reasonsById = loadReasonsByIds(reasonIds);

    List<Document> docs = new ArrayList<>();
    for (PolicyWaiver waiver : manualWaivers) {
      // buildDocument still returns null for unresolvable-owner / repository-owner waivers.
      Document doc = buildDocument(indexingContext, waiver, policiesById, reasonsById);
      if (doc != null) {
        docs.add(doc);
      }
    }
    for (AutoPolicyWaiver autoWaiver : autoPolicyWaiverDAO.getAll()) {
      Document doc = buildDocument(indexingContext, autoWaiver);
      if (doc != null) {
        docs.add(doc);
      }
    }
    return docs;
  }

  /**
   * Policy-rebuild path: rebuild docs for the manual waivers of a single policy (rename / threat
   * change). All waivers share {@code policy}, so the reasons are batch-loaded with one
   * {@code getAllByIds} instead of a {@code getById} per waiver. Container-image waivers are dropped
   * up front (they are never indexed); auto-waivers are not rebuilt here since their docs key on
   * threat level, not policy name.
   */
  public List<Document> buildPolicyWaiverDocsForPolicy(
      IndexingContext indexingContext,
      Collection<PolicyWaiver> waivers,
      Policy policy)
  {
    List<PolicyWaiver> manualWaivers = waivers.stream()
        .filter(waiver -> !waiver.isForContainerImage() && !waiver.isForContainerImageComponent())
        .toList();

    Set<String> reasonIds = manualWaivers.stream()
        .map(PolicyWaiver::getWaiverReasonId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, PolicyWaiverReason> reasonsById = loadReasonsByIds(reasonIds);
    Map<String, Policy> policiesById = policy == null ? Collections.emptyMap() : Map.of(policy.getId(), policy);

    List<Document> docs = new ArrayList<>();
    for (PolicyWaiver waiver : manualWaivers) {
      Document doc = buildDocument(indexingContext, waiver, policiesById, reasonsById);
      if (doc != null) {
        docs.add(doc);
      }
    }
    return docs;
  }

  // Public: called cross-package by AbstractSearchIndexClient.updateIndexForPolicyWaiver.
  public Document buildDocument(IndexingContext indexingContext, PolicyWaiver waiver) {
    // Incremental single-doc path: resolve the policy directly, then reuse the preloaded-policy path.
    String policyId = waiver == null ? null : waiver.getPolicyId();
    Policy policy = policyId == null ? null : policyDAO.getById(policyId);
    return buildDocument(indexingContext, waiver, policy);
  }

  /**
   * Single-doc path with a pre-loaded {@link Policy}. Used by the policy-rebuild path in
   * AbstractSearchIndexClient, where all waivers from {@code getByPolicyId} share one policy that
   * has already been fetched, so re-resolving it per waiver would be a redundant point query.
   */
  public Document buildDocument(IndexingContext indexingContext, PolicyWaiver waiver, Policy policy) {
    String reasonId = waiver == null ? null : waiver.getWaiverReasonId();
    PolicyWaiverReason reason = reasonId == null ? null : policyWaiverReasonDAO.getById(reasonId);
    return buildDocument(
        indexingContext,
        waiver,
        policy == null ? Collections.emptyMap() : Map.of(policy.getId(), policy),
        reason == null ? Collections.emptyMap() : Map.of(reasonId, reason));
  }

  private Document buildDocument(
      IndexingContext indexingContext,
      PolicyWaiver waiver,
      Map<String, Policy> policiesById,
      Map<String, PolicyWaiverReason> reasonsById)
  {
    if (waiver == null) {
      return null;
    }
    // Container-image waivers are managed on the Firewall container surface, not Global Search.
    if (waiver.isForContainerImage() || waiver.isForContainerImageComponent()) {
      return null;
    }
    Owner owner = indexingContext.getOwner(waiver.getOwnerId());
    // v1 indexes only app/org-scoped waivers; repository / repo-manager / repo-container owners have
    // no allowedContextIds closure, so they are out of scope rather than indexed with empty perms.
    if (!isIndexableOwner(owner)) {
      return null;
    }

    String policyName = null;
    Integer threatLevel = null;
    PolicyThreatCategory policyType = null;
    if (waiver.getPolicyId() != null) {
      Policy policy = policiesById.get(waiver.getPolicyId());
      if (policy != null) {
        policyName = policy.getName();
        threatLevel = policy.getThreatLevel();
        policyType = threatCategoryOrNull(policy);
      }
    }
    // A manual waiver whose policy cannot be resolved (orphaned policy) indexes with a null policy
    // name, so it lands in the null bucket of the POLICY_WAIVER_POLICY_NAME group field. Accepted
    // for v1; a synthetic fallback title would be a product decision.

    String reasonText = null;
    if (waiver.getWaiverReasonId() != null) {
      PolicyWaiverReason reason = reasonsById.get(waiver.getWaiverReasonId());
      if (reason != null) {
        reasonText = reason.getReasonText();
      }
    }

    DocumentBuilder builder = new DocumentBuilder(ItemType.POLICY_WAIVER)
        .setPolicyWaiverId(waiver.getId())
        .setPolicyWaiverPolicyName(policyName)
        .setPolicyWaiverPolicyId(waiver.getPolicyId())
        .setPolicyWaiverReason(reasonText)
        .setPolicyWaiverComment(waiver.getComment())
        .setPolicyWaiverCreatedAt(toIso8601(waiver.getCreateTime()))
        .setPolicyWaiverCreatedAtEpochMs(toEpochMs(waiver.getCreateTime()))
        .setPolicyWaiverExpiresAt(toIso8601(waiver.getExpiryTime()))
        .setPolicyWaiverExpiresAtEpochMs(toEpochMs(waiver.getExpiryTime()))
        .setPolicyWaiverScopeOwnerId(owner.getId())
        .setPolicyWaiverScopeOwnerType(owner.getType().name())
        .setPolicyWaiverScope(scopeFor(owner, isComponentTargeted(waiver)))
        .setPolicyWaiverWaivedBy(waiver.getCreatorName())
        .setPolicyWaiverAuto(false)
        .setPolicyWaiverIsAuto(false)
        .setPolicyWaiverExpiryStatus(computeExpiryStatus(waiver.getExpiryTime()))
        .setPolicyWaiverPolicyType(policyType)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner));
    applyWaiverOwnerHierarchy(builder, indexingContext, owner);
    // Orphaned / null policy → no policyWaiverThreatLevel. Those docs are invisible to the
    // policyThreatLevel range filter (no indexed numeric field to match).
    if (threatLevel != null) {
      builder.setPolicyWaiverThreatLevel(threatLevel);
    }
    return builder.build();
  }

  /**
   * Full-reindex docs for waiver REQUESTS ({@link PolicyWaiverRequest}), a distinct
   * {@code ItemType.POLICY_WAIVER_REQUEST} unioned into the WAIVER query type. Mirrors
   * {@link #buildPolicyWaiverDocs} exactly: batch-load the referenced policies/reasons with one
   * IN-clause query each (not a getById per request), skip non-indexable (repository-family) owners,
   * and reuse the identical {@code computeAllowedContextIdsForOwner} closure so request-doc RBAC is
   * byte-for-byte the same as waiver-doc RBAC (MTIQ-critical). Approved requests are indexed too (for
   * completeness); the {@code waiverStates} filter surface deliberately omits APPROVED, so the
   * requested/rejected tabs never select them (the API-only {@code status} TERMS filter can still
   * target any status value directly).
   */
  public List<Document> buildPolicyWaiverRequestDocs(IndexingContext indexingContext) {
    // TODO(CLM-41642): verify at large-tenant scale before broad rollout; shares the pre-existing
    // unpaged getAll() risk of the sibling builders (streaming is a shared future refactor).
    List<PolicyWaiverRequest> requests = policyWaiverRequestDAO.getAll();

    Set<String> policyIds = requests.stream()
        .map(PolicyWaiverRequest::getPolicyId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Set<String> reasonIds = requests.stream()
        .map(PolicyWaiverRequest::getWaiverReasonId)
        .filter(Objects::nonNull)
        .collect(Collectors.toSet());
    Map<String, Policy> policiesById = loadPoliciesByIds(policyIds);
    Map<String, PolicyWaiverReason> reasonsById = loadReasonsByIds(reasonIds);

    List<Document> docs = new ArrayList<>();
    for (PolicyWaiverRequest request : requests) {
      Document doc = buildDocument(indexingContext, request, policiesById, reasonsById);
      if (doc != null) {
        docs.add(doc);
      }
    }
    return docs;
  }

  /**
   * Incremental single-doc path keyed on the raw request id: loads the {@link PolicyWaiverRequest}
   * and builds its doc (null if the request is gone or its owner is non-indexable). Keeps the request
   * DAO lookup on this helper so {@code AbstractSearchIndexClient} needs no extra injected DAO.
   */
  public Document buildPolicyWaiverRequestDocById(IndexingContext indexingContext, String requestId) {
    PolicyWaiverRequest request = requestId == null ? null : policyWaiverRequestDAO.getById(requestId);
    return request == null ? null : buildDocument(indexingContext, request);
  }

  // Single-doc rebuild entry point (AbstractSearchIndexClient.updateIndexForPolicyWaiverRequest calls
  // buildPolicyWaiverRequestDocById, which delegates here after loading the request's policy/reason).
  public Document buildDocument(IndexingContext indexingContext, PolicyWaiverRequest request) {
    String policyId = request == null ? null : request.getPolicyId();
    Policy policy = policyId == null ? null : policyDAO.getById(policyId);
    String reasonId = request == null ? null : request.getWaiverReasonId();
    PolicyWaiverReason reason = reasonId == null ? null : policyWaiverReasonDAO.getById(reasonId);
    return buildDocument(
        indexingContext,
        request,
        policy == null ? Collections.emptyMap() : Map.of(policy.getId(), policy),
        reason == null ? Collections.emptyMap() : Map.of(reasonId, reason));
  }

  private Document buildDocument(
      IndexingContext indexingContext,
      PolicyWaiverRequest request,
      Map<String, Policy> policiesById,
      Map<String, PolicyWaiverReason> reasonsById)
  {
    if (request == null) {
      return null;
    }
    // Container-image requests are managed on the Firewall container surface, not Global Search,
    // mirroring the committed-waiver guard (:893). A committed docker-component waiver is dropped
    // there, so its request must be dropped here too or the request would surface where the waiver
    // is hidden. The request has no persisted isForContainerImage flag, so infer it from the docker
    // component format that ApiPolicyWaiverRequestService uses to set isForContainerImageComponent.
    if (isForContainerImageComponent(request)) {
      return null;
    }
    Owner owner = indexingContext.getOwner(request.getOwnerId());
    // v1 indexes only app/org-scoped requests; repository-family owners are out of scope, mirroring
    // the waiver rule so request-doc scope matches waiver-doc scope exactly.
    if (!isIndexableOwner(owner)) {
      return null;
    }

    String policyName = null;
    Integer threatLevel = null;
    PolicyThreatCategory policyType = null;
    if (request.getPolicyId() != null) {
      Policy policy = policiesById.get(request.getPolicyId());
      if (policy != null) {
        policyName = policy.getName();
        threatLevel = policy.getThreatLevel();
        policyType = threatCategoryOrNull(policy);
      }
    }

    String reasonText = null;
    if (request.getWaiverReasonId() != null) {
      PolicyWaiverReason reason = reasonsById.get(request.getWaiverReasonId());
      if (reason != null) {
        reasonText = reason.getReasonText();
      }
    }

    PolicyWaiverRequestStatus status = request.getStatus();
    DocumentBuilder builder = new DocumentBuilder(ItemType.POLICY_WAIVER_REQUEST)
        .setPolicyWaiverId(request.getId())
        .setPolicyWaiverPolicyName(policyName)
        .setPolicyWaiverPolicyId(request.getPolicyId())
        .setPolicyWaiverReason(reasonText)
        .setPolicyWaiverComment(request.getComment())
        .setPolicyWaiverCreatedAt(toIso8601(request.getRequestTime()))
        .setPolicyWaiverCreatedAtEpochMs(toEpochMs(request.getRequestTime()))
        .setPolicyWaiverExpiresAt(toIso8601(request.getExpiryTime()))
        .setPolicyWaiverExpiresAtEpochMs(toEpochMs(request.getExpiryTime()))
        .setPolicyWaiverScopeOwnerId(owner.getId())
        .setPolicyWaiverScopeOwnerType(owner.getType().name())
        .setPolicyWaiverScope(scopeFor(owner, isComponentTargeted(request)))
        .setPolicyWaiverPolicyType(policyType)
        .setPolicyWaiverRequestStatus(status == null ? null : status.name())
        .setRequesterName(request.getRequesterName())
        .setReviewerName(request.getReviewerName())
        .setReviewTime(toIso8601(request.getReviewTime()))
        .setRejectionReason(request.getRejectionReason())
        .setNoteToReviewer(request.getNoteToReviewer())
        .setOwner(owner)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner));
    if (threatLevel != null) {
      builder.setPolicyWaiverThreatLevel(threatLevel);
    }
    return builder.build();
  }

  // Public: called cross-package by AbstractSearchIndexClient.updateIndexForPolicyWaiver.
  public Document buildDocument(IndexingContext indexingContext, AutoPolicyWaiver waiver) {
    if (waiver == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(waiver.getOwnerId());
    // v1 indexes only app/org-scoped waivers; repository-owner waivers are out of scope.
    if (!isIndexableOwner(owner)) {
      return null;
    }
    // Auto-waivers have no policy name; policyWaiverPolicyName is left null so the synthetic display
    // title is composed on the read side (IndexQueryRowMapper). Keeping it out of the indexed field
    // means the label is not text-searchable or matched by the policy filter, and its wording can
    // change without a reindex.
    DocumentBuilder builder = new DocumentBuilder(ItemType.POLICY_WAIVER)
        .setPolicyWaiverId(waiver.getId())
        .setPolicyWaiverThreatLevel(waiver.getThreatLevel())
        .setPolicyWaiverWaivedBy(waiver.getCreatorName())
        .setPolicyWaiverCreatedAt(toIso8601(waiver.getCreateTime()))
        .setPolicyWaiverCreatedAtEpochMs(toEpochMs(waiver.getCreateTime()))
        .setPolicyWaiverScopeOwnerId(owner.getId())
        .setPolicyWaiverScopeOwnerType(owner.getType().name())
        // Auto-waivers apply to any component in scope, so they are owner-scoped, never component-targeted.
        .setPolicyWaiverScope(scopeFor(owner, false))
        .setPolicyWaiverAuto(true)
        .setPolicyWaiverIsAuto(true)
        .setPolicyWaiverExpiryStatus(PolicyWaiverExpiryStatuses.NEVER)
        .setAllowedContextIds(computeAllowedContextIdsForOwner(indexingContext, owner));
    applyWaiverOwnerHierarchy(builder, indexingContext, owner);
    return builder.build();
  }

  /**
   * Sets the polymorphic owner plus the full ancestor org chain on {@code parentOrganizationName/Id}
   * so WAIVER org filters match apps/violations (immediate org alone is not enough under nested orgs).
   * <p>
   * For application-scoped waivers: {@code setOwner(application)} writes application fields, then
   * organizationId/Name are set explicitly (not via {@code setOwner(Organization)}, which would also
   * write a temporary singleton parent list). The ancestor setters below replace/establish the full
   * parent chain used by the organizations filter.
   */
  private void applyWaiverOwnerHierarchy(
      final DocumentBuilder builder,
      final IndexingContext indexingContext,
      final Owner owner)
  {
    builder.setOwner(owner);
    Organization org = null;
    if (owner instanceof Application application) {
      Owner orgOwner = indexingContext.getOwner(application.getOrganizationId());
      if (orgOwner instanceof Organization o) {
        // Keep application fields from setOwner(app); only fill immediate org identity here.
        builder.setOrganizationId(o.getId());
        builder.setOrganizationName(o.getName());
        org = o;
      }
    }
    else if (owner instanceof Organization o) {
      org = o;
    }
    if (org == null) {
      return;
    }
    List<Organization> ancestors = resolveAncestorOrganizations(indexingContext, org);
    if (!ancestors.isEmpty()) {
      builder.setParentOrganizationNames(ancestors);
      builder.setParentOrganizationIds(ancestors);
    }
  }

  /**
   * Resolves {@code org, parent, ..., root} via the per-run ancestor cache. Falls back to the
   * immediate org when the chain is unavailable (e.g. unit-test mocks that stub only {@code getOwner}).
   */
  private List<Organization> resolveAncestorOrganizations(
      final IndexingContext indexingContext,
      final Organization org)
  {
    List<String> ancestorIds = indexingContext.getAncestorOrgIds(org);
    if (ancestorIds == null || ancestorIds.isEmpty()) {
      return List.of(org);
    }
    List<Organization> ancestors = new ArrayList<>();
    for (String id : ancestorIds) {
      Owner resolved = indexingContext.getOwner(id);
      if (resolved instanceof Organization ancestor) {
        ancestors.add(ancestor);
      }
    }
    return ancestors.isEmpty() ? List.of(org) : ancestors;
  }

  /**
   * Denormalized Active/Expired/Never for the Ana {@code expiryStatus} TERMS filter. A waiver that
   * crosses its expiry instant stays Active in the index until the next reindex / incremental
   * update for that waiver — same trade-off as other denormalized status fields.
   */
  private static String computeExpiryStatus(final Date expiryTime) {
    if (expiryTime == null) {
      return PolicyWaiverExpiryStatuses.NEVER;
    }
    Instant expiry = Instant.ofEpochMilli(expiryTime.getTime());
    return expiry.isBefore(Instant.now())
        ? PolicyWaiverExpiryStatuses.EXPIRED
        : PolicyWaiverExpiryStatuses.ACTIVE;
  }

  // Waiver/request indexing rule: only app/org-owned waivers are indexed (null and repository-family
  // owners are out of scope — repository/container waivers are managed on the Firewall surface). A
  // component-TARGETING waiver is still owned by an app/org, so it is indexed here with the owner's
  // permission closure and reports scope "component" via setPolicyWaiverScope; there is no separate
  // component owner type, so no waiver is ever indexed with an empty/unsafe RBAC closure.
  private static boolean isIndexableOwner(final Owner owner) {
    return owner instanceof Application || owner instanceof Organization;
  }

  // Scope discriminator values (lowercase, matching the FieldMap WAIVER_SCOPES vocabulary).
  private static final String SCOPE_COMPONENT = "component";

  private static final String SCOPE_APPLICATION = "application";

  private static final String SCOPE_ORGANIZATION = "organization";

  /**
   * Facet/filter scope granularity: {@code component} when the waiver/request targets a specific
   * component, otherwise the owner granularity ({@code application}/{@code organization}). The owner
   * is always an app or org here (repository-family owners are filtered out by isIndexableOwner), so
   * the RBAC closure is unchanged — this only classifies the display/facet scope, never the
   * permission owner.
   */
  private static String scopeFor(final Owner owner, final boolean componentTargeted) {
    if (componentTargeted) {
      return SCOPE_COMPONENT;
    }
    return owner instanceof Application ? SCOPE_APPLICATION : SCOPE_ORGANIZATION;
  }

  /**
   * A manual waiver targets a specific component when it carries a component identifier / hash rather
   * than applying to all components in its owner scope. {@code ALL_COMPONENTS} is the owner-wide
   * strategy; any other strategy (or a present component hash / purl) targets a component.
   */
  private static boolean isComponentTargeted(final PolicyWaiver waiver) {
    return waiver.getComponentMatchStrategy() != ComponentMatcherStrategyForWaiver.ALL_COMPONENTS
        || waiver.getHash() != null
        || waiver.getAssociatedPackageUrl() != null;
  }

  /** Batch-load policies by id into an id-keyed map (one IN-clause query; empty in → empty map). */
  private Map<String, Policy> loadPoliciesByIds(final Set<String> policyIds) {
    return policyIds.isEmpty()
        ? Collections.emptyMap()
        : policyDAO.getByIds(policyIds)
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Policy::getId, p -> p, (a, b) -> a));
  }

  /** Batch-load waiver reasons by id into an id-keyed map (one IN-clause query; empty in → empty map). */
  private Map<String, PolicyWaiverReason> loadReasonsByIds(final Set<String> reasonIds) {
    return reasonIds.isEmpty()
        ? Collections.emptyMap()
        : policyWaiverReasonDAO.getAllByIds(new ArrayList<>(reasonIds))
            .stream()
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(PolicyWaiverReason::getId, r -> r, (a, b) -> a));
  }

  /**
   * A request targets a container-image component when its component identifier is docker-format —
   * the same signal {@code ApiPolicyWaiverRequestService} uses to set {@code isForContainerImageComponent}
   * on the approved committed waiver. Kept out of Global Search to match the committed-waiver exclusion.
   */
  private static boolean isForContainerImageComponent(final PolicyWaiverRequest request) {
    final ComponentIdentifier identifier = request.getComponentIdentifier();
    return identifier != null && "docker".equalsIgnoreCase(identifier.getFormat());
  }

  /**
   * As {@link #isComponentTargeted(PolicyWaiver)}, but {@code policy_waiver_request.component_match_strategy}
   * is nullable with no {@code ALL_COMPONENTS} backfill (unlike the {@code policy_waiver} table, backfilled
   * by {@code schema_incremental_0266.sql}). A null strategy with no hash/purl is owner-wide, not
   * component-targeted, so guard the strategy comparison against null.
   */
  private static boolean isComponentTargeted(final PolicyWaiverRequest request) {
    return (request.getComponentMatchStrategy() != null
        && request.getComponentMatchStrategy() != ComponentMatcherStrategyForWaiver.ALL_COMPONENTS)
        || request.getHash() != null
        || request.getAssociatedPackageUrl() != null;
  }

  /**
   * Null-safe {@link Policy#getThreatCategory()}: a policy loaded without its constraints (e.g. a
   * detached/preloaded policy on the single-doc rebuild path) would NPE inside getThreatCategory,
   * which iterates getConstraints(). Return null in that case so the doc simply omits policyType
   * (read back as OTHER) rather than failing the build.
   */
  private static PolicyThreatCategory threatCategoryOrNull(final Policy policy) {
    if (policy == null || policy.getConstraints() == null) {
      return null;
    }
    return policy.getThreatCategory();
  }

  // Fixed-width UTC form (always millis) so lexicographic keyword sort is chronological;
  // Instant.toString() drops fractional seconds on whole seconds, which breaks string ordering.
  private static final DateTimeFormatter ISO8601_MILLIS =
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

  private static String toIso8601(final Date date) {
    return date == null ? null : ISO8601_MILLIS.format(Instant.ofEpochMilli(date.getTime()));
  }

  private static Long toEpochMs(final Date date) {
    return date == null ? null : date.getTime();
  }

  public List<Document> buildSbomDocs(IndexingContext indexingContext) {
    return buildSbomDocs(indexingContext, null);
  }

  /**
   * Variant that consumes a precomputed {@code parentOrgsByOrganization} map (as built by
   * {@code AbstractSearchIndexClient.computeParentsByOrganization}) so per-SBOM indexing does
   * not re-issue {@code OwnerDAO.walkHierarchy} for every document.
   */
  public List<Document> buildSbomDocs(
      IndexingContext indexingContext,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    return thirdPartySbomMetadataDAO.getAll()
        .stream()
        .map(sbomMetadata -> buildDocument(indexingContext, sbomMetadata, parentOrgsByOrganization))
        .toList();
  }

  public Document buildDocument(IndexingContext indexingContext, ThirdPartySbomMetadata sbomMetadata) {
    return buildDocument(indexingContext, sbomMetadata, null);
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      ThirdPartySbomMetadata sbomMetadata,
      Map<Organization, Collection<Organization>> parentOrgsByOrganization)
  {
    if (sbomMetadata == null) {
      return null;
    }
    Owner owner = indexingContext.getOwner(sbomMetadata.getApplicationId());
    if (owner == null) {
      return null;
    }
    if (!(owner instanceof Application application)) {
      log.warn("ThirdPartySbomMetadata {} has owner that is not of type Application: {}", sbomMetadata.getId(), owner);
      return null;
    }
    Owner orgOwner = indexingContext.getOwner(application.getOrganizationId());
    if (!(orgOwner instanceof Organization org)) {
      return null;
    }
    List<String> allowedContextIds =
        resolveClosure(indexingContext, parentOrgsByOrganization, org, application.getId());
    return new DocumentBuilder(ItemType.SBOM_METADATA)
        .setOwner(application)
        .setOwner(org)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .setAllowedContextIds(allowedContextIds)
        .build();
  }

  public List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {
    if (parentOrgsMap == null || organization == null || application == null) {
      return Collections.emptyList();
    }
    return StageTypes.getAll()
        .stream()
        .map(stageType -> CompletableFuture.supplyAsync(
            () -> buildApplicationStageSVDocs(
                indexingContext,
                organization,
                application,
                stageType,
                parentOrgsMap.get(organization)),
            getEvalExecutor()))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }));
  }

  public List<Document> buildApplicationStageSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      Collection<Organization> parentOrganizations)
  {
    if (parentOrganizations == null || organization == null || application == null) {
      return Collections.emptyList();
    }
    PolicyEvaluation latestPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageType.getId());
    if (latestPolicyEvaluation == null) {
      return Collections.emptyList();
    }
    String scanId = latestPolicyEvaluation.getScanId();
    LifecycleReport applicationReport = null;
    try {
      applicationReport = reportService.getReport(application.getId(), scanId);
      if (!applicationReport.exists()) {
        return Collections.emptyList();
      }
      ReportEntry licenseReportEntry = applicationReport.getEntry(LICENSES_JSON.getName());
      ReportEntry securityReportEntry = applicationReport.getEntry(SECURITY_JSON.getName());
      ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
      ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null ||
          dependenciesReportEntry == null)
      {
        return Collections.emptyList();
      }

      List<Component> components = componentLoaderFactory.createComponentLoader(application)
          .getAll(
              licenseReportEntry.buf,
              securityReportEntry.buf,
              bomReportEntry.buf,
              dependenciesReportEntry.buf);

      // Load the (app, stage) unfixed policy violations once, up front: they drive both the
      // POLICY_VIOLATION docs below AND the per-component violation rollup denormalized onto each
      // NON_VULNERABLE_COMPONENT doc (Components leg policyTypes/violationStates/policyThreatLevel
      // filters + sort). No extra query — the same list is reused.
      List<PolicyViolation> violations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(
          application.getId(), stageType.getId());
      Map<String, ComponentViolationRollup> violationRollupByHash = componentViolationRollupByHash(violations);
      // Load the violations' constraint facts once (single batch getByIds): they feed BOTH the
      // first-seen refId join below AND the POLICY_VIOLATION constraint-name lookup further down.
      List<PolicyViolationConstraintFacts> constraintFacts = loadConstraintFacts(violations);
      // First-seen (open_time) per vuln refId, joined from the same violations via their constraint
      // facts' SECURITY_VULNERABILITY_REFID triggers; earliest open time wins across stages/policies.
      Map<String, Long> firstSeenByVulnRefId = firstSeenEpochMsByVulnRefId(violations, constraintFacts);

      // Build security vulnerability documents (existing behavior)
      List<Document> securityVulnerabilityDocs = components.stream()
          .map(component -> CompletableFuture.supplyAsync(
              () -> buildApplicationComponentVulnerabilityDocuments(
                  indexingContext,
                  organization,
                  parentOrganizations,
                  application,
                  stageType,
                  scanId,
                  component,
                  violationRollupByHash,
                  firstSeenByVulnRefId),
              getComponentExecutor()))
          .collect(Collectors.collectingAndThen(
              Collectors.toList(),
              futures -> {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
                return futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();
              }));

      // Build policy violation documents
      Map<String, String> constraintNameByFactsId = loadConstraintNames(constraintFacts);
      List<Document> policyViolationDocs = buildPolicyViolationDocuments(
          indexingContext, organization, parentOrganizations, application, stageType, scanId, violations,
          constraintNameByFactsId);

      // Batch-load license threat groups for all components
      Set<String> allLicenseIds = components.stream()
          .map(Component::getLicenseIds)
          .filter(ids -> ids != null)
          .flatMap(Set::stream)
          .collect(Collectors.toSet());
      Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId;
      if (!allLicenseIds.isEmpty()) {
        List<String> ownerIds = parentOrganizations.stream()
            .map(Organization::getId)
            .collect(Collectors.toList());
        try (TransactionContext tx = licenseThreatGroupDAO.createTransactionContext()) {
          threatGroupsByLicenseId = licenseThreatGroupDAO.getLicenseIdThreatGroupsByOwnerIdsAndLicenseIds(
              tx, ownerIds, allLicenseIds);
        }
      }
      else {
        threatGroupsByLicenseId = Collections.emptyMap();
      }

      // Build legal violation documents
      List<Document> legalViolationDocs = new ArrayList<>();
      for (Component component : components) {
        legalViolationDocs.addAll(buildLegalViolationDocuments(
            indexingContext, organization, parentOrganizations, application, stageType, scanId, component,
            threatGroupsByLicenseId));
      }

      // Combine all documents
      List<Document> allDocs = new ArrayList<>();
      allDocs.addAll(securityVulnerabilityDocs);
      allDocs.addAll(policyViolationDocs);
      allDocs.addAll(legalViolationDocs);
      return allDocs;
    }
    catch (UncheckedIOException e) {
      log.error("Error parsing report files at {}",
          applicationReport == null ? "Unknown" : applicationReport.getLocation(), e);
    }
    catch (IOException | NotFoundException | InvalidComponentIdentifierException e) {
      log.error(e.getMessage(), e);
    }
    catch (Exception e) {
      Throwable rootCause = ExceptionUtils.getRootCause(e);
      if (rootCause instanceof UncheckedIOException || rootCause instanceof IOException ||
          rootCause instanceof NotFoundException || rootCause instanceof InvalidComponentIdentifierException)
      {
        log.error(e.getMessage(), e);
      }
      else {
        throw e;
      }
    }
    return Collections.emptyList();
  }

  public List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    return buildApplicationComponentVulnerabilityDocuments(indexingContext, organization, parentOrganizations,
        application, stageType, reportId, component, Collections.emptyMap(), Collections.emptyMap());
  }

  public List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      Map<String, ComponentViolationRollup> violationRollupByHash,
      Map<String, Long> firstSeenByVulnRefId)
  {
    if (parentOrganizations == null || organization == null || application == null || component == null) {
      return Collections.emptyList();
    }
    if (CollectionUtils.isNotEmpty(component.getSecurityVulnerabilities())) {
      return component.getSecurityVulnerabilities()
          .stream()
          .map(
              vulnerability -> buildDocument(indexingContext, organization, application, stageType, reportId, component,
                  vulnerability, parentOrganizations, firstSeenByVulnRefId))
          .toList();
    }
    else if (component.getComponentIdentifier() != null) {
      // The violation rollup rides only the NON_VULNERABLE_COMPONENT doc: a component with any
      // security vulnerability is emitted as SECURITY_VULNERABILITY docs above and never carries the
      // componentViolation* fields, and the Components leg queries NON_VULNERABLE_COMPONENT only. So a
      // vulnerable-and-violated component is a Vulnerabilities-tab row, not a Components-tab row, and
      // the Components policyTypes/violationStates/policyThreatLevel filters intentionally do not
      // reach it. This is the pre-existing tab partition (vulnerable components live on the
      // Vulnerabilities tab), which the new filters simply inherit.
      return Collections.singletonList(
          buildDocument(organization, parentOrganizations, application, stageType, reportId, component,
              violationRollupByHash.get(component.getHash())));
    }
    else {
      return Collections.emptyList();
    }
  }

  public Document buildDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    return buildDocument(organization, parentOrganizations, application, stageType, reportId, component, null);
  }

  public Document buildDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      ComponentViolationRollup violationRollup)
  {
    if (parentOrganizations == null || organization == null || application == null || component == null) {
      return null;
    }
    DocumentBuilder builder = new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT)
        .setOwner(application)
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType)
        .setReportId(reportId)
        .setComponentHash(component.getHash())
        .setComponentFormat(component.getComponentIdentifier().getFormat())
        .setComponentCoordinates(component)
        .setComponentName(component.getDisplayNameFromIdentifier())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()));
    if (violationRollup != null && !violationRollup.isEmpty()) {
      builder
          .setComponentViolationPolicyTypes(violationRollup.policyTypes())
          .setComponentViolationStates(violationRollup.states())
          .setComponentMaxPolicyThreatLevel(violationRollup.maxThreatLevel());
    }
    return builder.build();
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      SecurityVulnerability vulnerability,
      Collection<Organization> parentOrganizations)
  {
    return buildDocument(indexingContext, organization, application, stageType, reportId, component, vulnerability,
        parentOrganizations, Collections.emptyMap());
  }

  public Document buildDocument(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      SecurityVulnerability vulnerability,
      Collection<Organization> parentOrganizations,
      Map<String, Long> firstSeenByVulnRefId)
  {
    if (parentOrganizations == null || organization == null || application == null || component == null ||
        vulnerability == null)
    {
      return null;
    }
    DocumentBuilder builder = new DocumentBuilder(ItemType.SECURITY_VULNERABILITY)
        .setOwner(application)
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType)
        .setReportId(reportId)
        .setComponentHash(component.getHash())
        .setComponentFormat(component.getComponentIdentifier().getFormat())
        .setComponentCoordinates(component)
        .setComponentName(component.getDisplayNameFromIdentifier())
        .setVulnerabilityId(vulnerability.getRefId())
        .setVulnerabilitySeverity(vulnerability.getSeverity())
        .setVulnerabilityStatus(vulnerability.getStatus().getName())
        .setVulnerabilityDescription(getDescription(indexingContext.getVulnDescByVulnId(), vulnerability))
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()));
    // First-seen is set only when a policy violation triggered by this vuln refId supplies an
    // open time; a non-triggering (informational) vuln has no entry, so the field stays unset.
    Long firstSeenEpochMs = firstSeenByVulnRefId == null ? null : firstSeenByVulnRefId.get(vulnerability.getRefId());
    if (firstSeenEpochMs != null) {
      builder.setVulnerabilityFirstSeenEpochMs(firstSeenEpochMs);
    }
    return builder.build();
  }

  private String getDescription(
      final Map<String, String> vulnDescByVulnId,
      final SecurityVulnerability vulnerability)
  {
    if (IdentificationSource.SONATYPE_IAC.getId().equals(vulnerability.getSource())
        || "Sonatype-C".equals(vulnerability.getSource()))
    {
      return getThirdPartyVulnerabilityDescription(vulnDescByVulnId, vulnerability);
    }
    try {
      return vulnDescByVulnId.computeIfAbsent(vulnerability.getRefId(),
          vulnerabilityDescriptionFetcher::getVulnerabilityDescription);
    }
    catch (NotFoundException notFoundException) {
      log.warn(notFoundException.getMessage());
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return "";
  }

  private String getThirdPartyVulnerabilityDescription(
      final Map<String, String> vulnDescByVulnId,
      final SecurityVulnerability vulnerability)
  {
    if (vulnDescByVulnId.get(vulnerability.getRefId()) != null) {
      return vulnDescByVulnId.get(vulnerability.getRefId());
    }

    ThirdPartyVulnerability thirdPartyVulnerability = thirdPartyVulnerabilityDAO.getByRefId(vulnerability.getRefId());
    if (thirdPartyVulnerability == null || thirdPartyVulnerability.getDescription() == null) {
      log.warn("Description not found for vulnerability with refid: {}", vulnerability.getRefId());
      return "";
    }
    else {
      String description = thirdPartyVulnerability.getDescription();
      vulnDescByVulnId.put(vulnerability.getRefId(), description);
      return description;
    }
  }

  /**
   * Derives the single indexed waiver status for a policy violation. The field is single-valued, so a
   * violation that is both waived and legacy can carry only one status; waiver wins (precedence
   * AutoWaived &gt; Waived &gt; Legacy &gt; Active). A waived+legacy violation therefore indexes as Waived
   * and appears under WAIVED (not LEGACY). This is a deliberate divergence from the SQL read path, where
   * such a violation is a member of both states; the WAIVED facet is the primary triage signal, so a
   * waived violation staying under WAIVED is the safer V1 behavior. Pure-legacy (non-waived) violations
   * index as Legacy, matching SQL's OPEN-excludes-legacy semantics for the common case.
   */
  static String deriveWaiverStatus(PolicyViolation violation) {
    if (violation.getAutoPolicyWaiverId() != null) {
      return POLICY_VIOLATION_WAIVER_STATUS_AUTO_WAIVED;
    }
    if (violation.getWaiveTime() != null) {
      return POLICY_VIOLATION_WAIVER_STATUS_WAIVED;
    }
    if (violation.isLegacyViolation()) {
      return POLICY_VIOLATION_WAIVER_STATUS_LEGACY;
    }
    return POLICY_VIOLATION_WAIVER_STATUS_ACTIVE;
  }

  /**
   * Per-component denormalization of the (app, stage) unfixed policy violations, keyed by component
   * hash. Powers the Components leg policyTypes / violationStates / policyThreatLevel filters and the
   * policyThreatLevel sort without a per-row violation query at read time. Empty when a component has
   * no policy violation. The values are idempotent under the per-(app, stage) doc multiplicity: the
   * policy-type/state sets are unions and the threat level is a max, so a component's doc carries the
   * same rollup regardless of how many violation rows produced it.
   *
   * @param policyTypes distinct lower-cased threat categories (security/license/quality/other).
   * @param states distinct lower-cased API violation states ({@code open}/{@code waived}/{@code legacy}).
   * @param maxThreatLevel maximum threat level (0&ndash;10) across the component's violations, or
   *          {@code null} when none carry a threat level.
   */
  public record ComponentViolationRollup(Set<String> policyTypes, Set<String> states, Integer maxThreatLevel)
  {
    public ComponentViolationRollup {
      policyTypes = policyTypes == null
          ? Collections.emptySet()
          : Collections.unmodifiableSet(new LinkedHashSet<>(policyTypes));
      states = states == null
          ? Collections.emptySet()
          : Collections.unmodifiableSet(new LinkedHashSet<>(states));
    }

    boolean isEmpty() {
      return policyTypes.isEmpty() && states.isEmpty() && maxThreatLevel == null;
    }
  }

  /**
   * Rolls the (app, stage) unfixed policy violations up per component hash. One pass over the list —
   * no per-component query. A violation with a null hash is skipped (cannot be attributed to a
   * component row).
   */
  static Map<String, ComponentViolationRollup> componentViolationRollupByHash(final List<PolicyViolation> violations) {
    if (CollectionUtils.isEmpty(violations)) {
      return Collections.emptyMap();
    }
    Map<String, Set<String>> typesByHash = new HashMap<>();
    Map<String, Set<String>> statesByHash = new HashMap<>();
    Map<String, Integer> maxThreatByHash = new HashMap<>();
    for (PolicyViolation violation : violations) {
      String hash = violation.getHash();
      if (hash == null) {
        continue;
      }
      // A null threat category contributes no policyType but still contributes a state and a threat
      // level below. So a component whose violations all have null categories gets an indexed
      // componentMaxPolicyThreatLevel (and componentViolationState) but no componentViolationPolicyType:
      // a policyThreatLevel range or violationStates filter can match it, a policyTypes filter cannot.
      if (violation.getThreatCategory() != null) {
        typesByHash.computeIfAbsent(hash, h -> new LinkedHashSet<>())
            .add(violation.getThreatCategory().getName().toLowerCase(Locale.ROOT));
      }
      statesByHash.computeIfAbsent(hash, h -> new LinkedHashSet<>())
          .add(componentViolationState(deriveWaiverStatus(violation)));
      maxThreatByHash.merge(hash, violation.getThreatLevel(), Math::max);
    }
    Map<String, ComponentViolationRollup> out = new HashMap<>();
    Set<String> hashes = new LinkedHashSet<>();
    hashes.addAll(typesByHash.keySet());
    hashes.addAll(statesByHash.keySet());
    hashes.addAll(maxThreatByHash.keySet());
    for (String hash : hashes) {
      out.put(hash, new ComponentViolationRollup(
          typesByHash.getOrDefault(hash, Collections.emptySet()),
          statesByHash.getOrDefault(hash, Collections.emptySet()),
          maxThreatByHash.get(hash)));
    }
    return out;
  }

  /**
   * Maps an indexed waiver status to the lower-cased component violation state
   * (open/waived/legacy). Active&nbsp;&rarr;&nbsp;open, pure-legacy Legacy&nbsp;&rarr;&nbsp;legacy,
   * everything else (Waived/AutoWaived)&nbsp;&rarr;&nbsp;waived. Legacy is a distinct grandfathered-in
   * state, neither open nor waived. A waived+legacy violation never reaches the legacy branch here:
   * {@link #deriveWaiverStatus} resolves it to Waived (waiver precedence), so it classifies as waived.
   */
  private static String componentViolationState(final String waiverStatus) {
    if (POLICY_VIOLATION_WAIVER_STATUS_ACTIVE.equals(waiverStatus)) {
      return COMPONENT_VIOLATION_STATE_OPEN;
    }
    if (POLICY_VIOLATION_WAIVER_STATUS_LEGACY.equals(waiverStatus)) {
      return COMPONENT_VIOLATION_STATE_LEGACY;
    }
    return COMPONENT_VIOLATION_STATE_WAIVED;
  }

  /**
   * Loads the constraint-facts rows for a violation batch in a single batch fetch. The result feeds
   * both {@link #loadConstraintNames} and {@link #firstSeenEpochMsByVulnRefId}, so it is loaded once
   * per (app, stage) reindex cycle and shared rather than fetched twice.
   */
  private List<PolicyViolationConstraintFacts> loadConstraintFacts(List<PolicyViolation> violations) {
    if (CollectionUtils.isEmpty(violations)) {
      return Collections.emptyList();
    }
    Set<String> constraintFactsIds = new HashSet<>();
    for (PolicyViolation violation : violations) {
      if (violation.getConstraintFactsId() != null) {
        constraintFactsIds.add(violation.getConstraintFactsId());
      }
    }
    if (constraintFactsIds.isEmpty()) {
      return Collections.emptyList();
    }
    return policyViolationConstraintFactsDAO.getByIds(constraintFactsIds);
  }

  /**
   * Loads constraint names by constraint facts ID from a pre-loaded constraint-facts batch.
   */
  private Map<String, String> loadConstraintNames(List<PolicyViolationConstraintFacts> factsList) {
    if (CollectionUtils.isEmpty(factsList)) {
      return Collections.emptyMap();
    }
    Map<String, String> constraintNameByFactsId = new HashMap<>();
    for (PolicyViolationConstraintFacts facts : factsList) {
      String constraintName = extractFirstConstraintName(facts.getConstraintFactsJson());
      if (constraintName != null) {
        constraintNameByFactsId.put(facts.getId(), constraintName);
      }
    }
    return constraintNameByFactsId;
  }

  /**
   * Extracts the first constraint name from constraint facts JSON.
   */
  private String extractFirstConstraintName(String constraintFactsJson) {
    if (constraintFactsJson == null || constraintFactsJson.isEmpty()) {
      return null;
    }
    try {
      ConstraintFact[] facts =
          com.sonatype.insight.json.store.JsonUtils.parse(constraintFactsJson, ConstraintFact[].class);
      if (facts != null && facts.length > 0 && facts[0].getConstraintName() != null) {
        return facts[0].getConstraintName();
      }
    }
    catch (Exception e) {
      log.warn("Failed to parse constraint facts JSON: {}", e.getMessage());
    }
    return null;
  }

  /**
   * Resolves the first-seen epoch-millis per vulnerability refId from the (app, stage) unfixed policy
   * violations: a vuln-triggered violation references the vuln via a
   * {@link com.sonatype.clm.dto.model.policy.TriggerReference.Type#SECURITY_VULNERABILITY_REFID}
   * carried in its constraint facts, and the violation's {@code openTime} is when this IQ first
   * detected it. A vuln can trigger violations on multiple stages/policies, so the earliest open time
   * wins (true first-seen). Constraint facts are stored in a separate dedup table and are
   * {@code @Transient} on the violation, so they are not on the violations passed in. A vuln with no
   * triggering violation gets no entry (row shows a blank first-seen; never fabricated). The
   * constraint-facts rows are pre-loaded once by {@link #loadConstraintFacts} and shared with
   * {@link #loadConstraintNames}.
   */
  private Map<String, Long> firstSeenEpochMsByVulnRefId(
      List<PolicyViolation> violations,
      List<PolicyViolationConstraintFacts> factsList)
  {
    if (CollectionUtils.isEmpty(violations) || CollectionUtils.isEmpty(factsList)) {
      return Collections.emptyMap();
    }
    Map<String, Set<String>> vulnRefIdsByFactsId = new HashMap<>();
    for (PolicyViolationConstraintFacts facts : factsList) {
      Set<String> refIds = extractVulnerabilityRefIds(facts.getConstraintFactsJson());
      if (!refIds.isEmpty()) {
        vulnRefIdsByFactsId.put(facts.getId(), refIds);
      }
    }
    if (vulnRefIdsByFactsId.isEmpty()) {
      return Collections.emptyMap();
    }
    Map<String, Long> firstSeenByRefId = new HashMap<>();
    for (PolicyViolation violation : violations) {
      Date openTime = violation.getOpenTime();
      String factsId = violation.getConstraintFactsId();
      if (openTime == null || factsId == null) {
        continue;
      }
      Set<String> refIds = vulnRefIdsByFactsId.get(factsId);
      if (refIds == null) {
        continue;
      }
      long epochMs = openTime.getTime();
      for (String refId : refIds) {
        firstSeenByRefId.merge(refId, epochMs, Math::min);
      }
    }
    return firstSeenByRefId;
  }

  /**
   * Extracts the distinct {@code SECURITY_VULNERABILITY_REFID} trigger values from a constraint facts
   * JSON blob. A violation's constraint facts may carry condition facts for several conditions; only
   * the ones whose {@link com.sonatype.clm.dto.model.policy.TriggerReference} is of type
   * {@code SECURITY_VULNERABILITY_REFID} identify the triggering vuln. Returns an empty set (never
   * null) on missing/malformed JSON so a non-vuln violation contributes no first-seen.
   */
  private Set<String> extractVulnerabilityRefIds(String constraintFactsJson) {
    if (constraintFactsJson == null || constraintFactsJson.isEmpty()) {
      return Collections.emptySet();
    }
    Set<String> refIds = new HashSet<>();
    try {
      ConstraintFact[] facts =
          com.sonatype.insight.json.store.JsonUtils.parse(constraintFactsJson, ConstraintFact[].class);
      if (facts == null) {
        return Collections.emptySet();
      }
      for (ConstraintFact fact : facts) {
        if (fact == null || fact.getConditionFacts() == null) {
          continue;
        }
        for (ConditionFact conditionFact : fact.getConditionFacts()) {
          if (conditionFact == null) {
            continue;
          }
          TriggerReference reference = conditionFact.getReference();
          if (reference != null
              && reference.getType() == TriggerReference.Type.SECURITY_VULNERABILITY_REFID
              && reference.getValue() != null)
          {
            refIds.add(reference.getValue());
          }
        }
      }
    }
    catch (Exception e) {
      log.warn("Failed to parse constraint facts JSON for vulnerability first-seen: {}", e.getMessage());
    }
    return refIds;
  }

  /**
   * Builds POLICY_VIOLATION documents for each policy violation.
   */
  private List<Document> buildPolicyViolationDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      List<PolicyViolation> violations,
      Map<String, String> constraintNameByFactsId)
  {
    if (CollectionUtils.isEmpty(violations)) {
      return Collections.emptyList();
    }

    List<String> categoryNames = applicationCategoryNames(indexingContext, application.getId());
    List<Document> documents = new ArrayList<>();
    for (PolicyViolation violation : violations) {
      Document doc = buildPolicyViolationDocument(
          organization, parentOrganizations, application, stageType, reportId, violation, constraintNameByFactsId,
          categoryNames);
      if (doc != null) {
        documents.add(doc);
      }
    }
    return documents;
  }

  /**
   * Builds a single POLICY_VIOLATION document.
   */
  private Document buildPolicyViolationDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      PolicyViolation violation,
      Map<String, String> constraintNameByFactsId,
      List<String> categoryNames)
  {
    if (violation == null) {
      return null;
    }

    String constraintName = null;
    if (violation.getConstraintFactsId() != null) {
      constraintName = constraintNameByFactsId.get(violation.getConstraintFactsId());
    }

    DocumentBuilder builder = new DocumentBuilder(ItemType.POLICY_VIOLATION)
        .setOwner(application)
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType)
        .setReportId(reportId)
        .setPolicyViolationId(violation.getId())
        .setPolicyViolationThreatLevel(violation.getThreatLevel())
        .setPolicyViolationWaiverStatus(deriveWaiverStatus(violation))
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()));

    if (categoryNames != null && !categoryNames.isEmpty()) {
      builder.setApplicationCategoryNames(categoryNames);
    }

    if (violation.getPolicyId() != null) {
      builder.setPolicyViolationPolicyId(violation.getPolicyId());
    }
    if (violation.getPolicyName() != null) {
      builder.setPolicyViolationPolicyName(violation.getPolicyName());
    }

    if (violation.getHash() != null) {
      builder.setComponentHash(violation.getHash());
    }
    if (violation.getComponentIdentifier() != null) {
      builder.setComponentFormat(violation.getComponentIdentifier().getFormat())
          .setComponentCoordinates(violation.getComponentIdentifier())
          .setComponentName(
              ComponentDisplayNameUtil.fromIdentifier(violation.getComponentIdentifier()).toString());
    }
    if (violation.getThreatCategory() != null) {
      builder.setPolicyViolationThreatCategory(violation.getThreatCategory());
    }
    if (constraintName != null) {
      builder.setPolicyViolationConstraintName(constraintName);
    }

    return builder.build();
  }

  /**
   * Builds LEGAL_VIOLATION documents for each license associated with a component.
   */
  private List<Document> buildLegalViolationDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      Map<String, List<LicenseThreatGroup>> threatGroupsByLicenseId)
  {
    if (component == null) {
      return Collections.emptyList();
    }

    Set<String> licenseIds = component.getLicenseIds();
    if (CollectionUtils.isEmpty(licenseIds)) {
      return Collections.emptyList();
    }

    List<Document> documents = new ArrayList<>();
    Map<String, String> licenseNameCache = indexingContext.getLicenseNameById();
    List<String> categoryNames = applicationCategoryNames(indexingContext, application.getId());

    for (String licenseId : licenseIds) {
      String cachedName = licenseNameCache.computeIfAbsent(licenseId, id -> {
        MultiLicense license = multiLicenseDAO.getById(id);
        return license != null && license.getShortDisplayName() != null ? license.getShortDisplayName() : "";
      });
      String licenseName = cachedName.isEmpty() ? null : cachedName;

      List<LicenseThreatGroup> threatGroups = threatGroupsByLicenseId.get(licenseId);

      if (!CollectionUtils.isEmpty(threatGroups)) {
        for (LicenseThreatGroup threatGroup : threatGroups) {
          Document doc = buildLegalViolationDocument(
              organization, parentOrganizations, application, stageType, reportId, component,
              licenseId, licenseName, threatGroup, categoryNames);
          if (doc != null) {
            documents.add(doc);
          }
        }
      }
      else {
        Document doc = buildLegalViolationDocument(
            organization, parentOrganizations, application, stageType, reportId, component,
            licenseId, licenseName, null, categoryNames);
        if (doc != null) {
          documents.add(doc);
        }
      }
    }
    return documents;
  }

  /**
   * Builds a single LEGAL_VIOLATION document.
   */
  private Document buildLegalViolationDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      String licenseId,
      String licenseName,
      LicenseThreatGroup threatGroup,
      List<String> categoryNames)
  {
    if (component == null || licenseId == null) {
      return null;
    }

    DocumentBuilder builder = new DocumentBuilder(ItemType.LEGAL_VIOLATION)
        .setOwner(application)
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType)
        .setReportId(reportId)
        .setComponentEffectiveLicenseId(licenseId)
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()));

    if (categoryNames != null && !categoryNames.isEmpty()) {
      builder.setApplicationCategoryNames(categoryNames);
    }

    if (component.getHash() != null) {
      builder.setComponentHash(component.getHash());
    }
    if (component.getComponentIdentifier() != null) {
      builder.setComponentFormat(component.getComponentIdentifier().getFormat())
          .setComponentCoordinates(component)
          .setComponentName(component.getDisplayNameFromIdentifier());
    }

    if (licenseName != null) {
      builder.setComponentEffectiveLicenseName(licenseName);
    }

    if (threatGroup != null) {
      builder.setComponentLicenseThreatGroupName(threatGroup.getName())
          .setComponentLicenseThreatLevel(threatGroup.getThreatLevel());
    }

    return builder.build();
  }

  public List<Document> buildSbomSVDocs(
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {
    if (parentOrgsMap == null || organization == null || application == null) {
      return Collections.emptyList();
    }
    return thirdPartySbomMetadataDAO.getByApplicationId(application.getId())
        .stream()
        .map(sbomMetadata -> CompletableFuture.supplyAsync(
            () -> buildSbomVersionSVDocs(
                organization,
                application,
                sbomMetadata,
                parentOrgsMap.get(organization)),
            getEvalExecutor()))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }));
  }

  public List<Document> buildSbomVersionSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations)
  {
    if (parentOrganizations == null || organization == null || application == null || sbomMetadata == null) {
      return Collections.emptyList();
    }
    return thirdPartyFileCoordinateDAO.getBySbomMetadataId(sbomMetadata.getId())
        .stream()
        .map(fileCoord -> CompletableFuture.supplyAsync(
            () -> buildSbomFileCoordinateSVDocs(
                organization,
                application,
                sbomMetadata,
                parentOrganizations,
                fileCoord),
            getComponentExecutor()))
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            futures -> {
              CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
              return futures.stream()
                  .map(CompletableFuture::join)
                  .flatMap(List::stream)
                  .toList();
            }));
  }

  public List<Document> buildSbomFileCoordinateSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations,
      ThirdPartyFileCoordinate thirdPartyFileCoord)
  {
    if (parentOrganizations == null || organization == null || application == null || sbomMetadata == null ||
        thirdPartyFileCoord == null)
    {
      return Collections.emptyList();
    }
    List<ThirdPartyCoordinateSecurity> vulns = thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(
        Collections.singletonList(thirdPartyFileCoord.getId()));

    if (CollectionUtils.isNotEmpty(vulns)) {
      return vulns.stream()
          .map(vuln -> buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, vuln,
              parentOrganizations))
          .toList();
    }
    else if (thirdPartyFileCoord.getPackageUrl() != null) {
      return Collections.singletonList(
          buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, parentOrganizations));
    }
    else {
      return Collections.emptyList();
    }
  }

  public Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      Collection<Organization> parentOrganizations)
  {
    if (parentOrganizations == null || organization == null || application == null || sbomMetadata == null ||
        thirdPartyFileCoord == null)
    {
      return null;
    }
    DocumentBuilder documentBuilder = new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT);
    ComponentIdentifier componentIdentifier = tryConvert(thirdPartyFileCoord);
    if (componentIdentifier != null) {
      documentBuilder
          .setComponentFormat(componentIdentifier.getFormat())
          .setComponentCoordinates(componentIdentifier)
          .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    }
    return documentBuilder
        .setOwner(application)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setComponentHash(thirdPartyFileCoord.getHash())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()))
        .build();
  }

  public Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      Collection<Organization> parentOrganizations)
  {
    if (parentOrganizations == null || organization == null || application == null || sbomMetadata == null ||
        thirdPartyFileCoord == null || thirdPartyCoordinateSecurity == null)
    {
      return null;
    }
    DocumentBuilder documentBuilder = new DocumentBuilder(ItemType.SECURITY_VULNERABILITY);
    ComponentIdentifier componentIdentifier = tryConvert(thirdPartyFileCoord);
    if (componentIdentifier != null) {
      documentBuilder
          .setComponentFormat(componentIdentifier.getFormat())
          .setComponentCoordinates(componentIdentifier)
          .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString());
    }
    return documentBuilder
        .setOwner(application)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setComponentHash(thirdPartyFileCoord.getHash())
        .setVulnerabilityId(thirdPartyCoordinateSecurity.getRefId())
        .setVulnerabilitySeverity(sbomCvssSeverity(thirdPartyCoordinateSecurity))
        .setVulnerabilityDescription(thirdPartyCoordinateSecurity.getDescription())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()))
        .build();
  }

  /**
   * Resolves the CVSS severity to index for an SBOM vulnerability, or {@code null} when the vulnerability
   * carries no CVSS score. {@code ThirdPartyCoordinateSecurity.severity} is a primitive {@code double}
   * defaulting to {@code 0.0}; an EPSS-only (or otherwise unscored) SBOM vulnerability is still recorded
   * but leaves that default in place and sets no {@code ratingMethod}. Writing {@code 0.0} would put such
   * vulns in the {@code none} CVSS band, conflating "no score" with a real score of {@code 0.0}; returning
   * {@code null} instead omits the {@code vulnerabilitySeverity} field so they sit in no band. A genuine
   * CVSS {@code 0.0} always comes with a {@code ratingMethod}, so it is preserved.
   */
  private static Float sbomCvssSeverity(final ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity) {
    if (thirdPartyCoordinateSecurity.getSeverity() == 0.0d
        && thirdPartyCoordinateSecurity.getRatingMethod() == null)
    {
      return null;
    }
    return BigDecimal.valueOf(thirdPartyCoordinateSecurity.getSeverity())
        .setScale(2, RoundingMode.HALF_EVEN)
        .floatValue();
  }

  private ComponentIdentifier tryConvert(ThirdPartyFileCoordinate thirdPartyFileCoordinate) {
    PackageUrlIdentifier packageUrlIdentifier = null;

    // First try the pURL, if it exists
    if (thirdPartyFileCoordinate.getPackageUrl() != null) {
      try {
        packageUrlIdentifier = new PackageUrlIdentifier(thirdPartyFileCoordinate.getPackageUrl());
      }
      catch (Exception e) {
        log.error("Unable to create PackageUrlIdentifier from ThirdPartyFileCoordinate with id: '{}', and pURL: '{}'.",
            thirdPartyFileCoordinate.getId(), thirdPartyFileCoordinate.getPackageUrl());
      }
    }

    // Second, try the ThirdPartyFileCoordinate format, name, and version (which should always exist)
    if (packageUrlIdentifier == null) {
      try {
        packageUrlIdentifier = new PackageUrlIdentifier(PackageURLBuilder.aPackageURL()
            .withType(thirdPartyFileCoordinate.getFormat())
            .withName(thirdPartyFileCoordinate.getName())
            .withVersion(thirdPartyFileCoordinate.getVersion())
            .build()
            .canonicalize());
      }
      catch (Exception e) {
        log.warn("Unable to create PackageUrlIdentifier from ThirdPartyFileCoordinate with " +
            "id: '{}', format: '{}', name: '{}', and version: '{}'.", thirdPartyFileCoordinate.getId(),
            thirdPartyFileCoordinate.getFormat(), thirdPartyFileCoordinate.getName(),
            thirdPartyFileCoordinate.getVersion());
      }
    }

    // If one of the above worked, try to convert it to a component identifier and return it
    if (packageUrlIdentifier != null) {
      try {
        return packageUrlIdentifier.toComponentIdentifier();
      }
      catch (Exception e) {
        log.error("Unable to convert PackageUrlIdentifier from ThirdPartyFileCoordinate with id: " +
            "'{}', and pURL: '{}' to ComponentIdentifier.", thirdPartyFileCoordinate.getId(),
            packageUrlIdentifier.getPackageUrl());
      }
    }

    // If none of the above worked, log and return null
    log.error("Unable to create ComponentIdentifier from ThirdPartyFileCoordinate with id: " +
        "'{}', pURL: '{}', format: '{}', name: '{}', version: '{}'.",
        thirdPartyFileCoordinate.getId(), thirdPartyFileCoordinate.getPackageUrl(),
        thirdPartyFileCoordinate.getFormat(), thirdPartyFileCoordinate.getName(),
        thirdPartyFileCoordinate.getVersion());

    return null;
  }
}
