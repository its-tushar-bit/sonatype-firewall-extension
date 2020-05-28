/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.dataaccess.successmetrics.SuccessMetricsReportDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileDAO;
import com.sonatype.insight.brain.db.AggregationDataStoreProvider;
import com.sonatype.insight.brain.db.DataSourceFactory;
import com.sonatype.insight.brain.db.DatamartProvider;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.db.ThirdPartyScansProvider;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.successmetrics.SuccessMetricsReport;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.postgres.PostgresServer;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SqlDAOTest
{
  @Test
  public void testSqlDAOsAreStateless_ODSDatabase() {
    // SQL DAOs are supposed to be stateless.
    // In particular, if we reinitialize the database, any existent SQL DAO instance should work with the new database.
    // In this test, we create an instance of a DAO using the current default in-memory H2 db and switch the db to
    // postgres.
    // Any entity instance created by this DAO should be in the Postgres db, not the H2 db.
    OrganizationDAO beforeDAO = new OrganizationDAO();

    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      OperationalDataStoreProvider.init(postgres.getDatabaseConfig(), false);
      Organization org = new Organization("test");
      beforeDAO.insert(org);
      OrganizationDAO afterDAO = new OrganizationDAO();
      assertThat(afterDAO.getById(org.getId())).isNotNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testSqlDAOsAreStateless_DatamartDatabase() {
    // SQL DAOs are supposed to be stateless.
    // In particular, if we reinitialize the database, any existent SQL DAO instance should work with the new database.
    // In this test, we create an instance of a DAO using the current default in-memory H2 db and switch the db to
    // postgres.
    // Any entity instance created by this DAO should be in the Postgres db, not the H2 db.
    LicenseDAO beforeDAO = new LicenseDAO();
    beforeDAO.load();

    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      DatamartProvider.init(postgres.getDatabaseConfig());
      License license = new License(null, "test short name", "test long name");
      beforeDAO.insert(license);
      beforeDAO.load();
      LicenseDAO afterDAO = new LicenseDAO();
      assertThat(afterDAO.getById(license.getId())).isNotNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testSqlDAOsAreStateless_AggregationDatabase() {
    // SQL DAOs are supposed to be stateless.
    // In particular, if we reinitialize the database, any existent SQL DAO instance should work with the new database.
    // In this test, we create an instance of a DAO using the current default in-memory H2 db and switch the db to
    // postgres.
    // Any entity instance created by this DAO should be in the Postgres db, not the H2 db.
    SuccessMetricsReportDAO beforeDAO = new SuccessMetricsReportDAO();

    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      AggregationDataStoreProvider.init(postgres.getDatabaseConfig());
      SuccessMetricsReport successMetricsReport = new SuccessMetricsReport("test");
      successMetricsReport.setUsername("test");
      successMetricsReport.setScopeJson("");
      beforeDAO.insert(successMetricsReport);
      SuccessMetricsReportDAO afterDAO = new SuccessMetricsReportDAO();
      assertThat(afterDAO.getById(successMetricsReport.getId())).isNotNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }

  @Test
  public void testSqlDAOsAreStateless_ThirdPartScansDatabase() {
    // SQL DAOs are supposed to be stateless.
    // In particular, if we reinitialize the database, any existent SQL DAO instance should work with the new database.
    // In this test, we create an instance of a DAO using the current default in-memory H2 db and switch the db to
    // postgres.
    // Any entity instance created by this DAO should be in the Postgres db, not the H2 db.
    ThirdPartyFileDAO beforeDAO = new ThirdPartyFileDAO();

    DataSourceFactory.clear_ForTestsOnly();
    try (PostgresServer postgres = new PostgresServer()) {
      ThirdPartyScansProvider.init(postgres.getDatabaseConfig());
      ThirdPartyFile thirdPartyFile = new ThirdPartyFile("test", new Date());
      beforeDAO.insert(thirdPartyFile);
      ThirdPartyFileDAO afterDAO = new ThirdPartyFileDAO();
      assertThat(afterDAO.getById(thirdPartyFile.getId())).isNotNull();
    }
    finally {
      DataSourceFactory.clear_ForTestsOnly();
    }
  }
}
