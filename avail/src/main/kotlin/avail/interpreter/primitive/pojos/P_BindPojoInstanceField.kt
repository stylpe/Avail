/*
 * P_BindPojoInstanceField.kt
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
package avail.interpreter.primitive.pojos

import avail.descriptor.pojos.PojoFieldDescriptor
import avail.descriptor.pojos.PojoFieldDescriptor.Companion.pojoFieldVariableForInnerType
import avail.descriptor.pojos.RawPojoDescriptor.Companion.equalityPojo
import avail.descriptor.sets.SetDescriptor.Companion.set
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.typeVariables
import avail.descriptor.types.AbstractEnumerationTypeDescriptor.Companion.enumerationWith
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.PojoTypeDescriptor.Companion.mostGeneralPojoType
import avail.descriptor.types.PojoTypeDescriptor.Companion.resolvePojoType
import avail.descriptor.types.TupleTypeDescriptor.Companion.stringType
import avail.descriptor.types.VariableTypeDescriptor.Companion.mostGeneralVariableType
import avail.exceptions.AvailErrorCode.E_JAVA_FIELD_NOT_AVAILABLE
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag.CanFold
import avail.interpreter.Primitive.Flag.CanInline
import avail.interpreter.execution.Interpreter
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * **Primitive:** Given a value that can be successfully marshaled to Java and a
 * [string][A_String] that names an instance [field][Field] of that value, bind
 * the field to a [variable][PojoFieldDescriptor] such that reads and writes of
 * this variable pass through to the bound field.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 */
@Suppress("unused")
object P_BindPojoInstanceField : Primitive(2, CanFold, CanInline)
{
	override fun attempt(interpreter: Interpreter): Result
	{
		interpreter.checkArgumentCount(2)
		val pojo = interpreter.argument(0)
		val fieldName = interpreter.argument(1)

		interpreter.availLoaderOrNull()?.statementCanBeSummarized(false)

		// Use the actual Java runtime type of the pojo to perform the
		// reflective field lookup.
		val obj = pojo.rawPojo().javaObjectNotNull<Any>()
		val javaClass = obj.javaClass
		val field: Field
		try
		{
			field = javaClass.getField(fieldName.asNativeString())
		}
		catch (e: NoSuchFieldException)
		{
			return interpreter.primitiveFailure(E_JAVA_FIELD_NOT_AVAILABLE)
		}

		// This is not the right primitive to bind static fields.
		if (Modifier.isStatic(field.modifiers))
		{
			return interpreter.primitiveFailure(E_JAVA_FIELD_NOT_AVAILABLE)
		}
		val fieldType = resolvePojoType(
			field.genericType, pojo.kind().typeVariables)
		val variable = pojoFieldVariableForInnerType(
			equalityPojo(field), pojo.rawPojo(), fieldType)
		return interpreter.primitiveSuccess(variable)
	}

	override fun privateBlockTypeRestriction(): A_Type =
		functionType(
			tuple(
				mostGeneralPojoType(),
				stringType),
			mostGeneralVariableType)

	override fun privateFailureVariableType(): A_Type =
		enumerationWith(set(E_JAVA_FIELD_NOT_AVAILABLE))
}
