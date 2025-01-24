/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.insight.brain.dataaccess.ComponentChangeDetectionEventDAO.ComponentChangeDetectionEvent;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ComponentChangeDetectionEventDAOTest
{
  private ComponentChangeDetectionEventDAO underTest;

  @Before
  public void setUp() {
    underTest = new ComponentChangeDetectionEventDAO();
  }

  @Test
  public void test_CanAddToTable() {
    underTest.addEvent(new ComponentChangeDetectionEvent("purl1", "some data"));

    underTest.getAll().stream()
        .findFirst()
        .ifPresent(event -> {
          assert event.purl().equals("purl1");
          assert event.data().equals("some data");
        });
  }

  @Test
  public void test_CanDeleteEntriesOlderThan() throws Exception {
    DateTime started = DateTime.now();
    underTest.addEvent(new ComponentChangeDetectionEvent("purl1", "some data"));
    underTest.addEvent(new ComponentChangeDetectionEvent("purl2", "some data"));
    Thread.sleep(2);
    DateTime middleAdded = DateTime.now();
    underTest.addEvent(new ComponentChangeDetectionEvent("purl3", "some data"));

    underTest.deleteEntriesOlderThan(started);
    assertThat(underTest.getAll().size()).isEqualTo(3);

    underTest.deleteEntriesOlderThan(middleAdded);
    assertThat(underTest.getAll().size()).isEqualTo(1);
    assertThat(underTest.getAll().get(0).purl()).isEqualTo("purl3");
  }
}
