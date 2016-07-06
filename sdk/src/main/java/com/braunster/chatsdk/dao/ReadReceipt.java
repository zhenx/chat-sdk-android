package com.braunster.chatsdk.dao;

import com.braunster.chatsdk.dao.entities.BMessageEntity;

import java.security.Timestamp;
import java.util.Date;

/**
 * Created by kykrueger on 2016-06-21.
 */
public class ReadReceipt {

    private String userId;
    private Long date;
    private BMessageEntity.ReadStatus enumStatus;
    private Integer status;

    public ReadReceipt(){
        // empty default constructor for Firebase deserialization
    }

    public ReadReceipt(String userId) {
        this.userId = userId;
        this.date = null;
        this.enumStatus = BMessageEntity.ReadStatus.None;
    }

    public ReadReceipt(Long userId) {
        this.userId = userId.toString();
        this.date = null;
        this.enumStatus = BMessageEntity.ReadStatus.None;
    }

    public ReadReceipt(String userId, BMessageEntity.ReadStatus status) {
        this.userId = userId;
        this.date = null;
        this.enumStatus = status;
    }

    public ReadReceipt(Long userId, BMessageEntity.ReadStatus status) {
        this.userId = userId.toString();
        this.date = null;
        this.enumStatus = status;
    }

    public void setEnumStatus(BMessageEntity.ReadStatus textStatus) {
        this.enumStatus = textStatus;
        this.status = textStatus.ordinal(); // iOS app uses integers
    }

    public BMessageEntity.ReadStatus getEnumStatus() {
        if(this.enumStatus != null) return this.enumStatus;
        else if(this.status == null){
            this.enumStatus = BMessageEntity.ReadStatus.None;
            return this.enumStatus;
        } else{
            switch (this.status) {
                case 0:
                    this.enumStatus = BMessageEntity.ReadStatus.None;
                case 1:
                    this.enumStatus = BMessageEntity.ReadStatus.Delivered;
                    break;
                case 2:
                    this.enumStatus = BMessageEntity.ReadStatus.Read;
            }
        }
        return this.enumStatus;
    }

    // only exists to force Firebase to initialize this variable (iOS compat)
    public void setStatus(Integer status){
        this.status = status;
    }
    public Integer getStatus() {return this.status;}

    public void setDate(Long date) {
        this.date = date;
    }

    public Long getDate() {
        return this.date;
    }

    public String getUserId() {
        return this.userId;
    }

    public Boolean equals(ReadReceipt compare) {
        if (this == compare)
            return true;
        if (this.hashCode() == compare.hashCode())
            return true;
        if (this.date != compare.getDate())
            return false;
        if (this.enumStatus != compare.getEnumStatus())
            return false;

        return true;
    }

    public int hashCode() {
        return userId.hashCode();
    }
}
