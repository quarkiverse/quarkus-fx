// Lists the display modes of the main display and switches it, for the login session, to its largest mode at scale 1
// (points = pixels) up to 3840x2160 when that is larger than the current one : the showcase main window is 1400x900 at
// 40,40. CI only (a hosted macOS runner) : never on a user's Mac. Run with: xcrun swift macos-display.swift
import CoreGraphics

let display = CGMainDisplayID()
func size() -> String { "\(CGDisplayPixelsWide(display))x\(CGDisplayPixelsHigh(display))" }
print("display \(display): \(size())")
let options = [kCGDisplayShowDuplicateLowResolutionModes as String: true] as CFDictionary
let modes = (CGDisplayCopyAllDisplayModes(display, options) as? [CGDisplayMode]) ?? []
print("modes: " + modes.map { "\($0.width)x\($0.height)" + ($0.pixelWidth == $0.width ? "" : " (pixels \($0.pixelWidth)x\($0.pixelHeight))") }
    .joined(separator: ", "))
let candidates = modes.filter { $0.isUsableForDesktopGUI() && $0.pixelWidth == $0.width && $0.width <= 3840 && $0.height <= 2160 }
if let best = candidates.max(by: { $0.width * $0.height < $1.width * $1.height }),
   best.width * best.height > CGDisplayPixelsWide(display) * CGDisplayPixelsHigh(display) {
    var config: CGDisplayConfigRef?
    CGBeginDisplayConfiguration(&config)
    CGConfigureDisplayWithDisplayMode(config, display, best, nil)
    let result = CGCompleteDisplayConfiguration(config, .forSession)
    print("switch to \(best.width)x\(best.height): \(result == .success ? "done" : "error \(result.rawValue)")")
}
print("display now: \(size())")
