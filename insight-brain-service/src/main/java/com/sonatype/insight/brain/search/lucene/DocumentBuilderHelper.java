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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Map;
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
import com.sonatype.clm.dto.model.policy.ConstraintFact;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
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
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.PolicyViolation;
import com.sonatype.insight.brain.model.policy.PolicyViolationConstraintFacts;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.index.IndexingContext;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.tenancy.TenantReference;
import com.sonatype.insight.brain.tenancy.TenantThreadPoolExecutor;
import com.sonatype.insight.brain.utils.DefaultExecutorThreadPools;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import com.github.packageurl.PackageURLBuilder;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.codehaus.plexus.util.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;

@Named
@Singleton
public class DocumentBuilderHelper
{
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
      final LicenseThreatGroupDAO licenseThreatGroupDAO)
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
    return new DocumentBuilder(ItemType.APPLICATION)
        .setOwner(application)
        .setOwner(org)
        .setAllowedContextIds(allowedContextIds)
        .build();
  }

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
    ApplicationReport applicationReport = null;
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
                  component),
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
      List<PolicyViolation> violations = policyViolationDAO.getUnfixedByApplicationIdAndStageId(
          application.getId(), stageType.getId());
      Map<String, String> constraintNameByFactsId = loadConstraintNames(violations);
      List<Document> policyViolationDocs = buildPolicyViolationDocuments(
          organization, parentOrganizations, application, stageType, scanId, violations, constraintNameByFactsId);

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
    if (parentOrganizations == null || organization == null || application == null || component == null) {
      return Collections.emptyList();
    }
    if (CollectionUtils.isNotEmpty(component.getSecurityVulnerabilities())) {
      return component.getSecurityVulnerabilities()
          .stream()
          .map(
              vulnerability -> buildDocument(indexingContext, organization, application, stageType, reportId, component,
                  vulnerability, parentOrganizations))
          .toList();
    }
    else if (component.getComponentIdentifier() != null) {
      return Collections.singletonList(
          buildDocument(organization, parentOrganizations, application, stageType, reportId, component));
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
    if (parentOrganizations == null || organization == null || application == null || component == null) {
      return null;
    }
    return new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT)
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
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()))
        .build();
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
    if (parentOrganizations == null || organization == null || application == null || component == null ||
        vulnerability == null)
    {
      return null;
    }
    return new DocumentBuilder(ItemType.SECURITY_VULNERABILITY)
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
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()))
        .build();
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
   * Derives the waiver status for a policy violation.
   */
  private static String deriveWaiverStatus(PolicyViolation violation) {
    if (violation.getAutoPolicyWaiverId() != null) {
      return "AutoWaived";
    }
    if (violation.getWaiveTime() != null) {
      return "Waived";
    }
    return "Active";
  }

  /**
   * Loads constraint names for policy violations by constraint facts ID.
   */
  private Map<String, String> loadConstraintNames(List<PolicyViolation> violations) {
    if (CollectionUtils.isEmpty(violations)) {
      return Collections.emptyMap();
    }

    Set<String> constraintFactsIds = new HashSet<>();
    for (PolicyViolation violation : violations) {
      if (violation.getConstraintFactsId() != null) {
        constraintFactsIds.add(violation.getConstraintFactsId());
      }
    }

    if (constraintFactsIds.isEmpty()) {
      return Collections.emptyMap();
    }

    List<PolicyViolationConstraintFacts> factsList = policyViolationConstraintFactsDAO.getByIds(constraintFactsIds);
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
   * Builds POLICY_VIOLATION documents for each policy violation.
   */
  private List<Document> buildPolicyViolationDocuments(
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

    List<Document> documents = new ArrayList<>();
    for (PolicyViolation violation : violations) {
      Document doc = buildPolicyViolationDocument(
          organization, parentOrganizations, application, stageType, reportId, violation, constraintNameByFactsId);
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
      Map<String, String> constraintNameByFactsId)
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
              licenseId, licenseName, threatGroup);
          if (doc != null) {
            documents.add(doc);
          }
        }
      }
      else {
        Document doc = buildLegalViolationDocument(
            organization, parentOrganizations, application, stageType, reportId, component,
            licenseId, licenseName, null);
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
      LicenseThreatGroup threatGroup)
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
        .setVulnerabilitySeverity(
            BigDecimal.valueOf(thirdPartyCoordinateSecurity.getSeverity())
                .setScale(2, RoundingMode.HALF_EVEN)
                .floatValue())
        .setVulnerabilityDescription(thirdPartyCoordinateSecurity.getDescription())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .setAllowedContextIds(computeAllowedContextIds(parentOrganizations, application.getId()))
        .build();
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
