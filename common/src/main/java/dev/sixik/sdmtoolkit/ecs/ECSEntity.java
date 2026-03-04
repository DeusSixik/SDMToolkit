package dev.sixik.sdmtoolkit.ecs;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;

import java.util.*;

/**
 * Базовый класс для ECS-архитектуры. <br>
 * Содержит базовую логику для хранения, кэширования и управления компонентами.
 *
 * @param <T_COMPONENT> базовый тип компонентов, которые хранит сущность
 */
public abstract class ECSEntity<T_COMPONENT extends ECSBaseComponent> {

    private boolean initialized = false;

    /**
     * Список прикрепленных компонентов.
     */
    private final List<T_COMPONENT> components = createComponentList();

    /**
     * Кэш для быстрого O(1) доступа к отфильтрованным компонентам по их классу.
     */
    private final Map<Class<?>, List<T_COMPONENT>> componentCache = createComponentCacheMap();

    /**
     * Создаёт массив в котором хранит компоненты {@link ECSEntity#components}
     */
    protected List<T_COMPONENT> createComponentList() {
        return new ArrayList<>();
    }

    /**
     * Создаёт мапу в котором храняться закежированные компоненты {@link ECSEntity#componentCache}
     */
    protected Map<Class<?>, List<T_COMPONENT>> createComponentCacheMap() {
        return new IdentityHashMap<>();
    }

    /**
     * Создаёт массив в котором храняться закешированные компоненты в {@link ECSEntity#componentCache}, создаёться он при
     * вызове {@link ECSEntity#getComponents(Class)} при {@link Map#computeIfAbsent}
     */
    protected List<T_COMPONENT> createComponentCacheList() {
        return new ArrayList<>();
    }

    /**
     * Инициализирует все текущие компоненты сущности.
     */
    private void initComponents() {
        final List<T_COMPONENT> array = components;
        for (int i = 0; i < array.size(); i++) {
            array.get(i).init();
        }
        initialized = true;
    }

    /**
     * Добавляет компонент в сущность с учетом его приоритета.
     *
     * @param component компонент для добавления
     * @return добавленный компонент
     */
    public final <COMPONENT extends T_COMPONENT> COMPONENT addComponent(COMPONENT component) {
        onAddComponentPre(component);
        int i = 0;
        while (i < components.size() && components.get(i).priority() <= component.priority()) {
            i++;
        }
        components.add(i, component);

        componentCache.clear();

        if(initialized)
            component.init();

        onAddComponentPost(component);
        return component;
    }

    /**
     * Хук, вызываемый перед фактическим добавлением компонента.
     */
    protected <COMPONENT extends T_COMPONENT> void onAddComponentPre(COMPONENT component) { }

    /**
     * Хук, вызываемый после успешного добавления и инициализации компонента.
     */
    protected <COMPONENT extends T_COMPONENT> void onAddComponentPost(COMPONENT component) { }

    /**
     * Проверяет, существует ли компонент указанного класса.
     */
    public final boolean hasComponent(Class<?> type) {
        List<T_COMPONENT> cached = componentCache.get(type);
        if (cached != null) {
            return !cached.isEmpty();
        }

        for (int i = 0; i < components.size(); i++) {
            if (type.isInstance(components.get(i))) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return неизменяемый список всех компонентов
     */
    public final List<T_COMPONENT> getComponents() {
        return Collections.unmodifiableList(components);
    }

    /**
     * Возвращает список компонентов указанного типа (с ленивым кэшированием).
     */
    @SuppressWarnings("unchecked")
    public final <TYPE> List<TYPE> getComponents(Class<TYPE> type) {
        /*
            Ленивое кэширование. Фильтруем только при первом запросе конкретного типа.
         */
        return (List<TYPE>) componentCache.computeIfAbsent(type, k -> {
            List<T_COMPONENT> filtered = createComponentCacheList();
            for (int i = 0; i < components.size(); i++) {
                T_COMPONENT c = components.get(i);
                if (k.isInstance(c)) {
                    filtered.add(c);
                }
            }

            /*
                Используем emptyList() для экономии памяти, если компонентов нет
             */
            return filtered.isEmpty() ? Collections.emptyList() : Collections.unmodifiableList(filtered);
        });
    }

    /**
     * @return первый найденный компонент указанного типа, завернутый в {@link Optional}
     */
    public final <COMPONENT> Optional<COMPONENT> getComponent(Class<COMPONENT> type) {
        List<COMPONENT> list = getComponents(type);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * Сериализует все компоненты в JSON-массив.
     */
    public final JsonArray serializeComponents() {
        JsonArray compArray = new JsonArray();

        final List<T_COMPONENT> refArray = components;

        for (int i = 0; i < refArray.size(); i++) {
            compArray.add(serializeComponent(refArray.get(i)));
        }

        return compArray;
    }

    /**
     * Сериализует конкретный компонент в JSON.
     */
    protected abstract JsonElement serializeComponent(T_COMPONENT component);

    /**
     * Десериализует компоненты из переданного JSON-элемента.
     */
    public final void deserializeComponents(JsonElement element) {
        deserializeComponents(element.getAsJsonArray());
    }

    /**
     * Очищает текущие компоненты и загружает новые из JSON-массива.
     */
    public final void deserializeComponents(JsonArray array) {
        components.clear();
        componentCache.clear();

        for (JsonElement compJson : array) {
            components.add(deserializeComponent(compJson.getAsJsonObject()));
        }

        initializeServerOnlyComponents();
    }

    /**
     * Десериализует конкретный компонент из JSON-объекта.
     */
    protected abstract T_COMPONENT deserializeComponent(JsonObject json);

    /**
     * Записывает компоненты в буфер для отправки по сети. <br>
     * Отправляет только те компоненты, для которых {@code getSide().shouldSync()} равно {@code true}.
     */
    public final void serializeComponentsNetwork(FriendlyByteBuf buf) {
        int syncCount = 0;
        for (int i = 0; i < components.size(); i++) {
            if (components.get(i).getSide().shouldSync()) syncCount++;
        }

        buf.writeVarInt(syncCount);
        for (int i = 0; i < components.size(); i++) {
            T_COMPONENT component = components.get(i);
            if (component.getSide().shouldSync()) {
                writeComponentToNetwork(buf, component);
            }
        }
    }


    protected abstract void writeComponentToNetwork(FriendlyByteBuf buf, T_COMPONENT component);

    /**
     * Читает и инициализирует компоненты из сетевого буфера.
     */
    public final void deserializeComponentsNetwork(FriendlyByteBuf buf) {
        int count = buf.readVarInt();

        components.clear();
        componentCache.clear();
        for (int i = 0; i < count; i++) {
            addComponent(readComponentFromNetwork(buf));
        }

        initializeClientOnlyComponents();
    }

    /**
     * Метод для реализации загрузки компонентов через сеть используя {@link FriendlyByteBuf}
     */
    protected abstract T_COMPONENT readComponentFromNetwork(FriendlyByteBuf buf);

    protected final void initializeClientOnlyComponents() {
        initialized = false;
        customInitializeClientOnlyComponents();
        initComponents();
    }

    /**
     * Позволяет добавить компоненты по умолчанию которые будут только на стороне клиента
     */
    protected void customInitializeClientOnlyComponents() { }

    public final void initializeServerOnlyComponents() {
        initialized = false;
        customInitializeServerOnlyComponents();
        initComponents();
    }

    /**
     * Позволяет добавить компоненты по умолчанию которые будут только на стороне сервера
     */
    protected void customInitializeServerOnlyComponents() { }
}
