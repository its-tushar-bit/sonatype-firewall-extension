/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq } from 'ramda';
import classnames from 'classnames';
import { NxButton, NxFieldset, NxTextInput, NxRadio } from '@sonatype/react-shared-components';

import ViolationExclamation from '../react/ViolationExclamation';
import ArtifactNameDisplay from '../react/ArtifactNameDisplay';
import VulnerabilityDetailsModalContainer from '../vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import LoadError from '../react/LoadError';
import { waiverExpirations } from '../util/waiverUtils';

const ALL_COMPONENTS = 'ALL_COMPONENTS';

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
    submitError,
    openVulnerabilityDetailsModal,
    setWaiverScope,
    setWaiverComment,
    setApplyToAllComponents,
    setExpiryTime,
    saveWaiver,
    vulnerabilityId,
    cancelAction,
  } = props;

  const onSubmit = (evt) => {
    evt.preventDefault();

    const { type, id } = selectedWaiverScope;
    const { value } = waiverComments;
    const expiration = expiryTime === 'never' ? null : parseInt(expiryTime, 10);

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

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

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
            availableWaiverScopes.map(({ id, name, label }) => (
              <NxRadio
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
          <select id="waiver-expiration-select" onChange={onExpiryTimeChange} value={expiryTime || ''}>
            {waiverExpirations.map(({ name, value }, index) => (
              <option key={index} value={value}>
                {name}
              </option>
            ))}
          </select>
        </NxFieldset>

        {/* Comments */}
        <NxFieldset className="iq-add-waiver-form__comments" label="Comments">
          <NxTextInput type="textarea" maxLength={1000} {...waiverComments} onChange={setWaiverComment} />
        </NxFieldset>
      </div>

      {/* Actions */}
      <footer className="nx-footer">
        {submitError && <LoadError error={submitError} titleMessage="An error occurred saving the waiver." />}
        <div className="nx-btn-bar">
          <NxButton type="button" id="add-waiver-cancel" onClick={cancelAction}>
            Cancel
          </NxButton>

          <NxButton type="submit" id="add-waiver-submit" variant="primary">
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
  submitError: PropTypes.instanceOf(Error),
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired,
  setExpiryTime: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  vulnerabilityId: PropTypes.string,
  cancelAction: PropTypes.func.isRequired,
  componentIdentifier: PropTypes.object,
};
