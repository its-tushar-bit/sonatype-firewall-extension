/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import classNames from 'classnames';
import {
  NxDropdown,
  NxFontAwesomeIcon,
  NxOverflowTooltip,
  NxTextLink,
  NxTooltip,
  useToggle,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';

import {
  selectIsRootOrganization,
  selectIsApplication,
  selectIsOrganization,
  selectIsRepositoriesRelated,
  selectIsRepositoryManager,
  selectIsRepository,
  selectIsSbomManager,
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
import { actions as legacyViolationModalActions } from 'MainRoot/OrgsAndPolicies/legacyViolationModal/legacyViolationModalSlice';
import { actions as revokeLegacyViolationModalActions } from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/revokeLegacyViolationModalSlice';
import { actions as moveOwnerActions } from 'MainRoot/OrgsAndPolicies/moveOwner/moveOwnerSlice';
import { actions as evaluateApplicationActions } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSlice';
import copyIdToClipboardAction from 'MainRoot/OrgsAndPolicies/copyIdToClipboardToast/copyIdToClipboardSlice';
import { actions as changeApplicationIdActions } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSlice';
import { actions as importPoliciesActions } from 'MainRoot/OrgsAndPolicies/importPoliciesModal/importPoliciesSlice';
import { actions as ownerModalActions } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSlice';
import { actions as repositoryiesConfigurationActions } from 'MainRoot/OrgsAndPolicies/repositories/repositoriesConfigurationSlice';
import { actions as actionDropdownActions } from './actionDropdownSlice';
import { selectActionDropdownSlice } from './actionDropdownSelectors';
import { selectDashboardStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import {
  selectIsEvaluateApplicationAvailable,
  selectIsLegacyViolationSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectCalculatedEnabled } from '../legacyViolationSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

const getDisabledLegacyViolationsTooltipMessage = (support, enabled) => {
  if (!support) {
    return 'Legacy Violations are not supported by your license';
  }
  if (!enabled) {
    return 'Legacy Violations are not enabled for this application.';
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
  const isRepositories = useSelector(selectIsRepositoriesRelated);
  const isRepositoryManager = useSelector(selectIsRepositoryManager);
  const isRepository = useSelector(selectIsRepository);
  const isLegacyViolationSupported = useSelector(selectIsLegacyViolationSupported);
  const isLegacyViolationEnabled = useSelector(selectCalculatedEnabled);
  const isEvaluateApplicationAvailable = useSelector(selectIsEvaluateApplicationAvailable);
  const owner = useSelector(selectSelectedOwner);
  const { applicationSummary, hasPermissionToChangeAppId, hasPermissionToEvaluateApp } = useSelector(
    selectActionDropdownSlice
  );
  const stages = useSelector(selectDashboardStageTypes);
  const legacyViolationDisabled = !isLegacyViolationSupported || !isLegacyViolationEnabled;
  const LegacyViolationTooltip = legacyViolationDisabled ? NxTooltip : NxOverflowTooltip;
  const isSbomManager = useSelector(selectIsSbomManager);
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
  }, [owner]);

  const handleChangeAppID = () => {
    if (hasPermissionToChangeAppId) {
      dispatch(changeApplicationIdActions.openModal());
    }
  };

  const handleGrantLegacyViolationStatus = () => {
    if (!legacyViolationDisabled) {
      dispatch(legacyViolationModalActions.openModal());
    }
  };

  const handleRevokeLegacyViolationStatus = () => {
    if (isLegacyViolationSupported) {
      dispatch(revokeLegacyViolationModalActions.openModal());
    }
  };

  const handleEvaluateFile = () => {
    if (hasPermissionToEvaluateApp && isEvaluateApplicationAvailable) {
      dispatch(evaluateApplicationActions.openEvaluateAppModal());
    }
  };

  const getShortOwnerName = () => {
    if (isApp) {
      return 'App';
    } else if (isOrg) {
      return 'Org';
    } else if (isRepositoryManager) {
      return 'Repository Manager';
    }
  };

  const dropdownOptions = () => {
    if (isRepository) {
      const repositoryResultUrl = `${uiRouterState.href('repository-report', {
        repositoryId: owner.id,
      })}?hideBackButton=true`;
      return (
        <>
          <NxOverflowTooltip>
            <NxTextLink className="nx-dropdown-button nx-truncate-ellipsis" external href={repositoryResultUrl}>
              View repository Results
            </NxTextLink>
          </NxOverflowTooltip>
        </>
      );
    }
    return (
      <>
        <button
          id={isApp ? 'copy-app-id-link' : 'copy-org-id-link'}
          onClick={() => dispatch(copyIdToClipboardAction())}
          className="nx-dropdown-button"
        >
          <NxFontAwesomeIcon icon={faPaste} />
          <span>{getShortOwnerName()} ID to Clipboard</span>
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
            onClick={() => dispatch(ownerModalActions.openEditModal(owner))}
            className="nx-dropdown-button"
          >
            <NxFontAwesomeIcon icon={faPen} />
            <span>Edit {getShortOwnerName()} Name / Icon</span>
          </button>
        </NxOverflowTooltip>

        {isApp && !isSbomManager && (
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

        {!isRootOrg && !isRepositories && (
          <NxOverflowTooltip>
            <button
              id="owner-move-link"
              onClick={() => dispatch(moveOwnerActions.openMoveOwnerModal())}
              className="nx-dropdown-button"
            >
              <NxFontAwesomeIcon icon={faArrowsAlt} />
              <span>Move {owner.name}</span>
            </button>
          </NxOverflowTooltip>
        )}

        {isOrg && !isSbomManager && (
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
              <span>Delete {owner.name}</span>
            </button>
          </NxOverflowTooltip>
        )}

        {isApp && !isSbomManager && (
          <>
            <NxDropdown.Divider />
            <LegacyViolationTooltip
              title={
                legacyViolationDisabled
                  ? getDisabledLegacyViolationsTooltipMessage(isLegacyViolationSupported, isLegacyViolationEnabled)
                  : ''
              }
            >
              <button
                id="legacy-violation-link"
                onClick={handleGrantLegacyViolationStatus}
                className={`nx-dropdown-button ${legacyViolationDisabled ? 'disabled' : ''}`}
              >
                <NxFontAwesomeIcon icon={faHammer} className="fa-flip-horizontal" />
                <span>Legacy existing violations</span>
              </button>
            </LegacyViolationTooltip>

            <NxTooltip title={!isLegacyViolationSupported ? 'Legacy Violations are not supported by your license' : ''}>
              <button
                id="revoke-legacy-violation-link"
                onClick={handleRevokeLegacyViolationStatus}
                className={`nx-dropdown-button ${!isLegacyViolationSupported ? 'disabled' : ''}`}
              >
                <NxFontAwesomeIcon icon={faHistory} />
                <span>Revoke legacy status</span>
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

  const dropDownClasses = classNames('nx-dropdown--short', { 'repository-dropdown': isRepository });

  return (
    <NxDropdown
      id="iq-owner-actions-dropdown"
      className={dropDownClasses}
      label="Actions"
      isOpen={isOpen}
      onToggleCollapse={onToggleCollapse}
    >
      {dropdownOptions()}
    </NxDropdown>
  );
};

export default ActionDropdown;
