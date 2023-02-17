/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { mapObjIndexed, prop, omit, curryN, test, includes, equals, isNil, reject, values, forEach } from 'ramda';
import {
  validateNonEmpty,
  validateMinMax,
  validatePatternMatch,
  combineValidators,
} from 'MainRoot/util/validationUtil';

function parseDays(days) {
  return days % 365 === 0
    ? days / 365 + ' Years'
    : days % 30 === 0
    ? days / 30 + ' Months'
    : days % 7 === 0
    ? days / 7 + ' Weeks'
    : days + ' Days';
}

function getAvailableValue(valueParam, condition, conditionTypesMap) {
  let result = '';

  conditionTypesMap[condition.conditionTypeId].valueType.availableValues.some((availableValue) => {
    if (availableValue.id === condition.value.value) {
      result = availableValue[valueParam];
      return true;
    }
  });

  return result;
}

export function getCoordinatesValue(value) {
  const fields = omit(['format'], mapObjIndexed(prop('trimmedValue'), value));

  if (value.format === 'maven') {
    return `${value.format}:${fields.groupId}:${fields.artifactId}:${fields.version}:${fields.extension}:${fields.classifier}`;
  } else if (value.format === 'a-name') {
    return `${value.format}:${fields.name}:${fields.qualifier}:${fields.version}`;
  } else if (value.format === 'pypi') {
    return `${value.format}:${fields.name}:${fields.version}:${fields.qualifier}:${fields.extension}`;
  }
}

const constraintOperatorMap = new Map([
  ['=', 'equals'],
  ['<=', 'less than or equals'],
  ['>=', 'greater than or equals'],
  ['<', 'less than'],
  ['>', 'greater than'],
]);

const getConditionValue = (condition, conditionTypesMap) => {
  switch (condition.conditionTypeId) {
    case 'AgeInDays':
      return parseDays(condition.value.value);
    case 'Label':
      return getAvailableValue('label', condition, conditionTypesMap);
    case 'License':
      return getAvailableValue('shortDisplayName', condition, conditionTypesMap);
    case 'Coordinates':
      return getCoordinatesValue(condition.value);
    case 'License Threat Group':
    case 'SecurityVulnerabilityStatus':
    case 'LicenseStatus':
    case 'MatchState':
    case 'ComponentCategory':
    case 'HygieneRating':
    case 'IntegrityRating':
    case 'DependencyType':
    case 'SecurityVulnerabilityCategory':
    case 'SecurityVulnerabilitySource':
    case 'IacControlConditionType':
    case 'VulnerabilityGroup':
      return getAvailableValue('name', condition, conditionTypesMap);
    default:
      return condition.value?.value;
  }
};

export const conditionString = (condition, conditionTypesMap) => {
  const value = getConditionValue(condition, conditionTypesMap);
  const operator = constraintOperatorMap.has(condition.operator)
    ? constraintOperatorMap.get(condition.operator)
    : condition.operator;

  return `${conditionTypesMap[condition.conditionTypeId].name} ${operator}${value ? ' ' + value : ''}`;
};

export const dataTypeValidatorsMap = new Map([
  ['String', validateNonEmpty],
  [
    'Integer',
    combineValidators([
      validateNonEmpty,
      validateMinMax([0, 10], 'Value must be from 0 to 10'),
      validatePatternMatch(/^(\d|10)$/, 'Please enter a whole number'),
    ]),
  ],
  [
    'Float',
    combineValidators([
      validateNonEmpty,
      validateMinMax([0, 10], 'Value must be from 0 to 10'),
      validatePatternMatch(/^\d+(\.\d+)?$/, 'Please enter a decimal number'),
    ]),
  ],
]);

/* eslint-disable no-useless-escape */
const pkgRegExp = /^(pkg:)([a-z0-9\-\+\.]+)(\/(?:[a-z0-9\-\*\+\.\%\_]+))+(@[^?\s@]+)?(\?(?:&?[^=&@]*=[^=&@\?]+)+)?$/i;
/* eslint-enable no-useless-escape */
const packageUrlValueTypeValidator = combineValidators([
  validateNonEmpty,
  validatePatternMatch(pkgRegExp, 'Value must be a valid Package URL: pkg:type/name@version'),
]);

const percentageValueTypeValidator = combineValidators([
  validateNonEmpty,
  validateMinMax([0, 100], 'Value must be from 0 to 100'),
  validatePatternMatch(/^((\d{1,2})|(100))$/, 'Value must be from 0 to 100'),
]);

export const valueTypeIdValidatorMap = new Map([
  ['PercentageValueType', percentageValueTypeValidator],
  ['PackageUrlValueType', packageUrlValueTypeValidator],
]);

export const ageValidator = combineValidators([
  validateNonEmpty,
  validateMinMax([1, Number.MAX_SAFE_INTEGER], 'Minimum allowed value is 1'),
]);

export const withDefaultValue = ['qualifier', 'extension', 'classifier'];

// 'qualifier', 'extension', 'classifier' fields can be empty
// but if value is present it should not contain : symbol
export const validatePatternMatchAndEmptyValue = curryN(
  3,
  function validatePatternMatchAndEmptyValue(regex, message, val) {
    if (!val) {
      return null;
    }
    return test(regex, val) ? null : message;
  }
);

export const getCoordinatesValidator = (fieldName) => {
  const validator = includes(fieldName, withDefaultValue)
    ? [validatePatternMatchAndEmptyValue(/^[^:]+$/, '')]
    : [validateNonEmpty, validatePatternMatch(/^[^:]+$/, '')];

  return combineValidators(validator);
};

export const coordinatesTypes = {
  maven: ['groupId', 'artifactId', 'version', 'extension', 'classifier'],
  'a-name': ['name', 'qualifier', 'version'],
  pypi: ['name', 'version', 'qualifier', 'extension'],
};

export const coordinatesFormatOptions = ['maven', 'a-name', 'pypi'];

export const fieldTypeToPlaceholder = {
  groupId: 'Group ID',
  artifactId: 'Artifact ID',
  version: 'Version',
  extension: 'Extension',
  classifier: 'Classifier',
  qualifier: 'Qualifier',
  name: 'Name',
};

export const conditionsWithoutValue = [
  'Proprietary',
  'ProprietaryNameConflict',
  'SecurityVulnerabilityCustomRemediation',
];

export const getDisabledConditions = (conditionTypesMap) => {
  const keyPairs = mapObjIndexed(prop('enabled'), conditionTypesMap);
  return reject(isNil, values(mapObjIndexed((value, key) => (equals(value, false) ? key : null), keyPairs)));
};

export const getUnsupportedConditions = (conditionsMap, constraint) => {
  const disabled = getDisabledConditions(conditionsMap);
  const unsupported = new Set();

  forEach((condition) => {
    if (includes(condition.conditionTypeId, disabled)) {
      unsupported.add(conditionsMap[condition.conditionTypeId].name);
    }
  }, constraint.conditions);

  return unsupported;
};
