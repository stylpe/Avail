/*
 * L1InstructionWriter.kt
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

package avail.interpreter.levelOne

import avail.descriptor.functions.A_RawFunction
import avail.descriptor.functions.CompiledCodeDescriptor
import avail.descriptor.functions.CompiledCodeDescriptor.Companion.newCompiledCode
import avail.descriptor.functions.ContinuationDescriptor
import avail.descriptor.functions.FunctionDescriptor
import avail.descriptor.module.A_Module
import avail.descriptor.numbers.IntegerDescriptor
import avail.descriptor.phrases.A_Phrase
import avail.descriptor.phrases.A_Phrase.Companion.argumentsTuple
import avail.descriptor.phrases.A_Phrase.Companion.token
import avail.descriptor.phrases.BlockPhraseDescriptor.Companion.constants
import avail.descriptor.phrases.BlockPhraseDescriptor.Companion.locals
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.NilDescriptor
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.NybbleTupleDescriptor
import avail.descriptor.tuples.NybbleTupleDescriptor.Companion.generateNybbleTupleFrom
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.descriptor.tuples.TupleDescriptor
import avail.descriptor.tuples.TupleDescriptor.Companion.tupleFromIntegerList
import avail.descriptor.types.A_Type
import avail.descriptor.types.BottomTypeDescriptor.Companion.bottom
import avail.descriptor.types.FunctionTypeDescriptor.Companion.functionType
import avail.descriptor.types.InstanceMetaDescriptor.Companion.topMeta
import avail.descriptor.types.TypeDescriptor
import avail.interpreter.Primitive
import avail.interpreter.Primitive.Flag
import avail.interpreter.levelOne.L1Operation.L1_doExtension
import avail.io.NybbleOutputStream
import java.util.Collections.addAll

/**
 * An instance of this class can be used to construct a
 * [compiled&#32;code&#32;object][CompiledCodeDescriptor] without detailed
 * knowledge of the level one nybblecode instruction set.
 *
 * @property module
 *   The module containing this code.
 * @property startingLineNumber
 *   The line number at which this code starts.
 * @property phrase
 *   The phrase that should be captured for this raw function.
 *   [nil][NilDescriptor.nil] is also valid.
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 *
 * @constructor
 *
 * Create a new `L1InstructionWriter Level One instruction writer`.
 *
 * @param module
 *   The module containing this code.
 * @param startingLineNumber
 *   Where this code starts in the module.
 * @param phrase
 *   The phrase that should be captured for this raw function.
 *   [nil][NilDescriptor.nil] is also valid, but less informative.
 */
class L1InstructionWriter constructor(
	internal val module: A_Module,
	internal val startingLineNumber: Int,
	internal val phrase: A_Phrase)
{
	/** The stream of nybbles that have been generated thus far. */
	private val stream = NybbleOutputStream()

	/**
	 * The collection of literal objects that have been accumulated thus far.
	 */
	internal val literals: MutableList<AvailObject> = mutableListOf()

	/**
	 * An inverse mapping of the literal objects encountered thus far.  The map
	 * is from each literal object to its 1-based index.
	 */
	private val reverseLiterals = mutableMapOf<A_BasicObject, Int>()

	/**
	 * The [List] of argument [types][TypeDescriptor] for this
	 * [compiled&#32;code][CompiledCodeDescriptor].
	 */
	private var argumentTypes = mutableListOf<A_Type>()

	/** The return type of the [FunctionDescriptor] under construction. */
	var returnType: A_Type? = null

	/**
	 * The return type that will be produced if the function is not a primitive,
	 * or if the primitive fails.
	 */
	var returnTypeIfPrimitiveFails: A_Type? = null

	/** The types of the local variables. */
	private val localTypes = mutableListOf<A_Type>()

	/** The types of the local constants. */
	private val constantTypes = mutableListOf<A_Type>()

	/**
	 * The types of the outer (lexically captured) variables.  Note that
	 * arguments and constants in outer scopes can also be captured, which
	 * aren't technically variables.
	 */
	private val outerTypes = mutableListOf<A_Type>()

	/**
	 * The [primitive][Primitive] of the [compiled&#32;code][A_RawFunction]
	 * being generated.
	 */
	var primitive: Primitive? = null
		set(newValue)
		{
			assert(field === null) { "Don't set the primitive twice" }
			field = newValue
		}

	/** The line number at which the current nybblecode is associated. */
	private var currentLineNumber: Int = startingLineNumber

	/**
	 * For the start of each nybblecode instruction, indicate how much to add to
	 * the code's starting line number to get to the line number in the source
	 * that led to generation of that nybblecode.
	 *
	 * The values are encoded to increase the odds that the tuple can be
	 * represented as nybbles or unsigned bytes.  Expressions' evaluation can
	 * sometimes effectively step backwards syntactically, so negative numbers
	 * need to be encoded as well.  We use the low bit to distinguish forward
	 * deltas (even) from backward deltas (odd), shifting right to get the
	 * magnitude.  This introduces an intentional encoding hole (the value 1
	 * represents negative zero), which we may exploit at a later date.
	 */
	private val lineNumberEncodedDeltas = mutableListOf<Int>()

	/**
	 * The [mechanism][L1StackTracker] used to ensure the stack is correctly
	 * balanced at the end and does not pop more than has been pushed. It also
	 * records the maximum stack depth for correctly sizing
	 * [continuations][ContinuationDescriptor] (essentially stack frames) at
	 * runtime.
	 */
	private val stackTracker: L1StackTracker = object : L1StackTracker()
	{
		override fun literalAt(literalIndex: Int) = literals[literalIndex - 1]
	}

	/**
	 * Locate or record the specified literal object.  Answer its 1-based index.
	 *
	 * @param literal The literal object to look up.
	 * @return The object's 1-based literal index.
	 */
	fun addLiteral(literal: A_BasicObject): Int
	{
		var index: Int? = reverseLiterals[literal]
		if (index === null)
		{
			literals.add(literal as AvailObject)
			index = literals.size
			reverseLiterals[literal] = index
		}
		return index
	}

	/**
	 * Set the array of types of the arguments.
	 *
	 * @param argTypes
	 *   The vararg array of argument types.
	 */
	fun argumentTypes(vararg argTypes: A_Type)
	{
		assert(argumentTypes.isEmpty())
		assert(bottom !in argTypes)
		assert(localTypes.size == 0) {
			"Must declare argument types before allocating locals"
		}
		addAll(argumentTypes, *argTypes)
	}

	/**
	 * Specify the types of the arguments that the resulting
	 * [compiled&#32;code&#32;object][CompiledCodeDescriptor] will accept.
	 *
	 * @param argTypes
	 *   A [tuple][TupleDescriptor] of [types][TypeDescriptor] corresponding
	 *   with the types which the [FunctionDescriptor] under construction will
	 *   accept.
	 */
	fun argumentTypesTuple(argTypes: A_Tuple)
	{
		assert(argumentTypes.isEmpty())
		assert(bottom !in argTypes)
		assert(localTypes.size == 0) {
			"Must declare argument types before allocating locals"
		}
		argumentTypes.addAll(argTypes)
	}

	/**
	 * Declare a local variable with the specified type.  Answer its index.  The
	 * index is relative to the start of the arguments.
	 *
	 * @param localType
	 *   The [type][TypeDescriptor] of the local.
	 * @return
	 *   The index of the local variable.
	 */
	fun createLocal(localType: A_Type): Int
	{
		assert(localType.isInstanceOf(topMeta()))
		localTypes.add(localType)
		return localTypes.size + argumentTypes.size
	}

	/**
	 * Declare an outer (lexically captured) variable, specifying its type.
	 *
	 * @param outerType
	 *   The [type][TypeDescriptor] of the outer variable.
	 * @return
	 *   The index of the newly declared outer variable.
	 */
	fun createOuter(outerType: A_Type): Int
	{
		outerTypes.add(outerType)
		return outerTypes.size
	}

	/**
	 * Write a numerically encoded operand.  All operands are encoded the same
	 * way in the level one nybblecode instruction set.  Values 0-9 take a
	 * single nybble, 10-57 take two, 58-313 take three, 314-65535 take five,
	 * and 65536..2^31-1 take nine nybbles.
	 *
	 * @param operand
	 *   An [Int]-encoded operand of some [operation][L1Operation].
	 */
	private fun writeOperand(operand: Int)
	{
		when
		{
			operand < 10 ->
			{
				stream.write(operand)
			}
			operand < 58 ->
			{
				stream.write((operand + 150).ushr(4))
				stream.write((operand + 150) and 15)
			}
			operand < 314 ->
			{
				stream.write(13)
				stream.write((operand - 58).ushr(4))
				stream.write((operand - 58) and 15)
			}
			operand < 65536 ->
			{
				stream.write(14)
				stream.write(operand.ushr(12))
				stream.write(operand.ushr(8) and 15)
				stream.write(operand.ushr(4) and 15)
				stream.write(operand and 15)
			}
			else ->
			{
				stream.write(15)
				stream.write(operand.ushr(28))
				stream.write(operand.ushr(24) and 15)
				stream.write(operand.ushr(20) and 15)
				stream.write(operand.ushr(16) and 15)
				stream.write(operand.ushr(12) and 15)
				stream.write(operand.ushr(8) and 15)
				stream.write(operand.ushr(4) and 15)
				stream.write(operand and 15)
			}
		}
	}

	/**
	 * Write an L1 instruction to the nybblecode stream. Some opcodes
	 * (automatically) write an extension nybble (`0xF`).  Also write any
	 * [operands][L1OperandType].  Validate that the right number of operands
	 * are provided.
	 *
	 * @param lineNumber
	 *   The source code line number responsible for generating this
	 *   instruction.
	 * @param operation
	 *   The [L1Operation] to write.
	 * @param operands
	 *   The [Int] operands for the operation.
	 */
	fun write(lineNumber: Int, operation: L1Operation, vararg operands: Int)
	{
		val newLineNumber =
			if (lineNumber == 0) currentLineNumber else lineNumber
		val delta = newLineNumber - currentLineNumber
		val encodedDelta = if (delta < 0) delta shl 1 else -delta shl 1 or 1
		lineNumberEncodedDeltas.add(encodedDelta)
		currentLineNumber = newLineNumber

		stackTracker.track(operation, *operands)
		val opcode = operation.ordinal.toByte()
		if (opcode <= 15)
		{
			stream.write(opcode.toInt())
		}
		else
		{
			stream.write(L1_doExtension.ordinal)
			stream.write(opcode - 16)
		}
		operands.forEach(this::writeOperand)
	}

	/**
	 * Extract the [tuple&#32;of&#32;nybbles][NybbleTupleDescriptor] encoding
	 * the instructions of the [compiled&#32;code][CompiledCodeDescriptor] under
	 * construction.
	 *
	 * @return
	 *   A [TupleDescriptor] of nybbles ([integers][IntegerDescriptor] in the
	 *   range 0..15).
	 */
	private fun nybbles(): AvailObject
	{
		val size = stream.size()
		val byteArray = stream.toByteArray()
		val nybbles = generateNybbleTupleFrom(size) {
			val nybble = byteArray[it - 1].toInt()
			assert(nybble < 16)
			nybble
		}
		nybbles.makeImmutable()
		return nybbles
	}

	/**
	 * Produce the [compiled&#32;code&#32;object][CompiledCodeDescriptor] which
	 * we have just incrementally specified.
	 *
	 * @return
	 *   A compiled code object (which can be lexically closed to a
	 *   [function][FunctionDescriptor] by supplying the outer variables to
	 *   capture).
	 */
	fun compiledCode(): AvailObject
	{
		val p = primitive
		assert(p === null || p.hasFlag(Flag.CannotFail) || localTypes.size > 0) {
			"Fallible primitive needs a primitive failure variable"
		}
		val names = mutableListOf<String>()
		if (phrase.notNil)
		{
			listOf(phrase.argumentsTuple, locals(phrase), constants(phrase))
				.flatten()
				.map { it.token.string().asNativeString() }
		}
		else
		{
			// Synthesize fake names to ensure we have the right number of
			// names for the arguments and locals.
			var counter = 1
			names.addAll(argumentTypes.map { "arg${counter++}" })
			names.addAll(localTypes.map { "local${counter++}" })
			names.addAll(constantTypes.map { "constant${counter++}" })
		}
		val packedDeclarationNames = names.joinToString(",") { name ->
			// Quote-escape any name that contains a comma (,) or quote (").
			if (name.contains(',') || name.contains('\"'))
				stringFrom(name).toString()
			else name
		}

		return newCompiledCode(
			nybbles(),
			stackTracker.maxDepth,
			functionType(tupleFromList(argumentTypes), returnType!!),
			primitive,
			returnTypeIfPrimitiveFails!!,
			tupleFromList(literals),
			tupleFromList(localTypes),
			tupleFromList(constantTypes),
			tupleFromList(outerTypes),
			module,
			startingLineNumber,
			tupleFromIntegerList(lineNumberEncodedDeltas),
			-1,
			phrase,
			stringFrom(packedDeclarationNames))
	}
}
