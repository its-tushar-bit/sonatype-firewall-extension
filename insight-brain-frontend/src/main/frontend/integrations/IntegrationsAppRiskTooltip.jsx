/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';
import React from 'react';

export default function IntegrationsAppRiskTooltip() {
  return (
    <NxTooltip title="Total risk score is the aggregate threat scores of your application's policy violations. It indicates the total risk found in the latest scan.">
      <span>
        Total Risk
        <NxFontAwesomeIcon icon={faInfoCircle} className="iq-total-risk-info-icon" />
      </span>
    </NxTooltip>
  );
}
