/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.policy.actions.FailActionType;
import com.sonatype.insight.brain.model.policy.stages.ProxyStageType;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.brain.testing.AbstractBrainServiceIntegrationTest;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This class tests that the actual, current reference policy document can be imported successfully into IQ.
 * It is similar to parts of NewInstancePopulatorTest, but that test uses a canned policy as test data, and in doing
 * so, is able to verify the import contents to a greater degree of detail. With this class on the other hand, we don't
 * want to be updating it every time the reference policy gets updated, so we only test things that don't require
 * hard-coded assumptions about the policy file contents.  Mainly, that the number of entities in the JSON matches the
 * number of entities in the database tables after the import.
 */
public class ReferencePolicyImportIntegrationTest
    extends AbstractBrainServiceIntegrationTest
{
  private PolicyDAO policyDAO;

  private LabelDAO labelDAO;

  private LicenseThreatGroupDAO licenseThreatGroupDAO;

  private LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO;

  private TagDAO tagDAO;

  private PolicyTagDAO policyTagDAO;

  private final URL referencePolicyUrl = getClass()
      .getResource("/reference-policies-v" + ReferencePolicyFetcher.REFERENCE_POLICY_VERSION + ".json");

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

  /*
   * Every now and then, when hds version is bumped, this test will fail. An example for what must be done can be found
   * in: https://github.com/sonatype/insight-brain/pull/8781
   * To automate this we added ReferenceLicenseUpdater. Run the main method within that class and it will refresh the
   * required sql files. Run the test again with the refreshed files and the test should be passing. Do not forget to
   * commit and push the new sql files generated.
   *
   * There will most likely be applitools differences. Accept the ones that are caused by the added licenses.
   */
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

    Configurator configurator = config -> config.setImportReferencePoliciesFromHDS(true);

    hdsRespondWith(referencePolicyUrl).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);

    startIqTestServer(configurator);

    PolicyExportResult importData = JsonUtils.parse(referencePolicyUrl.openStream(), PolicyExportResult.class);
    int policyCount = importData.policies.size();
    int ltgCount = importData.licenseThreatGroups.size();
    int ltgLicenseCount = importData.licenseThreatGroupLicenses.size();
    int labelCount = importData.labels.size();
    int tagCount = importData.tags.size();
    int policyTagCount = importData.policyTags.size();

    assertThat(policyCount).isGreaterThan(0);
    assertThat(ltgCount).isGreaterThan(0);
    assertThat(ltgLicenseCount).isGreaterThan(0);
    assertThat(labelCount).isGreaterThan(0);
    assertThat(tagCount).isGreaterThan(0);
    assertThat(policyTagCount).isGreaterThan(0);

    assertThat(policyDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(policyCount);
    assertThat(licenseThreatGroupDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(ltgCount);
    assertThat(licenseThreatGroupLicenseDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(ltgLicenseCount);
    assertThat(labelDAO.getByOwnerId(Organization.ROOT_ORGANIZATION_ID)).hasSize(labelCount);
    assertThat(tagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).hasSize(tagCount);
    assertThat(policyTagDAO.getByOrganizationId(Organization.ROOT_ORGANIZATION_ID)).hasSize(policyTagCount);

    Policy integrityRatingPolicy = policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Integrity-Rating");
    assertThat(integrityRatingPolicy.getActions()).containsEntry(ProxyStageType.ID, FailActionType.ID);
    Policy namespaceConflictRatingPolicy =
        policyDAO.getByOwnerIdAndName(Organization.ROOT_ORGANIZATION_ID, "Security-Namespace Conflict");
    assertThat(namespaceConflictRatingPolicy.getActions()).containsEntry(ProxyStageType.ID, FailActionType.ID);
  }
}
