/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { remove } from 'ramda';
import { faPlus } from '@fortawesome/free-solid-svg-icons';
import {
  NxH2,
  NxButton,
  NxFontAwesomeIcon,
  NxList,
  nxTextInputStateHelpers,
  NxDivider,
} from '@sonatype/react-shared-components';
import { actions as policyActions } from 'MainRoot/OrgsAndPolicies/policySlice';
import {
  selectEditConstraintMap,
  selectConditionTypesMap,
  selectConditionTypes,
} from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import { actions as constraintActions } from 'MainRoot/OrgsAndPolicies/constraintSlice';
import {
  selectCurrentPolicyConstraints,
  selectHasEditIqPermission,
  selectIsInherited,
} from 'MainRoot/OrgsAndPolicies/policySelectors';

import ReadOnlyConstraint from './ReadOnlyConstraint';
import EditableConstraint from './EditableConstraint';
import { selectIsRepositoriesRelated, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { ageValidator, constraintNameValidator } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

const { initialState: initUserInput } = nxTextInputStateHelpers;

export default function ConstraintsEditor() {
  const dispatch = useDispatch();

  const editConstraintMap = useSelector(selectEditConstraintMap);
  const conditionTypesMap = useSelector(selectConditionTypesMap);
  const conditionTypes = useSelector(selectConditionTypes);
  const constraints = useSelector(selectCurrentPolicyConstraints);
  const isInherited = useSelector(selectIsInherited);
  const isRepositoriesRelated = useSelector(selectIsRepositoriesRelated);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const isSbomManager = useSelector(selectIsSbomManager);
  const readOnly = isInherited || !hasEditIqPermission || isSbomManager;

  const setConstraint = (constraints) => dispatch(policyActions.setConstraint(constraints));
  const setCondition = (constraints) => dispatch(policyActions.setCondition(constraints));
  const updateEditConstraintId = (id) => dispatch(constraintActions.updateEditConstraintId(id));

  const setConstraintName = (constraint) => dispatch(policyActions.setConstraintName(constraint));
  const setConstraintOperator = (constraint) => dispatch(policyActions.setConstraintOperator(constraint));
  const setConstraintCondition = (constraint) => dispatch(policyActions.setConstraintCondition(constraint));
  const setConditionOperator = (constraint) => dispatch(policyActions.setConditionOperator(constraint));
  const setConditionValue = (constraint) => dispatch(policyActions.setConditionValue(constraint));
  const setConstraintCoordinatesFormat = (constraint) =>
    dispatch(policyActions.setConstraintCoordinatesFormat(constraint));
  const setConstraintCoordinatesInput = (constraint) =>
    dispatch(policyActions.setConstraintCoordinatesInput(constraint));
  const setConditionAgeValue = (constraint) => dispatch(policyActions.setConditionAgeValue(constraint));
  const setMultiInputConditionValue = (constraint) => dispatch(policyActions.setMultiInputConditionValue(constraint));

  function deleteConstraint(constraintIndex) {
    const updatedConstraints = remove(constraintIndex, 1, constraints);
    setConstraint(updatedConstraints);
  }

  function addConstraint() {
    const newConstraint = {
      id: `${new Date().getTime()}`,
      conditions: [
        {
          conditionTypeId: 'AgeInDays',
          operator: 'older than',
          value: initUserInput('', ageValidator),
        },
      ],
      operator: 'OR',
    };
    newConstraint.name = initUserInput('', constraintNameValidator(constraints, newConstraint.name, newConstraint.id));
    updateEditConstraintId(newConstraint.id);
    setConstraint([...constraints, newConstraint]);
  }

  function onConstraintNameChange(value, constraintIdx, id) {
    setConstraintName({ constraintIndex: constraintIdx, value, id });
  }

  function onConstraintOperatorChange(event, constraintIndex) {
    setConstraintOperator({ constraintIndex, value: event.currentTarget.value });
  }

  function addCondition(constraintIndex) {
    const newCondition = {
      conditionTypeId: 'AgeInDays',
      operator: 'older than',
      value: initUserInput('', ageValidator),
    };
    const updatedConditions = [...constraints[constraintIndex].conditions, newCondition];
    setCondition({ constraintIndex, value: updatedConditions });
  }

  function deleteCondition(constraintIndex, conditionIndex) {
    const updatedConditions = remove(conditionIndex, 1, constraints[constraintIndex].conditions);
    setCondition({ constraintIndex, conditionIndex, value: updatedConditions });
  }

  function getConditionValue(conditionType) {
    let value = '';

    if (conditionType.valueType) {
      const availableValues = conditionType.valueType.availableValues;
      value = availableValues && availableValues.length > 0 ? availableValues[0].id : '';
    }

    return value;
  }

  function onConditionTypeIdChange(event, constraintIndex, conditionIndex) {
    const { value: updatedConditionTypeId } = event.currentTarget;
    const conditionType = conditionTypesMap[updatedConditionTypeId];

    const updatedCondition = {
      conditionTypeId: updatedConditionTypeId,
      operator: conditionType.supportedOperators[0],
      value: getConditionValue(conditionType),
    };
    setConstraintCondition({ constraintIndex, conditionIndex, value: updatedCondition });
  }

  function onCoordinatesFormatChange(event, constraintIndex, conditionIndex) {
    const { value: updatedFormat } = event.currentTarget;
    setConstraintCoordinatesFormat({ constraintIndex, conditionIndex, value: updatedFormat });
  }

  function onCoordinatesInputChange(value, name, constraintIndex, conditionIndex, format) {
    setConstraintCoordinatesInput({ constraintIndex, conditionIndex, value, name, format });
  }

  function onConditionOperatorChange(event, constraintIndex, conditionIndex) {
    const { value: updatedOperator } = event.currentTarget;
    setConditionOperator({ constraintIndex, conditionIndex, value: updatedOperator });
  }

  function onConditionValueChange(event, constraintIndex, conditionIndex) {
    const { value: updatedValue } = event.currentTarget;
    setConditionValue({ constraintIndex, conditionIndex, value: updatedValue });
  }

  function onConditionAgeValueChange(value, constraintIndex, conditionIndex) {
    setConditionAgeValue({ constraintIndex, conditionIndex, value });
  }

  function onMultiValueInputChange(value, selectedCondition, constraintIndex, conditionIndex) {
    const {
      valueType: { dataType },
      valueTypeId,
    } = selectedCondition;

    setMultiInputConditionValue({ constraintIndex, dataType, valueTypeId, conditionIndex, value });
  }

  return (
    <div id="policy-edit-constraints">
      <NxH2>Constraints</NxH2>
      <NxList className="edit-constraint__form">
        {constraints.map((constraint, constraintIdx) => {
          if (editConstraintMap[constraint.id]) {
            return (
              <EditableConstraint
                key={constraint.id}
                constraint={constraint}
                constraintIdx={constraintIdx}
                cannotBeRemoved={constraints.length < 2 || readOnly}
                conditionTypes={conditionTypes}
                conditionTypesMap={conditionTypesMap}
                deleteCondition={deleteCondition}
                addCondition={addCondition}
                deleteConstraint={deleteConstraint}
                onConstraintOperatorChange={onConstraintOperatorChange}
                onMultiValueInputChange={onMultiValueInputChange}
                onConditionAgeValueChange={onConditionAgeValueChange}
                onConditionValueChange={onConditionValueChange}
                onConditionOperatorChange={onConditionOperatorChange}
                onCoordinatesInputChange={onCoordinatesInputChange}
                onCoordinatesFormatChange={onCoordinatesFormatChange}
                onConditionTypeIdChange={onConditionTypeIdChange}
                onConstraintNameChange={onConstraintNameChange}
              />
            );
          }

          return (
            <ReadOnlyConstraint
              key={constraint.id}
              constraint={constraint}
              constraintIdx={constraintIdx}
              cannotBeRemoved={constraints.length < 2 || readOnly}
              readOnly={readOnly}
              conditionTypesMap={conditionTypesMap}
              updateEditConstraintId={updateEditConstraintId}
              deleteConstraint={deleteConstraint}
            />
          );
        })}
      </NxList>
      <NxButton
        className="constraint-editor__add-constraint-btn"
        type="button"
        onClick={addConstraint}
        disabled={isRepositoriesRelated && !isInherited ? false : readOnly}
      >
        <NxFontAwesomeIcon icon={faPlus} />
        <span>Add Constraint</span>
      </NxButton>
      <NxDivider />
    </div>
  );
}
