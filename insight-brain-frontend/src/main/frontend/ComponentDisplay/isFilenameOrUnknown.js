/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * A shared utility used by both the angular and react implementations of component-display.  Not intended for
 * use outside of this folder
 *
 * @param the component
 * @return true if the component has no displayName or if the displayName is a filename
 */
export default function isFilenameOrUnknown({ displayName }) {
  return !displayName || isDisplayNameFilename(displayName);
}

function isDisplayNameFilename({ parts }) {
  return parts.length === 1 && parts[0].field === 'Filename';
}
