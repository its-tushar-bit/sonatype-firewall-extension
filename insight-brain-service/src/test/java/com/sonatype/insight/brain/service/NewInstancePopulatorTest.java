/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.service;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sonatype.insight.brain.dataaccess.ApplicationDAO;
import com.sonatype.insight.brain.dataaccess.OrganizationDAO;
import com.sonatype.insight.brain.dataaccess.label.LabelDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupDAO;
import com.sonatype.insight.brain.dataaccess.license.LicenseThreatGroupLicenseDAO;
import com.sonatype.insight.brain.dataaccess.policy.PolicyDAO;
import com.sonatype.insight.brain.dataaccess.tag.PolicyTagDAO;
import com.sonatype.insight.brain.dataaccess.tag.TagDAO;
import com.sonatype.insight.brain.hds.ReferencePolicyFetcher;
import com.sonatype.insight.brain.model.Application;
import com.sonatype.insight.brain.model.Organization;
import com.sonatype.insight.brain.model.label.Label;
import com.sonatype.insight.brain.model.license.LicenseThreatGroup;
import com.sonatype.insight.brain.model.license.LicenseThreatGroupLicense;
import com.sonatype.insight.brain.model.policy.Policy;
import com.sonatype.insight.brain.model.tag.PolicyTag;
import com.sonatype.insight.brain.model.tag.Tag;
import com.sonatype.insight.brain.organization.SampleDataCreator;
import com.sonatype.insight.brain.service.TestInsightBrainService.Configurator;

import org.junit.After;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class NewInstancePopulatorTest
    extends AbstractBrainServiceTest
{
  private final ApplicationDAO applicationDAO = new ApplicationDAO();

  private final OrganizationDAO organizationDAO = new OrganizationDAO();

  private final PolicyDAO policyDAO = new PolicyDAO();

  private final LabelDAO labelDAO = new LabelDAO();

  private final LicenseThreatGroupDAO licenseThreatGroupDAO = new LicenseThreatGroupDAO();

  private final LicenseThreatGroupLicenseDAO licenseThreatGroupLicenseDAO = new LicenseThreatGroupLicenseDAO();

  private final TagDAO tagDAO = new TagDAO();

  private final PolicyTagDAO policyTagDAO = new PolicyTagDAO();

  @After
  public void cleanup() {
    Application app = applicationDAO.getByName(SampleDataCreator.SAMPLE_APPLICATION_NAME);
    Organization org = organizationDAO.getByName(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
    Collection<Policy> policies = policyDAO.getAll();
    Collection<Label> labels = labelDAO.getAll();
    Collection<LicenseThreatGroup> ltgs = licenseThreatGroupDAO.getAll();
    Collection<Tag> tags = tagDAO.getAll();

    if (app != null) {
      applicationDAO.delete(app);
    }
    if (org != null) {
      organizationDAO.delete(org);
    }

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
  public void testPopulateIfNewInstance_NoOrgsOrPolicies_SampleDataEnabled_CreatesSampleData()
      throws Exception
  {
    initServer(true, false);
    assertSampleDataCreated(true);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_NoOrgsOrPolicies_SampleDataDisabled_SampleDataNotCreated() throws Exception {
    initServer(false, false);
    assertSampleDataCreated(false);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_ExistingPolicy_SampleDataEnabled_SampleDataNotCreated() throws Exception {
    tempEntity.newPolicy();

    initServer(true, false);
    assertSampleDataCreated(false);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_ExistingOrg_SampleDataEnabled_SampleDataNotCreated() throws Exception {
    tempEntity.newOrganization();

    initServer(true, false);
    assertSampleDataCreated(false);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_NoOrgsOrPolicies_PolicyImportEnabled_ImportsReferencePolicies()
      throws Exception
  {
    initServer(false, true);
    assertReferencePoliciesImported(true);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_NoOrgsOrPolicies_PolicyImportDisabled_ReferencePoliciesNotImported()
      throws Exception
  {
    initServer(false, false);
    assertReferencePoliciesImported(false);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_ExistingPolicy_PolicyImportEnabled_ReferencePoliciesNotImported()
      throws Exception
  {
    tempEntity.newPolicy();

    initServer(false, true);
    assertReferencePoliciesImported(false);
  }

  @Test
  @ManualServerInit
  public void testPopulateIfNewInstance_ExistingOrg_PolicyImportEnabled_ReferencePoliciesNotImported()
      throws Exception
  {
    tempEntity.newOrganization();

    initServer(false, true);
    assertReferencePoliciesImported(false);
  }

  private void initServer(boolean createSampleData, boolean importReferencePoliciesFromHDS) throws Exception {
    Configurator configurator = new Configurator()
    {
      @Override
      public void configure(InsightConfig config) {
        config.setCreateSampleData(createSampleData);
        config.setImportRefrencePoliciesFromHDS(importReferencePoliciesFromHDS);
      }
    };

    hdsRespondWith(getClass().getResource("/NewInstancePopulatorTest/referencePolicies.json"))
        .atUri(ReferencePolicyFetcher.REFERENCE_POLICY_PATH);
    hdsRespondWith(getClass().getResource("/NewInstancePopulatorTest/licenses.json")).atUri("rest/license");

    initServer(configurator);
  }

  private void assertSampleDataCreated(boolean shouldHaveBeenCreated) {
    List<Organization> organizations = organizationDAO.getAll();
    Set<String> organizationNames = getUniqueStrings(organizations, Organization::getName);
    List<Application> applications = applicationDAO.getAll();

    if (shouldHaveBeenCreated) {
      assertThat(organizationNames).contains(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);

      assertThat(applications).hasSize(1);
      Application sampleApplication = applications.get(0);
      assertThat(sampleApplication.getName()).isEqualTo(SampleDataCreator.SAMPLE_APPLICATION_NAME);
      assertThat(sampleApplication.getParentOwnerId()).isEqualTo(organizations.get(1).getId());
      assertThat(sampleApplication.getPublicId()).isEqualTo(SampleDataCreator.SAMPLE_APPLICATION_PUBLIC_ID);
    }
    else {
      assertThat(organizationNames).doesNotContain(SampleDataCreator.SAMPLE_ORGANIZATION_NAME);
      assertThat(applications).isEmpty();
    }
  }

  private void assertReferencePoliciesImported(boolean shouldHaveBeenImported) {
    // NOTE: ids from the JSON are overwritten during import
    Set<String> policyNames = getUniqueStrings(policyDAO.getAll(), Policy::getName);
    Set<String> labels = getUniqueStrings(labelDAO.getAll(), Label::getLabel);
    Set<String> ltgNames = getUniqueStrings(licenseThreatGroupDAO.getAll(), LicenseThreatGroup::getName);
    Set<String> tagNames = getUniqueStrings(tagDAO.getAll(), Tag::getName);

    if (shouldHaveBeenImported) {
      assertThat(policyNames).containsExactlyInAnyOrder("policy1", "policy2");

      assertThat(labels).containsExactlyInAnyOrder("label1", "label2", "label3");

      assertThat(ltgNames).containsExactlyInAnyOrder("ltg1", "ltg2");

      assertThat(tagNames).containsExactlyInAnyOrder("tag1", "tag2");

      String ltg1Name = licenseThreatGroupDAO.getByName("ltg1").get(0).getId();
      String ltg2Name = licenseThreatGroupDAO.getByName("ltg2").get(0).getId();

      Set<String> licenseIdsForLtg1 = getUniqueStrings(licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(ltg1Name),
          LicenseThreatGroupLicense::getLicenseId);
      Set<String> licenseIdsForLtg2 = getUniqueStrings(licenseThreatGroupLicenseDAO.getByLicenseThreatGroupId(ltg2Name),
          LicenseThreatGroupLicense::getLicenseId);

      assertThat(licenseIdsForLtg1).containsExactlyInAnyOrder("Apache-2.0", "GPL-3.0");

      assertThat(licenseIdsForLtg2).containsExactlyInAnyOrder("MIT");

      String policy1Id = policyDAO.getByName("policy1").get(0).getId();
      String policy2Id = policyDAO.getByName("policy2").get(0).getId();
      String tag1Id = tagDAO.getByName("tag1").get(0).getId();
      String tag2Id = tagDAO.getByName("tag2").get(0).getId();

      Set<String> tagsForPolicy1 = getUniqueStrings(policyTagDAO.getByPolicyId(policy1Id), PolicyTag::getTagId);
      Set<String> tagsForPolicy2 = getUniqueStrings(policyTagDAO.getByPolicyId(policy2Id), PolicyTag::getTagId);

      assertThat(tagsForPolicy1).containsExactlyInAnyOrder(tag1Id, tag2Id);

      assertThat(tagsForPolicy2).isEmpty();
    }
    else {
      // might not be empty because some tests add their own pre-existing policy
      assertThat(policyNames).doesNotContain("policy1");
      assertThat(labels).isEmpty();
      assertThat(ltgNames).isEmpty();
      assertThat(tagNames).isEmpty();
    }
  }

  private <T> Set<String> getUniqueStrings(Collection<T> items, Function<T, String> mapper) {
    return items.stream().map(mapper).collect(Collectors.toSet());
  }
}
