package org.foxstudio.dinorace.race;

import java.util.List;

/** Một chủng tộc: tên, mô tả, danh sách quyền năng (từ config races.json). */
public class RaceInfo {

    public String key;
    public String origin;
    public String name;
    public String description;
    public String color;
    public List<RacePower> powers;

    public RaceInfo() {
    }
}