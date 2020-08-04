/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

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
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.telemetry.model.TelemetryData;
import com.sonatype.insight.telemetry.model.TelemetryPurpose;

import com.google.common.annotations.VisibleForTesting;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.Directory;
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

    IndexWriterConfig indexWriterConfig = new IndexWriterConfig(luceneComponents.newAnalyzerForSearch());
    indexWriterConfig.setOpenMode(OpenMode.CREATE);
    try (Directory directory = luceneComponents.openSearchIndex(false);
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
