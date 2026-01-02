/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.DefaultLicenseDataUpdater;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.experimental.categories.Category;
import com.sonatype.insight.brain.common.test.SlowTest;

/**
 * This class tests that the actual, current reference policy document can be imported successfully into IQ.
 * It is similar to parts of NewInstancePopulatorTest, but that test uses a canned policy as test data, and in doing
 * so, is able to verify the import contents to a greater degree of detail. With this class on the other hand, we don't
 * want to be updating it every time the reference policy gets updated, so we only test things that don't require
 * hard-coded assumptions about the policy file contents.  Mainly, that the number of entities in the JSON matches the
 * number of entities in the database tables after the import.
 */
@Category(SlowTest.class)
public class ReferencePolicyImportIntegrationTest
    extends AbstractBrainServiceIntegrationTest
{
  private PolicyDAO policyDAO;

  private LabelDAO labelDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private TagDAO tagDAO;

  private PolicyTagDAO policyTagDAO;

  @Before
  public void cleanup() {
    // Using DAOFactory instead of lookup as we have tests using @ManualIqServerInit annotation
    policyDAO = daoFactory.createPolicyDAO();
    labelDAO = daoFactory.createLabelDAO();
    licenseThreatGroupDAO = daoFactory.createLicenseThreatGroupDAO();
    licenseThreatGroupLicenseDAO = daoFactory.createLicenseThreatGroupLicenseDAO();
    tagDAO = daoFactory.createTagDAO();
    policyTagDAO = daoFactory.createPolicyTagDAO();
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // noop - do not set up test LTGs for this test
  }

  @Test
  @ManualIqServerInit
  public void testImportCurrentReferencePolicies() throws Exception {
    // Sanity checks
    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
    assertThat(licenseThreatGroupDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
    assertThat(labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
    assertThat(tagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();
    assertThat(policyTagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).isEmpty();

    try (CloseableHttpClient httpClient = HttpClientBuilder.create().build()) {
      HttpGet policyRequest = new HttpGet("https://clm-staging.sonatype.com/"
          + ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
      String policyResponse = EntityUtils.toString(httpClient.execute(policyRequest).getEntity());
      PolicyExportResult policyExportResult = JsonUtils.parse(policyResponse, PolicyExportResult.class);
      hdsRespondWith(policyExportResult).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);

      HttpGet licenseRequest = new HttpGet("https://clm-staging.sonatype.com/" +
          DefaultLicenseDataUpdater.HDS_LICENSE_PATH);
      String licenseResponse = EntityUtils.toString(httpClient.execute(licenseRequest).getEntity());
      hdsRespondWith(licenseResponse).atUri(DefaultLicenseDataUpdater.HDS_LICENSE_PATH);

      startIqTestServer();

      int policyCount = policyExportResult.policies.size();
      int ltgCount = policyExportResult.licenseThreatGroups.size();
      int ltgLicenseCount = policyExportResult.licenseThreatGroupLicenses.size();
      int labelCount = policyExportResult.labels.size();
      int tagCount = policyExportResult.tags.size();
      int policyTagCount = policyExportResult.policyTags.size();

      assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(policyCount);
      assertThat(licenseThreatGroupDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(ltgCount);
      assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(ltgLicenseCount);
      assertThat(labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(labelCount);
      assertThat(tagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).hasSize(tagCount);
      assertThat(policyTagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).hasSize(policyTagCount);

      Policy integrityRatingPolicy =
          policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
      assertThat(integrityRatingPolicy.getActions()).containsEntry(ProxyStageType.ID, FailActionType.ID);
      Policy namespaceConflictRatingPolicy =
          policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Namespace Conflict");
      assertThat(namespaceConflictRatingPolicy.getActions()).containsEntry(ProxyStageType.ID, FailActionType.ID);
    }
  }

  @Override
  protected void startIqTestServer() throws Exception {
    startIqTestServer(config -> config.setImportReferencePoliciesFromHDS(true));
  }
}
