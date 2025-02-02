/*
 * AvailRuntimeTestHelper.kt
 * Copyright © 1993-2022, The Avail Foundation, LLC.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of the contributors
 *   may be used to endorse or promote products derived from this software
 *   without specific prior written permission.
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
package avail.test

import avail.AvailRuntime
import avail.builder.AvailBuilder
import avail.builder.ModuleName
import avail.builder.ModuleNameResolver
import avail.builder.ModuleRoot
import avail.builder.ModuleRoots
import avail.builder.RenamesFileParser
import avail.builder.RenamesFileParserException
import avail.builder.UnresolvedDependencyException
import avail.descriptor.phrases.A_Phrase
import avail.files.FileManager
import avail.io.TextInterface
import avail.io.TextOutputChannel
import avail.test.AvailRuntimeTestHelper.Companion.testDirectory
import avail.utility.IO.closeIfNotNull
import avail.utility.cast
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.io.Reader
import java.io.StringReader
import java.nio.CharBuffer
import java.nio.channels.CompletionHandler
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Semaphore

/**
 * Set up the infrastructure for loading Avail modules and running Avail code.
 *
 * @constructor
 * Construct the helper that tracks information about a runtime system suitable
 * for running tests.
 *
 * @param includeTemporaryTestsDirectory
 *   Iff `true`, include an additional module root named "tests" for the
 *   [testDirectory].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class AvailRuntimeTestHelper constructor (
	private val includeTemporaryTestsDirectory: Boolean)
{
	/** The [FileManager] used in this test. */
	private val fileManager: FileManager = FileManager()

	/** The [ModuleRoots] under test. */
	private val moduleRoots: ModuleRoots = createModuleRoots(fileManager)

	/** The [module name resolver][ModuleNameResolver]. */
	private val resolver: ModuleNameResolver by lazy {
		createModuleNameResolver(moduleRoots)
	}

	/** The [Avail runtime][AvailRuntime]. */
	val runtime: AvailRuntime = createAvailRuntime(resolver, fileManager)

	/** The [Avail builder][AvailBuilder]. */
	val builder: AvailBuilder = createAvailBuilder()

	/** The last [System.currentTimeMillis] that an update was shown. */
	@Suppress("MemberVisibilityCanBePrivate")
	var lastUpdateMillis: Long = 0

	/** The maximum notification rate for partially-loaded modules. */
	@Suppress("MemberVisibilityCanBePrivate")
	var updateRateMillis: Long = 500

	/**
	 * A `TestErrorChannel` augments a [TextOutputChannel] with error detection.
	 *
	 * @property errorChannel
	 *   The original [error channel][TextOutputChannel].
	 *
	 * @constructor
	 * Construct a `TestErrorChannel` that decorates the specified
	 * [TextOutputChannel] with error detection.
	 *
	 * @param errorChannel
	 *   The underlying channel.
	 */
	class TestErrorChannel internal constructor(
		private val errorChannel: TextOutputChannel) : TextOutputChannel
	{
		/** Has an error been detected? */
		var errorDetected = false

		override fun <A> write(
			buffer: CharBuffer,
			attachment: A?,
			handler: CompletionHandler<Int, A>)
		{
			errorDetected = true
			errorChannel.write(buffer, attachment, handler)
		}

		override fun <A> write(
			data: String,
			attachment: A?,
			handler: CompletionHandler<Int, A>)
		{
			errorDetected = true
			errorChannel.write(data, attachment, handler)
		}

		override fun isOpen(): Boolean = errorChannel.isOpen

		@Throws(IOException::class)
		override fun close()
		{
			errorChannel.close()
		}
	}

	/**
	 * Create an [AvailBuilder] from the previously created
	 * [Avail runtime][AvailRuntime].
	 *
	 * @return
	 *   An Avail builder.
	 */
	private fun createAvailBuilder(): AvailBuilder
	{
		val b = AvailBuilder(runtime)
		val errorChannel = TestErrorChannel(
			b.textInterface.errorChannel)
		b.textInterface = TextInterface(
			b.textInterface.inputChannel,
			b.textInterface.outputChannel,
			errorChannel)
		return b
	}

	/**
	 * Clear all Avail binary repositories.
	 */
	fun clearAllRepositories()
	{
		resolver.moduleRoots.roots.forEach(ModuleRoot::clearRepository)
	}

	/**
	 * Clear any error detected on the [TestErrorChannel].
	 */
	fun clearError()
	{
		val channel: TestErrorChannel =
			builder.textInterface.errorChannel.cast()
		channel.errorDetected = false
	}

	/**
	 * Shut down the [AvailRuntime] after the tests.
	 */
	fun tearDownRuntime()
	{
		// Avoid racing against fibers that are winding down after a test.
		// Otherwise the tasks they spawn could race against the executor
		// shutdown, run into the [AbortPolicy] that we set for safety on the
		// executor, and produce a RejectedExecutionException.
		runtime.awaitNoFibers()
		runtime.destroy()
	}

	/**
	 * Was an error detected on the [TestErrorChannel]?
	 *
	 * @return
	 *   `true` if an error was detected, `false` otherwise.
	 */
	fun errorDetected(): Boolean
	{
		val channel: TestErrorChannel =
			builder.textInterface.errorChannel.cast()
		return channel.errorDetected
	}

	/** The global status notification text. */
	@Volatile
	private var globalStatus = ""

	/**
	 * Create a global tracker to store information about the progress on all
	 * modules to be compiled.
	 *
	 * @param processedBytes
	 *   The total source size in bytes.
	 * @param totalBytes
	 *   The number of bytes of source that have been parsed and executed so
	 *   far.
	 * @return
	 *   A global tracker.
	 */
	private fun globalTrack(processedBytes: Long, totalBytes: Long)
	{
		val perThousand = (processedBytes * 1000 / totalBytes).toInt()
		val percent = perThousand / 10.0f
		globalStatus = String.format(
			"\u001b[33mGlobal\u001b[0m - %5.1f%%", percent)
	}

	/**
	 * Create a local tracker to share information about the progress of the
	 * compilation of the current module.
	 *
	 * @param moduleName
	 *   The module's name.
	 * @param moduleSize
	 *   The module's source size in bytes.
	 * @param position
	 *   The number of bytes of the module source that have been parsed and
	 *   executed so far.
	 * @param phrase
	 *   The compiled [top-level&#32;statement][A_Phrase], or `null` if no
	 *   phrase is available.
	 * @return
	 *   A local tracker.
	 */
	private fun localTrack(
		moduleName: ModuleName,
		moduleSize: Long,
		position: Long,
		line: Int,
		@Suppress("UNUSED_PARAMETER") phrase: ()->A_Phrase?)
	{
		// Skip non-final per-module updates if they're too frequent.
		if (position < moduleSize
			&& System.currentTimeMillis() - lastUpdateMillis < updateRateMillis)
		{
			return
		}
		val percent = (position * 100 / moduleSize).toInt()
		var modName = moduleName.qualifiedName
		var maxModuleNameLength = 61
		if (line != Int.MAX_VALUE) {
			modName += "\u001b[35m:$line"
			maxModuleNameLength += 5  // Just the escape sequence.
		}
		val len = modName.length
		if (len > maxModuleNameLength)
		{
			modName = "…" + modName.substring(
				len - maxModuleNameLength + 1, len)
		}
		val status = String.format(
			"%s  |  \u001b[34m%-${maxModuleNameLength}s\u001b[0m - %3d%%",
			globalStatus,
			modName,
			percent)
		if (System.console() !== null)
		{
			val statusLength = status.length
			val finalStatus = if (position == moduleSize) "\n"
			else String.format(
				"%s\u001b[%dD\u001b[K",
				status,
				statusLength)
			print(finalStatus)
		}
		else
		{
			println(status)
		}
		lastUpdateMillis = System.currentTimeMillis()
	}

	/**
	 * Load a module with the given full name.  Answer whether the module was
	 * successfully loaded.
	 *
	 * @param moduleName
	 *   The name of the module.
	 * @throws UnresolvedDependencyException
	 *   If the module could not be resolved.
	 * @return
	 *   `true` iff the module was successfully loaded.
	 */
	@Throws(UnresolvedDependencyException::class)
	fun loadModule(moduleName: String): Boolean
	{
		val library = resolver.resolve(
			ModuleName(moduleName), null)
		builder.buildTarget(
			library,
			this::localTrack,
			this::globalTrack,
			builder.buildProblemHandler)
		builder.checkStableInvariants()
		return builder.getLoadedModule(library) !== null
	}

	/**
	 * Create [ModuleRoots] from the information supplied in the
	 * `availRoots` system property.
	 *
	 * @param fileManager
	 *   The [FileManager] used to access files.
	 * @return
	 *   The specified Avail roots.
	 */
	private fun createModuleRoots(fileManager: FileManager): ModuleRoots
	{
		val userDir = System.getProperty("user.dir")
		val path = userDir
			.replace("/avail-server", "")
			.replace("/avail", "")
		val uri = "file://$path"
		val rootsList = mutableListOf(
			"avail" to "$uri/distro/src/avail",
			"examples" to "$uri/distro/src/examples",
			"builder-tests" to "$uri/distro/src/builder-tests")
		if (includeTemporaryTestsDirectory)
		{
			rootsList.add("tests" to "$testDirectory/tests")
		}
		val roots = rootsList.joinToString(";") { (name, path) -> "$name=$path" }
		val semaphore = Semaphore(0)
		val moduleRoots = ModuleRoots(fileManager, roots) {
			if (it.isNotEmpty())
			{
				System.err.println("Failed to initialize module roots fully")
				it.forEach { msg -> System.err.println(msg) }
			}
			semaphore.release()
		}
		semaphore.acquireUninterruptibly()
		return moduleRoots
	}

	/**
	 * Create a [ModuleNameResolver] using the already created
	 * [Avail roots][ModuleRoots] and an option renames file supplied in the
	 * `availRenames` system property.
	 *
	 * @param moduleRoots
	 *   The [ModuleRoots] used to map names to modules and packages on the
	 *   file system.
	 * @return
	 *   The Avail module name resolver.
	 * @throws FileNotFoundException
	 *   If the renames file was specified but not found.
	 * @throws RenamesFileParserException
	 *   If the renames file exists but could not be interpreted correctly
	 *   for any reason.
	 */
	@Throws(FileNotFoundException::class, RenamesFileParserException::class)
	fun createModuleNameResolver(
		moduleRoots: ModuleRoots): ModuleNameResolver
	{
		var reader: Reader? = null
		return try
		{
			val renames = System.getProperty("availRenames", null)
			reader = if (renames == null)
			{
				StringReader("")
			}
			else
			{
				val renamesFile = File(renames)
				BufferedReader(
					InputStreamReader(
						FileInputStream(renamesFile),
						StandardCharsets.UTF_8))
			}
			val renameParser = RenamesFileParser(reader, moduleRoots)
			renameParser.parse()
		}
		finally
		{
			closeIfNotNull(reader)
		}
	}

	/**
	 * Create an [AvailRuntime] from the provided
	 * [Avail module name resolver][ModuleNameResolver].
	 *
	 * @param resolver
	 *   The [ModuleNameResolver] for resolving module names.
	 * @param fileManager
	 *   The system [FileManager].
	 * @return
	 *   An Avail runtime.
	 */
	private fun createAvailRuntime(
		resolver: ModuleNameResolver,
		fileManager: FileManager): AvailRuntime =
		AvailRuntime(resolver, fileManager)

	companion object
	{
		/** Lazily create a test directory in which to perform the tests. */
		val testDirectory: Path by lazy {
			Files.createTempDirectory("for-AvailRuntimeTestHelper-")
		}
	}
}
