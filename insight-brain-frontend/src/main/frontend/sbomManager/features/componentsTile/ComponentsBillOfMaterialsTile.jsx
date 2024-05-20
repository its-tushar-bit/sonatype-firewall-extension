/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  NxH2,
  NxLoadWrapper,
  NxOverflowTooltip,
  NxSmallThreatCounter,
  NxTable,
  NxTextLink,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';

import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import {
  selectComponentsSbomsResults,
  selectComponentsSbomsResultsError,
  selectComponentsSbomsResultsIsLoading,
  selectSortDir,
} from './componentsBillOfMaterialsSelectors.js';
import { actions } from './componentsBillOfMaterialsSlice.js';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

import './componentsBillOfMaterialsTile.scss';

export default function ComponentsBillOfMaterialsTile(props) {
  const routerParams = useSelector(selectRouterCurrentParams);
  const { internalAppId, sbomVersion, isInternalAppIdLoading } = props;
  const dispatch = useDispatch();
  const doLoad = () => dispatch(actions.loadSbomTableData({ internalAppId, sbomVersion }));
  const uiRouterState = useRouterState();
  const componentDetailsState = 'sbomManager.component';
  const componentDetailsHref = (componentHash) => {
    const { applicationPublicId, versionId } = routerParams;
    return uiRouterState.href(componentDetailsState, {
      applicationPublicId,
      sbomVersion: versionId,
      componentHash,
    });
  };

  useEffect(() => {
    if (internalAppId) doLoad();
  }, []);

  const componentsTableData = useSelector(selectComponentsSbomsResults);
  const isTableLoading = useSelector(selectComponentsSbomsResultsIsLoading);
  const errorTableLoading = useSelector(selectComponentsSbomsResultsError);
  const sortDir = useSelector(selectSortDir);

  const getLicenseList = (licenses) => {
    if (isNilOrEmpty(licenses)) {
      return '';
    }
    return licenses.map((l) => l.licenseName).join(', ');
  };

  const generateTableBodyRows = () => {
    if (!isNilOrEmpty(componentsTableData)) {
      return (
        <>
          {componentsTableData.map((component) => (
            <NxTable.Row key={component.hash}>
              <NxTable.Cell>
                <NxOverflowTooltip>
                  <div className="nx-truncate-ellipsis bom-component-display-name">
                    <NxTextLink id="sbom-component-details-link" href={componentDetailsHref(component.hash)}>
                      {component.displayName}
                    </NxTextLink>
                  </div>
                </NxOverflowTooltip>
              </NxTable.Cell>
              <NxTable.Cell>
                <NxSmallThreatCounter
                  maxDigits={2}
                  criticalCount={component.vulnerabilitySeverityCriticalCount}
                  severeCount={component.vulnerabilitySeverityHighCount}
                  moderateCount={component.vulnerabilitySeverityMediumCount}
                  lowCount={component.vulnerabilitySeverityLowCount}
                />
              </NxTable.Cell>
              <NxTable.Cell>
                <NxTooltip
                  title={getLicenseList(component.licenses)}
                  className="sbom-manager-bills-of-materials-tile-table__tooltip"
                >
                  <span className="sbom-manager-bills-of-materials-tile-table__licenses">
                    {getLicenseList(component.licenses)}
                  </span>
                </NxTooltip>
              </NxTable.Cell>
            </NxTable.Row>
          ))}
        </>
      );
    }
  };

  return (
    <NxLoadWrapper retryHandler={doLoad} loading={isInternalAppIdLoading || isTableLoading} error={errorTableLoading}>
      <NxTile className="sbom-manager-bills-of-materials-tile">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Components</NxH2>
          </NxTile.HeaderTitle>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable className="sbom-manager-bills-of-materials-tile-table">
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell isSortable sortDir={sortDir} onClick={() => dispatch(actions.toggleSortDir())}>
                  Name
                </NxTable.Cell>
                <NxTable.Cell>Vulnerabilities</NxTable.Cell>
                <NxTable.Cell>License</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body>{generateTableBodyRows()}</NxTable.Body>
          </NxTable>
        </NxTile.Content>
      </NxTile>
    </NxLoadWrapper>
  );
}

ComponentsBillOfMaterialsTile.propTypes = {
  isInternalAppIdLoading: PropTypes.bool.isRequired,
  internalAppId: PropTypes.string,
  sbomVersion: PropTypes.string,
};
