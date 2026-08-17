/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.session;

import java.io.IOException;

import com.sonatype.insight.brain.search.SearchConfig;
import com.sonatype.insight.brain.search.SearchMode;
import com.sonatype.insight.brain.search.index.HybridSearchIndexClient;
import com.sonatype.insight.brain.search.index.SearchIndexException;
import com.sonatype.insight.brain.search.lucene.LuceneIndexReadSession;
import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.lucene.LuceneSearcherManagerHolder;
import com.sonatype.insight.brain.search.lucene.SearcherManagerUnavailableException;
import com.sonatype.insight.brain.search.opensearch.OpenSearchSearchIndexClient;
import com.sonatype.insight.brain.security.CurrentUser;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexReadSessionFactory
{
  private static final Logger log = LoggerFactory.getLogger(IndexReadSessionFactory.class);

  private final LuceneIndexWriterOwner indexWriterOwner;

  private final LuceneSearcherManagerHolder searcherManagerHolder;

  private final CurrentUser currentUser;

  private final ReadableContextAuthzCache readableContextAuthzCache;

  private final OpenSearchSearchIndexClient openSearchSearchIndexClient;

  private final SearchConfig searchConfig;

  public static IndexReadSessionFactory forProduction(
      final LuceneIndexWriterOwner indexWriterOwner,
      final CurrentUser currentUser,
      final ReadableContextAuthzCache readableContextAuthzCache,
      final OpenSearchSearchIndexClient openSearchSearchIndexClient,
      final SearchConfig searchConfig)
  {
    return new IndexReadSessionFactory(
        indexWriterOwner, null, currentUser, readableContextAuthzCache, openSearchSearchIndexClient, searchConfig);
  }

  static IndexReadSessionFactory forTest(
      final LuceneSearcherManagerHolder searcherManagerHolder,
      final CurrentUser currentUser,
      final ReadableContextAuthzCache readableContextAuthzCache)
  {
    return new IndexReadSessionFactory(null, searcherManagerHolder, currentUser, readableContextAuthzCache, null, null);
  }

  private IndexReadSessionFactory(
      final LuceneIndexWriterOwner indexWriterOwner,
      final LuceneSearcherManagerHolder searcherManagerHolder,
      final CurrentUser currentUser,
      final ReadableContextAuthzCache readableContextAuthzCache,
      final OpenSearchSearchIndexClient openSearchSearchIndexClient,
      final SearchConfig searchConfig)
  {
    if (indexWriterOwner != null && searcherManagerHolder != null) {
      throw new IllegalArgumentException("Specify either indexWriterOwner or searcherManagerHolder, not both");
    }
    if (indexWriterOwner == null && searcherManagerHolder == null) {
      throw new IllegalArgumentException("Either indexWriterOwner or searcherManagerHolder is required");
    }
    this.indexWriterOwner = indexWriterOwner;
    this.searcherManagerHolder = searcherManagerHolder;
    this.currentUser = currentUser;
    this.readableContextAuthzCache = readableContextAuthzCache;
    this.openSearchSearchIndexClient = openSearchSearchIndexClient;
    this.searchConfig = searchConfig;
    if (searchConfig != null) {
      SearchMode configuredMode = searchConfig.getMode();
      if ((configuredMode == SearchMode.OPENSEARCH || configuredMode == SearchMode.HYBRID)
          && openSearchSearchIndexClient == null)
      {
        log.warn(
            "Search mode is {} but OpenSearch client is not wired; IndexReadSession will fall back to Lucene",
            configuredMode);
      }
    }
  }

  public IndexReadSession open() {
    Query rbacFilter = readableContextAuthzCache.compiledRbacFilter(currentUser.getUserPrincipal());
    SearchMode mode =
        openSearchSearchIndexClient == null || searchConfig == null ? SearchMode.LUCENE : searchConfig.getMode();
    if (mode == SearchMode.OPENSEARCH) {
      return openOpenSearchSession(rbacFilter);
    }
    if (mode == SearchMode.HYBRID) {
      return openHybridSession(rbacFilter);
    }
    return openLuceneSession(rbacFilter);
  }

  private IndexReadSession openHybridSession(final Query rbacFilter) {
    try {
      return HybridSearchIndexClient.pinReadSession(openOpenSearchSession(rbacFilter));
    }
    catch (RuntimeException primaryFailure) {
      try {
        return HybridSearchIndexClient.pinReadSession(openLuceneSession(rbacFilter));
      }
      catch (RuntimeException secondaryFailure) {
        secondaryFailure.addSuppressed(primaryFailure);
        throw secondaryFailure;
      }
    }
  }

  private IndexReadSession openOpenSearchSession(final Query rbacFilter) {
    return openSearchSearchIndexClient.openReadSession(rbacFilter);
  }

  private IndexReadSession openLuceneSession(final Query rbacFilter) {
    try {
      return acquireReadSession(rbacFilter, resolveSearcherManagerHolder());
    }
    catch (SearcherManagerUnavailableException unavailable) {
      // Subtype of IOException, so it must precede the general catch: a closed/paused searcher retries via the
      // blocking accessor rather than failing the read.
      return retryReadSessionViaBlockingAccessor(rbacFilter, unavailable);
    }
    catch (IOException e) {
      throw new SearchIndexException(e);
    }
  }

  /**
   * The lock-free holder became unavailable between the usability check and acquire (rebuild cutover, tenant
   * deregister, or shutdown); retry once via the blocking accessor, which serves once the write lock is released.
   */
  private IndexReadSession retryReadSessionViaBlockingAccessor(
      final Query rbacFilter,
      final SearcherManagerUnavailableException unavailable)
  {
    if (indexWriterOwner == null) {
      throw new SearchIndexException(unavailable);
    }
    log.debug("Lucene searcher holder was unavailable on the lock-free read; retrying via the blocking accessor");
    try {
      return acquireReadSession(rbacFilter, indexWriterOwner.getSearcherManagerHolder());
    }
    catch (IOException e) {
      log.warn("Lucene searcher holder still unavailable after a blocking retry; failing the read session", e);
      throw new SearchIndexException(e);
    }
  }

  private IndexReadSession acquireReadSession(
      final Query rbacFilter,
      final LuceneSearcherManagerHolder holder) throws IOException
  {
    IndexSearcher searcher = null;
    try {
      searcher = holder.acquire();
      return new LuceneIndexReadSession(searcher, rbacFilter, holder);
    }
    catch (RuntimeException e) {
      releaseAfterOpenFailure(holder, searcher, e);
      throw e;
    }
  }

  private LuceneSearcherManagerHolder resolveSearcherManagerHolder() {
    if (searcherManagerHolder != null) {
      return searcherManagerHolder;
    }
    return indexWriterOwner.getSearcherManagerHolderIfUsable()
        .orElseGet(indexWriterOwner::getSearcherManagerHolder);
  }

  private void releaseAfterOpenFailure(
      final LuceneSearcherManagerHolder holder,
      final IndexSearcher searcher,
      final RuntimeException failure)
  {
    if (searcher == null) {
      return;
    }
    try {
      holder.release(searcher);
    }
    catch (IOException releaseFailure) {
      failure.addSuppressed(releaseFailure);
    }
  }
}
