# MOPICK Backend — AI StyleSpec & Evidence Match

희망 사진과 검수된 작업 사례를 같은 기준으로 구조화하고, 선택 근거를 설명하는 백엔드.
헤어스타일을 생성하거나 미용사를 평가하지 않는다.

Spring Boot 4.1 / Java 17 / Spring AI 2.0 / Gemini

## 설계 원칙

**AI는 claim을 만들지 않는다.** 확정 태그가 claim 뼈대를 만들고, AI는 설명만 채운다.

```
확정 태그(사용자) × 확정 태그(미용사)
        ↓  ClaimSkeletonBuilder — AI 없음
   8개 EvidenceClaim 뼈대 (= confirmed_evidence fallback)
        ↓  PairwiseComparator — 사진 1:1 비교
   AI 상태·설명 후보
        ↓  ClaimValidator — 태그 충돌 / 금지 표현 / 길이 검증
   필드 단위로 통과한 것만 반영, 나머지는 뼈대로 롤백
```

이 구조 때문에 AI는 후보 포함 여부와 순서에 관여할 수 없고, API key가 없거나 AI가
실패해도 응답 구조가 동일하다.

## 실행

```bash
./gradlew bootRun
```

키 없이도 기동되며 세 API 모두 `confirmed_evidence` fallback으로 응답한다.
실제 사진 분석과 1:1 비교를 켜려면 AI Studio 키를 넣는다.

```bash
export GEMINI_API_KEY=...
./gradlew bootRun
```

모델 기본값은 `gemini-3.5-flash`이며 `MOPICK_AI_MODEL`로 교체한다.
계정에서 쓸 수 있는 모델은 아래로 확인한다. 임베딩은 되는데 채팅만 404가 나는 모델이 있으므로
채팅 모델은 실제 호출로 확인하는 편이 확실하다.

```bash
curl -H "x-goog-api-key: $GEMINI_API_KEY" https://generativelanguage.googleapis.com/v1beta/models
```

제공자를 바꿀 때 손대는 곳은 세 군데뿐이다. `build.gradle`의 starter, `application.yml`의
`spring.ai.*` 블록, `AiAvailability`가 읽는 프로퍼티 이름. 나머지 AI 코드는 Spring AI
`ChatClient`만 쓰므로 제공자에 묶여 있지 않다.

## API

| 메서드 | 경로 | 요청 | 응답 | AI |
|---|---|---|---|---|
| POST | `/api/analyze-goal` | multipart `image` | `StyleSpecResponse` | 사용 |
| POST | `/api/analyze-portfolio` | multipart `image` | `StyleSpecResponse` | 사용 |
| POST | `/api/matches` | JSON | `MatchResponse` | **미사용** |
| POST | `/api/evidence-match` | JSON | `EvidenceMatchResponse` | 사용(실패 시 fallback) |

사용 순서는 `analyze-goal` → 사용자 확정 → `matches` → 그 후보로 `evidence-match`다.

`matches`는 결정론적이다. 같은 입력이면 항상 같은 후보가 같은 순서로 나오고, AI를 한 번도 부르지 않는다.

```
권리·게시 검수 → 지역 → 회피 미용사
  → 8개 확정 태그 일치 점수 (중요 필드 2배: 계열·길이·앞머리)
  → 미용사당 최고 사례 1건으로 접기
  → 상위 3명, 2명 미만이면 추천하지 않음
```

한쪽이라도 값이 없는 필드는 점수에 넣지 않는다. 0점으로 깎으면 "기록하지 않은 것"이
"다른 것"과 같은 취급을 받아 꼼꼼히 기록한 미용사가 손해를 본다.
내부 유사도 점수는 응답에 담지 않는다. 사용자가 보는 근거는 점수가 아니라 evidence의 claim이다.

`StyleSpecResponse`의 `analysisStatus`는 `OK` / `AI_DISABLED` / `AI_FAILED`다. 8필드가 전부
`NOT_VISIBLE`로 왔을 때 "AI가 못 봤다"인지 "호출이 실패했다"인지 구분해준다. `AI_FAILED`면
잠시 후 재시도할 값어치가 있다.

`StyleSpecResponse`는 `vocabulary`를 함께 내려준다. 사용자·미용사가 값을 고치는 화면이
서버와 같은 허용 목록을 써야 확정 태그가 어긋나지 않는다.

```bash
curl -X POST localhost:8080/api/evidence-match -H 'Content-Type: application/json' -d '{
  "confirmedTags": [
    {"field":"LENGTH","value":"쇄골"},
    {"field":"BANGS","value":"시스루뱅"}
  ],
  "goalImageBase64": null,
  "portfolioIds": ["pf_0001","pf_0003"]
}'
```

응답은 `similar` / `different` / `toCheck` 세 묶음이다. 내부 유사도 점수는 노출하지 않는다.

## 8개 관찰 필드

`STYLE_FAMILY` `LENGTH` `BANGS` `SIDE_SILHOUETTE` `BACK_SILHOUETTE` `TOP_VOLUME` `PART` `TEXTURE`

값은 `Vocabulary`의 허용 목록에 있는 것만 인정한다. 목록 밖의 값은 AI가 냈든 클라이언트가
보냈든 "모름"으로 처리된다. 자유 텍스트를 허용하면 태그 비교가 무너지기 때문이다.

## AI가 하지 않는 것

미용사의 실력·등급·성공률·순위 평가 / 동일 결과와 재현 가능성 보장 / 사용자 모발에서의
시술 가능성 판정 / 얼굴형·외모·성별·인종·나이·신원 추론 / 사진만 보고 커트·펌·약제·시술법
추론 / 포트폴리오 진위 자동 인증.

프롬프트(`Prompts.SAFETY`)로 지시하고, `ForbiddenPhrasePolicy`가 응답을 다시 검사한다.
걸리면 그 필드는 확정 태그 설명으로 대체된다.

사용자 표현은 다음으로 고정한다.

> 원하는 스타일과 관련된 작업을 해본 근거가 있는 미용사

## 호출 절약 장치

무료 티어 한도(분당 20회) 안에서 데모가 끊기지 않게 두 가지가 붙어 있다.

- `AiRateLimiter` — 한도를 넘겨 429를 받느니 앞단에서 줄을 세운다. 429는 재시도까지 한도를
  먹어 손해가 크다. 최대 대기를 넘기면 AI를 포기하고 fallback으로 내려간다.
- `StyleSpecCache` — 같은 사진 + 같은 프롬프트면 재호출하지 않는다. 캐시 키에 프롬프트 지문이
  들어가므로 어휘나 판정 기준을 고치면 자동으로 무효화된다. 실패는 캐시하지 않는다.

```bash
MOPICK_AI_MAX_CALLS=15 MOPICK_AI_CACHE=false ./gradlew bootRun   # 정확도 측정용
```

안정성(같은 사진 반복 호출)을 측정할 때는 캐시를 반드시 꺼야 한다.

## 현재 상태와 남은 일

- 포트폴리오는 `resources/data/portfolios.json` 인메모리 fixture다. DB는 계약이 굳은 뒤 붙인다.
- 작업 사진은 `resources/data/portfolio-images/`에 없으면 1:1 비교를 건너뛴다.
- 원본 사진은 저장하지 않는다. 정제된 바이트가 요청 처리 중에만 존재한다.
- `/api/matches`의 필터는 권리·게시·지역·회피까지 구현했다. 스펙의 "시술 조건"은 포트폴리오에
  구조화된 시술 필드가 없어 미구현이다. 필드를 정한 뒤 필터 한 줄을 추가하면 된다.
- 중요 필드를 계열·길이·앞머리로 정한 것은 제품 판단이다. `MatchWeights` 한 곳에서 조정한다.
- EXIF 회전 보정이 없다. 세로로 누운 사진은 관찰 정확도가 떨어진다.
- production 전: 권리 확보 이미지, 전문가 holdout, 필드별 정확도, 금지 표현, 비용과 지연시간 검증.
