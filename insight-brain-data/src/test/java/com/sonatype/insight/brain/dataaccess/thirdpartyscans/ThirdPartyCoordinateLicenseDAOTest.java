/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sonatype.insight.brain.common.test.PostgresTestCategory;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.db.rule.DatabaseRuleAnnotations.PostgresTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyCoordinateLicenseDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyCoordinateLicenseDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartyCoordinateLicenseDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFileCoordinate coordinateFile = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateLicense entity = new ThirdPartyCoordinateLicense(coordinateFile.getId(), "Apache-2.0",
        "Apache", "https://www.apache.org/licenses/LICENSE-2.0");
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyCoordinateLicense retrievedCoordinateLicense = dao.getById(entity.getId());
    assertThirdPartyCoordinateLicense(retrievedCoordinateLicense, entity);

    // Update
    retrievedCoordinateLicense.setName("Name Updated");
    dao.update(retrievedCoordinateLicense);
    ThirdPartyCoordinateLicense updated = dao.getById(retrievedCoordinateLicense.getId());
    assertThat(updated.getName()).isEqualTo("Name Updated");

    // Delete
    dao.delete(retrievedCoordinateLicense);
    retrievedCoordinateLicense = dao.getById(retrievedCoordinateLicense.getId());
    assertThat(retrievedCoordinateLicense).isNull();
  }

  @Test
  public void testGetByCoordinateFileId() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateLicense coordLic1 = tempEntity.newThirdPartyCoordinateLicense(coord, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic2 = tempEntity.newThirdPartyCoordinateLicense(coord, "l2", "n2", "u2");

    final List<ThirdPartyCoordinateLicense> results = dao.getByFileCoordinateId(coord.getId());

    assertThat(results).usingElementComparator(Comparator.comparing(ThirdPartyCoordinateLicense::getId))
        .containsExactlyInAnyOrderElementsOf(Arrays.asList(coordLic1, coordLic2));
  }

  @Test
  public void testDeleteByFileCoordinateId() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "l1", "f1", "n1", "v1");
    ThirdPartyCoordinateLicense coordLic1 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic2 =
        tempEntity.newThirdPartyCoordinateLicense(coord1, "l2", "n3", "u3");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByFileCoordinateId(tx, coord1.getId());
      tx.commit();
    }
    assertThat(dao.getById(coordLic1.getId())).isNull();
    assertThat(dao.getById(coordLic2.getId())).isNull();
  }

  @Test
  public void testGetByFileCoordinateIdAndLicenseId() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    tempEntity.newThirdPartyCoordinateLicense(coord, "l1", "n1", "u1");

    final ThirdPartyCoordinateLicense result = dao.getByFileCoordinateIdAndLicenseId(coord.getId(), "l1");

    assertThat(result.getName()).isEqualTo("n1");
  }

  @Test
  public void testGetByFileCoordinateIdAndLicenseId_caseInsensitive() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    tempEntity.newThirdPartyCoordinateLicense(coord, "L1", "n1", "u1");

    final ThirdPartyCoordinateLicense result = dao.getByFileCoordinateIdAndLicenseId(coord.getId(), "l1");

    assertThat(result.getName()).isEqualTo("n1");
  }

  @Test
  public void testGetByComponentHash() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1", "sharedHash",
            "pkg:f1/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s2", "f2", "n2", "v2", "sharedHash",
            "pkg:f2/n2@v2");
    ThirdPartyCoordinateLicense lic1 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense lic2 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l2", "n2", "u2");

    List<ThirdPartyCoordinateLicense> results = dao.getByComponentHash("sharedHash");

    assertThat(results).usingElementComparator(Comparator.comparing(ThirdPartyCoordinateLicense::getId))
        .containsExactlyInAnyOrderElementsOf(Arrays.asList(lic1, lic2));
  }

  @Test
  public void testGetByComponentHashes_multipleHashes() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1", "hash1",
            "pkg:f1/n1@v1");
    ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s2", "f2", "n2", "v2", "hash2",
            "pkg:f2/n2@v2");
    ThirdPartyCoordinateLicense lic1 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense lic2 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l2", "n2", "u2");
    ThirdPartyCoordinateLicense lic3 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l3", "n3", "u3");

    Map<String, List<ThirdPartyCoordinateLicense>> results = dao.getByComponentHashes(Set.of("hash1", "hash2"));

    assertThat(results).containsOnlyKeys("hash1", "hash2");
    assertThat(results.get("hash1")).usingElementComparator(Comparator.comparing(ThirdPartyCoordinateLicense::getId))
        .containsExactly(lic1);
    assertThat(results.get("hash2")).usingElementComparator(Comparator.comparing(ThirdPartyCoordinateLicense::getId))
        .containsExactlyInAnyOrder(lic2, lic3);
  }

  @Test
  public void testGetByComponentHashes_noMatch() {
    Map<String, List<ThirdPartyCoordinateLicense>> results = dao.getByComponentHashes(Set.of("nonExistentHash"));

    assertThat(results).isEmpty();
  }

  @Test
  public void testGetByComponentHash_noMatch() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1", "someHash",
            "pkg:f1/n1@v1");
    tempEntity.newThirdPartyCoordinateLicense(coord, "l1", "n1", "u1");

    List<ThirdPartyCoordinateLicense> results = dao.getByComponentHash("nonExistentHash");

    assertThat(results).isEmpty();
  }

  private void assertThirdPartyCoordinateLicense(
      final ThirdPartyCoordinateLicense expected,
      final ThirdPartyCoordinateLicense actual)
  {
    assertThat(actual.getFileCoordinateId()).isEqualTo(expected.getFileCoordinateId());
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getName()).isEqualTo(expected.getName());
    assertThat(actual.getLicenseId()).isEqualTo(expected.getLicenseId());
    assertThat(actual.getUrl()).isEqualTo(expected.getUrl());
  }

  @Test
  public void testInsertSafelyBatch_mixedNewAndExisting() {
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertSafelyBatch_mixedNewAndExisting_postgres() {
    testInsertSafelyBatch_mixedNewAndExisting();
  }

  @Test
  public void testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively() {
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

  @Test
  @Category(PostgresTestCategory.class)
  @PostgresTest
  public void testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively_postgres() {
    testInsertSafelyBatch_matchesExistingLicenseIdCaseInsensitively();
  }
}
