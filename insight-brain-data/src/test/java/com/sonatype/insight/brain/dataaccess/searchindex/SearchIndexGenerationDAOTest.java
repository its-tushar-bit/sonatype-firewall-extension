/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.searchindex;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.searchindex.SearchIndexGeneration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchIndexGenerationDAOTest
    extends AbstractDbDAOTest
{
  private SearchIndexGenerationDAO dao;

  private final List<SearchIndexGeneration> inserted = new ArrayList<>();

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = new SearchIndexGenerationDAO(databaseRule.getOperationalDataStore());
  }

  @AfterEach
  public void removeInsertedGenerations() {
    inserted.forEach(generation -> dao.delete(generation));
    inserted.clear();
  }

  @Test
  public void findByRole_returnsTheGenerationHoldingTheRole() {
    insertGeneration("gen-a", SearchIndexGeneration.ROLE_SERVING, new Date(1_000L));

    Optional<SearchIndexGeneration> found = dao.findByRole(SearchIndexGeneration.ROLE_SERVING);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo("gen-a");
  }

  @Test
  public void findByRole_isEmptyWhenNothingHoldsTheRole() {
    assertThat(dao.findByRole(SearchIndexGeneration.ROLE_SERVING)).isEmpty();
  }

  /**
   * Role uniqueness is enforced in this layer rather than by a constraint, so two rows can share a
   * role. Without an order the query returns whichever row the planner reaches first, and Analyze
   * would report a different serving generation between two identical calls.
   */
  @Test
  public void findByRole_prefersTheNewestClaimantWhenTwoRowsShareARole() {
    insertGeneration("gen-old", SearchIndexGeneration.ROLE_SERVING, new Date(1_000L));
    insertGeneration("gen-new", SearchIndexGeneration.ROLE_SERVING, new Date(9_000L));

    Optional<SearchIndexGeneration> found = dao.findByRole(SearchIndexGeneration.ROLE_SERVING);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo("gen-new");
  }

  /** A building generation has no serving_since, so ordering falls through to creation time. */
  @Test
  public void findByRole_fallsBackToCreationTimeWhenNothingIsServingYet() {
    insertGeneration("build-old", SearchIndexGeneration.ROLE_BUILDING, null, new Date(1_000L));
    insertGeneration("build-new", SearchIndexGeneration.ROLE_BUILDING, null, new Date(9_000L));

    Optional<SearchIndexGeneration> found = dao.findByRole(SearchIndexGeneration.ROLE_BUILDING);

    assertThat(found).isPresent();
    assertThat(found.get().getId()).isEqualTo("build-new");
  }

  private void insertGeneration(final String id, final String role, final Date servingSince) {
    insertGeneration(id, role, servingSince, new Date());
  }

  private void insertGeneration(
      final String id,
      final String role,
      final Date servingSince,
      final Date createdAt)
  {
    SearchIndexGeneration generation = new SearchIndexGeneration();
    generation.setId(id);
    generation.setBackend(SearchIndexGeneration.BACKEND_LUCENE);
    generation.setRole(role);
    generation.setSchemaVersion(1);
    generation.setStorageRef("index/" + id);
    generation.setCreatedAt(createdAt);
    generation.setServingSince(servingSince);
    dao.insert(generation);
    inserted.add(generation);
  }
}
