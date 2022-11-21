/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { join, map, prop, sortBy, toPairs } from 'ramda';

/**
 * Takes component identifier object and returns a string representing the component identifier's value.
 * Equivalent component identifiers will result in equivalent strings, making the strings useful for constructing a map
 * keyed by component identifier.
 */
export function serializeComponentIdentifier(componentIdentifier) {
  const { format, coordinates } = componentIdentifier,
    // use U+001F UNIT SEPARATOR to separate coordinate field names from values, and use U+001E RECORD SEPARATOR to
    // separate the key/value pairs from each other
    coordinatePairStrings = map(join('\u001f'), sortBy(prop(0), toPairs(coordinates))),
    coordinatesString = join('\u001e', coordinatePairStrings);

  return `${format}:${coordinatesString}`;
}

export function stringifyComponentIdentifier(componentIdentifier, matchState) {
  const coordinates = !matchState || matchState === 'unknown' ? null : componentIdentifier.coordinates;
  return coordinates ? JSON.stringify({ format: componentIdentifier.format, coordinates }) : null;
}

export function stringifyPathName(componentIdentifier) {
  const coordinates = componentIdentifier.coordinates;
  return `${coordinates.artifactId}/${coordinates.groupId}/${coordinates.version}/${coordinates.artifactId}-${coordinates.version}.${coordinates.extension}`;
}
