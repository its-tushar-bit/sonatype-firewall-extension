/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.scanhealth;

import com.sonatype.insight.brain.dataaccess.configuration.ScanHealthConfigDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfig;
import com.sonatype.insight.brain.model.configuration.scanhealth.ScanHealthConfigDTO;
import com.sonatype.insight.brain.model.repository.HostedRepositoryComponent;
import com.sonatype.insight.brain.service.AbstractComponentTest;

import jakarta.inject.Inject;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ScanHealthServiceTest
    extends AbstractComponentTest
{
  @Inject
  private ScanHealthService scanHealthService;

  @Inject
  private ScanHealthConfigDAO scanHealthConfigDAO;

  @Test
  public void testGetEffectiveConfig_withAppOverride() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    scanHealthConfigDAO.save(new ScanHealthConfig(app.getId(), "application", "{\"failOnZeroComponents\":true}"));

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isTrue();
  }

  @Test
  public void testGetEffectiveConfig_withInheritFromOrg() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    scanHealthConfigDAO.save(new ScanHealthConfig(org.getId(), "organization", "{\"failOnZeroComponents\":true}"));

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isTrue();
  }

  @Test
  public void testGetEffectiveConfig_whenDefaultDisabled() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isNull();
  }

  @Test
  public void testShouldFailOnZeroComponents_enabled() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    scanHealthConfigDAO.save(new ScanHealthConfig(app.getId(), "application", "{\"failOnZeroComponents\":true}"));

    boolean shouldFail = scanHealthService.shouldFailOnZeroComponents(app);

    assertThat(shouldFail).isTrue();
  }

  @Test
  public void testShouldFailOnZeroComponents_disabled() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    scanHealthConfigDAO.save(new ScanHealthConfig(app.getId(), "application", "{\"failOnZeroComponents\":false}"));

    boolean shouldFail = scanHealthService.shouldFailOnZeroComponents(app);

    assertThat(shouldFail).isFalse();
  }

  @Test
  public void testGetEffectiveConfig_withEmptyJsonReturnsDefault() {
    Organization org = tempEntity.newOrganization();
    Application app = tempEntity.newApplication(org.getId());

    scanHealthConfigDAO.save(new ScanHealthConfig(app.getId(), "application", "{}"));

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isNull();
  }

  @Test
  public void testGetEffectiveConfig_inheritsFromGrandparentOrg() {
    Organization rootOrg = tempEntity.newOrganization();
    Organization childOrg = tempEntity.newOrganization(rootOrg);
    Application app = tempEntity.newApplication(childOrg.getId());

    scanHealthConfigDAO.save(
        new ScanHealthConfig(rootOrg.getId(), "organization", "{\"failOnZeroComponents\":true}"));

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isTrue();
  }

  @Test
  public void testGetEffectiveConfig_hrcInheritsFromAncestorOrg() {
    HostedRepositoryComponent hrc = tempEntity.newHostedRepositoryComponent(tempEntity.newRepository());

    scanHealthConfigDAO.save(new ScanHealthConfig(
        Organization.ROOT_ORGANIZATION_ID, "organization", "{\"failOnZeroComponents\":true}"));

    ScanHealthConfigDTO result =
        scanHealthService.getEffectiveConfig(hrc.getId(), OwnerType.HOSTED_REPOSITORY_COMPONENT);

    assertThat(result.failOnZeroComponents()).isTrue();
  }

  @Test
  public void testGetEffectiveConfig_childOrgConfigOverridesParentOrg() {
    Organization rootOrg = tempEntity.newOrganization();
    Organization childOrg = tempEntity.newOrganization(rootOrg);
    Application app = tempEntity.newApplication(childOrg.getId());

    scanHealthConfigDAO.save(
        new ScanHealthConfig(rootOrg.getId(), "organization", "{\"failOnZeroComponents\":true}"));
    scanHealthConfigDAO.save(
        new ScanHealthConfig(childOrg.getId(), "organization", "{\"failOnZeroComponents\":false}"));

    ScanHealthConfigDTO result = scanHealthService.getEffectiveConfig(app.getId(), app.getType());

    assertThat(result.failOnZeroComponents()).isFalse();
  }
}
