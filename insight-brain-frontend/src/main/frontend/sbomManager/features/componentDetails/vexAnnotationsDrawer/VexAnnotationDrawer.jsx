/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useState, useRef, useEffect } from 'react';
import {
  allThreatLevelNumbers,
  combineValidationErrors,
  hasValidationErrors,
  NxButton,
  NxButtonBar,
  NxDivider,
  NxDrawer,
  NxErrorAlert,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFooter,
  NxForm,
  NxFormSelect,
  nxFormSelectStateHelpers,
  NxH2,
  NxLoadWrapper,
  NxModal,
  NxTextInput,
  NxTextLink,
  NxThreatIndicator,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { faCheckCircle, faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import './_vexAnnotationDrawer.scss';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import RenderDetail from 'MainRoot/react/IqVulnerabilityDetails/details/RenderDetail';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { isNil } from 'ramda';
import { actions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectLoadingVulnerabilityAnalysisReferenceData,
  selectLoadSaveVexAnnotationFormError,
  selectLoadVulnerabilityAnalysisReferenceDataError,
  selectSubmitMaskStateForVexAnnotationForm,
} from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSelector';
import cx from 'classnames';
import { formatDate } from 'MainRoot/util/dateUtils';
import VexAnnotationDrawerHeader from 'MainRoot/sbomManager/features/componentDetails/vexAnnotationsDrawer/VexAnnotationDrawerHeader';
import PortalDrawer from 'MainRoot/react/PortalDrawer';

export default function VexAnnotationDrawer(props) {
  const {
    isDrawerOpen,

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
    isJustificationSet,
    isResponseSet,

    responsesOptions,
    analysisStatusesOptions,
    justificationsOptions,

    preSaveMaskActions,
    postSaveMaskActions,

    // popover header and parent file
    loadVexReferenceData,
    onClose,

    componentPurl,

    openVulnerabilityDetailsModal,
  } = props;

  // This isVexAnnotationPopoverOpen validation is necessary here to force destruction and regeneration of the Drawer
  // when is closed
  if (!isDrawerOpen) {
    return null;
  }

  const analysisStatusDropdownIsRequiredErrorMessage =
    'Analysis State field is required. Please select a value from the dropdown list';

  const isVulnerabilityReferenceDataLoading = useSelector(selectLoadingVulnerabilityAnalysisReferenceData);
  const errorLoadingAnalysisReferenceData = useSelector(selectLoadVulnerabilityAnalysisReferenceDataError);

  const isDropdownsReferenceDataReady =
    !isNilOrEmpty(responsesOptions) && !isNilOrEmpty(justificationsOptions) && !isNilOrEmpty(analysisStatusesOptions);

  const errorDropdownsContentEmpty = isDropdownsReferenceDataReady ? null : 'Please retry.';
  const popOverContentError = !isNil(errorLoadingAnalysisReferenceData)
    ? errorLoadingAnalysisReferenceData
    : errorDropdownsContentEmpty;

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

  const [showSaveFormError, setShowSaveFormError] = useState(false);
  const [showValidationErrors, setShowValidationErrors] = useState(false);
  const [validationErrors, setValidationErrors] = useState([]);
  const [showUnsavedChangesModal, setShowUnsavedChangesModal] = useState(false);
  const isClosingModalRef = useRef(false);

  // Refs to capture current form values (persist even when state is cleared)
  const currentDetailsRef = useRef('');
  const currentAnalysisStatusRef = useRef('');
  const currentJustificationRef = useRef('');
  const currentResponseRef = useRef('');

  // Helper functions to compute initial values (DRY)
  const getInitialDetailsValue = () => (isRowAnnotated ? (isNilOrEmpty(details) ? '' : details) : '');

  const getInitialDropdownValue = (initialValue, options, isSet) => {
    if (isSet) {
      return isNil(initialValue) ? options[0]?.key : pickFirstVexResponse(initialValue);
    }
    return DROPDOWN_SELECT_OPTION;
  };

  const getInitialAnalysisStatusValue = () =>
    getInitialDropdownValue(analysisStatus, analysisStatusesOptions, isRowAnnotated);

  const getInitialJustificationValue = () =>
    getInitialDropdownValue(justification, justificationsOptions, isJustificationSet);

  const getInitialResponseValue = () => getInitialDropdownValue(response, responsesOptions, isResponseSet);

  // Initialize form controls with computed initial values
  const [vexAnnotationDetailsControl, setVexAnnotationDetailsControl] = useState(
    initialState(getInitialDetailsValue())
  );

  const [analysisStatusControlState, setAnalysisStatusControlState] = nxFormSelectStateHelpers.useNxFormSelectState(
    getInitialAnalysisStatusValue()
  );

  const [justificationControlState, setJustificationControlState] = nxFormSelectStateHelpers.useNxFormSelectState(
    getInitialJustificationValue()
  );

  const [responseControlState, setResponseControlState] = nxFormSelectStateHelpers.useNxFormSelectState(
    getInitialResponseValue()
  );

  // Keep refs in sync with state values
  useEffect(() => {
    currentDetailsRef.current = vexAnnotationDetailsControl?.value || '';
    currentAnalysisStatusRef.current = analysisStatusControlState.value;
    currentJustificationRef.current = justificationControlState.value;
    currentResponseRef.current = responseControlState.value;
  }, [vexAnnotationDetailsControl, analysisStatusControlState, justificationControlState, responseControlState]);

  // Check if form has unsaved changes (using refs for persistence)
  const hasUnsavedChanges = () => {
    const initialDetails = isRowAnnotated ? (isNilOrEmpty(details) ? '' : details) : '';
    const currentDetails = currentDetailsRef.current || '';
    const detailsChanged = initialDetails !== currentDetails;

    const initialAnalysisStatus = isRowAnnotated
      ? isNil(analysisStatus)
        ? analysisStatusesOptions[0]?.key
        : pickFirstVexResponse(analysisStatus)
      : DROPDOWN_SELECT_OPTION;
    const analysisStatusChanged = initialAnalysisStatus !== currentAnalysisStatusRef.current;

    const initialJustification = isJustificationSet
      ? isNil(justification)
        ? justificationsOptions[0]?.key
        : pickFirstVexResponse(justification)
      : DROPDOWN_SELECT_OPTION;
    const justificationChanged = initialJustification !== currentJustificationRef.current;

    const initialResponse = isResponseSet
      ? isNil(response)
        ? responsesOptions[0]?.key
        : pickFirstVexResponse(response)
      : DROPDOWN_SELECT_OPTION;
    const responseChanged = initialResponse !== currentResponseRef.current;

    return detailsChanged || analysisStatusChanged || justificationChanged || responseChanged;
  };

  // Handle drawer close with unsaved changes check
  const handleCloseDrawer = () => {
    // Don't show modal again if we're in the process of closing it
    if (isClosingModalRef.current) {
      return;
    }

    if (hasUnsavedChanges()) {
      setShowUnsavedChangesModal(true);
    } else {
      onClose();
    }
  };

  const handleUnsavedChangesCancel = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    isClosingModalRef.current = true;
    setShowUnsavedChangesModal(false);
    // Reset the flag after a short delay
    setTimeout(() => {
      isClosingModalRef.current = false;
    }, 100);
  };

  const handleUnsavedChangesContinue = (e) => {
    if (e) {
      e.preventDefault();
      e.stopPropagation();
    }
    isClosingModalRef.current = true;
    setShowUnsavedChangesModal(false);
    onClose();
  };

  // Define validators for controls here

  // Validation conditions for form
  const isNoValueSelectedInDropdown = (dropdownState) =>
    isRowAnnotated
      ? isNilOrEmpty(dropdownState.value)
      : strIsSelectOption(dropdownState.value) || isNilOrEmpty(dropdownState.value);

  // Validators
  const analysisStatusIsRequiredValidator = (analysisStatusDropdownState) =>
    isNoValueSelectedInDropdown(analysisStatusDropdownState) ? analysisStatusDropdownIsRequiredErrorMessage : null;

  // Combine validators here if more than one
  const validateForm = () => {
    // All form validations to check here
    return combineValidationErrors(analysisStatusIsRequiredValidator(analysisStatusControlState));
  };

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

  const getValidValueForAnalysisDropdown = (dropdownControlState, validOptions) => {
    if (isRowAnnotated) {
      return getValidValueForDropdownForAnnotatedRow(dropdownControlState, validOptions);
    } else {
      return dropdownControlState.value;
    }
  };

  const getValidValueForJustificationDropdown = (dropdownControlState, validOptions) => {
    if (isJustificationSet) {
      return getValidValueForDropdownForAnnotatedRow(dropdownControlState, validOptions);
    } else {
      return dropdownControlState.value;
    }
  };

  const getValidValueForResponseDropdown = (dropdownControlState, validOptions) => {
    if (isResponseSet) {
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

    const dropdownError = analysisStatusIsRequiredValidator({
      ...analysisStatusControlState,
      value: evt.currentTarget.value,
    });

    if (!isNil(dropdownError)) {
      setShowValidationErrors(true);
      setValidationErrors(combineValidationErrors(dropdownError));
    } else {
      setShowValidationErrors(false);
      setValidationErrors([]);
    }
  };

  const onChangeResponse = (evt) => {
    setResponseControlState(evt.currentTarget.value);
  };

  const onLearnMoreClick = () => {
    onClose();
    openVulnerabilityDetailsModal({
      issue,
    });
  };

  //Save form handler
  const handleOnSubmit = () => {
    // Evaluate validators here before saving
    const updatedValidationErrors = validateForm();

    if (hasValidationErrors(updatedValidationErrors)) {
      // Turn on validation errors to be displayed
      setShowValidationErrors(true);
      setValidationErrors(updatedValidationErrors);
      return;
    }

    // Turn off validation messages after everything passes.
    setShowValidationErrors(false);
    setValidationErrors([]);

    // Validate if dropdown control values are invalid, if so, pick the
    // first one from their respective valid list to avoid errors
    const validJustification = getValidValueForJustificationDropdown(justificationControlState, justificationsOptions);
    const validResponse = getValidValueForResponseDropdown(responseControlState, responsesOptions);
    const validState = getValidValueForAnalysisDropdown(analysisStatusControlState, analysisStatusesOptions);

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

    dispatch(actions.saveVexAnnotation(savePayload))
      .then((result) => {
        if (result.error) {
          setShowSaveFormError(true);
          return;
        }

        setShowSaveFormError(false);

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
      })
      .catch(() => {
        dispatch(actions.clearFormSubmitMask());
        dispatch(actions.setFormErrorSaveMessage('An error occurred when trying to save the form. Please retry'));
        setShowSaveFormError(true);
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

  const dropDownOptions = (isVexFieldAnnotated, options) => {
    const optionsForRender = isVexFieldAnnotated ? options : getDropdownOptionsWithSelect(options);
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
        <NxFieldset className="vex-annotation-drawer__form__analysis-status" label="Analysis State" isRequired>
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
            {dropDownOptions(isJustificationSet, justificationsOptions)}
          </NxFormSelect>
        </NxFieldset>

        <NxFieldset className="vex-annotation-drawer__form__response" label="Response">
          <NxFormSelect
            id="vex-annotation-drawer__form__response-select"
            onChange={onChangeResponse}
            {...responseControlState}
          >
            {dropDownOptions(isResponseSet, responsesOptions)}
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
      <PortalDrawer
        id="vex-annotation-popover"
        className="vex-annotation-drawer"
        onClose={handleCloseDrawer}
        onCancel={handleCloseDrawer}
        open={isDrawerOpen}
        aria-label={`Annotate ${issue}`}
      >
        <NxDrawer.Header>
          <VexAnnotationDrawerHeader
            headerTitle={`Annotate ${issue}`}
            headerSize={'h2'}
            componentPurl={componentPurl}
          ></VexAnnotationDrawerHeader>
        </NxDrawer.Header>
        <NxDrawer.Content>
          <NxLoadWrapper
            retryHandler={loadVexReferenceData}
            loading={isVulnerabilityReferenceDataLoading}
            error={popOverContentError}
          >
            {vulnerabilityInformationHeaderFragment()}
            <NxDivider />

            <NxForm
              id="vex-annotation-drawer__form"
              showValidationErrors={false}
              submitMaskState={formIsSaving}
              submitMaskMessage="Saving..."
              onSubmit={() => null}
            >
              {vexAnnotationFormFragment()}
              {updatedInfoFragment()}
            </NxForm>
          </NxLoadWrapper>
        </NxDrawer.Content>

        <NxFooter className="vex-annotation-popover__footer-nx-drawer">
          <NxErrorAlert
            className={cx(
              'vex-annotation-popover__form-validation-errors',
              showValidationErrors && hasValidationErrors(validationErrors)
                ? ''
                : 'vex-annotation-popover__footer-hidden'
            )}
          >
            {validationErrors[0]}
          </NxErrorAlert>

          <NxErrorAlert
            className={cx(
              'vex-annotation-popover__form-save-errors',
              showSaveFormError ? '' : 'vex-annotation-popover__footer-hidden'
            )}
          >
            <span className={'vex-annotation-popover__form-save-error-message'}>{formError}</span>
            <NxButton variant="error" onClick={() => handleOnSubmit()}>
              Retry
            </NxButton>
          </NxErrorAlert>

          <div className="vex-annotation-popover__footer-button-bar">
            <NxButton
              className={cx(
                'vex-annotation-drawer__form__submit-button',
                !hasValidationErrors(validationErrors) ? '' : 'vex-annotation-popover__footer-hidden',
                showSaveFormError ? 'vex-annotation-popover__footer-hidden' : ''
              )}
              onClick={() => handleOnSubmit()}
              variant="primary"
            >
              {isRowAnnotated === true ? 'Update' : 'Save'}
            </NxButton>
          </div>
        </NxFooter>
      </PortalDrawer>

      {showUnsavedChangesModal && (
        <NxModal key="unsaved-changes-modal" id="unsaved-modal" variant="narrow" onCancel={handleUnsavedChangesCancel}>
          <NxModal.Header>
            <NxH2>Unsaved Changes</NxH2>
          </NxModal.Header>
          <NxModal.Content>
            <NxWarningAlert className="nx-alert--modifier">
              <span>The page may contain unsaved changes; continuing will discard them.</span>
            </NxWarningAlert>
          </NxModal.Content>
          <NxFooter>
            <NxButtonBar>
              <NxButton onClick={handleUnsavedChangesCancel}>Cancel</NxButton>
              <NxButton variant="primary" onClick={handleUnsavedChangesContinue}>
                Continue
              </NxButton>
            </NxButtonBar>
          </NxFooter>
        </NxModal>
      )}
    </>
  );
}

VexAnnotationDrawer.propTypes = {
  isDrawerOpen: PropTypes.bool,

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
  isJustificationSet: PropTypes.bool,
  isResponseSet: PropTypes.bool,

  // Functions
  preSaveMaskActions: PropTypes.func,
  postSaveMaskActions: PropTypes.func,
  onLearnMoreClick: PropTypes.func,

  loadVexReferenceData: PropTypes.func,
  openVulnerabilityDetailsModal: PropTypes.func,
  onClose: PropTypes.func,

  componentPurl: PropTypes.string,
};

export const pickFirstVexResponse = (commaSeparatedValue) =>
  !isNil(commaSeparatedValue) && commaSeparatedValue.includes(',')
    ? commaSeparatedValue.split(',')[0]
    : commaSeparatedValue;
