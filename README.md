<img src="app/src/main/ic_launcher-playstore.png" align="right" width="90" />

# PerfPuppy

## Getting started

To compile the project you neeed JDK 11 and the standard Android toolchain.

## Code overview

The project uses a standard MVVM design with repository pattern. If you're unfamiliar with that pattern please have a look at [this guide](https://developer.android.com/jetpack/guide) to get started.

The UI part is very simple, it only contains a single Activity (`MainActivity`) with a 3-tabs layout.

Packages are organized by screen (UI) and features (non UI) as follows:

* `ui`
    - `dashboard`: `Fragment` and `ViewModel` of the "Dashboard" tab
    - `alerts`: `Fragment` and `ViewModel` of the "Alerts" tab
    - `settings`: `Fragment` and `ViewModel` of the "Settings" tab
* `domain`: models for the UI (alerts)
* `database`: models for the database (alerts) and DAO
* `repository`: single source of truth for data (alerts)
* `di`: dependency injection stuff
* `data`: agents and service to collect data (cpu, mem, battery)

## Data collection

The core part of the app is data collection, that is handled by `CollectionService`; to start/stop data collection just start and stop that service with `context.startService()` and `context.stopService()`. This is done in the `DashboardFragment` when the user clicks the "Enable data collection" button. By default data collection happens also when the app is in background, if the user has opted out this feature then the `CollectionService` is stopped by the `MainActivity` when the app is no longer visible to the user (`onStop`).

The data flow is as follows:

* `CollectorService` creates a separate thread and spawns all agents on that thread.
* Each `Agent` collects the data (for example the cpu load) and reports it back to `CollectorService` thru `CollectorServiceCallback`
* `CollectorService` is then responsible for creating notifications and storing the alert in the repository.

`TODO`: add a diagram with the flow

### Background data collection

To run in the background the `CollectorService` calls `startForeground()` and creates a visible notification. This drammatically reduces the chances of being killed by the OS.

### How to add a new Agent

To create a new agent just subclass the `Agent` and implement its abstract methods:

```kotlin
    override val name: String
        get() = context.getString(R.string.mem_agent_name)

    override fun aboveThMessage(value: Int): String =
        context.getString(R.string.mem_above_th_message, value)

    override fun belowThMessage(value: Int): String =
        context.getString(R.string.mem_below_th_message)
```

`Agent` is a generic data collection class that runs an infinite loop and collects data every X seconds. 

If the new agent follows the same logic (collect data, wait for it, collect data, ...) then you should just implement `getData()` and return the data; as an example you can see the `MemoryAgent` which the simplest agent possible:

```kotlin
    override suspend fun getData(): PerfValue {
        val th = prefs.getInt(
            context.getString(R.string.mem_alert_pref_key),
            context.resources.getInteger(R.integer.mem_alert_default_th)
        )

        return parseProcMemInfo().toPerfValue(th)
    }
```

If the new agent has a completely different logic then you should override `enable()` and avoid calling `super()` so that the collect loop is not created. Then, just call setData() directly to report back to `CollectorService`; see `BatteryAgent` as an example of such logic (that agent relies on Android intents that are sent out when battery level changes).

Plase notice the value reported to `CollectorService` is a `PerfValue` object with two fields:

* the actual `value` expressed in percentage (e.g. "80")
* a boolean to tell if `valueIsAboveTh`: this is needed because "above the threshold" actually means "in an error state" and the logic could be agent-specific (for example `BatteryAgent` sets `valueIsAboveTh` to `true` when the value is below the threshold).

### CPU load

Starting from Android O it's no longer possible to collect cpu usage bacause it's an "information leak". See this [official thread](https://issuetracker.google.com/issues/37140047?pli=1). In the app I've implemented a few workarounds but they only work on older and rooted devices. In the market there are a few "cpu monitoring" tools, all of them either don't work or use the cpu frequency to assume the load. Since this approach is totally misleading I've decided not to implement it and just return 0 if no reliable way is possible.


