/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import { slice } from 'ramda';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import LegalDashboardComponentRow from './LegalDashboardComponentRow';

export default function LegalDashboardComponentsTab({ components, filtersAreDirty }) {
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 5;
  const rows = slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE, components);

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      { filtersAreDirty && <div className="form-mask" /> }
      <NxTable id="legal-dashboard-applications-table" className="legal-dashboard-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Component</NxTableCell>
            <NxTableCell>Licenses</NxTableCell>
            <NxTableCell>Occurrences</NxTableCell>
            <NxTableCell>Review Progress</NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody emptyMessage="No components found">
          { rows.map((row, index) => <LegalDashboardComponentRow key = { index } row={ row } />) }
        </NxTableBody>
      </NxTable>
      { components && components.length > 0 &&
        <div className="nx-table-container__footer">
          <NxPagination pageCount={Math.ceil(components.length / PAGE_SIZE)}
                        currentPage={page}
                        onChange={setPage}/>
        </div>
      }
    </div>
  );
}

LegalDashboardComponentsTab.propTypes = {
  components: PropTypes.any,
  filtersAreDirty: PropTypes.bool
};
