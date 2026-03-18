/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.support;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sonatype.insight.brain.api.v2.SystemConfigurationPropertyFeatureTestHelper;
import com.sonatype.insight.brain.dataaccess.AbstractOperationalSqlDAO;
import com.sonatype.insight.brain.dataaccess.ConditionTypesTestHelper;
import com.sonatype.insight.brain.dataaccess.DAOFactory;
import com.sonatype.insight.brain.dataaccess.TestDAOFactory;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.db.DatabaseConfigProviderFactory;
import com.sonatype.insight.brain.db.DatabaseContainer;
import com.sonatype.insight.brain.db.DatabaseContainerSupport;
import com.sonatype.insight.brain.db.DatabaseName;
import com.sonatype.insight.brain.db.DatabaseProvisioner;
import com.sonatype.insight.brain.db.DatabaseUtil;
import com.sonatype.insight.brain.db.DefaultDatabaseContainer;
import com.sonatype.insight.brain.db.datasource.DataSourceProvider;
import com.sonatype.insight.brain.db.datasource.DataSourceProviderFactory;
import com.sonatype.insight.brain.db.datastore.DefaultOperationalDataStore;
import com.sonatype.insight.brain.db.datastore.OperationalDataStore;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.MigrationTracker;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.configuration.DataRetentionPolicy;
import com.sonatype.insight.brain.model.configuration.ProprietaryConfig;
import com.sonatype.insight.brain.model.configuration.SystemConfigurationProperty;
import com.sonatype.insight.brain.model.configuration.webhook.Webhook;
import com.sonatype.insight.brain.model.label.ComponentLabel;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyMonitoring;
import com.sonatype.insight.brain.model.repository.Repository;
import com.sonatype.insight.brain.model.repository.RepositoryManager;
import com.sonatype.insight.brain.model.security.MembershipMapping;
import com.sonatype.insight.brain.model.security.Role;
import com.sonatype.insight.brain.model.security.RolePermission;
import com.sonatype.insight.brain.model.security.User;
import com.sonatype.insight.brain.model.sourcecontrol.SourceControl;
import com.sonatype.insight.brain.model.tag.ApplicationTag;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.model.vulnerability.SecurityVulnerabilityOverride;
import com.sonatype.insight.brain.service.InsightBrainService;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.db.DatabaseConfig;
import com.sonatype.insight.db.DatabaseEngine;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.core.cli.Cli;
import io.dropwizard.core.cli.ConfiguredCommand;
import io.dropwizard.core.setup.Bootstrap;
import net.sourceforge.argparse4j.inf.Namespace;
import org.apache.commons.io.FileUtils;

/**
 * Utility class to import the db data dump from a support.zip file.
 * Useful when working on customer tickets (aka zendesk) that have a support.zip file with db data dump.
 *
 * It extends InsightBrainService, so it can be run just like InsightBrainService.
 * This is helpful because you can just run InsightBrainService after the data is imported to start debugging with the
 * newly imported data.
 *
 * To run it provide these params:
 * import-db <my-config.yml>
 *
 * You will be prompted to enter the path for the unzipped db data.
 *
 * Hint:
 * You can use the same config.yml you use to run InsightBrainService to import the customer data into the same db.
 */
public class DbImportFromSupportZip
    extends InsightBrainService
{
  public static void main(final String[] args) {
    try {
      DbImportFromSupportZip dbImport = new DbImportFromSupportZip();

      dbImport.run(args);
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  @Override
  public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
    return new DefaultDatabaseContainer(insightConfig);
  }

  @Override
  public void initialize(final Bootstrap<InsightConfig> bootstrap) {
    super.initialize(bootstrap);
    bootstrap.addCommand(new DbImportCommand());
  }

  @Override
  protected void configureObjectMapperDeserializationFeature(ObjectMapper objectMapper) {
    // Disable for MTIQ, allow unknown properties in Insight Config
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
  }

  private class DbImportCommand
      extends ConfiguredCommand<InsightConfig>
      implements DatabaseContainerSupport
  {
    private OperationalDataStore operationalDataStore;

    private File dataDumpDir;

    private DAOFactory daoFactory;

    public DbImportCommand() {
      super("import-db", "Imports data from a support zip database dump files into the current database.");
    }

    @Override
    public void onError(Cli cli, Namespace namespace, Throwable t) {
      // throw up to let our main() method do the desired error logging/handling
      throw new IllegalStateException("Error trying to import db data: " + t.getMessage(), t);
    }

    protected OperationalDataStore getOperationalDataStore(DatabaseConfig databaseConfig) {
      DatabaseEngine databaseEngine = DatabaseUtil.getDatabaseEngine(databaseConfig);
      DataSourceProvider dataSourceProvider = DataSourceProviderFactory.createDataSourceProvider(databaseEngine);
      return new DefaultOperationalDataStore(dataSourceProvider, databaseConfig);
    }

    @Override
    protected void run(
        Bootstrap<InsightConfig> bootstrap,
        Namespace namespace,
        InsightConfig insightConfig) throws IOException
    {
      DatabaseContainer databaseContainer = createDatabaseContainer(insightConfig);

      DatabaseProvisioner databaseProvisioner = databaseContainer.getDatabaseProvisioner();
      databaseProvisioner.initializeDatabaseWithMigration();
      DatabaseConfig databaseConfig =
          DatabaseConfigProviderFactory.createDatabaseConfigProvider(insightConfig).getDatabaseConfig(DatabaseName.ods);
      operationalDataStore = getOperationalDataStore(databaseConfig);
      operationalDataStore.initialize();

      dataDumpDir = getDataDumpDir();
      if (!dataDumpDir.exists()) {
        throw new RuntimeException("Data dump dir " + dataDumpDir.getAbsolutePath() + " does not exist.");
      }
      log.info("Using data dump dir: {}", dataDumpDir.getAbsolutePath());
      daoFactory = new TestDAOFactory(databaseContainer);

      ConditionTypesTestHelper.initConditionTypes(daoFactory);
      ConditionTypesTestHelper.initConditionValueTypes(daoFactory);
      SystemConfigurationPropertyFeatureTestHelper.inject(daoFactory);

      deletePolicyData();
      importAllData();
    }

    private void deletePolicyData() {
      PolicyTagDAO policyTagDAO = daoFactory.createPolicyTagDAO();
      policyTagDAO.getAll().forEach(policyTagDAO::delete);
      TagDAO tagDAO = daoFactory.createTagDAO();
      tagDAO.getAll().forEach(tagDAO::delete);
      PolicyDAO policyDAO = daoFactory.createPolicyDAO();
      policyDAO.getAll().forEach(policyDAO::delete);
    }

    private void importAllData() throws IOException {
      importData("organization.json", daoFactory.createOrganizationDAO(), Organization[].class);
      importData("application.json", daoFactory.createApplicationDAO(), Application[].class);
      importData("tag.json", daoFactory.createTagDAO(), Tag[].class);
      importData("applicationTag.json", daoFactory.createApplicationTagDAO(), ApplicationTag[].class);
      importData("role.json", daoFactory.createRoleDAO(), Role[].class);
      importData("rolePermission.json", daoFactory.createRolePermissionDAO(), RolePermission[].class);
      importData("membershipMapping.json", daoFactory.createMembershipMappingDAO(), MembershipMapping[].class);
      importData("componentLabel.json", daoFactory.createComponentLabelDAO(), ComponentLabel[].class);
      importData("dataRetentionPolicy.json", daoFactory.createDataRetentionPolicyDAO(), DataRetentionPolicy[].class);
      importData("label.json", daoFactory.createLabelDAO(), Label[].class);
      importData("licenseThreatGroup.json", daoFactory.createLicenseThreatGroupDAO(), LicenseThreatGroup[].class);
      importData("licenseThreatGroupLicense.json", daoFactory.createLicenseThreatGroupLicenseDAO(),
          LicenseThreatGroupLicense[].class);
      importData("migrationTracker.json", daoFactory.createMigrationTrackerDAO(), MigrationTracker[].class);
      importPolicies("policy.json", daoFactory.createPolicyDAO());
      importData("policyMonitoring.json", daoFactory.createPolicyMonitoringDAO(), PolicyMonitoring[].class);
      importData("policyTag.json", daoFactory.createPolicyTagDAO(), PolicyTag[].class);
      importData("proprietaryConfig.json", daoFactory.createProprietaryConfigDAO(), ProprietaryConfig[].class);
      importData("repositoryManager.json", daoFactory.createRepositoryManagerDAO(), RepositoryManager[].class);
      importData("repository.json", daoFactory.createRepositoryDAO(), Repository[].class);
      importData("securityVulnerabilityOverride.json", daoFactory.createSecurityVulnerabilityOverrideDAO(),
          SecurityVulnerabilityOverride[].class);
      importData("sourceControl.json", daoFactory.createSourceControlDAO(), SourceControl[].class);
      importData("systemConfiguration.json", daoFactory.createSystemConfigurationPropertyDAO(),
          SystemConfigurationProperty[].class);
      importData("user.json", daoFactory.createUserDAO(), User[].class);
      importData("webhook.json", daoFactory.createWebhookDAO(), Webhook[].class);
    }

    private <T extends HasStringId> void importData(
        String jsonFilename,
        AbstractOperationalSqlDAO<T> dao,
        Class<T[]> clazz) throws IOException
    {
      String jsonString = FileUtils.readFileToString(new File(dataDumpDir, jsonFilename), StandardCharsets.UTF_8);
      jsonString = jsonString.substring(jsonString.indexOf(":") + 1, jsonString.length() - 1);
      T[] entities = JsonUtils.parse(jsonString, clazz);
      if (entities.length > 0 && entities[0] instanceof Organization) {
        entities = (T[]) sortOrganizations((Organization[]) entities);
      }
      for (T entity : entities) {
        try {
          if (dao.getById(entity.getId()) == null) {
            dao.insert(entity);
          }
          else {
            try {
              dao.update(entity);
            }
            catch (UnsupportedOperationException e) {
              // Some DAOs do not support updates
              log.warn(dao.getClass().getSimpleName() + " does not support updates.");
            }
          }
        }
        catch (RuntimeException e) {
          log.error(e.getMessage(), e);
        }
      }
    }

    private static class OrgWithChildren
    {
      Organization org;

      List<OrgWithChildren> children = new ArrayList<>();
    }

    private Organization[] sortOrganizations(Organization[] orgs) {
      Map<String, OrgWithChildren> orgsWithChildrenById = new HashMap<>();

      OrgWithChildren rootOrgWithChildren = null;
      for (Organization org : orgs) {
        OrgWithChildren orgWithChildren = orgsWithChildrenById.computeIfAbsent(org.getId(), k -> new OrgWithChildren());
        orgWithChildren.org = org;

        if (org.getParentOrganizationId() != null) {
          OrgWithChildren parentOrgWithChildren =
              orgsWithChildrenById.computeIfAbsent(org.getParentOrganizationId(), k -> new OrgWithChildren());
          parentOrgWithChildren.children.add(orgWithChildren);
        }
        else {
          rootOrgWithChildren = orgWithChildren;
        }
      }

      List<Organization> sorted = new ArrayList<>();
      sortOrgs(rootOrgWithChildren, sorted);
      return sorted.toArray(Organization[]::new);
    }

    private void sortOrgs(OrgWithChildren orgWithChildren, List<Organization> result) {
      result.add(orgWithChildren.org);
      orgWithChildren.children.forEach(o -> sortOrgs(o, result));
    }

    private void importPolicies(String jsonFilename, PolicyDAO dao) throws IOException {
      String jsonString = FileUtils.readFileToString(new File(dataDumpDir, jsonFilename), StandardCharsets.UTF_8);
      jsonString = jsonString.substring(jsonString.indexOf(":") + 1, jsonString.length() - 1);
      Policy[] entities = JsonUtils.parse(jsonString, Policy[].class);
      for (Policy entity : entities) {
        try {
          if (dao.getById(entity.getId()) == null) {
            dao.insert(entity);
          }
          else {
            dao.update(entity);
          }
        }
        catch (RuntimeException e) {
          log.error(e.getMessage());
        }
      }
    }

    private File getDataDumpDir() throws IOException {
      System.out.println("Support zip data dump files directory:");
      BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
      return new File(reader.readLine());
    }

    @Override
    public DatabaseContainer createDatabaseContainer(final InsightConfig insightConfig) {
      return new DefaultDatabaseContainer(insightConfig);
    }
  }
}
