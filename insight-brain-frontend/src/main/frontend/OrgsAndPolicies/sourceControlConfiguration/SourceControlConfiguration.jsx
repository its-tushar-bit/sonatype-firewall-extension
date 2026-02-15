/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect, useCallback } from 'react';
import {
  NxErrorAlert,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
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
  selectSourceControl,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
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
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
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
  const sourceControl = useSelector(selectSourceControl);
  const routerParams = useSelector(selectRouterCurrentParams);
  const currentState = useSelector((state) => selectRouterState(state)?.name);
  const isGitHubAppSupported = useSelector(selectIsGithubAppAuthenticationEnabled);

  // Read githubAppSuccess parameter from route
  const githubAppSuccess = routerParams?.githubAppSuccess === 'true';

  const doLoad = useCallback(() => {
    dispatch(actions.load());
  }, [dispatch]);

  useEffect(() => {
    doLoad();
  }, [doLoad]);

  useEffect(() => {
    if (!isGitHubAppSupported) return;
    if (isLoading) return;
    if (showGitHubAppSuccessModal) return;
    if (!githubAppSuccess) return;

    dispatch(actions.showGitHubAppSuccessModal());

    dispatch(stateGo(currentState, { githubAppSuccess: null }, { location: 'replace' }));
  }, [isGitHubAppSupported, isLoading, showGitHubAppSuccessModal, githubAppSuccess, currentState, dispatch]);

  const handleCloseGitHubAppSuccessModal = useCallback(() => {
    // Enable features that were previously disabled
    // Use enableGitHubAppFeatures action that doesn't mark form dirty
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
              <NxWarningAlert id="source-control-token-warning">Access Token must be configured</NxWarningAlert>
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
