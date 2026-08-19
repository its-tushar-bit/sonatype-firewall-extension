/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.relay;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.relay.RelayEventLogDAO;
import com.sonatype.insight.brain.model.Application;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RelayEventDeduplicatorTest
{
  @Mock
  private RelayEventLogDAO relayEventLogDAO;

  @Mock
  private ApplicationDAO applicationDAO;

  private RelayEventDeduplicator deduplicator;

  @BeforeEach
  public void before() {
    deduplicator = new RelayEventDeduplicator(relayEventLogDAO, applicationDAO);
  }

  @Test
  public void isPrimaryDuplicate_blankEventId_returnsFalse() {
    assertThat(deduplicator.isPrimaryDuplicate(null)).isFalse();
    assertThat(deduplicator.isPrimaryDuplicate("")).isFalse();
    assertThat(deduplicator.isPrimaryDuplicate("   ")).isFalse();
    verify(relayEventLogDAO, never()).existsByEventId(anyString());
  }

  @Test
  public void isPrimaryDuplicate_delegatesToDao() {
    when(relayEventLogDAO.existsByEventId("evt-1")).thenReturn(true);
    when(relayEventLogDAO.existsByEventId("evt-2")).thenReturn(false);

    assertThat(deduplicator.isPrimaryDuplicate("evt-1")).isTrue();
    assertThat(deduplicator.isPrimaryDuplicate("evt-2")).isFalse();
  }

  @Test
  public void isSecondaryDuplicate_delegatesToDao() {
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", "pat", "pull_request_opened"))
        .thenReturn(true);

    assertThat(deduplicator.isSecondaryDuplicate("app-pub-1", 7, "sha-1", "pat", "pull_request_opened")).isTrue();
    assertThat(deduplicator.isSecondaryDuplicate("app-pub-1", 8, "sha-1", "pat", "pull_request_opened")).isFalse();
  }

  @Test
  public void isSecondaryDuplicate_modeDiscriminatesAtDaoLevel() {
    // Same (app, pr, commit) triple under PAT mode is a dup; under GitHub App mode it isn't.
    // The deduplicator must thread the mode through verbatim.
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", "pat", "pull_request_opened"))
        .thenReturn(true);
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", "github-app", "pull_request_opened"))
        .thenReturn(false);

    assertThat(deduplicator.isSecondaryDuplicate("app-pub-1", 7, "sha-1", "pat", "pull_request_opened")).isTrue();
    assertThat(deduplicator.isSecondaryDuplicate("app-pub-1", 7, "sha-1", "github-app", "pull_request_opened"))
        .isFalse();
  }

  @Test
  public void recordProcessed_blankEventId_returnsTrueAndDoesNotWrite() {
    boolean recorded = deduplicator.recordProcessed(null, "app-pub-1", 1, "sha-1", "push", "pat");

    assertThat(recorded).isTrue();
    verify(relayEventLogDAO, never()).recordIfNew(any(), any(), any(), any(), any(), any());
  }

  @Test
  public void recordProcessed_returnsTrueWhenDaoInsertedRow() {
    when(relayEventLogDAO.recordIfNew("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "pat"))
        .thenReturn(true);

    boolean recorded = deduplicator.recordProcessed("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "pat");

    assertThat(recorded).isTrue();
  }

  @Test
  public void recordProcessed_returnsFalseWhenDuplicate() {
    when(relayEventLogDAO.recordIfNew(eq("evt-1"), any(), any(), any(), any(), any())).thenReturn(false);

    boolean recorded = deduplicator.recordProcessed("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "pat");

    assertThat(recorded).isFalse();
  }

  @Test
  public void recordProcessed_threadsModeArgumentToDao() {
    // Sanity: the `mode` arg is forwarded to the DAO call, not silently swallowed.
    when(relayEventLogDAO.recordIfNew("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "github-app"))
        .thenReturn(true);

    boolean recorded = deduplicator.recordProcessed("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened",
        "github-app");

    assertThat(recorded).isTrue();
    verify(relayEventLogDAO).recordIfNew("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "github-app");
  }

  @Test
  public void resolveApplicationPublicId_returnsPublicId() {
    Application app = new Application();
    app.setPublicId("MyApp");
    when(applicationDAO.getById("internal-id")).thenReturn(app);

    assertThat(deduplicator.resolveApplicationPublicId("internal-id")).isEqualTo("MyApp");
  }

  @Test
  public void resolveApplicationPublicId_unknownApp_returnsNull() {
    when(applicationDAO.getById(anyString())).thenReturn(null);

    assertThat(deduplicator.resolveApplicationPublicId("missing")).isNull();
  }

  @Test
  public void resolveApplicationPublicId_blankInput_returnsNullWithoutDaoCall() {
    assertThat(deduplicator.resolveApplicationPublicId(null)).isNull();
    assertThat(deduplicator.resolveApplicationPublicId("")).isNull();
    verify(applicationDAO, never()).getById(anyString());
  }

  @Test
  public void primaryThenRecord_simulatesNewEventFlow() {
    when(relayEventLogDAO.existsByEventId("evt-1")).thenReturn(false);
    when(relayEventLogDAO.recordIfNew(eq("evt-1"), any(), any(), any(), any(), any())).thenReturn(true);

    assertThat(deduplicator.isPrimaryDuplicate("evt-1")).isFalse();
    assertThat(deduplicator.recordProcessed("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "pat")).isTrue();
  }

  @Test
  public void primaryThenRecord_simulatesDuplicateFlow() {
    when(relayEventLogDAO.existsByEventId("evt-1")).thenReturn(true);

    assertThat(deduplicator.isPrimaryDuplicate("evt-1")).isTrue();
    // Caller should skip mapping; but if a race happened, recordIfNew would still return false:
    when(relayEventLogDAO.recordIfNew(eq("evt-1"), any(), any(), any(), any(), any())).thenReturn(false);
    assertThat(deduplicator.recordProcessed("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", "pat")).isFalse();
  }

  @Test
  public void secondaryDuplicate_isAppScoped() {
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-A", 1, "sha", "pat", "pull_request_opened")).thenReturn(true);
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-B", 1, "sha", "pat", "pull_request_opened")).thenReturn(false);

    assertThat(deduplicator.isSecondaryDuplicate("app-A", 1, "sha", "pat", "pull_request_opened")).isTrue();
    assertThat(deduplicator.isSecondaryDuplicate("app-B", 1, "sha", "pat", "pull_request_opened")).isFalse();
  }

  @Test
  public void crossModeEvent_isNotASecondaryDuplicate() {
    // Migration scenario: a (app, pr, commit) row from the prior mode is still in the dedup
    // window. A fresh event arrives under the new mode against the same PR + commit. The DAO
    // returns false for the new mode; the deduplicator must surface that as not-a-duplicate
    // so the new event is published and recorded under the new mode.
    org.mockito.Mockito.lenient()
        .when(
            relayEventLogDAO.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", "pat", "pull_request_opened"))
        .thenReturn(true);
    when(relayEventLogDAO.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", "github-app", "pull_request_opened"))
        .thenReturn(false);
    when(relayEventLogDAO.recordIfNew("evt-new", "app-pub-1", 7, "sha-1", "pull_request_updated", "github-app"))
        .thenReturn(true);

    assertThat(deduplicator.isSecondaryDuplicate("app-pub-1", 7, "sha-1", "github-app", "pull_request_opened"))
        .isFalse();
    assertThat(deduplicator.recordProcessed("evt-new", "app-pub-1", 7, "sha-1", "pull_request_updated", "github-app"))
        .isTrue();
    verify(relayEventLogDAO).recordIfNew("evt-new", "app-pub-1", 7, "sha-1", "pull_request_updated", "github-app");
  }
}
