/*
 * ExecutableChunk.kt
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
 *   may be used to endorse or promote products derived set this software
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
package avail.optimizer

import avail.descriptor.functions.CompiledCodeDescriptor
import avail.interpreter.execution.Interpreter
import avail.interpreter.levelTwo.L2Chunk

/**
 * An [ExecutableChunk] represents an optimized implementation of a
 * [compiled&#32;code&#32;object][CompiledCodeDescriptor].
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
interface ExecutableChunk
{
	/**
	 * Answer a descriptive (non-unique) name for the `ExecutableChunk`.
	 *
	 * @return
	 *   The effective name of the chunk.
	 */
	fun name(): String

	/**
	 * Run the `ExecutableChunk` to completion. Note that a
	 * [reification][StackReifier] request may cut this short. For an initial
	 * invocation, the [Interpreter.argsBuffer] will have been set up for the
	 * call. For a return into this continuation, the offset will refer to code
	 * that will rebuild the register set from the top reified continuation,
	 * using the [Interpreter.getLatestResult]. For resuming the continuation,
	 * the offset will point to code that also rebuilds the register set from
	 * the top reified continuation, but it won't expect a return value. These
	 * re-entry points should perform validity checks on the chunk, allowing an
	 * orderly off-ramp into the [L2Chunk.unoptimizedChunk] (which simply
	 * interprets the L1 nybblecodes).
	 *
	 * @param interpreter
	 *   An interpreter that is appropriately setup to execute the receiver.
	 * @param offset
	 *   The offset at which to begin execution.
	 * @return
	 *   `null` if returning normally, otherwise a [StackReifier] to effect
	 *   reification.
	 */
	fun runChunk(interpreter: Interpreter, offset: Int): StackReifier?
}
