import re

path = "app/src/main/java/com/example/MainActivity.kt"
with open(path, "r", encoding="utf-8") as f:
    text = f.read()

# 1. Share button
text = re.sub(
    r'(Icon\(Icons\.Default\.Share[^)]+\)\s*Spacer\(modifier = Modifier\.width\(4\.dp\)\))\s*\}',
    r'\1\n                        Text("Compartir", fontSize = 11.sp, color = Color.White)\n                    }',
    text
)

# 2. Call button
text = re.sub(
    r'(Icon\(Icons\.Default\.Call[^)]+\)\s*Spacer\(modifier = Modifier\.width\(4\.dp\)\))\s*\}',
    r'\1\n                        Text("Llamar", fontSize = 11.sp, color = Color.White)\n                    }',
    text
)

# 3. DeleteSweep button
text = re.sub(
    r'(Icon\(Icons\.Default\.DeleteSweep[^)]+\)\s*Spacer\(modifier = Modifier\.width\(4\.dp\)\))\s*\}',
    r'\1\n                        Text("Limpiar", fontSize = 10.sp, color = Color.LightGray)\n                    }',
    text
)

# 4. PersonAdd button
text = re.sub(
    r'(Icon\(Icons\.Default\.PersonAdd[^)]+\)\s*Spacer\(modifier = Modifier\.width\(6\.dp\)\))\s*\}',
    r'\1\n                        Text("Añadir", fontSize = 12.sp, color = Color.White)\n                    }',
    text
)

# 5. Block button
text = re.sub(
    r'(Icon\(Icons\.Default\.Block[^)]+\)\s*Spacer\(modifier = Modifier\.width\(4\.dp\)\))\s*\}',
    r'\1\n                        Text("Bloquear", fontSize = 11.sp, color = Color.White)\n                    }',
    text
)

with open(path, "w", encoding="utf-8") as f:
    f.write(text)

print("Successfully fixed remaining 5 button cutoffs via script file!")
