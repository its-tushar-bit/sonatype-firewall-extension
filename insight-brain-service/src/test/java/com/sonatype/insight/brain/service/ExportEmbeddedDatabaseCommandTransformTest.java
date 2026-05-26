/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ExportEmbeddedDatabaseCommandTransformTest
{
  @Test
  public void testTransformInsertValues_ColumnSeparator() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues(",1")).isEqualTo("\t1");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues(",    1")).isEqualTo("\t1");
  }

  @Test
  public void testTransformInsertValues_Null() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("NULL")).isEqualTo("\\N");
  }

  @Test
  public void testTransformInsertValues_Number() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("1234567890")).isEqualTo("1234567890");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("-1.234567890")).isEqualTo("-1.234567890");
  }

  @Test
  public void testTransformInsertValues_Boolean() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("TRUE")).isEqualTo("TRUE");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("FALSE")).isEqualTo("FALSE");
  }

  @Test
  public void testTransformInsertValues_Timestamp() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("TIMESTAMP '2019-06-14 19:25:51.334'"))
        .isEqualTo("2019-06-14 19:25:51.334");
  }

  @Test
  public void testTransformInsertValues_Date() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("DATE '2024-02-23'")).isEqualTo("2024-02-23");
  }

  @Test
  public void testTransformInsertValues_String_Quoted() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("''")).isEqualTo("");
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("'abc '' \\N'")).isEqualTo("abc ' \\\\N");
  }

  @Test
  public void testTransformInsertValues_String_Encoded() {
    assertThat(ExportEmbeddedDatabaseCommand
        .transformInsertValues("STRINGDECODE('abc '' \\n\\t\\\\ \\u20AC \\\\\\u20AC \\\\uASis')"))
            .isEqualTo("abc ' \\n\\t\\\\ \u20AC \\\\€ \\\\uASis");
  }

  @Test
  public void testTransformInsertValues_Binary() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues("X'0010abCDeF'")).isEqualTo("\\\\x0010abCDeF");
  }

  @Test
  public void testTransformInsertValues_MultipleColumns() {
    assertThat(ExportEmbeddedDatabaseCommand.transformInsertValues(
        "-1, NULL, TRUE, 2.0, 'abc', STRINGDECODE('xyz'), TIMESTAMP '2019-06-14 19:25:51.334', DATE '2024-02-23'"))
            .isEqualTo("-1\t\\N\tTRUE\t2.0\tabc\txyz\t2019-06-14 19:25:51.334\t2024-02-23");
  }

  @Test
  public void testIsExcludedMigrationTracker_excludesPostgresOnlyTracker() {
    assertThat(ExportEmbeddedDatabaseCommand.isExcludedMigrationTracker(
        "INSERT INTO INSIGHT_BRAIN_ODS.MIGRATION_TRACKER(MIGRATION_TRACKER_ID) VALUES('PolicyViolationIndexAsyncDbMigration');"))
            .isTrue();
  }

  @Test
  public void testIsExcludedMigrationTracker_keepsOtherTrackers() {
    assertThat(ExportEmbeddedDatabaseCommand.isExcludedMigrationTracker(
        "INSERT INTO INSIGHT_BRAIN_ODS.MIGRATION_TRACKER(MIGRATION_TRACKER_ID) VALUES('DisplayNameForFileCoordinateAsyncDbMigration');"))
            .isFalse();
  }

  @Test
  public void testIsExcludedMigrationTracker_ignoresNonTrackerInserts() {
    assertThat(ExportEmbeddedDatabaseCommand.isExcludedMigrationTracker(
        "INSERT INTO INSIGHT_BRAIN_ODS.POLICY_VIOLATION(POLICY_VIOLATION_ID) VALUES('abc');"))
            .isFalse();
  }
}
