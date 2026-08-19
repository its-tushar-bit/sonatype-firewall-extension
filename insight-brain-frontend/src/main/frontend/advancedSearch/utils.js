/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const getQueryBuilderGroups = (isSbomManager) => {
  return [
    {
      label: 'Organization',
      value: 'organization',
      show: true,
      example: 'ROOT_ORGANIZATION_ID',
      prefixList: [
        {
          value: 'organizationId',
          label: 'Organization ID',
          example: 'ROOT_ORGANIZATION_ID',
          show: true,
        },
        {
          value: 'organizationName',
          label: 'Organization Name',
          example: '"Root Organization"',
          show: true,
        },
      ],
    },
    {
      label: 'Application',
      value: 'application',
      show: true,
      example: 'MyApplicationName',
      prefixList: [
        { value: 'applicationId', label: 'Application ID', example: '22951997a36045ab8593e3b6aafb9745', show: true },
        { value: 'applicationName', label: 'Application Name', example: '"My Application Name"', show: true },
        {
          value: 'applicationPublicId',
          label: 'Application Public ID',
          example: 'MyApplicationPublicId',
          show: !isSbomManager,
        },
        { value: 'applicationVersion', label: 'Application Version', example: '1.0.0', show: isSbomManager },
        { value: 'sbomSpecification', label: 'SBOM Specification', example: 'CycloneDx', show: isSbomManager },
      ],
    },
    {
      label: 'Application Category',
      value: 'applicationCategory',
      show: !isSbomManager,
      example: 'Distributed',
      prefixList: [
        {
          value: 'applicationCategoryId',
          label: 'Application Category ID',
          example: '319cde35ef9749f4ab99a6473ad10b74',
          show: true,
        },
        { value: 'applicationCategoryName', label: 'Application Category Name', example: 'Distributed', show: true },
        { value: 'applicationCategoryColor', label: 'Application Category Color', example: 'yellow', show: true },
        {
          value: 'applicationCategoryDescription',
          label: 'Application Category Description',
          example: '"Outside the company"',
          show: true,
        },
      ],
    },
    {
      label: 'Component',
      value: 'component',
      show: true,
      example: 'mail:mailapi:1.4.2',
      prefixList: [
        { value: 'componentHash', label: 'Component Hash', example: 'f5149f0aaf01daf4bb2f', show: true },
        { value: 'componentFormat', label: 'Component Format', example: 'maven', show: true },
        { value: 'componentName', label: 'Component Name', example: 'mail:mailapi:1.4.2', show: true },
        {
          value: 'componentCoordinateGroupId',
          label: 'Component Coordinate (i.e. Maven) Group ID',
          example: 'commons-fileupload',
          show: true,
        },
        {
          value: 'componentCoordinateArtifactId',
          label: 'Component Coordinate (i.e. Maven) Artifact ID',
          example: 'mailapi',
          show: true,
        },
        { value: 'componentCoordinateVersion', label: 'Component Version', example: '1.2.6', show: true },
        { value: 'componentCoordinateClassifier', label: 'Version Classifier', example: 'dist', show: true },
        { value: 'componentCoordinateExtension', label: 'File Extension', example: 'jar', show: true },
        { value: 'componentCoordinateName', label: 'Coordinate Name', example: '"org.webjars bootstrap"', show: true },
        { value: 'componentCoordinateQualifier', label: 'Version Qualifier', example: 'cp37-cp37m-win32', show: true },
        {
          value: 'componentCoordinatePackageId',
          label: 'Package ID',
          example: 'loadash',
          show: true,
        },
        {
          value: 'componentCoordinateArchitecture',
          label: 'Target Architecture',
          example: 'x86_64',
          show: true,
        },
        { value: 'componentCoordinatePlatform', label: 'Target Platform', example: 'ruby', show: true },
      ],
    },
    {
      label: 'Component Label',
      value: 'componentLabel',
      show: !isSbomManager,
      example: 'Architecture-Cleanup',
      prefixList: [
        {
          value: 'componentLabelId',
          label: 'Component Label ID',
          example: '0d3f4015332e4b298ac1ed95c12ff3a3',
          show: true,
        },
        { value: 'componentLabelName', label: 'Label Name', example: 'Architecture-Cleanup', show: true },
        { value: 'componentLabelColor', label: 'Label Color', example: 'yellow', show: true },
        { value: 'componentLabelDescription', label: 'Label Description', example: '"relics of a build"', show: true },
      ],
    },
    {
      label: 'Policy',
      value: 'policy',
      show: true,
      example: 'Component-Unknown',
      prefixList: [
        { value: 'policyId', label: 'Policy ID', example: 'b4ca64a8b8264f03b65127016859b2a2', show: true },
        { value: 'policyName', label: 'Policy Name', example: 'Component-Unknown', show: true },
        { value: 'policyThreatCategory', label: 'Threat Category', example: 'security', show: true },
        { value: 'policyThreatLevel', label: 'Threat Security Level', example: '10', show: true },
      ],
    },
    {
      label: 'Security Vulnerability',
      value: 'securityVulnerability',
      show: true,
      example: 'CVE-2021-44228',
      prefixList: [
        { value: 'reportId', label: 'Report ID', example: 'a6860277aa844ab5af8bfef041f7e6e5', show: !isSbomManager },
        { value: 'policyEvaluationStage', label: 'Evaluation Stage', example: 'Build', show: !isSbomManager },
        { value: 'vulnerabilityId', label: 'CVE or Vulnerability ID', example: 'CVE-2021-44228', show: true },
        { value: 'vulnerabilityStatus', label: 'Vulnerability Status', example: 'Open', show: !isSbomManager },
        { value: 'vulnerabilitySeverity', label: 'CVSS Severity Score', example: '7.1', show: true },
        {
          value: 'vulnerabilityDescription',
          label: 'Vulnerability Description',
          example: '"directory traversal"',
          show: true,
        },
      ],
    },
    {
      label: 'Policy Violation',
      value: 'policyViolation',
      show: true,
      example: '"License-Copyleft"',
      prefixList: [
        {
          value: 'policyViolationPolicyName',
          label: 'Policy Violation Name',
          example: '"License-Copyleft"',
          show: true,
        },
        {
          value: 'policyViolationPolicyId',
          label: 'Policy Violation Policy ID',
          example: 'abc-123-def-456',
          show: true,
        },
        {
          value: 'policyViolationThreatCategory',
          label: 'Violation Threat Category',
          example: 'Legal',
          show: true,
        },
        { value: 'policyViolationThreatLevel', label: 'Violation Threat Level', example: '[7 TO 10]', show: true },
        { value: 'policyViolationWaiverStatus', label: 'Violation Waiver Status', example: 'Active', show: true },
        {
          value: 'policyViolationConstraintName',
          label: 'Violation Constraint',
          example: '"License Threat Group"',
          show: true,
        },
      ],
    },
    {
      label: 'License',
      value: 'license',
      show: true,
      example: 'Apache-2.0',
      prefixList: [
        {
          value: 'componentEffectiveLicenseId',
          label: 'Component Effective License ID',
          example: 'Apache-2.0',
          show: true,
        },
        {
          value: 'componentEffectiveLicenseName',
          label: 'Component Effective License Name',
          example: '"Apache License 2.0"',
          show: true,
        },
        {
          value: 'componentLicenseThreatGroupName',
          label: 'Component License Threat Group',
          example: '"Copyleft"',
          show: true,
        },
        {
          value: 'componentLicenseThreatLevel',
          label: 'Component License Threat Level',
          example: '[8 TO 10]',
          show: true,
        },
      ],
    },
    {
      label: 'Other',
      value: 'other',
      show: !isSbomManager,
      example: 'APPLICATION',
      prefixList: [{ value: 'itemType', label: 'Item Type', example: 'APPLICATION', show: true }],
    },
  ];
};

/**
 * Builds a search query string from an array of search criteria items.
 *
 * This function processes each search item and constructs a query string with the following rules:
 * - Adds wildcard (*) suffix unless the item is marked as exact match
 * - Adds logical operators (AND/OR) between terms, with the first term having no operator
 * - Handles proper spacing and formatting for the final query string
 *
 * @param {Array} searchItems - Array of search criteria objects containing:
 *   - field: Object with value (field name) and optional prefixList array
 *   - value: The search value to match against
 *   - operator: Logical operator ('AND' or 'OR') to use with this term
 *   - isExactMatch: Boolean indicating whether to add wildcard suffix
 */

export const buildSearchQuery = (searchItems) => {
  let query = '';
  searchItems.forEach((item, itemIndex) => {
    if (!item.field.value || !item.value) {
      return;
    }
    if (item.field.prefixList) {
      item.field.prefixList.forEach((prefix, prefixIndex) => {
        const operator = prefixIndex === 0 && itemIndex > 0 ? `${item.operator} ` : '';
        query += `${operator}${prefix.value}:${item.isExactMatch ? `"${item.value}"` : `*${item.value}*`} `;
      });
    } else {
      const operator = itemIndex > 0 ? `${item.operator} ` : '';
      query += `${operator}${item.field.value}:${item.isExactMatch ? `"${item.value}"` : `*${item.value}*`} `;
    }
  });
  return query;
};
