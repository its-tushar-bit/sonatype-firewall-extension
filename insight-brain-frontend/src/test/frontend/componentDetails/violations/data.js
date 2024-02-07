/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const transitiveData = {
  componentIdentifier: {
    format: 'maven',
    coordinates: {
      artifactId: 'ACME-business',
      classifier: '',
      extension: 'jar',
      groupId: 'org.example',
      version: '1.0-SNAPSHOT',
    },
  },
  packageUrl: 'pkg:maven/org.example/ACME-business@1.0-SNAPSHOT?type=jar',
  hash: '03ff80065de60b9287f4',
  displayName: 'org.example : ACME-business : 1.0-SNAPSHOT',
  isInnerSource: true,
  transitivePolicyViolations: [
    {
      policyId: 'b4208ba2fd564b5bb56499891269297b',
      policyName: 'Security-Medium',
      threatLevel: 7,
      threatCategory: 'security',
      policyViolationId: '8edaafce603b4c91b99665ff3e99f8f6',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'commons-io',
          classifier: '',
          extension: 'jar',
          groupId: 'commons-io',
          version: '2.6',
        },
      },
      packageUrl: 'pkg:maven/commons-io/commons-io@2.6?type=jar',
      hash: '815893df5f31da2ece40',
      displayName: 'commons-io : commons-io : 2.6',
    },
    {
      policyId: 'a20c5e100ba840afaa382b14d4d90176',
      policyName: 'Architecture-Quality',
      threatLevel: 1,
      threatCategory: 'quality',
      policyViolationId: '230d2e0619ca433cbdf6be6db055a0f8',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'commons-io',
          classifier: '',
          extension: 'jar',
          groupId: 'commons-io',
          version: '2.6',
        },
      },
      packageUrl: 'pkg:maven/commons-io/commons-io@2.6?type=jar',
      hash: '815893df5f31da2ece40',
      displayName: 'commons-io : commons-io : 2.6',
    },
    {
      policyId: 'a20c5e100ba840afaa382b14d4d90176',
      policyName: 'Architecture-Quality',
      threatLevel: 1,
      threatCategory: 'quality',
      policyViolationId: '83ea9cf754e6425c93b7d71d20a87234',
      componentIdentifier: {
        format: 'maven',
        coordinates: {
          artifactId: 'javax.inject',
          classifier: '',
          extension: 'jar',
          groupId: 'javax.inject',
          version: '1',
        },
      },
      packageUrl: 'pkg:maven/javax.inject/javax.inject@1?type=jar',
      hash: '6975da39a7040257bd51',
      displayName: 'javax.inject : javax.inject : 1',
    },
  ],
};

export const ownerHierarchyData = {
  id: 'ROOT_ORGANIZATION_ID',
  publicId: 'ROOT_ORGANIZATION_ID',
  name: 'Root Organization',
  type: 'organization',
  children: [
    {
      id: '42114440259e4214b2f1d6498016763e',
      publicId: '42114440259e4214b2f1d6498016763e',
      name: 'test',
      type: 'organization',
      children: [
        {
          id: 'f415c8f8830a4269b1f22493edbc3bda',
          publicId: 'ACME-CONSUMER',
          name: 'ACME-CONSUMER',
          type: 'application',
          children: null,
        },
      ],
    },
  ],
};

export const reportMockMetaData = {
  reportTime: 1706213339203,
  reportTitle: 'Build Report',
  application: {
    name: 'ACME-CONSUMER',
    nameLowercaseNoWhitespace: 'acme-consumer',
    id: 'f415c8f8830a4269b1f22493edbc3bda',
    publicId: 'ACME-CONSUMER',
    publicIdLowercase: 'acme-consumer',
    organizationId: '42114440259e4214b2f1d6498016763e',
    contactInternalName: null,
    organization: {
      name: 'test',
      nameLowercaseNoWhitespace: 'test',
      id: '42114440259e4214b2f1d6498016763e',
      parentOrganizationId: 'ROOT_ORGANIZATION_ID',
      legacyViolationEnabled: null,
      allowLegacyViolationOverride: true,
      repositoryConnectionEnabled: null,
      allowRepositoryConnectionOverride: true,
      artifactoryConnectionEnabled: null,
      allowArtifactoryConnectionOverride: true,
    },
    legacyViolationEnabled: null,
    repositoryConnectionEnabled: null,
    artifactoryConnectionEnabled: null,
  },
  stageId: 'build',
  commitHash: 'e57f8332cb061ddd2f8e064f18020be1deaa864a',
  initiator: 'admin',
  scanTriggerType: 'Continuous Integration',
  totalRisk: 419,
  reevaluation: false,
  forMonitoring: false,
};

export const waiveTransitiveData = {
  componentPolicyWaivers: [],
};

export const noTransitiveViolationsData = {
  componentIdentifier: {
    format: 'maven',
    coordinates: {
      artifactId: 'ACME-web',
      classifier: '',
      extension: 'jar',
      groupId: 'org.example',
      version: '1.0-SNAPSHOT',
    },
  },
  packageUrl: 'pkg:maven/org.example/ACME-web@1.0-SNAPSHOT?type=jar',
  hash: 'd0833800c2505e2f54d9',
  displayName: 'org.example : ACME-web : 1.0-SNAPSHOT',

  isInnerSource: true,
  transitivePolicyViolations: [],
};
