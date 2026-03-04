package dev.sixik.sdmtoolkit.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.datafixers.util.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.stream.Stream;

public abstract class AbstractDataTable<Key, Value> implements DataTable<Key, Value> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractDataTable.class);

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final Path dataSaveDir;

    public AbstractDataTable(Path dataSaveDir) {
        this.dataSaveDir = dataSaveDir;
    }

    protected abstract JsonObject saveDataElement(Key key, Value value);

    protected abstract Pair<Key, Value> loadDataElement(JsonObject object);

    protected abstract String getFileNameFromKey(Key key);

    public Future<?> saveDataAllAsync(ExecutorService service) {
        return service.submit(this::saveDataAll);
    }

    public void saveDataAll() {
        for (Map.Entry<Key, Value> entry : getDataMap().entrySet()) {
            saveDataToFile(entry.getKey(), entry.getValue());
        }
    }

    public void saveDataToFile(Key key, Value value) {
        final Path path = dataSaveDir.resolve(getFileNameFromKey(key) + ".json");
        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }

            try (Writer writer = Files.newBufferedWriter(path)) {
                GSON.toJson(saveDataElement(key, value), writer);
            }
        } catch (Exception e) {
            LOGGER.error("Failed save data {}", path, e);
        }
    }

    public void loadDataAll() {
        clearData();

        if (!Files.exists(dataSaveDir)) {
            try {
                Files.createDirectories(dataSaveDir);
            } catch (IOException e) {
                LOGGER.error("Can't create shops dir. {}", e.getMessage(), e);
                return;
            }
        }

        try (Stream<Path> files = Files.walk(dataSaveDir)) {
            files.filter(Files::isRegularFile)
                    .filter(p -> p.toString().endsWith(".json"))
                    .forEach(this::readFromFile);
        } catch (IOException e) {
            LOGGER.error("Failed to load shops", e);
        }
    }

    public Value loadDataFromFile(Key key) {
        final Path path = dataSaveDir.resolve(getFileNameFromKey(key) + ".json");
        return readFromFile(path);
    }

    private void loadAndPut(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            Pair<Key, Value> data = loadDataElement(JsonParser.parseReader(reader).getAsJsonObject());
            putData(data.getFirst(), data.getSecond());
        } catch (Exception e) {
            LOGGER.error("Failed load data: {}", path, e);
        }
    }

    protected Value readFromFile(Path path) {
        if (!Files.exists(path)) return null;
        try (Reader reader = Files.newBufferedReader(path)) {
            return loadDataElement(JsonParser.parseReader(reader).getAsJsonObject()).getSecond();
        } catch (Exception e) {
            LOGGER.error("Failed load data: {}", path, e);
            return null;
        }
    }
}
