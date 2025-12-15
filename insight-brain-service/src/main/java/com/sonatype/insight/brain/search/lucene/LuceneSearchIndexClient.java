/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.lucene;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.sonatype.insight.brain.audit.AuditData;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartySbomMetadataDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.product.license.ProductLicense;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.SearchModule;
import com.sonatype.insight.brain.search.index.AbstractSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.security.CurrentUser;
import com.sonatype.insight.brain.security.PermissionService;
import com.sonatype.insight.brain.service.Configuration;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.brain.telemetry.AdvancedSearchTelemetryMetrics;
import com.sonatype.insight.brain.telemetry.TelemetrySender;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.SegmentInfos;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.IndexSearcher.TooManyClauses;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits.Relation;
import org.apache.lucene.store.Directory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.vyarus.dropwizard.guice.module.installer.scanner.InvisibleForScanner;

/**
 * Lucene support for {@link SearchIndexClient}
 * <p>
 * Note: See {@link SearchModule} for Guice bindings
 */
@Singleton
@InvisibleForScanner
public class LuceneSearchIndexClient
    extends AbstractSearchIndexClient
{
  private static final Logger log = LoggerFactory.getLogger(LuceneSearchIndexClient.class);

  private static final String NO_INDEX_ERROR_MESSAGE =
      "Index does not exist or is unreadable, please (re)create your index.";

  private final InsightWork insightWork;

  @Inject
  public LuceneSearchIndexClient(
      final ApplicationDAO applicationDAO,
      final LabelDAO labelDAO,
      final OrganizationDAO organizationDAO,
      final OwnerDAO ownerDAO,
      final PolicyDAO policyDAO,
      final TagDAO tagDAO,
      final ThirdPartySbomMetadataDAO thirdPartySbomMetadataDAO,
      final DocumentBuilderHelper documentBuilderHelper,
      final ProductLicense productLicense,
      final TelemetrySender telemetrySender,
      final SearchIndexChangeDAO searchIndexChangeDAO,
      final LuceneComponents luceneComponents,
      final InsightWork insightWork,
      final AdvancedSearchTelemetryMetrics advancedSearchTelemetryMetrics,
      final Configuration configuration,
      final PermissionService permissionService,
      final CurrentUser currentUser,
      final ConversionHelper conversionHelper,
      final ShutdownHandler shutdownHandler)
  {
    super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, searchIndexChangeDAO, tagDAO,
        thirdPartySbomMetadataDAO, documentBuilderHelper, productLicense, telemetrySender, luceneComponents,
        advancedSearchTelemetryMetrics, configuration, permissionService, currentUser, conversionHelper,
        shutdownHandler);
    this.insightWork = insightWork;
  }

  @Override
  public void populateIndex() {
    log.info("creating search index...");
    long start = System.currentTimeMillis();
    try (Directory directory = luceneComponents.openSearchIndex(false);
         IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE)) {
      doPopulateIndex(new LuceneIndexingContext(ownerDAO, indexWriter, conversionHelper));
      indexWriter.commit();
      log.info("all indexing complete");
    }
    catch (Exception e) {
      throw new SearchIndexException("Error creating search index", e);
    }
    sendAdvancedSearchIndexingTelemetry(System.currentTimeMillis() - start);
    log.info("index creation exit");
  }

  @Override
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
    catch (Exception e) {
      throw new SearchIndexException(e);
    }
  }

  @Override
  public void updateIndex() {
    List<SearchIndexChange> searchIndexChanges = getSearchIndexChanges();
    if (searchIndexChanges.isEmpty()) {
      return;
    }
    try (Directory directory = luceneComponents.openSearchIndex(false);
         IndexWriter indexWriter = newIndexWriter(directory, OpenMode.CREATE_OR_APPEND)) {
      processSearchIndexChanges(searchIndexChanges, new LuceneIndexingContext(ownerDAO, indexWriter, conversionHelper));
    }
    catch (Exception e) {
      throw new SearchIndexException("Error updating the search index", e);
    }
  }

  @Override
  public Long getLastIndexTime() {
    try (Directory directory = luceneComponents.openSearchIndex(true)) {
      if (directory == null) {
        return null;
      }
      String lastCommitSegmentsFileName = SegmentInfos.getLastCommitSegmentsFileName(directory);
      if (lastCommitSegmentsFileName == null) {
        return null;
      }
      return new File(insightWork.getSearchIndexDir(), lastCommitSegmentsFileName).lastModified();
    }
    catch (Exception e) {
      log.error(e.getMessage(), e);
    }
    return null;
  }

  private IndexWriter newIndexWriter(final Directory directory, final OpenMode openMode) throws IOException {
    return new IndexWriter(directory,
        new IndexWriterConfig(luceneComponents.newAnalyzerForSearch()).setOpenMode(openMode));
  }

  @Override
  public SearchResultDTO searchIndex(
      final String searchQuery,
      final int pageSize,
      final int page,
      final boolean allComponents,
      final boolean isSbomManagerMode,
      final List<String> searchAfter) throws SearchIndexException
  {
    checkMode(isSbomManagerMode);

    boolean initialSearch = false;
    int finalPage = page;
    if (page == 0) {
      // when actually paging through the results, a positive page index is used
      // 0 denotes first page of new search
      finalPage = 1;
      initialSearch = true;
    }

    updateMaxQueryClauseCount();

    try (Directory directory = openSearchIndex();
         IndexReader indexReader = DirectoryReader.open(directory)) {
      AuditData.get()
          .setData("searchQuery", searchQuery)
          .setData("searchPageSize", pageSize)
          .setData("searchPageIndex", finalPage - 1);

      String initialQuery = createInitialQuery(searchQuery, allComponents);
      Set<String> fieldNames = getFieldNames(initialQuery);
      populateTelemetry(initialSearch, fieldNames);
      checkFieldNames(fieldNames);
      String finalQuery = createFinalQuery(initialQuery, isSbomManagerMode);

      // Passing 0 to IndexSearcher#search throws IllegalArgumentException with 'numHits must be > 0'
      IndexSearcher indexSearcher = new IndexSearcher(indexReader);
      Query query = conversionHelper.stringToQuery(finalQuery);
      TopDocs topDocs = indexSearcher.search(query, Math.max(1, indexReader.maxDoc()));

      SearchResultDTO searchResultDTO = new SearchResultDTO();
      searchResultDTO.searchQuery = searchQuery;
      searchResultDTO.page = finalPage;
      searchResultDTO.pageSize = pageSize;
      groupDocuments(indexSearcher, topDocs.scoreDocs, finalPage, pageSize, searchResultDTO,
          getGroupFieldNamesByItemType(fieldNames));
      searchResultDTO.totalNumberOfHits = (int) topDocs.totalHits.value;
      searchResultDTO.isExactTotalNumberOfHits = topDocs.totalHits.relation == Relation.EQUAL_TO;

      AuditData.get().setData("resultRecordCount", searchResultDTO.countSearchResults());
      return searchResultDTO;
    }
    catch (Exception e) {
      if (e instanceof TooManyClauses) {
        throw TOO_MANY_CLAUSES_EXCEPTION;
      }
      if (e instanceof BadRequestException badRequestException) {
        throw badRequestException;
      }
      if (e instanceof ConflictException conflictException) {
        throw conflictException;
      }
      throw new SearchIndexException(e);
    }
  }

  // Update the static setting within lucene for the max query clause count, based on the current value in the
  // configuration
  @Override
  protected void updateMaxQueryClauseCount() {
    IndexSearcher.setMaxClauseCount(configuration.getMaxAdvancedSearchClauseCount());
  }

  private void groupDocuments(
      final IndexSearcher indexSearcher,
      final ScoreDoc[] scoreDocs,
      final int page,
      final int pageSize,
      final SearchResultDTO searchResultDTO,
      final Map<String, String> groupFieldNamesByItemType)
  {
    int startIndex = (page - 1) * pageSize;
    int endIndex = page * pageSize;
    Supplier<Document> documentSupplier = new Supplier<>()
    {
      private int currentIndex = startIndex;

      @Override
      public Document get() {
        if (currentIndex < endIndex && currentIndex < scoreDocs.length) {
          try {
            return indexSearcher.storedFields().document(scoreDocs[currentIndex++].doc);
          }
          catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
        return null;
      }
    };
    groupDocuments(page, pageSize, documentSupplier, searchResultDTO, groupFieldNamesByItemType);
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
}
