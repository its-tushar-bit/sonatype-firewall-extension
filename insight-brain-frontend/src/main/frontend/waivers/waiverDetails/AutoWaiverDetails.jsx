/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxTile,
  NxH2,
  NxLoadWrapper,
  NxReadOnly,
  NxFontAwesomeIcon,
  NxSmallTag,
  NxThreatIndicator,
  NxTextLink,
} from '@sonatype/react-shared-components';

import {
  selectAutoWaiverDetails,
  selectAutoWaiverDetailsLoading,
  selectAutoWaiverDetailsError,
} from './autoWaiverDetailsSelectors';
import { actions } from './autoWaiverDetailsSlice';
import { faSitemap, faTerminal } from '@fortawesome/pro-solid-svg-icons';
import moment from 'moment';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function AutoWaiverDetails() {
  const uiRouterState = useRouterState();

  const isLoading = useSelector(selectAutoWaiverDetailsLoading);
  const loadError = useSelector(selectAutoWaiverDetailsError);
  const details = useSelector(selectAutoWaiverDetails);

  const { createTime, pathForward, reachability, threatLevel, ownerName, ownerType, publicId } = details || {};
  const formatDate = (date) => moment(date).format('MMMM D, YYYY');

  const isApplication = ownerType === 'application';
  const ownerManagementUrl = isApplication
    ? uiRouterState.href('management.view.application', {
        applicationPublicId: publicId,
      })
    : uiRouterState.href(`management.view.organization`, {
        organizationId: publicId,
      });

  const scopeIcon = isApplication ? <NxFontAwesomeIcon icon={faTerminal} /> : <NxFontAwesomeIcon icon={faSitemap} />;

  const dispatch = useDispatch();
  const getDetails = () => dispatch(actions.loadAutoWaiverDetails());

  useEffect(() => {
    getDetails();
  }, []);

  return (
    <NxTile className="nx-viewport-sized" id="auto-waiver-details-page" data-testid="auto-waiver-details-page">
      <NxTile.Header className="iq-auto-waiver-details-header-container">
        <NxTile.HeaderTitle>
          <NxH2 id="iq-auto-waiver-details-header">Auto-Waiver Details</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={getDetails}>
        <div className="iq-auto-waiver-details-content">
          {/* Policy */}
          <NxReadOnly className="iq-auto-waiver-details__policy">
            <NxReadOnly.Label id="iq-auto-waiver-details__policy">Policy Threat Level</NxReadOnly.Label>
            <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__policy">
              <NxThreatIndicator policyThreatLevel={threatLevel} /> ≤ {threatLevel}
            </NxReadOnly.Data>
          </NxReadOnly>

          <NxReadOnly className="nx-read-only--grid">
            {/* Scope */}
            <NxReadOnly.Item className="iq-auto-waiver-details__scope">
              <NxReadOnly.Label id="iq-auto-waiver-details__scope">Scope</NxReadOnly.Label>
              <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__scope">
                {scopeIcon}
                <NxTextLink href={ownerManagementUrl}>{ownerName}</NxTextLink>
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Waiver Expiration */}
            <NxReadOnly.Item className="iq-auto-waiver-details__expiration">
              <NxReadOnly.Label id="iq-auto-waiver-details__expiration">Expiration</NxReadOnly.Label>
              <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__expiration">
                <NxSmallTag color="green">Auto</NxSmallTag>
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Components */}
            <NxReadOnly.Item className="iq-auto-waiver-details__components">
              <NxReadOnly.Label id="iq-auto-waiver-details__components">Component(s)</NxReadOnly.Label>
              <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__components">Any component</NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Version */}
            <NxReadOnly.Item className="iq-auto-waiver-details__version">
              <NxReadOnly.Label id="iq-auto-waiver-details__version">Version</NxReadOnly.Label>
              <NxReadOnly.Data
                aria-labelledby="iq-auto-waiver-details__version"
                data-testid="auto-waiver-details-version"
              >
                Current or latest non-violating
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Reason */}
            <NxReadOnly.Item className="iq-auto-waiver-details__reason">
              <NxReadOnly.Label id="iq-auto-waiver-details__reason">Reason</NxReadOnly.Label>
              <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__reason">
                {!pathForward && !reachability ? (
                  'N/A'
                ) : (
                  <ul>
                    {pathForward && <li>No upgrade path</li>}
                    {reachability && (
                      <li>
                        <ReachabilityStatus reachabilityStatus={'NOT_REACHABLE'} />
                      </li>
                    )}
                  </ul>
                )}
              </NxReadOnly.Data>
            </NxReadOnly.Item>
            {/* Date Created */}
            <NxReadOnly.Item className="iq-auto-waiver-details__date-created">
              <NxReadOnly.Label id="iq-auto-waiver-details__date-created">Date Created</NxReadOnly.Label>
              <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__date-created">
                {formatDate(createTime)}
              </NxReadOnly.Data>
            </NxReadOnly.Item>
          </NxReadOnly>
        </div>
      </NxLoadWrapper>
    </NxTile>
  );
}
