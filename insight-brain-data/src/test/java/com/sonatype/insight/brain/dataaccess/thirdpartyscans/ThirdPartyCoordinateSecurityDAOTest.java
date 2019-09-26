/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyCoordinateSecurityDAOTest
    extends AbstractDbDAOTest
{
  private final ThirdPartyCoordinateSecurityDAO dao = new ThirdPartyCoordinateSecurityDAO();

  private static final Comparator<ThirdPartyCoordinateSecurity> THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR =
      Comparator.comparing(ThirdPartyCoordinateSecurity::getRefId)
          .thenComparing(ThirdPartyCoordinateSecurity::getFileCoordinateId);

  @Test
  public void testCRUD() throws Exception {
    // Create
    ThirdPartyFileCoordinate coordinateFile = tempEntity.newThirdPartyFileCoordinate();

    ThirdPartyCoordinateSecurity entity =
        new ThirdPartyCoordinateSecurity(coordinateFile.getId(), "refid", "description",
            "link", 6.8f, null);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity = dao.getById(entity.getId());
    assertThirdPartyCoordinateSecurity("refid", "description", "link", 6.8f, null, coordinateFile.getId(),
        retrievedCoordinateSecurity);

    // Update
    retrievedCoordinateSecurity.setDescription("Description updated");
    dao.update(retrievedCoordinateSecurity);
    ThirdPartyCoordinateSecurity updated = dao.getById(retrievedCoordinateSecurity.getId());
    assertThat(updated.getDescription()).isEqualTo("Description updated");

    // Delete
    dao.delete(retrievedCoordinateSecurity);
    retrievedCoordinateSecurity = dao.getById(retrievedCoordinateSecurity.getId());
    assertThat(retrievedCoordinateSecurity).isNull();
  }

  @Test
  public void testGetByCoordinateFileIdAndRefId() {
    ThirdPartyCoordinateSecurity coordinateSecurity = tempEntity.newThirdPartyCoordinateSecurity();
    ThirdPartyCoordinateSecurity retrievedCoordinateSecurity =
        dao.getByCoordinateFileIdAndRefId(coordinateSecurity.getFileCoordinateId(), coordinateSecurity.getRefId());

    assertThirdPartyCoordinateSecurity(coordinateSecurity.getRefId(), coordinateSecurity.getDescription(),
        coordinateSecurity.getLink(), coordinateSecurity.getSeverity(), coordinateSecurity.getFixedBy(),
        coordinateSecurity.getFileCoordinateId(), retrievedCoordinateSecurity);
  }

  @Test
  public void testGetByFileCoordinateIdList() {
    List<ThirdPartyCoordinateSecurity> coordinateSecurityList = newThirdPartyCoordinateSecurityList();
    List<String> listId =
        coordinateSecurityList.stream().map(ThirdPartyCoordinateSecurity::getFileCoordinateId)
            .collect(Collectors.toList());
    List<ThirdPartyCoordinateSecurity> results = dao.getByFileCoordinateIds(listId);

    assertThat(results).usingElementComparator(THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(coordinateSecurityList);
  }

  @Test
  public void testGetByFileCoordinateId() {
    ThirdPartyFileCoordinate coord =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity coordSec1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord, "r1", "d1", "l1", 1.1f, "f1");
    ThirdPartyCoordinateSecurity coordSec2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord, "r2", "d1", "l2", 1.1f, "f2");

    final List<ThirdPartyCoordinateSecurity> results = dao.getByFileCoordinateId(coord.getId());
    assertThat(results).usingElementComparator(THIRD_PARTY_COORDINATE_SECURITY_COMPARATOR)
        .containsExactlyInAnyOrderElementsOf(Arrays.asList(coordSec1, coordSec2));
  }

  @Test
  public void testDeleteByFileCoordinateId() {
    ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(tempEntity.newThirdPartyFile(), "s1", "f1", "n1", "v1");
    ThirdPartyCoordinateSecurity coordSec1 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 1.1f, "f1");
    ThirdPartyCoordinateSecurity coordSec2 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d1", "l2", 1.1f, "f2");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByFileCoordinateId(tx, coord1.getId());
      tx.commit();
    }
    assertThat(dao.getById(coordSec1.getId())).isNull();
    assertThat(dao.getById(coordSec2.getId())).isNull();
  }

  private void assertThirdPartyCoordinateSecurity(
      final String refId,
      final String description,
      final String link,
      final float score,
      final String fixedBy,
      final String cooedinateFileId, final ThirdPartyCoordinateSecurity actual)
  {
    assertThat(actual.getRefId()).isEqualTo(refId);
    assertThat(actual.getDescription()).isEqualTo(description);
    assertThat(actual.getLink()).isEqualTo(link);
    assertThat(actual.getSeverity()).isEqualTo(score);
    assertThat(actual.getFixedBy()).isEqualTo(fixedBy);
    assertThat(actual.getFileCoordinateId()).isEqualTo(cooedinateFileId);
  }

  private List<ThirdPartyCoordinateSecurity> newThirdPartyCoordinateSecurityList() {
    List<ThirdPartyCoordinateSecurity> list = new ArrayList<>();

    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity1 =
        tempEntity.newThirdPartyCoordinateSecurity(tempEntity.newThirdPartyFileCoordinate(), "r1", "d1", "l1", 5.5f,
            "1.1");
    list.add(thirdPartyCoordinateSecurity1);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity2 =
        tempEntity.newThirdPartyCoordinateSecurity(tempEntity.newThirdPartyFileCoordinate(), "r2", "d2", "l2", 1f,
            "1.2");
    list.add(thirdPartyCoordinateSecurity2);
    ThirdPartyCoordinateSecurity thirdPartyCoordinateSecurity3 =
        tempEntity.newThirdPartyCoordinateSecurity(tempEntity.newThirdPartyFileCoordinate(), "r3", "d3", "l3", 10f,
            "1.3");
    list.add(thirdPartyCoordinateSecurity3);

    return list;
  }
}
