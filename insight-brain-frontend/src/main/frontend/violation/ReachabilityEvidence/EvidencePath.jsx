/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxH3, NxTooltip } from '@sonatype/react-shared-components';

/**
 * Extracts a short class name + method name from a JVM method signature.
 * Example: "com/example/Foo.bar([Ljava/lang/String;)V" → "Foo.bar()"
 */
function formatMethodShort(method) {
  if (!method) return 'Unknown';

  // Strip descriptor (everything from '(' onward)
  const parenIndex = method.indexOf('(');
  const qualified = parenIndex === -1 ? method : method.substring(0, parenIndex);

  // Format: "org/example/ClassName.methodName" — split on last '/' then '.'
  const lastSlash = qualified.lastIndexOf('/');
  const dotAfterClass = qualified.indexOf('.', lastSlash + 1);

  if (dotAfterClass === -1) return method;

  const className = qualified.substring(lastSlash + 1, dotAfterClass);
  const methodName = qualified.substring(dotAfterClass + 1);

  return `${className}.${methodName}()`;
}

/**
 * Builds tooltip content for a method frame: full signature + component/path.
 */
function buildTooltipContent(method, component, filePath) {
  const location = component || filePath;
  if (!method && !location) return '';

  return (
    <>
      {method && <span className="iq-evidence-path__tooltip-method">{method}</span>}
      {method && location && <br />}
      {location && <span className="iq-evidence-path__tooltip-location">{location}</span>}
    </>
  );
}

/**
 * Groups segments into render items: either a "section" (bordered group of same-jar
 * frames/gaps) or an "elided" line between sections.
 */
function buildSections(segments) {
  if (!segments || !segments.length) return [];

  const sections = [];
  let currentLines = [];
  let currentFilePath = null;
  let currentComponent = null;

  function flushSection() {
    if (currentLines.length > 0) {
      sections.push({ type: 'section', label: currentComponent || currentFilePath, lines: currentLines });
      currentLines = [];
      currentFilePath = null;
      currentComponent = null;
    }
  }

  for (const segment of segments) {
    if (segment.type === 'elided') {
      flushSection();
      sections.push({ type: 'elided', count: segment.count });
    } else if (segment.type === 'gap') {
      currentLines.push({ type: 'gap' });
    } else if (segment.type === 'method') {
      if (currentFilePath !== segment.filePath) {
        flushSection();
      }
      currentFilePath = segment.filePath;
      if (segment.component) {
        currentComponent = segment.component;
      }
      currentLines.push({
        type: 'frame',
        method: segment.method,
        component: segment.component,
        filePath: segment.filePath,
      });
    }
  }
  flushSection();

  return sections;
}

/**
 * Renders a single evidence path as a <pre> block with bordered sections
 * for same-component frame groups.
 */
export default function EvidencePath({ path, index }) {
  const sections = buildSections(path.segments);

  return (
    <li className="iq-evidence-path">
      <NxH3>Path {index}</NxH3>
      <pre role="region" aria-label={`Call path trace ${index}`} className="nx-pre iq-evidence-path__trace">
        <span className="iq-evidence-path__trace-inner">
          {sections.map((section, i) => {
            if (section.type === 'elided') {
              return (
                <span key={i} className="iq-evidence-path__elided">
                  {'... ' + section.count + ' more components ...'}
                </span>
              );
            }

            return (
              <span
                key={i}
                role="group"
                aria-label={section.label || 'Unknown component'}
                className="iq-evidence-path__section"
              >
                {section.lines.map((line, j) => {
                  if (line.type === 'gap') {
                    return (
                      <span key={j} className="iq-evidence-path__gap">
                        ...
                      </span>
                    );
                  }

                  const short = formatMethodShort(line.method);
                  const tooltip = buildTooltipContent(line.method, line.component, line.filePath);

                  return (
                    <NxTooltip key={j} title={tooltip}>
                      <span className="iq-evidence-path__frame">{short}</span>
                    </NxTooltip>
                  );
                })}
              </span>
            );
          })}
        </span>
      </pre>
    </li>
  );
}

EvidencePath.propTypes = {
  path: PropTypes.shape({
    segments: PropTypes.arrayOf(PropTypes.object).isRequired,
  }).isRequired,
  index: PropTypes.number.isRequired,
};
