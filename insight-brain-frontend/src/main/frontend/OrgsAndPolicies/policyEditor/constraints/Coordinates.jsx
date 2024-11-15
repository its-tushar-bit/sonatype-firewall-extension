/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTextInput, NxFormSelect } from '@sonatype/react-shared-components';
import {
  coordinatesTypes,
  coordinatesFormatOptions,
  fieldTypeToPlaceholder,
} from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';

export default function Coordinates({ onFormatChange, onInputChange, fields, constraintIdx, conditionIdx }) {
  function onTextInputChange(event) {
    const { value, name } = event.currentTarget;
    onInputChange(value, name, constraintIdx, conditionIdx, fields.format);
  }

  return (
    <>
      <NxFormSelect
        className="constraint-editor__coordinate-format constraint-editor__150-width"
        onChange={onFormatChange}
        value={fields.format}
        aria-label="Coordinates format"
        data-testid="constraint__coordinates-format"
      >
        {coordinatesFormatOptions.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </NxFormSelect>
      {coordinatesTypes[fields.format].map((type) => (
        <NxTextInput
          key={`${type}-${constraintIdx}-${conditionIdx}`}
          className="constraint-editor__values--input constraint-editor__150-width"
          id={`${type}-${constraintIdx}-${conditionIdx}`}
          {...fields[type]}
          onChange={(_, event) => onTextInputChange(event)}
          name={type}
          placeholder={fieldTypeToPlaceholder[type]}
          validatable
        />
      ))}
    </>
  );
}

Coordinates.propTypes = {
  onFormatChange: PropTypes.func.isRequired,
  onInputChange: PropTypes.func.isRequired,
  fields: PropTypes.shape({
    value: PropTypes.oneOfType([PropTypes.string, PropTypes.object]),
    trimmedValue: PropTypes.string,
    isPristine: PropTypes.bool,
    validationErrors: PropTypes.oneOfType([PropTypes.arrayOf([PropTypes.string]), PropTypes.string]),
    format: PropTypes.string,
  }),
  constraintIdx: PropTypes.number,
  conditionIdx: PropTypes.number,
};
