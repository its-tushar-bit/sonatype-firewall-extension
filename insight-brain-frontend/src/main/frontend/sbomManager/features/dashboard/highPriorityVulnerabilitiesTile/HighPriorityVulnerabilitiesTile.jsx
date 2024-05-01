/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxFontAwesomeIcon,
  NxH2,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import moment from 'moment';

import LoadWrapper from 'MainRoot/react/LoadWrapper';

import './HighPriorityVulnerabilitiesTile.scss';

export default function HighPriorityVulnerabilitiesTile() {
  const doLoad = () => {};

  const vulnerabilities = [
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
    {
      threatLevel: 10,
      CVE: 'CVE-202104103',
      importDate: '2024-01-20',
    },
  ];

  const vulnerabilityItems = vulnerabilities.map((vulnerability, index) => (
    <li key={index} className="sbom-manager-high-priority-vulnerabilities-tile-list-item">
      <div className="sbom-manager-high-priority-vulnerabilities-tile-list-item__score">
        <span>
          <NxThreatIndicator policyThreatLevel={vulnerability.threatLevel} />
          {vulnerability.threatLevel}
        </span>
        <NxTextLink href="#@placeholder">{vulnerability.CVE}</NxTextLink>
      </div>
      <div className="sbom-manager-high-priority-vulnerabilities-tile-list-item__date">
        {moment(vulnerability.importDate).fromNow()}
      </div>
    </li>
  ));

  return (
    <NxTile id="high-priority-vulnerabilities-tile" className="sbom-manager-high-priority-vulnerabilities-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>High Priority Vulnerabilities</NxH2>
          <NxTooltip title="High severity vulnerabilities found in the most recent SBOM scans or import.">
            <NxFontAwesomeIcon
              icon={faInfoCircle}
              className="sbom-manager-high-priority-vulnerabilities-tile__info-icon"
            />
          </NxTooltip>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <LoadWrapper retryHandler={doLoad} error={null}>
          <ol className="sbom-manager-high-priority-vulnerabilities-tile-list">{vulnerabilityItems}</ol>
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}
