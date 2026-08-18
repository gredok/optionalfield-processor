pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    id("com.gradleup.nmcp.settings") version "1.6.1"
}

rootProject.name = "optionalfield-processor"

nmcpSettings {
    centralPortal {
        // Generate a token at https://central.sonatype.com -> your account -> Generate User Token.
        // Set via gradle.properties (local, gitignored) or CENTRAL_PORTAL_USERNAME/PASSWORD env vars (CI).
        username = (providers.gradleProperty("centralPortalUsername").orNull ?: System.getenv("CENTRAL_PORTAL_USERNAME")) ?: ""
        password = (providers.gradleProperty("centralPortalPassword").orNull ?: System.getenv("CENTRAL_PORTAL_PASSWORD")) ?: ""
        // AUTOMATIC releases immediately once validation passes. Switch to USER_MANAGED to review
        // and click "Release" manually in the Central Portal UI before it goes public.
        publishingType = "AUTOMATIC"
    }
}
