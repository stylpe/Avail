/*
 * AtomDescriptor.kt
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
package avail.descriptor.atoms

import avail.AvailRuntimeSupport
import avail.annotations.HideFieldInDebugger
import avail.compiler.ParserState
import avail.descriptor.atoms.A_Atom.Companion.atomName
import avail.descriptor.atoms.A_Atom.Companion.bundleOrCreate
import avail.descriptor.atoms.A_Atom.Companion.isAtomSpecial
import avail.descriptor.atoms.A_Atom.Companion.setAtomBundle
import avail.descriptor.atoms.A_Atom.Companion.setAtomProperty
import avail.descriptor.atoms.AtomDescriptor.Companion.falseObject
import avail.descriptor.atoms.AtomDescriptor.Companion.trueObject
import avail.descriptor.atoms.AtomDescriptor.IntegerSlots.Companion.HASH_OR_ZERO
import avail.descriptor.atoms.AtomDescriptor.IntegerSlots.HASH_AND_MORE
import avail.descriptor.atoms.AtomDescriptor.ObjectSlots.ISSUING_MODULE
import avail.descriptor.atoms.AtomDescriptor.ObjectSlots.NAME
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom.FALSE
import avail.descriptor.atoms.AtomDescriptor.SpecialAtom.TRUE
import avail.descriptor.atoms.AtomWithPropertiesSharedDescriptor.Companion.sharedForFalse
import avail.descriptor.atoms.AtomWithPropertiesSharedDescriptor.Companion.sharedForTrue
import avail.descriptor.bundles.A_Bundle
import avail.descriptor.fiber.A_Fiber
import avail.descriptor.fiber.A_Fiber.Companion.heritableFiberGlobals
import avail.descriptor.module.A_Module
import avail.descriptor.module.A_Module.Companion.moduleNameNative
import avail.descriptor.objects.ObjectTypeDescriptor
import avail.descriptor.representation.A_BasicObject
import avail.descriptor.representation.AbstractSlotsEnum
import avail.descriptor.representation.AvailObject
import avail.descriptor.representation.BitField
import avail.descriptor.representation.Descriptor
import avail.descriptor.representation.IntegerSlotsEnum
import avail.descriptor.representation.Mutability
import avail.descriptor.representation.NilDescriptor.Companion.nil
import avail.descriptor.representation.ObjectSlotsEnum
import avail.descriptor.sets.SetDescriptor
import avail.descriptor.tuples.A_String
import avail.descriptor.tuples.StringDescriptor.Companion.stringFrom
import avail.descriptor.types.A_Type
import avail.descriptor.types.A_Type.Companion.isSupertypeOfPrimitiveTypeEnum
import avail.descriptor.types.AbstractEnumerationTypeDescriptor
import avail.descriptor.types.PrimitiveTypeDescriptor.Types
import avail.descriptor.types.TypeTag
import avail.exceptions.MalformedMessageException
import avail.io.IOSystem.FileHandle
import avail.serialization.Serializer
import avail.serialization.SerializerOperation
import avail.utility.ifZero
import org.availlang.json.JSONWriter
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.IdentityHashMap
import java.util.regex.Pattern

/**
 * An `atom` is an object that has identity by fiat, i.e., it is distinguished
 * from all other objects by the fact of its creation event and the history of
 * what happens to its references.  Not all objects in Avail have that property
 * (hence the acronym Advanced Value And Identity Language), unlike most
 * object-oriented programming languages.
 *
 * When an atom is created, a [string][A_String] is supplied to act as the
 * atom's name. This name does not have to be unique among atoms, and is simply
 * used to describe the atom textually.
 *
 * Atoms fill the role of enumerations commonly found in other languages.
 * They're not the only things that can fill that role, but they're a simple way
 * to do so.  In particular, [enumerations][AbstractEnumerationTypeDescriptor]
 * and multiply polymorphic method dispatch provide a phenomenally powerful
 * technique when combined with atoms.  A collection of atoms, say named `red`,
 * `green`, and `blue`, are added to a [set][SetDescriptor] from which an
 * enumeration is then constructed. Such a type has exactly three instances: the
 * three atoms.  Unlike the vast majority of languages that support
 * enumerations, Avail allows one to define another enumeration containing the
 * same three values plus `yellow`, `cyan`, and `magenta`.  `red` is a member of
 * both enumerations, for example.
 *
 * Booleans are implemented with exactly this technique, with an atom
 * representing `true` and another representing `false`. The boolean type itself
 * is merely an enumeration of these two values.  The only thing special about
 * booleans is that they are referenced by the Avail virtual machine.  In fact,
 * this very class, `AtomDescriptor`, contains these references in [trueObject]
 * and [falseObject].
 *
 * @constructor
 *
 * @param mutability
 *   The [mutability][Mutability] of the new descriptor.
 * @param typeTag
 *   The [TypeTag] to embed in the new descriptor.
 * @param objectSlotsEnumClass
 *   The Java [Class] which is a subclass of [ObjectSlotsEnum] and defines this
 *   object's object slots layout, or null if there are no object slots.
 * @param integerSlotsEnumClass
 *   The Java [Class] which is a subclass of [IntegerSlotsEnum] and defines this
 *   object's object slots layout, or null if there are no integer slots.
 *
 * @author Mark van Gulik &lt;mark@availlang.org&gt;
 * @author Todd L Smith &lt;todd@availlang.org&gt;
 * @see AtomWithPropertiesDescriptor
 * @see AtomWithPropertiesSharedDescriptor
 */
open class AtomDescriptor protected constructor (
	mutability: Mutability,
	typeTag: TypeTag,
	objectSlotsEnumClass: Class<out ObjectSlotsEnum>,
	integerSlotsEnumClass: Class<out IntegerSlotsEnum>
) : Descriptor(mutability, typeTag, objectSlotsEnumClass, integerSlotsEnumClass)
{
	/**
	 * The layout of integer slots for my instances.
	 */
	enum class IntegerSlots : IntegerSlotsEnum {
		/**
		 * The low 32 bits are used for the [HASH_OR_ZERO], but the upper
		 * 32 can be used by other [BitField]s in subclasses.
		 */
		@HideFieldInDebugger
		HASH_AND_MORE;

		companion object {
			/**
			 * A slot to hold the hash value, or zero if it has not been
			 * computed. The hash of an atom is a random number, computed once.
			 */
			val HASH_OR_ZERO = BitField(HASH_AND_MORE, 0, 32) { null }
		}
	}

	/**
	 * The layout of object slots for my instances.
	 */
	enum class ObjectSlots : ObjectSlotsEnum {
		/**
		 * A string (non-uniquely) roughly identifying this atom.  It need not
		 * be unique among atoms.
		 */
		NAME,

		/**
		 * The [module][A_Module] that was active when this atom was issued.
		 * This information is crucial to [serialization][Serializer].
		 */
		ISSUING_MODULE
	}

	override fun allowsImmutableToMutableReferenceInField (
		e: AbstractSlotsEnum
	) = e === HASH_AND_MORE

	override fun printObjectOnAvoidingIndent (
		self: AvailObject,
		builder: StringBuilder,
		recursionMap: IdentityHashMap<A_BasicObject, Void>,
		indent: Int
	) = with(builder) {
		val nativeName = self.atomName.asNativeString()
		// Some atoms print nicer than others.
		when {
			self.isAtomSpecial -> {
				append(nativeName)
				return
			}
			wordPattern.matcher(nativeName).matches() ->
				append("\$$nativeName")
			else -> append("\$\"$nativeName\"")
		}
		val issuer: A_Module = self.slot(ISSUING_MODULE)
		if (issuer.notNil) {
			val issuerName = issuer.moduleNameNative
			val localIssuer =
				issuerName.substring(issuerName.lastIndexOf('/') + 1)
			append(" (from $localIssuer)")
		}
	}

	override fun o_AtomName(self: AvailObject): A_String = self.slot(NAME)

	@Throws(MalformedMessageException::class)
	override fun o_BundleOrCreate (self: AvailObject): A_Bundle
	{
		// An un-shared atom cannot have a bundle, so make it shared first.
		return self.makeShared().bundleOrCreate()
	}

	override fun o_BundleOrNil (self: AvailObject): A_Bundle = nil

	override fun o_Equals (
		self: AvailObject,
		another: A_BasicObject
	) = another.traversed().sameAddressAs(self)

	/**
	 * This atom has no properties, so always answer [nil].
	 */
	override fun o_GetAtomProperty (self: AvailObject, key: A_Atom) = nil

	override fun o_Hash (self: AvailObject): Int =
		self.slot(HASH_OR_ZERO).ifZero {
			// The shared subclass overrides to use synchronization.
			AvailRuntimeSupport.nextNonzeroHash().also { hash ->
				self.setSlot(HASH_OR_ZERO, hash)
			}
		}

	override fun o_IsAtom (self: AvailObject) = true

	override fun o_IsAtomSpecial(self: AvailObject) = false

	override fun o_IsInstanceOfKind (
		self: AvailObject,
		aType: A_Type
	) = aType.isSupertypeOfPrimitiveTypeEnum(Types.ATOM)

	override fun o_IssuingModule (self: AvailObject): A_Module =
		self.slot(ISSUING_MODULE)

	override fun o_Kind(self: AvailObject): AvailObject = Types.ATOM.o

	/**
	 * Convert to use an [AtomWithPropertiesSharedDescriptor], replacing self
	 * with an indirection.
	 */
	override fun o_MakeShared (self: AvailObject): AvailObject
	{
		assert(!isShared) // shared subclass should override.
		val substituteAtom: AvailObject =
			AtomWithPropertiesSharedDescriptor.shared.createInitialized(
				self.slot(NAME),
				self.slot(ISSUING_MODULE),
				nil,  // subclass with properties should override.
				self.slot(HASH_OR_ZERO))
		self.becomeIndirectionTo(substituteAtom)
		return substituteAtom
	}

	override fun o_SetAtomBundle(self: AvailObject, bundle: A_Bundle) =
		self.makeShared().setAtomBundle(bundle)

	/**
	 * Convert myself to an equivalent
	 * [atom&#32;with&#32;properties][AtomWithPropertiesDescriptor], then add
	 * the property to it.
	 */
	override fun o_SetAtomProperty (
		self: AvailObject,
		key: A_Atom,
		value: A_BasicObject
	) {
		assert(!isShared)
		val substituteAtom: AvailObject =
			AtomWithPropertiesDescriptor.createWithProperties(
				self.slot(NAME),
				self.slot(ISSUING_MODULE),
				self.slot(HASH_OR_ZERO))
		self.becomeIndirectionTo(substituteAtom)
		substituteAtom.setAtomProperty(key, value)
	}

	override fun o_SerializerOperation (self: AvailObject) =
		SerializerOperation.ATOM

	override fun o_WriteTo (self: AvailObject, writer: JSONWriter) =
		writer.writeObject {
			at("kind") { write("atom") }
			at("atom name") { self.slot(NAME).writeTo(writer) }
			val module = self.slot(ISSUING_MODULE)
			if (module.notNil) {
				at("issuing module") { module.writeSummaryTo(writer) }
			}
		}

	override fun mutable () = mutable

	override fun immutable () = immutable

	@Deprecated(
		"Shared atoms are implemented in subclasses",
		level = DeprecationLevel.HIDDEN)
	override fun shared () = unsupported

	/**
	 * `SpecialAtom` enumerates [atoms][A_Atom] that are known to the virtual
	 * machine.
	 *
	 * @constructor
	 *
	 * Create a `SpecialAtom` to hold the given already constructed [A_Atom].
	 *
	 * @param atom
	 *   The actual [A_Atom] to be held by this [SpecialAtom].
	 */
	enum class SpecialAtom
	constructor (val atom: A_Atom)
	{
		/** The atom representing the Avail concept "true". */
		TRUE(
			sharedForTrue.createInitialized(
				stringFrom("true").makeShared(), nil, nil, 0)),

		/** The atom representing the Avail concept "false". */
		FALSE(
			sharedForFalse.createInitialized(
				stringFrom("false").makeShared(), nil, nil, 0)),

		/**
		 * The atom used as a property key to name
		 * [object&#32;types][ObjectTypeDescriptor].  This property occurs
		 * within each atom which occurs as a field type key of the object type.
		 * The value is a map from object type to the set of names of that exact
		 * type (typically just one).  The naming information is set up via
		 * [ObjectTypeDescriptor.setNameForType], and removed by
		 * [ObjectTypeDescriptor.removeNameFromType].
		 */
		OBJECT_TYPE_NAME_PROPERTY_KEY("object names"),

		/**
		 * The atom used as a key in a [ParserState]'s
		 * [ParserState.clientDataMap] to store the current map of declarations
		 * that are in scope.
		 */
		COMPILER_SCOPE_MAP_KEY("Compilation scope"),

		/**
		 * The atom used as a key in a [ParserState]'s
		 * [ParserState.clientDataMap] to store a tuple of maps to restore as
		 * the blocks that are being parsed are completed.
		 */
		COMPILER_SCOPE_STACK_KEY("Compilation scope stack"),

		/**
		 * The atom used as a key in a [ParserState]'s
		 * [ParserState.clientDataMap] to accumulate the tuple of tokens that
		 * have been parsed so far for the current method/macro site.
		 */
		ALL_TOKENS_KEY("All tokens"),

		/**
		 * The atom used as a key in a [ParserState]'s
		 * [ParserState.clientDataMap] to accumulate the tuple of tokens that
		 * have been parsed so far for the current method/macro site and are
		 * mentioned by name in the method name.
		 */
		STATIC_TOKENS_KEY("Static tokens"),

		/**
		 * The atom used to identify the entry in a [ParserState]'s
		 * [ParserState.clientDataMap] containing the bundle of the macro send
		 * for which the current fiber is computing a replacement phrase.
		 */
		MACRO_BUNDLE_KEY("Macro bundle"),

		/**
		 * The atom used as a key in a [fiber's][A_Fiber] global map to
		 * extract the current [ParserState]'s
		 * [ParserState.clientDataMap].
		 */
		CLIENT_DATA_GLOBAL_KEY("Compiler client data"),

		/**
		 * The atom used as a property key under which to store a [FileHandle].
		 */
		FILE_KEY("file key"),

		/**
		 * The atom used as a property key under which to store an
		 * [AsynchronousServerSocketChannel].
		 */
		SERVER_SOCKET_KEY("server socket key"),

		/**
		 * The atom used as a property key under which to store an
		 * [AsynchronousSocketChannel].
		 */
		SOCKET_KEY("socket key"),

		/**
		 * The property key that indicates that a [fiber][A_Fiber]
		 * global is inheritable by its forked fibers.
		 */
		HERITABLE_KEY("heritability"),

		/**
		 * The property key whose presence indicates an atom is for explicit
		 * subclassing of [object&#32;types][ObjectTypeDescriptor].
		 */
		EXPLICIT_SUBCLASSING_KEY("explicit subclassing"),

		/**
		 * A heritable atom (has [HERITABLE_KEY] -> [trueObject] as a property)
		 * which, when present in as [A_Fiber]'s [heritableFiberGlobals],
		 * indicates that the fiber should not be subject to debugging.  This is
		 * a way to mark fibers launched by the debugger itself, or forked by
		 * such a fiber, say for stringification.
		 *
		 * Note that a fibers aren't currently (2022.05.02) serializable, and if
		 * they were, we still wouldn't want to serialize one launched from a
		 * debugger, so this atom itself doesn't need to be serializable.
		 */
		DONT_DEBUG_KEY("don't debug");

		/**
		 * Create a `SpecialAtom` to hold a new atom constructed with the given
		 * name.
		 *
		 * @param name The name of the atom to be created.
		 */
		constructor (name: String) : this(createSpecialAtom(name))

		companion object
		{
			init
			{
				DONT_DEBUG_KEY.atom.setAtomProperty(
					HERITABLE_KEY.atom, trueObject)
			}
		}
	}

	companion object
	{
		/** The mutable [AtomDescriptor]. */
		private val mutable = AtomDescriptor(
			Mutability.MUTABLE,
			TypeTag.ATOM_TAG,
			ObjectSlots::class.java,
			IntegerSlots::class.java)

		/** The immutable [AtomDescriptor]. */
		private val immutable = AtomDescriptor(
			Mutability.IMMUTABLE,
			TypeTag.ATOM_TAG,
			ObjectSlots::class.java,
			IntegerSlots::class.java)

		/** A [Pattern] of one or more word characters. */
		private val wordPattern = Pattern.compile("\\w(\\w|\\d|_)*")

		/**
		 * Create a new atom with the given name. The name is not globally
		 * unique, but serves to help to visually distinguish atoms.
		 *
		 * @param name
		 *   An [A_String] used to help identify the new atom.
		 * @param issuingModule
		 *   Which [A_Module] was active when the atom was created.
		 * @return
		 *   The new atom, not equal to any object in use before this method was
		 *   invoked.
		 */
		fun createAtom (
			name: A_String,
			issuingModule: A_Module
		) = mutable.createImmutable {
			setSlot(NAME, name.makeShared())
			setSlot(ISSUING_MODULE, issuingModule)
			setSlot(HASH_OR_ZERO, 0)
		}

		/**
		 * Create a new special atom with the given name. The name is not
		 * globally unique, but serves to help to visually distinguish atoms. A
		 * special atom should not have properties added to it after
		 * initialization.
		 *
		 * @param name
		 *   A [String] used to help identify the new atom.
		 * @return
		 *   The new atom, not equal to any object in use before this method was
		 *   invoked.
		 */
		fun createSpecialAtom (
			name: String
		) = AtomWithPropertiesSharedDescriptor.sharedSpecial.createInitialized(
			stringFrom(name), nil, nil, 0)

		/** The atom representing the Avail concept "true". */
		val trueObject get () = TRUE.atom

		/** The atom representing the Avail concept "false". */
		val falseObject get () = FALSE.atom

		/**
		 * Convert a Kotlin [Boolean] into an Avail boolean.  There are exactly
		 * two Avail booleans, which are just ordinary atoms, [trueObject] and
		 * [falseObject], which are known by the Avail virtual machine.
		 *
		 * @param aBoolean
		 *   A Kotlin [Boolean]]
		 * @return
		 *   An Avail boolean.
		 */
		fun objectFromBoolean (aBoolean: Boolean): A_Atom =
			if (aBoolean) trueObject else falseObject
	}
}
