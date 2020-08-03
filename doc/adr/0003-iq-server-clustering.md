# ADR 3. IQ Server Clustering

Date: 2020-07-30

## Status

Accepted

## Context

There is a demand for Nexus IQ Server to offer High Availability and Disaster Recovery solutions, for which customers
would be willing to pay for.

This is especially important to customers who want to provide a high service-level agreement (SLA) by minimising single
points of failure and by easing the management of multiple instances or nodes.

Running multiple IQ Server nodes in a way where they can seamlessly step-in for each other requires each node to have
the same state, at least where state is critical to IQ Server functioning correctly and to providing a good user
experience (UX).

IQ Server has state in its
- Database
- Files
- Memory

Thus, for nodes to have the same state, they need to share and/or synchronize their databases, files, and memories.

## Decisions

Given the above, to enable IQ Server clustering and multiple nodes, we will:
- Share the same external database between nodes in a cluster
- Share files where needed for correct functionality/a good UX
- Avoid storing state only in-memory that is needed for correct functionality/a good UX
- Ensure nodes (re)load state in-memory from a shared storage if needed for correct functionality/a good UX
- Ensure operations affecting shared state are safe

Regarding files that should be shared (e.g. scan files and report files), these will be placed in a directory that must
be shared between nodes. It will be the responsibility of the system administrator(s) to actually share that directory.

To help facilitate these decisions, we will use Quartz job scheduling for the following reasons:
- It's already used by Repository Manager and so gives a unified tech stack across products
- It's tried and tested and has a track record of being reliable
- It's a library and can be easily incorporated
- It's simple, implementing cluster communication entirely via a shared database and does not require configuring
additional network connections between each node
- It has/can be extended to have features including - restricting a job to specific nodes, running a job on all nodes,
detecting crashed nodes, automatically re-scheduling jobs originally assigned to nodes that crashed

In particular, we will create a high-level task scheduler component which can be used to schedule one-time or periodic
jobs to run on one or more nodes in a cluster.

Additionally, we will rely on database locks to avoid concurrent operations on different nodes where needed. For
example, an application policy evaluation should not occur concurrently with (un)grandfathering an application because
whether or not a policy violation is grandfathered can affect policy evaluation results and an application should only
be grandfathered or not grandfathered, not somewhere in-between.

Also given IQ Server clustering is a feature we will sell, we will ensure it cannot be used for free by adding a new
license feature which, if not present, will shutdown excess nodes connecting to the same database.

## Consequences

- Properly shared/synchronized state ensures consistency across cluster nodes allowing them to step-in for each other
- Clustering will not work with embedded databases (i.e. H2), which by definition are per node, which is why we will use
a single shared database
- Clustering requires an external database (i.e. postgres), which must be configured
- Clustering requires a license that supports an external database and node clustering
- Storing state only in-memory and/or not (re)loading state into memory where needed may break clustering and/or result
in a poor UX and so should usually be avoided (one example is caching, careful consideration should be given to any IQ
Server side caching as to if/how it will work with clustering)
- A shared database and files will put more demand on those resources and so the cluster performance of operations that
use/update/synchronise state in these resources should be taken into account
- It will ultimately be the responsibility of the system administrator(s) to ensure, with the help of our documentation,
that node clustering is correctly setup
