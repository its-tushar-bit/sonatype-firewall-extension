/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

/**
 * Gets the URL for a security vulnerability
 */
export function getSvUrl(item) {
  if (item.url) {
    return item.url;
  } else if (item.source === 'osvdb') {
    return 'https://osv.dev/vulnerability/' + item.refId;
  } else if (item.source === 'cve') {
    return 'https://cve.mitre.org/cgi-bin/cvename.cgi?name=' + item.refId;
  } else {
    return null;
  }
}

/**
 * Gets the display name for a security vulnerability
 */
export function getSvName(issue) {
  let retVal = issue.refId.toUpperCase();
  if (retVal.indexOf(issue.source.toUpperCase()) !== 0) {
    retVal = issue.source.toUpperCase() + '-' + retVal;
  }
  return retVal;
}

/**
 * Processes security vulnerabilities from the API
 */
export function processSecurityVulnerabilities(vulnerabilities) {
  return (vulnerabilities || [])
    .map((item) => ({
      ...item,
      severity: item.severity !== null ? Math.floor(item.severity) : null,
    }))
    .sort((a, b) => {
      if (b.severity === null) {
        return a.severity === null ? 0 : -1;
      } else if (a.severity === null) {
        return 1;
      } else {
        return b.severity - a.severity;
      }
    });
}
