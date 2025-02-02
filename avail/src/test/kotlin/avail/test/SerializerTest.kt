/*
 * SerializerTest.kt
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
package avail.test

import avail.AvailRuntime
import avail.builder.ModuleNameResolver
import avail.builder.ModuleRoots
import avail.builder.RenamesFileParser
import avail.builder.RenamesFileParserException
import avail.descriptor.atoms.A_Atom
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.atoms.AtomDescriptor.Companion.createAtom
import avail.descriptor.atoms.AtomDescriptor.Companion.falseObject
import avail.descriptor.atoms.AtomDescriptor.Companion.trueObject
import avail.descriptor.character.CharacterDescriptor.Companion.fromCodePoint
import avail.descriptor.functions.A_Function
import avail.descriptor.functions.A_RawFunction
import avail.descriptor.functions.A_RawFunction.Companion.literalAt
import avail.descriptor.functions.A_RawFunction.Companion.numArgs
import avail.descriptor.functions.A_RawFunction.Companion.numLiterals
import avail.descriptor.functions.A_RawFunction.Companion.numLocals
import avail.descriptor.functions.A_RawFunction.Companion.numOuters
import avail.descriptor.functions.A_RawFunction.Companion.numSlots
import avail.descriptor.functions.A_RawFunction.Companion.nybbles
import avail.descriptor.functions.FunctionDescriptor.Companion.createFunction
import avail.descriptor.maps.A_Map.Companion.mapAtPuttingCanDestroy
import avail.descriptor.maps.MapDescriptor.Companion.emptyMap
import avail.descriptor.module.A_Module.Companion.addPrivateName
import avail.descriptor.module.ModuleDescriptor.Companion.newModule
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromInt
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromLong
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.NilDescriptor
import avail.descriptor.sets.SetDescriptor.Companion.setFromCollection
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tuple
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.tuples.TupleDescriptor.Companion.tupleFromIntegerList
import avail.descriptor.types.BottomTypeDescriptor.Companion.bottom
import avail.descriptor.types.PrimitiveTypeDescriptor.Types
import avail.files.FileManager
import avail.interpreter.levelOne.L1InstructionWriter
import avail.interpreter.primitive.floats.P_FloatFloor
import avail.serialization.Deserializer
import org.availlang.persistence.MalformedSerialStreamException
import avail.serialization.Serializer
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.StringReader
import java.util.Random
import java.util.concurrent.Semaphore
import kotlin.math.min

/**
 * Unit tests for object serialization.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 */
@TestInstance(Lifecycle.PER_CLASS)
class SerializerTest
{
	/**
	 * The [AvailRuntime] for use by the [serializer] or the [deserializer].
	 */
	private var runtime: AvailRuntime? = null

	/**
	 * The [FileManager] used to conduct these tests.
	 */
	private val fileManager = FileManager()

	/**
	 * @return
	 *   The [AvailRuntime] used by the serializer and deserializer.
	 */
	private fun runtime(): AvailRuntime = runtime!!

	/**
	 * Test fixture: clear and then create all special objects well-known to the
	 * Avail runtime.
	 */
	@BeforeAll
	fun initializeAllWellKnownObjects()
	{
		val semaphore = Semaphore(0)
		val projectDirectory = File(".").absolutePath
			.replace("/avail", "")
		val rootDirectory =
			projectDirectory.substring(0..(projectDirectory.length - 2))
		val availRoot =
			File(rootDirectory).resolve("distro/src/avail").absolutePath
		val roots = ModuleRoots(fileManager, "avail=$availRoot")
		{
			if (it.isNotEmpty())
			{
				System.err.println(
					"Serializer Test: Failed to fully resolve ModuleRoots")
				it.forEach { msg -> System.err.println(msg) }
			}
			semaphore.release()
		}
		semaphore.acquireUninterruptibly()
		val parser = RenamesFileParser(StringReader(""), roots)
		val resolver: ModuleNameResolver = try
		{
			parser.parse()
		}
		catch (e: RenamesFileParserException)
		{
			throw RuntimeException(e)
		}
		runtime = AvailRuntime(resolver, fileManager)
		fileManager.associateRuntime(runtime())
	}

	/**
	 * Test fixture: clear all special objects.
	 */
	@AfterAll
	fun clearAllWellKnownObjects()
	{
		val theRuntime = runtime
		if (theRuntime !== null)
		{
			theRuntime.destroy()
			runtime = null
		}
	}

	/**
	 * The stream onto which the serializer writes its bytes.
	 */
	var out: ByteArrayOutputStream? = null

	/**
	 * The [Serializer] which converts objects to bytes.
	 */
	private var serializer: Serializer? = null

	/**
	 * @return
	 *   The [Serializer] used by this test class.
	 */
	private fun serializer(): Serializer = serializer!!

	/**
	 * The source of bytes for the [Deserializer].
	 */
	private var inStream: ByteArrayInputStream? = null

	/**
	 * @return
	 *   The [ByteArrayInputStream] used by the deserializer.
	 */
	private fun inStream(): ByteArrayInputStream = inStream!!

	/**
	 * The [Deserializer] which (re)produces objects from bytes.
	 */
	private var deserializer: Deserializer? = null

	/**
	 * @return
	 *   The [Deserializer] used by this test class.
	 */
	private fun deserializer(): Deserializer = deserializer!!

	/**
	 * Get ready to write objects to the [serializer].
	 */
	private fun prepareToWrite()
	{
		out = ByteArrayOutputStream(1000)
		serializer = Serializer(out!!)
		deserializer = null
	}

	/**
	 * Finish writing serialized objects and prepare to deserialize them back
	 * again.
	 */
	private fun prepareToReadBack()
	{
		val theByteStream = out!!
		val bytes = theByteStream.toByteArray()
		inStream = ByteArrayInputStream(bytes)
		deserializer = Deserializer(inStream!!, runtime())
		serializer = null
	}

	/**
	 * Serialize then deserialize the given object.
	 *
	 * @param object
	 *   The object to serialize and deserialize.
	 * @return
	 *   The result of serializing and deserializing the argument.
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Throws(MalformedSerialStreamException::class)
	private fun roundTrip(`object`: A_BasicObject): AvailObject
	{
		prepareToWrite()
		serializer().serialize(`object`)
		prepareToReadBack()
		val newObject = deserializer().deserialize()!!
		assertEquals(
			0,
			inStream().available(),
			"Serialization stream was not fully emptied")
		val objectAfter = deserializer().deserialize()
		assert(objectAfter === null)
		return newObject
	}

	/**
	 * Ensure that the given object can be serialized and deserialized,
	 * producing an object equal to the original.  This is not universally
	 * expected to be true for all objects, as for example
	 * [atoms][AtomDescriptor] need to be looked up or recreated (unequally) by
	 * a [Deserializer].
	 *
	 * @param object
	 *   The object to serialize, deserialize, and compare to the original.
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Throws(MalformedSerialStreamException::class)
	private fun checkObject(`object`: A_BasicObject)
	{
		val newObject: A_BasicObject = roundTrip(`object`)
		assertEquals(`object`, newObject)
	}

	/**
	 * Test serialization of booleans.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testBooleans()
	{
		checkObject(trueObject)
		checkObject(falseObject)
	}

	/**
	 * Test serialization of various integers.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testIntegers()
	{
		for (i in -500 .. 499)
		{
			checkObject(fromInt(i))
		}
		checkObject(fromInt(10000))
		checkObject(fromInt(100000))
		checkObject(fromInt(1000000))
		checkObject(fromInt(10000000))
		checkObject(fromInt(100000000))
		checkObject(fromInt(1000000000))
		checkObject(fromInt(Int.MAX_VALUE))
		checkObject(fromInt(Int.MIN_VALUE))
		checkObject(fromLong(Long.MIN_VALUE))
		checkObject(fromLong(Long.MAX_VALUE))
	}

	/**
	 * Test serialization of some strings.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testStrings()
	{
		checkObject(stringFrom(""))
		for (i in 1 .. 99)
		{
			checkObject(stringFrom(i.toString()))
		}
		checkObject(stringFrom("\u0000"))
		checkObject(stringFrom("\u0001"))
		checkObject(stringFrom("\u0003\u0002\u0001\u0000"))
		checkObject(stringFrom("Cheese \"cake\" surprise"))
		checkObject(stringFrom("\u00FF"))
		checkObject(stringFrom("\u0100"))
		checkObject(stringFrom("\u0101"))
		checkObject(stringFrom("I like peace ☮"))
		checkObject(stringFrom("I like music 𝄞"))
		checkObject(stringFrom("I really like music 𝄞𝄞"))
	}

	/**
	 * Test serialization of some tuples of integers.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testNumericTuples()
	{
		checkObject(tupleFromIntegerList(listOf(0)))
		checkObject(tupleFromIntegerList(listOf(1)))
		checkObject(tupleFromIntegerList(listOf(2)))
		checkObject(tupleFromIntegerList(listOf(3)))
		checkObject(tupleFromIntegerList(listOf(10, 20)))
		checkObject(tupleFromIntegerList(listOf(10, 20, 10)))
		checkObject(tupleFromIntegerList(listOf(100, 200)))
		checkObject(tupleFromIntegerList(listOf(100, 2000)))
		checkObject(tupleFromIntegerList(listOf(999999999)))
		for (i in -500 .. 499)
		{
			checkObject(tupleFromIntegerList(listOf(i)))
		}
		checkObject(
			tuple(
				fromLong(Int.MIN_VALUE.toLong()),
				fromLong(Int.MAX_VALUE.toLong())))
		checkObject(
			tuple(fromLong(Long.MIN_VALUE), fromLong(Long.MAX_VALUE)))
	}

	/**
	 * Test random combinations of tuples, sets, maps, integers, and characters.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testRandomSimpleObjects()
	{
		val random = Random(-4673928647810677481L)
		for (run in 0 .. 99)
		{
			val partsCount = 1 + random.nextInt(1000)
			val parts = mutableListOf<A_BasicObject>()
			for (partIndex in 0 until partsCount)
			{
				val newObject: A_BasicObject
				val choice =
					if (partIndex == partsCount - 1) random.nextInt(3) + 2
					else random.nextInt(5)
				newObject = when (choice)
				{
					0 -> fromInt(random.nextInt())
					1 -> fromCodePoint(random.nextInt(0x110000))
					else ->
					{
						var size = random.nextInt(min(20, partIndex + 1))
						if (choice == 4)
						{
							// For a map.
							size = size and 1.inv()
						}
						val members = mutableListOf<A_BasicObject>()
						for (i in 0 until size)
						{
							members.add(parts[random.nextInt(partIndex)])
						}
						when (choice)
						{
							2 -> tupleFromList(members)
							3 -> setFromCollection(members)
							else ->
							{
								//if (choice == 4)
								var map = emptyMap
								var i = 0
								while (i < size)
								{
									map = map.mapAtPuttingCanDestroy(
										members[i],
										members[i + 1],
										true)
									i += 2
								}
								map
							}
						}
					}
				}
				newObject.makeImmutable()
				parts.add(newObject)
			}
			assert(parts.size == partsCount)
			checkObject(parts[partsCount - 1])
		}
	}

	/**
	 * Test serialization and deserialization of atom references.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testAtoms()
	{
		val inputModule = newModule(stringFrom("Imported"))
		val currentModule = newModule(stringFrom("Current"))
		val atom1: A_Atom = createAtom(stringFrom("importAtom1"), inputModule)
		inputModule.addPrivateName(atom1)
		val atom2: A_Atom = createAtom(
			stringFrom("currentAtom2"), currentModule)
		currentModule.addPrivateName(atom2)
		val tuple = tuple(atom1, atom2)
		prepareToWrite()
		serializer().serialize(tuple)
		runtime().addModule(inputModule)
		prepareToReadBack()
		deserializer().currentModule = currentModule
		val newObject: A_BasicObject? = deserializer().deserialize()
		assertNotNull(newObject)
		assertEquals(
			0,
			inStream().available(),
			"Serialization stream was not fully emptied")
		val nullObject: A_BasicObject? = deserializer().deserialize()
		assertNull(nullObject)
		assertEquals(tuple, newObject)
	}

	/**
	 * Test serialization and deserialization of functions.
	 *
	 * @throws MalformedSerialStreamException
	 *   If the stream is malformed.
	 */
	@Test
	@Throws(MalformedSerialStreamException::class)
	fun testFunctions()
	{
		val writer = L1InstructionWriter(
			NilDescriptor.nil, 0, NilDescriptor.nil)
		writer.argumentTypes(Types.FLOAT.o)
		writer.primitive = P_FloatFloor
		writer.returnType = Types.FLOAT.o
		writer.returnTypeIfPrimitiveFails = bottom
		val code: A_RawFunction = writer.compiledCode()
		val function = createFunction(code, emptyTuple)
		val newFunction: A_Function = roundTrip(function)
		val code2 = newFunction.code()
		assertEquals(code.numOuters, code2.numOuters)
		assertEquals(code.numSlots, code2.numSlots)
		assertEquals(code.numArgs(), code2.numArgs())
		assertEquals(code.numLocals, code2.numLocals)
		assertEquals(code.codePrimitive(), code2.codePrimitive())
		assertEquals(code.nybbles, code2.nybbles)
		assertEquals(code.functionType(), code2.functionType())
		for (i in code.numLiterals downTo 1)
		{
			assertEquals(code.literalAt(i), code2.literalAt(i))
		}
	}
}
