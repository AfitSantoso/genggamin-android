// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.spotless)
}

spotless {
    kotlin {
        target("**/*.kt")
        targetExclude("**/build/**/*.kt")
        ktlint("1.0.1")
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_function-naming" to "disabled",
                    "ktlint_standard_package-name" to "disabled",
                    "ktlint_standard_no-wildcard-imports" to "disabled",
                    "ktlint_standard_discouraged-comment-location" to "disabled",
                    "ktlint_standard_property-naming" to "disabled", // Mengizinkan snake_case atau konstanta dengan huruf besar
                    "ij_kotlin_allow_trailing_comma" to "true",
                    "ij_kotlin_allow_trailing_comma_on_call_site" to "true",
                ),
            )
        trimTrailingWhitespace()
        endWithNewline()
    }
    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
    }
}
