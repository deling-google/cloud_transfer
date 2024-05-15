//
//  Header.h
//  
//
//  Created by Deling Ren on 5/14/24.
//

#ifndef PLATFORM_IMPL_APPLE_WIFI_HOTSPOT_H_
#define PLATFORM_IMPL_APPLE_WIFI_HOTSPOT_H_

#include "internal/platform/implementation/wifi_hotspot.h"

namespace nearby {
namespace apple {

class WifiHotspotMedium : public api::WifiHotspotMedium {
public:
  WifiHotspotMedium();
  ~WifiHotspotMedium() override;

  WifiHotspotMedium(const WifiHotspotMedium&) = delete;
  WifiHotspotMedium(WifiHotspotMedium&&) = delete;
  WifiHotspotMedium& operator=(const WifiHotspotMedium&) = delete;
  WifiHotspotMedium& operator=(WifiHotspotMedium&&) = delete;

  // If the WiFi Adaptor supports to start a Hotspot interface.
  bool IsInterfaceValid() const override { return true; }

  // Discoverer connects to server socket
  std::unique_ptr<api::WifiHotspotSocket> ConnectToService(
      absl::string_view ip_address, int port,
      CancellationFlag* cancellation_flag) override;

  // Advertiser starts to listen on server socket
  std::unique_ptr<api::WifiHotspotServerSocket> ListenForService(
      int port) override;

  // Advertiser start WiFi Hotspot with specific Crendentials
  bool StartWifiHotspot(HotspotCredentials* hotspot_credentials) override;
  // Advertiser stop the current WiFi Hotspot
  bool StopWifiHotspot() override;
  // Discoverer connects to the Hotspot
  bool ConnectWifiHotspot(HotspotCredentials* hotspot_credentials) override;
  // Discoverer disconnects from the Hotspot
  bool DisconnectWifiHotspot() override;

  std::optional<std::pair<std::int32_t, std::int32_t>> GetDynamicPortRange()
      override {
    return std::nullopt;
  }
};

}
}
#endif /* PLATFORM_IMPL_APPLE_WIFI_HOTSPOT_H_ */
