/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.report;

import com.sonatype.insight.brain.organization.ContactDTO;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class PdfTest
{
  private ObjectNode summary;

  private String projectName = "test project";

  private String stageName = "Build";

  private ContactDTO contact;

  @Before
  public void setup() {
    summary = new ObjectNode(JsonNodeFactory.instance);
    contact = new ContactDTO("internalName", "displayName", "email", "realm");
  }

  @After
  public void tearDown() {
    summary = null;
    contact = null;
  }

  @Test
  public void testFillSummary() {
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryStageName = summary.get("stageName").asText();
    String email = summary.get("applicationContactEmail").asText();
    String displayName = summary.get("applicationContactName").asText();

    assertThat(summaryApplicationName).isEqualTo(projectName);
    assertThat(summaryStageName).isEqualTo(stageName);
    assertThat(email).isEqualTo("email");
    assertThat(displayName).isEqualTo("displayName");
  }

  /*
   * Tests fields used for backwards compatibility with older PDF templates are populated
   */
  @Test
  public void testFill_BackwardsCompat() {
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryProjectName = summary.get("projectName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();

    assertThat(summaryProjectName).isEqualTo(projectName);
    assertThat(summaryBuildName).isEqualTo(stageName);
  }

  @Test
  public void testFillSummaryWithNullContact() {
    Pdf.fillSummary(summary, projectName, stageName, null);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String summaryStageName = summary.get("stageName").asText();

    assertThat(summaryApplicationName).isEqualTo(projectName);
    assertThat(summaryBuildName).isEqualTo(stageName);
    assertThat(summaryStageName).isEqualTo(stageName);
    assertThat(summary.get("applicationContactEmail")).isNull();
    assertThat(summary.get("applicationContactName")).isNull();
  }

  @Test
  public void testFillSummaryWithNoEmail() {
    contact.setEmail(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String displayName = summary.get("applicationContactName").asText();

    assertThat(summaryApplicationName).isEqualTo(projectName);
    assertThat(summaryBuildName).isEqualTo(stageName);
    assertThat(summary.get("applicationContactEmail")).isNull();
    assertThat(displayName).isEqualTo("displayName");
  }

  @Test
  public void testFillSummaryWithNoDisplayName() {
    contact.setDisplayName(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String email = summary.get("applicationContactEmail").asText();

    assertThat(summaryApplicationName).isEqualTo(projectName);
    assertThat(summaryBuildName).isEqualTo(stageName);
    assertThat(email).isEqualTo("email");
    assertThat(summary.get("applicationContactName")).isNull();
  }
}
