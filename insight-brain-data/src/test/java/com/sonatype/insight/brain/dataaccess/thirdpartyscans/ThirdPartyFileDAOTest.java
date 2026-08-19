/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyUnknownComponent;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyFileDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyScanDAO thirdPartyScanDAO;

  private ThirdPartyUnknownComponentDAO thirdPartyUnknownComponentDAO;

  private ThirdPartyFileDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    thirdPartyScanDAO = daoFactory.createThirdPartyScanDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyUnknownComponentDAO = daoFactory.createThirdPartyUnknownComponentDAO();
    dao = daoFactory.createThirdPartyFileDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    Date created = new Date();
    ThirdPartyFile entity = new ThirdPartyFile("filename", created);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    final ThirdPartyFile retrievedThirdPartyFile = dao.getById(entity.getId());
    assertThirdPartyScannedFile(entity.getId(), "filename", created, retrievedThirdPartyFile);

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
  public void testDelete_Cascade() {
    // one scan, two coordinates with each having some sec issues
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartyFileCoordinate coord1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate coord2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n2", "v2");
    ThirdPartyCoordinateSecurity tpcs11 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 5.5f, "Medium", "f1");
    ThirdPartyCoordinateSecurity tpcs12 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d2", "l2", 1.5f, "Low", null);
    ThirdPartyCoordinateSecurity tpcs21 =
        tempEntity.newThirdPartyCoordinateSecurity(coord2, "r1", "d1", "l1", 5.5f, "Medium", "f1");

    ThirdPartyCoordinateLicense coordLic11 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic12 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l2", "n2", "u2");
    ThirdPartyCoordinateLicense coordLic21 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic22 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l2", "n2", "u2");

    ThirdPartyUnknownComponent unknownComponent =
        tempEntity.newThirdPartyUnknownComponent("someFile.xml", thirdPartyFile);

    dao.delete(thirdPartyFile);

    assertThat(dao.getById(thirdPartyFile.getId())).isNull();
    assertThat(thirdPartyScanDAO.getById(scan.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord1.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord2.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs11.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs12.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs21.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs12.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs21.getId())).isNull();

    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic11.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic12.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic21.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic22.getId())).isNull();

    assertThat(thirdPartyUnknownComponentDAO.getById(unknownComponent.getId())).isNull();
  }

  @Test
  public void testGetByScanId_RepeatedFileInSameScan() {
    String scanRequestId = TemporaryEntity.uuid();
    String scanId = TemporaryEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile("f1");
    ThirdPartyFile thirdPartyFile2 = tempEntity.newThirdPartyFile("f2");

    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile1);
    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile2);

    List<ThirdPartyFile> retrievedThirdPartyFiles = dao.getByScanId(scanId);
    assertThat(retrievedThirdPartyFiles).hasSize(2);

    // Sorts the list to have a deterministic order when comparing and doing the asserts
    retrievedThirdPartyFiles = new ArrayList<>(retrievedThirdPartyFiles);
    retrievedThirdPartyFiles.sort(Comparator.comparing(ThirdPartyFile::getCreated));

    assertThirdPartyScannedFile(thirdPartyFile1.getId(), thirdPartyFile1.getFilename(), thirdPartyFile1.getCreated(),
        retrievedThirdPartyFiles.get(0));

    assertThirdPartyScannedFile(thirdPartyFile2.getId(), thirdPartyFile2.getFilename(), thirdPartyFile2.getCreated(),
        retrievedThirdPartyFiles.get(1));
  }

  @Test
  public void testGetByHashAndScanId_RepeatedFileInDifferentScans() {
    String scanRequestId = TemporaryEntity.uuid();
    String scanId = TemporaryEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile("f1");
    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile1);

    ThirdPartyFile thirdPartyFile2 = tempEntity.newThirdPartyFile("f2");
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), TemporaryEntity.uuid(), thirdPartyFile2);

    List<ThirdPartyFile> retrievedThirdPartyFiles = dao.getByScanId(scanId);
    assertThat(retrievedThirdPartyFiles).hasSize(1);

    assertThirdPartyScannedFile(thirdPartyFile1.getId(), thirdPartyFile1.getFilename(), thirdPartyFile1.getCreated(),
        retrievedThirdPartyFiles.get(0));
  }

  @Test
  public void testGetByScanId() {
    String scanRequestId = TemporaryEntity.uuid();
    String scanId = TemporaryEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    ThirdPartyFile thirdPartyFile2 = tempEntity.newThirdPartyFile();

    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile1);
    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile2);

    List<ThirdPartyFile> thirdPartyFiles = dao.getByScanId(scanId);

    assertThat(thirdPartyFiles).hasSize(2);

    ThirdPartyFile found = findByThirdPartyFileIdInList(thirdPartyFiles, thirdPartyFile1.getId());
    assertThirdPartyScannedFile(thirdPartyFile1.getId(), thirdPartyFile1.getFilename(), thirdPartyFile1.getCreated(),
        found);

    found = findByThirdPartyFileIdInList(thirdPartyFiles, thirdPartyFile2.getId());
    assertThirdPartyScannedFile(thirdPartyFile2.getId(), thirdPartyFile2.getFilename(),
        thirdPartyFile2.getCreated(), found);
  }

  @Test
  public void testDeleteByScanId() {
    String scanId = TemporaryEntity.uuid();

    ThirdPartyFile thirdPartyFile1 = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), scanId, thirdPartyFile1);

    dao.deleteByScanId(scanId);

    assertThat(dao.getByScanId(scanId)).isEmpty();
  }

  @Test
  public void testDelete_CascadeByThirdPartyFileId() {
    // one scan, two coordinates with each having some sec issues
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(thirdPartyFile);
    ThirdPartyFileCoordinate coord1 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    ThirdPartyFileCoordinate coord2 = tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n2", "v2");
    ThirdPartyCoordinateSecurity tpcs11 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 5.5f, "Medium", "f1");
    ThirdPartyCoordinateSecurity tpcs12 =
        tempEntity.newThirdPartyCoordinateSecurity(coord1, "r2", "d2", "l2", 1.5f, "Low", null);
    ThirdPartyCoordinateSecurity tpcs21 =
        tempEntity.newThirdPartyCoordinateSecurity(coord2, "r1", "d1", "l1", 5.5f, "Medium", "f1");

    ThirdPartyCoordinateLicense coordLic11 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic12 = tempEntity.newThirdPartyCoordinateLicense(coord1, "l2", "n2", "u2");
    ThirdPartyCoordinateLicense coordLic21 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l1", "n1", "u1");
    ThirdPartyCoordinateLicense coordLic22 = tempEntity.newThirdPartyCoordinateLicense(coord2, "l2", "n2", "u2");

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.delete(tx, thirdPartyFile.getId());
      tx.commit();
    }

    assertThat(dao.getById(thirdPartyFile.getId())).isNull();
    assertThat(thirdPartyScanDAO.getById(scan.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord1.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord2.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs11.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs12.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs21.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs12.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(tpcs21.getId())).isNull();

    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic11.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic12.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic21.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(coordLic22.getId())).isNull();
  }

  private void assertThirdPartyScannedFile(
      final String id,
      final String filename,
      final Date created,
      final ThirdPartyFile actual)
  {
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getFilename()).isEqualTo(filename);
    assertThat(actual.getCreated()).isEqualTo(created);
  }

  private ThirdPartyFile findByThirdPartyFileIdInList(List<ThirdPartyFile> list, String thirdPartyFileId) {
    return list.stream()
        .filter(thirdPartyFile -> thirdPartyFile.getId().equals(thirdPartyFileId))
        .findFirst()
        .get();
  }
}
