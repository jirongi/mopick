# MOPICK Backend — AI StyleSpec & Evidence Match

희망 사진과 검수된 작업 사례를 같은 기준으로 구조화하고, 선택 근거를 설명하는 백엔드.
헤어스타일을 생성하거나 미용사를 평가하지 않는다.

Spring Boot 4.1 / Java 17 / Spring AI 2.0 / Gemini · OpenAI (환경변수로 전환)

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

제공자는 **폴백 체인**으로 묶여 있다. 앞에서부터 시도하고, 실패하면 다음으로 넘어간다.
키가 없는 제공자는 체인에서 자동으로 빠지므로 키 하나만 넣으면 단독 운영이 된다.

```bash
GEMINI_API_KEY=... OPENAI_API_KEY=... ./gradlew bootRun   # Gemini 우선, 실패 시 OpenAI
GEMINI_API_KEY=... ./gradlew bootRun                      # Gemini 단독
./gradlew bootRun                                         # AI 없이 fallback만
```

순서는 `MOPICK_AI_PROVIDERS`로 바꾼다(기본 `google-genai,openai`).

평소에는 앞 제공자만 쓰인다. 뒤 제공자는 앞이 한도에 걸리거나 차단됐을 때만 호출되며,
정확도가 낮더라도 빈 결과보다는 낫다는 판단이다. 어느 제공자가 응답했는지는 로그에 남는다.

```
StyleSpec 분석[google-genai] 실패 (1회 시도): ... 403 Your project has been denied access
StyleSpec 분석 - 'google-genai' 실패, 'openai'로 넘어간다
StyleSpec 분석 - 앞선 제공자 실패로 'openai'가 응답했다
```

**두 모델을 합쳐 쓰는 방식(합의 앙상블)은 넣지 않았다.** 실측에서 오히려 나빴기 때문이다.
두 모델이 갈린 18개 필드 중 정답 판정이 가능한 10건에서 Gemini가 전부 옳았고 OpenAI가 이긴
사례는 없었다. 합의를 요구하면 값 채움이 87%→43%로 떨어지면서 정밀도도 85%→80%로 같이 내려간다.
앙상블은 두 모델의 실력이 비슷할 때만 이득이다.

### 모델 비교 (남성 사진 4장, 같은 프롬프트로 실측)

| 모델 | 정답지 일치 | 반복 호출 안정성 | temperature 0 |
|---|---|---|---|
| **gemini-3.5-flash** | **86%** | **93~100%** | 가능 |
| gpt-4.1 | 43% | 100% | 가능 |
| gpt-4o | 43% | 84% | 가능 |
| gpt-5 | 78% | 68% | **불가**(1 고정) |

Gemini를 기본으로 두는 이유는 정확도와 안정성이 함께 가장 높기 때문이다.
같은 사진에서 같은 태그가 나와야 확정 태그와 매칭이 흔들리지 않는다.

OpenAI 점수가 낮은 것은 vision 성능 자체보다 **판정 기준 문장이 Gemini에 맞춰 세 라운드에 걸쳐
다듬어진 탓이 크다.** 특히 "이마가 안 보이면 앞머리를 NOT_VISIBLE로 두라"는 규칙을 OpenAI는
옆모습 사진에까지 과하게 적용한다. OpenAI를 주력으로 쓰려면 `Vocabulary`의 criteria를
그 모델 기준으로 다시 조정해야 한다.

gpt-5는 정확도가 높지만 `temperature` 0을 받지 못해 같은 사진에도 값이 흔들린다.
확정 태그가 근거인 서비스에는 맞지 않는다.

모델과 온도는 `MOPICK_GEMINI_MODEL` / `MOPICK_OPENAI_MODEL` / `MOPICK_AI_TEMPERATURE`로 바꾼다.

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

## 기하 측정 (`geometry/`)

8필드 중 **좌표로 잴 수 있는 필드**는 LLM 눈대중 대신 픽셀 측정으로 정한다.

```
① 측정  사진 → HeadMeasurement (귀·턱·두상·머리카락 좌표)   ← HeadSegmenter 구현체
② 판정  측정값 → 필드값                                    ← GeometricFieldDeriver
③ 병합  기하 + LLM → 최종 StyleSpec                        ← GeometryMerger
```

| 필드 | 판정 |
|---|---|
| `LENGTH` | **기하** — 머리카락 최하단이 귀 하단보다 얼마나 아래인지 |
| 나머지 7개 | LLM 관찰 |

**기하로 다루는 건 `LENGTH` 하나다.** 나머지 후보 둘은 실측 결과 근거가 부족해 뺐다.

- `TOP_VOLUME` — 두상 최상단이 언제나 머리카락에 덮여 있어 "두상 위 머리 두께"를 잴 수 없다.
  귀 상단 기준 대체 지표를 실측했더니 정답과 순서가 **반대로** 나왔다.
- `SIDE_SILHOUETTE` — 옆·뒷모습에서는 보이는 얼굴 폭이 작아져 비율이 부풀려진다.
  가장 붙은 머리(투블럭 뒷모습)가 가장 큰 값으로 나왔다.
- `STYLE_FAMILY`·`BANGS`·`TEXTURE` — 의미·질감 판단이라 애초에 기하로 환원되지 않는다.

**단위는 귀 상단~턱**이다. 정수리~턱을 쓰고 싶지만 두상 최상단은 관측할 수 없다.

임계값은 실측으로 정했다. 짧은 커트도 뒷목 잔머리가 늘 귀 아래로 조금은 내려오기 때문에
0으로 두면 전부 "귀덮음"이 된다. 사진 4장의 `(머리끝 − 귀하단) ÷ 단위`:

```
투블럭 -0.28   짧은펌 -0.03   귀 드러난 커트 0.17   목덜미까지 오는 커트 0.33
                              └────────── 임계값 0.25 ──────────┘
```

병합 규칙은 하나다 — **잰 필드는 측정이 이기고, 못 잰 필드는 LLM 관찰이 남는다.**

### 측정기 켜기

`models/face-parsing.onnx`를 두면 `OnnxHeadSegmenter`가 JVM 안에서 모델을 돌린다.
파일이 없으면 아무것도 재지 않고 전부 LLM 관찰로 채워지며, 기동은 정상적으로 된다.

```bash
MOPICK_FACE_PARSING_MODEL=/path/to/model.onnx ./gradlew bootRun
```

모델 요구 사항과 라이선스 주의는 `models/README.md` 참고. 출력이 CelebAMask-HQ 19분류
체계여야 하며, 다른 체계를 쓰려면 `FaceParsingMask`의 상수만 바꾸면 된다.

### 왜 필요했나 — 그리고 결과

`LENGTH`는 판정 기준 문장을 세 라운드에 걸쳐 고쳤는데도 두 사진(귀가 드러나는 짧은 커트 /
뒷머리가 목까지 오는 커트)을 끝내 구분하지 못했다. 기준을 바꿀 때마다 두 사진이 **함께**
움직였다 — 규칙은 일관되게 적용되는데 모델이 두 사진의 차이 자체를 지각하지 못한 것이다.

측정으로 바꾼 뒤 실제 사진 4장 결과(`EndToEndLengthTest`):

```
1.jpeg 귀위   2.jpeg 귀위   3.jpeg 귀덮음   4.jpeg 귀위
```

3회 반복해도 값이 동일하다. LLM 방식의 흔들림이 사라졌다.

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
