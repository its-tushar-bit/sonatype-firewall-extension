/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Copied from com.sonatype.insight.datamart.dao.MultiLicenseDAO
public class MultiLicenseDAO
    extends AbstractDatamartSqlDAO<MultiLicense>
{
  private static final Logger log = LoggerFactory.getLogger(MultiLicenseDAO.class);

  private static volatile Map<String, MultiLicense> multiLicensesById = null;

  private static volatile Map<String, MultiLicense> multiLicensesByName = null;

  private static volatile Map<String, Set<License>> licenseSetsById = null;

  @Override
  public MultiLicense getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM MultiLicense entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  public Collection<MultiLicense> getAll() {
    if (multiLicensesByName == null) {
      load();
    }
    return multiLicensesByName.values();
  }

  @Override
  public MultiLicense getById(String id) {
    if (multiLicensesById == null) {
      load();
    }
    MultiLicense multiLicense = multiLicensesById.get(id);
    if (multiLicense == null) {
      log.info("Cannot find a multi-license with id '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update();
      multiLicense = multiLicensesById.get(id);
    }
    return multiLicense;
  }

  public MultiLicense getByIdNotNull(String id) {
    MultiLicense license = getById(id);
    if (license == null) {
      throw new NotFoundException("A multi-license with id '" + id + "' does not exist.");
    }
    return license;
  }

  public MultiLicense getByName(String name) {
    if (multiLicensesByName == null) {
      load();
    }
    MultiLicense multiLicense = multiLicensesByName.get(name);
    if (multiLicense == null) {
      log.info("Cannot find a multi-license with name '{}'.  Refreshing license data.", name);
      LicenseDataUpdater.update();
      multiLicense = multiLicensesByName.get(name);
    }
    return multiLicense;
  }

  public MultiLicense getByNameNotNull(String name) {
    MultiLicense license = getByName(name);
    if (license == null) {
      throw new NotFoundException("A multi-license with name '" + name + "' does not exist.");
    }
    return license;
  }

  /**
   * Look for license by id locally, will attempt to refresh from remote if local data is lacking.
   */
  public Set<License> getLicensesByMultiLicenseIdNotNull(String id) {
    if (licenseSetsById == null) {
      load();
    }
    Set<License> licenses = licenseSetsById.get(id);
    if (licenses == null) {
      log.info("Cannot find a multi-license with id '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update();
      load();
    }

    licenses = licenseSetsById.get(id);
    if (licenses == null) {
      throw new NotFoundException("A multi-license with id '" + id + "' does not exist locally or remotely.");
    }

    return licenses;
  }

  public Integer getLicenseThreatLevelByApplicationAndMultiLicenseId(Application application, String multiLicenseId) {
    final LicenseDAO licenseDAO = new LicenseDAO();
    final Set<License> licenses = getLicensesByMultiLicenseIdNotNull(multiLicenseId);
    Integer threatLevel = null;
    for (License license : licenses) {
      threatLevel = max(threatLevel,
          licenseDAO.getLicenseThreatLevelByApplicationAndLicenseId(application, license.getId()));
    }
    return threatLevel;
  }

  /**
   * @since 1.6
   */
  private Integer max(Integer threatLevel1, Integer threatLevel2) {
    if (threatLevel1 == null) {
      return threatLevel2;
    }
    if (threatLevel2 == null) {
      return threatLevel1;
    }
    return Math.max(threatLevel1, threatLevel2);
  }

  /**
   * Load and cache license information from locally available data
   */
  void load() {
    synchronized (this.getClass()) {
      long start = System.currentTimeMillis();

      String sQuery = "SELECT license FROM MultiLicense license" + //
          " ORDER BY license.shortDisplayName";
      List<MultiLicense> multiLicenses = getList(sQuery);

      sQuery = "SELECT license FROM MultiLicenseLicenseInternal license";
      @SuppressWarnings({ "unchecked", "rawtypes" })
      List<MultiLicenseLicenseInternal> mappings = (List) getList(sQuery);

      Map<String, Set<License>> _licenseSetsById = new LinkedHashMap<String, Set<License>>();

      Map<String, MultiLicense> _licensesById = new LinkedHashMap<String, MultiLicense>();
      for (MultiLicense license : multiLicenses) {
        _licensesById.put(license.getId(), license);
        _licenseSetsById.put(license.getId(), new LinkedHashSet<License>());
      }
      multiLicensesById = _licensesById;

      Map<String, MultiLicense> _licensesByName = new TreeMap<String, MultiLicense>(String.CASE_INSENSITIVE_ORDER);
      for (MultiLicense license : multiLicenses) {
        _licensesByName.put(license.getShortDisplayName(), license);
      }
      multiLicensesByName = _licensesByName;

      LicenseDAO licenseDAO = new LicenseDAO();
      for (MultiLicenseLicenseInternal mapping : mappings) {
        License license = licenseDAO.getByIdNotNull(mapping.getLicenseId());
        _licenseSetsById.get(mapping.getMultiLicenseId()).add(license);
      }

      for (Map.Entry<String, Set<License>> entry : _licenseSetsById.entrySet()) {
        entry.setValue(Collections.unmodifiableSet(entry.getValue()));
      }
      licenseSetsById = _licenseSetsById;

      log.debug("Loaded all multi-licenses in {} ms.", System.currentTimeMillis() - start);
    }
  }
}
