/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTable } from '@sonatype/react-shared-components';
import ExclusionLogTableRow from './ExclusionLogTableRow';

export default function ExclusionLogTable({ exclusions }) {
  function renderTableHeader() {
    return (
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header">Date</div>
          </NxTable.Cell>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header">Threat</div>
          </NxTable.Cell>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header">Policy</div>
          </NxTable.Cell>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header">Component</div>
          </NxTable.Cell>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header">Vulnerability</div>
          </NxTable.Cell>
          <NxTable.Cell>
            <div className="iq-auto-waiver-exclusion-log-table-header"></div>
          </NxTable.Cell>
        </NxTable.Row>
      </NxTable.Head>
    );
  }

  function renderTableBody() {
    return (
      <NxTable.Body emptyMessage={'No exclusions found'}>
        {exclusions.map((exclusion) => (
          <ExclusionLogTableRow
            key={`${exclusion.autoPolicyWaiverId}-${exclusion.autoPolicyWaiverExclusionId}`}
            exclusion={exclusion}
          />
        ))}
      </NxTable.Body>
    );
  }

  return (
    <NxTable>
      {renderTableHeader()}
      {renderTableBody()}
    </NxTable>
  );
}

ExclusionLogTable.propTypes = {
  exclusions: PropTypes.arrayOf(
    PropTypes.shape({
      createTime: PropTypes.string.isRequired,
      autoPolicyWaiverId: PropTypes.string.isRequired,
      autoPolicyWaiverExclusionId: PropTypes.string.isRequired,
      threatLevel: PropTypes.number.isRequired,
      policyName: PropTypes.string,
      componentDisplayName: PropTypes.string,
      vulnerabilityIdentifiers: PropTypes.string,
    })
  ).isRequired,
};
