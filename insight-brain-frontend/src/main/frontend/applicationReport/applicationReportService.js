/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  all,
  apply,
  compose,
  concat,
  contains,
  curry,
  either,
  filter,
  flatten,
  flip,
  into,
  isEmpty,
  groupBy,
  isNil,
  map,
  mapObjIndexed,
  pick,
  pipe,
  prop,
  propEq,
  reduceBy,
  sort,
  toPairs,
  values
} from 'ramda';

const flatMap = pipe(map, flatten),
    toKey = component => component.hash || (component.pathnames || []).join('\t'),
    nullHashCheck = ({ hash }) => !!hash && hash !== 'null',
    indexComponentsByKey = components => mapObjIndexed(([data]) => data, groupBy(toKey, components)),
    makeNonViolatingComponentEntry = component => ({
      ...component,
      policyThreatLevel: 0,
      policyName: 'None',
      waived: false,
      grandfathered: false
    });

/**
 * In this version, each entry in policyResult represents a component, with nested lists of violations
 * for each
 */
function makeViolationEntriesV3(policyResult, bomDataByKey) {
  function makeEntriesForComponent(component) {
    const key = toKey(component),
        bomComponent = bomDataByKey[key],
        makeEntryForViolation = violation => ({
          ...pick(['policyThreatLevel', 'policyName', 'waived', 'grandfathered'], violation),
          ...bomComponent
        });

    return map(makeEntryForViolation, component.allViolations);
  }

  return flatMap(makeEntriesForComponent, filter(nullHashCheck, policyResult.aaData));
}

/**
 * In these version, each entry in policyResult is a component, with separate lists for activeViolations
 * and waivedViolations
 */
function makeViolationEntriesV1V2(policyResult, bomDataByKey) {
  function makeEntriesForComponent(component) {
    const key = toKey(component),
        bomComponent = bomDataByKey[key],
        makeEntryForViolation = waived => violation => ({
          waived,
          grandfathered: false,
          ...pick(['policyThreatLevel', 'policyName'], violation),
          ...bomComponent
        });

    return concat(
        map(makeEntryForViolation(false), component.activeViolations),
        map(makeEntryForViolation(true), component.waivedViolations)
    );
  }

  return flatMap(makeEntriesForComponent, filter(nullHashCheck, policyResult.aaData));
}

/**
 * In these versions, each entry is a violation
 */
function makeViolationEntriesNoVersion(policyResult, bomDataByKey) {
  function makeEntryForViolation(violation) {
    const key = toKey(violation),
        bomComponent = bomDataByKey[key];

    return {
      waived: false,
      grandfathered: false,
      ...pick(['policyThreatLevel', 'policyName'], violation),
      ...bomComponent
    };
  }

  return map(makeEntryForViolation, filter(nullHashCheck, policyResult.aaData));
}

// A map of makeViolationEntries functions, indexed by policyResult version
const makeViolationEntriesMap = new window.Map([
  [3, makeViolationEntriesV3],
  [2, makeViolationEntriesV1V2],
  [1, makeViolationEntriesV1V2],
  [null, makeViolationEntriesNoVersion]
]);

const defaultParamValue = { aaData: [] };

export function createReportEntries(policyResult = defaultParamValue, bomResult = defaultParamValue,
                                    unknownJsResult = defaultParamValue) {

  // BOM (and unknownJS) records indexed by their key
  const bomDataByKey = indexComponentsByKey(concat(bomResult.aaData, unknownJsResult.aaData)),

      // select the right processing function for this version of the data
      makeViolationEntries = makeViolationEntriesMap.get(policyResult.version || null),

      // make entries for all violations
      violationEntries = makeViolationEntries(policyResult, bomDataByKey),
      violatingEntriesByKey = groupBy(toKey, violationEntries);

  function isKeyInactive([key]) {
    const violations = violatingEntriesByKey[key];

    return isNil(violations) || all(propEq('grandfathered', true), violations);
  }

  const nonViolatingBomData = map(prop(1), filter(isKeyInactive, toPairs(bomDataByKey))),
      nonViolatingComponentEntries = map(makeNonViolatingComponentEntry, nonViolatingBomData);

  return concat(violationEntries, nonViolatingComponentEntries);
}

/**
 * Take a list of all report entries and return a list of just the "aggregated" entries (ie one entry per component).
 * The violation selected for each component is the one with the highest threat level, that is unwaived and
 * ungrandfathered. If none are unwaived/ungrandfathered, a non-violating component entry is added for that component
 */
export function aggregateReportEntries(entries) {
  const waivedOrGrandfathered = either(prop('waived'), prop('grandfathered')),

      highestViolationReducer = (acc, entry) =>
        !acc || waivedOrGrandfathered(acc) || !waivedOrGrandfathered(entry) &&
          entry.policyThreatLevel > acc.policyThreatLevel ? entry : acc,

      highestViolationsByKey = reduceBy(highestViolationReducer, null, toKey, entries),
      waivedViolationTransformer = entry =>
        waivedOrGrandfathered(entry) ? makeNonViolatingComponentEntry(entry) : entry;

  return map(waivedViolationTransformer, values(highestViolationsByKey));
}

/**
 * Take a list of all report entries and return a new list of only entries that have allowed values for all properties
 * in the filterConfig
 * @param filters an object mapping from property name to list of allowed values
 */
export const filterReportEntries = curry(function filterReportEntries(filterConfig, entries) {
  const filterPairs = toPairs(filterConfig);

  if (isEmpty(filterPairs)) {
    return entries;
  }
  else {
    const hasAllowedPropValue = (propName, allowedValues) => entry => contains(entry[propName], allowedValues),

        // make a partially applied filter function for each property
        filters = map(([propName, allowedValues]) => filter(hasAllowedPropValue(propName, allowedValues)), filterPairs),
        overallFilter = apply(compose)(filters);

    return into([], overallFilter, entries);
  }
});

/**
 * Return a list of the specified entries sorted by the specified property, optionally in reverse
 */
export const sortReportEntries = curry(function sortReportEntries(sortProperty, reverse, entries) {
  if (sortProperty) {
    const propGetter = prop(sortProperty),
        sortFn = (a, b) => {
          const aProp = propGetter(a),
              bProp = propGetter(b);

          return aProp < bProp ? -1 :
            aProp > bProp ? 1 :
              0;
        },
        sorter = sort(reverse ? flip(sortFn) : sortFn);

    return sorter(entries);
  }
  else {
    return entries;
  }
});
