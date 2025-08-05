/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.container;

import static org.assertj.core.api.Assertions.assertThat;

import javax.inject.Inject;

import org.junit.Test;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractComponentTest;

public class FirewallContainerHelperTest
    extends AbstractComponentTest
{
  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private FirewallContainerHelper firewallContainerHelper;

  @Test
  public void testIsFormatValidForFirewallForContainerImages_formatIsInvalid() {
    assertThat(
        firewallContainerHelper.isFormatValidForFirewallForContainerImages(ComponentIdentifier.FORMAT_CONTAINER, null))
            .isFalse();
  }

  @Test
  public void testIsFormatValidForFirewallForContainerImages_formatIsContainerAndApplicationNotExists() {
    assertThat(firewallContainerHelper.isFormatValidForFirewallForContainerImages(ComponentIdentifier.FORMAT_CONTAINER,
        "fake-app-id")).isFalse();
  }

  @Test
  public void testIsFormatValidForFirewallForContainerImages_formatIsContainerAndNotFirewallForDocker() {
    Application application = tempEntity.newApplicationWithParent();
    assertThat(firewallContainerHelper.isFormatValidForFirewallForContainerImages(ComponentIdentifier.FORMAT_CONTAINER,
        application.getId())).isFalse();
  }

  @Test
  public void testIsFormatValidForFirewallForContainerImages_formatIsContainerAndIsFirewallForDocker() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);

    assertThat(firewallContainerHelper.isFormatValidForFirewallForContainerImages(ComponentIdentifier.FORMAT_CONTAINER,
        application.getId())).isTrue();
  }

  @Test
  public void testIsDockerForFirewallApplication() {
    Repository repository =
            tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");
    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);
    Application application = tempEntity.newApplicationWithParent(organization);

    assertThat(firewallContainerHelper.isDockerForFirewallApplication(application.getId())).isTrue();

    Organization organization1 = tempEntity.newOrganization();
    Application application1 = tempEntity.newApplication(organization1.getId());

    assertThat(firewallContainerHelper.isDockerForFirewallApplication(application1.getId())).isFalse();
  }
}
