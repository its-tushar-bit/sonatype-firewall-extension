/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Arrays;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL-backed tests relocated from {@link ThirdPartyCoordinateLicenseDAOTest} (CLM-45228).
 */
@PostgresTest
public class ThirdPartyCoordinateLicenseDAOPgTest
    extends AbstractDbDAOTest
{
  private ThirdPartyCoordinateLicenseDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartyCoordinateLicenseDAO();
  }

  @Test
  public void testInsertSafelyBatch_mixedNewAndExisting_postgres() {
    testInsertSafelyBatch_mixedNewAndExisting();
  }

  @Test
  public void testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively_postgres() {
    testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively();
  }

  private void testInsertSafelyBatch_mixedNewAndExisting() {
    ThirdPartyFileCoordinate coord = tempEntity.newThirdPartyFileCoordinate();
    ThirdPartyCoordinateLicense existing =
        tempEntity.newThirdPartyCoordinateLicense(coord, "Apache-2.0", "Apache", "https://apache.org");

    ThirdPartyCoordinateLicense duplicateOfExisting = new ThirdPartyCoordinateLicense(coord.getId(),
        "Apache-2.0", "Apache-should-ignore", "https://ignored");
    ThirdPartyCoordinateLicense newRow = new ThirdPartyCoordinateLicense(coord.getId(),
        "MIT", "MIT", "https://opensource.org/mit");
    ThirdPartyCoordinateLicense duplicateInInput = new ThirdPartyCoordinateLicense(coord.getId(),
        "MIT", "MIT-again", "https://opensource.org/mit-again");

    int inserted;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      inserted = dao.insertSafelyBatch(tx, Arrays.asList(duplicateOfExisting, newRow, duplicateInInput));
      tx.commit();
    }
    assertThat(inserted).isEqualTo(1);

    List<ThirdPartyCoordinateLicense> stored = dao.getByFileCoordinateId(coord.getId());
    assertThat(stored).extracting(ThirdPartyCoordinateLicense::getLicenseId)
        .containsExactlyInAnyOrder("Apache-2.0", "MIT");
    ThirdPartyCoordinateLicense keptApache = stored.stream()
        .filter(l -> "Apache-2.0".equals(l.getLicenseId()))
        .findFirst()
        .orElseThrow();
    assertThat(keptApache.getId()).isEqualTo(existing.getId());
    assertThat(keptApache.getName()).isEqualTo(existing.getName());
  }

  private void testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively() {
    ThirdPartyFileCoordinate coord = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateLicense existing = tempEntity.newThirdPartyCoordinateLicense(coord,
        "Apache-2.0", "Apache", "https://apache.org");

    ThirdPartyCoordinateLicense mixedCase = new ThirdPartyCoordinateLicense(coord.getId(),
        "apache-2.0", "should-be-ignored", "https://ignored");

    int inserted;
    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      inserted = dao.insertSafelyBatch(tx, Arrays.asList(mixedCase));
      tx.commit();
    }
    assertThat(inserted).isEqualTo(0);

    List<ThirdPartyCoordinateLicense> stored = dao.getByFileCoordinateId(coord.getId());
    assertThat(stored).hasSize(1);
    assertThat(stored.get(0).getId()).isEqualTo(existing.getId());
    assertThat(stored.get(0).getName()).isEqualTo(existing.getName());
  }
}
