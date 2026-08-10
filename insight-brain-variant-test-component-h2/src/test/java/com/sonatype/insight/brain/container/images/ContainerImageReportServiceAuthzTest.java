/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.variant.AbstractComponentH2AuthzTest;
import com.sonatype.insight.brain.variant.ComponentH2Test;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

@ComponentH2Test
public class ContainerImageReportServiceAuthzTest
    extends AbstractComponentH2AuthzTest
{
  @Inject
  ContainerImageReportService containerImageReportService;

  private RepositoryDAO repositoryDAO;

  @Test
  public void testGetContainerImagesSummary_Unauthenticated() {
    assertThrows(UnauthenticatedException.class,
        () -> containerImageReportService.getContainerImagesSummary("repositoryId"));
  }

  @Test
  public void testGetContainerImagesSummary_Unauthorized() {
    repositoryDAO = lookup(RepositoryDAO.class);
    login();
    Repository repository = tempEntity.newRepository();
    repository.setFormat("docker");
    assertThrows(UnauthorizedException.class,
        () -> containerImageReportService.getContainerImagesSummary(repository.getId()));
  }

  @Test
  public void testGetContainerImagesSummary_Authorized() {
    repositoryDAO = lookup(RepositoryDAO.class);
    login();
    Repository repository = tempEntity.newRepository();
    repository.setFormat("docker");
    repositoryDAO.update(repository);
    grantReadPermission(repository.getId());
    containerImageReportService.getContainerImagesSummary(repository.getId());
  }
}
