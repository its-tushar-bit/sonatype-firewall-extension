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
  NxForm,
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

  const getInheritableStatus = () => {
    const index = getHierarchyIndexById(ownerId) + 1;
    const override = hierarchy.slice(index).find(({ licenseOverride }) => licenseOverride);
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

  const createOptionFromValue = (value) => (
    <option id={`edit-license-status-option-${value}`} key={value} value={value}>
      {value}
    </option>
  );

  const toggleCheckbox = (license) => {
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
    const inheritedStatus = getInheritableStatus();
    return [...EDIT_LICENSE_MODAL_STATUS_OPTIONS, ...(canInherit ? [`Inherit Status (${inheritedStatus})`] : [])];
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
    setCheckedLicenses(new Set([]));
    setShowLicenseDiv(statusSelection === 'Selected' || statusSelection === 'Overridden');
    populateLicenseOptions(statusSelection);
  };

  function getSelectedLicensesOptions() {
    return [
      ...new Set(
        union(declaredLicenses, observedLicenses).map((license) => {
          const licenseMetadata =
            licenseLegalMetadata[findSingleLicenseIndex(license || '', licenseLegalMetadata || [])];
          return licenseMetadata.licenseId;
        })
      ),
    ].sort();
  }

  const getOverriddenLicenses = () => allLicenses || [];

  const getOwnerInformationFromScopeDropdown = () =>
    compose(
      mergeAll,
      filter((scope) => scope.id === scopeVal)
    )(availableScopes.values);

  const trySave = () => {
    const ownerInformation = getOwnerInformationFromScopeDropdown();
    const postBody = {
      componentIdentifier: component.componentIdentifier,
      comment: commentsTextInput.value,
      status: statusVal.toUpperCase(),
      ownerId: ownerInformation.publicId,
      licenseIds: statusVal === 'Selected' || statusVal === 'Overridden' ? Array.from(checkedLicenses) : [],
    };
    saveLicenses({
      ownerType: ownerInformation.type,
      ownerId: ownerInformation.publicId,
      postBody,
      hash,
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
      <NxForm
        onCancel={() => {
          setShowLicensesModal(false);
        }}
        submitError={licensesError}
        submitMaskState={saveLicensesSubmitMask}
        onSubmit={trySave}
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
                <select
                  id="edit-licenses-status-selection"
                  className="nx-form-select"
                  value={statusVal}
                  onChange={(event) => onStatusChange(event.currentTarget.value)}
                >
                  {getStatusOptions().map(createOptionFromValue)}
                </select>
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
                  onChange={(payload) => setCommentsTextInput(userInput(null, payload))}
                />
              </NxFormGroup>
              <NxFormGroup label="Scope" sublabel="Apply changes to" isRequired>
                <select
                  id="edit-licenses-scope-selection"
                  className="nx-form-select nx-form-select--long"
                  value={scopeVal}
                  onChange={(event) => setScopeVal(event.currentTarget.value)}
                >
                  {availableScopes.values.map(createScopeOption)}
                </select>
              </NxFormGroup>
            </div>
          </div>
        </div>
      </NxForm>
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
