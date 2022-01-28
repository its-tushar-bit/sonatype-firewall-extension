/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxThreatIndicator } from '@sonatype/react-shared-components';
import { formatTimeAgo } from '../../util/dateUtils';

export default function QuarantineComponentOverviewTile(props) {
  // viewState
  const { componentOverview } = props;

  const generalInfoContent = (
    <dl className="nx-read-only nx-read-only--grid iq-general-info-content">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Status</dt>
        <dd className="nx-read-only__data">
          <NxThreatIndicator threatLevelCategory={componentOverview.isQuarantined ? 'critical' : 'none'} />
          <span>{getQuarantineLabel(componentOverview.isQuarantined)}</span>
        </dd>
      </div>

      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Quarantine Reason</dt>
        <dd className="nx-read-only__data">{componentOverview.quarantinedPolicyViolationsCount} policy violations</dd>
      </div>

      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">First Quarantined</dt>
        <dd className="nx-read-only__data">{formatTimeAgo(new Date(componentOverview.quarantinedDate))}</dd>
      </div>

      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Catalogued Date</dt>
        <dd className="nx-read-only__data">{formatTimeAgo(new Date(componentOverview.cataloguedDate))}</dd>
      </div>
    </dl>
  );

  const repoInfoContent = (
    <dl className="nx-read-only nx-read-only--grid iq-repo-info-content">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Repository</dt>
        <dd className="nx-read-only__data">{componentOverview.repositoryName}</dd>
      </div>
    </dl>
  );

  return (
    <section className="nx-tile iq-quarantine-report-component-overview-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">{componentOverview.componentDisplayName}</h2>
        </div>
      </header>
      <div className="nx-tile-content">
        <div className="nx-grid-row">
          <div className="nx-grid-col iq-component-data-col">{generalInfoContent}</div>
          <div className="nx-grid-col iq-component-data-col">{repoInfoContent}</div>
        </div>
      </div>
    </section>
  );
}

function getQuarantineLabel(isQuarantined) {
  return isQuarantined ? 'Quarantined' : 'Unquarantined';
}

QuarantineComponentOverviewTile.propTypes = {
  componentOverview: PropTypes.shape({
    componentOverviewLoading: PropTypes.bool.isRequired,
    componentDisplayName: PropTypes.string.isRequired,
    isQuarantined: PropTypes.bool.isRequired,
    quarantinedPolicyViolationsCount: PropTypes.number.isRequired,
    repositoryName: PropTypes.string.isRequired,
    quarantinedDate: PropTypes.string.isRequired,
    cataloguedDate: PropTypes.string.isRequired,
  }).isRequired,
};
