/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useState } from 'react';
import {
  NxDropdown,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  nxTextInputStateHelpers,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import {
  OBLIGATION_STATUS_FULFILLED,
  OBLIGATION_STATUS_TO_DISPLAY,
  OBLIGATION_STATUSES,
} from '../advancedLegalConstants';
import * as PropTypes from 'prop-types';
import { availableScopesPropType } from '../advancedLegalPropTypes';
import { createScopeOption } from '../legalUtility';
const { initialState, userInput } = nxTextInputStateHelpers;

export default function AllLicenseObligationsModal(props) {
  const {
    // actions
    cancelAllObligationsModal,
    createObligationStatusIcon,
    saveAllObligations,
    // state
    availableScopes,
    submitMask,
    errorMessage,
  } = props;

  const [obligationStatus, setObligationStatus] = useState(OBLIGATION_STATUS_FULFILLED);
  const [ownerId, setOwnerId] = useState('ROOT_ORGANIZATION_ID');
  const [isStatusDropdownOpen, setStatusDropdownOpen] = useState(false);

  const createStatusDropdownLabel = () => {
    return (
      <Fragment>
        {createObligationStatusIcon(obligationStatus)}
        <span>{OBLIGATION_STATUS_TO_DISPLAY[obligationStatus]}</span>
      </Fragment>
    );
  };

  const createModalDropdownOptions = () => {
    return OBLIGATION_STATUSES.filter((os) => os !== obligationStatus).map((os) => {
      return (
        <button
          key={os + '-dropdown-option'}
          id={os + '-dropdown-option'}
          type="button"
          className="nx-dropdown-button"
          onClick={() => {
            setStatusDropdownOpen(false);
            setObligationStatus(os);
          }}
        >
          {createObligationStatusIcon(os)}
          <span>{OBLIGATION_STATUS_TO_DISPLAY[os]}</span>
        </button>
      );
    });
  };

  const [commentTextInput, setCommentTextInput] = useState(initialState(''));

  return (
    <NxModal id="all-obligations-modal" onClose={() => cancelAllObligationsModal()}>
      <NxStatefulForm
        onCancel={() => cancelAllObligationsModal()}
        submitBtnText="Submit"
        submitMaskState={submitMask}
        submitError={errorMessage}
        onSubmit={() => saveAllObligations(obligationStatus, commentTextInput.value, ownerId)}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Edit Review Status For All Obligations</h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Review Status" isRequired>
            <NxDropdown
              id="all-obligations-status-dropdown"
              label={createStatusDropdownLabel()}
              isOpen={isStatusDropdownOpen}
              onToggleCollapse={() => setStatusDropdownOpen(!isStatusDropdownOpen)}
            >
              {createModalDropdownOptions()}
            </NxDropdown>
          </NxFormGroup>
          <NxFormGroup label="Comments">
            <NxTextInput
              type="textarea"
              {...commentTextInput}
              onChange={(payload) => {
                setCommentTextInput(userInput(null, payload));
              }}
            />
          </NxFormGroup>
          <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
            <NxFormSelect
              className="nx-form-select--long"
              id="all-obligations-scope-selection"
              value={ownerId}
              onChange={(payload) => setOwnerId(payload.currentTarget.value)}
            >
              {availableScopes.values.map(createScopeOption)}
            </NxFormSelect>
          </NxFormGroup>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

AllLicenseObligationsModal.propTypes = {
  cancelAllObligationsModal: PropTypes.func.isRequired,
  createObligationStatusIcon: PropTypes.func.isRequired,
  saveAllObligations: PropTypes.func.isRequired,
  availableScopes: availableScopesPropType,
  submitMask: PropTypes.bool,
  errorMessage: PropTypes.string,
};
