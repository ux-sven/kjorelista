// Genererer typede tekstoppslag (L10n.generated.swift) fra Localizable.xcstrings.
//
// Localizable.xcstrings er fasit for tekstene på iOS. Legg til eller endre tekster der
// (i Xcode sin String Catalog-editor eller direkte i JSON), og kjør så fra ios/:
//   node scripts/generate-strings.mjs
//
// Nøkkelformat:
//   <seksjon>.<navn>  → t.<seksjon>.<navn>   (tekster delt med webløsningen)
//   app.<navn>        → s.<navn>             (tekster som bare finnes i appene)
//   months.<0-11>     → t.months[i]
import fs from 'node:fs'
import path from 'node:path'
import { fileURLToPath } from 'node:url'

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..')
const catalogPath = path.join(root, 'Kjorelista/Localization/Localizable.xcstrings')
const catalog = JSON.parse(fs.readFileSync(catalogPath, 'utf8'))
const keys = Object.keys(catalog.strings)

// Advar om tekster som mangler en av oversettelsene
for (const key of keys) {
  const loc = catalog.strings[key].localizations ?? {}
  for (const lang of ['nb', 'en']) {
    if (!loc[lang]?.stringUnit?.value) console.warn(`Mangler ${lang}: ${key}`)
  }
}

const sections = new Map()
const appKeys = []
for (const key of keys) {
  const [section, ...rest] = key.split('.')
  const name = rest.join('.')
  if (section === 'months') continue
  if (section === 'app') appKeys.push(name)
  else sections.set(section, [...(sections.get(section) ?? []), name])
}

const cap = (s) => s.charAt(0).toUpperCase() + s.slice(1)
let swift = `// GENERERT av scripts/generate-strings.mjs fra Localizable.xcstrings – ikke rediger for hånd.
import Foundation

/// Tekster delt med webløsningen. Bruk: \`t.trips.addNew\`.
struct Translations {
    let lookup: Lookup
`
for (const section of sections.keys()) swift += `    var ${section}: ${cap(section)} { ${cap(section)}(lookup: lookup) }\n`
swift += `    var months: [String] { (0..<12).map { lookup("months.\\($0)") } }\n\n`
for (const [section, names] of sections) {
  swift += `    struct ${cap(section)} {\n        let lookup: Lookup\n`
  for (const name of names) swift += `        var ${name}: String { lookup("${section}.${name}") }\n`
  swift += `    }\n\n`
}
swift = swift.trimEnd() + `\n}\n\n/// Tekster som bare finnes i appene. Bruk: \`s.settings\`.\nstruct AppStrings {\n    let lookup: Lookup\n`
for (const name of appKeys) swift += `    var ${name}: String { lookup("app.${name}") }\n`
swift += `}\n`

fs.writeFileSync(path.join(root, 'Kjorelista/Localization/L10n.generated.swift'), swift)
console.log(`Skrev oppslag for ${keys.length} tekster`)
