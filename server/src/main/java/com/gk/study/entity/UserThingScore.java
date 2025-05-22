package com.gk.study.entity;

public class UserThingScore {
    private Long userId;
    private Long thingId;
    private Double weight;

    public UserThingScore() {}

    public UserThingScore(Long userId, Long thingId, Double weight) {
        this.userId = userId;
        this.thingId = thingId;
        this.weight = weight;
    }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public Long getThingId() { return thingId; }
    public void setThingId(Long thingId) { this.thingId = thingId; }

    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
}