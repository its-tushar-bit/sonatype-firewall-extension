/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.dataaccess.security;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import javax.persistence.PersistenceException;

import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.db.AbstractMultiTenantDatabaseTest;
import com.sonatype.insight.brain.db.MultiTenantGlobalSchemaProtection;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.tenancy.Tenant;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MultiTenantGlobalWriteProtectionTest
    extends AbstractMultiTenantDatabaseTest
{
  private MultiTenantGlobalSchemaProtection multiTenantGlobalSchemaProtection;

  private static final String GLOBAL_SCHEMA_NAME = Tenant.GLOBAL_TENANT.databaseSchema;

  private static final String CREATE_TRIGGER_FUNCTION =
      "create or replace function write_protect()" +
          "    returns trigger " +
          "as " +
          "$func$ " +
          "    begin " +
          "        raise exception '" + GLOBAL_SCHEMA_NAME + " write protection'; " +
          "    end; " +
          "$func$ " +
          "language plpgsql;";

  private static final String SET_SCHEMA = "SET SCHEMA";

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

      assertThatThrownBy(
          // cannot write to Global organization table
          () -> organizationDAO.insert(organization))
          .withFailMessage("ERROR: global write protection")
          .isInstanceOf(PersistenceException.class);
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

      // organization table does not have the write_protection and insertions can be executed
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

      // organization table does not have the write_protection and insertions can be executed
      organizationDAO.insert(organization);

      multiTenantGlobalSchemaProtection.enableWriteProtection();

      Organization organization2 = new Organization();
      organization2.setName("bar");

      // Global tenant is again prevented from writing to organization table
      assertThatThrownBy(
          () -> organizationDAO.insert(organization2))
          .withFailMessage("ERROR: global write protection")
          .isInstanceOf(PersistenceException.class);
    });
  }

  @Test
  public void testGlobalSchema_CreatesWriteProtectionIfNotExistsBeforeEnabling() {
    //Creating the trigger function but not creating the write protection trigger
    executeUpdateOnOperationalDataStore(SET_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "';", CREATE_TRIGGER_FUNCTION);

    multiTenantGlobalSchemaProtection.enableWriteProtection();

    testAsGlobalTenant(tenant -> {
      OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
      Organization organization = new Organization();
      organization.setName("test1");

      assertThatThrownBy(
          // cannot write to Global organization table
          () -> organizationDAO.insert(organization))
          .withFailMessage("ERROR: global write protection")
          .isInstanceOf(PersistenceException.class);
    });
  }

  @Test
  public void testGlobalSchema_CreatesWriteProtectionIfNotExistsBeforeDisabling() {
    //Creating the trigger function but not creating the write protection trigger
    executeUpdateOnOperationalDataStore(SET_SCHEMA + " '" + GLOBAL_SCHEMA_NAME + "';", CREATE_TRIGGER_FUNCTION);

    multiTenantGlobalSchemaProtection.disableWriteProtection();

    testAsGlobalTenant(tenant -> {
      OrganizationDAO organizationDAO = daoFactory.createOrganizationDAO();
      Organization organization = new Organization();
      organization.setName("test2");

      // organization table does not have the write_protection and insertions can be executed
      organizationDAO.insert(organization);
    });
  }

  private void executeUpdateOnOperationalDataStore(String... updates) {
    try (Connection connection = databaseRule.getOperationalDataStore().getDataSource().getConnection();
         Statement statement = connection.createStatement()) {
      connection.setAutoCommit(true);

      for (String update : updates) {
        statement.executeUpdate(update);
      }
    }
    catch (SQLException e) {
      throw new RuntimeException("Error trying update Operational Data Store.", e);
    }
  }
}
