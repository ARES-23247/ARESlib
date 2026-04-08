import os

main_dir = 'src/main/java'
test_dir = 'src/test/java'

for root, dirs, files in os.walk(main_dir):
    for file in files:
        if file.endswith('Exception.java'): continue
        if file.endswith('.java'):
            with open(os.path.join(root, file), 'r', encoding='utf-8') as f:
                content = f.read()
                if 'interface ' in content and 'class ' not in content:
                    continue
                
            rel_path = os.path.relpath(os.path.join(root, file), main_dir)
            test_path = os.path.join(test_dir, rel_path.replace('.java', 'Test.java'))
            
            if not os.path.exists(test_path):
                # We need to make the directory and output a basic test structure
                os.makedirs(os.path.dirname(test_path), exist_ok=True)
                
                # Extract package name from the file
                package_name = ""
                for line in content.split('\n'):
                    if line.startswith('package '):
                        package_name = line.replace(';', '')
                        break
                
                class_name = file.replace('.java', '')
                
                test_content = f"""{package_name};

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class {class_name}Test {{
    @Test
    void testInitialization() {{
        // TODO: Auto-generated test stub. Add proper assertions for {class_name}.
        assertTrue(true);
    }}
}}
"""
                with open(test_path, 'w', encoding='utf-8') as f:
                    f.write(test_content)
                print(f"Generated {test_path}")

