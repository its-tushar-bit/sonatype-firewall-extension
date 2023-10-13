/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { NOT_ENABLED, actions } from 'MainRoot/OrgsAndPolicies/retentionSlice';
import {
  NxFieldset,
  NxRadio,
  NxTextInput,
  NxFormRow,
  NxFormGroup,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import { selectIsRootOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import * as PropTypes from 'prop-types';
import { capitalize } from '../../util/jsUtil';

const getParentMaxReportsAndMaxAge = (stage, serverData) => {
  if (serverData) {
    const parentData = stage === 'successMetrics' ? serverData.successMetrics : serverData[stage];
    if (parentData.enablePurging) {
      const prefix = 'keep at most';
      if (parentData.maxCount && parentData.maxAge) {
        return `${prefix} ${parentData.maxAge}, ${parentData.maxCount} report${parentData.maxCount > 1 ? 's' : ''}`;
      }
      if (parentData.maxCount) {
        return `${prefix} ${parentData.maxCount} report${parentData.maxCount > 1 ? 's' : ''}`;
      }
      if (parentData.maxAge) {
        return `${prefix} ${parentData.maxAge}`;
      }
    } else {
      return `${NOT_ENABLED.toLowerCase()}`;
    }
  }
  return '';
};

export default function DataRetentionEditorItem({ stage, stages, parentData }) {
  const isRootOrg = useSelector(selectIsRootOrganization);
  const dispatch = useDispatch();

  const { inheritPolicy, enablePurging, maxAge, maxCount, maxAgeUnit } = stages[stage];

  const ageValidationErrors = stages[stage].maxAge?.validationErrors;

  const handleChange = (stage, val) => {
    dispatch(actions.setRadio({ stage, val }));
  };

  const handleInputChange = (stage, inputType, value) => {
    dispatch(actions.handleInputChange({ stage, inputType, value }));
  };

  const [hideError, setHideError] = useState(false);

  useEffect(() => {
    if (ageValidationErrors?.[0] === 'Age or number of reports is required') {
      setHideError(true);
    } else {
      setHideError(false);
    }
  }, [ageValidationErrors]);

  const formattedStageName = stage === 'successMetrics' ? 'success-metrics' : stage;

  return (
    <NxFieldset
      id={`retention-editor-${formattedStageName}`}
      key={stage}
      label={
        stage === 'successMetrics'
          ? 'Report'
          : stage
              .split('-')
              .map((word) => capitalize(word))
              .join('-')
      }
      isRequired
    >
      {!isRootOrg && (
        <NxRadio
          id={`retention-editor-inherit-${formattedStageName}`}
          name={stage}
          value="inherit"
          onChange={(value) => handleChange(stage, value)}
          isChecked={inheritPolicy === true}
        >
          Inherit ({getParentMaxReportsAndMaxAge(stage, parentData)})
        </NxRadio>
      )}
      <NxRadio
        id={`retention-editor-disable-${formattedStageName}`}
        name={stage}
        value="dont-purge"
        onChange={(value) => handleChange(stage, value)}
        isChecked={inheritPolicy ? false : enablePurging === false}
      >
        {NOT_ENABLED}
      </NxRadio>
      <NxRadio
        id={`retention-editor-custom-${formattedStageName}`}
        name={stage}
        value="custom"
        onChange={(value) => handleChange(stage, value)}
        isChecked={inheritPolicy ? false : enablePurging === true}
      >
        Custom
      </NxRadio>
      {!inheritPolicy && enablePurging && (
        <NxFormRow>
          <div className="custom-purge-row">
            <span className="label--horizontal">Purge after </span>
            <NxFormGroup label="" className="nx-form-group--hide-optional--short">
              <NxTextInput
                name={`${formattedStageName}-age-input`}
                className="nx-text-input--short"
                {...maxAge}
                placeholder="Age"
                onChange={(value) => handleInputChange(stage, 'ageNum', value)}
                validatable={true}
              />
            </NxFormGroup>
            {stage === 'successMetrics' ? (
              <span className="label--horizontal">Years</span>
            ) : (
              <>
                <NxFormGroup label="" className="nx-form-group--hide-optional">
                  <NxFormSelect
                    name={`${formattedStageName}-age-modifier`}
                    className="nx-form-select--short"
                    value={maxAgeUnit}
                    onChange={(e) => handleInputChange(stage, 'ageUnit', e.target.value)}
                  >
                    <option value="days">Days</option>
                    <option value="weeks">Weeks</option>
                    <option value="months">Months</option>
                    <option value="years"> Years </option>
                  </NxFormSelect>
                </NxFormGroup>
                <span>or</span>
                <NxFormGroup label="" className="nx-form-group--hide-optional--short">
                  <NxTextInput
                    className={`nx-text-input--short ${hideError ? 'hide-error' : ''}`}
                    name={`${formattedStageName}-count-input`}
                    {...maxCount}
                    placeholder="No."
                    onChange={(value) => handleInputChange(stage, 'report', value)}
                    validatable={true}
                  />
                </NxFormGroup>
                <span className="label--horizontal">Reports</span>
              </>
            )}
          </div>
        </NxFormRow>
      )}
    </NxFieldset>
  );
}

DataRetentionEditorItem.propTypes = {
  stage: PropTypes.string,
  stages: PropTypes.object,
  parentData: PropTypes.object,
};
