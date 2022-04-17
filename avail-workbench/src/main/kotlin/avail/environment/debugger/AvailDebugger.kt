/*
 * AvailDebugger.kt
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

package avail.environment.debugger

import avail.AvailDebuggerModel
import avail.descriptor.atoms.A_Atom.Companion.atomName
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.bundles.A_Bundle.Companion.message
import avail.descriptor.character.A_Character.Companion.isCharacter
import avail.descriptor.fiber.A_Fiber
import avail.descriptor.fiber.A_Fiber.Companion.continuation
import avail.descriptor.fiber.A_Fiber.Companion.executionState
import avail.descriptor.fiber.A_Fiber.Companion.fiberName
import avail.descriptor.fiber.FiberDescriptor.Companion.debuggerPriority
import avail.descriptor.functions.A_Continuation
import avail.descriptor.functions.A_Continuation.Companion.caller
import avail.descriptor.functions.A_Continuation.Companion.currentLineNumber
import avail.descriptor.functions.A_Continuation.Companion.frameAt
import avail.descriptor.functions.A_Continuation.Companion.function
import avail.descriptor.functions.A_Continuation.Companion.numSlots
import avail.descriptor.functions.A_Continuation.Companion.pc
import avail.descriptor.functions.A_Continuation.Companion.stackp
import avail.descriptor.functions.A_RawFunction
import avail.descriptor.functions.A_RawFunction.Companion.declarationNames
import avail.descriptor.functions.A_RawFunction.Companion.methodName
import avail.descriptor.functions.A_RawFunction.Companion.module
import avail.descriptor.functions.A_RawFunction.Companion.numArgs
import avail.descriptor.functions.A_RawFunction.Companion.numConstants
import avail.descriptor.functions.A_RawFunction.Companion.numLocals
import avail.descriptor.functions.A_RawFunction.Companion.numOuters
import avail.descriptor.module.A_Module.Companion.moduleNameNative
import avail.descriptor.numbers.A_Number.Companion.equalsInt
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.types.A_Type.Companion.instance
import avail.descriptor.types.A_Type.Companion.instanceCount
import avail.descriptor.types.PrimitiveTypeDescriptor
import avail.descriptor.types.PrimitiveTypeDescriptor.Types
import avail.descriptor.types.VariableTypeDescriptor.Companion.mostGeneralVariableType
import avail.environment.AvailWorkbench
import avail.environment.actions.AbstractWorkbenchAction
import avail.interpreter.execution.Interpreter
import avail.interpreter.levelOne.L1Disassembler
import avail.utility.safeWrite
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.Collections.synchronizedMap
import java.util.IdentityHashMap
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicLong
import javax.swing.GroupLayout
import javax.swing.GroupLayout.PREFERRED_SIZE
import javax.swing.JButton
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JPopupMenu
import javax.swing.JScrollPane
import javax.swing.JTextArea
import javax.swing.ListCellRenderer
import javax.swing.ListSelectionModel.SINGLE_SELECTION
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter

/**
 * [AvailDebugger] presents a user interface for debugging Avail
 * [fibers][A_Fiber].  It's associated with a [AvailWorkbench]. It can be used
 * to explore and experiment with a running Avail program.
 *
 * The layout is as follows:
 * ```
 * ┌──────────────────────┬─────────────────────────────────┐
 * │                      │                                 │
 * │ Fibers               │ Stack                           │
 * │                      │                                 │
 * ├──────┬──────┬─────┬──┴──────┬────────┬─────────┬───────┤
 * │ Into │ Over │ Out │ To Line │ Resume │ Restart │       │
 * ├──────┴──────┴─────┴───┬─────┴────────┴─────────┴───────┤
 * │                       │                                │
 * │ Code                  │  Source (TODO)                 │
 * │                       │                                │
 * ├────────────────┬──────┴────────────────────────────────┤
 * │                │                                       │
 * │ Variables      │ Variable Value                        │
 * │                │                                       │
 * └────────────────┴───────────────────────────────────────┘
 * ```
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @property workbench
 *   The [AvailWorkbench] associated with this debugger.
 *
 * @constructor
 * Construct a new [AvailDebugger].
 *
 */
class AvailDebugger internal constructor (
	val workbench: AvailWorkbench
) : JFrame("Avail Debugger")
{
	/**
	 * The [AvailDebuggerModel] through which the fibers are controlled.
	 */
	val debuggerModel = AvailDebuggerModel(workbench.runtime)

	val runtime = debuggerModel.runtime

	/** Renders entries in the list of fibers. */
	class FiberRenderer : ListCellRenderer<A_Fiber>
	{
		override fun getListCellRendererComponent(
			list: JList<out A_Fiber>,
			fiber: A_Fiber,
			index: Int,
			isSelected: Boolean,
			cellHasFocus: Boolean): Component
		{
			return fiber.run {
				JLabel("[$executionState] ${fiberName.asNativeString()}")
			}
		}
	}

	/** Renders entries in the list of frames ([A_Continuation]s) of a fiber. */
	class FrameRenderer : ListCellRenderer<A_Continuation>
	{
		override fun getListCellRendererComponent(
			list: JList<out A_Continuation>,
			frame: A_Continuation,
			index: Int,
			isSelected: Boolean,
			cellHasFocus: Boolean): Component
		{
			val code = frame.function().code()
			val module = code.module
			val text = String.format(
				"%s (%s:%d) pc=%d",
				code.methodName.asNativeString(),
				if (module.isNil) "?"
				else module.moduleNameNative,
				frame.currentLineNumber(),
				frame.pc())
			return JLabel(text)
		}
	}

	private val inspectFrame = object :
		AbstractWorkbenchAction(workbench, "Inspect")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			stackListPane.selectedValue?.let {
				inspect(it.function().code().toString(), it as AvailObject)
			}
		}
	}

	/** Helper for capturing values and labels for the variables list view. */
	data class Variable(
		val name: String,
		val value: AvailObject)
	{
		val label = JLabel("$name = ${stringIfSimple(value)}")

		/**
		 * Answer a [String] representation of the given [value] if it's simple,
		 * otherwise null.
		 */
		private fun stringIfSimple(
			value: AvailObject,
			depth: Int = 0
		): String = when {
			depth > 3 -> "***depth***"
			value.isNil -> "nil"
			value.isString -> value.toString()
			value.isInstanceOf(Types.NUMBER.o) -> value.toString()
			value.isInstanceOf(Types.MESSAGE_BUNDLE.o) ->
				value.message.atomName.asNativeString()
			value.isInstanceOf(Types.METHOD.o) -> value.toString()
			value.isAtom -> value.toString()
			value.isCharacter -> value.toString()
			value.isInstanceOf(mostGeneralVariableType) ->
				"var(${stringIfSimple(value.getValueForDebugger(), depth + 1)})"
			!value.isType -> "(${value.typeTag})"
			value.isTop -> value.toString()
			value.isBottom -> value.toString()
			value.traversed().descriptor() is PrimitiveTypeDescriptor ->
				value.toString()
			value.instanceCount.equalsInt(1) ->
				"${stringIfSimple(value.instance, depth + 1)}'s type"
			else -> "(${value.typeTag})"
		}
	}

	/** Renders variables in the variables list view. */
	class VariablesRenderer : ListCellRenderer<Variable>
	{
		override fun getListCellRendererComponent(
			list: JList<out Variable>,
			variable: Variable,
			index: Int,
			isSelected: Boolean,
			cellHasFocus: Boolean
		) = variable.label
	}

	/**
	 * A cache of the disassembly of [A_RawFunction]s, each with a map from
	 * Level One pc to the character range to highlight for that pc.
	 */
	private val disassemblyCache =
		synchronizedMap<A_RawFunction, Pair<String, Map<Int, IntRange>>>(
			mutableMapOf())

	/**
	 * Produce a Pair containing the textual disassembly of the given
	 * [A_RawFunction] and the map from pc to character range.
	 */
	private fun disassembledWithMapThen(
		code: A_RawFunction,
		then: (String, Map<Int, IntRange>)->Unit)
	{
		// Access the cache in a wait-free manner, where multiple simultaneous
		// requesters for the same code will simply compute the (idempotent)
		// value redundantly.
		disassemblyCache[code]?.let {
			then(it.first, it.second)
			return
		}
		// The disassembly isn't cached yet.  Compute it in an AvailThread.
		runtime.whenRunningInterpretersDo(debuggerPriority) {
			val map = mutableMapOf<Int, IntRange>()
			val string = buildString {
				L1Disassembler(code).printInstructions(
					IdentityHashMap<A_BasicObject, Void>(10),
					0
				) { pc, line, string ->
					if (pc != 1) append("\n")
					val before = length
					append("$pc. [:$line] $string")
					val after = length
					map[pc] = before .. after
				}
			}
			// Write to the cache, even if it overwrites.
			disassemblyCache[code] = string to map
			then(string, map)
		}
	}

	/**
	 * The [HighlightPainter] with which to show the current instruction in the
	 * [codePane].  This is updated to something sensible as part of opening the
	 * frame.
	 */
	private var codeHighlightPainter = DefaultHighlightPainter(Color.BLACK)

	/**
	 * The [HighlightPainter] with which to show the current instruction of any
	 * frame that is *not* the top-of-stack (i.e., for any frame that is
	 * currently waiting for a call to complete).  This is updated to something
	 * sensible as part of opening the frame.
	 */
	private var secondaryCodeHighlightPainter =
		DefaultHighlightPainter(Color.BLACK)

	/**
	 * The single-step action.  Allow the selected fiber to execute one L1
	 * nybblecode.
	 */
	private val stepIntoAction =
		object : AbstractWorkbenchAction(workbench, "Into")
		{
			override fun actionPerformed(e: ActionEvent?)
			{
				runtime.whenSafePointDo(debuggerPriority) {
					runtime.runtimeLock.safeWrite {
						fiberListPane.selectedValue?.let {
							debuggerModel.singleStep(it)
						}
					}
				}
			}
		}

	private val stepOverAction = object : AbstractWorkbenchAction(workbench, "Over")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			TODO("Not yet implemented")
		}
	}
	private val stepOutAction = object : AbstractWorkbenchAction(workbench, "Out")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			TODO("Not yet implemented")
		}
	}
	private val stepToLineAction = object : AbstractWorkbenchAction(
		workbench, "To Line"
	)
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			TODO("Not yet implemented")
		}
	}
	private val resumeAction = object : AbstractWorkbenchAction(workbench, "Resume")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			TODO("Not yet implemented")
		}
	}
	private val restartAction = object : AbstractWorkbenchAction(workbench, "Restart")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			TODO("Not yet implemented")
		}
	}

	private val inspectVariable = object :
		AbstractWorkbenchAction(workbench, "Inspect")
	{
		override fun actionPerformed(e: ActionEvent?)
		{
			variablesPane.selectedValue?.run { inspect(name, value) }
		}
	}

	////////////////////////////////////
	///      Visual components       ///
	////////////////////////////////////

	/** The list of fibers captured by the debugger. */
	private val fiberListPane = JList(arrayOf<A_Fiber>()).apply {
		cellRenderer = FiberRenderer()
		selectionMode = SINGLE_SELECTION
		selectionModel.addListSelectionListener { updateStackList() }
	}

	/** The selected fiber's stack frames. */
	private val stackListPane = JList(arrayOf<A_Continuation>()).apply {
		cellRenderer = FrameRenderer()
		selectionMode = SINGLE_SELECTION
		selectionModel.addListSelectionListener {
			if (!it.valueIsAdjusting)
			{
				updateCodePane()
				updateVariablesList()
			}
		}
	}
	/** The button to single-step. */
	private val stepIntoButton = JButton(stepIntoAction)
	private val stepOverButton = JButton(stepOverAction)
	private val stepOutButton = JButton(stepOutAction)
	private val stepToLineButton = JButton(stepToLineAction)
	private val resumeButton = JButton(resumeAction)
	private val restartButton = JButton(restartAction)

	/** A view of the source code or L1 disassembly for the selected frame. */
	private val codePane = JTextArea().apply {
		lineWrap = false
		tabSize = 2
	}

	/** The list of variables in scope in the selected frame. */
	private val variablesPane = JList(arrayOf<Variable>()).apply {
		cellRenderer = VariablesRenderer()
		selectionMode = SINGLE_SELECTION
		selectionModel.addListSelectionListener {
			if (!it.valueIsAdjusting)
			{
				updateVariableValuePane()
			}
		}
	}

	/** The stringification of the selected variable. */
	private val variableValuePane = JTextArea().apply {
		tabSize = 2
	}

	/**
	 * The [A_RawFunction] currently displayed in the [codePane].  This assists
	 * caching to avoid having to disassemble the code repeatedly during
	 * stepping.
	 */
	private var currentCode: A_RawFunction = nil

	/**
	 * Either the current fiber has changed or that fiber has made progress, so
	 * update the presented stack frames.
	 */
	private fun updateStackList()
	{
		when (val fiber = fiberListPane.selectedValue)
		{
			null ->
			{
				stackListPane.setListData(emptyArray())
			}
			else ->
			{
				var frame = fiber.continuation.makeShared()
				val frames = mutableListOf<A_Continuation>()
				while (frame.notNil)
				{
					frames.add(frame)
					frame = frame.caller() as AvailObject
				}
				stackListPane.valueIsAdjusting = true
				try
				{
					stackListPane.setListData(frames.toTypedArray())
					stackListPane.selectedIndices = intArrayOf(0)
				}
				finally
				{
					stackListPane.valueIsAdjusting = false
				}
			}
		}
	}

	/**
	 * The selected stack frame has changed.  Note that stepping causes the top
	 * stack frame to be replaced.
	 */
	private fun updateCodePane()
	{
		val isTopFrame = stackListPane.selectedIndex == 0
		when (val frame = stackListPane.selectedValue)
		{
			null ->
			{
				currentCode = nil
				codePane.highlighter.removeAllHighlights()
				codePane.text = ""
			}
			else ->
			{
				val code = frame.function().code()
				disassembledWithMapThen(code) { text, map ->
					SwingUtilities.invokeLater {
						codePane.highlighter.removeAllHighlights()
						if (!code.equals(currentCode))
						{
							currentCode = code
							codePane.text = text
						}
						val pc = frame.pc()
						val highlightPc = when (isTopFrame)
						{
							true -> frame.pc()
							// Highlight the previous instruction, which is the
							// call that is outstanding.
							else -> map.keys.maxOf {
								if (it < pc) it else Int.MIN_VALUE
							}
						}
						map[highlightPc]?.let { range ->
							codePane.highlighter.addHighlight(
								range.first,
								range.last + 1,
								if (isTopFrame) codeHighlightPainter
								else secondaryCodeHighlightPainter)
							codePane.select(range.first, range.last + 1)
						}
					}
				}
			}
		}
	}

	/**
	 * The current stack frame has changed, so re-present the variables that are
	 * in scope.  Try to preserve selection of a [Variable] with the same
	 * [name][Variable.name] as the previous selection.
	 */
	private fun updateVariablesList()
	{
		val oldName = variablesPane.selectedValue?.name
		val entries = mutableListOf<Variable>()
		stackListPane.selectedValue?.let { frame ->
			val function = frame.function()
			val code = function.code()
			val numArgs = code.numArgs()
			val numLocals = code.numLocals
			val numConstants = code.numConstants
			val numOuters = code.numOuters
			val names = code.declarationNames.map(AvailObject::asNativeString)
				.iterator()
			var frameIndex = 1
			repeat (numArgs)
			{
				entries.add(
					Variable(
						"[arg] " + names.next(),
						frame.frameAt(frameIndex++)))
			}
			repeat (numLocals)
			{
				entries.add(
					Variable(
						"[local] " + names.next(),
						frame.frameAt(frameIndex++)))
			}
			repeat (numConstants)
			{
				entries.add(
					Variable(
						"[const] " + names.next(),
						frame.frameAt(frameIndex++)))
			}
			for (i in 1..numOuters)
			{
				entries.add(
					Variable(
						"[outer] " + names.next(),
						function.outerVarAt(i)))
			}
			for (i in frame.numSlots() downTo frame.stackp())
			{
				// Give the top-of-stack its own name, so that leaving it
				// selected during stepping will continue to re-select it.
				val name =
					if (i == frame.stackp()) "stack top"
					else "stack [$i]"
				entries.add(Variable(name, frame.frameAt(i)))
			}
		}
		variablesPane.valueIsAdjusting = true
		try
		{
			val index = entries.indexOfFirst { it.name == oldName }
			variablesPane.setListData(entries.toTypedArray())
			variablesPane.selectedIndices =
				if (index == -1) intArrayOf() else intArrayOf(index)
		}
		finally
		{
			variablesPane.valueIsAdjusting = false
		}
	}

	/**
	 * A mechanism by which a monotonic [Long] is allocated, to associate with a
	 * variable stringification request; when the stringification completes, the
	 * completion lock is held while the [variableValuePane] is updated, but
	 * only if the associated [Long] is the largest that has been completed.
	 */
	private val paneVersionTracker = object
	{
		val allocator = AtomicLong(0)

		var renderedVersion: Long = -1
	}

	/**
	 * The user has indicated the desire to textually render a variable's value
	 * in the [variableValuePane]. Allow multiple overlapping requests, but only
	 * ever display the most recently allocated request which has completed.  In
	 * theory, we could terminate the stringification fibers corresponding to
	 * values we'll never see (because a newer request has already been
	 * satisfied), but we'll save that for a later optimization.
	 */
	private fun updateVariableValuePane()
	{
		val id = paneVersionTracker.allocator.getAndIncrement()
		val variable = variablesPane.selectedValue
		// Do some trickery to avoid stringifying null or nil.
		val valueToStringify = when
		{
			variable == null -> AtomDescriptor.trueObject //dummy
			variable.value.isNil -> AtomDescriptor.trueObject //dummy
			else -> variable.value
		}
		Interpreter.stringifyThen(
			runtime, runtime.textInterface(), valueToStringify)
		{
			// Undo the above trickery.
			val string = when
			{
				variable == null -> ""
				variable.value.isNil -> "nil\n"
				else -> "${variable.value.typeTag}\n\n$it\n"
			}
			// Delegate to the UI thread, for safety and simplicity.
			SwingUtilities.invokeLater {
				// We're now in the UI thread, so check if we should replace the
				// text.  There's no need for a lock, since the UI thread runs
				// such actions serially.
				if (id > paneVersionTracker.renderedVersion)
				{
					// It's a more recent stringification than the currently
					// displayed string, so replace it.
					variableValuePane.text = string
					paneVersionTracker.renderedVersion = id
				}
			}
		}
	}

	/** Construct the user interface and display it. */
	fun open()
	{
		// Pay attention to any debugged fibers changing state.
		debuggerModel.whenPausedActions.add {
			if (it === fiberListPane.selectedValue)
			{
				// Regenerate the stack.
				updateStackList()
			}
			repaint()
		}
		addWindowListener(object : WindowAdapter()
		{
			override fun windowClosing(e: WindowEvent) {
				releaseAllFibers()
			}
		})
		val panel = JPanel(BorderLayout(20, 20))
		panel.border = EmptyBorder(10, 10, 10, 10)
		background = panel.background
		panel.layout = GroupLayout(panel).apply {
			val pref = PREFERRED_SIZE
			//val def = DEFAULT_SIZE
			val max = Int.MAX_VALUE
			autoCreateGaps = true
			setHorizontalGroup(
				createParallelGroup()
					.addGroup(createSequentialGroup()
						.addComponent(scroll(fiberListPane), 100, 100, max)
						.addComponent(scroll(stackListPane), 200, 200, max))
					.addGroup(createSequentialGroup()
						.addComponent(stepIntoButton)
						.addComponent(stepOverButton)
						.addComponent(stepOutButton)
						.addComponent(stepToLineButton)
						.addComponent(resumeButton)
						.addComponent(restartButton))
					.addComponent(scroll(codePane))
					.addGroup(createSequentialGroup()
						.addComponent(scroll(variablesPane), 60, 60, max)
						.addComponent(scroll(variableValuePane), 100, 100, max)))
			setVerticalGroup(
				createSequentialGroup()
					.addGroup(createParallelGroup()
						.addComponent(scroll(fiberListPane), 60, 60, max)
						.addComponent(scroll(stackListPane), 60, 60, max))
					.addGroup(createParallelGroup()
						.addComponent(stepIntoButton, pref, pref, pref)
						.addComponent(stepOverButton, pref, pref, pref)
						.addComponent(stepOutButton, pref, pref, pref)
						.addComponent(stepToLineButton, pref, pref, pref)
						.addComponent(resumeButton, pref, pref, pref)
						.addComponent(restartButton, pref, pref, pref))
					.addComponent(scroll(codePane), 150, 150, max)
					.addGroup(createParallelGroup()
						.addComponent(scroll(variablesPane), 80, 80, max)
						.addComponent(scroll(variableValuePane), 80, 80, max)))
			linkSize(
				SwingConstants.HORIZONTAL,
				stepIntoButton,
				stepOverButton,
				stepOutButton,
				stepToLineButton,
				resumeButton,
				restartButton)
		}
		minimumSize = Dimension(550, 350)
		preferredSize = Dimension(1000, 1000)
		add(panel)
		pack()
		codeHighlightPainter = run {
			val selectionColor = codePane.selectionColor
			val currentLineColor = AvailWorkbench.AdaptiveColor(
				selectionColor.darker(), selectionColor.brighter())
			DefaultHighlightPainter(currentLineColor.color)
		}
		secondaryCodeHighlightPainter = run {
			val washedOut = AvailWorkbench.AdaptiveColor.blend(
				codeHighlightPainter.color,
				background,
				0.4.toFloat())
			DefaultHighlightPainter(washedOut)
		}

		stackListPane.componentPopupMenu = JPopupMenu("Stack").apply {
			add(inspectFrame)
		}
		variablesPane.componentPopupMenu = JPopupMenu("Variable").apply {
			add(inspectVariable)
		}
		isVisible = true
		fiberListPane.setListData(
			debuggerModel.debuggedFibers.toTypedArray())
	}

	/**
	 * For every existing fiber that isn't already captured by another debugger,
	 * capture that fiber with this debugger.  Those fibers are not permitted to
	 * run unless *this* debugger says they may.  Any fibers launched after this
	 * point (say, to compute a print representation or evaluate an expression)
	 * will *not* be captured by this debugger.
	 *
	 * Note that this operation will block the current thread (which should be
	 * a UI-spawned thread) while holding the runtime at a safe point, to ensure
	 * no other fibers are running, and to ensure other debuggers don't conflict
	 * with this one.
	 *
	 * @param fibersProvider
	 *   A nullary function that will be executed within a safe point to produce
	 *   the collection of fibers to be debugged.  Already-terminated fibers
	 *   will be automatically excluded.
	 */
	fun gatherFibers(fibersProvider: () -> Collection<A_Fiber>)
	{
		val semaphore = Semaphore(0)
		debuggerModel.gatherFibersThen(fibersProvider) {
			semaphore.release()
		}
		semaphore.acquire()
	}

	/** Un-capture all of the debugger's captured fibers, allowing them to
	 * continue running freely.
	 */
	private fun releaseAllFibers()
	{
		val semaphore = Semaphore(0)
		debuggerModel.whenPausedActions.clear()
		debuggerModel.releaseFibersThen { semaphore.release() }
	}

	companion object
	{
		/**
		 * Either places the given component inside a JScrollPane or answers the
		 * JScrollPane that it's already inside.
		 */
		private fun scroll(component: Component) =
			component.parent?.parent ?: JScrollPane(component)
	}
}

/**
 * Helper function to minimize which variables will presented in scope
 * when using the "inspect" action.
 */
@Suppress("UNUSED_PARAMETER")
fun inspect(name: String, value: AvailObject)
{
	// Put a Kotlin debugger breakpoint on the next line.
	@Suppress("UNUSED_EXPRESSION") value
}
