import os
import re

main_dir = 'src/main/java'
test_dir = 'src/test/java'

def find_java_files(directory):
    java_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.java'):
                # Get path relative to the directory root
                rel_path = os.path.relpath(os.path.join(root, file), directory)
                java_files.append(rel_path)
    return java_files

main_files = find_java_files(main_dir)
test_files = find_java_files(test_dir)

missing_tests = []
for main_file in main_files:
    if main_file.endswith('Exception.java'):
        continue
    # skip interfaces
    with open(os.path.join(main_dir, main_file), 'r', encoding='utf-8') as f:
        content = f.read()
        if 'interface ' in content and 'class ' not in content:
            continue
        # simple check for interface/enum maybe?
        
    test_path = main_file.replace('.java', 'Test.java')
    if test_path not in test_files:
        missing_tests.append(main_file)

print('=== Files Missing Corresponding *Test.java ===')
for m in sorted(missing_tests):
    print(m)

# Let's do a quick regex for public methods missing javadoc
print('\n=== Files with potentially missing Javadoc on public methods ===')
method_pattern = re.compile(r'(?<!\*/\s)(?<!\*/\n)(?<!\*/\r\n)(?:public|protected)\s+(?:static\s+)?(?:final\s+)?[\w<>\.,\s]+\s+(\w+)\s*\(')

for main_file in main_files:
    with open(os.path.join(main_dir, main_file), 'r', encoding='utf-8') as f:
        content = f.read()
        # To avoid being too complex in python, let's just do a simpler grep instead later or report files missing /**
        if '/**' not in content:
            print('No javadoc at all:', main_file)

