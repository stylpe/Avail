/*
 * ReportProblemsAction.kt
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

package org.availlang.ide.anvil.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import org.availlang.ide.anvil.language.psi.AvailFile
import org.availlang.ide.anvil.models.project.availProjectService

/**
 * A `ReportProblemsAction` is TODO: Document this!
 *
 * @author Richard Arriaga &lt;rich@availlang.org&gt;
 */
class BuildAction: AnAction
{
	constructor(): super()
	constructor(name: String, description: String): super(name, description, null)
	override fun actionPerformed(e: AnActionEvent)
	{
		val project = e.project
		if (project != null)
		{
			val service = project.availProjectService

//			val editor: Editor? = e.getData<Editor>(CommonDataKeys.EDITOR)
			val file = e.getData(CommonDataKeys.PSI_FILE)
			if (file is AvailFile)
			{
				ApplicationManager.getApplication().executeOnPooledThread {
					file.build {
						println("I built ${file.node?.reference?.qualifiedName}")
					}
				}
			}
			val i = 5
		}
	}

	override fun update(e: AnActionEvent)
	{
		val project = e.project
		if (project != null)
		{
			e.presentation.isEnabledAndVisible =
				e.getData(CommonDataKeys.PSI_FILE) is AvailFile
		}
		else
		{
			e.presentation.isEnabled = false
		}
		super.update(e)
	}
}
