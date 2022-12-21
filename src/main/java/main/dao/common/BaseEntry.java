package main.dao.common;

public record BaseEntry<Data>(long requestTime, Data key, Data value) implements Entry<Data> {
    @Override
    public String toString() {
        return "{" + requestTime + " " + key + ":" + value + "}";
    }
}
