package dev.sixik.sdmtoolkit.ecs;

/**
 * Указывает физическую или логическую сторону, к которой привязан компонент.
 */
public enum ECSSide {
    Server,
    Client,
    Both;

    /**
     * @return true, если компонент должен передаваться по сети (от сервера к клиенту).
     */
    public final boolean shouldSync() {
        return this == Both;
    }
}
