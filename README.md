<img src="app/src/main/ic_launcher-playstore.png" align="right" width="90" />

# PerfPuppy

## Getting started

To compile the project you neeed JDK 11 and the standard Android toolchain.

## Code walkthough

The project uses a standard MVVM design with repository pattern. If you're unfamiliar with that pattern please have a look at [this guide](https://developer.android.com/jetpack/guide) to get started.

The UI part is very simple, it only contains a single Activity (`MainActivity`) with a 3-tabs layout.

Packages are organized by screen (UI) and features (non UI) as follows:

* `ui`

    - `dashboard`: `Fragment` and `ViewModel` of the "Dashboard" tab
    - `alerts`: `Fragment` and `ViewModel` of the "Alerts" tab
    - `settings`: `Fragment` and `ViewModel` of the "Settings" tab

* 