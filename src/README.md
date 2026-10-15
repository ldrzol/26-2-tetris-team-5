# src

Java 패키지 경로 때문에 `kr/ac/jbnu/se/tetris` 까지 내려가야 코드가 나온다.

```
src/kr/ac/jbnu/se/tetris/
├─ game/      게임 규칙 (GameBoard), 화면에 그리는 판 (Board), 기믹, 효과음
├─ ui/        화면들 (MainFrame, 메뉴, 1인, 대전, 숨은 레벨, 상점, 설정, 랭킹)
├─ item/      아이템(Attack), 상자(Box), 블록 스킨(Skin)
├─ ai/        AI 플레이어
├─ network/   2인 대전 소켓 연결과 주고받는 메시지
├─ save/      코인·설정·랭킹 저장 (Preferences)
├─ shared/    땅따먹기 — 넓은 판 하나를 둘이 같이 쓰는 모드
├─ images/    아이콘과 블록 그림
└─ font/      픽셀 글꼴
```

그림과 글꼴이 `src` 아래에 있으므로 터미널에서 돌릴 때는 클래스패스에 `src` 를 같이 넣는다.
실행 방법과 조작키는 [루트 README](../README.md) 참고.
