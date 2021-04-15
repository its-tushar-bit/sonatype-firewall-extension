/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Provides a globally cached version of role mappings for the current context. Note that get() callers should not
 * modify the returned object as it is shared.
 */
export default function RoleMappingService(
  CachedServiceFactory,
  $http,
  CLMContextLocations
) {
  var serviceCache = CachedServiceFactory.create(
    CLMContextLocations.getRoleMappingUrl
  );

  serviceCache.put = function (roleId, contents) {
    return $http
      .put(CLMContextLocations.getRoleMappingUrl(roleId), contents)
      .then(function () {
        // update shared copy
        return serviceCache.get().then(function (roleMappings) {
          roleMappings.membersByRole.forEach(function (role) {
            if (role.roleId === roleId && role.membersByOwner.length > 0) {
              role.membersByOwner[0].members = contents;
            }
          });
        });
      });
  };

  return serviceCache;
}
RoleMappingService.$inject = [
  'cached.service.factory',
  '$http',
  'CLMContextLocations',
];
