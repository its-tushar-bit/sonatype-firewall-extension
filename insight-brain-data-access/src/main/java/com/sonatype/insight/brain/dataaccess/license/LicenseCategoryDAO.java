package com.sonatype.insight.brain.dataaccess.license;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractDatamartSqlDAO;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// Copied from com.sonatype.insight.datamart.dao.LicenseCategoryDAO
public class LicenseCategoryDAO
    extends AbstractDatamartSqlDAO<LicenseCategory>
{
  private static final Logger log = LoggerFactory.getLogger(LicenseCategoryDAO.class);

  private static volatile Map<String, LicenseCategory> licenseCategoriesById = null;

  @Override
  public LicenseCategory getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseCategory entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  @Override
  public LicenseCategory getById(String id) {
    if (licenseCategoriesById == null) {
      load();
    }
    LicenseCategory licenseCategory = licenseCategoriesById.get(id);
    if (licenseCategory == null) {
      log.info("Cannot find a license category with id '{}'.  Refreshing license data.", id);
      LicenseDataUpdater.update();
      licenseCategory = licenseCategoriesById.get(id);
    }
    return licenseCategory;
  }

  public LicenseCategory getByIdNotNull(String id) {
    LicenseCategory licenseCategory = getById(id);
    if (licenseCategory == null) {
      throw new NotFoundException("A license category with id '" + id + "' does not exist.");
    }
    return licenseCategory;
  }

  void load() {
    synchronized (getClass()) {
      long start = System.currentTimeMillis();

      String sQuery = "SELECT entity FROM LicenseCategory entity" + //
          " ORDER BY entity.severity DESC";
      List<LicenseCategory> licenseCategories = getList(sQuery);

      Map<String, LicenseCategory> _licenseCategoriesById = new LinkedHashMap<String, LicenseCategory>();
      for (LicenseCategory licenseCategory : licenseCategories) {
        _licenseCategoriesById.put(licenseCategory.getId(), licenseCategory);
      }
      licenseCategoriesById = _licenseCategoriesById;

      log.debug("Loaded all license categories in {} ms.", System.currentTimeMillis() - start);
    }
  }

  public Collection<LicenseCategory> getAll() {
    if (licenseCategoriesById == null) {
      load();
    }
    return licenseCategoriesById.values();
  }
}
