# ADR 3. Insight brain core functionality shared module.
Date: 2020-07-28

## Status
Proposed

## Context
We want to enable our immediate cloud deployed services as well as future development of applications to utilize core functionality developed by using a shared policy interface and logic.
Enable insight to eventually decouple into services or smaller products to support our vision for more cloud native solutions. Ultimately we are here to enable product management to evolve and develop solutions in more efficient, valuable approaches.

## Decision
We will create a new module within insight-brain intended to be utilized by insight-brain as well as other Sonatype code bases.
We will implement initial interfaces intended to share 

## Consequences
Code drift will be mitigated since the longer we take to consolidate, the further apart our applications get.
Duplication of efforts will be avoided between disparate teams including feature development, bug fixes, policy violation fixes, etc.