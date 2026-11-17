// GENERERT av scripts/generate-strings.mjs fra Localizable.xcstrings – ikke rediger for hånd.
import Foundation

/// Tekster delt med webløsningen. Bruk: `t.trips.addNew`.
struct Translations {
    let lookup: Lookup
    var auth: Auth { Auth(lookup: lookup) }
    var export: Export { Export(lookup: lookup) }
    var footer: Footer { Footer(lookup: lookup) }
    var general: General { General(lookup: lookup) }
    var header: Header { Header(lookup: lookup) }
    var navigation: Navigation { Navigation(lookup: lookup) }
    var privacy: Privacy { Privacy(lookup: lookup) }
    var summary: Summary { Summary(lookup: lookup) }
    var supabase: Supabase { Supabase(lookup: lookup) }
    var trips: Trips { Trips(lookup: lookup) }
    var months: [String] { (0..<12).map { lookup("months.\($0)") } }

    struct Auth {
        let lookup: Lookup
        var alreadyHaveAccount: String { lookup("auth.alreadyHaveAccount") }
        var backToLogin: String { lookup("auth.backToLogin") }
        var confirmPassword: String { lookup("auth.confirmPassword") }
        var createAccount: String { lookup("auth.createAccount") }
        var email: String { lookup("auth.email") }
        var emailRequired: String { lookup("auth.emailRequired") }
        var enterEmail: String { lookup("auth.enterEmail") }
        var firstName: String { lookup("auth.firstName") }
        var firstNameRequired: String { lookup("auth.firstNameRequired") }
        var forgotPassword: String { lookup("auth.forgotPassword") }
        var forgotPasswordDescription: String { lookup("auth.forgotPasswordDescription") }
        var forgotPasswordTitle: String { lookup("auth.forgotPasswordTitle") }
        var invalidCredentials: String { lookup("auth.invalidCredentials") }
        var lastName: String { lookup("auth.lastName") }
        var lastNameRequired: String { lookup("auth.lastNameRequired") }
        var noAccountYet: String { lookup("auth.noAccountYet") }
        var password: String { lookup("auth.password") }
        var passwordRequired: String { lookup("auth.passwordRequired") }
        var passwordTooShort: String { lookup("auth.passwordTooShort") }
        var passwordsDoNotMatch: String { lookup("auth.passwordsDoNotMatch") }
        var resetEmailSent: String { lookup("auth.resetEmailSent") }
        var resetPassword: String { lookup("auth.resetPassword") }
        var sendResetEmail: String { lookup("auth.sendResetEmail") }
        var signIn: String { lookup("auth.signIn") }
        var signInSuccess: String { lookup("auth.signInSuccess") }
        var signOut: String { lookup("auth.signOut") }
        var signOutConfirmation: String { lookup("auth.signOutConfirmation") }
        var signOutSuccess: String { lookup("auth.signOutSuccess") }
        var signUp: String { lookup("auth.signUp") }
        var signUpSuccess: String { lookup("auth.signUpSuccess") }
        var welcomeBack: String { lookup("auth.welcomeBack") }
    }

    struct Export {
        let lookup: Lookup
        var csv: String { lookup("export.csv") }
        var csvDescription: String { lookup("export.csvDescription") }
        var description: String { lookup("export.description") }
        var download: String { lookup("export.download") }
        var exporting: String { lookup("export.exporting") }
        var html: String { lookup("export.html") }
        var htmlDescription: String { lookup("export.htmlDescription") }
        var month: String { lookup("export.month") }
        var noTripsToExport: String { lookup("export.noTripsToExport") }
        var numberOfTrips: String { lookup("export.numberOfTrips") }
        var pdf: String { lookup("export.pdf") }
        var pdfDescription: String { lookup("export.pdfDescription") }
        var selectFormat: String { lookup("export.selectFormat") }
        var title: String { lookup("export.title") }
        var totalKm: String { lookup("export.totalKm") }
    }

    struct Footer {
        let lookup: Lookup
        var contactUs: String { lookup("footer.contactUs") }
        var copyright: String { lookup("footer.copyright") }
        var footerNavigation: String { lookup("footer.footerNavigation") }
        var privacyPolicy: String { lookup("footer.privacyPolicy") }
    }

    struct General {
        let lookup: Lookup
        var back: String { lookup("general.back") }
        var close: String { lookup("general.close") }
        var cloud: String { lookup("general.cloud") }
        var darkMode: String { lookup("general.darkMode") }
        var dataStoredLocally: String { lookup("general.dataStoredLocally") }
        var error: String { lookup("general.error") }
        var finish: String { lookup("general.finish") }
        var info: String { lookup("general.info") }
        var language: String { lookup("general.language") }
        var lightMode: String { lookup("general.lightMode") }
        var loading: String { lookup("general.loading") }
        var local: String { lookup("general.local") }
        var next: String { lookup("general.next") }
        var no: String { lookup("general.no") }
        var optional: String { lookup("general.optional") }
        var previous: String { lookup("general.previous") }
        var required: String { lookup("general.required") }
        var skipToMain: String { lookup("general.skipToMain") }
        var success: String { lookup("general.success") }
        var syncing: String { lookup("general.syncing") }
        var warning: String { lookup("general.warning") }
        var yes: String { lookup("general.yes") }
    }

    struct Header {
        let lookup: Lookup
        var subtitle: String { lookup("header.subtitle") }
        var tagline: String { lookup("header.tagline") }
        var title: String { lookup("header.title") }
    }

    struct Navigation {
        let lookup: Lookup
        var monthNavigation: String { lookup("navigation.monthNavigation") }
        var nextMonth: String { lookup("navigation.nextMonth") }
        var previousMonth: String { lookup("navigation.previousMonth") }
    }

    struct Privacy {
        let lookup: Lookup
        var changes: String { lookup("privacy.changes") }
        var changesContent: String { lookup("privacy.changesContent") }
        var close: String { lookup("privacy.close") }
        var contact: String { lookup("privacy.contact") }
        var contactContent: String { lookup("privacy.contactContent") }
        var cookies: String { lookup("privacy.cookies") }
        var cookiesContent: String { lookup("privacy.cookiesContent") }
        var dataController: String { lookup("privacy.dataController") }
        var dataHowWeUse: String { lookup("privacy.dataHowWeUse") }
        var dataHowWeUseContent: String { lookup("privacy.dataHowWeUseContent") }
        var dataSecurity: String { lookup("privacy.dataSecurity") }
        var dataSecurityContent: String { lookup("privacy.dataSecurityContent") }
        var dataSharing: String { lookup("privacy.dataSharing") }
        var dataSharingContent: String { lookup("privacy.dataSharingContent") }
        var dataStorage: String { lookup("privacy.dataStorage") }
        var dataStorageContent: String { lookup("privacy.dataStorageContent") }
        var dataWhatWeCollect: String { lookup("privacy.dataWhatWeCollect") }
        var dataWhatWeCollectContent: String { lookup("privacy.dataWhatWeCollectContent") }
        var introduction: String { lookup("privacy.introduction") }
        var lastUpdated: String { lookup("privacy.lastUpdated") }
        var title: String { lookup("privacy.title") }
        var userRights: String { lookup("privacy.userRights") }
        var userRightsContent: String { lookup("privacy.userRightsContent") }
    }

    struct Summary {
        let lookup: Lookup
        var addFirstTrip: String { lookup("summary.addFirstTrip") }
        var expenses: String { lookup("summary.expenses") }
        var export: String { lookup("summary.export") }
        var noTripsThisMonth: String { lookup("summary.noTripsThisMonth") }
        var printReport: String { lookup("summary.printReport") }
        var title: String { lookup("summary.title") }
        var totalDriveTime: String { lookup("summary.totalDriveTime") }
        var totalDriven: String { lookup("summary.totalDriven") }
        var totalDrivenInYear: String { lookup("summary.totalDrivenInYear") }
        var totalExpenses: String { lookup("summary.totalExpenses") }
        var totalInYear: String { lookup("summary.totalInYear") }
        var totalKilometers: String { lookup("summary.totalKilometers") }
        var totalTime: String { lookup("summary.totalTime") }
        var totalTrips: String { lookup("summary.totalTrips") }
        var tripsThisYear: String { lookup("summary.tripsThisYear") }
        var withPassengers: String { lookup("summary.withPassengers") }
        var year: String { lookup("summary.year") }
    }

    struct Supabase {
        let lookup: Lookup
        var createDatabase: String { lookup("supabase.createDatabase") }
        var createProject: String { lookup("supabase.createProject") }
        var getApiKeys: String { lookup("supabase.getApiKeys") }
        var of: String { lookup("supabase.of") }
        var setupTitle: String { lookup("supabase.setupTitle") }
        var step: String { lookup("supabase.step") }
        var updateCode: String { lookup("supabase.updateCode") }
    }

    struct Trips {
        let lookup: Lookup
        var addExpenses: String { lookup("trips.addExpenses") }
        var addNew: String { lookup("trips.addNew") }
        var cancel: String { lookup("trips.cancel") }
        var comment: String { lookup("trips.comment") }
        var commentDescription: String { lookup("trips.commentDescription") }
        var commentPlaceholder: String { lookup("trips.commentPlaceholder") }
        var confirmDelete: String { lookup("trips.confirmDelete") }
        var date: String { lookup("trips.date") }
        var dateRequired: String { lookup("trips.dateRequired") }
        var delete: String { lookup("trips.delete") }
        var deleteMessage: String { lookup("trips.deleteMessage") }
        var edit: String { lookup("trips.edit") }
        var expenseDescription: String { lookup("trips.expenseDescription") }
        var expensePlaceholder: String { lookup("trips.expensePlaceholder") }
        var hadPassengers: String { lookup("trips.hadPassengers") }
        var hours: String { lookup("trips.hours") }
        var hoursRequired: String { lookup("trips.hoursRequired") }
        var keyboardShortcuts: String { lookup("trips.keyboardShortcuts") }
        var kilometers: String { lookup("trips.kilometers") }
        var kilometersRequired: String { lookup("trips.kilometersRequired") }
        var minutes: String { lookup("trips.minutes") }
        var minutesRequired: String { lookup("trips.minutesRequired") }
        var noTrips: String { lookup("trips.noTrips") }
        var noTripsThisMonth: String { lookup("trips.noTripsThisMonth") }
        var otherExpenses: String { lookup("trips.otherExpenses") }
        var passenger: String { lookup("trips.passenger") }
        var passengerCount: String { lookup("trips.passengerCount") }
        var passengerCountRequired: String { lookup("trips.passengerCountRequired") }
        var passengerKilometers: String { lookup("trips.passengerKilometers") }
        var passengerKilometersRequired: String { lookup("trips.passengerKilometersRequired") }
        var passengers: String { lookup("trips.passengers") }
        var passengersDescription: String { lookup("trips.passengersDescription") }
        var requiredFields: String { lookup("trips.requiredFields") }
        var route: String { lookup("trips.route") }
        var routePlaceholder: String { lookup("trips.routePlaceholder") }
        var routeRequired: String { lookup("trips.routeRequired") }
        var save: String { lookup("trips.save") }
        var saveTrip: String { lookup("trips.saveTrip") }
        var saving: String { lookup("trips.saving") }
        var swipeInstructions: String { lookup("trips.swipeInstructions") }
        var thisMonth: String { lookup("trips.thisMonth") }
        var travelTime: String { lookup("trips.travelTime") }
        var trip: String { lookup("trips.trip") }
        var tripCount: String { lookup("trips.tripCount") }
        var tripDeleteError: String { lookup("trips.tripDeleteError") }
        var tripDeleted: String { lookup("trips.tripDeleted") }
        var tripSaveError: String { lookup("trips.tripSaveError") }
        var tripSaved: String { lookup("trips.tripSaved") }
        var tripUpdateError: String { lookup("trips.tripUpdateError") }
        var tripUpdated: String { lookup("trips.tripUpdated") }
        var tripsPlural: String { lookup("trips.tripsPlural") }
        var update: String { lookup("trips.update") }
        var updateTrip: String { lookup("trips.updateTrip") }
        var updating: String { lookup("trips.updating") }
    }
}

/// Tekster som bare finnes i appene. Bruk: `s.settings`.
struct AppStrings {
    let lookup: Lookup
    var account: String { lookup("app.account") }
    var amount: String { lookup("app.amount") }
    var appearance: String { lookup("app.appearance") }
    var autofillKilometers: String { lookup("app.autofillKilometers") }
    var autofillKilometersFooter: String { lookup("app.autofillKilometersFooter") }
    var checkEmail: String { lookup("app.checkEmail") }
    var choose: String { lookup("app.choose") }
    var count: String { lookup("app.count") }
    var deleteAccount: String { lookup("app.deleteAccount") }
    var deleteAccountError: String { lookup("app.deleteAccountError") }
    var deleteAccountFooter: String { lookup("app.deleteAccountFooter") }
    var deleteAccountMessage: String { lookup("app.deleteAccountMessage") }
    var demoMode: String { lookup("app.demoMode") }
    var description: String { lookup("app.description") }
    var duplicate: String { lookup("app.duplicate") }
    var duration: String { lookup("app.duration") }
    var editTripTitle: String { lookup("app.editTripTitle") }
    var emailPlaceholder: String { lookup("app.emailPlaceholder") }
    var errorTitle: String { lookup("app.errorTitle") }
    var hidePassword: String { lookup("app.hidePassword") }
    var hoursShort: String { lookup("app.hoursShort") }
    var hoursWord: String { lookup("app.hoursWord") }
    var kmWithPassengers: String { lookup("app.kmWithPassengers") }
    var minutesShort: String { lookup("app.minutesShort") }
    var newTripTitle: String { lookup("app.newTripTitle") }
    var notSignedIn: String { lookup("app.notSignedIn") }
    var over1000km: String { lookup("app.over1000km") }
    var pdf: String { lookup("app.pdf") }
    var perTrip: String { lookup("app.perTrip") }
    var routeHint: String { lookup("app.routeHint") }
    var routeSuggestions: String { lookup("app.routeSuggestions") }
    var routeSuggestionsFooter: String { lookup("app.routeSuggestionsFooter") }
    var routesWord: String { lookup("app.routesWord") }
    var save: String { lookup("app.save") }
    var sectionComment: String { lookup("app.sectionComment") }
    var sectionExpenses: String { lookup("app.sectionExpenses") }
    var sectionPassengers: String { lookup("app.sectionPassengers") }
    var sectionTime: String { lookup("app.sectionTime") }
    var sectionTrip: String { lookup("app.sectionTrip") }
    var settings: String { lookup("app.settings") }
    var showPassword: String { lookup("app.showPassword") }
    var swipeMonthHint: String { lookup("app.swipeMonthHint") }
    var themeDark: String { lookup("app.themeDark") }
    var themeLight: String { lookup("app.themeLight") }
    var themeSystem: String { lookup("app.themeSystem") }
    var thisYear: String { lookup("app.thisYear") }
    var timeFooter: String { lookup("app.timeFooter") }
    var today: String { lookup("app.today") }
    var tripActions: String { lookup("app.tripActions") }
    var tripEntry: String { lookup("app.tripEntry") }
    var version: String { lookup("app.version") }
}
