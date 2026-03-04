package dev.sixik.sdmtoolkit.ecs;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Тип компонента. Выступает в роли фабрики и сериализатора.
 * Реализует паттерн Type Object, отделяя логику (сохранение/сеть) от самих данных компонента.
 */
public interface ECSComponentType<T extends ECSBaseComponent> {

    /**
     * Заглушка для пустого/неизвестного типа компонента.
     */
    ResourceLocation EMPTY = ResourceLocation.tryBuild("sdm", "null");

    /**
     * Уникальный идентификатор компонента для регистрации в реестре мода.
     */
    ResourceLocation getId();

    /**
     * Сериализует стейт компонента в JSON для сохранения на диск.
     */
    JsonObject serialize(T component);

    /**
     * Создает новый экземпляр компонента, восстанавливая его стейт из JSON.
     */
    T deserialize(JsonObject json);

    /**
     * Записывает данные компонента в буфер для отправки клиенту. <br> <br>
     * ГОТЯЧИЙ ПУТЬ: Вызывается часто при синхронизации. Пиши только минимально необходимые байты.
     */
    void toNetwork(FriendlyByteBuf buf, T component);

    /**
     * Читает данные из сетевого пакета и создает/обновляет компонент на принимающей стороне.
     */
    T fromNetwork(FriendlyByteBuf buf);

    //TODO: Добавить метод toNetworkDelta(FriendlyByteBuf buf, T component) для отправки данных при dirty
}
