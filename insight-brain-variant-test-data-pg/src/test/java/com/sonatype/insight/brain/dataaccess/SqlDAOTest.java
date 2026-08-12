/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlDAOTest
    extends AbstractDbDAOTest
{
  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testSqlDAOsAreStateless_ODSDatabase() {
    OrganizationDAO beforeDAO = daoFactory.createOrganizationDAO();
    Organization org = new Organization("test");
    beforeDAO.insert(org);
    OrganizationDAO afterDAO = daoFactory.createOrganizationDAO();
    assertThat(afterDAO.getById(org.getId())).isNotNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testSqlDAOsAreStateless_DatamartDatabase() {
    LicenseDAO beforeDAO = daoFactory.createLicenseDAO();
    beforeDAO.load();
    License license = new License(null, "test short name", "test long name");

    beforeDAO.insert(license);

    beforeDAO.load();
    LicenseDAO afterDAO = daoFactory.createLicenseDAO();
    assertThat(afterDAO.getById(license.getId())).isNotNull();
    beforeDAO.load(); // also sync license cache with new db state
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testSqlDAOsAreStateless_AggregationDatabase() {
    SuccessMetricsReportDAO beforeDAO = daoFactory.createSuccessMetricsReportDAO();
    SuccessMetricsReport successMetricsReport = new SuccessMetricsReport("test");
    successMetricsReport.setUsername("test");
    successMetricsReport.setScopeJson("");

    beforeDAO.insert(successMetricsReport);

    SuccessMetricsReportDAO afterDAO = daoFactory.createSuccessMetricsReportDAO();
    assertThat(afterDAO.getById(successMetricsReport.getId())).isNotNull();
  }

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testSqlDAOsAreStateless_ThirdPartScansDatabase() {
    ThirdPartyFileDAO beforeDAO = daoFactory.createThirdPartyFileDAO();

    ThirdPartyFile thirdPartyFile = new ThirdPartyFile("test", new Date());

    beforeDAO.insert(thirdPartyFile);

    ThirdPartyFileDAO afterDAO = daoFactory.createThirdPartyFileDAO();
    assertThat(afterDAO.getById(thirdPartyFile.getId())).isNotNull();
  }
}
