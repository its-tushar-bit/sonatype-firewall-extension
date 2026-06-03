/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.relay;

import java.time.Duration;
import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.relay.RelayEventLog;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class RelayEventLogDAOTest
    extends AbstractDbDAOTest
{
  private RelayEventLogDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createRelayEventLogDAO();
  }

  private static final String MODE_PAT = "pat";

  private static final String MODE_GITHUB_APP = "github-app";

  @Test
  public void recordIfNew_persistsRowAndReturnsTrue() {
    boolean inserted = dao.recordIfNew("evt-1", "app-pub-1", 42, "abc123", "pull_request_opened", MODE_PAT);

    assertThat(inserted).isTrue();
    assertThat(dao.getAll())
        .extracting(RelayEventLog::getEventId, RelayEventLog::getApplicationPublicId,
            RelayEventLog::getPullRequestNumber, RelayEventLog::getCommitHash, RelayEventLog::getEventType,
            RelayEventLog::getMode)
        .containsExactly(tuple("evt-1", "app-pub-1", 42, "abc123", "pull_request_opened", MODE_PAT));
    assertThat(dao.getAll().get(0).getProcessedAt()).isNotNull();
    assertThat(dao.getAll().get(0).getId()).isNotBlank();
  }

  @Test
  public void recordIfNew_secondCallWithSameEventIdIsNoop() {
    assertThat(dao.recordIfNew("evt-1", "app-pub-1", 42, "abc123", "pull_request_opened", MODE_PAT)).isTrue();

    boolean second = dao.recordIfNew("evt-1", "app-pub-different", 99, "different-hash", "pull_request_updated",
        MODE_GITHUB_APP);

    assertThat(second).isFalse();
    // The original row is preserved; the duplicate insert did not overwrite it.
    assertThat(dao.getAll()).hasSize(1);
    RelayEventLog row = dao.getAll().get(0);
    assertThat(row.getApplicationPublicId()).isEqualTo("app-pub-1");
    assertThat(row.getPullRequestNumber()).isEqualTo(42);
    assertThat(row.getCommitHash()).isEqualTo("abc123");
    assertThat(row.getMode()).isEqualTo(MODE_PAT);
  }

  @Test
  public void recordIfNew_distinctEventIdsAreAllInserted() {
    assertThat(dao.recordIfNew("evt-1", "app-pub-1", 1, "h1", "push", MODE_PAT)).isTrue();
    assertThat(dao.recordIfNew("evt-2", "app-pub-1", 1, "h1", "push", MODE_PAT)).isTrue();

    assertThat(dao.getAll()).hasSize(2);
  }

  @Test
  public void recordIfNew_persistsModeColumn() {
    dao.recordIfNew("evt-pat", "app-pub-1", 1, "sha", "pull_request_opened", MODE_PAT);
    dao.recordIfNew("evt-app", "app-pub-1", 2, "sha", "pull_request_opened", MODE_GITHUB_APP);
    dao.recordIfNew("evt-null", "app-pub-1", 3, "sha", "pull_request_opened", null);

    assertThat(dao.getAll())
        .extracting(RelayEventLog::getEventId, RelayEventLog::getMode)
        .containsExactlyInAnyOrder(
            tuple("evt-pat", MODE_PAT),
            tuple("evt-app", MODE_GITHUB_APP),
            tuple("evt-null", null));
  }

  @Test
  public void isDuplicateBySecondaryKey_matchesOnExactTuple() {
    dao.recordIfNew("evt-1", "app-pub-1", 7, "sha-1", "pull_request_opened", MODE_PAT);

    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 8, "sha-1", MODE_PAT, "pull_request_opened")).isFalse();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-2", MODE_PAT, "pull_request_opened")).isFalse();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-other", 7, "sha-1", MODE_PAT, "pull_request_opened")).isFalse();
  }

  @Test
  public void isDuplicateBySecondaryKey_discriminatesByEventType() {
    // Same (app, pr, commit, mode) tuple but different event_type — e.g. close + reopen on
    // the same head SHA — must NOT collide. Reopen is a logically distinct event that drives
    // a fresh scan workflow; collapsing it as a secondary duplicate of the close silently
    // drops it.
    dao.recordIfNew("evt-close", "app-pub-1", 7, "sha-1", "pull_request_closed", MODE_PAT);

    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_closed")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isFalse();
    // Now record the reopen on the same SHA: both must be detectable independently.
    dao.recordIfNew("evt-reopen", "app-pub-1", 7, "sha-1", "pull_request_opened", MODE_PAT);
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_closed")).isTrue();
  }

  @Test
  public void isDuplicateBySecondaryKey_matchesOnNullPullRequestNumberForPushEvents() {
    dao.recordIfNew("evt-push", "app-pub-1", null, "sha-1", "push", MODE_PAT);

    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", null, "sha-1", MODE_PAT, "push")).isTrue();
    // A PR with a non-null number against the same commit must not collide with the push row.
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 1, "sha-1", MODE_PAT, "push")).isFalse();
  }

  @Test
  public void isDuplicateBySecondaryKey_returnsFalseForNullApplication() {
    dao.recordIfNew("evt-1", null, 7, "sha-1", "pull_request_opened", MODE_PAT);

    assertThat(dao.isDuplicateBySecondaryKey(null, 7, "sha-1", MODE_PAT, "pull_request_opened")).isFalse();
  }

  @Test
  public void isDuplicateBySecondaryKey_discriminatesByMode() {
    // Same (app, pr, commit) tuple recorded under PAT mode must NOT be reported as a duplicate
    // when looked up under GitHub App mode (and vice versa). This is the migration scenario:
    // open PR has a row from the prior mode; new events arrive under the new mode.
    dao.recordIfNew("evt-pat", "app-pub-1", 7, "sha-1", "pull_request_opened", MODE_PAT);

    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_GITHUB_APP, "pull_request_opened"))
        .isFalse();
    // Now record the same logical tuple under github-app: both must be detectable independently.
    dao.recordIfNew("evt-app", "app-pub-1", 7, "sha-1", "pull_request_opened", MODE_GITHUB_APP);
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_GITHUB_APP, "pull_request_opened")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isTrue();
  }

  @Test
  public void isDuplicateBySecondaryKey_nullModeMatchesNullModeRowsOnly() {
    // A null mode parameter only matches rows whose mode column is also null. This is the
    // documented contract on the DAO: callers must pass the current mode explicitly.
    dao.recordIfNew("evt-null-mode", "app-pub-1", 7, "sha-1", "pull_request_opened", null);

    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", null, "pull_request_opened")).isTrue();
    assertThat(dao.isDuplicateBySecondaryKey("app-pub-1", 7, "sha-1", MODE_PAT, "pull_request_opened")).isFalse();
  }

  @Test
  public void existsByEventId_returnsTrueForKnownEventIdAndFalseOtherwise() {
    dao.recordIfNew("evt-1", "app-pub-1", 1, "h1", "push", MODE_PAT);

    assertThat(dao.existsByEventId("evt-1")).isTrue();
    assertThat(dao.existsByEventId("evt-unknown")).isFalse();
    assertThat(dao.existsByEventId(null)).isFalse();
    assertThat(dao.existsByEventId("")).isFalse();
    assertThat(dao.existsByEventId("   ")).isFalse();
  }

  @Test
  public void recordIfNew_nullEventIdReturnsFalseAndInsertsNothing() {
    boolean inserted = dao.recordIfNew(null, "app-pub-1", 1, "h1", "push", MODE_PAT);

    assertThat(inserted).isFalse();
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void recordIfNew_blankEventIdReturnsFalseAndInsertsNothing() {
    // Blank is treated like null so a stray empty string cannot consume the unique-key slot.
    assertThat(dao.recordIfNew("", "app-pub-1", 1, "h1", "push", MODE_PAT)).isFalse();
    assertThat(dao.recordIfNew("   ", "app-pub-1", 1, "h1", "push", MODE_PAT)).isFalse();
    assertThat(dao.getAll()).isEmpty();
  }

  @Test
  public void deleteOlderThan_removesOnlyOldRows() {
    dao.recordIfNew("recent", "app-pub-1", 1, "h-recent", "push", MODE_PAT);
    RelayEventLog stale = newEntity("stale", "app-pub-1", 2, "h-stale", "push", MODE_PAT,
        new Date(System.currentTimeMillis() - Duration.ofDays(30).toMillis()));
    dao.insert(stale);

    int deleted = dao.deleteOlderThan(Duration.ofDays(7));

    assertThat(deleted).isEqualTo(1);
    assertThat(dao.getAll()).extracting(RelayEventLog::getEventId).containsExactly("recent");
  }

  @Test
  public void deleteOlderThan_zeroAge_deletesAllPastRows() {
    // Insert with explicit past processedAt to avoid relying on sub-millisecond clock precision.
    RelayEventLog past = newEntity("evt-1", "app-pub-1", 1, "h-1", "push", MODE_PAT,
        new Date(System.currentTimeMillis() - Duration.ofSeconds(10).toMillis()));
    dao.insert(past);

    int deleted = dao.deleteOlderThan(Duration.ZERO);

    assertThat(deleted).isEqualTo(1);
    assertThat(dao.getAll()).isEmpty();
  }

  private static RelayEventLog newEntity(
      String eventId,
      String applicationPublicId,
      Integer pullRequestNumber,
      String commitHash,
      String eventType,
      String mode,
      Date processedAt)
  {
    RelayEventLog row = new RelayEventLog();
    row.setEventId(eventId);
    row.setApplicationPublicId(applicationPublicId);
    row.setPullRequestNumber(pullRequestNumber);
    row.setCommitHash(commitHash);
    row.setEventType(eventType);
    row.setMode(mode);
    row.setProcessedAt(processedAt);
    return row;
  }

  private static org.assertj.core.groups.Tuple tuple(Object... values) {
    return org.assertj.core.api.Assertions.tuple(values);
  }
}
