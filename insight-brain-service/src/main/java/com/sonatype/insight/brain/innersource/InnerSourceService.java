/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceComponentDAO;
import com.sonatype.insight.brain.model.innersource.InnerSourceComponent;
import com.sonatype.insight.brain.report.InnerSourceUtils;
import com.sonatype.insight.error.exception.BadRequestException;
import com.sonatype.insight.error.exception.NotFoundException;
import com.sonatype.insight.purl.PackageUrlIdentifier;

/**
 * @since 1.120
 */
@Named
public class InnerSourceService
{
  private final InnerSourceComponentDAO innerSourceComponentDAO;

  @Inject
  public InnerSourceService(InnerSourceComponentDAO innerSourceComponentDAO) {
    this.innerSourceComponentDAO = innerSourceComponentDAO;
  }

  public String getComponentLatestVersion(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
    InnerSourceComponent innerSourceComponent = innerSourceComponentDAO.getByPackageUrl(packageUrl);
    if (innerSourceComponent == null) {
      throw new NotFoundException("InnerSource component not found for " + componentIdentifier);
    }

    return innerSourceComponent.getLatestVersion();
  }
}
