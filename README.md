# Expense Tracker (Java)

A console-based Expense Tracker application built using Java and clean layered architecture principles.

## Features
- Add, view, edit, and delete expenses
- Category-wise expense summary
- Total expense calculation
- Persistent storage using file system
- Input validation for expense amount

## Tech Stack
- Java 17
- Object-Oriented Programming (OOP)
- File I/O

## Project Structure
- model – contains data models
- repository – handles file-based persistence
- service – contains business logic
- Main – application entry point

## How to Run

Compile:
```bash
javac src\model\*.java src\repository\*.java src\service\*.java src\Main.java

## Roadmap
- [x] Core Java version with clean architecture
- [ ] Spring Boot REST API
- [ ] MySQL integration

