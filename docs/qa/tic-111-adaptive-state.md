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
  Workspace-backed InstallAndRun and seeded live checks succeeded.
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

## Final hosted-test corrections

Run `41083374819` uploaded diagnostics successfully. Its Compose spindump shows
XCTest timing out inside an all-descendants accessibility query; the template
menu is now queried specifically as a button. The group screenshot shows the
retained draft in a scrolled form with navigation minimized. Scrolling the form
back up reveals Cancel; `GroupToolbarReveal.xcresult` and
`FinalRecordedFailures.xcresult` pass cancellation with all draft assertions
unchanged. The final recorded-failures run passed both previously failing cases.

Experimental UIKit/trait-harness and toolbar-role changes were discarded. Their
patch and logs remain under the ignored local log folder for diagnosis; they
are not part of the PR. The original opt-in SwiftUI environment harness remains
explicitly a controlled class test, not a physical-window/fold test.


## Equal panes on the open Duo

Review of the seeded native simulator showed that the automatic fully open
layout used a narrow sidebar and a wide detail pane. The requested Duo layout
uses two equal content panes for Network, Groups, and Tickle. iPad retains the
system's automatic proportions.

`FoldableSplitColumns` identifies folding context through the local hinge and
reserved-division APIs on iOS 27.1. It retains that capability across display
changes, measures the local split-view area, requests half its width for the
sidebar, permits the detail to shrink to the other half, and uses the balanced
native split style with both columns visible when open. It retains the last
positive measurement during transient layout updates. Nonfolding contexts and
older systems receive no column-width preference.

The native open Duo has 867pt of app content beside the 84pt system rail.
The verified list width is 433.7pt; the detail occupies the remaining 433.3pt
(one physical pixel of rounding). Initial attempts that sized only the list
produced an overlay and were rejected; setting the detail minimum and open
column visibility produced two tiled panes automatically.

Build output and native captures are retained in
`logs/duo/tic111/equal-columns/`. The final binary cold-launched while closed preserved the unsaved 16-character
`Final fold draft` through its first native opening. The disposable draft was
canceled without saving. Native previews show the populated Network contact
detail, Family group, and Carlos reminder in tiled equal panes.

The iPad baseline and the same final binary both use a 320pt sidebar at a
1210×834pt landscape window on iOS 27.0. After the final portrait launch was
rotated to landscape, Show Sidebar was tapped before measuring. This verifies
unchanged column proportions, not every automatic visibility transition.
The before/after screenshots and hierarchies are retained in the local evidence.
The earlier hosted run for `8cd1704` finished successfully, including 244 unit
tests, 18 Duo UI tests, the unsigned archive, and standard iPhone/iPad launch.
A fresh hosted run is required for the equal-pane change.
