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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

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

    Assert.assertEquals(projectName, summaryApplicationName);
    Assert.assertEquals(stageName, summaryStageName);
    Assert.assertEquals("email", email);
    Assert.assertEquals("displayName", displayName);
  }

  /*
   * Tests fields used for backwards compatibility with older PDF templates are populated
   */
  @Test
  public void testFill_BackwardsCompat() {
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryProjectName = summary.get("projectName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();

    Assert.assertEquals(projectName, summaryProjectName);
    Assert.assertEquals(stageName, summaryBuildName);
  }

  @Test
  public void testFillSummaryWithNullContact() {
    Pdf.fillSummary(summary, projectName, stageName, null);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String summaryStageName = summary.get("stageName").asText();

    Assert.assertEquals(projectName, summaryApplicationName);
    Assert.assertEquals(stageName, summaryBuildName);
    Assert.assertEquals(stageName, summaryStageName);
    Assert.assertNull(summary.get("applicationContactEmail"));
    Assert.assertNull(summary.get("applicationContactName"));
  }

  @Test
  public void testFillSummaryWithNoEmail() {
    contact.setEmail(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String displayName = summary.get("applicationContactName").asText();

    Assert.assertEquals(projectName, summaryApplicationName);
    Assert.assertEquals(stageName, summaryBuildName);
    Assert.assertNull(summary.get("applicationContactEmail"));
    Assert.assertEquals("displayName", displayName);
  }

  @Test
  public void testFillSummaryWithNoDisplayName() {
    contact.setDisplayName(null);
    Pdf.fillSummary(summary, projectName, stageName, contact);

    String summaryApplicationName = summary.get("applicationName").asText();
    String summaryBuildName = summary.get("buildNumber").asText();
    String email = summary.get("applicationContactEmail").asText();

    Assert.assertEquals(projectName, summaryApplicationName);
    Assert.assertEquals(stageName, summaryBuildName);
    Assert.assertEquals("email", email);
    Assert.assertNull(summary.get("applicationContactName"));
  }

}
