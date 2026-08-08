/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import userEvent from '@testing-library/user-event';
import { axiosMockAdapter, render, screen, waitFor } from 'TestRoot/SpecUtil';
import AddProxyRepositoryModal from 'MainRoot/firewall/iqProxy/AddProxyRepositoryModal';
import { getAddRepositoryUrl } from 'MainRoot/util/CLMLocation';

describe('AddProxyRepositoryModal (FIRE-665)', () => {
  const MANAGER_ID = 'vrm-1';
  let axiosMock, onClose;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  beforeEach(() => {
    axiosMock.reset();
    onClose = jest.fn();
  });

  const renderModal = () => render(<AddProxyRepositoryModal managerId={MANAGER_ID} onClose={onClose} />);

  const typeName = async (user, value) => user.type(screen.getByPlaceholderText('Repository name'), value);
  const typeUpstream = async (user, value) => user.type(screen.getByPlaceholderText('Upstream repository URL'), value);
  const selectFormat = async (user, formatValue) => user.selectOptions(screen.getByRole('combobox'), formatValue);

  it('renders the FIRE-665 header', () => {
    renderModal();
    expect(screen.getByRole('heading', { name: 'Add Proxy Repository' })).toBeVisible();
  });

  describe('per-format field visibility (AC matrix)', () => {
    it('Maven → no PCCS, no Package host URL, no NuGet protocol', async () => {
      const user = userEvent.setup();
      renderModal();

      await selectFormat(user, 'maven2');

      expect(screen.queryByPlaceholderText('Hosted repository URL')).toBeNull();
      expect(screen.queryByText(/Enable PCCS/i)).toBeNull();
      expect(screen.queryByText('Protocol version')).toBeNull();
    });

    it('NuGet → protocol version radios, no PCCS, no Package host URL', async () => {
      const user = userEvent.setup();
      renderModal();

      await selectFormat(user, 'nuget');

      expect(screen.getByText('Protocol version')).toBeVisible();
      expect(screen.getByRole('radio', { name: 'Nuget V2' })).toBeInTheDocument();
      expect(screen.getByRole('radio', { name: 'Nuget V3' })).toBeInTheDocument();
      expect(screen.queryByPlaceholderText('Hosted repository URL')).toBeNull();
      expect(screen.queryByText(/Enable PCCS/i)).toBeNull();
    });

    it('npm → PCCS visible, no Package host URL', async () => {
      const user = userEvent.setup();
      renderModal();

      await selectFormat(user, 'npm');

      expect(screen.getByLabelText(/Enable PCCS for this proxy repository/i)).toBeVisible();
      expect(screen.queryByPlaceholderText('Hosted repository URL')).toBeNull();
    });

    it('PyPI → PCCS visible AND Package host URL visible', async () => {
      const user = userEvent.setup();
      renderModal();

      await selectFormat(user, 'pypi');

      expect(screen.getByLabelText(/Enable PCCS for this proxy repository/i)).toBeVisible();
      expect(screen.getByPlaceholderText('Package host URL')).toBeVisible();
    });
  });

  it('renders the verbatim PCCS tooltip on the info icon', async () => {
    const user = userEvent.setup();
    renderModal();

    await selectFormat(user, 'npm');
    // The info icon lives next to the "Enable PCCS" fieldset label.
    const pccsLabel = screen.getByText('Enable PCCS').closest('legend, label, span, div');
    const iconContainer = pccsLabel.querySelector('.iq-firewall-proxy-form__info-icon');
    await user.hover(iconContainer);
    expect(
      await screen.findByText(
        'PCCS — Quarantine, plus metadata filtering to help clients select a policy compliance version.'
      )
    ).toBeInTheDocument();
  });

  it('POSTs the correct DTO for a Maven happy path and opens the "Repository Created" success modal', async () => {
    const user = userEvent.setup();
    const created = {
      repositoryId: 'r1',
      publicId: 'maven-central',
      format: 'maven2',
      upstreamUrl: 'https://repo1.maven.org/maven2/',
      proxyUrl: 'https://iq.example/api/v2/firewall/enterprise/instance-1/maven-central/maven2',
    };
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(200, created);

    renderModal();

    await typeName(user, 'maven-central');
    await selectFormat(user, 'maven2');
    await typeUpstream(user, 'https://repo1.maven.org/maven2/');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(axiosMock.history.post).toHaveLength(1));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      publicId: 'maven-central',
      format: 'maven2',
      upstreamUrl: 'https://repo1.maven.org/maven2/',
    });
    // The success modal appears, populated with the server-computed proxyUrl,
    // and onClose is deferred until the user clicks Close/Copy.
    expect(await screen.findByRole('heading', { name: 'Repository Created' })).toBeVisible();
    expect(screen.getByText('Repository created successfully.')).toBeVisible();
    expect(screen.getByText(created.proxyUrl)).toBeVisible();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('closes the success modal via the Close button and reports the created repo to the parent', async () => {
    const user = userEvent.setup();
    const created = {
      repositoryId: 'r1',
      publicId: 'maven-central',
      format: 'maven2',
      upstreamUrl: 'https://repo1.maven.org/maven2/',
      proxyUrl: 'https://iq.example/api/v2/firewall/enterprise/instance-1/maven-central/maven2',
    };
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(200, created);

    renderModal();

    await typeName(user, 'maven-central');
    await selectFormat(user, 'maven2');
    await typeUpstream(user, 'https://repo1.maven.org/maven2/');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await screen.findByRole('heading', { name: 'Repository Created' });
    await user.click(screen.getByRole('button', { name: 'Close' }));

    expect(onClose).toHaveBeenCalledWith(created);
  });

  it('copies the proxyUrl to the clipboard from the success modal and swaps the label to "Copied!"', async () => {
    const user = userEvent.setup();
    const writeText = jest.fn().mockResolvedValue(undefined);
    Object.defineProperty(navigator, 'clipboard', { value: { writeText }, configurable: true });
    const created = {
      repositoryId: 'r1',
      publicId: 'maven-central',
      format: 'maven2',
      upstreamUrl: 'https://repo1.maven.org/maven2/',
      proxyUrl: 'https://iq.example/api/v2/firewall/enterprise/instance-1/maven-central/maven2',
    };
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(200, created);

    renderModal();

    await typeName(user, 'maven-central');
    await selectFormat(user, 'maven2');
    await typeUpstream(user, 'https://repo1.maven.org/maven2/');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await screen.findByRole('heading', { name: 'Repository Created' });
    await user.click(screen.getByRole('button', { name: 'Copy URL' }));

    expect(writeText).toHaveBeenCalledWith(created.proxyUrl);
    expect(await screen.findByRole('button', { name: 'Copied!' })).toBeVisible();
  });

  it('POSTs pccsEnabled and packageHostUrl for a PyPI happy path', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(200, { repositoryId: 'r1' });

    renderModal();

    await typeName(user, 'pypi-mirror');
    await selectFormat(user, 'pypi');
    await typeUpstream(user, 'https://pypi.org');
    // packageHostUrl has a sensible default; toggle PCCS on
    await user.click(screen.getByLabelText(/Enable PCCS for this proxy repository/i));
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(axiosMock.history.post).toHaveLength(1));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      publicId: 'pypi-mirror',
      format: 'pypi',
      upstreamUrl: 'https://pypi.org',
      packageHostUrl: 'https://files.pythonhosted.org',
      pccsEnabled: true,
    });
  });

  it('POSTs protocolVersion for a NuGet happy path', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(200, { repositoryId: 'r1' });

    renderModal();

    await typeName(user, 'nuget-mirror');
    await selectFormat(user, 'nuget');
    await typeUpstream(user, 'https://api.nuget.org/v3/index.json');
    // v3 is the default; select v2 to prove the field flows through
    await user.click(screen.getByRole('radio', { name: 'Nuget V2' }));
    await user.click(screen.getByRole('button', { name: 'Save' }));

    await waitFor(() => expect(axiosMock.history.post).toHaveLength(1));
    expect(JSON.parse(axiosMock.history.post[0].data)).toEqual({
      publicId: 'nuget-mirror',
      format: 'nuget',
      upstreamUrl: 'https://api.nuget.org/v3/index.json',
      protocolVersion: 'v2',
    });
  });

  it('surfaces the type-scoped duplicate-name red banner on 409, keeps Save visible, no Retry', async () => {
    const user = userEvent.setup();
    axiosMock.onPost(getAddRepositoryUrl(MANAGER_ID)).reply(409, 'already exists');

    renderModal();

    await typeName(user, 'maven-central');
    await selectFormat(user, 'maven2');
    await typeUpstream(user, 'https://repo1.maven.org/maven2/');
    await user.click(screen.getByRole('button', { name: 'Save' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent(
      `A proxy repository named 'maven-central' already exists in this Virtual Repository Manager.`
    );

    expect(screen.getByRole('button', { name: 'Save' })).toBeVisible();
    expect(screen.queryByRole('button', { name: /retry/i })).toBeNull();
    expect(onClose).not.toHaveBeenCalled();
  });

  it('calls onClose with null when Cancel is clicked', async () => {
    const user = userEvent.setup();
    renderModal();

    await user.click(screen.getByRole('button', { name: 'Cancel' }));

    expect(onClose).toHaveBeenCalledWith(null);
  });
});
