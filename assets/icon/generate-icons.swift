// Genererer app-ikon, Android adaptive-lag, splash og ordmerke til assets/icon/.
// Kjør fra repoets rot:  swift assets/icon/generate-icons.swift assets/icon
// Kopier deretter resultatet inn i ios/Kjorelista/Assets.xcassets og android/app/src/main/res/.
//
// Profil (fra logoen): lilla gradient #63517B → #7E6F91 og gull #DBC388.
// Motiv: bil sett ovenfra, tegnet som vektor i samme stil som bilen i logoen.
import AppKit
import CoreGraphics

let outDir = URL(fileURLWithPath: CommandLine.arguments.count > 1 ? CommandLine.arguments[1] : "assets/icon")

func rgb(_ hex: UInt32, _ a: CGFloat = 1) -> CGColor {
    CGColor(srgbRed: CGFloat((hex >> 16) & 0xFF) / 255, green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255, alpha: a)
}

let purpleLight = rgb(0x7E6F91)
let purpleDark = rgb(0x4E3F66)
let gold = rgb(0xDBC388)
let sRGB = CGColorSpace(name: CGColorSpace.sRGB)!

func bitmap(_ size: Int) -> CGContext {
    let ctx = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: 0,
                        space: sRGB, bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue)!
    // Enhetskoordinater med origo øverst til venstre
    ctx.translateBy(x: 0, y: CGFloat(size))
    ctx.scaleBy(x: CGFloat(size), y: -CGFloat(size))
    return ctx
}

// MARK: - Bilen (front mot høyre). Tegnes i en boks på 1 × 0.56 og skaleres inn.

let carHeight: CGFloat = 0.56
/// Speiler en form fra øvre til nedre halvdel av bilen
func mirrored(_ path: CGPath) -> CGPath {
    var t = CGAffineTransform(translationX: 0, y: carHeight).scaledBy(x: 1, y: -1)
    return path.copy(using: &t)!
}

func carParts() -> [CGPath] {
    let p = CGMutablePath()
    // Avrundet karosseri, rundere foran enn bak (som en boble sett ovenfra)
    p.move(to: CGPoint(x: 0.15, y: 0.035))
    p.addLine(to: CGPoint(x: 0.70, y: 0.03))
    p.addCurve(to: CGPoint(x: 1.0, y: 0.28), control1: CGPoint(x: 0.93, y: 0.03), control2: CGPoint(x: 1.0, y: 0.12))
    p.addCurve(to: CGPoint(x: 0.70, y: 0.53), control1: CGPoint(x: 1.0, y: 0.44), control2: CGPoint(x: 0.93, y: 0.53))
    p.addLine(to: CGPoint(x: 0.15, y: 0.525))
    p.addCurve(to: CGPoint(x: 0.0, y: 0.28), control1: CGPoint(x: 0.04, y: 0.52), control2: CGPoint(x: 0.0, y: 0.43))
    p.addCurve(to: CGPoint(x: 0.15, y: 0.035), control1: CGPoint(x: 0.0, y: 0.13), control2: CGPoint(x: 0.04, y: 0.04))
    p.closeSubpath()
    // Speil (øvre, og speilet nedre)
    let mirror = CGMutablePath()
    mirror.move(to: CGPoint(x: 0.625, y: 0.045))
    mirror.addLine(to: CGPoint(x: 0.655, y: -0.03))
    mirror.addQuadCurve(to: CGPoint(x: 0.685, y: -0.015), control: CGPoint(x: 0.675, y: -0.032))
    mirror.addLine(to: CGPoint(x: 0.68, y: 0.045))
    mirror.closeSubpath()
    return [p, mirror, mirrored(mirror)]
}

func carCutouts() -> [CGPath] {
    var paths: [CGPath] = []
    // Bakrute
    let rear = CGMutablePath()
    rear.move(to: CGPoint(x: 0.255, y: 0.105))
    rear.addCurve(to: CGPoint(x: 0.255, y: 0.455), control1: CGPoint(x: 0.235, y: 0.2), control2: CGPoint(x: 0.235, y: 0.36))
    rear.addLine(to: CGPoint(x: 0.135, y: 0.425))
    rear.addCurve(to: CGPoint(x: 0.135, y: 0.135), control1: CGPoint(x: 0.10, y: 0.33), control2: CGPoint(x: 0.10, y: 0.23))
    rear.closeSubpath()
    paths.append(rear)
    // Frontrute
    let front = CGMutablePath()
    front.move(to: CGPoint(x: 0.605, y: 0.095))
    front.addLine(to: CGPoint(x: 0.75, y: 0.075))
    front.addCurve(to: CGPoint(x: 0.75, y: 0.485), control1: CGPoint(x: 0.85, y: 0.17), control2: CGPoint(x: 0.85, y: 0.39))
    front.addLine(to: CGPoint(x: 0.605, y: 0.465))
    front.addCurve(to: CGPoint(x: 0.605, y: 0.095), control1: CGPoint(x: 0.625, y: 0.36), control2: CGPoint(x: 0.625, y: 0.2))
    front.closeSubpath()
    paths.append(front)
    // Sidevindu: smal kile langs taket, tynn bak og bredere foran
    let side = CGMutablePath()
    side.move(to: CGPoint(x: 0.27, y: 0.092))
    side.addQuadCurve(to: CGPoint(x: 0.62, y: 0.062), control: CGPoint(x: 0.45, y: 0.068))
    side.addLine(to: CGPoint(x: 0.595, y: 0.093))
    side.addQuadCurve(to: CGPoint(x: 0.27, y: 0.096), control: CGPoint(x: 0.43, y: 0.098))
    side.closeSubpath()
    paths.append(side)
    paths.append(mirrored(side))
    // Frontlykt og baklys
    let headlight = CGPath(ellipseIn: CGRect(x: 0.885, y: 0.095, width: 0.05, height: 0.035), transform: nil)
    let taillight = CGPath(roundedRect: CGRect(x: 0.032, y: 0.15, width: 0.012, height: 0.07), cornerWidth: 0.006, cornerHeight: 0.006, transform: nil)
    paths += [headlight, mirrored(headlight), taillight, mirrored(taillight)]
    return paths
}

/// Tegner bilen i `color` innenfor bredden `width`, sentrert i (cx, cy), i enhetskoordinater.
func drawCar(_ ctx: CGContext, color: CGColor, width: CGFloat, cx: CGFloat = 0.5, cy: CGFloat = 0.5) {
    // Egen gjennomsiktig flate slik at vinduene kan «klippes ut» uten å kjenne bakgrunnen
    ctx.beginTransparencyLayer(auxiliaryInfo: nil)
    ctx.saveGState()
    ctx.translateBy(x: cx - width / 2, y: cy - width * carHeight / 2)
    ctx.scaleBy(x: width, y: width)
    ctx.setFillColor(color)
    // Hver del fylles for seg: det speilede speilet har motsatt retning og ville ellers gitt hull der det overlapper
    for part in carParts() {
        ctx.addPath(part)
        ctx.fillPath()
    }
    ctx.setBlendMode(.clear)
    for cut in carCutouts() {
        ctx.addPath(cut)
        ctx.fillPath()
    }
    ctx.restoreGState()
    ctx.endTransparencyLayer()
}

func drawBackground(_ ctx: CGContext) {
    let gradient = CGGradient(colorsSpace: sRGB, colors: [purpleLight, purpleDark] as CFArray, locations: [0, 1])!
    ctx.drawLinearGradient(gradient, start: CGPoint(x: 0.85, y: 0), end: CGPoint(x: 0.15, y: 1),
                           options: [.drawsBeforeStartLocation, .drawsAfterEndLocation])
}

func write(_ ctx: CGContext, _ name: String, opaque: Bool = false) {
    var image = ctx.makeImage()!
    if opaque {
        // App Store krever ikon uten alfakanal
        let flat = CGContext(data: nil, width: image.width, height: image.height, bitsPerComponent: 8, bytesPerRow: 0,
                             space: sRGB, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
        flat.draw(image, in: CGRect(x: 0, y: 0, width: image.width, height: image.height))
        image = flat.makeImage()!
    }
    let rep = NSBitmapImageRep(cgImage: image)
    try! rep.representation(using: .png, properties: [:])!.write(to: outDir.appendingPathComponent(name))
    print("✓ \(name) \(image.width)x\(image.height)")
}

// iOS / App Store: full flate, iOS runder hjørnene selv
do {
    let ctx = bitmap(1024)
    drawBackground(ctx)
    drawCar(ctx, color: gold, width: 0.74)
    write(ctx, "icon.png", opaque: true)
}

// Android adaptive icon: 108dp-lag der kun midtre 72dp vises (sirkel 66dp er trygg sone)
do {
    let ctx = bitmap(1024)
    drawBackground(ctx)
    write(ctx, "android-icon-background.png")
}
do {
    let ctx = bitmap(1024)
    drawCar(ctx, color: gold, width: 0.5)
    write(ctx, "android-icon-foreground.png")
}
do {
    let ctx = bitmap(1024)
    drawCar(ctx, color: rgb(0xFFFFFF), width: 0.5)
    write(ctx, "android-icon-monochrome.png")
}

// Splash / innloggingslogo: ikonet med avrundede hjørner på transparent bakgrunn
do {
    let ctx = bitmap(1024)
    ctx.addPath(CGPath(roundedRect: CGRect(x: 0, y: 0, width: 1, height: 1), cornerWidth: 0.2237, cornerHeight: 0.2237, transform: nil))
    ctx.clip()
    drawBackground(ctx)
    drawCar(ctx, color: gold, width: 0.74)
    write(ctx, "splash-icon.png")
}

// Bilen alene i gull (brukes i appen ved siden av ordmerket)
do {
    let ctx = bitmap(1024)
    drawCar(ctx, color: gold, width: 0.96)
    write(ctx, "car-gold.png")
}
