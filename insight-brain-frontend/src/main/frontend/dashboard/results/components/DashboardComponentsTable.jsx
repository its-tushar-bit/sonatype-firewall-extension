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
  NxTableHead,
  NxTableRow,
  NxThreatIndicator,
  NxTableContainer,
} from '@sonatype/react-shared-components';

import DashboardComponentsTableRow, { componentPropTypes } from './DashboardComponentsTableRow';
import { Messages } from '../../../utilAngular/CommonServices';
import { heatMapColorStylerPropTypes } from '../DashboardHeatMapCell';
import { extractSortFieldName, getColumnDirection, sortColumn } from '../../../util/sortUtils';
import NeedsAcknowledgementInfoRow from '../NeedsAcknowledgementInfoRow';
import { isNilOrEmpty } from '../../../util/jsUtil';

export default function DashboardComponentsTable(props) {
  const {
      componentResults: { results, hasNextPage, sortFields, error, hasMultiplePages, page },
      colorStyler,
      needsAcknowledgement,
      reload,
      sortComponents,
      stateGo,
      setNextComponentsPage,
      setPreviousComponentsPage,
    } = props,
    currentPage = hasMultiplePages ? page : null,
    isLoading = !error && !results && !needsAcknowledgement,
    currentSortedColumnName = sortFields && extractSortFieldName(sortFields[0]),
    isCurrentColumnSortDescending = sortFields && sortFields[0].includes('-'),
    sort = (colName) => sortColumn(sortComponents, currentSortedColumnName, isCurrentColumnSortDescending, colName),
    sortDir = (colName) =>
      !error && results && getColumnDirection(currentSortedColumnName, isCurrentColumnSortDescending, colName),
    emptyTableMessage = 'No data available given the applied filters and permissions.',
    colSpan = 8;
  const generateTableBodyRows = () => {
    if (isNilOrEmpty(results)) {
      return null;
    }

    return (
      <Fragment>
        {results.map((component) => (
          <DashboardComponentsTableRow
            component={component}
            page={page}
            key={component.hash}
            stateGo={stateGo}
            colorStyler={colorStyler}
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
            <NxTableCell>Name</NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--affected-app'}
              onClick={() => sort('-affectedApplications')}
              sortDir={sortDir('affectedApplications')}
              isSortable
            >
              Apps
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--total-risk'}
              onClick={() => sort('-score')}
              sortDir={sortDir('score')}
              isSortable
            >
              Total Risk
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--critical-risk'}
              onClick={() => sort('-scoreCritical')}
              sortDir={sortDir('scoreCritical')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="critical" presentational />
              <span>Critical</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--severe-risk'}
              onClick={() => sort('-scoreSevere')}
              sortDir={sortDir('scoreSevere')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="severe" presentational />
              <span>Severe</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--moderate-risk'}
              onClick={() => sort('-scoreModerate')}
              sortDir={sortDir('scoreModerate')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="moderate" presentational />
              <span>Moderate</span>
            </NxTableCell>
            <NxTableCell
              className={'nx-cell--num iq-cell--low-risk'}
              onClick={() => sort('-scoreLow')}
              sortDir={sortDir('scoreLow')}
              isSortable
            >
              <NxThreatIndicator threatLevelCategory="low" presentational />
              <span>Low</span>
            </NxTableCell>
            <NxTableCell chevron />
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
              onPrevPageSelect={setPreviousComponentsPage}
              onNextPageSelect={setNextComponentsPage}
              isFirstPage={currentPage === 0}
              isLastPage={!hasNextPage}
            />
          </NxTableContainer.Footer>
        ))}
    </div>
  );
}

DashboardComponentsTable.propTypes = {
  componentResults: PropTypes.shape({
    results: PropTypes.arrayOf(componentPropTypes),
    hasNextPage: PropTypes.bool,
    sortFields: PropTypes.arrayOf(PropTypes.string),
    error: PropTypes.oneOfType([PropTypes.string, PropTypes.instanceOf(Error), PropTypes.object]),
    hasMultiplePages: PropTypes.bool,
    page: PropTypes.number,
  }),
  colorStyler: heatMapColorStylerPropTypes,
  needsAcknowledgement: PropTypes.bool,
  reload: PropTypes.func.isRequired,
  sortComponents: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  setNextComponentsPage: PropTypes.func.isRequired,
  setPreviousComponentsPage: PropTypes.func.isRequired,
};
