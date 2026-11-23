# TETRIS (26-2 팀 5)

Java Swing 으로 만든 테트리스. 1인 모드, 2인 대전(네트워크/AI), 상점과 아이템, 랭킹이 들어 있다.
화면은 Figma 디자인(2000x1422)을 좌표 그대로 옮겨 그리고, 창 크기에 맞춰 비율만 조절한다.

## 실행

IntelliJ 에서는 `src/kr/ac/jbnu/se/tetris/ui/MainFrame.java` 을 실행하면 된다.

```bash
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp "out;src" kr.ac.jbnu.se.tetris.ui.MainFrame
```

## 모드

| 모드 | 설명 |
|---|---|
| 1인 모드 | 5분 제한. 점수가 오르면 레벨이 오르고 레벨마다 기믹이 하나씩 붙는다. 끝나면 랭킹에 기록을 남긴다 |
| 숨은 레벨 | 레벨 5를 깨면 열린다. 다방향 테트리스 — 판 네 개가 십자로 놓이고 위/아래/좌/우에서 블록이 떨어진다 |
| 2인 대전 | 같은 네트워크에서 한쪽이 방을 만들고(포트 5000) 다른 쪽이 IP 로 접속한다. 참가비를 걸고 이긴 쪽이 가져간다 |
| AI 대전 | 쉬움 / 보통 / 어려움 |
| 상점 | 코인으로 블록 스킨과 아이템 상자를 산다 |

## 조작

기본 조작키는 환경 설정에서 바꿀 수 있다. (1p / 2p 를 따로 둔다)

| 동작 | 1p | 2p |
|---|---|---|
| 좌 / 우 이동 | ← → | A D |
| 시계 / 반시계 회전 | ↑ / Z | W / Q |
| 소프트 드롭 | ↓ | S |
| 하드 드롭 | Space | E |
| 홀드 | C | R |
| 일시정지 | Esc (또는 P) | |

가진 아이템은 `1` `2` 로 쓴다. 메뉴와 상점, 숨은 레벨의 판 고르기는 마우스로도 된다.

## 기믹과 아이템

- **기믹** 14종: 가속, 소멸 블록, 회전 불가, 조작 반전, 투명 블록, 중력 뒤집기, 바닥 상승, 먹물, 랜덤 방향키, 회전 고정, 폭발(한 칸 / 십자 / 정사각 / 일자) 등. 레벨이 오를 때마다 하나씩 무작위로 붙고 동시에 최대 3개까지 걸린다.
- **아이템** 17종(`item/Attack.java`): 2인 대전에서는 상대에게 걸고, 1인 모드에서는 상점에서 산 것을 내 판에 쓴다. 상자 등급이 높을수록 한 번에 많이 나온다(금 3 / 은 2 / 동 1).
- **코인**: 점수 100당 1코인, 레벨 클리어 보너스, 주간 랭킹 보상으로 모은다.

## 폴더

```
src/kr/ac/jbnu/se/tetris/
├─ game/      게임 규칙 (GameBoard), 화면에 그리는 판 (Board), 기믹, 효과음
├─ ui/        화면들 (MainFrame, 메뉴, 1인, 대전, 숨은 레벨, 상점, 설정, 랭킹)
├─ item/      아이템(Attack), 상자(Box), 블록 스킨(Skin)
├─ ai/        AI 플레이어
├─ network/   2인 대전 소켓 연결과 주고받는 메시지
├─ save/      코인·설정·랭킹 저장 (Preferences)
├─ shared/    땅따먹기 — 넓은 판 하나를 둘이 같이 쓰는 모드 (로비에서 고르면 따로 창이 뜬다)
├─ images/    아이콘과 블록 그림
└─ font/      픽셀 글꼴
```

## 자가 점검

테스트 프레임워크 없이 클래스마다 `main` 에 `assert` 를 적어 두었다. `-ea` 를 붙여 실행한다.

```bash
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.game.GameBoardSelfCheck
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.game.Gimmick
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.save.Wallet
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.save.Settings
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.ui.GameKeys
java -ea -cp "out;src" kr.ac.jbnu.se.tetris.ui.HiddenPanel
```
