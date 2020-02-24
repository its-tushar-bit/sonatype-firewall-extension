/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

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
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.iterator.FieldIterator;
import com.sonatype.insight.brain.security.Authorize;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.stream.Collectors.toList;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Singleton
public class IndexService
{
  private static final ImmutableSet<String> SUGGESTER_FIELDS_TO_IGNORE = ImmutableSet.of(
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

  private final Analyzer analyzer;

  class IndexingContext
  {
    final List<Organization> organizations;

    final List<Application> applications;

    private final Map<String, Owner> ownersById = new ConcurrentHashMap<>();

    private final Function<String, String> vulnIdToHtml;

    private final Map<String, String> htmlByVulnId = new ConcurrentHashMap<>();

    public IndexingContext(Function<String, String> vulnIdToHtml) {
      organizations = organizationDAO.getAll();
      organizations.forEach(org -> ownersById.put(org.getId(), org));
      applications = applicationDAO.getAll();
      applications.forEach(app -> ownersById.put(app.getId(), app));
      this.vulnIdToHtml = vulnIdToHtml;
    }

    public Owner getOwner(String id) {
      return ownersById.computeIfAbsent(id, ownerDAO::getById);
    }

    public String getVulnerabilityHtml(String vulnerabilityId) {
      return htmlByVulnId.computeIfAbsent(vulnerabilityId, vulnIdToHtml);
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
      Analyzer analyzer)
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
    this.analyzer = analyzer;
  }

  @Authorize(permission = Permission.CONFIGURE_SYSTEM)
  public void createSearchIndex(Function<String, String> refIdToHtml) throws IOException  {
    log.info("creating search index...");
    try {
      Path indexPath = insightWork.getSearchIndexDir().toPath();
      Path suggesterPath = insightWork.getSearchSuggesterDir().toPath();
      Files.createDirectories(indexPath);
      Files.createDirectories(suggesterPath);

      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      indexWriterConfig.setOpenMode(OpenMode.CREATE);
      try (Directory directory = FSDirectory.open(indexPath);
          IndexWriter indexWriter = new IndexWriter(directory, indexWriterConfig)) {
        log.info("begin indexing");

        IndexingContext indexingContext = new IndexingContext(refIdToHtml);

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
        log.info("all indexing complete");
      }

      // write to search-suggester dir
      try (IndexReader sourceIndexReader = DirectoryReader.open(FSDirectory.open(indexPath));
           FSDirectory suggesterFile = FSDirectory.open(suggesterPath);
           AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(suggesterFile, new SimpleAnalyzer())) {
        log.info("started building suggester");
        IndexSearcher indexSearcher = new IndexSearcher(sourceIndexReader);
        try (IndexReader indexReader = indexSearcher.getIndexReader()) {
          long maxId = indexReader.maxDoc();
          Set<String> searchKeys = new HashSet<>();
          for (int i = 0; i < maxId; i++) {
            Document doc = indexSearcher.doc(i);
            searchKeys.addAll(getDocFieldValues(doc));
          }
          suggester.build(new FieldIterator(searchKeys.iterator()));
          suggester.commit();
          log.info("completed building suggester");
        }
      }
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      throw e;
    }
    log.info("index creation exit");
  }

  private Set<String> getDocFieldValues(Document doc) {
    Set<String> docFieldValues = new HashSet<>();

    for (String identifier : FieldIdentifier.labelIdentifiers()) {
      if (!SUGGESTER_FIELDS_TO_IGNORE.contains(identifier)) {
        String value = doc.get(identifier);
        if (value != null) {
          docFieldValues.add(identifier + ":" + value);
        }
      }
    }

    return docFieldValues;
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
      return new DocumentBuilder(ItemType.ORGANIZATION) //
          .setOwner(org) //
          .build();
    }).collect(toList());
  }

  private List<Document> buildApplicationDocs(IndexingContext indexingContext) {
    return indexingContext.applications.stream().map(app -> {
      return new DocumentBuilder(ItemType.APPLICATION) //
          .setOwner(app) //
          .setOwner(indexingContext.getOwner(app.getOrganizationId())) //
          .build();
    }).collect(toList());
  }

  private List<Document> buildTagDocs(IndexingContext indexingContext) {
    return tagDAO.getAll().stream().map(tag -> {
      return new DocumentBuilder(ItemType.APPLICATION_CATEGORY) //
          .setApplicationCategoryId(tag.getId()) //
          .setApplicationCategoryName(tag.getName()) //
          .setApplicationCategoryColor(tag.getColor().toValue()) //
          .setApplicationCategoryDescription(tag.getDescription()) //
          .setOwner(indexingContext.getOwner(tag.getOrganizationId())) //
          .build();
    }).collect(toList());
  }

  private List<Document> buildLabelDocs(IndexingContext indexingContext) {
    return labelDAO.getAll().stream().map(label -> {
      return new DocumentBuilder(ItemType.COMPONENT_LABEL) //
          .setComponentLabelId(label.getId()) //
          .setComponentLabelName(label.getLabel()) //
          .setComponentLabelColor(label.getColor().toValue()) //
          .setComponentLabelDescription(label.getDescription()) //
          .setOwner(indexingContext.getOwner(label.getOwnerId())) //
          .build();
    }).collect(toList());
  }

  private List<Document> buildPolicyDocs(IndexingContext indexingContext) {
    return policyDAO.getAll().stream().map(policy -> {
      return new DocumentBuilder(ItemType.POLICY) //
          .setPolicyId(policy.getId()) //
          .setPolicyName(policy.getName()) //
          .setPolicyThreatCategory(policy.getThreatCategory().getName()) //
          .setPolicyThreatLevel(policy.getThreatLevel()) //
          .setOwner(indexingContext.getOwner(policy.getOwnerId())) //
          .build();
    }).collect(toList());
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
      Path reportCachePath = Report.getCacheDir(insightWork.getReportFile(application.getId(), scanId)).toPath();
      if (!Files.exists(reportCachePath)) {
        return Collections.emptyList();
      }
      byte[] licenseReportEntry = Files.readAllBytes(reportCachePath.resolve("licenses.json"));
      byte[] securityReportEntry = Files.readAllBytes(reportCachePath.resolve("security.json"));
      byte[] bomReportEntry = Files.readAllBytes(reportCachePath.resolve("bom.json"));
      if (licenseReportEntry == null || securityReportEntry == null || bomReportEntry == null) {
        return Collections.emptyList();
      }

      return componentDAO.getAll(application, licenseReportEntry, securityReportEntry, bomReportEntry).parallelStream()
          .map(component -> buildApplicationComponentSecurityVulnerabilities(
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

  private List<Document> buildApplicationComponentSecurityVulnerabilities(
      IndexingContext indexingContext,
      Application application,
      StageType stageType,
      String scanId,
      Component component)
  {
    return component.getSecurityVulnerabilities().parallelStream().map(securityVulnerability -> {
      return new DocumentBuilder(ItemType.SECURITY_VULNERABILITY) //
          .setOwner(application) //
          .setPolicyEvaluationStage(stageType.getName()) //
          .setReportId(scanId) //
          .setComponentHash(component.getHash()) //
          .setComponentFormat(component.getComponentIdentifier().getFormat()) //
          .setComponentCoordinates(component) //
          .setComponentName(component.getDisplayName()) //
          .setVulnerabilityId(securityVulnerability.getRefId()) //
          .setVulnerabilityStatus(securityVulnerability.getStatus().getName()) //
          .setVulnerabilityDescription(getDescription(indexingContext, securityVulnerability.getRefId())) //
          .build();
    }).collect(toList());
  }

  private String getDescription(
      IndexingContext indexingContext,
      String refId)
  {
    try {
      String html = indexingContext.getVulnerabilityHtml(refId);
      if (StringUtils.isBlank(html)) {
        return "";
      }
      org.jsoup.nodes.Document doc = Jsoup.parse(html);
      Elements elements = doc.select("dt:contains(description)");
      if (elements.isEmpty()) {
        elements = doc.select("dt:contains(explanation)");
      }
      if (!elements.isEmpty()) {
        elements = elements.first().siblingElements();
      }
      if (!elements.isEmpty()) {
        return elements.first().text();
      }
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
