/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import {
  NxErrorAlert,
  NxH1,
  NxLoadWrapper,
  NxPageTitle,
  NxTile,
  NxWarningAlert,
} from '@sonatype/react-shared-components';
import { selectIsSourceControlForSourceTileSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsAccessTokenRequiredOnNode,
  selectIsLoading,
  selectSourceControlConfigurationSlice,
} from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSelectors';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/sourceControlConfigurationSlice';
import { selectSelectedOwnerId, selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import ResetSourceControlModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/ResetSourceControlModal';
import UpdateSourceControlConfirmationModal from 'MainRoot/OrgsAndPolicies/sourceControlConfiguration/UpdateSourceControlConfirmationModal';
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
  const isSourceControlSupported = useSelector(selectIsSourceControlForSourceTileSupported);
  const ownerId = useSelector(selectSelectedOwnerId);
  const ownerName = useSelector(selectSelectedOwnerName);
  const doLoad = () => dispatch(actions.loadSCMRootConfig());
  const isLoading = useSelector(selectIsLoading);

  useEffect(() => {
    if (isSourceControlSupported && ownerId) {
      doLoad();
    }
  }, [isSourceControlSupported, ownerId]);

  return (
    <div id="source-control-editor">
      <NxPageTitle>
        <NxH1>Source Control Configuration</NxH1>
        <NxPageTitle.Description>
          Configures the integration with an external SCM for {ownerName}
        </NxPageTitle.Description>
      </NxPageTitle>
      {!isSourceControlSupported ? (
        <NxErrorAlert id="source-control-not-supported">{SOURCE_CONTROL_UNSUPPORTED_MESSAGE}</NxErrorAlert>
      ) : (
        <NxLoadWrapper loading={isLoading} retryHandler={doLoad} error={loadError}>
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
          </>
        </NxLoadWrapper>
      )}
    </div>
  );
};

export default SourceControlConfiguration;
