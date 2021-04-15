/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { curry, flip, lensPath, sortWith, view } from 'ramda';
import { isNilOrEmpty } from './jsUtil';

/**
 * Return a list of the given items sorted by the specified properties, optionally in reverse
 */
export const sortItemsByFields = curry(function sortItemsByFields(sortFields, entries) {
  if (!isNilOrEmpty(sortFields)) {
    const sorters = sortFields.map((f) => {
      const reverse = f.indexOf('-') === 0,
        sortProperty = f.match(/(\w|\.)+/)[0],
        lens = lensPath(sortProperty.split('.')),
        propGetter = view(lens),
        sortFn = (a, b) => {
          const aProp = propGetter(a),
            bProp = propGetter(b);

          if (aProp === bProp) {
            return 0;
          }
          if (aProp === undefined) {
            return -1;
          }
          if (bProp === undefined) {
            return 1;
          }
          if (aProp < bProp) {
            return -1;
          }
          if (aProp > bProp) {
            return 1;
          }
          return 0;
        };
      return reverse ? flip(sortFn) : sortFn;
    });
    return sortWith(sorters, entries);
  } else {
    return entries;
  }
});

export const extractSortFieldName = (orderedField) => {
  if (orderedField && orderedField.indexOf('-') === 0) {
    return orderedField.substring(1);
  } else {
    return orderedField;
  }
};

export const sortColumn = (
  sortFunction,
  currentSortedColumnName,
  isCurrentColumnSortDescending,
  columnNameWithDefaultSortDirection
) => {
  const columnNameAscending = extractSortFieldName(columnNameWithDefaultSortDirection);
  if (currentSortedColumnName === columnNameAscending) {
    sortFunction(isCurrentColumnSortDescending ? [columnNameAscending] : [`-${columnNameAscending}`]);
  } else {
    sortFunction([columnNameWithDefaultSortDirection]);
  }
};

export const getColumnDirection = (currentSortedColumnName, isCurrentColumnSortDescending, columnName) => {
  const isThisColumnSorted = currentSortedColumnName === columnName,
    isAscending = isThisColumnSorted && !isCurrentColumnSortDescending,
    isDescending = isThisColumnSorted && isCurrentColumnSortDescending;

  return isAscending ? 'asc' : isDescending ? 'desc' : null;
};
