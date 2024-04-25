/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxAccordion, NxFontAwesomeIcon, NxTable, NxTooltip, useToggle } from '@sonatype/react-shared-components';
import PrioritiesPageRow from 'MainRoot/development/prioritiesPage/PrioritiesPageRow';
import { faInfoCircle } from '@fortawesome/free-solid-svg-icons';
import { selectComponent } from 'MainRoot/applicationReport/applicationReportActions';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';

const NUM_OF_PRIORITY_ROWS_TO_SHOW = 3;

export default function PrioritiesPageTable() {
  const [showPriorityFindings, toggleShowPriorityFindings] = useToggle(true);
  const [showAllFindings, toggleShowAllFindings] = useToggle(true);

  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadTableData());

  const { loadingTableData, loadErrorTableData, tableData } = useSelector(selectPrioritiesPageSlice);
  const priorityRows = tableData?.slice(0, NUM_OF_PRIORITY_ROWS_TO_SHOW);
  const allRows = tableData?.slice(NUM_OF_PRIORITY_ROWS_TO_SHOW, tableData.length);

  useEffect(() => {
    doLoad();
  }, []);

  return (
    <NxTable className="iq-priorities-page-table">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>
            <NxTooltip title="Some title">
              <span>
                Priority <NxFontAwesomeIcon className="iq-priorities-page-table-info-icon" icon={faInfoCircle} />
              </span>
            </NxTooltip>
          </NxTable.Cell>
          <NxTable.Cell>Component</NxTable.Cell>
          <NxTable.Cell>Highest Policy Threat</NxTable.Cell>
          <NxTable.Cell>Recommendation</NxTable.Cell>
          <NxTable.Cell chevron />
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body isLoading={loadingTableData} retryHandler={doLoad} error={loadErrorTableData}>
        <NxTable.Row>
          <NxTable.Cell className="iq-priorities-page-priority-findings-toggle" colSpan={5}>
            <NxAccordion open={showPriorityFindings} onToggle={toggleShowPriorityFindings}>
              <NxAccordion.Header>
                <NxAccordion.Title>Top Priorities</NxAccordion.Title>
              </NxAccordion.Header>
            </NxAccordion>
          </NxTable.Cell>
        </NxTable.Row>
        {showPriorityFindings && <DataRows dataset={priorityRows} />}
        <NxTable.Row>
          <NxTable.Cell className="iq-priorities-page-all-findings-toggle" colSpan={5}>
            <NxAccordion open={showAllFindings} onToggle={toggleShowAllFindings}>
              <NxAccordion.Header>
                <NxAccordion.Title>All Other Findings</NxAccordion.Title>
              </NxAccordion.Header>
            </NxAccordion>
          </NxTable.Cell>
        </NxTable.Row>
        {showAllFindings && <DataRows dataset={allRows} />}
      </NxTable.Body>
    </NxTable>
  );
}

function DataRows({ dataset }) {
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const setSelectedComponent = (idx) => dispatch(selectComponent(idx));
  const goToCDPPage = (hash) =>
    dispatch(stateGo('applicationReport.componentDetails.violations', { hash, publicId: publicAppId, scanId }));

  if (!dataset) return [];
  return dataset.map((component, index) => {
    const { componentHash } = component;

    const onRowClick = () => {
      setSelectedComponent(index);
      goToCDPPage(componentHash);
    };

    return <PrioritiesPageRow key={component.displayName} component={component} onClick={onRowClick} />;
  });
}
