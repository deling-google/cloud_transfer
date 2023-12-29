package com.google.hellocloud;

import static com.google.hellocloud.Utils.TAG;

import android.content.Context;
import android.content.ContextWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.databinding.BindingAdapter;
import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import com.google.hellocloud.databinding.FragmentMainBinding;
import com.google.hellocloud.databinding.ItemEndpointBinding;
import com.google.zxing.integration.android.IntentIntegrator;
import java.util.List;

/** Fragment for the home screen */
public class MainFragment extends Fragment {
  static class EndpointAdapter extends ArrayAdapter<Endpoint> {
    private final Context context;

    public EndpointAdapter(Context context, List<Endpoint> endpoints) {
      super(context, R.layout.item_endpoint, endpoints);
      this.context = context;
    }

    @Override
    public @NonNull View getView(int position, View convertView, ViewGroup parent) {
      ItemEndpointBinding binding;
      View view;

      if (convertView == null) {
        LayoutInflater inflater = LayoutInflater.from(context);
        binding = DataBindingUtil.inflate(inflater, R.layout.item_endpoint, parent, false);
        view = binding.getRoot();
      } else {
        binding = DataBindingUtil.getBinding(convertView);
        view = convertView;
      }
      final Endpoint endpoint = getItem(position);
      binding.setModel(endpoint);

      binding.connect.setOnClickListener(
          v -> {
            endpoint.setState(Endpoint.State.CONNECTING);
            Main.shared.requestConnection(endpoint.id);
          });

      binding.disconnect.setOnClickListener(
          v -> {
            endpoint.setState(Endpoint.State.DISCONNECTING);
            Main.shared.disconnect(endpoint.id);
          });

      binding.pick.setOnClickListener(
          v -> {
            MainFragment mainFragment = getMainFragment();
            mainFragment.pickMedia(endpoint);
          });

      return view;
    }

    // TODO: this is super ugly, lol. But I have no time to think this through right now.
    private MainFragment getMainFragment() {
      Context context = getContext();
      while (context instanceof ContextWrapper) {
        if (context instanceof MainActivity mainActivity) {
          Fragment navHostFragment =
              mainActivity.getSupportFragmentManager().findFragmentById(R.id.fragmentContainerView);
          Fragment mainFragment =
              navHostFragment == null
                  ? null
                  : navHostFragment.getChildFragmentManager().getFragments().get(0);
          return (MainFragment) mainFragment;
        }
        context = ((ContextWrapper) context).getBaseContext();
      }
      return null;
    }
  }

  private final Main model = Main.shared;
  private Endpoint endpointForPicker = null;

  ActivityResultLauncher<PickVisualMediaRequest> picker;

  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    picker =
        registerForActivityResult(
            new ActivityResultContracts.PickMultipleVisualMedia(), this::onMediaPicked);

    FragmentMainBinding binding =
        DataBindingUtil.inflate(inflater, R.layout.fragment_main, container, false);
    Main.shared.context = getActivity().getApplicationContext();
    binding.setModel(model);
    getActivity().setTitle(R.string.app_name);

    binding.sendQr.setOnClickListener(v -> pickMedia(null));
    binding.receiveQr.setOnClickListener(v -> scanQrCode());
    return binding.getRoot();
  }

  public void pickMedia(Endpoint endpoint) {
    assert picker != null;
    endpointForPicker = endpoint;
    picker.launch(
        new PickVisualMediaRequest.Builder()
            .setMediaType(ActivityResultContracts.PickVisualMedia.ImageAndVideo.INSTANCE)
            .build());
  }

  private void onMediaPicked(List<Uri> uris) {
    if (uris.size() == 0) {
      return;
    }

    if (endpointForPicker == null) {
      // Media was launched from the main fragment's send_qr button.
      new SendQrDialogFragment(loadAndGenerateQr(uris)).show(getChildFragmentManager(), TAG);
      return;
    }

    Context context = getView().getContext();
    new AlertDialog.Builder(context)
        .setMessage("Do you want to send the claim token to the remote endpoint?")
        .setPositiveButton("Yes", (dialog, button) -> endpointForPicker.loadAndsend(context, uris))
        .setNegativeButton("No", null)
        .show();
  }

  private String loadAndGenerateQr(List<Uri> uris) {
    Packet<OutgoingFile> packet = Utils.loadPhotos(getView().getContext(), uris, null, null);
    return DataWrapper.getGson().toJson(packet);
  }

  private void scanQrCode() {
    new IntentIntegrator(getActivity())
        .setPrompt("Ask the sender to tap on \"Send QR\" button")
        .setOrientationLocked(false)
        .initiateScan();
  }

  public void onQrCodeReceived(String qrString) {
    System.out.println(qrString);
    Packet<IncomingFile> packet = DataWrapper.getGson().fromJson(qrString, Packet.class);
    // The QR code wouldn't include state information. So let's append it.
    packet.setState(Packet.State.RECEIVED);
    for (IncomingFile file : packet.files) {
      file.setState(IncomingFile.State.RECEIVED);
    }
    new AlertDialog.Builder(getView().getContext())
        .setMessage("You have received a packet from " + packet.sender)
        .setNeutralButton("OK", null)
        .show();
    model.addIncomingPacket(packet);
  }

  @BindingAdapter("entries")
  public static void setEntries(View view, List<Endpoint> endpoints) {
    Context context = view.getContext();
    ArrayAdapter<Endpoint> endpointAdapter = new EndpointAdapter(context, endpoints);
    ((ListView) view).setAdapter(endpointAdapter);
  }
}
