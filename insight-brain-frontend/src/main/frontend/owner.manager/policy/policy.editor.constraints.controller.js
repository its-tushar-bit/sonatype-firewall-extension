/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSlice';
import {
  selectIsLoading,
  selectLoadError,
  selectEditConstraintMap,
  selectConditionTypesMap,
  selectConditionTypes,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSelectors';
import { conditionString } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

export default function PolicyEditorConstraintsController($ngRedux) {
  let vm = this;

  Object.assign(vm, {
    constraintOperatorOptions: [
      {
        operator: 'OR',
        name: 'any',
      },
      {
        operator: 'AND',
        name: 'all',
      },
    ],

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
        loadConstraint: actions.loadConstraint,
        updateEditConstraintId: actions.updateEditConstraintId,
      })(vm);

      vm.doLoad();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    doLoad() {
      vm.loadConstraint({ isNewPolicy: vm.isNewPolicy, constraints: vm.constraints });
    },

    conditionString,

    addConstraint() {
      const newConstraint = {
        id: `${new Date().getTime()}`,
        conditions: [
          {
            conditionTypeId: 'AgeInDays',
            operator: 'older than',
          },
        ],
        operator: 'OR',
      };

      vm.updateEditConstraintId(newConstraint.id);
      vm.constraints.push(newConstraint);
    },

    addCondition(constraint) {
      const newCondition = {
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: null,
      };

      constraint.conditions.push(newCondition);
    },

    deleteConstraint(constraintIndex) {
      vm.constraints.splice(constraintIndex, 1);
    },

    deleteCondition(constraint, conditionIndex) {
      constraint.conditions.splice(conditionIndex, 1);
    },

    getEmptyOptionCondition(condition) {
      if (!vm.conditionTypesMap[condition.conditionTypeId].enabled) {
        return vm.conditionTypesMap[condition.conditionTypeId].name;
      }
      return 'None Selected';
    },

    updateConditionType(condition) {
      const conditionType = vm.conditionTypesMap[condition.conditionTypeId];
      condition.operator = conditionType.supportedOperators[0];
      condition.value = null;

      if (conditionType.valueType) {
        const availableValues = conditionType.valueType.availableValues;
        condition.value = availableValues && availableValues.length > 0 ? availableValues[0].id : null;
      }
    },
  });
}

export const mapStateToThis = (state) => ({
  loadError: selectLoadError(state),
  loading: selectIsLoading(state),
  editConstraintMap: angular.copy(selectEditConstraintMap(state)),
  conditionTypesMap: angular.copy(selectConditionTypesMap(state)),
  conditionTypes: angular.copy(selectConditionTypes(state)),
});

PolicyEditorConstraintsController.$inject = ['$ngRedux'];
