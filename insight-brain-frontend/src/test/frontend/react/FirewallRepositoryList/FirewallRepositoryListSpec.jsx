/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import FirewallRepositoryList from 'MainRoot/react/FirewallRepositoryList/FirewallRepositoryList';

let renderComponent, minimalProps, repositories, title, emptyMessage, onChange;

describe('FirewallRepositoryList', () => {
  const getCheckboxName = (name) => `firewall ${name} repository item`;

  beforeEach(function () {
    repositories = [
      { id: 1, publicId: 'repo1' },
      { id: 2, publicId: 'repo2' },
      { id: 3, publicId: 'repo3' },
    ];
    emptyMessage = 'No repositories available';
    title = 'My Repositories';
    onChange = () => {};
    minimalProps = { title, repositories, emptyMessage, onChange };
    renderComponent = (additionalProps = {}) =>
      render(<FirewallRepositoryList {...minimalProps} {...additionalProps} />);
  });

  it('renders the title and total number of repositories', () => {
    renderComponent();
    const titleElement = screen.getByText(title);
    const totalReposElement = screen.getByText(`0 of ${repositories.length}`);

    expect(titleElement).toBeInTheDocument();
    expect(totalReposElement).toBeInTheDocument();
  });

  it('renders empty message if no repositories are passed', () => {
    const repositories = [];
    renderComponent({ repositories });
    const emptyMessageElement = screen.getByText(emptyMessage);
    expect(emptyMessageElement).toBeInTheDocument();
  });

  it('selects/deselects all items when header checkbox is clicked', () => {
    renderComponent();
    const headerCheckbox = screen.getByRole('checkbox', { name: 'firewall repository list check all' });
    fireEvent.click(headerCheckbox);
    repositories.forEach((repo) => {
      const itemCheckbox = screen.getByRole('checkbox', { name: getCheckboxName(repo.publicId) });
      expect(itemCheckbox).toBeChecked();
    });
    fireEvent.click(headerCheckbox);
    repositories.forEach((repo) => {
      const itemCheckbox = screen.getByRole('checkbox', { name: getCheckboxName(repo.publicId) });
      expect(itemCheckbox).not.toBeChecked();
    });
  });

  it('selects/deselects individual items when item checkbox is clicked', () => {
    renderComponent();

    const [repo1, repo2, repo3] = repositories;
    const repo1Checkbox = screen.getByRole('checkbox', { name: getCheckboxName(repo1.publicId) });
    const repo2Checkbox = screen.getByRole('checkbox', { name: getCheckboxName(repo2.publicId) });
    const repo3Checkbox = screen.getByRole('checkbox', { name: getCheckboxName(repo3.publicId) });
    fireEvent.click(repo1Checkbox);
    expect(repo1Checkbox).toBeChecked();
    expect(repo2Checkbox).not.toBeChecked();
    expect(repo3Checkbox).not.toBeChecked();
    fireEvent.click(repo2Checkbox);
    expect(repo1Checkbox).toBeChecked();
    expect(repo2Checkbox).toBeChecked();
    expect(repo3Checkbox).not.toBeChecked();
    fireEvent.click(repo1Checkbox);
    fireEvent.click(repo2Checkbox);
    fireEvent.click(repo3Checkbox);
    expect(repo1Checkbox).not.toBeChecked();
    expect(repo2Checkbox).not.toBeChecked();
    expect(repo3Checkbox).toBeChecked();
  });

  it('should sort items asc first and then desc by name when name cell is clicked twice', () => {
    renderComponent();

    const nameCell = screen.getByText('name');

    fireEvent.click(nameCell);

    let items = screen.getAllByRole('row', { name: /repository item/i });
    expect(items[0]).toHaveTextContent(repositories[0].publicId);
    expect(items[1]).toHaveTextContent(repositories[1].publicId);
    expect(items[2]).toHaveTextContent(repositories[2].publicId);

    fireEvent.click(nameCell);

    items = screen.getAllByRole('row', { name: /repository item/i });
    expect(items[0]).toHaveTextContent(repositories[2].publicId);
    expect(items[1]).toHaveTextContent(repositories[1].publicId);
    expect(items[2]).toHaveTextContent(repositories[0].publicId);
  });

  it('adds an item to selected items when checkbox is clicked', () => {
    const onChange = jasmine.createSpy('onChange');
    renderComponent({ onChange });

    let totalReposElement = screen.getByText(`0 of ${repositories.length}`);
    expect(totalReposElement).toBeInTheDocument();

    const checkbox = screen.getByRole('checkbox', { name: getCheckboxName(repositories[0].publicId) });
    fireEvent.click(checkbox);

    expect(checkbox).toBeChecked();
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith([repositories[0]]);

    totalReposElement = screen.getByText(`1 of ${repositories.length}`);
    expect(totalReposElement).toBeInTheDocument();
  });

  it('removes an item from selected items when checkbox is clicked again', () => {
    const selectedRepositories = [repositories[0]];
    const onChange = jasmine.createSpy('onChange');
    renderComponent({ onChange, selectedRepositories });

    let totalReposElement = screen.getByText(`1 of ${repositories.length}`);
    expect(totalReposElement).toBeInTheDocument();

    const checkbox = screen.getByRole('checkbox', { name: getCheckboxName(repositories[0].publicId) });
    fireEvent.click(checkbox);

    expect(checkbox).not.toBeChecked();
    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenCalledWith([]);

    totalReposElement = screen.getByText(`0 of ${repositories.length}`);
    expect(totalReposElement).toBeInTheDocument();
  });
});
