/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxButton, NxFooter } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

import { actions } from './firewallOnboardingSlice';
import { next, prev } from './firewallOnboardingUtils';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectLaunchFirewall } from './firewallOnboardingSelectors';

const HELP_URL = 'http://links.sonatype.com/products/nxiq/doc/firewall-onboarding';

export default function ActionsFooter({ currentStep = {}, isNextButtonDisabled, ...otherProps }) {
  const dispatch = useDispatch();

  const continueToNextStep = () => dispatch(actions.continueToNextStep());
  const goBackToPreviousStep = () => dispatch(actions.goBackToPreviousStep());

  // When cancelling onboarding it should go back to firewall initial screen
  const handleCancel = () => dispatch(stateGo('firewall.firewallPage'));

  const launchFirewall = () => dispatch(actions.launchFirewall());

  const isPrevAvailable = !!prev(currentStep);
  const isNextAvailable = !!next(currentStep);

  const { saving } = useSelector(selectLaunchFirewall);

  return (
    <NxFooter className="actions-footer" id="actions-footer" role="navigation" {...otherProps}>
      <div className="actions-footer__button-group">
        <a className="nx-btn nx-btn--tertiary" id="help-button" target="_blank" rel="noreferrer" href={HELP_URL}>
          Help
        </a>
      </div>
      <div className="actions-footer__button-group">
        <NxButton variant="tertiary" id="cancel-button" onClick={handleCancel}>
          Cancel
        </NxButton>
        {isPrevAvailable && (
          <NxButton variant="secondary" id="previous-button" onClick={goBackToPreviousStep}>
            Previous
          </NxButton>
        )}
        {isNextAvailable ? (
          <NxButton variant="primary" id="continue-button" disabled={isNextButtonDisabled} onClick={continueToNextStep}>
            Continue
          </NxButton>
        ) : (
          <NxButton variant="primary" id="launch-button" onClick={launchFirewall} disabled={saving}>
            Launch Firewall
          </NxButton>
        )}
      </div>
    </NxFooter>
  );
}

ActionsFooter.propTypes = {
  isNextButtonDisabled: PropTypes.bool,
  currentStep: PropTypes.shape({
    id: PropTypes.string,
  }).isRequired,
};
