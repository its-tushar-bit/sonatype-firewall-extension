/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  __,
  always,
  append,
  apply,
  both,
  chain,
  complement,
  compose,
  concat,
  contains,
  curry,
  defaultTo,
  either,
  filter,
  flatten,
  flip,
  identity,
  groupBy,
  gte,
  has,
  into,
  isNil,
  lte,
  join,
  map,
  mapObjIndexed,
  maxBy,
  pick,
  pipe,
  prop,
  propEq,
  reduce,
  reduceBy,
  reject,
  toLower,
  sortWith,
  toPairs,
  uniqBy,
  values
} from 'ramda';

import { isNilOrEmpty, multiGroupBy, setToArray } from '../util/jsUtil';
import { serializeComponentIdentifier, getDependencyInfoComponentId } from '../util/componentIdentifierUtils';
import { getComponentName } from '../util/componentNameUtils';
import { getDeclaredLicensesDisplay, getObservedLicensesDisplay } from './licenseDisplayUtils';
import DependencyInfoGenerator from './DependencyInfoGenerator';

const joinPathnames = join('\t'),
    toKey = component => component.hash || joinPathnames(component.pathnames || []),
    nullHashCheck = ({ hash }) => !!hash && hash !== 'null',
    indexBy = reduceBy((acc, c) => c, null),
    indexByKey = indexBy(toKey),
    makeNonViolatingComponentEntry = component => ({
      ...component,
      policyThreatLevel: 0,
      policyName: 'None',
      waived: false,
      grandfathered: false,
      derivedComponentName: deriveComponentName(component),
      derivedViolationState: 'notViolating'
    });

/**
 * In this version, each entry in policyResult represents a component, with nested lists of violations
 * for each.
 * Note that v3 violations have no `policyThreatCategory`
 */
function makeViolationEntriesV3Plus(policyResult, bomDataByKey) {
  function makeEntriesForComponent(component) {
    const key = toKey(component),
        bomComponent = bomDataByKey[key],
        makeEntryForViolation = violation => {
          const { waived, grandfathered } = violation;

          return {
            ...pick(['policyThreatLevel', 'policyName', 'policyThreatCategory', 'constraints'], violation),
            ...bomComponent,
            waived,
            grandfathered,
            derivedComponentName: deriveComponentName(bomComponent),
            derivedViolationState: deriveViolationState(waived, grandfathered)
          };
        };

    return map(makeEntryForViolation, component.allViolations);
  }

  return chain(makeEntriesForComponent, filter(nullHashCheck, policyResult.aaData));
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
          derivedComponentName: deriveComponentName(bomComponent),
          derivedViolationState: deriveViolationState(waived, false)
        });

    return concat(
        map(makeEntryForViolation(false), component.activeViolations),
        map(makeEntryForViolation(true), component.waivedViolations)
    );
  }

  return chain(makeEntriesForComponent, filter(nullHashCheck, policyResult.aaData));
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
      derivedComponentName: deriveComponentName(bomComponent),
      derivedViolationState: deriveViolationState(false, false)
    };
  }

  return map(makeEntryForViolation, filter(nullHashCheck, policyResult.aaData));
}

const deriveComponentName = pipe(getComponentName, toLower);

const getLicenseSortKey = (licenseObj) => {
  if (!licenseObj) {
    return '';
  }
  const observedLicenses = getObservedLicensesDisplay(licenseObj);
  return getDeclaredLicensesDisplay(licenseObj) +
      (observedLicenses ? ', ' + observedLicenses : '');
};

// Violation state is a combination of Waived and Grandfathered.  These two values need to be stored in the same
// field so that OR-based filtering can be performed on them.  If only their separate-field values were used, the
// current filtering engine could only do AND-based filtering on them.
const deriveViolationState = (waived, grandfathered) => waived && grandfathered ? 'waived+grandfathered' :
  waived ? 'waived' :
    grandfathered ? 'grandfathered' :
      'open';

// A map of makeViolationEntries functions, indexed by policyResult version
const violationEntryMakersByPolicyThreatsVersion = new window.Map([
  [5, makeViolationEntriesV3Plus],
  [4, makeViolationEntriesV3Plus],
  [3, makeViolationEntriesV3Plus],
  [2, makeViolationEntriesV1V2],
  [1, makeViolationEntriesV1V2],
  [null, makeViolationEntriesNoVersion]
]);

const addPartialMatchData = curry(function(partialMatchesByKey, entry) {
  const partialMatches = partialMatchesByKey[toKey(entry)];

  return partialMatches ? { ...entry, matchDetails: partialMatches.matchDetails } : entry;
});

function addDependencyInfoComponentId(entry) {
  const { componentIdentifier } = entry;
  if (componentIdentifier) {
    return { ...entry, dependencyInfoComponentId: getDependencyInfoComponentId(componentIdentifier) };
  }

  return entry;
}

const defaultParamValue = { aaData: [] };

export function createReportEntries(policyResult = defaultParamValue, bomResult = defaultParamValue,
                                    unknownJsResult = defaultParamValue, partialMatches = defaultParamValue,
                                    dependencies) {

  // BOM (and unknownJS) records indexed by their key
  const bomDataByKey = indexByKey(concat(bomResult.aaData, unknownJsResult.aaData)),
      partialMatchesByKey = indexByKey(partialMatches.aaData),
      dependencyInfoGenerator = DependencyInfoGenerator(dependencies),

      // select the right processing function for this version of the data
      makeViolationEntries = violationEntryMakersByPolicyThreatsVersion.get(policyResult.version || null),

      // make entries for all violations
      violationEntries = makeViolationEntries(policyResult, bomDataByKey),
      augmentViolationEntries = compose(
          map(addPartialMatchData(partialMatchesByKey)),
          map(addDependencyInfo),
          map(addDependencyInfoComponentId)
      ),
      augmentedViolationEntries = into([], augmentViolationEntries, violationEntries),
      violatingEntriesByKey = groupBy(toKey, augmentedViolationEntries);

  function isKeyInactive([key]) {
    const violations = violatingEntriesByKey[key];

    return isNil(violations);
  }

  function getDerivedDependencyType(dependencyInfo) {
    if (dependencyInfo) {
      if (dependencyInfo.isDirectDependency) {
        return 'direct';
      }
      else {
        return 'transitive';
      }
    }
    return 'unknown';
  }

  function addDependencyInfo(entry) {
    const dependencyInfo = dependencyInfoGenerator.getDependencyInfo(entry);
    const derivedDependencyType = getDerivedDependencyType(dependencyInfo);
    const entryWithDerivedDependencyType = { ...entry, derivedDependencyType };
    return dependencyInfo ? { ...entryWithDerivedDependencyType, dependencyInfo } : entryWithDerivedDependencyType;
  }

  const nonViolatingBomData = map(prop(1), filter(isKeyInactive, toPairs(bomDataByKey))),
      nonViolatingEntriesTransducer = compose(
          map(makeNonViolatingComponentEntry),
          map(addDependencyInfo),
          map(addDependencyInfoComponentId)
      ),
      nonViolatingComponentEntries = into([], nonViolatingEntriesTransducer, nonViolatingBomData);

  return concat(augmentedViolationEntries, nonViolatingComponentEntries);
}

export function createRawDataEntries(securityResult = defaultParamValue, licensesResult = defaultParamValue,
                                     bomResult = defaultParamValue, unknownJsResult = defaultParamValue) {

  const bomDataByComponentId = groupBy(serializeComponentId, bomResult.aaData);
  const licenseEntriesByComponentId = indexBy(serializeComponentId, licensesResult.aaData);

  // flatten all the bom entries for a given component id into one, creating a new property to aggregate all the hashes
  const flattenedBomDataByComponentId = map(
      reduce((acc, comp) => ({ ...acc, ...comp, hashes: append(comp.hash, acc.hashes) }), { hashes: [] }),
      bomDataByComponentId);

  const securityEntriesByKey = groupBy(toKey, securityResult.aaData);

  const reportRawData = mapObjIndexed(({ hashes, ...oneBomData }, componentId) => {

    // distinct security entries associated with any detected hash of this component id
    const securityEntries = pipe(
            chain(hash => securityEntriesByKey[hash] || []),
            uniqBy(prop('reference'))
        )(hashes),
        license = licenseEntriesByComponentId[componentId],
        derivedComponentName = deriveComponentName(oneBomData);

    if (securityEntries.length) {
      return map((oneSecurityEntry) => ({
        ...oneBomData,
        derivedComponentName,
        license,
        securityCode: oneSecurityEntry.reference,
        cvssScore: oneSecurityEntry.score,
        url: oneSecurityEntry.url,
        licenseSortKey: getLicenseSortKey(license),
        source: oneSecurityEntry.source
      }), securityEntries);
    }
    else {
      return {
        ...oneBomData,
        derivedComponentName,
        license,
        licenseSortKey: getLicenseSortKey(license)
      };
    }
  }, flattenedBomDataByComponentId);

  const bomRawData = flatten(values(reportRawData));

  const allRawDeportRawData = bomRawData.concat(map(oneUnknownJsResult => ({
    ...oneUnknownJsResult,
    derivedComponentName: deriveComponentName(oneUnknownJsResult)
  }), unknownJsResult.aaData));

  return allRawDeportRawData;
}

/**
 * Takes an object that has a component identifier and returns a string representing the component identifier's value.
 * Equivalent component identifiers will result in equivalent strings, making the strings useful for constructing a map
 * keyed by component identifier. Also includes a fallback to the pathnames property if there is no component
 * identifier. If pathnames aren't available either, returns null
 */
function serializeComponentId({ componentIdentifier, pathnames }) {
  if (componentIdentifier) {
    return serializeComponentIdentifier(componentIdentifier);
  }
  else if (pathnames) {
    return `pathnames:${joinPathnames(pathnames)}`;
  }
  else {
    return null;
  }
}

function highestViolationReducer(highestViolationSoFar, violation) {
  const isActive = complement(either(isNil, either(prop('waived'), prop('grandfathered')))),
      activeViolations = filter(isActive, [highestViolationSoFar, violation]),
      highestActiveViolation = activeViolations.length < 2 ?
        activeViolations[0] : apply(maxBy(prop('policyThreatLevel')))(activeViolations);

  // return the highest active violation, or if there isn't one, merge the inactive violations
  if (highestActiveViolation) {
    return highestActiveViolation;
  }
  else {
    const waived = (highestViolationSoFar && highestViolationSoFar.waived) || violation.waived,
        grandfathered = (highestViolationSoFar && highestViolationSoFar.grandfathered) || violation.grandfathered;

    return {
      ...violation,
      policyThreatLevel: 0,
      policyName: 'None',
      waived,
      grandfathered,
      derivedViolationState: deriveViolationState(waived, grandfathered)
    };
  }

}

const unsetWaivedAndGrandfatheredOnViolatingEntry = entry =>
  entry.policyThreatLevel === 0 ? entry : { ...entry, waived: false, grandfathered: false };

/**
 * Take a list of all report entries and return a list of just the "aggregated" entries (ie one entry per component).
 * The violation selected for each component is the one with the highest threat level, that is unwaived and
 * ungrandfathered. If none are unwaived/ungrandfathered, a non-violating component entry is added for that component
 */
export const aggregateReportEntries = pipe(
    reduceBy(highestViolationReducer, null, toKey),
    values,

    // waived and grandfathered indicators should only be shown on non-violating components
    map(unsetWaivedAndGrandfatheredOnViolatingEntry)
);

/**
 * Take a list of all report entries and return a new list of only entries that have allowed values for all properties
 * in the exactValueFilters and substringFilters
 * @param exactValueFilters an object mapping from property name to Set of allowed values
 * @param substringFilters an object mapping from property name to substring to match.
 */
export const filterReportEntries = curry(function filterReportEntries(exactValueFilters, substringFilters,
                                                                      rawDataNumericFilters, entries) {
  const overallFilter = compose(filterByExactValues(exactValueFilters), filterBySubstring(substringFilters),
      filterBetweenValues(rawDataNumericFilters));

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
const filterByExactValues = makeFilterTransducer(allowedValues =>
  // if `allowedValues` is empty, do no filtering
  allowedValues.size ? contains(__, setToArray(allowedValues)) : always(true)
);

const filterBySubstring = makeFilterTransducer(filterString => {
  const lowerCasedFilterString = toLower(filterString);

  return pipe(defaultTo(''), toLower, contains(lowerCasedFilterString));
});

const filterBetweenValues = makeFilterTransducer(([min, max]) => {
  return (!min && !max) ? always(true) : both(gte(__, min || 0), lte(__, max || 10));
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
  }
  else {
    return entries;
  }
});

/**
 * Combine policy and vulnerability information (matching via component id and security issue id)
 * to get policy-oriented information for each vulnerability.
 * policyEntries must be from a V5+ policythreats.json
 */
export function getVulnerabilities(policyEntries, rawDataEntries) {
  const getVulnIdsFromPolicyEntry = pipe(
          prop('constraints'),
          defaultTo([]),
          into([], compose(
              chain(prop('conditions')),
              map(prop('conditionTriggerReference')),
              reject(isNil),
              filter(propEq('type', 'SECURITY_VULNERABILITY_REFID')),
              map(prop('value'))
          ))
      ),

      // map from CVE -> list of policy entries that reference that CVE
      policyEntriesBySecurityCode = multiGroupBy(getVulnIdsFromPolicyEntry, policyEntries),

      // two level map: CVE -> Component Id -> highest threat policy entry for that CVE and component
      highestPolicyEntryBySecurityCodeByComponentId =
          map(reduceBy(highestViolationReducer, null, serializeComponentId), policyEntriesBySecurityCode),

      // make a vulnerability entry by matching a raw data entry with a policy entry. If there is no policy
      // entry, null is returned because non-violating vulnerabilities are not to be included
      mkVulnerabilityEntry = rawDataEntry => {
        const { securityCode } = rawDataEntry,
            serializedComponentId = serializeComponentId(rawDataEntry),
            policyEntry = pipe(
                prop(securityCode),
                defaultTo({}),
                prop(serializedComponentId)
            )(highestPolicyEntryBySecurityCodeByComponentId);

        return policyEntry ? {
          ...pick(['policyThreatLevel', 'waived', 'grandfathered'], policyEntry),
          ...rawDataEntry,
          violationSortState: vulnerabilitySortStateMap[policyEntry.derivedViolationState],
          key: `${serializedComponentId}\u001D${securityCode}`
        } : null;
      };

  return into([], compose(
      filter(has('securityCode')),
      map(mkVulnerabilityEntry),
      reject(isNil)
  ), rawDataEntries);
}

const vulnerabilitySortStateMap = {
  'open': 0,
  'notViolating': 1,
  'waived': 2,
  'waived+grandfathered': 3,
  'grandfathered': 4
};
