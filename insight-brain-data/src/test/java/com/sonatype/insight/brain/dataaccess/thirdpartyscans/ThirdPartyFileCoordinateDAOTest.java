/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateLicense;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyCoordinateSecurity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFileCoordinate;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyFileCoordinateDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyFileCoordinateDAO thirdPartyFileCoordinateDAO;

  private ThirdPartyCoordinateSecurityDAO thirdPartyCoordinateSecurityDAO;

  private ThirdPartyFileCoordinate fileCoordinate;

  private ThirdPartyCoordinateLicenseDAO thirdPartyCoordinateLicenseDAO;

  @Before
  @Override
  public void setup() {
    super.setup();
    thirdPartyCoordinateSecurityDAO = daoFactory.createThirdPartyCoordinateSecurityDAO();
    thirdPartyFileCoordinateDAO = daoFactory.createThirdPartyFileCoordinateDAO();
    thirdPartyCoordinateLicenseDAO = daoFactory.createThirdPartyCoordinateLicenseDAO();
    fileCoordinate = tempEntity.newThirdPartyFileCoordinate();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFile scannedFile = tempEntity.newThirdPartyFile();

    ThirdPartyFileCoordinate entity =
        new ThirdPartyFileCoordinate("filehash2", "source", "format",
            "name2", "version2", scannedFile.getId());
    thirdPartyFileCoordinateDAO.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyFileCoordinate retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(entity.getId());
    assertThirdPartyCoordinateFile("filehash2", "source", "format", "name2", "version2", scannedFile.getId(), entity);

    // Update
    retrievedCoordinateFile.setName("UpdatedName");
    thirdPartyFileCoordinateDAO.update(retrievedCoordinateFile);
    ThirdPartyFileCoordinate updated = thirdPartyFileCoordinateDAO.getById(retrievedCoordinateFile.getId());
    assertThat(updated.getName()).isEqualTo("UpdatedName");

    // Delete
    thirdPartyFileCoordinateDAO.delete(retrievedCoordinateFile);
    retrievedCoordinateFile = thirdPartyFileCoordinateDAO.getById(retrievedCoordinateFile.getId());
    assertThat(retrievedCoordinateFile).isNull();
  }

  @Test
  public void testGetBySourceFormatNameVersionAndScannedFileId() {
    List<ThirdPartyFileCoordinate> retrievedCoordinateFile = thirdPartyFileCoordinateDAO
        .getBySourceFormatNameVersionAndThirdPartyFileId(fileCoordinate.getSource(), fileCoordinate.getFormat(),
            fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId());

    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(),
        retrievedCoordinateFile.get(0));
  }

  @Test
  public void testGetByThirdPartyFileId() {
    List<ThirdPartyFileCoordinate> results =
        thirdPartyFileCoordinateDAO.getByThirdPartyFileId(fileCoordinate.getThirdPartyFileId());

    assertThat(results).hasSize(1);
    assertThirdPartyCoordinateFile(fileCoordinate.getHash(), fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getThirdPartyFileId(), results.get(0));
  }

  @Test
  public void testGetByHashAndScanId() {
    String scanId = TemporaryEntity.uuid();
    String hash = tempEntity.newRandomHash();
    List<ThirdPartyFileCoordinate> fileCoordinateList = createThirdPartyScans(scanId, hash);
    List<ThirdPartyFileCoordinate> results =
        thirdPartyFileCoordinateDAO.getByHashAndScanId(hash, scanId);

    assertThat(results).hasSize(2);

    Comparator<ThirdPartyFileCoordinate> thirdPartySecurityComparator =
        Comparator.comparing(ThirdPartyFileCoordinate::getHash);

    assertThat(results).usingElementComparator(thirdPartySecurityComparator)
        .containsAnyElementsOf(fileCoordinateList);
  }

  @Test
  public void testDeleteByThirdPartyFileId() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    final ThirdPartyFileCoordinate coord1 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    final ThirdPartyFileCoordinate coord2 =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n2", "v2");
    final ThirdPartyCoordinateSecurity sec1 =
        tempEntity
            .newThirdPartyCoordinateSecurity(coord1, "r1", "d1", "l1", 1.1f, "2.1", "CVE", "v:1", "Low", "<dd>c1</>",
                "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>");
    final ThirdPartyCoordinateSecurity sec2 = tempEntity
        .newThirdPartyCoordinateSecurity(coord1, "r2", "d2", "l2", 1.2f, "2.2", "CVE", "v:2", "Low", "<dd>c2</>",
            "CVSSv2", "<dd>r2</dd>", "<dd>a2</dd>");

    try (TransactionContext tx = thirdPartyFileCoordinateDAO.createTransactionContext()) {
      tx.begin();
      thirdPartyFileCoordinateDAO.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());
      tx.commit();
    }

    assertThat(thirdPartyFileCoordinateDAO.getById(coord1.getId())).isNull();
    assertThat(thirdPartyFileCoordinateDAO.getById(coord2.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec1.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec2.getId())).isNull();
  }

  @Test
  public void testDelete_Cascade() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    final ThirdPartyFileCoordinate coordinate =
        tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, "s1", "f1", "n1", "v1");
    final ThirdPartyCoordinateSecurity sec =
        tempEntity.newThirdPartyCoordinateSecurity(coordinate, "r1", "d1", "l1", 1.1f, "2.1", "CVE", "v:1", "Low",
            "<dd>c1</>", "CVSSv3", "<dd>r1</dd>", "<dd>a1</dd>");
    final ThirdPartyCoordinateLicense lic =
        tempEntity.newThirdPartyCoordinateLicense(coordinate, "lic1", "name1", "url");

    thirdPartyFileCoordinateDAO.delete(coordinate);

    assertThat(thirdPartyFileCoordinateDAO.getById(coordinate.getId())).isNull();
    assertThat(thirdPartyCoordinateSecurityDAO.getById(sec.getId())).isNull();
    assertThat(thirdPartyCoordinateLicenseDAO.getById(lic.getId())).isNull();
  }

  @Test
  public void testGetByScanId() {
    String scanId = TemporaryEntity.uuid();
    String hash = tempEntity.newRandomHash();
    createThirdPartyScans(scanId, hash);

    List<ThirdPartyFileCoordinate> fileCoordinates = thirdPartyFileCoordinateDAO.getByScanId(scanId);

    assertThat(fileCoordinates).hasSize(3);
    assertThat(fileCoordinates.stream().map(ThirdPartyFileCoordinate::getSource))
        .containsExactlyInAnyOrder("s1", "s2", "s3");
  }

  private void assertThirdPartyCoordinateFile(
      final String hash,
      final String source,
      final String format,
      final String name,
      final String version,
      final String thirdPartyFileId, final ThirdPartyFileCoordinate entity)
  {
    assertThat(entity.getHash()).isEqualTo(hash);
    assertThat(entity.getSource()).isEqualTo(source);
    assertThat(entity.getName()).isEqualTo(name);
    assertThat(entity.getFormat()).isEqualTo(format);
    assertThat(entity.getVersion()).isEqualTo(version);
    assertThat(entity.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
  }

  private List<ThirdPartyFileCoordinate> createThirdPartyScans(String scanId, String hash) {
    List<ThirdPartyFileCoordinate> fileCoordinateList = new ArrayList<>();

    String scanRequestId = TemporaryEntity.uuid();

    ThirdPartyFileCoordinate fileCoordinate1 = new ThirdPartyFileCoordinate(hash, "s1", "f1", "n1", "v1", null);
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate1);
    fileCoordinateList.add(fileCoordinate1);

    ThirdPartyFileCoordinate fileCoordinate2 = new ThirdPartyFileCoordinate(hash, "s2", "f2", "n2", "v2", null);
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate2);
    fileCoordinateList.add(fileCoordinate2);

    ThirdPartyFileCoordinate fileCoordinate3 =
        new ThirdPartyFileCoordinate(tempEntity.newRandomHash(), "s3", "f3", "n3", "v3", null);
    newThirdPartyScan(scanId, scanRequestId, fileCoordinate3);
    fileCoordinateList.add(fileCoordinate3);

    return fileCoordinateList;
  }

  private void newThirdPartyScan(String scanId, String scanRequestId, ThirdPartyFileCoordinate fileCoordinate) {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    tempEntity.newThirdPartyFileCoordinate(thirdPartyFile, fileCoordinate.getSource(), fileCoordinate.getFormat(),
        fileCoordinate.getName(), fileCoordinate.getVersion(), fileCoordinate.getHash(),
        fileCoordinate.getPackageUrl());
    tempEntity.newThirdPartyScan(scanRequestId, scanId, thirdPartyFile);
  }
}
