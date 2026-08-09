/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildEstateComponentDetailsRequest,
  buildEstateComponentVersionDetailsRequest,
  buildEstateComponentVersionsRequest,
  mapEstateComponentDetailsResponse,
  mapEstateComponentVersionHash,
} from 'MainRoot/nosc/components/detail/estate/estateComponentDetailsApi';

const COMPONENT_IDENTIFIER = {
  format: 'maven',
  coordinates: { groupId: 'org.example', artifactId: 'example', version: '1.0.0' },
};

describe('estateComponentDetailsApi', () => {
  it('posts hash-only component details request', () => {
    expect(buildEstateComponentDetailsRequest('deadbeef')).toEqual({
      components: [{ hash: 'deadbeef' }],
    });
  });

  it('maps the first HDS componentDetails row', () => {
    const mapped = mapEstateComponentDetailsResponse({
      componentDetails: [
        {
          matchState: 'exact',
          component: {
            hash: 'deadbeef',
            displayName: 'log4j-core 2.14.1',
            packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
            componentIdentifier: {
              format: 'maven',
              coordinates: { groupId: 'org.apache.logging.log4j', artifactId: 'log4j-core', version: '2.14.1' },
            },
          },
          licenseData: {
            status: 'Open',
            effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
          },
          securityData: {
            securityIssues: [{ reference: 'CVE-2021-44228', severity: 10 }],
          },
        },
      ],
    });
    expect(mapped).toEqual({
      hash: 'deadbeef',
      displayName: 'log4j-core 2.14.1',
      packageUrl: 'pkg:maven/org.apache.logging.log4j/log4j-core@2.14.1',
      format: 'maven',
      componentIdentifier: {
        format: 'maven',
        coordinates: { groupId: 'org.apache.logging.log4j', artifactId: 'log4j-core', version: '2.14.1' },
      },
      matchState: 'exact',
      licenseData: {
        status: 'Open',
        effectiveLicenses: [{ licenseId: 'Apache-2.0', licenseName: 'Apache 2.0' }],
      },
      securityIssues: [{ reference: 'CVE-2021-44228', severity: 10 }],
    });
  });

  it('returns null when HDS yields no componentDetails', () => {
    expect(mapEstateComponentDetailsResponse({ componentDetails: [] })).toBeNull();
    expect(mapEstateComponentDetailsResponse(undefined)).toBeNull();
  });

  it('builds component versions request from the catalog package URL', () => {
    expect(buildEstateComponentVersionsRequest({ packageUrl: 'pkg:maven/org.example/example@1.0.0' })).toEqual({
      packageUrl: 'pkg:maven/org.example/example@1.0.0',
    });
  });

  it('builds component versions request from the component identifier when package URL is absent', () => {
    expect(buildEstateComponentVersionsRequest({ componentIdentifier: COMPONENT_IDENTIFIER })).toEqual({
      componentIdentifier: COMPONENT_IDENTIFIER,
    });
  });

  it('builds a single sibling details request by replacing the package URL version', () => {
    expect(
      buildEstateComponentVersionDetailsRequest(
        { packageUrl: 'pkg:maven/org.example/example@1.0.0?type=jar' },
        '1.1.0'
      )
    ).toEqual({
      components: [{ packageUrl: 'pkg:maven/org.example/example@1.1.0?type=jar' }],
    });
  });

  it('builds a single sibling details request from the component identifier when package URL is absent', () => {
    expect(buildEstateComponentVersionDetailsRequest({ componentIdentifier: COMPONENT_IDENTIFIER }, '1.1.0')).toEqual({
      components: [
        {
          componentIdentifier: {
            format: 'maven',
            coordinates: { groupId: 'org.example', artifactId: 'example', version: '1.1.0' },
          },
        },
      ],
    });
  });

  it('maps the first hash from a single-version details response', () => {
    expect(
      mapEstateComponentVersionHash({
        componentDetails: [
          {
            component: {
              hash: 'known-hash',
              packageUrl: 'pkg:maven/org.example/example@1.1.0',
              componentIdentifier: {
                format: 'maven',
                coordinates: { groupId: 'org.example', artifactId: 'example', version: '1.1.0' },
              },
            },
          },
        ],
      })
    ).toBe('known-hash');
  });

  it('returns null when single-version details response has no hash', () => {
    expect(mapEstateComponentVersionHash({ componentDetails: [{ component: {} }] })).toBeNull();
    expect(mapEstateComponentVersionHash({ componentDetails: [] })).toBeNull();
  });
});
