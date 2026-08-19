/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess;

import java.util.Collections;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.license.LicenseOverrideDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.OwnerComponent;
import com.sonatype.insight.brain.model.OwnerComponentLicense;
import com.sonatype.insight.brain.model.OwnerComponentLicensesDTO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.license.LicenseOverride;
import com.sonatype.insight.brain.model.license.LicenseOverrideStatus;
import com.sonatype.insight.brain.model.policy.StageType;
import com.sonatype.insight.brain.model.policy.stages.BuildStageType;
import com.sonatype.insight.brain.model.policy.stages.DevelopStageType;
import com.sonatype.insight.json.store.JsonUtils;

import com.google.common.collect.Sets;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OwnerComponentLicenseDAOTest
    extends AbstractDbDAOTest
{
  private LicenseOverrideDAO licenseOverrideDAO;

  private OwnerComponentLicenseDAO dao;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    licenseOverrideDAO = daoFactory.createLicenseOverrideDAO();
    dao = daoFactory.createOwnerComponentLicenseDAO();
  }

  @Test
  public void testCRUD() {
    // Create
    OwnerComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    OwnerComponentLicense applicationComponentLicense =
        new OwnerComponentLicense(applicationComponent.getId(), "license-test");
    dao.insert(applicationComponentLicense);

    // Read
    assertThat(dao.getById(applicationComponentLicense.getId()))
        .usingRecursiveComparison()
        .isEqualTo(applicationComponentLicense);

    // Update
    OwnerComponentLicense toUpdate = dao.getById(applicationComponentLicense.getId());
    toUpdate.setEffectiveLicenseId("new-license");
    assertThatThrownBy(() -> dao.update(toUpdate)).isInstanceOf(UnsupportedOperationException.class);

    // Delete
    dao.delete(applicationComponentLicense);
    assertThat(dao.getById(applicationComponentLicense.getId())).isNull();
  }

  @Test
  public void testGetByApplicationComponentId() {
    OwnerComponent applicationComponent = tempEntity.newApplicationComponent(application.getId(),
        BuildStageType.ID, "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));

    Application otherApplication = tempEntity.newApplicationWithParent();
    OwnerComponent otherApplicationComponent = tempEntity.newApplicationComponent(otherApplication.getId(),
        BuildStageType.ID, "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));

    OwnerComponentLicense applicationComponentLicense1 =
        tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "license-1");
    OwnerComponentLicense applicationComponentLicense2 =
        tempEntity.newApplicationComponentLicense(applicationComponent.getId(), "license-2");

    OwnerComponentLicense otherOwnerComponentLicense =
        tempEntity.newApplicationComponentLicense(otherApplicationComponent.getId(), "license-3");

    assertThat(dao.getByOwnerComponentId(applicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactlyInAnyOrder(applicationComponentLicense1, applicationComponentLicense2);

    assertThat(dao.getByOwnerComponentId(otherApplicationComponent.getId()))
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(otherOwnerComponentLicense);
  }

  @Test
  public void testGetApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization() {
    StageType stageType = new BuildStageType();

    OwnerComponent componentWithoutOverrides = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newApplicationComponentLicense(componentWithoutOverrides.getId(), "license-1");

    OwnerComponent componentWithOverrideAtApplication = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtApplication.getId(), "license-2.1");
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtApplication.getId(), "license-2.2");
    tempEntity.newLicenseOverride(application.getId(), componentWithOverrideAtApplication.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    OwnerComponent componentWithOverrideAtRootOrg = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(componentWithOverrideAtRootOrg.getId(), "license-3");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID,
        componentWithOverrideAtRootOrg.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");
    tempEntity.newLicenseOverride(application.getId(), componentWithOverrideAtRootOrg.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    Application otherApplication = tempEntity.newApplicationWithParent(organization);
    OwnerComponent componentFromOtherApplication = tempEntity.newApplicationComponent(otherApplication.getId(),
        stageType.getId(), "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(componentFromOtherApplication.getId(), "license-4");

    OwnerComponent componentFromOtherApplicationWithOverrides =
        tempEntity.newApplicationComponent(otherApplication.getId(), stageType.getId(), "hash5",
            ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(componentFromOtherApplicationWithOverrides.getId(), "license-5");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID,
        componentFromOtherApplicationWithOverrides.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN,
        Sets.newHashSet("MIT", "0BSD"));

    Application applicationNotQueried = tempEntity.newApplicationWithParent(organization);
    OwnerComponent componentFromNotQueriedApplication =
        tempEntity.newApplicationComponent(applicationNotQueried.getId(), stageType.getId(), "hash6",
            ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(componentFromNotQueriedApplication.getId(), "license-6");

    assertThat(dao.getApplicationComponentEffectiveLicensesWithOverridesAtRootOrganization(
        Sets.newHashSet(application.getId(), otherApplication.getId()), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(newOwnerComponentLicensesDTO(componentWithoutOverrides, "license-1"),
                newOwnerComponentLicensesDTO(componentWithOverrideAtApplication, "license-2.1", "license-2.2"),
                newOwnerComponentLicensesDTO(componentWithOverrideAtRootOrg, "Apache-2.0"),
                newOwnerComponentLicensesDTO(componentFromOtherApplication, "license-4"),
                // Licenses are ordered alphabetically by the query's listAgg.withinGroupOrderBy()
                newOwnerComponentLicensesDTO(componentFromOtherApplicationWithOverrides, "0BSD", "MIT"));
  }

  @Test
  public void testGetApplicationComponentEffectiveLicenses() {
    StageType stageType = new BuildStageType();

    OwnerComponent applicationComponent1 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g", "a", "v"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");

    OwnerComponent applicationComponent2 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-1");
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-2");

    OwnerComponent applicationComponent3 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash3", ComponentIdentifier.createMavenCoordinates("g3", "a3", "v3"));
    tempEntity.newApplicationComponentLicense(applicationComponent3.getId(), "license-1");
    tempEntity.newLicenseOverride(applicationComponent3.getOwnerId(),
        applicationComponent3.getComponentIdentifier(), LicenseOverrideStatus.OVERRIDDEN, "MIT");

    OwnerComponent applicationComponent4 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash4", ComponentIdentifier.createMavenCoordinates("g4", "a4", "v4"));
    tempEntity.newApplicationComponentLicense(applicationComponent4.getId(), "license-1");
    tempEntity.newLicenseOverride(application.getOrganizationId(), applicationComponent4.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    OwnerComponent applicationComponent5 = tempEntity.newApplicationComponent(application.getId(),
        stageType.getId(), "hash5", ComponentIdentifier.createMavenCoordinates("g5", "a5", "v5"));
    tempEntity.newApplicationComponentLicense(applicationComponent5.getId(), "license-1");
    tempEntity.newLicenseOverride(Organization.ROOT_ORGANIZATION_ID, applicationComponent5.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    // using other stage type
    OwnerComponent applicationComponent6 = tempEntity.newApplicationComponent(application.getId(),
        DevelopStageType.ID, "hash6", ComponentIdentifier.createMavenCoordinates("g6", "a6", "v6"));
    tempEntity.newApplicationComponentLicense(applicationComponent6.getId(), "license-6");

    // using other application
    Application otherApplication = tempEntity.newApplication(organization.getId());
    OwnerComponent applicationComponent7 = tempEntity.newApplicationComponent(otherApplication.getId(),
        stageType.getId(), "hash7", ComponentIdentifier.createMavenCoordinates("g7", "a7", "v7"));
    tempEntity.newApplicationComponentLicense(applicationComponent7.getId(), "license-7");

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(application.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(newOwnerComponentLicensesDTO(applicationComponent1, "license-1"),
                newOwnerComponentLicensesDTO(applicationComponent2, "license-1", "license-2"),
                newOwnerComponentLicensesDTO(applicationComponent3, "MIT"),
                newOwnerComponentLicensesDTO(applicationComponent4, "Apache-2.0"),
                newOwnerComponentLicensesDTO(applicationComponent5, "Apache-2.0"));
  }

  @Test
  public void testGetApplicationComponentEffectiveLicenses_LicenseOverridesNLevelOrganizations() {
    StageType stageType = new BuildStageType();
    Organization subOrganization = tempEntity.newOrganization(organization);
    Application testApplication = tempEntity.newApplicationWithParent(subOrganization);

    OwnerComponent applicationComponent1 = tempEntity.newApplicationComponent(testApplication.getId(),
        stageType.getId(), "hash1", ComponentIdentifier.createMavenCoordinates("g1", "a1", "v1"));
    tempEntity.newApplicationComponentLicense(applicationComponent1.getId(), "license-1");

    OwnerComponent applicationComponent2 = tempEntity.newApplicationComponent(testApplication.getId(),
        stageType.getId(), "hash2", ComponentIdentifier.createMavenCoordinates("g2", "a2", "v2"));
    tempEntity.newApplicationComponentLicense(applicationComponent2.getId(), "license-2");

    tempEntity.newLicenseOverride(organization.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "MIT");

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newOwnerComponentLicensesDTO(applicationComponent1, "license-1"),
                newOwnerComponentLicensesDTO(applicationComponent2, "MIT"));

    tempEntity.newLicenseOverride(subOrganization.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, "Apache-2.0");

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newOwnerComponentLicensesDTO(applicationComponent1, "license-1"),
                newOwnerComponentLicensesDTO(applicationComponent2, "Apache-2.0"));

    LicenseOverride licenseOverride = tempEntity.newLicenseOverride(testApplication.getId(),
        applicationComponent2.getComponentIdentifier(), LicenseOverrideStatus.OPEN, Collections.emptySet());

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newOwnerComponentLicensesDTO(applicationComponent1, "license-1"),
                newOwnerComponentLicensesDTO(applicationComponent2, "license-2"));

    licenseOverrideDAO.delete(licenseOverride);

    tempEntity.newLicenseOverride(testApplication.getId(), applicationComponent2.getComponentIdentifier(),
        LicenseOverrideStatus.OVERRIDDEN, Sets.newHashSet("CC0-1.0", "PUBLIC-DOMAIN"));

    assertThat(
        dao.getApplicationComponentEffectiveLicenses(testApplication.getId(), Collections.singleton(stageType.getId())))
            .usingRecursiveFieldByFieldElementComparator()
            .containsExactlyInAnyOrder(
                newOwnerComponentLicensesDTO(applicationComponent1, "license-1"),
                newOwnerComponentLicensesDTO(applicationComponent2, "CC0-1.0", "PUBLIC-DOMAIN"));
  }

  private OwnerComponentLicensesDTO newOwnerComponentLicensesDTO(
      OwnerComponent ownerComponent,
      String... licenses)
  {
    ComponentIdentifier componentIdentifier = ownerComponent.getComponentIdentifier();
    return new OwnerComponentLicensesDTO(ownerComponent.getOwnerId(), ownerComponent.getHash(),
        componentIdentifier.getFormat(),
        JsonUtils.writeUnformatted(ownerComponent.getComponentIdentifier().getCoordinates()),
        StringUtils.join(licenses, '\n'));
  }
}
