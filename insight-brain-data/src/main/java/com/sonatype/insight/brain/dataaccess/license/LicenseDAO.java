/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Copied from com.sonatype.insight.datamart.dao.LicenseDAO
@Named
@Singleton
public class LicenseDAO
    extends AbstractDatamartSqlDAO<License>
{
  private static final Logger log = LoggerFactory.getLogger(LicenseDAO.class);

  private static volatile List<License> licenses;

  private static volatile Map<String, License> licensesById = null;

  private static volatile Map<String, License> licensesByName = null;

  private final Provider<MultiLicenseDAO> multiLicenseDAOProvider;

  @Inject
  public LicenseDAO(
      final DataMartDataStore dataMartDataStore,
      final Provider<MultiLicenseDAO> multiLicenseDAOProvider)
  {
    super(dataMartDataStore);
    this.multiLicenseDAOProvider = multiLicenseDAOProvider;
  }

  @Override
  public License getById(TransactionContext tx, String id) {
    String sQuery = "SELECT entity FROM License entity" + //
        " WHERE entity.id=?1";
    return get(tx, sQuery, id);
  }

  @Override
  public License getById(String id) {
    if (licensesById == null) {
      load();
    }
    License license = licensesById.get(id);
    if (license == null) {
      log.info("Cannot find a license with ID '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update(this, multiLicenseDAOProvider.get());
      license = licensesById.get(id);
      if (null == license) {
        log.warn("License with ID '{}' not found after refresh.", id);
      }
    }
    return license;
  }

  @Override
  public License getByIdNotNull(String id) {
    License license = getById(id);
    if (license == null) {
      throw new NotFoundException("A license with ID '" + id + "' does not exist.");
    }
    return license;
  }

  // Visible for test
  public void load() {
    synchronized (this.getClass()) {
      long start = System.currentTimeMillis();

      String sQuery = "SELECT license FROM License license";
      List<License> newLicenses = new ArrayList<>();
      newLicenses.addAll(getList(sQuery));
      newLicenses.sort((license1, license2) -> {
        return license1.getShortDisplayName().compareToIgnoreCase(license2.getShortDisplayName());
      });

      Map<String, License> newlicensesById = new LinkedHashMap<>();
      for (License license : newLicenses) {
        newlicensesById.put(license.getId(), license);
      }

      Map<String, License> newLicensesByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      for (License license : newLicenses) {
        newLicensesByName.put(license.getShortDisplayName(), license);
      }

      licenses = Collections.unmodifiableList(newLicenses);
      licensesById = Collections.unmodifiableMap(newlicensesById);
      licensesByName = newLicensesByName;

      log.debug("Loaded all {} licenses in {} ms.", newlicensesById.size(), System.currentTimeMillis() - start);
    }
  }

  public List<License> getAll() {
    if (licenses == null) {
      load();
    }
    return licenses;
  }

  /**
   * @since 1.6
   */
  public License getByName(String name) {
    if (licensesByName == null) {
      load();
    }
    License license = licensesByName.get(name);
    if (license == null) {
      log.info("Cannot find a license with name '{}'.  Refreshing license data.", name);
      LicenseDataUpdater.update(this, multiLicenseDAOProvider.get());
      license = licensesByName.get(name);
    }
    return license;
  }

  /**
   * @since 1.6
   */
  public License getByNameNotNull(String name) {
    License license = getByName(name);
    if (license == null) {
      throw new NotFoundException("A license with name '" + name + "' does not exist.");
    }
    return license;
  }
}
