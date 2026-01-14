/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxPageMain, NxLoadWrapper, NxH2, NxPagination, NxTableContainer } from '@sonatype/react-shared-components';
import React2ShellHeader from './React2ShellHeader';
import React2ShellAbout from './React2ShellAbout';
import React2ShellSummaryTiles from './React2ShellSummaryTiles';
import React2ShellBanner from './React2ShellBanner';
import React2ShellImpactTable from './React2ShellImpactTable';
import { actions, PAGE_SIZE, REACT2SHELL_CVE_IDS } from './react2ShellSlice';
import {
  selectLoading,
  selectError,
  selectSummaryMetrics,
  selectImpactData,
  selectPagination,
  selectCurrentPage,
  selectSortBy,
  selectSortOrder,
} from './react2ShellSelectors';
import { sendGainsightCustomEvent } from 'MainRoot/util/gainsightUtils';

const REACT2SHELL_PAGE_VIEWED = 'react2shell_impact_report_viewed';

export default function React2ShellPage() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const summaryMetrics = useSelector(selectSummaryMetrics);
  const impactData = useSelector(selectImpactData);
  const pagination = useSelector(selectPagination);
  const currentPage = useSelector(selectCurrentPage);
  const sortBy = useSelector(selectSortBy);
  const sortOrder = useSelector(selectSortOrder);

  useEffect(() => {
    dispatch(
      actions.fetchReportData({
        pageNumber: currentPage + 1,
        pageSize: PAGE_SIZE,
        sortBy,
        sortOrder,
      })
    );
  }, [dispatch, currentPage]);

  useEffect(() => {
    sendGainsightCustomEvent(REACT2SHELL_PAGE_VIEWED);

    return () => {
      dispatch(actions.reset());
    };
  }, [dispatch]);

  const handleRetry = () => {
    dispatch(
      actions.fetchReportData({
        pageNumber: currentPage + 1,
        pageSize: PAGE_SIZE,
        sortBy,
        sortOrder,
      })
    );
  };

  const handlePageChange = (newPage) => {
    dispatch(actions.setPage(newPage));
  };

  const getPaginationText = () => {
    if (!pagination || !pagination.totalItems) {
      return null;
    }

    const { page, pageSize, totalItems } = pagination;
    const startItem = (page - 1) * pageSize + 1;
    const endItem = Math.min(page * pageSize, totalItems);

    return `Showing ${startItem}-${endItem} of ${totalItems} affected components`;
  };

  return (
    <NxPageMain className="iq-react2shell-page">
      <React2ShellHeader cveIds={REACT2SHELL_CVE_IDS} />
      <React2ShellAbout cveIds={REACT2SHELL_CVE_IDS} />

      <NxLoadWrapper loading={loading} error={error} retryHandler={handleRetry}>
        <React2ShellSummaryTiles metrics={summaryMetrics} />
        <React2ShellBanner />

        <div className="iq-react2shell-impact-header">
          <NxH2>Impact Summary</NxH2>
          {getPaginationText() && (
            <span className="iq-react2shell-impact-header__pagination">{getPaginationText()}</span>
          )}
        </div>
        <NxTableContainer>
          <React2ShellImpactTable data={impactData} />
          {pagination && pagination.totalPages > 1 && (
            <NxTableContainer.Footer>
              <NxPagination currentPage={currentPage} pageCount={pagination.totalPages} onChange={handlePageChange} />
            </NxTableContainer.Footer>
          )}
        </NxTableContainer>
      </NxLoadWrapper>
    </NxPageMain>
  );
}
