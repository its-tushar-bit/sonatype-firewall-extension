/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxAccordion, useToggle } from '@sonatype/react-shared-components';
import { useSelector } from 'react-redux';
import { selectBillOfMaterialsPage } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSelectors';
import { always, compose, head, is, join, map, split, when } from 'ramda';
import { capitalize, isNilOrEmpty } from 'MainRoot/util/jsUtil';

import './MetadataAccordion.scss';

const SBOM_METADATA_PROPERTIES = {
  cyclonedx: ['author', 'manufacturer', 'supplier', 'specification', 'specVersion', 'fileFormat', 'originalFile'],
  spdx: ['person', 'organization', 'specification', 'specVersion', 'fileFormat'],
};

const camelCaseToTitleCase = compose(join(' '), map(capitalize), split(/(?=[A-Z])/));
const formatMetadataValue = compose(when(is(Array), head), when(isNilOrEmpty, always('NONE')));

export default function MetadataAccordion() {
  const { sbomMetadata } = useSelector(selectBillOfMaterialsPage);
  const [open, toggleOpen] = useToggle(false);
  const metadataProperties = SBOM_METADATA_PROPERTIES[sbomMetadata.specification.toLowerCase()];
  const content = map(
    (property) =>
      property == 'originalFile' && sbomMetadata[property] == null ? null : (
        <div key={property} className="sbom-manager-bill-of-materials-summary-metadata-accordion__property-list-item">
          <dt>{camelCaseToTitleCase(property)}</dt>
          <dd>{formatMetadataValue(sbomMetadata[property])}</dd>
        </div>
      ),
    metadataProperties
  );

  return (
    <NxAccordion
      className="sbom-manager-bill-of-materials-summary-metadata-accordion"
      open={open}
      onToggle={toggleOpen}
    >
      <NxAccordion.Header>
        <NxAccordion.Title>Show metadata</NxAccordion.Title>
      </NxAccordion.Header>
      <dl className="sbom-manager-bill-of-materials-summary-metadata-accordion__property-list">{content}</dl>
    </NxAccordion>
  );
}
