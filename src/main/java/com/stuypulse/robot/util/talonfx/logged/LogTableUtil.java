/************************ PROJECT ZIXI ************************/
/* Copyright (c) 2026 StuyPulse Robotics. All rights reserved.*/
/* This work is licensed under the terms of the MIT license.  */
/**************************************************************/
package com.stuypulse.robot.util.talonfx.logged;

import org.wpilib.units.Measure;
import org.wpilib.util.Color;
import org.wpilib.util.WPISerializable;
import org.wpilib.util.struct.StructSerializable;

import java.util.Arrays;
import java.util.Objects;
import org.littletonrobotics.junction.LogTable;

/**
 *
 *
 * <h2>Log Table Utility</h2>
 *
 * <p>Utility methods for reading and writing dynamically typed values to and from a {@code
 * LogTable}.
 *
 * <p>This centralizes type handling without having to duplicate the same if else logic in multiple
 * places.
 *
 * @author Faizaan (https://github.com/Faizaan-J)
 */
public final class LogTableUtil {
    private LogTableUtil() {}

    /**
     * Puts a value into a {@code LogTable} with the given key, automatically handling the type of
     * the value.
     *
     * @param table the {@code LogTable} to put the value into
     * @param key The key where the value will be stored.
     * @param value The value to store in the {@code LogTable}. If it's not a supported type, it
     *     will be converted to a string.
     */
    public static void put(LogTable table, String key, Object value) {
        Objects.requireNonNull(table, "LogTable cannot be null");
        Objects.requireNonNull(key, "Logging key cannot be null");
        switch (value) {
            case Object _ when value.getClass().isArray() -> putArray(table, key, value);
            case Measure<?> measure -> table.putMeasure(key, measure);
            case WPISerializable serializable -> table.put(key, serializable);
            case Color color -> table.put(key, color);
            case Boolean b -> table.put(key, b);
            case Double d -> table.put(key, d);
            case Integer i -> table.put(key, i);
            case Long l -> table.put(key, l);
            case String s -> table.put(key, s);
            case Enum<?> enumerator -> putEnum(table, key, enumerator);
            case Record record -> table.put(key, record);
            case null -> table.put(key, "null");
            default -> table.put(key, value.toString());
        }
    }

    /**
     * Puts an array value into a {@code LogTable}, dispatching to the corresponding array overload.
     * Falls back to a {@code String[]} for element types with no array overload such as a {@code
     * Measure} or boxed wrapper types.
     */
    private static void putArray(LogTable table, String key, Object array) {
        switch (array) {
            case boolean[] bools -> table.put(key, bools);
            case int[] ints -> table.put(key, ints);
            case long[] longs -> table.put(key, longs);
            case float[] floats -> table.put(key, floats);
            case double[] doubles -> table.put(key, doubles);
            case String[] strings -> table.put(key, strings);
            case byte[] bytes -> table.put(key, bytes);
            case Object enums when enums instanceof Enum<?>[] ->
                    putEnumArray(table, key, (Enum<?>[]) enums);
            case StructSerializable[] serializables -> table.put(key, serializables);
            case Record[] records -> table.put(key, records);
            case Object[] objects ->
                    table.put(
                            key,
                            Arrays.stream(objects).map(String::valueOf).toArray(String[]::new));
            case null, default ->
                    throw new IllegalArgumentException(
                            "Unsupported or invalid array type: "
                                    + (array == null ? "null" : array.getClass().getName()));
        }
    }

    /**
     * Gets a value from a {@code LogTable} with the given key, automatically handling the type of
     * the value based on the provided default value.
     *
     * @param <T> The type of the value.
     * @param table The {@code LogTable} to get the value from.
     * @param key The key where the valid is stored.
     * @param defaultValue The default value to return if the key does not exist in the {@code
     *     LogTable} .
     * @return The value from the {@code LogTable} if it exists, otherwise the default value.
     */
    @SuppressWarnings({"unchecked"})
    public static <T> T get(LogTable table, String key, T defaultValue) {
        Objects.requireNonNull(table, "LogTable cannot be null");
        Objects.requireNonNull(key, "Logging key cannot be null");
        return (T)
                switch (defaultValue) {
                    case Object _ when defaultValue.getClass().isArray() ->
                            getArray(table, key, defaultValue);
                    case Measure<?> measure -> table.getMeasure(key, measure);
                    case WPISerializable serializable -> table.get(key, serializable);
                    case Color color -> table.get(key, color);
                    case Boolean b -> Boolean.valueOf(table.get(key, b));
                    case Double d -> Double.valueOf(table.get(key, d));
                    case Integer i -> Integer.valueOf(table.get(key, i));
                    case Long l -> Long.valueOf(table.get(key, l));
                    case String s -> table.get(key, s);
                    case Object enumerator when enumerator instanceof Enum<?> ->
                            getEnum(table, key, (Enum<?>) enumerator);
                    case Record record -> table.get(key, record);
                    case null, default -> defaultValue;
                };
    }

    /**
     * Gets an array value from a {@code LogTable}, dispatching to the corresponding array overload.
     * Types with no array overload such as {@code Measure} or boxed wrapper types aren't
     * replayable, so the default array is returned back.
     */
    private static Object getArray(LogTable table, String key, Object defaultArray) {
        return switch (defaultArray) {
            case boolean[] bools -> table.get(key, bools);
            case int[] ints -> table.get(key, ints);
            case long[] longs -> table.get(key, longs);
            case float[] floats -> table.get(key, floats);
            case double[] doubles -> table.get(key, doubles);
            case String[] strings -> table.get(key, strings);
            case byte[] bytes -> table.get(key, bytes);
            case Object enums when enums instanceof Enum<?>[] ->
                    getEnumArray(table, key, (Enum<?>[]) enums);
            case StructSerializable[] serializables -> table.get(key, serializables);
            case Record[] records -> table.get(key, records);
            case Object[] objects ->
                    table.get(
                            key,
                            Arrays.stream(objects).map(String::valueOf).toArray(String[]::new));
            case null, default -> defaultArray;
        };
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void putEnum(LogTable table, String key, Enum<?> value) {
        table.put(key, (E) value);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E getEnum(LogTable table, String key, Enum<?> defaultValue) {
        return table.get(key, (E) defaultValue);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> void putEnumArray(
            LogTable table, String key, Enum<?>[] value) {
        table.put(key, (E[]) value);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Enum<E>> E[] getEnumArray(
            LogTable table, String key, Enum<?>[] defaultValue) {
        return table.get(key, (E[]) defaultValue);
    }

    /**
     * Returns true if the value is a supported type for logging, false otherwise.
     *
     * @param value The value to check for logging support.
     * @return true if the value is a supported type for logging, false otherwise.
     */
    public static boolean isSupportedType(Object value) {
        if (value == null) {
            return false;
        }

        if (value.getClass().isArray()) {
            return isSupportedArrayType(value);
        }

        return switch (value) {
            case Measure<?> _ -> true;
            case WPISerializable _ -> true;
            case Color _ -> true;
            case Boolean _ -> true;
            case Double _ -> true;
            case Integer _ -> true;
            case Long _ -> true;
            case String _ -> true;
            case Enum<?> _ -> true;
            case Record _ -> true;
            case null, default -> false;
        };
    }

    private static boolean isSupportedArrayType(Object value) {
        return switch (value) {
            case boolean[] _ -> true;
            case int[] _ -> true;
            case long[] _ -> true;
            case float[] _ -> true;
            case double[] _ -> true;
            case byte[] _ -> true;
            case String[] _ -> true;
            case Object _ when value instanceof Enum<?>[] _ -> true;
            case StructSerializable[] _ -> true;
            case Object[] array -> array.getClass().getComponentType().isRecord();
            case null, default -> false;
        };
    }
}
