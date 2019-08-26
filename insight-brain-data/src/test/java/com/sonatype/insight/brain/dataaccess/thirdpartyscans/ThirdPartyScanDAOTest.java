/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyScan;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyScanDAOTest
    extends AbstractDbDAOTest
{
  private final ThirdPartyScanDAO dao = new ThirdPartyScanDAO();

  @Test
  public void testCRUD() throws Exception {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    // Create
    String scanId = tempEntity.uuid();
    Date created = new Date();
    ThirdPartyScan entity = new ThirdPartyScan(thirdPartyFile.getId(), scanId, created);
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Get
    ThirdPartyScan retrievedScan = dao.getById(entity.getId());
    assertThirdPartyScan(entity.getId(), thirdPartyFile.getId(), scanId, created, retrievedScan);

    // Update
    String updatedScanId = tempEntity.uuid();
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
    ThirdPartyScan scan = tempEntity.newThirdPartyScan();
    ThirdPartyScan retrievedMapping =
        dao.getByThirdPartyFileIdAndScanId(scan.getThirdPartyFileId(), scan.getScanId());

    assertThirdPartyScan(scan.getId(), scan.getThirdPartyFileId(), scan.getScanId(), scan.getCreateTime(),
        retrievedMapping);
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

  private void assertThirdPartyScan(
      final String id,
      final String thirdPartyFileId,
      final String scanId,
      final Date created, final ThirdPartyScan actual)
  {
    assertThat(actual.getId()).isEqualTo(id);
    assertThat(actual.getThirdPartyFileId()).isEqualTo(thirdPartyFileId);
    assertThat(actual.getScanId()).isEqualTo(scanId);
    assertThat(actual.getCreateTime()).isEqualTo(created);
  }
}
