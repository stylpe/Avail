/*
 * P_ServerSocketIPv4Bind.kt
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

package avail.interpreter.primitive.sockets

import avail.descriptor.atoms.A_Atom.Companion.getAtomProperty
import avail.descriptor.atoms.A_Atom.Companion.isAtomSpecial
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom.SERVER_SOCKET_KEY
import avail.descriptor.numbers.A_Number.Companion.extractInt
import avail.descriptor.numbers.A_Number.Companion.extractUnsignedShort
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.A_Tuple.Companion.tupleIntAt
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.bytes
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.singleInt
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.unsignedShorts
import avail.descriptor.types.PojoTypeDescriptor.Companion.intRange
import avail.descriptor.types.TupleTypeDescriptor.Companion.tupleTypeForSizesTypesDefaultType
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.ATOM
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AvailErrorCode.E_INVALID_HANDLE
import avail.exceptions.AvailErrorCode.E_IO_ERROR
import avail.exceptions.AvailErrorCode.E_PERMISSION_DENIED
import avail.exceptions.AvailErrorCode.E_SPECIAL_ATOM
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.Primitive.Flag.HasSideEffect
import avail.interpreter.execution.Interpreter
import avail.utility.cast
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress.getByAddress
import java.net.InetSocketAddress
import java.net.UnknownHostException
import java.nio.channels.AsynchronousServerSocketChannel

/**
 * **Primitive:** Bind the [AsynchronousServerSocketChannel] referenced by the
 * specified [handle][AtomDescriptor] to an [IPv4&#32;address][Inet4Address] and
 * port. The bytes of the address are specified in network byte order, i.e.,
 * big-endian.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_ServerSocketIPv4Bind : Primitive(4, CanInline, HasSideEffect)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(4)
		val handle = interpreter.argument(0)
		val addressTuple = interpreter.argument(1)
		val port = interpreter.argument(2)
		val backlog = interpreter.argument(3)
		val pojo = handle.getAtomProperty(SERVER_SOCKET_KEY.atom)
		if (pojo.isNil)
		{
			return interpreter.primitiveFailure(
				if (handle.isAtomSpecial) E_SPECIAL_ATOM
				else E_INVALID_HANDLE)
		}
		val socket = pojo.javaObjectNotNull<AsynchronousServerSocketChannel>()
		// Build the big-endian address byte array.
		val addressBytes = ByteArray(4) {
			addressTuple.tupleIntAt(it + 1).toByte()
		}
		val backlogInt = backlog.extractInt
		return try
		{
			val inetAddress: Inet4Address = getByAddress(addressBytes).cast()
			val address =
				InetSocketAddress(inetAddress, port.extractUnsignedShort)
			socket.bind(address, backlogInt)
			interpreter.primitiveSuccess(nil)
		}
		catch (e: IllegalStateException)
		{
			interpreter.primitiveFailure(E_INVALID_HANDLE)
		}
		catch (e: UnknownHostException)
		{
			// This shouldn't actually happen, since we carefully enforce the
			// range of addresses.
			assert(false)
			interpreter.primitiveFailure(E_IO_ERROR)
		}
		catch (e: IOException)
		{
			interpreter.primitiveFailure(E_IO_ERROR)
		}
		catch (e: SecurityException)
		{
			interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				ATOM.o,
				tupleTypeForSizesTypesDefaultType(
					singleInt(4),
					emptyTuple,
					bytes),
				unsignedShorts,
				intRange()),
			TOP.o)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(
				E_INVALID_HANDLE,
				E_SPECIAL_ATOM,
				E_IO_ERROR,
				E_PERMISSION_DENIED))
}
