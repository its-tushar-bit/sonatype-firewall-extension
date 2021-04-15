/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pipe, prop, map, join } from 'ramda';

const deriveComponentNameFromDisplayName = pipe(prop('parts'), map(prop('value')), join(''));
const deriveComponentNameFromFilenames = join(', ');

export const getComponentName = ({ displayName, filename, filenames }) =>
  (displayName && deriveComponentNameFromDisplayName(displayName)) ||
  filename ||
  (filenames && deriveComponentNameFromFilenames(filenames)) ||
  'Unknown';

export const getArtifactName = ({ displayName, filename }) => prop('name', displayName) || filename || 'Unknown';
