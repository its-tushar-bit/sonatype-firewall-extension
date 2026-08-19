/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service.legal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.api.v2.dto.ApiLicenseThreatDTOV2;
import com.sonatype.insight.brain.api.v2.dto.legal.ApiLicenseLegalStageScanDTO;
import com.sonatype.insight.brain.model.legal.ComponentCopyright;
import com.sonatype.insight.brain.model.legal.ComponentLegalFile;
import com.sonatype.insight.brain.model.legal.ComponentObligation;
import com.sonatype.insight.brain.model.legal.ComponentObligationAttribution;
import com.sonatype.insight.brain.model.legal.CopyrightOverride;
import com.sonatype.insight.brain.model.legal.LegalFileOverride;

public class ComponentIdentifierLegalData
{
  private ComponentIdentifier componentIdentifier;

  private List<CopyrightOverride> copyrightOverrides = new ArrayList<>();

  private ComponentCopyright componentCopyrights;

  private List<LegalFileOverride> licenseOverrides = new ArrayList<>();

  private ComponentLegalFile componentLicense;

  private List<LegalFileOverride> noticeOverrides = new ArrayList<>();

  private ComponentLegalFile componentNotice;

  private List<ComponentObligation> obligations = new ArrayList<>();

  private List<ComponentObligationAttribution> attributions = new ArrayList<>();

  private ApiLicenseThreatDTOV2 highestEffectiveLicenseThreatGroup;

  private List<ApiLicenseLegalStageScanDTO> stageScans;

  public ComponentIdentifierLegalData(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public ComponentIdentifier getComponentIdentifier() {
    return componentIdentifier;
  }

  public void setComponentIdentifier(final ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  public List<CopyrightOverride> getCopyrightOverrides() {
    if (copyrightOverrides == null) {
      return Collections.emptyList();
    }
    return copyrightOverrides;
  }

  public void setCopyrightOverrides(
      final List<CopyrightOverride> copyrightOverrides)
  {
    this.copyrightOverrides = copyrightOverrides;
  }

  public ComponentCopyright getComponentCopyrights() {
    return componentCopyrights;
  }

  public void setComponentCopyrights(final ComponentCopyright componentCopyrights) {
    this.componentCopyrights = componentCopyrights;
  }

  public List<LegalFileOverride> getLicenseOverrides() {
    if (licenseOverrides == null) {
      return Collections.emptyList();
    }
    return licenseOverrides;
  }

  public void setLicenseOverrides(final List<LegalFileOverride> licenseOverrides) {
    this.licenseOverrides = licenseOverrides;
  }

  public ComponentLegalFile getComponentLicense() {
    return componentLicense;
  }

  public void setComponentLicense(final ComponentLegalFile componentLicense) {
    this.componentLicense = componentLicense;
  }

  public List<LegalFileOverride> getNoticeOverrides() {
    if (noticeOverrides == null) {
      return Collections.emptyList();
    }
    return noticeOverrides;
  }

  public void setNoticeOverrides(final List<LegalFileOverride> noticeOverrides) {
    this.noticeOverrides = noticeOverrides;
  }

  public ComponentLegalFile getComponentNotice() {
    return componentNotice;
  }

  public void setComponentNotice(final ComponentLegalFile componentNotice) {
    this.componentNotice = componentNotice;
  }

  public List<ComponentObligation> getObligations() {
    if (obligations == null) {
      return Collections.emptyList();
    }
    return obligations;
  }

  public void setObligations(final List<ComponentObligation> obligations) {
    this.obligations = obligations;
  }

  public List<ComponentObligationAttribution> getAttributions() {
    if (attributions == null) {
      return Collections.emptyList();
    }
    return attributions;
  }

  public void setAttributions(
      final List<ComponentObligationAttribution> attributions)
  {
    this.attributions = attributions;
  }

  public ApiLicenseThreatDTOV2 getHighestEffectiveLicenseThreatGroup() {
    return highestEffectiveLicenseThreatGroup;
  }

  public void setHighestEffectiveLicenseThreatGroup(
      final ApiLicenseThreatDTOV2 highestEffectiveLicenseThreatGroup)
  {
    this.highestEffectiveLicenseThreatGroup = highestEffectiveLicenseThreatGroup;
  }

  public List<ApiLicenseLegalStageScanDTO> getStageScans() {
    if (stageScans == null) {
      return Collections.emptyList();
    }
    return stageScans;
  }

  public void setStageScans(final List<ApiLicenseLegalStageScanDTO> stageScans) {
    this.stageScans = stageScans;
  }
}
