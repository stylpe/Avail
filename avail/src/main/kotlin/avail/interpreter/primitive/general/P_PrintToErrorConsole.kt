/*
 * P_PrintToErrorConsole.kt
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

package avail.interpreter.primitive.general

import avail.descriptor.fiber.A_Fiber.Companion.textInterface
import avail.descriptor.fiber.FiberDescriptor.ExecutionState
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import avail.exceptions.AvailErrorCode.E_IO_ERROR
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanSuspend
import avail.interpreter.Primitive.Flag.Unknown
import avail.interpreter.execution.Interpreter
import avail.io.SimpleCompletionHandler
import avail.io.TextOutputChannel

/**
 * **Primitive:** Print the specified [string][StringDescriptor] to the
 * [current][Interpreter.fiber]'s
 * [standard&#32;error&#32;channel][TextOutputChannel],
 * [suspended][ExecutionState.SUSPENDED] the current fiber until the string can
 * be queued for writing.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_PrintToErrorConsole : Primitive(1, CanSuspend, Unknown)
{
	@Suppress("RedundantLambdaArrow")
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(1)
		val string = interpreter.argument(0)

		val loader = interpreter.availLoaderOrNull()
		loader?.statementCanBeSummarized(false)

		val textInterface = interpreter.fiber().textInterface
		return interpreter.suspendThen {
			SimpleCompletionHandler<Int>(
				{ succeed(nil) },
				{ fail(E_IO_ERROR) }
			).guardedDo {
				textInterface.errorChannel.write(
					string.asNativeString(), Unit, handler)
			}
		}
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(stringType), TOP.o)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(E_IO_ERROR))
}
