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
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class SuccessMetricsReportDAOTest
    extends AbstractDbDAOTest
{
  private SuccessMetricsReportDAO successMetricsReportDAO;

  private SuccessMetricsReportDataDAO successMetricsReportDataDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    successMetricsReportDataDAO = daoFactory.createSuccessMetricsReportDataDAO();
    successMetricsReportDAO = daoFactory.createSuccessMetricsReportDAO();
  }

  @Test
  public void testCRUD() {
    String username = "test123";
    // Add metrics
    String metricsName = "TestMetricsName";
    Date createTime = new Date();
    SuccessMetricsReport successMetrics = tempEntity.newSuccessMetricsReport(username, metricsName, "testScopeString",
        createTime);

    assertThat(successMetrics.getId()).isNotNull();

    // Retrieve metrics and test
    SuccessMetricsReport returnedMetric = successMetricsReportDAO.getById(successMetrics.getId());
    assertMetrics(returnedMetric, username, "testScopeString", metricsName, "testmetricsname", createTime);

    // attempt to update metric
    String username2 = "bob";
    successMetrics.setUsername(username2);
    assertThatThrownBy(() -> successMetricsReportDAO.update(successMetrics))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessage("SuccessMetricsReport does not support update operations.");

    // Delete
    successMetricsReportDAO.delete(successMetrics);

    // Retrieve metric and test
    assertThat(successMetricsReportDAO.getById(successMetrics.getId())).isNull();
  }

  @Test
  public void testGetByUsername() {
    String username = "test123";
    Date createTime1 = tempEntity.newSuccessMetricsReport(username, "metrics 1", "testMetricsString 1").getCreateTime();
    Date createTime2 = tempEntity.newSuccessMetricsReport(username, "metrics 2", "testMetricsString 2").getCreateTime();
    tempEntity.newSuccessMetricsReport("admin123", "metrics 3", "testMetricsString 3");

    // Retrieve metrics and test
    List<SuccessMetricsReport> actual = successMetricsReportDAO.getByUsername(username);
    assertThat(actual).hasSize(2);
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
  public void testInsert_ValidateNullName() {
    SuccessMetricsReport successMetrics = new SuccessMetricsReport(null);
    assertThatThrownBy(() -> successMetricsReportDAO.insert(successMetrics)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testInsert_ValidateEmptyName() {
    SuccessMetricsReport successMetrics = new SuccessMetricsReport("");
    assertThatThrownBy(() -> successMetricsReportDAO.insert(successMetrics)).isInstanceOf(InvalidNameException.class)
        .hasMessage("Name is required.");
  }

  @Test
  public void testInsert_ValidateNameInvalidChars() {
    for (String name : NameHelperTest.INVALID_CHARACTERS) {
      SuccessMetricsReport successMetrics = new SuccessMetricsReport(name);
      assertThatThrownBy(() -> successMetricsReportDAO.insert(successMetrics)).isInstanceOf(InvalidNameException.class)
          .hasMessage(NameHelper.INVALID_CHAR_MESSAGE, "Name", name.charAt(0));
    }
  }

  @Test
  public void testInsert_ValidateNameValidChars() {
    String username = "test123";
    for (String name : NameHelperTest.VALID_NAMES) {
      tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1");
    }
  }

  @Test
  public void testInsert_ValidateNameSpaces() {
    String username = "test123";
    for (String name : NameHelperTest.INVALID_SPACING_NAMES) {
      assertThatThrownBy(() -> tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1"))
          .isInstanceOf(InvalidNameException.class)
          .hasMessage("Name must not have leading or trailing spaces, or have two spaces in a row.");
    }
  }

  @Test
  public void testNameIsCaseAndWhitespaceInsensitive() {
    String username = "test123";
    String name = "test string With Case and Whitespace";
    SuccessMetricsReport successMetrics = tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1111");
    assertThat(successMetrics.getName()).isEqualTo(name);
    assertThat(successMetrics.getNameLowercaseNoWhitespace()).isEqualTo("teststringwithcaseandwhitespace");

    String name1 = "TEST String      With    cASE and      whitespace";
    SuccessMetricsReport actual = successMetricsReportDAO.getByUsernameAndName(username, name1);
    assertThat(actual).isNotNull();
    assertThat(actual.getId()).isEqualTo(successMetrics.getId());
  }

  @Test
  public void testInsert_DuplicateName() {
    String username = "test123";
    tempEntity.newSuccessMetricsReport(username, "Metrics12345", "testMetricsString 1111");
    assertThatThrownBy(() -> tempEntity.newSuccessMetricsReport(username, "METRICS 12345", "testMetricsString 1111"))
        .isInstanceOf(BadRequestException.class)
        .hasMessage("METRICS 12345 is already used as a name.");
  }

  @Test
  public void testInsert_ValidateNameLength() {
    String username = "test123";
    String name = StringUtils.repeat("a", NameHelper.MAX_NAME_LENGTH);
    assertThatThrownBy(() -> tempEntity.newSuccessMetricsReport(username, name + "a", "testMetricsString 1111"))
        .isInstanceOf(InvalidNameException.class)
        .hasMessage("Name must be 60 characters or less.");
    tempEntity.newSuccessMetricsReport(username, name, "testMetricsString 1111");
  }

  @Test
  public void testDelete_DeletesRelatedSuccessMetricsReportData() {
    SuccessMetricsReport report = tempEntity.newSuccessMetricsReport("username", "metrics", "{}");
    SuccessMetricsReportData reportData = tempEntity.newSuccessMetricsReportData(report.getId());

    successMetricsReportDAO.delete(report);

    assertThat(successMetricsReportDataDAO.getById(reportData.getId())).isNull();
  }

  private void assertMetrics(
      SuccessMetricsReport actualMetrics,
      String username,
      String scopeJson,
      String name,
      String nameLowercaseNoWhitespace,
      Date createTime)
  {
    assertThat(actualMetrics).isNotNull();
    assertThat(actualMetrics.getId()).isNotNull();
    assertThat(actualMetrics.getCreateTime()).isEqualTo(createTime);
    assertThat(actualMetrics.getUsername()).isEqualTo(username);
    assertThat(actualMetrics.getScopeJson()).isEqualTo(scopeJson);
    assertThat(actualMetrics.getName()).isEqualTo(name);
    assertThat(actualMetrics.getNameLowercaseNoWhitespace()).isEqualTo(nameLowercaseNoWhitespace);
  }
}
