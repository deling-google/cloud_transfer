#import "internal/platform/implementation/apple/wifi_hotspot.h"
#import <Foundation/Foundation.h>

namespace nearby {
namespace apple {

WifiHotspotMedium::WifiHotspotMedium() {
}

WifiHotspotMedium::~WifiHotspotMedium() {
}

std::unique_ptr<api::WifiHotspotSocket>
WifiHotspotMedium::ConnectToService(absl::string_view ip_address, int port,
                                    CancellationFlag* cancellation_flag) {
  return nullptr;
}

// Advertiser starts to listen on server socket
std::unique_ptr<api::WifiHotspotServerSocket> WifiHotspotMedium::ListenForService(int port) {
  return nullptr;
}

// Advertiser start WiFi Hotspot with specific Crendentials
bool WifiHotspotMedium::StartWifiHotspot(HotspotCredentials* hotspot_credentials) {
  return false;
}

// Advertiser stop the current WiFi Hotspot
bool WifiHotspotMedium::StopWifiHotspot() {
  return false;
}

// Discoverer connects to the Hotspot
bool WifiHotspotMedium::ConnectWifiHotspot(HotspotCredentials* hotspot_credentials) {
  return false;
}

// Discoverer disconnects from the Hotspot
bool WifiHotspotMedium::DisconnectWifiHotspot() {
  return false;
}

}
}
