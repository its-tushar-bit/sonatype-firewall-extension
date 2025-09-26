/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { useSelector } from 'react-redux';
import { NxFontAwesomeIcon, NxP, NxTextLink, NxTooltip } from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { selectApplicationReportMetaData } from 'MainRoot/applicationReport/applicationReportSelectors';

export default function LegacyScannerBanner() {
  const metadataDetails = useSelector(selectApplicationReportMetaData);

  if (metadataDetails?.containerScanningMode !== 'neuvector') {
    return null;
  }

  return (
    <NxP>
      <span className="nx-status-indicator nx-status-indicator--intermediate">Legacy Scanner Used</span>
      <NxTooltip title="This scan used an earlier version of the scanner.">
        <NxFontAwesomeIcon icon={faInfoCircle} className="iq-enterprise-reporting__dashboard-grouping__icon" />
      </NxTooltip>
      <span>
        <NxTextLink external href={'https://links.sonatype.com/products/nxiq/doc/container-scanning-with-fw'}>
          Learn more about the new container scanner
        </NxTextLink>
        .
      </span>
    </NxP>
  );
}
