/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.util.Collections;

import com.sonatype.insight.brain.search.results.SearchResultDTO;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
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
    assertThatThrownBy(() ->
        hybridClient.searchIndex("test", 10, 0, false, false, Collections.emptyList()))
        .isInstanceOf(SearchIndexException.class)
        .hasMessageContaining("Search failed on both primary and secondary clients");
  }

  @Test
  public void testUpdateIndex_UpdatesBothClients() {
    // When
    hybridClient.updateIndex();

    // Then
    verify(primaryClient, times(1)).updateIndex();
    verify(secondaryClient, times(1)).updateIndex();
  }

  @Test
  public void testUpdateIndex_ContinuesWhenPrimaryUpdateFails() {
    // Given
    doThrow(new RuntimeException("Primary update error")).when(primaryClient).updateIndex();

    // When
    hybridClient.updateIndex();

    // Then
    verify(primaryClient, times(1)).updateIndex();
    verify(secondaryClient, times(1)).updateIndex();
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
}
