/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.continuousmonitoring;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class PageTest
{
  @Test
  public void emptyHasNoRowsNoCursorNoMore() {
    Page<String> p = Page.empty();
    assertThat(p.rows()).isEmpty();
    assertThat(p.nextCursor()).isNull();
    assertThat(p.hasMore()).isFalse();
  }

  @Test
  public void carriesRowsCursorAndHasMoreFlag() {
    EligibilityCursor cursor = new EligibilityCursor(new Date(1L), "id-1");
    Page<String> p = new Page<>(List.of("a", "b"), cursor, true);
    assertThat(p.rows()).containsExactly("a", "b");
    assertThat(p.nextCursor()).isEqualTo(cursor);
    assertThat(p.hasMore()).isTrue();
  }

  @Test
  public void hasMoreWithoutCursorRejected() {
    assertThatThrownBy(() -> new Page<>(List.of("a"), null, true))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  public void emptyRowsWithHasMoreTrueRejected() {
    // Enforces the symmetric contract invariant from EligibilitySelector: a buggy selector that
    // returns empty rows + hasMore=true would bypass the producer's early-exit and the safety-net
    // WARN, silently aborting the cycle as success(0). Page.empty() is the only correct way to
    // signal end-of-stream.
    EligibilityCursor cursor = new EligibilityCursor(new Date(1L), "id-1");
    assertThatThrownBy(() -> new Page<>(List.of(), cursor, true))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("empty rows with hasMore=true");
  }

  @Test
  public void emptyRowsWithHasMoreFalseAllowed() {
    // The valid end-of-stream shape — equivalent to Page.empty() but with an explicit (and
    // ignored) nextCursor. Stays legal because hasMore=false consistently means "stop here".
    EligibilityCursor cursor = new EligibilityCursor(new Date(1L), "id-1");
    Page<String> p = new Page<>(List.of(), cursor, false);
    assertThat(p.rows()).isEmpty();
    assertThat(p.hasMore()).isFalse();
  }
}
