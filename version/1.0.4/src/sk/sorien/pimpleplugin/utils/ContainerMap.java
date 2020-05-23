package sk.sorien.pimpleplugin.utils;

import java.util.HashMap;

public class ContainerMap<T extends ContainerMapItem> extends HashMap<String, T> {

    @Override
    public T put(String key, T value) {
        return super.put(value.getName(), value);
    }

    public T put(T value) {
        return super.put(value.getName(), value);
    }
}
