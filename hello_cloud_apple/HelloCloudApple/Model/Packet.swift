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

class Packet: Encodable, Decodable {
  let notificationToken: String?
  let files: [File]

  init(notificationToken: String?, files: [File]) {
    self.notificationToken = notificationToken
    self.files = files
  }

  required init(from decoder: Decoder) throws {
    let container = try decoder.container(keyedBy: CodingKeys.self)
    notificationToken = try container.decode(String?.self, forKey: .notificationToken)
    files = try container.decode([IncomingFile].self, forKey: .files)
  }

  func encode(to encoder: Encoder) throws {
    var container = encoder.container(keyedBy: CodingKeys.self) 
    try container.encode(notificationToken, forKey: .notificationToken)
    try container.encode(files as? [OutgoingFile], forKey: .files)
  }

  enum CodingKeys: String, CodingKey {
    case notificationToken, files
  }
}
