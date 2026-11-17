// Henter ordmerket «KJØRELISTA» ut av logo-inspirasjon.png som gjennomsiktig PNG.
// Kjør fra repoets rot:  swift assets/icon/extract-wordmark.swift assets/icon
// Gir wordmark-gold.png (til lilla/mørk bakgrunn) og wordmark-purple.png (til lys bakgrunn).
import AppKit

let dir = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "assets/icon")
let src = NSBitmapImageRep(data: NSImage(contentsOf: dir.appendingPathComponent("logo-inspirasjon.png"))!.tiffRepresentation!)!

// Ordmerket står til høyre for bilen i bildet
let crop = (x0: 282, x1: 1008, y0: 30, y1: 152)
let w = crop.x1 - crop.x0, h = crop.y1 - crop.y0

func luminance(_ c: NSColor) -> CGFloat { 0.2126 * c.redComponent + 0.7152 * c.greenComponent + 0.0722 * c.blueComponent }
let goldL = luminance(NSColor(srgbRed: 0xDB / 255, green: 0xC3 / 255, blue: 0x88 / 255, alpha: 1))

func render(_ name: String, r: CGFloat, g: CGFloat, b: CGFloat) {
    let out = NSBitmapImageRep(bitmapDataPlanes: nil, pixelsWide: w, pixelsHigh: h, bitsPerSample: 8, samplesPerPixel: 4,
                               hasAlpha: true, isPlanar: false, colorSpaceName: .deviceRGB, bytesPerRow: 0, bitsPerPixel: 0)!
    for y in 0..<h {
        for x in 0..<w {
            let c = src.colorAt(x: crop.x0 + x, y: crop.y0 + y)!.usingColorSpace(.sRGB)!
            // Lokal bakgrunn: samme rad, helt til venstre i utsnittet (gradienten er tilnærmet vannrett)
            let bg = src.colorAt(x: crop.x0 + x, y: 8)!.usingColorSpace(.sRGB)!
            let a = max(0, min(1, (luminance(c) - luminance(bg)) / (goldL - luminance(bg))))
            out.setColor(NSColor(deviceRed: r, green: g, blue: b, alpha: a), atX: x, y: y)
        }
    }
    try! out.representation(using: .png, properties: [:])!.write(to: dir.appendingPathComponent(name))
    print("✓ \(name) \(w)x\(h)")
}
render("wordmark-gold.png", r: 0xDB / 255, g: 0xC3 / 255, b: 0x88 / 255)
render("wordmark-purple.png", r: 0x4E / 255, g: 0x3F / 255, b: 0x66 / 255)
