/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { NxTable, NxTableBody, NxTableContainer } from '@sonatype/react-shared-components';
import SastScanFindingsHeader from 'MainRoot/sastScan/SastScanFindingsHeader';
import SastFinding from 'MainRoot/sastScan/SastFinding';
import SastScanFindingsFilter from 'MainRoot/sastScan/SastScanFindingsFilter';
import * as PropTypes from 'prop-types';

export default function SastScanFindings({ findings }) {
  const [filter, onFilterChange] = useState(new Set());
  const filterDisabled = filter.size === 0;

  return (
    <div className="iq_sast_scan_findings__container">
      <SastScanFindingsFilter
        className="iq_sast_scan_findings__filter"
        title="Filter by Severity"
        options={getCurrentOptions()}
        selectedIds={filter}
        onChange={onFilterChange}
      />
      <NxTableContainer>
        <NxTable>
          <SastScanFindingsHeader />
          <NxTableBody>
            {findings
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
}

SastScanFindings.propTypes = {
  findings: PropTypes.array.isRequired,
};
