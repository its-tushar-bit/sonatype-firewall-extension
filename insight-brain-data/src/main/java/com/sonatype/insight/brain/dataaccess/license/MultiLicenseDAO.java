/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.db.datastore.DataMartDataStore;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.MultiLicense;
import com.sonatype.insight.brain.model.license.MultiLicenseLicenseInternal;
import com.sonatype.insight.dataaccess.TransactionContext;
import com.sonatype.insight.error.exception.NotFoundException;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.jooq.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.sonatype.insight.brain.jooq.generated.dm.tables.MultiLicense.MULTI_LICENSE;
import static com.sonatype.insight.brain.jooq.generated.dm.tables.MultiLicenseLicense.MULTI_LICENSE_LICENSE;

// Copied from com.sonatype.insight.datamart.dao.MultiLicenseDAO
@Named
@Singleton
public class MultiLicenseDAO
    extends AbstractDatamartSqlDAO<MultiLicense>
{
  private static final Logger log = LoggerFactory.getLogger(MultiLicenseDAO.class);

  private static volatile Map<String, MultiLicense> multiLicensesById = null;

  private static volatile Map<String, MultiLicense> multiLicensesByName = null;

  private static volatile Map<String, Set<License>> licenseSetsById = null;

  private final LicenseDAO licenseDAO;

  @Inject
  public MultiLicenseDAO(final DataMartDataStore dataMartDataStore, final LicenseDAO licenseDAO) {
    super(dataMartDataStore);
    this.licenseDAO = licenseDAO;
  }

  @Override
  public List<MultiLicense> getAll() {
    if (multiLicensesByName == null) {
      load();
    }
    return multiLicensesByName.values().stream().toList();
  }

  @Override
  public MultiLicense getById(String id) {
    if (multiLicensesById == null) {
      load();
    }
    MultiLicense multiLicense = multiLicensesById.get(id);
    if (multiLicense == null) {
      log.info("Cannot find a multi-license with ID '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update(licenseDAO, this);
      multiLicense = multiLicensesById.get(id);
    }
    return multiLicense;
  }

  @Override
  public MultiLicense getByIdNotNull(String id) {
    MultiLicense license = getById(id);
    if (license == null) {
      throw new NotFoundException("A multi-license with ID '" + id + "' does not exist.");
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
      LicenseDataUpdater.update(licenseDAO, this);
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
      log.info("Cannot find a multi-license with ID '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update(licenseDAO, this);
      load();
    }

    licenses = licenseSetsById.get(id);
    if (licenses == null) {
      throw new NotFoundException("A multi-license with ID '" + id + "' does not exist locally or remotely.");
    }

    return licenses;
  }

  /**
   * Load and cache license information from locally available data
   */
  public void load() {
    synchronized (this.getClass()) {
      long start = System.currentTimeMillis();

      List<MultiLicense> multiLicenses;
      List<MultiLicenseLicenseInternal> mappings;
      try (TransactionContext tx = createTransactionContext()) {
        multiLicenses = tx.dsl()
            .selectFrom(MULTI_LICENSE)
            .orderBy(MULTI_LICENSE.SHORTDISPLAYNAME)
            .fetchInto(MultiLicense.class);

        mappings = tx.dsl()
            .selectFrom(MULTI_LICENSE_LICENSE)
            .fetchInto(MultiLicenseLicenseInternal.class);
      }

      Map<String, Set<License>> newLicenseSetsById = new LinkedHashMap<>();

      Map<String, MultiLicense> newMultiLicensesById = new LinkedHashMap<>();
      for (MultiLicense license : multiLicenses) {
        newMultiLicensesById.put(license.getId(), license);
        newLicenseSetsById.put(license.getId(), new LinkedHashSet<>());
      }
      multiLicensesById = newMultiLicensesById;

      Map<String, MultiLicense> newMultiLicensesByName = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      for (MultiLicense license : multiLicenses) {
        newMultiLicensesByName.put(license.getShortDisplayName(), license);
      }
      multiLicensesByName = newMultiLicensesByName;

      for (MultiLicenseLicenseInternal mapping : mappings) {
        License license = licenseDAO.getByIdNotNull(mapping.getLicenseId());
        newLicenseSetsById.get(mapping.getMultiLicenseId()).add(license);
      }

      for (Map.Entry<String, Set<License>> entry : newLicenseSetsById.entrySet()) {
        entry.setValue(Collections.unmodifiableSet(entry.getValue()));
      }
      licenseSetsById = newLicenseSetsById;

      log.debug("Loaded all {} multi-licenses in {} ms.", newMultiLicensesById.size(),
          System.currentTimeMillis() - start);
    }
  }

  public MultiLicense getByIdNoReload(String id) {
    if (multiLicensesById == null) {
      load();
    }
    return multiLicensesById.get(id);
  }

  public MultiLicense getByIdNoReloadNotNull(String id) {
    MultiLicense multiLicense = getByIdNoReload(id);
    if (multiLicense == null) {
      throw new NotFoundException("A multi-license with ID '" + id + "' does not exist locally.");
    }
    return multiLicense;
  }

  public MultiLicense getByNameNoReload(String name) {
    if (multiLicensesByName == null) {
      load();
    }
    return multiLicensesByName.get(name);
  }

  public MultiLicense getByNameNoReloadNotNull(String name) {
    MultiLicense multiLicense = getByNameNoReload(name);
    if (multiLicense == null) {
      throw new NotFoundException("A multi-license with name '" + name + "' does not exist locally.");
    }
    return multiLicense;
  }

  @Override
  public Table<?> getJooqTable() {
    return MULTI_LICENSE;
  }

  @Override
  public Class<MultiLicense> getEntityClass() {
    return MultiLicense.class;
  }
}
