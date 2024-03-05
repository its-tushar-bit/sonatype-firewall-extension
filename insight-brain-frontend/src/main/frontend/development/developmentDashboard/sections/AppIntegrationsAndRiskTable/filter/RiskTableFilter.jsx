/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { IqPopover } from 'MainRoot/react/IqPopover';
import { NxRadio, NxFieldset, NxButton } from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectAppIntegrationsAndRiskSlice } from 'MainRoot/development/developmentDashboard/selectors/appIntegrationsAndRiskSelectors';
import { actions } from 'MainRoot/development/developmentDashboard/slices/appIntegrationsAndRiskSlice';
import './RiskTableFilter.scss';

export default function RiskTableFilter() {
  const appIntegrationsAndRiskSlice = useSelector(selectAppIntegrationsAndRiskSlice);
  const { scmFilter, ciCdFilter } = appIntegrationsAndRiskSlice;
  const dispatch = useDispatch();

  function handleCloseBtnClick() {
    dispatch(actions.toggleFilterSideBar(false));
  }

  function handleApplyBtnClick() {
    dispatch(actions.toggleFilterSideBar(false));
    dispatch(actions.loadAppIntegrationsAndRisk());
  }

  return (
    <IqPopover className="risk-table-filter" onClose={() => handleCloseBtnClick(false)}>
      <IqPopover.Header
        className="risk-table-filter-header"
        buttonId="risk-table-filter-close-btn"
        onClose={handleCloseBtnClick}
        headerSize="h3"
        headerTitle="Filter"
        closeTitle="Close"
      ></IqPopover.Header>
      <NxFieldset label="CI/CD Configuration">
        <NxRadio
          name="ci-all"
          value="ci-all"
          onChange={() => dispatch(actions.setCiCdFilter(null))}
          isChecked={ciCdFilter === null}
          radioId="filter-ci-all"
        >
          All
        </NxRadio>
        <NxRadio
          name="ci-configured"
          value="ci-configured"
          onChange={() => dispatch(actions.setCiCdFilter(true))}
          isChecked={ciCdFilter === true}
          radioId="filter-ci-configured"
        >
          Configured apps
        </NxRadio>
        <NxRadio
          name="ci-not-configured"
          value="ci-not-configured"
          onChange={() => dispatch(actions.setCiCdFilter(false))}
          isChecked={ciCdFilter === false}
          radioId="filter-ci-not-configured"
        >
          Non-configured apps
        </NxRadio>
      </NxFieldset>
      <NxFieldset label="SCM Feedback Configuration">
        <NxRadio
          name="scm-all"
          value="scm-all"
          onChange={() => dispatch(actions.setSCMFilter(null))}
          isChecked={scmFilter === null}
          radioId="filter-scm-all"
        >
          All
        </NxRadio>
        <NxRadio
          name="scm-configured"
          value="scm-configured"
          onChange={() => dispatch(actions.setSCMFilter(true))}
          isChecked={scmFilter === true}
          radioId="filter-scm-configured"
        >
          Configured apps
        </NxRadio>
        <NxRadio
          name="scm-not-configured"
          value="scm-not-configured"
          onChange={() => dispatch(actions.setSCMFilter(false))}
          isChecked={scmFilter === false}
          radioId="filter-scm-not-configured"
        >
          Non-configured apps
        </NxRadio>
      </NxFieldset>
      <IqPopover.Footer>
        <div className="nx-btn-bar risk-table-flter-footer-btns">
          <NxButton id="risk-table-filter-apply" variant="primary" onClick={handleApplyBtnClick}>
            Apply
          </NxButton>
        </div>
      </IqPopover.Footer>
    </IqPopover>
  );
}
