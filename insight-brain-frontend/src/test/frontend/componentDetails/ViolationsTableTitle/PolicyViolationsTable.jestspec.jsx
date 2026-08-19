/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { render, screen } from 'TestRoot/SpecUtil';
import React from 'react';
import PolicyViolationsTable from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsTable';

describe('PolicyViolationsTable', () => {
  const defaultState = {
    toggleShowViolationsDetailPopover: jest.fn(),
    setViolationsDetailRowClicked: jest.fn(),
    loadPolicyViolationsInformation: jest.fn(),
    showComponentWaiversPopover: false,
    toggleComponentWaiversPopover: jest.fn(),
    setSelectedPolicyViolationId: jest.fn(),
    setWaiverToDelete: jest.fn(),
  };

  const renderComponent = (state) => {
    render(<PolicyViolationsTable {...state} />);
  };

  it('should render all headers', () => {
    renderComponent(defaultState);

    expect(screen.getByRole('columnheader', { name: /reachability/i })).toBeInTheDocument();
    validateCommonColumns();
  });

  it('should render all headers except for reachability', () => {
    const state = {
      ...defaultState,
      isLegalTab: true,
    };

    renderComponent(state);

    expect(screen.queryByRole('columnheader', { name: /reachability/i })).not.toBeInTheDocument();
    validateCommonColumns();
  });

  const validateCommonColumns = () => {
    expect(screen.getByRole('columnheader', { name: /threat/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /policy\/action/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /constraint name/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /condition/i })).toBeInTheDocument();
    expect(screen.getByRole('columnheader', { name: /status/i })).toBeInTheDocument();
  };
});
