/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxButton,
  NxButtonBar,
  NxCollapsibleItems,
  NxFontAwesomeIcon,
  NxH1,
  NxH2,
  NxLoadWrapper,
  NxPageTitle,
  NxSearchDropdown,
  NxSmallThreatCounter,
  NxStatefulFilterDropdown,
  NxStatefulSegmentedButton,
  NxTable,
  NxTextLink,
  NxThreatIndicator,
  NxTile,
  useToggle,
} from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import DependencyIndicator from 'MainRoot/DependencyTree/DependencyIndicator';
import {
  selectLoadErrorFeatures,
  selectLoadingFeatures,
  selectNoSbomManagerEnabledError,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { faDownload, faSync } from '@fortawesome/pro-solid-svg-icons';

export default function BillOfMaterials() {
  const [toggleCheck, onToggleCollapse] = useToggle(false);
  const isProductFeaturesLoading = useSelector(selectLoadingFeatures);
  const errorLoadingProductFeatures = useSelector(selectLoadErrorFeatures);
  const noSbomManagerEnabledError = useSelector(selectNoSbomManagerEnabledError);

  return (
    <div id="sbom-manager-bom">
      <NxLoadWrapper
        retryHandler={() => {}}
        loading={isProductFeaturesLoading}
        error={errorLoadingProductFeatures || noSbomManagerEnabledError}
      >
        <NxPageTitle>
          <NxH1>Bill Of Materials</NxH1>
          <NxButtonBar>
            <NxButton variant="tertiary">
              <NxFontAwesomeIcon icon={faSync} />
              <span>Re-Evaluate</span>
            </NxButton>
            <NxStatefulSegmentedButton
              variant="primary"
              onClick={() => {}}
              buttonContent={
                <>
                  <NxFontAwesomeIcon icon={faDownload} />
                  <span>Download</span>
                </>
              }
            >
              <button className="nx-dropdown-button">Dropdown item 1</button>
              <button className="nx-dropdown-button">Dropdown item 2</button>
            </NxStatefulSegmentedButton>
          </NxButtonBar>
          <NxPageTitle.Tags>
            <NxThreatIndicator threatLevelCategory="critical" presentational />
            <span>Critical</span>
            <NxThreatIndicator threatLevelCategory="severe" presentational />
            <span>High</span>
            <NxThreatIndicator threatLevelCategory="moderate" presentational />
            <span>Medium</span>
            <NxThreatIndicator threatLevelCategory="low" presentational />
            <span>Low</span>
            <NxThreatIndicator threatLevelCategory="none" presentational />
            <span>None</span>
          </NxPageTitle.Tags>
        </NxPageTitle>
        <NxTile>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Summary</NxH2>
            </NxTile.HeaderTitle>
          </NxTile.Header>
          <NxTile.Content>
            <div className="summary-grid">
              <div>
                <span className="semibold">Imported:</span> 2023-01-01 19:00:34
              </div>
              <div>
                <span className="semibold">Last Evaluation:</span> 2024-02-19 19:00:34
              </div>
              <strong>Total vulnerabilities</strong>
              <div>
                <strong>136</strong> of 248
              </div>
              <strong>Vulnerable components</strong>
              <div>
                <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
              </div>
            </div>
            <NxCollapsibleItems onToggleCollapse={onToggleCollapse} isOpen={toggleCheck} triggerContent="Show metadata">
              <NxCollapsibleItems.Child>
                <div className="metadata-grid">
                  <div>
                    <div className="semibold">Author</div>
                    Sonatype Inc.
                  </div>
                  <div>
                    <div className="semibold">Manufacturer</div>
                    Sonatype Inc.
                  </div>
                  <div>
                    <div className="semibold">Supplier</div>
                    Sonatype Inc.
                  </div>
                  <div>
                    <div className="semibold">BOM Format</div>
                    Cyclone DX 1.2
                  </div>
                  <div>
                    <div className="semibold">File Format</div>
                    Json
                  </div>
                  <div>
                    <div className="semibold">Spec Version</div>
                    1.2
                  </div>
                </div>
              </NxCollapsibleItems.Child>
            </NxCollapsibleItems>
          </NxTile.Content>
        </NxTile>
        <NxTile>
          <NxTile.Header>
            <NxTile.HeaderTitle>
              <NxH2>Components</NxH2>
            </NxTile.HeaderTitle>
            <NxTile.HeaderActions>
              <NxSearchDropdown
                matches={[]}
                searchText=""
                onSearchTextChange={() => {}}
                onSearch={() => {}}
                onSelect={() => {}}
                className="sbom-search-component"
              />
              <NxStatefulFilterDropdown
                options={[]}
                selectedIds={[]}
                onChange={() => {}}
                placeholder="Filter by"
                className="sbom-filter-component"
              />
              <NxButton variant="tertiary">Dependency Tree</NxButton>
            </NxTile.HeaderActions>
          </NxTile.Header>
          <NxTile.Content>
            <NxTable>
              <NxTable.Head>
                <NxTable.Row>
                  <NxTable.Cell>Type</NxTable.Cell>
                  <NxTable.Cell>Name</NxTable.Cell>
                  <NxTable.Cell>Vulnerabilities</NxTable.Cell>
                  <NxTable.Cell>License</NxTable.Cell>
                </NxTable.Row>
              </NxTable.Head>
              <NxTable.Body>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="direct" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="direct" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="direct" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="direct" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="transitive" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
                <NxTable.Row>
                  <NxTable.Cell>
                    <DependencyIndicator type="direct" />
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxTextLink>jackson-databind 1.6.1</NxTextLink>
                  </NxTable.Cell>
                  <NxTable.Cell>
                    <NxSmallThreatCounter criticalCount={27} severeCount={5} moderateCount={1337} lowCount={323} />
                  </NxTable.Cell>
                  <NxTable.Cell>Apache 2.0</NxTable.Cell>
                </NxTable.Row>
              </NxTable.Body>
            </NxTable>
          </NxTile.Content>
        </NxTile>
      </NxLoadWrapper>
    </div>
  );
}
