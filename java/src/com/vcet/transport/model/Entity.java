package com.vcet.transport.model;

/** Abstract parent of all persistent objects (inheritance + abstract class). */
public abstract class Entity {
    protected int id;

    protected Entity(int id) { this.id = id; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    /** One-line human readable description. */
    public abstract String describe();

    @Override public String toString() { return describe(); }
}
