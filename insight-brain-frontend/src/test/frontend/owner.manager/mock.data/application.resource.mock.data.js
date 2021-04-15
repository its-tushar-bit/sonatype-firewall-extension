/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default {
  getApplicationSummaryUrl: function () {
    return {
      id: 'fakeId',
      name: 'fakeName',
      organizationId: 'fakeOrdId',
      organizationName: 'fakeOrgName',
      publicId: 'fakePublicId',
      policyEvaluations: {
        build: { id: 'fakePolicyEvaluationId', scanId: 'fakeScanId' },
        'stage-release': { id: 'fakePolicyEvaluationId', scanId: 'fakeScanId' },
        release: { id: 'fakePolicyEvaluationId', scanId: 'fakeScanId' },
      },
    };
  },
  getApplicationUrl: function () {
    return {
      contact: null,
      id: 'fakeId',
      name: 'fakeName',
      organizationId: 'fakeOrdId',
      organizationName: 'fakeOrgName',
      publicId: 'fakePublicId',
    };
  },
};
