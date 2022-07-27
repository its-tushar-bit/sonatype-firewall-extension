/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class LicenseDataUpdater
{
  private static final Logger log = LoggerFactory.getLogger(LicenseDataUpdater.class);

  private static LicenseDataUpdater updater;

  public static LicenseDataUpdater getUpdater() {
    return updater;
  }

  public static synchronized void setUpdater(LicenseDataUpdater updater) {
    LicenseDataUpdater.updater = updater;
  }

  public static synchronized void update() {
    if (updater == null) {
      log.warn("Cannot update license data because there is no license updater.");
      return;
    }

    updater.doUpdate();
    loadLicenses();
  }

  public static synchronized void loadLicenses() {
    new LicenseDAO().load();
    new MultiLicenseDAO().load();
  }

  public abstract void doUpdate();
}
