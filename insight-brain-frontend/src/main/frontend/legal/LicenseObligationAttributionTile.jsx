/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxAccordion,
  useToggle,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import { faPen, faPlus } from '@fortawesome/pro-solid-svg-icons';
import * as PropTypes from 'prop-types';
import classnames from 'classnames';
import { availableScopesPropType, licenseObligationPropType } from './advancedLegalPropTypes';
import ObligationStatusComponent from './shared/ObligationStatusComponent';
import { ATTRIBUTION_DISPLAY_MAP } from './advancedLegalConstants';
import { createScopeOption } from './legalUtility';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function LicenseObligationAttributionTile(props) {
  const {
    // actions
    setAttributionText,
    setAttributionScope,
    saveAttribution,
    setShowAttributionModal,
    cancelAttributionModal,
    setObligationStatus,
    setObligationScope,
    // state
    id,
    name,
    originalAttributionText,
    attributionText,
    availableScopes,
    originalScope,
    scope,
    error,
    saveAttributionSubmitMask,
    showAttributionModal,
    existingObligation,
  } = props;
  const isAttributionPresent = () => id !== null;
  const isAttributionDirty = () => attributionText !== originalAttributionText || scope !== originalScope;
  const isDirty = () => isAttributionDirty() || isObligationDirty();
  const isValid = () => isDirty() && (isAttributionPresent() || attributionText);
  const validationErrorMessage = isAttributionPresent()
    ? 'Must change attribution text or scope.'
    : 'Must add attribution text.';
  const [attributionTextInput, setAttributionTextInput] = useState(initialState(attributionText));

  const attributionConstantsMap = ATTRIBUTION_DISPLAY_MAP[name] || [];

  const isAdditionalAttribution = attributionConstantsMap.length === 0;
  const title = isAdditionalAttribution ? 'Additional Attributions' : attributionConstantsMap.headerTitle;
  const modalTitle = isAdditionalAttribution ? 'Additional Attributions' : attributionConstantsMap.modalTitleSuffix;
  const emptyMessage = isAdditionalAttribution ? 'None added' : attributionConstantsMap.emptyMessage;
  const editOrAdd = isAttributionPresent() ? 'Edit' : 'Add';
  const toId = (s) => (s ? s.toLowerCase().replace(/\s+/g, '-') : '');

  function isObligationDirty() {
    return existingObligation && existingObligation.status !== existingObligation.originalStatus;
  }

  const resetExistingObligation = () => {
    if (existingObligation) {
      setObligationStatus({
        name: existingObligation.name,
        value: existingObligation.originalStatus,
      });
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    }
  };

  const getSubmitMaskState = () => {
    const nullIfUndef = (b) => (b === undefined ? null : b);
    const mainSubmitMaskState = nullIfUndef(saveAttributionSubmitMask);
    const obligationSubmitMaskState = existingObligation
      ? nullIfUndef(existingObligation.saveObligationSubmitMask)
      : null;
    if (mainSubmitMaskState === null) {
      return obligationSubmitMaskState;
    }
    if (obligationSubmitMaskState === null) {
      return mainSubmitMaskState;
    }
    return mainSubmitMaskState && obligationSubmitMaskState;
  };

  const setObligationScopeIfNeeded = (event) => {
    if (existingObligation && existingObligation.status !== existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: event.target.value,
      });
    }
  };

  const onObligationChange = (value) => {
    setObligationStatus({ name: existingObligation.name, value });
    if (value === existingObligation.originalStatus) {
      setObligationScope({
        name: existingObligation.name,
        value: existingObligation.originalScope,
      });
    } else {
      setObligationScope({ name: existingObligation.name, value: scope });
    }
  };

  const trySave = (name) => {
    saveAttribution({
      obligationName: name,
      existingObligation,
      isAttributionDirty: isAttributionDirty(),
      isObligationDirty: isObligationDirty(),
    });
  };

  const createAttributionModal = () => {
    return (
      <NxModal
        id="edit-attribution-modal"
        onClose={() => {
          resetExistingObligation();
          cancelAttributionModal({ name });
        }}
      >
        <NxStatefulForm
          onCancel={() => {
            resetExistingObligation();
            cancelAttributionModal({ name });
          }}
          submitBtnText="Save"
          onSubmit={() => trySave(name)}
          submitError={error || existingObligation?.error}
          submitMaskState={getSubmitMaskState()}
          validationErrors={isValid() ? undefined : validationErrorMessage}
        >
          <header className="nx-modal-header">
            <h2 className="nx-h2">{editOrAdd + ' ' + modalTitle}</h2>
          </header>
          <div className="nx-modal-content">
            <NxFormGroup
              label="Attribution Text"
              sublabel={
                isAdditionalAttribution
                  ? 'Enter any additional information that needs to be included in the attribution report.'
                  : 'Enter information that needs to be included in the attribution report to fulfill the ' +
                    'related obligation.'
              }
              isRequired
            >
              <NxTextInput
                maxLength="1000"
                type="textarea"
                className="nx-text-input--full"
                {...attributionTextInput}
                onChange={(payload) => {
                  setAttributionText({ name, value: payload });
                  setAttributionTextInput(userInput(null, payload));
                }}
              />
            </NxFormGroup>
            {existingObligation && (
              <ObligationStatusComponent existingObligation={existingObligation} onChange={onObligationChange} />
            )}
            <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
              <NxFormSelect
                id="edit-attribution-scope-selection"
                className="nx-form-select nx-form-select--long"
                value={scope}
                onChange={(payload) => {
                  setAttributionScope({
                    name,
                    value: payload.currentTarget.value,
                  });
                  setObligationScopeIfNeeded(payload);
                }}
              >
                {availableScopes.values.map(createScopeOption)}
              </NxFormSelect>
            </NxFormGroup>
          </div>
        </NxStatefulForm>
      </NxModal>
    );
  };

  const classes = classnames({
    'license-no-legal-elements-text': !isAttributionPresent(),
  });

  const [open, toggleOpen] = useToggle(true);

  return (
    <React.Fragment>
      <NxAccordion
        open={open}
        onToggle={toggleOpen}
        className={'license-obligation-attribution-tile'}
        id={isAdditionalAttribution ? 'additional-attribution-tile' : toId(name) + '-attribution-tile'}
      >
        <NxAccordion.Header>
          <NxAccordion.Title>{title}</NxAccordion.Title>
          <div className="nx-btn-bar">
            <NxButton
              variant="tertiary"
              onClick={() => {
                setAttributionTextInput(initialState(attributionText));
                setShowAttributionModal({ name, value: true });
              }}
            >
              <NxFontAwesomeIcon icon={isAttributionPresent() ? faPen : faPlus} />
              <span>{editOrAdd}</span>
            </NxButton>
          </div>
        </NxAccordion.Header>
        <div className={classes}>{isAttributionPresent() ? originalAttributionText : emptyMessage}</div>
      </NxAccordion>
      {showAttributionModal && createAttributionModal()}
    </React.Fragment>
  );
}

LicenseObligationAttributionTile.propTypes = {
  setAttributionText: PropTypes.func.isRequired,
  setAttributionScope: PropTypes.func.isRequired,
  saveAttribution: PropTypes.func.isRequired,
  setShowAttributionModal: PropTypes.func.isRequired,
  cancelAttributionModal: PropTypes.func.isRequired,
  id: PropTypes.string,
  name: PropTypes.string,
  originalAttributionText: PropTypes.string,
  attributionText: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  originalScope: PropTypes.string,
  scope: PropTypes.string.isRequired,
  error: PropTypes.string,
  saveAttributionSubmitMask: PropTypes.bool,
  showAttributionModal: PropTypes.bool.isRequired,
  existingObligation: licenseObligationPropType,
  setObligationStatus: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
};
