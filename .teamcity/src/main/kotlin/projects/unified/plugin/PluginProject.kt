package projects.unified.plugin

import common.addGradleParam
import common.configuredGradle
import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.script

class PluginProject(private val buildAndTest: BuildType) : Project({
    id("Plugin")
    name = "Plugin"


    buildType {
        id("Deploy")
        name = "Deploy"
        description = "Deploy the Declarative Gradle unified prototype plugin. " +
                "Uses current version minus -SNAPSHOT as the release version. " +
                "The next development version will be the current version with an incremented patch version."
        type = BuildTypeSettings.Type.DEPLOYMENT

        vcs {
            root(DslContext.settingsRootId)
        }

        params {
            password(
                "env.GRADLE_PUBLISH_KEY",
                "%plugin.portal.publish.key%",
                description = "The Gradle publish key to publish this plugin into"
            )
            password(
                "env.GRADLE_PUBLISH_SECRET",
                "%plugin.portal.publish.secret%",
                description = "The Gradle publish secret to publish this plugin into"
            )
        }

        requirements {
            contains("teamcity.agent.jvm.os.name", "Linux")
        }

        dependencies {
            snapshot(buildAndTest) {
                onDependencyFailure = FailureAction.FAIL_TO_START
                onDependencyCancel = FailureAction.FAIL_TO_START
            }
        }

        steps {
            step(configuredGradle {
                name = "Change to Release Version"
                tasks = ":unified-plugin:build-update-utils:changeSnapshotToReleaseVersion"
                workingDir = "unified-prototype"
            })
            step(configuredGradle {
                name = "Publish Plugins"
                tasks = ":unified-plugin:publishAllPlugins"
                addGradleParam("-Dgradle.publish.skip.namespace.check=true")
                workingDir = "unified-prototype"
            })
            step(configuredGradle {
                name = "Change to next Snapshot Version"
                tasks = ":unified-plugin:build-update-utils:changeReleaseToNextSnapshotVersion"
                workingDir = "unified-prototype"
            })
            script {
                name = "Push version commits"
                scriptContent = """
                    set -e
                    git push origin main --tags
                """.trimIndent()
            }
        }
    }
})
