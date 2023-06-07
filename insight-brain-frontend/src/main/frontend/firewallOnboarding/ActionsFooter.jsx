/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch } from 'react-redux';
import { NxButton, NxFooter } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { useRouterState } from '../react/RouterStateContext';

import { actions } from './firewallOnboardingSlice';
import { next, prev } from './firewallOnboardingUtils';
import { stateGo } from '../reduxUiRouter/routerActions';
import { setShowWelcomeModalToTrueInStore } from '../firewall/firewallWelcomeModalStore';

export default function ActionsFooter({ currentStep = {}, isNextButtonDisabled, ...otherProps }) {
  const uiRouterState = useRouterState();
  const dispatch = useDispatch();
  const openIncompleteConfigurationModal = (href) => dispatch(actions.openIncompleteConfigurationModal(href));

  const continueToNextStep = () => dispatch(actions.continueToNextStep());
  const goBackToPreviousStep = () => dispatch(actions.goBackToPreviousStep());

  const handleCancel = () => openIncompleteConfigurationModal(uiRouterState.href('firewall.firewallPage'));
  const launchFirewall = () => {
    setShowWelcomeModalToTrueInStore();
    dispatch(actions.saveRepositories());
    dispatch(stateGo('firewall.firewallPage'));
  };

  const isPrevAvailable = Boolean(prev(currentStep));
  const isNextAvailable = Boolean(next(currentStep));

  return (
    <NxFooter id="actions-footer" role="navigation" {...otherProps}>
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
        <NxButton variant="primary" id="launch-button" onClick={launchFirewall}>
          Launch Firewall
        </NxButton>
      )}
    </NxFooter>
  );
}

ActionsFooter.propTypes = {
  isNextButtonDisabled: PropTypes.bool,
  currentStep: PropTypes.shape({
    id: PropTypes.string,
  }).isRequired,
};
