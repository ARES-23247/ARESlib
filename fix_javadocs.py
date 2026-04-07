import os
import re

log_file = "javadoc_output.txt"

with open(log_file, "r") as f:
    lines = f.readlines()

warnings = []
for line in lines:
    match = re.search(r'([a-zA-Z0-9\-\_\:\/\\]+\.java)\:(\d+)\: warning\: no \@param for (\w+)', line)
    if match:
        warnings.append({"type": "param", "file": match.group(1), "line": int(match.group(2)), "param": match.group(3)})
        continue
    
    match2 = re.search(r'([a-zA-Z0-9\-\_\:\/\\]+\.java)\:(\d+)\: warning\: no \@return', line)
    if match2:
        warnings.append({"type": "return", "file": match2.group(1), "line": int(match2.group(2))})

def insert_javadoc_tag(file_path, target_line, tag_text):
    if not os.path.exists(file_path):
        return

    with open(file_path, "r", encoding="utf-8") as f:
        content = f.readlines()
        
    # target_line is 1-indexed. The method is typically at target_line
    # We want to find the nearest `*/` above the target_line and insert before it
    # if there is no `*/` within a few lines, we might need to create the javadoc block,
    # but the warning usually implies a javadoc block exists, or we just insert it above the method.
    
    idx = target_line - 1
    found_end_comment = False
    insert_idx = idx
    for i in range(idx, max(-1, idx - 10), -1):
        if "*/" in content[i]:
            insert_idx = i
            found_end_comment = True
            break
            
    if found_end_comment:
        content.insert(insert_idx, f"     * {tag_text}\n")
    else:
        # Create a new javadoc block right above the method
        content.insert(idx, f"    /**\n     * {tag_text}\n     */\n")
        
    with open(file_path, "w", encoding="utf-8") as f:
        f.writelines(content)

# We need to process from bottom of file upwards to not offset line numbers!
# Group by file, then sort by line descending
from collections import defaultdict

file_warnings = defaultdict(list)
for w in warnings:
    file_warnings[w['file']].append(w)

for fpath, wlist in file_warnings.items():
    # Sort descending by line so insertions don't affect previous lines
    wlist = sorted(wlist, key=lambda x: x['line'], reverse=True)
    import os
    
    # robustly find the actual file path by recursively searching src/main/java for the matching basename
    target_basename = os.path.basename(fpath)
    real_path = None
    for root, dirs, files in os.walk("src"):
        if target_basename in files:
            real_path = os.path.join(root, target_basename)
            break
            
    if not real_path:
        print(f"Could not find {target_basename}")
        continue
        
    with open(real_path, "r", encoding="utf-8") as f:
        content = f.readlines()
        
    for w in wlist:
        target_line = w['line']
        if w['type'] == 'param':
            tag_text = f"@param {w['param']} The {w['param']} value."
        else:
            tag_text = "@return The current value."
            
        idx = w['line'] - 1
        if content:
            idx = min(idx, len(content) - 1)
        found_end_comment = False
        insert_idx = idx
        # look up to 15 lines above for */
        for i in range(idx, max(-1, idx - 15), -1):
            if i < len(content) and "*/" in content[i]:
                # find leading whitespace before */
                match = re.search(r'^(\s*)\*/', content[i])
                ws = match.group(1) if match else "     "
                insert_idx = i
                found_end_comment = True
                ws_text = ws + " * " + tag_text + "\n"
                break
                
        if found_end_comment:
            content.insert(insert_idx, ws_text)
        else:
            # find leading whitespace of the method
            match = re.search(r'^(\s*)', content[idx])
            ws = match.group(1) if match else "    "
            content.insert(idx, f"{ws}/**\n{ws} * {tag_text}\n{ws} */\n")
            
    with open(real_path, "w", encoding="utf-8") as f:
        f.writelines(content)

print("Applied fixes to all files.")
