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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.component.SecurityVulnerability;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.report.ReportEntry;
import com.sonatype.insight.brain.scheduler.TaskScheduler;
import com.sonatype.insight.brain.search.LuceneComponents;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.security.MDCUsernameScope;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import io.dropwizard.lifecycle.Managed;
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
import org.quartz.Job;
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
    implements Managed, Job
{
  static final String TASK_NAME = "SearchIndexUpdate";

  static final String TASK_PARAM_INDEX_ALL = "indexAll";

  static final String SEARCH_INDEX_DURATION_SECONDS = "search_index_duration_seconds";

  public static final String SEARCH_INDEX_SIZE_BYTES = "search_index_size_bytes";

  public static final String SEARCH_INDEX_REINDEX = "search_index_reindex";

  private static final Logger log = LoggerFactory.getLogger(IndexService.class);

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

  private volatile boolean fullIndexRunning;

  public boolean disableForTesting;

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
      LuceneComponents luceneComponents)
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
  }

  @Override
  public void start() {
    if (disableForTesting) {
      return;
    }
    taskScheduler.schedulePeriodicTask(IndexService.class, TASK_NAME, Duration.ofSeconds(3));
  }

  @Override
  public void stop() {
    // noop
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void createSearchIndexAsync() {
    fullIndexRunning = true;
    taskScheduler.triggerTaskNow(TASK_NAME, Collections.singletonMap(TASK_PARAM_INDEX_ALL, "true"));
  }

  @Override
  public void execute(JobExecutionContext context) {
    try (MDCUsernameScope mdcUsernameScope = MDCUsernameScope.forSystem()) {
      if (context.getMergedJobDataMap().containsKey(TASK_PARAM_INDEX_ALL)) {
        try {
          createSearchIndex();
        }
        finally {
          fullIndexRunning = false;
        }
      }
      else {
        updateIndex();
      }
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

  @VisibleForTesting
  public boolean isFullIndexRunning() {
    return fullIndexRunning;
  }

  public void createSearchIndex() throws IOException {
    log.info("creating search index...");
    long start = System.currentTimeMillis();

    try (Directory directory = luceneComponents.openSearchIndex(false);
        IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE)) {
      log.info("begin indexing");

      List<Organization> organizations = organizationDAO.getAll();
      List<Application> applications = applicationDAO.getAll();

      IndexingContext indexingContext = new IndexingContext(indexWriter);
      indexingContext.addOwners(organizations);
      indexingContext.addOwners(applications);

      CompletableFuture<Void> orgDocs =
          CompletableFuture.supplyAsync(() -> buildOrganizationDocs(indexingContext, organizations))
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> appDocs =
          CompletableFuture.supplyAsync(() -> buildApplicationDocs(indexingContext, applications))
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      List<CompletableFuture<Void>> appSVDocs = applications
          .parallelStream()
          .map(application -> CompletableFuture
              .supplyAsync(() -> buildApplicationSVDocs(indexingContext, application))
              .thenAccept(docs -> addDocsWithException(indexWriter, docs))).collect(toList());

      CompletableFuture<Void> tagDocs = CompletableFuture.supplyAsync(() -> buildTagDocs(indexingContext))
          .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> labelDocs = CompletableFuture.supplyAsync(() -> buildLabelDocs(indexingContext))
          .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> policyDocs = CompletableFuture.supplyAsync(() -> buildPolicyDocs(indexingContext))
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
    List<SearchIndexChange> changes = searchIndexChangeDAO.getAll();
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
      case LAST_POLICY_EVALUATION:
        String[] ids = change.getChangeData().split(":");
        updateIndexForPolicyEvaluation(ids[0], ids[1], indexingContext);
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
    StageType stageType = StageTypes.getById(stageTypeId);
    addDocsWithException(indexingContext.indexWriter,
        buildApplicationStageSVDocs(indexingContext, application, stageType));
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
    return organizations.stream().map(org -> {
      return buildDocument(indexingContext, org);
    }).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Organization organization) {
    return new DocumentBuilder(ItemType.ORGANIZATION) //
        .setOwner(organization) //
        .build();
  }

  private List<Document> buildApplicationDocs(IndexingContext indexingContext, Collection<Application> applications) {
    return applications.stream().map(app -> {
      return buildDocument(indexingContext, app);
    }).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Application application) {
    return new DocumentBuilder(ItemType.APPLICATION) //
        .setOwner(application) //
        .setOwner(indexingContext.getOwner(application.getOrganizationId())) //
        .build();
  }

  private List<Document> buildTagDocs(IndexingContext indexingContext) {
    return tagDAO.getAll().stream().map(tag -> {
      return buildDocument(indexingContext, tag);
    }).collect(toList());
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
    return labelDAO.getAll().stream().map(label -> {
      return buildDocument(indexingContext, label);
    }).collect(toList());
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
    return policyDAO.getAll().stream().map(policy -> {
      return buildDocument(indexingContext, policy);
    }).collect(toList());
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
      Application application)
  {
    return StageTypes.getAll().parallelStream()
        .map(stageType -> buildApplicationStageSVDocs(indexingContext, application, stageType))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildApplicationStageSVDocs(
      IndexingContext indexingContext,
      Application application,
      StageType stageType)
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

      return new ComponentDAO(application).getAll(licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf,
          dependenciesReportEntry.buf)
          .parallelStream().map(component -> buildApplicationComponentVulnerabilityDocuments(
              indexingContext,
              application,
              stageType,
              scanId,
              component)).flatMap(Collection::stream).collect(toList());
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return Collections.emptyList();
  }

  private List<Document> buildApplicationComponentVulnerabilityDocuments(
      IndexingContext indexingContext,
      Application application,
      StageType stageType,
      String reportId,
      Component component)
  {
    return component.getSecurityVulnerabilities().parallelStream().map(vulnerability -> {
      return buildDocument(indexingContext, application, stageType, reportId, component, vulnerability);
    }).collect(toList());
  }

  Document buildDocument(
      IndexingContext indexingContext,
      Application application,
      StageType stageType,
      String reportId,
      Component component,
      SecurityVulnerability vulnerability)
  {
    return new DocumentBuilder(ItemType.SECURITY_VULNERABILITY) //
        .setOwner(application) //
        .setPolicyEvaluationStage(stageType) //
        .setReportId(reportId) //
        .setComponentHash(component.getHash()) //
        .setComponentFormat(component.getComponentIdentifier().getFormat()) //
        .setComponentCoordinates(component) //
        .setComponentName(component.getDisplayNameFromIdentifier()) //
        .setVulnerabilityId(vulnerability.getRefId()) //
        .setVulnerabilitySeverity(vulnerability.getSeverity()) //
        .setVulnerabilityStatus(vulnerability.getStatus().getName()) //
        .setVulnerabilityDescription(getDescription(indexingContext, vulnerability.getRefId())) //
        .build();
  }

  private String getDescription(IndexingContext indexingContext, String refId) {
    try {
      return indexingContext.getVulnerabilityHtml(refId);
    }
    catch (NotFoundException notFoundException) {
      log.warn(notFoundException.getMessage());
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return "";
  }
}
