/*
 * SubmitInputAction.kt
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

package avail.environment.actions

import avail.builder.AvailBuilder.CompiledCommand
import avail.environment.AvailWorkbench
import avail.io.ConsoleInputChannel
import avail.io.ConsoleOutputChannel
import avail.io.TextInterface
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.Action
import javax.swing.JOptionPane
import javax.swing.KeyStroke
import javax.swing.SwingUtilities.invokeLater

/**
 * A `SubmitInputAction` sends a line of text from the input field to standard
 * input.
 *
 * @constructor
 * Construct a new `SubmitInputAction`.
 *
 * @param workbench
 *   The owning [AvailWorkbench].
 */
class SubmitInputAction constructor(
	workbench: AvailWorkbench
) : AbstractWorkbenchAction(workbench, "Submit Input")
{
	override fun actionPerformed(event: ActionEvent)
	{
		if (workbench.inputField.isFocusOwner)
		{
			assert(workbench.backgroundTask === null)
			if (workbench.isRunning)
			{
				// Program is running.  Feed this new line of text to the
				// input stream to be consumed by the running command, or
				// possibly discarded when that command completes.
				workbench.inputStream().update()
				return
			}
			// No program is running.  Treat this as a command and try to
			// run it.  Do not feed the string into the input stream.
			val string = workbench.inputField.text
			workbench.commandHistory.add(string)
			workbench.commandHistoryIndex = -1
			workbench.inputStream().feedbackForCommand(string)
			workbench.inputField.text = ""
			workbench.isRunning = true
			workbench.setEnablements()
			workbench.availBuilder.runtime.setTextInterface(TextInterface(
				ConsoleInputChannel(workbench.inputStream()),
				ConsoleOutputChannel(workbench.outputStream()),
				ConsoleOutputChannel(workbench.errorStream())))
			workbench.availBuilder.attemptCommand(
				command = string,
				onAmbiguity = { commands, proceed ->
					val selection = JOptionPane.showInputDialog(
						workbench,
						"Choose the desired entry point:",
						"Disambiguate",
						JOptionPane.QUESTION_MESSAGE,
						null,
						commands.sortedBy { it.toString() }.toTypedArray(),
						null) as CompiledCommand?
					// There may not be a selection, in which case the
					// command will not be run – but any necessary cleanup
					// will be run.
					proceed(selection)
				},
				onSuccess = { result, cleanup ->
					val afterward = {
						workbench.isRunning = false
						invokeLater {
							workbench.inputStream().clear()
							workbench.availBuilder.runtime
								.setTextInterface(TextInterface(
									ConsoleInputChannel(
										workbench.inputStream()),
									ConsoleOutputChannel(
										workbench.outputStream()),
									ConsoleOutputChannel(
										workbench.errorStream())))
							workbench.setEnablements()
						}
					}
					if (result.isNil)
					{
						cleanup(afterward)
						return@attemptCommand
					}
					workbench.availBuilder.runtime.stringifyThen(result)
					{ resultString ->
						workbench
							.outputStream()
							.append(resultString)
							.append("\n")
						cleanup(afterward)
					}
				},
				onFailure = {
					invokeLater {
						workbench.isRunning = false
						workbench.inputStream().clear()
						workbench.availBuilder.runtime.setTextInterface(
							TextInterface(
								ConsoleInputChannel(
									workbench.inputStream()),
								ConsoleOutputChannel(
									workbench.outputStream()),
								ConsoleOutputChannel(
									workbench.errorStream())))
						workbench.setEnablements()
					}
				})
		}
	}

	init
	{
		putValue(
			Action.SHORT_DESCRIPTION,
			"Submit the input field (plus a new line) to standard input.")
		putValue(
			Action.ACCELERATOR_KEY,
			KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0))
	}
}
