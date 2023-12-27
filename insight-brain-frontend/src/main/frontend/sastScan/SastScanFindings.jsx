/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxH2,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableContainer,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';
import SastFinding from 'MainRoot/sastScan/SastFinding';
import SastScanFindingsFilter from 'MainRoot/sastScan/SastScanFindingsFilter';
import * as PropTypes from 'prop-types';

export default function SastScanFindings({ findings }) {
  const [filter, onFilterChange] = useState(new Set());
  const filterDisabled = filter.size === 0;

  const [rows, setRows] = useState(findings);
  const [sortDir, setSortDir] = useState('desc');

  return (
    <div className="iq_sast_scan_findings__container">
      <div className="iq_sast_scan_findings_header_section">
        <NxH2>SAST Findings</NxH2>
        <SastScanFindingsFilter
          data-analytics-id="sonatype-developer-sast-filter-dropdown"
          className="iq_sast_scan_finding_header_section__filter"
          options={getCurrentOptions()}
          selectedIds={filter}
          onChange={onFilterChange}
        />
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
                return <SastFinding key={finding.id} finding={finding} />;
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
      .sort()
      .reverse()
      .map((severity) => {
        return { id: severity, displayName: severity };
      });
  }

  function sortBySeverity() {
    const severities = {
      CRITICAL: 4,
      HIGH: 3,
      MEDIUM: 2,
      LOW: 1,
      NONE: 0,
    };
    const newSortDir = sortDir === 'desc' ? 'asc' : 'desc';
    setSortDir(newSortDir);

    const sortedRows = [...rows].sort((a, b) => {
      if (newSortDir === 'asc') {
        return severities[a.severity] - severities[b.severity];
      }
      return severities[b.severity] - severities[a.severity];
    });

    setRows(sortedRows);
  }
}

SastScanFindings.propTypes = {
  findings: PropTypes.array.isRequired,
};
