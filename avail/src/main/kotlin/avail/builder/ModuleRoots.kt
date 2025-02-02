/*
 * ModuleRoots.kt
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

package avail.builder

import avail.annotations.ThreadSafe
import avail.descriptor.module.ModuleDescriptor
import avail.error.ErrorCode
import avail.files.FileManager
import avail.persistence.cache.Repositories
import avail.resolver.ModuleRootResolver
import avail.resolver.ModuleRootResolverRegistry.createResolver
import avail.resolver.ResolverReference
import org.availlang.json.JSONWriter
import java.net.URI
import java.util.Collections.singletonList
import java.util.Collections.synchronizedList
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantLock
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.withLock

/**
 * `ModuleRoots` encapsulates the Avail [module][ModuleDescriptor] path. The
 * Avail module path specifies bindings between *logical root names* and
 * [locations][ModuleRoot] of Avail modules. A logical root name should
 * typically belong to a vendor of Avail modules, ergo a domain name or
 * registered trademark suffices nicely.
 *
 * The format of an Avail module path is described by the following simple
 * grammar:
 *
 * ```
 * modulePath ::= binding ++ ";" ;
 * binding ::= logicalRoot "=" objectRepository ("," sourceDirectory) ;
 * logicalRoot ::= [^=;]+ ;
 * objectRepository ::= [^;]+ ;
 * sourceDirectory ::= [^;]+ ;
 * ```
 *
 * `logicalRoot` represents a logical root name. `objectRepository` represents
 * the absolute path of a binary module repository. `sourceDirectory` represents
 * the absolute path of a package, i.e., a directory containing source modules,
 * and may be sometimes be omitted (e.g., when compilation is not required).
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 *
 * @property fileManager
 *   The associated [FileManager].
 *
 * @constructor
 *
 * Construct a new `ModuleRoots` from the specified Avail roots path.
 *
 * @param fileManager
 *   The associated [FileManager].
 * @param modulePath
 *   An Avail [module][ModuleDescriptor] path.
 * @param withFailures
 *   A lambda that accepts [List] of the string [ModuleRoot] [URI]s that failed
 *   to [resolve][ModuleRootResolver.resolve].
 * @throws IllegalArgumentException
 *   If the Avail [module][ModuleDescriptor] path is malformed.
 */
@ThreadSafe
class ModuleRoots constructor(
	val fileManager: FileManager,
	modulePath: String,
	withFailures: (List<String>) -> Unit) : Iterable<ModuleRoot>
{
	/** A lock for accessing the rootMap. */
	private val lock = ReentrantLock()

	/**
	 * A [map][Map] from logical root names to [module&#32;root][ModuleRoot]s.
	 */
	@GuardedBy("lock")
	private val rootMap = LinkedHashMap<String, ModuleRoot>()

	/**
	 * The Avail [module][ModuleDescriptor] path.
	 */
	val modulePath: String
		get() = lock.withLock {
			rootMap.values.joinToString(";") { root ->
				"${root.name}=${root.resolver.uri}"
			}
		}

	/**
	 * Parse the Avail [module][ModuleDescriptor] path into a [map][Map] of
	 * logical root names to [module&#32;root][ModuleRoot]s.
	 *
	 * @param modulePath
	 *   The module roots path string.
	 * @param withFailures
	 *   A lambda that accepts the [List] of [ModuleRoot] string [URI]s that
	 *   failed to [resolve][ModuleRootResolver.resolve].
	 * @throws IllegalArgumentException
	 *   If any component of the Avail [module][ModuleDescriptor] path is
	 *   invalid.
	 */
	private fun parseAvailModulePathThen(
		modulePath: String,
		withFailures: (List<String>) -> Unit)
	{
		clearRoots()
		// Root definitions are separated by semicolons.
		val components =
			if (modulePath.isEmpty()) listOf()
			else modulePath.split(";")
		val workCount = AtomicInteger(components.size)
		val failures = mutableListOf<String>()
		if (components.isEmpty())
		{
			// We have to deal with the no-roots case specially, since workCount
			// will never decrement to zero – it starts there.
			withFailures(emptyList())
		}
		else
		{
			for (component in components)
			{
				// An equals separates the root name from its paths.
				val binding = component.split("=")
				require(binding.size == 2) {
					"Bad module root location setting: $component"
				}
				val (rootName, location) = binding
				addRoot(rootName, location) { newFailures ->
					if (newFailures.isNotEmpty())
					{
						synchronized(failures) {
							failures.addAll(newFailures)
						}
					}
					if (workCount.decrementAndGet() == 0)
					{
						withFailures(failures)
					}
				}
			}
		}
	}

	/**
	 * Create and add a [root][ModuleRoot] to the [rootMap].
	 *
	 * @param rootName
	 *   The name for the new [ModuleRoot].
	 * @param location
	 *   The [String] representation of the [URI] for the base of the new
	 *   [ModuleRoot]
	 * @param withFailures
	 *   What to invoke, with a [List] of new failure report strings, after the
	 *   root has been added and scanned.
	 */
	fun addRoot(
		rootName: String,
		location: String,
		withFailures: (List<String>) -> Unit)
	{
		if (location.isEmpty())
		{
			withFailures(
				singletonList(
				"Module root \"$rootName\" is missing a source URI"))
			return
		}
		val rootUri = URI(location)
		val resolver = createResolver(rootName, rootUri, fileManager)
		resolver.resolve(
			successHandler = {
				lock.withLock {
					rootMap[rootName] = resolver.moduleRoot
					Repositories.addRepository(resolver.moduleRoot)
				}
				withFailures(emptyList())
			},
			failureHandler = { code, ex ->
				val message =
					"$code: Could not resolve module root $rootName ($rootUri)"
				System.err.println(message)
				ex?.printStackTrace()
				withFailures(singletonList(message))
			})
	}

	/**
	 * Fully remove the provided [ModuleRoot.name].
	 *
	 * @param name
	 *   The name of the root to remove.
	 */
	fun removeRoot (name: String)
	{
		lock.withLock {
			rootMap.remove(name)?.let {
				Repositories.deleteRepository(name)
			}
		}
	}

	/**
	 * Clear the [root&#32;map][rootMap].
	 */
	fun clearRoots() = lock.withLock { rootMap.clear() }

	/**
	 * The [module&#32;roots][ModuleRoot] in the order that they are specified
	 * in the Avail [module][ModuleDescriptor] path.
	 */
	val roots get () = lock.withLock { rootMap.values.toSet() }

	override fun iterator () = roots.iterator()

	/**
	 * Answer the [module&#32;root][ModuleRoot] bound to the specified logical
	 * root name.
	 *
	 * @param rootName
	 *   A logical root name, typically something owned by a vendor of Avail
	 *   [modules][ModuleDescriptor].
	 * @return
	 *   The module root, or `null` if no such binding exists.
	 */
	fun moduleRootFor(rootName: String): ModuleRoot? =
		lock.withLock { rootMap[rootName] }

	/**
	 * Retrieve all of the root [ResolverReference]s for each [ModuleRoot] in
	 * this [ModuleRoots] and pass them to the provided function.
	 *
	 * @param withResults
	 *   A lambda that accepts a [list][List] of successfully resolved
	 *   [ResolverReference]s and a list of [ModuleRoot.name] - [ErrorCode] -
	 *   `nullable` [Throwable] [Triple]s that contains all failed resolutions.
	 */
	fun moduleRootTreesThen (
		withResults: (List<
			ResolverReference>,
			List<Triple<String, ErrorCode, Throwable?>>)->Unit)
	{
		val rootsToAcquire = roots
		val countdown = AtomicInteger(rootsToAcquire.size)
		val references = synchronizedList(mutableListOf<ResolverReference>())
		val failures = synchronizedList(
			mutableListOf<Triple<String, ErrorCode, Throwable?>>())
		rootsToAcquire.forEach { mr ->
			mr.resolver.provideModuleRootTree(
				successHandler = {
					references.add(it)
					if (countdown.decrementAndGet() == 0)
					{
						withResults(references, failures)
					}
				},
				failureHandler = { code, ex ->
					failures.add(Triple(mr.name, code, ex))
					if (countdown.decrementAndGet() == 0)
					{
						withResults(references, failures)
					}
				})
		}
	}

	init
	{
		parseAvailModulePathThen(modulePath, withFailures)
	}

	/**
	 * Write a JSON encoding of the module roots to the specified [JSONWriter].
	 *
	 * @param writer
	 *   A `JSONWriter`.
	 */
	fun writeOn(writer: JSONWriter)
	{
		writer.writeArray {
			roots.forEach { root -> write(root.name) }
		}
	}

	/**
	 * Write a JSON object whose fields are the module roots and whose values
	 * are [JSON&#32;arrays][ModuleRoot.writePathsOn] containing path
	 * information.
	 *
	 * @param writer
	 *   A [JSONWriter].
	 */
	fun writePathsOn(writer: JSONWriter)
	{
		writer.writeArray {
			roots.forEach { root ->
				at(root.name) { root.writePathsOn(writer) }
			}
		}
	}
}
