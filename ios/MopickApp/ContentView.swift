import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            HomeView()
                .tabItem {
                    Image(systemName: "house")
                    Text("홈")
                }
            StyleSelectionView()
                .tabItem {
                    Image(systemName: "scissors")
                    Text("스타일")
                }
            Text("매거진")
                .tabItem {
                    Image(systemName: "doc.text")
                    Text("매거진")
                }
            Text("프로필")
                .tabItem {
                    Image(systemName: "person")
                    Text("프로필")
                }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
