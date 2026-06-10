/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxH1,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
  NxPageMain,
  NxP,
  NxErrorAlert,
} from '@sonatype/react-shared-components';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterState as selectRouterCurrentState } from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectComponentExistingCount,
  selectComponentRequestedCount,
  selectContainerRequestedCount,
  selectContainerExistingCount,
} from './firewallWaiverRequestsSelectors';
import { actions } from './firewallWaiverRequestsSlice';
import { actions as containerImageWaiverActions } from './containerImageWaiversSlice';
import { loadWaiverResults } from 'MainRoot/dashboard/results/dashboardResultsActions';
import FirewallRequestedWaiversTable from './FirewallRequestedWaiversTable';
import DashboardWaivers from 'MainRoot/dashboard/results/waivers/DashboardWaivers';
import ContainerImageWaiversTable from './ContainerImageWaiversTable';
import LimitedFirewallAccessAlert from 'MainRoot/react/LimitedFirewallAccessAlert';
import { selectShowLimitedFirewallAccessAlert } from 'MainRoot/firewall/firewallSelectors';

const COMPONENTS_TOP_TAB = 0;
const CONTAINERS_TOP_TAB = 1;
const EXISTING_SUB_TAB = 0;
const REQUESTED_SUB_TAB = 1;

export default function FirewallWaiversPage() {
  const dispatch = useDispatch();
  const currentState = useSelector(selectRouterCurrentState);
  const stateName = currentState?.name || '';
  const componentExistingCount = useSelector(selectComponentExistingCount);
  const componentRequestedCount = useSelector(selectComponentRequestedCount);
  const containerExistingCount = useSelector(selectContainerExistingCount);
  const containerRequestedCount = useSelector(selectContainerRequestedCount);
  const showLimitedFirewallAccessAlert = useSelector(selectShowLimitedFirewallAccessAlert);

  const isContainersTab = stateName.startsWith('firewall.waivers.containers');
  const activeTopTab = isContainersTab ? CONTAINERS_TOP_TAB : COMPONENTS_TOP_TAB;
  const isRequestedSubTab = stateName.includes('.requested');
  const activeSubTab = isRequestedSubTab ? REQUESTED_SUB_TAB : EXISTING_SUB_TAB;

  useEffect(() => {
    dispatch(actions.loadWaiverRequests());
    if (isContainersTab) {
      dispatch(containerImageWaiverActions.loadContainerImageWaivers());
    } else {
      dispatch(loadWaiverResults());
    }
  }, [isContainersTab]);

  const limitedAccessAlert = (
    <NxErrorAlert>An error occurred loading data. Don&rsquo;t have required access to access waivers.</NxErrorAlert>
  );

  const onTopTabSelect = (index) => {
    if (index === CONTAINERS_TOP_TAB) {
      dispatch(stateGo('firewall.waivers.containers'));
    } else {
      dispatch(stateGo('firewall.waivers.components'));
    }
  };

  const onSubTabSelect = (index) => {
    if (isContainersTab) {
      dispatch(
        stateGo(
          index === REQUESTED_SUB_TAB ? 'firewall.waivers.containers.requested' : 'firewall.waivers.containers.approved'
        )
      );
    } else {
      dispatch(
        stateGo(
          index === REQUESTED_SUB_TAB ? 'firewall.waivers.components.requested' : 'firewall.waivers.components.approved'
        )
      );
    }
  };

  return (
    <NxPageMain className="iq-firewall-waivers-page-main">
      <div className="iq-firewall-waivers-page">
        <NxH1>Waivers</NxH1>
        <NxP>View approved policy exceptions for components and containers across your organization.</NxP>
        {showLimitedFirewallAccessAlert && <LimitedFirewallAccessAlert />}
        <NxTabs activeTab={activeTopTab} onTabSelect={onTopTabSelect}>
          <NxTabList>
            <NxTab id="firewall-waivers-components-tab">Components</NxTab>
            <NxTab id="firewall-waivers-containers-tab">Containers</NxTab>
          </NxTabList>

          <NxTabPanel id="firewall-waivers-components-tab-panel">
            {showLimitedFirewallAccessAlert ? (
              limitedAccessAlert
            ) : (
              <NxTabs activeTab={activeSubTab} onTabSelect={onSubTabSelect}>
                <NxTabList>
                  <NxTab id="firewall-waivers-components-approved-tab">
                    Existing Waivers ({componentExistingCount})
                  </NxTab>
                  <NxTab id="firewall-waivers-components-requested-tab">
                    Requested Waivers ({componentRequestedCount})
                  </NxTab>
                </NxTabList>
                <NxTabPanel id="firewall-waivers-components-approved-panel">
                  <DashboardWaivers repositoryFormat="component" />
                </NxTabPanel>
                <NxTabPanel id="firewall-waivers-components-requested-panel">
                  <FirewallRequestedWaiversTable repositoryFormat="component" />
                </NxTabPanel>
              </NxTabs>
            )}
          </NxTabPanel>

          <NxTabPanel id="firewall-waivers-containers-tab-panel">
            {showLimitedFirewallAccessAlert ? (
              limitedAccessAlert
            ) : (
              <NxTabs activeTab={activeSubTab} onTabSelect={onSubTabSelect}>
                <NxTabList>
                  <NxTab id="firewall-waivers-containers-approved-tab">
                    Existing Waivers ({containerExistingCount})
                  </NxTab>
                  <NxTab id="firewall-waivers-containers-requested-tab">
                    Requested Waivers ({containerRequestedCount})
                  </NxTab>
                </NxTabList>
                <NxTabPanel id="firewall-waivers-containers-approved-panel">
                  <ContainerImageWaiversTable />
                </NxTabPanel>
                <NxTabPanel id="firewall-waivers-containers-requested-panel">
                  <FirewallRequestedWaiversTable repositoryFormat="docker" />
                </NxTabPanel>
              </NxTabs>
            )}
          </NxTabPanel>
        </NxTabs>
      </div>
    </NxPageMain>
  );
}
