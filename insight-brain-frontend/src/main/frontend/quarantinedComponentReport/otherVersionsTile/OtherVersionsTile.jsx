/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxPagination, NxTable } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import {
  selectCurrentPage,
  selectLoadError,
  selectLoading,
  selectOtherVersions,
  selectPageCount,
  selectToken,
} from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSelectors';
import { actions } from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSlice';

export default function OtherVersionsTile() {
  //Selectors
  const selectedOtherVersions = useSelector(selectOtherVersions);
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const currentPage = useSelector(selectCurrentPage);
  const pageCount = useSelector(selectPageCount);
  const token = useSelector(selectToken);

  const dispatch = useDispatch();

  // Actions
  const loadOtherVersions = (token) => dispatch(actions.loadOtherVersions(token));
  const setOtherVersionsGrid = (token, page) => dispatch(actions.setOtherVersionsGridPage(token, page));

  useEffect(() => {
    loadOtherVersions(token);
  }, [token]);

  return (
    <section className="nx-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">Other Allowed Versions</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="nx-table-container iq-quarantine-report-component-other-versions">
          <NxTable>
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell>COMPONENT</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body emptyMessage="No data found." isLoading={loading} error={loadError}>
              {selectedOtherVersions.map((row) => {
                return (
                  <NxTable.Row key={row}>
                    <NxTable.Cell>{row}</NxTable.Cell>
                  </NxTable.Row>
                );
              })}
            </NxTable.Body>
          </NxTable>
          <div className="nx-table-container__footer">
            <NxPagination
              pageCount={pageCount}
              currentPage={currentPage}
              onChange={(page) => setOtherVersionsGrid(token, page)}
            />
          </div>
        </div>
      </div>
    </section>
  );
}
