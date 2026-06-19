package it.zbaldi.model.interfaces;

public interface DatasetDao<T> {

    void save(T data);
}
