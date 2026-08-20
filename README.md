# SHINE Backend

임신부 산전검사 결과지를 임신 주수 기준으로 다시 읽어주는 서비스의 서버입니다.

프론트엔드: https://github.com/LikeLionHGU/SHINE_Front

<br/>

## 왜 만들었나

병원에서 받은 산전검사지에는 이런 줄이 스무 개쯤 적혀 있습니다.

```
혈색소(Hb)          10.8 g/dL       (12.0~16.0)
```

참고치를 벗어났으니 빈혈처럼 보입니다. 그런데 임신 중이라면 정상입니다.

임신하면 혈장량이 크게 늘어 혈색소가 자연히 희석됩니다. 학회 기준은 주수에 따라 `10.5~14.0` 수준인데, 검사지에 인쇄된 `12.0~16.0`은 임신하지 않은 성인 기준이거든요. 검사지만 보면 멀쩡한 사람이 빈혈 환자가 됩니다.

반대도 있습니다. 검사지 기준으로는 정상인데 임신 중 기준으로는 조치가 필요한 항목도 있어요. 어느 쪽이든 인쇄된 참고치를 그대로 믿으면 안 됩니다.

그래서 검사지 한 줄 한 줄을 임신 주수 기준과 대조해 네 가지로 판정하고, 다음 진료 때 물어볼 질문까지 만들어 둡니다.

| 상태 | 뜻 |
|---|---|
| 안심 | 임신 주수 기준 범위 안 |
| 주의 | 기준을 벗어남 / 재검이 필요함 |
| 위험 | 담당 의료진과 바로 상의 필요 |
| 확인 필요 | 값을 못 읽었거나, 판정 기준이 없는 항목 |

<br/>

## 기술 스택

| 구분 | 사용 |
|---|---|
| 언어 · 프레임워크 | Java 21, Spring Boot 4.1, Spring Data JPA |
| 데이터 | MySQL 8.4, Redis 7 |
| 인증 | JWT (Access / Refresh) |
| 인프라 | Docker Compose, Caddy, Gabia |
| 외부 API | OpenAI (`gpt-4o` OCR, `gpt-4o-mini` 요약) |

<br/>

## 판정이 만들어지는 과정

```
검사지 사진
   → OCR              표를 읽어 항목명·값·단위·참고치를 뽑는다
   → LabelNormalizer  병원마다 다른 표기를 카탈로그와 맞춘다
   → ValueParser      "음성(0.07)" 을 판정과 측정치로 분해한다
   → UnitNormalizer   µ · μ · ㎕ 를 하나로 접는다
   → ResultEvaluator  ① 임신 주수 기준  ② 검사지 참고치 순으로 대조한다
   → VerdictGenerator 판정에 맞는 설명 문장을 코드가 만든다
   → 판정 + 근거 + 추천 질문
```

핵심은 `ResultEvaluator`의 순서입니다. 카탈로그에 `is_pregnancy_specific`이 켜진 항목은 검사지에 인쇄된 참고치를 무시하고 임신 기준을 씁니다. 앞의 혈색소 사례가 여기서 갈립니다.

<br/>

## 설계에서 지킨 것

**판정은 AI가 하지 않습니다.** 수치 판정은 전부 코드가 결정론적으로 계산합니다. AI는 사진에서 글자를 옮겨 적고, 이미 나온 판정을 문장으로 풀어쓰는 데만 씁니다. 같은 검사지를 두 번 올리면 같은 답이 나와야 하고 왜 그 판정인지 근거를 댈 수 있어야 하는데, 생성형 모델은 둘 다 보장하지 못합니다.

**애매하면 이상 쪽으로 기웁니다.** 판정 기준을 모르거나 값을 못 믿겠으면 정상이라고 추측하지 않고 "확인 필요"로 둡니다. 이상 수치를 정상이라고 말하면 병원에 가야 할 사람이 안 가게 되지만, 정상을 확인 필요라고 하면 한 번 더 물어보게 될 뿐입니다. 두 실패의 무게가 다르니 기울일 방향도 정해져 있습니다.

**못 읽은 항목도 버리지 않습니다.** 카탈로그에 매칭되지 않은 줄도 원문을 그대로 저장합니다. 나중에 항목 표기를 보강하면 살릴 수 있습니다. 지워버리면 사용자가 검사지를 다시 꺼내야 합니다.

<br/>

## 구조

```
domain/
  auth          회원가입 · 로그인 · 토큰 재발급
  user          프로필 · 임신 정보 · 알림 설정
  testsheet     검사지 업로드 → 파싱 → 판정 → 조회
  testitem      검사 항목 카탈로그와 이름 매칭
  analysis      항목별 추이 조회
  record        기록 타임라인
  appointment   진료 일정 · 캘린더
  question      진료 때 물어볼 질문
  briefing      진료 직전 요약
  home          홈 화면 집계
  nutrition     추천 식재료 화이트리스트
  ai            OpenAI 프록시
  compat        앱 화면 모양에 맞춘 응답 계층

global/
  jwt · config · exception · response · storage · entity
```

`compat`이 따로 있는 이유는 앱이 자체 판정 엔진을 갖게 되면서 역할이 갈렸기 때문입니다. 엔진이 판정한 항목은 앱 판정을 그대로 쓰고, 엔진이 모르는 항목만 서버 판정을 폴백으로 씁니다. 어느 쪽을 쓰든 화면에 보이는 값과 DB에 남는 값은 항상 같습니다. 다르면 업로드 직후엔 "안심"이던 항목이 기록 탭에서 다른 상태로 바뀝니다.

<br/>

## API

전부 `/api/v1` 아래이고, 응답은 `ApiResponse<T>`로 감쌉니다.

```json
{ "success": true, "code": null, "message": null, "data": {} }
```

| 그룹 | 엔드포인트 |
|---|---|
| 인증 | `POST /auth/signup` · `login` · `reissue` · `logout` |
| 사용자 | `GET·PATCH /users/me` · `/me/pregnancy` · `/me/settings` |
| 검사지 | `POST /test-sheets` · `/{id}/images` · `GET /{id}` · `/{id}/status` |
| 검사지(앱) | `POST /reports` |
| 분석 | `GET /analysis/items` · `/items/{id}` |
| 일정 | `GET /calendar` · `GET·POST·PATCH·DELETE /appointments` |
| 질문 | `GET·POST /questions` · `PATCH·DELETE /questions/{id}` |
| 브리핑 | `GET /appointments/{id}/briefing` |
| 앱 화면 | `GET·PATCH /app/me` · `GET /app/records` · `/trends` · `/visits` |
| AI 프록시 | `POST /ai/chat/completions` · `GET /ai/quota` |

전체 명세는 서버를 띄운 뒤 `/swagger-ui.html`에서 볼 수 있습니다.

### 검사지를 올리는 두 가지 경로

`POST /test-sheets`는 사진을 서버에 올리면 서버가 OCR부터 판정까지 합니다. 즉시 202를 주고 `GET /{id}/status`를 2초 간격으로 폴링합니다.

`POST /reports`는 앱이 OCR과 판정을 끝낸 뒤 결과만 보냅니다. 현재 앱이 쓰는 경로입니다. 사진이 없으므로 저장 후 `POST /test-sheets/{id}/images`로 원본을 따로 붙입니다.

`/reports` 응답은 요청과 같은 개수·같은 순서를 보장합니다. 앱이 배열 인덱스로 짝짓기 때문에, 서버가 정렬이나 중복 제거를 한 번만 해도 다른 항목의 이름이 붙습니다. 인덱스 대신 쓸 수 있도록 응답 항목마다 `resultId`를 함께 내려줍니다.

<br/>

## 실행

Java 21과 Docker가 필요합니다.

```bash
docker compose up -d          # MySQL(3307) · Redis(6379)
./gradlew bootRun
```

http://localhost:8080/swagger-ui.html

스키마와 시드는 `src/main/resources/db/`에 있습니다. 순서대로 실행합니다.

```bash
for f in 01_schema 02_seed_test_items 03_seed_extra_items 04_migration_2026-08-21; do
  mysql -h 127.0.0.1 -P 3307 -uroot -p shine < src/main/resources/db/$f.sql
done
```

`ddl-auto: validate`라 스키마가 엔티티와 어긋나면 기동 자체가 막힙니다. 마이그레이션을 건너뛰면 서버가 뜨지 않습니다.

### 테스트

```bash
./gradlew test
```

파서·매처 테스트는 단독으로 돌지만, `TestSheetAnalyzerTest`는 시드 카탈로그를 쓰므로 MySQL·Redis가 떠 있어야 합니다.

### 배포

```bash
cp .env.prod.example .env     # 값을 채운다
docker compose -f docker-compose.prod.yml --env-file .env up -d --build
```

<br/>

## 데이터

```
users               사용자 · 임신 정보 · 알림 설정
test_item_catalog   검사 항목 62개, 표기 300개, 임신 기준 범위
test_sheets         검사지 한 장 (사진 · 검사일 · 주수 · 분석 상태)
test_results        검사지의 한 줄 (값 · 단위 · 판정 · 근거)
appointments        진료 일정
questions           진료 때 물어볼 질문
```

카탈로그 한 항목은 이렇게 생겼습니다.

```sql
('HB', '혈색소', 'Hemoglobin', 'HEMATOLOGY', 'NUMBER', 'g/dL',
 11.0, 15.0, NULL, 0.1, 30, 0.100,
 TRUE,   -- is_pregnancy_specific : 검사지 참고치를 무시하고 이 범위를 쓴다
 TRUE,   -- is_trendable          : 추이 그래프에 그린다
 '["혈색소","헤모글로빈","Hb","HGB","Hemoglobin"]',
 '혈색소는 혈액 속에서 산소를 몸 곳곳으로 운반해주는 단백질이에요.', 1)
```

`name_variants`가 병원마다 다른 표기를 흡수하고, `hard_limit_min/max`는 OCR이 자릿수를 잘못 읽었을 때(`10.8` → `108`) 걸러냅니다.

`test_results`는 검사 당시의 `test_date`와 `pregnancy_week`를 복사해 둡니다. 정규화 관점에서는 중복이지만 판정 시점의 기준이 보존됩니다. 사용자가 나중에 출산예정일을 고쳐도 과거 판정이 소급해서 바뀌면 안 되니까요.

<br/>

