//
//  Copyright 2023 Google LLC
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      https://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//
import NearbyConnections
import SwiftUI
import PhotosUI

struct MainView: View {
  @State var model: Main
  @State var showingQrAck: (Bool, String?) = (false, nil)

  var body: some View {
    VStack {
      Label("Hello Cloud", systemImage: "icloud").font(.title).padding([.top], 10)
      Form {
        Section{
          Grid (horizontalSpacing: 20, verticalSpacing: 10) {
            GridRow {
              Label("ID:", systemImage: "1.square.fill").gridColumnAlignment(.leading)
              Text(model.localEndpointId).font(.custom("Menlo", fixedSize: 15)).gridColumnAlignment(.leading)
            }
            GridRow {
              Label("Name:", systemImage: "a.square.fill").gridColumnAlignment(.leading)
              TextField("Local Endpoint Name", text: $model.localEndpointName)
                .disabled(model.isAdvertising)
            }
          }

          HStack {
            Button(action: {model.isAdvertising.toggle()}) {
              Label("Advertise",
                    systemImage: model.isAdvertising ? "stop.circle" : "play.circle")
              .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .foregroundColor(model.isAdvertising ? .red : .green)
            .buttonStyle(.bordered)

            Button(action: {model.isDiscovering.toggle()}) {
              Label("Discover",
                    systemImage: model.isDiscovering ? "stop.circle" : "play.circle")
              .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .foregroundColor(model.isDiscovering ? .red : .green)
            .buttonStyle(.bordered)
          }

          HStack {
            Button(action: {model.showingInbox = true}) {
              Label("Inbox", systemImage: "tray")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $model.showingInbox) {
              IncomingPacketsView()
            }

            Button(action: {model.showingOutbox = true}) {
              Label("Outbox", systemImage: "paperplane")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $model.showingOutbox) {
              OutgoingPacketsView()
            }
          }

          HStack {
            Button (action: { model.showingFileImporter = true }) {
              ZStack {
                Label("Send", systemImage: "qrcode")
                  .frame(maxWidth: .infinity, maxHeight: .infinity)
                  .opacity(model.loading ? 0 : 1)
                ProgressView()
                  .opacity(model.loading ? 1 :0)
              }
            }
            .buttonStyle(.bordered)
            .disabled(model.loading)
            .sheet(isPresented: $model.showingQrCode) {
              QrCodeView()
                .aspectRatio(1.0, contentMode: .fit)
            }
            .fileImporter(
              isPresented: $model.showingFileImporter,
              allowedContentTypes: [.data],
              allowsMultipleSelection: true) { result in
                switch result {
                case .success(let urls):
                  Task { await model.loadFilesAndGenerateQr(urls) }
                case .failure(let error):
                  print("Failure: \(error)")
                }
              }

            Button(action: {model.showingQrScanner = true}) {
              Label("Receive", systemImage: "qrcode.viewfinder")
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .buttonStyle(.bordered)
            .sheet(isPresented: $model.showingQrScanner) {
              CodeScannerView(codeTypes: [.qr]) { result in
                model.showingQrScanner = false
                switch result {
                case .success(let result):
                  print("I: QR code scanned: \(result.string)")
                  if let packet = model.onQrCodeReceived(string: result.string) {
                    showingQrAck = (true, "You have received a packet from \(packet.sender!)")
                  }
                case .failure(let error):
                  print("E: Failed to scan QR code: \(error.localizedDescription)")
                }
              }
            }
            .alert(showingQrAck.1 ?? "", isPresented: $showingQrAck.0) {
              Button("OK") {
                Task {
                  showingQrAck = (false, nil)
                }
              }
            }
          }
        } header: {
          Text("Local endpoint")
        }

        List {
          Section {
            ForEach($model.endpoints) { $endpoint in
              HStack {
                HStack {
                  switch endpoint.state {
                  case .connected:
                    Image(systemName: "circle.fill").foregroundColor(.green)
                  case .discovered:
                    Image(systemName: "circle.fill").foregroundColor(.gray)
                  default:
                    Image(systemName: "circle.fill").foregroundColor(.gray)
                  }
                  VStack {
                    Text(endpoint.id).font(.custom("Menlo", fixedSize: 10))
                      .frame(maxWidth: .infinity, alignment: .leading)
                    Text(endpoint.name)
                      .frame(maxWidth: .infinity, alignment: .leading)
                  }
                }
                Spacer()
                HStack {
                  Button(action: { endpoint.connect() }) {
                    ZStack {
                      Image(systemName: "phone.connection.fill")
                        .foregroundColor(endpoint.state == .discovered ? .green : .gray)
                        .opacity(endpoint.state == .connecting ? 0 : 1)
                      ProgressView().opacity(endpoint.state == .connecting ? 1 : 0)
                    }
                  }
                  .disabled(endpoint.state != .discovered)
                  .buttonStyle(.bordered).fixedSize()
                  .frame(maxHeight: .infinity)
                  Button(action: { endpoint.disconnect() }) {
                    ZStack {
                      Image(systemName: "phone.down.fill")
                        .foregroundColor(endpoint.state == .connected ? .red : .gray)
                        .opacity(endpoint.state == .disconnecting ? 0 : 1)
                      ProgressView().opacity(endpoint.state == .disconnecting ? 1 : 0)
                    }
                  }
                  .disabled(endpoint.state != .connected)
                  .buttonStyle(.bordered).fixedSize()
                  .frame(maxHeight: .infinity)
                  
                  Button (action: { endpoint.showingFileImporter = true }) {
                    ZStack {
                      Image(systemName: "photo.badge.plus.fill")
                        .opacity(model.loading ? 0 : 1)
                      ProgressView()
                        .opacity(model.loading ? 1 :0)
                    }
                  }
                  .disabled(endpoint.loading || endpoint.state != .connected)
                  .buttonStyle(.bordered).fixedSize()
                  .frame(maxHeight: .infinity)
                  .fileImporter(
                    isPresented: $endpoint.showingFileImporter,
                    allowedContentTypes: [.data],
                    allowsMultipleSelection: true) { result in
                      switch result {
                      case .success(let urls):
                        Task { await endpoint.loadSendAndUpload(urls) }
                      case .failure(let error):
                        print("Failure: \(error)")
                      }
                    }
                }
              }
            }
          } header: {
            Text("Remote endpoints")
          }
        }
      }
    }
  }
}

#Preview {
  MainView(model: Main.createDebugModel())
}
