/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq, compose, toLower, findIndex, __ } from 'ramda';
import {
  NxStatefulForm,
  NxFieldset,
  NxTextInput,
  NxCheckbox,
  NxLoadingSpinner,
  NxFormSelect,
  NxFormGroup,
} from '@sonatype/react-shared-components';

import { capitalize, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { getStatusName } from 'MainRoot/legal/legalUtility';
import { isOverriddenOrSelected, renderLicensesList, renderObservedLicenses } from '../LegalTabUtils';
import {
  licensePropTypes,
  multiLicensesPropTypes,
  licenseOverridePropTypes,
} from '../LicenseDetectionsTile/LicenseDetections';
import OverriddenField from './OverriddenField';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';
import { MSG_NO_CHANGES_TO_UPDATE } from 'MainRoot/util/constants';

const NO_SELECTED_LICENSES_ERROR_MESSAGE = 'There must be at least one selected license';

const getLicenseStatuses = (hasSelectableLicenses) => [
  { name: 'Open', value: 'OPEN' },
  { name: 'Acknowledged', value: 'ACKNOWLEDGED' },
  { name: 'Overridden', value: 'OVERRIDDEN' },
  ...(hasSelectableLicenses ? [{ name: 'Selected', value: 'SELECTED' }] : []),
  { name: 'Confirmed', value: 'CONFIRMED' },
];

const getLicenseIdsFromOverride = (scope) => scope?.licenseOverride?.licenseIds || [];

export default function EditLicensesForm({
  onClose,
  resetFormFields,
  status,
  scope,
  comment,
  licenseIds,
  isDirty,
  setLicenseComment,
  setLicenseStatus,
  setLicenseScope,
  setSelectedLicenses,
  saveForm,
  deleteLicenseOverride,
  allLicenses,
  declaredLicenses,
  effectiveLicenses,
  observedLicenses,
  selectableLicenses,
  availableLicenseScopes,
  submitError,
  submitMaskState,
  identificationSource,
  setShowUnsavedChangesModal,
  hiddenObservedLicenses,
  supportAlpObservedLicenses,
  isAdvancedLegalPackSupported,
}) {
  const [showLoadingSpinnerForOverrideField, setShowLoadingSpinnerForOverrideField] = useState(true);
  useEffect(() => {
    // showLoadingSpinnerForOverrideField is true by default, which ensures
    // the EditLicensesPopover(parent of this form) and NxLoadingSpinner can be rendered prior to
    // the render of OverriddenField(this component is expensive to render when the dataset is large)
    if (status === 'OVERRIDDEN') {
      setTimeout(() => {
        setShowLoadingSpinnerForOverrideField(false);
      }, 0);
    }
  }, [status]);
  const isClaimed = identificationSource === 'Manual';

  const handleScopeChange = (selectedId) => {
    const targetScope = find(propEq('ownerId', selectedId), availableLicenseScopes);
    setSelectedLicenses(getLicenseIdsFromOverride(targetScope));
    setLicenseStatus(targetScope.licenseOverride?.status ?? 'OPEN');
    setLicenseScope(targetScope);
  };

  const formatOwnerType = (ownerType) => {
    switch (ownerType) {
      case 'repository_container':
        return '';
      case 'repository_manager':
        return 'Repository Manager - ';
      default:
        return capitalize(ownerType) + ' - ';
    }
  };

  const extractScopeOptionText = ({ ownerType, ownerName, licenseOverride }) => {
    const licenseOverrideStatus = !isNilOrEmpty(licenseOverride) ? ` (${getStatusName(licenseOverride.status)})` : '';
    return `${formatOwnerType(ownerType)}${ownerName}${licenseOverrideStatus}`;
  };

  const onStatusChange = (event) => {
    setSelectedLicenses([]);
    setLicenseStatus(event.currentTarget.value);
  };

  const toggleSelectedLicense = ({ licenseId }) => {
    if (licenseIds.includes(licenseId)) {
      setSelectedLicenses(licenseIds.filter((id) => id !== licenseId));
    } else {
      setSelectedLicenses([...licenseIds, licenseId]);
    }
  };

  const getValidationErrors = () => {
    if (!isDirty) {
      return MSG_NO_CHANGES_TO_UPDATE;
    }

    const noSelectedLicenses = isOverriddenOrSelected(status) && !licenseIds.length;
    if (noSelectedLicenses) {
      return NO_SELECTED_LICENSES_ERROR_MESSAGE;
    }

    return null;
  };

  const handleOnCancel = () => {
    if (isDirty) {
      setShowUnsavedChangesModal(true);
    } else {
      onClose();
      resetFormFields();
    }
  };

  const handleOnSubmit = () => {
    if (status === 'DELETE') {
      deleteLicenseOverride();
    } else {
      saveForm();
    }
  };

  const getAvailableScopeIndexById = compose(findIndex(__, availableLicenseScopes), propEq('ownerId'));

  const canInheritStatus = () => scope && getAvailableScopeIndexById(scope.ownerId) < availableLicenseScopes.length - 1;

  const getInheritableStatus = () => {
    const index = getAvailableScopeIndexById(scope.ownerId) + 1;
    for (let i = index; i < availableLicenseScopes.length; i++) {
      if (availableLicenseScopes[i].licenseOverride) {
        return compose(capitalize, toLower)(availableLicenseScopes[i].licenseOverride.status);
      }
    }

    return 'Open';
  };

  const inheritStatusOption = () =>
    !!availableLicenseScopes?.length &&
    canInheritStatus() && <option value="DELETE">Inherit Status ({getInheritableStatus()})</option>;

  const licenseStatuses = getLicenseStatuses(!!selectableLicenses?.length);
  const statusField = (
    <NxFormGroup className="iq-edit-licenses-form__status" label="Status" isRequired>
      <NxFormSelect id="status-select" onChange={onStatusChange} value={status || ''}>
        {licenseStatuses.map(({ name, value }) => (
          <option key={name} value={value}>
            {name}
          </option>
        ))}
        {inheritStatusOption()}
      </NxFormSelect>
    </NxFormGroup>
  );

  const selectedLicensesField = (
    <NxFieldset className="iq-edit-licenses-form__selected-licenses" label="Selected Licenses" isRequired>
      {selectableLicenses.map((license, index) => (
        <NxCheckbox
          key={license + index}
          id={license.LicenseId}
          isChecked={licenseIds.includes(license.licenseId)}
          onChange={() => toggleSelectedLicense(license)}
        >
          {license.licenseName}
        </NxCheckbox>
      ))}
    </NxFieldset>
  );

  const licenseInfoSection = (
    <div className="nx-grid-row iq-license-info-section">
      <div className="nx-grid-col nx-grid-col--33">
        <dl className="nx-read-only">
          <dt className="nx-read-only__label">Effective Licenses</dt>
          <dd className="nx-read-only__data" id="effective-licenses-container">
            <ul className="iq-legal-list">{renderLicensesList(effectiveLicenses, isClaimed, true)}</ul>
          </dd>
        </dl>
      </div>
      <div className="nx-grid-col nx-grid-col--33">
        <dl className="nx-read-only">
          <dt className="nx-read-only__label">Declared Licenses</dt>
          <dd className="nx-read-only__data" id="declared-licenses-container">
            <ul className="iq-legal-list">{renderLicensesList(declaredLicenses, isClaimed)}</ul>
          </dd>
        </dl>
      </div>
      <div className="nx-grid-col nx-grid-col--33">
        <dl className="nx-read-only">
          <dt className="nx-read-only__label">Observed Licenses</dt>
          <dd className="nx-read-only__data" id="observed-licenses-container">
            <ul className="iq-legal-list">
              {renderObservedLicenses(
                observedLicenses,
                isClaimed,
                hiddenObservedLicenses,
                isAdvancedLegalPackSupported,
                supportAlpObservedLicenses
              )}
            </ul>
          </dd>
        </dl>
      </div>
    </div>
  );

  const commentField = (
    <NxFormGroup className="iq-edit-licenses-form__comment" label="Comment">
      <NxTextInput
        type="textarea"
        maxLength={1000}
        {...comment}
        className="nx-text-input--long"
        onChange={setLicenseComment}
      />
    </NxFormGroup>
  );

  const scopeField = (
    <NxFieldset className="iq-edit-licenses-form__scope" label="Scope" isRequired>
      <IqScopeDropdown
        id="iq-edit-license-scope"
        onChangeHandler={handleScopeChange}
        availableScopes={availableLicenseScopes}
        getOptionText={extractScopeOptionText}
        currentValue={scope.ownerId}
      />
    </NxFieldset>
  );

  const overriddenFormField = (
    <Fragment>
      {status === 'OVERRIDDEN' && showLoadingSpinnerForOverrideField && <NxLoadingSpinner />}
      {status === 'OVERRIDDEN' && !showLoadingSpinnerForOverrideField && (
        <OverriddenField
          licenseIds={licenseIds}
          allLicenses={allLicenses}
          setSelectedLicenses={setSelectedLicenses}
          onUnmount={() => setShowLoadingSpinnerForOverrideField(true)}
        />
      )}
    </Fragment>
  );

  return (
    <NxStatefulForm
      id="iq-edit-licenses-form"
      onSubmit={handleOnSubmit}
      submitBtnText="Save"
      submitError={submitError}
      submitMaskState={submitMaskState}
      submitMaskMessage="Saving..."
      validationErrors={getValidationErrors()}
      onCancel={handleOnCancel}
    >
      {licenseInfoSection}
      <div>{scopeField}</div>
      <div>{statusField}</div>
      {status === 'SELECTED' && <div>{selectedLicensesField}</div>}
      {status === 'OVERRIDDEN' && <div>{overriddenFormField}</div>}
      <div>{commentField}</div>
    </NxStatefulForm>
  );
}

EditLicensesForm.propTypes = {
  allLicenses: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.string.isRequired,
      displayName: PropTypes.string.isRequired,
    })
  ),
  declaredLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  effectiveLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  observedLicenses: PropTypes.arrayOf(multiLicensesPropTypes),
  selectableLicenses: PropTypes.arrayOf(licensePropTypes),
  availableLicenseScopes: PropTypes.arrayOf(licenseOverridePropTypes),
  licenseIds: PropTypes.arrayOf(PropTypes.string),
  scope: licenseOverridePropTypes,
  comment: PropTypes.shape({
    value: PropTypes.string,
    isPristine: PropTypes.bool,
  }),
  status: PropTypes.string,
  setLicenseComment: PropTypes.func.isRequired,
  setLicenseStatus: PropTypes.func.isRequired,
  setLicenseScope: PropTypes.func.isRequired,
  setSelectedLicenses: PropTypes.func.isRequired,
  isDirty: PropTypes.bool,
  onClose: PropTypes.func.isRequired,
  resetFormFields: PropTypes.func.isRequired,
  saveForm: PropTypes.func.isRequired,
  deleteLicenseOverride: PropTypes.func.isRequired,
  submitError: PropTypes.string,
  submitMaskState: PropTypes.bool,
  identificationSource: PropTypes.string,
  setShowUnsavedChangesModal: PropTypes.func,
  hiddenObservedLicenses: PropTypes.bool,
  supportAlpObservedLicenses: PropTypes.bool,
  isAdvancedLegalPackSupported: PropTypes.bool,
};
