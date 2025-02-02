/*
 * build.gradle.kts
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *  * Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 *  * Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 *  * Neither the name of the copyright holder nor the names of the contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

import avail.build.AvailSetupContext.distroLib
import avail.plugins.gradle.CreateDigestsFileTask
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toUpperCaseAsciiOnly

plugins {
	java
	`maven-publish`
	publishing
	signing
	id("org.jetbrains.dokka")
}

version = "1.6.1"

dependencies {
	// Avail.
	implementation(project(":avail"))
}

///////////////////////////////////////////////////////////////////////////////
//                       Publish Utilities
///////////////////////////////////////////////////////////////////////////////
val ossrhUsername: String get() =
	System.getenv("OSSRH_USER") ?: ""
val ossrhPassword: String get() =
	System.getenv("OSSRH_PASSWORD") ?: ""

private val credentialsWarning =
	"Missing OSSRH credentials.  To publish, you'll need to create an OSSRH " +
		"JIRA account. Then ensure the user name, and password are available " +
		"as the environment variables: 'OSSRH_USER' and 'OSSRH_PASSWORD'"

/**
 * Check that the publish task has access to the necessary credentials.
 */
fun checkCredentials ()
{
	if (ossrhUsername.isEmpty() || ossrhPassword.isEmpty())
	{
		System.err.println(credentialsWarning)
	}
}

///////////////////////////////////////////////////////////////////////////////

tasks {
	val sourcesRoot = "$projectDir/../distro/src/avail"
	// Produce a JAR with the source of every module in the standard Avail library.
	val standardLibraryName = "$buildDir/avail-standard-library.jar"
	val digestsDirectory = "$buildDir/Avail-Digests"
	val digestsLocalFileName = "all_digests.txt"

	val createDigests by creating(CreateDigestsFileTask::class) {
		basePath = sourcesRoot
		inputs.files(fileTree(sourcesRoot))
		outputs.file("$digestsDirectory/$digestsLocalFileName")
	}

	jar {
		description = "The Avail standard library"
		manifest.attributes["Build-Version"] = project.extra.get("buildVersion")
		manifest.attributes["Implementation-Version"] = project.version
		archiveFileName.set(standardLibraryName)
		isZip64 = true
		dependsOn(createDigests)
		from(sourcesRoot) {
			include("**/*.*")
			into("Avail-Sources")
		}
		from(digestsDirectory) {
			include(digestsLocalFileName)
			into("Avail-Digests")
		}
		// Eventually we will add Avail-Compilations or something, to capture
		// serialized compiled modules, serialized phrases, manifest entries,
		// style information, and navigation indices.
		duplicatesStrategy = DuplicatesStrategy.FAIL
		manifest.attributes["Implementation-Title"] = "Avail standard library"
		manifest.attributes["Implementation-Version"] = project.version
		// Even though the jar only includes .avail source files, we need the
		// content to be available at runtime, so we use "" for the archive
		// classifier instead of "sources".
		archiveClassifier.set("")
	}

	// Copy the library into the distribution directory.
	val releaseStandardLibrary by creating(Copy::class) {
		group = "release"
		from(standardLibraryName)
		into("${rootProject.projectDir}/$distroLib")
		duplicatesStrategy = DuplicatesStrategy.INCLUDE

		dependsOn(jar)
	}

	// Update the dependencies of "assemble".
	assemble { dependsOn(releaseStandardLibrary) }

	val sourceJar by creating(Jar::class) {
		description = "Creates sources JAR."
		dependsOn(JavaPlugin.CLASSES_TASK_NAME)
		archiveClassifier.set("sources")
		from(sourceSets["main"].allSource)
	}

	val dokkaHtml by getting(org.jetbrains.dokka.gradle.DokkaTask::class)

	val javadocJar by creating(Jar::class)
	{
		dependsOn(dokkaHtml)
		description = "Creates Javadoc JAR."
		dependsOn(JavaPlugin.CLASSES_TASK_NAME)
		archiveClassifier.set("javadoc")
		from(dokkaHtml.outputDirectory)
	}

	artifacts {
		add("archives", sourceJar)
		add("archives", javadocJar)
	}
	publish {
		checkCredentials()
		dependsOn(build)
	}
}

val isReleaseVersion =
	!version.toString().toUpperCaseAsciiOnly().endsWith("SNAPSHOT")

rootProject.tasks.assemble {
	dependsOn(tasks.getByName("releaseStandardLibrary"))
}

signing {
	useGpgCmd()
	sign(the<PublishingExtension>().publications)
}

publishing {
	repositories {
		maven {
			url = if (isReleaseVersion)
			{
				// Release version
				uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
			}
			else
			{
				// Snapshot
				uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
			}
			println("Publishing snapshot: $isReleaseVersion")
			println("Publishing URL: $url")
			credentials {
				username = System.getenv("OSSRH_USER")
				password = System.getenv("OSSRH_PASSWORD")
			}
		}
	}

	publications {

		create<MavenPublication>("avail-stdlib") {
			pom {
				groupId = project.group.toString()
				name.set("Avail Standard Library")
				packaging = "jar"
				description.set("This module provides the entire Avail standard library.")
				url.set("https://www.availlang.org/")
				licenses {
					license {
						name.set("BSD 3-Clause \"New\" or \"Revised\" License")
						url.set("https://github.com/AvailLang/avail-storage/blob/main/LICENSE")
					}
				}
				scm {
					connection.set("scm:git:git@github.com:AvailLang/Avail.git")
					developerConnection.set("scm:git:git@github.com:AvailLang/Avail.git")
					url.set("https://github.com/AvailLang/Avail")
				}
				developers {
					developer {
						id.set("toddATAvail")
						name.set("Todd Smith")
					}
					developer {
						id.set("markATAvail")
						name.set("Mark van Gulik")
					}
					developer {
						id.set("richATAvail")
						name.set("Richard Arriaga")
					}
					developer {
						id.set("leslieATAvail")
						name.set("Leslie Schultz")
					}
				}
			}
			val sourceJar = tasks.getByName("sourceJar") as Jar
			val javadocJar = tasks.getByName("javadocJar") as Jar
			from(components["java"])
			artifact(sourceJar)
			artifact(javadocJar)
		}
	}
}
