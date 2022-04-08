/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSlice';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';

import {
  selectIsLoading,
  selectLoadError,
  selectEditConstraintMap,
  selectConditionTypesMap,
  selectConditionTypes,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesConstraintSelectors';
import { conditionString } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import { remove } from 'ramda';

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
        updateEditConstraintId: actions.updateEditConstraintId,
        addConstraintAction: policyActions.addConstraint,
        deleteConstraintAction: policyActions.deleteConstraint,
        setConstraintName: policyActions.setConstraintName,
        addConditionAction: policyActions.addCondition,
        deleteConditionAction: policyActions.deleteCondition,
        setConstraintCondition: policyActions.setConstraintCondition,
        setConstraintOperator: policyActions.setConstraintOperator,
        setConditionOperator: policyActions.setConditionOperator,
        setConditionValue: policyActions.setConditionValue,
      })(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
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
      vm.addConstraintAction([...vm.constraints, newConstraint]);
    },

    deleteConstraint(constraintIndex) {
      const numberOfElementsToRemove = 1;
      const updatedConstraints = remove(constraintIndex, numberOfElementsToRemove, vm.constraints);

      vm.deleteConstraintAction(updatedConstraints);
    },

    addCondition(constraintIndex) {
      const newCondition = {
        conditionTypeId: 'AgeInDays',
        operator: 'older than',
        value: null,
      };
      const updatedConditions = [...vm.constraints[constraintIndex].conditions, newCondition];

      vm.addConditionAction({ constraintIndex, value: updatedConditions });
    },

    deleteCondition(constraintIndex, conditionIndex) {
      const numberOfElementsToRemove = 1;
      const updatedConditions = remove(
        conditionIndex,
        numberOfElementsToRemove,
        vm.constraints[constraintIndex].conditions
      );

      vm.deleteConditionAction({ constraintIndex, conditionIndex, value: updatedConditions });
    },

    getEmptyOptionCondition(condition) {
      if (!vm.conditionTypesMap[condition.conditionTypeId].enabled) {
        return vm.conditionTypesMap[condition.conditionTypeId].name;
      }
      return 'None Selected';
    },
    getConditionValue(conditionType) {
      let value = null;

      if (conditionType.valueType) {
        const availableValues = conditionType.valueType.availableValues;
        value = availableValues && availableValues.length > 0 ? availableValues[0].id : null;
      }

      return value;
    },
    onConditionTypeIdChange(constraintIndex, conditionIndex) {
      const updatedConditionTypeId = vm.constraints[constraintIndex].conditions[conditionIndex].conditionTypeId;
      const conditionType = vm.conditionTypesMap[updatedConditionTypeId];

      const updatedCondition = {
        conditionTypeId: updatedConditionTypeId,
        operator: conditionType.supportedOperators[0],
        value: vm.getConditionValue(conditionType),
      };

      vm.setConstraintCondition({ constraintIndex, conditionIndex, value: updatedCondition });
    },
    onConstraintNameChange(constraintIndex) {
      vm.setConstraintName({ constraintIndex, value: vm.constraints[constraintIndex].name });
    },
    onConstraintOperatorChange(constraintIndex) {
      vm.setConstraintOperator({ constraintIndex, value: vm.constraints[constraintIndex].operator });
    },
    onConditionOperatorChange(constraintIndex, conditionIndex) {
      const updatedOperator = vm.constraints[constraintIndex].conditions[conditionIndex].operator;

      vm.setConditionOperator({
        constraintIndex,
        conditionIndex,
        value: updatedOperator,
      });
    },
    onConditionValueChange(constraintIndex, conditionIndex) {
      const updatedValue = vm.constraints[constraintIndex].conditions[conditionIndex].value;

      vm.setConditionValue({
        constraintIndex,
        conditionIndex,
        value: updatedValue,
      });
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
