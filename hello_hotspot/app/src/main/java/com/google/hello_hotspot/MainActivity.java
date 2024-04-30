package com.google.hello_hotspot;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import java.util.List;

public class MainActivity extends AppCompatActivity {

  ActivityResultLauncher<PickVisualMediaRequest> picker;
  FileServer fileServer;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    picker =
        registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(), this::onMediaPicked);

    fileServer = new FileServer(50000, getContentResolver());
    fileServer.start();
  }

  public void pickMedia(View view) {
    assert picker != null;
    picker.launch(
            new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
                    .build());
  }

  private void onMediaPicked(List<Uri> uris) {
    if (uris.size() == 0) {
      return;
    }
    fileServer.serveFiles(uris);
  }
}
