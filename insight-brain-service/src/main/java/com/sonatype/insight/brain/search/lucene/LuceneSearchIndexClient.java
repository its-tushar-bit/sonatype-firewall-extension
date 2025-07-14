/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentDisplayNameUtil;
import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyCoordinateSecurityDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.report.ApplicationReport;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.report.ReportService;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.search.index.FieldIdentifier;
import com.sonatype.insight.brain.search.index.ItemType;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.index.VulnerabilityDescriptionFetcher;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;
import com.sonatype.insight.purl.PackageUrlIdentifier;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.github.packageurl.PackageURLBuilder;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.FieldExistsQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndexSearcher.TooManyClauses;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.BOM_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.DEPENDENCIES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.LICENSES_JSON;
import static com.sonatype.insight.brain.report.ApplicationReport.ReportFile.SECURITY_JSON;
import static com.sonatype.insight.brain.search.index.FieldIdentifier.*;
import static com.sonatype.insight.brain.search.index.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.index.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.index.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.index.ItemType.POLICY;
import static com.sonatype.insight.brain.search.index.ItemType.SBOM_METADATA;
import static java.util.stream.Collectors.toList;

/**
 * Lucene support for {@link SearchIndexClient}
 * <p>
 * Note: See {@link SearchModule} for Guice bindings
 */
@Singleton
public class LuceneSearchIndexClient
    implements SearchIndexClient
{
  public static final String SEARCH_INDEX_CONFIG_PROPS = "AdvancedSearch.createSearchIndex";

  private static final int INDEX_THREADS_MIN = 1;

  private static final int INDEX_THREADS_MAX = 7;

  private static final int INDEX_THREADS_DEFAULT = 1;

  private static final String NO_INDEX_ERROR_MESSAGE =
      "Index does not exist or is unreadable, please (re)create your index.";

  private static final Logger log = LoggerFactory.getLogger(LuceneSearchIndexClient.class);

  private static final int QUEUE_POP_AMOUNT = 64_000;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final TagDAO tagDAO;

  private final LabelDAO labelDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final ThirdPartySbomMetadataDAO sbomMetadataDAO;

  private final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO;

  private final SearchIndexChangeDAO searchIndexChangeDAO;

  private final TelemetrySender telemetrySender;

  private final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  private final LuceneComponents luceneComponents;

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private final ForkJoinPool searchIndexPool;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final ReportService reportService;

  private final InsightWork insightWork;

  private final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics;

  private final Configuration configuration;

  private final PermissionService permissionService;

  private final CurrentUser currentUser;

  private final ProductLicense productLicense;

  @Inject
  public LuceneSearchIndexClient(
      final OrganizationDAO organizationDAO,
      final ApplicationDAO applicationDAO,
      final PolicyEvaluationDAO policyEvaluationDAO,
      final TagDAO tagDAO,
      final LabelDAO labelDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final TelemetrySender telemetrySender,
      final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher,
      final LuceneComponents luceneComponents,
      final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      final ThirdPartySbomMetadataDAO sbomMetadataDAO,
      final ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO,
      final ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final ComponentLoaderFactory componentLoaderFactory,
      final ReportService reportService,
      final InsightWork insightWork,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final CurrentUser currentUser,
      final ProductLicense productLicense)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.tagDAO = tagDAO;
    this.labelDAO = labelDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.searchIndexChangeDAO = searchIndexChangeDAO;
    this.telemetrySender = telemetrySender;
    this.vulnerabilityDescriptionFetcher = vulnerabilityDescriptionFetcher;
    this.luceneComponents = luceneComponents;
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
    this.sbomMetadataDAO = sbomMetadataDAO;
    this.thirdPartyFileCoordinateDAO = thirdPartyFileCoordinateDAO;
    this.thirdPartyCoordinateSecurityDAO = thirdPartyCoordinateSecurityDAO;
    this.thirdPartySbomMetadataDAO = thirdPartySbomMetadataDAO;
    this.componentLoaderFactory = componentLoaderFactory;
    this.reportService = reportService;
    this.insightWork = insightWork;
    this.advancedSearchTelemetryMetrics = advancedSearchTelemetryMetrics;
    this.configuration = configuration;
    this.permissionService = permissionService;
    this.currentUser = currentUser;
    this.productLicense = productLicense;

    searchIndexPool = ExecutorThreadPools.getInstance()
        .createThreadPool(INDEX_THREADS_MIN, INDEX_THREADS_MAX, INDEX_THREADS_DEFAULT,
            SEARCH_INDEX_CONFIG_PROPS);
  }

  private static void addDocsWithException(IndexWriter writer, List<Document> docs) {
    try {
      writer.addDocuments(docs);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  @Override
  public void createIndex() {
    log.info("creating search index...");
    long start = System.currentTimeMillis();

    try (Directory directory = luceneComponents.openSearchIndex(false);
         IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE)) {
      log.info("begin indexing");

      List<Organization> organizations = organizationDAO.getAll();
      Map<String, Organization> organizationById =
          organizations.stream().collect(Collectors.toMap(Organization::getId, item -> item));
      Map<Organization, Collection<Organization>> parentsByOrganization =
          computeParentsByOrganization(organizationById).asMap();
      List<Application> applications = applicationDAO.getAll();

      IndexingContext indexingContext = new IndexingContext(indexWriter);
      indexingContext.addOwners(organizations);
      indexingContext.addOwners(applications);

      CompletableFuture<Void> orgDocs = CompletableFuture.supplyAsync(
              new TenantAwareSupplier<>(() -> buildOrganizationDocs(indexingContext, organizations)), searchIndexPool)
          .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> appDocs = CompletableFuture.supplyAsync(
              new TenantAwareSupplier<>(() -> buildApplicationDocs(indexingContext, applications)), searchIndexPool)
          .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      TenantAwareFunction<Application, CompletableFuture<Void>> processSVDocsForApplication =
          new TenantAwareFunction<>(application -> CompletableFuture
              .supplyAsync(new TenantAwareSupplier<>(
                  () -> buildApplicationSVDocs(indexingContext, organizationById.get(application.getOrganizationId()),
                      application, parentsByOrganization)), searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs)));

      List<CompletableFuture<Void>> appSVDocs = applications
          .parallelStream()
          .map(processSVDocsForApplication)
          .toList();

      CompletableFuture<Void> tagDocs =
          CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> buildTagDocs(indexingContext)), searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> labelDocs =
          CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> buildLabelDocs(indexingContext)),
                  searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> policyDocs =
          CompletableFuture.supplyAsync(new TenantAwareSupplier<>(() -> buildPolicyDocs(indexingContext)),
                  searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> sbomDocs = CompletableFuture.supplyAsync(
          new TenantAwareSupplier<>(() -> buildSbomDocs(indexingContext)), searchIndexPool
      ).thenAccept(docs -> addDocsWithException(indexWriter, docs));

      TenantAwareFunction<Application, CompletableFuture<Void>> processSbomSVDocsForApplication =
          new TenantAwareFunction<>(application -> CompletableFuture
              .supplyAsync(new TenantAwareSupplier<>(
                  () -> buildSbomSVDocs(organizationById.get(application.getOrganizationId()),
                      application, parentsByOrganization)), searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs)));

      List<CompletableFuture<Void>> sbomSVDocs = applications
          .parallelStream()
          .map(processSbomSVDocsForApplication)
          .toList();

      log.info("indexing threads started");
      orgDocs.join();
      log.info("org indexing complete");
      appDocs.join();
      log.info("app indexing complete");
      appSVDocs.forEach(CompletableFuture::join);
      log.info("appSV indexing complete");
      tagDocs.join();
      log.info("tag indexing complete");
      labelDocs.join();
      log.info("label indexing complete");
      policyDocs.join();
      log.info("policy indexing complete");
      sbomDocs.join();
      log.info("SBOM metadata indexing complete");
      sbomSVDocs.forEach(CompletableFuture::join);
      log.info("sbomSV indexing complete");
      indexWriter.commit();
      log.info("all indexing complete");
    }
    catch (IOException e) {
      throw new SearchIndexException("Error creating search index", e);
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_DURATION_SECONDS, (System.currentTimeMillis() - start) / 1000);
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_SIZE_BYTES, getIndexSize());
    telemetryData.put(SearchIndexClient.SEARCH_INDEX_REINDEX, true);
    telemetrySender.send(telemetryData);

    log.info("index creation exit");
  }

  @Override
  public long getIndexSize() {
    try (Directory indexDir = luceneComponents.openSearchIndex(true)) {
      long bytes = 0;
      if (indexDir != null) {
        for (String filename : indexDir.listAll()) {
          bytes += indexDir.fileLength(filename);
        }
      }
      return bytes;
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  @Override
  public void updateIndex() {
    // Note: this pops a limited amount of records off the 'queue' as there are cases of large amounts of rows
    // accumulating. See CLM-29339. TODO Future enhancements will further improve this code - CLM-29618
    List<SearchIndexChange> changes = searchIndexChangeDAO.getBatch(QUEUE_POP_AMOUNT);
    if (changes.isEmpty()) {
      return;
    }
    log.debug("Updating search index with {} changes", changes.size());
    try (Directory directory = luceneComponents.openSearchIndex(false);
         IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE_OR_APPEND)) {
      IndexingContext indexingContext = new IndexingContext(indexWriter);
      Set<String> alreadyApplied = new HashSet<>();
      for (SearchIndexChange change : changes) {
        if (alreadyApplied.add(change.getChangeType() + "\t" + change.getChangeData())) {
          updateIndex(change, indexingContext);
          log.debug("Updated search index with change {}", change);
        }
        searchIndexChangeDAO.delete(change);
      }
    }
    catch (IOException e) {
      throw new SearchIndexException("Error updating the search index", e);
    }
    log.debug("Updated search index");
  }

  @Override
  public Long getLastIndexTime() {
    try (Directory directory = luceneComponents.openSearchIndex(true)) {
      if (directory == null) {
        return null;
      }
      String lastCommitSegmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(directory);
      if (lastCommitSegmentsFileName == null) {
        return null;
      }
      return new File(insightWork.getSearchIndexDir(), lastCommitSegmentsFileName).lastModified();
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      return null;
    }
  }

  private IndexWriter newIndexWriter(Directory directory, OpenMode openMode) throws IOException {
    return new IndexWriter(directory,
        new IndexWriterConfig(luceneComponents.newAnalyzerForSearch()).setOpenMode(openMode));
  }

  private void updateIndex(SearchIndexChange change, IndexingContext indexingContext) throws IOException {
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

  private void updateIndexForPolicyEvaluation(String applicationId, String stageTypeId, IndexingContext indexingContext)
      throws IOException
  {
    Query queryForObsoleteDocs = new BooleanQuery.Builder() //
        .add(indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId), Occur.MUST) //
        .add(indexingContext.newQuery(FieldIdentifier.POLICY_EVALUATION_STAGE, stageTypeId), Occur.MUST) //
        .build();
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);
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
    addDocsWithException(indexingContext.indexWriter,
        buildApplicationStageSVDocs(indexingContext, organization, application, stageType, parentOrganizations));
  }

  private void updateIndexForSbom(String applicationId, String applicationVersion, IndexingContext indexingContext)
      throws IOException
  {
    Query queryForObsoleteDocs = new BooleanQuery.Builder()
        .add(indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId), Occur.MUST)
        .add(indexingContext.newQuery(FieldIdentifier.APPLICATION_VERSION, applicationVersion), Occur.MUST)
        .build();
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);

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

    Document sbomDoc = buildDocument(indexingContext, sbomMetadata);
    List<Document> sbomContentsDocs =
        buildSbomVersionSVDocs(organization, application, sbomMetadata, parentOrganizations);

    List<Document> docsToAdd = new ArrayList<>(sbomContentsDocs.size() + 1);
    docsToAdd.addAll(sbomContentsDocs);
    docsToAdd.add(sbomDoc);

    addDocsWithException(indexingContext.indexWriter, docsToAdd);
  }

  private void updateIndexForLabel(String labelId, IndexingContext indexingContext)
      throws IOException
  {
    Query queryForObsoleteDocs = new BooleanQuery.Builder() //
        .add(indexingContext.newQuery(FieldIdentifier.COMPONENT_LABEL_ID, labelId), Occur.MUST) //
        .build();
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);
    Label label = labelDAO.getById(labelId);

    if (label == null) {
      return;
    }
    addDocsWithException(indexingContext.indexWriter, Collections.singletonList(buildDocument(indexingContext, label)));
  }

  private void updateIndexForPolicy(String policyId, IndexingContext indexingContext)
      throws IOException
  {
    Query queryForObsoleteDocs = new BooleanQuery.Builder() //
        .add(indexingContext.newQuery(FieldIdentifier.POLICY_ID, policyId), Occur.MUST) //
        .build();
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);
    Policy policy = policyDAO.getById(policyId);

    if (policy == null) {
      return;
    }
    addDocsWithException(indexingContext.indexWriter,
        Collections.singletonList(buildDocument(indexingContext, policy)));
  }

  private void updateIndexForApplicationCategory(String tagId, IndexingContext indexingContext)
      throws IOException
  {
    Query queryForObsoleteDocs = new BooleanQuery.Builder() //
        .add(indexingContext.newQuery(FieldIdentifier.APPLICATION_CATEGORY_ID, tagId), Occur.MUST) //
        .build();
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);
    Tag tag = tagDAO.getById(tagId);

    if (tag == null) {
      return;
    }
    addDocsWithException(indexingContext.indexWriter, Collections.singletonList(buildDocument(indexingContext, tag)));
  }

  private void updateIndexForApplication(String applicationId, IndexingContext indexingContext) throws IOException {
    Query queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.APPLICATION_ID, applicationId);
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);

    Application application = applicationDAO.getById(applicationId);
    if (application == null) {
      return;
    }
    Organization organization = organizationDAO.getById(application.getOrganizationId());
    if (organization == null) {
      return;
    }

    // Index the app itself
    addDocsWithException(indexingContext.indexWriter,
        Collections.singletonList(buildDocument(indexingContext, application)));
    // Index the app labels
    List<Document> appLabelDocs = labelDAO.getByOwnerId(application.getId()).stream()
        .map(label -> buildDocument(indexingContext, label)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, appLabelDocs);
    // Index the app policies
    List<Document> appPolicyDocs = policyDAO.getByOwnerId(application.getId()).stream()
        .map(policy -> buildDocument(indexingContext, policy)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, appPolicyDocs);
    // Index the app SVs
    addDocsWithException(indexingContext.indexWriter,
        buildApplicationSVDocs(indexingContext, organization, application));
  }

  private void updateIndexForOrganization(String organizationId, IndexingContext indexingContext) throws IOException {
    Query queryForObsoleteDocs = indexingContext.newQuery(FieldIdentifier.ORGANIZATION_ID, organizationId);
    indexingContext.indexWriter.deleteDocuments(queryForObsoleteDocs);

    Organization org = organizationDAO.getById(organizationId);
    if (org == null) {
      return;
    }

    // Index the org itself
    addDocsWithException(indexingContext.indexWriter, Collections.singletonList(buildDocument(indexingContext, org)));
    // Index the org apps
    List<Document> orgAppDocs = applicationDAO.getByOrganizationId(org.getId()).stream()
        .map(app -> buildDocument(indexingContext, app)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, orgAppDocs);
    // Index the org app categories
    List<Document> orgAppCategoryDocs = tagDAO.getByOrganizationId(org.getId()).stream()
        .map(appCategory -> buildDocument(indexingContext, appCategory)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, orgAppCategoryDocs);
    // Index the org labels
    List<Document> orgLabelDocs = labelDAO.getByOwnerId(org.getId()).stream()
        .map(label -> buildDocument(indexingContext, label)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, orgLabelDocs);
    // Index the org policies
    List<Document> orgPolicyDocs = policyDAO.getByOwnerId(org.getId()).stream()
        .map(policy -> buildDocument(indexingContext, policy)).collect(toList());
    addDocsWithException(indexingContext.indexWriter, orgPolicyDocs);

    // Index the security vulnerability data
    List<Application> applications = applicationDAO.getByOrganizationId(organizationId);
    for (Application application : applications) {
      addDocsWithException(indexingContext.indexWriter, buildApplicationSVDocs(indexingContext, org, application));
    }

    List<Organization> byParentOrganizationId = organizationDAO.getByParentOrganizationId(organizationId);
    for (Organization organization : byParentOrganizationId) {
      updateIndexForOrganization(organization.getId(), indexingContext);
    }
  }

  private List<Document> buildOrganizationDocs(
      IndexingContext indexingContext,
      Collection<Organization> organizations)
  {
    return organizations.stream().map(org -> buildDocument(indexingContext, org)).collect(toList());
  }

  Document buildDocument(@SuppressWarnings("unused") IndexingContext indexingContext, Organization organization) {
    return new DocumentBuilder(ItemType.ORGANIZATION) //
        .setOwner(organization) //
        .build();
  }

  private List<Document> buildApplicationDocs(IndexingContext indexingContext, Collection<Application> applications) {
    return applications.stream().map(app -> buildDocument(indexingContext, app)).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Application application) {
    return new DocumentBuilder(ItemType.APPLICATION) //
        .setOwner(application) //
        .setOwner(indexingContext.getOwner(application.getOrganizationId())) //
        .build();
  }

  private List<Document> buildTagDocs(IndexingContext indexingContext) {
    return tagDAO.getAll().stream().map(tag -> buildDocument(indexingContext, tag)).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Tag tag) {
    return new DocumentBuilder(ItemType.APPLICATION_CATEGORY) //
        .setApplicationCategoryId(tag.getId()) //
        .setApplicationCategoryName(tag.getName()) //
        .setApplicationCategoryColor(tag.getColor()) //
        .setApplicationCategoryDescription(tag.getDescription()) //
        .setOwner(indexingContext.getOwner(tag.getOrganizationId())) //
        .build();
  }

  private List<Document> buildLabelDocs(IndexingContext indexingContext) {
    return labelDAO.getAll().stream().map(label -> buildDocument(indexingContext, label)).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Label label) {
    return new DocumentBuilder(ItemType.COMPONENT_LABEL) //
        .setComponentLabelId(label.getId()) //
        .setComponentLabelName(label.getLabel()) //
        .setComponentLabelColor(label.getColor()) //
        .setComponentLabelDescription(label.getDescription()) //
        .setOwner(indexingContext.getOwner(label.getOwnerId())) //
        .build();
  }

  private List<Document> buildPolicyDocs(IndexingContext indexingContext) {
    return policyDAO.getAll().stream().map(policy -> buildDocument(indexingContext, policy)).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Policy policy) {
    return new DocumentBuilder(ItemType.POLICY) //
        .setPolicyId(policy.getId()) //
        .setPolicyName(policy.getName()) //
        .setPolicyThreatCategory(policy.getThreatCategory()) //
        .setPolicyThreatLevel(policy.getThreatLevel()) //
        .setOwner(indexingContext.getOwner(policy.getOwnerId())) //
        .build();
  }

  private List<Document> buildSbomDocs(IndexingContext indexingContext) {
    return sbomMetadataDAO.getAll().stream()
        .map(sbomMetadata -> buildDocument(indexingContext, sbomMetadata))
        .collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, ThirdPartySbomMetadata sbomMetadata) {
    Owner owner = indexingContext.getOwner(sbomMetadata.getApplicationId());
    if (!(owner instanceof Application)) {
      throw new IllegalStateException("ThirdPartySbomMetadata " + sbomMetadata.getId() + " has owner that is not " +
          "of type Application: " + owner);
    }

    Application application = (Application) owner;
    Organization org = (Organization) indexingContext.getOwner(application.getOrganizationId());

    return new DocumentBuilder(ItemType.SBOM_METADATA)
        .setOwner(application)
        .setOwner(org)
        .setApplicationVersion(sbomMetadata.getSbomVersion())
        .setSbomSpecification(sbomMetadata.getSpec())
        .build();
  }

  private List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application)
  {
    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    return buildApplicationSVDocs(indexingContext, organization, application,
        ImmutableMap.of(organization, parentOrganizations));
  }

  private List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {

    return StageTypes.getAll().parallelStream()
        .map(new TenantAwareFunction<>(
            stageType -> buildApplicationStageSVDocs(indexingContext, organization, application, stageType,
                parentOrgsMap.get(organization))))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildApplicationStageSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      Collection<Organization> parentOrganizations)
  {
    PolicyEvaluation latestPolicyEvaluation =
        policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageType.getId());
    if (latestPolicyEvaluation == null) {
      return Collections.emptyList();
    }
    String scanId = latestPolicyEvaluation.getScanId();
    ApplicationReport applicationReport = reportService.getReport(application.getId(), scanId);
    try {
      if (!applicationReport.exists()) {
        return Collections.emptyList();
      }
      ReportEntry licenseReportEntry = applicationReport.getEntry(LICENSES_JSON.getName());
      ReportEntry securityReportEntry = applicationReport.getEntry(SECURITY_JSON.getName());
      ReportEntry bomReportEntry = applicationReport.getEntry(BOM_JSON.getName());
      ReportEntry dependenciesReportEntry = applicationReport.getEntry(DEPENDENCIES_JSON.getName());
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null ||
          dependenciesReportEntry == null) {
        return Collections.emptyList();
      }

      return componentLoaderFactory.createComponentLoader(application)
          .getAll(licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf,
              dependenciesReportEntry.buf)
          .parallelStream()
          .map(new TenantAwareFunction<>(
              component -> buildApplicationComponentVulnerabilityDocuments(
                  indexingContext,
                  organization,
                  parentOrganizations,
                  application,
                  stageType,
                  scanId,
                  component))).flatMap(Collection::stream).collect(toList());
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    catch (UncheckedIOException e) {
      log.error("Error parsing report files at {}", applicationReport.getLocation(), e);
    }
    return Collections.emptyList();
  }

  private List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    if (CollectionUtils.isNotEmpty(component.getSecurityVulnerabilities())) {
      return component.getSecurityVulnerabilities().parallelStream()
          .map(new TenantAwareFunction<>(
              vulnerability -> buildDocument(indexingContext, application, stageType, reportId, component,
                  vulnerability, parentOrganizations)))
          .collect(toList());
    }
    else if (component.getComponentIdentifier() != null) {
      return Collections.singletonList(
          buildDocument(organization, parentOrganizations, application, stageType, reportId, component));
    }
    else {
      return Collections.emptyList();
    }
  }

  Document buildDocument(
      Organization organization,
      Collection<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    return new DocumentBuilder(ItemType.NON_VULNERABLE_COMPONENT) //
        .setOwner(application) //
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organization.getName())
        .setPolicyEvaluationStage(stageType) //
        .setReportId(reportId) //
        .setComponentHash(component.getHash()) //
        .setComponentFormat(component.getComponentIdentifier().getFormat()) //
        .setComponentCoordinates(component) //
        .setComponentName(component.getDisplayNameFromIdentifier()) //
        .setParentOrganizationNames(parentOrganizations) //
        .setParentOrganizationIds(parentOrganizations) //
        .build();
  }

  Document buildDocument(
      IndexingContext indexingContext,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      SecurityVulnerability vulnerability,
      Collection<Organization> parentOrganizations)
  {
    return new DocumentBuilder(ItemType.SECURITY_VULNERABILITY) //
        .setOwner(application) //
        .setOrganizationId(application.getOrganizationId())
        .setOrganizationName(organizationDAO.getById(application.getOrganizationId()).getName())
        .setPolicyEvaluationStage(stageType) //
        .setReportId(reportId) //
        .setComponentHash(component.getHash()) //
        .setComponentFormat(component.getComponentIdentifier().getFormat()) //
        .setComponentCoordinates(component) //
        .setComponentName(component.getDisplayNameFromIdentifier()) //
        .setVulnerabilityId(vulnerability.getRefId()) //
        .setVulnerabilitySeverity(vulnerability.getSeverity()) //
        .setVulnerabilityStatus(vulnerability.getStatus().getName()) //
        .setVulnerabilityDescription(getDescription(indexingContext, vulnerability)) //
        .setParentOrganizationNames(parentOrganizations) //
        .setParentOrganizationIds(parentOrganizations) //
        .build();
  }

  private List<Document> buildSbomSVDocs(
      Organization organization,
      Application application,
      Map<Organization, Collection<Organization>> parentOrgsMap)
  {
    return sbomMetadataDAO.getByApplicationId(application.getId()).parallelStream()
        .map(new TenantAwareFunction<>(
            sbomMetadata -> buildSbomVersionSVDocs(organization, application, sbomMetadata,
                parentOrgsMap.get(organization))))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildSbomVersionSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations)
  {
    return thirdPartyFileCoordinateDAO.getBySbomMetadataId(sbomMetadata.getId()).parallelStream()
        .map(new TenantAwareFunction<>(
            fileCoord -> buildSbomFileCoordinateSVDocs(organization, application, sbomMetadata,
                parentOrganizations, fileCoord)))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildSbomFileCoordinateSVDocs(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      Collection<Organization> parentOrganizations,
      ThirdPartyFileCoordinate thirdPartyFileCoord)
  {
    List<ThirdPartyCoordinateSecurity> vulns = thirdPartyCoordinateSecurityDAO.getByFileCoordinateIds(
        Collections.singletonList(thirdPartyFileCoord.getId())
    );

    if (CollectionUtils.isNotEmpty(vulns)) {
      return vulns.parallelStream()
          .map(new TenantAwareFunction<>(
              vuln -> buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, vuln,
                  parentOrganizations)))
          .collect(toList());
    }
    else if (thirdPartyFileCoord.getPackageUrl() != null) {
      return Collections.singletonList(
          buildDocument(organization, application, sbomMetadata, thirdPartyFileCoord, parentOrganizations));
    }
    else {
      return Collections.emptyList();
    }
  }

  Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      Collection<Organization> parentOrganizations)
  {
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
        .build();
  }

  Document buildDocument(
      Organization organization,
      Application application,
      ThirdPartySbomMetadata sbomMetadata,
      ThirdPartyFileCoordinate thirdPartyFileCoord,
      ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity,
      Collection<Organization> parentOrganizations)
  {
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
        .setComponentCoordinates(componentIdentifier)
        .setComponentName(ComponentDisplayNameUtil.fromIdentifier(componentIdentifier).toString())
        .setVulnerabilityId(thirdPartyCoordinateSecurity.getRefId())
        .setVulnerabilitySeverity(
            BigDecimal.valueOf(thirdPartyCoordinateSecurity.getSeverity()).setScale(2, RoundingMode.UNNECESSARY)
                .floatValue())
        .setVulnerabilityDescription(thirdPartyCoordinateSecurity.getDescription())
        .setParentOrganizationNames(parentOrganizations)
        .setParentOrganizationIds(parentOrganizations)
        .build();
  }

  private ComponentIdentifier tryConvert(ThirdPartyFileCoordinate thirdPartyFileCoordinate) {
    PackageUrlIdentifier packageUrlIdentifier = null;

    // First try the pURL, if it exists
    if (thirdPartyFileCoordinate.getPackageUrl() != null) {
      try {
        packageUrlIdentifier =
            new PackageUrlIdentifier(thirdPartyFileCoordinate.getPackageUrl());
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
            .withVersion(thirdPartyFileCoordinate.getVersion()).build()
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

  private String getDescription(IndexingContext indexingContext, SecurityVulnerability vulnerability) {
    if (IdentificationSource.SONATYPE_IAC.getId().equals(vulnerability.getSource())
        || "Sonatype-C".equals(vulnerability.getSource())) {
      return getThirdPartyVulnerabilityDescription(indexingContext, vulnerability);
    }
    try {
      return indexingContext.getVulnerabilityHtml(vulnerability.getRefId());
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
      final IndexingContext indexingContext,
      final SecurityVulnerability vulnerability)
  {
    if (indexingContext.vulnDescByVulnId.get(vulnerability.getRefId()) != null) {
      return indexingContext.vulnDescByVulnId.get(vulnerability.getRefId());
    }

    ThirdPartyVulnerability thirdPartyVulnerability = thirdPartyVulnerabilityDAO.getByRefId(vulnerability.getRefId());
    if (thirdPartyVulnerability == null || thirdPartyVulnerability.getDescription() == null) {
      log.warn("Description not found for vulnerability with refid: {}", vulnerability.getRefId());
      return "";
    }
    else {
      String description = thirdPartyVulnerability.getDescription();
      indexingContext.vulnDescByVulnId.put(vulnerability.getRefId(), description);
      return description;
    }
  }

  /**
   * @return a multimap mapping each organization to all of its ancestor orgs, in order
   */
  private ListMultimap<Organization, Organization> computeParentsByOrganization(
      Map<String, Organization> organizationsById)
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

  class IndexingContext
  {
    final IndexWriter indexWriter;

    private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

    private final Map<String, String> vulnDescByVulnId = new ConcurrentHashMap<>();

    public IndexingContext(IndexWriter indexWriter) {
      this.indexWriter = indexWriter;
    }

    public Query newQuery(FieldIdentifier fieldIdentifer, String fieldValue) {
      return new TermQuery(
          new Term(fieldIdentifer.label, indexWriter.getAnalyzer().normalize(fieldIdentifer.label, fieldValue)));
    }

    public void addOwners(Collection<? extends Owner> owners) {
      owners.forEach(owner -> ownersById.put(owner.getId(), owner));
    }

    public Owner getOwner(String id) {
      return ownersById.computeIfAbsent(id, ownerDAO::getById);
    }

    public String getVulnerabilityHtml(String vulnerabilityId) {
      return vulnDescByVulnId.computeIfAbsent(vulnerabilityId,
          vulnerabilityDescriptionFetcher::getVulnerabilityDescription);
    }
  }

  @Override
  public SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isSbomManagerMode) throws SearchIndexException
  {
    checkMode(isSbomManagerMode);
    boolean initialSearch = false;
    if (page == 0) {
      // when actually paging through the results, a positive page index is used
      // 0 denotes first page of new search
      page = 1;
      initialSearch = true;
    }

    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex(); //
         IndexReader indexReader = DirectoryReader.open(directory)) {
      AuditData.get() //
          .setData("searchQuery", searchQuery) //
          .setData("searchPageSize", pageSize) //
          .setData("searchPageIndex", page - 1);

      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = page;
      searchResultDTO.pageSize = pageSize;

      IndexSearcher indexSearcher = new IndexSearcher(indexReader);

      searchQuery = allComponents ? searchQuery :
          searchQuery + " -" + ITEM_TYPE.label + ":" + NON_VULNERABLE_COMPONENT.name();

      // parentOrganizationName and parentOrganizationId supports searching the hierarchy
      // including the organization itself
      // the replace here has no side affects and allows us to search within the org hierarchy
      searchQuery = searchQuery.replaceAll("organizationName", "parentOrganizationName");
      searchQuery = searchQuery.replaceAll("organizationId", "parentOrganizationId");

      Query query = createQuery(searchQuery);

      Set<String> fieldNames = getFieldNames(query);
      Set<String> invalidFieldNames = new TreeSet<>();

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

      for (String fieldName : fieldNames) {
        if (getFieldIdentifier(fieldName) == null) {
          invalidFieldNames.add(fieldName);
        }
      }
      if (!invalidFieldNames.isEmpty()) {
        throw new BadRequestException("The search query contains invalid field names: " + invalidFieldNames);
      }

      Query finalQuery;
      try {
        Query queryWithSbomFiltering = appendSbomFilteringToQuery(query, isSbomManagerMode);
        finalQuery = appendAllowedApplicationsAndOrganizationsToQuery(queryWithSbomFiltering);
      }
      catch (TooManyClauses e) {
        throw new BadRequestException("Error performing search due to too many clauses. " +
            "Please try narrowing down the query as much as possible " +
            "and consider updating Advanced Search configuration to support larger queries.");
      }

      // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
      TopDocs topDocs = indexSearcher.search(finalQuery, Math.max(1, indexReader.maxDoc()));
      groupDocuments(indexSearcher, topDocs.scoreDocs, page, pageSize, searchResultDTO,
          getGroupFieldNamesByItemType(fieldNames));

      int resultRecordCount = countSearchResults(searchResultDTO);

      searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;
      AuditData.get().setData("resultRecordCount", resultRecordCount);
      return searchResultDTO;
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  // Update the static setting within lucene for the max query clause count, based on the current value in the
  // configuration
  private void updateMaxQueryClauseCount() {
    IndexSearcher.setMaxClauseCount(configuration.getMaxAdvancedSearchClauseCount());
  }

  private int countSearchResults(final SearchResultDTO searchResultDTO) {
    int resultRecordCount = 0;
    for (GroupingByDTO groupingByDTO : searchResultDTO.groupingByDTOS) {
      resultRecordCount += groupingByDTO.searchResultItemDTOS.size();
    }
    return resultRecordCount;
  }

  /**
   * Only group sequential items if possible. This maintains lucene order/ranking ensuring more relevant results appear
   * earlier. This will also typically only iterate over the default pageSize number of documents which helps avoid too
   * much memory usage. See CLM-29232 for more details.
   */
  private void groupDocuments(
      final IndexSearcher indexSearcher,
      final ScoreDoc[] scoreDocs,
      final int page,
      final int pageSize,
      final SearchResultDTO searchResultDTO,
      final Map<String, String> groupFieldNamesByItemType) throws IOException
  {
    int startIndex = (page - 1) * pageSize;
    int endIndex = page * pageSize;
    int resultIndex = startIndex + 1;
    GroupingByDTO lastGroup = null;
    for (int i = startIndex; i < endIndex && i < scoreDocs.length; i++) {
      Document document = indexSearcher.storedFields().document(scoreDocs[i].doc);
      SearchResultItemDTO searchResultItemDTO = toDto(document);
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

  private Directory openSearchIndex() {
    try {
      @SuppressWarnings("resource")
      Directory directory = luceneComponents.openSearchIndex(true);
      if (directory == null || !DirectoryReader.indexExists(directory)) {
        if (directory != null) {
          directory.close();
        }
        throw new ConflictException(NO_INDEX_ERROR_MESSAGE);
      }
      return directory;
    }
    catch (IOException e) {
      throw new ConflictException(NO_INDEX_ERROR_MESSAGE, e);
    }
  }

  private Set<String> getFieldNames(Query query) {
    Set<String> fieldNames = new HashSet<>();
    query.visit(new QueryVisitor()
    {
      @Override
      public boolean acceptField(String field) {
        fieldNames.add(field);
        return false;
      }
    });
    return fieldNames;
  }

  private Map<String, String> getGroupFieldNamesByItemType(Set<String> fieldNames) {
    Map<String, String> groupFieldNamesByItemType = new HashMap<>();
    for (ItemType itemType : ItemType.values()) {
      groupFieldNamesByItemType.put(itemType.name(), getGroupFieldName(itemType, fieldNames).label);
    }
    return groupFieldNamesByItemType;
  }

  private FieldIdentifier getGroupFieldName(ItemType itemType, Set<String> fieldNames) {
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

  private FieldIdentifier getFieldIdentifier(String fieldName) {
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

  private Query createQuery(String searchQuery) {
    if (StringUtils.isBlank(searchQuery)) {
      throw new BadRequestException("The search query is empty");
    }
    return luceneComponents.newQueryParser().apply(searchQuery);
  }

  private SearchResultItemDTO toDto(Document document) {
    SearchResultItemDTO searchResultItemDTO = new SearchResultItemDTO();
    searchResultItemDTO.itemType = document.get(ITEM_TYPE.label);
    searchResultItemDTO.organizationId = document.get(ORGANIZATION_ID.label);
    searchResultItemDTO.organizationName = document.get(ORGANIZATION_NAME.label);
    searchResultItemDTO.applicationId = document.get(APPLICATION_ID.label);
    searchResultItemDTO.applicationPublicId = document.get(APPLICATION_PUBLIC_ID.label);
    searchResultItemDTO.applicationName = document.get(APPLICATION_NAME.label);
    searchResultItemDTO.applicationVersion = document.get(APPLICATION_VERSION.label);
    searchResultItemDTO.sbomSpecification = document.get(SBOM_SPECIFICATION.label);
    searchResultItemDTO.policyEvaluationStage = document.get(POLICY_EVALUATION_STAGE.label);
    if (searchResultItemDTO.policyEvaluationStage != null) {
      searchResultItemDTO.policyEvaluationStage =
          StageTypes.getById(searchResultItemDTO.policyEvaluationStage).getName();
    }
    searchResultItemDTO.reportId = document.get(REPORT_ID.label);
    searchResultItemDTO.componentHash = document.get(COMPONENT_HASH.label);
    String format = document.get(COMPONENT_FORMAT.label);
    if (format != null) {
      ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2 = new ApiComponentIdentifierDTOV2();
      apiComponentIdentifierDTOV2.setFormat(format);
      Map<String, String> coordinates = new TreeMap<>();
      for (String coordinateName : ComponentIdentifier.getAllCoordinateNames(format)) {
        String coordinateValue = document.get(DocumentBuilder.getFieldNameForCoordinate(coordinateName));
        if (coordinateValue != null) {
          coordinates.put(coordinateName, coordinateValue);
        }
      }
      apiComponentIdentifierDTOV2.setCoordinates(coordinates);
      searchResultItemDTO.componentIdentifier = apiComponentIdentifierDTOV2;
    }
    searchResultItemDTO.componentName = document.get(COMPONENT_NAME.label);
    searchResultItemDTO.vulnerabilityId = document.get(VULNERABILITY_ID.label);
    searchResultItemDTO.vulnerabilityDescription = document.get(VULNERABILITY_DESCRIPTION.label);
    searchResultItemDTO.vulnerabilityStatus = document.get(VULNERABILITY_STATUS.label);
    searchResultItemDTO.applicationCategoryId = document.get(APPLICATION_CATEGORY_ID.label);
    searchResultItemDTO.applicationCategoryName = document.get(APPLICATION_CATEGORY_NAME.label);
    searchResultItemDTO.applicationCategoryColor = document.get(APPLICATION_CATEGORY_COLOR.label);
    searchResultItemDTO.applicationCategoryDescription = document.get(APPLICATION_CATEGORY_DESCRIPTION.label);
    searchResultItemDTO.componentLabelId = document.get(COMPONENT_LABEL_ID.label);
    searchResultItemDTO.componentLabelName = document.get(COMPONENT_LABEL_NAME.label);
    searchResultItemDTO.componentLabelColor = document.get(COMPONENT_LABEL_COLOR.label);
    searchResultItemDTO.componentLabelDescription = document.get(COMPONENT_LABEL_DESCRIPTION.label);
    searchResultItemDTO.policyId = document.get(POLICY_ID.label);
    searchResultItemDTO.policyName = document.get(POLICY_NAME.label);
    searchResultItemDTO.policyThreatCategory = document.get(POLICY_THREAT_CATEGORY.label);
    String policyThreatLevel = document.get(POLICY_THREAT_LEVEL.label);
    searchResultItemDTO.policyThreatLevel = policyThreatLevel == null ? null : Integer.valueOf(policyThreatLevel);
    return searchResultItemDTO;
  }

  private Query appendAllowedApplicationsAndOrganizationsToQuery(Query query) {
    Set<String> contextIdsWithReadPermission =
        permissionService.getContextIdsForUserWithPermission(currentUser.getUserPrincipal(), Permission.READ);

    if (contextIdsWithReadPermission.contains(MembershipMapping.GLOBAL_CONTEXT_ID) ||
        contextIdsWithReadPermission.contains(Organization.ROOT_ORGANIZATION_ID)) {
      return query;
    }

    Map<String, OwnerType> contextIdsWithReadPermissionMap = getChildContextIds(contextIdsWithReadPermission);

    Builder allowedContextIdsQueryBuilder = new Builder();

    contextIdsWithReadPermissionMap.forEach((contextId, type) -> {
      if (OwnerType.APPLICATION.equals(type)) {
        allowedContextIdsQueryBuilder.add(new TermQuery(new Term(APPLICATION_ID.label, contextId)), Occur.SHOULD);
      }
      else if (OwnerType.ORGANIZATION.equals(type)) {
        allowedContextIdsQueryBuilder.add(new TermQuery(new Term(ORGANIZATION_ID.label, contextId)), Occur.SHOULD);
      }
    });

    return new Builder()
        .add(allowedContextIdsQueryBuilder.build(), Occur.MUST)
        .add(query, Occur.MUST)
        .build();
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
  private Query appendSbomFilteringToQuery(Query originalQuery, boolean isSbomManagerMode) {
    Query hasAppVersionQuery = new FieldExistsQuery(APPLICATION_VERSION.label);
    Occur shouldAppVersionResultsBeExcluded = isSbomManagerMode ? Occur.MUST_NOT : Occur.MUST;
    Query componentsToExcludeQuery = new Builder()
        .add(new TermQuery(new Term(ITEM_TYPE.label, ItemType.NON_VULNERABLE_COMPONENT.searchFieldName())), Occur.MUST)
        .add(hasAppVersionQuery, shouldAppVersionResultsBeExcluded)
        .build();
    Query vulnerabilitiesToExcludeQuery = new Builder()
        .add(new TermQuery(new Term(ITEM_TYPE.label, ItemType.SECURITY_VULNERABILITY.searchFieldName())), Occur.MUST)
        .add(hasAppVersionQuery, shouldAppVersionResultsBeExcluded)
        .build();

    Builder builder = new Builder();
    builder.add(originalQuery, Occur.MUST);
    // SBOM Manager -> -(NON_VULNERABLE_COMPONENT AND !APPLICATION_VERSION)
    // Default -> -(NON_VULNERABLE_COMPONENT AND APPLICATION_VERSION)
    builder.add(componentsToExcludeQuery, Occur.MUST_NOT);
    // SBOM Manager -> -(SECURITY_VULNERABILITY AND !APPLICATION_VERSION)
    // Default -> -(SECURITY_VULNERABILITY AND APPLICATION_VERSION)
    builder.add(vulnerabilitiesToExcludeQuery, Occur.MUST_NOT);
    if (isSbomManagerMode) {
      // SBOM Manager -> -APPLICATION_CATEGORY, -COMPONENT_LABEL, -POLICY
      builder.add(new TermQuery(new Term(ITEM_TYPE.label, APPLICATION_CATEGORY.searchFieldName())), Occur.MUST_NOT);
      builder.add(new TermQuery(new Term(ITEM_TYPE.label, COMPONENT_LABEL.searchFieldName())), Occur.MUST_NOT);
      builder.add(new TermQuery(new Term(ITEM_TYPE.label, POLICY.searchFieldName())), Occur.MUST_NOT);
    }
    else {
      // Default -> -SBOM_METADATA
      builder.add(new TermQuery(new Term(ITEM_TYPE.label, SBOM_METADATA.searchFieldName())), Occur.MUST_NOT);
    }
    return builder.build();
  }

  private Map<String, OwnerType> getChildContextIds(Set<String> contextIdsWithReadPermission) {
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

  private void checkMode(boolean isSbomManagerMode) {
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
}
