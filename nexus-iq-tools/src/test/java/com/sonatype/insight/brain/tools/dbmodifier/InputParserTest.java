/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class InputParserTest
{
  @Test
  public void testParseInput_NoSchemaOrColumnNames() {
    SQLLine sqlLine = InputParser.parseInput("INSERT INTO SYSTEM_LOB_STREAM VALUES(123)");
    assertThat(sqlLine.table).isEqualTo("SYSTEM_LOB_STREAM");
    assertThat(sqlLine.cols).isEmpty();
    assertThat(sqlLine.vals).containsExactly("123");
  }

  @Test
  public void testParseInput_NULL_Value() {
    SQLLine sqlLine = InputParser.parseInput(
        "INSERT INTO \"test_schema\".\"test_table\"(\"column1\", \"column2\", \"column3\")"
            + " VALUES ('value1', NULL, 'value3')");
    assertThat(sqlLine.table).isEqualTo("\"test_schema\".\"test_table\"");
    assertThat(sqlLine.cols).containsExactly("column1", "column2", "column3");
    assertThat(sqlLine.vals).containsExactly("'value1'", "NULL", "'value3'");
  }

  @Test
  public void testParseInput_TIMESTAMP_Value() {
    SQLLine sqlLine = InputParser.parseInput(
        "INSERT INTO \"test_schema\".\"test_table\"(\"column1\", \"column2\", \"column3\")"
            + " VALUES ('value1', TIMESTAMP '2019-11-27 07:53:29.595', 'value3')");
    assertThat(sqlLine.table).isEqualTo("\"test_schema\".\"test_table\"");
    assertThat(sqlLine.cols).containsExactly("column1", "column2", "column3");
    assertThat(sqlLine.vals).containsExactly("'value1'", "TIMESTAMP '2019-11-27 07:53:29.595'", "'value3'");
  }

  @Test
  public void testParseInput_STRINGDECODE_Value() {
    SQLLine sqlLine = InputParser.parseInput(
        "INSERT INTO \"test_schema\".\"test_table\"(\"column1\", \"column2\", \"column3\")"
            + " VALUES ('value1', STRINGDECODE('text'), 'value3')");
    assertThat(sqlLine.table).isEqualTo("\"test_schema\".\"test_table\"");
    assertThat(sqlLine.cols).containsExactly("column1", "column2", "column3");
    assertThat(sqlLine.vals).containsExactly("'value1'", "STRINGDECODE('text')", "'value3'");
  }

  @Test
  public void testParseInput_ColumnSeparatorInValue() {
    SQLLine sqlLine = InputParser.parseInput(
        "INSERT INTO \"test_schema\".\"test_table\"(\"column1\", \"column2\", \"column3\")"
            + " VALUES ('foo,bar', 'foo, bar', '''foo'', bar')");
    assertThat(sqlLine.table).isEqualTo("\"test_schema\".\"test_table\"");
    assertThat(sqlLine.cols).containsExactly("column1", "column2", "column3");
    assertThat(sqlLine.vals).containsExactly("'foo,bar'", "'foo, bar'", "'''foo'', bar'");
  }

  @Test
  public void testParseInput_BYTEA() {
    SQLLine sqlLine = InputParser.parseInput(
        "INSERT INTO \"test_schema\".\"test_table\"(\"column1\", \"column2\", \"column3\", \"column4\")"
            + " VALUES (X'', X'01FF', X'01 bc 2a', X'01' '02')");
    assertThat(sqlLine.table).isEqualTo("\"test_schema\".\"test_table\"");
    assertThat(sqlLine.cols).containsExactly("column1", "column2", "column3", "column4");
    assertThat(sqlLine.vals).containsExactly("X''", "X'01FF'", "X'01 bc 2a'", "X'01' '02'");
  }
}
