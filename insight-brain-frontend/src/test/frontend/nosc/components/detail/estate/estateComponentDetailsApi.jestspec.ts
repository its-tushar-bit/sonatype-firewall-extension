/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  buildEstateComponentDetailsRequest,
  mapEstateComponentDetailsResponse,
} from 'MainRoot/nosc/components/detail/estate/estateComponentDetailsApi';

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
            componentIdentifier: { format: 'maven' },
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
});
