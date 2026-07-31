# 파일 업로드 확장자 차단 시스템

`PRD.md` 기반 구현. 확장자 차단 정책 관리 화면, 파일 업로드 화면과 검증 로직, API, Docker/Render 배포까지 포함되어 있습니다.

## 배포 URL

**https://file-upload-rgxf.onrender.com**

- 파일 업로드: `/upload` (루트 `/`로 접속해도 자동으로 이동합니다)
- 확장자 차단 정책 관리: `/admin/extensions`

Render 무료 티어로 배포되어 있어 15분간 요청이 없으면 슬립 상태가 되고, 다음 요청 시 콜드스타트(약 30~50초)가 발생할 수 있습니다. 또한 영구 디스크가 없어 컨테이너가 재시작(슬립 후 재기동, 재배포 등)되면 H2 DB와 업로드된 파일이 모두 초기 상태로 리셋됩니다.

## 기술 스택

- Backend: Spring Boot 4.1.0 (Java 21, Maven)
- DB: H2 (인메모리, `spring.datasource.url=jdbc:h2:mem:extblocker`)
- View: Thymeleaf
- 배포: Docker, Render (무료 티어)

## 주요 기능

- **확장자 차단 정책 관리 화면** (`/admin/extensions`, 관리자 전용): 고정 위험 확장자 7종(`bat, cmd, com, cpl, exe, scr, js`) 체크박스와 커스텀 확장자(최대 200개) 등록/삭제. 별도 저장 버튼 없이 체크/추가/삭제 즉시 DB에 반영됩니다.
- **파일 업로드 화면** (`/upload`, 사용자 전용): 첨부한 파일을 아래 순서로 검증한 뒤 저장합니다.
  1. 매직 넘버 기반 실제 파일 형식 판별 (클라이언트가 보낸 MIME 타입은 신뢰하지 않음)
  2. 이중 확장자 검사
  3. 확장자 위장 검사
  4. 차단 정책 검사 (실제 확장자 기준, 매 요청마다 최신 정책 조회)
  5. 특수 파일명(확장자 없음 / `.`으로 시작 / 매우 긴 파일명) 치환
  6. 저장 및 성공 응답
- 두 화면은 각각 독립된 URL로 진입할 수 있고, 화면 안의 링크로 서로 이동할 수 있습니다. 자세한 요구사항은 `PRD.md` 4장을 참고하세요.

### API

| Method | Endpoint | 설명 |
|---|---|---|
| GET | `/api/extensions/fixed` | 고정 확장자 목록 + 체크 상태 조회 |
| PATCH | `/api/extensions/fixed/{extension}` | 고정 확장자 체크/해제 |
| GET | `/api/extensions/custom` | 커스텀 확장자 목록 조회 |
| POST | `/api/extensions/custom` | 커스텀 확장자 추가 |
| DELETE | `/api/extensions/custom/{id}` | 커스텀 확장자 삭제 |
| POST | `/api/files/upload` | 파일 업로드 (multipart/form-data) |

## 로컬 실행 방법

### 1. 서버 기동

```bash
./mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run       # macOS/Linux
```

기본 포트는 `8080`이며, `http://localhost:8080` 로 접속합니다. `PORT` 환경 변수를 지정하면 그 포트로 기동됩니다 (Render 등 PaaS 배포 시 자동으로 주입됨).

### 2. DB 초기화

별도 명령이 필요 없습니다. 서버가 기동될 때마다 `src/main/resources/schema.sql`,
`data.sql`이 자동으로 실행되어 인메모리 H2 DB에 스키마 생성과 시드 데이터 삽입이
이루어집니다 (`spring.sql.init.mode=always`). DB는 애플리케이션 프로세스가 살아있는
동안 유지되고(`DB_CLOSE_DELAY=-1`), 서버를 재기동하면 항상 초기 상태로 리셋됩니다.

- `blocked_extension`: FIXED 7종(`bat, cmd, com, cpl, exe, scr, js`)이
  `is_blocked=false` 상태로 미리 insert 되어 있습니다.
- `upload_file`: 업로드/거부 이력이 쌓이는 테이블입니다.

### 3. 데이터 확인 (H2 콘솔)

서버 기동 후 `http://localhost:8080/h2-console` 접속:

- JDBC URL: `jdbc:h2:mem:extblocker`
- User Name: `sa`
- Password: (공란)

접속 후 아래 쿼리로 시드 데이터를 확인할 수 있습니다.

```sql
SELECT extension, type, is_blocked FROM blocked_extension;
```

### 4. 테스트 실행

```bash
./mvnw.cmd test
```

`BlockedExtensionSeedDataTest`(시드 데이터), `BlockedExtensionUniqueConstraintTest`(DB 레벨 대소문자 무시 unique 제약), `ExtensionPolicyApiTest`(정책 관리 API), `FileUploadApiTest`(업로드 검증 파이프라인 전체 시나리오)가 PRD의 핵심 요구사항을 검증합니다.

## Docker로 실행하기

```bash
docker build -t file-upload:latest .
docker run -d --name file-upload -p 8080:8080 -v file-upload-data:/app/uploads-data file-upload:latest
```

또는 `docker-compose.yml`을 사용해 한 번에 빌드/실행할 수 있습니다.

```bash
docker compose up -d --build
```

`Dockerfile`은 Maven 이미지로 빌드하고 JRE 이미지로 실행하는 멀티스테이지 구성이며, non-root 사용자로 실행됩니다. `uploads-data` 볼륨을 마운트해 컨테이너를 재생성해도 업로드된 파일이 유지됩니다 (H2 DB는 설계상 인메모리라 이 경우에도 재시작 시 리셋됩니다).

## Render 배포

1. https://dashboard.render.com 에서 GitHub 계정으로 로그인
2. **New +** → **Web Service** → 이 저장소 선택
3. Runtime은 저장소의 `Dockerfile`을 자동 감지 (Docker) / Instance Type은 **Free** 선택
4. **Create Web Service** 클릭 → 빌드 완료 후 할당된 `*.onrender.com` 주소로 접속

## 구현 범위

- [x] 기술 스택 확정 (Spring Boot + H2 + Thymeleaf)
- [x] `blocked_extension`, `upload_file` 테이블 + FIXED 7종 seed data
- [x] 확장자 차단 정책 관리 화면 + API (`/admin/extensions`, `/api/extensions/*`)
- [x] 파일 업로드 화면 + 6단계 검증 파이프라인 + API (`/upload`, `/api/files/upload`)
- [x] 화면 간 내비게이션 (루트 리다이렉트, 상호 이동 링크)
- [x] Docker 배포 지원 (`Dockerfile`, `docker-compose.yml`)
- [x] Render 무료 호스팅 배포
- [ ] 관리자 인증/인가 (Out of Scope, `PRD.md` 9장 참고)
