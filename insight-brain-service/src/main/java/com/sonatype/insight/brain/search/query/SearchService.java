/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.search.LowerCaseAnalyzer;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.search.results.SearchSuggestionResultDTO;
import com.sonatype.insight.brain.service.InsightWork;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.analyzing.AnalyzingInfixSuggester;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.BytesRef;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.*;

/**
 * @since GLOBAL_SEARCH
 */
@Named
@Singleton
public class SearchService
{
  private static final Logger log = LoggerFactory.getLogger(SearchService.class);

  private final InsightWork insightWork;

  private final Set<String> analyzedFields = Stream
      .of(VULNERABILITY_DESCRIPTION.label, APPLICATION_NAME.label, ORGANIZATION_NAME.label).collect(Collectors.toSet());

  private final Analyzer standardAnalyzer = new StandardAnalyzer();

  private final Map<String, Analyzer> fieldsWithAnalyzers = new HashMap<String, Analyzer>()
  {
    {
      put(VULNERABILITY_DESCRIPTION.label, standardAnalyzer);
      put(APPLICATION_NAME.label, standardAnalyzer);
      put(ORGANIZATION_NAME.label, standardAnalyzer);
    }
  };

  @Inject
  public SearchService(InsightWork insightWork) {
    this.insightWork = insightWork;
  }

  public SearchResultDTO searchIndex(String searchQuery, int pageSize, int page) throws Exception {
    try (IndexReader indexReader = DirectoryReader.open(FSDirectory.open(insightWork.getSearchIndexDir().toPath()))) {
      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = page;
      searchResultDTO.pageSize = pageSize;

      IndexSearcher indexSearcher = new IndexSearcher(indexReader);

      // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
      if (indexReader.maxDoc() < 1) {
        searchResultDTO.totalNumberOfHits = 0;
        return searchResultDTO;
      }

      Query query = createQuery(searchQuery);

      int startIndex = (page - 1) * pageSize;
      TopDocs topDocs = indexSearcher.search(query, indexReader.maxDoc());
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;

      List<Document> documents = new ArrayList<>();

      Map<Document, Integer> documentScores = new HashMap<>();
      for (ScoreDoc scoreDoc : scoreDocs) {
        Document document = indexSearcher.doc(scoreDoc.doc);
        documents.add(document);
        documentScores.put(document, scoreDoc.shardIndex);
      }

      FieldIdentifier groupIdentifier = getGrouper(searchQuery);

      Comparator<Document> byLabel = Comparator.comparing(document -> document.get(groupIdentifier.label));
      Comparator<Document> byScore = Comparator.comparing(documentScores::get);
      documents.sort(byLabel.thenComparing(byScore));

      int resultIndex = startIndex + 1;
      for (int i = startIndex; i < startIndex + pageSize; i++) {
        if (i >= documents.size()) {
          break;
        }
        Document document = documents.get(i);
        SearchResultItemDTO searchResultItemDTO = toDto(document);
        if (searchResultItemDTO == null) {
          continue;
        }

        String groupBy = document.get(groupIdentifier.label);

        if (searchResultDTO.groupingByDTOS.stream().noneMatch(dto -> dto.groupBy.equals(groupBy))) {
          GroupingByDTO groupingByDTO = new GroupingByDTO();
          groupingByDTO.groupBy = groupBy;
          groupingByDTO.groupIdentifier = groupIdentifier;

          if (groupIdentifier == VULNERABILITY_ID || groupIdentifier == VULNERABILITY_DESCRIPTION) {
            groupingByDTO.additionalInfo = document.get(VULNERABILITY_DESCRIPTION.label);
          }

          searchResultDTO.groupingByDTOS.add(groupingByDTO);
        }
        GroupingByDTO groupingByDTO = searchResultDTO.groupingByDTOS.stream()
            .filter(dto -> dto.groupBy.equals(groupBy)).findAny().get();

        searchResultItemDTO.resultIndex = resultIndex++;
        groupingByDTO.searchResultItemDTOS.add(searchResultItemDTO);
      }
      searchResultDTO.totalNumberOfHits = Long.valueOf(topDocs.totalHits.value).intValue();
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;
      return searchResultDTO;
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
      throw e;
    }
  }

  public SearchSuggestionResultDTO autoCompleteSearchQuery(String searchQuery) throws Exception {
    List<String> searchSuggestionResultItems = new ArrayList<>();
    SearchSuggestionResultDTO searchResultDTO = new SearchSuggestionResultDTO();
    Analyzer analyzer = new SimpleAnalyzer();

    try (FSDirectory suggesterFile = FSDirectory.open(insightWork.getSearchSuggesterDir().toPath());
         AnalyzingInfixSuggester suggester = new AnalyzingInfixSuggester(suggesterFile, analyzer)) {
      List<Lookup.LookupResult> results;
      HashSet<BytesRef> contexts = new HashSet<>();
      // Do the lookup and get up to 10 results
      results = suggester.lookup(searchQuery, contexts, 10, false, false);

      for (Lookup.LookupResult result : results) {
        searchSuggestionResultItems.add(result.key.toString());
      }
    }
    catch (IOException e) {
      log.error(e.getMessage(), e);
    }

    searchResultDTO.searchResultItems = searchSuggestionResultItems;

    return searchResultDTO;
  }

  private FieldIdentifier getGrouper(String searchQuery) {
    Optional<String> searchBy = labelIdentifiers().stream().filter(searchQuery::startsWith).findAny();
    if (searchBy.isPresent()) {
      FieldIdentifier fieldIdentifier = byLabel(searchQuery.substring(0, searchQuery.indexOf(":")));

      if (fieldIdentifier == VULNERABILITY_DESCRIPTION) {  // Grouping by description does not make sense
        return VULNERABILITY_ID;
      }

      return fieldIdentifier;
    }
    return VULNERABILITY_ID;
  }

  private Query createQuery(String searchQuery) throws Exception {
    if (StringUtils.isBlank(searchQuery)) {
      throw new IllegalArgumentException("Search query is empty");
    }

    Optional<String> searchField = labelIdentifiers().stream().filter(searchQuery::startsWith).findAny();
    if (searchField.isPresent()) {
      searchQuery = searchQuery.substring(searchQuery.indexOf(":") + 1);
    }

    return createQuery(searchField.orElse(VULNERABILITY_ID.label), searchQuery);
  }

  private Query createQuery(String field, String searchQuery) throws Exception {
    if (fieldRequiresAnalysis(field) || field.equalsIgnoreCase(COMPONENT_NAME.label)) {
      return createQueryUsingParser(field, searchQuery);
    }
    else {
      return createBasicQuery(field, searchQuery);
    }
  }

  private boolean fieldRequiresAnalysis(final String field) {
    String analyzedField =
        analyzedFields.stream().filter(fieldName -> StringUtils.equalsIgnoreCase(fieldName, field)).findFirst()
            .orElse(null);
    return analyzedField != null;
  }

  private Query createQueryUsingParser(String field, String searchQuery) throws Exception {
    PerFieldAnalyzerWrapper analyzer = new PerFieldAnalyzerWrapper(new LowerCaseAnalyzer(), fieldsWithAnalyzers);

    String finalSearchQuery = searchQuery;
    // componentDisplayName in the form of: org.bouncycastle : bcprov-jdk15on : 1.50
    if (field.equalsIgnoreCase(COMPONENT_NAME.label)) {
      finalSearchQuery = searchQuery.replace(":", "\\:").replace(" ", "\\ ");
    }
    return new QueryParser(field, analyzer).parse(finalSearchQuery);
  }

  private Query createBasicQuery(String field, String searchQuery) {
    if (containsWildCardCharacter(searchQuery)) {
      return new WildcardQuery(new Term(field, searchQuery));
    }
    else {
      return new TermQuery(new Term(field, searchQuery));
    }
  }

  private boolean containsWildCardCharacter(String searchQuery) {
    for (char ch : searchQuery.toCharArray()) {
      if (ch == '*' || ch == '?') {
        return true;
      }
    }
    return false;
  }

  private SearchResultItemDTO toDto(Document document) {
    SearchResultItemDTO searchResultItemDTO = new SearchResultItemDTO();

    if (document.get(ORGANIZATION_ID.label) == null && document.get(APPLICATION_ID.label) == null) {
      // This means our index is not up to date
      log.warn("Document found in index but it is not present in the database. Please rebuild the index.");
      return null;
    }

    searchResultItemDTO.itemType = document.get(ITEM_TYPE.label);
    searchResultItemDTO.organizationId = document.get(ORGANIZATION_ID.label);
    searchResultItemDTO.organizationName = document.get(ORGANIZATION_NAME.label);
    searchResultItemDTO.applicationId = document.get(APPLICATION_ID.label);
    searchResultItemDTO.applicationPublicId = document.get(APPLICATION_PUBLIC_ID.label);
    searchResultItemDTO.applicationName = document.get(APPLICATION_NAME.label);
    searchResultItemDTO.policyEvaluationStage = document.get(POLICY_EVALUATION_STAGE.label);
    searchResultItemDTO.reportId = document.get(REPORT_ID.label);
    searchResultItemDTO.componentHash = document.get(COMPONENT_HASH.label);
    ApiComponentIdentifierDTOV2 apiComponentIdentifierDTOV2 = new ApiComponentIdentifierDTOV2();
    String format = document.get(COMPONENT_FORMAT.label);
    apiComponentIdentifierDTOV2.setFormat(format);
    Map<String, String> coordinates = new TreeMap<>();
    for (String coordinateName : ComponentIdentifier.getAllCoordinateNames(format)) {
      String coordinateValue = document.get(coordinateName);
      if (coordinateValue == null) {
        continue;
      }
      coordinates.put(coordinateName, coordinateValue);
    }
    apiComponentIdentifierDTOV2.setCoordinates(coordinates);
    searchResultItemDTO.componentIdentifier = apiComponentIdentifierDTOV2;
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
    searchResultItemDTO.policyThreatLevel = policyThreatLevel == null ? -1 : Integer.valueOf(policyThreatLevel);
    return searchResultItemDTO;
  }
}
