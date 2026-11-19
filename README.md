# Jam

A lightweight macOS system-tray utility that monitors Java Virtual Machine (JVM) heap memory in real time and lets you reclaim it on demand — so you always know how much memory your Java apps are using.

**Version:** 1.1.9 (build 411) · © 2007 netbridgelimited.com

---

## What it does

Jam lives in your macOS menu bar and continuously displays how much heap memory the JVM has allocated and is using. A colour-coded indicator (blue → green → amber → red) tells you at a glance whether memory pressure is low or approaching the limit. When memory gets tight, you can trigger a garbage collection directly from the menu without touching a terminal.

It also reports usage against both the JVM's maximum heap and the machine's total physical RAM.

---

## Features

- **Live memory readout** in the menu bar — JVM heap usage as a percentage, with GB used vs. available.
- **Colour-coded status icon** that changes as heap usage crosses thresholds:
  - 🔵 Blue — under 25%
  - 🟢 Green — 25–50%
  - 🟠 Amber — 50–75%
  - 🔴 Red — 75% and above
- **Rich tooltip** showing JVM and system-wide memory breakdown.
- **Run Garbage Collector** — one click to call `System.gc()` and refresh the status.
- **Load at Startup** — optionally registers a `LaunchAgent` so Jam launches automatically when you log in.
- **Show in App Switcher** — toggle the app's activation policy so it appears (or stays hidden) in ⌘-Tab.
- **About dialog** with version and build info.
- **Single-instance enforcement** — a loopback socket lock prevents multiple copies from running; launching a second copy notifies the existing instance instead.
- **Native macOS notifications** via `osascript` for startup and already-running messages.

---

## Requirements

- **macOS** (uses `SystemTray`, `osascript`, `sysctl`, `ps`, and the `com.apple.eawt` API).
- **Java** — compiled for a JDK that supports Java 8+ language features (anonymous inner classes, etc.).
- A system tray (standard on macOS).

> Note: Jam relies on macOS-specific command-line tools (`sysctl hw.memsize`, `/usr/bin/vmmap`, `ps`) to read system memory and Java process information, so it will not run unmodified on Windows or Linux.

---

## Building

The project is a NetBeans Java project and can be built with NetBeans or Ant.

**With NetBeans:**
1. Open the `JAM` folder as a project.
2. Run **Clean and Build** (or press `Shift+F11`).

**With Ant:**
```bash
ant clean jar
```

The build outputs:

- `build/classes/` — compiled `.class` files
- `dist/Jam.jar` — the runnable application JAR

A pre-packaged macOS app bundle is also included at `Jam.app/`.

---

## Running

**From the JAR:**
```bash
java -jar dist/Jam.jar
```

**From the macOS app bundle:**
```bash
open Jam.app
```

On launch, Jam installs itself into the menu bar. If another instance is already running, you'll receive a notification instead of a second copy starting.

### Menu options

| Menu item | Action |
|-----------|--------|
| **About** | Shows version, build, and description. |
| **Run Garbage Collector** | Calls `System.gc()` and refreshes the memory status. |
| **Load at Startup** | Checkbox — installs/removes `~/Library/LaunchAgents/com.netbridgelimited.jam.plist`. |
| **Show in App Switcher** | Toggles the app's macOS activation policy (ACCESSORY). |
| **Quit** | Exits Jam. |

---

## How it works

- **Memory sampling:** `Runtime.totalMemory() - Runtime.freeMemory()` gives JVM heap usage; `Runtime.maxMemory()` gives the heap ceiling. `sysctl hw.memsize` supplies total physical RAM.
- **Status rendering:** a `BufferedImage` is generated on each refresh containing the "M" icon plus a formatted text string, then set as the tray icon image.
- **Single instance:** Jam binds a `ServerSocket` on `127.0.0.1:49276`. A second launch connects and sends `show`, prompting the running instance to display an "already running" notification.
- **Startup:** writes a `LaunchAgent` plist that runs `java -jar <path>/dist/Jam.jar` at login.
- **Diagnostics:** on startup Jam also prints heap usage (bytes and MB) to stdout, allocates a 10 MB test array, and prints usage again after suggesting a GC — useful for verifying the memory-reading code.

---

## Project layout

```
JAM/
├── src/jam/Jam.java              # Application source
├── build.xml                     # Ant build script
├── manifest.mf                   # JAR manifest
├── nbproject/                    # NetBeans project metadata
├── resources/                    # App icons (.icns, .iconset, PNG)
├── Jam.app/                      # macOS app bundle
├── dist/                         # Build output (Jam.jar)
└── build/                        # Compiled classes
```

---

## Known limitations

- `System.gc()` is only a *suggestion* to the JVM — collection is not guaranteed to run immediately.
- The `Show in App Switcher` toggle uses the internal `com.apple.eawt.Application` API, which is not part of the public Java SE API and may require `--add-exports` / `--add-opens` flags on newer JDKs.
- System memory figures use hard-coded macOS commands and paths.

---

## License

© 2007 netbridgelimited.com. All rights reserved.
