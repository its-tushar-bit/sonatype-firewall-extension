/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import {
  NxDropdown,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxTooltip,
  useToggle,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import {
  selectIsRootOrganization,
  selectIsApplication,
  selectIsOrganization,
} from 'MainRoot/reduxUiRouter/routerSelectors';

import { selectSelectedOwner } from '../orgsAndPoliciesSelectors';
import {
  faTrash,
  faPaste,
  faPen,
  faDownload,
  faArrowsAlt,
  faUser,
  faHammer,
  faHistory,
  faExternalLinkAlt,
  faUpload,
  faRandom,
} from '@fortawesome/free-solid-svg-icons';

import { actions as deleteOwnerActions } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSlice';
import { actions as contactActions } from 'MainRoot/OrgsAndPolicies/selectContactModal/selectContactModalSlice';
import { actions as grandfatheringActions } from 'MainRoot/OrgsAndPolicies/grandfatheringModal/grandfatheringSlice';
import { actions as revokeGrandfatheringActions } from 'MainRoot/OrgsAndPolicies/revokeGrandfatheringModal/revokeGrandfatheringSlice';
import { actions as moveApplicationActions } from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSlice';
import { actions as evaluateApplicationActions } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSlice';
import copyIdToClipboardAction from 'MainRoot/OrgsAndPolicies/copyIdToClipboardToast/copyIdToClipboardSlice';
import { actions as changeApplicationIdActions } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSlice';
import { actions as importPoliciesActions } from 'MainRoot/OrgsAndPolicies/importPoliciesModal/importPoliciesSlice';
import { actions as ownerModalActions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { actions as actionDropdownActions } from './actionDropdownSlice';
import { selectActionDropdownSlice } from './actionDropdownSelectors';
import { selectDashboardStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import {
  selectIsEvaluateApplicationAvailable,
  selectIsGrandfatheringSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectCalculatedEnabled } from '../policyViolationGrandfatheringSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const getDisabledGrandfatherTooltipMessage = (support, enabled) => {
  if (!support) {
    return 'Policy Violation Grandfathering is not supported by your license';
  }
  if (!enabled) {
    return 'Grandfathering is not enabled for this application.';
  }
  return '';
};

const getDisabledEvaluateTooltipMessage = (permission, available) => {
  if (!available) {
    return 'Evaluate application is not supported by your license.';
  }
  if (!permission) {
    return 'Insufficient permissions to evaluate application';
  }
  return '';
};

const ActionDropdown = () => {
  const dispatch = useDispatch();
  const [isOpen, onToggleCollapse] = useToggle(false);
  const isRootOrg = useSelector(selectIsRootOrganization);
  const isOrg = useSelector(selectIsOrganization);
  const isApp = useSelector(selectIsApplication);
  const isGrandfatheringSupported = useSelector(selectIsGrandfatheringSupported);
  const isGrandfatheringEnabled = useSelector(selectCalculatedEnabled);
  const isEvaluateApplicationAvailable = useSelector(selectIsEvaluateApplicationAvailable);
  const { name: ownerName } = useSelector(selectSelectedOwner);
  const { applicationSummary, hasPermissionToChangeAppId, hasPermissionToEvaluateApp } = useSelector(
    selectActionDropdownSlice
  );
  const stages = useSelector(selectDashboardStageTypes);
  const grandfatheringDisabled = !isGrandfatheringSupported || !isGrandfatheringEnabled;
  const GrandfatheringTooltip = grandfatheringDisabled ? NxTooltip : NxOverflowTooltip;

  const uiRouterState = useRouterState();

  const openReport = (stageTypeId) => {
    if (applicationSummary.policyEvaluations[stageTypeId]) {
      window.open(
        uiRouterState.href('applicationReport.policy', {
          publicId: applicationSummary.publicId,
          scanId: applicationSummary.policyEvaluations[stageTypeId].scanId,
        }),
        '_blank'
      );
    }
  };

  useEffect(() => {
    if (isApp) {
      dispatch(actionDropdownActions.loadApplicationSummary());
      dispatch(actionDropdownActions.loadPermissions());
    }
  }, [isApp]);

  const handleChangeAppID = () => {
    if (hasPermissionToChangeAppId) {
      dispatch(changeApplicationIdActions.openModal());
    }
  };

  const handleGrandfather = () => {
    if (!grandfatheringDisabled) {
      dispatch(grandfatheringActions.openModal());
    }
  };

  const handleRevokeAllGrandfathered = () => {
    if (isGrandfatheringSupported) {
      dispatch(revokeGrandfatheringActions.openModal());
    }
  };

  const handleEvaluateFile = () => {
    if (hasPermissionToEvaluateApp && isEvaluateApplicationAvailable) {
      dispatch(evaluateApplicationActions.openEvaluateAppModal());
    }
  };

  const dropdownOptions = () => {
    return (
      <>
        <button
          id={isApp ? 'copy-app-id-link' : 'copy-org-id-link'}
          onClick={() => dispatch(copyIdToClipboardAction())}
          className="nx-dropdown-button"
        >
          <NxFontAwesomeIcon icon={faPaste} />
          <span>{isApp ? 'App' : 'Org'} ID to Clipboard</span>
        </button>

        {isApp && (
          <button
            id="select-contact-link"
            onClick={() => dispatch(contactActions.openContactModal())}
            className="nx-dropdown-button"
          >
            <NxFontAwesomeIcon icon={faUser} />
            <span>Select Contact</span>
          </button>
        )}

        <NxOverflowTooltip>
          <button
            id="app-org-link"
            onClick={() => dispatch(ownerModalActions.openEditModal())}
            className="nx-dropdown-button"
          >
            <NxFontAwesomeIcon icon={faPen} />
            <span>Edit {isApp ? 'App' : 'Org'} Name / Icon</span>
          </button>
        </NxOverflowTooltip>

        {isApp && (
          <NxTooltip title={!hasPermissionToChangeAppId ? 'Insufficient permissions to change App ID' : ''}>
            <button
              id="change-app-id-link"
              onClick={handleChangeAppID}
              className={`nx-dropdown-button ${!hasPermissionToChangeAppId ? 'disabled' : ''}`}
            >
              <NxFontAwesomeIcon icon={faRandom} />
              <span>Change App ID</span>
            </button>
          </NxTooltip>
        )}

        {isApp && (
          <NxOverflowTooltip>
            <button
              id="app-move-link"
              onClick={() => dispatch(moveApplicationActions.openMoveAppModal())}
              className="nx-dropdown-button"
            >
              <NxFontAwesomeIcon icon={faArrowsAlt} />
              <span>Move {ownerName}</span>
            </button>
          </NxOverflowTooltip>
        )}

        {isOrg && (
          <button
            id="import-policies-link"
            onClick={() => dispatch(importPoliciesActions.openModal())}
            className="nx-dropdown-button"
          >
            <NxFontAwesomeIcon icon={faDownload} />
            <span>Import Policies</span>
          </button>
        )}

        {!isRootOrg && (
          <NxOverflowTooltip>
            <button
              id="delete-owner-link"
              onClick={() => dispatch(deleteOwnerActions.openModal())}
              className="nx-dropdown-button"
            >
              <NxFontAwesomeIcon icon={faTrash} />
              <span>Delete {ownerName}</span>
            </button>
          </NxOverflowTooltip>
        )}

        {isApp && (
          <>
            <NxDropdown.Divider />
            <GrandfatheringTooltip
              title={
                grandfatheringDisabled
                  ? getDisabledGrandfatherTooltipMessage(isGrandfatheringSupported, isGrandfatheringEnabled)
                  : ''
              }
            >
              <button
                id="policy-violation-grandfather-link"
                onClick={handleGrandfather}
                className={`nx-dropdown-button ${grandfatheringDisabled ? 'disabled' : ''}`}
              >
                <NxFontAwesomeIcon icon={faHammer} className="fa-flip-horizontal" />
                <span>Grandfather {ownerName}</span>
              </button>
            </GrandfatheringTooltip>

            <NxTooltip
              title={
                !isGrandfatheringSupported ? 'Policy Violation Grandfathering is not supported by your license' : ''
              }
            >
              <button
                id="revoke-policy-violation-grandfathering-link"
                onClick={handleRevokeAllGrandfathered}
                className={`nx-dropdown-button ${!isGrandfatheringSupported ? 'disabled' : ''}`}
              >
                <NxFontAwesomeIcon icon={faHistory} />
                <span>Revoke All Grandfathered</span>
              </button>
            </NxTooltip>

            <NxDropdown.Divider />

            <NxTooltip
              title={getDisabledEvaluateTooltipMessage(hasPermissionToEvaluateApp, isEvaluateApplicationAvailable)}
            >
              <button
                id="eval-file-link"
                onClick={handleEvaluateFile}
                className={`nx-dropdown-button ${
                  !hasPermissionToEvaluateApp || !isEvaluateApplicationAvailable ? 'disabled' : ''
                }`}
              >
                <NxFontAwesomeIcon icon={faUpload} />
                <span>Evaluate a File</span>
              </button>
            </NxTooltip>

            {stages?.map(({ stageTypeId, shortName }) => {
              const isDisabled = !applicationSummary?.policyEvaluations?.[stageTypeId];
              return (
                <button
                  key={stageTypeId}
                  id="app-report-link"
                  onClick={() => openReport(stageTypeId)}
                  className={`nx-dropdown-link ${isDisabled ? 'disabled' : ''}`}
                  disabled={isDisabled}
                >
                  <NxFontAwesomeIcon icon={faExternalLinkAlt} />
                  <span>View {shortName.toLowerCase()} report</span>
                </button>
              );
            })}
          </>
        )}
      </>
    );
  };

  return (
    <NxDropdown className="nx-dropdown--short" label="Actions" isOpen={isOpen} onToggleCollapse={onToggleCollapse}>
      {dropdownOptions()}
    </NxDropdown>
  );
};

export default ActionDropdown;
