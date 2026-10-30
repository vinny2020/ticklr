# TIC-111 — adaptive navigation and state continuity

The Groups and Tickle tabs previously replaced their complete navigation
hierarchies when `horizontalSizeClass` changed. That recreated child views
holding unsaved drafts, search state, sheets, and scroll positions.

## Implementation

- Groups uses one `NavigationSplitView` at every width. Custom group cards set
  both the selected group and `preferredCompactColumn = .detail`, so compact
  navigation still opens the detail. SwiftUI owns the back/collapse behavior.
- Tickle uses one split view and one list. Modal presentations attach to the
  persistent split host. Editors opened as compact sheets remain sheets during
  expansion; editors opened in the wide detail column remain in that column
  during collapse. Resizing never transfers a draft between editor instances.
- A hidden detail draft is resumed by Add rather than covered by a second
  editor. Finishing a new detail editor returns the compact split to its list.
- ContentView, NetworkListView, and ComposeView already keep tab, contact,
  search/filter, recipient/template, and draft state above their adaptive
  content. Their production navigation architecture remains intact.
- Navigation stays native. Optional sidebar placement was not added because
  native TabView already adapts; an extra navigation surface adds no benefit.

## Regression harness

`-debugTestAdaptiveResize` enables a shared compact/regular trait control in
DEBUG builds, including editor sheets. With the argument absent it adds no
controls or trait overrides; release builds compile it to the original view.
The UI tests change the environment of the same live view tree repeatedly and
assert content, selection, draft, scroll, and save behavior. They also cover
cancellation and the existing compact group-card navigation regression.

These are controlled size-class transitions, **not proof of physical folding
or changed window geometry**. Physical Duo fold/unfold/rotation verification
must record actual scene frames and traits and is tracked separately from
this deterministic regression suite (TIC-115).

## Verification record

- Xcode 27.1; iOS 27.1 Duo simulator.
- Initial build succeeded; all 244 unit tests passed.
- The first UI run had inactive display/input routing following simulator
  display discovery. Existing Compose/Groups tests failed along with the new
  tests. Restarting Duo without erasing data restored input; the compact draft
  regression then passed. Failure logs and xcresults are retained locally.
- Unsigned release archive passed. Binary string checks found no `test.resize`,
  `debugTestAdaptiveResize`, or `AdaptiveResizeTestState`; the app has no bundled
  third-party frameworks.
- All 11 adaptive scenarios and all five existing Groups/Compose navigation
  cases passed locally. The combined run's sole failure was an assertion that
  assumed TextEditor typing appends at the end; capturing the actual entered
  draft before resizing corrected the test, and its focused rerun passed.
- The final editor checks also pass after re-querying controls across layout
  changes. Native overlay sidebars are reopened before asserting retained
  search/scroll state; the tests do not assume both columns fit in the fixed
  test frame.
- Negative control: using the old `TickleListView` from `edfa109` with the same
  instrumented editor and final test, the first compact-to-regular transition
  removed the draft TextField and failed `testCompactTickleDraftSurvivesResize`.
  Restoring the fixed source preserves the draft. Source SHA-256 before/after
  the temporary baseline substitution matches; no baseline code was committed.
- GitHub CI is linked from [PR #140](https://github.com/vinny2020/ticklr/pull/140).
  It includes all adaptive cases, the existing Duo core UI suite, the 244 unit
  tests, unsigned archive, standard iPhone/iPad launch, Android, and localization.
- The isolated project and scoped Python agent are permitted by Xcode MCP.
  Workspace-backed InstallAndRun succeeded; live verification is in progress.
  No broad access was granted and no simulator data was erased.
- Hosted run `41075813966` retained draft text but failed two Cancel-control
  checks. The group name field now supports Done to dismiss the keyboard and
  exposes a stable Cancel identifier. The Compose assertion checks the native
  tab's selected state rather than availability of a toolbar control. Artifact
  upload also failed with runner DNS `ENOTFOUND`; the full job log is retained
  locally as `ci-123fd85.log`. These fixes require a fresh CI run.

Local logs and result bundles: `logs/duo/tic111/` in the isolated worktree.
`negative-fresh.log` / `NegativeFresh.xcresult` record the direct draft-loss
failure; `RestoredFinal.xcresult`, `RegressionFixed.xcresult`, and
`FinalFocused.xcresult` record passing editor checks. `release-verification.json`
records binary inspection. Earlier failed runs are retained for diagnosis.

Native navigation API reference: [Apple NavigationSplitView documentation](https://developer.apple.com/documentation/swiftui/navigationsplitview).
