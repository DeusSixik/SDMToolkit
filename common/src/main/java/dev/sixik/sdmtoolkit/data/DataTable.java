package dev.sixik.sdmtoolkit.data;

import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Map;

public interface DataTable<Key, Value>{

    void putData(Key key, Value value);

    @Nullable Value invalidate(Key key);

    @Nullable Value getData(Key key);

    Map<Key, Value> getDataMap();

    boolean hasData(Key key);

    Collection<Value> getAllData();

    void clearData();
}
