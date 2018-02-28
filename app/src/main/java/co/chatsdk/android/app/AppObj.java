package co.chatsdk.android.app;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.session.Configuration;
import co.chatsdk.firebase.FirebaseModule;
import co.chatsdk.firebase.file_storage.FirebaseFileStorageModule;
import co.chatsdk.firebase.push.FirebasePushModule;
import co.chatsdk.firebase.social_login.FirebaseSocialLoginModule;
import co.chatsdk.ui.manager.UserInterfaceModule;

public class AppObj extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        Configuration.Builder builder = new Configuration.Builder(context);

        // Stefan
        builder.defaultUserAvatarUrl("https://firebasestorage.googleapis.com/v0/b/beep-3e40a.appspot.com/o/default%2Ficn_100_car%403x.png?alt=media&token=f71f9b3b-75c4-4c4f-9317-9eaffeaad05b");

//        builder.firebaseRootPath("firebase_v4_web_new_4");
        builder.firebaseRootPath("18_02");

        ChatSDK.initialize(builder.build());

        FirebaseModule.activate();
        UserInterfaceModule.activate(context);


        FirebaseFileStorageModule.activate();
        FirebasePushModule.activateForFirebase();
        FirebaseSocialLoginModule.activate(context);


    }

    @Override
    protected void attachBaseContext (Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
