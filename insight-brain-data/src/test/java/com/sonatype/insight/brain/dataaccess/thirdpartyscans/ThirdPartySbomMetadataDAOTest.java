/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.Date;
import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartySbomMetadata;
import com.sonatype.insight.dataaccess.TransactionContext;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

public class ThirdPartySbomMetadataDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartySbomMetadataDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartySbomMetadataDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    // Read
    ThirdPartySbomMetadata fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Update
    entity.setSbomVersion("new version");
    entity.setSerialNumber("new serial number");
    dao.update(entity);

    fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThirdPartySbomMetadata(fetchedThirdPartySbomMetadata, entity);

    // Delete
    dao.delete(entity);
    fetchedThirdPartySbomMetadata = dao.getById(entity.getId());
    assertThat(fetchedThirdPartySbomMetadata).isNull();
  }

  @Test
  public void testGetAll() {
    // Create one entry
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    // Read all
    List<ThirdPartySbomMetadata> allSbomMetadata = dao.getAll();
    assertThat(allSbomMetadata.size()).isOne();

    // Create another entry
    ThirdPartySbomMetadata anotherEntity = createSbomMetadata(true);
    allSbomMetadata = dao.getAll();
    assertThat(allSbomMetadata)
        .hasSize(2)
        .extracting(
            ThirdPartySbomMetadata::getId, ThirdPartySbomMetadata::getThirdPartyFileId,
            ThirdPartySbomMetadata::getApplicationId, ThirdPartySbomMetadata::getFilename,
            ThirdPartySbomMetadata::getSerialNumber, ThirdPartySbomMetadata::getSbomVersion,
            ThirdPartySbomMetadata::getSpec, ThirdPartySbomMetadata::getSpecFormat,
            ThirdPartySbomMetadata::getSpecVersion, ThirdPartySbomMetadata::getStatus,
            ThirdPartySbomMetadata::getCreatedAt, ThirdPartySbomMetadata::getMetadataJson)
        .contains(
            tuple(entity.getId(), entity.getThirdPartyFileId(), entity.getApplicationId(), entity.getFilename(),
                entity.getSerialNumber(), entity.getSbomVersion(), entity.getSpec(),
                entity.getSpecFormat(), entity.getSpecVersion(), entity.getStatus(), entity.getCreatedAt(),
                entity.getMetadataJson()),
            tuple(anotherEntity.getId(), anotherEntity.getThirdPartyFileId(), anotherEntity.getApplicationId(),
                anotherEntity.getFilename(), anotherEntity.getSerialNumber(), anotherEntity.getSbomVersion(),
                anotherEntity.getSpec(), anotherEntity.getSpecFormat(), anotherEntity.getSpecVersion(),
                anotherEntity.getStatus(), anotherEntity.getCreatedAt(), anotherEntity.getMetadataJson()));
  }

  @Test
  public void testGetByThirdPartyFileId() {
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    final ThirdPartySbomMetadata sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
    assertThat(sbomMetadata).isNotNull();
    assertThirdPartySbomMetadata(entity, sbomMetadata);
  }

  @Test
  public void testDeleteByThirdPartyFileId() {
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    ThirdPartySbomMetadata sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
    assertThat(sbomMetadata).isNotNull();
    assertThirdPartySbomMetadata(entity, sbomMetadata);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByThirdPartyFileId(tx, entity.getThirdPartyFileId());
      tx.commit();

      sbomMetadata = dao.getByThirdPartyFileId(entity.getThirdPartyFileId());
      assertThat(sbomMetadata).isNull();
    }
  }

  @Test
  public void testGetByApplicationId() {
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    final List<ThirdPartySbomMetadata> sbomMetadata = dao.getByApplicationId(entity.getApplicationId());
    assertThat(sbomMetadata).isNotNull()
        .hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), entity);
  }

  @Test
  public void testDeleteByApplicationId() {
    ThirdPartySbomMetadata entity = createSbomMetadata(true);
    assertThat(entity.getId()).isNotNull();

    List<ThirdPartySbomMetadata> sbomMetadata = dao.getByApplicationId(entity.getApplicationId());
    assertThat(sbomMetadata).isNotNull()
        .hasSize(1);
    assertThirdPartySbomMetadata(sbomMetadata.get(0), entity);

    try (TransactionContext tx = dao.createTransactionContext()) {
      tx.begin();
      dao.deleteByApplicationId(tx, entity.getApplicationId());
      tx.commit();

      sbomMetadata = dao.getByApplicationId(entity.getThirdPartyFileId());
      assertThat(sbomMetadata).isEmpty();
    }
  }

  void assertThirdPartySbomMetadata(ThirdPartySbomMetadata actual, ThirdPartySbomMetadata expected) {
    assertThat(actual.getId()).isEqualTo(expected.getId());
    assertThat(actual.getCreatedAt()).isEqualTo(expected.getCreatedAt());
    assertThat(actual.getApplicationId()).isEqualTo(expected.getApplicationId());
    assertThat(actual.getSbomVersion()).isEqualTo(expected.getSbomVersion());
    assertThat(actual.getThirdPartyFileId()).isEqualTo(expected.getThirdPartyFileId());
    assertThat(actual.getFilename()).isEqualTo(expected.getFilename());
    assertThat(actual.getSerialNumber()).isEqualTo(expected.getSerialNumber());
    assertThat(actual.getSpec()).isEqualTo(expected.getSpec());
    assertThat(actual.getSpecFormat()).isEqualTo(expected.getSpecFormat());
    assertThat(actual.getSpecVersion()).isEqualTo(expected.getSpecVersion());
    assertThat(actual.getStatus()).isEqualTo(expected.getStatus());
    assertThat(actual.getMetadataJson()).isEqualTo(expected.getMetadataJson());
  }

  ThirdPartySbomMetadata createSbomMetadata(boolean save) {
    ThirdPartySbomMetadata thirdPartySbomMetadata = new ThirdPartySbomMetadata();
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();
    Application application = tempEntity.newApplicationWithParent();
    thirdPartySbomMetadata.setCreatedAt(new Date());
    thirdPartySbomMetadata.setApplicationId(application.getId());
    thirdPartySbomMetadata.setThirdPartyFileId(thirdPartyFile.getId());
    thirdPartySbomMetadata.setSbomVersion(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setFilename(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSerialNumber(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpec(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpecFormat(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setSpecVersion(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setStatus(RandomStringUtils.random(10));
    thirdPartySbomMetadata.setMetadataJson(RandomStringUtils.random(1500));

    if (save) {
      dao.insert(thirdPartySbomMetadata);
    }
    return thirdPartySbomMetadata;
  }
}
