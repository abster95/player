
package util.conf;

import java.lang.invoke.MethodHandle;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javafx.beans.InvalidationListener;
import javafx.beans.property.Property;
import javafx.beans.value.ObservableValue;
import javafx.beans.value.WritableValue;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.reactfx.Subscription;

import util.access.ApplicableValue;
import util.access.fieldvalue.EnumerableValue;
import util.access.TypedValue;
import util.access.V;
import util.access.Vo;
import util.dev.TODO;
import util.functional.Functors.Ƒ1;
import util.parsing.Parser;
import util.parsing.StringConverter;
import util.type.Util;

import static java.util.stream.Collectors.joining;
import static javafx.collections.FXCollections.observableArrayList;
import static util.type.Util.getValueFromFieldMethodHandle;
import static util.type.Util.isEnum;
import static util.type.Util.unPrimitivize;
import static util.conf.Configuration.configsOf;
import static util.dev.Util.log;
import static util.dev.Util.noØ;
import static util.functional.Util.*;

/**
 * Object representation of a configurable value.
 * <p/>
 * Config encapsulates access to a value. It allows to obtain the value or
 * change it and also provides additional information associated with it.
 * <p/>
 * Useful for creating {@link Configurable} objects or exporting values from
 * objects in a standardized way.
 * <p/>
 * An aggregation of configs is {@link Configurable}. Note that, technically,
 * config is a singleton configurable. Therefore config actually implements
 * it and can be used as non aggregate configurable type.
 * <p/>
 * Because config is convertible from String and back it also provides convert
 * methods and implements {@link StringConverter}.
 *
 * @param <T> type of value of this config
 *
 * @author Martin Polakovic
 */
public abstract class Config<T> implements ApplicableValue<T>, Configurable<T>, StringConverter<T>, TypedValue<T>, EnumerableValue<T> {

    @Override
    public abstract T getValue();

    @Override
    public abstract void setValue(T val);

    /**
     * {@inheritDoc}
     * <p/>
     * Semantically equivalent to getValue().getClass(), but null-safe and
     * potentially better performing.
     */
    @Override
    abstract public Class<T> getType();

    /**
     * Alternative name of this config. Intended to be human readable and
     * appropriately formated.
     * <p/>
     * Default value is set to be equivalent to name, but can be specified to
     * differ. Always use for gui building instead of {@link #getName()}.
     */
    abstract public String getGuiName();

    /**
     * Name of this config.
     */
    abstract public String getName();

    /**
     * Category or group this config belongs to. Use arbitrarily to group
     * multiple configs together - mostly semantically or by intention.
     */
    abstract public String getGroup();

    /**
     * Description of this config
     */
    abstract public String getInfo();

    /**
     * Indicates editability. Use arbitrarily. Most often sets whether this
     * config should be editable by user via graphical user interface.
     */
    abstract public boolean isEditable();

    /**
     * Minimum allowable value. Applicable only for numbers. In double.
     */
    abstract public double getMin();

    /**
     * Maximum allowable value. Applicable only for numbers. In double.
     */
    abstract public double getMax();

    /**
     * Use to determine whether min and max fields dont dontain illegal value.
     * If they dont, they can be used to query minimal and maximal number value.
     * Otherwise Double not a number is returned and should not be used.
     * @return true if and only if value is a number and both min and max value
     * are specified.
     */
    abstract public boolean isMinMax();

/******************************* default value ********************************/

    /**
     * Get default value for this config. It is the first value this config
     * contained.
     * @return default value. Never null.
     */
    abstract public T getDefaultValue();

    public void setDefaultValue() {
        setValue(getDefaultValue());
    }

    public void setNapplyDefaultValue() {
        setNapplyValue(getDefaultValue());
    }

/******************************** converting **********************************/

    /**
     * Converts the value to String utilizing generic {@link Parser}.
     * Use for serialization or filling out guis.
     */
    public String getValueS() {
        return toS(getValue());
    }

    /**
     * Sets value converted from string.
     * Equivalent to: return setValue(fromS(str));
     * @param str string to parse
     */
    @TODO(note = "Make this behave consistently for null values")
    public void setValueS(String str) {
        T v = fromS(str);
        if(v!=null) setValue(v);
    }

    /**
     * This method is inherited from {@link StringConverter} for compatibility & convenience reasons.
     * Note: invoking this method produces no effects on this config instance. Consider this method static.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public String toS(T v) {
        return Parser.DEFAULT.toS(v);
    }

    /**
     * This method is inherited from {@link StringConverter} for compatibility & convenience reasons.
     * Note: invoking this method produces no effects on this config instance. Consider this method static.
     * <p/>
     * {@inheritDoc}
     */
    @Override
    public T fromS(String str) {
        if(isTypeEnumerable()) {
            // 1 Notice we are traversing all enumarated values to look up the one which we want to
            //   deserialize.
            //   We do this by converting each value to string and compare. This is potentially
            //   inefficient operation. It is much better to parse the string to value first and
            //   then compare obejcts. The problem is Im not sure if relying on Object equals() is
            //   very safe, this should be investigated and optimized.
            //
            // 2 OverridableConfig adds additional information as a prefix when serializing the
            //   value, then removing the prefix when deserializing. This causes the lookup not work
            //   because toS adds the prefix to the values and the string parameter of this method
            //   has it already removed. To bypass this, we rely on Parser.toS/fromS directly,
            //   rather than Config.toS/fromS. This is also dangerous. Of course we could fix this
            //   by having OverridableConfig provide its own implementation, but I dont want to
            //   spread problematic code such as this around. Not till 1 gets fixed up.
            for(T v : enumerateValues())
                if(Parser.DEFAULT.toS(v).equalsIgnoreCase(str)) return v;

            log(Config.class).warn("Cant parse '{}'. No enumerable value for: {}. Using default value.", str,getGuiName());
            return getDefaultValue();
        } else {
            return Parser.DEFAULT.fromS(getType(), str);
        }
    }

/*************************** configurable methods *****************************/

    Supplier<Collection<T>> valueEnumerator;
    private boolean init = false;

    public boolean isTypeEnumerable() {
        if(!init && valueEnumerator==null) {
            valueEnumerator = buildEnumEnumerator(getDefaultValue());
            init = true;
        }
        return valueEnumerator!=null;
    }

    @Override
    public Collection<T> enumerateValues() {
        if(isTypeEnumerable()) return valueEnumerator.get();
        throw new RuntimeException(getType() + " not enumerable.");
    }

    private static <T> Supplier<Collection<T>> buildEnumEnumerator(T v) {
        Class c = v==null ? Void.class : v.getClass();
        return isEnum(c) ? () -> list((T[]) Util.getEnumConstants(c)) : null;
    }

/*************************** configurable methods *****************************/

    /**
     * This method is inherited from Configurable and is not intended to be used
     * manually on objects of this class, rather, in situations this config
     * acts as singleton {@link Configurable}.
     * <p/>
     * {@inheritDoc }
     * <p/>
     * Implementation details: returns self if name equals with parameter or null
     * otherwise
     * @param name
     * @throws IllegalArgumentException if name doent equal name of this config.
     */
    @Override
    public final Config<T> getField(String name) {
        if(!name.equals(getName())) throw new IllegalArgumentException("Name mismatch");
        else return this;
    }

    /**
     * This method is inherited from Configurable and is not intended to be used
     * manually on objects of this class, rather, in situations this config
     * acts as singleton {@link Configurable}.
     * <p/>
     * {@inheritDoc }
     * <p/>
     * Implementation details: returns singleton list of self.
     * @return
     */
    @Override
    public final List<Config<T>> getFields() {
        return Collections.singletonList(this);
    }




/********************************* CREATING ***********************************/

    /**
     * Creates config for plain object - value. The difference from
     * {@link #forProperty(Class, String, Object)} is that
     * property is a value wrapper while value is considered immutable, thus
     * a wrapper needs to be created (and will be automatically).
     * <p/>
     * If the value is not a value (its class is supported by ({@link #forProperty(Class, String, Object)}),
     * then that method is called.
     * or is null, runtime exception is thrown.
     */
    public static <T> Config<T> forValue(Class type, String name, Object value) {
        noØ(value, "Config can not be created for null");
        if(value instanceof Config ||
           value instanceof VarList ||
           value instanceof Vo ||
           value instanceof WritableValue ||
           value instanceof ObservableValue)
            throw new RuntimeException("Value " + value + "is a property and can"
                    + "not be turned into Config as value.");
        return forProperty(type, name, new V<>(value));
    }

    /**
     * Creates config for property. Te property will become the underlying data
     * of the config and thus reflect any value changes and vice versa. If
     * the property is read only, config will also be read only (its set()
     * methods will not do anything). If the property already is config, it is
     * returned.
     *
     * @param name of of the config, will be used as gui name
     * @param property underlying property for the config.
     * The property must be instance of any of:
     * <ul>
     * <li> {@link Config}
     * <li> {@link VarList}
     * <li> {@link WritableValue}
     * <li> {@link ObservableValue}
     * </ul>
     * so standard javafx properties will all work. If not instance of any of
     * the above, runtime exception will be thrown.
     */
    public static <T> Config<T> forProperty(Class<T> type, String name, Object property) {
        if(property instanceof Config)
            return (Config<T>)property;
        if(property instanceof VarList)
            return new ListConfig(name,(VarList)property);
        if(property instanceof Vo)
            return new OverridablePropertyConfig<>(type,name,(Vo<T>)property);
        if(property instanceof WritableValue)
            return new PropertyConfig<>(type,name,(WritableValue<T>)property);
        if(property instanceof ObservableValue)
            return new ReadOnlyPropertyConfig<>(type,name,(ObservableValue<T>)property);
        throw new RuntimeException("Must be WritableValue or ReadOnlyValue, but is " + property.getClass());
    }

    public static Collection<Config<?>> configs(Object o) {
        return (Collection) configsOf(o.getClass(), o, false, true);
    }

/******************************* IMPLEMENTATIONS ******************************/

    public static abstract class ConfigBase<T> extends Config<T> {

        private final Class<T> type;
        private final String gui_name;
        private final String name;
        private final String group;
        private final String info;
        private final boolean editable;
        private final double min;
        private final double max;
        @util.dev.Dependency("DO NOT RENAME - accessed using reflection")
        private final T defaultValue;

        /**
         *
         * @throws NullPointerException if val parameter null. The wrapped value must
         * no be null.
         */
        @TODO(note = "make static map for valueEnumerators")
        ConfigBase(Class<T> type, String name, String gui_name, T val, String category, String info, boolean editable, double min, double max) {
            this.type = unPrimitivize(type);
            this.gui_name = gui_name;
            this.name = name;
            this.defaultValue = val;
            this.group = category;
            this.info = info;
            this.editable = editable;
            this.min = min;
            this.max = max;
            if(val==null) log(ConfigBase.class).info("Config '{}' initial value is null. {}",name);
        }

        /**
         *
         * @param name
         * @param c
         * @param val
         * @param category
         *
         * @throws NullPointerException if val parameter null. The wrapped value must
         * no be null.
         */
        ConfigBase(Class<T> type, String name, IsConfig c, T val, String category) {
            this(type, name, c.name().isEmpty() ? name : c.name(), val, category, c.info(), c.editable(), c.min(), c.max());
        }

        @Override
        public final String getGuiName() {
            return gui_name;
        }

        @Override
        public final String getName() {
            return name;
        }

        @Override
        public final String getGroup() {
            return group;
        }

        @Override
        public Class<T> getType() {
            return type;
        }

        @Override
        public final String getInfo() {
            return info;
        }

        @Override
        public final boolean isEditable() {
            return editable;
        }

        @Override
        public final double getMin() {
            return min;
        }

        @Override
        public final double getMax() {
            return max;
        }

        @Override
        public boolean isMinMax() {
            return !(Double.compare(min, Double.NaN)==0 || Double.compare(max, Double.NaN)==0) &&
                    Number.class.isAssignableFrom(getType());
        }

        @Override
        public T getDefaultValue() {
            return defaultValue;
        }
    }
    /** {@link Config} wrapping {@link java.lang.reflect.Field}. Can wrap both static or instance fields. */
    public static class FieldConfig<T> extends ConfigBase<T> {

        private final Object instance;
        private final MethodHandle getter;
        private final MethodHandle setter;
        MethodHandle applier = null;

        /**
         * @param name
         * @param c
         * @param category
         * @param instance owner of the field or null if static
         */
        @SuppressWarnings("unchecked")
        FieldConfig(String name, IsConfig c, Object instance, String category, MethodHandle getter, MethodHandle setter) {
            super((Class)getter.type().returnType(), name, c, getValueFromFieldMethodHandle(getter, instance), category);
            this.getter = getter;
            this.setter = setter;
            this.instance = instance;
        }

        @Override
        public T getValue() {
            return getValueFromFieldMethodHandle(getter, instance);
        }

        @Override
        public void setValue(T val) {
            try {
                if(instance==null) setter.invokeWithArguments(val);
                else setter.invokeWithArguments(instance,val);
            } catch (Throwable e) {
                throw new RuntimeException("Error setting config field " + getName(),e);
            }
        }

        @Override
        public void applyValue(T val) {
            if(applier != null) {
                try {
                    int i = applier.type().parameterCount();

                    if(i==1) applier.invokeWithArguments(val);
                    else applier.invoke();
                } catch (Throwable e) {
                    throw new RuntimeException("Error applying config field " + getName(),e);
                }
            }
        }

        /**
         * Equals if and only if non null, is Config type and source field is equal.
         */
        @Override
        public boolean equals(Object o) {
            if(this==o) return true;

            if (o == null || !(o instanceof FieldConfig)) return false;

            FieldConfig c = (FieldConfig)o;
            return setter.equals(c.setter) && getter.equals(c.getter) &&
                   applier.equals(c.applier);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + Objects.hashCode(this.applier);
            hash = 23 * hash + Objects.hashCode(this.getter);
            hash = 23 * hash + Objects.hashCode(this.setter);
            return hash;
        }

    }
    public static class PropertyConfig<T> extends ConfigBase<T> {

        protected final WritableValue<T> value;

        /**
         * Constructor to be used with framework
         * @param name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, IsConfig c, WritableValue<T> property, String category) {
            super(property_type, name, c, property.getValue(), category);
            value = property;

            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @param editable
         * @param min use in combination with max if value is Number
         * @param max use in combination with min if value is Number
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, String gui_name, WritableValue<T> property, String category, String info, boolean editable, double min, double max) {
            super(property_type, name, gui_name, property.getValue(), category, info, editable, min, max);
            value = property;

            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, WritableValue<T> property) {
            this(property_type, name, name, property, "", "", true, Double.NaN, Double.NaN);
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public PropertyConfig(Class<T> property_type, String name, WritableValue<T> property, String info) {
            this(property_type, name, name, property, "", info, true, Double.NaN, Double.NaN);
        }

        @Override
        public T getValue() {
            return value.getValue();
        }

        @Override
        public void setValue(T val) {
            value.setValue(val);
        }

        @Override
        public void applyValue() {
            if (value instanceof ApplicableValue)
                ApplicableValue.class.cast(value).applyValue();
        }

        @Override
        public void applyValue(T val) {
            if (value instanceof ApplicableValue)
                ApplicableValue.class.cast(value).applyValue(val);
        }

        public WritableValue<T> getProperty() {
            return value;
        }

        /**
         * Equals if and only if object instance of PropertyConfig and its property
         * is the same property as property of this: property==o.property;
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if(o==this) return true;
            return (o instanceof PropertyConfig && value==((PropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }

    }
    public static class ReadOnlyPropertyConfig<T> extends ConfigBase<T> {

        private final ObservableValue<T> value;

        /**
         * Constructor to be used with framework
         * @param name
         * @param c the annotation
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category
         * @throws IllegalStateException if the property field is not final
         */
        ReadOnlyPropertyConfig(Class<T> property_type, String name, IsConfig c, ObservableValue<T> property, String category) {
            super(property_type, name, c, property.getValue(), category);
            value = property;

            // support enumeration by delegation if property supports is
            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param category category, for generating config groups
         * @param info description, for tooltip for example
         * @param min use in combination with max if value is Number
         * @param max use in combination with min if value is Number
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(Class<T> property_type, String name, String gui_name, ObservableValue<T> property, String category, String info, double min, double max) {
            super(property_type, name, gui_name, property.getValue(), category, info, false, min, max);
            value = property;

            if(value instanceof EnumerableValue)
                valueEnumerator = EnumerableValue.class.cast(value)::enumerateValues;
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(Class<T> property_type, String name, ObservableValue<T> property) {
            this(property_type, name, name, property, "", "", Double.NaN, Double.NaN);
        }

        /**
         * @param name
         * @param property WritableValue to wrap. Mostly a {@link Property}.
         * @param info description, for tooltip for example
         * @throws IllegalStateException if the property field is not final
         */
        public ReadOnlyPropertyConfig(Class<T> property_type, String name, ObservableValue<T> property, String info) {
            this(property_type, name, name, property, "", info, Double.NaN, Double.NaN);
        }

        @Override
        public T getValue() {
            return value.getValue();
        }

        @Override
        public void setValue(T val) {
        }

        @Override
        public void applyValue() {}

        @Override
        public void applyValue(T val) {}

        public ObservableValue<T> getProperty() {
            return value;
        }

        /**
         * Equals if and only if object instance of PropertyConfig and its property
         * is the same property as property of this: property==o.property;
         * @param o
         * @return
         */
        @Override
        public boolean equals(Object o) {
            if(o==this) return true;
            return (o instanceof ReadOnlyPropertyConfig && value==((ReadOnlyPropertyConfig)o).value);
        }

        @Override
        public int hashCode() {
            return 43 * 7 + Objects.hashCode(this.value);
        }

    }
    public static class OverridablePropertyConfig<T> extends PropertyConfig<T> {
        private final boolean defaultOverride_value;

        public OverridablePropertyConfig(Class<T> property_type, String name, IsConfig c, Vo<T> property, String category) {
            super(property_type, name, c, property, category);
            Util.setField(this, "defaultValue", property.real.getValue());
            defaultOverride_value = property.override.getValue();
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, Vo<T> property) {
            this(property_type, name, name, property, "", "", true, Double.NaN, Double.NaN);
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, Vo<T> property, String info) {
            this(property_type, name, name, property, "", info, true, Double.NaN, Double.NaN);
        }

        public OverridablePropertyConfig(Class<T> property_type, String name, String gui_name, Vo<T> property, String category, String info, boolean editable, double min, double max) {
            super(property_type, name, gui_name, property, category, info, editable, min, max);
            Util.setField(this, "defaultValue", property.real.getValue());
            defaultOverride_value = property.override.getValue();
        }

        public Vo<T> getProperty() {
            return (Vo)value;
        }

        public boolean getDefaultOverrideValue() {
            return defaultOverride_value;
        }

        public void setDefaultValue() {
            getProperty().override.setValue(defaultOverride_value);
            setValue(getDefaultValue());
        }

        public void setNapplyDefaultValue() {
            setDefaultValue();
        }

        ///**
        // * Converts the value to String utilizing generic {@link Parser}.
        // * Use for serialization or filling out guis.
        // */
        //@Override
        //public String getValueS() {
        //    String prefix = value instanceof Ѵo ? "overrides:"+((Ѵo)value).override.getValue()+", " : "";
        //    return prefix + super.toS(getValue());
        //}

        /**
         * Sets value converted from string.
         * Equivalent to: return setValue(fromS(str));
         * @param str
         */
        @Override
        public void setValueS(String str) {
            getProperty().real.setValue(fromS(str));
        }

        /**
         * Inherited method from {@link StringConverter}
         * Note: this config remains intact.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public String toS(T v) {
            return "overrides:"+((Vo)value).override.getValue() + ", " + ((Vo)value).real.getValue();
        }

        /**
         * Inherited method from {@link StringConverter}
         * Note: this config remains intact.
         * <p/>
         * {@inheritDoc}
         */
        @Override
        public T fromS(String str) {
            String s = str;
            if(s.contains("overrides:true, ")) {
                getProperty().override.setValue(true);
                s = s.replace("overrides:true, ", "");
            }
            if(s.contains("overrides:false, ")) {
                getProperty().override.setValue(false);
                s = s.replace("overrides:false, ", "");
            }
            return super.fromS(s);
        }

    }
    public static final class ListConfig<T> extends ConfigBase<ObservableList<T>> {

        public final VarList<T> a;

        @SuppressWarnings("ubnchecked")
        public ListConfig(String name, IsConfig c, VarList<T> val, String category) {
            super((Class)ObservableList.class, name, c, val.getValue(), category);
            a = val;
        }

        @SuppressWarnings("ubnchecked")
        public ListConfig(String name, String gui_name, VarList<T> val, String category, String info, boolean editable, double min, double max) {
            super((Class)ObservableList.class, name, gui_name, val.getValue(), category, info, editable, min, max);
            a = val;
        }

        public ListConfig(String name, VarList<T> val) {
            this(name, name, val, "", "", true, Double.NaN, Double.NaN);
        }

        @Override
        public ObservableList<T> getValue() {
            return a.getValue();
        }

        @Override
        public void setValue(ObservableList<T> val) {}

        @Override
        public void applyValue(ObservableList<T> val) {}

        @Override
        public ObservableList<T> next() {
            return getValue();
        }

        @Override
        public ObservableList<T> previous() {
            return getValue();
        }

        @Override
        public ObservableList<T> cycle() {
            return getValue();
        }

        //************************* string converting

        @Override
        public String getValueS() {
            return toS(getValue());
        }

        @Override
        public void setValueS(String str) {
            List<T> v = fromS(str);
            a.list.setAll(v);
        }

        @Override
        public String toS(ObservableList<T> v) {
            // we convert every item of the list to string joining with ';;' delimiter
            // we convert items by converting all their fields, joining with ';' delimiter
            return v.stream().map(t ->
                a.toConfigurable.apply(t).getFields().stream().map(Config::getValueS).collect(joining(";"))
            ).collect(joining(";;"));
        }

        @Override
        public ObservableList<T> fromS(String str) {
            ObservableList<T> l = observableArrayList();
            split(str, ";;", x->x).stream()
                .map(s -> {
                    T t = a.factory.get();
                    List<Config<Object>> configs = (List)list(a.toConfigurable.apply(t).getFields());
                    List<String> vals = split(s, ";");
                    if(configs.size()==vals.size())
                         // its important to apply the values too
                        forEachBoth(configs, vals, (c,v)-> c.setNapplyValue(c.fromS(v)));

                    if(t.getClass().equals(configs.get(0).getType()))
                        return (T)configs.get(0).getValue();
                    else
                    return t;
                })
                .forEach(l::add);

            return l;
        }


    }

    public static class VarList<T> extends V<ObservableList<T>> {
        public final ObservableList<T> list;
        public final Supplier<T> factory;
        public final Ƒ1<T,Configurable<? extends Object>> toConfigurable;

        public VarList(Supplier<T> factory, Ƒ1<T,Configurable<? extends Object>> toConfigurable) {
            // construct the list and inject it as value (by calling setValue)
            super(observableArrayList());
            // remember the reference
            list = getValue();

            this.factory = factory;
            this.toConfigurable = toConfigurable;
        }

        /** This method does nothing.*/
        @Deprecated
        @Override
        public void setValue(ObservableList<T> v) {
             // guarantees that the list will be permanent value since it is
             // only null before initialization. thus we no overwriting it
            if(list==null) super.setValue(v);
        }

        /**
         * Clears list and adds items to it. Fires 1 event.
         * Fluent API - returns this object. This is to avoid multiple constructors.
         */
        public VarList<T> setItems(Collection<? extends T> items) {
            list.setAll(items);
            return this;
        }
        /** Array version of {@link #setItems(java.util.Collection)}*/
        public VarList<T> setItems(T... items) {
            list.setAll(items);
            return this;
        }


        /**
         * Adds invalidation listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onListInvalid(Consumer<ObservableList<T>> listener) {
            InvalidationListener l = o -> listener.accept((ObservableList<T>)o);
            list.addListener(l);
            return () -> list.removeListener(l);
        }

        /**
         * Adds list change listener to the list.
         * Returns subscription to dispose of the listening.
         */
        public Subscription onListChange(ListChangeListener<? super T> listener) {
            list.addListener(listener);
            return () -> list.removeListener(listener);
        }
    }
}