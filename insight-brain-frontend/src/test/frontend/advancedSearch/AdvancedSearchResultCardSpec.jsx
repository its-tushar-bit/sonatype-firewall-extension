/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchResultCard from 'MainRoot/advancedSearch/AdvancedSearchResultCard';

describe('AdvancedSearchResultCard', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      searchResultItem: {},
      groupIdentifier: '',
      $state: {
        get: jasmine.createSpy('$state.get').and.returnValue({ data: { title: 'some page' } }),
        href: jasmine.createSpy('$state.href').and.returnValue('/noop'),
      },
    };

    renderComponent = (additionalProps = {}) => {
      render(<AdvancedSearchResultCard {...minimalProps} {...additionalProps} />);
    };
  });

  it('only displays component name and report when result does not contain a vulnerability', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationName: 'testApp',
      policyEvaluationStage: 'testStage',
      groupIdentifier: 'testGroupIdentifier',
    };

    renderComponent({ searchResultItem: componentTypeResult });

    // Application and Report rows should exist
    expect(screen.getByRole('row', { name: 'Application testApp' })).toBeVisible();
    expect(screen.getByRole('row', { name: 'Report testStage' })).toBeVisible();

    // All other named cells should not exist
    expect(screen.queryByRole('cell', { name: 'Organization' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Application Category' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Name' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Label' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Security Issue' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Vulnerability Description' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Version' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy' })).not.toBeInTheDocument();
  });

  it('displays vulnerability link when not in SBOM Manager', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationName: 'testApp',
      policyEvaluationStage: 'testStage',
      groupIdentifier: 'testGroupIdentifier',
      vulnerabilityId: 'testVulnerabilityId',
      vulnerabilityDescription: 'testVulnerabilityDescription',
    };

    renderComponent({ searchResultItem: componentTypeResult });

    // Application and Report rows should exist
    expect(screen.getByRole('cell', { name: 'Security Issue' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'testVulnerabilityId' })).toHaveAttribute('href', '/noop');
    expect(screen.getByRole('row', { name: 'Application testApp' })).toBeVisible();
    expect(screen.getByRole('row', { name: 'Report testStage' })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Vulnerability Description' })).toBeVisible();

    // All other named cells should not exist
    expect(screen.queryByRole('cell', { name: 'Application Category' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Label' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Name' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Organization' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Version' })).not.toBeInTheDocument();
  });

  it('Does not display the vulnerability link when in SBOM Manager', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationName: 'testApp',
      policyEvaluationStage: 'testStage',
      groupIdentifier: 'testGroupIdentifier',
      applicationVersion: 'testApplicationVersion',
      vulnerabilityId: 'testVulnerabilityId',
      vulnerabilityDescription: 'testVulnerabilityDescription',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: true });

    // Application and Report rows should exist
    expect(screen.getByRole('cell', { name: 'Security Issue' })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'testVulnerabilityId' })).toBeVisible();
    expect(screen.getByRole('row', { name: 'Application testApp' })).toBeVisible();
    expect(screen.getByRole('row', { name: 'Report testStage' })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Version' })).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Vulnerability Description' })).toBeVisible();

    // All other named cells should not exist
    expect(screen.queryByRole('link', { name: 'testVulnerabilityId' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Application Category' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Label' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Name' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Organization' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy' })).not.toBeInTheDocument();
  });
});
