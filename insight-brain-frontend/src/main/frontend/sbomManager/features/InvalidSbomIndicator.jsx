/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { faExclamationTriangle } from '@fortawesome/pro-solid-svg-icons';
import { NxFontAwesomeIcon, NxTooltip } from '@sonatype/react-shared-components';

import { invalidSbomMessageDetails } from './messages';

export default function InvalidSbomIndicator() {
  return (
    <NxTooltip title={invalidSbomMessageDetails}>
      <NxFontAwesomeIcon
        aria-hidden="false"
        aria-label={invalidSbomMessageDetails}
        icon={faExclamationTriangle}
        className="sbom-manager-invalid-sbom-indicator"
      />
    </NxTooltip>
  );
}
