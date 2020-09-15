/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { find, propEq } from 'ramda';
import classnames from 'classnames';
import {
  NxButton,
  NxTextInput,
  NxRadio
} from '@sonatype/react-shared-components';

import ViolationExclamation from '../react/ViolationExclamation';
import ArtifactNameDisplay from '../react/ArtifactNameDisplay';
import VulnerabilityDetailsModalContainer from '../vulnerabilityDetails/VulnerabilityDetailsModalContainer';
import LoadError from '../react/LoadError';

const ALL_COMPONENTS = 'ALL_COMPONENTS';

export default function AddWaiverForm(props) {
  const {
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
    submitError,
    openVulnerabilityDetailsModal,
    setWaiverScope,
    setWaiverComment,
    setApplyToAllComponents,
    saveWaiver,
    vulnerabilityId,
    cancelAction
  } = props;

  const onSubmit = (evt) => {
    evt.preventDefault();

    const { type, id } = selectedWaiverScope;
    const { value } = waiverComments;

    saveWaiver(policyViolationId, type, id, value, applyToAllComponents);
  };

  const onVulnerabilityDetailsClick = () => {
    openVulnerabilityDetailsModal({
      vulnerabilityId: vulnerabilityId
    });
  };

  const handleComponentsChange = (value) => {
    setApplyToAllComponents(value === ALL_COMPONENTS);
  };

  const handleScopeChange = (selectedId) => {
    const target = find(propEq('id', selectedId), availableWaiverScopes);
    setWaiverScope(target);
  };

  const cancelAddWaiver = () => {
    cancelAction();
  };

  const policyClassnames = classnames('iq-threat-level', `iq-threat-level--${threatLevelCategory}`);

  return (
    <form className="nx-tile-content nx-form nx-form--simple iq-add-waiver-form"
          onSubmit={onSubmit}>
      { /* Component-Info Section */}
      <div className="nx-tile-header iq-add-waiver-form__component">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">
            <ArtifactNameDisplay { ...{ artifactName } } />
          </h2>
        </div>
        <div className="nx-tile-header__subtitle">{ componentName }</div>
      </div>

      { /* Policy Info */ }
      <div className="nx-form-group iq-add-waiver-form__policy iq-read-only">
        <label className="nx-label">Policy</label>
        <div className="iq-read-only-data">
          <ViolationExclamation threatLevelCategory={ threatLevelCategory } />
          <span className={ policyClassnames }>{ policyName }</span>
        </div>
      </div>

      { /* Constraint Info */ }
      <div className="nx-form-group iq-add-waiver-form__constraint iq-read-only">
        <label className="nx-label">Constraint Name</label>
        <div className="iq-read-only-data">{ constraintName }</div>
      </div>

      {/* Conditions */}
      <div className="nx-form-group nx-read-only iq-add-waiver-form__conditions iq-read-only">
        <label className="nx-label">Conditions</label>
        <div className="iq-read-only-data iq-read-only-data--vertical">
          {reasons && reasons.map((reason, index) =>
            <span key={index}>{reason}</span>
          )}
        </div>
      </div>

      { vulnerabilityId &&
      <div className="nx-form-group iq-add-waiver-form__vulnerability_details_link">
        <a onClick={ onVulnerabilityDetailsClick }>
          See Security Vulnerability Details
        </a>
        <VulnerabilityDetailsModalContainer />
      </div>
      }

      { /* Scope */ }
      <fieldset className="nx-fieldset iq-add-waiver-form__scope">
        <legend className="nx-label">Scope</legend>
        { availableWaiverScopes && availableWaiverScopes.map(({ id, name, label }) =>
          <NxRadio name="add-waiver-target"
                   value={id}
                   isChecked={selectedWaiverScope.id === id}
                   key={id}
                   onChange={handleScopeChange}>
            {label} - {name}
          </NxRadio>
        )}
      </fieldset>

      { /* Components */ }
      <fieldset className="nx-fieldset iq-add-waiver-form__components">
        <legend className="nx-label">Components</legend>
        <NxRadio name="add-waiver-components"
                 value={componentName}
                 isChecked={!applyToAllComponents}
                 onChange={handleComponentsChange}>
          { componentName }
        </NxRadio>
        <NxRadio name="add-waiver-components"
                 value={ALL_COMPONENTS}
                 isChecked={!!applyToAllComponents}
                 onChange={handleComponentsChange}>
          All Components
        </NxRadio>
      </fieldset>

      { /* Comments */}
      <div className="nx-form-group iq-add-waiver-form__comments">
        <label className="nx-label nx-label--optional">
          <span className="nx-label__text">Comments</span>
          <NxTextInput type="textarea"
                       maxLength={1000}
                       { ...waiverComments }
                       onChange={ setWaiverComment } />
        </label>
      </div>

      { /* Actions */ }
      <div className="nx-btn-bar nx-btn-bar--forms">
        {
          submitError && <LoadError error={submitError} titleMessage="An error occurred saving the waiver." />
        }
        <NxButton type="button" id="add-waiver-cancel" onClick={cancelAddWaiver}>
          Cancel
        </NxButton>

        <NxButton type="submit" id="add-waiver-submit" variant="primary">
          Submit
        </NxButton>
      </div>
    </form>
  );
}

export const waiverScopePropTypes = {
  id: PropTypes.string.isRequired,
  name: PropTypes.string.isRequired,
  label: PropTypes.string.isRequired,
  type: PropTypes.string.isRequired
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
    isPristine: PropTypes.bool.isRequired
  }).isRequired,
  availableWaiverScopes: PropTypes.arrayOf(PropTypes.shape(waiverScopePropTypes)).isRequired,
  selectedWaiverScope: PropTypes.shape(waiverScopePropTypes).isRequired,
  submitError: PropTypes.instanceOf(Error),
  setWaiverScope: PropTypes.func.isRequired,
  setApplyToAllComponents: PropTypes.func.isRequired,
  setWaiverComment: PropTypes.func.isRequired,
  saveWaiver: PropTypes.func.isRequired,
  openVulnerabilityDetailsModal: PropTypes.func.isRequired,
  vulnerabilityId: PropTypes.string,
  cancelAction: PropTypes.func.isRequired
};
