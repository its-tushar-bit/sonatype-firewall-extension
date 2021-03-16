/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxForm,
  NxFormGroup,
  NxModal,
  NxTextInput,
  NxToggle
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, legalFilesPropType } from '../../advancedLegalPropTypes';
import { faPlus } from '@fortawesome/pro-solid-svg-icons';

export default function LicensesModal(props) {
  const {
    // actions
    cancelLicensesModal,
    setLicenseContent,
    setLicenseStatus,
    addLicense,
    setLicensesScope,
    saveLicenses,
    // state
    scope,
    originalScope,
    availableScopes,
    licenses,
    error,
    submitMaskState
  } = props;

  const createFormRowItem = (license, index) =>
    <tr id={ 'license-row-' + index } key={ index }>
      <td>
        <NxTextInput id={ 'license-text-input-' + index }
                     className="nx-text-input nx-text-input--full"
                     type="textarea"
                     value={ license.content }
                     isPristine={ license.isPristine }
                     onChange={ payload => setLicenseContent({ index: index, value: payload }) }
                     disabled={ license.status === 'disabled' }/>
      </td>
      <td>
        <NxToggle inputId={ 'license-status-toggle-' + index }
                  onChange={ () => setLicenseStatus(
                      { index: index, value: license.status === 'enabled' ? 'disabled' : 'enabled' }) }
                  className="nx-toggle nx-toggle--no-gap"
                  isChecked={ license.status === 'enabled' }>
          { license.status === 'enabled' ? 'Included' : 'Excluded' }
        </NxToggle>
      </td>
    </tr>;

  const createScopeOption = value => (
    <option key={ value.id } value={ value.id }>{ value.label } - { value.name }</option>
  );

  const notValidErrorMessage = 'A custom license must have text.';

  const isValid = () => {
    return !licenses.some(
        license => license.id === null && license.originalContentHash === null && license.content === '');
  };

  const notDirtyErrorMessage = 'Must add a new license or change the content or status of a license.';

  const isDirty = () => {
    return scope !== originalScope ||
        licenses.some(license =>
          (license.id === null && license.originalContentHash === null) ||
          (license.content !== license.originalContent) ||
          (license.status !== license.originalStatus));
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

  return <NxModal id="edit-licenses-attribution-modal" onClose={ cancelLicensesModal } variant="wide">
    <NxForm onCancel={ cancelLicensesModal }
            submitBtnText="Save"
            onSubmit={ saveLicenses }
            submitError={ error }
            submitMaskState={ submitMaskState }
            validationErrors={ getValidationErrors() }>
      <header className="nx-modal-header">
        <h2 className="nx-h2">
          Edit License Texts
        </h2>
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
            { licenses.length > 0 ? licenses.map(createFormRowItem) :
            <tr><td className="no-legal-texts-found">No license texts found</td><td/></tr> }
          </tbody>
        </table>
        <div className="nx-btn-bar nx-btn-bar--left">
          <NxButton id="add-license" type="button" variant="tertiary" onClick={ addLicense }>
            <NxFontAwesomeIcon icon={ faPlus }/>
            <span>Add License Text</span>
          </NxButton>
        </div>
        <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
          <select id="edit-license-scope-selection"
                  className="nx-form-select nx-form-select--long"
                  value={ scope }
                  onChange={ payload => setLicensesScope(payload.currentTarget.value) }>
            { availableScopes.values.map(createScopeOption) }
          </select>
        </NxFormGroup>
      </div>
    </NxForm>
  </NxModal>;
}

LicensesModal.propTypes = {
  cancelLicensesModal: PropTypes.func.isRequired,
  setLicenseContent: PropTypes.func.isRequired,
  setLicenseStatus: PropTypes.func.isRequired,
  addLicense: PropTypes.func.isRequired,
  setLicensesScope: PropTypes.func.isRequired,
  saveLicenses: PropTypes.func.isRequired,
  scope: PropTypes.string.isRequired,
  originalScope: PropTypes.string.isRequired,
  availableScopes: availableScopesPropType,
  licenses: legalFilesPropType,
  error: PropTypes.string,
  submitMaskState: PropTypes.bool
};
