/*
 * Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.license;

import com.sonatype.insight.brain.model.license.LicenseCategory;

import org.junit.Assert;
import org.junit.Test;

public class LicenseCategoryDAOTest
    extends AbstractLicenseDAOTest
{
  @Test
  public void testLicenseDataRefresh() {
    String newId = "new category id";
    LicenseCategoryDAO dao = new LicenseCategoryDAO();
    Assert.assertNull(dao.getById(newId));
    int count = dao.getAll().size();

    LicenseCategory newLicenseCategory = new LicenseCategory();
    newLicenseCategory.setId(newId);
    newLicenseCategory.setName("New name");
    newLicenseCategory.setSeverity(4);
    dao.insert(newLicenseCategory);
    Assert.assertNull(dao.getById(newId));

    LicenseDataUpdater.setUpdater(new DummyLicenseDataUpdater());

    Assert.assertNotNull(dao.getById(newId));
    Assert.assertEquals(count + 1, dao.getAll().size());

    dao.delete(newLicenseCategory);
    dao.load();
  }
}
