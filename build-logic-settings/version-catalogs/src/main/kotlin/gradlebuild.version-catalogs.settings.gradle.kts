import java.util.Properties

/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * The Groovy major version bundled by Gradle, overridable via `-DbundleGroovyMajor` for
 * Gradleception coverage builds.
 *
 * Default should match the Groovy version declared in `gradle/dependency-management/shared-versions.properties`.
 */
val bundleGroovyMajor = providers.systemProperty("bundleGroovyMajor").map(String::toInt).getOrElse(4)

/**
 * Version-catalog aliases that must change together when bundling a non-default Groovy major.
 * CodeNarc is intentionally excluded: it has no Groovy 5 build, and its analysis runtime is
 * independent of the Groovy version bundled into the distribution.
 */
val groovyMajorOverrides: Map<String, String> = when (bundleGroovyMajor) {
    4 -> emptyMap()
    5 -> mapOf(
        "groovy" to "5.0.0!!",
        "spock" to "2.4-groovy-5.0!!",
    )
    else -> error("Unsupported bundled Groovy major version: $bundleGroovyMajor")
}

dependencyResolutionManagement {
    val root = if (rootProject.name.startsWith("build-logic")) {
        layout.rootDirectory.dir("..")
    } else {
        layout.rootDirectory
    }
    val basePath = root.dir("gradle").dir("dependency-management")
    versionCatalogs {
        create("libs") {
            from(files(basePath.file("distribution.versions.toml")))
        }
        create("providedLibs") {
            from(files(basePath.file("provided.versions.toml")))
        }
        create("testLibs") {
            from(files(basePath.file("test.versions.toml")))
        }
        create("buildLibs") {
            from(files(basePath.file("build.versions.toml")))
        }
        val sharedVersions = basePath.file("shared-versions.properties").asFile.inputStream().use {
            Properties().apply { load(it) }
        }
        all {
            // version() is first-wins, so by applying the Groovy major overrides first, we ensure that they take precedence over the shared versions.
            groovyMajorOverrides.forEach { (alias, version) ->
                version(alias, version)
            }
            sharedVersions.forEach { key, value ->
                version(key.toString(), value.toString())
            }
        }
    }
}
