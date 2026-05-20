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
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { selectIsSourceControlForSourceTileSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectSelectedOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectLoadError } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectHasPendingGitHubAppReturn,
  selectIsAccessTokenRequiredOnNode,
  selectIsLoading,
  selectSourceControlConfigurationSlice,
  selectShowGitHubAppSuccessModal,
  selectSourceControl,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { actions as gitHubAppActions } from 'MainRoot/configuration/githubApp/gitHubAppConfigurationSlice';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
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
  const hasPendingGitHubAppReturn = useSelector(selectHasPendingGitHubAppReturn);
  const showGitHubAppSuccessModal = useSelector(selectShowGitHubAppSuccessModal);
  const sourceControl = useSelector(selectSourceControl);
  const isGitHubAppRegistrationModalOpen = useSelector(selectIsGitHubAppRegistrationModalOpen);

  // Prevent duplicate modal shows on back navigation, URL manipulation, and Strict Mode double-renders.
  const githubAppReturnHandled = useRef(false);

  // Immediately redirect to manage page if returning from GitHub App OAuth flow
  // and the user originally came from the manage page. This avoids briefly showing
  // the Source Control Configuration page before redirecting.
  useEffect(() => {
    try {
      const returnToRaw = sessionStorage.getItem('githubAppReturnTo');
      if (returnToRaw) {
        const { returnTo } = JSON.parse(returnToRaw);
        const hash = window.location.hash || '';
        const hasGithubAppId = hash.includes('githubAppId=');
        if (returnTo === 'manage' && hasGithubAppId) {
          dispatch(stateGo('^.manage-github-apps'));
          return;
        }
      }
    } catch {
      // ignore parse errors
    }
  }, [dispatch]);

  const doLoad = useCallback(() => {
    dispatch(actions.load());
  }, [dispatch]);

  useEffect(() => {
    doLoad();
  }, [doLoad]);

  // Reset the ref when user opens the GitHub App registration modal to start a new setup
  useEffect(() => {
    if (isGitHubAppRegistrationModalOpen) {
      githubAppReturnHandled.current = false;
    }
  }, [isGitHubAppRegistrationModalOpen]);

  useEffect(() => {
    if (isLoading) return;
    if (showGitHubAppSuccessModal) return;
    if (!hasPendingGitHubAppReturn) return;
    if (githubAppReturnHandled.current) return;

    githubAppReturnHandled.current = true;

    // Close the GitHubAppRegistrationModal if it's still open from the OAuth flow
    dispatch(gitHubAppActions.closeModal());

    // If the user came from the manage-github-apps page, redirect back there.
    // Leave the sessionStorage item intact so the manage page can show a success toast.
    try {
      const returnToRaw = sessionStorage.getItem('githubAppReturnTo');
      if (returnToRaw) {
        const { returnTo } = JSON.parse(returnToRaw);
        if (returnTo === 'manage') {
          dispatch(stateGo('^.manage-github-apps'));
          return;
        }
        sessionStorage.removeItem('githubAppReturnTo');
      }
    } catch {
      sessionStorage.removeItem('githubAppReturnTo');
    }

    dispatch(actions.showGitHubAppSuccessModal());
  }, [isLoading, showGitHubAppSuccessModal, hasPendingGitHubAppReturn, dispatch]);

  const handleCloseGitHubAppSuccessModal = useCallback(() => {
    // Enable features that were previously disabled
    if (!sourceControl?.remediationPullRequestsEnabled?.value || !sourceControl?.manualPullRequestsEnabled?.value) {
      dispatch(actions.enableGitHubAppFeatures());
    }

    dispatch(actions.closeGitHubAppSuccessModal());
  }, [dispatch, sourceControl]);

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
              <NxWarningAlert id="source-control-token-warning" data-testid="source-control-token-warning">
                Authentication method must be configured
              </NxWarningAlert>
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
            <GitHubAppRegistrationModal />
            <GitHubAppSuccessModal
              isOpen={showGitHubAppSuccessModal}
              onClose={handleCloseGitHubAppSuccessModal}
              autoEnabledGoldenPRs={!sourceControl?.remediationPullRequestsEnabled?.value}
              autoEnabledManualPRs={!sourceControl?.manualPullRequestsEnabled?.value}
              serverId={sourceControl?.githubApps?.value?.name}
              organizationName={sourceControl?.githubApps?.value?.accountName}
              submitBtnText={isRootOrg ? (sourceControl?.id ? 'Update' : 'Create') : 'Update'}
            />
          </>
        </NxLoadWrapper>
      )}
    </div>
  );
};

export default SourceControlConfiguration;
