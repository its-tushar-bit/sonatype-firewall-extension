/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.cpematching;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.repository.RepositoryDAO;

@Named
@Singleton
public class CpeMatchingHelper
{
  private final RepositoryDAO repositoryDAO;

  @Inject
  public CpeMatchingHelper(RepositoryDAO repositoryDAO) {
    this.repositoryDAO = repositoryDAO;
  }

  public boolean isFormatValidForCpeMatching(String format, String applicationId) {
    if (ComponentIdentifier.isFormatValidForCpeMatching(format)) {
      return true;
    }
    return repositoryDAO.getByContainerImageId(applicationId) != null;
  }
}
