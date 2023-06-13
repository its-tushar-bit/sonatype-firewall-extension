/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { useDispatch } from 'react-redux';
import { NxButton } from '@sonatype/react-shared-components';
import { actions } from './firewallOnboardingSlice';

export default function WelcomeScreen() {
  const dispatch = useDispatch();
  const hideWelcomeScreen = () => dispatch(actions.hideWelcomeScreen());

  return (
    <div className="welcome-screen-wrapper">
      <div className="welcome-screen-content">
        <header className="nx-page-title">
          <hgroup className="nx-page-title__headings">
            <h1 className="nx-h1 iq-dependency-tree__title">Welcome to Repository Firewall</h1>
          </hgroup>
        </header>
        <h2 className="nx-h2">Start step-by-step configuration</h2>
        <div className="nx-page-title__description">
          <h3 className="nx-h3">
            Protect against 3rd party malicious attacks, dependency confusion and investigate existing threats and risks
            in your repositories.
          </h3>
          <NxButton variant="primary" id="get-started-button" onClick={hideWelcomeScreen}>
            Get Started
          </NxButton>
        </div>
      </div>
    </div>
  );
}
