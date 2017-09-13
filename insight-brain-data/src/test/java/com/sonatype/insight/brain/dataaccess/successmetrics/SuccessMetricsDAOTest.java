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
import com.sonatype.insight.brain.model.successmetrics.SuccessMetrics;
import com.sonatype.insight.error.exception.BadRequestException;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class SuccessMetricsDAOTest
    extends AbstractDbDAOTest
{
  private final SuccessMetricsDAO successMetricsDAO = new SuccessMetricsDAO();

  @Test
  public void testCRUD() {
    String username = "test123";
    // Add metrics
    String metricsName = "TestMetricsName";
    Date createTime = new Date();
    SuccessMetrics successMetrics = tempEntity.newSuccessMetrics(username, metricsName, "testScopeString", createTime);

    assertNotNull(successMetrics.getId());

    // Retrieve metrics and test
    SuccessMetrics returnedMetric = successMetricsDAO.getById(successMetrics.getId());
    assertMetrics(returnedMetric, username, "testScopeString", metricsName, "testmetricsname", createTime);

    // attempt to update metric
    String username2 = "bob";
    successMetrics.setUsername(username2);
    try {
      successMetricsDAO.update(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (UnsupportedOperationException expected) {
      assertEquals("SuccessMetrics does not support update operations.", expected.getMessage());
    }

    // Delete
    successMetricsDAO.delete(successMetrics);

    // Retrieve metric and test
    assertThat(successMetricsDAO.getById(successMetrics.getId()), nullValue());
  }

  @Test
  public void testGetByUsername() {
    String username = "test123";
    Date createTime1 = tempEntity.newSuccessMetrics(username, "metrics 1", "testMetricsString 1").getCreateTime();
    Date createTime2 = tempEntity.newSuccessMetrics(username, "metrics 2", "testMetricsString 2").getCreateTime();
    tempEntity.newSuccessMetrics("admin123", "metrics 3", "testMetricsString 3");

    // Retrieve metrics and test
    List<SuccessMetrics> actual = successMetricsDAO.getByUsername(username);
    assertThat(actual, hasSize(2));
    assertMetrics(actual.get(0), username, "testMetricsString 1", "metrics 1", "metrics1", createTime1);
    assertMetrics(actual.get(1), username, "testMetricsString 2", "metrics 2", "metrics2", createTime2);
  }

  @Test
  public void testGetByUsernameAndName() {
    String username = "test123";
    String metricName = "Abc Metrics";
    Date createTime = tempEntity.newSuccessMetrics(username, metricName, "testMetricsString 1").getCreateTime();
    tempEntity.newSuccessMetrics(username, "Xyz Metric", "testMetricsString 1");
    tempEntity.newSuccessMetrics("admin123", metricName, "testMetricsString 2");

    // Retrieve metric and test
    SuccessMetrics actual = successMetricsDAO.getByUsernameAndName(username, metricName);
    assertMetrics(actual, username, "testMetricsString 1", metricName, "abcmetrics", createTime);
  }

  @Test
  public void testValidateNullName_Insert() {
    SuccessMetrics successMetrics = new SuccessMetrics(null);
    try {
      successMetricsDAO.insert(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateEmptyName_Insert() {
    SuccessMetrics successMetrics = new SuccessMetrics("");
    try {
      successMetricsDAO.insert(successMetrics);
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name is required.", expected.getMessage());
    }
  }

  @Test
  public void testValidateNameInvalidChars_Insert() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      SuccessMetrics successMetrics = new SuccessMetrics(name);
      try {
        successMetricsDAO.insert(successMetrics);
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
      tempEntity.newSuccessMetrics(username, name, "testMetricsString 1");
    }
  }

  @Test
  public void testValidateNameSpaces_Insert() {
    String username = "test123";
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      try {
        tempEntity.newSuccessMetrics(username, name, "testMetricsString 1");
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
    SuccessMetrics successMetrics = tempEntity.newSuccessMetrics(username, name, "testMetricsString 1111");
    assertEquals(name, successMetrics.getName());
    assertEquals("teststringwithcaseandwhitespace", successMetrics.getNameLowercaseNoWhitespace());

    String name1 = "TEST String      With    cASE and      whitespace";
    SuccessMetrics actual = successMetricsDAO.getByUsernameAndName(username, name1);
    assertNotNull(actual);
    assertEquals(successMetrics.getId(), actual.getId());
  }

  @Test
  public void testDuplicateName_Insert() {
    String username = "test123";
    tempEntity.newSuccessMetrics(username, "Metrics12345", "testMetricsString 1111");
    try {
      tempEntity.newSuccessMetrics(username, "METRICS 12345", "testMetricsString 1111");
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
      tempEntity.newSuccessMetrics(username, name + "a", "testMetricsString 1111");
      fail("Expected exception to be thrown.");
    }
    catch (InvalidNameException expected) {
      assertEquals("Name must be 60 characters or less.", expected.getMessage());
    }
    tempEntity.newSuccessMetrics(username, name, "testMetricsString 1111");
  }

  private void assertMetrics(SuccessMetrics actualMetrics,
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
