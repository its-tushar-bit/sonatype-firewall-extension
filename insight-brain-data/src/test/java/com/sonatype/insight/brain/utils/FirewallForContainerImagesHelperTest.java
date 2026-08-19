/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.utils;

import java.util.List;

import com.sonatype.clm.dto.model.policy.Stage;
import com.sonatype.clm.dto.model.repository.RepositoryType;
import com.sonatype.insight.brain.dataaccess.AbstractDbDAOTest;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.OwnerDAO;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class FirewallForContainerImagesHelperTest
    extends AbstractDbDAOTest
{
  private OrganizationDAO organizationDAO;

  private OwnerDAO ownerDAO;

  private FirewallForContainerImagesHelper helper;

  @BeforeEach
  @Override
  public void setup() {
    super.setup();
    organizationDAO = daoFactory.createOrganizationDAO();
    RepositoryDAO repositoryDAO = daoFactory.createRepositoryDAO();
    ownerDAO = daoFactory.createOwnerDAO();
    helper = new FirewallForContainerImagesHelper(organizationDAO, repositoryDAO, ownerDAO);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_nonProxyStage() {
    Application app = tempEntity.newApplicationWithParent(organization);

    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_BUILD, app);

    List<String> expectedOwnerIds = ownerDAO.getOwnerIds(app);
    assertThat(result).isEqualTo(expectedOwnerIds);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_proxyStageButNotApplicationOwner() {
    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_PROXY, organization);

    List<String> expectedOwnerIds = ownerDAO.getOwnerIds(organization);
    assertThat(result).isEqualTo(expectedOwnerIds);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_proxyStageApplicationOwnerNoRelatedRepository() {
    Application app = tempEntity.newApplicationWithParent(organization);

    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_PROXY, app);

    List<String> expectedOwnerIds = ownerDAO.getOwnerIds(app);
    assertThat(result).isEqualTo(expectedOwnerIds);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_proxyStageApplicationOwnerWithNonProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository hostedRepo =
        tempEntity.newRepository(repoManager, "hosted-docker-repo", RepositoryType.hosted, "docker");

    Organization orgWithRepo = tempEntity.newOrganization("org-with-hosted-repo");
    orgWithRepo.setRelatedRepositoryId(hostedRepo.getId());
    organizationDAO.update(orgWithRepo);

    Application app = tempEntity.newApplicationWithParent(orgWithRepo);

    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_PROXY, app);

    List<String> expectedOwnerIds = ownerDAO.getOwnerIds(app);
    assertThat(result).isEqualTo(expectedOwnerIds);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_proxyStageApplicationOwnerWithProxyButNonDockerRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository proxyMavenRepo =
        tempEntity.newRepository(repoManager, "proxy-maven-repo", RepositoryType.proxy, "maven2");

    Organization orgWithRepo = tempEntity.newOrganization("org-with-proxy-maven-repo");
    orgWithRepo.setRelatedRepositoryId(proxyMavenRepo.getId());
    organizationDAO.update(orgWithRepo);

    Application app = tempEntity.newApplicationWithParent(orgWithRepo);

    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_PROXY, app);

    List<String> expectedOwnerIds = ownerDAO.getOwnerIds(app);
    assertThat(result).isEqualTo(expectedOwnerIds);
  }

  @Test
  public void testGetApplicableOwnersForPolicies_proxyStageApplicationOwnerWithDockerProxyRepository() {
    RepositoryManager repoManager = tempEntity.newRepositoryManager();
    Repository dockerProxyRepo =
        tempEntity.newRepository(repoManager, "docker-proxy-repo", RepositoryType.proxy, "docker");

    Organization orgWithDockerRepo = tempEntity.newOrganization("org-with-docker-proxy-repo");
    orgWithDockerRepo.setRelatedRepositoryId(dockerProxyRepo.getId());
    organizationDAO.update(orgWithDockerRepo);

    Application app = tempEntity.newApplicationWithParent(orgWithDockerRepo);

    List<String> result = helper.getApplicableOwnersForPolicies(Stage.ID_PROXY, app);

    List<String> expectedRepositoryOwnerIds = ownerDAO.getOwnerIds(dockerProxyRepo);
    assertThat(result).isEqualTo(expectedRepositoryOwnerIds);
  }
}
