# Geolocation API Support for uni-dom

## Overview

Add a `Geolocation` object that provides reactive geolocation tracking using the browser's Geolocation API.

## Goals

1. Provide reactive position tracking with `Rx[GeoPosition]`
2. Support both one-shot and continuous position watching
3. Handle errors gracefully (permission denied, unavailable, timeout)
4. Support configuration options (high accuracy, timeout, max age)

## API Design

### Core Types

```scala
// Position data from the Geolocation API
case class GeoPosition(
  latitude: Double,
  longitude: Double,
  altitude: Option[Double],
  accuracy: Double,
  altitudeAccuracy: Option[Double],
  heading: Option[Double],
  speed: Option[Double],
  timestamp: Long
)

// Geolocation errors
enum GeoError:
  case PermissionDenied
  case PositionUnavailable
  case Timeout
  case NotSupported

// Options for position requests
case class GeoOptions(
  enableHighAccuracy: Boolean = false,
  timeout: Long = Long.MaxValue,
  maximumAge: Long = 0
)
```

### Geolocation Object

```scala
object Geolocation:
  // Check if geolocation is supported
  def isSupported: Boolean

  // One-shot position request
  def getCurrentPosition(
    onSuccess: GeoPosition => Unit,
    onError: GeoError => Unit = _ => (),
    options: GeoOptions = GeoOptions()
  ): Unit

  // Continuous position watching - returns a watcher that can be cancelled
  def watch(options: GeoOptions = GeoOptions()): PositionWatcher

  class PositionWatcher extends Cancelable:
    def position: Rx[Option[GeoPosition]]  // None until first position
    def error: Rx[Option[GeoError]]        // None if no error
    def lastPosition: Option[GeoPosition]
    def lastError: Option[GeoError]
    def cancel: Unit
```

### Usage Examples

```scala
import wvlet.uni.dom.all.*

// Check if supported
if Geolocation.isSupported then
  // One-shot position
  Geolocation.getCurrentPosition(
    onSuccess = pos => println(s"Location: ${pos.latitude}, ${pos.longitude}"),
    onError = err => println(s"Error: ${err}")
  )

// Continuous watching with reactive updates
val watcher = Geolocation.watch(GeoOptions(enableHighAccuracy = true))

div(
  watcher.position.map {
    case Some(pos) => span(s"Lat: ${pos.latitude}, Lon: ${pos.longitude}")
    case None => span("Waiting for location...")
  },
  watcher.error.map {
    case Some(GeoError.PermissionDenied) => span(cls := "error", "Location access denied")
    case Some(err) => span(cls := "error", s"Error: ${err}")
    case None => DomNode.empty
  }
)

// Don't forget to cancel when done
watcher.cancel
```

## Implementation Notes

- Use `dom.window.navigator.geolocation` from scala-js-dom
- Convert PositionError codes to GeoError enum
- Handle null/undefined values from optional coordinates
- PositionWatcher uses RxVar internally for reactive updates

## Test Plan

- Test GeoPosition case class
- Test GeoError enum values
- Test GeoOptions defaults
- Test Geolocation.isSupported returns Boolean
- Test PositionWatcher returns proper Rx types
- Test watch() returns PositionWatcher
