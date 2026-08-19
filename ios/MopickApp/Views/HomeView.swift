import SwiftUI

struct HomeView: View {
    @State private var query: String = ""

    var body: some View {
        NavigationView {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Mopick")
                            .font(.largeTitle).bold()
                        Spacer()
                        Text("강남구")
                            .font(.subheadline)
                    }

                    HStack {
                        TextField("텍스트로 검색", text: $query)
                            .padding(10)
                            .background(Color(.systemGray6))
                            .cornerRadius(10)
                        Button(action: {}) {
                            Image(systemName: "camera.fill")
                                .padding(10)
                                .background(Color.purple)
                                .foregroundColor(.white)
                                .cornerRadius(10)
                        }
                    }

                    Text("많이 찾는 스타일")
                        .font(.headline)

                    ScrollView(.horizontal, showsIndicators: false) {
                        HStack(spacing: 12) {
                            ForEach(["레이어드컷","애즈펌","태슬컷"], id: \.
                                self) { tag in
                                Text(tag)
                                    .padding(.horizontal, 12)
                                    .padding(.vertical, 8)
                                    .background(Color(.systemGray5))
                                    .cornerRadius(20)
                            }
                        }
                    }

                    VStack(alignment: .leading, spacing: 12) {
                        Text("나를 위한 추천")
                            .font(.headline)

                        ForEach(0..<2) { _ in
                            RoundedRectangle(cornerRadius: 12)
                                .fill(Color(.systemGray5))
                                .frame(height: 160)
                                .overlay(Text("포트폴리오")
                                            .padding(8)
                                            .background(Color.black.opacity(0.6))
                                            .foregroundColor(.white)
                                            .cornerRadius(6)
                                            .padding(12), alignment: .bottomLeading)
                        }
                    }
                }
                .padding()
            }
            .navigationBarHidden(true)
        }
    }
}

struct HomeView_Previews: PreviewProvider {
    static var previews: some View {
        HomeView()
    }
}
