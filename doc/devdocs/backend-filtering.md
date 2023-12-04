<!--

    Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
    Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
    "Sonatype" is a trademark of Sonatype, Inc.

-->

# Backend Filtering of Data

There are a number of screens in IQ that allow users to search through lists of data. When this filtering is done on the
backend there is a risk of introducing performance issues. This doc is meant as a list of guidelines and considerations
to help prevent performance problems.

## Recommended limitations

Be careful searching on every character input by the user. Recommended restrictions:

- Frontend field requires X (usually 2) characters before searching
- Implement a delay between user input and searching (Note RSC
  implements [NX_STANDARD_DEBOUNCE_TIME](https://gallery.sonatype.dev/#/pages/Search%20Dropdown)

Only return what is required to the front-end

- Use paging to only return a limited number of results
- Set limits on the total number of rows that can be returned by the API

We've had situations in the past where the filtering was done in memory on the server but that relied on first
returning a full table from the database to the application and using a large amount of memory. In some situations this
makes sense but when network latency or total memory are issues:

- Perform the paging in the query or at least limit the results returned from the query as much as possible
- Set limits on the amount of data returned from the database to the backend application

## General considerations

- Limit the number of filters - Avoid allowing too many filters in a single query. Each additional filter adds complexity
  to the query, potentially impacting performance.
- Be careful of deeply nested queries.
- Try to ensure that the columns you filter on are indexed.
- Avoid over-indexing - While indexing is crucial, over-indexing can also lead to performance issues during data
  modification operations (insert, update, delete). Indexes can also require a lot of disk storage. Strike a balance.
- Implement pagination - If the result set can be large, implement pagination to return a limited number of records per
  request. This reduces the load on both the server and the network.
- Set query execution time limits.
- Test your filtering - Try out complex queries on a large dataset to understand the impact.

