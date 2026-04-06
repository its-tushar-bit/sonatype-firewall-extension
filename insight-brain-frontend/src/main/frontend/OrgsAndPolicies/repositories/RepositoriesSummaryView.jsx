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
import {
  selectLoadError as selectLoadSelectedOwnerError,
  selectSelectedOwner,
  selectShowLimitedFirewallAccessAlert,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

export default function RepositoriesSummaryView() {
  const dispatch = useDispatch();
  const loading = useSelector(selectLoading);
  const loadError = useSelector(selectLoadError);
  const loadSelectedOwnerError = useSelector(selectLoadSelectedOwnerError);
  const owner = useSelector(selectSelectedOwner);
  const showLimitedFirewallAccessAlert = useSelector(selectShowLimitedFirewallAccessAlert);

  const doLoad = () => dispatch(actions.loadOwnerSummary());

  useEffect(() => {
    dispatch(actions.checkEditIqPermission());
    if (!owner) {
      doLoad();
    }
  }, []);

  function repositoriesSummary() {
    return (
      <NxLoadWrapper loading={loading} error={loadError || loadSelectedOwnerError} retryHandler={doLoad}>
        <div id="repository-page">
          <header>
            <NxPageTitle id="repositories-summary" className="iq-page-title">
              <NxH1>
                <span>{owner.name}</span>
              </NxH1>
            </NxPageTitle>
            <RepositoriesPills />
          </header>

          <div
            className="iq-tile-scroll-container iq-tile-scroll-container--owner-summary-view nx-viewport-sized__scrollable"
            id="repositories-summary-sections"
          >
            <div id="scrollable-content">
              <RepositoriesConfigurationTile key="container-view" />
              <PoliciesTile />
              <NamespaceConfusionProtectionTile />
              <AccessTile />
            </div>
          </div>
        </div>
      </NxLoadWrapper>
    );
  }

  return showLimitedFirewallAccessAlert ? <LimitedFirewallAccessAlert /> : repositoriesSummary();
}
