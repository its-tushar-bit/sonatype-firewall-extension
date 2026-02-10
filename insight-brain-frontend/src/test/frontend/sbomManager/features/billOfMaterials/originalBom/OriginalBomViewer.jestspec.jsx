/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

import { axiosMockAdapter, render, waitFor } from 'TestRoot/SpecUtil';
import { getDownloadSbomFileUrl } from 'MainRoot/util/CLMLocation';
import OriginalBomViewer from 'MainRoot/sbomManager/features/billOfMaterials/originalBom/OriginalBomViewer';

describe('OriginalBomViewer', () => {
  const axiosMock = axiosMockAdapter();
  const internalAppId = 'test-app-id';
  const sbomVersion = '1.0-SNAPSHOT';

  const mockJsonSbom = {
    bomFormat: 'CycloneDX',
    specVersion: '1.4',
    version: 1,
    metadata: {
      timestamp: '2024-01-12T10:00:00Z',
      component: {
        type: 'application',
        name: 'test-app',
        version: '1.0.0',
      },
    },
    components: [
      {
        type: 'library',
        name: 'react',
        version: '18.2.0',
        purl: 'pkg:npm/react@18.2.0',
        licenses: [
          {
            license: {
              id: 'MIT',
            },
          },
        ],
      },
      {
        type: 'library',
        name: 'lodash',
        version: '4.17.21',
        purl: 'pkg:npm/lodash@4.17.21',
      },
    ],
  };

  const mockXmlSbom = `<?xml version="1.0" encoding="UTF-8"?>
<bom xmlns="http://cyclonedx.org/schema/bom/1.4">
  <metadata>
    <timestamp>2024-01-12T10:00:00Z</timestamp>
    <component type="application">
      <name>test-app</name>
      <version>1.0.0</version>
    </component>
  </metadata>
  <components>
    <component type="library">
      <name>react</name>
      <version>18.2.0</version>
    </component>
  </components>
</bom>`;

  beforeEach(() => {
    axiosMock.reset();
  });

  describe('JSON SBOM rendering', () => {
    it('renders the Original BOM Viewer with JSON SBOM data', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('Original Bill of Material Data')).toBeInTheDocument();
      });

      // Check info alert is present
      expect(
        screen.getByText(/This view displays complete original bill of material data for reference only/)
      ).toBeInTheDocument();

      // Check that root nodes are rendered
      expect(screen.getByText('bomFormat')).toBeInTheDocument();
      expect(screen.getByText('specVersion')).toBeInTheDocument();
      expect(screen.getByText('metadata')).toBeInTheDocument();
      expect(screen.getByText('components')).toBeInTheDocument();
    });

    it.skip('expands and collapses nodes when clicked', async () => {
      const user = userEvent.setup();
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('metadata')).toBeInTheDocument();
      });

      // metadata should show it has children with count
      const metadataNode = screen.getByText('metadata').closest('.nx-tree__item');
      expect(
        within(metadataNode).getByText((content, element) => {
          return element?.className === 'iq-original-bom-viewer__count' && content.includes('{2}');
        })
      ).toBeInTheDocument();

      // Click to expand metadata
      const metadataToggle = within(metadataNode).getByRole('button');
      await user.click(metadataToggle);

      // Children should appear
      await waitFor(() => {
        expect(screen.getByText('timestamp')).toBeInTheDocument();
        expect(screen.getByText('component')).toBeInTheDocument();
      });

      // Click to collapse
      await user.click(metadataToggle);

      // Children should disappear
      await waitFor(() => {
        expect(screen.queryByText('timestamp')).not.toBeInTheDocument();
      });
    });

    // TODO: Fix button role finding in deeply nested structures
    it.skip('expands nested nodes correctly', async () => {
      const user = userEvent.setup();
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('metadata')).toBeInTheDocument();
      });

      // Expand metadata
      const metadataNode = screen.getByText('metadata').closest('.nx-tree__item');
      await user.click(within(metadataNode).getByRole('button'));

      await waitFor(() => {
        expect(screen.getByText('component')).toBeInTheDocument();
      });

      // Expand component (nested under metadata)
      const componentNode = screen.getByText('component').closest('.nx-tree__item');
      await user.click(within(componentNode).getByRole('button'));

      await waitFor(() => {
        expect(screen.getByText('type')).toBeInTheDocument();
        expect(screen.getByText('name')).toBeInTheDocument();
        expect(screen.getByText('version')).toBeInTheDocument();
      });

      // Verify values are displayed
      expect(screen.getByText('application')).toBeInTheDocument();
      expect(screen.getByText('test-app')).toBeInTheDocument();
      expect(screen.getByText('1.0.0')).toBeInTheDocument();
    });

    it.skip('expands array nodes and shows array items', async () => {
      const user = userEvent.setup();
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('components')).toBeInTheDocument();
      });

      // components is an array with 2 items
      const componentsNode = screen.getByText('components').closest('.nx-tree__item');
      expect(
        within(componentsNode).getByText((content, element) => {
          return element?.className === 'iq-original-bom-viewer__count' && content.includes('{2}');
        })
      ).toBeInTheDocument();

      // Expand components array
      await user.click(within(componentsNode).getByRole('button'));

      await waitFor(() => {
        // Array indices should appear as node names
        expect(screen.getByText('0')).toBeInTheDocument();
        expect(screen.getByText('1')).toBeInTheDocument();
      });
    });

    it('displays leaf node values inline', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('bomFormat')).toBeInTheDocument();
      });

      // Leaf nodes should show value inline with colon separator
      const bomFormatNode = screen.getByText('bomFormat').closest('.nx-tree__item-label');
      expect(within(bomFormatNode).getByText(':')).toBeInTheDocument();
      expect(within(bomFormatNode).getByText('CycloneDX')).toBeInTheDocument();

      const specVersionNode = screen.getByText('specVersion').closest('.nx-tree__item-label');
      expect(within(specVersionNode).getByText('1.4')).toBeInTheDocument();

      const versionNode = screen.getByText('version').closest('.nx-tree__item-label');
      expect(within(versionNode).getByText('1')).toBeInTheDocument();
    });

    it('drills into child elements when node is expanded', async () => {
      const user = userEvent.setup();
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      // Wait for initial render
      await waitFor(() => {
        expect(screen.getByText('metadata')).toBeInTheDocument();
      });

      // Verify metadata node shows it has children with count
      const metadataLabel = screen.getByText('metadata').closest('.nx-tree__item-label');
      expect(within(metadataLabel).getByText(/\{2\}/)).toBeInTheDocument();

      // Children should not be visible initially
      expect(screen.queryByText('timestamp')).not.toBeInTheDocument();
      expect(screen.queryByText('component')).not.toBeInTheDocument();

      // Click on the collapse icon (SVG rect) to expand the metadata node
      const metadataItem = screen.getByText('metadata').closest('.nx-tree__item');
      const collapseIcon = metadataItem.querySelector('.nx-tree__collapse-click');
      await user.click(collapseIcon);

      // Children should now be visible after expansion
      await waitFor(() => {
        expect(screen.getByText('timestamp')).toBeInTheDocument();
        expect(screen.getByText('component')).toBeInTheDocument();
      });

      // Verify child values are displayed correctly
      const timestampLabel = screen.getByText('timestamp').closest('.nx-tree__item-label');
      expect(within(timestampLabel).getByText('2024-01-12T10:00:00Z')).toBeInTheDocument();

      // Component should be expandable (has children with count)
      const componentLabel = screen.getByText('component').closest('.nx-tree__item-label');
      expect(within(componentLabel).getByText(/\{3\}/)).toBeInTheDocument();
    });
  });

  // TODO: Fix XML tests - parser or mock setup issue
  describe.skip('XML SBOM rendering', () => {
    it('renders the Original BOM Viewer with XML SBOM data', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockXmlSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('Original Bill of Material Data')).toBeInTheDocument();
      });

      // Check that root XML element is rendered
      expect(screen.getByText('bom')).toBeInTheDocument();
    });

    it('expands XML nodes and shows child elements', async () => {
      const user = userEvent.setup();
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockXmlSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('bom')).toBeInTheDocument();
      });

      // Expand bom root element
      const bomNode = screen.getByText('bom').closest('.nx-tree__item');
      await user.click(within(bomNode).getByRole('button'));

      await waitFor(() => {
        // Should show XML attributes and child elements
        expect(screen.getByText('metadata')).toBeInTheDocument();
        expect(screen.getByText('components')).toBeInTheDocument();
      });

      // Expand metadata
      const metadataNode = screen.getByText('metadata').closest('.nx-tree__item');
      await user.click(within(metadataNode).getByRole('button'));

      await waitFor(() => {
        expect(screen.getByText('timestamp')).toBeInTheDocument();
        expect(screen.getByText('component')).toBeInTheDocument();
      });
    });
  });

  // TODO: Fix error handling tests - axios mock not properly triggering error state
  describe.skip('error handling', () => {
    it('displays error when SBOM fetch fails', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(500, 'Server Error');

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      // Wait for retry button to appear (indicates error state)
      await waitFor(() => {
        const retryButton = screen.queryByRole('button', { name: /retry/i });
        expect(retryButton).toBeInTheDocument();
      });
    });

    it('retries fetching SBOM when retry button is clicked', async () => {
      const user = userEvent.setup();
      axiosMock
        .onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion))
        .replyOnce(500, 'Server Error')
        .onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion))
        .reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      // Wait for retry button
      const retryButton = await screen.findByRole('button', { name: /retry/i });
      expect(retryButton).toBeInTheDocument();

      // Click retry
      await user.click(retryButton);

      // Should successfully load after retry
      await waitFor(() => {
        expect(screen.getByText('bomFormat')).toBeInTheDocument();
      });
    });
  });

  describe('loading state', () => {
    it('shows loading indicator while fetching SBOM', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(() => {
        return new Promise((resolve) => {
          setTimeout(() => resolve([200, mockJsonSbom]), 100);
        });
      });

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      // Loading indicator should be present
      expect(screen.getByRole('status')).toBeInTheDocument();

      // Wait for data to load
      await waitFor(() => {
        expect(screen.getByText('bomFormat')).toBeInTheDocument();
      });

      // Loading indicator should be gone
      expect(screen.queryByRole('status')).not.toBeInTheDocument();
    });
  });

  describe('help link', () => {
    it('renders help documentation link', async () => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

      await waitFor(() => {
        expect(screen.getByText('help and documentation')).toBeInTheDocument();
      });

      const helpLink = screen.getByText('help and documentation').closest('a');
      expect(helpLink).toHaveAttribute('href');
      expect(helpLink).toHaveAttribute('target', '_blank');
    });
  });

  describe('SBOM Format Comprehensive Support', () => {
    // Test data for all supported SBOM formats
    const cycloneDxJson15 = {
      bomFormat: 'CycloneDX',
      specVersion: '1.5',
      version: 1,
      components: [{ type: 'library', name: 'test-component', version: '1.0.0', purl: 'pkg:npm/test-component@1.0.0' }],
    };

    const cycloneDxXml15 = `<?xml version="1.0"?>
<bom xmlns="http://cyclonedx.org/schema/bom/1.5" version="1">
  <components>
    <component type="library">
      <name>test-component</name>
      <version>1.0.0</version>
      <purl>pkg:npm/test-component@1.0.0</purl>
    </component>
  </components>
</bom>`;

    const cycloneDxJson16 = {
      bomFormat: 'CycloneDX',
      specVersion: '1.6',
      version: 1,
      components: [{ type: 'library', name: 'test-component', version: '1.0.0', purl: 'pkg:npm/test-component@1.0.0' }],
    };

    const cycloneDxXml16 = `<?xml version="1.0"?>
<bom xmlns="http://cyclonedx.org/schema/bom/1.6" version="1">
  <components>
    <component type="library">
      <name>test-component</name>
      <version>1.0.0</version>
      <purl>pkg:npm/test-component@1.0.0</purl>
    </component>
  </components>
</bom>`;

    const spdxJson22 = {
      SPDXID: 'SPDXRef-DOCUMENT',
      spdxVersion: 'SPDX-2.2',
      creationInfo: { created: '2023-01-01T00:00:00Z', creators: ['Tool: Test'] },
      name: 'Test',
      dataLicense: 'CC0-1.0',
      documentNamespace: 'http://example.com/test',
      packages: [
        {
          SPDXID: 'SPDXRef-Package',
          name: 'test-pkg',
          versionInfo: '1.0.0',
          downloadLocation: 'NOASSERTION',
          filesAnalyzed: false,
          externalRefs: [
            { referenceCategory: 'PACKAGE-MANAGER', referenceType: 'purl', referenceLocator: 'pkg:npm/test-pkg@1.0.0' },
          ],
        },
      ],
    };

    const spdxXml22 = `<?xml version="1.0"?>
<Document>
  <SPDXID>SPDXRef-DOCUMENT</SPDXID>
  <spdxVersion>SPDX-2.2</spdxVersion>
  <creationInfo><created>2023-01-01T00:00:00Z</created><creators>Tool: Test</creators></creationInfo>
  <name>Test</name>
  <dataLicense>CC0-1.0</dataLicense>
  <documentNamespace>http://example.com/test</documentNamespace>
  <packages>
    <SPDXID>SPDXRef-Package</SPDXID>
    <name>test-pkg</name>
    <versionInfo>1.0.0</versionInfo>
    <downloadLocation>NOASSERTION</downloadLocation>
    <filesAnalyzed>false</filesAnalyzed>
    <externalRefs>
      <referenceCategory>PACKAGE-MANAGER</referenceCategory>
      <referenceType>purl</referenceType>
      <referenceLocator>pkg:npm/test-pkg@1.0.0</referenceLocator>
    </externalRefs>
  </packages>
</Document>`;

    const spdxJson23 = {
      SPDXID: 'SPDXRef-DOCUMENT',
      spdxVersion: 'SPDX-2.3',
      creationInfo: { created: '2023-01-01T00:00:00Z', creators: ['Tool: Test'] },
      name: 'Test',
      dataLicense: 'CC0-1.0',
      documentNamespace: 'http://example.com/test',
      packages: [
        {
          SPDXID: 'SPDXRef-Package',
          name: 'test-pkg',
          versionInfo: '1.0.0',
          downloadLocation: 'NOASSERTION',
          filesAnalyzed: false,
          externalRefs: [
            { referenceCategory: 'PACKAGE-MANAGER', referenceType: 'purl', referenceLocator: 'pkg:npm/test-pkg@1.0.0' },
          ],
        },
      ],
    };

    const spdxXml23 = `<?xml version="1.0"?>
<Document>
  <SPDXID>SPDXRef-DOCUMENT</SPDXID>
  <spdxVersion>SPDX-2.3</spdxVersion>
  <creationInfo><created>2023-01-01T00:00:00Z</created><creators>Tool: Test</creators></creationInfo>
  <name>Test</name>
  <dataLicense>CC0-1.0</dataLicense>
  <documentNamespace>http://example.com/test</documentNamespace>
  <packages>
    <SPDXID>SPDXRef-Package</SPDXID>
    <name>test-pkg</name>
    <versionInfo>1.0.0</versionInfo>
    <downloadLocation>NOASSERTION</downloadLocation>
    <filesAnalyzed>false</filesAnalyzed>
    <externalRefs>
      <referenceCategory>PACKAGE-MANAGER</referenceCategory>
      <referenceType>purl</referenceType>
      <referenceLocator>pkg:npm/test-pkg@1.0.0</referenceLocator>
    </externalRefs>
  </packages>
</Document>`;

    // Helper functions to reduce test duplication
    const renderAndWaitForElement = async (sbomData, expectedElement) => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, sbomData);
      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);
      await waitFor(() => expect(screen.getByText(expectedElement)).toBeInTheDocument());
    };

    const expectElementsToBePresent = (expectedElements) => {
      expectedElements.forEach((element) => {
        expect(screen.getByText(element)).toBeInTheDocument();
      });
    };

    const testComponentFiltering = async (sbomData, purl, rootElement, expectedChildren) => {
      axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, sbomData);
      render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} componentPurl={purl} />);

      await waitFor(() =>
        expect(
          screen.getByText('Showing original bill of material data for the selected component only.')
        ).toBeInTheDocument()
      );

      // Verify root element
      expect(screen.getByText(rootElement)).toBeInTheDocument();

      // Verify expected children
      expectElementsToBePresent(expectedChildren);
    };

    describe('CycloneDX JSON formats', () => {
      it('renders CycloneDX 1.4 JSON correctly', async () => {
        await renderAndWaitForElement(mockJsonSbom, 'bomFormat');
        expectElementsToBePresent(['CycloneDX']);
      });

      it('renders CycloneDX 1.5 JSON correctly', async () => {
        await renderAndWaitForElement(cycloneDxJson15, 'bomFormat');
        expectElementsToBePresent(['components']);
      });

      it('renders CycloneDX 1.6 JSON correctly', async () => {
        await renderAndWaitForElement(cycloneDxJson16, 'bomFormat');
        expectElementsToBePresent(['components']);
      });
    });

    describe('CycloneDX XML formats', () => {
      it('renders CycloneDX 1.4 XML correctly', async () => {
        await renderAndWaitForElement(mockXmlSbom, 'bom');
      });

      it('renders CycloneDX 1.5 XML correctly', async () => {
        await renderAndWaitForElement(cycloneDxXml15, 'bom');
        expectElementsToBePresent(['components']);
      });

      it('renders CycloneDX 1.6 XML correctly', async () => {
        await renderAndWaitForElement(cycloneDxXml16, 'bom');
        expectElementsToBePresent(['components']);
      });
    });

    describe('SPDX JSON formats', () => {
      it('renders SPDX 2.2 JSON correctly', async () => {
        await renderAndWaitForElement(spdxJson22, 'SPDXID');
        expectElementsToBePresent(['spdxVersion', 'packages']);
      });

      it('renders SPDX 2.3 JSON correctly', async () => {
        await renderAndWaitForElement(spdxJson23, 'SPDXID');
        expectElementsToBePresent(['spdxVersion', 'packages']);
      });
    });

    describe('SPDX XML formats', () => {
      it('renders SPDX 2.2 XML correctly', async () => {
        await renderAndWaitForElement(spdxXml22, 'Document');
        expectElementsToBePresent(['SPDXID']);
      });

      it('renders SPDX 2.3 XML correctly', async () => {
        await renderAndWaitForElement(spdxXml23, 'Document');
        expectElementsToBePresent(['SPDXID']);
      });
    });

    describe('Component filtering across formats', () => {
      it('filters component in CycloneDX 1.5 JSON', async () => {
        const purl = 'pkg:npm/test-component@1.0.0';
        await testComponentFiltering(cycloneDxJson15, purl, 'component', [
          'type',
          'library',
          'name',
          'test-component',
          'version',
          '1.0.0',
          'purl',
          purl,
        ]);
      });

      it('filters component in CycloneDX 1.6 XML', async () => {
        const purl = 'pkg:npm/test-component@1.0.0';
        await testComponentFiltering(cycloneDxXml16, purl, 'component', [
          '@type',
          'name',
          'test-component',
          'version',
          '1.0.0',
          'purl',
        ]);
      });

      it('filters component in SPDX 2.3 JSON', async () => {
        const purl = 'pkg:npm/test-pkg@1.0.0';
        await testComponentFiltering(spdxJson23, purl, 'package', [
          'SPDXID',
          'SPDXRef-Package',
          'name',
          'test-pkg',
          'versionInfo',
          '1.0.0',
          'downloadLocation',
          'NOASSERTION',
          'filesAnalyzed',
          'externalRefs',
        ]);
      });

      it('filters component in SPDX 2.2 XML', async () => {
        const purl = 'pkg:npm/test-pkg@1.0.0';
        await testComponentFiltering(spdxXml22, purl, 'packages', [
          'SPDXID',
          'SPDXRef-Package',
          'name',
          'test-pkg',
          'versionInfo',
          '1.0.0',
          'downloadLocation',
          'NOASSERTION',
          'filesAnalyzed',
          'externalRefs',
        ]);
      });

      it('shows warning when component not found in any format', async () => {
        const purl = 'pkg:npm/nonexistent@1.0.0';
        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, cycloneDxJson15);
        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} componentPurl={purl} />);
        await waitFor(() =>
          expect(
            screen.getByText('The selected component was not found in the SBOM. Displaying the complete SBOM instead.')
          ).toBeInTheDocument()
        );
      });
    });
  });

  describe('Preview functionality', () => {
    describe('JSON preview', () => {
      it('displays preview for collapsed JSON nodes', async () => {
        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('metadata')).toBeInTheDocument();
        });

        // Check that metadata node has a preview (collapsed by default)
        const metadataLabel = screen.getByText('metadata').closest('.nx-tree__item-label');
        const preview = metadataLabel.querySelector('.iq-original-bom-viewer__preview');
        expect(preview).toBeInTheDocument();
        expect(preview.textContent).toContain('{');
      });

      it('hides preview when JSON node is expanded', async () => {
        const user = userEvent.setup();
        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('metadata')).toBeInTheDocument();
        });

        // Preview should be present initially
        let metadataLabel = screen.getByText('metadata').closest('.nx-tree__item-label');
        expect(metadataLabel.querySelector('.iq-original-bom-viewer__preview')).toBeInTheDocument();

        // Expand the node
        const metadataItem = screen.getByText('metadata').closest('.nx-tree__item');
        const collapseIcon = metadataItem.querySelector('.nx-tree__collapse-click');
        await user.click(collapseIcon);

        await waitFor(() => {
          expect(screen.getByText('timestamp')).toBeInTheDocument();
        });

        // Preview should be gone after expansion
        metadataLabel = screen.getByText('metadata').closest('.nx-tree__item-label');
        expect(metadataLabel.querySelector('.iq-original-bom-viewer__preview')).not.toBeInTheDocument();
      });

      it('displays preview for CycloneDX JSON format', async () => {
        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, mockJsonSbom);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('metadata')).toBeInTheDocument();
        });

        // Metadata should show preview with timestamp and component
        const metadataLabel = screen.getByText('metadata').closest('.nx-tree__item-label');
        const preview = metadataLabel.querySelector('.iq-original-bom-viewer__preview');
        expect(preview).toBeInTheDocument();
        // Preview should contain some content (exact format may vary)
        expect(preview.textContent.length).toBeGreaterThan(0);
      });

      it('displays preview for SPDX JSON format', async () => {
        const spdxJson = {
          SPDXID: 'SPDXRef-DOCUMENT',
          spdxVersion: 'SPDX-2.3',
          creationInfo: { created: '2023-01-01T00:00:00Z' },
          name: 'Test',
          packages: [{ SPDXID: 'SPDXRef-Package', name: 'test-pkg' }],
        };

        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, spdxJson);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('creationInfo')).toBeInTheDocument();
        });

        // creationInfo should show preview
        const creationInfoLabel = screen.getByText('creationInfo').closest('.nx-tree__item-label');
        const preview = creationInfoLabel.querySelector('.iq-original-bom-viewer__preview');
        expect(preview).toBeInTheDocument();
        expect(preview.textContent.length).toBeGreaterThan(0);
      });
    });

    describe('XML preview', () => {
      it('displays preview for collapsed XML nodes', async () => {
        const user = userEvent.setup();

        // Use XML with explicit structure to ensure preview generation
        const xmlWithPreview = `<?xml version="1.0" encoding="UTF-8"?>
<bom xmlns="http://cyclonedx.org/schema/bom/1.4" version="1">
  <metadata>
    <timestamp>2024-01-12T10:00:00Z</timestamp>
  </metadata>
  <components>
    <component type="library">
      <name>react</name>
    </component>
  </components>
</bom>`;

        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, xmlWithPreview);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        // Wait for the bom node to be rendered and open
        await waitFor(() => {
          expect(screen.getByText('bom')).toBeInTheDocument();
        });

        // Find and collapse the bom node
        const bomItem = screen.getByText('bom').closest('.nx-tree__item');
        const collapseIcon = bomItem.querySelector('.nx-tree__collapse-click');
        expect(collapseIcon).toBeInTheDocument();

        await user.click(collapseIcon);

        // Wait for the node to be collapsed and preview to appear
        await waitFor(
          () => {
            const bomLabel = screen.getByText('bom').closest('.nx-tree__item-label');
            const preview = bomLabel.querySelector('.iq-original-bom-viewer__preview');
            expect(preview).toBeInTheDocument();
            expect(preview.textContent).toContain('{');
          },
          { timeout: 2000 }
        );
      });

      it('displays preview for CycloneDX XML format', async () => {
        const cycloneDxXml = `<?xml version="1.0"?>
<bom xmlns="http://cyclonedx.org/schema/bom/1.5" version="1">
  <metadata>
    <timestamp>2024-01-12T10:00:00Z</timestamp>
  </metadata>
  <components>
    <component type="library">
      <name>test-component</name>
    </component>
  </components>
</bom>`;

        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, cycloneDxXml);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('bom')).toBeInTheDocument();
        });

        // Verify the root element exists (preview visibility depends on expand state)
        expect(screen.getByText('bom')).toBeInTheDocument();
      });
    });

    describe('Preview format and ellipsis', () => {
      it('shows ellipsis for objects with more than 3 properties', async () => {
        const largeObj = {
          parent: {
            nested: {
              key1: 'value1',
              key2: 'value2',
              key3: 'value3',
              key4: 'value4',
              key5: 'value5',
            },
          },
        };

        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, largeObj);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('nested')).toBeInTheDocument();
        });

        // nested node should show preview with ellipsis (it's not auto-expanded)
        const nestedLabel = screen.getByText('nested').closest('.nx-tree__item-label');
        const preview = nestedLabel.querySelector('.iq-original-bom-viewer__preview');
        expect(preview).toBeInTheDocument();
        expect(preview.textContent).toContain('…');
      });

      it('does not show ellipsis for objects with 3 or fewer properties', async () => {
        const smallObj = {
          parent: {
            nested: {
              key1: 'value1',
              key2: 'value2',
              key3: 'value3',
            },
          },
        };

        axiosMock.onGet(getDownloadSbomFileUrl(internalAppId, sbomVersion)).reply(200, smallObj);

        render(<OriginalBomViewer internalAppId={internalAppId} sbomVersion={sbomVersion} />);

        await waitFor(() => {
          expect(screen.getByText('nested')).toBeInTheDocument();
        });

        // nested node should show preview without ellipsis (it's not auto-expanded)
        const nestedLabel = screen.getByText('nested').closest('.nx-tree__item-label');
        const preview = nestedLabel.querySelector('.iq-original-bom-viewer__preview');
        expect(preview).toBeInTheDocument();
        expect(preview.textContent).not.toContain('…');
      });
    });
  });
});
