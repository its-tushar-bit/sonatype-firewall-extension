/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function getReleaseVersion(clmServerVersion) {
  const trimmedClmServerVersion = clmServerVersion?.trim();
  if (!trimmedClmServerVersion) {
    throw new TypeError(`Cannot determine release version from '${clmServerVersion}'.`);
  }
  const serverVersionWithoutQualifier = trimmedClmServerVersion.split('-')[0];
  const serverVersionParts = serverVersionWithoutQualifier.split('.');
  // remove major version if present
  if (serverVersionParts.length === 3) {
    serverVersionParts.shift();
  }
  const [minorVersion, patchVersion] = serverVersionParts;
  let result = minorVersion;
  if (patchVersion && patchVersion !== '0') {
    result += '.';
    result += patchVersion;
  }
  return result;
}

export { getReleaseVersion };
