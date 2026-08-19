import Foundation

final class ApiClient {
    static let shared = ApiClient()
    var baseURL = URL(string: "http://localhost:8080")!

    private init() {}

    func analyzeGoal(imageData: Data, completion: @escaping (Result<StyleSpecResponse, Error>) -> Void) {
        let url = baseURL.appendingPathComponent("/api/analyze-goal")
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        let boundary = UUID().uuidString
        req.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")

        var body = Data()
        body.append("--\(boundary)\r\n".data(using: .utf8)!)
        body.append("Content-Disposition: form-data; name=\"image\"; filename=\"img.jpg\"\r\n".data(using: .utf8)!)
        body.append("Content-Type: image/jpeg\r\n\r\n".data(using: .utf8)!)
        body.append(imageData)
        body.append("\r\n--\(boundary)--\r\n".data(using: .utf8)!)
        req.httpBody = body

        URLSession.shared.dataTask(with: req) { data, resp, err in
            if let err = err { completion(.failure(err)); return }
            guard let data = data else { completion(.failure(NSError(domain:"", code:-1))); return }
            do {
                let decoded = try JSONDecoder().decode(StyleSpecResponse.self, from: data)
                completion(.success(decoded))
            } catch {
                completion(.failure(error))
            }
        }.resume()
    }

    func evidenceMatch(requestBody: EvidenceMatchRequest, completion: @escaping (Result<Data, Error>) -> Void) {
        let url = baseURL.appendingPathComponent("/api/evidence-match")
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        do {
            req.httpBody = try JSONEncoder().encode(requestBody)
        } catch { completion(.failure(error)); return }

        URLSession.shared.dataTask(with: req) { data, resp, err in
            if let err = err { completion(.failure(err)); return }
            guard let data = data else { completion(.failure(NSError(domain:"", code:-1))); return }
            completion(.success(data))
        }.resume()
    }
}

// Simple request model for evidence-match
struct EvidenceMatchRequest: Codable {
    struct Tag: Codable { var field: String; var value: String }
    var confirmedTags: [Tag]
    var goalImageBase64: String?
    var portfolioIds: [String]
}
