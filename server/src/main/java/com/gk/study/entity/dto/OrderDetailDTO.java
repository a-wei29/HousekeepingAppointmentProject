package com.gk.study.entity.dto;

import java.io.Serializable;
import java.util.List;

public class OrderDetailDTO implements Serializable {
    // Order 中的字段
    private Long id;
    private String status;
    private String orderTime;
    private String payTime;
    private String thingId;
    private String userId;
    private String count;
    private String orderNumber;
    private String receiverAddress;
    private String receiverName;
    private String receiverPhone;
    private String remark;
    private String receiverLatitude;
    private String receiverLongitude;
    // 从 order 中扩展的信息（如重复的 title、cover、price 等可以保留 order 中已设置的）
    private String username;
    private String title;   // 订单中填写的标题
    private String cover;   // 订单中填写的封面
    private String price;   // 订单中填写的价格

    // 下面字段来自 Thing（家政服务实体），且 Order 中没有的（即非重复字段）
    private String description;        // 服务描述
    private String thingCreateTime;    // 服务发布时间（避免与订单时间冲突，改名为 thingCreateTime）
    private String score;
    private String mobile;
    private String age;
    private String sex;
    private String location;
    private String pv;
    private Integer recommendCount;
    private Integer wishCount;
    private Integer collectCount;
    private Long classificationId;
    private Double latitude;
    private Double longitude;
    private List<Long> tags;
    private String classificationName;
    private String publisherName;
    private Integer collected;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getOrderTime() {
        return orderTime;
    }

    public void setOrderTime(String orderTime) {
        this.orderTime = orderTime;
    }

    public String getPayTime() {
        return payTime;
    }

    public void setPayTime(String payTime) {
        this.payTime = payTime;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getThingId() {
        return thingId;
    }

    public void setThingId(String thingId) {
        this.thingId = thingId;
    }

    public String getCount() {
        return count;
    }

    public void setCount(String count) {
        this.count = count;
    }

    public String getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(String orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getReceiverAddress() {
        return receiverAddress;
    }

    public void setReceiverAddress(String receiverAddress) {
        this.receiverAddress = receiverAddress;
    }

    public String getReceiverName() {
        return receiverName;
    }

    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }

    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public String getReceiverLatitude() {
        return receiverLatitude;
    }

    public void setReceiverLatitude(String receiverLatitude) {
        this.receiverLatitude = receiverLatitude;
    }

    public String getReceiverLongitude() {
        return receiverLongitude;
    }

    public void setReceiverLongitude(String receiverLongitude) {
        this.receiverLongitude = receiverLongitude;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getPrice() {
        return price;
    }

    public void setPrice(String price) {
        this.price = price;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThingCreateTime() {
        return thingCreateTime;
    }

    public void setThingCreateTime(String thingCreateTime) {
        this.thingCreateTime = thingCreateTime;
    }

    public String getScore() {
        return score;
    }

    public void setScore(String score) {
        this.score = score;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getPv() {
        return pv;
    }

    public void setPv(String pv) {
        this.pv = pv;
    }

    public Integer getRecommendCount() {
        return recommendCount;
    }

    public void setRecommendCount(Integer recommendCount) {
        this.recommendCount = recommendCount;
    }

    public Integer getWishCount() {
        return wishCount;
    }

    public void setWishCount(Integer wishCount) {
        this.wishCount = wishCount;
    }

    public Integer getCollectCount() {
        return collectCount;
    }

    public void setCollectCount(Integer collectCount) {
        this.collectCount = collectCount;
    }

    public Long getClassificationId() {
        return classificationId;
    }

    public void setClassificationId(Long classificationId) {
        this.classificationId = classificationId;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public List<Long> getTags() {
        return tags;
    }

    public void setTags(List<Long> tags) {
        this.tags = tags;
    }

    public String getClassificationName() {
        return classificationName;
    }

    public void setClassificationName(String classificationName) {
        this.classificationName = classificationName;
    }

    public String getPublisherName() {
        return publisherName;
    }

    public void setPublisherName(String publisherName) {
        this.publisherName = publisherName;
    }

    public Integer getCollected() {
        return collected;
    }

    public void setCollected(Integer collected) {
        this.collected = collected;
    }

    // Getter 和 Setter 略
    // 可使用 Lombok @Data 注解简化代码，如下：
    // @Data
}