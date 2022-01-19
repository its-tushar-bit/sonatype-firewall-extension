/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq, compose, toLower, findIndex, __ } from 'ramda';
import {
  NxForm,
  NxFieldset,
  NxTextInput,
  NxRadio,
  NxCheckbox,
  NxLoadingSpinner,
} from '@sonatype/react-shared-components';

import { capitalize, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { getStatusName } from 'MainRoot/legal/legalUtility';
import { isOverriddenOrSelected, renderLicensesList } from '../LegalTabUtils';
import { licensesPropTypes, licenseOverridePropTypes } from '../LicenseDetectionsTile/LicenseDetections';
import OverriddenField from './OverriddenField';

const NOT_DIRTY_ERROR_MESSAGE = 'There are no changes to update';
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
  declaredlicenses,
  effectiveLicenses,
  observedlicenses,
  selectableLicenses,
  availableLicenseScopes,
  submitError,
  submitMaskState,
  identificationSource,
  setShowUnsavedChangesModal,
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
    setLicenseComment(targetScope.licenseOverride?.comment ?? '');
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
      return NOT_DIRTY_ERROR_MESSAGE;
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
    !!availableLicenseScopes.length &&
    canInheritStatus() && <option value="DELETE">Inherit Status ({getInheritableStatus()})</option>;

  const licenseStatuses = getLicenseStatuses(!!selectableLicenses.length);
  const statusField = (
    <NxFieldset className="iq-edit-licenses-form__status" label="Status" isRequired>
      <select id="status-select" className="nx-form-select" onChange={onStatusChange} value={status || ''}>
        {licenseStatuses.map(({ name, value }) => (
          <option key={name} value={value}>
            {name}
          </option>
        ))}
        {inheritStatusOption()}
      </select>
    </NxFieldset>
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
    <dl className="nx-read-only">
      <dt className="nx-read-only__label">Declared Licenses</dt>
      <dd className="nx-read-only__data" id="declared-licenses-container">
        {renderLicensesList(declaredlicenses, isClaimed)}
      </dd>
      <dt className="nx-read-only__label">Observed Licenses</dt>
      <dd className="nx-read-only__data " id="observed-licenses-container">
        {renderLicensesList(observedlicenses, isClaimed)}
      </dd>
      <dt className="nx-read-only__label">Effective Licenses</dt>
      <dd className="nx-read-only__data" id="effective-licenses-container">
        {renderLicensesList(effectiveLicenses, isClaimed, true)}
      </dd>
    </dl>
  );

  const commentField = (
    <div className="nx-form-group iq-edit-licenses-form__comment">
      <label className="nx-label">
        <span className="nx-label__text">Comment</span>
        <NxTextInput type="textarea" maxLength={1000} {...comment} onChange={setLicenseComment} />
      </label>
    </div>
  );

  const scopeField = (
    <NxFieldset className="iq-edit-licenses-form__scope" label="Scope" isRequired>
      {availableLicenseScopes?.map(({ ownerId, ownerName, ownerType, licenseOverride }) => (
        <NxRadio
          name="license-scope-target"
          value={ownerId}
          isChecked={scope.ownerId === ownerId}
          key={ownerId}
          onChange={handleScopeChange}
        >
          {capitalize(ownerType)} - {ownerName}
          {!isNilOrEmpty(licenseOverride) && (
            <span className="iq-edit-licenses-form__scope-status"> ({getStatusName(licenseOverride.status)})</span>
          )}
        </NxRadio>
      ))}
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
    <NxForm
      id="iq-edit-licenses-form"
      onSubmit={handleOnSubmit}
      submitBtnText="Save"
      submitError={submitError}
      submitMaskState={submitMaskState}
      submitMaskMessage="Saving..."
      validationErrors={getValidationErrors()}
      onCancel={handleOnCancel}
    >
      <div className="nx-grid-row">
        <div className="nx-grid-col iq-license-info-section">{licenseInfoSection}</div>
        <div className="nx-grid-col iq-license-form-fields">
          {scopeField}
          {statusField}
          {status === 'SELECTED' && selectedLicensesField}
          {overriddenFormField}
          {commentField}
        </div>
      </div>
    </NxForm>
  );
}

EditLicensesForm.propTypes = {
  allLicenses: PropTypes.arrayOf(licensesPropTypes),
  declaredlicenses: PropTypes.arrayOf(licensesPropTypes),
  effectiveLicenses: PropTypes.arrayOf(licensesPropTypes),
  observedlicenses: PropTypes.arrayOf(licensesPropTypes),
  selectableLicenses: PropTypes.arrayOf(licensesPropTypes),
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
};
