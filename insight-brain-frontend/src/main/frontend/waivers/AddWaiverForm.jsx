/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq } from 'ramda';
import classnames from 'classnames';
import moment from 'moment';
import {
  NxButton,
  NxFieldset,
  NxTextInput,
  NxRadio,
  NxDateInput,
  NxFormSelect,
} from '@sonatype/react-shared-components';

import ViolationExclamation from '../react/ViolationExclamation';
import ArtifactNameDisplay from '../react/ArtifactNameDisplay';
import VulnerabilityDetailsModalContainer from '../vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import LoadError from '../react/LoadError';
import { waiverExpirations } from '../util/waiverUtils';
import ownerConstant from 'MainRoot/utility/services/owner.constant';

const ALL_COMPONENTS = 'ALL_COMPONENTS';

export const isCustomExpiryTimeValid = (value) => {
  if (!value) {
    return false;
  }
  return new Date(value) > new Date();
};

export default function AddWaiverForm(props) {
  const {
    componentIdentifier,
    applyToAllComponents,
    artifactName,
    componentName,
    constraintName,
    policyName,
    policyViolationId,
    reasons,
    threatLevelCategory,
    waiverComments,
    availableWaiverScopes,
    selectedWaiverScope,
    expiryTime,
    customExpiryTime,
    submitError,
    openVulnerabilityDetailsModal,
    closeVulnerabilityDetailsModal,
    setWaiverScope,
    setWaiverComment,
    setApplyToAllComponents,
    setExpiryTime,
    setCustomExpiryTime,
    saveWaiver,
    vulnerabilityId,
    cancelAction,
    currentUser,
  } = props;

  useEffect(() => {
    return () => closeVulnerabilityDetailsModal();
  }, []);

  const isCustomExpiryTimeSelected = expiryTime === 'custom';

  const isNeverExpiryTimeSelected = expiryTime === 'never' || expiryTime === null;

  const getExpiration = () => {
    if (isCustomExpiryTimeSelected) {
      return customExpiryTime.value;
    }
    if (isNeverExpiryTimeSelected) {
      return null;
    }
    return parseInt(expiryTime, 10);
  };

  const onSubmit = (evt) => {
    evt.preventDefault();

    if (isCustomExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime.value)) {
      return;
    }

    const { type, id } = selectedWaiverScope;
    const { value } = waiverComments;
    const expiration = getExpiration();

    saveWaiver(policyViolationId, type, id, value, applyToAllComponents, expiration);
  };

  const onVulnerabilityDetailsClick = () => {
    openVulnerabilityDetailsModal({
      vulnerabilityId: vulnerabilityId,
      componentIdentifier,
    });
  };

  const handleComponentsChange = (value) => {
    setApplyToAllComponents(value === ALL_COMPONENTS);
  };

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    setWaiverScope(target);
  };

  const onExpiryTimeChange = (event) => {
    const value = event.currentTarget.value === 'never' ? null : event.currentTarget.value;
    setExpiryTime(value);
  };

  const getAvailableScopeId = (id, type) => {
    if (id === ownerConstant.ROOT_ORGANIZATION_ID) {
      return 'root-scope';
    } else if (type === ownerConstant.ORGANIZATION_TYPE) {
      return 'organization-scope';
    } else {
      return 'application-scope';
    }
  };

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  const daysDiff = () => {
    if (isCustomExpiryTimeSelected && isCustomExpiryTimeValid(customExpiryTime.value)) {
      const today = moment().startOf('day');
      const customDate = moment(customExpiryTime.value, 'YYYY-MM-DD');
      const diff = Math.floor(moment.duration(customDate.diff(today)).asDays());
      return `This waiver will expire in ${diff} days`;
    }
    if (!isCustomExpiryTimeSelected && !isNeverExpiryTimeSelected) {
      return `This waiver will expire in ${expiryTime} days`;
    }
    return '';
  };

  return (
    <form className="nx-form iq-add-waiver-form" onSubmit={onSubmit}>
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Waiver Configuration</h2>
        </div>
      </header>

      <div className="nx-tile-content">
        {/* Component Info */}
        <div className="nx-read-only iq-add-waiver-form__component">
          <header className="nx-read-only__label">
            <ArtifactNameDisplay {...{ artifactName }} />
          </header>
          <div className="nx-read-only__data">{componentName}</div>
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
            <a onClick={onVulnerabilityDetailsClick}>See Security Vulnerability Details</a>
            <VulnerabilityDetailsModalContainer />
          </div>
        )}

        {/* Scope */}
        <NxFieldset className="iq-add-waiver-form__scope" label="Scope" isRequired>
          {availableWaiverScopes &&
            availableWaiverScopes.map(({ id, name, label, type }) => (
              <NxRadio
                id={getAvailableScopeId(id, type)}
                name="add-waiver-target"
                value={id}
                isChecked={selectedWaiverScope.id === id}
                key={id}
                onChange={handleScopeChange}
              >
                {label} - {name}
              </NxRadio>
            ))}
        </NxFieldset>

        {/* Components */}
        <NxFieldset className="iq-add-waiver-form__components" label="Components" isRequired>
          <NxRadio
            name="add-waiver-components"
            value={componentName}
            isChecked={!applyToAllComponents}
            onChange={handleComponentsChange}
          >
            {componentName}
          </NxRadio>
          <NxRadio
            name="add-waiver-components"
            value={ALL_COMPONENTS}
            isChecked={!!applyToAllComponents}
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
              <div className="iq-add-waiver-form__expiration-days-diff visual-testing-ignore">{daysDiff()}</div>
            </div>
            {isCustomExpiryTimeSelected && (
              <NxDateInput
                className="iq-add-waiver-form__date-input"
                {...customExpiryTime}
                onChange={setCustomExpiryTime}
                validatable={true}
              />
            )}
          </div>
        </NxFieldset>

        {/* Comments */}
        <NxFieldset className="iq-add-waiver-form__comments" label="Comments">
          <NxTextInput type="textarea" maxLength={1000} {...waiverComments} onChange={setWaiverComment} />
        </NxFieldset>

        {/* Created By */}
        <div className="nx-read-only iq-add-waiver-form__created-by">
          <header className="nx-read-only__label">Created By</header>
          <div className="nx-read-only__data">{currentUser}</div>
        </div>
      </div>

      {/* Actions */}
      <footer className="nx-footer">
        {submitError && <LoadError error={submitError} titleMessage="An error occurred saving the waiver." />}
        <div className="nx-btn-bar">
          <NxButton type="button" id="add-waiver-cancel" onClick={cancelAction}>
            Cancel
          </NxButton>

          <NxButton
            disabled={isCustomExpiryTimeSelected && !isCustomExpiryTimeValid(customExpiryTime.value)}
            type="submit"
            id="add-waiver-submit"
            variant="primary"
          >
            Submit
          </NxButton>
        </div>
      </footer>
    </form>
  );
}

export const waiverScopePropTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired,
};

AddWaiverForm.propTypes = {
  applyToAllComponents: PropTypes.bool.isRequired,
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
  selectedWaiverScope: PropTypes.shape(waiverScopePropTypes).isRequired,
  expiryTime: PropTypes.string,
  customExpiryTime: PropTypes.shape({
    value: PropTypes.string,
    isPristine: PropTypes.bool,
  }).isRequired,
  submitError: PropTypes.instanceOf(Error),
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  setCustomExpiryTime: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  closeVulnerabilityDetailsModal: PropTypes.func.isRequired,
  vulnerabilityId: PropTypes.string,
  cancelAction: PropTypes.func.isRequired,
  componentIdentifier: PropTypes.object,
  currentUser: PropTypes.string,
};
