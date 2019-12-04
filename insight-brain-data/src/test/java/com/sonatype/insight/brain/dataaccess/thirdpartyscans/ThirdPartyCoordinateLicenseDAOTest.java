/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyCoordinateLicenseDAOTest
    extends AbstractDbDAOTest
{
  private final ThirdPartyCoordinateLicenseDAO dao = new ThirdPartyCoordinateLicenseDAO();

  @Test
  public void testCRUD() throws Exception {
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
}
