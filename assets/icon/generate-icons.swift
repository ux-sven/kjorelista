// Genererer app-ikon, Android adaptive-lag, splash og favicon til assets/.
// Kjør fra repoets rot:  swift assets/icon/generate-icons.swift assets/icon
// Kopier deretter resultatet inn i ios/Kjorelista/Assets.xcassets og android/app/src/main/res/mipmap-*.
//
// Motiv: en svingete vei som leder fram til en destinasjonsnål – «turen som logges».
import AppKit
import CoreGraphics

let outDir = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "assets")

func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: a)
}

let skyTop = rgb(0x4FB0EE)
let skyBottom = rgb(0x1C64A8)
let roadColor = rgb(0xFFFFFF)
let dashColor = rgb(0x3498DB)
let pinColor = rgb(0x27AE60)

// Veiens senterlinje (kubisk Bézier) i enhetskoordinater, origo øverst til venstre
let p0 = CGPoint(x: 0.36, y: 1.10), p1 = CGPoint(x: 0.24, y: 0.74)
let p2 = CGPoint(x: 0.84, y: 0.66), p3 = CGPoint(x: 0.58, y: 0.40)

func bezier(_ t: CGFloat) -> CGPoint {
    let u = 1 - t
    let a = u * u * u, b = 3 * u * u * t, c = 3 * u * t * t, d = t * t * t
    return CGPoint(x: a * p0.x + b * p1.x + c * p2.x + d * p3.x, y: a * p0.y + b * p1.y + c * p2.y + d * p3.y)
}

func tangent(_ t: CGFloat) -> CGPoint {
    let u = 1 - t
    let x = 3 * u * u * (p1.x - p0.x) + 6 * u * t * (p2.x - p1.x) + 3 * t * t * (p3.x - p2.x)
    let y = 3 * u * u * (p1.y - p0.y) + 6 * u * t * (p2.y - p1.y) + 3 * t * t * (p3.y - p2.y)
    let len = max(sqrt(x * x + y * y), 0.0001)
    return CGPoint(x: x / len, y: y / len)
}

// Perspektiv: veien smalner av mot horisonten
func roadWidth(_ t: CGFloat) -> CGFloat { 0.03 + 0.44 * pow(1 - t, 1.35) }

func band(from t0: CGFloat, to t1: CGFloat, width: (CGFloat) -> CGFloat) -> CGPath {
    let path = CGMutablePath()
    let steps = 120
    var left: [CGPoint] = [], right: [CGPoint] = []
    for i in 0...steps {
        let t = t0 + (t1 - t0) * CGFloat(i) / CGFloat(steps)
        let c = bezier(t), d = tangent(t), w = width(t) / 2
        left.append(CGPoint(x: c.x - d.y * w, y: c.y + d.x * w))
        right.append(CGPoint(x: c.x + d.y * w, y: c.y - d.x * w))
    }
    path.addLines(between: left + right.reversed())
    path.closeSubpath()
    return path
}

func pinPath() -> (outer: CGPath, hole: CGPath) {
    let tip = p3
    let r: CGFloat = 0.115
    let center = CGPoint(x: tip.x, y: tip.y - 0.205)
    let outer = CGMutablePath()
    // Dråpeform: sirkel + tangentlinjer ned til spissen
    let beta = acos(r / (tip.y - center.y))
    outer.move(to: tip)
    outer.addArc(center: center, radius: r, startAngle: .pi / 2 + beta, endAngle: .pi / 2 - beta, clockwise: false)
    outer.closeSubpath()
    let hole = CGPath(ellipseIn: CGRect(x: center.x - r * 0.42, y: center.y - r * 0.42, width: r * 0.84, height: r * 0.84), transform: nil)
    return (outer, hole)
}

enum Mode { case full, foreground, monochrome }

func drawGlyph(_ ctx: CGContext, mode: Mode) {
    // Litt luft over nålen
    ctx.saveGState()
    defer { ctx.restoreGState() }
    ctx.translateBy(x: 0, y: 0.035)
    let roadFill = mode == .monochrome ? rgb(0xFFFFFF) : roadColor
    ctx.setFillColor(roadFill)
    ctx.addPath(band(from: 0, to: 1, width: roadWidth))
    ctx.fillPath()

    // Midtstripe – stiplet, kortere mot horisonten
    let dashes: [(CGFloat, CGFloat)] = [(0.0, 0.10), (0.20, 0.29), (0.38, 0.45), (0.54, 0.59), (0.68, 0.71), (0.79, 0.81)]
    if mode == .monochrome { ctx.setBlendMode(.clear) } else { ctx.setFillColor(dashColor) }
    for (a, b) in dashes {
        ctx.addPath(band(from: a, to: b) { roadWidth($0) * 0.075 })
        ctx.fillPath()
    }
    ctx.setBlendMode(.normal)

    // Destinasjonsnål med hvit kant
    let pin = pinPath()
    if mode != .monochrome {
        ctx.setStrokeColor(rgb(0xFFFFFF))
        ctx.setLineWidth(0.026)
        ctx.setLineJoin(.round)
        ctx.addPath(pin.outer)
        ctx.strokePath()
    }
    ctx.setFillColor(mode == .monochrome ? rgb(0xFFFFFF) : pinColor)
    ctx.addPath(pin.outer)
    ctx.fillPath()
    if mode == .monochrome { ctx.setBlendMode(.clear) } else { ctx.setFillColor(rgb(0xFFFFFF)) }
    ctx.addPath(pin.hole)
    ctx.fillPath()
    ctx.setBlendMode(.normal)
}

func drawBackground(_ ctx: CGContext) {
    let gradient = CGGradient(colorsSpace: CGColorSpace(name: CGColorSpace.sRGB), colors: [skyTop, skyBottom] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(gradient, start: CGPoint(x: 0.2, y: 0), end: CGPoint(x: 0.8, y: 1), options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
}

func render(_ name: String, size: Int, draw: (CGContext) -> Void) {
    let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
                        space: CGColorSpace(name: CGColorSpace.sRGB)!, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    // Enhetskoordinater med origo øverst til venstre
    ctx.translateBy(x: 0, y: CGFloat(size))
    ctx.scaleBy(x: CGFloat(size), y: -CGFloat(size))
    draw(ctx)
    let rep = NSBitmapImageRep(cgImage: ctx.makeImage()!)
    try! rep.representation(using: .png, properties: [:])!.write(to: outDir.appendingPathComponent(name))
    print("✓ \(name) \(size)x\(size)")
}

// Skaler rundt midten (Android-lagene beskjæres av en maske)
func scaled(_ ctx: CGContext, _ s: CGFloat, _ body: () -> Void) {
    ctx.saveGState()
    ctx.translateBy(x: 0.5, y: 0.5)
    ctx.scaleBy(x: s, y: s)
    ctx.translateBy(x: -0.5, y: -0.5)
    body()
    ctx.restoreGState()
}

// iOS / generelt: fullflate, iOS legger på avrundede hjørner selv
render("icon.png", size: 1024) { ctx in
    drawBackground(ctx)
    drawGlyph(ctx, mode: .full)
}

// Android adaptive icon: 108dp-lag der kun midtre 72dp vises
render("android-icon-background.png", size: 1024) { ctx in drawBackground(ctx) }
render("android-icon-foreground.png", size: 1024) { ctx in
    ctx.addRect(CGRect(x: 0, y: 0, width: 1, height: 1)); ctx.clip()
    scaled(ctx, 0.62) { drawGlyph(ctx, mode: .foreground) }
}
render("android-icon-monochrome.png", size: 1024) { ctx in
    ctx.addRect(CGRect(x: 0, y: 0, width: 1, height: 1)); ctx.clip()
    scaled(ctx, 0.62) { drawGlyph(ctx, mode: .monochrome) }
}

// Splash og favicon: ikonet med avrundede hjørner på transparent bakgrunn
func roundedIcon(_ ctx: CGContext) {
    ctx.addPath(CGPath(roundedRect: CGRect(x: 0, y: 0, width: 1, height: 1), cornerWidth: 0.2237, cornerHeight: 0.2237, transform: nil))
    ctx.clip()
    drawBackground(ctx)
    drawGlyph(ctx, mode: .full)
}
render("splash-icon.png", size: 1024, draw: roundedIcon)
render("favicon.png", size: 48, draw: roundedIcon)
