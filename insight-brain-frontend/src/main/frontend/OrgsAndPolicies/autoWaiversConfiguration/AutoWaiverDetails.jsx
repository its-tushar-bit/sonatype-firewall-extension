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
  NxTextLink, NxH3,
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
import AutoWaiverExclusionLogTable from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverExclusionLogTable';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import './_autoWaiverDetails.scss';

export default function AutoWaiverDetails() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const isLoading = useSelector(selectAutoWaiverDetailsLoading);
  const loadError = useSelector(selectAutoWaiverDetailsError);
  const details = useSelector(selectAutoWaiverDetails);
  const selectedOwner = useSelector(selectSelectedOwner);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);

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

  const loadAutoWaiverDetails = () => {
    dispatch(actions.loadAutoWaiverDetails());
  };

  const isInherited = selectedOwner && routerCurrentParams &&
      selectedOwner.id && routerCurrentParams.autoWaiverOwnerId &&
      selectedOwner.id !== routerCurrentParams.autoWaiverOwnerId;

  useEffect(() => {
    loadAutoWaiverDetails();
  }, []);

  return (
    <>
      <NxTile className="nx-viewport-sized" id="auto-waiver-details" data-testid="auto-waiver-details">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Auto-Waiver Details</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxLoadWrapper loading={isLoading} error={loadError} retryHandler={loadAutoWaiverDetails}>
          <div>
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
              <NxReadOnly.Item>
                <NxReadOnly.Label id="iq-auto-waiver-details__components">Component(s)</NxReadOnly.Label>
                <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__components">Any component</NxReadOnly.Data>
              </NxReadOnly.Item>
              {/* Version */}
              <NxReadOnly.Item>
                <NxReadOnly.Label id="iq-auto-waiver-details__version">Version</NxReadOnly.Label>
                <NxReadOnly.Data
                  aria-labelledby="iq-auto-waiver-details__version"
                  data-testid="auto-waiver-details-version"
                >
                  Current or latest non-violating
                </NxReadOnly.Data>
              </NxReadOnly.Item>
              {/* Reason */}
              <NxReadOnly.Item>
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
              <NxReadOnly.Item>
                <NxReadOnly.Label id="iq-auto-waiver-details__date-created">Date Created</NxReadOnly.Label>
                <NxReadOnly.Data aria-labelledby="iq-auto-waiver-details__date-created">
                  {formatDate(createTime)}
                </NxReadOnly.Data>
              </NxReadOnly.Item>
            </NxReadOnly>
          </div>
        </NxLoadWrapper>
      </NxTile>
      <NxTile className="iq-exclusion-log-tile">
        <NxH2>Exclusion Log</NxH2>
        <NxH3>Violations excluded from this automation</NxH3>
        <AutoWaiverExclusionLogTable disableDelete={isInherited}/>
      </NxTile>
    </>
  );
}
