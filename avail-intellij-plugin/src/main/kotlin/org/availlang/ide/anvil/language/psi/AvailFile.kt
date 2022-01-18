/*
 * AvailFile.kt
 * Copyright © 1993-2021, The Avail Foundation, LLC.
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

package org.availlang.ide.anvil.language.psi

import avail.builder.ModuleRoot
import avail.compiler.ModuleManifestEntry
import avail.persistence.cache.RepositoryDescriber
import com.intellij.extapi.psi.PsiFileBase
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.tree.TreeElement
import org.availlang.ide.anvil.language.AvailFileElement
import org.availlang.ide.anvil.language.AvailIcons
import org.availlang.ide.anvil.language.AvailLanguage
import org.availlang.ide.anvil.language.file.AvailFileType
import org.availlang.ide.anvil.models.AvailNode
import org.availlang.ide.anvil.models.project.AvailProject
import org.availlang.ide.anvil.models.project.AvailProjectService
import org.availlang.ide.anvil.models.ModuleNode
import org.availlang.ide.anvil.models.RootNode
import org.availlang.ide.anvil.models.project.availProjectService
import javax.swing.Icon

/**
 * `AvailFile` is the [PsiFileBase] for an Avail file.
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class AvailFile constructor(
	viewProvider: FileViewProvider
): PsiFileBase(viewProvider, AvailLanguage)
{
	override fun getFileType(): FileType = AvailFileType

	override fun getBaseIcon(): Icon =
		if (isModified) { AvailIcons.availFileDirty }
		else { super.getBaseIcon() }


	/**
	 * The running [AvailProjectService].
	 */
	val projectService: AvailProjectService
		get() =
		project.availProjectService

	/**
	 * Provide the file contents.
	 */
	val text: CharSequence get() = viewProvider.contents

	/**
	 * The active [AvailProject].
	 */
	val availProject: AvailProject get() = projectService.availProject

	fun build (then: () -> Unit): Boolean =
		node?.let {
			availProject.build(it.reference.qualifiedName)
			{
				refreshAndGetManifest()
				then()
			}
		} ?: false

	/**
	 * Has this file been modified since it was loaded?
	 */
	val isModified get() =
		node?.let {
			this.modificationStamp > it.reference.lastModified
		} ?: false

	/**
	 * The [RootNode] of the [ModuleRoot] this [AvailFile] belongs to or `null`
	 * if not an Avail module in any of the [AvailProject]'s [ModuleRoot]s.
	 */
	val rootNode: RootNode? get() =
		availProject.rootForModuleUri(viewProvider.virtualFile.path)

	/**
	 * `true` indicates that this [AvailFile] represents an Avail module that is
	 * a module in a [ModuleRoot] that is included the active [AvailProject];
	 * `false` otherwise.
	 */
	val isIncludedProject: Boolean get() = rootNode != null

	/**
	 * The associated [AvailNode] in the active [AvailProject]; or `null` if
	 * not found in the project.
	 */
	val node: ModuleNode? get()
	{
		val service = project.availProjectService
		val path = viewProvider.virtualFile.path
		return service.availProject.nodesURI[path] as? ModuleNode
	}

	/**
	 * Answer the [List] of [ModuleManifestEntry]s for this [AvailFile]. An
	 * empty list indicates that either
	 *  * The module has not been built
	 *  * The module is [not included][isIncludedProject] in the active
	 *  [AvailProject].
	 */
	val manifest: MutableList<ModuleManifestEntry> by lazy {
		calculateManifest()
	}

	/**
	 * Refresh the [manifest].
	 *
	 * @return
	 *   Answer a [MutableList] of the [ModuleManifestEntry]'s that have been
	 *   produced by a previous compilation of this module.
	 */
	fun refreshAndGetManifest (): MutableList<ModuleManifestEntry>
	{
		manifest.clear()
		manifest.addAll(calculateManifest())
		return manifest
	}

	/**
	 * @return
	 *   Answer a [MutableList] of the [ModuleManifestEntry]'s that have been
	 *   produced by a previous compilation of this module.
	 */
	private fun calculateManifest (): MutableList<ModuleManifestEntry>
	{
		val moduleName = node?.resolved
		val tempList = mutableListOf<ModuleManifestEntry>()
		moduleName?.repository?.use { repository ->
			repository.reopenIfNecessary()
			val archive =
				repository.getArchive(moduleName.rootRelativeName)
			val compilations = archive.allKnownVersions.flatMap {
				it.value.allCompilations
			}
			val compilationsArray = compilations.toTypedArray()
			val selectedCompilation =
				if (compilationsArray.isNotEmpty())
				{
					compilationsArray[0]
				}
				else
				{
					return mutableListOf()
				}
			val describer = RepositoryDescriber(repository)
			tempList.addAll(describer.manifestEntries(
				selectedCompilation.recordNumberOfManifestEntries))
		}
		return tempList
	}

	override fun getFirstChild(): PsiElement?
	{
		if (manifest.isEmpty())
		{
			return null
		}
		val manifestEntry = manifest[0]
		return AvailPsiElement(this, manifestEntry, 0, manager)
	}

	override fun getLastChild(): PsiElement?
	{
		if (manifest.isEmpty())
		{
			return null
		}
		val manifestEntry = manifest.last()
		return AvailPsiElement(
			this, manifestEntry, manifest.size - 1, manager)
	}

	val availChildPsiElements: Array<PsiElement> by lazy {
		manifest.mapIndexed { i, it ->
			AvailPsiElement(this, it, i, manager)
		}.toTypedArray()
	}

	override fun getChildren(): Array<PsiElement> = availChildPsiElements

	override fun createContentLeafElement(leafText: CharSequence?): TreeElement
	{
		return AvailFileElement(leafText!!, this)
	}

	override fun toString(): String =
		this.node?.resolved?.qualifiedName ?: viewProvider.virtualFile.path
}
