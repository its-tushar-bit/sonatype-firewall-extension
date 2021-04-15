/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxButton,
  NxFontAwesomeIcon,
  NxPagination,
  NxStatefulCheckbox,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow,
} from '@sonatype/react-shared-components';

import { faSync } from '@fortawesome/pro-solid-svg-icons';

export default function FirewallQuarantineTable() {
  const currentPage = 0,
    pageCount = 100,
    setPage = () => {};
  return (
    <section id="firewall-quarantine-table">
      <header className="iq-firewall-table-header nx-page-title">
        <h2 className="nx-h2 iq-firewall-table-label">Quarantine</h2>
        <div className="iq-firewall-table__time">Updated 2:42 PM 2021-01-11</div>
        <div className="nx-btn-bar">
          <NxButton variant="tertiary">
            <NxFontAwesomeIcon icon={faSync} />
            <span>Refresh</span>
          </NxButton>
        </div>
      </header>
      <div className="nx-table-container">
        <NxTable>
          <NxTableHead>
            <NxTableRow>
              <NxTableCell hasIcon>THREAT</NxTableCell>
              <NxTableCell>POLICY TYPE</NxTableCell>
              <NxTableCell isNumeric>QUARANTINE DATE</NxTableCell>
              <NxTableCell>COMPONENT</NxTableCell>
              <NxTableCell>REPOSITORY</NxTableCell>
              <NxTableCell>RELEASE</NxTableCell>
            </NxTableRow>

            <NxTableRow isFilterHeader>
              <NxTableCell />
              <NxTableCell>
                <select className="nx-form-select">
                  <option>Release Integrity</option>
                </select>
              </NxTableCell>
              <NxTableCell />
              <NxTableCell />
              <NxTableCell />
              <NxTableCell>
                <NxStatefulCheckbox checkboxId="subscribe-check" defaultChecked={false}>
                  all
                </NxStatefulCheckbox>
              </NxTableCell>
            </NxTableRow>
          </NxTableHead>
          <NxTableBody emptyMessage="No data found." />
        </NxTable>

        <div className="nx-table-container__footer iq-firewall-table__footer">
          <NxPagination
            className="iq-firewall-table__nav-bar"
            aria-controls="pagination-filter-table"
            {...{ pageCount, currentPage }}
            onChange={setPage}
          />
          <NxButton className="iq-firewall-table__release_btn" variant="primary">
            Release
          </NxButton>
        </div>
      </div>
    </section>
  );
}
