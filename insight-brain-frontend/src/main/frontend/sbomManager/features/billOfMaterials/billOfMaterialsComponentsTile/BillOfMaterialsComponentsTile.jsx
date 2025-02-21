/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect, useCallback } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import * as PropTypes from 'prop-types';
import {
  allPass,
  always,
  compose,
  cond,
  dec,
  equals,
  flip,
  gt,
  ifElse,
  inc,
  includes,
  is,
  join,
  lt,
  map,
  min,
  prop,
  T,
  trim,
  when,
} from 'ramda';
import debounce from 'debounce';
import classNames from 'classnames';
import {
  NxBinaryDonutChart,
  NxButton,
  NxFontAwesomeIcon,
  NxH2,
  NxPagination,
  NxSmallThreatCounter,
  NxStatefulTextInput,
  NxTable,
  NxTextLink,
  NxTile,
  NxTooltip,
} from '@sonatype/react-shared-components';
import { faFilter, faInfoCircle, faSearch } from '@fortawesome/pro-solid-svg-icons';

import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { formatNumberLocale } from 'MainRoot/util/formatUtils';

import ComponentsFilterDrawer from './componentsFilterDrawer/ComponentsFilterDrawer';
import {
  selectBillOfMaterialsComponentsTile,
  selectComponentNameSearch,
} from './billOfMaterialsComponentsTileSelectors.js';
import { actions, COMPONENTS_PER_PAGE, SORT_BY_FIELDS, SORT_DIRECTION } from './billOfMaterialsComponentsTileSlice';
import { actions as cdpActions } from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';
import { selectIsSbomPoliciesSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';

import './billOfMaterialsComponentsTile.scss';
import {
  selectSbomMetadata,
  selectInternalAppId,
} from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';

const LOAD_COMPONENTS_DEBOUNCE_TIMEOUT_MS = 300;

const ComponentsTileComponentSearch = ({ onSearch }) => {
  const handleOnChange = (value) => compose(onSearch, when(isNilOrEmpty, always(null)), when(is(String), trim))(value);
  return (
    <div className="bill-of-materials-components-tile-search">
      <NxFontAwesomeIcon icon={faSearch} />
      <NxStatefulTextInput
        aria-label="Component Search"
        id="component-search"
        placeholder="Search by component or license"
        onChange={handleOnChange}
      />
    </div>
  );
};

ComponentsTileComponentSearch.propTypes = {
  onSearch: PropTypes.func.isRequired,
};

export default function BillOfMaterialsComponentsTile() {
  const { applicationPublicId, versionId: sbomVersion } = useSelector(selectRouterCurrentParams);
  const internalAppId = useSelector(selectInternalAppId);
  const { displayNameSortingEnabled } = useSelector(selectSbomMetadata);
  const isSbomPoliciesSupported = useSelector(selectIsSbomPoliciesSupported);
  const uiRouterState = useRouterState();

  const dispatch = useDispatch();

  const toggleFilterDrawer = () => {
    if (!loadingComponents) dispatch(actions.toggleShowFilterDrawer());
  };

  const loadComponents = () => dispatch(actions.loadComponents({ internalAppId, sbomVersion }));
  const debouncedLoadComponents = useCallback(debounce(loadComponents, LOAD_COMPONENTS_DEBOUNCE_TIMEOUT_MS), []);
  const componentSearch = (searchTerm) => {
    dispatch(actions.setComponentSearch(searchTerm));
    dispatch(actions.setCurrentPage(0));
    debouncedLoadComponents();
  };

  const loadSortedComponents = (sortBy) => {
    dispatch(actions.setSortByAndCycleDirection(sortBy));
    debouncedLoadComponents();
  };

  const setCurrentPageAndLoadComponents = (page) => {
    dispatch(actions.setCurrentPage(page));
    debouncedLoadComponents();
  };

  const {
    loadingComponents,
    loadingComponentsErrorMessage,
    components,
    totalNumberOfComponents,
    sortConfiguration,
    filterConfiguration,
    pagination,
  } = useSelector(selectBillOfMaterialsComponentsTile);
  const componentNameSearchFromState = useSelector(selectComponentNameSearch);

  useEffect(() => {
    if (internalAppId) {
      dispatch(actions.resetLoadComponentsConfigurations());
      loadComponents();
    }
  }, [internalAppId, sbomVersion]);

  useEffect(() => {
    if (components) {
      dispatch(
        cdpActions.setComponentDetailsPaginationData({
          pagination,
          pagesData: { [pagination.currentPage]: components },
          totalNumberOfComponents,
          sortConfiguration,
          filterConfiguration,
          componentNameSearchFromState,
        })
      );
    }
  }, [components]);

  const componentDetailsState = 'sbomManager.component';
  const componentDetailsHref = (componentHash) =>
    uiRouterState.href(componentDetailsState, {
      applicationPublicId,
      sbomVersion,
      componentHash,
    });

  const licenseListToString = compose(
    join(', '),
    map(ifElse(compose(isNilOrEmpty(), prop('licenseName')), prop('licenseId'), prop('licenseName'))),
    when(isNilOrEmpty, always([]))
  );
  const hasComponents = !isNilOrEmpty(components);
  const componentRows = hasComponents
    ? components.map((component) => {
        const originalComponent = component.filenames ? component.filenames[0] : null;
        const licenseString = licenseListToString(component.licenses);
        const displayNameClasses = classNames('sbom-manager-bill-of-materials-components-tile__display-name', {
          'sbom-manager-bill-of-materials-components-tile__display-name--direct-dependency':
            component.dependencyType === 'direct',
        });
        return (
          <NxTable.Row key={component.hash}>
            <NxTable.Cell>
              {includes(component.dependencyType, ['direct', 'transitive']) ? (
                <DependencyIndicator type={component.dependencyType} />
              ) : null}
            </NxTable.Cell>
            <NxTable.Cell>
              <span className="sbom-manager-bill-of-materials-components-tile__component-name-content">
                <NxTooltip
                  title={component.displayName}
                  className="sbom-manager-bill-of-materials-components-tile__tooltip--display-name"
                >
                  <NxTextLink className={displayNameClasses} href={componentDetailsHref(component.hash)}>
                    {component.displayName}
                  </NxTextLink>
                </NxTooltip>
                {includes(component.matchStateId, ['similar']) ? (
                  <NxTooltip
                    title={
                      <span>
                        Original Component Name: {originalComponent}.<br />
                        Similar component match: This component is similar to a known open <br />
                        source component within your application based on its attributes.
                      </span>
                    }
                    className="sbom-manager-bill-of-materials-components-tile__tooltip--similar-match"
                    data-testid="similarMatchIcon"
                  >
                    <NxFontAwesomeIcon
                      icon={faInfoCircle}
                      className="sbom-manager-bill-of-materials-components-tile__info-icon"
                    />
                  </NxTooltip>
                ) : null}
              </span>
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
            {isSbomPoliciesSupported && <NxTable.Cell>{component.policyViolationCount}</NxTable.Cell>}
            <NxTable.Cell>
              <div className="sbom-manager-bill-of-materials-components-tile__release-status-percentage">
                <NxBinaryDonutChart
                  className="sbom-manager-bill-of-materials-components-tile__release-status-percentage-donut"
                  value={component.releaseStatusPercentage}
                  aria-label={`${component.releaseStatusPercentage}% release ready`}
                  innerRadiusPercent={80}
                />
                <span>{component.releaseStatusPercentage}%</span>
              </div>
            </NxTable.Cell>
            <NxTable.Cell>
              <NxTooltip title={licenseString} className="sbom-manager-bill-of-materials-components-tile__tooltip">
                <span className="sbom-manager-bill-of-materials-components-tile__licenses">{licenseString}</span>
              </NxTooltip>
            </NxTable.Cell>
          </NxTable.Row>
        );
      })
    : null;

  const showTableContent = !loadingComponents && !loadingComponentsErrorMessage && hasComponents;

  const paginationSection = () => {
    if (showTableContent) {
      const status = cond([
        [equals(0), always(min(COMPONENTS_PER_PAGE, totalNumberOfComponents))],
        [
          allPass([flip(gt)(0), flip(lt)(dec(pagination.pageCount))]),
          always(
            `${formatNumberLocale(inc(pagination.currentPage * COMPONENTS_PER_PAGE))}\u2014${formatNumberLocale(
              inc(pagination.currentPage) * COMPONENTS_PER_PAGE
            )}`
          ),
        ],
        [T, always(formatNumberLocale(totalNumberOfComponents))],
      ])(pagination.currentPage);

      return (
        <div className="sbom-manager-bill-of-materials-components-tile__pagination-section">
          <div className="sbom-manager-bill-of-materials-components-tile__pagination-wrapper">
            <NxPagination
              className="sbom-manager-bill-of-materials-components-tile__pagination"
              aria-controls="bill-of-materials-components-table"
              pageCount={pagination.pageCount}
              currentPage={pagination.currentPage}
              onChange={setCurrentPageAndLoadComponents}
            />
          </div>
          <div
            className="sbom-manager-bill-of-materials-components-tile__pagination-status"
            data-testid="bill-of-materials-components-tile-pagination-status"
          >
            Showing {status} of {formatNumberLocale(totalNumberOfComponents)} components
          </div>
        </div>
      );
    }
    return null;
  };

  const createColumnSortHandler = (field) =>
    showTableContent && totalNumberOfComponents > 1
      ? {
          sortDir: field === sortConfiguration.sortBy ? sortConfiguration.sortDirection : SORT_DIRECTION.DEFAULT,
          onClick: () => loadSortedComponents(field),
          isSortable: true,
        }
      : {};

  const isEmpty = !loadingComponents && !loadingComponentsErrorMessage && !hasComponents;
  const tableBodyProps = {
    isLoading: loadingComponents,
    retryHandler: loadComponents,
    ...(loadingComponentsErrorMessage && { error: loadingComponentsErrorMessage }),
    ...(isEmpty && { emptyMessage: 'No components found' }),
  };

  return (
    <>
      <ComponentsFilterDrawer internalAppId={internalAppId} sbomVersion={sbomVersion} />
      <NxTile className="sbom-manager-bill-of-materials-components-tile">
        <NxTile.Header>
          <NxTile.HeaderTitle>
            <NxH2>Components</NxH2>
          </NxTile.HeaderTitle>
          <NxTile.HeaderActions className="sbom-manager-bill-of-materials-components-tile__actions">
            <ComponentsTileComponentSearch onSearch={componentSearch} />
            <NxButton variant="tertiary" onClick={toggleFilterDrawer} disabled={loadingComponents}>
              <NxFontAwesomeIcon icon={faFilter} />
              <span>Filter By</span>
            </NxButton>
          </NxTile.HeaderActions>
        </NxTile.Header>
        <NxTile.Content>
          <NxTable
            id="bill-of-materials-components-table"
            className="sbom-manager-bill-of-materials-components-tile__table"
          >
            <NxTable.Head>
              <NxTable.Row>
                <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.type)}>Type</NxTable.Cell>
                <NxTable.Cell
                  {...(displayNameSortingEnabled ? createColumnSortHandler(SORT_BY_FIELDS.displayName) : {})}
                >
                  Name
                </NxTable.Cell>
                <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.vulnerabilities)}>
                  Vulnerabilities
                </NxTable.Cell>
                {isSbomPoliciesSupported && <NxTable.Cell>Violations</NxTable.Cell>}
                <NxTable.Cell {...createColumnSortHandler(SORT_BY_FIELDS.releaseStatusPercentage)}>
                  Release Status
                </NxTable.Cell>
                <NxTable.Cell>License</NxTable.Cell>
              </NxTable.Row>
            </NxTable.Head>
            <NxTable.Body {...tableBodyProps}>{componentRows}</NxTable.Body>
          </NxTable>
          {paginationSection()}
        </NxTile.Content>
      </NxTile>
    </>
  );
}
