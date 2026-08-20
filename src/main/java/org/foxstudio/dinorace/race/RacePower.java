package org.foxstudio.dinorace.race;

/** Một quyền năng: tên + mô tả (từ config races.json). */
public class RacePower {

    public String name;
    public String description;
    public boolean active = false;
    public int requiredLevel = 0;

    public RacePower() {
    }
}