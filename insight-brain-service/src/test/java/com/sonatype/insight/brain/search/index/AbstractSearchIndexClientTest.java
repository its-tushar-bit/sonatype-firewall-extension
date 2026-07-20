/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.index;

import java.io.IOException;
import java.net.ConnectException;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import jakarta.inject.Inject;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class AbstractSearchIndexClientTest
    extends AbstractComponentTest
{
  @Inject
  private SearchIndexChangeDAO searchIndexChangeDAO;

  private TestSearchIndexClient client;

  @Before
  public void setup() throws Exception {
    client = spy(new TestSearchIndexClient());
  }

  @Test
  public void describeSort_relevanceSort_rendersReadableToken() {
    Sort sort = new Sort(SortField.FIELD_SCORE, new SortField("documentKey", SortField.Type.STRING));
    assertThat(AbstractSearchIndexClient.describeSort(sort)).isEqualTo("relevance,documentKey");
  }

  @Test
  public void testProcessSearchIndexChanges_ParseExceptionIsSwallowed() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("test-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    doThrow(new IOException(new ParseException("Parse error"))).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    assertThatCode(
        () -> client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
        })).doesNotThrowAnyException();
  }

  @Test
  public void testProcessSearchIndexChanges_WrappedParseExceptionIsSwallowed() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("test-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    IOException wrapped = new IOException("Wrapped", new ParseException("Parse error"));
    doThrow(wrapped).when(client).updateIndex(any(SearchIndexChange.class), any());

    assertThatCode(
        () -> client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
        })).doesNotThrowAnyException();
  }

  @Test
  public void testProcessSearchIndexChanges_NonParseExceptionIsSwallowed() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("test-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("test-data");
    IOException nonParseException = new IOException("Other error");
    doThrow(nonParseException).when(client).updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
      });
    });
    // Try 3 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
      });
    });

    // Try 4 - should skip updateIndex due to max failures (3 failures recorded)
    client.processSearchIndexChanges(Collections.singletonList(change), null, searchIndexChange -> {
      searchIndexChange.setProcessed(true);
    });

    assertThat(change.isProcessed()).isTrue();
    // updateIndex should have been called exactly 3 times (the 4th was skipped)
    verify(client, times(3)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_TooManyConsecutiveFailuresAborts() throws Exception {
    SearchIndexChange change1 = new SearchIndexChange();
    change1.setId("id1");
    change1.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change1.setChangeData("data1");
    SearchIndexChange change2 = new SearchIndexChange();
    change2.setId("id2");
    change2.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change2.setChangeData("data2");
    SearchIndexChange change3 = new SearchIndexChange();
    change3.setId("id3");
    change3.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change3.setChangeData("data3");
    SearchIndexChange change4 = new SearchIndexChange();
    change4.setId("id4");
    change4.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change4.setChangeData("data4");
    SearchIndexChange change5 = new SearchIndexChange();
    change5.setId("id5");
    change5.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change5.setChangeData("data5");

    doThrow(new IOException("IO error")).when(client).updateIndex(any(SearchIndexChange.class), any());

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> client.processSearchIndexChanges(List.of(change1, change2, change3, change4, change5), null,
            searchIndexChange -> {
            }));
  }

  @Test
  public void testProcessSearchIndexChanges_SkipsBadChangeAfterTooManyFailures() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("bad-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("bad-data");

    doThrow(new IOException("Persistent error")).when(client).updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 3 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    // Try 4 - should skip updateIndex due to max failures (3 failures recorded)
    client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      c.setProcessed(true);
    });

    assertThat(change.isProcessed()).isTrue();
    // updateIndex should have been called exactly 3 times (the 4th was skipped)
    verify(client, times(3)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_SystemicExceptionDoesntCountAgainstChange() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("good-change-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("good-data");

    // Systemic exception (connectivity issue)
    doThrow(new ConnectException("Connection refused")).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 3 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    // The change should NOT be marked as processed since it's a systemic issue
    assertThat(change.isProcessed()).isFalse();
    // updateIndex should have been called 3 times (never skipped because systemic exceptions don't count toward
    // per-change failure limit)
    verify(client, times(3)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_SystemicExceptionAbortsAfter5Consecutive() throws Exception {
    SearchIndexChange change1 = new SearchIndexChange();
    change1.setId("id1");
    change1.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change1.setChangeData("data1");
    SearchIndexChange change2 = new SearchIndexChange();
    change2.setId("id2");
    change2.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change2.setChangeData("data2");
    SearchIndexChange change3 = new SearchIndexChange();
    change3.setId("id3");
    change3.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change3.setChangeData("data3");
    SearchIndexChange change4 = new SearchIndexChange();
    change4.setId("id4");
    change4.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change4.setChangeData("data4");
    SearchIndexChange change5 = new SearchIndexChange();
    change5.setId("id5");
    change5.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change5.setChangeData("data5");

    // Systemic exception
    doThrow(new ConnectException("Connection refused")).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    assertThatExceptionOfType(IOException.class)
        .isThrownBy(() -> client.processSearchIndexChanges(
            List.of(change1, change2, change3, change4, change5), null, searchIndexChange -> {
            }));
  }

  @Test
  public void testProcessSearchIndexChanges_ChangeSpecificExceptionSkipsImmediately() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("bad-data-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("bad-data");

    // Change-specific exception (parse error)
    doThrow(new IOException(new ParseException("Invalid data"))).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should skip immediately
    client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      c.setProcessed(true);
    });

    // The change should be marked as processed (skipped)
    assertThat(change.isProcessed()).isTrue();
    // updateIndex should have been called only once (then skipped permanently)
    verify(client, times(1)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_UnknownExceptionCountsAgainstChange() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("unknown-error-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("unknown-error-data");

    // Unknown exception type
    doThrow(new IOException("Unknown error type")).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 3 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    // Try 4 - should skip updateIndex due to max failures (3 failures recorded)
    client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      c.setProcessed(true);
    });

    assertThat(change.isProcessed()).isTrue();
    // updateIndex should have been called exactly 3 times (the 4th was skipped)
    verify(client, times(3)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_NetworkExceptionsAreSystemic() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("network-error-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("network-data");

    // Test UnknownHostException
    doThrow(new UnknownHostException("opensearch.example.com")).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 3 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    assertThat(change.isProcessed()).isFalse();
    verify(client, times(3)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_SocketExceptionIsSystemic() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("socket-error-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("socket-data");

    // Test SocketException
    doThrow(new SocketException("Network is unreachable")).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    assertThat(change.isProcessed()).isFalse();
    verify(client, times(2)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_TimeoutExceptionsAreSystemic() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("timeout-error-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("timeout-data");

    // Test SocketTimeoutException
    doThrow(new IOException(new SocketTimeoutException("Read timed out"))).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Try 1 - should throw because batch size is 1 and maxConsecutiveFailures = 1
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });
    // Try 2 - should throw again
    assertThatExceptionOfType(IOException.class).isThrownBy(() -> {
      client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      });
    });

    assertThat(change.isProcessed()).isFalse();
    verify(client, times(2)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_IllegalArgumentExceptionIsChangeSpecific() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("illegal-arg-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("bad-arg-data");

    // Test IllegalArgumentException
    doThrow(new IOException(new IllegalArgumentException("Invalid field value"))).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Should skip immediately (change-specific error)
    client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      c.setProcessed(true);
    });

    assertThat(change.isProcessed()).isTrue();
    verify(client, times(1)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testProcessSearchIndexChanges_NullPointerExceptionIsChangeSpecific() throws Exception {
    SearchIndexChange change = new SearchIndexChange();
    change.setId("npe-id");
    change.setChangeType(SearchIndexChange.ChangeType.APPLICATION);
    change.setChangeData("null-data");

    // Test NullPointerException
    doThrow(new IOException(new NullPointerException("Null field"))).when(client)
        .updateIndex(any(SearchIndexChange.class), any());

    // Should skip immediately (change-specific error)
    client.processSearchIndexChanges(Collections.singletonList(change), null, c -> {
      c.setProcessed(true);
    });

    assertThat(change.isProcessed()).isTrue();
    verify(client, times(1)).updateIndex(any(SearchIndexChange.class), any());
  }

  @Test
  public void testShouldThrow_ExceptionThatShouldAlwaysThrow() {
    assertThat(AbstractSearchIndexClient.shouldThrow(
        new NullPointerException(),
        e -> e instanceof ConnectException,
        new AtomicLong(System.currentTimeMillis()), // It just threw already
        new AtomicReference<>(Duration.ofSeconds(30)),
        Duration.ofSeconds(30),
        Duration.ofMinutes(10))).isTrue();
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_WithinCooldown() {
    assertThat(AbstractSearchIndexClient.shouldThrow(
        new ConnectException(),
        e -> e instanceof ConnectException,
        new AtomicLong(System.currentTimeMillis()), // It just threw already
        new AtomicReference<>(Duration.ofSeconds(30)),
        Duration.ofSeconds(30),
        Duration.ofMinutes(10))).isFalse();
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_OutsideCooldown() {
    AtomicReference<Duration> cooldown = new AtomicReference<>(Duration.ofSeconds(30));
    // Cooldown has expired
    AtomicLong lastRecordedExceptionEpochMs = new AtomicLong(System.currentTimeMillis() - cooldown.get().toMillis());
    assertThat(AbstractSearchIndexClient.shouldThrow(
        new ConnectException(),
        e -> e instanceof ConnectException,
        lastRecordedExceptionEpochMs,
        cooldown,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10))).isTrue();
    // Cooldown should be increased by x2
    assertThat(cooldown.get()).isEqualTo(Duration.ofMinutes(1));
  }

  @Test
  public void testShouldThrow_ExceptionWithCooldown_CantIncreaseCooldownBeyondMax() {
    AtomicReference<Duration> cooldown = new AtomicReference<>(Duration.ofMinutes(9));
    // Cooldown has expired
    AtomicLong lastRecordedExceptionEpochMs = new AtomicLong(System.currentTimeMillis() - cooldown.get().toMillis());
    assertThat(AbstractSearchIndexClient.shouldThrow(
        new ConnectException(),
        e -> e instanceof ConnectException,
        lastRecordedExceptionEpochMs,
        cooldown,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10))).isTrue();
    // Cooldown should be increased to max
    assertThat(cooldown.get()).isEqualTo(Duration.ofMinutes(10));
  }

  @Test
  public void testShouldThrow_CooldownResetsAfterLongPeriodWithoutErrors() {
    AtomicReference<Duration> cooldown = new AtomicReference<>(Duration.ofMinutes(5));
    long longTimeAgo = System.currentTimeMillis() - Duration.ofMinutes(15).toMillis();
    AtomicLong lastRecordedExceptionEpochMs = new AtomicLong(longTimeAgo);

    assertThat(AbstractSearchIndexClient.shouldThrow(
        new ConnectException(),
        e -> e instanceof ConnectException,
        lastRecordedExceptionEpochMs,
        cooldown,
        Duration.ofSeconds(30),
        Duration.ofMinutes(10))).isTrue();
    assertThat(cooldown.get()).isEqualTo(Duration.ofSeconds(30));
  }

  private class TestSearchIndexClient
      extends AbstractSearchIndexClient
  {
    public TestSearchIndexClient() {
      super(null, null, null, null, null, searchIndexChangeDAO, null, null, null, null, null, null, null, null,
          null, null, null, null, null);
    }

    @Override
    protected void updateMaxQueryClauseCount() {
    }

    @Override
    protected void updateIndex(SearchIndexChange change, IndexingContext indexingContext) throws IOException {
    }

    @Override
    public SearchResultDTO searchIndex(
        String searchQuery,
        int pageSize,
        int page,
        boolean allComponents,
        boolean isSbomManagerMode,
        List<String> searchAfter)
    {
      return null;
    }

    @Override
    public void populateIndex() {
    }

    @Override
    public void updateIndex(
        final List<SearchIndexChange> searchIndexChanges,
        final Consumer<SearchIndexChange> deletionCallback)
    {

    }

    @Override
    public void updateIndex() {
    }

    @Override
    public Long getLastIndexTime() {
      return null;
    }

    @Override
    public long getIndexSize() {
      return 0;
    }

    @Override
    protected boolean isChangeSpecificError(final Exception e) {
      // Use common change-specific errors for testing
      return isCommonChangeSpecificError(e);
    }

    @Override
    protected boolean isSystemicError(final Exception e) {
      // Simulate network/connectivity errors for testing (mimics OpenSearch behavior)
      return hasCauseOrMessage(e, cause -> cause instanceof UnknownHostException || cause instanceof SocketException ||
          cause instanceof SocketTimeoutException || cause instanceof TimeoutException);
    }

    @Override
    public boolean shouldThrow(final Exception e) {
      // For testing, always throw (no cooldown)
      return true;
    }

    @Override
    public long count(String metricQuery) {
      return 0L;
    }

    @Override
    public MetricAggregationResult aggregateCountByField(
        String metricQuery,
        String bucketField,
        Map<String, int[]> ranges)
    {
      return new MetricAggregationResult(0L, Map.of());
    }

    @Override
    public long countDistinct(String metricQuery, List<String> compositeKeyFields) {
      return 0L;
    }
  }
}
