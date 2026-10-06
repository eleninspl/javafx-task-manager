package com.example.model;

import java.util.UUID;

/**
 * Represents a category with a unique identifier and a name.
 */
public class Category {
    private String id;
    private String name;

    /**
     * Constructs a new {@code Category} with a generated unique id and the specified name.
     *
     * @param name the name of the category
     */
    public Category(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    /**
     * Constructs a new {@code Category} with the specified id and name.
     *
     * @param id   the unique identifier for the category
     * @param name the name of the category
     */
    public Category(String id, String name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the unique identifier of the category.
     *
     * @return the category id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the name of the category.
     *
     * @return the category name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the category.
     *
     * @param name the new name for the category
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the string representation of the category, which is its name.
     *
     * @return the name of the category
     */
    @Override
    public String toString() {
        return name;
    }
}
