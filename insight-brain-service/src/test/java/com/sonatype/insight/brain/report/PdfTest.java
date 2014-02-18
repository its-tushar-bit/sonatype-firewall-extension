/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
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
  private int buildNumber = 0;
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
    Pdf.fillSummary(summary, projectName, buildNumber, contact);

    String summaryProjectName = summary.get("projectName").asText();
    int summaryBuildNumber = summary.get("buildNumber").asInt();
    String email = summary.get("applicationContactEmail").asText();
    String displayName = summary.get("applicationContactName").asText();

    Assert.assertEquals(projectName, summaryProjectName);
    Assert.assertEquals(buildNumber, summaryBuildNumber);
    Assert.assertEquals("email", email);
    Assert.assertEquals("displayName", displayName);
  }

  @Test
  public void testFillSummaryWithNullContact() {
    Pdf.fillSummary(summary, projectName, buildNumber, null);

    String summaryProjectName = summary.get("projectName").asText();
    int summaryBuildNumber = summary.get("buildNumber").asInt();

    Assert.assertEquals(projectName, summaryProjectName);
    Assert.assertEquals(buildNumber, summaryBuildNumber);
    Assert.assertNull(summary.get("applicationContactEmail"));
    Assert.assertNull(summary.get("applicationContactName"));
  }

  @Test
  public void testFillSummaryWithNoEmail() {
    contact.setEmail(null);
    Pdf.fillSummary(summary, projectName, buildNumber, contact);

    String summaryProjectName = summary.get("projectName").asText();
    int summaryBuildNumber = summary.get("buildNumber").asInt();
    String displayName = summary.get("applicationContactName").asText();

    Assert.assertEquals(projectName, summaryProjectName);
    Assert.assertEquals(buildNumber, summaryBuildNumber);
    Assert.assertNull(summary.get("applicationContactEmail"));
    Assert.assertEquals("displayName", displayName);
  }

  @Test
  public void testFillSummaryWithNoDisplayName() {
    contact.setDisplayName(null);
    Pdf.fillSummary(summary, projectName, buildNumber, contact);

    String summaryProjectName = summary.get("projectName").asText();
    int summaryBuildNumber = summary.get("buildNumber").asInt();
    String email = summary.get("applicationContactEmail").asText();

    Assert.assertEquals(projectName, summaryProjectName);
    Assert.assertEquals(buildNumber, summaryBuildNumber);
    Assert.assertEquals("email", email);
    Assert.assertNull(summary.get("applicationContactName"));
  }

}
