/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { dropRepeatsWith, dropLastWhile, reject, pipe, prop, propEq, map, join } from 'ramda';

const deriveComponentNameFromDisplayName = pipe(prop('parts'), map(prop('value')), join(''));
const deriveComponentNameFromFilenames = join(', ');
const isPathname = (displayName) =>
  prop('parts')(displayName)?.length === 1 && prop('parts')(displayName)[0]?.field === 'Pathname';

// Check all displayName parts, removing parts with "Version"
// field and removing duplicates without a field property (":"),
// map remaining values to a new string
const deriveComponentNameFromDisplayNameWithoutVersion = pipe(
  prop('parts'),
  reject(propEq('field', 'Version')),
  reject(propEq('field', 'version')),
  dropRepeatsWith((a, b) => a.field === undefined && b.field === undefined),
  dropLastWhile(propEq('field', undefined)),
  map(prop('value')),
  join('')
);

export const getComponentName = ({ displayName, filename, filenames }) =>
  (isPathname(displayName) && getFilenameFromPath(deriveComponentNameFromDisplayName(displayName))) ||
  (displayName && deriveComponentNameFromDisplayName(displayName)) ||
  filename ||
  (filenames && deriveComponentNameFromFilenames(filenames)) ||
  'Unknown';

export const getArtifactName = ({ displayName, filename }) => prop('name', displayName) || filename || 'Unknown';

/**
 * Returns the name of the file from a path.
 *
 * @param {string} filenamePath name of the component
 * @returns {string} Return the name of the file in case the filenamePath is a path.
 */
export const getFilenameFromPath = (filenamePath) => {
  // eslint-disable-next-line no-useless-escape
  const pathRegex = /(^.*[\\\/])/;
  // checks if the name of the component is a path
  if (pathRegex.test(filenamePath)) {
    // if the name is a path return just the name of the file
    return filenamePath.replace(pathRegex, '');
  }
  return filenamePath;
};

export const getComponentNameWithoutVersion = ({ displayName, filename, filenames }) =>
  (displayName && deriveComponentNameFromDisplayNameWithoutVersion(displayName)) ||
  filename ||
  (filenames && deriveComponentNameFromFilenames(filenames)) ||
  'Unknown';
