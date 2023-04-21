/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import LoadWrapper from '../react/LoadWrapper';

export default function FirewallOnboardingPage() {
  return (
    <main id="firewall-onboarding-page" className="nx-page-main nx-viewport-sized">
      <LoadWrapper loading={false} error={null} retryHandler={() => {}}>
        <div className="nx-card-container nx-card-container--no-wrap">
          <div className="sidebar">Sidebar content</div>
          <div className="content">Main content</div>
          <div className="footer">
            <div className="footer-left">footer left settings</div>
            <div className="footer-right">footer right paginator</div>
          </div>
        </div>
      </LoadWrapper>
    </main>
  );
}
