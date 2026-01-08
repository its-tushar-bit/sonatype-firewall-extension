/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxInfoAlert } from '@sonatype/react-shared-components';

/**
 * Alert component displayed when a user has limited access to Repository Firewall
 * based on their current permissions (typically shown when receiving a 403 response).
 */
export default function LimitedFirewallAccessAlert() {
  return (
    <NxInfoAlert className="iq-limited-firewall-access-alert">
      <strong>You have limited access to Repository Firewall based on your current permissions.</strong>
      <br />
      Some data or settings may not be visible. Contact your administrator to request full access to Repository
      Firewall.
    </NxInfoAlert>
  );
}
