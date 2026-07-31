# 파일 업로드 확장자 차단 시스템

`PRD.md` 기반 구현. 현재 단계는 프로젝트 기반 설정(기술 스택, DB 스키마, 최소 라우팅)까지만 포함합니다.

## 기술 스택

- Backend: Spring Boot 4.1.0 (Java 21, Maven)
- DB: H2 (인메모리, `spring.datasource.url=jdbc:h2:mem:extblocker`)
- View: Thymeleaf

## 실행 방법

### 1. 서버 기동

```bash
./mvnw.cmd spring-boot:run   # Windows
./mvnw spring-boot:run       # macOS/Linux
```

기본 포트는 `8080`이며, `http://localhost:8080` 로 접속합니다.

### 2. DB 초기화

별도 명령이 필요 없습니다. 서버가 기동될 때마다 `src/main/resources/schema.sql`,
`data.sql`이 자동으로 실행되어 인메모리 H2 DB에 스키마 생성과 시드 데이터 삽입이
이루어집니다 (`spring.sql.init.mode=always`). DB는 애플리케이션 프로세스가 살아있는
동안 유지되고(`DB_CLOSE_DELAY=-1`), 서버를 재기동하면 항상 초기 상태로 리셋됩니다.

- `blocked_extension`: FIXED 7종(`bat, cmd, com, cpl, exe, scr, js`)이
  `is_blocked=false` 상태로 미리 insert 되어 있습니다.
- `upload_file`: 빈 테이블로 생성만 되어 있습니다 (업로드 이력 추적용, 선택 사항).

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

`BlockedExtensionSeedDataTest`가 FIXED 7종이 `is_blocked=false`로 정확히
시드되었는지, `BlockedExtensionUniqueConstraintTest`가 대소문자 무시 unique
제약(DB 레벨)이 실제로 걸려있는지 검증합니다.

## 현재 단계 범위

- [x] 기술 스택 확정 (Spring Boot + H2 + Thymeleaf)
- [x] `blocked_extension` 테이블 + FIXED 7종 seed data
- [x] `upload_file` 테이블 (이력 추적용, 선택)
- [x] 서버/라우팅 최소 골격 (`/admin/extensions`, `/upload` — 준비 중 placeholder)
- [ ] 정책 관리 화면, 파일 업로드 화면, 검증 로직, API 엔드포인트 → 다음 단계
