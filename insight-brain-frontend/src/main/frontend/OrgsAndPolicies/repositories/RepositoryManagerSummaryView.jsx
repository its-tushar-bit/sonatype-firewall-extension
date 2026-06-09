/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useSelector, useDispatch } from 'react-redux';
import { getAddIconUrl } from 'MainRoot/util/CLMLocation';
import { NxLoadWrapper, NxPageTitle, NxH1, NxH2, NxTile } from '@sonatype/react-shared-components';
import {
  selectLoading,
  selectLoadError,
  selectHasEditIqPermission,
  selectHasViewIqPermission,
} from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import LimitedFirewallAccessAlert from 'MainRoot/react/LimitedFirewallAccessAlert';
import {
  selectSelectedOwner,
  selectLoadError as selectLoadSelectedOwnerError,
  selectEntityId,
  selectShowLimitedFirewallAccessAlert,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';

import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import RepositoryManagerPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoryManagerPills';
import NamespaceConfusionProtectionTile from '../repositories/namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import RepositoriesConfigurationTile from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesConfigurationTile';
import ActionDropdown from 'MainRoot/OrgsAndPolicies/actionDropdown/ActionDropdown';
import DeleteOwnerModal from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/DeleteOwnerModal';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import FirewallProxyConfigurationPage from 'MainRoot/firewall/iqProxy/FirewallProxyConfigurationPage';
import { selectIsVirtualRepositoryManager } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import WaiverExpirationNotificationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/waiverExpirationNotificationTile/WaiverExpirationNotificationTile';
import { selectIsFirewall, selectIsSbomManager } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectIsHostedRepositoryEvaluationEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';

export default function RepositoryManagerSummaryView() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const entityId = useSelector(selectEntityId);
  const owner = useSelector(selectSelectedOwner);
  const showLimitedFirewallAccessAlert = useSelector(selectShowLimitedFirewallAccessAlert);
  const isVirtualRepositoryManager = useSelector(selectIsVirtualRepositoryManager);
  const hasEditIqPermission = useSelector(selectHasEditIqPermission);
  const hasViewIqPermission = useSelector(selectHasViewIqPermission);
  const isFirewall = useSelector(selectIsFirewall);
  const isSbomManager = useSelector(selectIsSbomManager);
  const isHostedRepositoryEvaluationEnabled = useSelector(selectIsHostedRepositoryEvaluationEnabled);

  const doLoad = () => dispatch(actions.loadOwnerSummary());
  const getIconUrl = () => getAddIconUrl('repository_manager', owner.id) + `?${Math.random()}`;

  useEffect(() => {
    dispatch(actions.checkEditIqPermission());
    if (entityId) {
      doLoad();
    }
  }, [entityId]);

  function repositoryManagerSummary() {
    return (
      <NxLoadWrapper loading={loading} error={loadError || loadSelectedOwnerError} retryHandler={doLoad}>
        <div id="repository-page">
          <header>
            <NxPageTitle id="repositories-summary" className="iq-page-title">
              <NxH1>
                <span className="nx-icon">
                  <img src={getIconUrl()} />
                </span>
                <span>{owner.name}</span>
              </NxH1>
              <div className="nx-btn-bar">
                <ActionDropdown />
              </div>
            </NxPageTitle>
            <RepositoryManagerPills />
          </header>

          <div
            className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
            id="repositories-summary-sections"
          >
            <div id="scrollable-content">
              <RepositoriesConfigurationTile
                key={`manager-view-${entityId}`}
                showHostedRepoLink={!isFirewall && !isSbomManager && isHostedRepositoryEvaluationEnabled}
              />
              <PoliciesTile />
              <NamespaceConfusionProtectionTile sortFilterSectionValues={`repository-manager_${entityId}`} />
              {isFirewall && <WaiverExpirationNotificationTile />}
              <AccessTile />
              {isVirtualRepositoryManager && hasViewIqPermission && hasEditIqPermission && (
                <NxTile id="iq-proxy-repo-pill-configuration" data-testid="iq-proxy-repo-tile">
                  <NxTile.Header>
                    <NxTile.Headings>
                      <NxTile.HeaderTitle>
                        <NxH2>IQ proxy</NxH2>
                      </NxTile.HeaderTitle>
                    </NxTile.Headings>
                  </NxTile.Header>
                  <NxTile.Content>
                    <FirewallProxyConfigurationPage embedded />
                  </NxTile.Content>
                </NxTile>
              )}
            </div>
          </div>
        </div>
        <DeleteOwnerModal />
      </NxLoadWrapper>
    );
  }

  return showLimitedFirewallAccessAlert ? <LimitedFirewallAccessAlert /> : repositoryManagerSummary();
}
