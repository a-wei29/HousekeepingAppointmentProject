package com.gk.study.entity;

public class ItemSim {
    private Long thingId;
    private Long neighborId;
    private Double similarity;

    public ItemSim() {}

    public ItemSim(Long thingId, Long neighborId, Double similarity) {
        this.thingId     = thingId;
        this.neighborId  = neighborId;
        this.similarity  = similarity;
    }

    public Long getThingId() {
        return thingId;
    }
    public void setThingId(Long thingId) {
        this.thingId = thingId;
    }

    public Long getNeighborId() {
        return neighborId;
    }
    public void setNeighborId(Long neighborId) {
        this.neighborId = neighborId;
    }

    public Double getSimilarity() {
        return similarity;
    }
    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }
}