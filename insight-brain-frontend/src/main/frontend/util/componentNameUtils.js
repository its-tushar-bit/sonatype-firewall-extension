/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { dropRepeatsWith, dropLastWhile, reject, pipe, prop, propEq, map, join } from 'ramda';

const deriveComponentNameFromDisplayName = pipe(prop('parts'), map(prop('value')), join(''));
const deriveComponentNameFromFilenames = join(', ');

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
  (displayName && deriveComponentNameFromDisplayName(displayName)) ||
  filename ||
  (filenames && deriveComponentNameFromFilenames(filenames)) ||
  'Unknown';

export const getArtifactName = ({ displayName, filename }) => prop('name', displayName) || filename || 'Unknown';

export const getComponentNameWithoutVersion = ({ displayName, filename, filenames }) =>
  (displayName && deriveComponentNameFromDisplayNameWithoutVersion(displayName)) ||
  filename ||
  (filenames && deriveComponentNameFromFilenames(filenames)) ||
  'Unknown';
