# Jlox ⚙️


An abstract syntax tree walk interpreter written in Java for a modified version of [lox](https://craftinginterpreters.com/the-lox-language.html) with support for better control flow (e.g. continue, break statements), improved error messages and many other fun features!


## Run guide  📝

First, clone the repository locally:
```bash
git clone https://github.com/Sarakuzoi/jlox.git
```
Then in your IDE of choice run the `Lox.java` class. You can run the interpreter with or without a CLI argument specifying an input file for the program.

If you choose to run the program without an input file, you will be promped with a REPL environment in your console:
```
> var a = 4;
> var b = 2;
> a + b == 6;
true
```
## Example code 🎮

Here is a simple program showcasing some of JLox's syntax with an (inefficient) implementation of a program printing out the first 20 Fibonacci numbers:

```java
fun fib(n) {
    if (n <= 1) return n;
    return fib(n - 2) + fib(n - 1);
}

for (var i = 0; i < 20; i = i + 1) {
    print fib(i);
}
```



## License ⚖️

This project is released under MIT license. Check out the [LICENSE](LICENSE) file for more information.