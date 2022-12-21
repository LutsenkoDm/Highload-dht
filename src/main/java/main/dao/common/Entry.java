package main.dao.common;

public interface Entry<D> {
    D key();

    D value();
}
