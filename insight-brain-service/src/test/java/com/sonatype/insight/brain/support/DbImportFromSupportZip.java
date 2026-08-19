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
import com.sonatype.insight.brain.db.DatabaseContainer;
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
import com.sonatype.insight.brain.spring.DropwizardConfigBootstrap;
import com.sonatype.insight.brain.spring.InsightBrainSpringApplication;
import com.sonatype.insight.json.store.JsonUtils;
import com.sonatype.insight.model.HasStringId;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Utility class to import the db data dump from a support.zip file.
 * Useful when working on customer tickets (aka zendesk) that have a support.zip file with db data dump.
 *
 * <p>
 * This boots a non-web Spring context using {@link InsightBrainSpringApplication}, so the database
 * is fully initialized (including migrations) before import begins. You can run InsightBrainSpringApplication
 * after the data is imported to start debugging with the newly imported data.
 * </p>
 *
 * <p>
 * To run it provide these params:
 * </p>
 *
 * <pre>
 *   java -cp ... DbImportFromSupportZip &lt;config.yml&gt; [data-dump-dir]
 * </pre>
 *
 * <p>
 * If the data dump directory is not provided as a command-line argument, you will be prompted to enter it.
 * </p>
 *
 * <p>
 * Hint: You can use the same config.yml you use to run InsightBrainSpringApplication to import the
 * customer data into the same db.
 * </p>
 */
public class DbImportFromSupportZip
{
  private static final Logger log = LoggerFactory.getLogger(DbImportFromSupportZip.class);

  public static void main(final String[] args) {
    try {
      String configFilePath = resolveConfigFile(args);

      SpringApplicationBuilder builder = new SpringApplicationBuilder(InsightBrainSpringApplication.class)
          .web(WebApplicationType.NONE)
          .properties(
              "spring.main.web-application-type=none",
              "spring.main.register-shutdown-hook=false",
              // Enable migrations so the database schema is ready for import
              "sonatype.database.startup-migrations.enabled=true");

      DropwizardConfigBootstrap.configure(builder, configFilePath, false);

      try (ConfigurableApplicationContext context = builder.run()) {
        DatabaseContainer databaseContainer = context.getBean(DatabaseContainer.class);
        DAOFactory daoFactory = new TestDAOFactory(databaseContainer);

        ConditionTypesTestHelper.initConditionTypes(daoFactory);
        ConditionTypesTestHelper.initConditionValueTypes(daoFactory);
        SystemConfigurationPropertyFeatureTestHelper.inject(daoFactory);

        File dataDumpDir = resolveDataDumpDir(args);
        if (!dataDumpDir.exists()) {
          throw new RuntimeException("Data dump dir " + dataDumpDir.getAbsolutePath() + " does not exist.");
        }
        log.info("Using data dump dir: {}", dataDumpDir.getAbsolutePath());

        deletePolicyData(daoFactory);
        importAllData(daoFactory, dataDumpDir);

        log.info("Import complete.");
      }
    }
    catch (Throwable t) {
      // Try to log to stderr before trying the standard logging because the standard logging may not be operational at
      // this point.
      t.printStackTrace();
      log.error(t.getMessage(), t);
      System.exit(2);
    }
  }

  private static String resolveConfigFile(String[] args) {
    if (args.length >= 1 && args[0].endsWith(".yml")) {
      return args[0];
    }
    return "config.yml";
  }

  private static File resolveDataDumpDir(String[] args) throws IOException {
    // Check for data dump dir as second arg (or first if no config file)
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (!arg.endsWith(".yml") && !arg.endsWith(".yaml")) {
        return new File(arg);
      }
    }
    // Prompt interactively
    System.out.println("Support zip data dump files directory:");
    BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
    return new File(reader.readLine());
  }

  private static void deletePolicyData(DAOFactory daoFactory) {
    PolicyTagDAO policyTagDAO = daoFactory.createPolicyTagDAO();
    policyTagDAO.getAll().forEach(policyTagDAO::delete);
    TagDAO tagDAO = daoFactory.createTagDAO();
    tagDAO.getAll().forEach(tagDAO::delete);
    PolicyDAO policyDAO = daoFactory.createPolicyDAO();
    policyDAO.getAll().forEach(policyDAO::delete);
  }

  private static void importAllData(DAOFactory daoFactory, File dataDumpDir) throws IOException {
    importData(dataDumpDir, "organization.json", daoFactory.createOrganizationDAO(), Organization[].class);
    importData(dataDumpDir, "application.json", daoFactory.createApplicationDAO(), Application[].class);
    importData(dataDumpDir, "tag.json", daoFactory.createTagDAO(), Tag[].class);
    importData(dataDumpDir, "applicationTag.json", daoFactory.createApplicationTagDAO(), ApplicationTag[].class);
    importData(dataDumpDir, "role.json", daoFactory.createRoleDAO(), Role[].class);
    importData(dataDumpDir, "rolePermission.json", daoFactory.createRolePermissionDAO(), RolePermission[].class);
    importData(dataDumpDir, "membershipMapping.json", daoFactory.createMembershipMappingDAO(),
        MembershipMapping[].class);
    importData(dataDumpDir, "componentLabel.json", daoFactory.createComponentLabelDAO(), ComponentLabel[].class);
    importData(dataDumpDir, "dataRetentionPolicy.json", daoFactory.createDataRetentionPolicyDAO(),
        DataRetentionPolicy[].class);
    importData(dataDumpDir, "label.json", daoFactory.createLabelDAO(), Label[].class);
    importData(dataDumpDir, "licenseThreatGroup.json", daoFactory.createLicenseThreatGroupDAO(),
        LicenseThreatGroup[].class);
    importData(dataDumpDir, "licenseThreatGroupLicense.json", daoFactory.createLicenseThreatGroupLicenseDAO(),
        LicenseThreatGroupLicense[].class);
    importData(dataDumpDir, "migrationTracker.json", daoFactory.createMigrationTrackerDAO(), MigrationTracker[].class);
    importPolicies(dataDumpDir, "policy.json", daoFactory.createPolicyDAO());
    importData(dataDumpDir, "policyMonitoring.json", daoFactory.createPolicyMonitoringDAO(), PolicyMonitoring[].class);
    importData(dataDumpDir, "policyTag.json", daoFactory.createPolicyTagDAO(), PolicyTag[].class);
    importData(dataDumpDir, "proprietaryConfig.json", daoFactory.createProprietaryConfigDAO(),
        ProprietaryConfig[].class);
    importData(dataDumpDir, "repositoryManager.json", daoFactory.createRepositoryManagerDAO(),
        RepositoryManager[].class);
    importData(dataDumpDir, "repository.json", daoFactory.createRepositoryDAO(), Repository[].class);
    importData(dataDumpDir, "securityVulnerabilityOverride.json",
        daoFactory.createSecurityVulnerabilityOverrideDAO(), SecurityVulnerabilityOverride[].class);
    importData(dataDumpDir, "sourceControl.json", daoFactory.createSourceControlDAO(), SourceControl[].class);
    importData(dataDumpDir, "systemConfiguration.json", daoFactory.createSystemConfigurationPropertyDAO(),
        SystemConfigurationProperty[].class);
    importData(dataDumpDir, "user.json", daoFactory.createUserDAO(), User[].class);
    importData(dataDumpDir, "webhook.json", daoFactory.createWebhookDAO(), Webhook[].class);
  }

  @SuppressWarnings("unchecked")
  private static <T extends HasStringId> void importData(
      File dataDumpDir,
      String jsonFilename,
      AbstractOperationalSqlDAO<T> dao,
      Class<T[]> clazz) throws IOException
  {
    File jsonFile = new File(dataDumpDir, jsonFilename);
    if (!jsonFile.exists()) {
      log.warn("Skipping missing file: {}", jsonFilename);
      return;
    }
    String jsonString = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
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

  private static Organization[] sortOrganizations(Organization[] orgs) {
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

  private static void sortOrgs(OrgWithChildren orgWithChildren, List<Organization> result) {
    result.add(orgWithChildren.org);
    orgWithChildren.children.forEach(o -> sortOrgs(o, result));
  }

  private static void importPolicies(File dataDumpDir, String jsonFilename, PolicyDAO dao) throws IOException {
    File jsonFile = new File(dataDumpDir, jsonFilename);
    if (!jsonFile.exists()) {
      log.warn("Skipping missing file: {}", jsonFilename);
      return;
    }
    String jsonString = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
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
}
