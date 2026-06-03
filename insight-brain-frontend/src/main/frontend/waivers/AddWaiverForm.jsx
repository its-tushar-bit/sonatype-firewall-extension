/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq } from 'ramda';
import classnames from 'classnames';
import {
  NxFieldset,
  NxTextInput,
  NxRadio,
  NxDateInput,
  NxFormSelect,
  NxTooltip,
  NxStatefulForm,
  NxTextLink,
} from '@sonatype/react-shared-components';

import ViolationExclamation from '../react/ViolationExclamation';
import ArtifactNameDisplay from '../react/ArtifactNameDisplay';
import VulnerabilityDetailsModalContainer from '../vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import {
  useWaiverExpirations,
  isCustomExpiryTimeValid,
  isCustomExpiryTimeSelected,
  isNeverExpiryTimeSelected,
  isExpireWhenRemediationAvailableSelected,
  getExpirationDaysMessage,
  waiverMatcherStrategy,
} from 'MainRoot/util/waiverUtils';
import IqScopeDropdown from 'MainRoot/react/iqScopeDropdown/IqScopeDropdown';

export default function AddWaiverForm(props) {
  const {
    componentIdentifier,
    componentMatcherStrategy,
    artifactName,
    componentName,
    allVersionsComponentName,
    constraintName,
    policyName,
    policyViolationId,
    reasons,
    threatLevelCategory,
    waiverComments,
    availableWaiverScopes,
    waiverReasons,
    selectedWaiverScope,
    expiryTime,
    customExpiryTime,
    submitError,
    openVulnerabilityDetailsModal,
    closeVulnerabilityDetailsModal,
    setWaiverScope,
    setWaiverComment,
    setComponentMatcherStrategy,
    setExpiryTime,
    setWaiverReason,
    waiverReasonId,
    setCustomExpiryTime,
    saveWaiver,
    vulnerabilityId,
    cancelAction,
    currentUser,
    componentDisplayName,
    isExpireWhenRemediationAvailable,
  } = props;

  useEffect(() => {
    return () => closeVulnerabilityDetailsModal();
  }, []);

  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);

  const customExpiryTimeSelected = isCustomExpiryTimeSelected(expiryTime);

  const neverExpiryTimeSelected = isNeverExpiryTimeSelected(expiryTime);

  const expireWhenRemediationAvailableSelected = isExpireWhenRemediationAvailableSelected(expiryTime);

  const waiverReasonsToRender = [{ id: '', reasonText: 'Select a reason', type: 'system' }, ...waiverReasons];

  const getExpiration = () => {
    if (customExpiryTimeSelected) {
      return customExpiryTime.value;
    }
    if (neverExpiryTimeSelected || expireWhenRemediationAvailableSelected) {
      return null;
    }
    return parseInt(expiryTime, 10);
  };

  const onSubmit = () => {
    if (customExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime.value)) {
      return;
    }

    const { type, id } = selectedWaiverScope;
    const { value } = waiverComments;
    const expiration = getExpiration();

    saveWaiver(
      policyViolationId,
      type,
      id,
      value,
      componentMatcherStrategy,
      expiration,
      waiverReasonId,
      expireWhenRemediationAvailableSelected
    );
  };

  const onVulnerabilityDetailsClick = () => {
    openVulnerabilityDetailsModal({
      vulnerabilityId: vulnerabilityId,
      componentIdentifier,
    });
  };

  const handleComponentsChange = (value) => {
    setComponentMatcherStrategy(value);
  };

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    setWaiverScope(target);
  };

  const extractScopeOptionText = ({ label, name }) => {
    switch (label) {
      case 'Repository_container':
        return name;
      case 'Repository_manager':
        return `Repository Manager - ${name}`;
      default:
        return `${label} - ${name}`;
    }
  };

  const onExpiryTimeChange = (value) => {
    setExpiryTime(value === 'never' ? null : value);
  };

  const onReasonChange = (value) => {
    setWaiverReason(value ?? null);
  };

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  const replaceUnknownComponentNameByComponentDisplayName = (componentName) =>
    componentName === 'Unknown' ? componentDisplayName : componentName;

  const getAllVersionsRadioButton = () => {
    if (componentIdentifier === null) {
      return (
        <NxTooltip title="Claim this component to apply all versions waiver">
          <NxRadio
            id="all-versions"
            name="add-waiver-components"
            value={waiverMatcherStrategy.ALL_VERSIONS}
            isChecked={false}
            onChange={() => {}}
            disabled={true}
          >
            {allVersionsComponentName === 'Unknown' ? 'All Versions' : `${allVersionsComponentName} (all versions)`}
          </NxRadio>
        </NxTooltip>
      );
    } else {
      return (
        <NxRadio
          id="all-versions"
          name="add-waiver-components"
          value={waiverMatcherStrategy.ALL_VERSIONS}
          isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_VERSIONS}
          onChange={handleComponentsChange}
        >
          {allVersionsComponentName} (all versions)
        </NxRadio>
      );
    }
  };

  const daysDiffMessage = getExpirationDaysMessage(expiryTime, customExpiryTime);

  return (
    <NxStatefulForm
      className="iq-add-waiver-form"
      onCancel={cancelAction}
      submitError={submitError?.response?.data || submitError?.message}
      showValidationErrors={!!submitError}
      submitBtnClasses="add-waiver-submit"
      onSubmit={onSubmit}
    >
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Waiver Configuration</h2>
        </div>
      </header>

      <div className="nx-tile-content">
        {/* Component Info */}
        <div className="nx-read-only iq-add-waiver-form__component">
          <header className="nx-read-only__label">
            <ArtifactNameDisplay
              {...{ artifactName: replaceUnknownComponentNameByComponentDisplayName(artifactName) }}
            />
          </header>
          <div className="nx-read-only__data">{replaceUnknownComponentNameByComponentDisplayName(componentName)}</div>
        </div>

        {/* Policy Info */}
        <div className="nx-read-only iq-add-waiver-form__policy">
          <header className="nx-read-only__label">Policy</header>
          <div className="nx-read-only__data">
            <ViolationExclamation threatLevelCategory={threatLevelCategory} />
            <span className={policyClassnames}>{policyName}</span>
          </div>
        </div>

        {/* Constraint Info */}
        <div className="nx-read-only iq-add-waiver-form__constraint">
          <header className="nx-read-only__label">Constraint Name</header>
          <div className="nx-read-only__data">{constraintName}</div>
        </div>

        {/* Conditions */}
        <div className="nx-read-only iq-add-waiver-form__conditions">
          <header className="nx-read-only__label">Conditions</header>
          {reasons &&
            reasons.map((reason, index) => (
              <div className="nx-read-only__data" key={index}>
                <span>{reason}</span>
              </div>
            ))}
        </div>

        {vulnerabilityId && (
          <div className="iq-add-waiver-form__vulnerability_details_link">
            <NxTextLink onClick={onVulnerabilityDetailsClick}>See Security Vulnerability Details</NxTextLink>
            <VulnerabilityDetailsModalContainer />
          </div>
        )}

        {/* Scope */}
        <NxFieldset className="iq-add-waiver-form__scope" label="Scope" isRequired>
          <IqScopeDropdown
            id="iq-add-waiver-scope"
            onChangeHandler={handleScopeChange}
            availableScopes={availableWaiverScopes}
            getOptionText={extractScopeOptionText}
            currentValue={selectedWaiverScope.id}
          />
        </NxFieldset>

        {/* Components */}
        <NxFieldset className="iq-add-waiver-form__components" label="Components" isRequired>
          <NxRadio
            id="current-component"
            name="add-waiver-components"
            value={waiverMatcherStrategy.EXACT_COMPONENT}
            isChecked={componentMatcherStrategy === waiverMatcherStrategy.EXACT_COMPONENT}
            onChange={handleComponentsChange}
          >
            {replaceUnknownComponentNameByComponentDisplayName(componentName)}
          </NxRadio>
          {getAllVersionsRadioButton()}
          <NxRadio
            id="all-components"
            name="add-waiver-components"
            value={waiverMatcherStrategy.ALL_COMPONENTS}
            isChecked={componentMatcherStrategy === waiverMatcherStrategy.ALL_COMPONENTS}
            onChange={handleComponentsChange}
          >
            All Components
          </NxRadio>
        </NxFieldset>

        {/* Expiry time */}
        <NxFieldset className="iq-add-waiver-form__expiryTime" label="Waiver Expiration" isRequired>
          <div className="nx-form-row iq-add-waiver-form__expiryTime-block">
            <div className="iq-add-waiver-form__select-block">
              <NxFormSelect id="waiver-expiration-select" onChange={onExpiryTimeChange}>
                {waiverExpirations.map(({ name, value }, index) => (
                  <option key={index} value={value}>
                    {name}
                  </option>
                ))}
              </NxFormSelect>
              <div className="iq-add-waiver-form__expiration-days-diff visual-testing-ignore">{daysDiffMessage}</div>
              {customExpiryTimeSelected && (
                <NxDateInput
                  className="iq-add-waiver-form__date-input"
                  {...customExpiryTime}
                  onChange={setCustomExpiryTime}
                  validatable={true}
                />
              )}
            </div>
          </div>
        </NxFieldset>

        {/* Reason */}
        <NxFieldset className="iq-add-waiver-form__reason" label="Reason">
          <NxFormSelect id="waiver-reason-select" onChange={onReasonChange}>
            {waiverReasonsToRender.map(({ id, reasonText }) => (
              <option key={id} value={id} selected={waiverReasonId && id === waiverReasonId}>
                {reasonText}
              </option>
            ))}
          </NxFormSelect>
        </NxFieldset>

        {/* Comments */}
        <NxFieldset className="iq-add-waiver-form__comments" label="Comments">
          <NxTextInput
            type="textarea"
            inputAttributes={{ maxLength: 1000 }}
            {...waiverComments}
            onChange={setWaiverComment}
          />
        </NxFieldset>

        {/* Created By */}
        <div className="nx-read-only iq-add-waiver-form__created-by">
          <header className="nx-read-only__label">Created By</header>
          <div className="nx-read-only__data">{currentUser}</div>
        </div>
      </div>
    </NxStatefulForm>
  );
}

export const waiverScopePropTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
};

AddWaiverForm.propTypes = {
  componentMatcherStrategy: PropTypes.string.isRequired,
  allVersionsComponentName: PropTypes.string,
  artifactName: PropTypes.string.isRequired,
  componentName: PropTypes.string.isRequired,
  constraintName: PropTypes.string.isRequired,
  policyName: PropTypes.string.isRequired,
  policyViolationId: PropTypes.string.isRequired,
  reasons: PropTypes.arrayOf(PropTypes.string).isRequired,
  threatLevelCategory: ViolationExclamation.propTypes.threatLevelCategory,
  waiverComments: PropTypes.shape({
    value: PropTypes.string.isRequired,
    isPristine: PropTypes.bool.isRequired,
  }).isRequired,
  availableWaiverScopes: PropTypes.arrayOf(PropTypes.shape(waiverScopePropTypes)).isRequired,
  waiverReasons: PropTypes.arrayOf(
    PropTypes.shape({ id: PropTypes.string, reasonText: PropTypes.string, type: PropTypes.string })
  ),
  selectedWaiverScope: PropTypes.shape(waiverScopePropTypes).isRequired,
  expiryTime: PropTypes.string,
  waiverReasonId: PropTypes.string,
  customExpiryTime: PropTypes.shape({
    value: PropTypes.string,
    isPristine: PropTypes.bool,
  }).isRequired,
  submitError: PropTypes.instanceOf(Error),
  setWaiverScope: PropTypes.func.isRequired,
  setComponentMatcherStrategy: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  setWaiverReason: PropTypes.func.isRequired,
  setCustomExpiryTime: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  closeVulnerabilityDetailsModal: PropTypes.func.isRequired,
  vulnerabilityId: PropTypes.string,
  cancelAction: PropTypes.func.isRequired,
  componentIdentifier: PropTypes.object,
  currentUser: PropTypes.string,
  componentDisplayName: PropTypes.string,
};
