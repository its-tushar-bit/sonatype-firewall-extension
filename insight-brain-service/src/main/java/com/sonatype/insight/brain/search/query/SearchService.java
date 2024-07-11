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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
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
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Permission;
import com.sonatype.insight.brain.product.license.InvalidLicenseException;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.search.LuceneComponents;
import com.sonatype.insight.brain.search.docs.DocumentBuilder;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier;
import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.export.LifecycleSearchRowFactory;
import com.sonatype.insight.brain.search.export.SbomSearchRowFactory;
import com.sonatype.insight.brain.search.export.SearchRowFactory;
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
import com.sonatype.insight.license.model.LicensedFeature;
import com.sonatype.insight.license.model.ProductLicenseDetails;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
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

import static com.sonatype.insight.brain.search.docs.DocumentBuilder.FieldIdentifier.*;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.APPLICATION_CATEGORY;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.COMPONENT_LABEL;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.NON_VULNERABLE_COMPONENT;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.POLICY;
import static com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType.SBOM_METADATA;
import static com.sonatype.insight.brain.search.export.SearchPaths.EXPORT_FILE_NAME;

/**
 * @since 1.88
 */
@Named
@Singleton
public class SearchService
{
  private static final Logger log = LoggerFactory.getLogger(SearchService.class);

  private static final String NO_INDEX_ERROR_MESSAGE =
      "Index does not exist or is unreadable, please (re)create your index.";

  private static final int MAX_PAGE_SIZE = 10000;

  private final LuceneComponents luceneComponents;

  private final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics;

  private final PermissionService permissionService;

  private final CurrentUser currentUser;

  private final OwnerDAO ownerDAO;

  private final Configuration configuration;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  private final ProductLicense productLicense;

  private final LifecycleSearchRowFactory lifecycleSearchRowFactory;

  private final SbomSearchRowFactory sbomManagerSearchRowFactory;

  @Inject
  public SearchService(
      LuceneComponents luceneComponents,
      AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      PermissionService permissionService,
      CurrentUser currentUser,
      OwnerDAO ownerDAO,
      Configuration configuration,
      SystemConfigurationPropertyDAO systemConfigurationPropertyDAO,
      ProductLicense productLicense,
      LifecycleSearchRowFactory lifecycleSearchRowFactory,
      SbomSearchRowFactory sbomManagerSearchRowFactory)
  {
    this.luceneComponents = luceneComponents;
    this.advancedSearchTelemetryMetrics = advancedSearchTelemetryMetrics;
    this.permissionService = permissionService;
    this.currentUser = currentUser;
    this.ownerDAO = ownerDAO;
    this.configuration = configuration;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
    this.productLicense = productLicense;
    this.lifecycleSearchRowFactory = lifecycleSearchRowFactory;
    this.sbomManagerSearchRowFactory = sbomManagerSearchRowFactory;
  }

  public SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      ProductMode mode)
      throws IOException
  {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    return searchIndex(searchQuery, pageSize, page, allComponents, isSbomManagerMode(mode));
  }

  private SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      boolean isSbomManagerMode)
      throws IOException
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

  public Response exportSearch(
      String searchQuery,
      Integer pageSize,
      int page,
      boolean allComponents,
      ProductMode mode)
  {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    boolean isSbomManagerMode = isSbomManagerMode(mode);
    Iterator<List<SearchResultItemDTO>> iterator = new Iterator<>()
    {
      private int currentPage = Math.max(1, page);

      private Integer lastResultsSize = null;

      @Override
      public boolean hasNext() {
        return !Objects.equals(lastResultsSize, 0);
      }

      @Override
      public List<SearchResultItemDTO> next() {
        try {
          List<SearchResultItemDTO> results = searchIndex(
              searchQuery,
              Math.min(pageSize == null ? SearchService.MAX_PAGE_SIZE : pageSize, SearchService.MAX_PAGE_SIZE),
              currentPage++,
              allComponents,
              isSbomManagerMode
          )
              .groupingByDTOS
              .stream()
              .flatMap(g -> g.searchResultItemDTOS.stream())
              .collect(Collectors.toList());
          lastResultsSize = results.size();
          return results;
        }
        catch (IOException e) {
          throw new UncheckedIOException("The response with CSV file could not be sent", e);
        }
      }
    };
    ResponseBuilder responseBuilder = Response.ok(createAdvancedSearchCSV(iterator, pageSize, isSbomManagerMode))
        .type("application/csv; charset=UTF-8")
        .encoding("UTF-8")
        .header(HttpHeaders.CONTENT_DISPOSITION,
            HttpHeaderUtils.buildContentDispositionHeaderValue(EXPORT_FILE_NAME));
    return responseBuilder.build();
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

  private StreamingOutput createAdvancedSearchCSV(
      Iterator<List<SearchResultItemDTO>> searchResultItemsDTOSIterator,
      Integer pageSize,
      boolean isSbomManagerMode)
  {
    SearchRowFactory searchExportRowFactory = getSearchRowFactory(isSbomManagerMode);

    CSVFormat csvFormat = CSVFormat.Builder.create()
        .setHeader(searchExportRowFactory.getHeaders())
        .setDelimiter(configuration.getAdvancedSearchCSVExportDelimiter())
        .build();

    String baseUrl = Objects.toString(systemConfigurationPropertyDAO.get(SystemConfigurationProperty.BASE_URL), "");

    return os -> {
      int count = 0;
      try (Writer writer = new BufferedWriter(new OutputStreamWriter(os));
           CSVPrinter printer = new CSVPrinter(writer, csvFormat)) {
        while (searchResultItemsDTOSIterator.hasNext() && (pageSize == null || count < pageSize)) {
          for (SearchResultItemDTO searchResultItemDTO : searchResultItemsDTOSIterator.next()) {
            count++;
            printer.printRecord(searchExportRowFactory.create(searchResultItemDTO, baseUrl));
          }
          printer.flush();
          writer.flush();
          os.flush();
        }
      }
    };
  }

  private SearchRowFactory getSearchRowFactory(boolean isSbomManagerMode) {
    return isSbomManagerMode ? sbomManagerSearchRowFactory : lifecycleSearchRowFactory;
  }

  private static boolean isSbomManagerMode(ProductMode mode) {
    return ProductMode.SBOM_MANAGER == mode;
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
