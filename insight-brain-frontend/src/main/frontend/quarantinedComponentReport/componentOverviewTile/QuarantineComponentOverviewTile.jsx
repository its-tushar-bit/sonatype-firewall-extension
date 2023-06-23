/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxButton, NxTextLink, NxThreatIndicator } from '@sonatype/react-shared-components';
import { formatTimeAgo } from '../../util/dateUtils';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { goToRepositoryComponentDetailsPage } from 'MainRoot/firewall/firewallActions';
import { useDispatch } from 'react-redux';

export default function QuarantineComponentOverviewTile(props) {
  // viewState
  const { componentOverview } = props;
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();

  const goToRepositoryComponentDetails = () =>
    dispatch(
      goToRepositoryComponentDetailsPage(
        componentOverview.repositoryId,
        componentOverview.componentIdentifier,
        componentOverview.componentHash,
        componentOverview.matchState,
        componentOverview.pathname,
        componentOverview.componentDisplayName
      )
    );

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
    </dl>
  );

  const repoInfoContent = (
    <dl className="nx-read-only nx-read-only--grid iq-repo-info-content">
      <div className="nx-read-only__item">
        <dt className="nx-read-only__label">Repository</dt>
        <dd className="nx-read-only__data">
          <NxTextLink
            newTab
            href={uiRouterState.href('repository-report', { repositoryId: componentOverview.repositoryId })}
          >
            {componentOverview.repositoryName}
          </NxTextLink>
        </dd>
      </div>
    </dl>
  );

  return (
    <section className="nx-tile iq-quarantine-report-component-overview-tile">
      <header className="nx-tile-header">
        <div className="nx-tile-header__title">
          <h2 className="nx-h2">{componentOverview.componentDisplayName}</h2>
        </div>
        <div className="nx-tile__actions">
          <NxButton variant="tertiary" onClick={goToRepositoryComponentDetails}>
            View Component Details
          </NxButton>
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
    componentIdentifier: PropTypes.object.isRequired,
    componentHash: PropTypes.string.isRequired,
    matchState: PropTypes.string.isRequired,
    pathname: PropTypes.string.isRequired,
    componentDisplayName: PropTypes.string.isRequired,
    isQuarantined: PropTypes.bool.isRequired,
    quarantinedPolicyViolationsCount: PropTypes.number.isRequired,
    repositoryId: PropTypes.string.isRequired,
    repositoryName: PropTypes.string.isRequired,
    quarantinedDate: PropTypes.string.isRequired,
  }).isRequired,
};
