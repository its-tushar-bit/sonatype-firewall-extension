/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function PolicyViolationsServiceProvider(
  OwnerContext,
  RepositoryPolicyViolationsService,
  CiPolicyViolationsService
) {
  return {
    get: function () {
      return OwnerContext.ownerType === 'repository'
        ? RepositoryPolicyViolationsService.get()
        : CiPolicyViolationsService.get();
    },
  };
}
PolicyViolationsServiceProvider.$inject = [
  'OwnerContext',
  'RepositoryPolicyViolationsService',
  'CiPolicyViolationsService',
];
