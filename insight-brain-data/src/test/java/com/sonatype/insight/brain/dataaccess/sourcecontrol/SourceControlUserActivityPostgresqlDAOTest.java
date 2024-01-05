/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.sourcecontrol;

import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;

import org.junit.Test;

public class SourceControlUserActivityPostgresqlDAOTest
    extends SourceControlUserActivityDAOTest
{
  @Override
  @Test
  @PostgresTest
  public void testInsertAllIfNew_onlyNewActivities() {
    super.testInsertAllIfNew_onlyNewActivities();
  }

  @Override
  @Test
  @PostgresTest
  public void testInsertAllIfNew_someActivityExists_notFailAndIgnore() {
    super.testInsertAllIfNew_someActivityExists_notFailAndIgnore();
  }

  @Override
  @Test
  @PostgresTest
  public void testDeleteBySourceControlUserId() {
    super.testDeleteBySourceControlUserId();
  }

  @Override
  @Test
  @PostgresTest
  public void testUpdateActivitiesSentToTelemetry() {
    super.testUpdateActivitiesSentToTelemetry();
  }

  @Override
  @Test
  @PostgresTest
  public void testGetActivitiesNotSentToTelemetry() {
    super.testGetActivitiesNotSentToTelemetry();
  }
}
