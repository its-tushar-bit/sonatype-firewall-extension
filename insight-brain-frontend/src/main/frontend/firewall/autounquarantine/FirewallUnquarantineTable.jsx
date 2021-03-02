/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';

export default function FirewallUnquarantineTable() {
  const currentPage = 0,
      pageCount = 100,
      setPage = () => {};
  return (
    <div className="nx-table-container">
      <NxTable>
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>COMPONENT</NxTableCell>
            <NxTableCell>POLICY TYPE</NxTableCell>
            <NxTableCell isNumeric>QUARANTINE DATE</NxTableCell>
            <NxTableCell>REPOSITORY</NxTableCell>
            <NxTableCell isNumeric>DATE CLEARED</NxTableCell>
          </NxTableRow>

          <NxTableRow isFilterHeader>
            <NxTableCell/>
            <NxTableCell>
              <select className="nx-form-select">
                <option>Release Integrity</option>
              </select>
            </NxTableCell>
            <NxTableCell/>
            <NxTableCell/>
            <NxTableCell/>
          </NxTableRow>

        </NxTableHead>
        <NxTableBody emptyMessage="No data found."/>
      </NxTable>

      <div className="nx-table-container__footer">
        <NxPagination className="iq-firewall-table__nav-bar" aria-controls="pagination-filter-table"
                      {...{pageCount, currentPage}} onChange={setPage}/>
      </div>
    </div>
  );
}

