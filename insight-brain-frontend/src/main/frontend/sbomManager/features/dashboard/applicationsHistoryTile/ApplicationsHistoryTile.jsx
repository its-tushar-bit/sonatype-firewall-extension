/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxTile, NxH2, NxTooltip, NxFontAwesomeIcon, NxTextLink } from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';
import { useDispatch, useSelector } from 'react-redux';

import { useRouterState } from 'MainRoot/react/RouterStateContext';
import LoadWrapper from 'MainRoot/react/LoadWrapper';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';
import { selectApplicationsHistoryTile } from './applicationsHistoryTileSelectors';
import { actions } from './applicationsHistoryTileSlice';

import './ApplicationsHistoryTile.scss';

export default function ApplicationsHistoryTile() {
  const dispatch = useDispatch();

  const {
    loading,
    loadError,
    totalScannedApplications,
    applicationsUpdatedLastYear,
    applicationsUpdatedLastMonth,
    applicationsUpdatedLastWeek,
  } = useSelector(selectApplicationsHistoryTile);
  const uiRouterState = useRouterState();

  const load = () => dispatch(actions.loadApplicationsHistory());

  useEffect(() => {
    load();
  }, []);

  const applicationsPageHref = uiRouterState.href('sbomManager.applications');

  return (
    <NxTile id="applications-history-tile" className="sbom-manager-applications-history-tile">
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>Applications History</NxH2>
          <NxTooltip title="Track the number of applications with updated SBOMs.">
            <NxFontAwesomeIcon icon={faInfoCircle} className="sbom-manager-applications-history-tile__info-icon" />
          </NxTooltip>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <LoadWrapper retryHandler={() => load()} loading={loading} error={loadError}>
          <dl className="sbom-manager-applications-history-tile-list">
            <dt className="sbom-manager-applications-history-tile-list__label">
              Total scanned appplications (all time)
            </dt>
            <dd
              id="applications-history-tile-total-scanned-applications"
              className="sbom-manager-applications-history-tile-list__value"
              data-testid="applications-history-tile-total-scanned-applications"
            >
              {formatNumberLocale(totalScannedApplications)}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last year</dt>
            <dd
              id="applications-history-tile-applications-updated-last-year"
              className="sbom-manager-applications-history-tile-list__value"
              data-testid="applications-history-tile-applications-updated-last-year"
            >
              {formatNumberLocale(applicationsUpdatedLastYear)}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last month</dt>
            <dd
              id="applications-history-tile-applications-updated-last-month"
              className="sbom-manager-applications-history-tile-list__value"
              data-testid="applications-history-tile-applications-updated-last-month"
            >
              {formatNumberLocale(applicationsUpdatedLastMonth)}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last week</dt>
            <dd
              id="applications-history-tile-applications-updated-last-week"
              className="sbom-manager-applications-history-tile-list__value"
              data-testid="applications-history-tile-applications-updated-last-week"
            >
              {formatNumberLocale(applicationsUpdatedLastWeek)}
            </dd>
          </dl>
          <hr className="nx-divider" />
          <div className="sbom-manager-applications-history-tile__action">
            <NxTextLink className="sbom-manager-applications-history-tile__link" href={applicationsPageHref}>
              View Latest Application Versions
            </NxTextLink>
          </div>
        </LoadWrapper>
      </NxTile.Content>
    </NxTile>
  );
}
