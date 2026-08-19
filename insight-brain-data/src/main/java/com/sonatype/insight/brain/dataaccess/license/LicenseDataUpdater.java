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

  protected LicenseDAO licenseDAO;

  protected MultiLicenseDAO multiLicenseDAO;

  public LicenseDataUpdater(LicenseDAO licenseDAO, MultiLicenseDAO multiLicenseDAO) {
    this.licenseDAO = licenseDAO;
    this.multiLicenseDAO = multiLicenseDAO;
  }

  public static LicenseDataUpdater getUpdater() {
    return updater;
  }

  public static synchronized void setUpdater(LicenseDataUpdater newUpdater) {
    if (updater != null && newUpdater != null) {
      log.debug("Replacing existing LicenseDataUpdater instance");
    }
    updater = newUpdater;
  }

  public static synchronized void clearUpdater() {
    updater = null;
  }

  public static void update(LicenseDAO licenseDAO, MultiLicenseDAO multiLicenseDAO) {
    if (updater == null) {
      log.warn("Cannot update license data because there is no license updater.");
      return;
    }

    synchronized (LicenseDataUpdater.class) {
      // Use this class lock to update data, load methods below use their own locks so this only takes 1 lock at a time
      updater.doUpdate();
    }

    licenseDAO.load();
    multiLicenseDAO.load();
  }

  public synchronized void loadLicenses() {
    licenseDAO.load();
    multiLicenseDAO.load();
  }

  public abstract void doUpdate();
}
