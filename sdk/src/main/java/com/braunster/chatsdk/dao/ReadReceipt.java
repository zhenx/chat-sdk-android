package com.braunster.chatsdk.dao;

import com.google.firebase.database.Exclude;

/**
 * Created by kykrueger on 2016-06-21.
 */
public class ReadReceipt {
    public enum ReadStatus{
        None, Delivered, Read
    }

    private String userId;
    private Long date;
    private ReadStatus readStatus;
    private Integer status;

    public ReadReceipt(){
        // empty default constructor for Firebase deserialization
    }

    public ReadReceipt(String userId) {
        this.userId = userId;
        this.date = null;
        this.readStatus = ReadStatus.None;
    }

    public ReadReceipt(Long userId) {
        this.userId = userId.toString();
        this.date = null;
        this.readStatus = ReadStatus.None;
    }

    public ReadReceipt(String userId, ReadStatus status) {
        this.userId = userId;
        this.date = null;
        this.readStatus = status;
    }

    public ReadReceipt(Long userId, ReadStatus status) {
        this.userId = userId.toString();
        this.date = null;
        this.readStatus = status;
    }

    @Exclude
    public void setReadStatusActual(ReadStatus textStatus) {
        this.readStatus = textStatus;
        this.status = textStatus.ordinal(); // iOS app uses integers
    }

    @Exclude
    public ReadStatus getReadStatusActual() {
        if(this.readStatus != null) return this.readStatus;
        else if(this.status == null){
            this.readStatus = ReadStatus.None;
            return this.readStatus;
        } else{
            switch (this.status) {
                case 0:
                    this.readStatus = ReadStatus.None;
                case 1:
                    this.readStatus = ReadStatus.Delivered;
                    break;
                case 2:
                    this.readStatus = ReadStatus.Read;
            }
        }
        return this.readStatus;
    }

    /***
     * Not recommended to call, Only here to satisfy an issue with firebase
     * @return
     */
    public void setReadStatus(String textStatusStr) {
        // Get enum from string fix for firebase removing jackson
        if (textStatusStr == null) {
            this.readStatus = null;
        } else {
            this.readStatus = ReadStatus.valueOf(textStatusStr);
        }
    }

    /***
     * Not recommended to call, Only here to satisfy an issue with firebase
     * @return
     */
    public String getReadStatus() {
        // Convert enum to string
        if (readStatus == null) {
            return null;
        } else {
            return readStatus.name();
        }
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
        if (this.readStatus != compare.getReadStatusActual())
            return false;

        return true;
    }

    public int hashCode() {
        return userId.hashCode();
    }

}
