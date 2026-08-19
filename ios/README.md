Mopick iOS UI Skeleton

이 폴더에는 `Mopick` iOS 앱용 SwiftUI 뼈대 코드가 들어 있습니다.

빠른 시작:

1. Xcode에서 새 iOS 프로젝트를 생성합니다 (App, Interface: SwiftUI, Language: Swift).
2. 생성한 Xcode 프로젝트에 `ios/MopickApp` 아래의 Swift 파일들을 추가합니다.
3. 시뮬레이터에서 앱을 실행하고 `ApiClient.baseURL`을 필요에 따라 조정하세요 (기본: `http://localhost:8080`).

참고:
- 이 스켈레톤은 백엔드의 기존 엔드포인트(`/api/analyze-goal`, `/api/analyze-portfolio`, `/api/evidence-match`)와 통신하도록 `ApiClient`를 제공합니다.
- 실제 이미지 업로드, 인증, 에셋 등은 필요에 따라 확장하세요.
