/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:27 PM
 */

package co.chatsdk.ui.threads;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.facebook.drawee.view.SimpleDraweeView;

import org.apache.commons.lang3.StringUtils;

import co.chatsdk.core.dao.Thread;
import co.chatsdk.core.events.NetworkEvent;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.session.NM;
import co.chatsdk.core.session.StorageManager;
import co.chatsdk.core.utils.DisposableList;
import co.chatsdk.core.utils.Strings;
import co.chatsdk.ui.R;
import co.chatsdk.ui.chat.ChatActivity;
import co.chatsdk.ui.contacts.ContactsFragment;
import co.chatsdk.ui.helpers.ProfilePictureChooserOnClickListener;
import co.chatsdk.ui.main.BaseActivity;
import co.chatsdk.ui.manager.BaseInterfaceAdapter;
import co.chatsdk.ui.manager.InterfaceManager;
import co.chatsdk.ui.utils.ToastHelper;
import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Pepe Becker on 09/05/18.
 */
public class PublicThreadEditDetailsActivity extends BaseActivity {

    /** Set true if you want slide down animation for this context exit. */
    protected boolean animateExit = false;

    protected DisposableList disposableList = new DisposableList();

    protected String threadEntityID;
    protected Thread thread;
    protected EditText nameInput;
    protected Button saveButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        threadEntityID = getIntent().getStringExtra(BaseInterfaceAdapter.THREAD_ENTITY_ID);
        if (threadEntityID != null)
            thread = StorageManager.shared().fetchThreadWithEntityID(threadEntityID);

        setContentView(R.layout.chat_sdk_activity_edit_public_thread_details);
        initViews();
    }

    protected void initViews() {
        nameInput = findViewById(R.id.chat_sdk_edit_thread_name_et);
        saveButton = findViewById(R.id.chat_sdk_edit_thread_update_b);

        if (thread != null) {
            nameInput.setText(thread.getName());
            saveButton.setText(R.string.update_public_thread);
        }

        saveButton.setOnClickListener(v -> {
            setSaveButtonOnClickListener();
        });
    }

    protected void setSaveButtonOnClickListener() {
        final String threadName = nameInput.getText().toString();
        if (thread == null) {
            showOrUpdateProgressDialog(getString(R.string.add_public_chat_dialog_progress_message));

            disposableList.add(NM.publicThread().createPublicThreadWithName(threadName)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe((_thread, throwable) -> {
                        if (throwable == null) {
                            dismissProgressDialog();
                            ToastHelper.show(ChatSDK.shared().context(), String.format(getString(R.string.public_thread__is_created), threadName));

                            InterfaceManager.shared().a.startChatActivityForID(ChatSDK.shared().context(), _thread.getEntityID());
                        } else {
                            ChatSDK.logError(throwable);
                            Toast.makeText(ChatSDK.shared().context(), throwable.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                            dismissProgressDialog();
                        }
                    }));
        } else {
//            TODO: add thread name to meta data
//            thread.setMetaValue("name", threadName);
//            disposableList.add(new ThreadWrapper(thread).pushMeta().subscribe(this::finish));
            thread.setName(threadName);
            thread.update();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        setResult(AppCompatActivity.RESULT_OK);

        finish();
        if (animateExit) {
            overridePendingTransition(R.anim.dummy, R.anim.slide_top_bottom_out);
        }
    }

    @Override
    protected void onStop() {
        disposableList.dispose();
        super.onStop();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

}
