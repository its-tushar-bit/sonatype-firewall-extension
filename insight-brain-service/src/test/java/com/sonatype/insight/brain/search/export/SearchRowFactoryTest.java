/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
package com.sonatype.insight.brain.search.export;

import java.util.List;

import com.sonatype.insight.brain.search.docs.DocumentBuilder.ItemType;
import com.sonatype.insight.brain.search.results.SearchResultItemDTO;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class SearchRowFactoryTest
{
  private static final String baseUrl = "https://test.base.url/";

  private static final SearchResultItemDTO searchResultItemDTO = new SearchResultItemDTO();

  private static final SearchRowFactory lifecycleSearchRowFactory = new LifecycleSearchRowFactory();

  private static final SearchRowFactory sbomSearchRowFactory = new SbomSearchRowFactory();

  @Before
  public void before() {
    searchResultItemDTO.organizationId = "testOrganizationId";
    searchResultItemDTO.organizationName = "testOrganizationName";
    searchResultItemDTO.applicationPublicId = "testApplicationPublicId";
    searchResultItemDTO.applicationName = "testApplicationName";
    searchResultItemDTO.applicationVersion = "testApplicationVersion";
    searchResultItemDTO.policyEvaluationStage = "testPolicyEvaluationStage";
    searchResultItemDTO.reportId = "testReportId";
    searchResultItemDTO.componentName = "testComponentName";
    searchResultItemDTO.vulnerabilityId = "testVulnerabilityId";
    searchResultItemDTO.applicationCategoryId = "testApplicationCategoryId";
    searchResultItemDTO.applicationCategoryName = "testApplicationCategoryName";
    searchResultItemDTO.componentLabelId = "testComponentLabelId";
    searchResultItemDTO.componentLabelName = "testComponentLabelName";
    searchResultItemDTO.policyId = "testPolicyId";
    searchResultItemDTO.policyName = "testPolicyName";
    searchResultItemDTO.policyThreatCategory = "testPolicyThreatCategory";
    searchResultItemDTO.policyThreatLevel = 999;
    searchResultItemDTO.sbomSpecification = "testSbomSpecification";
  }

  @Test
  public void testSearchRowFactory_create_normalMode_Organization() {
    searchResultItemDTO.itemType = ItemType.ORGANIZATION.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);

    checkColumnsAreEmpty(row, 3, 4, 5, 6 , 7, 8, 9, 10, 11, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_Application() {
    searchResultItemDTO.itemType = ItemType.APPLICATION.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    checkColumnsAreEmpty(row, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_ApplicationCategory() {
    searchResultItemDTO.itemType = ItemType.APPLICATION_CATEGORY.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(5)).isEqualTo(searchResultItemDTO.applicationCategoryName);
    assertThat(row.get(6)).contains(baseUrl);
    assertThat(row.get(6)).contains(searchResultItemDTO.applicationCategoryId);

    checkColumnsAreEmpty(row, 3, 4 , 7, 8, 9, 10, 11, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_ComponentLabel_withOrganization() {
    searchResultItemDTO.itemType = ItemType.COMPONENT_LABEL.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(7)).isEqualTo(searchResultItemDTO.componentLabelName);
    assertThat(row.get(8)).contains(baseUrl);
    assertThat(row.get(8)).contains(searchResultItemDTO.componentLabelId);

    checkColumnsAreEmpty(row, 3, 4, 5, 6, 9, 10, 11, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_ComponentLabel_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.COMPONENT_LABEL.name();
    searchResultItemDTO.organizationId = null;

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(7)).isEqualTo(searchResultItemDTO.componentLabelName);
    assertThat(row.get(8)).contains(baseUrl);
    assertThat(row.get(8)).contains(searchResultItemDTO.componentLabelId);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 9, 10, 11, 12, 13 ,14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_Policy_withOrganization() {
    searchResultItemDTO.itemType = ItemType.POLICY.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(9)).isEqualTo(searchResultItemDTO.policyName);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.policyThreatLevel.toString());
    assertThat(row.get(11)).contains(baseUrl);
    assertThat(row.get(11)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(11)).contains(searchResultItemDTO.policyId);

    checkColumnsAreEmpty(row, 3, 4, 5, 6, 7, 8, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_Policy_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.POLICY.name();
    searchResultItemDTO.organizationId = null;

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(9)).isEqualTo(searchResultItemDTO.policyName);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.policyThreatLevel.toString());
    assertThat(row.get(11)).contains(baseUrl);
    assertThat(row.get(11)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(11)).contains(searchResultItemDTO.policyId);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 7, 8, 12, 13, 14, 15);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_SecurityVulnerability_withOrganization() {
    searchResultItemDTO.itemType = ItemType.SECURITY_VULNERABILITY.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).contains(baseUrl);
    assertThat(row.get(13)).contains(searchResultItemDTO.reportId);
    assertThat(row.get(13)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(14)).contains(baseUrl);
    assertThat(row.get(14)).contains(searchResultItemDTO.vulnerabilityId);
    assertThat(row.get(15)).isEqualTo(searchResultItemDTO.policyEvaluationStage);

    checkColumnsAreEmpty(row,5, 6 , 7, 8, 9, 10, 11);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_SecurityVulnerability_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.SECURITY_VULNERABILITY.name();
    searchResultItemDTO.organizationName = null;

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).contains(baseUrl);
    assertThat(row.get(13)).contains(searchResultItemDTO.reportId);
    assertThat(row.get(13)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(14)).contains(baseUrl);
    assertThat(row.get(14)).contains(searchResultItemDTO.vulnerabilityId);
    assertThat(row.get(15)).isEqualTo(searchResultItemDTO.policyEvaluationStage);

    checkColumnsAreEmpty(row,1, 2, 5, 6, 7, 8, 9, 10, 11);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_NonVulnerableComponent_withOrganization() {
    searchResultItemDTO.itemType = ItemType.NON_VULNERABLE_COMPONENT.name();

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).contains(baseUrl);
    assertThat(row.get(13)).contains(searchResultItemDTO.reportId);
    assertThat(row.get(13)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(15)).isEqualTo(searchResultItemDTO.policyEvaluationStage);

    checkColumnsAreEmpty(row, 5, 6 , 7, 8, 9, 10, 11, 14);
  }

  @Test
  public void testSearchRowFactory_create_normalMode_NonVulnerableComponent_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.NON_VULNERABLE_COMPONENT.name();
    searchResultItemDTO.organizationName = null;

    List<String> row = lifecycleSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(16);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).contains(baseUrl);
    assertThat(row.get(13)).contains(searchResultItemDTO.reportId);
    assertThat(row.get(13)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(15)).isEqualTo(searchResultItemDTO.policyEvaluationStage);

    checkColumnsAreEmpty(row,  1, 2, 5, 6 , 7, 8, 9, 10, 11, 14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_Organization() {
    searchResultItemDTO.itemType = ItemType.ORGANIZATION.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);

    checkColumnsAreEmpty(row,  3, 4, 5, 6 ,7 , 8, 9, 10, 11, 12, 13 ,14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_Application() {
    searchResultItemDTO.itemType = ItemType.APPLICATION.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);

    checkColumnsAreEmpty(row,  5, 6 , 7, 8, 9, 10, 11, 12, 13, 14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_ApplicationCategory() {
    searchResultItemDTO.itemType = ItemType.APPLICATION_CATEGORY.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    // Explicitly set to empty strings for now, will be updated
    assertThat(row.get(5)).isEqualTo("");
    assertThat(row.get(6)).isEqualTo("");

    checkColumnsAreEmpty(row, 3, 4, 7, 8, 9, 10, 11, 12, 13, 14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_ComponentLabel_withOrganization() {
    searchResultItemDTO.itemType = ItemType.COMPONENT_LABEL.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);

    checkColumnsAreEmpty(row, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13 ,14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_ComponentLabel_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.COMPONENT_LABEL.name();
    searchResultItemDTO.organizationId = null;

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12, 13 ,14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_Policy_withOrganization() {
    searchResultItemDTO.itemType = ItemType.POLICY.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    // Explicitly set to empty strings for now, will be updated
    assertThat(row.get(7)).isEqualTo("");
    assertThat(row.get(8)).isEqualTo("");
    assertThat(row.get(9)).isEqualTo("");

    checkColumnsAreEmpty(row, 3, 4, 5, 6, 10, 11, 12, 13 ,14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_Policy_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.POLICY.name();
    searchResultItemDTO.organizationId = null;

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    // Explicitly set to empty strings for now, will be updated
    assertThat(row.get(7)).isEqualTo("");
    assertThat(row.get(8)).isEqualTo("");
    assertThat(row.get(9)).isEqualTo("");
    checkColumnsAreEmpty(row, 1, 2, 5, 6, 10, 11, 12, 13, 14);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_SecurityVulnerability_withOrganization() {
    searchResultItemDTO.itemType = ItemType.SECURITY_VULNERABILITY.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.vulnerabilityId);
    assertThat(row.get(13)).isEqualTo(searchResultItemDTO.applicationVersion);
    assertThat(row.get(14)).isEqualTo(searchResultItemDTO.sbomSpecification);

    checkColumnsAreEmpty(row, 5, 6, 7, 8, 9, 11);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_SecurityVulnerability_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.SECURITY_VULNERABILITY.name();
    searchResultItemDTO.organizationName = null;

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(12)).isEqualTo(searchResultItemDTO.vulnerabilityId);
    assertThat(row.get(13)).isEqualTo(searchResultItemDTO.applicationVersion);
    assertThat(row.get(14)).isEqualTo(searchResultItemDTO.sbomSpecification);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 7, 8, 9, 11);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_NonVulnerableComponent_withOrganization() {
    searchResultItemDTO.itemType = ItemType.NON_VULNERABLE_COMPONENT.name();

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(1)).isEqualTo(searchResultItemDTO.organizationName);
    assertThat(row.get(2)).contains(baseUrl);
    assertThat(row.get(2)).contains(searchResultItemDTO.organizationId);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).isEqualTo(searchResultItemDTO.applicationVersion);
    assertThat(row.get(14)).isEqualTo(searchResultItemDTO.sbomSpecification);

    checkColumnsAreEmpty(row, 7, 8, 9, 11, 12);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_NonVulnerableComponent_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.NON_VULNERABLE_COMPONENT.name();
    searchResultItemDTO.organizationName = null;

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(10)).isEqualTo(searchResultItemDTO.componentName);
    assertThat(row.get(13)).isEqualTo(searchResultItemDTO.applicationVersion);
    assertThat(row.get(14)).isEqualTo(searchResultItemDTO.sbomSpecification);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 7, 8, 9, 11, 12);
  }

  @Test
  public void testSearchRowFactory_create_sbomManagerMode_SbomMetadata_withOutOrganization() {
    searchResultItemDTO.itemType = ItemType.SBOM_METADATA.name();
    searchResultItemDTO.organizationName = null;

    List<String> row = sbomSearchRowFactory.create(searchResultItemDTO, baseUrl);

    assertThat(row).hasSize(15);
    assertThat(row.get(0)).isEqualTo(searchResultItemDTO.itemType);
    assertThat(row.get(3)).isEqualTo(searchResultItemDTO.applicationName);
    assertThat(row.get(4)).contains(baseUrl);
    assertThat(row.get(4)).contains(searchResultItemDTO.applicationPublicId);
    assertThat(row.get(13)).isEqualTo(searchResultItemDTO.applicationVersion);
    assertThat(row.get(14)).isEqualTo(searchResultItemDTO.sbomSpecification);

    checkColumnsAreEmpty(row, 1, 2, 5, 6, 7, 8, 9, 10, 11, 12);
  }

  private void checkColumnsAreEmpty(List<String> row, int... columnIndexes) {
    for (int idx: columnIndexes) {
      assertThat(row.get(idx)).isEmpty();
    }
  }
}
