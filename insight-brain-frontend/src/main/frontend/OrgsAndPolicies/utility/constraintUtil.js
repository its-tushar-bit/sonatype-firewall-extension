/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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
    if (availableValue.id === condition.value) {
      result = availableValue[valueParam];
      return true;
    }
  });

  return result;
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
      return parseDays(condition.value);
    case 'Label':
      return getAvailableValue('label', condition, conditionTypesMap);
    case 'License':
      return getAvailableValue('shortDisplayName', condition, conditionTypesMap);
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
      return getAvailableValue('name', condition, conditionTypesMap);
    default:
      return condition.value;
  }
};

export const conditionString = (condition, conditionTypesMap) => {
  const value = getConditionValue(condition, conditionTypesMap);
  const operator = constraintOperatorMap.has(condition.operator)
    ? constraintOperatorMap.get(condition.operator)
    : condition.operator;

  return `${conditionTypesMap[condition.conditionTypeId].name} ${operator}${value ? ' ' + value : ''}`;
};
