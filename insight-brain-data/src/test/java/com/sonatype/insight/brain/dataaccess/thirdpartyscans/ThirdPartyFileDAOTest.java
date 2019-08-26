/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyFileDAOTest
    extends AbstractDbDAOTest
{
  private final ThirdPartyFileDAO dao = new ThirdPartyFileDAO();

  @Test
  public void testCRUD() throws Exception {
    // Create
    Date created = new Date();
    ThirdPartyFile entity = new ThirdPartyFile("hash", "filename", "image", created);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    final ThirdPartyFile retrievedThirdPartyFile = dao.getById(entity.getId());
    assertThirdPartyScannedFile(entity.getId(), "hash", "filename", "image", created, retrievedThirdPartyFile);

    // Update
    retrievedThirdPartyFile.setFilename("updated filename");
    dao.update(retrievedThirdPartyFile);
    assertThat(retrievedThirdPartyFile.getFilename()).isEqualTo("updated filename");

    // Delete
    dao.delete(retrievedThirdPartyFile);
    ThirdPartyFile deletedScannedFile = dao.getById(retrievedThirdPartyFile.getId());
    assertThat(deletedScannedFile).isNull();
  }

  @Test
  public void testDelete_Cascade() throws Exception {
    //one scan, two coordinates with each having some sec issues
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartyFileCoordinate coord1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1","f1","n1","v1");
    ThirdPartyFileCoordinate coord2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1","f1","n2","v2");
    ThirdPartyCoordinateSecurity tpcs11 = tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1","l1",5.5f,"f1");
    ThirdPartyCoordinateSecurity tpcs12 = tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d2","l2",1.5f,null);
    ThirdPartyCoordinateSecurity tpcs21 = tempEntity.newThirdPartyCoordinateSecurity(coord2, "r1", "d1","l1",5.5f,"f1");

    dao.delete(thirdPartyFile);

    ThirdPartyFileCoordinateDAO coordDAO = new ThirdPartyFileCoordinateDAO();
    ThirdPartyFileCoordinateDAO coordSecurityDAO = new ThirdPartyFileCoordinateDAO();

    assertThat(dao.getById(thirdPartyFile.getId())).isNull();
    assertThat(new ThirdPartyScanDAO().getById(scan.getId())).isNull();
    assertThat(coordDAO.getById(coord1.getId())).isNull();
    assertThat(coordDAO.getById(coord2.getId())).isNull();
    assertThat(coordSecurityDAO.getById(tpcs11.getId())).isNull();
    assertThat(coordSecurityDAO.getById(tpcs12.getId())).isNull();
    assertThat(coordSecurityDAO.getById(tpcs21.getId())).isNull();
  }

  @Test
  public void testGetByHash() {
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();
    ThirdPartyFile retrievedThirdPartyFile = dao.getByHash(scannedFile.getHash());
    assertThirdPartyScannedFile(scannedFile.getId(), scannedFile.getHash(), scannedFile.getFilename(),
        scannedFile.getImage(), scannedFile.getCreated(), retrievedThirdPartyFile);
  }

  private void assertThirdPartyScannedFile(
      final String id,
      final String hash,
      final String filename,
      final String image,
      final Date created,
      final ThirdPartyFile actual)
  {
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getHash()).isEqualTo(hash);
    assertThat(actual.getFilename()).isEqualTo(filename);
    assertThat(actual.getImage()).isEqualTo(image);
    assertThat(actual.getCreated()).isEqualTo(created);
  }
}
