/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.innersource;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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

  /**
   * Batch variant of {@link #getComponentLatestVersionByStage(ComponentIdentifier, String)} —
   * returns a map of versionless package URL to latest version at the given stage. Components that
   * have no registered inner source application or no version row at the stage are simply absent
   * from the map (no exception, unlike the single-component variant).
   */
  public Map<String, String> getLatestVersionsByStage(
      Collection<ComponentIdentifier> componentIdentifiers,
      String stage)
  {
    if (stage == null || stage.isEmpty()) {
      throw new BadRequestException("stage is required");
    }
    if (componentIdentifiers == null || componentIdentifiers.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<PackageUrlIdentifier> versionlessPurls = componentIdentifiers.stream()
        .filter(Objects::nonNull)
        .map(InnerSourceUtils::getVersionlessPackageUrl)
        .collect(Collectors.toSet());
    if (versionlessPurls.isEmpty()) {
      return Collections.emptyMap();
    }

    List<InnerSourceApplication> apps = innerSourceApplicationDAO.getByPackageUrls(versionlessPurls);
    if (apps.isEmpty()) {
      return Collections.emptyMap();
    }

    Set<String> appIds = apps.stream()
        .map(InnerSourceApplication::getId)
        .collect(Collectors.toSet());
    Map<String, InnerSourceVersion> versionByAppId =
        innerSourceVersionDAO.getByInnerSourceApplicationIdsAndStage(appIds, stage);
    if (versionByAppId.isEmpty()) {
      return Collections.emptyMap();
    }

    // package_url is UNIQUE on inner_source_application (schema.sql), so duplicate keys are
    // impossible — the bare Collectors.toMap will fail loud if that ever changes.
    return apps.stream()
        .map(app -> {
          InnerSourceVersion v = versionByAppId.get(app.getId());
          return v == null ? null : Map.entry(app.getPackageUrl(), v.getLatestVersion());
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }
}
