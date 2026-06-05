/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxLoadWrapper,
  NxPageTitle,
  NxH1,
  NxH4,
  NxP,
  NxTextLink,
  NxTile,
  NxButton,
  NxFontAwesomeIcon,
} from '@sonatype/react-shared-components';
import { faEllipsisVertical, faCircle } from '@fortawesome/free-solid-svg-icons';
import { faDatabase } from '@fortawesome/pro-regular-svg-icons';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { actions } from './hostedReposSlice';
import { selectRepositoryManagers, selectLoading, selectError } from './hostedReposSelectors';
import { selectIsHostedRepositoryEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

function getActivityIndicator(lastActivityTime) {
  if (lastActivityTime == null) {
    return {
      label: 'No activity recorded',
      className: 'iq-hosted-repos__status--no-activity',
    };
  }
  const sevenDaysMs = 7 * 24 * 60 * 60 * 1000;
  const isStale = Date.now() - lastActivityTime > sevenDaysMs;
  const date = new Date(lastActivityTime);
  const label = `Last activity: ${date.toLocaleString(undefined, {
    year: 'numeric',
    month: 'numeric',
    day: 'numeric',
    hour: 'numeric',
    minute: '2-digit',
  })}`;
  return {
    label,
    className: isStale ? 'iq-hosted-repos__status--stale' : 'iq-hosted-repos__status--active',
  };
}

export default function HostedReposPage() {
  const dispatch = useDispatch();
  const repositoryManagers = useSelector(selectRepositoryManagers);
  const loading = useSelector(selectLoading);
  const error = useSelector(selectError);
  const isHostedRepositoryEvaluationEnabled = useSelector(selectIsHostedRepositoryEvaluationEnabled);

  useEffect(() => {
    if (isHostedRepositoryEvaluationEnabled) {
      dispatch(actions.fetchRepositoryManagers());
    }
  }, [dispatch, isHostedRepositoryEvaluationEnabled]);

  const retryHandler = () => {
    if (isHostedRepositoryEvaluationEnabled) {
      dispatch(actions.fetchRepositoryManagers());
    }
  };

  const renderEmptyState = () => {
    return (
      <div className="iq-hosted-repos__empty">
        <NxFontAwesomeIcon icon={faDatabase} className="iq-hosted-repos__empty-icon" />
        <NxH4>No Nexus Repository Managers are currently connected.</NxH4>
        <NxP>
          To connect a Repository Manager, open the desired Nexus Repository Manager and configure the connection under
          Settings → IQ Server.
        </NxP>
      </div>
    );
  };

  const handleCardClick = (rm) => {
    dispatch(stateGo('hostedRepositories', { repositoryManagerId: rm.instanceId }));
  };

  const renderCards = () => {
    return (
      <div className="iq-hosted-repos__grid">
        {repositoryManagers.map((rm) => {
          const activity = getActivityIndicator(rm.lastActivityTime);

          return (
            <NxTile
              key={rm.instanceId}
              className="iq-hosted-repos__card iq-hosted-repos__card--clickable"
              onClick={(e) => {
                // Don't navigate when clicking the "More options" button
                if (!e.target.closest('.iq-hosted-repos__card-actions')) {
                  handleCardClick(rm);
                }
              }}
            >
              <NxTile.Header>
                <NxTile.HeaderTitle>
                  <div className="iq-hosted-repos__card-header">
                    <NxFontAwesomeIcon icon={faDatabase} className="iq-hosted-repos__card-icon" />
                    <div className="iq-hosted-repos__card-header-text">
                      <span className="iq-hosted-repos__card-title">{rm.instanceId}</span>
                      {rm.baseUrl && <span className="iq-hosted-repos__card-url">{rm.baseUrl}</span>}
                    </div>
                  </div>
                </NxTile.HeaderTitle>
                <NxTile.HeaderActions className="iq-hosted-repos__card-actions">
                  <NxButton variant="icon-only" title="More options">
                    <NxFontAwesomeIcon icon={faEllipsisVertical} />
                  </NxButton>
                </NxTile.HeaderActions>
              </NxTile.Header>
              <NxTile.Content>
                <div className="iq-hosted-repos__card-footer">
                  <div className={`iq-hosted-repos__card-status ${activity.className}`}>
                    <NxFontAwesomeIcon icon={faCircle} className="iq-hosted-repos__status-icon" />
                    <span className="iq-hosted-repos__status-label">{activity.label}</span>
                  </div>
                </div>
              </NxTile.Content>
            </NxTile>
          );
        })}
      </div>
    );
  };

  const renderContent = () => {
    if (!repositoryManagers || repositoryManagers.length === 0) {
      return renderEmptyState();
    }

    return renderCards();
  };

  if (!isHostedRepositoryEvaluationEnabled) {
    return null;
  }

  return (
    <div className="iq-hosted-repos">
      <NxPageTitle>
        <NxH1>Repository Managers</NxH1>
        <NxPageTitle.Description>
          <NxP>Select a Nexus Repository Manager instance to view its hosted repositories.</NxP>
          <NxTextLink href="#" external>
            Learn more about hosted repository evaluation
          </NxTextLink>
        </NxPageTitle.Description>
      </NxPageTitle>

      <NxLoadWrapper loading={loading} error={error} retryHandler={retryHandler}>
        {renderContent()}
      </NxLoadWrapper>
    </div>
  );
}
