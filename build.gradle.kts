// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    // alias(libs.plugins.kotlin.compose) apply false // این خط ممکن است دیگر لازم نباشد اگر از نسخه‌های جدیدتر AGP استفاده می‌کنید.
                                                    // Gradle خودش پلاگین کامپوز را از طریق application و kotlin.android مدیریت می‌کند.
                                                    // اگر پروژه شما بدون آن کار می‌کند، می‌توانید حذفش کنید.
}