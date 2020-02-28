/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.tools.datagenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.sonatype.clm.dto.model.component.ComponentIdentifier;
import com.sonatype.insight.brain.dataaccess.TemporaryEntity;
import com.sonatype.insight.brain.db.OperationalDataStoreProvider;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.PolicyEvaluation;
import com.sonatype.insight.db.DatabaseConfig;

/**
 * Quick and dirty code to generate ODS databases.
 */
public class GenerateOdsDb
{
  private static TemporaryEntity tempEntity;

  public static void main(String[] args) {
    int orgCount = 1000;
    int appCount = 10;
    int evalCount = 300;
    int appPVCount = 5;
    int appComponentCount = 200;
    // int repoMgrCount = 1;
    // int repoCount = 5;
    // int repoComponentCount = 1500;
    // int repoPVCount = 5;

    File databaseDir = new File("c:/temp");

    DatabaseConfig odsDatabaseConfig = new DatabaseConfig();
    odsDatabaseConfig.setDriverClassName("org.h2.Driver");
    odsDatabaseConfig.setUrl("jdbc:h2:" + databaseDir.getAbsolutePath() + "/ods"
        + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000;MV_STORE=FALSE");
    odsDatabaseConfig.setUsername("sa");
    odsDatabaseConfig.setPassword("");
    odsDatabaseConfig.setMaxConnections(50);
    OperationalDataStoreProvider.init(odsDatabaseConfig, true);

    tempEntity = new TemporaryEntity();
    tempEntity.before();

    Policy policy = tempEntity.newPolicy(Organization.ROOT_ORGANIZATION_ID);

    long start = System.currentTimeMillis();

    List<String> orgIds = generateOrgs(orgCount);
    List<String> appIds = generateApps(orgIds, appCount);
    generateAppPolicyData(appIds, policy, evalCount, appPVCount, appComponentCount);

    System.out.println("Generated data in " + (System.currentTimeMillis() - start) + "ms");
  }

  private static void generateAppPolicyData(
      List<String> appIds,
      Policy policy,
      int evalCount,
      int appPVCount,
      int appComponentCount)
  {
    int iApp = 0;
    for (String appId : appIds) {
      iApp++;
      long start = System.currentTimeMillis();

      for (int iEval = 0; iEval < evalCount; iEval++) {
        PolicyEvaluation eval = tempEntity.newPolicyEvaluation(appId, "Build", randomString());
        for (int iPV = 0; iPV < appPVCount; iPV++) {
          tempEntity.newPolicyViolation(eval, policy);
        }
      }

      for (int iComp = 0; iComp < appComponentCount; iComp++) {
        tempEntity.newApplicationComponent(appId, "Build", randomString().substring(0, 20),
            ComponentIdentifier.createMavenCoordinates(randomString(), randomString(), "1.0.0", "", "jar"));
      }
      System.out
          .println("***************** generated app " + iApp + " in " + (System.currentTimeMillis() - start) + "ms");
    }
  }

  private static List<String> generateApps(List<String> orgIds, int count) {
    List<String> result = new ArrayList<>();
    for (String orgId : orgIds) {
      for (int i = 0; i < count; i++) {
        Application app = tempEntity.newApplication(orgId);
        result.add(app.getId());
      }
    }
    return result;
  }

  private static List<String> generateOrgs(int count) {
    List<String> result = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      Organization org = tempEntity.newOrganization();
      result.add(org.getId());
    }
    return result;
  }

  private static String randomString() {
    return UUID.randomUUID().toString().replace("-", "");
  }
}
