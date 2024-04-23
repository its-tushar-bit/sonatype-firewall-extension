/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import {
  NxTile,
  NxH2,
  NxLoadWrapper,
  NxTextLink,
  NxTooltip,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faInfoCircle } from '@fortawesome/pro-solid-svg-icons';

import './ApplicationsHistoryTile.scss';

export default function ApplicationsHistoryTile() {
  const doLoad = () => {};

  const totalScannedApplications = 18528;
  const applicationsUpdatedLastYear = 18528;
  const applicationsUpdatedLastMonth = 18528;
  const applicationsUpdatedLastWeek = 18528;

  return (
    <NxLoadWrapper retryHandler={doLoad}>
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
          <dl className="sbom-manager-applications-history-tile-list">
            <dt className="sbom-manager-applications-history-tile-list__label">
              Total scanned appplications (all time)
            </dt>
            <dd
              id="applications-history-tile-total-scanned-applications"
              className="sbom-manager-applications-history-tile-list__value"
            >
              {totalScannedApplications.toLocaleString('en-US')}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last year</dt>
            <dd
              id="applications-history-tile-applications-updated-last-year"
              className="sbom-manager-applications-history-tile-list__value"
            >
              {applicationsUpdatedLastYear.toLocaleString('en-US')}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last month</dt>
            <dd
              id="applications-history-tile-applications-updated-last-month"
              className="sbom-manager-applications-history-tile-list__value"
            >
              {applicationsUpdatedLastMonth.toLocaleString('en-US')}
            </dd>

            <dt className="sbom-manager-applications-history-tile-list__label">Applications updated last week</dt>
            <dd
              id="applications-history-tile-applications-updated-last-week"
              className="sbom-manager-applications-history-tile-list__value"
            >
              {applicationsUpdatedLastWeek.toLocaleString('en-US')}
            </dd>
          </dl>
          <hr className="nx-divider" />
          <div className="sbom-manager-applications-history-tile__action">
            <NxTextLink href="#">View Latest Application Versions</NxTextLink>
          </div>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}
