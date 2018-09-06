/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.successmetrics;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.InvalidNameException;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.NameHelperTest;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReportData;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SuccessMetricsReportDAOTest
    extends AbstractDbDAOTest
{
  private final SuccessMetricsReportDAO successMetricsReportDAO = new SuccessMetricsReportDAO();

  @Test
  public void testCRUD() {
    String username = "test123";
    // Add metrics
    String metricsName = "TestMetricsName";
    Date createTime = new Date();
    SuccessMetricsReport successMetrics = tempEntity.newSuccessMetricsReport(username, metricsName, "testScopeString",
        createTime);

    assertNotNull(successMetrics.getId());

    // Retrieve metrics and test
    SuccessMetricsReport returnedMetric = successMetricsReportDAO.getById(successMetrics.getId());
    assertMetrics(returnedMetric, username, "testScopeString", metricsName, "testmetricsname", createTime);

    // attempt to update metric
    String username2 = "bob";
    successMetrics.setUsername(username2);
    try {
      successMetricsReportDAO.update(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (UnsupportedOperationException expected) {
      assertEquals("SuccessMetricsReport does not support update operations.", expected.getMessage());
    }

    // Delete
    successMetricsReportDAO.delete(successMetrics);

    // Retrieve metric and test
    assertThat(successMetricsReportDAO.getById(successMetrics.getId()), nullValue());
  }

  @Test
  public void testGetByUsername() {
    String username = "test123";
    Date createTime1 = tempEntity.newSuccessMetricsReport(username, "metrics 1", "testMetricsString 1").getCreateTime();
    Date createTime2 = tempEntity.newSuccessMetricsReport(username, "metrics 2", "testMetricsString 2").getCreateTime();
    tempEntity.newSuccessMetricsReport("admin123", "metrics 3", "testMetricsString 3");

    // Retrieve metrics and test
    List<SuccessMetricsReport> actual = successMetricsReportDAO.getByUsername(username);
    assertThat(actual, hasSize(2));
    assertMetrics(actual.get(0), username, "testMetricsString 1", "metrics 1", "metrics1", createTime1);
    assertMetrics(actual.get(1), username, "testMetricsString 2", "metrics 2", "metrics2", createTime2);
  }

  @Test
  public void testGetByUsernameAndName() {
    String username = "test123";
    String metricName = "Abc Metrics";
    Date createTime = tempEntity.newSuccessMetricsReport(username, metricName, "testMetricsString 1").getCreateTime();
    tempEntity.newSuccessMetricsReport(username, "Xyz Metric", "testMetricsString 1");
    tempEntity.newSuccessMetricsReport("admin123", metricName, "testMetricsString 2");

    // Retrieve metric and test
    SuccessMetricsReport actual = successMetricsReportDAO.getByUsernameAndName(username, metricName);
    assertMetrics(actual, username, "testMetricsString 1", metricName, "abcmetrics", createTime);
  }

  @Test
  public void testValidateNullName_Insert() {
    SuccessMetricsReport successMetrics = new SuccessMetricsReport(null);
    try {
      successMetricsReportDAO.insert(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    SuccessMetricsReport successMetrics = new SuccessMetricsReport("");
    try {
      successMetricsReportDAO.insert(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      SuccessMetricsReport successMetrics = new SuccessMetricsReport(name);
      try {
        successMetricsReportDAO.insert(successMetrics);
        fail("Expected exception to be thrown.");
      }
      catch (InvalidNameException expected) {
        assertEquals(String.format(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0)), expected.getMessage());
      }
    }
  }

  @Test
  public void testValidateNameValidChars_Insert() {
    String username = "test123";
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1");
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    String username = "test123";
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      try {
        tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1");
        fail("Expected exception to be thrown.");
      }
      catch (InvalidNameException expected) {
        assertEquals("Name must not have leading or trailing spaces, or have two spaces in a row.",
            expected.getMessage());
      }
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String username = "test123";
    String name = "test string With Case and Whitespace";
    SuccessMetricsReport successMetrics = tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1111");
    assertEquals(name, successMetrics.getName());
    assertEquals("teststringwithcaseandwhitespace", successMetrics.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    SuccessMetricsReport actual = successMetricsReportDAO.getByUsernameAndName(username, name1);
    assertNotNull(actual);
    assertEquals(successMetrics.getId(), actual.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    String username = "test123";
    tempEntity.newSuccessMetricsReport(username, "Metrics12345", "testMetricsString 1111");
    try {
      tempEntity.newSuccessMetricsReport(username, "METRICS 12345", "testMetricsString 1111");
      fail("Expected exception to be thrown.");
    }
    catch (BadRequestException expected) {
      assertEquals("METRICS 12345 is already used as a name.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameLength_Insert() {
    String username = "test123";
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    try {
      tempEntity.newSuccessMetricsReport(username, name + "a", "testMetricsString 1111");
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }
    tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1111");
  }

  @Test
  public void testDelete_DeletesRelatedSuccessMetricsReportData() {
    SuccessMetricsReport report = tempEntity.newSuccessMetricsReport("username", "metrics", "{}");
    SuccessMetricsReportData reportData = tempEntity.newSuccessMetricsReportData(report.getId());
    SuccessMetricsReportDataDAO successMetricsReportDataDAO = new SuccessMetricsReportDataDAO();

    successMetricsReportDAO.delete(report);

    assertThat(successMetricsReportDataDAO.getById(reportData.getId()), is(nullValue()));
  }

  private void assertMetrics(SuccessMetricsReport actualMetrics,
                             String username,
                             String scopeJson,
                             String name,
                             String nameLowercaseNoWhitespace,
                             Date createTime)
  {
    assertThat(actualMetrics, notNullValue());
    assertThat(actualMetrics.getId(), is(notNullValue()));
    assertThat(actualMetrics.getCreateTime(), is(createTime));
    assertThat(actualMetrics.getUsername(), is(username));
    assertThat(actualMetrics.getScopeJson(), is(scopeJson));
    assertThat(actualMetrics.getName(), is(name));
    assertThat(actualMetrics.getNameLowercaseNoWhitespace(), is(nameLowercaseNoWhitespace));
  }
}
