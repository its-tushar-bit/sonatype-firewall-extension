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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

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

    assertEquals(projectName, summaryApplicationName);
    assertEquals(stageName, summaryStageName);
    assertEquals("email", email);
    assertEquals("displayName", displayName);
  }

  /*
   * Tests fields used for backwards compatibility with older PDF templates are populated
   */
  @Test
  public void testFill_BackwardsCompat() {
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryProjectName = summary.get("projectName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();

    assertEquals(projectName, summaryProjectName);
    assertEquals(stageName, summaryBuildName);
  }

  @Test
  public void testFillSummaryWithNullContact() {
    Pdf.fillSummary(summary, projectName, stageName, null);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String summaryStageName = summary.get("stageName").asText();

    assertEquals(projectName, summaryApplicationName);
    assertEquals(stageName, summaryBuildName);
    assertEquals(stageName, summaryStageName);
    assertNull(summary.get("applicationContactEmail"));
    assertNull(summary.get("applicationContactName"));
  }

  @Test
  public void testFillSummaryWithNoEmail() {
    contact.setEmail(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String displayName = summary.get("applicationContactName").asText();

    assertEquals(projectName, summaryApplicationName);
    assertEquals(stageName, summaryBuildName);
    assertNull(summary.get("applicationContactEmail"));
    assertEquals("displayName", displayName);
  }

  @Test
  public void testFillSummaryWithNoDisplayName() {
    contact.setDisplayName(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String email = summary.get("applicationContactEmail").asText();

    assertEquals(projectName, summaryApplicationName);
    assertEquals(stageName, summaryBuildName);
    assertEquals("email", email);
    assertNull(summary.get("applicationContactName"));
  }

}
