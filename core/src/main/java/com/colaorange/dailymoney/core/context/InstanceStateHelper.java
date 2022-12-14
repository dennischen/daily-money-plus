package com.colaorange.dailymoney.core.context;

import android.app.Activity;
import android.os.Bundle;
import android.util.LruCache;

import com.colaorange.dailymoney.core.data.Book;
import com.colaorange.dailymoney.core.util.Logger;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;


/**
 * @author Dennis
 */
public class InstanceStateHelper {

    static private LruCache<Class, List<Field>> fieldsCache = new LruCache<>(20);
    static private List<Field> empty = new ArrayList<>(0);

    private Object instance;

    public InstanceStateHelper(Object instance) {
        this.instance = instance;
    }

    protected void onRestore(Bundle state) {
        if (state == null) {
            return;
        }
        try {
            int idx = 0;
            Class clz = instance.getClass();
            while (clz != null) {
                InstanceState ann = (InstanceState) clz.getAnnotation(InstanceState.class);
                if (ann != null) {
                    List<Field> fields = scanFields(clz);
                    for (Field f : fields) {
                        restoreField(++idx, f, state);
                    }
                    if (ann.stopLookup()) {
                        break;
                    }
                }
                clz = clz.getSuperclass();
            }
        } catch (Exception x) {
            Logger.e(x.getMessage(), x);
        }
    }

    public void onBackup(Bundle state) {
        if (state == null) {
            return;
        }
        try {
            int idx = 0;
            Class clz = instance.getClass();
            while (clz != null) {
                InstanceState ann = (InstanceState) clz.getAnnotation(InstanceState.class);
                if (ann != null) {
                    List<Field> fields = scanFields(clz);
                    for (Field f : fields) {
                        saveField(++idx, f, state);
                    }
                    if (ann.stopLookup()) {
                        break;
                    }
                }
                clz = clz.getSuperclass();
            }
        } catch (Exception x) {
            Logger.e(x.getMessage(), x);
        }
    }

    private void saveField(int i, Field f, Bundle state) throws IllegalAccessException {
        String key = "iState-" + i;
        Class valClz = f.getType();
        Object val = f.get(instance);
        if (val == null) {
            Logger.d(">>> skip to save null filed {}, {}, {}", key, f.getName(), val);
        } else if (Boolean.class.isAssignableFrom(valClz) || boolean.class.isAssignableFrom(valClz)) {
            state.putBoolean(key, (Boolean) val);
            Logger.d(">>> save Boolean filed {}, {}, {}", key, f.getName(), val);
        } else if (String.class.isAssignableFrom(valClz)) {
            state.putString(key, (String) val);
            Logger.d(">>> save String filed {}, {}, {}", key, f.getName(), val);
        } else if (Byte.class.isAssignableFrom(valClz) || byte.class.isAssignableFrom(valClz)) {
            state.putByte(key, (Byte) val);
            Logger.d(">>> save Byte filed {}, {}, {}", key, f.getName(), val);
        } else if (Long.class.isAssignableFrom(valClz) || long.class.isAssignableFrom(valClz)) {
            state.putLong(key, (Long) val);
            Logger.d(">>> save Long filed {}, {}, {}", key, f.getName(), val);
        } else if (Integer.class.isAssignableFrom(valClz) || int.class.isAssignableFrom(valClz)) {
            state.putInt(key, (Integer) val);
            Logger.d(">>> save Integer filed {}, {}, {}", key, f.getName(), val);
        } else if (Double.class.isAssignableFrom(valClz) || double.class.isAssignableFrom(valClz)) {
            state.putDouble(key, (Double) val);
            Logger.d(">>> save Double filed {}, {}, {}", key, f.getName(), val);
        } else if (Float.class.isAssignableFrom(valClz) || float.class.isAssignableFrom(valClz)) {
            state.putFloat(key, (Float) val);
            Logger.d(">>> save Float filed {}, {}, {}", key, f.getName(), val);
        } else if (Character.class.isAssignableFrom(valClz) || char.class.isAssignableFrom(valClz)) {
            state.putChar(key, (Character) val);
            Logger.d(">>> save Float filed {}, {}, {}", key, f.getName(), val);
        } else {
            Logger.w(">>> unsupported save filed {}, {}, valClz, {}", key, f.getName(), valClz, val);
        }
    }

    private void restoreField(int i, Field f, Bundle state) throws IllegalAccessException {
        String key = "iState-" + i;
        Class valClz = f.getType();
        boolean isPrimitive = valClz.isPrimitive();
        Object val;
        if (Boolean.class.isAssignableFrom(valClz) || boolean.class.isAssignableFrom(valClz)) {
            val = state.getBoolean(key);
            if (isPrimitive && val == null) {
                val = Boolean.FALSE;
            }
            Logger.d(">>> restore Boolean filed {}, {}, {}", key, f.getName(), val);
        } else if (String.class.isAssignableFrom(valClz)) {
            val = state.getString(key);
            Logger.d(">>> restore String filed {}, {}, {}", key, f.getName(), val);
        } else if (Byte.class.isAssignableFrom(valClz) || byte.class.isAssignableFrom(valClz)) {
            val = state.getByte(key);
            if (isPrimitive && val == null) {
                val = 0x0B;
            }
            Logger.d(">>> restore Byte filed {}, {}, {}", key, f.getName(), val);
        } else if (Long.class.isAssignableFrom(valClz) || long.class.isAssignableFrom(valClz)) {
            val = state.getLong(key);
            if (isPrimitive && val == null) {
                val = 0L;
            }
            Logger.d(">>> restore Long filed {}, {}, {}", key, f.getName(), val);
        } else if (Integer.class.isAssignableFrom(valClz) || int.class.isAssignableFrom(valClz)) {
            val = state.getInt(key);
            if (isPrimitive && val == null) {
                val = 0;
            }
            Logger.d(">>> restore Integer filed {}, {}, {}", key, f.getName(), val);
        } else if (Double.class.isAssignableFrom(valClz) || double.class.isAssignableFrom(valClz)) {
            val = state.getDouble(key);
            if (isPrimitive && val == null) {
                val = 0F;
            }
            Logger.d(">>> restore Double filed {}, {}, {}", key, f.getName(), val);
        } else if (Float.class.isAssignableFrom(valClz) || float.class.isAssignableFrom(valClz)) {
            val = state.getFloat(key);
            if (isPrimitive && val == null) {
                val = 0D;
            }
            Logger.d(">>> restore Float filed {}, {}, {}", key, f.getName(), val);
        } else if (Character.class.isAssignableFrom(valClz) || char.class.isAssignableFrom(valClz)) {
            val = state.getChar(key);
            if (isPrimitive && val == null) {
                val = (char) 0;
            }
            Logger.d(">>> restore Float filed {}, {}, {}", key, f.getName(), val);
        } else {
            Logger.w(">>> unsupported restore filed {}, {}, {}", key, f.getName(), valClz);
            return;
        }
        f.set(instance, val);
    }

    static synchronized private List<Field> scanFields(Class clz) {
        List<java.lang.reflect.Field> fields = fieldsCache.get(clz);
        if (fields == null) {
            synchronized (InstanceStateHelper.class) {
                fields = fieldsCache.get(clz);
                if (fields != null) {
                    return fields;
                }
                for (Field f : clz.getDeclaredFields()) {
                    if (fields == null) {
                        fields = new LinkedList<>();
                    }
                    if (f.getAnnotation(InstanceState.class) != null) {
                        if (!f.isAccessible()) {
                            f.setAccessible(true);
                        }
                        fields.add(f);
                        Logger.d("Instance {}, field {}:{}", clz.getSimpleName(), f.getName(), f.getType());
                    }
                }

                //just in case the order is different between save/restore.
                if (fields != null && fields.size() > 1) {
                    Collections.sort(fields, new Comparator<Field>() {
                        @Override
                        public int compare(Field o1, Field o2) {
                            return o1.getName().compareTo(o2.getName());
                        }
                    });
                }

                fields = fields == null ? empty : fields;

                fieldsCache.put(clz, fields);
            }
        }

        return fields;
    }

}
