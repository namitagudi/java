# ESCAPE THE CODE
A JavaFX escape-room mini project. Solve logic/programming puzzles to unlock rooms, collect XP and escape.

## Requirements
- JDK 17+
- Maven 3.9+
- Internet for first Maven dependency download

## Run
From the project folder:
```bash
mvn javafx:run
```
Windows: if Maven is not installed, install Maven and ensure `mvn` is on PATH.

## Features
- Game-themed home screen, mission progression and XP
- 3 playable rooms with puzzle types: logic, code output, cipher
- Hint system (uses a hint token)
- Mission timer (3 minutes per room)
- Collect room key and unlock next mission
- Progress saved locally in `~/.escape-the-code/progress.properties`
- Reset progress button

## Rooms
1. Locked Laboratory — logic sequence
2. Forgotten Server — Java output puzzle
3. Cipher Vault — simple shift decoding

## Notes
This is a starter mini-project, intentionally designed so you can extend it with more rooms,
inventory objects, animated scenes, sound effects, a leaderboard, and additional puzzle categories.
