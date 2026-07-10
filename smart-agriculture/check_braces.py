with open("/app/src/main/java/com/example/ui/screens/AgriScreens.kt", "r") as f:
    lines = f.readlines()

stack = []
for i, line in enumerate(lines):
    line_num = i + 1
    for char in line:
        if char == '{':
            stack.append(('{', line_num))
        elif char == '}':
            if len(stack) == 0:
                print(f"Extra closing brace at line {line_num}")
            else:
                stack.pop()

if stack:
    print(f"Unclosed braces: {len(stack)}")
    for item in stack[:20]:
        print(f"  Unclosed brace of type '{item[0]}' opened at line {item[1]}")
else:
    print("All braces are fully balanced!")
