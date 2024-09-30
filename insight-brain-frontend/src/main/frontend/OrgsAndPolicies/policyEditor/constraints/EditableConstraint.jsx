/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import classnames from 'classnames';
import { faTrashAlt, faPlus } from '@fortawesome/free-solid-svg-icons';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxList,
  NxFormGroup,
  NxTextInput,
  NxFormSelect,
  NxH3,
} from '@sonatype/react-shared-components';
import AgeInDaysInput from './AgeInDaysInput';
import Coordinates from './Coordinates';
import ConditionIsNotSupportedError from './ConditionIsNotSupportedError';

const constraintOperatorOptions = [
  {
    operator: 'OR',
    name: 'any',
  },
  {
    operator: 'AND',
    name: 'all',
  },
];

const conditionDataType = new Set(['String', 'Integer', 'Float']);

const fieldToWidthMap = new Map([
  ['ComponentCategoryValueType', 300],
  ['ComponentFormatValueType', 150],
  ['HygieneRatingValueType', 150],
  ['IntegrityRatingValueType', 300],
  ['IdentificationSourceValueType', 300],
  ['LabelValueType', 300],
  ['LicenseValueType', 300],
  ['LicenseStatusValueType', 300],
  ['LicenseThreatGroupValueType', 300],
  ['MatchStateValueType', 150],
  ['SecurityVulnerabilityStatusValueType', 300],
  ['SecurityVulnerabilityCategoryValueType', 300],
  ['DataSourceValueType', 150],
  ['DependencyTypeValueType', 300],
  ['IacControlValueType', 300],
  ['PercentageValueType', 150],
  ['PackageUrlValueType', 300],
  ['IntegerValueType', 150],
  ['FloatValueType', 150],
  ['SecurityVulnerabilityCweValueType', 300],
]);

export default function EditableConstraint({
  constraint,
  constraintIdx,
  cannotBeRemoved,
  conditionTypes,
  conditionTypesMap,
  onMultiValueInputChange,
  onConditionAgeValueChange,
  onConditionValueChange,
  onConditionOperatorChange,
  onCoordinatesInputChange,
  onCoordinatesFormatChange,
  onConditionTypeIdChange,
  deleteCondition,
  addCondition,
  onConstraintOperatorChange,
  onConstraintNameChange,
  deleteConstraint,
}) {
  const { name } = constraint;

  function getEmptyOptionCondition(condition) {
    if (!conditionTypesMap[condition.conditionTypeId].enabled) {
      return conditionTypesMap[condition.conditionTypeId].name;
    }
    return 'None Selected';
  }

  function renderAvailableValues(condition, selectedCondition, constraintIdx, conditionIdx) {
    const { value } = condition;

    const widthClassName = classnames('constraint-editor__values', {
      [`constraint-editor__${fieldToWidthMap.get(selectedCondition.valueTypeId)}-width`]: fieldToWidthMap.has(
        selectedCondition.valueTypeId
      ),
    });

    if (selectedCondition.valueType?.availableValues) {
      return (
        <NxFormSelect
          id={`condition-value-${constraintIdx}-${conditionIdx}`}
          className={widthClassName}
          onChange={(event) => onConditionValueChange(event, constraintIdx, conditionIdx)}
          value={value.value}
          data-testid="constraint__condition-value"
          aria-label="Condition value"
        >
          {selectedCondition.valueType.availableValues.map((value) => {
            const nameField =
              condition.conditionTypeId === 'Label'
                ? 'label'
                : condition.conditionTypeId === 'License'
                ? 'shortDisplayName'
                : 'name';
            const fieldValue = value[nameField];

            if (fieldValue === 'Security-Reachable') {
              return null;
            }

            return (
              <option key={value.id} value={value.id}>
                {fieldValue}
              </option>
            );
          })}
        </NxFormSelect>
      );
    }

    if (selectedCondition.valueTypeId === 'AgeInDaysValueType') {
      return (
        <AgeInDaysInput
          id={`condition-value-${constraintIdx}-${conditionIdx}`}
          ageInDays={value}
          onChange={(value) => onConditionAgeValueChange(value, constraintIdx, conditionIdx)}
          data-testid="constraint__condition-value"
        />
      );
    }

    if (selectedCondition.valueTypeId === 'CoordinatesValueType') {
      return (
        <Coordinates
          onFormatChange={(e) => onCoordinatesFormatChange(e, constraintIdx, conditionIdx)}
          fields={value}
          constraintIdx={constraintIdx}
          conditionIdx={conditionIdx}
          onInputChange={onCoordinatesInputChange}
          data-testid="constraint__condition-value"
        />
      );
    }

    if (conditionDataType.has(selectedCondition.valueType?.dataType)) {
      const id = `condition${selectedCondition.valueTypeId.split('Type')[0]}-${constraintIdx}-${conditionIdx}`;
      return (
        <NxTextInput
          className={`${widthClassName} constraint-editor__values--input`}
          placeholder={selectedCondition.name}
          {...value}
          id={id}
          onChange={(value) => onMultiValueInputChange(value, selectedCondition, constraintIdx, conditionIdx)}
          aria-required={true}
          validatable
          data-testid="constraint__condition-value"
        />
      );
    }
  }

  const isDeleteConditionButtonDisabled = constraint.conditions.length < 2;

  return (
    <NxList.Item
      key={constraint.id}
      className="constraint-editor__item constraint-editor__item--editable"
      data-testid="editable-constraint"
    >
      <div className="constraint-editor__editable-header">
        <NxFormGroup label="Constraint Name" isRequired>
          <NxTextInput
            onChange={(value) => onConstraintNameChange(value, constraintIdx, constraint.id)}
            aria-required={true}
            {...name}
            validatable
            id={`editor-constraint-name-${constraintIdx}`}
          />
        </NxFormGroup>
        <NxList.Actions>
          <NxButton
            variant="icon-only"
            title={cannotBeRemoved ? '' : 'Delete constraint'}
            aria-label="Delete constraint"
            type="button"
            disabled={cannotBeRemoved}
            className="constraint-editor__delete-btn"
            onClick={() => deleteConstraint(constraintIdx)}
          >
            <NxFontAwesomeIcon icon={faTrashAlt} />
          </NxButton>
        </NxList.Actions>
      </div>
      <ConditionIsNotSupportedError constraint={constraint} conditionTypesMap={conditionTypesMap} />
      <NxH3>Conditions</NxH3>
      <div className="constraint-editor__operator-wrapper">
        <span>This constraint is in violation if</span>
        <NxFormSelect
          id={`editor-constraint-operator-${constraintIdx}`}
          className="constraint-editor__operator constraint-editor__150-width"
          onChange={(e) => onConstraintOperatorChange(e, constraintIdx)}
          value={constraint.operator}
          data-testid="constraintsOperator"
          aria-label="Constraint operator"
        >
          <option hidden>-- Any or All --</option>
          {constraintOperatorOptions.map((option) => (
            <option key={option.operator} value={option.operator}>
              {option.name}
            </option>
          ))}
        </NxFormSelect>
        <span>of the following are true:</span>
      </div>
      <NxList className="constraint-editor__conditions">
        {constraint.conditions.map((condition, conditionIdx) => {
          const selectedCondition = conditionTypesMap[condition.conditionTypeId];

          const operatorClassName = classnames('constraint-editor__condition-operator', {
            'constraint-editor__data-source-width': condition.conditionTypeId === 'DataSource',
            'constraint-editor__150-width': condition.conditionTypeId !== 'DataSource',
          });

          return (
            <NxList.Item key={`${constraintIdx}${conditionIdx}`} data-testid="editable-constraint__condition">
              <NxList.Text>
                <NxFormSelect
                  className="constraint-editor__condition-type"
                  id={`condition-type-${constraintIdx}-${conditionIdx}`}
                  onChange={(event) => onConditionTypeIdChange(event, constraintIdx, conditionIdx)}
                  value={condition.conditionTypeId}
                  data-testid="constraint__condition-type"
                  aria-label="Constraint condition type"
                >
                  <option hidden>{getEmptyOptionCondition(condition)}</option>
                  {conditionTypes.map((type) => (
                    <option key={type.id} value={type.id}>
                      {type.name}
                    </option>
                  ))}
                </NxFormSelect>
                <NxFormSelect
                  id={`condition-operator-${constraintIdx}-${conditionIdx}`}
                  className={operatorClassName}
                  onChange={(event) => onConditionOperatorChange(event, constraintIdx, conditionIdx)}
                  value={condition.operator}
                  data-testid="constraint__condition-operator"
                  aria-label="Condition operator"
                >
                  {selectedCondition.supportedOperators.map((operator) => (
                    <option key={operator} value={operator}>
                      {operator}
                    </option>
                  ))}
                </NxFormSelect>
                {renderAvailableValues(condition, selectedCondition, constraintIdx, conditionIdx)}
              </NxList.Text>
              <NxList.Actions>
                <NxButton
                  variant="icon-only"
                  title={isDeleteConditionButtonDisabled ? '' : 'Delete condition'}
                  aria-label="Delete condition"
                  type="button"
                  disabled={isDeleteConditionButtonDisabled}
                  className="constraint-editor__delete-condition-btn"
                  onClick={() => deleteCondition(constraintIdx, conditionIdx)}
                >
                  <NxFontAwesomeIcon icon={faTrashAlt} />
                </NxButton>
              </NxList.Actions>
            </NxList.Item>
          );
        })}
      </NxList>
      <NxButton
        className="constraint-editor__add-condition-btn"
        variant="tertiary"
        type="button"
        onClick={() => addCondition(constraintIdx)}
      >
        <NxFontAwesomeIcon icon={faPlus} />
        <span>Add Condition</span>
      </NxButton>
    </NxList.Item>
  );
}

EditableConstraint.propTypes = {
  constraint: PropTypes.object,
  constraintIdx: PropTypes.number,
  cannotBeRemoved: PropTypes.bool,
  conditionTypes: PropTypes.array,
  conditionTypesMap: PropTypes.object,
  onMultiValueInputChange: PropTypes.func,
  onConditionAgeValueChange: PropTypes.func,
  onConditionValueChange: PropTypes.func,
  onConditionOperatorChange: PropTypes.func,
  onCoordinatesInputChange: PropTypes.func,
  onCoordinatesFormatChange: PropTypes.func,
  onConditionTypeIdChange: PropTypes.func,
  deleteCondition: PropTypes.func,
  addCondition: PropTypes.func,
  onConstraintOperatorChange: PropTypes.func,
  onConstraintNameChange: PropTypes.func,
  deleteConstraint: PropTypes.func,
};
