/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.dbmodifier;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import static com.sonatype.insight.brain.tools.dbmodifier.ScrubberInsertMods.h2OdsTable;
import static org.assertj.core.api.Assertions.assertThat;

public class ScrubberInsertModsTest
{
  @Before
  public void init() throws Exception {
    ScrubberInsertMods.resetCache(null);
  }

  @Test
  public void testScrubInputLine_Generic() {
    // show only targeted columns are updated
    List<SQLLine> scrubbed1 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application"))
            .setLineCols("nontargeted", "name")
            .setLineVals("'test'", "'abcdef'")
            .build()
    );
    assertThat(scrubbed1).hasSize(1);
    assertThat(scrubbed1.get(0).columnValue("nontargeted")).isEqualTo("'test'");
    assertThat(scrubbed1.get(0).columnValue("name")).isEqualTo("'qfLPbb'");

    // demonstrate consistent replacement for same source value, abcdef
    List<SQLLine> scrubbed2 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("hash_component_identifier"))
            .setLineCols("comment")
            .setLineVals("STRINGDECODE('abcdef')")
            .build()
    );
    assertThat(scrubbed2).hasSize(1);
    assertThat(scrubbed2.get(0).columnValue("comment")).isEqualTo("STRINGDECODE('qfLPbb')");
  }

  @Test
  public void testScrubInputLine_SimilarPath() {
    List<SQLLine> scrubbed1 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application_component"))
            .setLineCols("name", "pathnames")
            .setLineVals("'abcdef'", "'com/jcraft/jsch/0.1.38/jsch-0.1.38.jar'")
            .build()
    );

    List<SQLLine> scrubbed2 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application_component"))
            .setLineCols("name", "pathnames")
            .setLineVals("'abcdef'", "'com/jcraft/jsch/0.1.53/jsch-0.1.53.jar'")
            .build()
    );
    assertThat(scrubbed1.get(0).columnValue("pathnames")).isNotEqualTo(scrubbed2.get(0).columnValue("pathnames"));
    assertThat(scrubbed1.get(0).columnValue("pathnames"))
        .isNotEqualToIgnoringCase(scrubbed2.get(0).columnValue("pathnames"));
  }

  @Test
  public void testScrubInputLine_Path() {
    List<SQLLine> scrubbed1 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application_component"))
            .setLineCols("name", "pathnames")
            .setLineVals("'abcdef'", "'C:\\\\some\\\\path\\\\One.txt;D:\\\\other\\\\path\\\\file_name-1.12.50.txt'")
            .build()
    );
    assertThat(scrubbed1).hasSize(1);
    assertThat(scrubbed1.get(0).columnValue("pathnames"))
        .isEqualTo("'q:\\\\fLPb\\\\bXOw\\\\P0ghpzs;B:\\\\14BNe\\\\bXOw\\\\19yjysSDMzBI2ncpICWTz'");

    List<SQLLine> scrubbed2 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application_component"))
            .setLineCols("name", "pathnames")
            .setLineVals("'abcdef'", "STRINGDECODE('/other/path/One.txt')")
            .build()
    );
    assertThat(scrubbed2).hasSize(1);
    assertThat(scrubbed2.get(0).columnValue("pathnames")).isEqualTo("STRINGDECODE('/14BNe/bXOw/P0ghpzs')");
  }

  @Test
  public void testScrubInputLine_CombineClob() {
    List<SQLLine> scrubbed1 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable("SYSTEM_LOB_STREAM")
            .setLineVals("0", "0", "STRINGDECODE('example part 1.')", "NULL")
            .build()
    );

    List<SQLLine> scrubbed2 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable("SYSTEM_LOB_STREAM")
            .setLineVals("0", "1", "STRINGDECODE('example part 2.')", "NULL")
            .build()
    );

    List<SQLLine> scrubbed3 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("application_component"))
            .setLineCols("comment", "name", "pathnames")
            .setLineVals("'test'", "'wxyz'", "SYSTEM_COMBINE_CLOB(0)")
            .build()
    );
    assertThat(scrubbed1).hasSize(0);
    assertThat(scrubbed2).hasSize(0);
    assertThat(scrubbed3).hasSize(3);
    assertThat(scrubbed3.get(0).vals.get(2)).isEqualTo("STRINGDECODE('qfLPbbX OwP0 gh')");
    assertThat(scrubbed3.get(1).vals.get(2)).isEqualTo("STRINGDECODE('pzsB14B OwP0 Ne')");
    assertThat(scrubbed3.get(2).columnValue("pathnames")).isEqualTo("SYSTEM_COMBINE_CLOB(0)");
  }

  @Test
  public void testScrubInputLine_UserScrubber() {
    List<SQLLine> scrubbed1 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("user"))
            .setLineCols("username", "username_lowercase", "password", "first_name", "last_name", "email")
            .setLineVals("'admin'", "'admin'", "'oldpass'", "'ad'", "'min'", "'admin@test.com'")
            .build()
    );
    assertThat(scrubbed1).hasSize(1);
    assertThat(scrubbed1.get(0).columnValue("username")).isEqualTo("'admin'");

    List<SQLLine> scrubbed2 = ScrubberInsertMods.scrubInputLine(
        SQLLine.builder()
            .setLineTable(h2OdsTable("user"))
            .setLineCols("username", "username_lowercase", "password", "first_name", "last_name", "email")
            .setLineVals("'Newton'", "'newton'", "'oldpass'", "'Isaac'", "'Newton'", "'newton@testdemo.com'")
            .build()
    );
    assertThat(scrubbed2).hasSize(1);
    assertThat(scrubbed2.get(0).columnValue("password")).isEqualTo("'" + ScrubberInsertMods.DEFAULT_PASS + "'");
    assertThat(scrubbed2.get(0).columnValue("username")).isEqualTo("'BNe19y'");
  }

  @Test
  public void testJsonRandomizer_Generic() {
    String samplePolicyJson = "{\n" +
        "  \"id\" : \"03ec05bee50c4e911111111a000bc5c4\",\n" +
        "  \"name\" : \"Security-Low\",\n" +
        "  \"ownerId\" : \"11111111bbbbbbbb55555555aaaaaaaa\",\n" +
        "  \"enabled\" : true,\n" +
        "  \"threatLevel\" : 3,\n" +
        "  \"constraints\" : [ {\n" +
        "    \"id\" : \"22222222cccccccc3333333300000000\",\n" +
        "    \"name\" : \"CVSS > 0 and < 4\",\n" +
        "    \"enabled\" : true,\n" +
        "    \"operator\" : \"AND\",\n" +
        "    \"conditions\" : [ {\n" +
        "      \"conditionTypeId\" : \"SecurityVulnerabilitySeverity\",\n" +
        "      \"operator\" : \"<\",\n" +
        "      \"value\" : \"4\"\n" +
        "    }, {\n" +
        "      \"conditionTypeId\" : \"SecurityVulnerabilityStatus\",\n" +
        "      \"operator\" : \"is not\",\n" +
        "      \"value\" : \"NOT_APPLICABLE\"\n" +
        "    }, {\n" +
        "      \"conditionTypeId\" : \"SecurityVulnerabilitySeverity\",\n" +
        "      \"operator\" : \">\",\n" +
        "      \"value\" : \"0\"\n" +
        "    } ]\n" +
        "  } ],\n" +
        "  \"actions\" : {\n" +
        "    \"build\" : \"warn\",\n" +
        "    \"release\" : \"warn\",\n" +
        "    \"develop\" : \"warn\",\n" +
        "    \"stage-release\" : \"warn\"\n" +
        "  },\n" +
        "  \"notifications\" : {\n" +
        "    \"userNotifications\" : [ {\n" +
        "      \"stageIds\" : [ \"build\", \"release\", \"stage-release\" ],\n" +
        "      \"emailAddress\" : \"some.one@sonatype.com\"\n" +
        "    }, {\n" +
        "      \"stageIds\" : [ \"build\", \"release\", \"stage-release\" ],\n" +
        "      \"emailAddress\" : \"nobody@sonatype.com\"\n" +
        "    } ],\n" +
        "    \"roleNotifications\" : [ ],\n" +
        "    \"jiraNotifications\" : [ ]\n" +
        "  }\n" +
        "}";

    String randomized = ScrubberInsertMods.jsonRandomizer(samplePolicyJson);
    assertThat(randomized).endsWith(ScrubberInsertMods.NOTIFICATIONS_EMPTY_CLOSING_JSON);
    List<String> original = Arrays.stream(samplePolicyJson.split("\n"))
        .filter(s -> s.contains(ScrubberInsertMods.NAME_OPEN_JSON)).collect(Collectors.toList());
    List<String> mutated = Arrays.stream(randomized.split("\n"))
        .filter(s -> s.contains(ScrubberInsertMods.NAME_OPEN_JSON)).collect(Collectors.toList());

    assertThat(mutated).hasSameSizeAs(original);
    assertThat(mutated).contains("  \"name\" : \"qfLPbbXOwP0g\",","    \"name\" : \"hpzsB14BNe19yjys\",");
  }
}
