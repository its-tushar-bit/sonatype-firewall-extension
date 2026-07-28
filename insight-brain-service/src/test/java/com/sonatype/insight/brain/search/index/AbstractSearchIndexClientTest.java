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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.SearchIndexChangeDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.policy.AutoPolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyWaiverDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.SearchIndexChange;
import com.sonatype.insight.brain.model.policy.AutoPolicyWaiver;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyWaiver;
import com.sonatype.insight.brain.search.ConversionHelper;
import com.sonatype.insight.brain.search.lucene.DocumentBuilderHelper;
import com.sonatype.insight.brain.search.results.SearchResultDTO;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import jakarta.inject.Inject;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.UUID;

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

  @Inject
  private PolicyWaiverDAO policyWaiverDAO;

  @Inject
  private AutoPolicyWaiverDAO autoPolicyWaiverDAO;

  @Inject
  private DocumentBuilderHelper documentBuilderHelper;

  @Inject
  private OwnerDAO ownerDAO;

  @Inject
  private ApplicationDAO applicationDAO;

  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private PolicyDAO policyDAO;

  @Inject
  private LabelDAO labelDAO;

  @Inject
  private TagDAO tagDAO;

  @Inject
  private ConversionHelper conversionHelper;

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

  @Test
  public void getGroupFieldName_policyWaiver_returnsPolicyName() {
    assertThat(client.groupFieldNameFor(ItemType.POLICY_WAIVER, Collections.emptySet()))
        .isEqualTo(FieldIdentifier.POLICY_WAIVER_POLICY_NAME);
  }

  @Test
  public void getGroupFieldName_everyItemType_doesNotThrow() {
    for (ItemType itemType : ItemType.values()) {
      assertThatCode(() -> client.groupFieldNameFor(itemType, Collections.emptySet()))
          .doesNotThrowAnyException();
    }
  }

  @Test
  public void updateIndexForPolicyWaiver_manualHit_deletesOldDocAndAddsManualDoc() throws Exception {
    TestSearchIndexClient indexingClient =
        new TestSearchIndexClient(policyWaiverDAO, autoPolicyWaiverDAO, documentBuilderHelper, ownerDAO);
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.POLICY_WAIVER,
            SearchIndexChange.POLICY_WAIVER_MANUAL_PREFIX + waiver.getId()),
        ctx);

    assertThat(ctx.deleteQueries)
        .containsExactly(FieldIdentifier.POLICY_WAIVER_ID.label + ":" + waiver.getId());
    assertThat(ctx.addedDocs).hasSize(1);
    Document doc = ctx.addedDocs.get(0);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)).isEqualTo(waiver.getId());
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("my policy");
  }

  @Test
  public void updateIndexForPolicyWaiver_autoHit_deletesOldDocAndAddsAutoDoc() throws Exception {
    TestSearchIndexClient indexingClient =
        new TestSearchIndexClient(policyWaiverDAO, autoPolicyWaiverDAO, documentBuilderHelper, ownerDAO);
    Organization organization = tempEntity.newOrganization();
    AutoPolicyWaiver autoWaiver = tempEntity.newAutoPolicyWaiver(organization.getId(), 9, true, false);
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.POLICY_WAIVER,
            SearchIndexChange.POLICY_WAIVER_AUTO_PREFIX + autoWaiver.getId()),
        ctx);

    assertThat(ctx.deleteQueries)
        .containsExactly(FieldIdentifier.POLICY_WAIVER_ID.label + ":" + autoWaiver.getId());
    assertThat(ctx.addedDocs).hasSize(1);
    Document doc = ctx.addedDocs.get(0);
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)).isEqualTo(autoWaiver.getId());
    // Auto-waivers carry no indexed policy name; the display title is synthesized on the read side.
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isNull();
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_AUTO.label)).isEqualTo("true");
    assertThat(doc.get(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label)).isEqualTo("9");
  }

  @Test
  public void updateIndexForPolicyWaiver_neitherResolves_deletesOnlyNoAdd() throws Exception {
    TestSearchIndexClient indexingClient =
        new TestSearchIndexClient(policyWaiverDAO, autoPolicyWaiverDAO, documentBuilderHelper, ownerDAO);
    String missingId = UUID.randomUUID().toString();
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.POLICY_WAIVER,
            SearchIndexChange.POLICY_WAIVER_MANUAL_PREFIX + missingId),
        ctx);

    assertThat(ctx.deleteQueries)
        .containsExactly(FieldIdentifier.POLICY_WAIVER_ID.label + ":" + missingId);
    assertThat(ctx.addedDocs).isEmpty();
  }

  @Test
  public void updateIndexForPolicyWaiver_manualFlippedToContainerImage_deletesOnlyNoAdd() throws Exception {
    TestSearchIndexClient indexingClient =
        new TestSearchIndexClient(policyWaiverDAO, autoPolicyWaiverDAO, documentBuilderHelper, ownerDAO);
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");
    waiver.setForContainerImage(true);
    try (var tx = policyWaiverDAO.createTransactionContext()) {
      tx.begin();
      policyWaiverDAO.updateForRenewal(tx, waiver);
      tx.commit();
    }
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.POLICY_WAIVER,
            SearchIndexChange.POLICY_WAIVER_MANUAL_PREFIX + waiver.getId()),
        ctx);

    // buildDocument filters container-image waivers to null, so the stale doc is deleted with no re-add.
    assertThat(ctx.deleteQueries)
        .containsExactly(FieldIdentifier.POLICY_WAIVER_ID.label + ":" + waiver.getId());
    assertThat(ctx.addedDocs).isEmpty();
  }

  @Test
  public void appendSbomFilteringToQuery_excludesPolicyWaiverInBothModes() {
    String defaultMode = client.appendSbomFilteringToQuery("foo", false);
    String sbomMode = client.appendSbomFilteringToQuery("foo", true);
    String exclusion = "AND NOT itemType:" + ItemType.POLICY_WAIVER.searchFieldName();
    String requestExclusion = "AND NOT itemType:" + ItemType.POLICY_WAIVER_REQUEST.searchFieldName();

    assertThat(defaultMode).contains(exclusion).contains(requestExclusion);
    assertThat(sbomMode).contains(exclusion).contains(requestExclusion);
  }

  @Test
  public void updateIndexForApplication_rebuildsWaiversThatCascadeDeleteRemoved() throws Exception {
    TestSearchIndexClient indexingClient = new TestSearchIndexClient(applicationDAO, labelDAO, organizationDAO,
        ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO, tagDAO, documentBuilderHelper);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), application.getId(), "comment");
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.APPLICATION, application.getId()), ctx);

    // The APPLICATION_ID cascade delete sweeps the app's waiver docs, so the update must re-add them.
    assertThat(ctx.deleteQueries)
        .contains(FieldIdentifier.APPLICATION_ID.label + ":" + application.getId());
    assertThat(ctx.addedDocs)
        .anyMatch(doc -> waiver.getId().equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)));
  }

  @Test
  public void updateIndexForOrganization_rebuildsWaiversThatCascadeDeleteRemoved() throws Exception {
    TestSearchIndexClient indexingClient = new TestSearchIndexClient(applicationDAO, labelDAO, organizationDAO,
        ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO, tagDAO, documentBuilderHelper);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver orgWaiver = tempEntity.newWaiver("h1", policy.getId(), organization.getId(), "org");
    PolicyWaiver appWaiver = tempEntity.newWaiver("h2", policy.getId(), application.getId(), "app");
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.ORGANIZATION, organization.getId()), ctx);

    assertThat(ctx.addedDocs)
        .anyMatch(doc -> orgWaiver.getId().equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)))
        .anyMatch(doc -> appWaiver.getId().equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)));
  }

  @Test
  public void updateIndexForOrganization_deletesAppScopedWaiverDocsBeforeReadd() throws Exception {
    TestSearchIndexClient indexingClient = new TestSearchIndexClient(applicationDAO, labelDAO, organizationDAO,
        ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO, tagDAO, documentBuilderHelper);
    Organization organization = tempEntity.newOrganization();
    Application application = tempEntity.newApplication(organization.getId());
    Policy policy = tempEntity.newPolicy(organization.getId(), "my policy", 8);
    PolicyWaiver appWaiver = tempEntity.newWaiver("h1", policy.getId(), application.getId(), "app");
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.ORGANIZATION, organization.getId()), ctx);

    // The ORGANIZATION_ID cascade delete does NOT sweep app-scoped waiver docs (they are keyed only
    // by APPLICATION_ID), so the org update must delete them per app before re-adding, otherwise the
    // re-add leaves a duplicate POLICY_WAIVER doc for the app on every org index change.
    String expectedAppWaiverDelete = "(" +
        FieldIdentifier.APPLICATION_ID.label + ":" + application.getId() +
        " AND " +
        FieldIdentifier.ITEM_TYPE.label + ":" + ItemType.POLICY_WAIVER.searchFieldName() +
        ")";
    assertThat(ctx.deleteQueries).contains(expectedAppWaiverDelete);
    assertThat(ctx.addedDocs)
        .filteredOn(doc -> appWaiver.getId().equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)))
        .hasSize(1);
  }

  @Test
  public void updateIndexForPolicy_rebuildsReferencingWaiversWithNewNameAndThreat() throws Exception {
    TestSearchIndexClient indexingClient = new TestSearchIndexClient(applicationDAO, labelDAO, organizationDAO,
        ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO, tagDAO, documentBuilderHelper);
    Organization organization = tempEntity.newOrganization();
    Policy policy = tempEntity.newPolicy(organization.getId(), "old name", 3);
    PolicyWaiver waiver = tempEntity.newWaiver("hash", policy.getId(), organization.getId(), "comment");

    policy.setName("new name");
    policy.setThreatLevel(7);
    try (var tx = policyDAO.createTransactionContext()) {
      tx.begin();
      policyDAO.update(tx, policy);
      tx.commit();
    }
    RecordingIndexingContext ctx = new RecordingIndexingContext();

    indexingClient.runUpdateIndex(
        new SearchIndexChange(SearchIndexChange.ChangeType.POLICY, policy.getId()), ctx);

    // Waiver docs key on POLICY_WAIVER_POLICY_ID, so the rename must delete + re-add them fresh.
    assertThat(ctx.deleteQueries)
        .contains(FieldIdentifier.POLICY_WAIVER_POLICY_ID.label + ":" + policy.getId());
    Document waiverDoc = ctx.addedDocs.stream()
        .filter(doc -> waiver.getId().equals(doc.get(FieldIdentifier.POLICY_WAIVER_ID.label)))
        .findFirst()
        .orElse(null);
    assertThat(waiverDoc).isNotNull();
    assertThat(waiverDoc.get(FieldIdentifier.POLICY_WAIVER_POLICY_NAME.label)).isEqualTo("new name");
    assertThat(waiverDoc.getField(FieldIdentifier.POLICY_WAIVER_THREAT_LEVEL.label).numericValue().intValue())
        .isEqualTo(7);
  }

  private class TestSearchIndexClient
      extends AbstractSearchIndexClient
  {
    FieldIdentifier groupFieldNameFor(final ItemType itemType, final java.util.Set<String> fieldNames) {
      return getGroupFieldName(itemType, fieldNames);
    }

    public TestSearchIndexClient() {
      super(null, null, null, null, null, null, null, searchIndexChangeDAO, null, null, null, null, null, null, null,
          null, null, null, null, null, null, null);
    }

    // Wires the real waiver DAOs, document builder, and owner DAO so runUpdateIndex exercises the
    // genuine incremental waiver path against the test database.
    TestSearchIndexClient(
        final PolicyWaiverDAO policyWaiverDAO,
        final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
        final DocumentBuilderHelper documentBuilderHelper,
        final OwnerDAO ownerDAO)
    {
      super(null, null, null, ownerDAO, null, policyWaiverDAO, autoPolicyWaiverDAO, searchIndexChangeDAO, null, null,
          documentBuilderHelper, null, null, null, null, null, null, null, null, null, null, null);
    }

    // Wires the app/org/policy/label/tag DAOs so the app/org cascade and policy rebuild paths run
    // against the test database (Fix C/D: waivers survive cascades and follow policy renames).
    TestSearchIndexClient(
        final ApplicationDAO applicationDAO,
        final LabelDAO labelDAO,
        final OrganizationDAO organizationDAO,
        final OwnerDAO ownerDAO,
        final PolicyDAO policyDAO,
        final PolicyWaiverDAO policyWaiverDAO,
        final AutoPolicyWaiverDAO autoPolicyWaiverDAO,
        final TagDAO tagDAO,
        final DocumentBuilderHelper documentBuilderHelper)
    {
      super(applicationDAO, labelDAO, organizationDAO, ownerDAO, policyDAO, policyWaiverDAO, autoPolicyWaiverDAO,
          searchIndexChangeDAO, tagDAO, null, documentBuilderHelper, null, null, null, null, null, null, null, null,
          null, null, null);
    }

    // Bridge to the real protected updateIndex(change, ctx); the override below stubs it for the
    // error-handling tests, so callers that want the genuine path use this instead.
    void runUpdateIndex(final SearchIndexChange change, final IndexingContext indexingContext) throws IOException {
      super.updateIndex(change, indexingContext);
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
    public MetricAggregationResult aggregateCountByFloatField(
        String metricQuery,
        String bucketField,
        Map<String, float[]> ranges,
        String distinctField)
    {
      return new MetricAggregationResult(0L, Map.of());
    }

    @Override
    public long countDistinct(String metricQuery, List<String> compositeKeyFields) {
      return 0L;
    }

    @Override
    public Map<String, Long> countDistinctGroupedBy(
        String metricQuery,
        String groupField,
        String distinctField,
        Collection<String> groupValues)
    {
      return Map.of();
    }
  }

  /**
   * Records the delete query and added documents so the incremental waiver-indexing branches can be
   * asserted without a real Lucene/OpenSearch backend.
   */
  private class RecordingIndexingContext
      extends IndexingContext
  {
    final List<String> deleteQueries = new ArrayList<>();

    final List<Document> addedDocs = new ArrayList<>();

    RecordingIndexingContext() {
      super(ownerDAO, conversionHelper);
    }

    @Override
    public void deleteDocuments(final String query) {
      deleteQueries.add(query);
    }

    @Override
    public void addDocuments(final List<Document> documents) {
      addedDocs.addAll(documents);
    }
  }

}
