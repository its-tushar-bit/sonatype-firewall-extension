/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import { compose, filter, mergeAll, path, union } from 'ramda';
import {
  NxCheckbox,
  NxFieldset,
  NxStatefulForm,
  NxFormSelect,
  NxFormGroup,
  NxModal,
  NxTextInput,
  NxThreatIndicator,
  nxTextInputStateHelpers,
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { availableScopesPropType, componentPropType, licenseLegalMetadataPropType } from '../advancedLegalPropTypes';
import { createScopeOption, findSingleLicenseIndex, getStatusName } from '../legalUtility';
import { EDIT_LICENSE_MODAL_STATUS_OPTIONS } from '../advancedLegalConstants';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export default function LicensesModal(props) {
  const {
    component,
    licenseLegalMetadata,
    setShowLicensesModal,
    saveLicenses,
    availableScopes,
    loadLicenseModalInformation,
    ownerId,
    hash,
  } = props;
  const {
    declaredLicenses,
    effectiveLicenses,
    observedLicenses,
    allLicenses,
    saveLicensesSubmitMask,
    licensesError,
    effectiveLicenseStatus,
  } = component.licenseLegalData;
  const hierarchy = component.licenseLegalData.hierarchy || [];
  const { initialState, userInput } = nxTextInputStateHelpers;
  const [commentsTextInput, setCommentsTextInput] = useState(initialState(''));
  const [scopeVal, setScopeVal] = useState('');
  const [validationErrors, setValidationErrors] = useState(MSG_NO_CHANGES_TO_SAVE);
  const [statusVal, setStatusVal] = useState(effectiveLicenseStatus);
  const [licenseOptions, setLicenseOptions] = useState([]);
  const [showLicenseDiv, setShowLicenseDiv] = useState(
    effectiveLicenseStatus === 'Selected' || effectiveLicenseStatus === 'Overridden'
  );
  const [checkedLicenses, setCheckedLicenses] = useState(new Set(getInitialCheckedLicenses()));

  function getInitialCheckedLicenses() {
    return effectiveLicenseStatus === 'Selected' || effectiveLicenseStatus === 'Overridden' ? effectiveLicenses : [];
  }

  function setScopeValueFromHierarchy() {
    const ownerPublicId = (hierarchy.find((h) => h.licenseOverride !== null) || {}).ownerId;
    const scopes = (availableScopes && availableScopes.values) || [];
    setScopeVal((scopes.find((scope) => scope.publicId === ownerPublicId) || scopes[0]).id);
  }

  function load() {
    const visitedScope = availableScopes.values[0];
    const componentIdentifier = JSON.stringify(component.componentIdentifier);
    loadLicenseModalInformation({
      ownerType: visitedScope.type,
      ownerId: visitedScope.publicId,
      componentIdentifier,
    });
  }

  useEffect(load, []);
  useEffect(() => populateLicenseOptions(statusVal), [allLicenses]);
  useEffect(() => setScopeValueFromHierarchy(), [hierarchy, availableScopes]);

  const getHierarchyIndexById = (id) => hierarchy.findIndex((h) => h.ownerId === id);

  const canInherit = getHierarchyIndexById(ownerId) < hierarchy.length - 1;

  function getMostValidOverride() {
    const index = getHierarchyIndexById(ownerId) + 1;
    return hierarchy.slice(index).find(({ licenseOverride }) => licenseOverride);
  }

  const getInheritableStatus = () => {
    const override = getMostValidOverride();
    return override ? getStatusName(override.licenseOverride.status) : 'Open';
  };

  const displaySingleLicenseInGroup = (license) => {
    const threatLevel = path(
      [findSingleLicenseIndex(license || '', licenseLegalMetadata || []), 'threatGroup', 'threatLevel'],
      licenseLegalMetadata
    );
    return (
      <div className="license-modal--license" key={license}>
        <NxThreatIndicator policyThreatLevel={threatLevel} />
        <span>{license}</span>
      </div>
    );
  };

  const getAllSingleLicenses = (licenses) => {
    return [
      ...new Set(
        licenses.reduce((acc, l) => {
          const metadata = licenseLegalMetadata.find((m) => m.licenseId === l);
          if (!metadata) {
            return l;
          }
          return acc.concat(!metadata.isMulti ? metadata.licenseId : metadata.singleLicenseIds);
        }, [])
      ),
    ].sort();
  };

  const displayLicenseGroup = (headerTitle, id, licenses) => {
    return (
      <section id={id}>
        <header>
          <h3 className="nx-h3">{headerTitle}</h3>
        </header>
        <div>{getAllSingleLicenses(licenses).map((license) => displaySingleLicenseInGroup(license))}</div>
      </section>
    );
  };

  const createOptionFromValue = (status) => {
    const displayValue = status.inheritable ? `Inherit Status (${status.status})` : status.status;
    const value = status.inheritable ? 'Inherit' : status.status;
    return (
      <option id={`edit-license-status-option-${displayValue}`} key={displayValue} value={value}>
        {displayValue}
      </option>
    );
  };

  const toggleCheckbox = (license) => {
    setValidationErrors(undefined);
    if (checkedLicenses.has(license)) {
      const newCheckedLicenseSet = new Set(checkedLicenses);
      newCheckedLicenseSet.delete(license);
      setCheckedLicenses(newCheckedLicenseSet);
    } else {
      setCheckedLicenses(new Set(checkedLicenses).add(license));
    }
  };

  const createCheckBoxFromLicense = (license) => {
    return (
      <NxCheckbox
        key={license}
        id={license}
        isChecked={checkedLicenses.has(license)}
        onChange={() => toggleCheckbox(license)}
      >
        {license}
      </NxCheckbox>
    );
  };

  const getStatusOptions = () => {
    const basicStatusOptions = EDIT_LICENSE_MODAL_STATUS_OPTIONS.map((status) => ({ status, inheritable: false }));
    const inheritStatusOption = canInherit ? [{ status: getInheritableStatus(), inheritable: true }] : [];
    return [...basicStatusOptions, ...inheritStatusOption];
  };

  const populateLicenseOptions = (statusSelection) => {
    switch (statusSelection) {
      case 'Selected':
        setLicenseOptions(getSelectedLicensesOptions());
        break;
      case 'Overridden':
        setLicenseOptions(getOverriddenLicenses());
        break;
      default:
        setLicenseOptions([]);
    }
  };

  const onStatusChange = (statusSelection) => {
    setStatusVal(statusSelection);
    setValidationErrors(undefined);
    setCheckedLicenses(new Set([]));
    setShowLicenseDiv(statusSelection === 'Selected' || statusSelection === 'Overridden');
    populateLicenseOptions(statusSelection);
  };

  function getSelectedLicensesOptions() {
    return [
      ...new Set(
        union(declaredLicenses, observedLicenses)
          .map((license) => {
            const licenseMetadata = licenseLegalMetadata.find((metadata) => metadata.licenseId === license);
            return licenseMetadata.isMulti ? licenseMetadata.singleLicenseIds : licenseMetadata.licenseId;
          })
          .flat()
      ),
    ].sort();
  }

  const getOverriddenLicenses = () => allLicenses || [];

  const getOwnerInformationFromScopeDropdown = () =>
    compose(
      mergeAll,
      filter((scope) => scope.id === scopeVal)
    )(availableScopes.values);

  const getLicenseIdsToSave = (status) => {
    if (status === 'Selected' || status === 'Overridden') {
      return Array.from(checkedLicenses);
    } else if (status === 'Inherit') {
      return Array.from(getMostValidOverride().licenseOverride.licenseIds);
    }
    return [];
  };

  const updateScopeOption = (scope) => {
    setValidationErrors(undefined);
    setScopeVal(scope);
  };

  const updateCommentsInput = (payload) => {
    setValidationErrors(undefined);
    setCommentsTextInput(userInput(null, payload));
  };

  const trySave = () => {
    const ownerInformation = getOwnerInformationFromScopeDropdown();
    const statusValueToSave = statusVal === 'Inherit' ? getInheritableStatus() : statusVal;
    const postBody = {
      componentIdentifier: component.componentIdentifier,
      comment: commentsTextInput.value,
      status: statusValueToSave.toUpperCase(),
      ownerId: ownerInformation.publicId,
      licenseIds: getLicenseIdsToSave(statusVal),
    };
    saveLicenses({
      ownerType: ownerInformation.type,
      ownerId: ownerInformation.publicId,
      postBody,
      hash,
      componentIdentifier: JSON.stringify(component.componentIdentifier),
    });
  };

  return (
    <NxModal
      id="edit-licenses-modal"
      onClose={() => {
        setShowLicensesModal(false);
      }}
      variant="wide"
    >
      <NxStatefulForm
        onCancel={() => {
          setShowLicensesModal(false);
        }}
        submitError={licensesError}
        submitMaskState={saveLicensesSubmitMask}
        onSubmit={trySave}
        validationErrors={validationErrors}
        submitBtnText="Save"
      >
        <header className="nx-modal-header">
          <h2 className="nx-h2">Edit Licenses</h2>
        </header>
        <div className="nx-modal-content">
          <div className="nx-grid-row">
            <div className="nx-grid-col nx-grid-col--25">
              {displayLicenseGroup('Declared Licenses', 'licenses-modal-declared-licenses', declaredLicenses)}
              {displayLicenseGroup('Effective Licenses', 'licenses-modal-effective-licenses', effectiveLicenses)}
              {displayLicenseGroup('Observed Licenses', 'licenses-modal-observed-licenses', observedLicenses)}
            </div>
            <div className="nx-grid-col">
              <NxFormGroup label="Status" isRequired>
                <NxFormSelect
                  id="edit-licenses-status-selection"
                  value={statusVal}
                  onChange={(event) => onStatusChange(event.target.value)}
                >
                  {getStatusOptions().map(createOptionFromValue)}
                </NxFormSelect>
              </NxFormGroup>
              {showLicenseDiv && (
                <NxFieldset label="Licenses" isRequired>
                  <div className="nx-scrollable nx-scrollable--edit-license-modal-licenses edit-license-modal-border">
                    {licenseOptions.map(createCheckBoxFromLicense)}
                  </div>
                </NxFieldset>
              )}
              <NxFormGroup label="Comments" isRequired>
                <NxTextInput
                  type="textarea"
                  {...commentsTextInput}
                  className="nx-text-input--long edit-licenses-modal-textarea"
                  onChange={(payload) => updateCommentsInput(payload)}
                />
              </NxFormGroup>
              <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
                <NxFormSelect
                  id="edit-licenses-scope-selection"
                  className="nx-form-select--long"
                  value={scopeVal}
                  onChange={(event) => updateScopeOption(event.currentTarget.value)}
                >
                  {availableScopes.values.map(createScopeOption)}
                </NxFormSelect>
              </NxFormGroup>
            </div>
          </div>
        </div>
      </NxStatefulForm>
    </NxModal>
  );
}

LicensesModal.propTypes = {
  component: componentPropType,
  licenseLegalMetadata: licenseLegalMetadataPropType,
  setShowLicensesModal: PropTypes.func,
  saveLicenses: PropTypes.func,
  ownerId: PropTypes.string,
  hash: PropTypes.string,
  loadLicenseModalInformation: PropTypes.func,
  availableScopes: availableScopesPropType,
};
