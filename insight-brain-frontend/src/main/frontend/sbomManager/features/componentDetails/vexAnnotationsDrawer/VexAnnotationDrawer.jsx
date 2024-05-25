/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useState } from 'react';
import {
  allThreatLevelNumbers,
  combineValidationErrors,
  hasValidationErrors,
  NxDivider,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormSelect,
  nxFormSelectStateHelpers,
  NxStatefulForm,
  NxTextInput,
  NxTextLink,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import './_vexAnnotationDrawer.scss';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import RenderDetail from 'MainRoot/react/IqVulnerabilityDetails/details/RenderDetail';
import { userInput, initialState } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { isNil } from 'ramda';
import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadSaveVexAnnotationFormError,
  selectSubmitMaskStateForVexAnnotationForm,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import cx from 'classnames';
import { formatDate } from 'MainRoot/util/dateUtils';

export default function VexAnnotationDrawer(props) {
  const {
    issue,
    description,
    cvssScore,
    verified,
    details,
    justification,
    analysisStatus,
    componentHash,
    internalAppId,
    sbomVersion,
    response,
    updatedAt,
    lastUpdatedBy,

    isRowAnnotated,

    // responsesOptions,
    // analysisStatusesOptions,
    // justificationsOptions,

    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,

    preSaveMaskActions,
    postSaveMaskActions,
    onLearnMoreClick,
  } = props;

  const DESCRIPTION_MAX_LENGTH = 150;
  const DROPDOWN_SELECT_OPTION = 'SELECT';
  const defaultDropdownEntry = { key: DROPDOWN_SELECT_OPTION, value: DROPDOWN_SELECT_OPTION };
  const strIsSelectOption = (str) => str === DROPDOWN_SELECT_OPTION;
  const getDropdownOptionsWithSelect = (options) => {
    const defaultDropdown = [defaultDropdownEntry];
    defaultDropdown.push(...options);
    return defaultDropdown;
  };

  const textTruncate = (t) =>
    t.length > DESCRIPTION_MAX_LENGTH ? t.substring(0, DESCRIPTION_MAX_LENGTH) + ' ... ' : t;

  const [showValidationErrors, setShowValidationErrors] = useState(false);
  const [vexAnnotationDetailsControl, setVexAnnotationDetailsControl] = useState(
    initialState(isRowAnnotated ? (isNil(details) ? '' : details) : '')
  );

  const getDefaultStateForDropdown = (initialValue, options) =>
    nxFormSelectStateHelpers.useNxFormSelectState(
      isRowAnnotated
        ? isNil(initialValue)
          ? options[0]?.key
          : filterCommaSeparatedValues(initialValue)
        : DROPDOWN_SELECT_OPTION
    );

  const filterCommaSeparatedValues = (commaSeparatedValue) =>
    commaSeparatedValue.includes(',') ? commaSeparatedValue.split(',')[0] : commaSeparatedValue;

  const [analysisStatusControlState, setAnalysisStatusControlState] = getDefaultStateForDropdown(
    analysisStatus,
    analysisStatusesOptions
  );

  const [justificationControlState, setJustificationControlState] = getDefaultStateForDropdown(
    justification,
    justificationsOptions
  );

  const [responseControlState, setResponseControlState] = getDefaultStateForDropdown(response, responsesOptions);

  // Define validator for controls here
  const isNoValueSelectedInDropdown = (dropdownState) =>
    isRowAnnotated
      ? isNilOrEmpty(dropdownState.value)
      : strIsSelectOption(dropdownState.value) || isNilOrEmpty(dropdownState.value);

  const analysisStatusIsRequiredValidator = isNoValueSelectedInDropdown(analysisStatusControlState)
    ? 'Analysis status field is required. Please select a value from the dropdown list'
    : null;
  // Then combine then
  const validationErrors = combineValidationErrors(analysisStatusIsRequiredValidator);

  const dispatch = useDispatch();
  const formIsSaving = useSelector(selectSubmitMaskStateForVexAnnotationForm);
  const formError = useSelector(selectLoadSaveVexAnnotationFormError);

  const isDropdownSelectedValueValid = (selectedValue, validOptions) => {
    return validOptions.map((entry) => entry.key).indexOf(selectedValue) > -1;
  };

  const getValidValueForDropdownForAnnotatedRow = (dropdownControlState, validOptions) =>
    isDropdownSelectedValueValid(dropdownControlState.value, validOptions)
      ? dropdownControlState.value
      : validOptions[0].key;

  const getValidValueForDropdown = (dropdownControlState, validOptions) => {
    if (isRowAnnotated) {
      return getValidValueForDropdownForAnnotatedRow(dropdownControlState, validOptions);
    } else {
      return dropdownControlState.value;
    }
  };

  const onChangeVexAnnotationDetails = (vexAnnotationDetails) => {
    setVexAnnotationDetailsControl(userInput(null, vexAnnotationDetails));
  };

  const onChangeJustification = (evt) => {
    setJustificationControlState(evt.currentTarget.value);
  };

  const onChangeAnalysisStatus = (evt) => {
    setAnalysisStatusControlState(evt.currentTarget.value);
  };

  const onChangeResponse = (evt) => {
    setResponseControlState(evt.currentTarget.value);
  };

  //Save form handler
  const handleOnSubmit = () => {
    // Evaluate validators here before saving
    // Turn on validation errors to be displayed
    setShowValidationErrors(true);

    if (hasValidationErrors(validationErrors)) {
      return;
    }

    // Turn off validation messages after everything passes.
    setShowValidationErrors(false);

    // Validate if dropdown control values are invalid, if so, pick the
    // first one from their respective valid list to avoid errors
    const validJustification = getValidValueForDropdown(justificationControlState, justificationsOptions);
    const validResponse = getValidValueForDropdown(responseControlState, responsesOptions);
    const validState = getValidValueForDropdown(analysisStatusControlState, analysisStatusesOptions);

    // Craft proper request payload data. In non-annotated rows, if SELECT is selected, that property
    // should not be included in the payload
    const payloadVulnerabilityAnalysisData = { detail: vexAnnotationDetailsControl?.value };
    if (!strIsSelectOption(validJustification)) {
      payloadVulnerabilityAnalysisData['justification'] = validJustification;
    }

    if (!strIsSelectOption(validResponse)) {
      payloadVulnerabilityAnalysisData['response'] = validResponse;
    }

    if (!strIsSelectOption(validState)) {
      payloadVulnerabilityAnalysisData['state'] = validState;
    }

    const saveRequestObject = {
      componentLocator: {
        hash: componentHash,
      },
      vulnerabilityAnalysis: payloadVulnerabilityAnalysisData,
    };
    const savePayload = {
      internalAppId,
      sbomVersion,
      vulnerabilityRefId: issue,
      vexAnnotationFormData: saveRequestObject,
    };
    dispatch(actions.saveVexAnnotation(savePayload)).then(() => {
      //Execute actions after saving the form and before SUCCESS mask disappears
      if (!isNil(preSaveMaskActions)) {
        preSaveMaskActions();
      }

      //Clear success modal after some time
      setTimeout(() => {
        dispatch(actions.clearFormSubmitMask());
        // Actions after the modal success disappears will execute here
        if (!isNil(postSaveMaskActions)) {
          postSaveMaskActions();
        }
      }, 3000);
    });
  };

  const vulnerabilityScore = function () {
    return (
      cvssScore !== undefined && (
        <div className="vex-annotation-drawer__cvss-score">
          <div>
            <b>CVSS Score</b>
          </div>
          <div>
            <NxThreatIndicator
              policyThreatLevel={allThreatLevelNumbers.find((n) => n === Math.floor(cvssScore))}
              presentational
              className="threat-indicator-icon"
            />
            <span data-testid="cvssScore">{cvssScore}</span>
          </div>
        </div>
      )
    );
  };

  const vulnerabilityVerification = function () {
    return (
      verified !== undefined && (
        <div className="vex-annotation-drawer__vulnerability-verified">
          <div>
            <b>Verification Status</b>
          </div>
          <div>
            <NxFontAwesomeIcon
              className={cx(
                'verification-status-icon',
                verified === true ? 'sbom-verified-icon' : 'sbom-unverified-icon'
              )}
              icon={verified === true ? faCheckCircle : faExclamationTriangle}
            />
            <span data-testid="verification-text">{verified === true ? 'Sonatype Verified' : 'Unverified'}</span>
          </div>
        </div>
      )
    );
  };

  const vulnerabilityDescriptionFragment = function (vulnerabilityDescription) {
    return (
      !isNilOrEmpty(vulnerabilityDescription) && (
        <div className="vex-annotation-drawer__vulnerability-description">
          <RenderDetail title={'Description'}>
            <p className="vulnerability-description-paragraph">{textTruncate(vulnerabilityDescription)} </p>
            <div>
              {vulnerabilityDescription.length > DESCRIPTION_MAX_LENGTH && (
                <NxTextLink id="vex-annotation-drawer__link__learn-more" onClick={() => onLearnMoreClick()}>
                  Learn more
                </NxTextLink>
              )}
            </div>
          </RenderDetail>
        </div>
      )
    );
  };

  const updatedInfoFragment = function () {
    return (
      isRowAnnotated &&
      (!isNilOrEmpty(updatedAt) || !isNilOrEmpty(lastUpdatedBy)) && (
        <div className="vex-annotation-drawer__updated-info">
          <RenderDetail title={'Updated'}>
            <div>
              {!isNilOrEmpty(updatedAt) && <div>{formatDate(updatedAt, 'YYYY-MM-DD HH:mm:ss')}</div>}
              {!isNilOrEmpty(lastUpdatedBy) && <div>By {lastUpdatedBy}</div>}
            </div>
          </RenderDetail>
        </div>
      )
    );
  };

  const vulnerabilityInformationHeaderFragment = () => (
    <>
      <div className="vex-annotation-drawer__summary">
        {vulnerabilityScore()}
        {vulnerabilityVerification()}
      </div>
      {vulnerabilityDescriptionFragment(description)}
    </>
  );

  const dropDownOptions = (isRowAnnotated, options) => {
    const optionsForRender = isRowAnnotated ? options : getDropdownOptionsWithSelect(options);
    return optionsForRender.map((optionEntry) => {
      return (
        <option key={optionEntry.key} value={optionEntry.key}>
          {optionEntry.value}
        </option>
      );
    });
  };

  const vexAnnotationFormFragment = function () {
    return (
      <>
        <NxFieldset className="vex-annotation-drawer__form__analysis-status" label="Analysis status" isRequired>
          <NxFormSelect
            id="vex-annotation-drawer__form__analysis-status-select"
            onChange={onChangeAnalysisStatus}
            {...analysisStatusControlState}
          >
            {dropDownOptions(isRowAnnotated, analysisStatusesOptions)}
          </NxFormSelect>
        </NxFieldset>

        <NxFieldset className="vex-annotation-drawer__form__justification" label="Justification">
          <NxFormSelect
            id="vex-annotation-drawer__form__justification-select"
            onChange={onChangeJustification}
            {...justificationControlState}
          >
            {dropDownOptions(isRowAnnotated, justificationsOptions)}
          </NxFormSelect>
        </NxFieldset>

        <NxFieldset className="vex-annotation-drawer__form__response" label="Response">
          <NxFormSelect
            id="vex-annotation-drawer__form__response-select"
            onChange={onChangeResponse}
            {...responseControlState}
          >
            {dropDownOptions(isRowAnnotated, responsesOptions)}
          </NxFormSelect>
        </NxFieldset>

        <NxFieldset className="vex-annotation-drawer__form__details" label="Description">
          <NxTextInput
            type="textarea"
            maxLength={1000}
            onChange={onChangeVexAnnotationDetails}
            className="nx-text-input--long"
            placeholder={'Entry'}
            {...vexAnnotationDetailsControl}
          />
        </NxFieldset>
      </>
    );
  };

  return (
    <>
      {vulnerabilityInformationHeaderFragment()}
      <NxDivider />
      <NxStatefulForm
        id="vex-annotation-drawer__form"
        onSubmit={handleOnSubmit}
        submitBtnText={isRowAnnotated === true ? 'Update' : 'Save'}
        submitError={formError}
        submitMaskState={formIsSaving}
        submitMaskMessage="Saving..."
        validationErrors={validationErrors}
        showValidationErrors={showValidationErrors}
      >
        {vexAnnotationFormFragment()}
        {updatedInfoFragment()}
      </NxStatefulForm>
    </>
  );
}

VexAnnotationDrawer.propTypes = {
  responsesOptions: PropTypes.array.isRequired,
  analysisStatusesOptions: PropTypes.array.isRequired,
  justificationsOptions: PropTypes.array.isRequired,
  issue: PropTypes.string,
  description: PropTypes.string,
  sbomVersion: PropTypes.string,
  componentHash: PropTypes.string,
  internalAppId: PropTypes.string,
  cvssScore: PropTypes.number,
  verified: PropTypes.bool,
  details: PropTypes.string,
  justification: PropTypes.string,
  analysisStatus: PropTypes.string,
  response: PropTypes.string,
  updatedAt: PropTypes.number,
  lastUpdatedBy: PropTypes.string,
  isRowAnnotated: PropTypes.bool,

  // Functions
  preSaveMaskActions: PropTypes.func,
  postSaveMaskActions: PropTypes.func,
  onLearnMoreClick: PropTypes.func,
};
