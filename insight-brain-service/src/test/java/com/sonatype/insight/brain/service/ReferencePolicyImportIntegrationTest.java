/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.net.URL;
import java.util.Collection;

import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.policy.PolicyExportResult;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;
import com.sonatype.insight.json.store.JsonUtils;

import org.junit.After;
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
    extends AbstractBrainServiceTest
{
  private final PolicyDAO policyDAO = new PolicyDAO();

  private final LabelDAO labelDAO = new LabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  private final URL referencePolicyUrl = getClass()
      .getResource("/reference-policies-v" + ReferencePolicyFetcher.REFERENCE_POLICY_VERSION + ".json");

  @Before
  @After
  public void cleanup() {
    Collection<Policy> policies = policyDAO.getAll();
    Collection<Label> labels = labelDAO.getAll();
    Collection<LicenseThreatGroup> ltgs = licenseThreatGroupDAO.getAll();
    Collection<Tag> tags = tagDAO.getAll();

    for (Policy policy : policies) {
      policyDAO.delete(policy);
    }

    for (Label label : labels) {
      labelDAO.delete(label);
    }

    for (LicenseThreatGroup ltg : ltgs) {
      licenseThreatGroupDAO.delete(ltg);
    }

    for (Tag tag : tags) {
      tagDAO.delete(tag);
    }

    // LicenseThreatGroupLicenses and PolicyTags get deleted with the LicenseThreatGroups and Policies
  }

  @Override
  public void setUpTestLicenseThreatGroups() {
    // noop - do not set up test LTGs for this test
  }

  @Test
  @ManualServerInit
  public void testImportCurrentReferencePolicies() throws Exception {
    Configurator configurator = config -> config.setImportRefrencePoliciesFromHDS(true);

    hdsRespondWith(referencePolicyUrl).atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);

    initServer(configurator);

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

    assertThat(policyDAO.getAll()).hasSize(policyCount);
    assertThat(licenseThreatGroupDAO.getAll()).hasSize(ltgCount);
    assertThat(licenseThreatGroupLicenseDAO.getAll()).hasSize(ltgLicenseCount);
    assertThat(labelDAO.getAll()).hasSize(labelCount);
    assertThat(tagDAO.getAll()).hasSize(tagCount);
    assertThat(policyTagDAO.getAll()).hasSize(policyTagCount);
  }
}
