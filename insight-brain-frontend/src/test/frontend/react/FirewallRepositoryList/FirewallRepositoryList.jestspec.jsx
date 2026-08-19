/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent, within } from 'TestRoot/SpecUtil';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';

let renderComponent, repositories, title, onChangeMock, minimalProps;
describe('FirewallRepositoryList', () => {
  beforeEach(() => {
    repositories = [
      {
        id: '1',
        repositoryManagerId: 'rm1',
        publicId: 'public1',
        repositoryType: 'type1',
        enabled: true,
        quarantineEnabled: true,
        policyCompliantComponentSelectionEnabled: false,
        namespaceConfusionProtectionEnabled: true,
        format: 'format1',
      },
      {
        id: '2',
        repositoryManagerId: 'rm2',
        publicId: 'public2',
        repositoryType: 'type2',
        enabled: false,
        quarantineEnabled: false,
        policyCompliantComponentSelectionEnabled: true,
        namespaceConfusionProtectionEnabled: false,
        format: 'format2',
      },
      {
        id: '3',
        repositoryManagerId: 'rm3',
        publicId: 'public3',
        repositoryType: 'type3',
        enabled: true,
        quarantineEnabled: true,
        policyCompliantComponentSelectionEnabled: true,
        namespaceConfusionProtectionEnabled: false,
        format: 'format3',
      },
    ];
    title = 'Test Repositories';
    onChangeMock = jest.fn();
    minimalProps = {
      title,
      repositories,
      onChange: onChangeMock,
    };
    renderComponent = (additionalProps = {}) =>
      render(<FirewallRepositoryList {...minimalProps} {...additionalProps} />);
  });

  it('renders the title', () => {
    renderComponent();
    expect(screen.getByText('Test Repositories')).toBeInTheDocument();
  });

  it('renders the repository items', () => {
    renderComponent();
    expect(screen.getByText('public1')).toBeInTheDocument();
    expect(screen.getByText('public2')).toBeInTheDocument();
    expect(screen.getByText('public3')).toBeInTheDocument();
  });

  it('renders unsupported repository formats with disabled checkbox', () => {
    renderComponent({ supportedFormats: ['format1', 'format3'] });
    expect(screen.getByText('public2')).toBeInTheDocument();
    expect(screen.getByText('format2')).toBeInTheDocument();
    expect(screen.getByRole('checkbox', { name: 'firewall public2 repository item' })).toBeDisabled();
  });

  it('renders unsupported repository tooltip', async function () {
    renderComponent({ supportedFormats: ['format1', 'format3'] });
    const disabledIcon = screen.getByTestId('repo-disabled-icon');

    fireEvent.mouseOver(disabledIcon);

    const tooltip = await screen.findByRole('tooltip');

    expect(
      within(tooltip).getByText(
        "This repository with format 'format2' is not supported by the Firewall. Please contact your administrator for more information."
      )
    ).toBeInTheDocument();
  });

  it('displays the empty message when there are no repositories', () => {
    const props = {
      title: 'Test Repositories',
      repositories: [],
      emptyMessage: 'No list available',
    };
    renderComponent(props);

    expect(screen.getByText('No list available')).toBeInTheDocument();
  });

  it('calls onChange with all items set to true when "Select All" checkbox is clicked and there is at least one item in false', () => {
    renderComponent();
    const selectAllCheckbox = screen.getByLabelText('firewall repository list check all');

    fireEvent.click(selectAllCheckbox);

    expect(onChangeMock).toHaveBeenCalledWith([
      { id: '1', key: 'quarantineEnabled', value: true },
      { id: '2', key: 'quarantineEnabled', value: true },
      { id: '3', key: 'quarantineEnabled', value: true },
    ]);
  });

  it('calls onChange with all items set to false when "Select All" checkbox is clicked and all items are set to true', () => {
    repositories[0].quarantineEnabled = true;
    repositories[1].quarantineEnabled = true;
    repositories[2].quarantineEnabled = true;
    renderComponent({ repositories });

    const selectAllCheckbox = screen.getByLabelText('firewall repository list check all');

    fireEvent.click(selectAllCheckbox);

    expect(onChangeMock).toHaveBeenCalledWith([
      { id: '1', key: 'quarantineEnabled', value: false },
      { id: '2', key: 'quarantineEnabled', value: false },
      { id: '3', key: 'quarantineEnabled', value: false },
    ]);
  });

  it('calls onChange with the clicked item when an item checkbox is clicked', () => {
    renderComponent();
    const itemCheckbox = screen.getByLabelText('firewall public1 repository item');

    fireEvent.click(itemCheckbox);

    expect(onChangeMock).toHaveBeenCalledWith([{ id: '1', key: 'quarantineEnabled', value: false }]);
  });

  it('calls onChange for all items when an item checkbox is clicked', () => {
    renderComponent();
    expect(screen.getByText('2 of 3')).toBeInTheDocument();

    const headerCheckbox = screen.getByRole('checkbox', { name: 'firewall repository list check all' });
    fireEvent.click(headerCheckbox);

    expect(onChangeMock).toHaveBeenCalledWith([
      { id: '1', key: 'quarantineEnabled', value: true },
      { id: '2', key: 'quarantineEnabled', value: true },
      { id: '3', key: 'quarantineEnabled', value: true },
    ]);
  });

  it('sorts items by name in ascending order when header cell is clicked once', () => {
    renderComponent();
    const nameCell = screen.getByText('name');

    fireEvent.click(nameCell);

    const sortItems = screen.getAllByRole('row', { name: /repository item/i });
    expect(sortItems[0]).toHaveTextContent(repositories[0].publicId);
    expect(sortItems[1]).toHaveTextContent(repositories[1].publicId);
    expect(sortItems[2]).toHaveTextContent(repositories[2].publicId);
  });

  it('sorts items by name in descending order when header cell is clicked twice', () => {
    renderComponent();
    const nameHeaderCell = screen.getByText('name');

    fireEvent.click(nameHeaderCell);
    fireEvent.click(nameHeaderCell);

    const sortItems = screen.getAllByRole('row', { name: /repository item/i });
    expect(sortItems[0]).toHaveTextContent(repositories[2].publicId);
    expect(sortItems[1]).toHaveTextContent(repositories[1].publicId);
    expect(sortItems[2]).toHaveTextContent(repositories[0].publicId);
  });

  it('does not sort items when there is only one item', () => {
    const props = {
      repositories: [repositories[0]],
    };
    renderComponent(props);

    const nameCell = screen.getByRole('checkbox', { name: 'firewall repository list check all' });
    fireEvent.click(nameCell);

    const item = screen.getByText('public1');
    expect(item).toBeInTheDocument();
  });
});
