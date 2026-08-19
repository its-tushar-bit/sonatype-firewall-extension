/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen } from 'TestRoot/SpecUtil';
import { RecommendedVersions } from 'MainRoot/componentDetails/overview/riskRemediation/RecommendedVersions';

describe('RecommendedVersionsComponent', () => {
  let minimalProps, renderComponent;

  beforeEach(function () {
    minimalProps = {
      actualVersion: '2.4.9',
      stageId: 'build',
      remediation: [],
      handleCompare: () => {},
    };

    renderComponent = (props) => render(<RecommendedVersions {...props} {...minimalProps} />);
  });

  it('renders a component', () => {
    renderComponent();
    expect(screen.getByTestId('iq-recommended-version')).toBeInTheDocument();
  });

  it("Title is 'Suggested Version Change'", () => {
    const remediation = [];
    renderComponent({ remediation });

    expect(screen.getByRole('heading', { name: /suggested version change/i })).toBeInTheDocument();
  });
});
