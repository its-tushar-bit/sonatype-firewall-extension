/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { mapObjIndexed, prop, omit, curryN, test, includes, equals, isNil, reject, values, forEach, any } from 'ramda';
import { DOES_NOT_EXIST_OPERATOR } from './constants';
import {
  validateNonEmpty,
  validateMinMax,
  validatePatternMatch,
  combineValidators,
} from 'MainRoot/util/validationUtil';
import {
  A_NAME,
  CARGO,
  COCOAPODS,
  COMPOSER,
  CONAN,
  CONDA,
  CRAN,
  GEM,
  GOLANG,
  HF_MODEL,
  MAVEN,
  NUGET,
  NPM,
  PECOFF,
  PUB,
  PYPI,
  RPM,
  SWID,
  SWIFT,
} from 'MainRoot/OrgsAndPolicies/formats';

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

  if (value.format === MAVEN) {
    return `${value.format}:${fields.groupId}:${fields.artifactId}:${fields.version}:${fields.extension}:${fields.classifier}`;
  } else if (value.format === A_NAME) {
    return `${value.format}:${fields.name}:${fields.qualifier}:${fields.version}`;
  } else if (value.format === PYPI) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.qualifier}:${fields.extension}`;
  } else if (value.format === NPM) {
    return `${value.format}:${fields.packageId}:${fields.version}`;
  } else if (value.format === COCOAPODS) {
    return `${value.format}:${fields.name}:${fields.version}`;
  } else if (value.format === CONAN) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.channel}:${fields.owner}`;
  } else if (value.format === CONDA) {
    return `${value.format}:${fields.channel}:${fields.name}:${fields.version}:${fields.build}:${fields.subdir}:${fields.type}`;
  } else if (value.format === COMPOSER) {
    return `${value.format}:${fields.namespace}:${fields.name}:${fields.version}`;
  } else if (value.format === CRAN) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.type}`;
  } else if (value.format === CARGO) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.type}`;
  } else if (value.format === GEM) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.platform}`;
  } else if (value.format === GOLANG) {
    return `${value.format}:${fields.name}:${fields.version}`;
  } else if (value.format === HF_MODEL) {
    return `${value.format}:${fields.repoId}:${fields.model}:${fields.version}:${fields.extension}:${fields.modelFormat}`;
  } else if (value.format === NUGET) {
    return `${value.format}:${fields.packageId}:${fields.version}`;
  } else if (value.format === PECOFF) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.namespace}`;
  } else if (value.format === PUB) {
    return `${value.format}:${fields.name}:${fields.version}`;
  } else if (value.format === RPM) {
    return `${value.format}:${fields.name}:${fields.version}:${fields.architecture}`;
  } else if (value.format === SWID) {
    return `${value.format}:${fields.namespace}:${fields.name}:${fields.version}:${fields.tag_id}:${fields.tag_version}:${fields.tag_creator_name}:${fields.tag_creator_regid}:${fields.patch}`;
  } else if (value.format === SWIFT) {
    return `${value.format}:${fields.name}:${fields.version}`;
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
    case 'SecurityVulnerabilityDetectionType':
    case 'SecurityVulnerabilityResearchType':
    case 'SecurityVulnerabilitySource':
    case 'IacControlConditionType':
    case 'VulnerabilityGroup':
    case 'AiModelContent':
    case 'KevStatus':
      return getAvailableValue('name', condition, conditionTypesMap);
    default:
      return condition.value?.value;
  }
};

export const conditionString = (condition, conditionTypesMap) => {
  // For "does not exist" operator, don't show a value
  const value = condition.operator === DOES_NOT_EXIST_OPERATOR ? '' : getConditionValue(condition, conditionTypesMap);
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
  [
    'Double',
    combineValidators([
      validateNonEmpty,
      validateMinMax([0, 100], 'Value must be from 0 to 100'),
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

export const constraintNameValidator = (constraints = [], value = '', id = '') => {
  const duplicationValidator = () => {
    const exists = any(
      (item) => item.name?.trimmedValue?.toLowerCase() === value.toLowerCase() && id !== item.id,
      constraints
    );
    return exists ? 'Name is already in use' : null;
  };

  return combineValidators([validateNonEmpty, duplicationValidator]);
};

export const withDefaultValue = {
  maven: ['extension', 'classifier'],
  'a-name': ['qualifier'],
  pypi: ['qualifier', 'extension'],
  npm: [],
  cocoapods: [],
  conan: ['channel', 'owner'],
  conda: ['channel', 'build', 'subdir', 'type'],
  composer: [],
  cran: ['type'],
  cargo: ['type'],
  gem: ['platform'],
  golang: [],
  'hf-model': [],
  nuget: [],
  pecoff: ['namespace'],
  pub: [],
  rpm: [],
  swid: ['namespace', 'tag_version', 'tag_creator_name', 'tag_creator_regid', 'patch'],
  swift: [],
};

export const optionalFields = {
  maven: ['classifier'],
  'a-name': ['qualifier'],
  pypi: ['qualifier', 'extension'],
  npm: [],
  cocoapods: [],
  conan: ['channel', 'owner'],
  conda: ['channel', 'build', 'subdir', 'type'],
  composer: [],
  cran: ['type'],
  cargo: ['type'],
  gem: ['platform'],
  golang: [],
  'hf-model': [],
  nuget: [],
  pecoff: ['namespace'],
  pub: [],
  rpm: [],
  swid: ['namespace', 'tag_version', 'tag_creator_name', 'tag_creator_regid', 'patch'],
  swift: [],
};
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

export const getCoordinatesValidator = (fieldName, type) => {
  const validator = includes(fieldName, optionalFields[type])
    ? [validatePatternMatchAndEmptyValue(/^[^:]+$/, '')]
    : [validateNonEmpty, validatePatternMatch(/^[^:]+$/, '')];

  return combineValidators(validator);
};

export const coordinatesTypes = {
  maven: ['groupId', 'artifactId', 'version', 'extension', 'classifier'],
  'a-name': ['name', 'qualifier', 'version'],
  pypi: ['name', 'version', 'qualifier', 'extension'],
  npm: ['packageId', 'version'],
  cocoapods: ['name', 'version'],
  conan: ['name', 'version', 'channel', 'owner'],
  conda: ['channel', 'name', 'version', 'build', 'subdir', 'type'],
  composer: ['namespace', 'name', 'version'],
  cran: ['name', 'version', 'type'],
  cargo: ['name', 'version', 'type'],
  gem: ['name', 'version', 'platform'],
  golang: ['name', 'version'],
  'hf-model': ['repoId', 'model', 'version', 'extension', 'modelFormat'],
  nuget: ['packageId', 'version'],
  pecoff: ['name', 'version', 'namespace'],
  pub: ['name', 'version'],
  rpm: ['name', 'version', 'architecture'],
  swid: ['namespace', 'name', 'version', 'tag_id', 'tag_version', 'tag_creator_name', 'tag_creator_regid', 'patch'],
  swift: ['name', 'version'],
};

// This determines the order of the formats in the form
export const coordinatesFormatOptions = [
  MAVEN,
  A_NAME,
  CARGO,
  COCOAPODS,
  COMPOSER,
  CONAN,
  CONDA,
  CRAN,
  GEM,
  GOLANG,
  HF_MODEL,
  NPM,
  NUGET,
  PECOFF,
  PUB,
  PYPI,
  RPM,
  SWID,
  SWIFT,
];

export const fieldTypeToPlaceholder = {
  groupId: 'Group ID',
  artifactId: 'Artifact ID',
  version: 'Version',
  extension: 'Extension',
  classifier: 'Classifier',
  qualifier: 'Qualifier',
  name: 'Name',
  packageId: 'Package ID',
  channel: 'Channel',
  owner: 'Owner',
  namespace: 'Namespace',
  type: 'Type',
  repoId: 'Repo ID',
  model: 'Model',
  modelFormat: 'Model Format',
  subdir: 'Subdir',
  build: 'Build',
  architecture: 'Architecture',
  platform: 'Platform',
  tag_id: 'Tag ID',
  tag_version: 'Tag Version',
  tag_creator_name: 'Tag Creator Name',
  tag_creator_regid: 'Tag Creator Regid',
  patch: 'Patch',
};

export const conditionsWithoutValue = [
  'Proprietary',
  'ProprietaryNameConflict',
  'SecurityVulnerabilityCustomRemediation',
  'ComponentEndOfLife',
  'DerivativeAiModel',
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
