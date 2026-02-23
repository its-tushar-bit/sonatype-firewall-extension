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
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import 'TestRoot/SpecUtil';

describe('EditPolicySummary', () => {
  let renderComponent;

  beforeEach(() => {
    renderComponent = () => render(<EditPolicySummary />);
  });

  it('focuses name input on load', () => {
    jest.spyOn(policySelectors, 'selectHasEditIqPermission').mockReturnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    expect(policyNameInput).toHaveFocus();
  });

  it('renders disabled inputs when there is no permission', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(true);
    jest.spyOn(policySelectors, 'selectIsInherited').mockReturnValue(false);
    jest.spyOn(policySelectors, 'selectHasEditIqPermission').mockReturnValue(false);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getByRole('button');
    const legacyViolationCheckbox = screen.getByRole('checkbox');
    const legacyViolationMessage = screen.queryByText(/Legacy Violations are not supported by your license/i);

    expect(legacyViolationMessage).toBeNull();

    expect(policyNameInput).toBeDisabled();
    expect(threatLevelInput).toHaveClass('disabled');
    expect(legacyViolationCheckbox).toBeDisabled();
  });

  it('renders disabled inputs when the policy is inherited', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(true);
    jest.spyOn(policySelectors, 'selectIsInherited').mockReturnValue(true);
    jest.spyOn(policySelectors, 'selectHasEditIqPermission').mockReturnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getByRole('button');
    const legacyViolationCheckbox = screen.getByRole('checkbox');
    const legacyViolationMessage = screen.queryByText(/Legacy Violations are not supported by your license/i);

    expect(legacyViolationMessage).toBeNull();

    expect(policyNameInput).toBeDisabled();
    expect(threatLevelInput).toHaveClass('disabled');
    expect(legacyViolationCheckbox).toBeDisabled();
  });

  it('renders disabled Legacy Violation Input when inherited is not supported', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(false);
    jest.spyOn(policySelectors, 'selectHasEditIqPermission').mockReturnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getAllByRole('button')[0];
    const legacyViolationCheckbox = screen.getByRole('checkbox');
    const legacyViolationMessage = screen.getByText(/Legacy Violations are not supported by your license/i);

    expect(legacyViolationMessage).toBeVisible();
    expect(policyNameInput).not.toBeDisabled();
    expect(threatLevelInput).not.toHaveClass('disabled');
    expect(legacyViolationCheckbox).toBeDisabled();
  });

  it('renders disabled inputs when SBOM Manager is enabled', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(true);
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);
    jest.spyOn(policySelectors, 'selectIsInherited').mockReturnValue(false);
    jest.spyOn(policySelectors, 'selectHasEditIqPermission').mockReturnValue(true);

    renderComponent();

    const policyNameInput = screen.getByRole('textbox');
    const threatLevelInput = screen.getByRole('button');
    const legacyViolationCheckbox = screen.queryByRole('checkbox');

    expect(policyNameInput).toBeDisabled();
    expect(threatLevelInput).toHaveClass('disabled');
    expect(legacyViolationCheckbox).not.toBeInTheDocument();
  });

  it('renders the legacy violations text', () => {
    jest.spyOn(productFeaturesSelectors, 'selectIsLegacyViolationSupported').mockReturnValue(true);

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

  it('hides the legacy violations text when is sbomManager', () => {
    jest.spyOn(routerSelectors, 'selectIsSbomManager').mockReturnValue(true);

    renderComponent();

    const legacyViolationTitle = screen.queryByText(/Legacy Violations/i);
    const legacyViolationSubtitle = screen.queryByText(
      /Eligible violations will be reported but will not trigger actions/i
    );
    const legacyViolationCheckboxText = screen.queryByText(
      /Allow violations of this policy to be granted legacy status/i
    );
    expect(legacyViolationTitle).not.toBeInTheDocument();
    expect(legacyViolationSubtitle).not.toBeInTheDocument();
    expect(legacyViolationCheckboxText).not.toBeInTheDocument();
  });
});
