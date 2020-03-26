/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  join,
  map,
  omit,
  pipe,
  prop,
  sortBy,
  toPairs
} from 'ramda';

// use U+001F UNIT SEPARATOR to separate coordinate field names from values, and use U+001E RECORD SEPARATOR to
// separate the key/value pairs from each other
const coordinatesToString = pipe(
    toPairs,
    sortBy(prop(0)),
    map(join('\u001f')),
    join('\u001e')
);

/**
 * Takes component identifier object and returns a string representing the component identifier's value.
 * Equivalent component identifiers will result in equivalent strings, making the strings useful for constructing a map
 * keyed by component identifier.
 */
export function serializeComponentIdentifier(componentIdentifier) {
  const { format, coordinates } = componentIdentifier;
  return `${format}:${coordinatesToString(coordinates)}`;
}

/**
 * Takes component identifier object and returns a string representing the component identifier's value omitting
 * extension and classifier.
 * Equivalent component identifiers will result in equivalent strings, making the strings useful for constructing a map
 * keyed by component identifier.
 */
export function getDependencyInfoComponentId(componentIdentifier) {
  const { format, coordinates } = componentIdentifier,
      coordinatesString = coordinatesToString(omit(['extension', 'classifier'], coordinates));
  return `${format}:${coordinatesString}`;
}
