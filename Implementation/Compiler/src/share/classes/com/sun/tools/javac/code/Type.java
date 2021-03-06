/*
 * Copyright 1999-2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package com.sun.tools.javac.code;

import static com.sun.tools.javac.code.BoundKind.EXTENDS;
import static com.sun.tools.javac.code.BoundKind.SUPER;
import static com.sun.tools.javac.code.BoundKind.UNBOUND;
import static com.sun.tools.javac.code.Flags.ACYCLIC;
import static com.sun.tools.javac.code.Flags.COMPOUND;
import static com.sun.tools.javac.code.Flags.INTERFACE;
import static com.sun.tools.javac.code.Flags.PUBLIC;
import static com.sun.tools.javac.code.Flags.STATIC;
import static com.sun.tools.javac.code.Kinds.ERR;
import static com.sun.tools.javac.code.Kinds.TYP;
import static com.sun.tools.javac.code.TypeTags.ARRAY;
import static com.sun.tools.javac.code.TypeTags.BOOLEAN;
import static com.sun.tools.javac.code.TypeTags.BOT;
import static com.sun.tools.javac.code.TypeTags.BYTE;
import static com.sun.tools.javac.code.TypeTags.CHAR;
import static com.sun.tools.javac.code.TypeTags.CLASS;
import static com.sun.tools.javac.code.TypeTags.DOUBLE;
import static com.sun.tools.javac.code.TypeTags.ERROR;
import static com.sun.tools.javac.code.TypeTags.FLOAT;
import static com.sun.tools.javac.code.TypeTags.FORALL;
import static com.sun.tools.javac.code.TypeTags.INT;
import static com.sun.tools.javac.code.TypeTags.LONG;
import static com.sun.tools.javac.code.TypeTags.METHOD;
import static com.sun.tools.javac.code.TypeTags.NONE;
import static com.sun.tools.javac.code.TypeTags.PACKAGE;
import static com.sun.tools.javac.code.TypeTags.SHORT;
import static com.sun.tools.javac.code.TypeTags.TYPEVAR;
import static com.sun.tools.javac.code.TypeTags.UNDETVAR;
import static com.sun.tools.javac.code.TypeTags.VOID;
import static com.sun.tools.javac.code.TypeTags.WILDCARD;

import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;

import com.sun.tools.javac.code.Effect.VariableEffect;
import com.sun.tools.javac.code.RPLElement.RPLParameterElement;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.code.Symbol.RegionParameterSymbol;
import com.sun.tools.javac.code.Symbol.TypeSymbol;
import com.sun.tools.javac.code.Symbol.VarSymbol;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Name;

/** This class represents Java types. The class itself defines the behavior of
 *  the following types:
 *  <pre>
 *  base types (tags: BYTE, CHAR, SHORT, INT, LONG, FLOAT, DOUBLE, BOOLEAN),
 *  type `void' (tag: VOID),
 *  the bottom type (tag: BOT),
 *  the missing type (tag: NONE).
 *  </pre>
 *  <p>The behavior of the following types is defined in subclasses, which are
 *  all static inner classes of this class:
 *  <pre>
 *  class types (tag: CLASS, class: ClassType),
 *  array types (tag: ARRAY, class: ArrayType),
 *  method types (tag: METHOD, class: MethodType),
 *  package types (tag: PACKAGE, class: PackageType),
 *  type variables (tag: TYPEVAR, class: TypeVar),
 *  type arguments (tag: WILDCARD, class: WildcardType),
 *  polymorphic types (tag: FORALL, class: ForAll),
 *  the error type (tag: ERROR, class: ErrorType).
 *  </pre>
 *
 *  <p><b>This is NOT part of any API supported by Sun Microsystems.  If
 *  you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 *
 *  @see TypeTags
 */
public class Type implements PrimitiveType {

    /** Constant type: no type at all. */
    public static final JCNoType noType = new JCNoType(NONE);

    /** If this switch is turned on, the names of type variables
     *  and anonymous classes are printed with hashcodes appended.
     */
    public static boolean moreInfo = false;

    /** The tag of this type.
     *
     *  @see TypeTags
     */
    public int tag;

    /** The defining class / interface / package / type variable
     */
    public TypeSymbol tsym;
    
    /** Is this an erased type? (DPJ)
     */
    public boolean DPJerased = false;
    
    /** Get and set the cell type, if any
     */
    public Type getCellType() { return null; }
    public void setCellType(Type t) {}

    /**
     * The constant value of this type, null if this type does not
     * have a constant value attribute. Only primitive types and
     * strings (ClassType) can have a constant value attribute.
     * @return the constant value attribute of this type
     */
    public Object constValue() {
        return null;
    }

    public <R,S> R accept(Type.Visitor<R,S> v, S s) { return v.visitType(this, s); }

    /** Define a type given its tag and type symbol
     */
    public Type(int tag, TypeSymbol tsym) {
        this.tag = tag;
        this.tsym = tsym;
    }

    /** An abstract class for mappings from types to types
     */
    public static abstract class Mapping {
        private String name;
        public Mapping(String name) {
            this.name = name;
        }
        public abstract Type apply(Type t);
        public String toString() {
            return name;
        }
    }

    /** map a type function over all immediate descendants of this type
     */
    public Type map(Mapping f) {
        return this;
    }

    /** map a type function over a list of types
     */
    public static List<Type> map(List<Type> ts, Mapping f) {
        if (ts.nonEmpty()) {
            List<Type> tail1 = map(ts.tail, f);
            Type t = f.apply(ts.head);
            if (tail1 != ts.tail || t != ts.head)
                return tail1.prepend(t);
        }
        return ts;
    }

    /** Define a constant type, of the same kind as this type
     *  and with given constant value
     */
    public Type constType(Object constValue) {
        final Object value = constValue;
        assert tag <= BOOLEAN;
        return new Type(tag, tsym) {
                @Override
                public Object constValue() {
                    return value;
                }
                @Override
                public Type baseType() {
                    return tsym.type;
                }
            };
    }

    /**
     * If this is a constant type, return its underlying type.
     * Otherwise, return the type itself.
     */
    public Type baseType() {
        return this;
    }

    /** Return the base types of a list of types.
     */
    public static List<Type> baseTypes(List<Type> ts) {
        if (ts.nonEmpty()) {
            Type t = ts.head.baseType();
            List<Type> baseTypes = baseTypes(ts.tail);
            if (t != ts.head || baseTypes != ts.tail)
                return baseTypes.prepend(t);
        }
        return ts;
    }

    /** The Java source which this type represents.
     */
    public String toString() {
        String s = (tsym == null || tsym.name == null)
            ? "<none>"
            : tsym.name.toString();
        if (moreInfo && tag == TYPEVAR) s = s + hashCode();
        return s;
    }

    /**
     * The Java source which this type list represents.  A List is
     * represented as a comma-separated listing of the elements in
     * that list.
     */
    public static String toString(List<Type> ts) {
        if (ts.isEmpty()) {
            return "";
        } else {
            StringBuffer buf = new StringBuffer();
            buf.append(ts.head.toString());
            for (List<Type> l = ts.tail; l.nonEmpty(); l = l.tail)
                buf.append(",").append(l.head.toString());
            return buf.toString();
        }
    }

    /**
     * The constant value of this type, converted to String
     */
    public String stringValue() {
        assert constValue() != null;
        if (tag == BOOLEAN)
            return ((Integer) constValue()).intValue() == 0 ? "false" : "true";
        else if (tag == CHAR)
            return String.valueOf((char) ((Integer) constValue()).intValue());
        else
            return constValue().toString();
    }

    /**
     * This method is analogous to isSameType, but weaker, since we
     * never complete classes. Where isSameType would complete a
     * class, equals assumes that the two types are different.
     */
    public boolean equals(Object t) {
        return super.equals(t);
    }

    public int hashCode() {
        return super.hashCode();
    }

    /** Is this a constant type whose value is false?
     */
    public boolean isFalse() {
        return
            tag == BOOLEAN &&
            constValue() != null &&
            ((Integer)constValue()).intValue() == 0;
    }

    /** Is this a constant type whose value is true?
     */
    public boolean isTrue() {
        return
            tag == BOOLEAN &&
            constValue() != null &&
            ((Integer)constValue()).intValue() != 0;
    }

    public String argtypes(boolean varargs) {
        List<Type> args = getParameterTypes();
        if (!varargs) return args.toString();
        StringBuffer buf = new StringBuffer();
        while (args.tail.nonEmpty()) {
            buf.append(args.head);
            args = args.tail;
            buf.append(',');
        }
        if (args.head.tag == ARRAY) {
            buf.append(((ArrayType)args.head).elemtype);
            buf.append("...");
        } else {
            buf.append(args.head);
        }
        return buf.toString();
    }

    /** Access methods.
     */
    public List<Type>        getTypeArguments()  { return List.nil(); }
    public List<RPL>         getRPLArguments()  { return List.nil(); }
    public List<Effects>     getEffectArguments() { return List.nil(); }
    public Type              getEnclosingType()  { return null; }
    public List<Type>        getParameterTypes() { return List.nil(); }
    public Type              getReturnType()     { return null; }
    public List<Type>        getThrownTypes()    { return List.nil(); }
    public Type              getUpperBound()     { return null; }
    public Type              getLowerBound()     { return null; }
    public RPL               getOwner()          { 
        List<RPL> rgnActuals = getRPLArguments();
        return (rgnActuals.nonEmpty()) ? rgnActuals.head : RPLs.ROOT;
    }

    public void setThrown(List<Type> ts) {
        throw new AssertionError();
    }

    /** Navigation methods, these will work for classes, type variables,
     *  foralls, but will return null for arrays and methods.
     */

   /** Return all type parameters of this type and all its outer types in order
    *  outer (first) to inner (last).
    */
    public List<Type> alltyparams() { return List.nil(); }
    
    /** Return all region parameters of this type and all its outer types
     *  in order outer (first) to inner (last)
     */
    public List<RPL> allrgnparams() { return List.nil(); }
    
    /** Return all effect parameters of this type and all its outer types in order
     *  outer (first) to inner (last).
     */
     public List<Effects> alleffectparams() { return List.nil(); }

     /** Does this type contain "error" elements?
     */
    public boolean isErroneous() {
        return false;
    }

    public static boolean isErroneous(List<Type> ts) {
        for (List<Type> l = ts; l.nonEmpty(); l = l.tail)
            if (l.head.isErroneous()) return true;
        return false;
    }

    /** Is this type parameterized?
     *  A class type is parameterized if it has some parameters.
     *  An array type is parameterized if its element type is parameterized.
     *  All other types are not parameterized.
     */
    public boolean isParameterized() {
        return false;
    }

    /**
     * Does this type have region parameters?
     * A class type has region parameters if region parameters appear in 
     * its definition.
     * An array type has parameters if region parameters appear in its definition or
     * its element type has region parameters.
     * All other types do not have region parameters.
     */
    public boolean hasRegionParams() {
	return false;
    }
    
    /**
     * Does this type have effect parameters?
     * A class type has region parameters if effect parameters appear in 
     * its definition.
     * All other types do not have effect parameters.
     */
    public boolean hasEffectParams() {
	return false;
    }
    
    /** Is this type a raw type?
     *  A class type is a raw type if it misses some of its parameters.
     *  An array type is a raw type if its element type is raw.
     *  All other types are not raw.
     *  Type validation will ensure that the only raw types
     *  in a program are types that miss all their type variables.
     */
    public boolean isRaw() {
        return false;
    }

    public boolean isCompound() {
        return tsym.completer == null
            // Compound types can't have a completer.  Calling
            // flags() will complete the symbol causing the
            // compiler to load classes unnecessarily.  This led
            // to regression 6180021.
            && (tsym.flags() & COMPOUND) != 0;
    }

    public boolean isInterface() {
        return (tsym.flags() & INTERFACE) != 0;
    }

    public boolean isPrimitive() {
        return tag < VOID;
    }

    /**
     * Does this type contain occurrences of type t?
     */
    public boolean contains(Type t) {
        return t == this;
    }

    public static boolean contains(List<Type> ts, Type t) {
        for (List<Type> l = ts;
             l.tail != null /*inlined: l.nonEmpty()*/;
             l = l.tail)
            if (l.head.contains(t)) return true;
        return false;
    }

    /** Does this type contain an occurrence of some type in `elems'?
     */
    public boolean containsSome(List<Type> ts) {
        for (List<Type> l = ts; l.nonEmpty(); l = l.tail)
            if (this.contains(ts.head)) return true;
        return false;
    }

    public boolean isSuperBound() { return false; }
    public boolean isExtendsBound() { return false; }
    public boolean isUnbound() { return false; }
    public Type withTypeVar(Type t) { return this; }

    public static List<Type> removeBounds(List<Type> ts) {
        ListBuffer<Type> result = new ListBuffer<Type>();
        for(;ts.nonEmpty(); ts = ts.tail) {
            result.append(ts.head.removeBounds());
        }
        return result.toList();
    }
    public Type removeBounds() {
        return this;
    }

    /** The underlying method type of this type.
     */
    public MethodType asMethodType() { throw new AssertionError(); }

    /** Complete loading all classes in this type.
     */
    public void complete() {}

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public TypeSymbol asElement() {
        return tsym;
    }

    public TypeKind getKind() {
        switch (tag) {
        case BYTE:      return TypeKind.BYTE;
        case CHAR:      return TypeKind.CHAR;
        case SHORT:     return TypeKind.SHORT;
        case INT:       return TypeKind.INT;
        case LONG:      return TypeKind.LONG;
        case FLOAT:     return TypeKind.FLOAT;
        case DOUBLE:    return TypeKind.DOUBLE;
        case BOOLEAN:   return TypeKind.BOOLEAN;
        case VOID:      return TypeKind.VOID;
        case BOT:       return TypeKind.NULL;
        case NONE:      return TypeKind.NONE;
        default:        return TypeKind.OTHER;
        }
    }

    public <R, P> R accept(TypeVisitor<R, P> v, P p) {
        if (isPrimitive())
            return v.visitPrimitive(this, p);
        else
            throw new AssertionError();
    }

    public static class WildcardType extends Type
            implements javax.lang.model.type.WildcardType {

        public Type type;
        public BoundKind kind;
        public TypeVar bound;

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitWildcardType(this, s);
        }

        public WildcardType(Type type, BoundKind kind, TypeSymbol tsym) {
            super(WILDCARD, tsym);
            assert(type != null);
            this.kind = kind;
            this.type = type;
        }
        public WildcardType(WildcardType t, TypeVar bound) {
            this(t.type, t.kind, t.tsym, bound);
        }

        public WildcardType(Type type, BoundKind kind, TypeSymbol tsym, TypeVar bound) {
            this(type, kind, tsym);
            this.bound = bound;
        }

        public boolean isSuperBound() {
            return kind == SUPER ||
                kind == UNBOUND;
        }
        public boolean isExtendsBound() {
            return kind == EXTENDS ||
                kind == UNBOUND;
        }
        public boolean isUnbound() {
            return kind == UNBOUND;
        }

        public Type withTypeVar(Type t) {
            //-System.err.println(this+".withTypeVar("+t+");");//DEBUG
            if (bound == t)
                return this;
            bound = (TypeVar)t;
            return this;
        }

        boolean isPrintingBound = false;
        public String toString() {
            StringBuffer s = new StringBuffer();
            s.append(kind.toString());
            if (kind != UNBOUND)
                s.append(type);
            if (moreInfo && bound != null && !isPrintingBound)
                try {
                    isPrintingBound = true;
                    s.append("{:").append(bound.getUpperBound()).append(":}");
                } finally {
                    isPrintingBound = false;
                }
            return s.toString();
        }

        public Type map(Mapping f) {
            //- System.err.println("   (" + this + ").map(" + f + ")");//DEBUG
            Type t = type;
            if (t != null)
                t = f.apply(t);
            if (t == type)
                return this;
            else
                return new WildcardType(t, kind, tsym, bound);
        }

        public Type removeBounds() {
            return isUnbound() ? this : type;
        }

        public Type getExtendsBound() {
            if (kind == EXTENDS)
                return type;
            else
                return null;
        }

        public Type getSuperBound() {
            if (kind == SUPER)
                return type;
            else
                return null;
        }

        public TypeKind getKind() {
            return TypeKind.WILDCARD;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitWildcard(this, p);
        }
    }

    public static class ClassType extends Type implements DeclaredType {

        /** The enclosing type of this type. If this is the type of an inner
         *  class, outer_field refers to the type of its enclosing
         *  instance class, in all other cases it referes to noType.
         */
        public Type outer_field; // DPJ -- changed private to public

        /** The type parameters of this type (to be set once class is loaded).
         */
        public List<Type> typarams_field;
        
        /** The RPL parameters of this type
         */
        public List<RPL> rplparams_field;

        /** The effect parameters of this type
         */
        public List<Effects> effectparams_field;

        /** A cache variable for the type parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allparams
         */
        public List<Type> alltyparams_field;
        
        /** A cache variable for the RPL parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allregionparams
         */
        public List<RPL> allrplparams_field;
        
        /** A cache variable for the region parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allregionactuals
         */
        //public List<RPL> allrgnactuals_field;

        /** A cache variable for the effect parameters of this type,
         *  appended to all parameters of its enclosing class.
         *  @see #allparams
         */
        public List<Effects> alleffectparams_field;
 
        /** The supertype of this class (to be set once class is loaded).
         */
        public Type supertype_field;

        /** The interfaces of this class (to be set once class is loaded).
         */
        public List<Type> interfaces_field;

        /** Type of an array cell, if this is an array class                                                                
         */
        public Type cellType;

        @Override
        public Type getCellType() { return cellType; }
        @Override
        public void setCellType(Type t) { cellType = t; }
        
	public static int counter = 0;
        public ClassType(Type outer, List<Type> typarams, 
        	List<RPL> rgnparams, 
        	List<Effects> effectparams,
        	TypeSymbol tsym,
        	Type cellType) {
            super(CLASS, tsym);
            this.outer_field = outer;
            this.typarams_field = typarams;
            this.rplparams_field = rgnparams;
            this.effectparams_field = effectparams;
            this.alltyparams_field = null;
            this.allrplparams_field = null;
            this.alleffectparams_field = null;
            this.supertype_field = null;
            this.interfaces_field = null;
            this.cellType = cellType;
            /*
            // this can happen during error recovery
            assert
                outer.isParameterized() ?
                typarams.length() == tsym.type.typarams().length() :
                outer.isRaw() ?
                typarams.length() == 0 :
                true;
            */
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitClassType(this, s);
        }

        public Type constType(Object constValue) {
            final Object value = constValue;
            return new ClassType(getEnclosingType(), typarams_field, 
        	    rplparams_field, effectparams_field, tsym,
        	    cellType) {
                    @Override
                    public Object constValue() {
                        return value;
                    }
                    @Override
                    public Type baseType() {
                        return tsym.type;
                    }
                };
        }

        /** The Java source which this type represents.
         */
        public String toString() {
            StringBuffer buf = new StringBuffer();
            if (getEnclosingType().tag == CLASS && tsym.owner.kind == TYP) {
                buf.append(getEnclosingType().toString());
                buf.append(".");
                buf.append(className(tsym, false));
            } else {
                buf.append(className(tsym, true));
            }
            boolean typarams = getTypeArguments().nonEmpty();
            boolean rplparams = Types.printDPJ && getRPLArguments().nonEmpty();
            boolean effectparams = Types.printDPJ && getEffectArguments().nonEmpty();
            boolean params = typarams | rplparams | effectparams;
            if (params) buf.append('<');
            buf.append(getTypeArguments().toString());
            if (rplparams) {
        	if (typarams) buf.append(", ");
        	buf.append(getRPLArguments().toString());
            }
            if (effectparams) {
        	if (typarams | rplparams) buf.append(", ");
        	buf.append(getEffectArguments().toString());
            }
            if (params) buf.append('>');
            return buf.toString();
        }
//where
            private String className(Symbol sym, boolean longform) {
                if (sym.name.len == 0 && (sym.flags() & COMPOUND) != 0) {
                    StringBuffer s = new StringBuffer(supertype_field.toString());
                    for (List<Type> is=interfaces_field; is.nonEmpty(); is = is.tail) {
                        s.append("&");
                        s.append(is.head.toString());
                    }
                    return s.toString();
                } else if (sym.name.len == 0) {
                    String s;
                    ClassType norm = (ClassType) tsym.type;
                    if (norm == null) {
                        s = Log.getLocalizedString("anonymous.class", (Object)null);
                    } else if (norm.interfaces_field != null && norm.interfaces_field.nonEmpty()) {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.interfaces_field.head);
                    } else {
                        s = Log.getLocalizedString("anonymous.class",
                                                   norm.supertype_field);
                    }
                    if (moreInfo)
                        s += String.valueOf(sym.hashCode());
                    return s;
                } else if (longform) {
                    return sym.getQualifiedName().toString();
                } else {
                    return sym.name.toString();
                }
            }

        public List<Type> getTypeArguments() {
            if (typarams_field == null) {
                complete();
                if (typarams_field == null)
                    typarams_field = List.nil();
            }
            return typarams_field;
        }
        
        public List<RPL> getRPLArguments() {
            if (rplparams_field == null) {
        	complete();
                if (rplparams_field == null) {
                    rplparams_field = List.nil();                    
                }
            }
            
            // This piece of code handles the case of default region parameters, e.g.,
            // declare 'class C<region R>' but use 'new C()' with the binding omitted.
            // In that case, we'll see empty 'actuals' but nonempty 'formals' stored
            // in tsym.type.getRegionParams().  So we need to fill in the 'actuals'
            // with Root.  We only do this if the type has not been erased -- if it has,
            // then we really want those empty actuals.  Whew!
            if (!DPJerased && rplparams_field.isEmpty() && 
        	    tsym.type != this &&
        	    !tsym.type.getRPLArguments().isEmpty()) {
        	ListBuffer<RPL> buf = ListBuffer.lb();
        	int size = tsym.type.getRPLArguments().size();
        	for (int i = 0; i < size; ++i) {
        	    buf.append(RPLs.ROOT);
        	}
        	rplparams_field = buf.toList();
            }
            return rplparams_field;
        }
        
        public List<Effects> getEffectArguments() {
            if (effectparams_field == null) {
        	complete();
        	if (effectparams_field == null)
        	    effectparams_field = List.nil();
            }
            return effectparams_field;
        }
                
        public Type getEnclosingType() {
            return outer_field;
        }

        public void setEnclosingType(Type outer) {
            outer_field = outer;
        }

        public List<Type> alltyparams() {
            if (alltyparams_field == null) {
                alltyparams_field = 
                    getTypeArguments().prependList(getEnclosingType().alltyparams());
            }
            return alltyparams_field;
        }

        public List<RPL> allrgnparams() {
            if (allrplparams_field == null) {
        	allrplparams_field = 
        		getRPLArguments().prependList(getEnclosingType().allrgnparams());
            }
            return allrplparams_field;
        }

        @Override
        public List<Effects> alleffectparams() {
            if (alleffectparams_field == null) {
                alleffectparams_field = 
                    getEffectArguments();
                Type enclosingType = getEnclosingType();
                if (enclosingType != this)
                    alleffectparams_field =
                    	alleffectparams_field.prependList(enclosingType.alleffectparams());
            }
            return alleffectparams_field;
        }

        public boolean isErroneous() {
            return
                getEnclosingType().isErroneous() ||
                isErroneous(getTypeArguments()) ||
                this != tsym.type && tsym.type.isErroneous();
        }

        public boolean isParameterized() {
            return alltyparams().tail != null;
            // optimization, was: allparams().nonEmpty();
        }
        
        public boolean hasRegionParams() {
            return allrgnparams().tail != null;
        }
        
        public boolean hasEffectParams() {
            return alleffectparams().tail != null;
        }

        /** A cache for the rank. */
        int rank_field = -1;

        /** A class type is raw if it misses some
         *  of its type parameter sections.
         *  After validation, this is equivalent to:
         *  allparams.isEmpty() && tsym.type.allparams.nonEmpty();
         */
        public boolean isRaw() {
            return
                this != tsym.type && // necessary, but not sufficient condition
                tsym.type.alltyparams().nonEmpty() &&
                alltyparams().isEmpty();
        }

        public Type map(Mapping f) {
            Type outer = getEnclosingType();
            Type outer1 = f.apply(outer);
            List<Type> typarams = getTypeArguments();
            List<Type> typarams1 = map(typarams, f);
            if (outer1 == outer && typarams1 == typarams) return this;
            else return new ClassType(outer1, typarams1, rplparams_field, 
        	    effectparams_field, tsym, getCellType());
        }

        public boolean contains(Type elem) {
            return
                elem == this
                || (isParameterized()
                    && (getEnclosingType().contains(elem) || contains(getTypeArguments(), elem)));
        }

        public void complete() {
            if (tsym.completer != null) tsym.complete();
        }

        public TypeKind getKind() {
            return TypeKind.DECLARED;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitDeclared(this, p);
        }
    }

    public static class ArrayType extends Type
            implements javax.lang.model.type.ArrayType {

        public Type elemtype;
        public RPL rpl = RPLs.ROOT; //new RPL(Symtab.ROOT);
        public VarSymbol indexVar;
        public Symtab syms;
        
        public ArrayType(Type elemtype, RPL rpl, VarSymbol indexVar, TypeSymbol arrayClass) {
            super(ARRAY, arrayClass);
            this.elemtype = elemtype;
            if (rpl != null) this.rpl = rpl;
            this.indexVar = indexVar;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitArrayType(this, s);
        }

        public String toString() {
            if (Types.printDPJ) {
        	StringBuffer sb = new StringBuffer();
        	Type baseType = elemtype;
        	while (baseType instanceof ArrayType) {
        	    baseType = ((ArrayType) baseType).elemtype;
        	}
        	sb.append(baseType);
        	Type nextType = this;
        	do {
        	    ArrayType currentType = (ArrayType) nextType;
        	    sb.append("[]");
        	    if (!currentType.rpl.equals(RPLs.ROOT)) {
        		sb.append("<");
        		sb.append(currentType.rpl);
        		sb.append(">");
        	    }
        	    if (currentType.indexVar != null &&
        		    !currentType.indexVar.toString().equals("_")) {
        		sb.append("#");
        		sb.append(indexVar);
        	    }
        	    nextType = currentType.elemtype;
        	} while (nextType instanceof ArrayType);
        	return sb.toString();
            }
            return elemtype + "[]";
        }

        public boolean equals(Object obj) {
            return
                this == obj ||
                (obj instanceof ArrayType &&
                 this.elemtype.equals(((ArrayType)obj).elemtype));
        }

        public int hashCode() {
            return (ARRAY << 5) + elemtype.hashCode();
        }

        public List<Type> alltyparams() { return elemtype.alltyparams(); }

        public boolean isErroneous() {
            return elemtype.isErroneous();
        }

        public boolean isParameterized() {
            return elemtype.isParameterized();
        }

        public boolean isRaw() {
            return elemtype.isRaw();
        }

        public Type map(Mapping f) {
            Type elemtype1 = f.apply(elemtype);
            if (elemtype1 == elemtype) return this;
            else return new ArrayType(elemtype1, null, null, tsym);
        }

        public boolean contains(Type elem) {
            return elem == this || elemtype.contains(elem);
        }

        public void complete() {
            elemtype.complete();
        }

        public Type getComponentType() {
            return elemtype;
        }

        public TypeKind getKind() {
            return TypeKind.ARRAY;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitArray(this, p);
        }
    }

    public static class MethodType extends Type
                    implements Cloneable, ExecutableType {

        public List<Type> argtypes;
        public Type restype;
        public List<Type> thrown;
        
        // DPJ:  We need to know these bindings to resolve effects at the
        // method call site
        public List<Type> typeactuals = List.nil();
        public List<RPL> regionActuals = List.nil();
        public List<Effects> effectactuals = List.nil();

        public MethodType(List<Type> argtypes,
                          Type restype,
                          List<Type> thrown,
                          TypeSymbol methodClass) {
            super(METHOD, methodClass);
            this.argtypes = argtypes;
            this.restype = restype;
            this.thrown = thrown;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitMethodType(this, s);
        }

        /** The Java source which this type represents.
         *
         *  XXX 06/09/99 iris This isn't correct Java syntax, but it probably
         *  should be.
         */
        public String toString() {
            return "(" + argtypes + ")" + restype;
        }

        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (!(obj instanceof MethodType))
                return false;
            MethodType m = (MethodType)obj;
            List<Type> args1 = argtypes;
            List<Type> args2 = m.argtypes;
            while (!args1.isEmpty() && !args2.isEmpty()) {
                if (!args1.head.equals(args2.head))
                    return false;
                args1 = args1.tail;
                args2 = args2.tail;
            }
            if (!args1.isEmpty() || !args2.isEmpty())
                return false;
            return restype.equals(m.restype);
        }

        public int hashCode() {
            int h = METHOD;
            for (List<Type> thisargs = this.argtypes;
                 thisargs.tail != null; /*inlined: thisargs.nonEmpty()*/
                 thisargs = thisargs.tail)
                h = (h << 5) + thisargs.head.hashCode();
            return (h << 5) + this.restype.hashCode();
        }

        public List<Type>        getParameterTypes() { return argtypes; }
        public Type              getReturnType()     { return restype; }
        public List<Type>        getThrownTypes()    { return thrown; }

        public void setThrown(List<Type> t) {
            thrown = t;
        }

        public boolean isErroneous() {
            return
                isErroneous(argtypes) ||
                restype != null && restype.isErroneous();
        }

        public Type map(Mapping f) {
            List<Type> argtypes1 = map(argtypes, f);
            Type restype1 = f.apply(restype);
            List<Type> thrown1 = map(thrown, f);
            if (argtypes1 == argtypes &&
                restype1 == restype &&
                thrown1 == thrown) return this;
            else return new MethodType(argtypes1, restype1, thrown1, tsym);
        }

        public boolean contains(Type elem) {
            return elem == this || contains(argtypes, elem) || restype.contains(elem);
        }

        public MethodType asMethodType() { return this; }

        public void complete() {
            for (List<Type> l = argtypes; l.nonEmpty(); l = l.tail)
                l.head.complete();
            restype.complete();
            for (List<Type> l = thrown; l.nonEmpty(); l = l.tail)
                l.head.complete();
        }

        public List<TypeVar> getTypeVariables() {
            return List.nil();
        }
        
        public List<VariableEffect> getEffectVariables() {
            return List.nil();
        }

	public TypeSymbol asElement() {
	    return null;
	}

        public TypeKind getKind() {
            return TypeKind.EXECUTABLE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }
    }

    public static class PackageType extends Type implements NoType {

        PackageType(TypeSymbol tsym) {
            super(PACKAGE, tsym);
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitPackageType(this, s);
        }

        public String toString() {
            return tsym.getQualifiedName().toString();
        }

        public TypeKind getKind() {
            return TypeKind.PACKAGE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    public static class TypeVar extends Type implements TypeVariable {

        /** The bound of this type variable; set from outside.
         *  Must be nonempty once it is set.
         *  For a bound, `bound' is the bound type itself.
         *  Multiple bounds are expressed as a single class type which has the
         *  individual bounds as superclass, respectively interfaces.
         *  The class type then has as `tsym' a compiler generated class `c',
         *  which has a flag COMPOUND and whose owner is the type variable
         *  itself. Furthermore, the erasure_field of the class
         *  points to the first class or interface bound.
         */
        public Type bound = null;
        public Type lower;
        public List<RPL> rplparams = List.nil();
        public List<RPL> rplargs = List.nil();
        /** If this type var is an instantiated type, the prototype is the original
         *  type var.  Bounds will be set for that one.
         */
        public TypeVar prototype = null;

        public TypeVar(Name name, Symbol owner, Type lower) {
            super(TYPEVAR, null);
            tsym = new TypeSymbol(0, name, this, owner);
            this.lower = lower;
        }

        public TypeVar(TypeSymbol tsym, Type bound, Type lower) {
            super(TYPEVAR, tsym);
            this.bound = bound;
            this.lower = lower;
        }

        @Override
        public List<RPL> getRPLArguments() { 
            if (rplargs.isEmpty() && rplparams.nonEmpty())
        	rplargs = rplparams;
            return rplargs; 
        }
        
        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitTypeVar(this, s);
        }

        public Type getUpperBound() { 
            if (bound == null && prototype != null) return prototype.bound;
            return bound; 
        }

        public void setUpperBound(Type bound) {
            this.bound = bound;
            if (prototype != null) prototype.bound = bound;
        }
        
        int rank_field = -1;

        public Type getLowerBound() {
            return lower;
        }

        public TypeKind getKind() {
            return TypeKind.TYPEVAR;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitTypeVariable(this, p);
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer(super.toString());
            if (Types.printDPJ && getRPLArguments().nonEmpty()) {
        	sb.append('<');
        	sb.append(getRPLArguments().toString());
        	sb.append('>');
            }
            return sb.toString();
        }
    }

    /** A captured type variable comes from wildcards which can have
     *  both upper and lower bound.  CapturedType extends TypeVar with
     *  a lower bound.
     */
    public static class CapturedType extends TypeVar {

        public Type lower;
        public WildcardType wildcard;

        public CapturedType(Name name,
			    Symbol owner,
			    Type upper,
			    Type lower,
			    WildcardType wildcard) {
            super(name, owner, lower);
            assert lower != null;
            this.bound = upper;
            this.lower = lower;
	    this.wildcard = wildcard;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitCapturedType(this, s);
        }

        public Type getLowerBound() {
            return lower;
        }

	@Override
	public String toString() {
            return "capture#"
		+ (hashCode() & 0xFFFFFFFFL) % PRIME
		+ " of "
		+ wildcard;
        }
	static final int PRIME = 997;  // largest prime less than 1000
    }

    public static abstract class DelegatedType extends Type {
        public Type qtype;
        public DelegatedType(int tag, Type qtype) {
            super(tag, qtype.tsym);
            this.qtype = qtype;
        }
        public String toString() { return qtype.toString(); }
        public List<Type> getTypeArguments() { return qtype.getTypeArguments(); }
        public List<Effects> getEffectArguments() { return qtype.getEffectArguments(); }
        public Type getEnclosingType() { return qtype.getEnclosingType(); }
        public List<Type> getParameterTypes() { return qtype.getParameterTypes(); }
        public Type getReturnType() { return qtype.getReturnType(); }
        public List<Type> getThrownTypes() { return qtype.getThrownTypes(); }
        public List<Type> alltyparams() { return qtype.alltyparams(); }
        public Type getUpperBound() { return qtype.getUpperBound(); }
        public Object clone() { DelegatedType t = (DelegatedType)super.clone(); t.qtype = (Type)qtype.clone(); return t; }
        public boolean isErroneous() { return qtype.isErroneous(); }
    }

    public static class ForAll extends DelegatedType
            implements Cloneable, ExecutableType {
        public List<Type> tvars;
        public List<RPL> rvars;
        public List<Effects> evars;

        public ForAll(List<Type> tvars, List<RPL> rvars, 
        	List<Effects> evars, Type qtype) {
            super(FORALL, qtype);
            this.tvars = tvars;
            this.rvars = rvars;
            this.evars = evars;
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitForAll(this, s);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append('<');
            sb.append(tvars);
            if (rvars.nonEmpty()) {
        	if (tvars.nonEmpty()) sb.append(", ");
        	sb.append("region ");
        	sb.append(rvars);
            }
            if (evars.nonEmpty()) {
        	if (tvars.nonEmpty() || rvars.nonEmpty()) sb.append(", ");
        	sb.append(evars);
            }
            sb.append('>');
            sb.append(qtype);
            return sb.toString();
        }

        public List<Type> getTypeArguments()   { return tvars; }
        public List<RPL> getRPLArguments() { return rvars; }
        public List<Effects> getEffectArguments() { return evars; }

        public void setThrown(List<Type> t) {
            qtype.setThrown(t);
        }

        public Object clone() {
            ForAll result = (ForAll)super.clone();
            result.qtype = (Type)result.qtype.clone();
            return result;
        }

        public boolean isErroneous()  {
            return qtype.isErroneous();
        }

        public Type map(Mapping f) {
            return f.apply(qtype);
        }

        public boolean contains(Type elem) {
            return qtype.contains(elem);
        }

        public MethodType asMethodType() {
            return qtype.asMethodType();
        }

        public void complete() {
            for (List<Type> l = tvars; l.nonEmpty(); l = l.tail) {
                ((TypeVar)l.head).getUpperBound().complete();
            }
            qtype.complete();
        }

        public List<TypeVar> getTypeVariables() {
            return List.convert(TypeVar.class, getTypeArguments());
        }

        public List<VariableEffect> getEffectVariables() {
            return List.convert(VariableEffect.class, getEffectArguments());
        }
        
        public TypeKind getKind() {
            return TypeKind.EXECUTABLE;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitExecutable(this, p);
        }
    }

    /** A class for instantiatable variables, for use during type
     *  inference.
     */
    public static class UndetVar extends DelegatedType {
        public List<Type> lobounds = List.nil();
        public List<Type> hibounds = List.nil();
        public Type inst = null;

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitUndetVar(this, s);
        }

        public UndetVar(Type origin) {
            super(UNDETVAR, origin);
        }

        public String toString() {
            if (inst != null) return inst.toString();
            else return qtype + "?";
        }

        public Type baseType() {
            if (inst != null) return inst.baseType();
            else return this;
        }
    }

    /** Represents VOID or NONE.
     */
    static class JCNoType extends Type implements NoType {
	public JCNoType(int tag) {
	    super(tag, null);
	}

	@Override
        public TypeKind getKind() {
	    switch (tag) {
	    case VOID:  return TypeKind.VOID;
	    case NONE:  return TypeKind.NONE;
            default:
		throw new AssertionError("Unexpected tag: " + tag);
	    }
        }

	@Override
        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNoType(this, p);
        }
    }

    static class BottomType extends Type implements NullType {
	public BottomType() {
	    super(TypeTags.BOT, null);
	}

	@Override
        public TypeKind getKind() {
            return TypeKind.NULL;
        }

	@Override
        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitNull(this, p);
        }
	
	@Override
	public Type constType(Object value) {
	    return this;
	}
	
	@Override 
	public String stringValue() {
	    return "null";
	}
    }

    public static class ErrorType extends ClassType
            implements javax.lang.model.type.ErrorType {

        public ErrorType() {
            super(noType, List.<Type>nil(), List.<RPL>nil(), 
        	    List.<Effects>nil(), null, null);
            tag = ERROR;
        }

        public ErrorType(ClassSymbol c) {
            this();
            tsym = c;
            c.type = this;
            c.kind = ERR;
            c.members_field = new Scope.ErrorScope(c);
        }

        public ErrorType(Name name, TypeSymbol container) {
            this(new ClassSymbol(PUBLIC|STATIC|ACYCLIC, name, null, container));
        }

        @Override
        public <R,S> R accept(Type.Visitor<R,S> v, S s) {
            return v.visitErrorType(this, s);
        }

        public Type constType(Object constValue) { return this; }
        public Type getEnclosingType()          { return this; }
        public Type getReturnType()              { return this; }
        public Type asSub(Symbol sym)            { return this; }
        public Type map(Mapping f)               { return this; }

        public boolean isGenType(Type t)         { return true; }
        public boolean isErroneous()             { return true; }
        public boolean isCompound()              { return false; }
        public boolean isInterface()             { return false; }

        public List<Type> alltyparams()          { return List.nil(); }
        public List<RPL> allrgnparams()          { return List.nil(); }
        public List<Type> getTypeArguments()     { return List.nil(); }
        public List<Effects> getEffectArguments() { return List.nil(); }
        public List<RPL>         getRPLArguments()  { return List.nil(); }


        public TypeKind getKind() {
            return TypeKind.ERROR;
        }

        public <R, P> R accept(TypeVisitor<R, P> v, P p) {
            return v.visitError(this, p);
        }
    }

    /**
     * A visitor for types.  A visitor is used to implement operations
     * (or relations) on types.  Most common operations on types are
     * binary relations and this interface is designed for binary
     * relations, that is, operations on the form
     * Type&nbsp;&times;&nbsp;S&nbsp;&rarr;&nbsp;R.
     * <!-- In plain text: Type x S -> R -->
     *
     * @param <R> the return type of the operation implemented by this
     * visitor; use Void if no return type is needed.
     * @param <S> the type of the second argument (the first being the
     * type itself) of the operation implemented by this visitor; use
     * Void if a second argument is not needed.
     */
    public interface Visitor<R,S> {
        R visitClassType(ClassType t, S s);
        R visitWildcardType(WildcardType t, S s);
        R visitArrayType(ArrayType t, S s);
        R visitMethodType(MethodType t, S s);
        R visitPackageType(PackageType t, S s);
        R visitTypeVar(TypeVar t, S s);
        R visitCapturedType(CapturedType t, S s);
        R visitForAll(ForAll t, S s);
        R visitUndetVar(UndetVar t, S s);
        R visitErrorType(ErrorType t, S s);
        R visitType(Type t, S s);
    }
}
