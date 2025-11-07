/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getBaseUrl, uriTemplate } from '../util/urlUtil';

export function getOwnerImageUrl(owner) {
  const servicePath = owner.publicId ? 'application' : 'organization',
    id = window.encodeURIComponent(owner.publicId || owner.id);

  return `${getBaseUrl(window.location.href)}/rest/${servicePath}/icon/${id}`;
}

/*
 * @since 1.18.0
 */
export function getPermissionContextTestUrl(ownerType, ownerId) {
  var path = ownerType;
  if (ownerId) {
    path += '/' + ownerId;
  }
  return `${getBaseUrl(window.location.href)}/rest/user/permissions/${path}`;
}

export function getGlobalPermissionTestUrl() {
  return uriTemplate`/rest/user/permissions/global/global`;
}

export function getGlobalRoleMappingUrl() {
  return uriTemplate`/api/v2/roleMemberships/global/roles`;
}
