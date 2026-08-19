import SwiftUI

struct Designer: Identifiable {
    let id = UUID()
    let name: String
    let salon: String
    let location: String
}

struct DesignerListView: View {
    let designers: [Designer] = [
        Designer(name: "우영", salon: "어쩌고 헤어", location: "강남"),
        Designer(name: "수빈", salon: "모아 디자이너", location: "강동")
    ]

    var body: some View {
        List(designers) { d in
            HStack {
                RoundedRectangle(cornerRadius: 8).fill(Color(.systemGray5)).frame(width: 80, height: 80)
                VStack(alignment: .leading) {
                    Text(d.name).bold()
                    Text(d.salon).font(.subheadline).foregroundColor(.secondary)
                }
                Spacer()
                Button("선택하기") {}
            }
            .padding(.vertical, 8)
        }
        .navigationTitle("디자이너를 선택해 주세요")
    }
}

struct DesignerListView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationView { DesignerListView() }
    }
}
