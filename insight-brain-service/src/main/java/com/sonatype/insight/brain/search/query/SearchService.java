/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.query;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.StreamingOutput;

import com.sonatype.insight.brain.dataaccess.configuration.SystemConfigurationPropertyDAO;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationPropertyFeature;
import com.sonatype.insight.brain.product.license.ProductMode;
import com.sonatype.insight.brain.search.export.LifecycleSearchRowFactory;
import com.sonatype.insight.brain.search.export.SbomSearchRowFactory;
import com.sonatype.insight.brain.search.export.SearchRowFactory;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.utils.HttpHeaderUtils;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;

import static com.sonatype.insight.brain.search.export.SearchPaths.EXPORT_FILE_NAME;

@Named
@Singleton
public class SearchService
{
  public static final int MAX_PAGE_SIZE = 10000;

  public static final String SEARCH_AFTER_HEADER = "Search-After";

  private final SearchIndexClient searchIndexClient;

  private final LifecycleSearchRowFactory lifecycleSearchRowFactory;

  private final SbomSearchRowFactory sbomManagerSearchRowFactory;

  private final Configuration configuration;

  private final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO;

  @Inject
  public SearchService(
      final SearchIndexClient searchIndexClient,
      final LifecycleSearchRowFactory lifecycleSearchRowFactory,
      final SbomSearchRowFactory sbomManagerSearchRowFactory,
      final Configuration configuration,
      final SystemConfigurationPropertyDAO systemConfigurationPropertyDAO)
  {
    this.searchIndexClient = searchIndexClient;
    this.sbomManagerSearchRowFactory = sbomManagerSearchRowFactory;
    this.lifecycleSearchRowFactory = lifecycleSearchRowFactory;
    this.configuration = configuration;
    this.systemConfigurationPropertyDAO = systemConfigurationPropertyDAO;
  }

  /**
   * Primary entry point for querying the search index.
   */
  public SearchResultDTO searchIndex(
      String searchQuery,
      int pageSize,
      int page,
      boolean allComponents,
      ProductMode mode,
      String searchAfter)
  {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    return searchIndexClient.searchIndex(searchQuery, pageSize, page, allComponents, isSbomManagerMode(mode),
        parseSearchAfter(searchAfter));
  }

  private List<String> parseSearchAfter(final String searchAfter) {
    if (searchAfter == null) {
      return null;
    }
    List<String> split = Arrays.asList(searchAfter.split(","));
    if (split.isEmpty() || split.size() % 2 != 0) {
      throw new BadRequestException("Invalid searchAfter " + searchAfter + ".");
    }
    return split;
  }

  /**
   * Exporting query results from the search index
   */
  public Response exportSearch(
      String searchQuery,
      Integer pageSize,
      int page,
      boolean allComponents,
      ProductMode mode,
      String searchAfter,
      HttpServletRequest httpServletRequest,
      HttpServletResponse httpServletResponse)
  {
    SystemConfigurationPropertyFeature.ADVANCED_SEARCH_CONFIGURATION.verifyEnabled();
    boolean isSbomManagerMode = isSbomManagerMode(mode);
    List<String> parsedSearchAfter = parseSearchAfter(searchAfter);
    AtomicReference<List<String>> searchAfterRef = new AtomicReference<>();
    Iterator<List<SearchResultItemDTO>> iterator = new Iterator<>()
    {
      private int currentPage = Math.max(1, page);

      private Integer lastResultsSize = null;

      private List<String> searchAfter = parsedSearchAfter;

      @Override
      public boolean hasNext() {
        return !Objects.equals(lastResultsSize, 0);
      }

      @Override
      public List<SearchResultItemDTO> next() {
        try {
          int finalPageSize = Math.min(pageSize == null ? MAX_PAGE_SIZE : pageSize, MAX_PAGE_SIZE);
          SearchResultDTO searchResultDTO = searchIndexClient.searchIndex(
              searchQuery,
              finalPageSize,
              currentPage++,
              allComponents,
              isSbomManagerMode,
              searchAfter);
          searchAfter = searchResultDTO.searchAfter;
          searchAfterRef.set(searchAfter);
          List<SearchResultItemDTO> results = searchResultDTO.groupingByDTOS.stream()
              .flatMap(g -> g.searchResultItemDTOS.stream())
              .toList();
          lastResultsSize = results.size();
          return results;
        }
        catch (SearchIndexException e) {
          throw new RuntimeException("The response with CSV file could not be sent", e);
        }
      }
    };
    if (supportsTrailers(httpServletRequest)) {
      httpServletResponse.setTrailerFields(() -> searchAfterRef.get() == null
          ? Map.of()
          : Map.of(SEARCH_AFTER_HEADER, String.join(",", searchAfterRef.get())));
    }
    ResponseBuilder responseBuilder = Response.ok(createAdvancedSearchCSV(iterator, pageSize, isSbomManagerMode))
        .type("application/csv; charset=UTF-8")
        .encoding("UTF-8")
        .header(HttpHeaders.CONTENT_DISPOSITION, HttpHeaderUtils.buildContentDispositionHeaderValue(EXPORT_FILE_NAME));
    return responseBuilder.build();
  }

  boolean supportsTrailers(HttpServletRequest request) {
    String protocol = request.getProtocol();
    return StringUtils.isNotBlank(protocol) && !protocol.equals("HTTP/1.0");
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
          CSVPrinter printer = new CSVPrinter(writer,
              csvFormat))
      {
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
}
