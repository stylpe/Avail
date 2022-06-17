/*
 * P_FileMove.kt
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

import avail.descriptor.atoms.A_Atom.Companion.extractBoolean
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.EnumerationTypeDescriptor
import avail.descriptor.types.EnumerationTypeDescriptor.Companion.booleanType
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import avail.descriptor.types.PrimitiveTypeDescriptor.Types.TOP
import avail.exceptions.AvailErrorCode.E_FILE_EXISTS
import avail.exceptions.AvailErrorCode.E_INVALID_PATH
import avail.exceptions.AvailErrorCode.E_IO_ERROR
import avail.exceptions.AvailErrorCode.E_NO_FILE
import avail.exceptions.AvailErrorCode.E_PERMISSION_DENIED
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.Primitive.Flag.HasSideEffect
import avail.interpreter.execution.Interpreter
import avail.io.IOSystem
import java.io.IOException
import java.nio.file.AccessDeniedException
import java.nio.file.CopyOption
import java.nio.file.FileAlreadyExistsException
import java.nio.file.Files
import java.nio.file.InvalidPathException
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/**
 * **Primitive:** Move the source [path][Path] to the destination path. Use the
 * supplied [boolean][EnumerationTypeDescriptor.booleanType] to decide whether
 * to permit the destination to be overwritten.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_FileMove : Primitive(3, CanInline, HasSideEffect)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(3)
		val source = interpreter.argument(0)
		val destination = interpreter.argument(1)
		val overwrite = interpreter.argument(2)
		val sourcePath: Path
		val destinationPath: Path =
			try
			{
				sourcePath = IOSystem.fileSystem.getPath(
					source.asNativeString())
				IOSystem.fileSystem.getPath(destination.asNativeString())
			}
			catch (e: InvalidPathException)
			{
				return interpreter.primitiveFailure(E_INVALID_PATH)
			}

		val options =
			if (overwrite.extractBoolean)
			{
				arrayOf<CopyOption>(StandardCopyOption.REPLACE_EXISTING)
			}
			else
			{
				arrayOf()
			}
		try
		{
			Files.move(
				sourcePath,
				destinationPath,
				*options)
		}
		catch (e: SecurityException)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}
		catch (e: AccessDeniedException)
		{
			return interpreter.primitiveFailure(E_PERMISSION_DENIED)
		}
		catch (e: NoSuchFileException)
		{
			return interpreter.primitiveFailure(E_NO_FILE)
		}
		catch (e: FileAlreadyExistsException)
		{
			return interpreter.primitiveFailure(E_FILE_EXISTS)
		}
		catch (e: IOException)
		{
			return interpreter.primitiveFailure(E_IO_ERROR)
		}

		return interpreter.primitiveSuccess(nil)
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(tuple(stringType, stringType, booleanType), TOP.o)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(
			set(
				E_INVALID_PATH,
				E_PERMISSION_DENIED,
				E_NO_FILE,
				E_FILE_EXISTS,
				E_IO_ERROR))
}
