/*
 * P_FileTruncate.kt
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

package avail.interpreter.primitive.files

import avail.AvailRuntime.Companion.currentRuntime
import avail.descriptor.atoms.A_Atom.Companion.getAtomProperty
import avail.descriptor.atoms.A_Atom.Companion.isAtomSpecial
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom.FILE_KEY
import avail.descriptor.fiber.A_Fiber.Companion.availLoader
import avail.descriptor.fiber.A_Fiber.Companion.heritableFiberGlobals
import avail.descriptor.fiber.A_Fiber.Companion.textInterface
import avail.descriptor.fiber.FiberDescriptor.Companion.newFiber
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.numbers.A_Number.Companion.extractLong
import avail.descriptor.numbers.A_Number.Companion.isLong
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.returnType
import avail.descriptor.types.A_Type.Companion.typeUnion
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FiberTypeDescriptor.Companion.fiberType
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.InstanceTypeDescriptor.Companion.instanceType
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.bytes
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.wholeNumbers
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ATOM
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AvailErrorCode.E_INVALID_HANDLE
import avail.exceptions.AvailErrorCode.E_IO_ERROR
import avail.exceptions.AvailErrorCode.E_NOT_OPEN_FOR_WRITE
import avail.exceptions.AvailErrorCode.E_SPECIAL_ATOM
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.Primitive.Flag.HasSideEffect
import avail.interpreter.execution.Interpreter
import avail.io.IOSystem.FileHandle
import java.io.IOException
import java.nio.channels.AsynchronousFileChannel

/**
 * **Primitive:** If the specified size is less than the size of the indicated
 * [writable][FileHandle.canWrite] [file&#32;channel][AsynchronousFileChannel]
 * associated with the [handle][AtomDescriptor], then reduce its size as
 * indicated, discarding any data beyond the new file size.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_FileTruncate : Primitive(5, CanInline, HasSideEffect)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(5)
		val atom = interpreter.argument(0)
		val sizeObject = interpreter.argument(1)
		val succeed = interpreter.argument(2)
		val fail = interpreter.argument(3)
		val priority = interpreter.argument(4)

		val pojo = atom.getAtomProperty(FILE_KEY.atom)
		if (pojo.isNil)
		{
			return interpreter.primitiveFailure(
				if (atom.isAtomSpecial) E_SPECIAL_ATOM else E_INVALID_HANDLE)
		}
		val handle = pojo.javaObjectNotNull<FileHandle>()
		if (!handle.canWrite)
		{
			return interpreter.primitiveFailure(E_NOT_OPEN_FOR_WRITE)
		}
		val fileChannel = handle.channel
		// Truncating to something beyond the file size has no effect, so use
		// Long.MAX_VALUE if the newSize is bigger than that.
		val size =
			if (sizeObject.isLong) sizeObject.extractLong
			else java.lang.Long.MAX_VALUE
		val runtime = currentRuntime()
		// Guaranteed non-negative by argument constraint.
		assert(size >= 0L)

		val priorityInt = priority.extractInt
		val current = interpreter.fiber()
		val newFiber = newFiber(
			succeed.kind().returnType.typeUnion(fail.kind().returnType),
			runtime,
			current.textInterface,
			priorityInt)
		{
			StringDescriptor.stringFrom(
				"Asynchronous truncate, ${handle.filename}")
		}
		// If the current fiber is an Avail fiber, then the new one should be
		// also.
		newFiber.availLoader = current.availLoader
		// Share and inherit any heritable variables.
		newFiber.heritableFiberGlobals =
			current.heritableFiberGlobals.makeShared()
		// Share everything that will potentially be visible to the fiber.
		newFiber.makeShared()
		succeed.makeShared()
		fail.makeShared()

		runtime.ioSystem.executeFileTask(
			Runnable {
				try
				{
					fileChannel.truncate(size)
				}
				catch (e: IOException)
				{
					runtime.runOutermostFunction(
						newFiber, fail, listOf(E_IO_ERROR.numericCode()))
					return@Runnable
				}

				runtime.runOutermostFunction(newFiber, succeed, emptyList())
			})

		return interpreter.primitiveSuccess(newFiber)
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				ATOM.o,
				wholeNumbers,
				functionType(
					emptyTuple,
					TOP.o),
				functionType(
					tuple(instanceType(E_IO_ERROR.numericCode())),
					TOP.o),
				bytes),
			fiberType(TOP.o))

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(E_INVALID_HANDLE, E_NOT_OPEN_FOR_WRITE, E_SPECIAL_ATOM))
}
