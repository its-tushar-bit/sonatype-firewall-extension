/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import DependencyTypeTag from './DependencyTypeTag';
import ComponentLabelTag from './ComponentLabelTag';
import ComponentFormatTag from './ComponentFormatTag';

export default function TagDisplayComponent() {
  const liStyle = { margin: '12px 0' };

  const componentLabelSection = (
    <div>
      <span>ComponentLabel tags</span>
      <ul>
        <li style={liStyle}>
          <ComponentLabelTag>Component-Label-currently-applied</ComponentLabelTag>
        </li>
      </ul>
    </div>
  );

  const dependencyTypeSection = (
    <div>
      <span>DependencyType tags</span>
      <ul>
        <li style={liStyle}>
          <DependencyTypeTag isDirect={true} isInnerSource={false} />
        </li>
        <li style={liStyle}>
          <DependencyTypeTag isDirect={false} isInnerSource={false} />
        </li>
        <li style={liStyle}>
          <DependencyTypeTag isDirect={true} isInnerSource={true} />
        </li>
      </ul>
    </div>
  );

  const componentFormatSection = () => {
    const formatTagStyle = { backgroundColor: '#f4f5f9', padding: '12px 12px' };
    const formatsThatHaveIcon = ['maven', 'npm', 'nuget', 'pypi', 'rpm', 'gem', 'golang', 'swift', 'cocoapods'];
    const formatTagGenerator = (format) => {
      return (
        <li key={format} style={liStyle}>
          <span>{`${format} Format`}</span>
          <span>{'     '}</span>
          <span style={formatTagStyle}>
            <ComponentFormatTag name={format} />
          </span>
        </li>
      );
    };

    return (
      <div>
        <span>ComponentFormat tags</span>
        <ul>
          <li style={liStyle}>
            <span>a-name Format, which does not have logo</span>
            <span>{'     '}</span>
            <span style={formatTagStyle}>
              <ComponentFormatTag name="a-name" />
            </span>
          </li>
          <li style={liStyle}>
            <span>pecoff Format, which does not have logo</span>
            <span>{'     '}</span>
            <span style={formatTagStyle}>
              <ComponentFormatTag name="pecoff" />
            </span>
          </li>
          <li style={liStyle}>
            <span>terraform Format, which does not have logo</span>
            <span>{'     '}</span>
            <span style={formatTagStyle}>
              <ComponentFormatTag name="terraform" />
            </span>
          </li>
          {formatsThatHaveIcon.map((format) => formatTagGenerator(format))}
        </ul>
      </div>
    );
  };

  return (
    <div className="nx-tile">
      <div className="nx-tile-header">
        <h2 className="nx-h2">Pills showcase header</h2>
      </div>
      <div className="nx-tile-content nx-scrollable">
        {componentLabelSection}
        {dependencyTypeSection}
        {componentFormatSection()}
      </div>
    </div>
  );
}
