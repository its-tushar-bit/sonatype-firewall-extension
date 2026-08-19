/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxH2,
  NxOverflowTooltip,
  NxSmallThreatCounter,
  NxTable,
  NxTextLink,
  NxTile,
} from '@sonatype/react-shared-components';
import { keys, props, values, zipObj } from 'ramda';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { formatDate, STANDARD_DATE_TIME_FORMAT_NO_TZ } from 'MainRoot/util/dateUtils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

import { selectRecentlyImportedSbomsTile } from './recentlyImportedSbomsTileSelectors';
import { actions } from './recentlyImportedSbomsTileSlice';

import './RecentlyImportedSbomsTile.scss';

const SMALL_THREAT_INDICATOR_KEYS_MAP = {
  criticalCount: 'criticalCount',
  highCount: 'severeCount',
  mediumCount: 'moderateCount',
  lowCount: 'lowCount',
};

const VIEW_BOM_STATE = 'sbomManager.management.view.bom';

const extractSmallThreatCounterProps = (sbom) =>
  zipObj(values(SMALL_THREAT_INDICATOR_KEYS_MAP), props(keys(SMALL_THREAT_INDICATOR_KEYS_MAP), sbom));

export default function RecentlyImportedSbomsTile() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const { loading, loadingErrorMessage, sortDirection, sboms } = useSelector(selectRecentlyImportedSbomsTile);

  const doLoad = () => dispatch(actions.loadRecentlyImportedSboms());

  useEffect(() => {
    doLoad();
  }, []);

  const isEmpty = isNilOrEmpty(sboms);

  const getBillOfMaterialsPageHref = (applicationPublicId, sbomVersion) =>
    uiRouterState.href(VIEW_BOM_STATE, { applicationPublicId, versionId: sbomVersion });

  const sbomsRows =
    !isEmpty &&
    sboms.map((sbom, index) => (
      <NxTable.Row key={index}>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <div className="nx-truncate-ellipsis sbom-manager-recently-imported-sboms-tile-table__application-name">
              {sbom.applicationName}
            </div>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>
          <NxOverflowTooltip>
            <NxTextLink
              className="nx-truncate-ellipsis sbom-manager-recently-imported-sboms-tile-table__sbom-version"
              href={getBillOfMaterialsPageHref(sbom.publicApplicationId, sbom.sbomVersion)}
            >
              {sbom.sbomVersion}
            </NxTextLink>
          </NxOverflowTooltip>
        </NxTable.Cell>
        <NxTable.Cell>{sbom.specification}</NxTable.Cell>
        <NxTable.Cell>{formatDate(sbom.importDate, STANDARD_DATE_TIME_FORMAT_NO_TZ)}</NxTable.Cell>
        <NxTable.Cell>
          <NxSmallThreatCounter {...extractSmallThreatCounterProps(sbom)} />
        </NxTable.Cell>
      </NxTable.Row>
    ));

  const isSortable = !loading && !loadingErrorMessage && !isEmpty;
  const appNameSortOptions = {
    isSortable,
    sortDir: sortDirection,
    onClick: () => {
      if (isSortable) {
        dispatch(actions.cycleNextSortDirection());
        dispatch(actions.sortSboms());
      }
    },
  };

  const tableBodyProps = {
    isLoading: loading,
    ...(loadingErrorMessage && { error: loadingErrorMessage }),
    ...(!loading && isEmpty && { emptyMessage: 'No recently imported SBOMs.' }),
  };

  return (
    <NxTile id="recently-imported-sboms-tile" className="sbom-manager-recently-imported-sboms-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Recently Imported SBOMs</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <NxTable id="recently-imported-sboms-tile-table" className="sbom-manager-recently-imported-sboms-tile-table">
          <NxTable.Head>
            <NxTable.Row>
              <NxTable.Cell {...appNameSortOptions}>Application Name</NxTable.Cell>
              <NxTable.Cell>Version</NxTable.Cell>
              <NxTable.Cell>BOM Format</NxTable.Cell>
              <NxTable.Cell>Import Date</NxTable.Cell>
              <NxTable.Cell>Vulnerabilities</NxTable.Cell>
            </NxTable.Row>
          </NxTable.Head>
          <NxTable.Body {...tableBodyProps}>{sbomsRows}</NxTable.Body>
        </NxTable>
      </NxTile.Content>
    </NxTile>
  );
}
