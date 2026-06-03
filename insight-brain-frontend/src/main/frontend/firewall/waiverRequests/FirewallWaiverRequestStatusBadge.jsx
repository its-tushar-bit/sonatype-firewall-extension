/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useState } from 'react';
import * as PropTypes from 'prop-types';
import { Badge } from '@radix-ui/themes';
import '@radix-ui/themes/styles.css';

const STATUS_CONFIG = {
  REQUESTED: {
    label: 'Requested',
    color: "blue"
  },
  APPROVED: {
    label: 'Approved',
    color: "green"
  },
  REJECTED: {
    label: 'Rejected',
    color: "tomato"
  },
};

export default function FirewallWaiverRequestStatusBadge({ status }) {
  const config = STATUS_CONFIG[status] || STATUS_CONFIG.REQUESTED;

  const [isDarkMode, setIsDarkMode] = useState(
    () => document.documentElement.classList.contains('nx-html--dark-mode')
  );

  useEffect(() => {
    const observer = new MutationObserver(() => {
      setIsDarkMode(document.documentElement.classList.contains('nx-html--dark-mode'));
    });
    observer.observe(document.documentElement, { attributeFilter: ['class'] });
    return () => observer.disconnect();
  }, []);

  return (
    <Badge color={config.color} variant={isDarkMode ? "outline" : "soft"} style={{ borderRadius: '999px' }}>
      <div className="iq-waiver-request-status-badge__content">
        {config.label}
      </div>
    </Badge>
  );
}

FirewallWaiverRequestStatusBadge.propTypes = {
  status: PropTypes.oneOf(['REQUESTED', 'APPROVED', 'REJECTED']).isRequired,
};
