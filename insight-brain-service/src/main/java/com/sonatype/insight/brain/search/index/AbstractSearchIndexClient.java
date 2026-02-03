/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor.AbortPolicy;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.QueryVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.index.FieldIdentifier.*;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.index.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;
import static java.util.stream.Collectors.toList;

public abstract class AbstractSearchIndexClient
    implements SearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(AbstractSearchIndexClient.class);

  public static final String ADVANCED_SEARCH_CREATE_SEARCH_INDEX = "AdvancedSearch.createSearchIndex";

  private static final int INDEX_THREADS_MIN = 1;

  private static final int INDEX_THREADS_MAX = Integer.MAX_VALUE;

  private static final int INDEX_THREADS_DEFAULT = 1;

  private static final int QUEUE_POP_AMOUNT = 64_000;

  public static final BadRequestException TOO_MANY_CLAUSES_EXCEPTION =
      new BadRequestException("Error performing search due to too many clauses. " +
          "Please try narrowing down the query as much as possible " +
          "and consider updating Advanced Search configuration to support larger queries.");

  private final ApplicationDAO applicationDAO;

  private final LabelDAO labelDAO;

  private final OrganizationDAO organizationDAO;

  protected final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

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

  private final CurrentUser currentUser;

  protected final ConversionHelper conversionHelper;

  protected final TenantReference<TenantThreadPoolExecutor> indexingExecutors;

  private final ShutdownHandler shutdownHandler;

  public AbstractSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
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
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler)
  {
    this.applicationDAO = applicationDAO;
    this.labelDAO = labelDAO;
    this.organizationDAO = organizationDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
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
    this.currentUser = currentUser;
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
            .anyMatch(field -> fieldNames.contains(field.label))) {
          return VULNERABILITY_ID;
        }
        if (Stream.of(COMPONENT_FORMAT, COMPONENT_HASH, COMPONENT_NAME)
            .anyMatch(field -> fieldNames.contains(field.label))
            || fieldNames.stream().anyMatch(fieldName -> fieldName.startsWith(COMPONENT_COORDINATE.label))) {
          return COMPONENT_NAME;
        }
        return APPLICATION_NAME;
      case NON_VULNERABLE_COMPONENT:
        return COMPONENT_NAME;
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
          .filter(fieldIdentifier -> fieldIdentifier.label.equals(fieldName)).findAny().orElse(null);
    }
    return identifier;
  }

  protected Map<String, OwnerType> getChildContextIds(final Set<String> contextIdsWithReadPermission) {
    Map<String, OwnerType> childContextIds = new HashMap<>();
    for (String contextIdWithReadPermission : contextIdsWithReadPermission) {
      Owner owner = ownerDAO.getById(contextIdWithReadPermission);
      if (owner != null) {
        childContextIds.put(owner.getId(), owner.getType());
        childContextIds
            .putAll(ownerDAO.walkChildren(owner).stream().collect(Collectors.toMap(Owner::getId, Owner::getType)));
      }
    }
    return childContextIds;
  }

  protected void checkMode(final boolean isSbomManagerMode) {
    if (isSbomManagerMode && !productLicense.hasFeature(LicensedFeature.SBOM_MANAGER)) {
      log.error("License does not have the SBOM Manager feature.");
      throw new InvalidLicenseException("The SBOM Manager feature is not supported by your license.");
    }
    if (!isSbomManagerMode && productLicense.hasFeature(LicensedFeature.SBOM_MANAGER) &&
        !hasProductSupportingDefaultMode()) {
      log.error("License does not support anything other than SBOM Manager mode.");
      throw new InvalidLicenseException("Only SBOM Manager mode is supported by your license.");
    }
  }

  // TODO possibly add a LicensedFeature.ADVANCED_SEARCH to replace this
  private boolean hasProductSupportingDefaultMode() {
    //     Auditor
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
      default:
        throw new IllegalArgumentException("Unknown change type: " + change.getChangeType());
    }
  }

  private void updateIndexForPolicyEvaluation(
      final String applicationId,
      final String stageTypeId,
      final IndexingContext indexingContext)
      throws IOException
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
    indexingContext.addDocumentsWithException(
        documentBuilderHelper.buildApplicationStageSVDocs(indexingContext, organization, application, stageType,
            parentOrganizations));
  }

  private void updateIndexForSbom(
      final String applicationId,
      final String applicationVersion,
      final IndexingContext indexingContext)
      throws IOException
  {
    String queryForObsoleteDocs = "(" +
        indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId) +
        " AND " +
        indexingContext.newQuery(FieldIdentifier.APPLICATION_VERSION, QueryParser.escape(applicationVersion)) +
        ")";
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Application application = applicationDAO.getById(applicationId);
    ThirdPartySbomMetadata sbomMetadata =
        thirdPartySbomMetadataDAO.getByApplicationIdAndSbomVersion(applicationId, applicationVersion);

    if (application == null || sbomMetadata == null) {
      return;
    }

    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      return;
    }

    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    Document sbomDoc = documentBuilderHelper.buildDocument(indexingContext, sbomMetadata);
    List<Document> sbomContentsDocs =
        documentBuilderHelper.buildSbomVersionSVDocs(organization, application, sbomMetadata, parentOrganizations);

    List<Document> docsToAdd = new ArrayList<>(sbomContentsDocs.size() + 1);
    docsToAdd.addAll(sbomContentsDocs);
    docsToAdd.add(sbomDoc);

    indexingContext.addDocumentsWithException(docsToAdd);
  }

  private void updateIndexForLabel(final String labelId, final IndexingContext indexingContext)
      throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.COMPONENT_LABEL_ID, labelId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Label label = labelDAO.getById(labelId);

    if (label == null) {
      return;
    }

    indexingContext.addDocumentsWithException(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, label)));
  }

  private void updateIndexForPolicy(final String policyId, final IndexingContext indexingContext)
      throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.POLICY_ID, policyId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Policy policy = policyDAO.getById(policyId);

    if (policy == null) {
      return;
    }

    indexingContext.addDocumentsWithException(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, policy)));
  }

  private void updateIndexForApplicationCategory(final String tagId, final IndexingContext indexingContext)
      throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.APPLICATION_CATEGORY_ID, tagId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);
    Tag tag = tagDAO.getById(tagId);

    if (tag == null) {
      return;
    }

    indexingContext.addDocumentsWithException(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, tag)));
  }

  private void updateIndexForApplication(final String applicationId, final IndexingContext indexingContext)
      throws IOException
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

    // Index the app itself
    indexingContext.addDocumentsWithException(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, application)));
    // Index the app labels
    List<Document> appLabelDocs = labelDAO.getByOwnerId(application.getId()).stream()
        .map(label -> documentBuilderHelper.buildDocument(indexingContext, label)).collect(toList());
    indexingContext.addDocumentsWithException(appLabelDocs);
    // Index the app policies
    List<Document> appPolicyDocs = policyDAO.getByOwnerId(application.getId()).stream()
        .map(policy -> documentBuilderHelper.buildDocument(indexingContext, policy)).collect(toList());
    indexingContext.addDocumentsWithException(appPolicyDocs);
    // Index the app SVs
    indexingContext.addDocumentsWithException(
        documentBuilderHelper.buildApplicationSVDocs(indexingContext, organization, application));
  }

  private void updateIndexForOrganization(final String organizationId, final IndexingContext indexingContext)
      throws IOException
  {
    String queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.ORGANIZATION_ID, organizationId);
    indexingContext.deleteDocuments(queryForObsoleteDocs);

    Organization org = organizationDAO.getById(organizationId);
    if (org == null) {
      return;
    }

    // Index the org itself
    indexingContext.addDocumentsWithException(
        Collections.singletonList(documentBuilderHelper.buildDocument(indexingContext, org)));
    // Index the org apps
    List<Document> orgAppDocs = applicationDAO.getByOrganizationId(org.getId()).stream()
        .map(app -> documentBuilderHelper.buildDocument(indexingContext, app)).collect(toList());
    indexingContext.addDocumentsWithException(orgAppDocs);
    // Index the org app categories
    List<Document> orgAppCategoryDocs = tagDAO.getByOrganizationId(org.getId()).stream()
        .map(appCategory -> documentBuilderHelper.buildDocument(indexingContext, appCategory)).collect(toList());
    indexingContext.addDocumentsWithException(orgAppCategoryDocs);
    // Index the org labels
    List<Document> orgLabelDocs = labelDAO.getByOwnerId(org.getId()).stream()
        .map(label -> documentBuilderHelper.buildDocument(indexingContext, label)).collect(toList());
    indexingContext.addDocumentsWithException(orgLabelDocs);
    // Index the org policies
    List<Document> orgPolicyDocs = policyDAO.getByOwnerId(org.getId()).stream()
        .map(policy -> documentBuilderHelper.buildDocument(indexingContext, policy)).collect(toList());
    indexingContext.addDocumentsWithException(orgPolicyDocs);

    // Index the security vulnerability data
    List<Application> applications = applicationDAO.getByOrganizationId(organizationId);
    for (Application application : applications) {
      indexingContext.addDocumentsWithException(
          documentBuilderHelper.buildApplicationSVDocs(indexingContext, org, application));
    }

    List<Organization> byParentOrganizationId = organizationDAO.getByParentOrganizationId(organizationId);
    for (Organization organization : byParentOrganizationId) {
      updateIndexForOrganization(organization.getId(), indexingContext);
    }
  }

  protected String createInitialQuery(final String searchQuery, final boolean allComponents) {
    if (StringUtils.isBlank(searchQuery)) {
      throw new BadRequestException("The search query is empty");
    }
    String finalSearchQuery = allComponents ? searchQuery :
        searchQuery + " -" + ITEM_TYPE.label + ":" + NON_VULNERABLE_COMPONENT.name();

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
    String queryWithSbomFiltering = appendSbomFilteringToQuery(query, isSbomManagerMode);
    return appendAllowedApplicationsAndOrganizationsToQuery(queryWithSbomFiltering);
  }

  private String appendAllowedApplicationsAndOrganizationsToQuery(final String query) {
    Set<String> contextIdsWithReadPermission =
        permissionService.getContextIdsForUserWithPermission(currentUser.getUserPrincipal(), Permission.READ);

    if (contextIdsWithReadPermission.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        contextIdsWithReadPermission.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return query;
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = getChildContextIds(contextIdsWithReadPermission);

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
      return "(" + query + ") AND (NOT *:*)";
    }

    String allowedContextsQuery = String.join(" OR ", allowedContextConditions);
    return "(" + allowedContextsQuery + ") AND (" + query + ")";
  }

  /**
   * When the REST API is called in: <br/><br/> SBOM Manager mode
   * <ul>
   *   <li>Components without an applicationVersion MUST NOT be returned</li>
   *   <li>Vulnerabilities without an applicationVersion MUST NOT be returned</li>
   *   <li>Application categories MUST NOT be returned</li>
   *   <li>Component labels MUST NOT be returned</li>
   *   <li>Policies MUST NOT be returned</li>
   * </ul>
   * Default Mode
   * </ul>
   *   <li>Components with an applicationVersion MUST NOT be returned</li>
   *   <li>Vulnerabilities with an applicationVersion MUST NOT be returned</li>
   *   <li>SBOM metadata MUST NOT be returned</li>
   * </ul>
   */
  private String appendSbomFilteringToQuery(final String originalQuery, final boolean isSbomManagerMode) {
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
          ADVANCED_SEARCH_CREATE_SEARCH_INDEX
      );
      TenantThreadPoolExecutor tenantThreadPoolExecutor = new TenantThreadPoolExecutor(
          threadCount,
          threadCount,
          5L,
          TimeUnit.SECONDS,
          new LinkedBlockingQueue<>(),
          new ThreadFactoryBuilder().setNameFormat(getClass().getSimpleName() + "-%d").build(),
          new AbortPolicy(),
          "advanced_search_index",
          getClass().getSimpleName()
      );
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

    CompletableFuture<Void> orgDocs = CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildOrganizationDocs(indexingContext, organizations),
            getIndexingExecutor())
        .thenAccept(indexingContext::addDocumentsWithException);

    CompletableFuture<Void> appDocs = CompletableFuture.supplyAsync(
            () -> documentBuilderHelper.buildApplicationDocs(indexingContext, applications),
            getIndexingExecutor())
        .thenAccept(indexingContext::addDocumentsWithException);

    Function<Application, CompletableFuture<Void>> processSVDocsForApplication =
        application -> CompletableFuture
            .supplyAsync(
                () -> documentBuilderHelper.buildApplicationSVDocs(indexingContext,
                    organizationById.get(application.getOrganizationId()),
                    application, parentsByOrganization), getIndexingExecutor())
            .thenAccept(indexingContext::addDocumentsWithException);

    List<CompletableFuture<Void>> appSVDocs = applications
        .stream()
        .map(processSVDocsForApplication)
        .toList();

    CompletableFuture<Void> tagDocs =
        CompletableFuture.supplyAsync(
                () -> documentBuilderHelper.buildTagDocs(indexingContext), getIndexingExecutor())
            .thenAccept(indexingContext::addDocumentsWithException);

    CompletableFuture<Void> labelDocs =
        CompletableFuture.supplyAsync(
                () -> documentBuilderHelper.buildLabelDocs(indexingContext),
                getIndexingExecutor())
            .thenAccept(indexingContext::addDocumentsWithException);

    CompletableFuture<Void> policyDocs =
        CompletableFuture.supplyAsync(
                () -> documentBuilderHelper.buildPolicyDocs(indexingContext),
                getIndexingExecutor())
            .thenAccept(indexingContext::addDocumentsWithException);

    CompletableFuture<Void> sbomDocs = CompletableFuture.supplyAsync(
        () -> documentBuilderHelper.buildSbomDocs(indexingContext), getIndexingExecutor()
    ).thenAccept(indexingContext::addDocumentsWithException);

    Function<Application, CompletableFuture<Void>> processSbomSVDocsForApplication =
        application -> CompletableFuture
            .supplyAsync(
                () -> documentBuilderHelper.buildSbomSVDocs(organizationById.get(application.getOrganizationId()),
                    application, parentsByOrganization), getIndexingExecutor())
            .thenAccept(indexingContext::addDocumentsWithException);

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
    sbomDocs.join();
    log.info("SBOM metadata indexing complete");
    CompletableFuture.allOf(sbomSVDocs.toArray(CompletableFuture[]::new)).join();
    log.info("sbomSV indexing complete");
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
      final Consumer<SearchIndexChange> deletionCallback)
      throws IOException
  {
    log.debug("Updating search index with {} changes", searchIndexChanges.size());
    Set<String> alreadyApplied = new HashSet<>();
    for (SearchIndexChange change : searchIndexChanges) {
      if (alreadyApplied.add(change.getChangeType() + "\t" + change.getChangeData())) {
        try {
          updateIndex(change, indexingContext);
          log.debug("Updated search index with change {}", change);
        }
        catch (IOException e) {
          if (hasParseExceptionInCauseChain(e)) {
            log.warn("Skipping search index update due to parse exception", e);
          }
          else {
            throw e;
          }
        }
        change.setProcessed(true);
        deletionCallback.accept(change);
      }
    }
    log.debug("Updated search index");
  }

  private boolean hasParseExceptionInCauseChain(Throwable throwable) {
    Throwable cause = throwable;
    while (cause != null) {
      if (cause instanceof ParseException) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }

  @Override
  public void deleteSearchIndexChange(final SearchIndexChange change) {
    searchIndexChangeDAO.delete(change);
  }

  protected abstract void updateMaxQueryClauseCount() throws IOException;
}
