/*
 * PojoTypeDescriptor.kt
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
package avail.descriptor.types

import avail.descriptor.atoms.A_Atom
import avail.descriptor.atoms.AtomDescriptor
import avail.descriptor.atoms.AtomDescriptor.Companion.createSpecialAtom
import avail.descriptor.atoms.AtomDescriptor.Companion.objectFromBoolean
import avail.descriptor.maps.A_Map
import avail.descriptor.maps.A_Map.Companion.keysAsSet
import avail.descriptor.maps.A_Map.Companion.mapAt
import avail.descriptor.maps.A_Map.Companion.mapAtOrNull
import avail.descriptor.maps.A_Map.Companion.mapAtPuttingCanDestroy
import avail.descriptor.maps.MapDescriptor
import avail.descriptor.maps.MapDescriptor.Companion.emptyMap
import avail.descriptor.numbers.DoubleDescriptor.Companion.fromDouble
import avail.descriptor.numbers.FloatDescriptor.Companion.fromFloat
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromBigInteger
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromInt
import avail.descriptor.numbers.IntegerDescriptor.Companion.fromLong
import avail.descriptor.pojos.PojoDescriptor.Companion.newPojo
import avail.descriptor.pojos.PojoDescriptor.Companion.nullPojo
import avail.descriptor.pojos.RawPojoDescriptor
import avail.descriptor.pojos.RawPojoDescriptor.Companion.equalityPojo
import avail.descriptor.pojos.RawPojoDescriptor.Companion.rawObjectClass
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.IntegerSlotsEnum
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.NilDescriptor
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.sets.A_Set
import avail.descriptor.sets.A_Set.Companion.setIntersectionCanDestroy
import avail.descriptor.sets.A_Set.Companion.setUnionCanDestroy
import avail.descriptor.sets.SetDescriptor
import avail.descriptor.sets.SetDescriptor.Companion.setFromCollection
import avail.descriptor.tuples.A_Tuple
import avail.descriptor.tuples.A_Tuple.Companion.tupleAt
import avail.descriptor.tuples.A_Tuple.Companion.tupleSize
import avail.descriptor.tuples.ObjectTupleDescriptor.Companion.tupleFromList
import avail.descriptor.tuples.RepeatedElementTupleDescriptor.Companion.createRepeatedElementTuple
import avail.descriptor.tuples.StringDescriptor
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.descriptor.tuples.TupleDescriptor
import avail.descriptor.tuples.TupleDescriptor.Companion.emptyTuple
import avail.descriptor.types.A_Type.Companion.isSubtypeOf
import avail.descriptor.types.A_Type.Companion.isSupertypeOfPojoType
import avail.descriptor.types.A_Type.Companion.typeIntersection
import avail.descriptor.types.A_Type.Companion.typeIntersectionOfPojoType
import avail.descriptor.types.A_Type.Companion.typeUnion
import avail.descriptor.types.A_Type.Companion.typeUnionOfPojoType
import avail.descriptor.types.ArrayPojoTypeDescriptor.Companion.arrayPojoType
import avail.descriptor.types.BottomPojoTypeDescriptor.Companion.pojoBottom
import avail.descriptor.types.BottomTypeDescriptor.Companion.bottom
import avail.descriptor.types.EnumerationTypeDescriptor.Companion.booleanType
import avail.descriptor.types.FusedPojoTypeDescriptor.Companion.createFusedPojoType
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.nonnegativeInt32
import avail.descriptor.types.IntegerRangeTypeDescriptor.Companion.wholeNumbers
import avail.descriptor.types.PrimitiveTypeDescriptor.Types
import avail.exceptions.MarshalingException
import avail.utility.Mutable
import avail.utility.cast
import org.availlang.cache.LRUCache
import java.lang.reflect.Constructor
import java.lang.reflect.Executable
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.math.BigInteger
import java.util.IdentityHashMap

/**
 * An `PojoTypeDescriptor` describes the type of a plain-old Java object (pojo)
 * that is accessible to an Avail programmer as an
 * [Avail&#32;object][AvailObject].
 *
 * Even though Java uses type erasure for its generic types, Java class files
 * contain enough reflectively available information about genericity for Avail
 * to expose Java types as if they were fully polymorphic (like Avail's own
 * types). Avail does not need to create new Java types by extending the Java
 * class hierarchy, so there is no need to model Java generic types directly.
 * Polymorphic types are therefore sufficient for construction and employment,
 * which runs the gamut of purposes from an Avail programmer's perspective.
 *
 * Java interfaces are presented to Avail as though they were Java classes.
 * Avail sees interface inheritance as though it were class inheritance, with
 * root interfaces implicitly inheriting from [Object]. So an Avail
 * programmer sees Java as though it supported multiple inheritance of classes.
 *
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 *
 * @constructor
 * Construct a new [PojoTypeDescriptor].
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 * @param objectSlotsEnumClass
 *   The Java [Class] which is a subclass of [ObjectSlotsEnum] and defines this
 *   object's object slots layout, or null if there are no object slots.
 * @param integerSlotsEnumClass
 *   The Java [Class] which is a subclass of [IntegerSlotsEnum] and defines this
 *   object's object slots layout, or null if there are no integer slots.
 */
abstract class PojoTypeDescriptor
protected constructor(
	mutability: Mutability,
	objectSlotsEnumClass: Class<out ObjectSlotsEnum>?,
	integerSlotsEnumClass: Class<out IntegerSlotsEnum>?
) : TypeDescriptor(
	mutability,
	TypeTag.POJO_TYPE_TAG,
	TypeTag.POJO_TAG,
	objectSlotsEnumClass,
	integerSlotsEnumClass)
{
	/**
	 * `Canon` specifies a [map][Map] from [Java&#32;classes][Class] to
	 * [type&#32;parameterization][TupleDescriptor].
	 */
	private class Canon : LinkedHashMap<Class<*>, AvailObject>(5)
	{
		/**
		 * Answer the locally canonical [raw&#32;pojo][RawPojoDescriptor] that
		 * represents the specified [Java&#32;class][Class]. (If the canon
		 * already contains a raw pojo for the class, then answer it. If not,
		 * then install a new one and answer that one.)
		 *
		 * @param javaClass
		 *   A Java class or interface.
		 * @return
		 *   A locally canonical raw pojo corresponding to the argument.
		 */
		fun canonize(javaClass: Class<*>?): AvailObject
		{
			var rawPojo = get(javaClass)
			if (rawPojo === null)
			{
				rawPojo = equalityPojo(javaClass!!)
				put(javaClass, rawPojo)
			}
			return rawPojo
		}

		/**
		 * Construct a new `Canon` that has initial capacity for five
		 * bindings and includes a binding for `java.lang.Object`.
		 */
		init
		{
			put(Any::class.java, rawObjectClass())
		}
	}

	/**
	 * `TypeVariableMap` is a [map][Map] from
	 * [local&#32;type&#32;variable&#32;names][String] to their type
	 * parameterization indices.
	 *
	 * @constructor
	 * Construct a new `TypeVariableMap` for the specified
	 * [Java&#32;class&#32;or&#32;interface][Class].
	 *
	 * @param javaClass
	 *   A Java class or interface.
	 */
	private class TypeVariableMap(javaClass: Class<*>)
		: LinkedHashMap<String, Int>(2)
	{
		init
		{
			val vars = javaClass.typeParameters
			vars.forEachIndexed { i, param ->
				put(param.name, i)
			}
		}
	}

	/**
	 * `LRUCacheKey` combines a [Java&#32;class&#32;or&#32;interface][Class]
	 * with its complete type parameterization. It serves as the key to the
	 * [pojo type][PojoTypeDescriptor] [cache].
	 *
	 * @property javaClass
	 *   The [Java&#32;class&#32;or&#32;interface][Class].
	 * @property typeArgs
	 *   /** The type arguments. */
	 * @constructor
	 * Construct a new `LRUCacheKey`.
	 *
	 * @param javaClass
	 *   The [Java&#32;class&#32;or&#32;interface][Class].
	 * @param typeArgs
	 *   The type arguments.
	 */
	class LRUCacheKey internal constructor(
		val javaClass: Class<*>,
		val typeArgs: A_Tuple)
	{

		override fun equals(other: Any?): Boolean
		{
			if (other is LRUCacheKey)
			{
				return javaClass == other.javaClass
					&& typeArgs.equals(other.typeArgs)
			}
			return false
		}

		override fun hashCode(): Int
		{
			return javaClass.hashCode() * typeArgs.hash() xor 0x1FA07381
		}

	}

	override fun o_Equals(
		self: AvailObject,
		another: A_BasicObject): Boolean
	{
		// Short circuit if the arguments are reference identical.
		return if (another.traversed().sameAddressAs(self))
		{
			true
		}
		else
		{
			self.isPojoType == another.isPojoType
				&& self.isPojoFusedType == another.isPojoFusedType
				&& self.isPojoArrayType == another.isPojoArrayType
				&& self.hash() == another.hash()
				&& another.equalsPojoType(self)
		}
		// Note that pojo bottom is a pojo array type.
	}

	abstract override fun o_EqualsPojoType(
		self: AvailObject,
		aPojoType: AvailObject): Boolean

	abstract override fun o_Hash(self: AvailObject): Int

	abstract override fun o_IsAbstract(self: AvailObject): Boolean

	abstract override fun o_IsPojoArrayType(self: AvailObject): Boolean

	abstract override fun o_IsPojoFusedType(self: AvailObject): Boolean

	override fun o_IsPojoSelfType(self: AvailObject): Boolean = false

	override fun o_IsPojoType(self: AvailObject): Boolean = true

	override fun o_IsSubtypeOf(self: AvailObject, aType: A_Type): Boolean =
		aType.isSupertypeOfPojoType(self)

	// Every pojo type is a supertype of pojo bottom.
	override fun o_IsSupertypeOfPojoBottomType(
		self: AvailObject, aPojoType: A_Type): Boolean = true

	override fun o_IsSupertypeOfPojoType(
		self: AvailObject,
		aPojoType: A_Type): Boolean
	{
		// If aPojoType is a self type, then answer whether object's self type
		// is a supertype of aPojoType.
		if (aPojoType.isPojoSelfType)
		{
			return self.pojoSelfType().isSupertypeOfPojoType(aPojoType)
		}
		// Check type compatibility by computing the set intersection of the
		// unparameterized ancestry of the arguments. If the result is not equal
		// to the unparameterized ancestry of object, then object is not a
		// supertype of aPojoType.
		val ancestors: A_Map = self.javaAncestors()
		val otherAncestors: A_Map = aPojoType.javaAncestors()
		val javaClasses = ancestors.keysAsSet
		val otherJavaClasses = otherAncestors.keysAsSet
		val intersection =
			javaClasses.setIntersectionCanDestroy(otherJavaClasses, false)
		if (!javaClasses.equals(intersection))
		{
			return false
		}
		// For each Java class in the intersection, ensure that the
		// parameterizations are compatible. Java's type parameters are
		// (brokenly) always covariant, so check that the type arguments of
		// aPojoType are subtypes of the corresponding type argument of object.
		for (javaClass in intersection)
		{
			val params: A_Tuple = ancestors.mapAt(javaClass)
			val otherParams: A_Tuple = otherAncestors.mapAt(javaClass)
			val limit = params.tupleSize
			for (i in 1 .. limit)
			{
				val x: A_Type = params.tupleAt(i)
				val y: A_Type = otherParams.tupleAt(i)
				if (!y.isSubtypeOf(x))
				{
					return false
				}
			}
		}
		// If object is a supertype of aPojoType sans any embedded pojo self
		// types, then object is really a supertype of aPojoType. The
		// corresponding pojo self types must be compatible, and that
		// compatibility has already been checked indirectly by some part of the
		// above computation.
		return true
	}

	abstract override fun o_JavaAncestors(self: AvailObject): AvailObject

	abstract override fun o_JavaClass(self: AvailObject): AvailObject

	abstract override fun o_MarshalToJava(
		self: AvailObject,
		classHint: Class<*>?): Any?

	abstract override fun o_PojoSelfType(self: AvailObject): A_Type

	override fun o_TypeIntersection(self: AvailObject, another: A_Type): A_Type
	{
		if (self.isSubtypeOf(another))
		{
			return self
		}
		return if (another.isSubtypeOf(self)) another
		else another.typeIntersectionOfPojoType(self)
	}

	abstract override fun o_TypeIntersectionOfPojoType(
		self: AvailObject,
		aPojoType: A_Type): A_Type

	abstract override fun o_TypeIntersectionOfPojoFusedType(
		self: AvailObject,
		aFusedPojoType: A_Type): A_Type

	abstract override fun o_TypeIntersectionOfPojoUnfusedType(
		self: AvailObject,
		anUnfusedPojoType: A_Type): A_Type

	override fun o_TypeUnion(
		self: AvailObject,
		another: A_Type): A_Type
	{
		if (self.isSubtypeOf(another))
		{
			return another
		}
		return if (another.isSubtypeOf(self)) self
		else another.typeUnionOfPojoType(self)
	}

	abstract override fun o_TypeUnionOfPojoType(
		self: AvailObject,
		aPojoType: A_Type): A_Type

	abstract override fun o_TypeUnionOfPojoFusedType(
		self: AvailObject,
		aFusedPojoType: A_Type): A_Type

	abstract override fun o_TypeUnionOfPojoUnfusedType(
		self: AvailObject,
		anUnfusedPojoType: A_Type): A_Type

	abstract override fun o_TypeVariables(self: AvailObject): A_Map

	abstract override fun printObjectOnAvoidingIndent(
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int)

	companion object
	{
		/**
		 * Answer the most general pojo type.
		 *
		 * @return
		 *   The most general pojo type.
		 */
		fun mostGeneralPojoType(): A_Type =
			UnfusedPojoTypeDescriptor.mostGeneralType

		/**
		 * Answer the most general pojo array type.
		 *
		 * @return
		 *   The most general pojo array type.
		 */
		fun mostGeneralPojoArrayType(): A_Type =
			ArrayPojoTypeDescriptor.mostGeneralType

		/**
		 * A special [atom][AtomDescriptor] whose
		 * [instance&#32;type][InstanceTypeDescriptor] represents the self type
		 * of a [Java&#32;class&#32;or&#32;interface][Class].
		 */
		private val selfTypeAtom: A_Atom = createSpecialAtom("pojo self")

		/**
		 * Answer a special [atom][AtomDescriptor] whose
		 * [instance&#32;type][InstanceTypeDescriptor] represents the self type
		 * of a [Java&#32;class&#32;or&#32;interface][Class].
		 *
		 * @return
		 *   The pojo self type atom.
		 */
		fun pojoSelfTypeAtom(): A_Atom = selfTypeAtom

		/**
		 * A special [instance&#32;type][InstanceTypeDescriptor] that represents
		 * the self type of a [Java&#32;class&#32;or&#32;interface][Class].
		 */
		private val selfType: A_Type =
			InstanceTypeDescriptor.instanceType(selfTypeAtom).makeShared()

		/**
		 * Answer a special [instance&#32;type][InstanceTypeDescriptor] that
		 * represents the self type of a
		 * [Java&#32;class&#32;or&#32;interface][Class].
		 *
		 * @return
		 *   The pojo self type atom.
		 */
		fun pojoSelfType(): A_Type = selfType

		/**
		 * The [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] that
		 * corresponds to Java `byte`.
		 */
		private val byteRange: A_Type =
			IntegerRangeTypeDescriptor.inclusive(
				Byte.MIN_VALUE.toLong(),
				Byte.MAX_VALUE.toLong()).makeShared()

		/**
		 * Answer the [integer&#32;range&#32;type][IntegerRangeTypeDescriptor]
		 * that corresponds to Java `byte`.
		 *
		 * @return
		 *   `[-128..127]`.
		 */
		fun byteRange(): A_Type = byteRange

		/**
		 * The [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] that
		 * corresponds to Java `short`.
		 */
		private val shortRange: A_Type =
			IntegerRangeTypeDescriptor.inclusive(
				Short.MIN_VALUE.toLong(),
				Short.MAX_VALUE.toLong()).makeShared()

		/**
		 * Answer the [integer&#32;range&#32;type][IntegerRangeTypeDescriptor]
		 * that corresponds to Java `short`.
		 *
		 * @return
		 *   `[-32768..32767]`.
		 */
		fun shortRange(): A_Type = shortRange

		/**
		 * The [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] that
		 * corresponds to Java `int`.
		 */
		private val intRange = IntegerRangeTypeDescriptor.int32

		/**
		 * Answer the [integer&#32;range&#32;type][IntegerRangeTypeDescriptor]
		 * that corresponds to Java `int`.
		 *
		 * @return
		 *   `[-2147483648..2147483647]`.
		 */
		fun intRange(): A_Type = intRange

		/**
		 * The [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] that
		 * corresponds to Java `long`.
		 */
		private val longRange = IntegerRangeTypeDescriptor.int64

		/**
		 * Answer the [integer&#32;range&#32;type][IntegerRangeTypeDescriptor]
		 * that corresponds to Java `long`.
		 *
		 * @return
		 *   `[-9223372036854775808..9223372036854775807]`.
		 */
		fun longRange(): A_Type = longRange

		/**
		 * The [integer&#32;range&#32;type][IntegerRangeTypeDescriptor] that
		 * corresponds to Java `char`.
		 */
		private val charRange: A_Type =
			IntegerRangeTypeDescriptor.inclusive(
				Character.MIN_VALUE.code.toLong(),
				Character.MAX_VALUE.code.toLong()).makeShared()

		/**
		 * Answer the [integer&#32;range&#32;type][IntegerRangeTypeDescriptor]
		 * that corresponds to Java `char`.
		 *
		 * @return
		 *   `[-9223372036854775808..9223372036854775807]`.
		 */
		fun charRange(): A_Type = charRange


		/**
		 * Given an [LRUCacheKey], compute the corresponding
		 * [pojo&#32;type][PojoTypeDescriptor].
		 *
		 * @param key
		 * An `LRUCacheKey`.
		 * @return
		 *   A pojo type.
		 */
		fun computeValue(key: LRUCacheKey): AvailObject
		{
			// Java allows the operations defined in java.lang.Object to be
			// performed on interface types, so interfaces are implicitly subtypes
			// of java.lang.Object. Make this relationship explicit: seed the
			// ancestry with java.lang.Object.
			val canon = Canon()
			val ancestors = Mutable(emptyMap)
			ancestors.update {
				mapAtPuttingCanDestroy(
					canon[Any::class.java]!!, emptyTuple, true)
			}
			computeAncestry(key.javaClass, key.typeArgs, ancestors, canon)
			return UnfusedPojoTypeDescriptor.createUnfusedPojoType(
				canon[key.javaClass]!!, ancestors.value)
		}

		/**
		 * [Pojo&#32;types][PojoTypeDescriptor] are somewhat expensive to build,
		 * so cache them for efficiency.
		 */
		private val cache =
			LRUCache(1000, 10, { key: LRUCacheKey -> computeValue(key) })

		/**
		 * Compute the intersection of two [pojo&#32;types][PojoTypeDescriptor].
		 * This is utility method that only examines the
		 * [ancestry][AvailObject.javaAncestors] of the pojo types. It computes
		 * and answers the union of the key sets and the intersections of their
		 * parameterizations.
		 *
		 * @param self
		 *   A pojo type.
		 * @param aPojoType
		 *   Another pojo type.
		 * @return
		 *   A new ancestry map OR the bottom pojo type.
		 */
		@JvmStatic
		protected fun computeIntersection(
			self: A_BasicObject,
			aPojoType: A_BasicObject): A_BasicObject
		{
			val ancestors: A_Map = self.javaAncestors()
			val otherAncestors: A_Map = aPojoType.javaAncestors()
			val javaClasses = ancestors.keysAsSet
			val otherJavaClasses = otherAncestors.keysAsSet
			val union = javaClasses.setUnionCanDestroy(
				otherJavaClasses, false)
			var unionAncestors = emptyMap
			for (javaClass in union)
			{
				val params: A_Tuple = ancestors.mapAtOrNull(javaClass) ?:
					otherAncestors.mapAt(javaClass)
				val otherParams: A_Tuple =
					otherAncestors.mapAtOrNull(javaClass) ?:
						ancestors.mapAt(javaClass)
				val limit = params.tupleSize
				assert(limit == otherParams.tupleSize)
				val intersectionParams = mutableListOf<A_Type>()
				for (i in 1 .. limit)
				{
					val x: A_Type = params.tupleAt(i)
					val y: A_Type = otherParams.tupleAt(i)
					val intersection = x.typeIntersection(y)
					if (intersection.isSubtypeOf(pojoBottom()))
					{
						return pojoBottom()
					}
					intersectionParams.add(intersection)
				}
				unionAncestors = unionAncestors.mapAtPuttingCanDestroy(
					javaClass,
					tupleFromList(intersectionParams),
					true)
			}
			return unionAncestors
		}

		/**
		 * Compute the union of two [pojo&#32;types][PojoTypeDescriptor]. This
		 * is utility method that only examines the
		 * [ancestry][AvailObject.javaAncestors] of the pojo types. It computes
		 * and answers the intersection of the key sets and the union of their
		 * parameterizations.
		 *
		 * @param self
		 *   A pojo type.
		 * @param aPojoType
		 *   Another pojo type.
		 * @return
		 *   A new ancestry map.
		 */
		@JvmStatic
		protected fun computeUnion(
			self: A_BasicObject,
			aPojoType: A_BasicObject): A_Map
		{
			// Find the intersection of the key sets and the union of their
			// parameterizations.
			val ancestors: A_Map = self.javaAncestors()
			val otherAncestors: A_Map = aPojoType.javaAncestors()
			val javaClasses = ancestors.keysAsSet
			val otherJavaClasses = otherAncestors.keysAsSet
			val intersection = javaClasses.setIntersectionCanDestroy(
				otherJavaClasses, false)
			var intersectionAncestors = emptyMap
			for (javaClass in intersection)
			{
				val params: A_Tuple = ancestors.mapAt(javaClass)
				val otherParams: A_Tuple = otherAncestors.mapAt(javaClass)
				val limit = params.tupleSize
				assert(limit == otherParams.tupleSize)
				val unionParams = mutableListOf<A_Type>()
				for (i in 1 .. limit)
				{
					val x: A_Type = params.tupleAt(i)
					val y: A_Type = otherParams.tupleAt(i)
					val union = x.typeUnion(y)
					unionParams.add(union)
				}
				intersectionAncestors =
					intersectionAncestors.mapAtPuttingCanDestroy(
						javaClass.makeImmutable(),
						tupleFromList(unionParams),
						true)
			}
			return intersectionAncestors
		}

		/**
		 * Answer the locally childless [Java&#32;types][Class] from among the
		 * types present in the specified ancestry.
		 *
		 * @param ancestry
		 *   A [set][SetDescriptor] of [raw&#32;pojos][RawPojoDescriptor] that
		 *   wrap related Java types.
		 * @return
		 *   Those subset of the ancestry that is locally childless, i.e., those
		 *   elements that do not have any subtypes also present in the
		 *   ancestry.
		 */
		@JvmStatic
		protected fun childlessAmong(ancestry: A_Set): Set<AvailObject>
		{
			val childless = mutableSetOf<AvailObject>()
			for (ancestor in ancestry)
			{
				childless.add(ancestor)
			}
			for (ancestor in ancestry)
			{
				val possibleAncestor = ancestor.javaObjectNotNull<Class<*>>()
				for (child in ancestry)
				{
					val possibleChild = child.javaObjectNotNull<Class<*>>()
					if (possibleAncestor != possibleChild
						&& possibleAncestor.isAssignableFrom(possibleChild))
					{
						childless.remove(ancestor)
					}
				}
			}
			return childless
		}

		/**
		 * Answer the most specific [Java&#32;type][Class] present in the
		 * specified ancestry.
		 *
		 * @param ancestry
		 *   A [set][SetDescriptor] of [raw&#32;pojos][RawPojoDescriptor] that
		 *   wrap Java types. The set contains related types that were computed
		 *   during a type union of two [pojo&#32;types][PojoTypeDescriptor].
		 * @return
		 *   The most specific Java type in the set. Answer [nil][NilDescriptor]
		 *   if there is not a single most specific type (this can only happen
		 *   for interfaces).
		 */
		@JvmStatic
		protected fun mostSpecificOf(ancestry: A_Set): AvailObject
		{
			var answer = rawObjectClass()
			var mostSpecific: Class<*> = Any::class.java
			for (rawType in ancestry)
			{
				val javaClass = rawType.javaObjectNotNull<Class<*>>()
				if (mostSpecific.isAssignableFrom(javaClass))
				{
					mostSpecific = javaClass
					answer = rawType
				}
			}
			// If the (tentative) answer is an interface, then verify that it is
			// strictly more specific than all other types in the set.
			val modifiers = mostSpecific.modifiers
			if (Modifier.isInterface(modifiers))
			{
				for (rawType in ancestry)
				{
					val javaClass = rawType.javaObjectNotNull<Class<*>>()
					if (!javaClass.isAssignableFrom(mostSpecific))
					{
						return NilDescriptor.nil
					}
				}
			}
			return answer
		}

		/**
		 * Marshal the supplied [A_Tuple] of [A_Type]s.
		 *
		 * @param types
		 *   A [tuple][TupleDescriptor] of types.
		 * @return
		 *   The Java [classes][Class] that represent the supplied types.
		 * @throws MarshalingException
		 *   If marshaling fails for any of the supplied types.
		 */
		@Throws(MarshalingException::class)
		fun marshalTypes(types: A_Tuple): Array<Class<*>> =
			// Marshal the argument types.
			types.map {
				it.marshalToJava(null)!!.cast<Any, Class<*>>()
			}.toTypedArray()

		/**
		 * Marshal the supplied [A_Type], as though it will be used for
		 * [Executable] lookup, using a boxed Java class to represent a
		 * primitive Java type.
		 *
		 * @param type
		 *   A type.
		 * @return
		 *   The Java class that represents the supplied type.
		 * @throws MarshalingException
		 *   If marshaling fails for any reason.
		 */
		fun marshalDefiningType(type: A_Type): Class<*>
		{
			val marshalledType = type.marshalToJava(null)
			val aClass: Class<*> = marshalledType.cast()
			if (aClass.isPrimitive)
			{
				return when (aClass)
				{
					java.lang.Boolean::class.javaPrimitiveType ->
						java.lang.Boolean::class.java
					java.lang.Byte::class.javaPrimitiveType ->
						java.lang.Byte::class.java
					java.lang.Short::class.javaPrimitiveType ->
						java.lang.Short::class.java
					java.lang.Integer::class.javaPrimitiveType ->
						java.lang.Integer::class.java
					java.lang.Long::class.javaPrimitiveType ->
						java.lang.Long::class.java
					java.lang.Float::class.javaPrimitiveType ->
						java.lang.Float::class.java
					java.lang.Double::class.javaPrimitiveType ->
						java.lang.Double::class.java
					java.lang.Character::class.javaPrimitiveType ->
						java.lang.Character::class.java
					else -> aClass
				}
			}
			return aClass
		}

		/**
		 * Marshal the arbitrary [Java object][Object] to its counterpart
		 * [Avail&#32;object][AvailObject].
		 *
		 * @param self
		 *   A Java object, or `null`.
		 * @param type
		 *   A [type][TypeDescriptor] to which the resultant Avail object must
		 *   conform.
		 * @return
		 *   An Avail Object.
		 */
		fun unmarshal(self: Any?, type: A_Type): AvailObject
		{
			self ?: return nullPojo()
			val availObject = when (self.javaClass)
			{
				AvailObject::class.java -> self as AvailObject
				java.lang.Boolean::class.java ->
					objectFromBoolean(self as Boolean)
				java.lang.Byte::class.java ->
					fromInt((self as Byte).toInt())
				java.lang.Short::class.java ->
					fromInt((self as Short).toInt())
				java.lang.Integer::class.java -> fromInt(self as Int)
				java.lang.Long::class.java -> fromLong(self as Long)
				java.lang.Float::class.java -> fromFloat(self as Float)
				java.lang.Double::class.java -> fromDouble(self as Double)
				java.lang.Character::class.java ->
					fromInt((self as Char).code)
				java.lang.String::class.java -> stringFrom(self as String)
				BigInteger::class.java -> fromBigInteger(self as BigInteger)
				else -> newPojo(equalityPojo(self), type)
			}
			if (!availObject.isInstanceOf(type))
			{
				throw MarshalingException()
			}
			return availObject as AvailObject
		}

		/**
		 * Resolve the specified [type][Type] using the given
		 * [type&#32;variables][A_Type.typeVariables].
		 *
		 * @param type
		 *   A type.
		 * @param typeVars
		 *   A [map][MapDescriptor] from fully-qualified
		 *   [type&#32;variable][TypeVariable] [names][StringDescriptor] to
		 *   their [types][TypeDescriptor].
		 * @return
		 *   An Avail type.
		 */
		fun resolvePojoType(type: Type, typeVars: A_Map): A_Type
		{
			// If type is a Java class or interface, then answer a pojo type.
			if (type is Class<*>)
			{
				// If type represents java.lang.Object, then answer any.
				if (type == Any::class.java)
				{
					return Types.ANY.o
				}
				// If type represents a Java primitive, then unmarshal it.
				if (type.isPrimitive)
				{
					// If type represents Java void, then answer top.
					return when (type)
					{
						Void.TYPE -> Types.TOP.o
						java.lang.Boolean::class.javaPrimitiveType ->
							booleanType
						java.lang.Byte::class.javaPrimitiveType -> byteRange()
						java.lang.Short::class.javaPrimitiveType -> shortRange()
						java.lang.Integer::class.javaPrimitiveType -> intRange()
						java.lang.Long::class.javaPrimitiveType -> longRange()
						java.lang.Float::class.javaPrimitiveType ->
							Types.FLOAT.o
						java.lang.Double::class.javaPrimitiveType ->
							Types.DOUBLE.o
						java.lang.Character::class.javaPrimitiveType ->
							charRange()
						else ->
						{
							assert(false) {
								"There are only nine primitive types!"
							}
							throw RuntimeException()
						}
					}
				}
				when (type)
				{
					Void::class.java -> return Types.TOP.o
					java.lang.Boolean::class.java -> return booleanType
					java.lang.Byte::class.java -> return byteRange()
					java.lang.Short::class.java -> return shortRange()
					java.lang.Integer::class.java -> return intRange()
					java.lang.Long::class.java -> return longRange()
					java.lang.Float::class.java -> return Types.FLOAT.o
					java.lang.Double::class.java -> return Types.DOUBLE.o
					java.lang.Character::class.java -> return charRange()
					String::class.java -> return TupleTypeDescriptor.stringType
					else ->
					{
						return if (type == BigInteger::class.java)
						{
							IntegerRangeTypeDescriptor.integers
						}
						else
						{
							pojoTypeForClass(type)
						}
					}
				}
			}
			// If type is a type variable, then resolve it using the map of type
			// variables.
			if (type is TypeVariable<*>)
			{
				// class Foo<X> { ... }
				val javaClass = when (val decl = type.genericDeclaration)
					{
						is Class<*> -> decl
						is Constructor<*> -> decl.declaringClass
						is Method -> decl.declaringClass
						else ->
						{
							assert(false) {
								"There should only be three contexts that " +
									"can define a type variable!"
							}
							throw RuntimeException()
						}
					}
				val name = stringFrom("${javaClass.name}.${type.name}")
				// If the type variable is bound, answer the binding.
				typeVars.mapAtOrNull(name)?.let { return it }
				// The type variable was unbound, so compute the upper bound.
				return type.bounds.fold(bottom) { union, bound ->
					union.typeIntersection(resolvePojoType(bound, typeVars))
				}
			}
			// If type is a parameterized type, then recursively resolve it
			// using the map of type variables.
			if (type is ParameterizedType)
			{
				val unresolved = type.actualTypeArguments
				val resolved = mutableListOf<A_Type>()
				for (anUnresolved in unresolved)
				{
					resolved.add(resolvePojoType(anUnresolved, typeVars))
				}
				return pojoTypeForClassWithTypeArguments(
					type.rawType as Class<*>,
					tupleFromList(resolved))
			}
			assert(false) { "Unsupported generic declaration" }
			throw RuntimeException()
		}

		/**
		 * Answer the canonical pojo type for the specified pojo type.
		 * This marshals certain pojo types to Avail types (e.g.,
		 * java.lang.String -> string).
		 *
		 * @param probablePojoType
		 *   An arbitrary Avail type, but one that might be a pojo type.
		 * @param allowMetas
		 *   `true` if metatypes are contextually possible outcomes, `false` if
		 *   only nontype values are contextually possible outcomes.
		 * @return
		 *   The canonical Avail type for the given pojo type.
		 */
		fun canonicalPojoType(
			probablePojoType: A_Type,
			allowMetas: Boolean): A_Type
		{
			if (probablePojoType.isPojoType
				&& !probablePojoType.equalsPojoBottomType())
			{
				val pojoClass = probablePojoType.javaClass()
				if (pojoClass.notNil)
				{
					val javaClass = pojoClass.javaObjectNotNull<Class<*>>()
					if (javaClass.typeParameters.isEmpty())
					{
						val resolved = resolvePojoType(javaClass, emptyMap)
						return if (!allowMetas && resolved.equals(
								Types.ANY.o))
						{
							Types.NONTYPE.o
						}
						else resolved
					}
				}
			}
			return probablePojoType
		}

		/**
		 * In the context of a reference
		 * [Java&#32;class&#32;or&#32;interface][Class] implicitly specified by
		 * the [type&#32;variable&#32;map][TypeVariableMap] and
		 * [tuple&#32;of&#32;type&#32;arguments][TupleDescriptor], compute the
		 * type arguments of the specified target Java class or interface.
		 *
		 * @param target
		 *   A Java class or interface (encountered during processing of the
		 *   reference type's ancestry).
		 * @param vars
		 *   The reference type's type variable map. Indices are the order in
		 *   which new type variables are introduced in [target].
		 * @param typeArgs
		 *   The reference type's type arguments.
		 * @param canon
		 *   The current [canon][Canon], used to identify recursive type
		 *   dependency.
		 * @return
		 *   The type arguments of the target.
		 */
		private fun computeTypeArgumentsOf(
			target: ParameterizedType,
			vars: TypeVariableMap,
			typeArgs: A_Tuple,
			canon: Canon): A_Tuple
		{
			val args = target.actualTypeArguments
			val propagation = mutableListOf<A_Type>()
			for (arg in args)
			{
				// class Target<...> extends Supertype<Arg> { ... }
				//
				// If the type argument is an unparameterized class or
				// interface, then add its pojo type to the supertype's tuple of
				// type arguments.
				when (arg)
				{
					is Class<*> ->
					{
						val typeArg: A_Type =
							if (canon.containsKey(arg))
							{
								selfTypeForClass(arg)
							}
							else
							{
								pojoTypeForClass(arg)
							}
						propagation.add(typeArg)
					}
					is TypeVariable<*> ->
					{
						val index = vars[arg.name]!!
						propagation.add(typeArgs.tupleAt(index + 1))
					}
					is ParameterizedType ->
					{
						val localArgs = computeTypeArgumentsOf(
							arg,
							vars,
							typeArgs,
							canon)
						propagation.add(pojoTypeForClassWithTypeArguments(
							arg.rawType as Class<*>, localArgs))
					}
					else ->
					{
						assert(false) { "Unsupported generic declaration" }
						throw RuntimeException()
					}
				}
			}
			return tupleFromList(propagation)
		}

		/**
		 * Given the type parameterization of the [target&#32;Java][Class], use
		 * type propagation to determine the type parameterization of the
		 * specified direct [supertype][Type].
		 *
		 * @param target
		 *   A Java class or interface.
		 * @param supertype
		 *   A parameterized direct supertype of the target.
		 * @param typeArgs
		 *   The type parameters of the target. These may be any
		 *   [Avail&#32;types][TypeDescriptor], not just pojo types.
		 * @param canon
		 *   The current [canon][Canon], used to identify recursive type
		 *   dependency.
		 * @return
		 *   The type parameters of the specified supertype.
		 */
		private fun computeSupertypeParameters(
			target: Class<*>,
			supertype: Type,
			typeArgs: A_Tuple,
			canon: Canon): A_Tuple
		{
			// class Target<...> extends GenericSupertype { ... }
			//
			// If the supertype is an unparameterized class or interface, then
			// answer an empty type parameterization tuple.
			return when (supertype)
			{
				is Class<*> -> emptyTuple
				is ParameterizedType ->
					computeTypeArgumentsOf(
						supertype,
						TypeVariableMap(target),
						typeArgs,
						canon)
				else ->
				{
					assert(false) { "Unsupported generic declaration" }
					throw RuntimeException()
				}
			}
		}

		/**
		 * Recursively compute the complete ancestry (of Java types) of the
		 * specified [Java&#32;class&#32;or&#32;interface][Class].
		 *
		 * @param target
		 *   A Java class or interface.
		 * @param typeArgs
		 *   The type arguments. These may be any
		 *   [Avail&#32;types][TypeDescriptor], not just pojo types.
		 * @param ancestry
		 *   The working partial [ancestry][MapDescriptor].
		 * @param canon
		 *   The current [canon][Canon], used to deduplicate the collection of
		 *   ancestors.
		 */
		private fun computeAncestry(
			target: Class<*>,
			typeArgs: A_Tuple,
			ancestry: Mutable<A_Map>,
			canon: Canon)
		{
			val javaClass = canon.canonize(target)
			ancestry.update {
				mapAtPuttingCanDestroy(javaClass, typeArgs, true)
			}
			// Recursively accumulate the class ancestry.
			val superclass = target.superclass
			if (superclass !== null)
			{
				if (!canon.containsKey(superclass))
				{
					val supertypeParams = computeSupertypeParameters(
						target,
						target.genericSuperclass,
						typeArgs,
						canon)
					computeAncestry(
						superclass,
						supertypeParams,
						ancestry,
						canon)
				}
			}
			// Recursively accumulate the interface ancestry.
			val superinterfaces = target.interfaces
			val genericSuperinterfaces = target.genericInterfaces
			for (i in superinterfaces.indices)
			{
				if (!canon.containsKey(superinterfaces[i]))
				{
					val supertypeParams = computeSupertypeParameters(
						target,
						genericSuperinterfaces[i],
						typeArgs,
						canon)
					computeAncestry(
						superinterfaces[i],
						supertypeParams,
						ancestry,
						canon)
				}
			}
		}

		/**
		 * Create a [pojo type][PojoTypeDescriptor] from the specified
		 * [Java&#32;class][Class] and type arguments.
		 *
		 * @param target
		 *   A Java class or interface.
		 * @param typeArgs
		 *   The type arguments. These may be any
		 *   [Avail&#32;types][TypeDescriptor], not just pojo types.
		 * @return
		 *   The requested pojo type.
		 */
		fun pojoTypeForClassWithTypeArguments(
			target: Class<*>,
			typeArgs: A_Tuple): AvailObject
		{
			return cache[LRUCacheKey(target, typeArgs)]
		}

		/**
		 * Create a [pojo&#32;type][PojoTypeDescriptor] for the specified
		 * [Java&#32;class][Class].
		 *
		 * @param target
		 *   A Java class or interface.
		 * @return
		 *   The requested pojo type.
		 */
		fun pojoTypeForClass(target: Class<*>): AvailObject
		{
			if (target.isArray)
			{
				return arrayPojoType(
					resolvePojoType(target.componentType!!, emptyMap),
					nonnegativeInt32)
			}
			return pojoTypeForClassWithTypeArguments(
				target,
				createRepeatedElementTuple(
					target.typeParameters.size, Types.ANY.o))
		}

		/**
		 * Create a [pojo&#32;type][PojoTypeDescriptor] that represents an array
		 * of the specified [element&#32;type][TypeDescriptor].
		 *
		 * @param elementType
		 *   The element type. This may be any Avail type, not just a pojo type.
		 * @param sizeRange
		 *   An [integer range][IntegerRangeTypeDescriptor] that specifies all
		 *   allowed array sizes for instances of this type. This must be a
		 *   subtype of
		 *   [whole&#32;number][IntegerRangeTypeDescriptor.wholeNumbers].
		 * @return
		 *   The requested pojo type.
		 */
		fun pojoArrayType(elementType: A_Type, sizeRange: A_Type): AvailObject
		{
			assert(sizeRange.isSubtypeOf(wholeNumbers))
			return arrayPojoType(elementType, sizeRange)
		}

		/**
		 * Create a [fused&#32;pojo&#32;type][FusedPojoTypeDescriptor] based on
		 * the given complete parameterization map.  Each ancestor class and
		 * interface occurs as a key, with that class or interface's parameter
		 * tuple as the value.
		 *
		 * @param ancestorMap
		 *   A map from [equality-wrapped][RawPojoDescriptor.equalityPojo]
		 *   [raw&#32;pojos][RawPojoDescriptor] to their tuples of type
		 *   parameters.
		 * @return
		 *   A fused pojo type.
		 */
		fun fusedTypeFromAncestorMap(ancestorMap: A_Map): AvailObject
		{
			assert(ancestorMap.isMap)
			return createFusedPojoType(ancestorMap)
		}

		/**
		 * Recursively compute the complete ancestry (of Java types) of the
		 * specified [Java&#32;class&#32;or&#32;interface][Class]. Ignore type
		 * parameters.
		 *
		 * @param target
		 *   A Java class or interface.
		 * @param ancestors
		 *   The [set][Set] of ancestors.
		 * @param canon
		 *   The current [canon][Canon], used to deduplicate the collection of
		 *   ancestors.
		 */
		private fun computeUnparameterizedAncestry(
			target: Class<*>,
			ancestors: MutableSet<AvailObject>,
			canon: Canon)
		{
			ancestors.add(canon.canonize(target))
			val superclass = target.superclass
			superclass?.let { computeUnparameterizedAncestry(it, ancestors, canon) }
			for (superinterface in target.interfaces)
			{
				computeUnparameterizedAncestry(superinterface, ancestors, canon)
			}
		}

		/**
		 * Create a [pojo&#32;self&#32;type][PojoTypeDescriptor] for the
		 * specified [Java&#32;class][Class].
		 *
		 * @param target
		 *   A Java class or interface. This element should define no type
		 *   parameters.
		 * @return
		 *   The requested pojo type.
		 */
		fun selfTypeForClass(target: Class<*>): AvailObject
		{
			val canon = Canon()
			val ancestors = mutableSetOf<AvailObject>()
			ancestors.add(canon[Any::class.java]!!)
			computeUnparameterizedAncestry(target, ancestors, canon)
			return SelfPojoTypeDescriptor.newSelfPojoType(
				canon[target]!!, setFromCollection(ancestors))
		}
	}
}
