import SwiftUI

struct StyleSelectionView: View {
    enum Length: String, CaseIterable { case unknown = "선택해 주세요", short = "숏", medium = "미디엄", long = "롱" }
    @State private var length: Length = .unknown
    @State private var styleFamily: String = ""
    @State private var bangs: String = ""
    @State private var topVolume: String = ""

    var body: some View {
        NavigationView {
            Form {
                Section(header: Text("스타일")) {
                    TextField("스타일을 입력", text: $styleFamily)
                }
                Section(header: Text("길이")) {
                    Picker("길이", selection: $length) {
                        ForEach(Length.allCases, id: \.self) { l in
                            Text(l.rawValue).tag(l)
                        }
                    }
                    .pickerStyle(MenuPickerStyle())
                }
                Section(header: Text("앞머리")) {
                    TextField("앞머리", text: $bangs)
                }
                Section(header: Text("상단 볼륨")) {
                    TextField("상단 볼륨", text: $topVolume)
                }
                Button(action: { /* 다음 단계로 이동 예정 */ }) {
                    Text("다음으로")
                        .frame(maxWidth: .infinity, alignment: .center)
                }
            }
            .navigationTitle("세부 내용을 선택해 주세요!")
        }
    }
}

struct StyleSelectionView_Previews: PreviewProvider {
    static var previews: some View {
        StyleSelectionView()
    }
}
