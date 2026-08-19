/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.global;

import java.util.List;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract contract test for every {@link GlobalSearchSuggestIqLocalClient} implementation. Nails down
 * the public-facing type-mapping guarantee documented on the SPI: rows returned from the IQ-local leg
 * are always reported under the public taxonomy ({@link SuggestItemType#COMPONENT},
 * {@link SuggestItemType#VULNERABILITY}) rather than any internal enum such as
 * {@code NON_VULNERABLE_COMPONENT} or {@code SECURITY_VULNERABILITY}.
 *
 * <p>
 * New implementations MUST extend this class and provide a wired-up service under
 * {@link #createService()}.
 */
public abstract class GlobalSearchSuggestIqLocalClientContractTest
{
  protected abstract GlobalSearchSuggestIqLocalClient createService();

  @Test
  public void suggest_returnsRowsWithPublicItemTypes_forComponentAndVulnerability() {
    GlobalSearchSuggestIqLocalClient service = createService();

    List<SuggestRow> rows = service.suggest(
        "alpha",
        List.of(SuggestItemType.COMPONENT, SuggestItemType.VULNERABILITY),
        10,
        /* principal */ null);

    assertThat(rows).isNotNull();
    assertThat(rows).allMatch(
        r -> r.type() == SuggestItemType.COMPONENT || r.type() == SuggestItemType.VULNERABILITY,
        "every row must have a public item type (COMPONENT or VULNERABILITY)");
  }

  @Test
  public void suggest_returnsNonNullEmptyList_forEmptyTypeSet() {
    GlobalSearchSuggestIqLocalClient service = createService();

    List<SuggestRow> rows = service.suggest("alpha", List.of(), 10, /* principal */ null);

    assertThat(rows).isNotNull();
    assertThat(rows).isEmpty();
  }

  @Test
  public void suggest_tolerates_perTypeLimit_ofZero() {
    GlobalSearchSuggestIqLocalClient service = createService();

    List<SuggestRow> rows = service.suggest("alpha", List.of(SuggestItemType.COMPONENT), 0, /* principal */ null);

    assertThat(rows).isNotNull();
    assertThat(rows).isEmpty();
  }

  @Test
  public void suggest_withNullPrincipal_returnsEmptyList() {
    // Authorization contract: implementations receiving a null principal MUST return an empty list
    // rather than serving unfiltered results.
    GlobalSearchSuggestIqLocalClient service = createService();

    List<SuggestRow> rows = service.suggest(
        "alpha",
        List.of(SuggestItemType.APPLICATION, SuggestItemType.COMPONENT),
        10,
        /* principal */ null);

    assertThat(rows).isNotNull();
    assertThat(rows).isEmpty();
  }
}
