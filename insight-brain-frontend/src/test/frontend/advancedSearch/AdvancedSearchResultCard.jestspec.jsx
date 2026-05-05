/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import AdvancedSearchResultCard from 'MainRoot/advancedSearch/AdvancedSearchResultCard';
import * as routeSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('AdvancedSearchResultCard', () => {
  let renderComponent, minimalProps;

  beforeEach(() => {
    minimalProps = {
      searchResultItem: {},
      groupIdentifier: '',
      $state: {
        get: jest.fn().mockReturnValue({ data: { title: 'some page' } }),
        href: jest.fn().mockReturnValue('/noop'),
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
    expect(screen.queryByRole('cell', { name: 'License' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Threat Group' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy Violation' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Security Issue' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Vulnerability Description' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Version' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy' })).not.toBeInTheDocument();
  });

  it('displays License and Threat Group rows when data is present', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationName: 'testApp',
      componentEffectiveLicenseName: 'Apache License 2.0',
      componentLicenseThreatGroupName: 'Liberal',
      componentLicenseThreatLevel: 0,
    };

    renderComponent({ searchResultItem: componentTypeResult });

    expect(screen.getByRole('cell', { name: 'License' })).toBeVisible();
    expect(screen.getByText('Apache License 2.0')).toBeVisible();
    expect(screen.getByRole('cell', { name: 'Threat Group' })).toBeVisible();
    expect(screen.getByText(/0 - Liberal/)).toBeVisible();
  });

  it('hides License row when grouped by COMPONENT_EFFECTIVE_LICENSE_NAME', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      componentEffectiveLicenseName: 'Apache License 2.0',
    };

    renderComponent({
      searchResultItem: componentTypeResult,
      groupIdentifier: 'COMPONENT_EFFECTIVE_LICENSE_NAME',
    });

    expect(screen.queryByRole('cell', { name: 'License' })).not.toBeInTheDocument();
  });

  it('hides Threat Group row when grouped by COMPONENT_LICENSE_THREAT_GROUP_NAME', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      componentLicenseThreatGroupName: 'Copyleft',
      componentLicenseThreatLevel: 5,
    };

    renderComponent({
      searchResultItem: componentTypeResult,
      groupIdentifier: 'COMPONENT_LICENSE_THREAT_GROUP_NAME',
    });

    expect(screen.queryByRole('cell', { name: 'Threat Group' })).not.toBeInTheDocument();
  });

  it('displays Policy Violation row with threat level and waiver status when data is present', () => {
    const componentTypeResult = {
      itemType: 'POLICY_VIOLATION',
      applicationName: 'testApp',
      policyViolationPolicyName: 'License-Copyleft',
      policyViolationThreatLevel: 8,
      policyViolationWaiverStatus: 'Active',
    };

    renderComponent({ searchResultItem: componentTypeResult });

    expect(screen.getByRole('cell', { name: 'Policy Violation' })).toBeVisible();
    expect(screen.getByText(/8 -/)).toBeVisible();
    expect(screen.getByText(/License-Copyleft/)).toBeVisible();
    expect(screen.getByText(/\(Active\)/)).toBeVisible();
  });

  it('hides Policy Violation row when grouped by POLICY_VIOLATION_POLICY_NAME', () => {
    const componentTypeResult = {
      itemType: 'POLICY_VIOLATION',
      policyViolationPolicyName: 'License-Copyleft',
      policyViolationThreatLevel: 8,
    };

    renderComponent({
      searchResultItem: componentTypeResult,
      groupIdentifier: 'POLICY_VIOLATION_POLICY_NAME',
    });

    expect(screen.queryByRole('cell', { name: 'Policy Violation' })).not.toBeInTheDocument();
  });

  it('passes the correct parameters to construct the SBOM Manager org link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      organizationId: 'testOrgId',
      organizationName: 'testOrg',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: true });

    expect(screen.getByRole('link', { name: 'testOrg' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('sbomManager.management.view.organization');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ organizationId: 'testOrgId' })
    );
  });

  it('passes the correct parameters to construct the non SBOM Manager org link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      organizationId: 'testOrgId',
      organizationName: 'testOrg',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: false });

    expect(screen.getByRole('link', { name: 'testOrg' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('management.view.organization');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ organizationId: 'testOrgId' })
    );
  });

  it('passes the correct parameters to construct the SBOM Manager app link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationPublicId: 'testAppPublicId',
      applicationName: 'testApp',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: true });

    expect(screen.getByRole('link', { name: 'testApp' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('sbomManager.management.view.application');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ applicationPublicId: 'testAppPublicId' })
    );
  });

  it('passes the correct parameters to construct the SBOM Manager BOM link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationPublicId: 'testAppPublicId',
      applicationName: 'testApp',
      applicationVersion: 'testAppVersion',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: true });

    expect(screen.getByRole('link', { name: 'testAppVersion' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('sbomManager.management.view.bom');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ applicationPublicId: 'testAppPublicId', versionId: 'testAppVersion' })
    );
  });

  it('passes the correct parameters to construct the SBOM Manager CDP link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationPublicId: 'testAppPublicId',
      applicationName: 'testApp',
      applicationVersion: 'testAppVersion',
      componentHash: 'testHash',
      vulnerabilityId: 'testVulnerabilityId',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: true });

    expect(screen.getByRole('link', { name: 'testVulnerabilityId' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('sbomManager.component');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({
        applicationPublicId: 'testAppPublicId',
        sbomVersion: 'testAppVersion',
        componentHash: 'testHash',
      })
    );
  });

  it('passes the correct parameters to construct the non SBOM Manager app link', () => {
    const componentTypeResult = {
      itemType: 'testItemType',
      applicationPublicId: 'testAppPublicId',
      applicationName: 'testApp',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: false });

    expect(screen.getByRole('link', { name: 'testApp' })).toHaveAttribute('href', '/noop');
    expect(minimalProps.$state.get).toHaveBeenCalledWith('management.view.application');
    expect(minimalProps.$state.href).toHaveBeenCalledWith(
      expect.anything(),
      expect.objectContaining({ applicationPublicId: 'testAppPublicId' })
    );
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
    expect(screen.queryByRole('cell', { name: 'Policy Violation' })).not.toBeInTheDocument();
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
    expect(screen.getByRole('link', { name: 'testVulnerabilityId' })).toBeVisible();
    expect(screen.queryByRole('cell', { name: 'Application Category' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Label' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Component Name' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Organization' })).not.toBeInTheDocument();
    expect(screen.queryByRole('cell', { name: 'Policy' })).not.toBeInTheDocument();
  });

  it('Results links in standalone developer app redirects to an external tab', () => {
    jest.spyOn(routeSelectors, 'selectIsDeveloper').mockReturnValue(true);

    const componentTypeResult = {
      organizationName: 'orgName',
      itemType: 'ORGANIZATION',
      applicationName: 'testApp',
      policyEvaluationStage: 'testStage',
      groupIdentifier: 'testGroupIdentifier',
      applicationVersion: 'testApplicationVersion',
      vulnerabilityId: 'testVulnerabilityId',
      vulnerabilityDescription: 'testVulnerabilityDescription',
    };

    renderComponent({ searchResultItem: componentTypeResult, isSbomManager: false });

    expect(screen.getByRole('link', { name: 'orgName' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'orgName' })).toHaveAttribute('target', '_blank');

    expect(screen.getByRole('link', { name: 'testApp' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'testApp' })).toHaveAttribute('target', '_blank');

    expect(screen.getByRole('link', { name: 'testStage' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'testStage' })).toHaveAttribute('target', '_blank');

    expect(screen.getByRole('link', { name: 'testVulnerabilityId' })).toBeVisible();
    expect(screen.getByRole('link', { name: 'testVulnerabilityId' })).toHaveAttribute('target', '_blank');
  });
});
