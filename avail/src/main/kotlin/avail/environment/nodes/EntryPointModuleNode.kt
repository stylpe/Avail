/*
 * EntryPointModuleNode.kt
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

package avail.environment.nodes

import avail.builder.AvailBuilder
import avail.builder.ResolvedModuleName

/**
 * This is a tree node representing a module that has one or more entry points,
 * presented via [EntryPointNode]s.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @property resolvedModuleName
 *   The resolved name of the represented module.
 * @constructor
 *   Construct a new [EntryPointNode].
 *
 * @param builder
 *   The builder for which this node is being built.
 * @param resolvedModuleName
 *   The name of the represented module.
 */
class EntryPointModuleNode constructor(
		builder: AvailBuilder, val resolvedModuleName: ResolvedModuleName)
	: AbstractBuilderFrameTreeNode(builder)
{
	override fun modulePathString(): String =
		throw UnsupportedOperationException()

	/**
	 * Is the [module&#32;or&#32;package][ModuleOrPackageNode] loaded?
	 *
	 * @return
	 *   `true` if the module or package is already loaded, `false` otherwise.
	 */
	private val isLoaded: Boolean
		get() = synchronized(builder) {
			return builder.getLoadedModule(resolvedModuleName) !== null
		}

	override fun iconResourceName(): String = "ModuleInTree"

	override fun text(selected: Boolean): String =
		resolvedModuleName.qualifiedName

	override fun htmlStyle(selected: Boolean): String =
		fontStyle(bold = true, italic = !isLoaded) +
			colorStyle(selected, isLoaded, resolvedModuleName.isRename)
}
