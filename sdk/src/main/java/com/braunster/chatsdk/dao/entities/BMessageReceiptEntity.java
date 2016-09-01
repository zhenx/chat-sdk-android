package com.braunster.chatsdk.dao.entities;

/**
 * Created by kykrueger on 2016-09-01.
 */

public abstract class BMessageReceiptEntity extends Entity {

    public static class ReadStatus{
        public static final int none = 0;
        public static final int delivered = 1;
        public static final int read = 2;
    }
}
