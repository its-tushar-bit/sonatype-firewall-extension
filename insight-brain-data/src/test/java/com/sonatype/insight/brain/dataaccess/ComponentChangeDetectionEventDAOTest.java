/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import java.util.stream.Collectors;

import com.sonatype.insight.brain.model.ComponentChangeDetectionEvent;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentChangeDetectionEventDAOTest
    extends AbstractDbDAOTest
{
  private ComponentChangeDetectionEventDAO underTest;

  @Before
  public void setUp() {
    underTest = daoFactory.createComponentChangeDetectionEventDAO();
  }

  @Test
  public void test_CanAddToTable() {
    tempEntity.newComponentChangeDetectionEvent("purl1", "some data", new Date());

    underTest.getAll()
        .stream()
        .findFirst()
        .ifPresent(event -> {
          assert event.getPurl().equals("purl1");
          assert event.getComponentEvaluationData().equals("some data");
        });
  }

  @Test
  public void test_CanRemoveExcessEvents() throws Exception {
    tempEntity.newComponentChangeDetectionEvent("purl1", "some data", new Date());
    Thread.sleep(2);
    tempEntity.newComponentChangeDetectionEvent("purl2", "some data", new Date());
    Thread.sleep(2);
    tempEntity.newComponentChangeDetectionEvent("purl3", "some data", new Date());

    underTest.removeExcessEvents(2);
    assertThat(underTest.getAll().size()).isEqualTo(2);
    assertThat(underTest.getAll().stream().map(ComponentChangeDetectionEvent::getPurl).collect(Collectors.toList()))
        .containsExactlyInAnyOrder("purl2", "purl3");
  }

  @Test
  public void test_CanDeleteEntriesOlderThan() throws Exception {
    Date started = new Date();
    tempEntity.newComponentChangeDetectionEvent("purl1", "some data", new Date());
    tempEntity.newComponentChangeDetectionEvent("purl2", "some data", new Date());
    Thread.sleep(2);
    Date middleAdded = new Date();
    tempEntity.newComponentChangeDetectionEvent("purl3", "some data", new Date());

    underTest.deleteEntriesOlderThan(started);
    assertThat(underTest.getAll().size()).isEqualTo(3);

    underTest.deleteEntriesOlderThan(middleAdded);
    assertThat(underTest.getAll().size()).isEqualTo(1);
    assertThat(underTest.getAll().get(0).getPurl()).isEqualTo("purl3");
  }
}
