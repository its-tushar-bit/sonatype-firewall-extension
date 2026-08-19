/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { faShieldCheck } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxList } from '@sonatype/react-shared-components';
import classNames from 'classnames';
import * as PropTypes from 'prop-types';

import { steps } from './firewallOnboardingUtils';

export default function OnboardingSteps({ currentStep }) {
  return (
    <NxList id="onboarding-steps" className="steps">
      {steps.map((step, index) => (
        <NxList.Item
          key={step.id}
          aria-labelledby={`${step.id}_label`}
          className={classNames('step', { selected: step.id === currentStep.id })}
        >
          <NxFontAwesomeIcon icon={faShieldCheck} />
          <span id={`${step.id}_label`}>
            {index + 1}. {step.name}
          </span>
        </NxList.Item>
      ))}
    </NxList>
  );
}

OnboardingSteps.propTypes = {
  currentStep: PropTypes.shape({
    id: PropTypes.string,
  }),
};
