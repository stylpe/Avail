/*
 * P_ExecuteAttachedExternalProcess.kt
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

package avail.interpreter.primitive.processes

import avail.descriptor.fiber.A_Fiber
import avail.descriptor.fiber.A_Fiber.Companion.availLoader
import avail.descriptor.fiber.A_Fiber.Companion.heritableFiberGlobals
import avail.descriptor.fiber.A_Fiber.Companion.textInterface
import avail.descriptor.fiber.FiberDescriptor.Companion.newFiber
import avail.descriptor.maps.A_Map.Companion.mapIterable
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.representation.AvailObject
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromArray
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FiberTypeDescriptor.Companion.fiberType
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.bytes
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.wholeNumbers
import avail.descriptor.types.MapTypeDescriptor.Companion.mapTypeForSizesKeyTypeValueType
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.descriptor.types.TupleTypeDescriptor.Companion.oneOrMoreOf
import avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import avail.descriptor.types.TupleTypeDescriptor.Companion.zeroOrOneOf
import avail.exceptions.AvailErrorCode.E_NO_EXTERNAL_PROCESS
import avail.exceptions.AvailErrorCode.E_PERMISSION_DENIED
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.Primitive.Flag.HasSideEffect
import avail.interpreter.execution.Interpreter
import avail.io.ProcessInputChannel
import avail.io.ProcessOutputChannel
import avail.io.TextInterface
import avail.utility.Tuple3
import java.io.File
import java.io.IOException
import java.io.PrintStream
import java.lang.ProcessBuilder.Redirect

/**
 * **Primitive**: Execute an attached external [process][Process]. The forked
 * [fiber][A_Fiber] is wired to the external process's standard input, output,
 * and error mechanisms.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_ExecuteAttachedExternalProcess : Primitive(6, CanInline, HasSideEffect)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(6)
		val processArgsTuple = interpreter.argument(0)
		val optDir = interpreter.argument(1)
		val optEnvironment = interpreter.argument(2)
		val succeed = interpreter.argument(3)
		val fail = interpreter.argument(4)
		val priority = interpreter.argument(5)

		// Transform the process arguments into native strings.
		val processArgs = processArgsTuple.map(AvailObject::asNativeString)
		// Set up the process builder, taking care to explicitly redirect the
		// external process's streams to interface with us.
		val builder = ProcessBuilder(processArgs)
		builder.redirectInput(Redirect.PIPE)
		builder.redirectOutput(Redirect.PIPE)
		builder.redirectError(Redirect.PIPE)
		if (optDir.tupleSize == 1)
		{
			val dir = File(optDir.tupleAt(1).asNativeString())
			builder.directory(dir)
		}
		if (optEnvironment.tupleSize == 1)
		{
			val oldEnvironmentMap = optEnvironment.tupleAt(1)
			val newEnvironmentMap = oldEnvironmentMap.mapIterable.associate {
				(k, v) -> k.asNativeString() to v.asNativeString()
			}
			val environmentMap = builder.environment()
			environmentMap.clear()
			environmentMap.putAll(newEnvironmentMap)
		}
		// Create the new fiber that will be connected to the external process.
		val current = interpreter.fiber()

		// Start the process, running the success function on the new fiber if
		// the process launches successfully.
		val (textInterface, toRun, args) = try
		{
			builder.start().run {
				Tuple3(
					TextInterface(
						ProcessInputChannel(inputStream),
						ProcessOutputChannel(PrintStream(outputStream)),
						ProcessOutputChannel(PrintStream(outputStream))),
					succeed,
					emptyList())
			}
		}
		catch (e: SecurityException)
		{
			Tuple3(
				current.textInterface,
				fail,
				listOf(E_PERMISSION_DENIED.numericCode()))
		}
		catch (e: IOException)
		{
			Tuple3(
				current.textInterface,
				fail,
				listOf(E_NO_EXTERNAL_PROCESS.numericCode()))
		}
		// Run either the success or failure function in a new fiber.
		val runtime = interpreter.runtime
		val newFiber = newFiber(
			TOP.o,
			runtime,
			textInterface,
			priority.extractInt)
		{
			stringFrom("External process execution")
		}
		newFiber.availLoader = current.availLoader
		newFiber.heritableFiberGlobals =
			current.heritableFiberGlobals.makeShared()
		newFiber.makeShared()
		toRun.makeShared()
		runtime.runOutermostFunction(newFiber, toRun, args)
		return interpreter.primitiveSuccess(newFiber)
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tupleFromArray(
				oneOrMoreOf(stringType),
				zeroOrOneOf(stringType),
				zeroOrOneOf(
					mapTypeForSizesKeyTypeValueType(
						wholeNumbers, stringType, stringType)),
				functionType(
					emptyTuple,
					TOP.o),
				functionType(
					tuple(
						enumerationWith(
							set(
								E_PERMISSION_DENIED,
								E_NO_EXTERNAL_PROCESS))),
					TOP.o),
				bytes),
			fiberType(TOP.o))

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(
				E_PERMISSION_DENIED,
				E_NO_EXTERNAL_PROCESS))
}
