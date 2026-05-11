; LLVM-like IR for Program 4
declare i32 @printf(i8*, ...)
@.intfmt = private constant [4 x i8] c"%d\0A\00"
define i32 @main() {
  %val = alloca i32
  store i32 1, i32* %val
  %1 = alloca i32
  store i32 2, i32* %val
  %2 = alloca i32
  store i32 3, i32* %val
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 3)
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 2)
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 1)
  ret i32 0
}
