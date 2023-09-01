/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';

import { render, screen } from 'TestRoot/SpecUtil';
import EditPolicySummary from 'MainRoot/OrgsAndPolicies/policyEditor/editPolicySummary/EditPolicySummary';
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

describe('EditPolicySummary', () => {
  let renderComponent;

  beforeEach(() => {
    renderComponent = () => render(<EditPolicySummary />);
  });

  it('focuses name input on load', () => {
    spyOn(policySelectors, 'selectHasEditIqPermission').and.returnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    expect(policyNameInput).toHaveFocus();
  });

  it('renders disabled inputs when there is no permission', () => {
    spyOn(productFeaturesSelectors, 'selectIsGrandfatheringSupported').and.returnValue(true);
    spyOn(policySelectors, 'selectIsInherited').and.returnValue(false);
    spyOn(policySelectors, 'selectHasEditIqPermission').and.returnValue(false);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getByRole('button');
    const policyViolationGrandfatheringInput = screen.getByRole('checkbox');
    const policyViolationGrandfatheringMessage = screen.queryByText(
      /Legacy Violations are not supported by your license/i
    );

    expect(policyViolationGrandfatheringMessage).toBeNull();

    expect(policyNameInput).toBeDisabled();
    expect(threatLevelInput).toHaveClassName('disabled');
    expect(policyViolationGrandfatheringInput).toBeDisabled();
  });

  it('renders disabled inputs when the policy is inherited', () => {
    spyOn(productFeaturesSelectors, 'selectIsGrandfatheringSupported').and.returnValue(true);
    spyOn(policySelectors, 'selectIsInherited').and.returnValue(true);
    spyOn(policySelectors, 'selectHasEditIqPermission').and.returnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getByRole('button');
    const policyViolationGrandfatheringInput = screen.getByRole('checkbox');
    const policyViolationGrandfatheringMessage = screen.queryByText(
      /Legacy Violations are not supported by your license/i
    );

    expect(policyViolationGrandfatheringMessage).toBeNull();

    expect(policyNameInput).toBeDisabled();
    expect(threatLevelInput).toHaveClassName('disabled');
    expect(policyViolationGrandfatheringInput).toBeDisabled();
  });

  it('renders disabled Grandfathering Input when inherited is not supported', () => {
    spyOn(productFeaturesSelectors, 'selectIsGrandfatheringSupported').and.returnValue(false);
    spyOn(policySelectors, 'selectHasEditIqPermission').and.returnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getAllByRole('button')[0];
    const policyViolationGrandfatheringInput = screen.getByRole('checkbox');
    const policyViolationGrandfatheringMessage = screen.getByText(
      /Legacy Violations are not supported by your license/i
    );

    expect(policyViolationGrandfatheringMessage).toBeVisible();
    expect(policyNameInput).not.toBeDisabled();
    expect(threatLevelInput).not.toHaveClassName('disabled');
    expect(policyViolationGrandfatheringInput).toBeDisabled();
  });

  it('renders the legacy violations text', () => {
    spyOn(productFeaturesSelectors, 'selectIsGrandfatheringSupported').and.returnValue(true);

    renderComponent();

    const legacyViolationTitle = screen.getByText(/Legacy Violations/i);
    const legacyViolationSubtitle = screen.getByText(
      /Eligible violations will be reported but will not trigger actions/i
    );
    const legacyViolationCheckboxText = screen.getByText(
      /Allow violations of this policy to be granted legacy status/i
    );
    expect(legacyViolationTitle).toBeVisible();
    expect(legacyViolationSubtitle).toBeVisible();
    expect(legacyViolationCheckboxText).toBeVisible();
  });
});
