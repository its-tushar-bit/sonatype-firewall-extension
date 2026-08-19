/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.thirdparty;

import jakarta.inject.Inject;

import com.sonatype.insight.brain.dataaccess.MigrationTrackerDAO;
import com.sonatype.insight.brain.dataaccess.thirdpartyscans.ThirdPartyFileCoordinateDAO;
import com.sonatype.insight.brain.migration.DisplayNameForFileCoordinateAsyncDbMigration;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.variant.AbstractComponentH2Test;
import com.sonatype.insight.brain.variant.ComponentH2Test;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@ComponentH2Test
public class DisplayNameForFileCoordinateAsyncDbMigrationTest
    extends AbstractComponentH2Test
{
  @Inject
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  @Inject
  private MigrationTrackerDAO migrationTrackerDAO;

  @Inject
  private DisplayNameForFileCoordinateAsyncDbMigration underTest;

  @Test
  public void testMigrationDisplayNameFromPackageUrl() {
    String validPurl = "pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1?type=war";
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();

    createThirdPartyFileCoordinate(tpFile, validPurl, "maven", "log4j-core", "2.14.1");

    // Ensure the migration tracker does not exist
    migrationTrackerDAO.deleteById(underTest.getMigrationName());
    assertThat(migrationTrackerDAO.getById(underTest.getMigrationName())).isNull();

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
        validPurl);
    assertThat(result).isNotNull();
    assertThat(result.getDisplayName()).isNull();

    underTest.runMigration();

    ThirdPartyFileCoordinate resultUpdated =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
            validPurl);
    assertThat(resultUpdated).isNotNull();
    assertThat(resultUpdated.getDisplayName()).isEqualTo("org.apache.logging.log4j : log4j-core : war : 2.14.1");

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigrationDisplayName_invalidPackageUrlAndValidFormat() {
    String invalidPurl = "invalidPurlForTest";
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();

    createThirdPartyFileCoordinate(tpFile, invalidPurl, "cpe", "p1", "v1");

    // Ensure the migration tracker does not exist
    migrationTrackerDAO.deleteById(underTest.getMigrationName());
    assertThat(migrationTrackerDAO.getById(underTest.getMigrationName())).isNull();

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
        invalidPurl);
    assertThat(result).isNotNull();
    assertThat(result.getDisplayName()).isNull();

    underTest.runMigration();

    ThirdPartyFileCoordinate resultUpdated =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(), invalidPurl);
    assertThat(resultUpdated).isNotNull();
    assertThat(resultUpdated.getDisplayName()).isEqualTo(" : p1 : v1");

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  @Test
  public void testMigrationDisplayName_invalidPackageUrlAndFormat() {
    String invalidPurl = "invalidPurlForTest";
    ThirdPartyFile tpFile = tempEntity.newThirdPartyFile();

    createThirdPartyFileCoordinate(tpFile, invalidPurl, "hf-repo", "p1", "v1");

    // Ensure the migration tracker does not exist
    migrationTrackerDAO.deleteById(underTest.getMigrationName());
    assertThat(migrationTrackerDAO.getById(underTest.getMigrationName())).isNull();

    ThirdPartyFileCoordinate result = thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(),
        invalidPurl);
    assertThat(result).isNotNull();
    assertThat(result.getDisplayName()).isNull();

    underTest.runMigration();

    ThirdPartyFileCoordinate resultUpdated =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileIdAndPackageUrl(tpFile.getId(), invalidPurl);
    assertThat(resultUpdated).isNotNull();
    assertThat(resultUpdated.getDisplayName()).isEqualTo("p1 : v1");

    MigrationTracker newMigrationTracker = migrationTrackerDAO.getById(underTest.getMigrationName());
    assertThat(newMigrationTracker).isNotNull();
  }

  private void createThirdPartyFileCoordinate(
      ThirdPartyFile tpFile,
      String packageUrl,
      String format,
      String name,
      String version)
  {
    ThirdPartyFileCoordinate thirdPartyFileCoordinate =
        tempEntity.newThirdPartyFileCoordinate(tpFile, "Third-Party", format, name, version, "absdefghijklman",
            packageUrl);

    // This simulates empty display_name row
    thirdPartyFileCoordinate.setDisplayName(null);
    thirdPartyFileCoordinateDAO.update(thirdPartyFileCoordinate);
  }
}
