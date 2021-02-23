/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.hds;

import com.sonatype.clm.dto.model.component.ComponentDetails;
import com.sonatype.insight.brain.dataaccess.component.ComponentDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseDAO;
import com.sonatype.insight.brain.model.Owner;
import com.sonatype.insight.brain.model.OwnerType;
import com.sonatype.insight.brain.model.component.Component;
import com.sonatype.insight.brain.repository.ProprietaryComponentNameDetector;

/**
 * Assists in loading data for the CIP.
 */
public class DefaultComponentDetailsLoader extends ComponentDetailsLoader
{
  private final LicenseDAO licenseDAO = new LicenseDAO();

  private final ComponentDAO componentDAO;

  private final ProprietaryComponentNameDetector proprietaryComponentNameDetector;

  DefaultComponentDetailsLoader(Owner owner, ProprietaryComponentNameDetector proprietaryComponentNameDetector) {
    componentDAO = new ComponentDAO(owner);
    this.proprietaryComponentNameDetector =
        OwnerType.REPOSITORY.equals(owner.getType()) ? proprietaryComponentNameDetector : null;
  }

  @Override
  protected Component getComponent(ComponentDetails componentDetails) {
    Component component = componentDAO.getComponent(componentDetails);
    if (proprietaryComponentNameDetector != null) {
      String conflictingProprietaryName =
          proprietaryComponentNameDetector.findProprietaryComponentName(component.getComponentIdentifier());
      component.setConflictingProprietaryName(conflictingProprietaryName != null ? conflictingProprietaryName : "");
    }
    return component;
  }

  @Override
  protected com.sonatype.insight.brain.model.license.License getLicense(final String licenseId) {
    return licenseDAO.getByIdNotNull(licenseId);
  }
}
