/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxFontAwesomeIcon,
  NxH2,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableContainer,
  NxTableHead,
  NxTableRow,
  NxTextLink,
} from '@sonatype/react-shared-components';
import SastFinding from 'MainRoot/sastScan/SastFinding';
import SastScanFindingsFilter from 'MainRoot/sastScan/SastScanFindingsFilter';
import * as PropTypes from 'prop-types';
import { faGithub, faGitlab, faMicrosoft } from '@fortawesome/free-brands-svg-icons';
import { faExternalLinkAlt } from '@fortawesome/free-solid-svg-icons';

const severities = {
  CRITICAL: 4,
  HIGH: 3,
  MEDIUM: 2,
  LOW: 1,
  NONE: 0,
};

export default function SastScanFindings({ findings, sastPullRequestURL }) {
  const [filter, onFilterChange] = useState(new Set());
  const filterDisabled = filter.size === 0;
  const [rows, setRows] = useState(findings);
  const [sortDir, setSortDir] = useState('desc');

  return (
    <div className="iq_sast_scan_findings__container">
      <div className="iq_sast_scan_findings_header_section">
        <NxH2>SAST Findings</NxH2>
        <div className="iq_sast_scan_findings_header_actions_section">
          {sastPullRequestURL && (
            <NxTextLink className="nx-btn nx-btn--tertiary" href={sastPullRequestURL} external>
              <NxFontAwesomeIcon icon={getScmIcon(sastPullRequestURL)} />
              View PR
            </NxTextLink>
          )}
          <SastScanFindingsFilter
            data-analytics-id="sonatype-developer-sast-filter-dropdown"
            className="iq_sast_scan_finding_header_section__filter"
            options={getCurrentOptions()}
            selectedIds={filter}
            onChange={onFilterChange}
          />
        </div>
      </div>
      <NxTableContainer>
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell isSortable sortDir={sortDir} onClick={sortBySeverity}>
                THREATS
              </NxTableCell>
              <NxTableCell chevron />
            </NxTableRow>
          </NxTableHead>
          <NxTableBody>
            {rows
              .filter((value) => filterDisabled || filter.has(value.severity))
              .map((finding) => {
                return <SastFinding key={finding.sastFindingId} finding={finding} />;
              })}
          </NxTableBody>
        </NxTable>
      </NxTableContainer>
    </div>
  );

  function getAvailableSeverities() {
    return [
      ...new Set(
        findings.map((finding) => {
          return finding.severity;
        })
      ),
    ];
  }
  function getCurrentOptions() {
    return getAvailableSeverities()
      .sort((a, b) => {
        return severities[b] - severities[a];
      })
      .map((severity) => {
        return { id: severity, displayName: severity };
      });
  }

  function sortBySeverity() {
    const newSortDir = sortDir === 'desc' ? 'asc' : 'desc';
    setSortDir(newSortDir);

    const sortedRows = [...rows].sort((a, b) => {
      const severity1 = a.severity.toUpperCase();
      const severity2 = b.severity.toUpperCase();

      if (newSortDir === 'asc') {
        return severities[severity1] - severities[severity2];
      }
      return severities[severity2] - severities[severity1];
    });
    setRows(sortedRows);
  }

  function getScmIcon(sastPullRequestUrl) {
    const hostname = new URL(sastPullRequestUrl).hostname;
    if (hostname.includes('github')) {
      return faGithub;
    } else if (hostname.includes('gitlab')) {
      return faGitlab;
    } else if (hostname.includes('azure')) {
      return faMicrosoft;
    }
    return faExternalLinkAlt;
  }
}

SastScanFindings.propTypes = {
  findings: PropTypes.array.isRequired,
};
