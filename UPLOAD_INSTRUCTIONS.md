# GitHub Upload Instructions — Yield Browser v0.10.05

The source is divided into two archives because the GitHub web uploader accepts a maximum of 100 files per upload batch.

## Archive contents

- `Part1_Core_100_files.zip`: exactly 100 core application, source, resource, and build files.
- `Part2_Tests_Docs_50_files.zip`: exactly 50 remaining resources, tests, workflow, metadata, and documentation files.

## Upload order

1. Extract both ZIP archives into the same local project directory while preserving folder paths.
2. Upload the extracted contents of Part 1 to the repository root.
3. Upload the extracted contents of Part 2 to the same repository root.
4. Do not place Part 2 inside a separate subfolder.
5. After both batches are committed, run the included GitHub Actions workflow.

The two archives contain no duplicate paths. Together they reconstruct the complete 150-file project.
