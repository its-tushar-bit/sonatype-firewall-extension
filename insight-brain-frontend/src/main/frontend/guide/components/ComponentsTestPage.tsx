/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { useEffect, useState } from 'react';
import { searchComponents } from 'GuideRoot/api/componentsBackend';
import type { ComponentSearchResponse } from '@guide/ui-core/types';

export function ComponentsTestPage() {
  const [data, setData] = useState<ComponentSearchResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    searchComponents()
      .then(setData)
      .catch((e) => setError(e instanceof Error ? e.message : String(e)))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div>Loading...</div>;
  if (error) return <div>Error: {error}</div>;
  if (!data) return <div>No data</div>;

  return (
    <div style={{ padding: '2rem' }}>
      <h1>Components Test Page</h1>
      <p>Total: {data.total} | Showing: {data.hits.length}</p>

      <h3>Results</h3>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '1rem' }}>
        {data.hits.map((component) => (
          <div
            key={component.originId}
            style={{
              border: '1px solid #ccc',
              padding: '1rem',
              borderRadius: '4px',
              width: '270px',
            }}
          >
            <h4>{component.name}</h4>
            <p>
              <strong>Format:</strong> {component.format}
            </p>
            <p>
              <strong>Version:</strong> {component.version}
            </p>
            <p>
              <strong>CVSS:</strong> {component.maxCvss ?? 'N/A'}
            </p>
            <p>
              <strong>Score:</strong> {component.versionScore ?? 'N/A'}
            </p>
            <p>
              <strong>Licenses:</strong>{' '}
              {(component.licenses ?? []).map((l) => l.licenseName).join(', ')}
            </p>
            {component.isMalware && <p style={{ color: 'red' }}>⚠️ MALWARE</p>}
          </div>
        ))}
      </div>
    </div>
  );
}
