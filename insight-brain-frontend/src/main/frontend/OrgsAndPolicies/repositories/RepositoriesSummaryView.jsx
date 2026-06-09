/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { NxPageTitle, NxH1, NxLoadWrapper } from '@sonatype/react-shared-components';
import RepositoriesConfigurationTile from './RepositoriesConfigurationTile';
import AccessTile from 'MainRoot/react/accessTile/AccessTile';
import NamespaceConfusionProtectionTile from './namespaceConfusionProtectionTile/NamespaceConfusionProtectionTile';
import PoliciesTile from 'MainRoot/OrgsAndPolicies/ownerSummary/policiesTile/PoliciesTile';
import { actions } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';
import { useDispatch, useSelector } from 'react-redux';
import RepositoriesPills from 'MainRoot/OrgsAndPolicies/repositories/RepositoriesPills';
import { selectLoadError, selectLoading } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import LimitedFirewallAccessAlert from 'MainRoot/react/LimitedFirewallAccessAlert';
import WaiverExpirationNotificationTile from 'MainRoot/OrgsAndPolicies/ownerSummary/waiverExpirationNotificationTile/WaiverExpirationNotificationTile';
import {
  selectLoadError as selectLoadSelectedOwnerError,
  selectSelectedOwner,
  selectShowLimitedFirewallAccessAlert,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectIsVirtualRepositoryContainer, selectIsFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export default function RepositoriesSummaryView() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const owner = useSelector(selectSelectedOwner);
  const showLimitedFirewallAccessAlert = useSelector(selectShowLimitedFirewallAccessAlert);
  const isVirtualRepositoryContainer = useSelector(selectIsVirtualRepositoryContainer);
  const isFirewall = useSelector(selectIsFirewall);

  const doLoad = () => dispatch(actions.loadOwnerSummary());

  useEffect(() => {
    if (!owner) {
      doLoad();
    }
  }, []);

  function repositoriesSummary() {
    const pageTitle = isVirtualRepositoryContainer ? 'Virtual Repository Managers' : owner.name;
    return (
      <NxLoadWrapper loading={loading} error={loadError || loadSelectedOwnerError} retryHandler={doLoad}>
        <div id="repository-page">
          <header>
            <NxPageTitle id="repositories-summary" className="iq-page-title">
              <NxH1>
                <span>{pageTitle}</span>
              </NxH1>
            </NxPageTitle>
            <RepositoriesPills />
          </header>

          <div
            className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
            id="repositories-summary-sections"
          >
            <div id="scrollable-content">
              <RepositoriesConfigurationTile
                key="container-view"
                virtualOnly={isVirtualRepositoryContainer}
                showHostedRepoLink={false}
              />
              <PoliciesTile />
              <NamespaceConfusionProtectionTile />
              {isFirewall && <WaiverExpirationNotificationTile />}
              <AccessTile />
            </div>
          </div>
        </div>
      </NxLoadWrapper>
    );
  }

  return showLimitedFirewallAccessAlert ? <LimitedFirewallAccessAlert /> : repositoriesSummary();
}
