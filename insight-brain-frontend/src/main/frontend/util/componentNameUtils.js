/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pipe, prop, map, join, find } from 'ramda';

const deriveComponentNameFromDisplayName = pipe(prop('parts'), map(prop('value')), join(''));
const deriveComponentNameFromFilenames = join(', ');
const deriveArtifactNameFromDisplayName = pipe(
    prop('parts'),
    find(({field}) => field === 'Artifact' || field === 'Name' || field === 'packageId'),
    prop('value'));

export const getComponentName = ({ displayName, filename, filenames }) =>
  displayName && deriveComponentNameFromDisplayName(displayName) ||
  filename ||
  filenames && deriveComponentNameFromFilenames(filenames) ||
  'Unknown';

export const getArtifactName = ({ displayName, filename }) =>
  displayName && deriveArtifactNameFromDisplayName(displayName) || filename || 'Unknown';
