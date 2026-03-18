/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.List;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.repository.Repository;

@Named
@Singleton
public class FirewallForContainerImagesHelper
{
  private final OrganizationDAO organizationDAO;

  private final RepositoryDAO repositoryDAO;

  private final OwnerDAO ownerDAO;

  @Inject
  public FirewallForContainerImagesHelper(
      OrganizationDAO organizationDAO,
      RepositoryDAO repositoryDAO,
      OwnerDAO ownerDAO)
  {
    this.organizationDAO = organizationDAO;
    this.repositoryDAO = repositoryDAO;
    this.ownerDAO = ownerDAO;
  }

  public List<String> getApplicableOwnersForPolicies(String stageTypeId, Owner owner) {
    // Proxy stage means Firewall, but to be extra sure the code below checks if it's really about Firewall for Docker
    if (Stage.ID_PROXY.equals(stageTypeId) && owner.getType() == OwnerType.APPLICATION) {
      Organization organization = organizationDAO.getById(owner.getParentOwnerId());

      // Check if the parent organization has a related docker proxy repository. If it's then use the repo hierarchy
      if (organization.getRelatedRepositoryId() != null) {
        Repository relatedRepository = repositoryDAO.getById(organization.getRelatedRepositoryId());

        if (relatedRepository.getRepositoryType() == RepositoryType.proxy
            && "docker".equals(relatedRepository.getFormat()))
        {
          return ownerDAO.getOwnerIds(relatedRepository);
        }
      }
    }

    return ownerDAO.getOwnerIds(owner);
  }
}
