/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.thirdpartyscans;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyFile;
import com.sonatype.insight.brain.model.thirdpartyscans.ThirdPartyUnknownComponent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ThirdPartyThirdPartyUnknownComponentDAOTest
    extends AbstractDbDAOTest
{
  private ThirdPartyUnknownComponentDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createThirdPartyUnknownComponentDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    ThirdPartyUnknownComponent entity = new ThirdPartyUnknownComponent();
    entity.setFilename("test-filename.xml");
    entity.setHash(TemporaryEntity.uuid().substring(0, 20));
    entity.setThirdPartyFileId(thirdPartyFile.getId());
    dao.insert(entity);
    assertThat(entity.getId()).isNotNull();

    // Read
    ThirdPartyUnknownComponent componentFromDB = dao.getById(entity.getId());
    assertThat(componentFromDB).isNotNull();
    assertThat(componentFromDB.getFilename()).isEqualTo(entity.getFilename());
    assertThat(componentFromDB.getHash()).isEqualTo(entity.getHash());
    assertThat(componentFromDB.getThirdPartyFileId()).isEqualTo(entity.getThirdPartyFileId());

    // Update
    entity.setFilename("new-filename.xml");
    dao.update(entity);

    componentFromDB = dao.getById(entity.getId());
    assertThat(componentFromDB.getFilename()).isEqualTo("new-filename.xml");

    // Delete
    dao.delete(entity);
    componentFromDB = dao.getById(entity.getId());
    assertThat(componentFromDB).isNull();
  }

  @Test
  public void testGetByThirdPartyFileId() {
    ThirdPartyFile thirdPartyFile = tempEntity.newThirdPartyFile();

    ThirdPartyUnknownComponent component1 =
        tempEntity.newThirdPartyUnknownComponent("test-filename.xml", thirdPartyFile);
    ThirdPartyUnknownComponent component2 =
        tempEntity.newThirdPartyUnknownComponent("other-file.json", thirdPartyFile);

    List<ThirdPartyUnknownComponent> componentsFromDB = dao.getByThirdPartyFileId(thirdPartyFile.getId());
    assertThat(componentsFromDB).hasSize(2);
    assertThat(componentsFromDB).extracting(ThirdPartyUnknownComponent::getId)
        .containsExactlyInAnyOrder(component1.getId(), component2.getId());
    assertThat(componentsFromDB).extracting(ThirdPartyUnknownComponent::getHash)
        .containsExactlyInAnyOrder(component1.getHash(), component2.getHash());
    assertThat(componentsFromDB).extracting(ThirdPartyUnknownComponent::getFilename)
        .containsExactlyInAnyOrder(component1.getFilename(), component2.getFilename());
    assertThat(componentsFromDB).extracting(ThirdPartyUnknownComponent::getThirdPartyFileId)
        .containsExactlyInAnyOrder(component1.getThirdPartyFileId(), component2.getThirdPartyFileId());
  }
}
