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

import Foundation

@Observable class Packet<T: File>: Identifiable, Encodable, Decodable {
  enum State: Int {
    case unknown, picked, loading, loaded, uploading, uploaded, received, downloading, downloaded
  }

  // This is the Identifiable.id used by SwiftUI, not the packet id we communicate with other
  // devices.
  let id = UUID().uuidString

  var notificationToken: String? = nil
  var packetId: String = ""
  var files: [T] = []
  
  var receiver: String? = nil
  var sender: String? = nil
  var state: State = .unknown
  var expanded: Bool = true

  init() {}

  required init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    packetId = try container.decode(String.self, forKey: .packetId)
    files = try container.decode([T].self, forKey: .files)
  }

  func encode(to encoder: Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self)
    try container.encode(packetId, forKey: .packetId)
    try container.encode(files, forKey: .files)
  }

  enum CodingKeys: String, CodingKey {
    case packetId, files
  }
}

extension Packet<OutgoingFile> {
  func upload() -> Void {
    self.state = .uploading
    // Upload each outging file
    for file in files {
      if file.state == .loaded {
        let beginTime = Date()
        file.upload() {[beginTime, weak self] size, error in
          guard let self else {
            return
          }

          if error == nil {
            let duration: TimeInterval = Date().timeIntervalSince(beginTime)
            print("Uploaded. Size(b): \(file.fileSize). Time(s): \(duration).")
            try? FileManager.default.removeItem(at: file.localUrl!)

            if self.files.allSatisfy({ $0.state == .uploaded }) {
              self.state = .uploaded
            }
          } else {
            print("Failed to upload packet")
            self.state = .loaded
          }
        }
      }
    }
    // Update packet status in Firebase
    // The update will automatically trigger a push notification
  }
}
