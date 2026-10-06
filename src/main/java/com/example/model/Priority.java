package com.example.model;

import java.util.UUID;

public class Priority {
    private String id;
    private String name;

    // Constructor with generated id
    public Priority(String name) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
    }

    // Constructor with specified id
    public Priority(String id, String name) {
        this.id = id;
        this.name = name;
    }

    // Get priority id
    public String getId() {
        return id;
    }

    // Get priority name
    public String getName() {
        return name;
    }

    // Set priority name
    public void setName(String name) {
        this.name = name;
    }

    // Return priority name as string
    @Override
    public String toString() {
        return name;
    }
}
