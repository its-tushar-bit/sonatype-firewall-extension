/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxStatefulForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  NxToggle,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, legalFilesPropType, licenseObligationPropType } from '../../advancedLegalPropTypes';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';
import ObligationStatusComponent from '../../shared/ObligationStatusComponent';

export default function LicenseFilesModal(props) {
  const {
    // actions
    cancelLicenseFilesModal,
    setLicenseFileContent,
    setLicenseFileStatus,
    addLicenseFile,
    setLicenseFilesScope,
    saveLicenseFiles,
    setObligationStatus,
    setObligationScope,
    // state
    scope,
    originalScope,
    availableScopes,
    licenses,
    error,
    submitMaskState,
    existingObligation,
  } = props;

  const createFormRowItem = (license, index) => (
    <tr id={'license-row-' + index} key={index}>
      <td>
        <NxTextInput
          id={'license-text-input-' + index}
          className="nx-text-input nx-text-input--full"
          type="textarea"
          value={license.content}
          isPristine={license.isPristine}
          onChange={(payload) => setLicenseFileContent({ index: index, value: payload })}
          disabled={license.status === 'disabled'}
        />
      </td>
      <td>
        <NxToggle
          inputId={'license-status-toggle-' + index}
          onChange={() =>
            setLicenseFileStatus({
              index: index,
              value: license.status === 'enabled' ? 'disabled' : 'enabled',
            })
          }
          className="nx-toggle nx-toggle--no-gap"
          isChecked={license.status === 'enabled'}
        >
          {license.status === 'enabled' ? 'Included' : 'Excluded'}
        </NxToggle>
      </td>
    </tr>
  );

  const createScopeOption = (value) => (
    <option key={value.id} value={value.id}>
      {value.label} - {value.name}
    </option>
  );

  const notValidErrorMessage = 'A custom license must have text.';

  const isValid = () => {
    return !licenses.some(
      (license) => license.id === null && license.originalContentHash === null && license.content === ''
    );
  };

  const notDirtyErrorMessage = 'Must add a new license or change the content or status of a license.';

  function isObligationDirty() {
    return existingObligation && existingObligation.status !== existingObligation.originalStatus;
  }

  const licenseFilesExist = () => licenses && licenses.length > 0;

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

  const isLicensesDirty = () => {
    return (
      scope !== originalScope ||
      licenses.some(
        (license) =>
          (license.id === null && license.originalContentHash === null) ||
          license.content !== license.originalContent ||
          license.status !== license.originalStatus
      )
    );
  };

  const isDirty = () => {
    return isLicensesDirty() || isObligationDirty();
  };

  const getValidationErrors = () => {
    if (!isValid()) {
      return notValidErrorMessage;
    }
    if (!isDirty()) {
      return notDirtyErrorMessage;
    }
    return undefined;
  };

  const getSubmitMaskState = () => {
    const nullIfUndef = (b) => (b === undefined ? null : b);
    const mainSubmitMaskState = nullIfUndef(submitMaskState);
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

  const trySave = () => {
    saveLicenseFiles({
      existingObligation,
      isLicensesDirty: isLicensesDirty(),
      isObligationDirty: isObligationDirty(),
    });
  };

  return (
    <NxModal
      id="edit-licenses-attribution-modal"
      onClose={() => {
        resetExistingObligation();
        cancelLicenseFilesModal();
      }}
      variant="wide"
    >
      <NxStatefulForm
        onCancel={() => {
          resetExistingObligation();
          cancelLicenseFilesModal();
        }}
        submitBtnText="Save"
        onSubmit={trySave}
        submitError={error || existingObligation?.error}
        submitMaskState={getSubmitMaskState()}
        validationErrors={getValidationErrors()}
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">{`${licenseFilesExist() ? 'Edit' : 'Add'} License Files`}</h2>
        </header>
        <div className="nx-modal-content">
          <table className="legal-file-override-table">
            <thead>
              <tr>
                <th>License Text</th>
                <th>Attribution Report Status</th>
              </tr>
            </thead>
            <tbody>
              {licenses.length > 0 ? (
                licenses.map(createFormRowItem)
              ) : (
                <tr>
                  <td className="no-legal-texts-found">No license texts found</td>
                  <td />
                </tr>
              )}
            </tbody>
          </table>
          <div className="nx-btn-bar nx-btn-bar--left">
            <NxButton id="add-license" type="button" variant="tertiary" onClick={addLicenseFile}>
              <NxFontAwesomeIcon icon={faPlus} />
              <span>Add License Text</span>
            </NxButton>
          </div>
          {existingObligation && (
            <ObligationStatusComponent existingObligation={existingObligation} onChange={onObligationChange} />
          )}
          <NxFormGroup
            className="legal-modal-scope-selection-group"
            label="Scope"
            sublabel="Apply changes to"
            isRequired
          >
            <NxFormSelect
              id="edit-license-scope-selection"
              className="nx-form-select--long"
              value={scope}
              onChange={(payload) => {
                setLicenseFilesScope(payload.currentTarget.value);
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
}

LicenseFilesModal.propTypes = {
  cancelLicenseFilesModal: PropTypes.func.isRequired,
  setLicenseFileContent: PropTypes.func.isRequired,
  setLicenseFileStatus: PropTypes.func.isRequired,
  addLicenseFile: PropTypes.func.isRequired,
  setLicenseFilesScope: PropTypes.func.isRequired,
  saveLicenseFiles: PropTypes.func.isRequired,
  scope: PropTypes.string.isRequired,
  originalScope: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  licenses: legalFilesPropType,
  error: PropTypes.string,
  submitMaskState: PropTypes.bool,
  existingObligation: licenseObligationPropType,
  setObligationStatus: PropTypes.func.isRequired,
  setObligationScope: PropTypes.func.isRequired,
};
