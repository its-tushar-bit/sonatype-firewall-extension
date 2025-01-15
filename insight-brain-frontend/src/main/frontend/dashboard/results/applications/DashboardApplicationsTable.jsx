/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxIndeterminatePagination,
  NxTable,
  NxTableBody,
  NxTableCell,
  NxTableContainer,
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
} from '@sonatype/react-shared-components';

import DashboardApplicationsTableRow, { applicationPropTypes } from './DashboardApplicationsTableRow';
import { Messages } from '../../../utilAngular/CommonServices';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import { sortColumn, getColumnDirection, extractSortFieldName } from '../../../util/sortUtils';
import { isNilOrEmpty } from '../../../util/jsUtil';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';

export default function DashboardApplicationsTable(props) {
  const {
      applicationResults: { results, hasNextPage, sortFields, error, hasMultiplePages, page },
      setNextApplicationsPage,
      setPreviousApplicationsPage,
      colorStyler,
      needsAcknowledgement,
      reload,
      sortApplications,
    } = props,
    currentPage = hasMultiplePages ? page : null,
    isLoading = !error && !results && !needsAcknowledgement,
    currentSortedColumnName = sortFields && extractSortFieldName(sortFields[0]),
    isCurrentColumnSortDescending = sortFields && sortFields[0].includes('-'),
    sort = (colName) => sortColumn(sortApplications, currentSortedColumnName, isCurrentColumnSortDescending, colName),
    sortDir = (colName) =>
      !error && results && getColumnDirection(currentSortedColumnName, isCurrentColumnSortDescending, colName),
    emptyTableMessage = 'No data available given the applied filters and permissions.',
    colSpan = 6;

  const generateTableBodyRows = () => {
    if (isNilOrEmpty(results)) {
      return null;
    }

    return (
      <Fragment>
        {results.map((application, rowIndex) => (
          <DashboardApplicationsTableRow
            application={application}
            key={application.applicationId}
            colorStyler={colorStyler}
            tableRowIndex={rowIndex}
          />
        ))}
      </Fragment>
    );
  };

  return (
    <div className="nx-table-container">
      <NxTable className="nx-table--fixed-layout">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell onClick={() => sort('applicationName')} sortDir={sortDir('applicationName')} isSortable>
              Name
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--total-risk'}
              onClick={() => sort('-totalApplicationRisk.totalRisk')}
              sortDir={sortDir('totalApplicationRisk.totalRisk')}
              isSortable
            >
              Total Risk
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--critical-risk'}
              onClick={() => sort('-totalApplicationRisk.criticalRisk')}
              sortDir={sortDir('totalApplicationRisk.criticalRisk')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="critical" presentational></NxThreatIndicator>
              <span>Critical</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--severe-risk'}
              onClick={() => sort('-totalApplicationRisk.severeRisk')}
              sortDir={sortDir('totalApplicationRisk.severeRisk')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="severe" presentational></NxThreatIndicator>
              <span>Severe</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--moderate-risk'}
              onClick={() => sort('-totalApplicationRisk.moderateRisk')}
              sortDir={sortDir('totalApplicationRisk.moderateRisk')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="moderate" presentational></NxThreatIndicator>
              <span>Moderate</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--low-risk'}
              onClick={() => sort('-totalApplicationRisk.lowRisk')}
              sortDir={sortDir('totalApplicationRisk.lowRisk')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="low" presentational></NxThreatIndicator>
              <span>Low</span>
            </NxTableCell>
          </NxTableRow>
        </NxTableHead>
        <NxTableBody
          isLoading={isLoading}
          error={Messages.getHttpErrorMessage(error)}
          retryHandler={reload}
          emptyMessage={emptyTableMessage}
        >
          {needsAcknowledgement ? <NeedsAcknowledgementInfoRow colSpan={colSpan} /> : generateTableBodyRows()}
        </NxTableBody>
      </NxTable>

      {!isLoading &&
        (currentPage === null || (currentPage === 0 && !hasNextPage) ? null : (
          <NxTableContainer.Footer>
            <NxIndeterminatePagination
              onPrevPageSelect={setPreviousApplicationsPage}
              onNextPageSelect={setNextApplicationsPage}
              isFirstPage={currentPage === 0}
              isLastPage={!hasNextPage}
            />
          </NxTableContainer.Footer>
        ))}
    </div>
  );
}

DashboardApplicationsTable.propTypes = {
  applicationResults: PropTypes.shape({
    results: PropTypes.arrayOf(applicationPropTypes),
    hasNextPage: PropTypes.bool,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
    hasMultiplePages: PropTypes.bool,
    page: PropTypes.number,
  }),
  colorStyler: heatMapColorStylerPropTypes,
  needsAcknowledgement: PropTypes.bool,
  reload: PropTypes.func.isRequired,
  sortApplications: PropTypes.func.isRequired,
  setNextApplicationsPage: PropTypes.func.isRequired,
  setPreviousApplicationsPage: PropTypes.func.isRequired,
};
