/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxButton, NxFooter } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

import { next, prev } from './firewallOnboardingUtils';

export default function ActionsFooter({ currentStep = {}, onNext, onPrevious, onLaunch, ...otherProps }) {
  return (
    <NxFooter id="actions-footer" role="navigation" {...otherProps}>
      {!!prev(currentStep) && (
        <NxButton variant="secondary" id="previous-button" onClick={onPrevious}>
          Previous
        </NxButton>
      )}
      {!!next(currentStep) ? (
        <NxButton variant="primary" id="continue-button" onClick={onNext}>
          Continue
        </NxButton>
      ) : (
        <NxButton variant="primary" id="launch-button" onClick={onLaunch}>
          Launch Firewall
        </NxButton>
      )}
    </NxFooter>
  );
}

ActionsFooter.propTypes = {
  onPrevious: PropTypes.func,
  onNext: PropTypes.func,
  onLaunch: PropTypes.func,
  currentStep: PropTypes.shape({
    id: PropTypes.string,
  }).isRequired,
};
