/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
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

  @Test
  public void testGetApplicationComponentEffectiveLicenses() {
    StageType stageType = new BuildStageType();

    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");

    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-1");
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-2");

    ApplicationComponent applicationComponent3 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(applicationComponent3.getId(), "license-1");
    tempEntity.newLicenseOverride(applicationComponent3.getApplicationId(),
        applicationComponent3.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "MIT");

    ApplicationComponent applicationComponent4 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(applicationComponent4.getId(), "license-1");
    tempEntity.newLicenseOverride(application.getOrganizationId(), applicationComponent4.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    ApplicationComponent applicationComponent5 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash5", ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(applicationComponent5.getId(), "license-1");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, applicationComponent5.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // using other stage type
    ApplicationComponent applicationComponent6 = tempEntity.newApplicationComponent(application.getId(),
        DevelopStageType.ID, "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(applicationComponent6.getId(), "license-6");

    // using other application
    Application otherApplication = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent7 = tempEntity.newApplicationComponent(otherApplication.getId(),
        stageType.getId(), "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newApplicationComponentLicense(applicationComponent7.getId(), "license-7");

    assertThat(dao.getApplicationComponentEffectiveLicenses(application.getId(), Sets.newHashSet(stageType.getId())))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(
            newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
            newApplicationComponentLicensesDTO(applicationComponent2, "license-1", "license-2"),
            newApplicationComponentLicensesDTO(applicationComponent3, "MIT"),
            newApplicationComponentLicensesDTO(applicationComponent4, "Apache-2.0"),
            newApplicationComponentLicensesDTO(applicationComponent5, "Apache-2.0"));
  }

  @Test
  public void testGetApplicationComponentEffectiveLicensesMultiApplications() {
    StageType stageType = new BuildStageType();

    Application app1 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent1 =
        tempEntity.newApplicationComponent(app1.getId(), stageType.getId(), "hash1",
        ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");

    Application app2 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent2 =
        tempEntity.newApplicationComponent(app2.getId(), stageType.getId(), "hash2",
        ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-1");
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-2");

    Application app3 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent3 =
        tempEntity.newApplicationComponent(app3.getId(), stageType.getId(), "hash3",
        ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(applicationComponent3.getId(), "license-1");
    tempEntity.newLicenseOverride(applicationComponent3.getApplicationId(),
        applicationComponent3.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "MIT");

    Application app4 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent4 =
        tempEntity.newApplicationComponent(app4.getId(), stageType.getId(), "hash4",
        ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(applicationComponent4.getId(), "license-1");
    tempEntity.newLicenseOverride(application.getOrganizationId(), applicationComponent4.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    Application app5 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent5 =
        tempEntity.newApplicationComponent(app5.getId(), stageType.getId(), "hash5",
        ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(applicationComponent5.getId(), "license-1");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, applicationComponent5.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // using other stage type
    Application app6 = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent6 = tempEntity.newApplicationComponent(app6.getId(), DevelopStageType.ID,
        "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(applicationComponent6.getId(), "license-6");

    // using other application
    Application otherApplication = tempEntity.newApplication(organization.getId());
    ApplicationComponent applicationComponent7 = tempEntity.newApplicationComponent(otherApplication.getId(),
        stageType.getId(), "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newApplicationComponentLicense(applicationComponent7.getId(), "license-7");

    Set<String> applicationsIds =
        new HashSet<>(Arrays.asList(app1.getId(), app2.getId(), app3.getId(), app4.getId(), app5.getId(), app6.getId(),
            otherApplication.getId()));

    assertThat(dao.getApplicationComponentEffectiveLicenses(applicationsIds, Sets.newHashSet(stageType.getId())))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
            newApplicationComponentLicensesDTO(applicationComponent2, "license-1", "license-2"),
            newApplicationComponentLicensesDTO(applicationComponent3, "MIT"),
            newApplicationComponentLicensesDTO(applicationComponent4, "Apache-2.0"),
            newApplicationComponentLicensesDTO(applicationComponent5, "Apache-2.0"),
            newApplicationComponentLicensesDTO(applicationComponent7, "license-7"));
  }

  @Test
  public void testDeleteByApplicationComponentId() {
    ApplicationComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    ApplicationComponentLicense applicationComponentLicense1 =
        new ApplicationComponentLicense(applicationComponent.getId(), "effectiveLicenseId1");
    ApplicationComponentLicense applicationComponentLicense2 =
        new ApplicationComponentLicense(applicationComponent.getId(), "effectiveLicenseId2");
    dao.insert(applicationComponentLicense1);
    dao.insert(applicationComponentLicense2);
    ApplicationComponent applicationComponentOther = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash4", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    ApplicationComponentLicense applicationComponentLicense3 =
        new ApplicationComponentLicense(applicationComponentOther.getId(), "effectiveLicenseId3");
    dao.insert(applicationComponentLicense3);

    dao.deleteByApplicationComponentId(applicationComponent.getId());

    assertThat(dao.getByApplicationComponentId(applicationComponent.getId())).isEmpty();
    assertThat(dao.getByApplicationComponentId(applicationComponentOther.getId())).extracting(
        ApplicationComponentLicense::getId).containsExactly(applicationComponentLicense3.getId());
  }

  private ApplicationComponentLicensesDTO newApplicationComponentLicensesDTO(
      ApplicationComponent applicationComponent,
      String... licenses)
  {
    ComponentIdentifier componentIdentifier = applicationComponent.getComponentIdentifier();
    return new ApplicationComponentLicensesDTO(applicationComponent.getApplicationId(), applicationComponent.getHash(),
        componentIdentifier.getFormat(),
        JsonUtils.writeUnformatted(applicationComponent.getComponentIdentifier().getCoordinates()),
        StringUtils.join(licenses, '\n'));
  }
}
