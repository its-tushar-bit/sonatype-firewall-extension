/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.firewall.container;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;

@Named
@Singleton
public class FirewallContainerHelper
{
  private final RepositoryDAO repositoryDAO;

  @Inject
  public FirewallContainerHelper(RepositoryDAO repositoryDAO) {
    this.repositoryDAO = repositoryDAO;
  }

  public boolean isFormatValidForFirewallForContainerImages(String format, String applicationId) {
    return ComponentIdentifier.FORMAT_CONTAINER.equals(format)
        && isDockerForFirewallApplication(applicationId);
  }

  public boolean isDockerForFirewallApplication(String applicationId) {
    return repositoryDAO.getByContainerImageId(applicationId) != null;
  }
}
