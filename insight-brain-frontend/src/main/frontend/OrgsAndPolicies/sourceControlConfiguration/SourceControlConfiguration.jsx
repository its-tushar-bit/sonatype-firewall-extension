/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useCallback, useRef } from 'react';
import {
  NxErrorAlert,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxSuccessAlert,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import {
  selectIsSourceControlForSourceTileSupported,
  selectIsGithubAppAuthenticationEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectLoadError } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectIsAccessTokenRequiredOnNode,
  selectIsLoading,
  selectSourceControlConfigurationSlice,
  selectShowGitHubAppSuccessModal,
  selectShowGitHubAppReplacedAlert,
  selectSourceControl,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { actions as gitHubAppActions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { selectIsModalOpen as selectIsGitHubAppRegistrationModalOpen } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSelectors';
import ResetSourceControlModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ResetSourceControlModal';
import UpdateSourceControlConfirmationModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/UpdateSourceControlConfirmationModal';
import GitHubAppRegistrationModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppRegistrationModal';
import GitHubAppSuccessModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/GitHubAppSuccessModal';
import RootSourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/RootSourceControlConfiguration';
import OrgSourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/OrgSourceControlConfiguration';
import AppSourceControlConfiguration from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/AppSourceControlConfiguration';
import {
  selectIsApplication,
  selectIsOrganization,
  selectIsRootOrganization,
  selectRouterCurrentParams,
  selectRouterState,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import SourceControlAutomatedPullRequestTable from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/SourceControlAutomatedPullRequestTable';
import { SOURCE_CONTROL_UNSUPPORTED_MESSAGE } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/utils';

const SourceControlConfiguration = () => {
  const dispatch = useDispatch();
  const isShowAccessTokenWarning = useSelector(selectIsAccessTokenRequiredOnNode);
  const isRootOrg = useSelector(selectIsRootOrganization);
  const isOrg = useSelector(selectIsOrganization);
  const isApp = useSelector(selectIsApplication);
  const { sourceControlMetrics, loadError } = useSelector(selectSourceControlConfigurationSlice);
  const loadSelectedOwnerError = useSelector(selectLoadError);
  const isSourceControlSupported = useSelector(selectIsSourceControlForSourceTileSupported);
  const owner = useSelector(selectSelectedOwner);
  const isLoading = useSelector(selectIsLoading);
  const showGitHubAppSuccessModal = useSelector(selectShowGitHubAppSuccessModal);
  const showGitHubAppReplacedAlert = useSelector(selectShowGitHubAppReplacedAlert);
  const sourceControl = useSelector(selectSourceControl);
  const routerParams = useSelector(selectRouterCurrentParams);
  const currentState = useSelector((state) => selectRouterState(state)?.name);
  const isGitHubAppSupported = useSelector(selectIsGithubAppAuthenticationEnabled);
  const isGitHubAppRegistrationModalOpen = useSelector(selectIsGitHubAppRegistrationModalOpen);

  // Read githubAppSuccess parameter from route
  const githubAppSuccess = routerParams?.githubAppSuccess === 'true';

  // Track whether we've already handled the githubAppSuccess parameter
  // Prevents duplicate modal shows on back navigation, URL manipulation, and Strict Mode double-renders
  const githubAppSuccessHandled = useRef(false);

  const doLoad = useCallback(() => {
    dispatch(actions.load());
  }, [dispatch]);

  useEffect(() => {
    doLoad();
  }, [doLoad]);

  // Reset the ref when user opens the GitHub App registration modal to start a new setup
  useEffect(() => {
    if (isGitHubAppRegistrationModalOpen) {
      githubAppSuccessHandled.current = false;
    }
  }, [isGitHubAppRegistrationModalOpen]);

  useEffect(() => {
    if (!isGitHubAppSupported) return;
    if (isLoading) return;
    if (showGitHubAppSuccessModal) return;
    if (!githubAppSuccess) return;
    if (githubAppSuccessHandled.current) return;

    // Mark as handled to prevent re-execution on back navigation, URL manipulation, or Strict Mode
    githubAppSuccessHandled.current = true;

    // Close the GitHubAppRegistrationModal if it's still open from the OAuth flow
    dispatch(gitHubAppActions.closeModal());

    dispatch(actions.showGitHubAppSuccessModal());
  }, [
    isGitHubAppSupported,
    isLoading,
    showGitHubAppSuccessModal,
    githubAppSuccess,
    currentState,
    dispatch,
    sourceControl,
  ]);

  const handleCloseGitHubAppSuccessModal = useCallback(() => {
    // Enable features that were previously disabled
    if (!sourceControl?.remediationPullRequestsEnabled?.value || !sourceControl?.manualPullRequestsEnabled?.value) {
      dispatch(actions.enableGitHubAppFeatures());
    }

    dispatch(actions.closeGitHubAppSuccessModal());
  }, [dispatch, sourceControl]);

  const handleCloseGitHubAppReplacedAlert = useCallback(() => {
    dispatch(actions.closeGitHubAppReplacedAlert());
  }, [dispatch]);

  return (
    <div id="source-control-editor">
      <NxPageTitle>
        <NxH1>Source Control Configuration</NxH1>
        {!isLoading && (
          <NxPageTitle.Description>
            Configures the integration with an external SCM for {owner.name}
          </NxPageTitle.Description>
        )}
      </NxPageTitle>
      {!isSourceControlSupported && !isLoading ? (
        <NxErrorAlert id="source-control-not-supported">{SOURCE_CONTROL_UNSUPPORTED_MESSAGE}</NxErrorAlert>
      ) : (
        <NxLoadWrapper loading={isLoading} retryHandler={doLoad} error={loadError || loadSelectedOwnerError}>
          <>
            {isShowAccessTokenWarning && (
              <NxWarningAlert id="source-control-token-warning">Access Token must be configured</NxWarningAlert>
            )}
            {showGitHubAppReplacedAlert && (
              <NxSuccessAlert id="github-app-replaced-alert" onClose={handleCloseGitHubAppReplacedAlert}>
                The GitHub App was replaced successfully. The current configuration was overwritten
              </NxSuccessAlert>
            )}
            <NxTile className="iq-source-control-configuration-tile">
              {isRootOrg && <RootSourceControlConfiguration />}
              {isOrg && !isRootOrg && <OrgSourceControlConfiguration />}
              {isApp && !isRootOrg && <AppSourceControlConfiguration />}
            </NxTile>
            {isApp && (
              <NxTile>
                <SourceControlAutomatedPullRequestTable automatedPullRequests={sourceControlMetrics?.results || []} />
              </NxTile>
            )}
            <ResetSourceControlModal />
            <UpdateSourceControlConfirmationModal />
            {isGitHubAppSupported && (
              <>
                <GitHubAppRegistrationModal />
                <GitHubAppSuccessModal
                  isOpen={showGitHubAppSuccessModal}
                  onClose={handleCloseGitHubAppSuccessModal}
                  autoEnabledGoldenPRs={!sourceControl?.remediationPullRequestsEnabled?.value}
                  autoEnabledManualPRs={!sourceControl?.manualPullRequestsEnabled?.value}
                  serverId={sourceControl?.githubApp?.value?.name}
                  organizationName={sourceControl?.githubApp?.value?.accountName}
                  submitBtnText={isRootOrg ? (sourceControl?.id ? 'Update' : 'Create') : 'Update'}
                />
              </>
            )}
          </>
        </NxLoadWrapper>
      )}
    </div>
  );
};

export default SourceControlConfiguration;
