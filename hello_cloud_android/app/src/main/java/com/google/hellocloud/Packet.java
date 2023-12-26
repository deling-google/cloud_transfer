package com.google.hellocloud;

import static com.google.hellocloud.Util.TAG;
import static com.google.hellocloud.Util.logErrorAndToast;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import androidx.databinding.BaseObservable;
import androidx.databinding.Bindable;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;

public class Packet<T extends File> extends BaseObservable {
  public enum State {
    UNKNOWN,
    LOADED,
    UPLOADING,
    UPLOADED,
    RECEIVED,
    DOWNLOADING,
    DOWNLOADED
  }

  public UUID id;
  public String notificationToken;
  public ArrayList<T> files = new ArrayList<>();
  public String receiver;
  public String sender;

  public transient State state;

  public Packet() {
    id = UUID.randomUUID();
  }

  public Packet(UUID uuid) {
    this.id = uuid;
  }

  public Packet<T> setState(State state) {
    this.state = state;
    notifyPropertyChanged(BR.isBusy);
    notifyPropertyChanged(BR.outgoingStateIcon);
    notifyPropertyChanged(BR.incomingStateIcon);
    notifyPropertyChanged(BR.canUpload);
    notifyPropertyChanged(BR.canDownload);
    return this;
  }

  public String getOutgoingDescription() {
    return "Packet" + (receiver == null ? "" : (" for " + receiver));
  }

  public String getIncomingDescription() {
    return "Packet" + (sender == null ? "" : (" from " + sender));
  }

  @Bindable
  public boolean getIsBusy() {
    return state == State.DOWNLOADING || state == State.UPLOADING;
  }

  @Bindable
  public Drawable getOutgoingStateIcon() {
    if (state == null) {
      return null;
    }
    // LOADED: upload button
    // UPLOADED: green filled circle
    // UPLOADING: spinner
    int resource;
    switch (state) {
      case UPLOADED -> resource = R.drawable.uploaded_outgoing;
      default -> {
        return null;
      }
    }
    return Main.shared.context.getResources().getDrawable(resource, null);
  }

  @Bindable
  public Drawable getIncomingStateIcon() {
    if (state == null) {
      return null;
    }
    // RECEIVED: grey dotted circle
    // UPLOADED: download button
    // DOWNLOADING: spinner
    // DOWNLOADED: green filled circle
    int resource;
    switch (state) {
      case RECEIVED -> resource = R.drawable.received;
      case DOWNLOADED -> resource = R.drawable.downloaded;
      default -> {
        return null;
      }
    }
    return Main.shared.context.getResources().getDrawable(resource, null);
  }

  @Bindable
  public boolean getCanUpload() {
    return state == State.LOADED;
  }

  @Bindable
  public boolean getCanDownload() {
    return state == State.UPLOADED;
  }

  public void upload() {
    if (state != State.LOADED) {
      Log.e(TAG, "Packet is not loaded before being uploaded.");
      return;
    }
    setState(State.UPLOADING);
    for (T file : files) {
      OutgoingFile outgoingFile = (OutgoingFile) file;
      assert (outgoingFile != null);

      Instant beginTime = Instant.now();
      outgoingFile
          .upload()
          .addOnSuccessListener(
              result -> {
                setState(State.UPLOADED);
                Instant endTime = Instant.now();
                Duration duration = Duration.between(beginTime, endTime);
                Log.i(
                    TAG,
                    String.format(
                        "Uploaded. Size(b): %s. Time(s): %d.",
                        outgoingFile.fileSize, duration.getSeconds()));
              })
          .addOnFailureListener(
              error -> {
                setState(State.LOADED);
                logErrorAndToast(
                    Main.shared.context, R.string.error_toast_cannot_upload, error.getMessage());
              });
    }
  }

  public void download(Context context) {
    if (state != State.UPLOADED) {
      Log.e(TAG, "Packet is not uploaded before being downloaded.");
      return;
    }
    setState(State.DOWNLOADING);

    for (T file : files) {
      IncomingFile incomingFile = (IncomingFile) file;
      assert incomingFile != null;

      ContentResolver resolver = context.getContentResolver();
      String fileName = UUID.randomUUID().toString().toUpperCase();
      Uri imagesUri = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
      ContentValues values = new ContentValues();
      values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
      values.put(MediaStore.MediaColumns.MIME_TYPE, file.mimeType);
      Uri uri = resolver.insert(imagesUri, values);
      incomingFile.setLocalUri(uri);

      Instant beginTime = Instant.now();
      incomingFile
          .download()
          .addOnSuccessListener(
              result -> {
                setState(State.DOWNLOADED);
                Instant endTime = Instant.now();
                Duration duration = Duration.between(beginTime, endTime);
                Log.i(
                    TAG,
                    String.format(
                        "Downloaded. Size(b): %s. Time(s): %d.",
                        incomingFile.fileSize, duration.getSeconds()));
              })
          .addOnFailureListener(
              error -> {
                setState(State.UPLOADED);
                logErrorAndToast(
                    Main.shared.context, R.string.error_toast_cannot_download, error.getMessage());
              });
    }
  }
}
