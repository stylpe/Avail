/*
 * ModuleRootResolver.kt
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

package avail.resolver

import avail.AvailThread
import avail.builder.ModuleName
import avail.builder.ModuleNameResolver
import avail.builder.ModuleNameResolver.Companion.availExtension
import avail.builder.ModuleNameResolver.ModuleNameResolutionResult
import avail.builder.ModuleRoot
import avail.builder.ResolvedModuleName
import avail.builder.UnresolvedModuleException
import avail.error.ErrorCode
import avail.files.AbstractFileWrapper
import avail.files.FileErrorCode
import avail.files.FileManager
import avail.files.ManagedFileWrapper
import avail.persistence.cache.Repository
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Collections
import java.util.UUID

/**
 * `ModuleRootResolver` declares an interface for accessing Avail [ModuleRoot]s
 * given a [URI]. It is responsible for asynchronously retrieving, creating,
 * deleting, and saving files and packages where the `ModuleRoot` is stored.
 *
 * It is responsible for producing [ResolverReference]s, the object linking a
 * resource in a module root to its actual file at the [URI] location of the
 * module root.
 *
 * All file actions are conducted via the [ModuleRootResolver.fileManager].
 * Given the files may originate anywhere from the local file system to a remote
 * database accessed via network API, a REST-ful webserver, etc., all file
 * requests must be handled asynchronously.
 *
 * A `ModuleRootResolver` establishes access to a `ModuleRoot` based upon the
 * [URI.scheme] given the appropriate `ModuleRootResolver` that supports the
 * scheme. A `ModuleRootResolver` is created by a corresponding
 * [ModuleRootResolverFactory]. For each `ModuleRootResolver` type implemented a
 * `ModuleRootResolverFactory` must be implemented to enable resolver creation.
 * The `ModuleRootResolverFactory` must be
 * [registered][ModuleRootResolverRegistry.register] in the
 * [ModuleRootResolverRegistry]. A `ModuleRootResolver` cannot be created if
 * the factory has not been registered. Only one registered
 * `ModuleRootResolverFactory` is allowed per `URI` scheme.
 *
 * @constructor
 * Construct a new ModuleRootResolver for some root.
 *
 * @property name
 *   The name of this root.
 * @property uri
 *   The [URI] that identifies the location of the [ModuleRoot].
 * @property fileManager
 *   The [FileManager] used for pooling I/O requests for this root.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
abstract class ModuleRootResolver
constructor(
	val name: String,
	val uri: URI,
	val fileManager: FileManager)
{
	/** Answer whether data can be written to modules under this resolver. */
	abstract val canSave: Boolean

	/**
	 * The [ModuleRoot] this [ModuleRootResolver] resolves to.
	 */
	@Suppress("LeakingThis")
	val moduleRoot: ModuleRoot = ModuleRoot(name, this)
	/**
	 * The map from the [ModuleName.qualifiedName] string to the respective
	 * [ResolverReference].
	 */
	protected val referenceMap = mutableMapOf<String, ResolverReference>()

	/**
	 * The [exception][Throwable] that prevented most recent attempt at
	 * accessing the source location of this [ModuleRootResolver].
	 */
	var accessException: Throwable? = null

	/**
	 * The [Map] from a UUID that represents an interested party to a lambda
	 * that accepts a [WatchEventType] that describes the event that occurred
	 * at the source location and a [ResolverReference] that identifies to what
	 * the event occurred to.
	 */
	val watchEventSubscriptions:
			MutableMap<UUID, (WatchEventType, ResolverReference)->Unit> =
		Collections.synchronizedMap(mutableMapOf())

	/**
	 * The full [ModuleRoot] tree if available; or `null` if not yet set.
	 */
	protected var moduleRootTree: ResolverReference? = null

	/**
	 * Provide the non-`null` [ResolverReference] that represents the
	 * [moduleRoot]. There is no guarantee made by this interface as to how
	 * this should be run. It can be all executed on the calling thread or it
	 * can be executed on a separate thread.
	 *
	 * @param successHandler
	 *   Accepts the [resolved][resolve] `ResolverReference`.
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] and a `nullable` [Throwable]
	 *   to be called in the event of failure.
	 */
	open fun provideModuleRootTree(
		successHandler: (ResolverReference)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)
	{
		executeTask {
			moduleRootTree?.let(successHandler) ?:
			resolve(successHandler, failureHandler)
		}
	}

	/**
	 * Connect to the source of the [moduleRoot] and populate this resolver with
	 * all the [ResolverReference]s from the module root. This is not required
	 * nor expected to be executed in the calling thread.
	 *
	 * @param successHandler
	 *   Accepts the [resolved][resolve] [ResolverReference].
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] and a `nullable` [Throwable]
	 *   to be called in the event of failure.
	 */
	abstract fun resolve(
		successHandler: (ResolverReference)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Asynchronously execute the provided task.
	 *
	 * @param task
	 *   The lambda to run outside the calling thread of execution.
	 */
	open fun executeTask(task: ()->Unit) = fileManager.executeFileTask(task)

	/**
	 * Close this [ModuleRootResolver]. Should be called at shutdown to ensure
	 * proper clean up of any open resources.
	 */
	open fun close () = Unit

	/**
	 * Subscribe to receive notifications of [WatchEventType]s occurring to this
	 * [ModuleRoot].
	 *
	 * @param
	 *   The lambda that accepts a [WatchEventType] that describes the event
	 *   that occurred at the source location and a [ResolverReference] that
	 *   identifies to what the event occurred to.
	 * @return
	 *   A [UUID] that uniquely identifies the subscription.
	 */
	fun subscribeRootWatcher(
		watchAction: (WatchEventType, ResolverReference)->Unit): UUID
	{
		val id = UUID.randomUUID()
		watchEventSubscriptions[id] = watchAction
		return id
	}

	/**
	 * Remove a watch subscription.
	 *
	 * @param id
	 *   The watch subscription id of the subscription to remove.
	 */
	fun unsubscribeRootWatcher(id: UUID)
	{
		watchEventSubscriptions.remove(id)
	}

	/**
	 * @return
	 *   `true` indicates the [uri] resolves to a valid Avail [ModuleRoot];
	 *   `false` otherwise.
	 */
	abstract fun resolvesToValidModuleRoot (): Boolean

	/**
	 * Answer a [ModuleNameResolver.ModuleNameResolutionResult] when attempting
	 * to locate a module in this [ModuleRootResolver].
	 *
	 * @param qualifiedName
	 *   The [ModuleName] being searched for.
	 * @param initialCanonicalName
	 *   The canonical name used in case a rename is registered w/ the
	 *   [ModuleRootResolver].
	 * @param moduleNameResolver
	 *   The [ModuleRootResolver].
	 * @return
	 *   A `ModuleNameResolutionResult` or `null` if could not be located in
	 *   this root.
	 */
	open fun find(
		qualifiedName: ModuleName,
		initialCanonicalName: ModuleName,
		moduleNameResolver: ModuleNameResolver
	): ModuleNameResolutionResult?
	{
		val components =
			initialCanonicalName.qualifiedName.split("/").toMutableList()
		assert(components.size > 1)
		assert(components[0].isEmpty())

		while (components.size >= 2)
		{
			components.removeAt(components.size - 2)
			val name = components.joinToString("/")
			referenceMap[name]?.let { resolved ->
				when (resolved.type)
				{
					ResourceType.PACKAGE ->
					{
						// Look for the package representative.
						val representativeComponents =
							components.toMutableList()
						representativeComponents.add(
							representativeComponents.last())
						val representativeName =
							representativeComponents.joinToString("/")
						return when (val rep = referenceMap[representativeName])
						{
							null -> ModuleNameResolutionResult(
								UnresolvedModuleException(
									null, representativeName, this))
							else -> ModuleNameResolutionResult(
								ResolvedModuleName(
									rep.moduleName,
									moduleNameResolver.moduleRoots,
									rep,
									initialCanonicalName.isRename))
						}
					}
					ResourceType.MODULE ->
					{
						return ModuleNameResolutionResult(
							ResolvedModuleName(
								resolved.moduleName,
								moduleNameResolver.moduleRoots,
								resolved,
								initialCanonicalName.isRename))
					}
					else -> Unit
				}
			}
		}
		// Resolution failed within this root.  The caller will try other roots.
		return null
	}

	/**
	 * Provide the [ResolverReference] for the given qualified file name for a
	 * file in [moduleRoot].
	 *
	 * @param qualifiedName
	 *   The qualified name of the target file.
	 * @param withReference
	 *   The lambda that accepts the retrieved reference.
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] and a `nullable` [Throwable]
	 *   to be called in the event of failure.
	 */
	open fun provideResolverReference(
		qualifiedName: String,
		withReference: (ResolverReference)->Unit,
		failureHandler: (ErrorCode, Throwable?) -> Unit)
	{
		val qname = qualifiedName.replace(availExtension, "")
		referenceMap[qname]?.let { reference ->
			return withReference(reference)
		}
		failureHandler(FileErrorCode.FILE_NOT_FOUND, null)
	}

	/**
	 * Answer the [ResolverReference] for the given qualified file name for a
	 * a file in [moduleRoot].
	 *
	 * This should not make async calls. Either the [ModuleRootResolver] has
	 * it readily available or it does not.
	 *
	 * @param qualifiedName
	 *   The qualified name of the target file.
	 * @return
	 *   The [ResolverReference] or `null` if not presently available.
	 */
	open fun getResolverReference(
		qualifiedName: String
	): ResolverReference? = referenceMap[qualifiedName]

	/**
	 * Specifically refresh the [ResolverReference.digest] in the
	 * [Repository.ModuleArchive] for the most recent
	 * [ResolverReference.lastModified] timestamp. This also refreshes the
	 * metrics (mutable state) of the provided [ResolverReference].
	 * It should refresh:
	 *
	 *  * [ResolverReference.lastModified]
	 *  * [ResolverReference.size]
	 *
	 * **NOTE:** This might be run on a separate standard thread. If it is
	 * required that the callback(s) be run in an [AvailThread], that should be
	 * handled *inside* the callback.
	 *
	 * @param reference
	 *   The [ResolverReference] to refresh.
	 * @param successHandler
	 *   A function that accepts the new digest and last modified time to call
	 *   after the new digest has been created.
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] and a `nullable` [Throwable]
	 *   to be called in the event of failure.
	 */
	abstract fun refreshResolverReferenceDigest(
		reference: ResolverReference,
		successHandler: (ByteArray, Long)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Specifically refresh the [ResolverReference] mutable state:
	 *
	 *  * [ResolverReference.lastModified]
	 *  * [ResolverReference.size]
	 *
	 * @param reference
	 *   The [ResolverReference] to refresh.
	 * @param successHandler
	 *   A function that accepts the last modified time to call after the refresh
	 *   is complete.
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] and a `nullable` [Throwable]
	 *   to be called in the event of failure.
	 */
	abstract fun refreshResolverMetaData(
		reference: ResolverReference,
		successHandler: (Long)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Provide the full list of all [ResolverReference]s in this
	 * [ModuleRootResolver].
	 *
	 * @param forceRefresh
	 *   `true` indicates a requirement that the resolver refresh any locally
	 *   cached list it may have directly from the [ModuleRoot] source location;
	 *   `false` indicates, if there is a cached list, providing the cached list
	 *   is acceptable.
	 * @param withList
	 *   The lambda that accepts the [List] of [ResolverReference]s.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the nature
	 *   of the failure and a `nullable` [Throwable].
	 */
	abstract fun rootManifest(
		forceRefresh: Boolean,
		withList: (List<ResolverReference>)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Retrieve the resource and provide it with a request to obtain
	 * the raw file bytes.
	 *
	 * @param bypassFileManager
	 *   `true` indicates the file should be read directly from the source
	 *   location; `false` indicates an attempt to read from the [FileManager]
	 *   should be made.
	 * @param reference
	 *   The [ResolverReference] that identifies the target file to read.
	 * @param withContents
	 *   A function that accepts the raw bytes of the file in the [moduleRoot]
	 *   and optionally a [FileManager] file [UUID] or `null` if
	 *   [bypassFileManager] is `true`.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the nature
	 *   of the failure and a `nullable` [Throwable].
	 */
	abstract fun readFile(
		bypassFileManager: Boolean,
		reference: ResolverReference,
		withContents: (ByteArray, UUID?)->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Create a file.
	 *
	 * @param qualifiedName
	 *   The [fully-qualified name][ModuleName] of the module or resource.
	 * @param mimeType
	 *   The MIME type of the file being created.
	 * @param completion
	 *   A function to be run upon the successful creation of the file.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the failure
	 *   and a `nullable` [Throwable].
	 */
	abstract fun createFile(
		qualifiedName: String,
		mimeType: String,
		completion: ()->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Create a package and its representative Avail module.
	 *
	 * @param qualifiedName
	 *   The [fully-qualified name][ModuleName] of the package.
	 * @param completion
	 *   A function to be run upon the successful creation of the package.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the failure
	 *   and a `nullable` [Throwable].
	 */
	abstract fun createPackage(
		qualifiedName: String,
		completion: ()->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Create a directory.
	 *
	 * @param qualifiedName
	 *   The fully-qualified name of the directory.
	 * @param completion
	 *   A function to be run upon the successful creation of the directory.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the failure
	 *   and a `nullable` [Throwable].
	 */
	abstract fun createDirectory(
		qualifiedName: String,
		completion: ()->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Delete the [ResourceType] linked to the qualified name. If the
	 * [ResourceType] is a [ResourceType.PACKAGE] or a [ResourceType.DIRECTORY],
	 * all of its children should be deleted as well. All deleted references
	 * should be removed from the [reference tree][provideResolverReference].
	 *
	 * If it is a [ResourceType.REPRESENTATIVE], the package it represents
	 * should be deleted and handled as if the package were the target of
	 * deletion.
	 *
	 * @param qualifiedName
	 *   The fully-qualified name of the directory.
	 * @param completion
	 *   A function to be run upon the successful creation of the directory.
	 * @param failureHandler
	 *   A function that accepts a [ErrorCode] that describes the failure
	 *   and a `nullable` [Throwable].
	 */
	abstract fun deleteResource(
		qualifiedName: String,
		completion: ()->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Save the file to where it is stored.
	 *
	 * @param reference
	 *   The [ResolverReference] that identifies the target file to save.
	 * @param fileContents
	 *   The contents of the file to save.
	 * @param failureHandler
	 *   A function that accepts an [ErrorCode] that describes the nature
	 *   of the failure and a `nullable` [Throwable].
	 */
	abstract fun saveFile(
		reference: ResolverReference,
		fileContents: ByteArray,
		successHandler: ()->Unit,
		failureHandler: (ErrorCode, Throwable?)->Unit)

	/**
	 * Answer a [AbstractFileWrapper] for the targeted file.
	 *
	 * @param id
	 *   The [AbstractFileWrapper.id].
	 * @param reference
	 *   The [AbstractFileWrapper] of the file.
	 * @return
	 *   A `AbstractFileWrapper`.
	 */
	fun fileWrapper(
		id: UUID, reference: ResolverReference): AbstractFileWrapper =
			ManagedFileWrapper(id, reference, fileManager)

	/**
	 * Answer the [URI] for a resource in the source [moduleRoot] given a
	 * qualified name. There is no guarantee the target exists.
	 *
	 * @param qualifiedName
	 *   The [fully-qualified name][ModuleName] of the module or resource.
	 * @return
	 *   A `URI` for the resource.
	 *
	 *   URLEncoder.encode(value, StandardCharsets.UTF_8.toString())
	 */
	fun fullResourceURI(qualifiedName: String): URI =
		URI(URLEncoder.encode(
			"$uri/$qualifiedName",
			StandardCharsets.UTF_8.toString()))

	/**
	 * Answer a qualified name for the given [URI] that points to a file in the
	 * [moduleRoot].
	 *
	 * @param targetURI
	 *   The [URI] to transform into qualified name.
	 * @return The qualified name.
	 */
	fun getQualifiedName(targetURI: String): String
	{
		assert(targetURI.startsWith(uri.path)) {
			"$targetURI is not in ModuleRoot, $moduleRoot"
		}
		val relative = targetURI.split(uri.path)[1]
		val cleansedRelative = relative.replace(availExtension, "")
		return "/${moduleRoot.name}" +
			(if (relative.startsWith("/")) "" else "/") +
			cleansedRelative
	}

	/**
	 * A `WatchEventType` indicates the types of events that can occur to
	 * resources at the represented [ModuleRoot] source location.
	 */
	enum class WatchEventType
	{
		/**
		 * A new [ResourceType] is created.
		 */
		CREATE,

		/**
		 * A [ResourceType] is changed. This should only happen to actual files.
		 */
		MODIFY,

		/**
		 * A [ResourceType] has been deleted.
		 */
		DELETE
	}
}
