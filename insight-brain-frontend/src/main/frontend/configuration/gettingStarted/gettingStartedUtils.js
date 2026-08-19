/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const mkLimit = (name, count) => ({ name, count });

export function getUserLimits(license) {
  return [
    license.licensedUsersToDisplay && mkLimit('Lifecycle', license.licensedUsersToDisplay),
    license.firewallUsersToDisplay && mkLimit('Firewall', license.firewallUsersToDisplay),
  ].filter(Boolean);
}
