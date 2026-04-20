# Changelog

All notable changes to this project will be documented in this file atleast once a day (if there are any changes).

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.2] - 2026-04-20

### Added
- Allow editing plan name from TopAppBar in PlanEdit screen
- Cardio exercise category with specific UI and tracking logic
- Day of week labels to Training Heatmap with localization
- Highlight and animation effects when dragging exercises to reorder plans
- Infinite horizontal swiping and month/year navigation for Training Heatmap
- Bottom navigation bar with Home, Exercise (active sessions only), and Profile tabs
- Direct entry to Home screen, bypassing the onboarding flow
- Fully rounded Material Design 3 styling across all screens
- Rounded card layouts for sessions, plans, and exercises
- Unified and simplified SetItem styling to match SessionCard
- Rounded backgrounds for swipe-to-delete actions and heatmaps
- Improved spacing and eliminated sharp corners in all UI components
- Training heatmap on Home screen with monthly calendar view
- Language selection and Simplified Chinese support
- Setting to toggle exercise name capitalization
- +2 increment button to AddSet screen
- Draggable sets input and improved set count management in ViewModel
- Backup and restore for whole data
- Edit mode toggle for past sessions in Session Detail
- Persistence for imported exercises using SavedStateHandle

### Fixed
- Implemented modern SplashScreen API to fix startup flickering and ensure smooth transition
- Delete functionality for sessions in Session History with confirmation dialog
- Support for importing plans from other days on rest days
- Body weight tracking with custom line chart and history management
- Interactive multi-digit scrolling selection for body weight (tens, ones, decimal)
- Enhanced body weight chart with X (date) and Y (value) axis labels
- Collapsible exercise sections in Session Detail
- Double-click to edit reps and weight in Session Detail (edit mode)
- Custom day titles for training plans
- Full day names instead of abbreviations across the app

### Changed
- Redesigned Home screen: removed top bar and profile icon for a cleaner look
- Compact bottom navigation styling with icon-only mode and enlarged icons
- Reduced header font size and optimized layout for better vertical visibility
- Hidden bottom navigation on sub-pages (history, settings, etc.) for focused navigation
- Updated Chinese translation for system theme label
- Optimized Home screen headings and session start/continue logic
- Removed debug mock data button from Plan Edit screen
- Removed redundant Floating Action Button from Sessions screen
- Replaced "Lifts" card with "Weight" card in Profile screen
- Show custom titles in plan editing, exercise selection, and import lists
- Format training header as "Title (Day)"

### Fixed
- Ghosting artifacts and shadow clipping when dragging exercises
- Inconsistent list styling across Exercises, Sessions, Plans, and Select Exercise screens
- Smooth settle animation when releasing a dragged exercise
- Selection indicator clipping on compact bottom navigation bar
- Deprecated `monthNumber` property in heatmap implementation
- Issue where editing history sessions would create today's session instead
- Overlapping points and labels in body weight chart by increasing spacing
- Issue where imported exercises would disappear after app restart on rest days
- Automatic recovery of imported plans by matching performed exercises
- Incorrect "Today" logic for historical session views
- Hardcoded rest day heading in Session Detail
- Overlapping delete icon in swipe-to-delete lists

- Turkish (`values-tr`) support
- Unnecessary quotes and fixed gradle warnings

## [1.3.2] - 2025-11-13

### Added
- Session History card on Home screen
- Set Type selection for sets
- Timer which shows time since last set
- Monochrome launcher icon on Android 12+
- Allow adding a new exercise directly when it cannot be found in the list
- Clean up empty plans from the Plans screen via confirmation dialog
- Enable predictive back navigation
- Empty state on Sessions screen

### Changed
- Removed Bottom navigation bar, added user icon to top bar
- Updated Day Switcher component styling for clarity
- Hide Lifts card when there are no lifts
- Redesigned Session History card and screen
- Replaced profile icon on Home screen
- Removed "Today" label on Sessions and filtered out empty sessions

### Fixed
- Select Plan button alignment
- Prevent unintended translation for Turkish app name
- Session list now shows most recent first
- Lifts card not showing even when lifts existed
- Sets from past sessions not shown when the exercise was removed from the corresponding plan

## [1.3.0] - 2025-01-17

### Added
- Drag text field in "Add Set"
- Double tap to edit "set info"
- History Icon (You can check last week's session if it exists)
- Support for Monochrome icon on Android 12+
- Text animation on Onboarding
- Safer way to delete Sets / Exercises / Plans
- New Font for headings

### Changed
- Targets Android 15
- Onboarding screen
- Default theme for new users
- Sorting of muscle groups chips
- Always save plan on going back
- Color in Profile
- Home Screen and On-boarding Screen
- Some buttons and UI elements

### Fixed
- Save button not visible
- Two `Default` theme in Settings
- Scrolling on `Select Exercise` Sheet
- Performance issues on `Add Set` Sheet
- Weird line in the setting wave
- Crash on deleting plan
- On boarding not completing
- Loads of performance improvements

### Removed
- Gradient in settings

## [1.2.0] - 2024-05-26

### Added
- Support for isometric exercises
- Deleting Sets / Exercises / Plans

### Changed
- Error message height
- Chips type in `Select Exercise`

### Fixed
- Navigation to same page again
- Double back presses
- Swipe gesture on reps and weight text field
- Elements squashing on small screens
- Empty exercises
- Invalid reference
- False reference icon

## [1.1.1] - 2024-05-19

### Fixed
- Navigation from home screen
- Annoying animations on home page
- Plan Edit Page
- Back button on all pages

## [1.1.0] - 2024-05-19

### Added
- New Home Page
- Back button on Exercises Page
- Option to open References from workout page(if added)

### Changed
- Splash Screen Image to reduce dependency on `NonFreeNet`
- Whole Plan card is clickable

### Fixed
- APK dependency tree encryption
- Color of icons on some buttons
- `Zestful` Color Palettes
- Crash when using invalid reference
- UI/UX for Exercises Page
- Some navigation crashes

## [1.0.0] - 2024-05-12

### Added
- Initial Release
