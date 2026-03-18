/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyScanDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyScanDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartyScanDAO();
  }

  @Test
  public void testCRUD() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    // Create
    String scanRequestId = TemporaryEntity.uuid();
    Date created = new Date();
    ThirdPartyScan entity = new ThirdPartyScan(thirdPartyFile.getId(), scanRequestId, created);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyScan retrievedScan = dao.getById(entity.getId());
    assertThirdPartyScan(entity.getId(), thirdPartyFile.getId(), scanRequestId, null, created, retrievedScan);

    // Update
    String updatedScanId = TemporaryEntity.uuid();
    retrievedScan.setScanId(updatedScanId);
    dao.update(retrievedScan);
    ThirdPartyScan updated = dao.getById(retrievedScan.getId());
    assertThat(updated.getScanId()).isEqualTo(updatedScanId);

    // Delete
    dao.delete(retrievedScan);
    retrievedScan = dao.getById(retrievedScan.getId());
    assertThat(retrievedScan).isNull();
  }

  @Test
  public void testGetByScannedFileIdAndScanId() {
    String scanId = TemporaryEntity.uuid();
    ThirdPartyScan scan = tempEntity.newThirdPartyScan(TemporaryEntity.uuid(), scanId);

    ThirdPartyScan retrievedMapping =
        dao.getByThirdPartyFileIdAndScanId(scan.getThirdPartyFileId(), scan.getScanId());

    assertThirdPartyScan(scan.getId(), scan.getThirdPartyFileId(), scan.getScanRequestId(), scanId,
        scan.getCreateTime(), retrievedMapping);
  }

  @Test
  public void testGetByScanId() {
    ThirdPartyScan scan = tempEntity.newThirdPartyScan();
    List<ThirdPartyScan> scanList = dao.getByScanId(scan.getScanId());

    assertThat(scanList).hasSize(1);
    assertThirdPartyScan(scan, scanList.get(0));
  }

  @Test
  public void testDeleteByThirdPartyFileId() {
    final ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    final ThirdPartyScan scan1 = tempEntity.newThirdPartyScan(thirdPartyFile);
    final ThirdPartyScan scan2 = tempEntity.newThirdPartyScan(thirdPartyFile);

    try (final TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByThirdPartyFileId(tx, thirdPartyFile.getId());
      tx.commit();
    }

    assertThat(dao.getById(scan1.getId())).isNull();
    assertThat(dao.getById(scan2.getId())).isNull();
  }

  @Test
  public void testGetByScanRequestId_MultipleRecords() {
    ThirdPartyScan expected1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan expected2 = tempEntity.newThirdPartyScan(expected1.getScanRequestId(), TemporaryEntity.uuid());

    List<ThirdPartyScan> found = dao.getByScanRequestId(expected1.getScanRequestId());

    assertThat(found).hasSize(2);

    assertThirdPartyScan(expected1.getId(), expected1.getThirdPartyFileId(), expected1.getScanRequestId(),
        expected1.getScanId(), expected1.getCreateTime(), found.get(0));

    assertThirdPartyScan(expected2.getId(), expected2.getThirdPartyFileId(), expected2.getScanRequestId(),
        expected2.getScanId(), expected2.getCreateTime(), found.get(1));
  }

  @Test
  public void testGetSingleByScanRequestId_Single() {
    ThirdPartyScan expected1 = tempEntity.newThirdPartyScan();

    ThirdPartyScan found = dao.getSingleByScanRequestId(expected1.getScanRequestId());
    assertThat(found).isNotNull();
    assertThat(found.getId()).isEqualTo(expected1.getId());
  }

  @Test
  public void testGetSingleByScanRequestId_Multiple() throws Exception {
    ThirdPartyScan expected1 = tempEntity.newThirdPartyScan();
    ThirdPartyScan expected2 = tempEntity.newThirdPartyScan(expected1.getScanRequestId(), TemporaryEntity.uuid());

    ThirdPartyScan found = dao.getSingleByScanRequestId(expected1.getScanRequestId());
    assertThat(found).isNotNull();
    assertThat(found.getId()).isIn(expected1.getId(), expected2.getId());
  }

  @Test
  public void testUpdateScanIdForScanRequest() throws Exception {
    ThirdPartyScan expected1 = tempEntity.newThirdPartyScan();
    tempEntity.newThirdPartyScan(expected1.getScanRequestId(), TemporaryEntity.uuid());

    dao.updateScanIdForScanRequest(expected1.getScanRequestId(), "newScanId");

    List<ThirdPartyScan> updated = dao.getByScanRequestId(expected1.getScanRequestId());
    assertThat(updated).hasSize(2).extracting("scanId").containsOnly("newScanId");
  }

  @Test
  public void testGetByThirdPartyFileId() {
    ThirdPartyScan expected = tempEntity.newThirdPartyScan();
    ThirdPartyScan result = dao.getByThirdPartyFileId(expected.getThirdPartyFileId());
    assertThat(result).isNotNull();
    assertThat(result.getScanId()).isEqualTo(expected.getScanId());
    assertThat(result.getScanRequestId()).isEqualTo(expected.getScanRequestId());
  }

  private void assertThirdPartyScan(final ThirdPartyScan expected, final ThirdPartyScan actual) {
    assertThirdPartyScan(expected.getId(), expected.getThirdPartyFileId(), expected.getScanRequestId(),
        expected.getScanId(), expected.getCreateTime(), actual);
  }

  private void assertThirdPartyScan(
      final String id,
      final String thirdPartyFileId,
      final String scanRequestId,
      final String scanId,
      final Date created,
      final ThirdPartyScan actual)
  {
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
    assertThat(actual.getScanRequestId()).isEqualTo(scanRequestId);
    assertThat(actual.getScanId()).isEqualTo(scanId);
    assertThat(actual.getCreateTime()).isEqualTo(created);
  }
}
