/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import {
  NxPagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableHead,
  NxTableRow
} from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { slice } from 'ramda';
import LegalDashboardApplicationRow from './LegalDashboardApplicationRow';
import { applicationPropType } from '../advancedLegalPropTypes';

export default function LegalDashboardApplicationsTab({ applications, filtersAreDirty }) {
  const [page, setPage] = useState(0);
  const PAGE_SIZE = 30;
  const rows = slice(page * PAGE_SIZE, (page + 1) * PAGE_SIZE, applications);

  return (
    <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
      { filtersAreDirty && <div className="form-mask" /> }
      <NxTable id="legal-dashboard-applications-table" className="legal-dashboard-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Application</NxTableCell>
            <NxTableCell>Last Scan</NxTableCell>
            <NxTableCell>App Categories</NxTableCell>
            <NxTableCell>Components Reviewed</NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody emptyMessage="No applications found">
          { rows.map((row, index) => <LegalDashboardApplicationRow key={ index } row={ row } />) }
        </NxTableBody>
      </NxTable>
      { applications && applications.length > 0 &&
        <div className="nx-table-container__footer">
          <NxPagination pageCount={ Math.ceil(applications.length / PAGE_SIZE) }
                        currentPage={ page }
                        onChange={ setPage } />
        </div>
      }
    </div>
  );
}

LegalDashboardApplicationsTab.propTypes = {
  applications: PropTypes.arrayOf(applicationPropType),
  filtersAreDirty: PropTypes.bool
};
