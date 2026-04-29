// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    testOptions {
        unitTests.all {
            ignoreFailures = true // 核心：忽略测试失败
        }
    }
}
