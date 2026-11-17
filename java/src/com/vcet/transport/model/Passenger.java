package com.vcet.transport.model;

public class Passenger extends Entity {
    private final String name;
    private final String phone;
    private final String email;   // optional
    private final Integer age;    // optional

    public Passenger(int id, String name, String phone, String email, Integer age) {
        super(id);
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.age = age;
    }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public Integer getAge() { return age; }

    @Override public String describe() {
        return String.format("Passenger #%d | %s | %s | %s | %s", id, name, phone,
                email == null ? "-" : email, age == null ? "-" : age.toString());
    }
}
