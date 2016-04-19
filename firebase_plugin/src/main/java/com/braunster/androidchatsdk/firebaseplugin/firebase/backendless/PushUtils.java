/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:35 PM
 */

package com.braunster.androidchatsdk.firebaseplugin.firebase.backendless;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.backendless.messaging.DeliveryOptions;
import com.backendless.messaging.MessageStatus;
import com.backendless.messaging.PublishOptions;
import com.backendless.messaging.PushBroadcastMask;
import com.backendless.messaging.PushPolicyEnum;
import com.braunster.chatsdk.Utils.Debug;
import com.braunster.chatsdk.dao.BMessage;
import com.parse.ParseInstallation;
import com.parse.ParsePush;
import com.parse.ParseQuery;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collection;

import timber.log.Timber;

import static com.braunster.chatsdk.dao.entities.BMessageEntity.Type.IMAGE;
import static com.braunster.chatsdk.dao.entities.BMessageEntity.Type.LOCATION;

public class PushUtils {

    private static final String TAG = PushUtils.class.getSimpleName();
    private static final boolean DEBUG = Debug.PushUtils;

    static final String ACTION = "action";
    static final String ALERT = "alert";
    static final String BADGE = "badge", INCREMENT = "Increment";
    static final String CONTENT = "text";
    static final String MESSAGE_ENTITY_ID = "message_entity_id";
    static final String THREAD_ENTITY_ID = "thread_entity_id";
    static final String MESSAGE_DATE ="message_date";
    static final String MESSAGE_SENDER_ENTITY_ID ="message_sender_entity_id";
    static final String MESSAGE_TYPE = "message_type";
    static final String MESSAGE_PAYLOAD= "message_payload";

    static final String SOUND = "sound";
    static final String Default = "default";

    static final String DeviceType = "deviceType";
    static final String iOS = "ios";
    static final String Android = "android";

    static final String Channels = "channels";
    static final String Channel = "channel";


    public static void sendMessage(BMessage message, Collection<String> channels){
        if (DEBUG) Timber.v("pushutils sendmessage");
        String messageText = message.getText();

        if (message.getType() == LOCATION)
            messageText = "Location Message";
        else if (message.getType() == IMAGE)
            messageText = "Picture Message";

        String sender = message.getBUserSender().getMetaName();
        String fullText = sender + " " + messageText;

        JSONObject data = new JSONObject();
        try {
            data.put(ACTION, ChatSDKReceiver.ACTION_MESSAGE);

            data.put(CONTENT, fullText);
            data.put(MESSAGE_ENTITY_ID, message.getEntityID());
            data.put(THREAD_ENTITY_ID, message.getBThreadOwner().getEntityID());
            data.put(MESSAGE_DATE, message.getDate().getTime());
            data.put(MESSAGE_SENDER_ENTITY_ID, message.getBUserSender().getEntityID());
            data.put(MESSAGE_TYPE, message.getType());
            data.put(MESSAGE_PAYLOAD, message.getText());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Configure the header
        PublishOptions publishOptions = new PublishOptions();
        publishOptions.putHeader("android-ticker-text", fullText);
        publishOptions.putHeader("android-content-title", "Message from " + sender);
        publishOptions.putHeader("android-content-text", messageText);
        publishOptions.setPublisherId(message.getBUserSender().getEntityID());

        // Only push to android devices
        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setPushPolicy(PushPolicyEnum.ONLY);
        deliveryOptions.setPushBroadcast(PushBroadcastMask.ANDROID);

        // Publish a push notification to each channel
        for(String channel : channels) {
            try {
                data.put(Channel, channel);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            Backendless.Messaging.publish(channel, data.toString(), publishOptions, deliveryOptions, new AsyncCallback<MessageStatus>() {
                @Override
                public void handleResponse(MessageStatus response) {
                    if (DEBUG) Timber.v("Message published");
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    if (DEBUG) Timber.v("Publish failed, " + fault.getMessage());
                }
            });
        }

        //For iOS
        try {
            JSONObject dataIOS = data;
            dataIOS.put(BADGE, INCREMENT);
            dataIOS.put(ALERT, fullText);
            // For making sound in iOS
            dataIOS.put(SOUND, Default);

            PublishOptions publishOptionsIOS = new PublishOptions();
            publishOptionsIOS.putHeader("android-ticker-text", fullText);
            publishOptionsIOS.putHeader("android-content-title", "Message from " + sender);
            publishOptionsIOS.putHeader("android-content-text", messageText);
            publishOptionsIOS.setPublisherId(message.getBUserSender().getEntityID());

            DeliveryOptions deliveryOptionsIOS = new DeliveryOptions();
            deliveryOptions.setPushPolicy(PushPolicyEnum.ONLY);
            deliveryOptionsIOS.setPushBroadcast(PushBroadcastMask.IOS);

            for(String channel : channels) {
                Backendless.Messaging.publish(channel, dataIOS.toString(), publishOptionsIOS, deliveryOptionsIOS, new AsyncCallback<MessageStatus>() {
                    @Override
                    public void handleResponse(MessageStatus response) {
                        if (DEBUG) Timber.v("Message published");
                    }

                    @Override
                    public void handleFault(BackendlessFault fault) {
                        if (DEBUG) Timber.v("Publish failed");
                    }
                });
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }

        /*ParseQuery<ParseInstallation> androidQuery = ParseInstallation.getQuery();
        androidQuery.whereEqualTo(DeviceType, Android);
        androidQuery.whereContainedIn(Channels, channels);

        ParsePush androidPush = new ParsePush();
        androidPush.setQuery(androidQuery);
        androidPush.setData(data);
        androidPush.sendInBackground();

        //For iOS
        try {
            data.put(BADGE, INCREMENT);
            data.put(ALERT, text);
            // For making sound in iOS
            data.put(SOUND, Default);

            ParseQuery<ParseInstallation> iosQuery = ParseInstallation.getQuery();
            iosQuery.whereEqualTo(DeviceType, iOS);
            iosQuery.whereContainedIn(Channels, channels);

            ParsePush iOSPush = new ParsePush();
            iOSPush.setQuery(iosQuery);
            iOSPush.setData(data);
            iOSPush.sendInBackground();

        } catch (JSONException e) {
            e.printStackTrace();
        }*/
    }


    /** @param channel The channel to push to.
     * @param content The follow notification content.*/
    public static void sendFollowPush(String channel, String content){
        if (DEBUG) Timber.v("pushutils sendfollowpush");
        JSONObject data = new JSONObject();
        try {
            data.put(ACTION, ChatSDKReceiver.ACTION_FOLLOWER_ADDED);
            data.put(CONTENT, content);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        PublishOptions publishOptions = new PublishOptions();
        publishOptions.putHeader("android-ticker-text", content);
        publishOptions.putHeader("android-content-title", "Follower added");
        publishOptions.putHeader("android-content-text", "");

        DeliveryOptions deliveryOptions = new DeliveryOptions();
        deliveryOptions.setPushPolicy(PushPolicyEnum.ONLY);
        deliveryOptions.setPushBroadcast(PushBroadcastMask.ANDROID);

        Backendless.Messaging.publish(channel, data.toString(), publishOptions, deliveryOptions, new AsyncCallback<MessageStatus>() {
            @Override
            public void handleResponse(MessageStatus response) {
                if (DEBUG) Timber.v("Message published");
            }

            @Override
            public void handleFault(BackendlessFault fault) {
                if (DEBUG) Timber.v("Publish failed");
            }
        });

        //For iOS
        try {
            JSONObject dataIOS = data;
            dataIOS.put(BADGE, INCREMENT);
            dataIOS.put(ALERT, content);
            // For making sound in iOS
            dataIOS.put(SOUND, Default);

            PublishOptions publishOptionsIOS = new PublishOptions();
            publishOptionsIOS.putHeader("android-ticker-text", content);
            publishOptionsIOS.putHeader("android-content-title", "Follower added");
            publishOptionsIOS.putHeader("android-content-text", "");

            DeliveryOptions deliveryOptionsIOS = new DeliveryOptions();
            deliveryOptions.setPushPolicy(PushPolicyEnum.ONLY);
            deliveryOptionsIOS.setPushBroadcast(PushBroadcastMask.IOS);

            Backendless.Messaging.publish(channel, dataIOS.toString(), publishOptionsIOS, deliveryOptionsIOS, new AsyncCallback<MessageStatus>() {
                @Override
                public void handleResponse(MessageStatus response) {
                    if (DEBUG) Timber.v("Message published");
                }

                @Override
                public void handleFault(BackendlessFault fault) {
                    if (DEBUG) Timber.v("Publish failed");
                }
            });

        } catch (JSONException e) {
            e.printStackTrace();
        }

        //For iOS
        try {
            data.put(BADGE, INCREMENT);
            data.put(ALERT, content);
            // For making sound in iOS
            data.put(SOUND, Default);

            ParseQuery<ParseInstallation> iosQuery = ParseInstallation.getQuery();
            iosQuery.whereEqualTo(DeviceType, iOS);
            iosQuery.whereEqualTo(Channel, channel);

            ParsePush iOSPush = new ParsePush();
            iOSPush.setQuery(iosQuery);
            iOSPush.setData(data);
            iOSPush.sendInBackground();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
