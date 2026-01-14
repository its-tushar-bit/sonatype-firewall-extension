/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxTable, NxTextLink } from '@sonatype/react-shared-components';
import { getApplicationReportDeepLinkUrl } from 'MainRoot/util/CLMLocation';
import { formatDate, STANDARD_DATE_FORMAT } from 'MainRoot/util/dateUtils';
import { actions } from './react2ShellSlice';
import { selectSortBy, selectSortOrder, selectSorting } from './react2ShellSelectors';

export default function React2ShellImpactTable({ data }) {
  const dispatch = useDispatch();
  const sortBy = useSelector(selectSortBy);
  const sortOrder = useSelector(selectSortOrder);
  const sorting = useSelector(selectSorting);

  const handleSort = (column) => {
    dispatch(actions.fetchWithSort({ column }));
  };

  const columns = [
    { key: 'applicationName', label: 'Application' },
    { key: 'stage', label: 'Stage' },
    { key: 'componentName', label: 'Component' },
    { key: 'version', label: 'Version' },
    { key: 'cveId', label: 'CVE ID' },
    { key: 'recommendedAction', label: 'Recommended Action' },
    { key: 'activeWaiver', label: 'Active Waiver' },
    { key: 'violating', label: 'Violating' },
    { key: 'evaluation', label: 'Evaluation' },
    { key: 'evaluationDate', label: 'Evaluation Date' },
  ];

  const NON_SORTABLE_COLUMNS = ['evaluation', 'version', 'recommendedAction'];

  const getReportUrl = (row) => getApplicationReportDeepLinkUrl(row.applicationPublicId, row.reportId);

  return (
    <NxTable>
      <NxTable.Head>
        <NxTable.Row>
          {columns.map((column) => {
            const isSortable = !NON_SORTABLE_COLUMNS.includes(column.key);
            return (
              <NxTable.Cell
                key={column.key}
                isSortable={isSortable}
                sortDir={isSortable && sortBy === column.key ? sortOrder : null}
                onClick={isSortable ? () => handleSort(column.key) : undefined}
              >
                {column.label}
              </NxTable.Cell>
            );
          })}
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body
        isLoading={sorting}
        emptyMessage="No impact data available. Run a scan to identify affected components."
      >
        {data.map((row, index) => (
          <NxTable.Row key={index}>
            <NxTable.Cell>{row.applicationName}</NxTable.Cell>
            <NxTable.Cell>{row.stage}</NxTable.Cell>
            <NxTable.Cell>{row.componentDisplayName}</NxTable.Cell>
            <NxTable.Cell>{row.version}</NxTable.Cell>
            <NxTable.Cell>{row.cveId ? row.cveId : '—'}</NxTable.Cell>
            <NxTable.Cell>{row.recommendedAction}</NxTable.Cell>
            <NxTable.Cell>{row.activeWaiver ? 'Yes' : 'No'}</NxTable.Cell>
            <NxTable.Cell>{row.violating ? 'Yes' : 'No'}</NxTable.Cell>
            <NxTable.Cell>
              <NxTextLink href={getReportUrl(row)}>View</NxTextLink>
            </NxTable.Cell>
            <NxTable.Cell>{formatDate(row.evaluationDate, STANDARD_DATE_FORMAT)}</NxTable.Cell>
          </NxTable.Row>
        ))}
      </NxTable.Body>
    </NxTable>
  );
}

React2ShellImpactTable.propTypes = {
  data: PropTypes.arrayOf(
    PropTypes.shape({
      applicationName: PropTypes.string.isRequired,
      applicationPublicId: PropTypes.string,
      applicationInternalId: PropTypes.string,
      stage: PropTypes.string,
      reportId: PropTypes.string,
      componentDisplayName: PropTypes.string.isRequired,
      packageUrl: PropTypes.string,
      hash: PropTypes.string,
      version: PropTypes.string.isRequired,
      cveId: PropTypes.string,
      recommendedAction: PropTypes.string.isRequired,
      activeWaiver: PropTypes.bool.isRequired,
      violating: PropTypes.bool,
      evaluation: PropTypes.string.isRequired,
      evaluationDate: PropTypes.string,
      baseUrl: PropTypes.string,
    })
  ),
};
