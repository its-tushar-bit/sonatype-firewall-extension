/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.global.StaleCursorException;
import com.sonatype.insight.brain.search.global.GlobalSearchRequest;
import com.sonatype.insight.brain.search.global.GlobalSearchResult;
import com.sonatype.insight.brain.search.lucene.LuceneComponents;
import com.sonatype.insight.brain.search.lucene.LuceneIndexWriterOwner;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;
import com.sonatype.insight.brain.service.InsightWork;
import com.sonatype.insight.brain.shutdown.ShutdownHandler;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.ConflictException;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.MatchAllDocsQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class HybridSearchIndexClientTest
{
  @Mock
  private SearchIndexClient primaryClient;

  @Mock
  private SearchIndexClient secondaryClient;

  @Rule
  public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private HybridSearchIndexClient hybridClient;

  @Before
  public void setUp() {
    hybridClient = new HybridSearchIndexClient(primaryClient, secondaryClient);
  }

  @Test
  public void buildPermittedQuery_delegatesToPrimaryClient_notThrowingDefault() {
    // Hybrid must resolve the permission filter through the primary client, never the throwing
    // interface default, and must never consult the secondary for permission resolution.
    java.util.Set<String> contexts = java.util.Set.of("org-1");
    org.apache.lucene.search.Query base = new MatchAllDocsQuery();
    org.apache.lucene.search.Query filter = new org.apache.lucene.search.MatchNoDocsQuery("filter");
    org.apache.lucene.search.Query wrapped = new org.apache.lucene.search.MatchNoDocsQuery("wrapped");
    when(primaryClient.getCurrentUserContextIdsWithReadPermission()).thenReturn(contexts);
    when(primaryClient.buildAllowedContextIdsFilter(contexts)).thenReturn(filter);
    when(primaryClient.wrapWithPermissionFilter(base, filter)).thenReturn(wrapped);

    org.apache.lucene.search.Query result = hybridClient.buildPermittedQuery(base);

    assertThat(result).isSameAs(wrapped);
    verify(secondaryClient, never()).getCurrentUserContextIdsWithReadPermission();
  }

  @Test
  public void testCount_DelegatesToPrimaryClient() {
    // Given
    long expectedCount = 42L;
    String metricQuery = "itemType:APPLICATION";
    when(primaryClient.count(metricQuery)).thenReturn(expectedCount);

    // When
    long result = hybridClient.count(metricQuery);

    // Then
    assertThat(result).isEqualTo(expectedCount);
    verify(primaryClient, times(1)).count(metricQuery);
    verify(secondaryClient, never()).count(anyString());
  }

  @Test
  public void testCount_FallsBackToSecondary_WhenPrimaryFails() {
    String metricQuery = "itemType:application";
    when(primaryClient.count(metricQuery)).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.count(metricQuery)).thenReturn(7L);

    long result = hybridClient.count(metricQuery);

    assertThat(result).isEqualTo(7L);
    verify(primaryClient, times(1)).count(metricQuery);
    verify(secondaryClient, times(1)).count(metricQuery);
  }

  @Test
  public void testCount_ThrowsException_WhenBothFail() {
    String metricQuery = "itemType:application";
    when(primaryClient.count(metricQuery)).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.count(metricQuery)).thenThrow(new RuntimeException("Secondary client error"));

    assertThatThrownBy(() -> hybridClient.count(metricQuery))
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Count failed on both primary and secondary clients");
  }

  @Test
  public void testAggregateCountByField_FallsBackToSecondary_WhenPrimaryFails() {
    String metricQuery = "itemType:application";
    String bucketField = "policyViolationThreatLevel";
    Map<String, int[]> ranges = Map.of("critical", new int[]{9, 10});
    MetricAggregationResult expectedResult = new MetricAggregationResult(50L, Map.of("critical", 10L));
    when(primaryClient.aggregateCountByField(metricQuery, bucketField, ranges))
        .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.aggregateCountByField(metricQuery, bucketField, ranges)).thenReturn(expectedResult);

    MetricAggregationResult result = hybridClient.aggregateCountByField(metricQuery, bucketField, ranges);

    assertThat(result).isSameAs(expectedResult);
    verify(primaryClient, times(1)).aggregateCountByField(metricQuery, bucketField, ranges);
    verify(secondaryClient, times(1)).aggregateCountByField(metricQuery, bucketField, ranges);
  }

  @Test
  public void testAggregateCountByField_DelegatesToPrimaryClient() {
    // Given
    String metricQuery = "itemType:APPLICATION";
    String bucketField = "policyViolationThreatLevel";
    Map<String, int[]> ranges = Map.of("critical", new int[]{9, 10});
    MetricAggregationResult expectedResult = new MetricAggregationResult(100L, Map.of("critical", 25L));
    when(primaryClient.aggregateCountByField(metricQuery, bucketField, ranges)).thenReturn(expectedResult);

    // When
    MetricAggregationResult result = hybridClient.aggregateCountByField(metricQuery, bucketField, ranges);

    // Then
    assertThat(result).isSameAs(expectedResult);
    verify(primaryClient, times(1)).aggregateCountByField(metricQuery, bucketField, ranges);
    verify(secondaryClient, never()).aggregateCountByField(anyString(), anyString(), any());
  }

  @Test
  public void testCountDistinct_DelegatesToPrimaryClient() {
    long expectedCount = 42L;
    String metricQuery = "itemType:SECURITY_VULNERABILITY";
    List<String> compositeKeyFields = List.of("applicationId", "componentHash");
    when(primaryClient.countDistinct(metricQuery, compositeKeyFields)).thenReturn(expectedCount);

    long result = hybridClient.countDistinct(metricQuery, compositeKeyFields);

    assertThat(result).isEqualTo(expectedCount);
    verify(primaryClient, times(1)).countDistinct(metricQuery, compositeKeyFields);
    verify(secondaryClient, never()).countDistinct(anyString(), anyList());
  }

  @Test
  public void testCountDistinct_FallsBackToSecondary_WhenPrimaryFails() {
    String metricQuery = "itemType:SECURITY_VULNERABILITY";
    List<String> compositeKeyFields = List.of("applicationId", "componentHash");
    when(primaryClient.countDistinct(metricQuery, compositeKeyFields))
        .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.countDistinct(metricQuery, compositeKeyFields)).thenReturn(7L);

    long result = hybridClient.countDistinct(metricQuery, compositeKeyFields);

    assertThat(result).isEqualTo(7L);
    verify(primaryClient, times(1)).countDistinct(metricQuery, compositeKeyFields);
    verify(secondaryClient, times(1)).countDistinct(metricQuery, compositeKeyFields);
  }

  @Test
  public void testCountDistinct_ThrowsException_WhenBothFail() {
    String metricQuery = "itemType:SECURITY_VULNERABILITY";
    List<String> compositeKeyFields = List.of("applicationId", "componentHash");
    when(primaryClient.countDistinct(metricQuery, compositeKeyFields))
        .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.countDistinct(metricQuery, compositeKeyFields))
        .thenThrow(new RuntimeException("Secondary client error"));

    assertThatThrownBy(() -> hybridClient.countDistinct(metricQuery, compositeKeyFields))
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Distinct count failed on both primary and secondary clients");
  }

  @Test
  public void rankGroupsByMaxMetricDelegatesToPrimary() {
    RankedGroupsResult expected =
        new RankedGroupsResult(List.of(new RankedGroup("cve-2021-44228", 10.0f)), 1L, true, Map.of(), 0L);
    when(primaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of())).thenReturn(expected);

    RankedGroupsResult actual =
        hybridClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void rankGroupsByMaxMetricFallsBackToSecondaryWhenPrimaryFails() {
    RankedGroupsResult expected =
        new RankedGroupsResult(List.of(new RankedGroup("cve-2021-45046", 9.0f)), 1L, true, Map.of(), 0L);
    when(primaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .thenThrow(new SearchIndexException(new RuntimeException("primary down")));
    when(secondaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of())).thenReturn(expected);

    RankedGroupsResult actual =
        hybridClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of());

    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void rankGroupsByMaxMetricThrowsExceptionWhenBothClientsFail() {
    when(primaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .thenThrow(new RuntimeException("primary down"));
    when(secondaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .thenThrow(new RuntimeException("secondary down"));

    assertThatThrownBy(() -> hybridClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Ranked groups failed on both primary and secondary clients");
  }

  @Test
  public void rankGroupsByMaxMetricPreservesAPrimaryConflictWhenTheSecondaryAlsoFails() {
    // A conflict carries its own downstream status, so wrapping it in the generic dual-failure
    // exception would turn a retryable answer into a server error.
    ConflictException conflict = new ConflictException("index is rebuilding");
    when(primaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of())).thenThrow(conflict);
    when(secondaryClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .thenThrow(new RuntimeException("secondary down"));

    assertThatThrownBy(() -> hybridClient.rankGroupsByMaxMetric("q", "g", "m", 25, false, Map.of()))
        .isSameAs(conflict);
  }

  @Test
  public void testSearchIndex_UsesPrimaryClient() {
    // Given
    SearchResultDTO expectedResult = new SearchResultDTO();
    when(primaryClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(expectedResult);

    // When
    SearchResultDTO result = hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList());

    // Then
    assertThat(result).isSameAs(expectedResult);
    verify(primaryClient, times(1)).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
    verify(secondaryClient, never()).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(),
        anyList());
  }

  @Test
  public void testSearchIndex_FallsBackToSecondary_WhenPrimaryFails() {
    // Given
    SearchResultDTO expectedResult = new SearchResultDTO();
    when(primaryClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenReturn(expectedResult);

    // When
    SearchResultDTO result = hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList());

    // Then
    assertThat(result).isSameAs(expectedResult);
    verify(primaryClient, times(1)).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList());
    verify(secondaryClient, times(1)).searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(),
        anyList());
  }

  @Test
  public void testSearchIndex_ThrowsException_WhenBothFail() {
    // Given
    when(primaryClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.searchIndex(anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList()))
        .thenThrow(new RuntimeException("Secondary client error"));

    // When/Then
    assertThatThrownBy(() -> hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList()))
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Search failed on both primary and secondary clients");
  }

  @Test
  public void searchIndexWithIdSet_usesPrimaryClient() {
    SearchResultDTO expectedResult = new SearchResultDTO();
    List<String> ids = List.of("cve-1", "cve-2");
    List<IndexTermSetRestriction> restrictions =
        List.of(IndexTermSetRestriction.of("vulnerabilityId", ids));
    when(primaryClient.searchIndex(
        anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), eq(restrictions)))
            .thenReturn(expectedResult);

    SearchResultDTO result =
        hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList(), restrictions);

    assertThat(result).isSameAs(expectedResult);
    verify(primaryClient).searchIndex(
        "test", 10, 0, false, false, Collections.emptyList(), restrictions);
    verify(secondaryClient, never()).searchIndex(
        anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), anyList());
  }

  @Test
  public void searchIndexWithIdSet_fallsBackToSecondaryWhenPrimaryFails() {
    SearchResultDTO expectedResult = new SearchResultDTO();
    List<String> ids = List.of("cve-1");
    List<IndexTermSetRestriction> restrictions =
        List.of(IndexTermSetRestriction.of("vulnerabilityId", ids));
    when(primaryClient.searchIndex(
        anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), eq(restrictions)))
            .thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.searchIndex(
        anyString(), anyInt(), anyInt(), anyBoolean(), anyBoolean(), anyList(), eq(restrictions)))
            .thenReturn(expectedResult);

    SearchResultDTO result =
        hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList(), restrictions);

    assertThat(result).isSameAs(expectedResult);
    verify(secondaryClient).searchIndex(
        "test", 10, 0, false, false, Collections.emptyList(), restrictions);
  }

  @Test
  public void countDistinctGroupedByWithIdSet_usesPrimaryClient() {
    Map<String, Long> expected = Map.of("cve-1", 3L);
    List<String> ids = List.of("cve-1");
    List<IndexTermSetRestriction> restrictions =
        List.of(IndexTermSetRestriction.of("vulnerabilityId", ids));
    when(primaryClient.countDistinctGroupedBy(
        anyString(), anyString(), anyString(), anyCollection(), eq(restrictions)))
            .thenReturn(expected);

    Map<String, Long> result = hybridClient.countDistinctGroupedBy(
        "itemType:SECURITY_VULNERABILITY", "vulnerabilityId", "applicationPublicId", ids, restrictions);

    assertThat(result).isSameAs(expected);
    verify(secondaryClient, never()).countDistinctGroupedBy(
        anyString(), anyString(), anyString(), anyCollection(), anyList());
  }

  @Test
  public void testUpdateIndex_UpdatesBothClients() {
    // Given - create a hybrid client with AbstractSearchIndexClient mocks
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    // Mock primary client to mark changes as processed
    doAnswer(invocation -> {
      change1.setProcessed(true);
      return null;
    }).when(mockPrimaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then
    verify(mockPrimaryAbstract, times(1)).updateIndex(anyList(), any());
    verify(mockSecondaryAbstract, times(1)).updateIndex(anyList(), any());
    verify(mockPrimaryAbstract, times(1)).deleteSearchIndexChange(change1);
  }

  @Test
  public void testUpdateIndex_ContinuesWhenPrimaryUpdateFails() {
    // Given - create a hybrid client with AbstractSearchIndexClient mocks
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    doThrow(new RuntimeException("Primary update error")).when(mockPrimaryAbstract).updateIndex(anyList(), any());
    // Mock secondary client to mark changes as processed (since it succeeds)
    doAnswer(invocation -> {
      change1.setProcessed(true);
      return null;
    }).when(mockSecondaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then
    verify(mockPrimaryAbstract, times(1)).updateIndex(anyList(), any());
    verify(mockSecondaryAbstract, times(1)).updateIndex(anyList(), any());
    // Should still delete changes because secondary succeeded
    verify(mockPrimaryAbstract, times(1)).deleteSearchIndexChange(change1);
  }

  /**
   * A delegate holds its cancel flag until the rebuild it belongs to ends and clears it, so a delegate that is not
   * building must not be armed — there would be no rebuild coming to clear the flag, and its next rebuild would abort
   * on entry for a cancel that was never meant for it.
   */
  @Test
  public void testCancelFullRebuild_ArmsNeitherDelegateWhenNothingIsBuilding() {
    hybridClient.cancelFullRebuild();

    verify(primaryClient, never()).cancelFullRebuild();
    verify(secondaryClient, never()).cancelFullRebuild();
  }

  @Test
  public void testCancelFullRebuild_ArmsOnlyThePrimaryWhileThePrimaryIsBuilding() {
    doAnswer(invocation -> {
      hybridClient.cancelFullRebuild();
      return null;
    }).when(primaryClient).populateIndex();

    hybridClient.populateIndex();

    verify(primaryClient, times(1)).cancelFullRebuild();
    verify(secondaryClient, never()).cancelFullRebuild();
  }

  @Test
  public void testCancelFullRebuild_ArmsOnlyTheSecondaryWhileTheSecondaryIsBuilding() {
    doAnswer(invocation -> {
      hybridClient.cancelFullRebuild();
      return null;
    }).when(secondaryClient).populateIndex();

    hybridClient.populateIndex();

    verify(primaryClient, never()).cancelFullRebuild();
    verify(secondaryClient, times(1)).cancelFullRebuild();
  }

  /**
   * Exercises the flag against a real {@link LuceneIndexWriterOwner} rather than a mock secondary, because a mock
   * holds no flag and so cannot show the failure: a cancel during the primary phase used to arm the Lucene owner even
   * though no Lucene rebuild was running, and the skip below then meant the rebuild that would have cleared the flag
   * never ran. The following rebuild aborted on entry, leaving the Lucene fallback a generation stale.
   */
  @Test
  public void testPopulateIndex_CancelDuringThePrimaryPhaseDoesNotPoisonTheNextLuceneRebuild() throws Exception {
    File search = new File(temporaryFolder.getRoot(), "hybrid-cancel");
    Files.createDirectories(search.toPath());
    InsightWork insightWork = mock(InsightWork.class);
    lenient().when(insightWork.getSearchDir()).thenReturn(search);
    lenient().when(insightWork.getSearchIndexDir()).thenReturn(new File(search, "index"));
    lenient().when(insightWork.getSearchIndexGenerationsDir()).thenReturn(new File(search, "generations"));
    // Blue/green is left to the host filesystem rather than pinned on. The flag this test is about is cleared by
    // rebuildExclusive's finally, which both the blue/green and the in-place path run through.
    LuceneIndexWriterOwner owner =
        new LuceneIndexWriterOwner(new LuceneComponents(insightWork), mock(ShutdownHandler.class));

    try {
      // Materialises the tenant's index. requestCancelFullRebuild() resolves an existing index and no-ops without one,
      // so a fresh owner cannot be armed at all and the test would pass whatever the fan-out does.
      owner.runWithWriter(writer -> writer.addDocument(new Document()));

      AtomicInteger luceneRebuilds = new AtomicInteger();
      SearchIndexClient luceneBackedSecondary = mock(SearchIndexClient.class);
      doAnswer(invocation -> {
        owner.rebuildExclusive(writer -> {
          luceneRebuilds.incrementAndGet();
          writer.addDocument(new Document());
        });
        return null;
      }).when(luceneBackedSecondary).populateIndex();
      // Lenient because going unused is the fixed behaviour: the secondary is no longer armed for a cancel that lands
      // while the primary is building. It has to stay stubbed all the same, since it is what arms the real owner and
      // reproduces the stale fallback if the fan-out ever regresses.
      lenient().doAnswer(invocation -> {
        owner.requestCancelFullRebuild();
        return null;
      }).when(luceneBackedSecondary).cancelFullRebuild();

      SearchIndexClient cancellingPrimary = mock(SearchIndexClient.class);
      HybridSearchIndexClient hybrid = new HybridSearchIndexClient(cancellingPrimary, luceneBackedSecondary);
      AtomicBoolean cancelThisRun = new AtomicBoolean(true);
      doAnswer(invocation -> {
        if (cancelThisRun.getAndSet(false)) {
          hybrid.cancelFullRebuild();
        }
        return null;
      }).when(cancellingPrimary).populateIndex();

      hybrid.populateIndex();
      assertThat(luceneRebuilds.get()).isZero();

      hybrid.populateIndex();
      assertThat(luceneRebuilds.get()).isEqualTo(1);
    }
    finally {
      owner.close();
    }
  }

  /**
   * A cancel landing after the primary finishes and before the secondary starts reaches neither delegate — the
   * primary is done and the secondary has not begun — so the secondary would otherwise go on to rebuild work that was
   * already cancelled.
   */
  @Test
  public void testPopulateIndex_SkipsSecondaryWhenCancelledAfterPrimaryCompletes() {
    doAnswer(invocation -> {
      hybridClient.cancelFullRebuild();
      return null;
    }).when(primaryClient).populateIndex();

    hybridClient.populateIndex();

    verify(primaryClient, times(1)).populateIndex();
    verify(secondaryClient, never()).populateIndex();
  }

  /**
   * The cancel belongs to the rebuild it was requested against; the next one starts clean.
   */
  @Test
  public void testPopulateIndex_DoesNotCarryACancelIntoTheFollowingRebuild() {
    hybridClient.cancelFullRebuild();
    hybridClient.populateIndex();

    hybridClient.populateIndex();

    verify(secondaryClient, times(1)).populateIndex();
  }

  @Test
  public void testPopulateIndex_PopulatesBothClients() {
    // When
    hybridClient.populateIndex();

    // Then
    verify(primaryClient, times(1)).populateIndex();
    verify(secondaryClient, times(1)).populateIndex();
  }

  @Test
  public void testPopulateIndex_PopulatesSecondaryEvenWhenPrimarySucceeds() {
    // When
    hybridClient.populateIndex();

    // Then
    verify(primaryClient, times(1)).populateIndex();
    verify(secondaryClient, times(1)).populateIndex();
  }

  @Test
  public void testPopulateIndex_ThrowsException_WhenPrimaryFails() {
    // Given
    doThrow(new RuntimeException("Primary populate error")).when(primaryClient).populateIndex();

    // When/Then
    assertThatThrownBy(() -> hybridClient.populateIndex())
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Failed to populate primary client index");

    verify(primaryClient, times(1)).populateIndex();
    verify(secondaryClient, never()).populateIndex();
  }

  @Test
  public void testGetLastIndexTime_UsesPrimaryClient() {
    // Given
    Long expectedTime = 123456789L;
    when(primaryClient.getLastIndexTime()).thenReturn(expectedTime);

    // When
    Long result = hybridClient.getLastIndexTime();

    // Then
    assertThat(result).isEqualTo(expectedTime);
    verify(primaryClient, times(1)).getLastIndexTime();
    verify(secondaryClient, never()).getLastIndexTime();
  }

  @Test
  public void testGetLastIndexTime_FallsBackToSecondary_WhenPrimaryFails() {
    // Given
    Long expectedTime = 123456789L;
    when(primaryClient.getLastIndexTime()).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.getLastIndexTime()).thenReturn(expectedTime);

    // When
    Long result = hybridClient.getLastIndexTime();

    // Then
    assertThat(result).isEqualTo(expectedTime);
    verify(primaryClient, times(1)).getLastIndexTime();
    verify(secondaryClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetIndexSize_UsesPrimaryClient() {
    // Given
    long expectedSize = 1024L;
    when(primaryClient.getIndexSize()).thenReturn(expectedSize);

    // When
    long result = hybridClient.getIndexSize();

    // Then
    assertThat(result).isEqualTo(expectedSize);
    verify(primaryClient, times(1)).getIndexSize();
    verify(secondaryClient, never()).getIndexSize();
  }

  @Test
  public void testGetIndexSize_FallsBackToSecondary_WhenPrimaryFails() {
    // Given
    long expectedSize = 1024L;
    when(primaryClient.getIndexSize()).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.getIndexSize()).thenReturn(expectedSize);

    // When
    long result = hybridClient.getIndexSize();

    // Then
    assertThat(result).isEqualTo(expectedSize);
    verify(primaryClient, times(1)).getIndexSize();
    verify(secondaryClient, times(1)).getIndexSize();
  }

  @Test
  public void testUpdateIndex_RetriesChanges_WhenBothClientsFailAndWithinLimit() {
    // Given - create a hybrid client with AbstractSearchIndexClient mocks
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    // Create a small number of changes (well under the 10000 limit)
    List<SearchIndexChange> changes = Arrays.asList(
        new SearchIndexChange(),
        new SearchIndexChange());

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    doThrow(new RuntimeException("Primary update failed")).when(mockPrimaryAbstract).updateIndex(anyList(), any());
    doThrow(new RuntimeException("Secondary update failed")).when(mockSecondaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then - should NOT delete any changes (they will be retried since under limit)
    verify(mockPrimaryAbstract, never()).deleteSearchIndexChange(any());
  }

  @Test
  public void testUpdateIndex_DeletesAllChanges_WhenPrimarySucceeds() {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    // Mock primary client to mark changes as processed (since it succeeds)
    doAnswer(invocation -> {
      change1.setProcessed(true);
      return null;
    }).when(mockPrimaryAbstract).updateIndex(anyList(), any());
    // Primary succeeds, secondary fails
    doThrow(new RuntimeException("Secondary update failed")).when(mockSecondaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then - should delete all changes because primary succeeded
    verify(mockPrimaryAbstract).deleteSearchIndexChange(change1);
  }

  @Test
  public void testUpdateIndex_DeletesAllChanges_WhenSecondarySucceeds() {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    // Primary fails, secondary succeeds
    doThrow(new RuntimeException("Primary update failed")).when(mockPrimaryAbstract).updateIndex(anyList(), any());
    // Mock secondary client to mark changes as processed (since it succeeds)
    doAnswer(invocation -> {
      change1.setProcessed(true);
      return null;
    }).when(mockSecondaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then - should delete all changes because secondary succeeded
    verify(mockPrimaryAbstract).deleteSearchIndexChange(change1);
  }

  @Test
  public void testUpdateIndex_HandlesEmptyChangeList() {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(Collections.emptyList());

    // When
    testClient.updateIndex();

    // Then - should not attempt updates or deletions
    verify(mockPrimaryAbstract, never()).updateIndex();
    verify(mockSecondaryAbstract, never()).updateIndex();
    verify(mockPrimaryAbstract, never()).deleteSearchIndexChange(any());
  }

  @Test
  public void testPopulateIndex_ThrowsException_WhenSecondaryFails() {
    // Given
    doThrow(new RuntimeException("Secondary populate error")).when(secondaryClient).populateIndex();

    // When/Then
    assertThatThrownBy(() -> hybridClient.populateIndex())
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Failed to populate secondary client index");

    verify(primaryClient, times(1)).populateIndex();
    verify(secondaryClient, times(1)).populateIndex();
  }

  @Test
  public void testUpdateIndex_SkipsWhenPrimaryReindexing() throws Exception {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);
    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);

    // Set primaryReindexing flag to true using reflection
    Field primaryReindexingField =
        HybridSearchIndexClient.class.getDeclaredField("primaryReindexing");
    primaryReindexingField.setAccessible(true);
    AtomicBoolean primaryReindexing =
        (AtomicBoolean) primaryReindexingField.get(testClient);
    primaryReindexing.set(true);

    // When
    testClient.updateIndex();

    // Then - should skip update entirely
    verify(mockPrimaryAbstract, never()).updateIndex(anyList());
    verify(mockSecondaryAbstract, never()).updateIndex(anyList());
  }

  @Test
  public void testUpdateIndex_SkipsWhenSecondaryReindexing() throws Exception {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);
    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);

    // Set secondaryReindexing flag to true using reflection
    Field secondaryReindexingField =
        HybridSearchIndexClient.class.getDeclaredField("secondaryReindexing");
    secondaryReindexingField.setAccessible(true);
    AtomicBoolean secondaryReindexing =
        (AtomicBoolean) secondaryReindexingField.get(testClient);
    secondaryReindexing.set(true);

    // When
    testClient.updateIndex();

    // Then - should skip update entirely
    verify(mockPrimaryAbstract, never()).updateIndex(anyList());
    verify(mockSecondaryAbstract, never()).updateIndex(anyList());
  }

  @Test
  public void testUpdateIndex_ContinuesWhenDeletionFails() {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change1 = new SearchIndexChange();
    List<SearchIndexChange> changes = Arrays.asList(change1);

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    // Mock primary client to mark changes as processed
    doAnswer(invocation -> {
      change1.setProcessed(true);
      return null;
    }).when(mockPrimaryAbstract).updateIndex(anyList(), any());

    // Mock deleteSearchIndexChange to throw exception
    doThrow(new RuntimeException("Delete failed")).when(mockPrimaryAbstract).deleteSearchIndexChange(change1);

    // When - should not throw exception despite deletion failure
    testClient.updateIndex();

    // Then
    verify(mockPrimaryAbstract, times(1)).updateIndex(anyList(), any());
    verify(mockSecondaryAbstract, times(1)).updateIndex(anyList(), any());
    verify(mockPrimaryAbstract, times(1)).deleteSearchIndexChange(change1);
  }

  @Test
  public void testUpdateIndex_DeletesExcessChanges_WhenBothClientsFailAndLimitExceeded() {
    // Given - create a hybrid client with AbstractSearchIndexClient mocks to test deletion logic
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    // Create more than 10000 changes to exceed the limit
    List<SearchIndexChange> changes = new java.util.ArrayList<>();
    for (int i = 0; i < 10100; i++) {
      changes.add(new SearchIndexChange());
    }

    when(mockPrimaryAbstract.getSearchIndexChanges()).thenReturn(changes);
    doThrow(new RuntimeException("Primary update failed")).when(mockPrimaryAbstract).updateIndex(anyList(), any());
    doThrow(new RuntimeException("Secondary update failed")).when(mockSecondaryAbstract).updateIndex(anyList(), any());

    // When
    testClient.updateIndex();

    // Then - HybridSearchIndexClient.deleteSearchIndexChange is a no-op, so no actual deletions occur
    // The excess changes remain in the database for retry, but a warning is logged
    verify(mockPrimaryAbstract, times(100)).deleteSearchIndexChange(any());
  }

  @Test
  public void testGetLastIndexTime_ReturnsNull_WhenBothFail() {
    // Given
    when(primaryClient.getLastIndexTime()).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.getLastIndexTime()).thenThrow(new RuntimeException("Secondary client error"));

    // When
    Long result = hybridClient.getLastIndexTime();

    // Then
    assertThat(result).isNull();
    verify(primaryClient, times(1)).getLastIndexTime();
    verify(secondaryClient, times(1)).getLastIndexTime();
  }

  @Test
  public void testGetIndexSize_ReturnsZero_WhenBothFail() {
    // Given
    when(primaryClient.getIndexSize()).thenThrow(new RuntimeException("Primary client error"));
    when(secondaryClient.getIndexSize()).thenThrow(new RuntimeException("Secondary client error"));

    // When
    long result = hybridClient.getIndexSize();

    // Then
    assertThat(result).isEqualTo(0L);
    verify(primaryClient, times(1)).getIndexSize();
    verify(secondaryClient, times(1)).getIndexSize();
  }

  @Test
  public void testGetSearchIndexChanges_DelegatesToPrimary() {
    // Given
    SearchIndexChange change1 = new SearchIndexChange();
    SearchIndexChange change2 = new SearchIndexChange();
    List<SearchIndexChange> expectedChanges = Arrays.asList(change1, change2);
    when(primaryClient.getSearchIndexChanges()).thenReturn(expectedChanges);

    // When
    List<SearchIndexChange> result = hybridClient.getSearchIndexChanges();

    // Then
    assertThat(result).isSameAs(expectedChanges);
    verify(primaryClient, times(1)).getSearchIndexChanges();
    verify(secondaryClient, never()).getSearchIndexChanges();
  }

  @Test
  public void testGetPrimaryClient() {
    // When
    SearchIndexClient result = hybridClient.getPrimaryClient();

    // Then
    assertThat(result).isSameAs(primaryClient);
  }

  @Test
  public void testGetSecondaryClient() {
    // When
    SearchIndexClient result = hybridClient.getSecondaryClient();

    // Then
    assertThat(result).isSameAs(secondaryClient);
  }

  @Test
  public void testDeleteSearchIndexChange_DelegatesToPrimary() {
    // Given
    AbstractSearchIndexClient mockPrimaryAbstract = mock(AbstractSearchIndexClient.class);
    AbstractSearchIndexClient mockSecondaryAbstract = mock(AbstractSearchIndexClient.class);
    HybridSearchIndexClient testClient = new HybridSearchIndexClient(mockPrimaryAbstract, mockSecondaryAbstract);

    SearchIndexChange change = new SearchIndexChange();

    // When - call the overridden deleteSearchIndexChange
    testClient.deleteSearchIndexChange(change);

    // Then - should delegate to the primary client
    verify(mockPrimaryAbstract).deleteSearchIndexChange(any());
    verify(mockSecondaryAbstract, never()).deleteSearchIndexChange(any());
  }

  // ---- searchGlobal -------------------------------------------------------------------------

  private static GlobalSearchRequest globalSearchRequest(final List<String> searchAfter) {
    return new GlobalSearchRequest(new MatchAllDocsQuery(), null, 25, searchAfter);
  }

  private static GlobalSearchResult globalSearchResult() {
    return new GlobalSearchResult(List.<SearchResultItemDTO>of(), 0L, List.of());
  }

  @Test
  public void searchGlobal_primarySucceeds_noFallback() {
    GlobalSearchResult expected = globalSearchResult();
    when(primaryClient.searchGlobal(any())).thenReturn(expected);

    GlobalSearchResult actual = hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList()));

    assertThat(actual).isSameAs(expected);
    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, never()).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryInfraFail_noCursor_fallsBackToSecondary() {
    // The secondary's page is re-wrapped (not returned verbatim) so it can be pinned to the
    // secondary's backendId; assert the payload is preserved rather than object identity.
    GlobalSearchResult secondaryResult =
        new GlobalSearchResult(List.<SearchResultItemDTO>of(), 7L, List.of(), false);
    when(primaryClient.searchGlobal(any())).thenThrow(new RuntimeException("boom"));
    when(secondaryClient.searchGlobal(any())).thenReturn(secondaryResult);
    when(secondaryClient.backendId()).thenReturn("lucene");

    GlobalSearchResult actual = hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList()));

    assertThat(actual.rows()).isEqualTo(secondaryResult.rows());
    assertThat(actual.totalHits()).isEqualTo(7L);
    assertThat(actual.exactTotalHits()).isFalse();
    assertThat(actual.servingBackendId()).isEqualTo("lucene");
    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, times(1)).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryInfraFail_page1FallsBackToSecondary_pinsCursorToSecondaryBackend() {
    // Page 1 (empty cursor): primary fails, Hybrid serves from the secondary. The result must be
    // pinned to the secondary's backendId so the next-page cursor minted from it carries the
    // secondary's generation token. If the primary recovers before page 2 (cursor in flight, which
    // Hybrid routes to the primary), the primary-expected token will not match and the follow-up
    // cursor is rejected as stale instead of being silently mis-paginated by the primary.
    GlobalSearchResult secondaryResult =
        new GlobalSearchResult(List.<SearchResultItemDTO>of(), 3L, List.of("acme-prod"));
    when(primaryClient.searchGlobal(any())).thenThrow(new RuntimeException("boom"));
    when(secondaryClient.searchGlobal(any())).thenReturn(secondaryResult);
    when(secondaryClient.backendId()).thenReturn("lucene");

    GlobalSearchResult actual = hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList()));

    assertThat(actual.servingBackendId()).isEqualTo("lucene");
    assertThat(actual.nextSearchAfter()).containsExactly("acme-prod");
    assertThat(actual.totalHits()).isEqualTo(3L);
    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, times(1)).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryInfraFail_withCursor_throwsStaleCursorException() {
    when(primaryClient.searchGlobal(any())).thenThrow(new RuntimeException("boom"));

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(List.of("0.7", "3"))))
        .isInstanceOf(StaleCursorException.class);

    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, never()).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryBadRequest_surfacesWithoutFallback() {
    when(primaryClient.searchGlobal(any())).thenThrow(new BadRequestException("bad"));

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList())))
        .isInstanceOf(BadRequestException.class);

    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, never()).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryConflict_surfacesWithoutFallback() {
    when(primaryClient.searchGlobal(any())).thenThrow(new ConflictException("conflict"));

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList())))
        .isInstanceOf(ConflictException.class);

    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, never()).searchGlobal(any());
  }

  @Test
  public void searchGlobal_primaryStaleCursor_surfacesWithoutFallback() {
    // A StaleCursorException from the primary maps downstream to HTTP 410; it must never be masked
    // by falling back to the secondary, even with an empty cursor on the request.
    when(primaryClient.searchGlobal(any())).thenThrow(new StaleCursorException("stale"));

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList())))
        .isInstanceOf(StaleCursorException.class);

    verify(primaryClient, times(1)).searchGlobal(any());
    verify(secondaryClient, never()).searchGlobal(any());
  }

  @Test
  public void searchGlobal_bothFail_wrappedInSearchIndexExceptionWithSuppressedPrimary() {
    RuntimeException primaryFail = new RuntimeException("primary boom");
    RuntimeException secondaryFail = new RuntimeException("secondary boom");
    when(primaryClient.searchGlobal(any())).thenThrow(primaryFail);
    when(secondaryClient.searchGlobal(any())).thenThrow(secondaryFail);

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList())))
        .isInstanceOf(SearchIndexException.class)
        .satisfies(t -> {
          assertThat(t.getCause()).isSameAs(secondaryFail);
          assertThat(t.getSuppressed()).containsExactly(primaryFail);
        });
  }

  @Test
  public void searchGlobal_secondaryStaleCursor_surfacesWithSuppressedPrimary() {
    RuntimeException primaryFail = new RuntimeException("primary boom");
    StaleCursorException secondaryStale = new StaleCursorException("stale");
    when(primaryClient.searchGlobal(any())).thenThrow(primaryFail);
    when(secondaryClient.searchGlobal(any())).thenThrow(secondaryStale);

    assertThatThrownBy(() -> hybridClient.searchGlobal(globalSearchRequest(Collections.emptyList())))
        .isInstanceOf(StaleCursorException.class)
        .satisfies(t -> assertThat(t.getSuppressed()).containsExactly(primaryFail));
  }
}
