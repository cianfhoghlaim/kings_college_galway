# Update Specs

Trigger the research synthesis workflow for a specific file.

## Configuration

-   **Command**: `/update-specs`
-   **Arguments**:
    -   `file`: (Optional) Path to the research file. If omitted, ask the user to select a recent file.

## Workflow

1.  If `file` is not provided, list the 5 most recently modified files in `research/` and ask the user to choose.
2.  Invoke the `synthesize-research` skill with the selected file path.
3.  Report back which files were updated (Project Context, Spec, Skill).
