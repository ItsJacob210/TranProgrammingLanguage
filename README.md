# Tran Language

Tran is an object-oriented interpreted programming language designed and implemented in Java. It features full language functionality, including a lexer, recursive-descent parts, AST, and a tree-walking interpreter. Tran was developed while taking ICSI311 at the University at Albany.

## Layout
- src/
  - Lexer, Parser, TokenManager, SyntaxErrorException, TextManager
  - AST/… (nodes for classes, interfaces, methods, expressions, statements)
  - Interpreter/… (interpreter core + IDT runtime types, builtin's)
  - JUnit tests for lexer/parser/interpreter
- EBNF/ (grammar struct)

## Purpose
- Tran was designed & developed to demonstrate practical skills in:
  - Language design: Implementing a consistent & extendable object-oriented language
  - Parsing Techniques: Applying recursive-decent parsing to generate an Abstract Syntax Tree
  - Interpreter Development: Executing code through a tree-walking interpreter with runtime type management
  - Software testing: Ensures correctness & AST structure with comprehensive JUnit tests
