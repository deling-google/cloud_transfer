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

import SwiftUI

struct PacketView: View {
  @Binding var id: UUID?
  @Binding var index: Int

  var body: some View {
    let packet = id == nil ? nil : Main.shared.incomingPackets.first(where: {p in p.id == self.id})
    let fileCount = packet?.files.count
    let imageUrl = packet == nil || index > fileCount! - 1 ? nil : packet!.files[index].localUrl

    let imageData = imageUrl != nil ? try? Data(contentsOf: imageUrl!) : nil
    let uiImage: UIImage? = imageData != nil ? UIImage(data: imageData!) : nil

    VStack {
      Label ("Swipe left or right to navigate. Tap to dismiss.", systemImage: "info.circle")

      Form {
        if uiImage != nil{
          Image(uiImage: uiImage!)
            .resizable()
            .aspectRatio(contentMode: .fill)
        }
      }
      .gesture(DragGesture(minimumDistance: 50).onEnded({action in
        if action.translation.width > 0 {
          self.index = (self.index + 1) % fileCount!
        } else {
          self.index -= 1
          if self.index < 0 {
            self.index = fileCount! - 1
          }
        }
      }))
      .onTapGesture { self.id = nil }
    }
  }
}
