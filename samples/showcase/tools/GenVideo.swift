// Generates the showcase test video (H.264 MP4) : swift tools/GenVideo.swift <output.mp4>
// Each second has its own background color, a white square moves left to right,
// and the frame number is encoded as small white squares (binary) in the top left corner.
import AVFoundation
import CoreGraphics
import CoreVideo
import Foundation

let url = URL(fileURLWithPath: CommandLine.arguments.last!)
try? FileManager.default.removeItem(at: url)

let width = 320
let height = 180
let fps: Int32 = 30
let seconds = 3

let writer = try AVAssetWriter(outputURL: url, fileType: .mp4)
let settings: [String: Any] = [
    AVVideoCodecKey: AVVideoCodecType.h264,
    AVVideoWidthKey: width,
    AVVideoHeightKey: height,
]
let input = AVAssetWriterInput(mediaType: .video, outputSettings: settings)
input.expectsMediaDataInRealTime = false
let adaptor = AVAssetWriterInputPixelBufferAdaptor(
    assetWriterInput: input,
    sourcePixelBufferAttributes: [
        kCVPixelBufferPixelFormatTypeKey as String: kCVPixelFormatType_32ARGB,
        kCVPixelBufferWidthKey as String: width,
        kCVPixelBufferHeightKey as String: height,
    ])
writer.add(input)
writer.startWriting()
writer.startSession(atSourceTime: .zero)

let colors: [(CGFloat, CGFloat, CGFloat)] = [(0.85, 0.2, 0.2), (0.2, 0.6, 0.25), (0.2, 0.35, 0.85)]
let frames = Int(fps) * seconds
for frame in 0..<frames {
    while !input.isReadyForMoreMediaData { usleep(1000) }
    var pixelBuffer: CVPixelBuffer?
    CVPixelBufferPoolCreatePixelBuffer(nil, adaptor.pixelBufferPool!, &pixelBuffer)
    let buffer = pixelBuffer!
    CVPixelBufferLockBaseAddress(buffer, [])
    let context = CGContext(
        data: CVPixelBufferGetBaseAddress(buffer), width: width, height: height, bitsPerComponent: 8,
        bytesPerRow: CVPixelBufferGetBytesPerRow(buffer), space: CGColorSpaceCreateDeviceRGB(),
        bitmapInfo: CGImageAlphaInfo.noneSkipFirst.rawValue)!
    let color = colors[(frame / Int(fps)) % colors.count]
    context.setFillColor(red: color.0, green: color.1, blue: color.2, alpha: 1)
    context.fill(CGRect(x: 0, y: 0, width: width, height: height))
    context.setFillColor(red: 1, green: 1, blue: 1, alpha: 1)
    let x = CGFloat(frame) / CGFloat(frames - 1) * CGFloat(width - 40)
    context.fill(CGRect(x: x, y: 70, width: 40, height: 40))
    for bit in 0..<7 where (frame >> bit) & 1 == 1 {
        context.fill(CGRect(x: 10 + bit * 14, y: height - 20, width: 10, height: 10))
    }
    CVPixelBufferUnlockBaseAddress(buffer, [])
    adaptor.append(buffer, withPresentationTime: CMTime(value: CMTimeValue(frame), timescale: fps))
}
input.markAsFinished()
let done = DispatchSemaphore(value: 0)
writer.finishWriting { done.signal() }
done.wait()
print(writer.status == .completed ? "video ok: \(url.path)" : "video failed: \(String(describing: writer.error))")
