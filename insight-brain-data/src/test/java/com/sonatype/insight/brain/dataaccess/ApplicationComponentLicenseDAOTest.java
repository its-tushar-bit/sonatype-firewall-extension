/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.ApplicationComponent;
import com.sonatype.insight.brain.model.ApplicationComponentLicense;
import com.sonatype.insight.brain.model.ApplicationComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
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

@Category(SlowTest.class)
public class ApplicationComponentLicenseDAOTest
    extends AbstractDbDAOTest
{
  private LicenseOverrideDAO licenseOverrideDAO;

  private ApplicationComponentLicenseDAO dao;

  @Before
  @Override
  public void setup() {
    super.setup();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    dao = daoFactory.createApplicationComponentLicenseDAO();
  }

  @Test
  public void testCRUD() {
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
    assertThatThrownBy(() -> dao.update(toUpdate)).isInstanceOf(UnsupportedOperationException.class);

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
  public void testGetApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization() {
    StageType stageType = new BuildStageType();

    ApplicationComponent componentWithoutOverrides = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newApplicationComponentLicense(componentWithoutOverrides.getId(), "license-1");

    ApplicationComponent componentWithOverrideAtApplication = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtApplication.getId(), "license-2.1");
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtApplication.getId(), "license-2.2");
    tempEntity.newLicenseOverride(application.getId(), componentWithOverrideAtApplication.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    ApplicationComponent componentWithOverrideAtRootOrg = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtRootOrg.getId(), "license-3");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID,
        componentWithOverrideAtRootOrg.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    tempEntity.newLicenseOverride(application.getId(), componentWithOverrideAtRootOrg.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    Application otherApplication = tempEntity.newApplicationWithParent(organization);
    ApplicationComponent componentFromOtherApplication = tempEntity.newApplicationComponent(otherApplication.getId(),
        stageType.getId(), "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(componentFromOtherApplication.getId(), "license-4");

    ApplicationComponent componentFromOtherApplicationWithOverrides =
        tempEntity.newApplicationComponent(otherApplication.getId(), stageType.getId(), "hash5",
            ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(componentFromOtherApplicationWithOverrides.getId(), "license-5");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID,
        componentFromOtherApplicationWithOverrides.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN,
        Sets.newHashSet("MIT", "0BSD"));

    Application applicationNotQueried = tempEntity.newApplicationWithParent(organization);
    ApplicationComponent componentFromNotQueriedApplication =
        tempEntity.newApplicationComponent(applicationNotQueried.getId(), stageType.getId(), "hash6",
            ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(componentFromNotQueriedApplication.getId(), "license-6");

    assertThat(dao.getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
        Sets.newHashSet(application.getId(), otherApplication.getId()), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(newApplicationComponentLicensesDTO(componentWithoutOverrides, "license-1"),
                newApplicationComponentLicensesDTO(componentWithOverrideAtApplication, "license-2.1", "license-2.2"),
                newApplicationComponentLicensesDTO(componentWithOverrideAtRootOrg, "Apache-2.0"),
                newApplicationComponentLicensesDTO(componentFromOtherApplication, "license-4"),
                newApplicationComponentLicensesDTO(componentFromOtherApplicationWithOverrides, "MIT", "0BSD"));
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

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(application.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
                newApplicationComponentLicensesDTO(applicationComponent2, "license-1", "license-2"),
                newApplicationComponentLicensesDTO(applicationComponent3, "MIT"),
                newApplicationComponentLicensesDTO(applicationComponent4, "Apache-2.0"),
                newApplicationComponentLicensesDTO(applicationComponent5, "Apache-2.0"));
  }

  @Test
  public void testGetApplicationComponentEffectiveLicenses_LicenseOverridesNLevelOrganizations() {
    StageType stageType = new BuildStageType();
    Organization subOrganization = tempEntity.newOrganization(organization);
    Application testApplication = tempEntity.newApplicationWithParent(subOrganization);

    ApplicationComponent applicationComponent1 = tempEntity.newApplicationComponent(testApplication.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");

    ApplicationComponent applicationComponent2 = tempEntity.newApplicationComponent(testApplication.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-2");

    tempEntity.newLicenseOverride(organization.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
                newApplicationComponentLicensesDTO(applicationComponent2, "MIT"));

    tempEntity.newLicenseOverride(subOrganization.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
                newApplicationComponentLicensesDTO(applicationComponent2, "Apache-2.0"));

    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(testApplication.getId(),
        applicationComponent2.getComponentIdentifier(), LicenseOverrideStatus.OPEN, Collections.emptySet());

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
                newApplicationComponentLicensesDTO(applicationComponent2, "license-2"));

    licenseOverrideDAO.delete(licenseOverride);

    tempEntity.newLicenseOverride(testApplication.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("CC0-1.0", "PUBLIC-DOMAIN"));

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newApplicationComponentLicensesDTO(applicationComponent1, "license-1"),
                newApplicationComponentLicensesDTO(applicationComponent2, "CC0-1.0", "PUBLIC-DOMAIN"));
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
