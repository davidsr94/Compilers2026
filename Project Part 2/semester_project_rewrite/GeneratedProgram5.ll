; LLVM-like IR for Program 5
declare i32 @printf(i8*, ...)
@.intfmt = private constant [4 x i8] c"%d\0A\00"
define i32 @main() {
  %a = alloca i32
  store i32 1, i32* %a
  ret i32 0
}
