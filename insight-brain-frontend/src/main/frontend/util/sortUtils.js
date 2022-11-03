/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { curry, flip, lensPath, sortWith, view } from 'ramda';
import { isNilOrEmpty } from './jsUtil';
import { isWaiverAllVersionsOrExact } from './waiverUtils';
import { getComponentNameWithoutVersion } from './componentNameUtils';

/**
 * Return a list of the given items sorted by the specified properties, optionally in reverse
 */
export const sortItemsByFieldsWithComparator = curry(function sortItemsByFieldsWithComparator(
  comparator,
  sortFields,
  entries
) {
  if (!isNilOrEmpty(sortFields)) {
    const sorters = sortFields.map((f) => {
      const reverse = f.indexOf('-') === 0,
        sortProperty = f.match(/(\w|\.)+/)[0],
        lens = lensPath(sortProperty.split('.')),
        propGetter = view(lens),
        sortFn = (a, b) => comparator(propGetter(a), propGetter(b));
      return reverse ? flip(sortFn) : sortFn;
    });
    return sortWith(sorters, entries);
  } else {
    return entries;
  }
});

/**
 * Compares parameters a and b using the JavaScript greater-than and less-than operators.
 */
export const defaultComparator = (a, b) => {
  if (a === b) {
    return 0;
  }
  if (a === undefined) {
    return -1;
  }
  if (b === undefined) {
    return 1;
  }
  if (a < b) {
    return -1;
  }
  if (a > b) {
    return 1;
  }
  return 0;
};

/**
 * Compares parameters a and b using the JavaScript greater-than and less-than operators and taking null/undefined as infinity value.
 */
export const defaultComparatorWithNull = (a, b) => {
  const isAinfinity = a === null || a === undefined,
    isBinfinity = b === null || b === undefined;

  if (isAinfinity && isBinfinity) {
    return 0;
  }
  if (a && isBinfinity) {
    return -1;
  }
  if (isAinfinity && b) {
    return 1;
  }
  if (a < b) {
    return -1;
  }
  if (a > b) {
    return 1;
  }
  return 0;
};

/**
 * Same as defaultComparator but case insensitive.
 */
export const caseInsensitiveComparator = (a, b) => defaultComparator(a.toLowerCase(), b.toLowerCase());

/**
 * Returns sorted fields compared using the JavaScript greater-than and less-than operators.
 */
export const sortItemsByFields = sortItemsByFieldsWithComparator(defaultComparator);

/**
 * Returns sorted fields compared using the JavaScript greater-than and less-than operators and taking null as a infinity.
 */
export const sortItemsByFieldsWithNull = sortItemsByFieldsWithComparator(defaultComparatorWithNull);

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

export const sortWaiversByComponent = (sortFields, results = []) => {
  let groupWaiversByComponentName = {
    exactAndAllversion: [],
    allComponents: [],
    ['Unknown']: [],
  };

  results.forEach((waiver) => {
    let component = isWaiverAllVersionsOrExact(waiver) ? getComponentNameWithoutVersion(waiver) : 'allComponents';
    const waiverWithComponentName = { ...waiver, component };

    groupWaiversByComponentName[component]
      ? groupWaiversByComponentName[component].push(waiverWithComponentName)
      : groupWaiversByComponentName.exactAndAllversion.push(waiverWithComponentName);
  });

  Object.entries(groupWaiversByComponentName).forEach(([key, val]) => {
    groupWaiversByComponentName[key] = sortItemsByFieldsWithNull(sortFields, val);
  });

  const resultsSortedByGroup = [
    ...groupWaiversByComponentName.exactAndAllversion,
    ...groupWaiversByComponentName['allComponents'],
    ...groupWaiversByComponentName['Unknown'],
  ];

  resultsSortedByGroup.forEach((waiver) => delete waiver.component);

  return resultsSortedByGroup;
};

export const sortWaiversByFields = (sortFields, results) => {
  const sortFieldsWithExpiryTime = [...sortFields, 'expiryTime'];
  const primarySort = sortFieldsWithExpiryTime[0];
  let sortedResults = [];
  if (['component', '-component'].includes(primarySort)) {
    sortedResults = sortWaiversByComponent(sortFieldsWithExpiryTime, results);
  } else {
    sortedResults = sortItemsByFieldsWithNull(sortFieldsWithExpiryTime, results);
  }

  return sortedResults;
};
