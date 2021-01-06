/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.search.LuceneComponents;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.results.GroupingByDTO;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.model.HasStringId;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.Directory;
import org.codehaus.plexus.util.StringUtils;

import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.*;

/**
 * @since 1.88
 */
@Named
@Singleton
public class SearchService
{
  private static final String NO_INDEX_ERROR_MESSAGE =
      "Index does not exist or is unreadable, please (re)create your index.";

  private final LuceneComponents luceneComponents;

  private final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics;

  private final PermissionService permissionService;

  private final CurrentUser currentUser;
  
  private OwnerDAO ownerDAO;

  @Inject
  public SearchService(
      LuceneComponents luceneComponents,
      AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      PermissionService permissionService,
      CurrentUser currentUser,
      OwnerDAO ownerDAO)
  {
    this.luceneComponents = luceneComponents;
    this.advancedSearchTelemetryMetrics = advancedSearchTelemetryMetrics;
    this.permissionService = permissionService;
    this.currentUser = currentUser;
    this.ownerDAO = ownerDAO;
  }

  public SearchResultDTO searchIndex(String searchQuery, int pageSize, int page) throws IOException {
    boolean initialSearch = false;
    if (page == 0) {
      // when actually paging through the results, a positive page index is used
      // 0 denotes first page of new search
      page = 1;
      initialSearch = true;
    }

    AuditData.get() //
        .setData("searchQuery", searchQuery) //
        .setData("searchPageSize", pageSize) //
        .setData("searchPageIndex", page - 1);

    try (IndexReader indexReader = DirectoryReader.open(openSearchIndex())) {
      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = page;
      searchResultDTO.pageSize = pageSize;

      IndexSearcher indexSearcher = new IndexSearcher(indexReader);

      Query query = createQuery(searchQuery);

      Set<String> fieldNames = getFieldNames(query);
      Set<String> invalidFieldNames = new TreeSet<>();

      // We only add telemetry for the initial search request in order to
      // avoid adding the same data when the user navigates search results.
      if (initialSearch) {
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

      Query queryWithPermissions = appendAllowedApplicationsAndOrganizationsToQuery(query);

      // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
      TopDocs topDocs = indexSearcher.search(queryWithPermissions, Math.max(1, indexReader.maxDoc()));
      ScoreDoc[] scoreDocs = topDocs.scoreDocs;

      List<Document> documents = new ArrayList<>();

      Map<Document, Integer> documentScores = new HashMap<>();
      for (ScoreDoc scoreDoc : scoreDocs) {
        Document document = indexSearcher.doc(scoreDoc.doc);
        documents.add(document);
        documentScores.put(document, scoreDoc.shardIndex);
      }

      Map<String, String> groupFieldNamesByItemType = getGroupFieldNamesByItemType(fieldNames);

      int startIndex = (page - 1) * pageSize;
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

        String groupFieldName = groupFieldNamesByItemType.get(searchResultItemDTO.itemType);
        FieldIdentifier groupIdentifier = getFieldIdentifier(groupFieldName);
        String groupBy = document.get(groupFieldName);

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
      searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;
      AuditData.get().setData("resultRecordCount", resultIndex - startIndex - 1);
      return searchResultDTO;
    }
  }

  private Directory openSearchIndex() {
    try {
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

    contextIdsWithReadPermission.addAll(getChildContextIds(contextIdsWithReadPermission));

    Builder allowedContextIdsQueryBuilder = new Builder();
    for (String contextId : contextIdsWithReadPermission) {
      allowedContextIdsQueryBuilder.add(new TermQuery(new Term(APPLICATION_ID.label, contextId)), Occur.SHOULD);
      allowedContextIdsQueryBuilder.add(new TermQuery(new Term(ORGANIZATION_ID.label, contextId)), Occur.SHOULD);
    }

    BooleanQuery allowedContextIdsAndUserQueryConcat =
        new Builder()
            .add(allowedContextIdsQueryBuilder.build(), Occur.MUST)
            .add(query, Occur.MUST).build();

    return allowedContextIdsAndUserQueryConcat;
  }

  private Set<String> getChildContextIds(Set<String> contextIdsWithReadPermission) {
    Set<String> childContextIds = new HashSet<>();
    for (String contextIdWithReadPermission : contextIdsWithReadPermission) {
      Owner owner = ownerDAO.getById(contextIdWithReadPermission);
      if (owner != null && OwnerType.ORGANIZATION.equals(owner.getType())) {
        childContextIds
            .addAll(ownerDAO.getChildOwners(owner).stream().map(HasStringId::getId).collect(Collectors.toSet()));
      }
    }
    return childContextIds;
  }
}
