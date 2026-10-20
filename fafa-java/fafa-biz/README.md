# fafa-biz DDD module boundaries

`fafa-biz` is one deployable service. Its domain contexts are packages, not separate Maven modules or services. Each context follows the same inward dependency direction:

- `interfaces`: HTTP and RPC adapters.
- `application`: use-case orchestration and transaction boundaries.
- `domain`: business rules and repository abstractions.
- `infrastructure`: persistence and external-service adapters.

Initial contexts: `identity`, `pet`, `care`, `reminder`, `media`, `sharing`, and `community`. Cross-context calls should go through application-facing interfaces and stable IDs. Do not reach into another context's repository or tables. The package layout is a convention at this stage; add automated architecture enforcement when real modules and rules exist.
