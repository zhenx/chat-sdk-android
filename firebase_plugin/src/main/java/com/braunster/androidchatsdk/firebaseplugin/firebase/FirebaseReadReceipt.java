package com.braunster.androidchatsdk.firebaseplugin.firebase;

/**
 * Created by kykrueger on 2016-06-21.
 */
public class FirebaseReadReceipt {

    private String userId;
    private Long date;
    private Integer status;
    public FirebaseReadReceipt(){
        // empty default constructor for Firebase deserialization
    }

    public FirebaseReadReceipt(String userId, Long date, Integer status){
        this.userId = userId;
        this.date = date;
        this.status = status;
    }


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

    public Boolean equals(FirebaseReadReceipt compare) {
        if (this == compare)
            return true;
        if (this.hashCode() == compare.hashCode())
            return true;
        if (this.date != compare.getDate())
            return false;
        if (this.status != compare.getStatus())
            return false;

        return true;
    }

    public int hashCode() {
        return userId.hashCode();
    }

}
