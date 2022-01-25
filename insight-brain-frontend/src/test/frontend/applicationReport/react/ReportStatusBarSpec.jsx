/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import ReportStatusBar from 'MainRoot/applicationReport/react/ReportStatusBar';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';

describe('ReportStatusBar', () => {
  let renderComponent, selectedReport;

  beforeEach(function () {
    selectedReport = {
      knownArtifactCount: 250,
      totalArtifactCount: 500,
      policyComponentCount: 555,
      grandfatheredPolicyViolationCount: 33,
      criticalViolationCount: 111,
      severeViolationCount: 222,
      moderateViolationCount: 333,
      nonLowViolationCount: 123,
    };
    spyOn(applicationReportSelectors, 'selectSelectedReport').and.returnValue(selectedReport);

    renderComponent = () => render(<ReportStatusBar />);
  });

  it('renders critical threat indicator with count', () => {
    renderComponent();
    const criticalThreatIndicator = screen.getByText(selectedReport.criticalViolationCount);

    expect(criticalThreatIndicator).toBeVisible();
    expect(criticalThreatIndicator).toHaveClassName('iq-threat-indicator critical');
  });

  it('renders severe threat indicator with count', () => {
    renderComponent();
    const severeThreatIndicator = screen.getByText(selectedReport.severeViolationCount);

    expect(severeThreatIndicator).toBeVisible();
    expect(severeThreatIndicator).toHaveClassName('iq-threat-indicator severe');
  });

  it('renders moderate threat indicator with count', () => {
    renderComponent();
    const moderateThreatIndicator = screen.getByText(selectedReport.moderateViolationCount);

    expect(moderateThreatIndicator).toBeVisible();
    expect(moderateThreatIndicator).toHaveClassName('iq-threat-indicator moderate');
  });

  it('renders total violation', () => {
    selectedReport.nonLowViolationCount = 1;

    renderComponent();
    const totalViolationText = screen.getByText(`${selectedReport.nonLowViolationCount} VIOLATION`);

    expect(totalViolationText).toBeVisible();
  });

  it('renders total violations', () => {
    renderComponent();
    const totalViolationText = screen.getByText(`${selectedReport.nonLowViolationCount} VIOLATIONS`);

    expect(totalViolationText).toBeVisible();
  });

  it('renders affected component', () => {
    selectedReport.policyComponentCount = 1;

    renderComponent();
    const affectedComponentText = screen.getByText(`Affecting ${selectedReport.policyComponentCount} component`);

    expect(affectedComponentText).toBeVisible();
  });

  it('renders affected components', () => {
    renderComponent();
    const affectedComponentText = screen.getByText(`Affecting ${selectedReport.policyComponentCount} components`);

    expect(affectedComponentText).toBeVisible();
  });

  it('renders total artifact count', () => {
    renderComponent();
    const totalArtifactText = screen.getByText(`${selectedReport.totalArtifactCount} COMPONENTS`);

    expect(totalArtifactText).toBeVisible();
  });

  it('renders coverage percentage', () => {
    renderComponent();
    const coveragePercentageText = screen.getByText('50% of all components identified');

    expect(coveragePercentageText).toBeVisible();
  });

  it('renders empty coverage percentage', () => {
    selectedReport.knownArtifactCount = 0;
    selectedReport.totalArtifactCount = 0;

    renderComponent();
    const totalArtifactText = screen.getByText('0% of all components identified');

    expect(totalArtifactText).toBeVisible();
  });

  it('renders grandfathered count', () => {
    renderComponent();
    const grandfatheredCountText = screen.getByText(
      `${selectedReport.grandfatheredPolicyViolationCount} Grandfathered`
    );

    expect(grandfatheredCountText).toBeVisible();
  });
});
