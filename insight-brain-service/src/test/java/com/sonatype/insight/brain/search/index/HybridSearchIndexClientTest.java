/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.results.SearchResultDTO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
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

  private HybridSearchIndexClient hybridClient;

  @Before
  public void setUp() {
    hybridClient = new HybridSearchIndexClient(primaryClient, secondaryClient);
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
}
