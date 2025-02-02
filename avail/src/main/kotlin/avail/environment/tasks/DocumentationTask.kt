/*
 * DocumentationTask.kt
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

package avail.environment.tasks

import avail.builder.ResolvedModuleName
import avail.descriptor.module.ModuleDescriptor
import avail.environment.AvailWorkbench
import avail.environment.AvailWorkbench.AbstractWorkbenchTask
import java.awt.Cursor


/**
 * A `DocumentationTask` initiates and manages documentation
 * generation for the target [module][ModuleDescriptor].
 *
 * @constructor
 * Construct a new `DocumentationTask`.
 *
 * @param workbench
 *  The owning [AvailWorkbench].
 * @param targetModuleName
 * The resolved name of the target [module][ModuleDescriptor] to unload.
 */
class DocumentationTask (
		workbench: AvailWorkbench, targetModuleName: ResolvedModuleName?)
	: AbstractWorkbenchTask(workbench, targetModuleName)
{
	override fun executeTaskThen(afterExecute: ()->Unit)
	{
		try
		{
			workbench.resolver.moduleRoots.roots.forEach { root ->
				root.repository.reopenIfNecessary()
			}
			workbench.availBuilder.generateDocumentation(
				targetModuleName(),
				workbench.documentationPath,
				workbench.availBuilder.buildProblemHandler)
		}
		catch (e: Exception)
		{
			// Put a breakpoint here to debug documentation generation
			// exceptions.
			throw e
		}
		finally
		{
			afterExecute()
		}
	}

	override fun done()
	{
		workbench.backgroundTask = null
		reportDone()
		workbench.availBuilder.checkStableInvariants()
		workbench.setEnablements()
		workbench.cursor = Cursor.getDefaultCursor()
	}
}
