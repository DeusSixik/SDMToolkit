package dev.sixik.sdmtoolkit.nbt.exceptions;

public class NbtKeyNotFoundException extends RuntimeException {

    public NbtKeyNotFoundException(String key) {
        super("Key with name '" + key + "' not found!");
    }
}
