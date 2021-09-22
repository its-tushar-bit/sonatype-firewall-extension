/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNil, join, pick, startsWith, toUpper } from 'ramda';

export const processAuditRecord = (record) => ({
  user: 'anonymous', // default value, usually overridden by the `pick` below
  action: statusToActionMap[record.status] || record.status,
  detail: record.filename === 'security.json' ? createSecurityDetails(record) : createLicenseDetails(record),

  // some audit entries use null when there isn't a comment while others use a blank string.  Normalize to prevent
  // confusing sorting
  comment: record.comment || '',
  ...pick(['time', 'user'], record),
});

/**
 * The `action` column is basically based on the `status` from the backend, but some statuses
 * get mapped to different words in the display instead of being passed straight through
 */
const statusToActionMap = {
  Open: 'Reopened',
  'Not Applicable': 'Ignored',
  Overridden: 'Overrode',
};

function createSecurityDetails(record) {
  const { source, reference } = record,
    referenceIncludesSource = !isNil(source) && startsWith(toUpper(source), toUpper(reference)),
    refString = referenceIncludesSource ? reference : `${source}-${reference}`;

  return `Vulnerability ${refString}`;
}

function createLicenseDetails(record) {
  const { overriddenLicenses } = record;

  return overriddenLicenses ? `License as ${join(', ', overriddenLicenses)}` : 'License Analysis';
}

const PATHNAME_REGEX = /^(dependency:\/)?((.*?)\/)?([^/]+)$/;

// @visibleForTesting
export function parseOccurrencePathname(pathname) {
  const [
      ,
      /* overall match */ dependency /* dirname including delimiter */,
      ,
      dirname,
      originalBasename,
    ] = PATHNAME_REGEX.exec(pathname),
    isDependency = !!dependency;

  // component names which contains '/' are replaced with '\' by the Occurrence pathnames string in the backend. This is
  // to avoid considering them as part of basename. Replacing them back as how it should be after resolving base name -
  // CLM-12606
  const basename = originalBasename.replace(/\\/g, '/');

  return { isDependency, dirname, basename };
}
