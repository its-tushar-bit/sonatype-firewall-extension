/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { Routes, Route } from 'react-router';

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<h1>Sonatype Guide</h1>} />
    </Routes>
  );
}
