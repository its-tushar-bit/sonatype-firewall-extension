/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ApplicationComponentLicenseDAOTest
    extends AbstractDbDAOTest
{
  private ApplicationComponentLicenseDAO dao;

  @Before
  public void before() {
    dao = new ApplicationComponentLicenseDAO();
  }

  @Test
  public void testCRUD() throws Exception {
    // Create
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    ApplicationComponentLicense applicationComponentLicense =
        new ApplicationComponentLicense(applicationComponent.getId(), "license-test");
    dao.insert(applicationComponentLicense);

    // Read
    assertThat(dao.getById(applicationComponentLicense.getId()))
        .usingRecursiveComparison()
        .isEqualTo(applicationComponentLicense);

    // Update
    ApplicationComponentLicense toUpdate = dao.getById(applicationComponentLicense.getId());
    toUpdate.setEffectiveLicenseId("new-license");
    assertThatThrownBy(() -> {
      dao.update(toUpdate);
    }).isInstanceOf(UnsupportedOperationException.class);

    // Delete
    dao.delete(applicationComponentLicense);
    assertThat(dao.getById(applicationComponentLicense.getId())).isNull();
  }

  @Test
  public void testGetByApplicationComponentId() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    Application otherApplication = tempEntity.newApplicationWithParent();
    ApplicationComponent otherApplicationComponent = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

    ApplicationComponentLicense applicationComponentLicense1 =
        tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "license-1");
    ApplicationComponentLicense applicationComponentLicense2 =
        tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "license-2");

    ApplicationComponentLicense otherApplicationComponentLicense =
        tempEntity.newApplicationComponentLicense(otherApplicationComponent.getId(), "license-3");

    assertThat(dao.getByApplicationComponentId(applicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(applicationComponentLicense1, applicationComponentLicense2);

    assertThat(dao.getByApplicationComponentId(otherApplicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(otherApplicationComponentLicense);
  }
}
