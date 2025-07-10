/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

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

public class CpeMatchingHelperTest
    extends AbstractComponentTest
{
  @Inject
  private OrganizationDAO organizationDAO;

  @Inject
  private CpeMatchingHelper cpeMatchingHelper;

  @Test
  public void testIsFormatValidForCpeMatching_formatIsValid() {
    assertThat(cpeMatchingHelper.isFormatValidForCpeMatching("redhat", null)).isTrue();
  }

  @Test
  public void testIsFormatValidForCpeMatching_formatIsInvalid() {
    assertThat(cpeMatchingHelper.isFormatValidForCpeMatching(ComponentIdentifier.FORMAT_CONTAINER, null)).isFalse();
  }

  @Test
  public void testIsFormatValidForCpeMatching_formatIsContainerAndApplicationNotExists() {
    assertThat(cpeMatchingHelper.isFormatValidForCpeMatching(ComponentIdentifier.FORMAT_CONTAINER, "fake-app-id"))
        .isFalse();
  }

  @Test
  public void testIsFormatValidForCpeMatching_formatIsContainerAndNotFirewallForDocker() {
    Application application = tempEntity.newApplicationWithParent();
    assertThat(cpeMatchingHelper.isFormatValidForCpeMatching(ComponentIdentifier.FORMAT_CONTAINER, application.getId()))
        .isFalse();
  }

  @Test
  public void testIsFormatValidForCpeMatching_formatIsContainerAndIsFirewallForDocker() {
    Repository repository =
        tempEntity.newRepository(tempEntity.newRepositoryManager(), "docker-repo", RepositoryType.proxy, "docker");

    Organization organization = tempEntity.newOrganization();
    organization.setRelatedRepositoryId(repository.getId());
    organizationDAO.update(organization);

    Application application = tempEntity.newApplicationWithParent(organization);

    assertThat(cpeMatchingHelper.isFormatValidForCpeMatching(ComponentIdentifier.FORMAT_CONTAINER, application.getId()))
        .isTrue();
  }
}
