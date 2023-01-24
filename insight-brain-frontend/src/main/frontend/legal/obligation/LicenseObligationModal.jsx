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
import { OBLIGATION_STATUS_TO_DISPLAY, OBLIGATION_STATUSES } from '../advancedLegalConstants';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, licenseObligationPropType } from '../advancedLegalPropTypes';
import { createScopeOption } from '../legalUtility';

const { initialState, userInput } = nxTextInputStateHelpers;

export default function LicenseObligationModal(props) {
  const {
    // actions
    setObligationStatus,
    setObligationComment,
    setObligationScope,
    saveObligation,
    cancelObligationModal,
    createObligationStatusIcon,
    // state
    licenseObligation,
    availableScopes,
  } = props;

  const isDirty = () =>
    licenseObligation.comment !== licenseObligation.originalComment ||
    licenseObligation.status !== licenseObligation.originalStatus ||
    licenseObligation.ownerId !== licenseObligation.originalOwnerId;

  const validationErrorMessage = 'Must change obligation status, or comments, or scope.';

  const createStatusDropdownLabel = () => {
    return (
      <Fragment>
        {createObligationStatusIcon(licenseObligation.status)}
        <span>{OBLIGATION_STATUS_TO_DISPLAY[licenseObligation.status]}</span>
      </Fragment>
    );
  };

  const [isStatusDropdownOpen, setStatusDropdownOpen] = useState(false);

  const createModalDropdownOptions = () => {
    return OBLIGATION_STATUSES.filter((obligationStatus) => obligationStatus !== licenseObligation.status).map(
      (obligationStatus) => {
        return (
          <button
            key={obligationStatus + '-dropdown-option'}
            type="button"
            className="nx-dropdown-button"
            onClick={() => {
              setStatusDropdownOpen(false);
              setObligationStatus({
                name: licenseObligation.name,
                value: obligationStatus,
              });
            }}
          >
            {createObligationStatusIcon(obligationStatus)}
            <span>{OBLIGATION_STATUS_TO_DISPLAY[obligationStatus]}</span>
          </button>
        );
      }
    );
  };

  const [commentTextInput, setCommentTextInput] = useState(initialState(licenseObligation.comment));

  return (
    <NxModal key={licenseObligation.name} onClose={() => cancelObligationModal({ name: licenseObligation.name })}>
      <NxStatefulForm
        onCancel={() => cancelObligationModal({ name: licenseObligation.name })}
        submitBtnText="Submit"
        onSubmit={() => saveObligation(licenseObligation.name)}
        submitError={licenseObligation.error}
        submitMaskState={licenseObligation.saveObligationSubmitMask}
        validationErrors={isDirty() ? undefined : validationErrorMessage}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Edit Review Status</h2>
        </header>
        <div className="nx-modal-content">
          <NxFormGroup label="Review Status" isRequired>
            <NxDropdown
              label={createStatusDropdownLabel()}
              isOpen={isStatusDropdownOpen}
              onToggleCollapse={() => setStatusDropdownOpen(!isStatusDropdownOpen)}
            >
              {createModalDropdownOptions()}
            </NxDropdown>
          </NxFormGroup>
          <NxFormGroup label="Comments">
            <NxTextInput
              className="license-obligations-modal-textarea"
              type="textarea"
              {...commentTextInput}
              onChange={(payload) => {
                setObligationComment({
                  name: licenseObligation.name,
                  value: payload,
                });
                setCommentTextInput(userInput(null, payload));
              }}
            />
          </NxFormGroup>
          <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
            <NxFormSelect
              className="nx-form-select--long"
              value={licenseObligation.ownerId}
              onChange={(payload) =>
                setObligationScope({
                  name: licenseObligation.name,
                  value: payload.currentTarget.value,
                })
              }
            >
              {availableScopes.values.map(createScopeOption)}
            </NxFormSelect>
          </NxFormGroup>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

LicenseObligationModal.propTypes = {
  setObligationStatus: PropTypes.func.isRequired,
  setObligationComment: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
  saveObligation: PropTypes.func.isRequired,
  cancelObligationModal: PropTypes.func.isRequired,
  createObligationStatusIcon: PropTypes.func.isRequired,
  licenseObligation: licenseObligationPropType,
  availableScopes: availableScopesPropType,
};
