/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { render, screen, within } from 'TestRoot/SpecUtil';

import EditableConstraint from 'MainRoot/OrgsAndPolicies/policyEditor/constraints/EditableConstraint';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { DOES_NOT_EXIST_OPERATOR } from 'MainRoot/OrgsAndPolicies/utility/constants';

describe('EditableConstraint', () => {
  let renderComponent, props, constraint, conditionTypes, conditionTypesMap;
  let ageCondition,
    componentCategoryCondition,
    formatCondition,
    coordinatesCondition,
    packageUrlCondition,
    hygieneRatingCondition,
    integrityRatingCondition,
    identificationSourceCondition,
    labelCondition,
    licenseCondition,
    licenseStatusCondition,
    licenseThreatGroupCondition,
    licenseThreatGroupLevelCondition,
    matchStateCondition,
    proprietaryCondition,
    proprietaryNameConflictCondition,
    relativePopularityCondition,
    svSeverityCondition,
    svStatusCondition,
    svCategoryCondition,
    svCWECondition,
    svSourceCondition,
    dependencyTypeCondition,
    dataSourceCondition,
    iacControlTypeCondition;

  beforeEach(() => {
    constraint = {
      id: '1660233905005',
      name: {
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      },
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
      ],
      operator: 'OR',
    };
    ageCondition = {
      enabled: true,
      name: 'Age',
      id: 'AgeInDays',
      threatCategory: 'QUALITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['older than', 'younger than'],
      valueHint: 'Enter term',
      valueTypeId: 'AgeInDaysValueType',
      valueType: {
        id: 'AgeInDaysValueType',
        dataType: 'Integer',
        allowMultiple: false,
        availableValues: null,
      },
    };
    componentCategoryCondition = {
      enabled: false,
      name: 'Component Category',
      id: 'ComponentCategory',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'ComponentCategoryValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'ComponentCategoryValueType',
        dataType: 'ComponentCategory',
        allowMultiple: false,
        availableValues: [
          {
            id: '2',
            path: 'Analytics',
            name: 'Analytics',
          },
          {
            id: '5',
            path: 'Analytics/Application Analytics',
            name: 'Analytics/Application Analytics',
          },
          {
            id: '3',
            path: 'Analytics/Business Intelligence',
            name: 'Analytics/Business Intelligence',
          },
          {
            id: '4',
            path: 'Analytics/Web Analytics',
            name: 'Analytics/Web Analytics',
          },
          {
            id: '128',
            path: 'Application and Server Management',
            name: 'Application and Server Management',
          },
          {
            id: '164',
            path: 'Application Product Development Platform',
            name: 'Application Product Development Platform',
          },
          {
            id: '22',
            path: 'Audio and Video Management',
            name: 'Audio and Video Management',
          },
          {
            id: '23',
            path: 'Audio and Video Management/Graphics and Image Processing',
            name: 'Audio and Video Management/Graphics and Image Processing',
          },
          {
            id: '7',
            path: 'Big Data',
            name: 'Big Data',
          },
        ],
      },
    };
    formatCondition = {
      enabled: true,
      name: 'Format',
      id: 'ComponentFormat',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'ComponentFormatValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'ComponentFormatValueType',
        dataType: 'ComponentFormat',
        allowMultiple: false,
        availableValues: [
          {
            id: 'a-name',
            name: 'a-name',
          },
          {
            id: 'cargo',
            name: 'cargo',
          },
          {
            id: 'cocoapods',
            name: 'cocoapods',
          },
          {
            id: 'composer',
            name: 'composer',
          },
          {
            id: 'conan',
            name: 'conan',
          },
          {
            id: 'conda',
            name: 'conda',
          },
          {
            id: 'cran',
            name: 'cran',
          },
          {
            id: 'gem',
            name: 'gem',
          },
          {
            id: 'golang',
            name: 'golang',
          },
          {
            id: 'maven',
            name: 'maven',
          },
          {
            id: 'npm',
            name: 'npm',
          },
          {
            id: 'nuget',
            name: 'nuget',
          },
          {
            id: 'pecoff',
            name: 'pecoff',
          },
          {
            id: 'pypi',
            name: 'pypi',
          },
          {
            id: 'rpm',
            name: 'rpm',
          },
          {
            id: 'swift',
            name: 'swift',
          },
          {
            id: 'terraform',
            name: 'terraform',
          },
        ],
      },
    };
    coordinatesCondition = {
      enabled: true,
      name: 'Coordinates',
      id: 'Coordinates',
      autoUnquarantineSupported: false,
      supportedOperators: ['match', 'do not match'],
      valueTypeId: 'CoordinatesValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'CoordinatesValueType',
        dataType: 'String',
        allowMultiple: false,
        availableValues: null,
      },
    };
    packageUrlCondition = {
      enabled: true,
      name: 'Package URL',
      id: 'Package URL',
      autoUnquarantineSupported: false,
      supportedOperators: ['matches', 'does not match'],
      valueTypeId: 'PackageUrlValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'PackageUrlValueType',
        dataType: 'String',
        allowMultiple: false,
        availableValues: null,
      },
    };
    hygieneRatingCondition = {
      enabled: true,
      name: 'Hygiene Rating',
      id: 'HygieneRating',
      threatCategory: 'QUALITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'HygieneRatingValueType',
      valueHint: null,
      valueType: {
        id: 'HygieneRatingValueType',
        dataType: 'HygieneRating',
        allowMultiple: false,
        availableValues: [
          {
            id: '1',
            name: 'Exemplar',
          },
          {
            id: '4',
            name: 'Laggard',
          },
        ],
      },
    };
    integrityRatingCondition = {
      enabled: true,
      name: 'Integrity Rating',
      id: 'IntegrityRating',
      threatCategory: 'QUALITY',
      autoUnquarantineSupported: true,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'IntegrityRatingValueType',
      valueHint: null,
      valueType: {
        id: 'IntegrityRatingValueType',
        dataType: 'IntegrityRating',
        allowMultiple: false,
        availableValues: [
          {
            id: '0',
            name: 'Normal',
          },
          {
            id: '1',
            name: 'Suspicious',
          },
          {
            id: '2',
            name: 'Pending',
          },
          {
            id: '3',
            name: 'Not Applicable',
          },
        ],
      },
    };
    identificationSourceCondition = {
      enabled: true,
      name: 'Identification Source',
      id: 'IdentificationSource',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'IdentificationSourceValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'IdentificationSourceValueType',
        dataType: 'IdentificationSource',
        allowMultiple: false,
        availableValues: [
          {
            id: 'Sonatype',
            name: 'Sonatype',
          },
          {
            id: 'IaC',
            name: 'IaC',
          },
          {
            id: 'Manual',
            name: 'Manual',
          },
          {
            id: 'Clair',
            name: 'Clair',
          },
          {
            id: 'Package Manifest',
            name: 'Package Manifest',
          },
        ],
      },
    };
    labelCondition = {
      enabled: true,
      name: 'Label',
      id: 'Label',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'LabelValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'LabelValueType',
        dataType: 'Label',
        allowMultiple: false,
        availableValues: [
          {
            id: 'b223503100e34d088998bb4b670f1378',
            ownerId: 'ROOT_ORGANIZATION_ID',
            label: 'New Label',
            labelLowercase: 'new label',
            description: '',
            color: 'dark-blue',
          },
          {
            id: 'd5582b6edc4a4e18ba4942339b8da1e2',
            ownerId: 'ROOT_ORGANIZATION_ID',
            label: 'test label',
            labelLowercase: 'test label',
            description: 'this is working',
            color: 'light-green',
          },
          {
            id: '436de63263244a409cc54689b30d6c63',
            ownerId: 'ROOT_ORGANIZATION_ID',
            label: 'Security-Reachable',
            labelLowercase: 'security-reachable',
            description: 'Components with vulnerable methods that are reachable by the application',
            color: 'dark-red',
          },
        ],
      },
    };
    licenseCondition = {
      enabled: true,
      name: 'License',
      id: 'License',
      threatCategory: 'LICENSE',
      autoUnquarantineSupported: true,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'LicenseValueType',
      valueHint: null,
      valueType: {
        id: 'LicenseValueType',
        dataType: 'License',
        allowMultiple: false,
        availableValues: [
          {
            id: '0BSD',
            shortDisplayName: '0BSD',
            longDisplayName: 'BSD Zero Clause License',
          },
          {
            id: '10tec-Company-License-Agreement',
            shortDisplayName: '10tec-Company-License-Agreement',
            longDisplayName: '10tec Company License Agreement',
          },
          {
            id: '123-OSO-MIT-PL-2.0',
            shortDisplayName: '123-OSO-MIT-PL-2.0',
            longDisplayName: '123 Open-Source Organization MIT Public License v2.0',
          },
          {
            id: '2KSYS-EULA',
            shortDisplayName: '2KSYS-EULA',
            longDisplayName: '2KSYS End User License Agreement',
          },
          {
            id: 'AAL',
            shortDisplayName: 'AAL',
            longDisplayName: 'Attribution Assurance License',
          },
        ],
      },
    };
    licenseStatusCondition = {
      enabled: true,
      name: 'License Status',
      id: 'LicenseStatus',
      threatCategory: 'LICENSE',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'LicenseStatusValueType',
      valueHint: null,
      valueType: {
        id: 'LicenseStatusValueType',
        dataType: 'LicenseStatus',
        allowMultiple: false,
        availableValues: [
          {
            id: 'OPEN',
            name: 'Open',
          },
          {
            id: 'ACKNOWLEDGED',
            name: 'Acknowledged',
          },
          {
            id: 'OVERRIDDEN',
            name: 'Overridden',
          },
          {
            id: 'SELECTED',
            name: 'Selected',
          },
          {
            id: 'CONFIRMED',
            name: 'Confirmed',
          },
        ],
      },
    };
    licenseThreatGroupCondition = {
      enabled: true,
      name: 'License Threat Group',
      id: 'License Threat Group',
      threatCategory: 'LICENSE',
      autoUnquarantineSupported: true,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'LicenseThreatGroupValueType',
      valueHint: null,
      valueType: {
        id: 'LicenseThreatGroupValueType',
        dataType: 'LicenseThreatGroup',
        allowMultiple: false,
        availableValues: [
          {
            id: 'e038b5f69a96488f937623bae8c23484',
            ownerId: 'df9ad82193e44f4f9385e0c9e8835409',
            name: 'NewGroup',
            nameLowercaseNoWhitespace: 'newgroup',
            threatLevel: 10,
          },
          {
            id: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'New LTG',
            nameLowercaseNoWhitespace: 'newltg',
            threatLevel: 7,
          },
          {
            id: '876e9a143d56451489adda40a2e5bafa',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'New LTG 2',
            nameLowercaseNoWhitespace: 'newltg2',
            threatLevel: 2,
          },
          {
            id: '03b9f5fa8c05429f9be3323d5dcd8017',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'New LTG Group Test 2',
            nameLowercaseNoWhitespace: 'newltggrouptest2',
            threatLevel: 1,
          },
          {
            id: '6e6e32098eed4376b8674eaeb53a69cc',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'None Thread LTG Test',
            nameLowercaseNoWhitespace: 'nonethreadltgtest',
            threatLevel: 0,
          },
          {
            id: 'd66ade37b0d14e698816e0cd6c582af6',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'Testing all the names 12341234',
            nameLowercaseNoWhitespace: 'testingallthenames12341234',
            threatLevel: 10,
          },
          {
            id: '0eb52c770a1d495db2942fcc3009a0a9',
            ownerId: 'ROOT_ORGANIZATION_ID',
            name: 'lkisjha hjsufb hr hf',
            nameLowercaseNoWhitespace: 'lkisjhahjsufbhrhf',
            threatLevel: 5,
          },
          {
            id: 'UNASSIGNED_LICENSE_THREAT_GROUP_ID',
            ownerId: null,
            name: '[unassigned]',
            nameLowercaseNoWhitespace: '[unassigned]',
            threatLevel: 0,
          },
        ],
      },
    };
    licenseThreatGroupLevelCondition = {
      enabled: true,
      name: 'License Threat Group Level',
      id: 'License Threat Group Level',
      threatCategory: 'LICENSE',
      autoUnquarantineSupported: false,
      supportedOperators: ['<=', '>='],
      valueTypeId: 'IntegerValueType',
      valueHint: null,
      valueType: {
        id: 'IntegerValueType',
        dataType: 'Integer',
        allowMultiple: false,
        availableValues: null,
      },
    };
    matchStateCondition = {
      enabled: true,
      name: 'Match State',
      id: 'MatchState',
      autoUnquarantineSupported: true,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'MatchStateValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'MatchStateValueType',
        dataType: 'MatchState',
        allowMultiple: false,
        availableValues: [
          {
            id: 'exact',
            name: 'Exact',
          },
          {
            id: 'similar',
            name: 'Similar',
          },
          {
            id: 'unknown',
            name: 'Unknown',
          },
        ],
      },
    };
    proprietaryCondition = {
      enabled: true,
      name: 'Proprietary',
      id: 'Proprietary',
      autoUnquarantineSupported: false,
      supportedOperators: ['is true', 'is false'],
      valueTypeId: null,
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: null,
    };
    proprietaryNameConflictCondition = {
      enabled: true,
      name: 'Proprietary Name Conflict',
      id: 'ProprietaryNameConflict',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['is present', 'is not present'],
      valueTypeId: null,
      valueHint: null,
      valueType: null,
    };
    relativePopularityCondition = {
      enabled: true,
      name: 'Relative Popularity (Percentage)',
      id: 'RelativePopularity',
      threatCategory: 'QUALITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['=', '<', '<=', '>', '>='],
      valueHint: 'Enter percent value, 1 to 100',
      valueTypeId: 'PercentageValueType',
      valueType: {
        id: 'PercentageValueType',
        dataType: 'Integer',
        allowMultiple: false,
        availableValues: null,
      },
    };
    svSeverityCondition = {
      enabled: true,
      name: 'Security Vulnerability Severity',
      id: 'SecurityVulnerabilitySeverity',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: true,
      supportedOperators: ['=', '<', '<=', '>', '>='],
      valueHint: 'Enter value 0 to 10',
      valueTypeId: 'FloatValueType',
      valueType: {
        id: 'FloatValueType',
        dataType: 'Float',
        allowMultiple: false,
        availableValues: null,
      },
    };
    svStatusCondition = {
      enabled: true,
      name: 'Security Vulnerability Status',
      id: 'SecurityVulnerabilityStatus',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'SecurityVulnerabilityStatusValueType',
      valueHint: null,
      valueType: {
        id: 'SecurityVulnerabilityStatusValueType',
        dataType: 'SecurityVulnerabilityStatus',
        allowMultiple: false,
        availableValues: [
          {
            id: 'OPEN',
            name: 'Open',
          },
          {
            id: 'ACKNOWLEDGED',
            name: 'Acknowledged',
          },
          {
            id: 'NOT_APPLICABLE',
            name: 'Not Applicable',
          },
          {
            id: 'CONFIRMED',
            name: 'Confirmed',
          },
        ],
      },
    };
    svCategoryCondition = {
      enabled: true,
      name: 'Security Vulnerability Category',
      id: 'SecurityVulnerabilityCategory',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: true,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'SecurityVulnerabilityCategoryValueType',
      valueHint: null,
      valueType: {
        id: 'SecurityVulnerabilityCategoryValueType',
        dataType: 'SecurityVulnerabilityCategory',
        allowMultiple: false,
        availableValues: [
          {
            id: 'configuration',
            name: 'Configuration',
          },
          {
            id: 'data',
            name: 'Data',
          },
          {
            id: 'functional',
            name: 'Functional',
          },
          {
            id: 'operational',
            name: 'Operational',
          },
          {
            id: 'malicious_code',
            name: 'Malicious Code',
          },
          {
            id: 'other',
            name: 'Other',
          },
          {
            id: 'privileged',
            name: 'Privileged',
          },
          {
            id: 'sample_code',
            name: 'Sample Code',
          },
          {
            id: 'test_code',
            name: 'Test Code',
          },
        ],
      },
    };
    svCWECondition = {
      enabled: true,
      name: 'Security Vulnerability CWE',
      id: 'SecurityVulnerabilityCwe',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'SecurityVulnerabilityCweValueType',
      valueHint: null,
      valueType: {
        id: 'SecurityVulnerabilityCweValueType',
        dataType: 'String',
        allowMultiple: false,
        availableValues: null,
      },
    };
    svSourceCondition = {
      enabled: true,
      name: 'Data Source',
      id: 'DataSource',
      threatCategory: 'OTHER',
      autoUnquarantineSupported: false,
      supportedOperators: ['has support for', 'has no support for'],
      valueTypeId: 'DataSourceValueType',
      valueHint: null,
      valueType: {
        id: 'DataSourceValueType',
        dataType: 'DataSourceValue',
        allowMultiple: false,
        availableValues: [
          {
            id: 'license',
            name: 'License',
          },
          {
            id: 'identity',
            name: 'Identity',
          },
        ],
      },
    };
    dependencyTypeCondition = {
      enabled: true,
      name: 'Dependency Type',
      id: 'DependencyType',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'DependencyTypeValueType',
      threatCategory: 'OTHER',
      valueHint: null,
      valueType: {
        id: 'DependencyTypeValueType',
        dataType: 'DependencyType',
        allowMultiple: false,
        availableValues: [
          {
            id: 'direct',
            name: 'Direct',
          },
          {
            id: 'transitive',
            name: 'Transitive',
          },
          {
            id: 'innersource',
            name: 'InnerSource',
          },
        ],
      },
    };
    conditionTypes = [
      ageCondition,
      componentCategoryCondition,
      formatCondition,
      coordinatesCondition,
      packageUrlCondition,
      hygieneRatingCondition,
      integrityRatingCondition,
      identificationSourceCondition,
      labelCondition,
      licenseCondition,
      licenseStatusCondition,
      licenseThreatGroupCondition,
      licenseThreatGroupLevelCondition,
      matchStateCondition,
      proprietaryCondition,
      proprietaryNameConflictCondition,
      relativePopularityCondition,
      svSeverityCondition,
      svStatusCondition,
      svCategoryCondition,
      svCWECondition,
      svSourceCondition,
      dependencyTypeCondition,
    ];
    dataSourceCondition = {
      enabled: true,
      name: 'Data Source',
      id: 'DataSource',
      threatCategory: 'OTHER',
      autoUnquarantineSupported: false,
      supportedOperators: ['has support for', 'has no support for'],
      valueTypeId: 'DataSourceValueType',
      valueHint: null,
      valueType: {
        id: 'DataSourceValueType',
        dataType: 'DataSourceValue',
        allowMultiple: false,
        availableValues: [
          {
            id: 'license',
            name: 'License',
          },
          {
            id: 'identity',
            name: 'Identity',
          },
        ],
      },
    };
    iacControlTypeCondition = {
      enabled: false,
      name: 'IaC Compliance Family',
      id: 'IacControlConditionType',
      threatCategory: 'SECURITY',
      autoUnquarantineSupported: false,
      supportedOperators: ['is', 'is not'],
      valueTypeId: 'IacControlValueType',
      valueHint: null,
      valueType: {
        id: 'IacControlValueType',
        dataType: 'IacControl',
        allowMultiple: true,
        availableValues: [
          {
            id: 'CIS-AWS_v1.2.0',
            name: 'CIS AWS Foundations Benchmark (v1.2.0)',
          },
          {
            id: 'CIS-AWS_v1.4.0',
            name: 'CIS AWS Foundations Benchmark (v1.4.0)',
          },
          {
            id: 'CIS-Azure_v1.1.0',
            name: 'CIS Azure (v1.1.0)',
          },
          {
            id: 'CIS-Azure_v1.3.0',
            name: 'CIS Azure (v1.3.0)',
          },
          {
            id: 'CIS-Controls_v7.1',
            name: 'CIS Controls (v7.1)',
          },
          {
            id: 'CIS-Google_v1.1.0',
            name: 'CIS Google (v1.1.0)',
          },
          {
            id: 'CIS-Google_v1.2.0',
            name: 'CIS Google (v1.2.0)',
          },
          {
            id: 'CIS-Kubernetes_v1.6.1',
            name: 'CIS Kubernetes Benchmark (v1.6.1)',
          },
          {
            id: 'CSA-CCM_v3.0.1',
            name: 'CSA CCM (v3.0.1)',
          },
          {
            id: 'GDPR_v2016',
            name: 'GDPR (v2016)',
          },
          {
            id: 'HIPAA_v2013',
            name: 'HIPAA (v2013)',
          },
          {
            id: 'ISO-27001_v2013',
            name: 'ISO 27001 (v2013)',
          },
          {
            id: 'NIST-800-53_vRev4',
            name: 'NIST 800-53 (vRev4)',
          },
          {
            id: 'PCI-DSS_v3.2.1',
            name: 'PCI DSS (v3.2.1)',
          },
          {
            id: 'SOC-2_v2017',
            name: 'SOC 2 (v2017)',
          },
        ],
      },
    };
    conditionTypesMap = {
      AgeInDays: ageCondition,
      ComponentCategory: componentCategoryCondition,
      ComponentFormat: formatCondition,
      Coordinates: coordinatesCondition,
      'Package URL': packageUrlCondition,
      HygieneRating: hygieneRatingCondition,
      IntegrityRating: integrityRatingCondition,
      IdentificationSource: identificationSourceCondition,
      Label: labelCondition,
      License: licenseCondition,
      LicenseStatus: licenseStatusCondition,
      'License Threat Group': licenseThreatGroupCondition,
      'License Threat Group Level': licenseThreatGroupLevelCondition,
      MatchState: matchStateCondition,
      Proprietary: proprietaryCondition,
      ProprietaryNameConflict: proprietaryNameConflictCondition,
      RelativePopularity: relativePopularityCondition,
      SecurityVulnerabilitySeverity: svSeverityCondition,
      SecurityVulnerabilityStatus: svStatusCondition,
      SecurityVulnerabilityCategory: svCategoryCondition,
      SecurityVulnerabilityCwe: svCWECondition,
      SecurityVulnerabilitySource: svSourceCondition,
      DataSource: dataSourceCondition,
      DependencyType: dependencyTypeCondition,
      IacControlConditionType: iacControlTypeCondition,
    };

    props = {
      constraint: constraint,
      constraintIdx: 0,
      cannotBeRemoved: true,
      conditionTypes: conditionTypes,
      conditionTypesMap: conditionTypesMap,
    };

    renderComponent = (additionalProps) => {
      render(<EditableConstraint {...props} {...additionalProps} />);
    };
  });

  describe('renders a correct initial state constraint editor', () => {
    it('with empty constrain header', () => {
      renderComponent();
      const constrainElement = screen.getByTestId('editable-constraint');
      validateConstrainHeader(props, constrainElement);
    });

    it('with default conditions header', () => {
      renderComponent();
      const constrainElement = screen.getByTestId('editable-constraint');
      validateConditionHeader(props, constrainElement);
    });

    it('with one default condition', () => {
      renderComponent();
      const constrainElement = screen.getByTestId('editable-constraint');
      expect(constrainElement).toBeVisible();
      const conditionElements = within(constrainElement).getAllByTestId('editable-constraint__condition');
      expect(conditionElements.length).toBe(1);
      validateCondition(props.constraint.conditions[0], conditionElements[0], 0);
    });

    describe('renders correct empty condition', () => {
      const emptyConditions = [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'ComponentCategory',
          operator: 'is',
          value: {
            isPristine: true,
            value: '2',
            trimmedValue: '2',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'ComponentFormat',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'a-name',
            trimmedValue: 'a-name',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'Coordinates',
          operator: 'match',
          value: {
            format: 'maven',
            groupId: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            artifactId: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            version: {
              isPristine: true,
              value: '',
              trimmedValue: '',
              validationErrors: null,
            },
            extension: {
              isPristine: true,
              value: '*',
              trimmedValue: '*',
              validationErrors: null,
            },
            classifier: {
              isPristine: true,
              value: '*',
              trimmedValue: '*',
              validationErrors: null,
            },
          },
        },
        {
          conditionTypeId: 'Package URL',
          operator: 'matches',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'HygieneRating',
          operator: 'is',
          value: {
            isPristine: true,
            value: '1',
            trimmedValue: '1',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'IntegrityRating',
          operator: 'is',
          value: {
            isPristine: true,
            value: '0',
            trimmedValue: '0',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'IdentificationSource',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'Sonatype',
            trimmedValue: 'Sonatype',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'Label',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'b223503100e34d088998bb4b670f1378',
            trimmedValue: 'b223503100e34d088998bb4b670f1378',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'License',
          operator: 'is',
          value: {
            isPristine: true,
            value: '0BSD',
            trimmedValue: '0BSD',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'LicenseStatus',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'OPEN',
            trimmedValue: 'OPEN',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'License Threat Group',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'e038b5f69a96488f937623bae8c23484',
            trimmedValue: 'e038b5f69a96488f937623bae8c23484',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'License Threat Group Level',
          operator: '<=',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'MatchState',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'exact',
            trimmedValue: 'exact',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'Proprietary',
          operator: 'is true',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'ProprietaryNameConflict',
          operator: 'is present',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'RelativePopularity',
          operator: '=',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'SecurityVulnerabilitySeverity',
          operator: '=',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'SecurityVulnerabilityStatus',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'OPEN',
            trimmedValue: 'OPEN',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'SecurityVulnerabilityCategory',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'configuration',
            trimmedValue: 'configuration',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'SecurityVulnerabilityCwe',
          operator: 'is',
          value: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'DataSource',
          operator: 'has support for',
          value: {
            isPristine: true,
            value: 'license',
            trimmedValue: 'license',
            validationErrors: null,
          },
        },
        {
          conditionTypeId: 'DependencyType',
          operator: 'is',
          value: {
            isPristine: true,
            value: 'direct',
            trimmedValue: 'direct',
            validationErrors: null,
          },
        },
      ];

      emptyConditions.forEach(testCondition);
    });
  });

  it('renders error alert if condition is not supported', () => {
    renderComponent({
      constraint: {
        ...constraint,
        conditions: [
          {
            conditionTypeId: 'ComponentCategory',
            operator: 'is not',
            value: '4',
          },
        ],
        operator: 'OR',
      },
    });
    expect(
      screen.getByText('Component Category condition is not supported by your license. Please revise the constraint.')
    ).toBeVisible();
  });

  describe('renders a correct information for constraint editor', () => {
    const conditionWithInformation = [
      {
        conditionTypeId: 'AgeInDays',
        operator: 'younger than',
        value: {
          isPristine: false,
          value: '14',
          trimmedValue: '14',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'ComponentCategory',
        operator: 'is',
        value: '7',
      },
      {
        conditionTypeId: 'ComponentFormat',
        operator: 'is',
        value: 'cargo',
      },
      {
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'maven',
          groupId: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          artifactId: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          version: {
            isPristine: true,
            value: '',
            trimmedValue: '',
            validationErrors: null,
          },
          extension: {
            isPristine: true,
            value: '*',
            trimmedValue: '*',
            validationErrors: null,
          },
          classifier: {
            isPristine: true,
            value: '*',
            trimmedValue: '*',
            validationErrors: null,
          },
        },
      },
      {
        conditionTypeId: 'Package URL',
        operator: 'matches',
        value: {
          isPristine: false,
          value: 'pkg:maven/javax.test@2.4.0',
          trimmedValue: 'pkg:maven/javax.test@2.4.0',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'HygieneRating',
        operator: 'is not',
        value: '4',
      },
      {
        conditionTypeId: 'IntegrityRating',
        operator: 'is not',
        value: '1',
      },
      {
        conditionTypeId: 'IdentificationSource',
        operator: 'is not',
        value: {
          isPristine: true,
          value: 'Sonatype',
          trimmedValue: 'Sonatype',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'Label',
        operator: 'is not',
        value: 'd5582b6edc4a4e18ba4942339b8da1e2',
      },
      {
        conditionTypeId: 'License',
        operator: 'is',
        value: 'AAL',
      },
      {
        conditionTypeId: 'LicenseStatus',
        operator: 'is not',
        value: {
          isPristine: true,
          value: 'OPEN',
          trimmedValue: 'OPEN',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'License Threat Group',
        operator: 'is',
        value: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
      },
      {
        conditionTypeId: 'License Threat Group Level',
        operator: '<=',
        value: {
          isPristine: false,
          value: '2',
          trimmedValue: '2',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'MatchState',
        operator: 'is',
        value: 'similar',
      },
      {
        conditionTypeId: 'Proprietary',
        operator: 'is false',
        value: {
          isPristine: true,
          value: '',
          trimmedValue: '',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'ProprietaryNameConflict',
        operator: 'is not present',
        value: {
          isPristine: true,
          value: '',
          trimmedValue: '',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'RelativePopularity',
        operator: '<=',
        value: {
          isPristine: false,
          value: '1',
          trimmedValue: '1',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'SecurityVulnerabilitySeverity',
        operator: '>',
        value: {
          isPristine: false,
          value: '5',
          trimmedValue: '5',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'SecurityVulnerabilityStatus',
        operator: 'is not',
        value: 'CONFIRMED',
      },
      {
        conditionTypeId: 'SecurityVulnerabilityCategory',
        operator: 'is not',
        value: 'other',
      },
      {
        conditionTypeId: 'SecurityVulnerabilityCwe',
        operator: 'is not',
        value: {
          isPristine: false,
          value: '111',
          trimmedValue: '111',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'DataSource',
        operator: 'has no support for',
        value: 'identity',
      },
      {
        conditionTypeId: 'DependencyType',
        operator: 'is',
        value: 'transitive',
      },
    ];
    conditionWithInformation.forEach(testCondition);
  });

  describe('renders warning message if values are not correct', () => {
    const conditionWithError = [
      {
        conditionTypeId: 'AgeInDays',
        operator: 'younger than',
        value: {
          isPristine: false,
          value: '',
          trimmedValue: '',
          validationErrors: ['Must be non-empty', 'Minimum allowed value is 1'],
        },
      },
      {
        conditionTypeId: 'ComponentCategory',
        operator: 'is',
        value: '7',
      },
      {
        conditionTypeId: 'ComponentFormat',
        operator: 'is',
        value: 'cargo',
      },
      {
        conditionTypeId: 'Coordinates',
        operator: 'match',
        value: {
          format: 'maven',
          groupId: {
            isPristine: false,
            value: '   ',
            trimmedValue: '',
            validationErrors: ['Must be non-empty'],
          },
          artifactId: {
            isPristine: false,
            value: '   ',
            trimmedValue: '',
            validationErrors: ['Must be non-empty'],
          },
          version: {
            isPristine: false,
            value: '',
            trimmedValue: '',
            validationErrors: ['Must be non-empty'],
          },
          extension: {
            isPristine: false,
            value: '  ',
            trimmedValue: '',
            validationErrors: [],
          },
          classifier: {
            isPristine: false,
            value: '  ',
            trimmedValue: '',
            validationErrors: [],
          },
        },
      },
      {
        conditionTypeId: 'Package URL',
        operator: 'matches',
        value: {
          isPristine: false,
          value: 'pkg:maven/javax.test@2.4.0',
          trimmedValue: 'pkg:maven/javax.test@2.4.0',
          validationErrors: [],
        },
      },
      {
        conditionTypeId: 'HygieneRating',
        operator: 'is not',
        value: '4',
      },
      {
        conditionTypeId: 'IntegrityRating',
        operator: 'is not',
        value: '1',
      },
      {
        conditionTypeId: 'IdentificationSource',
        operator: 'is not',
        value: {
          isPristine: true,
          value: 'Sonatype',
          trimmedValue: 'Sonatype',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'Label',
        operator: 'is not',
        value: 'd5582b6edc4a4e18ba4942339b8da1e2',
      },
      {
        conditionTypeId: 'License',
        operator: 'is',
        value: 'AAL',
      },
      {
        conditionTypeId: 'LicenseStatus',
        operator: 'is not',
        value: {
          isPristine: true,
          value: 'OPEN',
          trimmedValue: 'OPEN',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'License Threat Group',
        operator: 'is',
        value: 'e4183d8c1c6b4a52a2dba8cf9137cc82',
      },
      {
        conditionTypeId: 'License Threat Group Level',
        operator: '<=',
        value: {
          isPristine: false,
          value: 'aa',
          trimmedValue: 'aa',
          validationErrors: ['Please enter a whole number'],
        },
      },
      {
        conditionTypeId: 'MatchState',
        operator: 'is',
        value: 'similar',
      },
      {
        conditionTypeId: 'Proprietary',
        operator: 'is false',
        value: {
          isPristine: true,
          value: '',
          trimmedValue: '',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'ProprietaryNameConflict',
        operator: 'is not present',
        value: {
          isPristine: true,
          value: '',
          trimmedValue: '',
          validationErrors: null,
        },
      },
      {
        conditionTypeId: 'RelativePopularity',
        operator: '<=',
        value: {
          isPristine: false,
          value: 'aa',
          trimmedValue: 'aa',
          validationErrors: ['Value must be from 0 to 100'],
        },
      },
      {
        conditionTypeId: 'SecurityVulnerabilitySeverity',
        operator: '>',
        value: {
          isPristine: false,
          value: 'aa',
          trimmedValue: 'aa',
          validationErrors: ['Please enter a decimal number'],
        },
      },
      {
        conditionTypeId: 'SecurityVulnerabilityStatus',
        operator: 'is not',
        value: 'CONFIRMED',
      },
      {
        conditionTypeId: 'SecurityVulnerabilityCategory',
        operator: 'is not',
        value: 'other',
      },
      {
        conditionTypeId: 'SecurityVulnerabilityCwe',
        operator: 'is not',
        value: {
          isPristine: false,
          value: '',
          trimmedValue: '',
          validationErrors: ['Must be non-empty'],
        },
      },
      {
        conditionTypeId: 'DataSource',
        operator: 'has no support for',
        value: 'identity',
      },
      {
        conditionTypeId: 'DependencyType',
        operator: 'is',
        value: 'transitive',
      },
    ];
    conditionWithError.forEach(testCondition);
  });

  describe('EPSS Score condition with "does not exist" operator', () => {
    let epssScoreCondition;

    beforeEach(() => {
      epssScoreCondition = {
        enabled: true,
        name: 'EPSS Score (percentage)',
        id: 'SecurityVulnerabilityEpssScore',
        threatCategory: 'SECURITY',
        autoUnquarantineSupported: false,
        supportedOperators: ['=', '<', '<=', '>', '>=', DOES_NOT_EXIST_OPERATOR],
        valueHint: 'Enter value 0 to 100',
        valueTypeId: 'DoubleValueType',
        valueType: {
          id: 'DoubleValueType',
          dataType: 'Double',
          allowMultiple: false,
          availableValues: null,
        },
      };
      conditionTypesMap['SecurityVulnerabilityEpssScore'] = epssScoreCondition;
    });

    it('disables value field when "does not exist" operator is selected', () => {
      const epssProps = {
        ...props,
        constraint: {
          ...constraint,
          conditions: [
            {
              conditionTypeId: 'SecurityVulnerabilityEpssScore',
              operator: DOES_NOT_EXIST_OPERATOR,
              value: {
                isPristine: true,
                value: '',
                trimmedValue: '',
                validationErrors: null,
              },
            },
          ],
        },
      };

      renderComponent(epssProps);
      const constrainElement = screen.getByTestId('editable-constraint');
      expect(constrainElement).toBeVisible();
      const conditionElements = within(constrainElement).getAllByTestId('editable-constraint__condition');
      expect(conditionElements.length).toBe(1);

      const conditionInputValue = within(conditionElements[0]).getByTestId('constraint__condition-value');
      expect(conditionInputValue).toBeDisabled();
    });

    it('enables value field when numeric operator is selected', () => {
      const epssProps = {
        ...props,
        constraint: {
          ...constraint,
          conditions: [
            {
              conditionTypeId: 'SecurityVulnerabilityEpssScore',
              operator: '>=',
              value: {
                isPristine: false,
                value: '50',
                trimmedValue: '50',
                validationErrors: [],
              },
            },
          ],
        },
      };

      renderComponent(epssProps);
      const constrainElement = screen.getByTestId('editable-constraint');
      expect(constrainElement).toBeVisible();
      const conditionElements = within(constrainElement).getAllByTestId('editable-constraint__condition');
      expect(conditionElements.length).toBe(1);

      const conditionInputValue = within(conditionElements[0]).getByTestId('constraint__condition-value');
      expect(conditionInputValue).not.toBeDisabled();
      expect(conditionInputValue).toHaveValue('50');
    });
  });

  describe('custom label condition', () => {
    it('renders the labels dropdown with available values except the "Security-Reachable" label', () => {
      const labelProps = {
        ...props,
        constraint: {
          ...constraint,
          conditions: [
            {
              conditionTypeId: 'Label',
              operator: 'is not',
              value: 'd5582b6edc4a4e18ba4942339b8da1e2',
            },
          ],
        },
      };

      renderComponent(labelProps);
      const constrainElement = screen.getByTestId('editable-constraint');
      expect(constrainElement).toBeVisible();
      const conditionElements = within(constrainElement).getAllByTestId('editable-constraint__condition');
      expect(conditionElements.length).toBe(1);

      const condition = conditionTypesMap['Label'];
      const conditionElement = conditionElements[0];
      const availableValues = condition.valueType.availableValues;

      const conditionInputValue = within(conditionElement).getByTestId('constraint__condition-value');
      const valuesRenderedInCombobox = within(conditionInputValue).getAllByRole('option');

      expect(availableValues.length).toBe(3); //contains a security-reachable label
      expect(valuesRenderedInCombobox.length).toBe(2); //does not contain a security-reachable label

      expect(valuesRenderedInCombobox[0]).toHaveTextContent('New Label');
      expect(valuesRenderedInCombobox[1]).toHaveTextContent('test label');

      expect(within(conditionInputValue).getByRole('option', { name: /new label/i })).toBeInTheDocument();
      expect(within(conditionInputValue).getByRole('option', { name: /test label/i })).toBeInTheDocument();
      expect(
        within(conditionInputValue).queryByRole('option', { name: /security-reachable/i })
      ).not.toBeInTheDocument();
    });
  });

  function testCondition(condition) {
    constraint = {
      ...constraint,
      conditions: [condition],
    };
    props = {
      ...props,
      constraint: constraint,
    };

    it(`for ${condition.conditionTypeId}`, () => {
      renderComponent();
      const constrainElement = screen.getByTestId('editable-constraint');
      expect(constrainElement).toBeVisible();
      const conditionElements = within(constrainElement).getAllByTestId('editable-constraint__condition');
      expect(conditionElements.length).toBe(1);
      validateCondition(props.constraint.conditions[0], conditionElements[0], 0);
    });
  }

  function validateConstrainHeader(constrainInfo, constrainElement) {
    expect(constrainElement).toBeVisible();
    expect(within(constrainElement).getByText('Constraint Name')).toBeVisible();
    const constrainNameInput = within(constrainElement).getByRole('textbox', { name: 'Constraint Name' });
    expect(constrainNameInput).toBeVisible();
    expect(constrainNameInput).toHaveValue(constrainInfo.constraint.name.value);
    const deleteConstraintButton = within(constrainElement).getByRole('button', { name: 'Delete constraint' });

    constrainInfo.cannotBeRemoved
      ? expect(deleteConstraintButton).toBeDisabled()
      : expect(deleteConstraintButton).not.toBeDisabled();
  }

  function validateConditionHeader(constrainInfo, constrainElement) {
    expect(constrainElement).toBeVisible();
    expect(within(constrainElement).getByText('Conditions')).toBeVisible();

    let constrainsOption = within(constrainElement).getByTestId('constraintsOperator');
    expect(constrainsOption).toBeVisible();

    expect(within(constrainsOption).getAllByRole('option').length).toBe(2);
    if (constrainInfo.constraint.operator === 'OR') {
      expect(within(constrainsOption).getByRole('option', { name: 'any' }).selected).toBe(true);
      expect(within(constrainsOption).getByRole('option', { name: 'all' }).selected).toBe(false);
    } else {
      expect(within(constrainsOption).getByRole('option', { name: 'any' }).selected).toBe(false);
      expect(within(constrainsOption).getByRole('option', { name: 'all' }).selected).toBe(true);
    }

    let addConditionButton = within(constrainElement).getByRole('button', { name: 'Add Condition' });
    expect(addConditionButton).not.toBeDisabled();
  }

  function validateCondition(conditionInformation, conditionElement, conditionIndex) {
    const condition = conditionTypesMap[conditionInformation.conditionTypeId];

    const conditionTypeComboBox = within(conditionElement).getByTestId('constraint__condition-type');
    validateComboBox(conditionTypeComboBox, conditionTypes, condition.name);

    const conditionOperatorComboBox = within(conditionElement).getByTestId('constraint__condition-operator');
    validateComboBox(conditionOperatorComboBox, condition.supportedOperators, conditionInformation.operator);

    const deleteConditionButton = within(conditionElement).getByRole('button', { name: 'Delete condition' });
    expect(deleteConditionButton).toBeVisible();
    conditionIndex > 0
      ? expect(deleteConditionButton).not.toBeDisabled()
      : expect(deleteConditionButton).toBeDisabled();

    const actualValue = conditionInformation.value;
    if (condition.valueTypeId === 'AgeInDaysValueType') {
      validateAgeInDaysInput(conditionElement);
    } else if (condition.valueTypeId !== 'CoordinatesValueType') {
      validateCoordinatesInput(conditionElement, actualValue);
    } else {
      const conditionDataType = new Set(['String', 'Integer', 'Float']);
      const conditionInputValue = within(conditionElement).getByTestId('constraint__condition-value');
      expect(conditionInputValue).toBeVisible();

      if (condition.valueType?.availableValues) {
        validateComboBox(conditionInputValue, condition.valueType.availableValues, actualValue.value);
      }

      if (conditionDataType.has(condition.valueType?.dataType)) {
        validateTextInput(conditionInputValue, actualValue);
      }

      const invalidInputMessage = within(conditionElement).getByRole('alert');
      validateInputErrorMessage(invalidInputMessage, actualValue);
    }
  }

  function validateAgeInDaysInput(conditionElement) {
    const ageInput = within(conditionElement).getByPlaceholderText('Age');
    expect(ageInput).toBeVisible();

    const ageModifierSelect = within(conditionElement).getByRole('combobox', { name: /age modifier/i });
    expect(ageModifierSelect).toBeVisible();
  }

  function validateCoordinatesInput(conditionElement, actualValues) {
    const coordinateFormat = within(conditionElement).getByRole('combobox');
    validateComboBoxSelectedOption(coordinateFormat, actualValues.format);
    const groupId = within(conditionElement).getByRole('textbox', { name: 'groupId' });
    validateTextInput(groupId, actualValues.value.groupId);
    validateInputErrorMessage(groupId, actualValues.value.groupId);
    const artifactId = within(conditionElement).getByRole('textbox', { name: 'artifactId' });
    validateTextInput(artifactId, actualValues.value.artifactId);
    validateInputErrorMessage(artifactId, actualValues.value.artifactId);
    const version = within(conditionElement).getByRole('textbox', { name: 'version' });
    validateTextInput(version, actualValues.value.version);
    validateInputErrorMessage(version, actualValues.value.version);
    const extension = within(conditionElement).getByRole('textbox', { name: 'extension' });
    validateTextInput(extension, actualValues.value.extension);
    validateInputErrorMessage(extension, actualValues.value.extension);
    const classifier = within(conditionElement).getByRole('textbox', { name: 'classifier' });
    validateTextInput(classifier, actualValues.value.classifier);
    validateInputErrorMessage(classifier, actualValues.value.classifier);
  }

  function validateTextInput(textInput, expectedValue) {
    expect(textInput).toBeVisible();
    expect(textInput).toHaveValue(expectedValue.value);
    expectedValue.isPristine && isNilOrEmpty(expectedValue.validationErrors)
      ? expect(textInput).toHaveAttribute('aria-invalid', false)
      : expect(textInput).toHaveAttribute('aria-invalid', true);
  }

  function validateComboBox(comboBoxInput, options, selectedOptionValue) {
    const valueOptions = within(comboBoxInput).getAllByRole('option');
    expect(valueOptions.length).toBe(options.length);
    validateComboBoxSelectedOption(comboBoxInput, selectedOptionValue);
  }

  function validateComboBoxSelectedOption(comboBoxInput, selectedOptionValue) {
    const selectedValueOption = within(comboBoxInput).getByRole('option', {
      name: selectedOptionValue,
    });
    expect(selectedValueOption).toBeVisible();
    expect(selectedValueOption.selected).toBe(true);
  }

  function validateInputErrorMessage(invalidInputMessage, actualValue) {
    if (actualValue.isPristine) {
      expect(invalidInputMessage).not.toBeInTheDocument();
    } else {
      if (isNilOrEmpty(actualValue.validationErrors)) {
        expect(invalidInputMessage).not.toBeInTheDocument();
      } else {
        expect(invalidInputMessage).toBeVisible();
        expect(invalidInputMessage).toHaveTextContent(actualValue.validationErrors[0]);
      }
    }
  }
});
