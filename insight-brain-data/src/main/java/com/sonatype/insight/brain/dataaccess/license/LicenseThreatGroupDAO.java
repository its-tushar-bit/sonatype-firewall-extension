/*
 * Copyright (c) 2011-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.NameHelper;
import com.sonatype.insight.brain.model.license.License;
import com.sonatype.insight.brain.model.license.LicenseCategory;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.error.exception.NotFoundException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LicenseThreatGroupDAO
    extends AbstractOperationalSqlDAO<LicenseThreatGroup>
{
  public static final int DEFAULT_LICENSE_THREAT_GROUP_COUNT = 4;

  private static final Logger log = LoggerFactory.getLogger(LicenseThreatGroupDAO.class);

  public List<LicenseThreatGroup> getByOwnerId(EntityManager em, String ownerId) {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.ownerId=?1" + //
        " ORDER BY entity.name";
    return getList(em, sQuery, ownerId);
  }

  public List<LicenseThreatGroup> getByOwnerId(String ownerId) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerId(em, ownerId);
    }
    finally {
      close(em);
    }
  }

  public List<LicenseThreatGroup> getByOwnerIdAndLicenseId(String ownerId, String licenseId) {
    String sQuery = "SELECT licenseThreatGroup" + //
        " FROM LicenseThreatGroup licenseThreatGroup, LicenseThreatGroupLicense licenseThreatGroupLicense" + //
        " WHERE licenseThreatGroup.id=licenseThreatGroupLicense.licenseThreatGroupId" + //
        " AND licenseThreatGroup.ownerId=?1 AND licenseThreatGroupLicense.licenseId=?2";
    return getList(sQuery, ownerId, licenseId);
  }

  @Override
  protected LicenseThreatGroup getById(EntityManager em, String id) {
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.id=?1";
    return get(em, sQuery, id);
  }

  LicenseThreatGroup getByIdNotNull(EntityManager em, String id) {
    LicenseThreatGroup licenseThreatGroup = getById(em, id);
    if (licenseThreatGroup == null) {
      throw new NotFoundException("Cannot find a license threat group with id " + id);
    }
    return licenseThreatGroup;
  }

  public LicenseThreatGroup getByIdNotNull(String id) {
    EntityManager em = createEntityManager();
    try {
      return getByIdNotNull(em, id);
    }
    finally {
      close(em);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(String ownerId, String name) {
    EntityManager em = createEntityManager();
    try {
      return getByOwnerIdAndName(em, ownerId, name);
    }
    finally {
      close(em);
    }
  }

  public LicenseThreatGroup getByOwnerIdAndName(EntityManager em, String ownerId, String name) {
    name = NameHelper.normalize(name);
    String sQuery = "SELECT entity FROM LicenseThreatGroup entity" + //
        " WHERE entity.ownerId=?1 AND entity.nameLowercaseNoWhitespace=?2";
    return get(em, sQuery, ownerId, name);
  }

  @Override
  public void insert(EntityManager em, LicenseThreatGroup licenseThreatGroup) {
    validateThreatLevel(licenseThreatGroup.getThreatLevel());

    validateName(em, licenseThreatGroup);
    if (getByOwnerIdAndName(em, licenseThreatGroup.getOwnerId(), licenseThreatGroup.getName()) != null) {
      throw new InvalidLicenseThreatGroupException("A license threat group with the same name already exists");
    }

    super.insert(em, licenseThreatGroup);
  }

  private void validateName(EntityManager em, LicenseThreatGroup licenseThreatGroup) {
    NameHelper.validate(licenseThreatGroup.getName());

    ApplicationDAO applicationDAO = new ApplicationDAO();
    Application parentApplication = applicationDAO.getById(em, licenseThreatGroup.getOwnerId());
    if (parentApplication != null) {
      // The owner is an application
      if (getByOwnerIdAndName(em, parentApplication.getOrganizationId(), licenseThreatGroup.getName()) != null) {
        throw new InvalidLicenseThreatGroupException(
            "A license threat group with the same name already exists for the parent organization");
      }
    }
    else {
      // The owner is an organization
      List<Application> applications = applicationDAO.getByOrganizationId(em, licenseThreatGroup.getOwnerId());
      for (Application application : applications) {
        if (getByOwnerIdAndName(em, application.getId(), licenseThreatGroup.getName()) != null) {
          throw new InvalidLicenseThreatGroupException(
              "A license threat group with the same name already exists for application '" + application.getName()
                  + "'");
        }
      }
    }
  }

  @Override
  public void update(EntityManager em, LicenseThreatGroup licenseThreatGroup) {
    validateThreatLevel(licenseThreatGroup.getThreatLevel());

    validateName(em, licenseThreatGroup);
    LicenseThreatGroup otherLicenseThreatGroup = getByOwnerIdAndName(em, licenseThreatGroup.getOwnerId(),
        licenseThreatGroup.getName());
    if (otherLicenseThreatGroup != null && !otherLicenseThreatGroup.getId().equals(licenseThreatGroup.getId())) {
      throw new InvalidLicenseThreatGroupException("A license threat group with the same name already exists");
    }

    super.update(em, licenseThreatGroup);
  }

  private void validateThreatLevel(int threatLevel) {
    if (threatLevel < 0 || threatLevel > 10) {
      throw new InvalidLicenseThreatGroupException("The threat level must be a number between 0 and 10");
    }
  }

  @Override
  public void delete(EntityManager em, LicenseThreatGroup licenseThreatGroup) {
    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();
    List<LicenseThreatGroupLicense> licenseThreatGroupLicenses = licenseThreatGroupLicenseDAO
        .getByLicenseThreatGroupId(em, licenseThreatGroup.getId());
    for (LicenseThreatGroupLicense licenseThreatGroupLicense : licenseThreatGroupLicenses) {
      licenseThreatGroupLicenseDAO.delete(em, licenseThreatGroupLicense);
    }
    super.delete(em, licenseThreatGroup);
  }

  public void createDefaultGroups(EntityManager em, String ownerId) {
    long start = System.currentTimeMillis();

    LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

    Map<String, LicenseThreatGroup> licenseThreatGroupsByName = new LinkedHashMap<String, LicenseThreatGroup>();
    List<License> allLicenses = new LicenseDAO().getAll();
    for (License license : allLicenses) {
      String licenseCategoryId = license.getLicenseCategoryId();
      if (licenseCategoryId == null) {
        continue;
      }
      String licenseThreatGroupName = null;
      int threatLevel = 0;
      if (LicenseCategory.COPYLEFT_ID.equals(licenseCategoryId)) {
        licenseThreatGroupName = "Copyleft";
        threatLevel = 9;
      }
      else if (LicenseCategory.NON_STANDARD_ID.equals(licenseCategoryId)) {
        licenseThreatGroupName = "Non Standard";
        threatLevel = 6;
      }
      else if (LicenseCategory.WEAKCOPYLEFT_ID.equals(licenseCategoryId)) {
        licenseThreatGroupName = "Weak Copyleft";
        threatLevel = 2;
      }
      else if (LicenseCategory.LIBERAL_ID.equals(licenseCategoryId)) {
        licenseThreatGroupName = "Liberal";
        threatLevel = 0;
      }
      else {
        throw new IllegalStateException("Unknown license category id: " + licenseCategoryId);
      }
      LicenseThreatGroup licenseThreatGroup = licenseThreatGroupsByName.get(licenseThreatGroupName);
      if (licenseThreatGroup == null) {
        licenseThreatGroup = new LicenseThreatGroup();
        licenseThreatGroup.setOwnerId(ownerId);
        licenseThreatGroup.setName(licenseThreatGroupName);
        licenseThreatGroup.setThreatLevel(threatLevel);
        insert(em, licenseThreatGroup);
        licenseThreatGroupsByName.put(licenseThreatGroupName, licenseThreatGroup);
      }
      LicenseThreatGroupLicense licenseThreatGroupLicense = new LicenseThreatGroupLicense();
      licenseThreatGroupLicense.setOwnerId(ownerId);
      licenseThreatGroupLicense.setLicenseThreatGroupId(licenseThreatGroup.getId());
      licenseThreatGroupLicense.setLicenseId(license.getId());
      licenseThreatGroupLicenseDAO.insert(em, licenseThreatGroupLicense);
    }

    log.debug("Created default license threat groups for owner id {} in {} ms.", ownerId, System.currentTimeMillis()
        - start);
  }
}
