/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function PolicyEditorConstraintsController(ConstraintStore) {
  var vm = this;

  vm.conditionString = conditionString;
  vm.addConstraint = addConstraint;
  vm.addCondition = addCondition;
  vm.deleteCondition = deleteCondition;
  vm.deleteConstraint = deleteConstraint;
  vm.conditionTypesMap = undefined;
  vm.conditionTypes = undefined;
  vm.doLoad = doLoad;
  vm.editConstraintMap = {};
  vm.constraintOperatorOptions = [
    {
      operator: 'OR',
      name: 'any'
    }, {
      operator: 'AND',
      name: 'all'
    }
  ];
  vm.updateConditionType = updateConditionType;
  vm.getEmptyOptionCondition = getEmptyOptionCondition;
  vm.loadError = undefined;

  vm.doLoad();

  function conditionString(condition) {
    var operator,
        value;

    switch (condition.conditionTypeId) {
      case 'AgeInDays':
        value = parseDays(condition.value);
        break;
      case 'Label':
        value = getAvailableValue('label');
        break;
      case 'License':
        value = getAvailableValue('shortDisplayName');
        break;
      case 'License Threat Group':
      case 'SecurityVulnerabilityStatus':
      case 'LicenseStatus':
      case 'MatchState':
      case 'ComponentCategory':
      case 'HygieneRating':
      case 'IntegrityRating':
      case 'DependencyType':
      case 'SecurityVulnerabilityCategory':
        value = getAvailableValue('name');
        break;
      default:
        value = condition.value;
        break;
    }

    switch (condition.operator) {
      case '=':
        operator = 'equals';
        break;
      case '<=':
        operator = 'less than or equals';
        break;
      case '>=':
        operator = 'greater than or equals';
        break;
      case '<':
        operator = 'less than';
        break;
      case '>':
        operator = 'greater than';
        break;
      default :
        operator = condition.operator;
        break;
    }

    return vm.conditionTypesMap[condition.conditionTypeId].name + ' ' + operator + (value ? (' ' + value) : '');

    function parseDays(days) {
      return days % 365 === 0 ? days / 365 + ' Years' : days % 30 === 0 ? days / 30 + ' Months' : days % 7 ===
      0 ? days / 7 + ' Weeks' : days + ' Days';
    }

    function getAvailableValue(valueParam) {
      var result = '';

      vm.conditionTypesMap[condition.conditionTypeId].valueType.availableValues.some(function(availableValue) {
        if (availableValue.id === condition.value) {
          result = availableValue[valueParam];
          return true;
        }
      });

      return result;
    }
  }

  function doLoad() {
    ConstraintStore.get().then(function(constraintStore) {
      var typeValues = {};
      var allConditionTypes = constraintStore[0];
      vm.conditionTypes = allConditionTypes.filter(type => type.enabled);
      vm.conditionTypesMap = {};

      constraintStore[1].forEach(function(typeValue) {
        typeValues[typeValue.id] = typeValue;
      });

      allConditionTypes.forEach(function(type) {
        type.valueType = type.valueTypeId ? typeValues[type.valueTypeId] : null;
        vm.conditionTypesMap[type.id] = type;
      });

      if (vm.isNewPolicy) {
        vm.editConstraintMap[vm.constraints[0].id] = true;
      }
    }, function(error) {
      vm.loadError = error;
    });

    delete vm.loadError;
  }

  function updateConditionType(condition) {
    var conditionType = vm.conditionTypesMap[condition.conditionTypeId];
    condition.operator = conditionType.supportedOperators[0];
    condition.value = null;

    if (conditionType.valueType) {
      var availableValues = conditionType.valueType.availableValues;
      condition.value = (availableValues && availableValues.length > 0) ? availableValues[0].id : null;
    }
  }

  function getEmptyOptionCondition(condition) {
    if (!vm.conditionTypesMap[condition.conditionTypeId].enabled) {
      return vm.conditionTypesMap[condition.conditionTypeId].name;
    }
    return 'None Selected';
  }

  function deleteCondition(constraint, conditionIndex) {
    constraint.conditions.splice(conditionIndex, 1);
  }

  function addCondition(constraint) {
    var newCondition = {
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: null
    };

    constraint.conditions.push(newCondition);
  }

  function deleteConstraint(constraintIndex) {
    vm.constraints.splice(constraintIndex, 1);
  }

  function addConstraint() {
    var newConstraint = {
      id: '' + new Date().getTime(),
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than'
        }
      ],
      operator: 'OR'
    };

    vm.editConstraintMap[newConstraint.id] = true;
    vm.constraints.push(newConstraint);
  }
}

PolicyEditorConstraintsController.$inject = ['ConstraintStore'];
