/*
 * Created by Itzik Braun on 12/3/2015.
 * Copyright (c) 2015 deluge. All rights reserved.
 *
 * Last Modification at: 3/12/15 4:35 PM
 */

package com.braunster.androidchatsdk.firebaseplugin.firebase.backendless;

import android.graphics.Bitmap;
import android.os.Handler;

import com.backendless.Backendless;
import com.backendless.async.callback.AsyncCallback;
import com.backendless.exceptions.BackendlessFault;
import com.braunster.chatsdk.Utils.ImageUtils;
import com.braunster.chatsdk.Utils.volley.VolleyUtils;
import com.braunster.chatsdk.dao.BMessage;
import com.braunster.chatsdk.dao.core.DaoCore;
import com.braunster.chatsdk.network.BDefines;
import com.braunster.chatsdk.object.BError;
import com.braunster.chatsdk.object.SaveImageProgress;

import org.jdeferred.Deferred;
import org.jdeferred.Promise;
import org.jdeferred.impl.DeferredObject;

import java.io.ByteArrayOutputStream;

import timber.log.Timber;

public class BackendlessUtils {
    public static final String TAG = BackendlessUtils.class.getSimpleName();
    public static final boolean DEBUG = true;

    public static Promise<String, BError, SaveImageProgress> saveImageToBackendless(final String path) {

        //  Loading the bitmap
        Bitmap b = ImageUtils.getCompressed(path);

        if (b == null) {
            return reject();
        }

        SaveImageProgress saveImageProgress = new SaveImageProgress();

        saveImageProgress.savedImage = b;
        
        return save(getByteArray(b), saveImageProgress);
    }

    public static Promise<String, BError, SaveImageProgress> saveImageToBackendless(Bitmap b, int size){

        if (b == null) {
            return reject();
        }
        
        b = ImageUtils.scaleImage(b, size);

        SaveImageProgress saveImageProgress = new SaveImageProgress();

        saveImageProgress.savedImage = b;

        // Save
        return save(getByteArray(b), saveImageProgress);
    }

    public static Promise<String[], BError, SaveImageProgress> saveImageFileToBackendlessWithThumbnail(final String path, final int thumbnailSize){
        //  Loading the bitmap
        Bitmap b = ImageUtils.getCompressed(path);

        if (b == null) {
            return rejectMultiple();
        }

        Bitmap thumbnail = ImageUtils.getCompressed(path, thumbnailSize, thumbnailSize);

        String imageDimensions = ImageUtils.getDimensionAsString(b);

        if (DEBUG) Timber.d("dimensionsString: %s", imageDimensions);

        SaveImageProgress saveImageProgress = new SaveImageProgress();

        saveImageProgress.dimensionsString = imageDimensions;
        saveImageProgress.savedImage = b;
        saveImageProgress.savedImageThumbnail = thumbnail;

        return save(getByteArray(b), getByteArray(thumbnail, 50), imageDimensions, saveImageProgress);
    }

    public static Promise<String[], BError, SaveImageProgress> saveBMessageWithImage(BMessage message){
        //  Loading the bitmap
        Bitmap b = ImageUtils.getCompressed(message.getResourcesPath());

        if (b == null) {
            return rejectMultiple();
        }

        Bitmap thumbnail = ImageUtils.getCompressed(message.getResourcesPath(),
                BDefines.ImageProperties.MAX_IMAGE_THUMBNAIL_SIZE,
                BDefines.ImageProperties.MAX_IMAGE_THUMBNAIL_SIZE);

        String imageDimensions = ImageUtils.getDimensionAsString(b);

        if (DEBUG) Timber.d("dimensionsString: %s", imageDimensions);

        SaveImageProgress saveImageProgress = new SaveImageProgress();

        saveImageProgress.dimensionsString = imageDimensions;
        saveImageProgress.savedImage = b;
        saveImageProgress.savedImageThumbnail = thumbnail;

        // Adding the image to the cache
        VolleyUtils.getBitmapCache().put(
                VolleyUtils.BitmapCache.getCacheKey(message.getResourcesPath()),
                saveImageProgress.savedImageThumbnail);

        message.setImageDimensions(saveImageProgress.dimensionsString);
        
        return save(getByteArray(b), getByteArray(thumbnail, 50), imageDimensions, saveImageProgress);
    }

    private static Promise<String, BError, SaveImageProgress> save(final byte[] fileBytes,  final SaveImageProgress saveImageProgress){
        final Deferred<String, BError, SaveImageProgress> deferred = new DeferredObject<>();
        // Save image byte array to folder images
        Backendless.Files.saveFile("/images", DaoCore.generateEntity() + ".jpeg", fileBytes, new AsyncCallback<String>() {
            @Override
            public void handleResponse(String url) {
                deferred.resolve(url);
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                if (DEBUG)
                    Timber.e(backendlessFault.getMessage(), "Backendless Exception while saving");
                deferred.reject(new BError(BError.Code.BACKENDLESS_EXCEPTION, backendlessFault));
                return;
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                deferred.notify(saveImageProgress);
            }
        }, 100);
        
        return deferred.promise();
    }

    private static Promise<String[], BError, SaveImageProgress> save(final byte[] fileBytes, final byte[] thumbnailBytes,
                                                                final String imageDimensions, final SaveImageProgress saveImageProgress){
        final Deferred<String[], BError, SaveImageProgress> deferred = new DeferredObject<>();
        // Save image byte array to folder images
        Backendless.Files.saveFile("/images", DaoCore.generateEntity() + ".jpeg", fileBytes, new AsyncCallback<String>() {
            @Override
            public void handleResponse(String url) {
                final String fileURL = url;
                // Save thumbnail byte array to folder images/thumbnails
                Backendless.Files.saveFile("/images/thumbnails", DaoCore.generateEntity() + ".jpeg", fileBytes, new AsyncCallback<String>() {
                    @Override
                    public void handleResponse(String url) {
                        deferred.resolve(new String[]{fileURL, url, imageDimensions});
                    }

                    @Override
                    public void handleFault(BackendlessFault backendlessFault) {
                        if (DEBUG)
                            Timber.e(backendlessFault.getMessage(), "Backendless Exception while saving");
                        deferred.reject(new BError(BError.Code.BACKENDLESS_EXCEPTION, backendlessFault));
                        return;
                    }
                });
            }

            @Override
            public void handleFault(BackendlessFault backendlessFault) {
                if (DEBUG)
                    Timber.e(backendlessFault.toString(), "Backendless Exception while saving");
                deferred.reject(new BError(BError.Code.BACKENDLESS_EXCEPTION, backendlessFault));
            }
        });

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                deferred.notify(saveImageProgress);
            }
        }, 10);
        
        return deferred.promise();
    }

    private static byte[] getByteArray(Bitmap bitmap){
        return getByteArray(bitmap, 50);
    }

    private static byte[] getByteArray(Bitmap bitmap, int quality){
        // Converting file to a JPEG and then to byte array.
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream);
        return stream.toByteArray();
    }

    private static Promise<String, BError, SaveImageProgress> reject(){
        return new DeferredObject<String, BError, SaveImageProgress>().reject(new BError(BError.Code.NULL, "Image Is Null"));
    }

    private static Promise<String[], BError, SaveImageProgress> rejectMultiple(){
        return new DeferredObject<String[], BError, SaveImageProgress>().reject(new BError(BError.Code.NULL, "Image Is Null"));
    }
    

}
