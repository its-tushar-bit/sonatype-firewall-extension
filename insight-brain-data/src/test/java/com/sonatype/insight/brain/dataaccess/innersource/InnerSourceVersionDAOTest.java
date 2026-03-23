/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.innersource;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.JPA;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
import com.sonatype.insight.brain.model.policy.stages.StageTypes;
import com.sonatype.insight.purl.PackageUrlIdentifier;

import org.jooq.exception.IntegrityConstraintViolationException;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class InnerSourceVersionDAOTest
    extends AbstractDbDAOTest
{
  private InnerSourceVersionDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    dao = daoFactory.createInnerSourceVersionDAO();
  }

  @Test
  public void testCRUD() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source@1.0.0");
    String stageTypeId = StageTypes.RELEASE.getId();

    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);

    // Create
    InnerSourceVersion innerSourceVersion =
        tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", stageTypeId);
    assertThat(innerSourceVersion.getId()).isNotNull();

    // Get
    innerSourceVersion = dao.getById(innerSourceVersion.getId());
    JPA.assertEntityEquals(dao.getById(innerSourceVersion.getId()), innerSourceVersion);

    // Update
    innerSourceVersion.setLatestVersion("2.0.0");
    dao.update(innerSourceVersion);
    JPA.assertEntityEquals(dao.getById(innerSourceVersion.getId()), innerSourceVersion);

    // Delete
    dao.delete(innerSourceVersion);
    assertThat(dao.getById(innerSourceVersion.getId())).isNull();
  }

  @Test
  public void testUniqueConstraintOnInnerSourceApplicationAndStage() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source@1.0.0");
    String stageTypeId = StageTypes.RELEASE.getId();

    // Create the first component with version
    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    InnerSourceVersion innerSourceVersion =
        tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", stageTypeId);

    JPA.assertEntityEquals(dao.getById(innerSourceVersion.getId()), innerSourceVersion);

    // Attempt to create a second component with the same purl and stage and expect a unique constraint violation
    // jOOQ throws IntegrityConstraintViolationException directly (not wrapped)
    assertThatThrownBy(() -> tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", stageTypeId))
        .isInstanceOf(IntegrityConstraintViolationException.class)
        .hasMessageContaining("inner_source_version_uk");

    // A second component with a different stage should be created successfully
    tempEntity.newInnerSourceVersion(innerSourceApplication, "1.0.0", StageTypes.BUILD.getId());
  }

  @Test
  public void testGetByInnerSourceApplication() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source@1.0.0");

    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    InnerSourceVersion innerSourceVersion1 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), StageTypes.RELEASE.getId());
    InnerSourceVersion innerSourceVersion2 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), StageTypes.DEVELOP.getId());
    InnerSourceVersion innerSourceVersion3 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), null);

    List<InnerSourceVersion> innerSourceVersions = dao.getByInnerSourceApplicationId(innerSourceApplication.getId());
    assertThat(innerSourceVersions).isNotNull();
    assertThat(innerSourceVersions).hasSize(3);

    JPA.assertContainsEntitiesExactlyInAnyOrder(innerSourceVersions, innerSourceVersion1, innerSourceVersion2,
        innerSourceVersion3);
  }

  @Test
  public void testGetByInnerSourceApplicationAndStage() {
    PackageUrlIdentifier purl = new PackageUrlIdentifier("pkg:maven/inner/source@1.0.0");

    InnerSourceApplication innerSourceApplication =
        tempEntity.newInnerSourceApplication(purl.getPackageUrl(), application);
    InnerSourceVersion innerSourceVersion1 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), StageTypes.RELEASE.getId());
    InnerSourceVersion innerSourceVersion2 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), StageTypes.DEVELOP.getId());
    InnerSourceVersion innerSourceVersion3 =
        tempEntity.newInnerSourceVersion(innerSourceApplication, purl.getVersion(), null);

    JPA.assertEntityEquals(dao.getByInnerSourceApplicationIdAndStage(innerSourceVersion1.getInnerSourceApplicationId(),
        innerSourceVersion1.getStageTypeId()), innerSourceVersion1);
    JPA.assertEntityEquals(dao.getByInnerSourceApplicationIdAndStage(innerSourceVersion2.getInnerSourceApplicationId(),
        innerSourceVersion2.getStageTypeId()), innerSourceVersion2);
    JPA.assertEntityEquals(dao.getByInnerSourceApplicationIdAndStage(innerSourceVersion3.getInnerSourceApplicationId(),
        innerSourceVersion3.getStageTypeId()), innerSourceVersion3);
    InnerSourceVersion missing = dao.getByInnerSourceApplicationIdAndStage(
        innerSourceApplication.getId(), StageTypes.BUILD.getId());
    assertThat(missing).isNull();
  }
}
