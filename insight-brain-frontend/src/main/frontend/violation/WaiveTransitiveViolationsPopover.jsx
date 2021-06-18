/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { IqPopover } from '../react/IqPopover';
import * as PropTypes from 'prop-types';
import {
  NxButton,
  NxErrorAlert,
  NxFieldset,
  NxFontAwesomeIcon,
  NxFormGroup,
  NxStatefulTextInput,
  NxSubmitMask,
} from '@sonatype/react-shared-components';
import { faArrowFromLeft, faSync } from '@fortawesome/pro-solid-svg-icons';
import { availableScopesPropType, componentTransitivePolicyViolationsPropType } from './transitiveViolationsPropTypes';
import { capitalize } from '../util/jsUtil';
import { waiverExpirations } from '../util/waiverUtils';
import classnames from 'classnames';

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
  } = props;

  if (scope === null) {
    setScope(availableScopes.data[0].publicId);
  }

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

  const createSaveButton = () => {
    return (
      <NxButton id="waive-transitive-violations-popover-save" onClick={save} variant={saveError ? 'error' : 'primary'}>
        {saveError ? (
          <Fragment>
            <NxFontAwesomeIcon icon={faSync} /> Retry
          </Fragment>
        ) : (
          'Save'
        )}
      </NxButton>
    );
  };

  return (
    <IqPopover id="waive-transitive-violations-popover" onClose={toggleWaiveTransitiveViolations}>
      <IqPopover.Header className="waive-transitive-violations-popover-header">
        <div className="waive-transitive-violations-popover-header__title">
          <h2 className="nx-h2 waive-transitive-violations-popover-header__title-text">
            Add Waivers to Transitive Violations
          </h2>
          <NxButton
            id="waive-transitive-violations-popover-toggle"
            onClick={toggleWaiveTransitiveViolations}
            variant="icon-only"
          >
            <NxFontAwesomeIcon
              className="waive-transitive-violations-popover-header__title-icon"
              icon={faArrowFromLeft}
            />
          </NxButton>
        </div>
      </IqPopover.Header>
      <Fragment>
        <NxFormGroup
          id="waive-transitive-violations-counts-group"
          label="Summary of violations being waived"
          sublabel={
            componentTransitivePolicyViolations.threatCountsTotal +
            ' total violation' +
            (componentTransitivePolicyViolations.threatCountsTotal === 1 ? '' : 's') +
            ' brought in by ' +
            componentTransitivePolicyViolations.componentCount +
            ' component' +
            (componentTransitivePolicyViolations.componentCount === 1 ? '' : 's')
          }
          isRequired
        >
          <dl
            id="waive-transitive-violations-counts"
            className={classnames('nx-threat-counter-container', 'nx-threat-counter-container--column', {
              'nx-threat-counter-container--hide-zero-critical':
                componentTransitivePolicyViolations.threatCounts.critical === 0,
              'nx-threat-counter-container--hide-zero-severe':
                componentTransitivePolicyViolations.threatCounts.severe === 0,
              'nx-threat-counter-container--hide-zero-moderate':
                componentTransitivePolicyViolations.threatCounts.moderate === 0,
              'nx-threat-counter-container--hide-zero-low': componentTransitivePolicyViolations.threatCounts.low === 0,
              'nx-threat-counter-container--hide-zero-none':
                componentTransitivePolicyViolations.threatCounts.none === 0,
            })}
          >
            <div className="nx-threat-counter nx-threat-counter--critical">
              <dt className="nx-threat-counter__text">Critical</dt>
              <dd className="nx-threat-counter__count">{componentTransitivePolicyViolations.threatCounts.critical}</dd>
            </div>
            <div className="nx-threat-counter nx-threat-counter--severe">
              <dt className="nx-threat-counter__text">Severe</dt>
              <dd className="nx-threat-counter__count">{componentTransitivePolicyViolations.threatCounts.severe}</dd>
            </div>
            <div className="nx-threat-counter nx-threat-counter--moderate">
              <dt className="nx-threat-counter__text">Moderate</dt>
              <dd className="nx-threat-counter__count">{componentTransitivePolicyViolations.threatCounts.moderate}</dd>
            </div>
            <div className="nx-threat-counter nx-threat-counter--low">
              <dt className="nx-threat-counter__text">Low</dt>
              <dd className="nx-threat-counter__count">{componentTransitivePolicyViolations.threatCounts.low}</dd>
            </div>
            <div className="nx-threat-counter nx-threat-counter--none">
              <dt className="nx-threat-counter__text">None</dt>
              <dd className="nx-threat-counter__count">{componentTransitivePolicyViolations.threatCounts.none}</dd>
            </div>
          </dl>
        </NxFormGroup>
        <NxFieldset id="waive-transitive-violations-scopes" label="Scope" isRequired>
          <span>
            {capitalize(availableScopes.data[0].type)} - {availableScopes.data[0].name}
          </span>
        </NxFieldset>
        <NxFormGroup label="Waiver Expiration" isRequired>
          <select
            id="waive-transitive-violations-expirations"
            className="nx-form-select"
            onChange={(e) => setExpiration(e.currentTarget.value)}
            value={expiration}
          >
            {waiverExpirations.map(({ name, value }, index) => (
              <option key={index} value={value}>
                {name}
              </option>
            ))}
          </select>
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
      </Fragment>
      <IqPopover.Footer>
        {saveError ? (
          <Fragment>
            <NxErrorAlert>
              <span>{saveError}</span>
              {createSaveButton()}
            </NxErrorAlert>
            {createCancelButton()}
          </Fragment>
        ) : (
          <Fragment>
            {createSaveButton()}
            {createCancelButton()}
          </Fragment>
        )}
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
