/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { IqPopover } from '../react/IqPopover';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxFormGroup,
  NxInfoAlert,
  NxLoadError,
  NxTextLink,
  NxStatefulTextInput,
  NxSubmitMask,
  NxFormSelect,
} from '@sonatype/react-shared-components';
import { availableScopesPropType, componentTransitivePolicyViolationsPropType } from './transitiveViolationsPropTypes';
import { capitalize } from '../util/jsUtil';
import { useWaiverExpirations } from '../util/waiverUtils';
import TransitiveViolationsSummary from './TransitiveViolationsSummary';

export default function WaiveTransitiveViolationsPopover(props) {
  const {
    availableScopes,
    componentTransitivePolicyViolations,
    scope,
    expiration,
    comments,
    submitMaskState,
    saveError,
    toggleWaiveTransitiveViolations,
    setScope,
    setExpiration,
    setComments,
    cancel,
    save,
    isExpireWhenRemediationAvailable,
  } = props;

  if (scope === null) {
    setScope(availableScopes.data[0].publicId);
  }

  const waiverExpirations = useWaiverExpirations(isExpireWhenRemediationAvailable);

  const createCancelButton = () => {
    return (
      <NxButton
        id="waive-transitive-violations-popover-cancel"
        onClick={() => {
          cancel();
          toggleWaiveTransitiveViolations();
        }}
        variant="secondary"
      >
        Cancel
      </NxButton>
    );
  };

  const createSaveButtonIfNeeded = () => {
    return saveError ? null : (
      <NxButton id="waive-transitive-violations-popover-save" onClick={save} variant="primary">
        Save
      </NxButton>
    );
  };

  const createSaveErrorIfNeeded = () => {
    return saveError ? (
      <NxLoadError
        id="waive-transitive-violations-popover-save-error"
        titleMessage="An error occurred saving data."
        error={saveError}
        retryHandler={save}
      />
    ) : null;
  };

  return (
    <IqPopover id="waive-transitive-violations-popover" size="large" onClose={toggleWaiveTransitiveViolations}>
      <IqPopover.Header
        className="transitive-violations-popover-header"
        headerTitle="Add Waivers to Transitive Violations"
        buttonId="waive-transitive-violations-popover-toggle"
        onClose={toggleWaiveTransitiveViolations}
      />
      <TransitiveViolationsSummary
        threatCounts={componentTransitivePolicyViolations.threatCounts}
        threatCountsTotal={componentTransitivePolicyViolations.threatCountsTotal}
        componentCount={componentTransitivePolicyViolations.componentCount}
      />
      <dl id="waive-transitive-violations-scopes" className="nx-read-only">
        <div className="nx-read-only__item">
          <dt className="nx-read-only__label">Scope</dt>
          <dd className="nx-read-only__data">
            {capitalize(availableScopes.data[0].type)} - {availableScopes.data[0].name}
          </dd>
        </div>
      </dl>
      <NxInfoAlert>
        Get access to more Scoping options using our API. For more information, view the&nbsp;
        <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/transitive-violations-rest-api" external>
          Documentation
        </NxTextLink>
      </NxInfoAlert>
      <NxFormGroup label="Waiver Expiration" isRequired>
        <NxFormSelect
          id="waive-transitive-violations-expirations"
          onChange={(e) => setExpiration(e.currentTarget.value)}
          value={expiration}
        >
          {waiverExpirations.map(({ name, value }, index) => (
            <option key={index} value={value}>
              {name}
            </option>
          ))}
        </NxFormSelect>
      </NxFormGroup>
      <NxFormGroup label="Comments" isRequired>
        <NxStatefulTextInput
          id="waive-transitive-violations-comments"
          type="textarea"
          className="nx-text-input--long"
          defaultValue={comments}
          placeholder="Comments"
          onChange={setComments}
        />
      </NxFormGroup>
      <IqPopover.Footer>
        {createSaveErrorIfNeeded()}
        <div className="nx-btn-bar">
          {createCancelButton()}
          {createSaveButtonIfNeeded()}
        </div>
      </IqPopover.Footer>
      {submitMaskState !== null && <NxSubmitMask fullscreen success={submitMaskState} />}
    </IqPopover>
  );
}

WaiveTransitiveViolationsPopover.propTypes = {
  availableScopes: availableScopesPropType.isRequired,
  componentTransitivePolicyViolations: componentTransitivePolicyViolationsPropType.isRequired,
  scope: PropTypes.string,
  expiration: PropTypes.string.isRequired,
  comments: PropTypes.string.isRequired,
  submitMaskState: PropTypes.bool,
  saveError: PropTypes.string,
  toggleWaiveTransitiveViolations: PropTypes.func.isRequired,
  setScope: PropTypes.func.isRequired,
  setExpiration: PropTypes.func.isRequired,
  setComments: PropTypes.func.isRequired,
  cancel: PropTypes.func.isRequired,
  save: PropTypes.func.isRequired,
};
