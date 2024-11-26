/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { render, screen, fireEvent } from 'TestRoot/SpecUtil';
import MetadataAccordion from 'MainRoot/sbomManager/features/billOfMaterials/metadataAccordion/MetadataAccordion';

describe('MetadataAccordion', () => {
  const cycloneDXMetadata = {
    author: ['Alice', 'Bob'],
    manufacturer: ['Orange'],
    supplier: ['Apple'],
    specification: 'CycloneDx',
    specVersion: '2.3',
    fileFormat: 'json',
    originalFile: null,
  };

  const spdxMetadata = {
    person: ['John', 'Jane'],
    organization: ['Sonatype'],
    specification: 'SPDX',
    specVersion: '2.1',
    fileFormat: 'json',
  };

  const emptyFieldMetadata = {
    author: [],
    manufacturer: ['Orange'],
    supplier: ['Apple'],
    specification: 'CycloneDx',
    specVersion: '2.3',
    fileFormat: 'json',
    originalFile: null,
  };

  const cycloneDXMetadataFromBinaryScan = {
    author: ['Alice', 'Bob'],
    manufacturer: ['Orange'],
    supplier: ['Apple'],
    specification: 'CycloneDx',
    specVersion: '2.3',
    fileFormat: 'json',
    originalFile: 'test.jar',
  };

  describe('renders the correct content when', () => {
    it('the format is CycloneDx', async () => {
      const preloadedState = {
        billOfMaterialsPage: {
          sbomMetadata: { ...cycloneDXMetadata },
        },
      };

      render(<MetadataAccordion />, { preloadedState: { ...preloadedState } });
      const accordionHeader = screen.getByRole('button', { name: /Show metadata/i });

      expect(screen.getByText('Show metadata')).toBeVisible();
      fireEvent.click(accordionHeader);
      expect(await screen.getByText('Author')).toBeVisible();
      expect(await screen.getByText('Alice')).toBeVisible();
      expect(await screen.queryByText('Bob')).not.toBeInTheDocument();
      expect(await screen.getByText('Manufacturer')).toBeVisible();
      expect(await screen.getByText('Orange')).toBeVisible();
      expect(await screen.getByText('Supplier')).toBeVisible();
      expect(await screen.getByText('Apple')).toBeVisible();
      expect(await screen.getByText('Specification')).toBeVisible();
      expect(await screen.getByText('CycloneDx')).toBeVisible();
      expect(await screen.getByText('Spec Version')).toBeVisible();
      expect(await screen.getByText('2.3')).toBeVisible();
      expect(await screen.getByText('File Format')).toBeVisible();
      expect(await screen.getByText('json')).toBeVisible();
      expect(await screen.queryByText('Original File')).not.toBeInTheDocument();
    });

    it('the format is SPDX', async () => {
      const preloadedState = {
        billOfMaterialsPage: {
          sbomMetadata: { ...spdxMetadata },
        },
      };

      render(<MetadataAccordion />, { preloadedState: { ...preloadedState } });
      const accordionHeader = screen.getByRole('button', { name: /Show metadata/i });

      expect(screen.getByText('Show metadata')).toBeVisible();
      fireEvent.click(accordionHeader);
      expect(await screen.getByText('Person')).toBeVisible();
      expect(await screen.getByText('John')).toBeVisible();
      expect(await screen.queryByText('Jane')).not.toBeInTheDocument();
      expect(await screen.queryByText('Organization')).toBeVisible();
      expect(await screen.getByText('Sonatype')).toBeVisible();
      expect(await screen.getByText('Specification')).toBeVisible();
      expect(await screen.getByText('SPDX')).toBeVisible();
      expect(await screen.getByText('Spec Version')).toBeVisible();
      expect(await screen.getByText('2.1')).toBeVisible();
      expect(await screen.getByText('File Format')).toBeVisible();
      expect(await screen.getByText('json')).toBeVisible();
    });

    it('the metadata has an empty field', async () => {
      const preloadedState = {
        billOfMaterialsPage: {
          sbomMetadata: { ...emptyFieldMetadata },
        },
      };

      render(<MetadataAccordion />, { preloadedState: { ...preloadedState } });
      const accordionHeader = screen.getByRole('button', { name: /Show metadata/i });

      expect(screen.getByText('Show metadata')).toBeVisible();
      fireEvent.click(accordionHeader);
      expect(await screen.getByText('Author')).toBeVisible();
      expect(await screen.getByText('NONE')).toBeVisible();
    });

    it('the metadata has an original file', async () => {
      const preloadedState = {
        billOfMaterialsPage: {
          sbomMetadata: { ...cycloneDXMetadataFromBinaryScan },
        },
      };

      render(<MetadataAccordion />, { preloadedState: { ...preloadedState } });
      const accordionHeader = screen.getByRole('button', { name: /Show metadata/i });

      expect(screen.getByText('Show metadata')).toBeVisible();
      fireEvent.click(accordionHeader);
      expect(await screen.getByText('Author')).toBeVisible();
      expect(await screen.getByText('Alice')).toBeVisible();
      expect(await screen.queryByText('Bob')).not.toBeInTheDocument();
      expect(await screen.getByText('Manufacturer')).toBeVisible();
      expect(await screen.getByText('Orange')).toBeVisible();
      expect(await screen.getByText('Supplier')).toBeVisible();
      expect(await screen.getByText('Apple')).toBeVisible();
      expect(await screen.getByText('Specification')).toBeVisible();
      expect(await screen.getByText('CycloneDx')).toBeVisible();
      expect(await screen.getByText('Spec Version')).toBeVisible();
      expect(await screen.getByText('2.3')).toBeVisible();
      expect(await screen.getByText('File Format')).toBeVisible();
      expect(await screen.getByText('json')).toBeVisible();
      expect(await screen.getByText('Original File')).toBeVisible();
      expect(await screen.getByText('test.jar')).toBeVisible();
    });
  });
});
