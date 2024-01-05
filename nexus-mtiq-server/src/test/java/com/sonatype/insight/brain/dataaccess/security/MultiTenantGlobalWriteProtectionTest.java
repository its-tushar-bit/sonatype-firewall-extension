/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.model.Organization;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class MultiTenantGlobalWriteProtectionTest
    extends AbstractMultiTenantDatabaseTest
{
  private MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

  @Override
  @Before
  public void setup() {
    super.setup();
    multiTenantGlobalSchemaProtection = new MultiTenantGlobalSchemaProtection(databaseRule.getOperationalDataStore());
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionEnabled() {
    multiTenantGlobalSchemaProtection.createWriteProtection();

    testAsGlobalTenant(tenant -> {
      OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
      Organization organization = new Organization();
      organization.setName("foo");

      try {
        // global cannot write to organizations table
        organizationDAO.insert(organization);
      }
      catch (PersistenceException e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        assertThat(rootCause.getMessage()).contains("ERROR: global write protection");
      }
    });
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionDisabled() {
    multiTenantGlobalSchemaProtection.createWriteProtection(); //Global schema should be protected first
    multiTenantGlobalSchemaProtection.disableWriteProtection();

    testAsGlobalTenant(tenant -> {
      OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
      Organization organization = new Organization();
      organization.setName("foo");

      // role and role_permission tables do not have the write_protection and insertions can be executed
      organizationDAO.insert(organization);
    });
  }

  @Test
  public void testGlobalSchema_HasWriteProtectionReEnabled() {
    multiTenantGlobalSchemaProtection.createWriteProtection(); //Global schema should be protected first
    multiTenantGlobalSchemaProtection.disableWriteProtection();

    testAsGlobalTenant(global -> {
      OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
      Organization organization = new Organization();
      organization.setName("xrf");

      // role and role_permission tables do not have the write_protection and insertions can be executed
      organizationDAO.insert(organization);

      multiTenantGlobalSchemaProtection.enableWriteProtection();

      Organization organization2 = new Organization();
      organization2.setName("bar");

      // Global tenant is again prevented from writing to role and role_permission tables
      try {
        organizationDAO.insert(organization2);
      }
      catch (PersistenceException e) {
        Throwable rootCause = ExceptionUtils.getRootCause(e);
        assertThat(rootCause.getMessage()).contains("ERROR: global write protection");
      }
    });
  }
}
