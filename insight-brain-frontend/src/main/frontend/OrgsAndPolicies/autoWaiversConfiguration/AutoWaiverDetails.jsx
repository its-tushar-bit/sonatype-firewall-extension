/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useMemo } from 'react';
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
  NxH3,
  NxButton,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { selectHasAutoWaiverManagement } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { EnterpriseFullWidthBanner } from 'MainRoot/shared/enterpriseTier';

import AutoWaiverExclusionLogTable from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverExclusionLogTable';
import AutoWaiverModal from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/AutoWaiverModal';
import DeleteAutoWaiverModal from './DeleteAutoWaiverModal';
import ReachabilityStatus from 'MainRoot/componentDetails/ReachabilityStatus/ReachabilityStatus';

import {
  selectAutoWaiverDetails,
  selectAutoWaiverDetailsLoading,
  selectAutoWaiverDetailsError,
} from './autoWaiverDetailsSelectors';
import { selectAutoWaiverModalSlice } from './autoWaiverModalSelectors';
import { selectApplicableAutoWaivers } from 'MainRoot/OrgsAndPolicies/autoWaiversSelectors';
import { selectCurrentRouteName, selectRouterSlice } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import { actions } from './autoWaiverDetailsSlice';
import { actions as autoWaiverActions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/autoWaiverModalSlice';
import { actions as applicableAutoWaiversActions } from 'MainRoot/OrgsAndPolicies/autoWaiversConfiguration/applicableAutoWaiversSlice';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';

import { deriveEditRoute } from 'MainRoot/OrgsAndPolicies/utility/util';
import { faSitemap, faTerminal } from '@fortawesome/pro-solid-svg-icons';
import moment from 'moment';
import './_autoWaiverDetails.scss';

export default function AutoWaiverDetails() {
  const dispatch = useDispatch();
  const uiRouterState = useRouterState();
  const isLoading = useSelector(selectAutoWaiverDetailsLoading);
  const loadError = useSelector(selectAutoWaiverDetailsError);
  const details = useSelector(selectAutoWaiverDetails);
  const selectedOwner = useSelector(selectSelectedOwner);
  const routerCurrentParams = useSelector(selectRouterCurrentParams);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const isWaiverDetailsPage = currentRouteName === 'waiver.details' || currentRouteName === FIREWALL_WAIVER_DETAILS;
  const hasAutoWaiverManagement = useSelector(selectHasAutoWaiverManagement);

  // Check if this is a preview waiver (enterprise preview mode)
  const isPreviewWaiver = routerCurrentParams?.autoWaiverId === 'preview-auto-waiver';

  // Mock data for preview waiver
  const mockPreviewWaiver = useMemo(() => ({
    createTime: Date.now(),
    pathForward: true,
    reachability: true,
    threatLevel: 3,
    scopesOperatorAny: true,
    ownerId: routerCurrentParams?.autoWaiverOwnerId || 'ROOT_ORGANIZATION_ID',
    ownerName: 'Organization Name',
    ownerType: 'organization',
    publicId: routerCurrentParams?.autoWaiverOwnerId || 'ROOT_ORGANIZATION_ID',
    autoPolicyWaiverId: 'preview-auto-waiver',
  }), [routerCurrentParams]);

  // Use mock data if preview waiver, otherwise use real details
  const waiverDetails = isPreviewWaiver ? mockPreviewWaiver : details;

  const {
    createTime,
    pathForward,
    reachability,
    threatLevel,
    scopesOperatorAny,
    ownerId,
    ownerName,
    ownerType,
    publicId,
    autoPolicyWaiverId,
  } = waiverDetails || {};
  const formatDate = (date) => moment(date).format('MMMM D, YYYY');

  const applicableAutoWaivers = useSelector(selectApplicableAutoWaivers);
  const { isDeleteModalOpen, deleteSubmitMask } = applicableAutoWaivers || {};

  const router = useSelector(selectRouterSlice);
  const { to, params } = deriveEditRoute(router, 'auto-waivers-config');

  const { submitMaskState } = useSelector(selectAutoWaiverModalSlice);

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

  const isInherited =
    selectedOwner &&
    routerCurrentParams &&
    selectedOwner.id &&
    routerCurrentParams.autoWaiverOwnerId &&
    selectedOwner.id !== routerCurrentParams.autoWaiverOwnerId;

  const handleEditClick = () => {
    const autoWaiverDetails = {
      threatLevel,
      autoPolicyWaiverId,
      ownerId,
      reachability,
      pathForward,
      scope: scopesOperatorAny ? 'any' : 'all',
    };
    if (!isInherited) {
      dispatch(autoWaiverActions.openEditModal(autoWaiverDetails));
    }
  };

  const handleDeleteClick = () => {
    if (!isInherited) {
      dispatch(applicableAutoWaiversActions.openDeleteModal(autoPolicyWaiverId));
    }
  };

  useEffect(() => {
    // Skip loading for preview waiver
    if (!isPreviewWaiver) {
      loadAutoWaiverDetails();
    }
  }, []);

  useEffect(() => {
    if (deleteSubmitMask) {
      dispatch(stateGo(to, params));
    }
  }, [deleteSubmitMask]);

  useEffect(() => {
    if (submitMaskState) {
      loadAutoWaiverDetails();
    }
  }, [submitMaskState]);

  return (
    <>
      <NxTile
        className={`nx-viewport-sized ${!hasAutoWaiverManagement ? 'iq-banner-flush-top' : ''}`}
        id="auto-waiver-details"
        data-testid="auto-waiver-details"
      >
        {!hasAutoWaiverManagement && (
          <EnterpriseFullWidthBanner
            title="Auto-Waivers"
            description="Automatically apply waivers to low-risk, non-reachable or known issues so teams can stay unblocked."
          />
        )}
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Auto-Waiver Details</NxH2>
          </NxTile.HeaderTitle>
          {!isWaiverDetailsPage && !isPreviewWaiver && hasAutoWaiverManagement && (
            <NxTile.HeaderActions>
              <NxTooltip title={isInherited ? 'Cannot edit an inherited auto-waiver' : ''}>
                <NxButton variant="tertiary" className={isInherited ? 'disabled' : ''} onClick={handleEditClick}>
                  Edit
                </NxButton>
              </NxTooltip>

              <NxTooltip title={isInherited ? 'Cannot delete an inherited auto-waiver' : ''}>
                <NxButton variant="primary" className={isInherited ? 'disabled' : ''} onClick={handleDeleteClick}>
                  Delete
                </NxButton>
              </NxTooltip>
            </NxTile.HeaderActions>
          )}
        </NxTile.Header>
        <NxLoadWrapper loading={!isPreviewWaiver && isLoading} error={!isPreviewWaiver && loadError} retryHandler={loadAutoWaiverDetails}>
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
      {!isWaiverDetailsPage && (
        <NxTile className="iq-exclusion-log-tile">
          <NxH2>Exclusion Log</NxH2>
          <NxH3>Violations excluded from this automation</NxH3>
          <AutoWaiverExclusionLogTable disableDelete={isInherited} />
        </NxTile>
      )}
      <AutoWaiverModal />
      {isDeleteModalOpen && <DeleteAutoWaiverModal />}
    </>
  );
}
