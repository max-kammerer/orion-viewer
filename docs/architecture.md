# Orion Viewer — architecture notes

Developer-oriented notes on how the app is put together. Intended as a starting point for
contributors and as a reference when reviewing pull requests.


---

## 1. Repository layout

```
orion-viewer/        Android application module — activities, views, dialogs, preferences
common/              Kotlin Multiplatform module (commonMain / androidMain / linuxLibMain):
                     document model, geometry, layout strategy, LastPageInfo
tree-view-list-android/  Vendored tree-view widget used by outline navigation
nativeLibs/          MuPDF and DjVuLibre native modules (djvuModule, mupdfModule)
native/              Native sources (not part of settings.gradle by default)
utils/               Build/dev helper scripts
fastlane/            Store metadata
```

Modules are wired in `settings.gradle`; SDK levels and the Kotlin version live in the root
`build.gradle` (`orionMinSdk`, `orionTargetSdk`, `orionCompileSdk`, `ext.kotlin_version`).

The `common` module is the reason document-model classes such as
`common/.../document/Document.kt` use `expect`/`actual` declarations — it is compiled for Android
and for a Linux host target.

Source is a mix of Kotlin and legacy Java. New code is Kotlin; several dialogs and preference
helpers are still Java (`dialog/SearchDialog.java`, `prefs/TemporaryOptions.java`,
`prefs/OrionKeyBinderActivity.java`).

---

## 2. Runtime object graph

```
OrionViewerActivity
├── OrionApplication        process-wide singletons: GlobalOptions, TemporaryOptions, device
├── GlobalOptions           application-wide settings (SharedPreferences)
├── SubscriptionManager     fan-out of "rendering parameters changed" / "page changed" events
├── FullScene               ColorStuff (paints/background) + status bar + OrionDrawScene
│   └── OrionDrawScene      the actual android.view.View that everything is drawn on
└── Controller              per opened document
    ├── Document            format-specific implementation (MuPDF / DjVu / archives)
    ├── LayoutStrategy      zoom, rotation, crop, walk order → per-page LayoutPosition
    └── PageLayoutManager   which pages exist, where they sit, when they render
        └── PageView[]      one per currently active page
```

`Controller` is created when a document is opened and destroyed with it. It owns a coroutine
scope (`rootJob` + `Dispatchers.Default`) and a dedicated rendering dispatcher limited to
`availableProcessors() - 1`.

`Controller.init(info: LastPageInfo, ...)` is where the pieces are connected: the layout strategy
is initialised from the persisted book state, the `PageLayoutManager` is created, and the
controller subscribes itself to `SubscriptionManager`.

### Change notification

There is no data binding between settings and rendering. The flow is:

```
setting changed
   → Controller.<some>Change...()
   → Controller.sendViewChangeNotification()
   → SubscriptionManager.sendViewChangeNotification()
   → DocumentViewListener.renderingParametersChanged()
   → PageLayoutManager.forcePageUpdate()
   → PageView.invalidateAndUpdate() for every active page
```

Note what `forcePageUpdate` does and does not do: it invalidates each page and re-runs geometry
calculation, but it does **not** re-run the inter-page layout. Relative page positions are only
adjusted in `PageLayoutManager.onPageSizeCalculated`, and only by the *delta of the page's own
size*. A change that alters spacing without altering page size therefore will not be reflected in
already-placed pages — see §4.4.

---

## 3. Settings and persisted state

There are three distinct levels of state. Choosing the wrong one is the most common mistake in
contributions.

| Level | Class | Lifetime | Storage |
|---|---|---|---|
| Application-wide | `prefs/GlobalOptions.kt` | forever | `SharedPreferences` |
| Per book | `common/.../LastPageInfo.kt` | per document, persisted | XML file in app private storage |
| Per opened book, in memory | `prefs/TemporaryOptions.java` | recreated on every `onNewBook` | none |

`LastPageInfo` is serialised field-by-field via reflection in `PageInfoLoader.kt` (`save` / `load`),
skipping `transient` and `static` fields, with a `CURRENT_VERSION` upgrade path
(`LastPageInfo.upgrade`). Adding a public non-transient field to `LastPageInfo` therefore changes
the on-disk format — bump the version and add an upgrade rule.

Defaults for a freshly opened book are taken from `GlobalOptions` in
`prefs/lastInfoInitializer.kt`.

`TemporaryOptions` is recreated in `OrionApplication.onNewBook(fileName)`, which makes it the
right home for state that should be scoped to the currently opened document and must not leak
into the next one.

### 3.1 Two mechanisms for global options

`GlobalOptions` currently contains **two** ways of exposing a preference. New code should use the
first one.

**Modern: `pref(...)` + `LiveData`.**

```kotlin
val DRAW_PAGE_BORDER = pref("DRAW_PAGE_BORDER", true)
val SCREEN_BACKLIGHT_TIMEOUT = pref("SCREEN_BACKLIGHT_TIMEOUT", 10, stringAsInt = true)
```

`pref()` (in `prefs/Preference.kt`) builds a `Preference<T> : LiveData<T>`, caches the current
value, and registers it in `GlobalOptions.registeredPreferences`. The
`OnSharedPreferenceChangeListener` installed in the `GlobalOptions` constructor looks the key up
in that map first:

```kotlin
registeredPreferences[name]?.update()?.also {
    return@OnSharedPreferenceChangeListener
}
```

If the key is registered, the cached value is refreshed, observers are notified, and the legacy
chain below is skipped entirely.

Consumers subscribe with the standard LiveData API, e.g.
`globalOptions.SHOW_BATTERY_STATUS.observe(activity) { ... }` in `view/OrionStatusBar.kt`, or read
`.value` directly (`Controller.drawBorder`).

Supported types are `String`, `Boolean` and `Int`; `stringAsInt = true` is for preferences whose
widget persists an int as a string (`EditTextPreference`, `SeekBarPreferenceAsText`).

**Legacy: property getter + `if/else` chain + `OptionActions`.**

Older options are exposed as plain getters reading `SharedPreferences` on every access:

```kotlin
val defaultContrast: Int
    get() = getIntFromStringProperty(DEFAULT_CONTRAST, 100)
```

and reacted to via a chain of `else if (KEY == name)` branches in the change listener, which
dispatches into the `OptionActions` enum, which in turn calls a method on `Controller`.

Two practical downsides: nothing is cached, so every read is a `SharedPreferences.getString()`
plus a parse; and adding one option touches three files.

**Recipe for a new application-wide option**

1. `GlobalOptions.kt` — `val MY_OPTION = pref("MY_OPTION", <default>)` next to the other `pref`
   declarations.
2. `res/xml/user_pref_*.xml` — the widget, with a matching `android:key` and `android:defaultValue`.
3. `res/values/pref.xml` — title and summary strings (English only; translations come through
   Weblate).
4. Where the value is used — either `.observe(lifecycleOwner) { ... }` if something must react to
   the change, or `.value` for a plain read.

No `OptionActions` entry and no listener branch are needed.

### 3.2 Preference widgets

- `prefs/OrionEditPreference.kt` — `OrionEditTextPreference`, an `EditTextPreference` supporting
  `app:minValue`, `app:maxValue` and `app:pattern` (declared in `res/values/attrs.xml`).
- `prefs/SeekBarPreferenceAsText.kt` — `SeekBarPreference` that persists its int as a string, so
  it is interchangeable with the `EditTextPreference`-based numeric options.
- `prefs/ListPreferenceWithIcons.kt`, `prefs/IntListPreferenceWithIcons.kt` — list preferences
  with icons.
- `prefs/OrionBookPreferencesX.kt` — the per-book preference screen.

Preference screens live in `res/xml/`: `user_pref_appearance.xml`, `user_pref_controls.xml`,
`userpreferences.xml`, and the per-book screens.

### 3.3 Gotchas

- **`app:minValue` / `app:maxValue` are advisory.** `OrionEditTextPreference.onPreferenceChange`
  only sets an error on the `EditText`; it does not veto the write (no
  `OnPreferenceChangeListener` is installed). Code reading such a preference must tolerate
  out-of-range values.
- **`getIntFromStringProperty` throws on non-numeric input.** It handles `null` and empty string
  by returning the default, but calls `String.toInt()` otherwise
  (`prefs/PreferenceWrapper.kt`). It is safe today only because the numeric widgets force a
  numeric IME.
- **Do not read preferences in the draw path.** `PageView.draw` runs per page per frame; a
  legacy-style getter there costs a `getString()` plus a parse on every frame. Use a cached
  `Preference`/`LiveData` value.
- **Preference values are read on the UI thread**, but rendering runs on the coroutine
  dispatchers. Values that rendering depends on should be captured, not re-read from a
  background thread.

---

## 4. Continuous page layout and rendering

`view/PageLayoutManager.kt` maintains a vertical strip of pages. This is the core of the viewer
and the most subtle part of the codebase.

### 4.1 Coordinate model

- `LayoutData` (`LayoutData.kt`) holds a page's `position: PointF` (its origin in scene
  coordinates, mutated on scroll) and `wholePageRect: Rect` (its size, integral, in device
  pixels).
- `sceneRect` on `PageLayoutManager` is the visible viewport, `(0, 0, view.width, view.height)`.
- A page is on screen when `wholePageRect` offset by `position` intersects `sceneRect`
  (`LayoutData.pagePartOnScreen`).
- Vertical scrolling adds the same `distanceY` to every active page's `position.y`, so relative
  distances between pages stay exact; only the common origin drifts. Horizontal panning is
  per-page and clamped: it only applies to the page under the touch point, and a page narrower
  than the viewport is kept centred rather than dragged off screen.
- Pages are stacked with a fixed 2 px gap, applied at insertion time in `uploadPrevPage` /
  `uploadNextPage`.

### 4.2 Page lifecycle

`PageView` (`view/PageView.kt`) moves through `PageState`:

```
STUB → CALC_GEOMETRY → SIZE_AND_BITMAP_CREATED → DESTROYED
```

- Geometry and page data are loaded in coroutines rooted at the controller's `rootJob`; each
  `PageView` can cancel its own children (`cancelChildJobs`).
- The rendered content lives in a `FlexibleBitmap` (`bitmap/FlexibleBitmap.kt`) managed by
  `bitmap/BitmapManager.kt`, backed by `BitmapCache`.
- Pages are appended by `uploadNextPage` / `uploadPrevPage` as the viewport approaches them, and
  destroyed and removed from `activePages` once they and their neighbour are off screen
  (`updateCache`). A page that scrolls off screen and comes back is a **new** `PageView` object.
- When a page's real size becomes known, `onPageSizeCalculated` shifts every page after it (or
  before it, if the page sits above the origin) by the size delta, so the strip stays contiguous.

### 4.3 Drawing

`OrionDrawScene.onDraw` walks the active pages and calls `PageView.draw`, which:

1. `canvas.translate(position.x, position.y)` — note that `position` is `PointF`, so the
   translation is **fractional**, while `wholePageRect` is integral;
2. paints the blank/loading page, then the rendered bitmap clipped to the visible part
   (`calcDrawRect`);
3. optionally strokes a 1 px border (`DRAW_PAGE_BORDER`, or unconditionally in scaling mode);
4. runs registered `DrawTask`s (this is how search highlighting is drawn — see §5).

Colours and paints come from `view/ColorStuff.kt`, which also sets the `View` background:
white normally, `#E6E6E6` when "draw off-page space" is on, both passed through the active colour
matrix. That background is what shows through anywhere pages do not cover the viewport.

**Consequence worth knowing:** because the canvas translation is fractional and page rects are
integral, two vertically adjacent pages can round to device pixels in opposite directions and
leave a sub-pixel seam through which the view background is visible. Reducing the gap does not
fix this; rounding the translation at draw time does.

### 4.4 Reacting to layout-affecting changes

`forcePageUpdate` (§2) re-renders pages but does not re-space them. Any change that affects the
*distance between* pages rather than their size needs an explicit repositioning pass over
`activePages`, anchored on the current page. Without one, the change only takes effect for pages
created after it — that is, lazily, as the user scrolls and pages are destroyed and recreated.

### 4.5 The overlap invariant

`PageLayoutManager` has a debug-only `dump()` check that walks pairs of active pages and reports
any intersection through `errorInDebug`. The invariant it encodes — *active pages never
overlap* — is relied on by the positioning logic. Code that deliberately makes pages overlap
breaks it, and relaxing the check to accommodate that hides genuine layout bugs in every other
mode. `isSinglePageMode` is a separate mode in which the strip logic largely does not apply.

---

## 5. Search

`dialog/SearchDialog.java` is a `DialogFragment` shown over the viewer.

- The query is executed by `search/SearchTask`, which runs off the UI thread and calls back into
  `onResult` with a `SearchTaskResult` holding the page and the hit rectangles.
- Hits are grouped into `SubBatch`es — one per screen-sized area of the page, computed by walking
  the page with the current `LayoutStrategy` and `PageWalker`. A hit is assigned to a batch when
  a large enough part of it falls inside that screen area; the threshold in the code is
  `intersectionArea >= hitArea / 9`, annotated `/*33%*/` (i.e. a third in each dimension).
- `doSearch` distinguishes *iterate* (same query as `lastSearch`, move to the next/previous hit
  in the already computed batches) from *real search* (new query, or the current batch list is
  exhausted).
- Highlighting is drawn by `SearchResultRenderer`, a `DrawTask` registered on `OrionDrawScene`
  while the dialog is open and removed in `onDismiss`. The active hit is drawn at alpha 128, the
  rest at 64, reusing `ColorStuff.borderPaint` in `FILL` style.
- The dialog owns `lastPage` and destroys it explicitly (`destroyLastPage`) — search results hold
  a native page handle.

---

## 6. Tests

All tests live in `orion-viewer/src/androidTest` and are **instrumentation** tests — there is no
JVM unit-test source set. They fall into a few groups:

- `test/engine/` — document engine level: opening books, search, selection, parallel open,
  Unicode file names.
- `test/espresso/` — UI level: scrolling, zoom, page navigation, tap zones, themes, screenshots.
  `BaseViewerActivityTest` / `BaseViewerActivityTestWithConfig` are the entry points, with
  `Configuration.kt` parameterising books and devices.
- `test/rendering/`, `test/utils/`, `test/perf/` — bitmap handling, rect invariants, benchmarks.
- `test/framework/` — shared harness, including `OperationIdlingResource`.

`AGrantFilePermissionsTest` is a stub whose only assertion is that the test book is readable; the
leading `A` makes it run first, so that permission granting done by `BaseViewerActivityTest`
happens before the rest of the suite.

### Test hooks in production code

Two hooks live in `orion-viewer/src/main`, not in the test source set:

- `test/IdlingResource.kt` — an open no-op class instantiated as `OrionApplication.idlingRes` and
  marked busy/free around book opening in `OrionViewerActivity` and `FallbackDialogs`. Espresso
  tests replace it with `OperationIdlingResource`, which delegates to a `CountingIdlingResource`,
  making asynchronous book loading observable from the test thread.
- `test/configuration.kt` — `updateGlobalOptionsFromIntent`, which lets an instrumentation test
  override global options (theme, old/new UI, tap help, long-tap action, and even the view
  dimensions) through intent extras, guarded by `GlobalOptions.TEST_FLAG`.

Keep both in mind when changing startup or option handling: they are load-bearing for the
instrumentation suite.

---

## 8. Text Selection

Text selection is managed by `selection/SelectionAutomata.kt` and its associated view `selection/SelectionViewNew.kt`.

- **`SelectionAutomata`** is a state machine that handles touch events for selecting text. It has states: `START`, `ACTIVE_SELECTION`, `MOVING_HANDLER`, and `CANCELED`.
- **`SelectionViewNew`** is a dedicated transparent `View` layered over the document to draw the selection rectangles and adjustment handlers (triangles).
- Selection can be triggered by a long press or double tap (configured in `GlobalOptions`).
- When selection is active, `SelectionAutomata` intercepts touch events in `processTouch`.
- Selected text is extracted from the `Document` via `SelectionAutomata.getSelectedText()`, which returns a `TextAndSelection` object containing the text string and its bounding rectangles.
- Actions on selected text (copy, translate, etc.) are shown via a popup managed by `SelectionAutomata.showActionsPopupOrDoTranslation`.

---

## 9. Bookmarks and Outline

### 9.1 Bookmarks

Bookmarks are stored in a SQLite database and managed by `bookmarks/BookmarkAccessor.java`.

- `OrionBookmarkActivity` displays a list of bookmarks for the current book or all books.
- `Bookmark` class represents a single bookmark entry with a page number and optional comment.
- Bookmarks are persisted across sessions and can be exported/imported.

### 9.2 Outline (Table of Contents)

The outline is provided by the document engine and displayed using a tree-view widget.

- `outline/showOutline.kt` is the entry point for displaying the TOC dialog.
- `outline/OutlineAdapter.kt` bridges the document's `OutlineItem` tree to the `tree-view-list-android` widget.
- Tapping an outline item triggers a navigation event in `OrionViewerActivity`.

---

## 10. Analytics

The app uses a pluggable analytics system defined in `analytics/Analytics.kt`.

- `OrionApplication.analytics` is the entry point for logging events.
- By default, it uses a no-op implementation. If Firebase is enabled during build (controlled by `enableAnalytics` in `build.gradle`), it uses `FireBaseAnalytics`.
- Analytics events include book opening, errors, and significant UI interactions.

---

## 11. Review checklist

Points that recur when reviewing contributions:

**Settings**

- New application-wide option declared with `pref(...)`, not with a getter plus an
  `OptionActions` entry plus a listener branch.
- State scoped correctly: application (`GlobalOptions`), book (`LastPageInfo`), or opened-book
  session (`TemporaryOptions`). `static` mutable fields in fragments outlive the document.
- Default value not duplicated between the XML widget, the Kotlin declaration and the summary
  string.
- Changing an existing default changes behaviour for every current user — worth an explicit
  decision.
- Strings added to `res/values/pref.xml` only; translations arrive through Weblate. Avoid baking
  numeric ranges into translatable strings.
