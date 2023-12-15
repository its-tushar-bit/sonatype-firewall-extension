/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiComponentIdentifierDTOV2;
import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
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
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.BooleanQuery.TooManyClauses;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryVisitor;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.Directory;

import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getItemManagementPathEdit;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getManagementPath;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getReportUrl;
import static com.sonatype.insight.brain.landing.UserInterfaceLinksHelper.getVulnerabilityDetailsUrl;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.APPLICATION_CATEGORY_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.APPLICATION_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.EXPORT_FILE_NAME;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.EXPORT_SEARCH_HEADERS;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.LABEL_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.ORGANIZATION_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.AdvancedSearchExportPaths.POLICY_PATH_VARIABLE;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.*;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.APPLICATION;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.ORGANIZATION;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.POLICY;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.SECURITY_VULNERABILITY;

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

  private final OwnerDAO ownerDAO;

  private final Configuration configuration;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public SearchService(
      LuceneComponents luceneComponents,
      AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      PermissionService permissionService,
      CurrentUser currentUser,
      OwnerDAO ownerDAO,
      Configuration configuration,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.luceneComponents = luceneComponents;
    this.advancedSearchTelemetryMetrics = advancedSearchTelemetryMetrics;
    this.permissionService = permissionService;
    this.currentUser = currentUser;
    this.ownerDAO = ownerDAO;
    this.configuration = configuration;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  public SearchResultDTO searchIndex(String searchQuery, int pageSize, int page, boolean allComponents)
      throws IOException
  {
    return searchIndex(searchQuery, pageSize, page, allComponents, false);
  }

  public SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isExportable)
      throws IOException
  {
    boolean initialSearch = false;
    if (page == 0) {
      // when actually paging through the results, a positive page index is used
      // 0 denotes first page of new search
      page = 1;
      initialSearch = true;
    }

    try (Directory directory = openSearchIndex(); //
         IndexReader indexReader = DirectoryReader.open(directory)) {

      //Get all results
      pageSize = isExportable ? Math.max(1, indexReader.maxDoc()) : pageSize;

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
      groupAllDocuments(documents, searchResultDTO, groupFieldNamesByItemType);

      int resultRecordCount;
      if (isExportable) {
        resultRecordCount = (int) topDocs.totalHits.value;
      }
      else {
        resultRecordCount = filterResultsByPage(searchResultDTO, page, pageSize);
      }

      searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;
      AuditData.get().setData("resultRecordCount", resultRecordCount);
      return searchResultDTO;
    }
  }

  /**
   * Filter the returned documents based on a result index range corresponding to the page and pageSize values.
   */
  private int filterResultsByPage(final SearchResultDTO searchResultDTO, final int page, final int pageSize) {
    int startIndex = (page - 1) * pageSize + 1;
    int endIndex = page * pageSize;
    int resultRecordCount = 0;

    // remove searchResultItemDTOs that done fit in the resultIndex range
    for (GroupingByDTO groupingByDTO : searchResultDTO.groupingByDTOS) {
      groupingByDTO.searchResultItemDTOS.removeIf(e -> e.resultIndex < startIndex || e.resultIndex > endIndex);
      resultRecordCount += groupingByDTO.searchResultItemDTOS.size();
    }

    // remove possible empty groupingByDTOs
    searchResultDTO.groupingByDTOS.removeIf(e -> e.searchResultItemDTOS.isEmpty());

    return resultRecordCount;
  }

  /**
   * Groups all returned documents by their groupBy field. The page and pageSize values play no role in this.
   */
  private void groupAllDocuments(
      final List<Document> documents,
      final SearchResultDTO searchResultDTO,
      final Map<String, String> groupFieldNamesByItemType)
  {
    for (Document document : documents) {
      SearchResultItemDTO searchResultItemDTO = toDto(document);
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
      searchResultDTO.groupingByDTOS.stream().filter(dto -> dto.groupBy.equals(groupBy)).findAny().ifPresent(
          groupingByDTO -> groupingByDTO.searchResultItemDTOS.add(searchResultItemDTO)
      );
    }
    setResultIndexOnSearchResultItemDTOS(searchResultDTO);
  }

  private void setResultIndexOnSearchResultItemDTOS(final SearchResultDTO searchResultDTO) {
    int resultIndex = 1;
    for (GroupingByDTO groupingByDTO : searchResultDTO.groupingByDTOS) {
      for (SearchResultItemDTO searchResultItemDTO : groupingByDTO.searchResultItemDTOS) {
        searchResultItemDTO.resultIndex = resultIndex++;
      }
    }
  }

  public Response exportSearch(String searchQuery, boolean allComponents) {
    try {
      List<SearchResultItemDTO> searchResultItemsDTO =
          searchIndex(searchQuery, 0, 0, allComponents, true)
              .groupingByDTOS.stream()
              .flatMap(g -> g.searchResultItemDTOS.stream())
              .collect(Collectors.toList());

      ResponseBuilder responseBuilder = Response.ok(createAdvancedSearchCSV(searchResultItemsDTO))
          .type("application/csv; charset=UTF-8")
          .encoding("UTF-8")
          .header(HttpHeaders.CONTENT_DISPOSITION,
              HttpHeaderUtils.buildContentDispositionHeaderValue(EXPORT_FILE_NAME));
      return responseBuilder.build();
    }
    catch (IOException e) {
      throw new UncheckedIOException("The response with CSV file could not be sent", e);
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

    BooleanQuery.setMaxClauseCount(configuration.getMaxAdvancedSearchClauseCount());
    Builder allowedContextIdsQueryBuilder = new Builder();

    try {
      for (String contextId : contextIdsWithReadPermission) {
        allowedContextIdsQueryBuilder.add(new TermQuery(new Term(APPLICATION_ID.label, contextId)), Occur.SHOULD);
        allowedContextIdsQueryBuilder.add(new TermQuery(new Term(ORGANIZATION_ID.label, contextId)), Occur.SHOULD);
      }

      return new Builder()
          .add(allowedContextIdsQueryBuilder.build(), Occur.MUST)
          .add(query, Occur.MUST)
          .build();
    }
    catch (TooManyClauses e) {
      throw new BadRequestException("Error performing search due to too many clauses. " +
          "Please try narrowing down the query as much as possible " +
          "and consider updating Advanced Search configuration to support larger queries.");
    }
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

  private StreamingOutput createAdvancedSearchCSV(List<SearchResultItemDTO> searchResultItemsDTOS) {
    CSVFormat csvFormat = CSVFormat.Builder.create()
        .setHeader(EXPORT_SEARCH_HEADERS)
        .setDelimiter(configuration.getAdvancedSearchCSVExportDelimiter())
        .build();

    String baseUrl = Objects.toString(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL), "");

    return os -> {
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(os));
           CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
        for (SearchResultItemDTO searchResultItemDTO : searchResultItemsDTOS) {
          printer.printRecord(getAdvancedSearchCVSRowFromSearchResultItem(searchResultItemDTO, baseUrl));
        }
        printer.flush();
        writer.flush();
      }
    };
  }

  private List<String> getAdvancedSearchCVSRowFromSearchResultItem(
      SearchResultItemDTO searchResultItemDTO,
      String baseUrl)
  {
    List<String> row = new ArrayList<>(Collections.nCopies(16, ""));

    switch (ItemType.valueOf(searchResultItemDTO.itemType)) {
      case ORGANIZATION:
        row.set(0, ORGANIZATION.name());
        row.set(1, searchResultItemDTO.organizationName);
        row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
        break;
      case APPLICATION:
        row.set(0, APPLICATION.name());
        row.set(1, searchResultItemDTO.organizationName);
        row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
        row.set(3, searchResultItemDTO.applicationName);
        row.set(4, baseUrl + getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId));
        break;
      case APPLICATION_CATEGORY:
        row.set(0, APPLICATION_CATEGORY.name());
        row.set(1, searchResultItemDTO.organizationName);
        row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
        row.set(5, searchResultItemDTO.applicationCategoryName);
        row.set(6, baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
            APPLICATION_CATEGORY_PATH_VARIABLE, searchResultItemDTO.applicationCategoryId));
        break;
      case COMPONENT_LABEL:
        row.set(0, COMPONENT_LABEL.name());
        row.set(7, searchResultItemDTO.componentLabelName);
        if (!Objects.isNull(searchResultItemDTO.organizationId)) {
          row.set(1, searchResultItemDTO.organizationName);
          row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
          row.set(8, baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
              LABEL_PATH_VARIABLE, searchResultItemDTO.componentLabelId));
        }
        else {
          row.set(3, searchResultItemDTO.applicationName);
          row.set(4, baseUrl + getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId));
          row.set(8,
              baseUrl + getItemManagementPathEdit(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId,
                  LABEL_PATH_VARIABLE, searchResultItemDTO.componentLabelId));
        }
        break;
      case POLICY:
        row.set(0, POLICY.name());
        row.set(9, searchResultItemDTO.policyName);
        row.set(10, String.valueOf(searchResultItemDTO.policyThreatLevel));
        if (!Objects.isNull(searchResultItemDTO.organizationId)) {
          row.set(1, searchResultItemDTO.organizationName);
          row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
          row.set(11,
              baseUrl + getItemManagementPathEdit(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId,
                  POLICY_PATH_VARIABLE, searchResultItemDTO.policyId));
        }
        else {
          row.set(3, searchResultItemDTO.applicationName);
          row.set(4, baseUrl + getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId));
          row.set(11, baseUrl +
              getItemManagementPathEdit(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId,
                  POLICY_PATH_VARIABLE, searchResultItemDTO.policyId));
        }
        break;
      case SECURITY_VULNERABILITY:
        row.set(0, SECURITY_VULNERABILITY.name());
        if (searchResultItemDTO.organizationName != null) {
          row.set(1, searchResultItemDTO.organizationName);
          row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
        }
        row.set(3, searchResultItemDTO.applicationName);
        row.set(4, baseUrl + getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId));
        row.set(12, searchResultItemDTO.componentName);
        row.set(13, baseUrl + getReportUrl(searchResultItemDTO.applicationPublicId, searchResultItemDTO.reportId));
        row.set(14, baseUrl + getVulnerabilityDetailsUrl(searchResultItemDTO.vulnerabilityId));
        row.set(15, searchResultItemDTO.policyEvaluationStage);
        break;
      case NON_VULNERABLE_COMPONENT:
        row.set(0, NON_VULNERABLE_COMPONENT.name());
        if (searchResultItemDTO.organizationName != null) {
          row.set(1, searchResultItemDTO.organizationName);
          row.set(2, baseUrl + getManagementPath(ORGANIZATION_PATH_VARIABLE, searchResultItemDTO.organizationId));
        }
        row.set(3, searchResultItemDTO.applicationName);
        row.set(4, baseUrl + getManagementPath(APPLICATION_PATH_VARIABLE, searchResultItemDTO.applicationPublicId));
        row.set(12, searchResultItemDTO.componentName);
        row.set(13, baseUrl + getReportUrl(searchResultItemDTO.applicationPublicId, searchResultItemDTO.reportId));
        row.set(15, searchResultItemDTO.policyEvaluationStage);
        break;
      default:
        Collections.fill(row, "");
        break;
    }
    return row;
  }
}
