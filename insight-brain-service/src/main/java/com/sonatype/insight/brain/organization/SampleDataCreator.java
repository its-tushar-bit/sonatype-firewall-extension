/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.organization;

import java.util.List;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.service.InsightConfig;
import com.sonatype.insight.dataaccess.TransactionContext;

/**
 * @since 1.43
 */
public class SampleDataCreator
{
  public static final String SAMPLE_ORGANIZATION_NAME = "Sandbox Organization";

  public static final String SAMPLE_APPLICATION_NAME = "Sandbox Application";

  public static final String SAMPLE_APPLICATION_PUBLIC_ID = "sandbox-application";

  public static void createSampleData(InsightConfig insightConfig) {
    if (!insightConfig.isCreateSampleData()) {
      return;
    }
    ApplicationDAO applicationDAO = new ApplicationDAO();
    OrganizationDAO organizationDAO = new OrganizationDAO();
    List<Organization> orgs = organizationDAO.getAll();

    // create sample data only for new installs
    if (orgs.size() == 1 && orgs.get(0).getId().equalsIgnoreCase(Organization.ROOT_ORGANIZATION_ID)) {
      try (TransactionContext tx = organizationDAO.createTransactionContext()) {
        tx.begin();
        Organization sampleOrganization = new Organization(SAMPLE_ORGANIZATION_NAME);
        organizationDAO.insert(tx, sampleOrganization);
        Application sampleApplication = new Application(SAMPLE_APPLICATION_PUBLIC_ID, SAMPLE_APPLICATION_NAME,
            sampleOrganization.getId());
        applicationDAO.insert(tx, sampleApplication);
        tx.commit();
      }
    }
  }
}
