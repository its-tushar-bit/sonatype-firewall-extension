/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Comparator;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceApplicationDAO;
import com.sonatype.insight.brain.dataaccess.innersource.InnerSourceVersionDAO;
import com.sonatype.insight.brain.model.innersource.InnerSourceApplication;
import com.sonatype.insight.brain.model.innersource.InnerSourceVersion;
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
  private final InnerSourceApplicationDAO innerSourceApplicationDAO;

  private final InnerSourceVersionDAO innerSourceVersionDAO;

  @Inject
  public InnerSourceService(
      InnerSourceApplicationDAO innerSourceApplicationDAO,
      InnerSourceVersionDAO innerSourceVersionDAO)
  {
    this.innerSourceApplicationDAO = innerSourceApplicationDAO;
    this.innerSourceVersionDAO = innerSourceVersionDAO;
  }

  public String getComponentLatestVersion(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
    InnerSourceApplication innerSourceApplication = innerSourceApplicationDAO.getByPackageUrl(packageUrl);
    if (innerSourceApplication == null) {
      throw new NotFoundException("InnerSource component not found for " + componentIdentifier);
    }

    return innerSourceVersionDAO.getByInnerSourceApplicationId(innerSourceApplication.getId())
        .stream()
        .max(Comparator.comparing(c -> InnerSourceUtils.createCompositeComparableVersion(c.getLatestVersion(),
            componentIdentifier.getFormat())))
        .map(InnerSourceVersion::getLatestVersion)
        .orElse(null);
  }

  public String getComponentLatestVersionByStage(ComponentIdentifier componentIdentifier, String stage) {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }
    if (stage == null || stage.isEmpty()) {
      throw new BadRequestException("stage is required");
    }

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
    InnerSourceApplication innerSourceApplication = innerSourceApplicationDAO.getByPackageUrl(packageUrl);
    if (innerSourceApplication == null) {
      throw new NotFoundException("InnerSource component not found for " + componentIdentifier);
    }

    InnerSourceVersion innerSourceVersion =
        innerSourceVersionDAO.getByInnerSourceApplicationIdAndStage(innerSourceApplication.getId(), stage);

    return innerSourceVersion != null ? innerSourceVersion.getLatestVersion() : null;
  }

  public boolean isInnerSourceComponent(ComponentIdentifier componentIdentifier) {
    if (componentIdentifier == null) {
      throw new BadRequestException("componentIdentifier is required");
    }

    PackageUrlIdentifier packageUrl = InnerSourceUtils.getVersionlessPackageUrl(componentIdentifier);
    return innerSourceApplicationDAO.getByPackageUrl(packageUrl) != null;
  }
}
