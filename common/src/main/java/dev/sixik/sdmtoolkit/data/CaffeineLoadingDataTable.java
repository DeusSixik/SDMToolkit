package dev.sixik.sdmtoolkit.data;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class CaffeineLoadingDataTable<Key, Value> extends AbstractDataTable<Key, Value> {

    protected final LoadingCache<Key, Value> cache;

    public CaffeineLoadingDataTable(Path dataSaveDir, long duration, TimeUnit unit) {
        super(dataSaveDir);
        this.cache = createCaffeine(duration, unit);
    }

    protected LoadingCache<Key, Value> createCaffeine(long duration, TimeUnit unit) {
        return Caffeine.newBuilder()
                .expireAfterAccess(duration, unit)
                .removalListener(((key, value, cause) ->
                        removalListener((Key) key, (Value) value, cause)))
                .build(this::loadDataFromFile);
    }

    protected void removalListener(Key key, Value value, RemovalCause removalCause) {
        saveDataToFile(key, value);
    }

    @Override
    public void putData(Key key, Value value) {
        cache.put(key, value);
    }

    @Override
    public @Nullable Value invalidate(Key key) {
        final Value result = cache.getIfPresent(key);
        if (result != null) {
            cache.invalidate(key);
        }
        return result;
    }

    @Override
    public @Nullable Value getData(Key key) {
        return cache.get(key);
    }

    @Override
    public Map<Key, Value> getDataMap() {
        return cache.asMap();
    }

    @Override
    public boolean hasData(Key key) {
        return cache.getIfPresent(key) != null;
    }

    @Override
    public Collection<Value> getAllData() {
        return cache.asMap().values();
    }

    @Override
    public void clearData() {
        cache.invalidateAll();
    }
}
