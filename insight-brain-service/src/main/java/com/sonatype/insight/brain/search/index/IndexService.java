/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import com.sonatype.insight.IdentificationSource;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentLoaderFactory;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyVulnerabilityDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyVulnerability;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.LuceneComponents;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightJob;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.brain.tenancy.AllTenantsJob;
import com.sonatype.insight.brain.tenancy.Tenant;
import com.sonatype.insight.brain.tenancy.TenantAwareFunction;
import com.sonatype.insight.brain.tenancy.TenantAwareSupplier;
import com.sonatype.insight.brain.utils.ExecutorThreadPools;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import datadog.trace.api.DDTags;
import io.opentracing.Span;
import io.opentracing.util.GlobalTracer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.88
 */
@Named
@Singleton
@DisallowConcurrentExecution
public class IndexService
    implements InsightJob, AllTenantsJob
{
  static final String TASK_NAME = "SearchIndexUpdate";

  static final String SEARCH_INDEX_DURATION_SECONDS = "search_index_duration_seconds";

  public static final String SEARCH_INDEX_SIZE_BYTES = "search_index_size_bytes";

  public static final String SEARCH_INDEX_REINDEX = "search_index_reindex";

  private static final int INDEX_THREADS_MIN = 1;

  private static final int INDEX_THREADS_MAX = 7;

  private static final int INDEX_THREADS_DEFAULT = 1;

  public static final String SEARCH_INDEX_CONFIG_PROPS = "AdvancedSearch.createSearchIndex";

  private static final Logger log = LoggerFactory.getLogger(IndexService.class);

  private static final int QUEUE_POP_AMOUNT = 64_000;

  private static final int JOB_EXECUTION_INTERVAL_IN_SECONDS = 3;

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final TagDAO tagDAO;

  private final LabelDAO labelDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final SearchIndexChangeDAO searchIndexChangeDAO;

  private final InsightWork insightWork;

  private final TelemetrySender telemetrySender;

  private final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  private final TaskScheduler taskScheduler;

  private final LuceneComponents luceneComponents;

  public boolean disableForTesting;

  private final ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO;

  private final ForkJoinPool searchIndexPool;

  private final ComponentLoaderFactory componentLoaderFactory;

  private final Provider<IndexCreationScheduler> indexCreationScheduler;

  @Override
  public String getJobName() {
    return TASK_NAME;
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

  @Inject
  public IndexService(
      OrganizationDAO organizationDAO,
      ApplicationDAO applicationDAO,
      PolicyEvaluationDAO policyEvaluationDAO,
      TagDAO tagDAO,
      LabelDAO labelDAO,
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      SearchIndexChangeDAO searchIndexChangeDAO,
      InsightWork insightWork,
      TelemetrySender telemetrySender,
      VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher,
      TaskScheduler taskScheduler,
      LuceneComponents luceneComponents,
      ThirdPartyVulnerabilityDAO thirdPartyVulnerabilityDAO,
      ComponentLoaderFactory componentLoaderFactory,
      Provider<IndexCreationScheduler> indexCreationScheduler)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.tagDAO = tagDAO;
    this.labelDAO = labelDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.searchIndexChangeDAO = searchIndexChangeDAO;
    this.insightWork = insightWork;
    this.telemetrySender = telemetrySender;
    this.vulnerabilityDescriptionFetcher = vulnerabilityDescriptionFetcher;
    this.taskScheduler = taskScheduler;
    this.luceneComponents = luceneComponents;
    this.thirdPartyVulnerabilityDAO = thirdPartyVulnerabilityDAO;
    this.componentLoaderFactory = componentLoaderFactory;
    this.indexCreationScheduler = indexCreationScheduler;

    searchIndexPool = ExecutorThreadPools.getInstance()
        .createThreadPool(INDEX_THREADS_MIN, INDEX_THREADS_MAX, INDEX_THREADS_DEFAULT,
            SEARCH_INDEX_CONFIG_PROPS);
  }

  @Override
  public void register() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(this, Duration.ofSeconds(JOB_EXECUTION_INTERVAL_IN_SECONDS));
  }

  @Override
  public void deregister() {
    // noop
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void createSearchIndexAsync() {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    taskScheduler.scheduleOneTimeTask(indexCreationScheduler.get());
  }

  @Override
  public void executeForTenant(JobExecutionContext context, Tenant tenant) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      updateIndex();
      updateDatadogResourceName();
    }
    catch (Exception e) {
      log.error("Failed to update search index: {}", e.getMessage(), e);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational
      // at this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  /**
   * This class ends up being proxied by Guice and has hash appended to its name which ruins Datadog traces. This code
   * will alter the name to match the expected pattern. See CLM-25207.
   */
  private void updateDatadogResourceName() {
    final Span span = GlobalTracer.get().activeSpan();
    if (span != null) {
      span.setTag(DDTags.RESOURCE_NAME, "class com.sonatype.insight.brain.search.index.IndexService");
    }
  }

  public boolean isFullIndexTriggered() {
    return taskScheduler.isJobTriggered(indexCreationScheduler.get(), Collections.emptyMap());
  }

  public void createSearchIndex() throws IOException {
    log.info("creating search index...");
    long start = System.currentTimeMillis();

    try (Directory directory = luceneComponents.openSearchIndex(false);
         IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE)) {
      log.info("begin indexing");

      List<Organization> organizations = organizationDAO.getAll();
      Map<String, Organization> organizationById =
          organizations.stream().collect(Collectors.toMap(Organization::getId, item -> item));
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

      TenantAwareFunction<Application, CompletableFuture<Void>> function =
          new TenantAwareFunction<>(application -> CompletableFuture
              .supplyAsync(new TenantAwareSupplier<>(
                  () -> buildApplicationSVDocs(indexingContext, organizationById.get(application.getOrganizationId()),
                      application)), searchIndexPool)
              .thenAccept(docs -> addDocsWithException(indexWriter, docs)));
      List<CompletableFuture<Void>> appSVDocs = applications
          .parallelStream()
          .map(function)
          .collect(toList());

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
      indexWriter.commit();
      log.info("all indexing complete");
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    telemetryData.put(SEARCH_INDEX_DURATION_SECONDS, (System.currentTimeMillis() - start) / 1000);
    telemetryData.put(SEARCH_INDEX_SIZE_BYTES, getIndexSize());
    telemetryData.put(SEARCH_INDEX_REINDEX, true);
    telemetrySender.send(telemetryData);

    log.info("index creation exit");
  }

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

  public void updateIndex() throws IOException {
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
    log.debug("Updated search index");
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

  private static void addDocsWithException(IndexWriter writer, List<Document> docs) {
    try {
      writer.addDocuments(docs);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  private List<Document> buildOrganizationDocs(
      IndexingContext indexingContext,
      Collection<Organization> organizations)
  {
    return organizations.stream().map(org -> buildDocument(indexingContext, org)).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Organization organization) {
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

  private List<Document> buildApplicationSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application)
  {
    List<Organization> parentOrganizations = new ArrayList<>();
    ownerDAO.walkHierarchy(organization).forEach(o -> parentOrganizations.add((Organization) o));

    return StageTypes.getAll().parallelStream()
        .map(new TenantAwareFunction<StageType, List<Document>>(
            stageType -> buildApplicationStageSVDocs(indexingContext, organization, application, stageType,
                parentOrganizations)))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildApplicationStageSVDocs(
      IndexingContext indexingContext,
      Organization organization,
      Application application,
      StageType stageType,
      List<Organization> parentOrganizations)
  {
    try {
      PolicyEvaluation latestPolicyEvaluation =
          policyEvaluationDAO.getLastByApplicationIdAndStageId(application.getId(), stageType.getId());
      if (latestPolicyEvaluation == null) {
        return Collections.emptyList();
      }
      String scanId = latestPolicyEvaluation.getScanId();
      File reportFile = insightWork.getReportFile(application.getId(), scanId);
      if (!reportFile.exists()) {
        return Collections.emptyList();
      }
      ReportEntry licenseReportEntry = Report.getEntry(reportFile, Report.LICENSES_JSON_FILENAME);
      ReportEntry securityReportEntry = Report.getEntry(reportFile, Report.SECURITY_JSON_FILENAME);
      ReportEntry bomReportEntry = Report.getEntry(reportFile, Report.BOM_JSON_FILENAME);
      ReportEntry dependenciesReportEntry = Report.getEntry(reportFile, Report.DEPENDENCIES_JSON_FILENAME);
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null ||
          dependenciesReportEntry == null) {
        return Collections.emptyList();
      }

      return componentLoaderFactory.createComponentLoader(application)
          .getAll(licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf,
              dependenciesReportEntry.buf)
          .parallelStream()
          .map(new TenantAwareFunction<Component, List<Document>>(
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
    return Collections.emptyList();
  }

  private List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Organization organization,
      List<Organization> parentOrganizations,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    if (CollectionUtils.isNotEmpty(component.getSecurityVulnerabilities())) {
      return component.getSecurityVulnerabilities().parallelStream()
          .map(new TenantAwareFunction<SecurityVulnerability, Document>(
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
      List<Organization> parentOrganizations,
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
      List<Organization> parentOrganizations)
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
}
