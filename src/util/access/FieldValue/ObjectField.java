/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package util.access.FieldValue;

import util.Util;
import util.access.TypedValue;

import static util.Util.mapEnumConstant;

/**
 *
 * @author Plutonium_
 */
public interface ObjectField<V> extends TypedValue {

    public Object getOf(V value);

    /** Returns description of the field. */
    public String description();

    /** Returns name of the field. */
    public String name();

    /**
     * Used as string converter for fielded values. For example in tables.
     * When the object signifies empty value, empty string is returned.
     */
    public String toS(Object o, String empty_val);

    public default String toS(V v, Object o, String empty_val) {
        return ObjectField.this.toS(o, empty_val);
    };

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case.
     */
    public default String toStringCapital() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case and all others into
     * lower case.
     */
    public default String toStringCapitalCase() {
        String s = toString();
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }

    /**
     * Variation of {@link #toString()} method.
     * Converts first letter of the string to upper case and all others into
     * lower case and replaces all '_' with ' '.
     * <p>
     * Use to make {@link Enum} constants more human readable, for gui for example.
     */
    public default String toStringEnum() {
        String s = toString().replaceAll("_", " ");
        return s.isEmpty() ? "" : s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
    }


    public default double c_width() {
        return 70;
    }

    public default boolean c_visible() {
        return true;
    }

    public default int c_order() {
        return (this instanceof Enum) ? ((Enum)this).ordinal() : 1;
    }


    public static enum ColumnField implements ObjectField<Object> {
        INDEX;

        @Override
        public Object getOf(Object value) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        private ColumnField() {
            mapEnumConstant(this, f -> f.ordinal()==0 ? "#" : Util.enumToHuman(f));
        }

        @Override
        public String description() {
            return "";
        }

        @Override
        public String toS(Object o, String empty_val) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public Class getType() {
            return Integer.class;
        }

    }

}