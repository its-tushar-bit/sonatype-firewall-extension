/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { within } from '@testing-library/react';
import VulnerabilitiesTable from 'MainRoot/componentDetails/VulnerabilitiesTableTile/VulnerabilitiesTable';
import {
  CPE_MATCH_DETECTION_TYPE,
  FAST_TRACK_RESEARCH_TYPE,
  HIGH_CONFIDENCE,
  LOW_CONFIDENCE,
  PUBLIC_RESEARCH_TYPE,
  SBOM_ID_SOURCE,
  SONATYPE_ID_SOURCE,
  SONATYPE_ID_SOURCE_FOR_UI,
} from 'MainRoot/util/vulnerabilityUtils';
import { isNilOrEmpty } from 'MainRoot/util/jsUtil';

describe('VulnerabilitiesTable', () => {
  const TEST_REF_ID = 'CVE-123';
  const TEST_STATUS = 'Open';
  const TEST_SEVERITY = 2;
  const TEST_BASE_VULNERABILITY = {
    refId: TEST_REF_ID,
    status: TEST_STATUS,
    severity: TEST_SEVERITY,
  };

  let defaultPreloadedState;
  let renderComponent;

  beforeAll(() => {});

  beforeEach(() => {
    defaultPreloadedState = {
      loadVulnerabilities: () => null,
      vulnerabilities: { data: [], error: null },
      toggleVulnerabilityPopoverWithEffects: () => null,
    };

    renderComponent = (preloadedState = defaultPreloadedState) => render(<VulnerabilitiesTable {...preloadedState} />);
  });

  const withNewState = function (testVulnerabilities) {
    return {
      ...defaultPreloadedState,
      vulnerabilities: {
        ...defaultPreloadedState.vulnerabilities,
        data: testVulnerabilities,
      },
    };
  };

  const assertVulnerabilityTableRowData = (actualVulnerability) => {
    const dataRow = screen.getAllByRole('rowgroup')[1];
    const actualRow = within(dataRow).getAllByRole('row')[0];
    const actualCells = within(actualRow).getAllByRole('cell');

    expect(actualCells[0]).toHaveTextContent(actualVulnerability.severity);
    expect(actualCells[1]).toHaveTextContent(actualVulnerability.refId);

    isNilOrEmpty(actualVulnerability.identificationSource)
      ? expect(actualCells[2].firstChild).toBeEmptyDOMElement()
      : expect(actualCells[2]).toHaveTextContent(actualVulnerability.identificationSource);
    isNilOrEmpty(actualVulnerability.confidence)
      ? expect(actualCells[3].firstChild).toBeEmptyDOMElement()
      : expect(actualCells[3]).toHaveTextContent(actualVulnerability.confidence);
    expect(actualCells[4]).toHaveTextContent(actualVulnerability.status);
  };

  it('renders empty table', async () => {
    renderComponent();
    const vulnerabilitiesTable = screen.getByRole('table');
    const vulnerabilitiesTableCols = within(vulnerabilitiesTable).getAllByRole('columnheader');
    expect(vulnerabilitiesTableCols[0]).toHaveTextContent('CVSS');
    expect(vulnerabilitiesTableCols[1]).toHaveTextContent('ISSUE');
    expect(vulnerabilitiesTableCols[2]).toHaveTextContent('IDENTIFICATION SOURCE');
    expect(vulnerabilitiesTableCols[3]).toHaveTextContent('CONFIDENCE');
    expect(vulnerabilitiesTableCols[4]).toHaveTextContent('STATUS');

    const vulnerabilitiesTableRows = within(vulnerabilitiesTable).getAllByRole('row');
    expect(vulnerabilitiesTableRows[1]).toHaveTextContent('No vulnerabilities');
  });

  it('renders table with vulnerability without confidence or without identificationSource', async () => {
    const testVulnerabilities = [TEST_BASE_VULNERABILITY];

    renderComponent(withNewState(testVulnerabilities));
    assertVulnerabilityTableRowData(TEST_BASE_VULNERABILITY);
  });

  it('renders table with vulnerability without confidence and valid identification source', async () => {
    const testVulnerability = {
      ...TEST_BASE_VULNERABILITY,
      identificationSource: SONATYPE_ID_SOURCE,
    };

    renderComponent(withNewState([testVulnerability]));
    assertVulnerabilityTableRowData({
      ...testVulnerability,
      identificationSource: SONATYPE_ID_SOURCE_FOR_UI,
    });
  });

  it('renders table with vulnerability without identification source and valid confidence', async () => {
    const testVulnerability = {
      ...TEST_BASE_VULNERABILITY,
      detectionType: CPE_MATCH_DETECTION_TYPE,
      researchType: PUBLIC_RESEARCH_TYPE,
    };

    renderComponent(withNewState([testVulnerability]));
    assertVulnerabilityTableRowData({
      ...testVulnerability,
      confidence: LOW_CONFIDENCE,
    });
  });

  it('renders table with vulnerability with valid identification source and confidence', async () => {
    const testVulnerability = {
      ...TEST_BASE_VULNERABILITY,
      identificationSource: SBOM_ID_SOURCE,
      detectionType: 'PRIMARY',
      researchType: FAST_TRACK_RESEARCH_TYPE,
    };

    renderComponent(withNewState([testVulnerability]));
    assertVulnerabilityTableRowData({
      ...testVulnerability,
      confidence: HIGH_CONFIDENCE,
    });
  });
});
