/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.container.images;

import com.sonatype.insight.brain.common.test.SlowTest;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.service.AbstractServiceAuthzTest;
import jakarta.inject.Inject;
import org.apache.shiro.authz.UnauthenticatedException;
import org.apache.shiro.authz.UnauthorizedException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SlowTest.class)
public class ContainerImageReportServiceAuthzTest
    extends AbstractServiceAuthzTest
{
  @Inject
  ContainerImageReportService containerImageReportService;

  private RepositoryDAO repositoryDAO;

  @Test(expected = UnauthenticatedException.class)
  public void testGetContainerImagesSummary_Unauthenticated() {
    containerImageReportService.getContainerImagesSummary("repositoryId");
  }

  @Test(expected = UnauthorizedException.class)
  public void testGetContainerImagesSummary_Unauthorized() {
    repositoryDAO = lookup(RepositoryDAO.class);
    login();
    Repository repository = tempEntity.newRepository();
    repository.setFormat("docker");
    containerImageReportService.getContainerImagesSummary(repository.getId());
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
