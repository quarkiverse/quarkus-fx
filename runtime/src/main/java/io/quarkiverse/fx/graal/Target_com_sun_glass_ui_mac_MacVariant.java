package io.quarkiverse.fx.graal;

import java.util.Arrays;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * {@code MacVariant.toString()} builds its result with {@code Object v = getValue(); ... v += ...}. javac compiles this
 * compound assignment to a string concatenation returning {@code Object}, which the string builder outlining of
 * GraalVM (Oracle GraalVM, GraalVM Community 25.4+) cannot handle : the native build fails with "failed building
 * outlined SB method" (https://github.com/oracle/graal/issues/14533).
 * <p>
 * The method is reachable in every macOS native executable : {@code MacAccessible} creates {@code MacVariant} instances,
 * and the {@code toString()} of all instantiated classes is reachable. Same result, with a {@code String} concatenation.
 */
@TargetClass(className = "com.sun.glass.ui.mac.MacVariant", onlyWith = Target_com_sun_glass_ui_mac_MacVariant.IsPresent.class)
final class Target_com_sun_glass_ui_mac_MacVariant {

    @Alias
    int type;

    @Alias
    long[] longArray;

    @Alias
    Target_com_sun_glass_ui_mac_MacVariant[] variantArray;

    @Alias
    native Object getValue();

    @Substitute
    @Override
    public String toString() {
        Object value = getValue();
        String v;
        switch (type) {
            case Type.NS_ARRAY_ID:
                v = Arrays.toString((long[]) value);
                break;
            case Type.NS_ARRAY_INT:
            case Type.NS_VALUE_RANGE:
                v = Arrays.toString((int[]) value);
                break;
            case Type.NS_ATTRIBUTED_STRING:
                v = value + Arrays.toString(variantArray);
                break;
            case Type.NS_DICTIONARY:
                v = "keys: " + Arrays.toString(longArray) + " values: " + Arrays.toString(variantArray);
                break;
            default:
                v = String.valueOf(value);
        }
        return "MacVariant type: " + type + " value " + v;
    }

    /**
     * The MacVariant types used by toString() : a substitution class can only declare fields that alias, inject or
     * delete fields of its target class.
     */
    private static final class Type {
        static final int NS_ARRAY_ID = 1;
        static final int NS_ARRAY_INT = 3;
        static final int NS_ATTRIBUTED_STRING = 5;
        static final int NS_DICTIONARY = 8;
        static final int NS_VALUE_RANGE = 18;
    }

    /**
     * The Glass mac classes are only in the macOS JavaFX jars.
     */
    static final class IsPresent implements BooleanSupplier {

        @Override
        public boolean getAsBoolean() {
            try {
                Class.forName("com.sun.glass.ui.mac.MacVariant", false, Thread.currentThread().getContextClassLoader());
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}
