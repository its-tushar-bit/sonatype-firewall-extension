/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Copied from com.sonatype.insight.datamart.dao.LicenseDAO
public class LicenseDAO
    extends AbstractDatamartSqlDAO<License>
{
  private static final Logger log = LoggerFactory.getLogger(LicenseDAO.class);

  private static volatile List<License> licenses;

  private static volatile Map<String, License> licensesById = null;

  private static volatile Map<String, License> licensesByName = null;

  @Override
  public License getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM License entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public License getById(String id) {
    if (licensesById == null) {
      load();
    }
    License license = licensesById.get(id);
    if (license == null) {
      log.info("Cannot find a license with id '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update();
      license = licensesById.get(id);
    }
    return license;
  }

  public License getByIdNotNull(String id) {
    License license = getById(id);
    if (license == null) {
      throw new NotFoundException("A license with id '" + id + "' does not exist.");
    }
    return license;
  }

  void load() {
    synchronized (this.getClass()) {
      long start = System.currentTimeMillis();

      String sQuery = "SELECT license FROM License license";
      List<License> _licenses = new ArrayList<License>();
      _licenses.addAll(getList(sQuery));
      Collections.sort(_licenses, new Comparator<License>()
      {
        @Override
        public int compare(License license1, License license2) {
          return license1.getShortDisplayName().toLowerCase(Locale.ENGLISH)
              .compareTo(license2.getShortDisplayName().toLowerCase(Locale.ENGLISH));
        }
      });

      Map<String, License> _licensesById = new LinkedHashMap<String, License>();
      for (License license : _licenses) {
        _licensesById.put(license.getId(), license);
      }

      Map<String, License> _licensesByName = new TreeMap<String, License>(String.CASE_INSENSITIVE_ORDER);
      for (License license : _licenses) {
        _licensesByName.put(license.getShortDisplayName(), license);
      }

      licenses = Collections.unmodifiableList(_licenses);
      licensesById = Collections.unmodifiableMap(_licensesById);
      licensesByName = _licensesByName;

      log.debug("Loaded all licenses in {} ms.", System.currentTimeMillis() - start);
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
      LicenseDataUpdater.update();
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

  /**
   * @since 1.6
   */
  public Integer getLicenseThreatLevelByApplicationAndLicenseId(Application application, String licenseId) {
    final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();
    String organizationId = application.getOrganizationId();
    Integer threatLevel = null;
    List<LicenseThreatGroup> licenseThreatGroups = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(application.getId(),
        licenseId);
    threatLevel = max(threatLevel, licenseThreatGroups);

    if (organizationId != null) {
      licenseThreatGroups = licenseThreatGroupDAO.getByOwnerIdAndLicenseId(organizationId, licenseId);
      threatLevel = max(threatLevel, licenseThreatGroups);
    }
    return threatLevel;
  }

  /**
   * @since 1.6
   */
  private Integer max(Integer threatLevel, List<LicenseThreatGroup> licenseThreatGroups) {
    for (LicenseThreatGroup licenseThreatGroup : licenseThreatGroups) {
      if (threatLevel == null) {
        threatLevel = licenseThreatGroup.getThreatLevel();
      }
      else {
        threatLevel = Math.max(threatLevel, licenseThreatGroup.getThreatLevel());
      }
    }
    return threatLevel;
  }
}
