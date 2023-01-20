/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { actions } from './componentRiskSlice';
import { selectComponentRisk } from './componentRiskSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { NxPageMain, NxPageTitle, NxLoadWrapper, NxH1, NxH2, NxP } from '@sonatype/react-shared-components';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import ComponentRiskItem from './componentRiskItem/ComponentRiskItem';

export default function ComponentRiskDetails() {
  // System
  const dispatch = useDispatch();

  const uiRouterState = useRouterState();
  const dashboardHref = uiRouterState.href('dashboard.overview.components');

  // Selectors
  const { loading, loadError, componentName, totalRisk, applicationComponents } = useSelector(selectComponentRisk);
  const { hash } = useSelector(selectRouterCurrentParams);

  // actions
  const loadDetailsAndComponents = () => dispatch(actions.loadDetailsAndComponents(hash));

  useEffect(() => {
    if (!loading) {
      loadDetailsAndComponents();
    }
  }, []);

  const listItems = applicationComponents.map((element) => (
    <ComponentRiskItem key={element.application.id} totalRisk={totalRisk} {...element} />
  ));

  return (
    <NxPageMain className="nx-viewport-sized">
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={loadDetailsAndComponents}>
        <MenuBarBackButton text="Back to Dashboard" href={dashboardHref}></MenuBarBackButton>
        <NxPageTitle className="iq-component-risk-header">
          <NxPageTitle.Headings>
            <NxH1 id="iq-component-name">{componentName}</NxH1>
            <NxH2 className="iq-component-total-risk">
              Total risk: <span id="iq-component-total-risk">{totalRisk}</span>
            </NxH2>
          </NxPageTitle.Headings>
          <NxPageTitle.Description>
            <NxP>Risk score by application</NxP>
          </NxPageTitle.Description>
        </NxPageTitle>
        {listItems}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
