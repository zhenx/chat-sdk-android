/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:35 PM
 */

package com.braunster.androidchatsdk.firebaseplugin.firebase.wrappers;

import android.util.Log;

import com.braunster.androidchatsdk.firebaseplugin.firebase.FirebaseEventsManager;
import com.braunster.androidchatsdk.firebaseplugin.firebase.FirebasePaths;
import com.braunster.chatsdk.dao.BMessage;
import com.braunster.chatsdk.dao.BUser;
import com.braunster.chatsdk.dao.ReadReceipt;
import com.braunster.chatsdk.dao.core.DaoCore;
import com.braunster.chatsdk.network.BDefines;
import com.braunster.chatsdk.object.BError;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.Query;

import org.apache.commons.lang3.StringUtils;
import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import timber.log.Timber;

public class BMessageWrapper extends EntityWrapper<BMessage> {

    private final String TAG = this.getClass().getSimpleName();
    private static boolean DEBUG = true;
    private ChildEventListener readReceiptListener;
    
    public static BMessageWrapper initWithEntityId(String entityId){
        return new BMessageWrapper((BMessage) DaoCore.fetchOrCreateEntityWithEntityID(BMessage.class, entityId));
    }

    public static BMessageWrapper initWithModel(BMessage model){
        return new BMessageWrapper(model);
    }

    public static BMessageWrapper initWithSnapshot(DataSnapshot snapshot){
        return new BMessageWrapper(snapshot);
    }


    public BMessageWrapper(BMessage model){
        this.model = model;
        this.entityId = model.getEntityID();
    }

    public BMessageWrapper(DataSnapshot snapshot){
        this.model = DaoCore.fetchOrCreateEntityWithEntityID(BMessage.class, snapshot.getKey());
        this.entityId = snapshot.getKey();

        deserialize(snapshot);
    }

    
    
    Map<String, Object> serialize(){
        Map<String, Object> values = new HashMap<String, Object>();

        values.put(BDefines.Keys.BPayload, model.getText());
        values.put(BDefines.Keys.BDate, ServerValue.TIMESTAMP);
        values.put(BDefines.Keys.BType, model.getType());
        values.put(BDefines.Keys.BUserFirebaseId, model.getBUserSender().getEntityID());

        return values;
    }

    @SuppressWarnings("all") void deserialize(DataSnapshot snapshot){
        Map<String, Object> value = (Map<String, Object>) snapshot.getValue();
        if (DEBUG) Timber.v("deserialize, Value: %s", value);
        if (value == null) return;
        if (value.containsKey(BDefines.Keys.BPayload) && !value.get(BDefines.Keys.BPayload).equals(""))
        {
            model.setText((String) value.get(BDefines.Keys.BPayload));
        }

        if (value.containsKey(BDefines.Keys.BType) && !value.get(BDefines.Keys.BType).equals(""))
        {
            if (value.get(BDefines.Keys.BType) instanceof Integer)
                model.setType((Integer) value.get(BDefines.Keys.BType));
            else
                if (value.get(BDefines.Keys.BType) instanceof Long)
                    model.setType( ((Long) value.get(BDefines.Keys.BType)).intValue() );
        }

        if (value.containsKey(BDefines.Keys.BDate) && !value.get(BDefines.Keys.BDate).equals(""))
            model.setDate( new Date( (Long) value.get(BDefines.Keys.BDate) ) );

        if (value.containsKey(BDefines.Keys.BUserFirebaseId) && !value.get(BDefines.Keys.BUserFirebaseId).equals(""))
        {
            String userEntityId = (String) value.get(BDefines.Keys.BUserFirebaseId);
            BUser user = DaoCore.fetchEntityWithEntityID(BUser.class, userEntityId);

            // If there is no user saved in the db for this entity id,
            // Create a new one and do a once on it to get all the details.
            if (user == null)
            {
                user = DaoCore.fetchOrCreateEntityWithEntityID(BUser.class, userEntityId);

                BUserWrapper.initWithModel(user).once();
            }

            model.setBUserSender(user);
        }
        if (value.containsKey(BDefines.Keys.BRead) && !value.get(BDefines.Keys.BRead).equals(""))
        {
            Map<String,Object> readerHashMap = new HashMap<String,Object>();
            // loop through children of 'read' and rebuild the ReadReceipts hashmap
            for(DataSnapshot snap : snapshot.child(BDefines.Keys.BRead).getChildren()){
                ReadReceipt receipt = snap.getValue(ReadReceipt.class);
                readerHashMap.put(snap.getKey(),receipt);
            }
            model.setReaderHashMap(readerHashMap);
        }
                
        // Updating the db
        DaoCore.updateEntity(model);
    }

    public Promise<BMessage, BError, BMessage>  push(){
        if (DEBUG) Timber.v("push");

        final Deferred<BMessage, BError, BMessage> deferred = new DeferredObject<>();
        
        // Getting the message ref. Will be created if not exist.
        DatabaseReference ref = ref();
        model.setEntityID(ref.getKey());

        DaoCore.updateEntity(model);

        ref.setValue(serialize(), ServerValue.TIMESTAMP, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(DatabaseError firebaseError, DatabaseReference firebase) {

                if (DEBUG) Timber.v("push message, onDone");

                if (firebaseError == null) {
                    deferred.resolve(BMessageWrapper.this.model);
                } else {
                    deferred.reject(getFirebaseError(firebaseError));
                }
            }
        });
        
        return deferred.promise();
    }
    
    public Promise<BMessage, BError, BMessage> send(){
        if (DEBUG) Timber.v("send");
        
        if (model.getBThreadOwner() != null)
        {
            return push();
        }else
        {
            final Deferred<BMessage, BError, BMessage> deferred = new DeferredObject<>();
            deferred.reject(null);
            return deferred.promise();
        }           
    }
    
    /**
     * The message model will be updated after this call.
     **/
    public void setDelivered(int delivered){
        model.setDelivered(delivered);
    }

    public void readReceiptsOn(){
        if(getModel().getCommonReadStatus() != ReadReceipt.ReadStatus.Read){
            ref().child(BDefines.Keys.BRead).addChildEventListener(readReceiptListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    String userId = dataSnapshot.getKey();
                    ReadReceipt receipt = dataSnapshot.getValue(ReadReceipt.class);
                    model.setUserReadReceipt(userId,receipt);
                    FirebaseEventsManager.getInstance().onMessageReceived(model);

                    if(model.getCommonReadStatus() == ReadReceipt.ReadStatus.Read){
                        readReceiptsOff();
                    }
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    String userId = dataSnapshot.getKey();
                    ReadReceipt receipt = dataSnapshot.getValue(ReadReceipt.class);
                    model.setUserReadReceipt(userId,receipt);
                    FirebaseEventsManager.getInstance().onMessageReceived(model);

                    if(model.getCommonReadStatus() == ReadReceipt.ReadStatus.Read){
                        readReceiptsOff();
                    }
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                // children are not expected to be removed
                    if(DEBUG) Log.d(TAG,"ReadReceiptsOn: Unexpected Action - " +
                            "\n    child of readReceipts was removed");
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                    // children are not expected to move
                    if(DEBUG) Log.d(TAG,"ReadReceiptsOn: Unexpected Action - " +
                            "\n    child of readReceipts was moved");
                }

                @Override
                public void onCancelled(DatabaseError firebaseError) {
                    // children are not expected to be removed
                    if(DEBUG) Log.d(TAG,"ReadReceiptsOn: FirebaseError" +
                            "\n    msg: " + firebaseError.getMessage()  +
                            "\n    deets:" +firebaseError.getDetails());
                }
            });


        }

    }

    public void readReceiptsOff(){
        ref().child(BDefines.Keys.BRead).removeEventListener(readReceiptListener);
    }

    public void setReadReceipt(ReadReceipt.ReadStatus status){
        final BUser currentUser = getNetworkAdapter().currentUserModel();
        final String receiptId = currentUser.getEntityID();
        final ReadReceipt.ReadStatus newStatus = status;

        if(DEBUG) {
            Log.d(TAG, "setReadReceipt: " +
                    "\n    Querying ReadReceipt on message: " + model.getEntityID() +
                    "\n    for user: " + currentUser.getEntityID());
        }
        Query readReceiptQuery = ref().child(BDefines.Keys.BRead).orderByChild(receiptId);
        readReceiptQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(dataSnapshot.hasChild(receiptId)) {
                    ReadReceipt oldReceipt = dataSnapshot.child(receiptId).getValue(ReadReceipt.class);
                    // don't overwrite if exists and is already set to highest state
                    if (oldReceipt != null) {
                        if (oldReceipt.getReadStatusActual() == ReadReceipt.ReadStatus.Read) {
                            if (DEBUG) {
                                Log.d(TAG, "BMessageWrapper: Message is already set to highest state" +
                                        "\n    Aborting to avoid overwrite");
                            }
                            return;
                        }
                        if (oldReceipt.getReadStatusActual() == newStatus) {
                            if (DEBUG) {
                                Log.d(TAG, "BMessageWrapper: Message is already set to this state" +
                                        "\n    Aborting to avoid overwrite");
                            }
                            return;
                        }
                    }
                }
                if(DEBUG) Log.d(TAG, "BMessageWrapper: Writing new ReadReceipt to message");
                model.setUserReadReceipt(currentUser,newStatus);
                ReadReceipt newReceipt = model.getUserReadReceipt(currentUser);
                ref().child(BDefines.Keys.BRead).child(receiptId).setValue(newReceipt);
            }

            @Override
            public void onCancelled(DatabaseError firebaseError) {
                if(DEBUG) Log.d(TAG, "BMessageWrapper: ERROR OCURRED WITH FIREBASE");
            }
        });

    }

    public void initReadReceiptList(){
        model.initReaderList();
        ref().child(BDefines.Keys.BRead).setValue(model.getReaderHashMap());
    }
    
    private DatabaseReference ref(){
        if (StringUtils.isNotEmpty(model.getEntityID()))
        {
            return FirebasePaths.threadMessagesRef(model.getBThreadOwner().getEntityID()).child(model.getEntityID());
        }
        else
        {
            return FirebasePaths.threadMessagesRef(model.getBThreadOwner().getEntityID()).push();
        }
    }

}
