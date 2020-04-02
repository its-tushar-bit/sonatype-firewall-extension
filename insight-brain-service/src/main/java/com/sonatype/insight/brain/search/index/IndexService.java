/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyEvaluationDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
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
import com.sonatype.insight.brain.search.LuceneComponents;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.iterator.FieldIterator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since 1.88
 */
@Named
@Singleton
public class IndexService
{
  static final String SEARCH_INDEX_DURATION_SECONDS = "search_index_duration_seconds";

  static final String SEARCH_INDEX_SIZE_BYTES = "search_index_size_bytes";

  private static final ImmutableSet<String> SUGGESTER_FIELDS_TO_IGNORE = ImmutableSet.of(
          FieldIdentifier.POLICY_THREAT_LEVEL.label,
          FieldIdentifier.VULNERABILITY_SEVERITY.label,
          FieldIdentifier.COMPONENT_LABEL_DESCRIPTION.label, 
          FieldIdentifier.VULNERABILITY_DESCRIPTION.label, 
          FieldIdentifier.APPLICATION_CATEGORY_DESCRIPTION.label);

  private static final Logger log = LoggerFactory.getLogger(IndexService.class);

  private final OrganizationDAO organizationDAO;

  private final ApplicationDAO applicationDAO;

  private final PolicyEvaluationDAO policyEvaluationDAO;

  private final ComponentDAO componentDAO;

  private final TagDAO tagDAO;

  private final LabelDAO labelDAO;

  private final OwnerDAO ownerDAO;

  private final PolicyDAO policyDAO;

  private final InsightWork insightWork;

  private final TelemetrySender telemetrySender;

  private final VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher;

  private final LuceneComponents luceneComponents;

  private volatile boolean running = false;

  class IndexingContext
  {
    final List<Organization> organizations;

    final List<Application> applications;

    private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

    private final Map<String, String> vulnDescByVulnId = new ConcurrentHashMap<>();

    public IndexingContext() {
      organizations = organizationDAO.getAll();
      organizations.forEach(org -> ownersById.put(org.getId(), org));
      applications = applicationDAO.getAll();
      applications.forEach(app -> ownersById.put(app.getId(), app));
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
      ComponentDAO componentDAO,
      TagDAO tagDAO,
      LabelDAO labelDAO,
      OwnerDAO ownerDAO,
      PolicyDAO policyDAO,
      InsightWork insightWork,
      TelemetrySender telemetrySender,
      VulnerabilityDescriptionFetcher vulnerabilityDescriptionFetcher,
      LuceneComponents luceneComponents)
  {
    this.organizationDAO = organizationDAO;
    this.applicationDAO = applicationDAO;
    this.policyEvaluationDAO = policyEvaluationDAO;
    this.componentDAO = componentDAO;
    this.tagDAO = tagDAO;
    this.labelDAO = labelDAO;
    this.ownerDAO = ownerDAO;
    this.policyDAO = policyDAO;
    this.insightWork = insightWork;
    this.telemetrySender = telemetrySender;
    this.vulnerabilityDescriptionFetcher = vulnerabilityDescriptionFetcher;
    this.luceneComponents = luceneComponents;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public synchronized void createSearchIndexAsync() {
    if (!running) {
      running = true;
      new IndexThread().start();
    }
  }

  private class IndexThread
      extends Thread
  {
    IndexThread() {
      super("IndexService-0");
      setDaemon(true);
    }

    @Override
    public void run() {
      try {
        createSearchIndex();
      }
      catch (Exception e) {
        log.error(e.getMessage(), e);
      }
      catch (Throwable t) {
        // Try to log to stderr before trying the standard logging because the standard logging may not be operational
        // at this point.
        t.printStackTrace();
        log.error(t.getMessage(), t);
        System.exit(2);
      }
      finally {
        running = false;
      }
    }
  }

  @VisibleForTesting
  public boolean isRunning() {
    return running;
  }

  public void createSearchIndex() throws IOException {
    log.info("creating search index...");
    long start = System.currentTimeMillis();
    long totalIndexSize = 0;

    Path indexPath = insightWork.getSearchIndexDir().toPath();
    Path suggesterPath = insightWork.getSearchSuggesterDir().toPath();
    Files.createDirectories(indexPath);
    Files.createDirectories(suggesterPath);

    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(luceneComponents.newAnalyzerForSearch());
    indexWriterConfig.setOpenMode(OpenMode.CREATE);
    try (Directory directory = FSDirectory.open(indexPath);
        IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig)) {
      log.info("begin indexing");

      IndexingContext indexingContext = new IndexingContext();

      CompletableFuture<Void> orgDocs =
          CompletableFuture.supplyAsync(() -> buildOrganizationDocs(indexingContext))
              .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      CompletableFuture<Void> appDocs = CompletableFuture.supplyAsync(() -> buildApplicationDocs(indexingContext))
          .thenAccept(docs -> addDocsWithException(indexWriter, docs));

      List<CompletableFuture<Void>> appSVDocs = indexingContext.applications
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
      totalIndexSize += getDirectorySize(directory);
    }

    // write to search-suggester dir
    try (IndexReader sourceIndexReader = DirectoryReader.open(FSDirectory.open(indexPath));
        FSDirectory suggesterFile = FSDirectory.open(suggesterPath);
        AnalyzingInfixSuggester suggester = luceneComponents.newSuggester(suggesterFile)) {
      log.info("started building suggester");
      Function<String, Query> queryParser = luceneComponents.newQueryParser();
      IndexSearcher indexSearcher = new IndexSearcher(sourceIndexReader);
      try (IndexReader indexReader = indexSearcher.getIndexReader()) {
        long maxId = indexReader.maxDoc();
        Set<String> searchKeys = new HashSet<>();
        searchKeys.add(FieldIdentifier.VULNERABILITY_SEVERITY + ":[0 TO 4}");
        searchKeys.add(FieldIdentifier.VULNERABILITY_SEVERITY + ":[4 TO 7}");
        searchKeys.add(FieldIdentifier.VULNERABILITY_SEVERITY + ":[7 TO 9}");
        searchKeys.add(FieldIdentifier.VULNERABILITY_SEVERITY + ":[9 TO 10]");
        searchKeys.add(FieldIdentifier.POLICY_THREAT_LEVEL + ":0");
        searchKeys.add(FieldIdentifier.POLICY_THREAT_LEVEL + ":1");
        searchKeys.add(FieldIdentifier.POLICY_THREAT_LEVEL + ":[2 TO 3]");
        searchKeys.add(FieldIdentifier.POLICY_THREAT_LEVEL + ":[4 TO 7]");
        searchKeys.add(FieldIdentifier.POLICY_THREAT_LEVEL + ":[8 TO 10]");
        for (int i = 0; i < maxId; i++) {
          Document doc = indexSearcher.doc(i);
          searchKeys.addAll(getDocFieldValues(doc, queryParser));
        }
        suggester.build(new FieldIterator(searchKeys.iterator()));
        suggester.commit();
        log.info("completed building suggester");
      }
      totalIndexSize += getDirectorySize(suggesterFile);
    }

    TelemetryData telemetryData = new TelemetryData(TelemetryPurpose.ADVANCED_SEARCH_INDEXING);
    telemetryData.put(SEARCH_INDEX_DURATION_SECONDS, (System.currentTimeMillis() - start) / 1000);
    telemetryData.put(SEARCH_INDEX_SIZE_BYTES, totalIndexSize);
    telemetrySender.send(telemetryData);

    log.info("index creation exit");
  }

  private long getDirectorySize(Directory directory) throws IOException {
    long bytes = 0;
    for (String filename : directory.listAll()) {
      bytes += directory.fileLength(filename);
    }
    return bytes;
  }

  private Set<String> getDocFieldValues(Document doc, Function<String, Query> queryParser) {
    Set<String> docFieldValues = new HashSet<>();

    for (IndexableField field : doc) {
      String suggestion = toSuggestion(field, queryParser);
      if (suggestion != null) {
        docFieldValues.add(suggestion);
      }
    }

    return docFieldValues;
  }

  private String toSuggestion(IndexableField field, Function<String, Query> queryParser) {
    String fieldName = field.name();
    if (SUGGESTER_FIELDS_TO_IGNORE.contains(fieldName)) {
      return null;
    }
    String fieldValue = field.stringValue();
    if (fieldValue == null) {
      return null;
    }
    if (field.numericValue() == null && isQuotingRequired(fieldName, fieldValue, queryParser)) {
      fieldValue = quoteValue(fieldValue);
    }
    return fieldName + ':' + fieldValue;
  }

  private boolean isQuotingRequired(String fieldName, String fieldValue, Function<String, Query> queryParser) {
    try {
      return !(queryParser.apply(fieldName + ':' + fieldValue) instanceof TermQuery);
    }
    catch (Exception e) {
      return true;
    }
  }

  private static String quoteValue(String fieldValue) {
    return '"' + fieldValue.replace("\\", "\\\\").replace("\"", "\\\"") + '"';
  }

  private static void addDocsWithException(IndexWriter writer, List<Document> docs) {
    try {
      writer.addDocuments(docs);
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
  }

  private List<Document> buildOrganizationDocs(IndexingContext indexingContext) {
    return indexingContext.organizations.stream().map(org -> {
      return buildDocument(indexingContext, org);
    }).collect(toList());
  }

  Document buildDocument(IndexingContext indexingContext, Organization organization) {
    return new DocumentBuilder(ItemType.ORGANIZATION) //
        .setOwner(organization) //
        .build();
  }

  private List<Document> buildApplicationDocs(IndexingContext indexingContext) {
    return indexingContext.applications.stream().map(app -> {
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
      ReportEntry bomReportEntry = Report.getEntry(reportFile, "bom.json");
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null) {
        return Collections.emptyList();
      }

      return componentDAO.getAll(application, licenseReportEntry.buf, securityReportEntry.buf, bomReportEntry.buf)
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
        .setPolicyEvaluationStage(stageType.getName()) //
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
