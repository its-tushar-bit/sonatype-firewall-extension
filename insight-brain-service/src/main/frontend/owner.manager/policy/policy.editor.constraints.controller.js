/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorConstraintsController(ConstraintStore)
  {
    var vm = this;

    vm.conditionString = conditionString;
    vm.conditionTypes = undefined;
    vm.doLoad = doLoad;
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

      return vm.conditionTypes[condition.conditionTypeId].name + ' ' + operator + (value ? (' ' + value) : '');

      function parseDays(days) {
        return days % 365 === 0 ? days / 365 + ' Years' : days % 30 === 0 ? days / 30 + ' Months' : days + ' Days';
      }

      function getAvailableValue(valueParam) {
        var result = '';

        vm.conditionTypes[condition.conditionTypeId].valueType.availableValues.some(function(availableValue) {
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
        vm.conditionTypes = {};
        constraintStore[1].forEach(function(typeValue) {
          typeValues[typeValue.id] = typeValue;
        });

        constraintStore[0].forEach(function(type) {
          type.valueType = type.valueTypeId ? typeValues[type.valueTypeId] : null;
          vm.conditionTypes[type.id] = type;
        });
      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
    }
  }

  PolicyEditorConstraintsController.$inject = ['ConstraintStore'];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.constraints.controller', PolicyEditorConstraintsController);

}(angular));
