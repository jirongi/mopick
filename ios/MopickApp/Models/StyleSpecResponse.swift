import Foundation

struct StyleSpecResponse: Codable {
    enum AnalysisStatus: String, Codable { case OK, AI_DISABLED, AI_FAILED }
    var analysisStatus: AnalysisStatus?
    var vocabulary: [String: [String]]?
    // observations 등은 필요한 값에 맞춰 확장하세요.
}
