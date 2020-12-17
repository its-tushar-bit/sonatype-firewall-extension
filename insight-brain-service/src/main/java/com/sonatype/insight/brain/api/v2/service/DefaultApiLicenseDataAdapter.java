/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.api.v2.service;

import javax.inject.Inject;
import javax.inject.Named;

import com.sonatype.insight.brain.dataaccess.license.MultiLicenseDAO;

/**
 * @since 1.13.0
 */
@Named
public class DefaultApiLicenseDataAdapter extends ApiLicenseDataAdapter
{
  private final MultiLicenseDAO multiLicenseDAO;

  @Inject
  public DefaultApiLicenseDataAdapter(final MultiLicenseDAO multiLicenseDAO) {
    this.multiLicenseDAO = multiLicenseDAO;
  }

  @Override
  protected String getLicenseNameById(final String licenseId) {
    return multiLicenseDAO.getByIdNotNull(licenseId).getShortDisplayName();
  }
}
