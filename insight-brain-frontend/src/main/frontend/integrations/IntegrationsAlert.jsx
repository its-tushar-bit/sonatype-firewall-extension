/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxTextLink, NxInfoAlert } from '@sonatype/react-shared-components';
import { createPortal } from 'react-dom';

export default function IntegrationsAlert() {
  return createPortal(
    <NxInfoAlert className="iq-integrations-page-top-level-alert">
      Sonatype Developer is available for free in the <strong>Product Preview Program (PPP)</strong>. Innovate with us
      by submitting your feedback to{' '}
      <NxTextLink
        external
        href="mailto:sonatype-developer@sonatype.com"
        data-analytics-id="sonatype-developer-feedback-mailto"
      >
        sonatype-developer@sonatype.com
      </NxTextLink>
      .
    </NxInfoAlert>,
    document.getElementById('iq-portal-container-for-integrations-alert')
  );
}
