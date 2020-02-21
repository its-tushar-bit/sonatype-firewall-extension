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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.report.Report;
import com.sonatype.insight.brain.search.LowerCaseAnalyzer;
import com.sonatype.insight.brain.search.docs.DocumentFields;
import com.sonatype.insight.brain.search.docs.DocumentFields.ItemType;
import com.sonatype.insight.brain.search.docs.DocumentFields.FieldIdentifier;
import com.sonatype.insight.brain.search.iterator.FieldIterator;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.error.exception.NotFoundException;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
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

import static com.sonatype.insight.brain.search.docs.DocumentFields.FieldIdentifier.APPLICATION_NAME;
import static com.sonatype.insight.brain.search.docs.DocumentFields.FieldIdentifier.VULNERABILITY_DESCRIPTION;
import static com.sonatype.insight.brain.search.docs.DocumentFields.FieldIdentifier.ORGANIZATION_NAME;
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

  private final Analyzer standardAnalyzer = new StandardAnalyzer();

  private final Map<String, Analyzer>fieldsWithAnalyzers = new HashMap<String, Analyzer>()
  {
    {
      put(VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
      put(APPLICATION_NAME.label, standardAnalyzer);
      put(ORGANIZATION_NAME.label, standardAnalyzer);
    }
  };

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
      InsightWork insightWork)
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
  }

  public void createSearchIndex(Function<String, String> refIdToHtml) throws IOException  {
    log.info("creating search index...");
    ConcurrentMap<String, String> refIdToHtmlStore = new ConcurrentHashMap<String, String>()
    {
      @Override
      public String get(Object key) {
        return computeIfAbsent((String) key, refIdToHtml);
      }
    };

    IndexWriter indexWriter = null;
    try {
      Path indexPath = insightWork.getSearchIndexDir().toPath();
      Path suggesterPath = insightWork.getSearchSuggesterDir().toPath();
      Files.createDirectories(indexPath);
      Files.createDirectories(suggesterPath);

      Directory directory = FSDirectory.open(indexPath);
      Analyzer analyzer = new PerFieldAnalyzerWrapper(new LowerCaseAnalyzer(), fieldsWithAnalyzers);

      IndexWriterConfig indexWriterConfig = new IndexWriterConfig(analyzer);
      indexWriterConfig.setOpenMode(OpenMode.CREATE);
      indexWriter = new IndexWriter(directory, indexWriterConfig);
      final IndexWriter finalWriter = indexWriter;
      log.info("begin indexing");

      CompletableFuture<Void> orgDocs =
          CompletableFuture.supplyAsync(() -> buildOrganizationDocs(organizationDAO.getAll()))
              .thenAccept(docs -> addDocsWithException(finalWriter, docs));

      List<Application> applications = applicationDAO.getAll();

      CompletableFuture<Void> appDocs = CompletableFuture.supplyAsync(() -> buildApplicationDocs(applications))
          .thenAccept(docs -> addDocsWithException(finalWriter, docs));

      List<CompletableFuture<Void>> appSVDocs = applications
          .parallelStream()
          .map(application -> CompletableFuture
              .supplyAsync(() -> buildApplicationSVDocs(application, refIdToHtmlStore))
              .thenAccept(docs -> addDocsWithException(finalWriter, docs))).collect(toList());

      CompletableFuture<Void> tagDocs = CompletableFuture.supplyAsync(() -> buildTagDocs(tagDAO.getAll()))
          .thenAccept(docs -> addDocsWithException(finalWriter, docs));

      CompletableFuture<Void> labelDocs = CompletableFuture.supplyAsync(() -> buildLabelDocs(labelDAO.getAll()))
          .thenAccept(docs -> addDocsWithException(finalWriter, docs));

      CompletableFuture<Void> policyDocs = CompletableFuture.supplyAsync(() -> buildPolicyDocs(policyDAO.getAll()))
          .thenAccept(docs -> addDocsWithException(finalWriter, docs));

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
      indexWriter.close();

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
      catch (Exception e) {
        log.error(e.getMessage(), e);
        throw e;
      }
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
      throw e;
    }
    finally {
      if (indexWriter != null) {
        indexWriter.close();
      }
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

  private List<Document> buildOrganizationDocs(List<Organization> organizations) {
    return organizations.stream().map(org -> {
      DocumentFields documentFields = new DocumentFields(ItemType.ORGANIZATION);
      documentFields.setOrganizationId(org.getId());
      documentFields.setOrganizationName(org.getName());
      return documentFields.build();
    }).collect(toList());
  }

  private List<Document> buildApplicationDocs(List<Application> applications) {
    return applications.stream().map(app -> {
      DocumentFields documentFields = new DocumentFields(ItemType.APPLICATION);
      documentFields.setApplicationId(app.getId());
      documentFields.setApplicationPublicId(app.getPublicId());
      documentFields.setApplicationName(app.getName());
      String organizationId = app.getOrganizationId();
      documentFields.setOrganizationId(organizationId);
      documentFields.setOrganizationName(organizationDAO.getById(organizationId).getName());
      return documentFields.build();
    }).collect(toList());
  }

  private List<Document> buildTagDocs(List<Tag> tags) {
    return tags.stream().map(tag -> {
      DocumentFields documentFields = new DocumentFields(ItemType.APPLICATION_CATEGORY);
      documentFields.setApplicationCategoryId(tag.getId());
      documentFields.setApplicationCategoryName(tag.getName());
      documentFields.setApplicationCategoryColor(tag.getColor().toValue());
      documentFields.setApplicationCategoryDescription(tag.getDescription());
      documentFields.setOrganizationId(tag.getOrganizationId());
      documentFields.setOrganizationName(organizationDAO.getById(tag.getOrganizationId()).getName());
      return documentFields.build();
    }).collect(toList());
  }

  private List<Document> buildLabelDocs(List<Label> labels) {
    return labels.stream().map(label -> {
      DocumentFields documentFields = new DocumentFields(ItemType.COMPONENT_LABEL);
      documentFields.setComponentLabelId(label.getId());
      documentFields.setComponentLabelName(label.getLabel());
      documentFields.setComponentLabelColor(label.getColor().toValue());
      documentFields.setComponentLabelDescription(label.getDescription());
      setOwner(documentFields, ownerDAO.getById(label.getOwnerId()));
      return documentFields.build();
    }).collect(toList());
  }

  private List<Document> buildPolicyDocs(List<Policy> policies) {
    return policies.stream().map(policy -> {
      DocumentFields documentFields = new DocumentFields(ItemType.POLICY);
      documentFields.setPolicyId(policy.getId());
      documentFields.setPolicyName(policy.getName());
      documentFields.setPolicyThreatCategory(policy.getThreatCategory().getName());
      documentFields.setPolicyThreatLevel(policy.getThreatLevel());
      setOwner(documentFields, ownerDAO.getById(policy.getOwnerId()));
      return documentFields.build();
    }).collect(toList());
  }

  private void setOwner(DocumentFields documentFields, Owner owner) {
    if (owner.getType() == OwnerType.ORGANIZATION) {
      documentFields.setOrganizationId(owner.getId());
      documentFields.setOrganizationName(owner.getName());
    }
    else if (owner.getType() == OwnerType.APPLICATION) {
      documentFields.setApplicationId(owner.getId());
      documentFields.setApplicationPublicId(owner.getPublicId());
      documentFields.setApplicationName(owner.getName());
    }
  }

  private List<Document> buildApplicationSVDocs(
      Application application,
      Map<String, String> refIdToHtmlStore)
  {
    return StageTypes.getAll().parallelStream()
        .map(stageType -> buildApplicationStageSVDocs(application, stageType, refIdToHtmlStore))
        .flatMap(Collection::stream).collect(toList());
  }

  private List<Document> buildApplicationStageSVDocs(
      Application application,
      StageType stageType,
      Map<String, String> refIdToHtmlStore)
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
              application,
              stageType,
              scanId,
              component,
              refIdToHtmlStore)).flatMap(Collection::stream).collect(toList());
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }
    return Collections.emptyList();
  }

  private List<Document> buildApplicationComponentSecurityVulnerabilities(
      Application application,
      StageType stageType,
      String scanId,
      Component component,
      Map<String, String> refIdToHtmlStore)
  {
    return component.getSecurityVulnerabilities().parallelStream().map(securityVulnerability -> {
      DocumentFields documentFields = new DocumentFields(ItemType.SECURITY_VULNERABILITY);
      documentFields.setApplicationId(application.getId());
      documentFields.setApplicationPublicId(application.getPublicId());
      documentFields.setApplicationName(application.getName());
      documentFields.setPolicyEvaluationStage(stageType.getName());
      documentFields.setReportId(scanId);
      documentFields.setComponentHash(component.getHash());
      documentFields.setComponentFormat(component.getComponentIdentifier().getFormat());
      documentFields.setComponentCoordinates(component);
      documentFields.setComponentName(component.getDisplayName());
      documentFields.setVulnerabilityId(securityVulnerability.getRefId());
      documentFields.setVulnerabilityStatus(securityVulnerability.getStatus().getName());
      documentFields.setVulnerabilityDescription(getDescription(securityVulnerability.getRefId(), refIdToHtmlStore));
      return documentFields.build();
    }).collect(toList());
  }

  private String getDescription(
      String refId,
      Map<String, String> refIdToHtmlStore)
  {
    try {
      String html = refIdToHtmlStore.get(refId);
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
