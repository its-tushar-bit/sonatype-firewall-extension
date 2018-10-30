/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  apply,
  compose,
  concat,
  contains,
  curry,
  either,
  filter,
  flatten,
  flip,
  identity,
  groupBy,
  into,
  isNil,
  map,
  pick,
  pipe,
  prop,
  reduceBy,
  toLower,
  sortWith,
  toPairs,
  values
} from 'ramda';

import { isNilOrEmpty } from '../util/jsUtil';

const flatMap = pipe(map, flatten),
    toKey = component => component.hash || (component.pathnames || []).join('\t'),
    nullHashCheck = ({ hash }) => !!hash && hash !== 'null',
    indexComponentsByKey = reduceBy((acc, c) => c, null, toKey),
    makeNonViolatingComponentEntry = component => ({
      ...component,
      policyThreatLevel: 0,
      policyName: 'None',
      waived: false,
      grandfathered: false,
      derivedComponentName: deriveComponentNameFromDisplayName(component.displayName)
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
          ...bomComponent,
          derivedComponentName: deriveComponentNameFromDisplayName(bomComponent.displayName)
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
          ...bomComponent,
          derivedComponentName: deriveComponentNameFromDisplayName(bomComponent.displayName)
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
      ...bomComponent,
      derivedComponentName: deriveComponentNameFromDisplayName(bomComponent.displayName)
    };
  }

  return map(makeEntryForViolation, filter(nullHashCheck, policyResult.aaData));
}

function deriveComponentNameFromDisplayName(displayName) {
  return displayName.parts.map(p => p.field ? p.value.toLowerCase() : '').filter(s => s !== '').join('\u001F');
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

    return isNil(violations);
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
 * in the exactValueFilters and substringFilters
 * @param exactValueFilters an object mapping from property name to list of allowed values
 * @param substringFilters an object mapping from property name to substring to match.
 */
export const filterReportEntries = curry(function filterReportEntries(exactValueFilters, substringFilters, entries) {
  const overallFilter = compose(filterByExactValues(exactValueFilters), filterBySubstring(substringFilters));

  return into([], overallFilter, entries);
});

/**
 * A helper function that is the basis of filterByExactValues and filterBySubstring.
 * @param checkBuilder A function that takes a property value from the filterConfig object and
 * returns a function which takes the value of the corresponding property from a violation entry and determines whether
 * that violation entry passes or should be filtered
 * @param filterConfig The object mapping property names to values that specify how they should be
 * filtered (the objects that get passed into filterBuilder)
 */
const makeFilterTransducer = curry(function makeFilterTransducer(checkBuilder, filterConfig) {
  if (isNilOrEmpty(filterConfig)) {
    return identity;
  }
  else {
    // make a function which takes a violation and sees if the value of the specified property passes a
    // check built from the specified filterValues
    const makePropValueCheck = (propName, filterValue) => pipe(prop(propName), checkBuilder(filterValue)),

        // make a list-filtering function using a [propName, filterValue] tuple
        makeFilterFromPair = pipe(apply(makePropValueCheck), filter),
        filters = map(makeFilterFromPair, toPairs(filterConfig));

    return apply(compose)(filters);
  }
});

// `contains` can do both substring matching and exact-value-in-array matching, which are the two kinds we need. The
// first arg of contains is the thing to search for and the second is the thing within which to search for it.
const filterByExactValues = makeFilterTransducer(flip(contains));
const filterBySubstring = makeFilterTransducer(filterString => {
  const lowerCasedFilterString = toLower(filterString);

  return pipe(toLower, contains(lowerCasedFilterString));
});

/**
 * Return a list of the specified entries sorted by the specified property, optionally in reverse
 */
export const sortReportEntries = curry(function sortReportEntries(sortFields, entries) {
  if (!isNilOrEmpty(sortFields)) {
    const sorters = sortFields.map(f => {
      const reverse = f.indexOf('-') === 0,
          sortProperty = f.match(/\w+/)[0],
          propGetter = prop(sortProperty),
          sortFn = (a, b) => {
            const aProp = propGetter(a),
                bProp = propGetter(b);

            return aProp < bProp ? -1 :
              aProp > bProp ? 1 :
                0;
          };
      return reverse ? flip(sortFn) : sortFn;
    });
    return sortWith(sorters, entries);
  }
  else {
    return entries;
  }
});
