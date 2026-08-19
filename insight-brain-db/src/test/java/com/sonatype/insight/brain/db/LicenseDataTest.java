/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Collection;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LicenseDataTest
    extends AbstractDatabaseTest
{
  private void assertEmptyQuery(String sql) throws Exception {
    try (Connection connection = databaseRule.getDataMartDataStore().getDataSource().getConnection()) {
      try (Statement statement = connection.createStatement()) {
        try (ResultSet results = statement.executeQuery(sql)) {
          Collection<String> ids = new TreeSet<>();
          while (results.next()) {
            ids.add(results.getString(1));
          }
          assertThat(ids).isEmpty();
        }
      }
    }
  }

  @Test
  public void testEveryMultiLicenseReferencesAtLeastOneLicense() throws Exception {
    assertEmptyQuery("SELECT multi_license_id FROM insight_brain_dm.multi_license WHERE multi_license_id NOT IN " +
        "(SELECT multi_license_id FROM insight_brain_dm.multi_license_license)");
  }

  @Test
  public void testEveryLicenseIsReferencedByAtLeastOneMultiLicense() throws Exception {
    assertEmptyQuery("SELECT license_id FROM insight_brain_dm.license WHERE license_id NOT IN " +
        "(SELECT license_id FROM insight_brain_dm.multi_license_license)");
  }

  @Test
  public void testEveryLicenseCorrespondsToOneMultiLicense() throws Exception {
    assertEmptyQuery("SELECT license_id FROM insight_brain_dm.license WHERE license_id NOT IN " +
        "(SELECT multi_license_id FROM insight_brain_dm.multi_license)");
  }

  @Test
  public void testLicenseAndCorrespondingMultiLicenseHaveSameNames() throws Exception {
    assertEmptyQuery("SELECT l.license_id FROM insight_brain_dm.license l, insight_brain_dm.multi_license ml " +
        "WHERE l.license_id=ml.multi_license_id " +
        "AND (l.shortDisplayName!=ml.shortDisplayName OR l.longDisplayName!=ml.longDisplayName)");
  }
}
