; LLVM-like IR for Program 2
declare i32 @printf(i8*, ...)
@.intfmt = private constant [4 x i8] c"%d\0A\00"
define i32 @main() {
  %a = alloca i32
  %b = alloca i32
  store i32 0, i32* %b
  %"bee" = alloca i32
  store i32 1, i32* %b
  store i32 1, i32* %a
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 1)
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 1)
  call i32 (i8*, ...) @printf(i8* getelementptr ([4 x i8], [4 x i8]* @.intfmt, i32 0, i32 0), i32 0)
  ret i32 0
}
